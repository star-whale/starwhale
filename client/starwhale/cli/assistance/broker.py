import threading
from typing import Any, Dict, List, Optional
from datetime import datetime

from fastapi import Body, FastAPI, Request, Response, HTTPException

from starwhale.utils.pydantic import PYDANTIC_V2

if PYDANTIC_V2:
    # TODO: remove this when pydantic v1 is no longer supported and use pydantic-settings instead
    from pydantic.v1 import BaseSettings
else:
    from pydantic import BaseSettings  # type: ignore

from starwhale.utils import console

from .common import (
    random_id,
    ChunkBuffer,
    ClosedError,
    PeriodicRunner,
    InvalidOffsetError,
)


class Settings(BaseSettings):  # type: ignore
    broker_url: str = ""
    chunk_size: int = 65536
    max_chunk_count: int = 64
    gc_timeout_seconds: float = 600.0
    gc_interval_seconds: float = 60.0
    keep_alive_seconds: float = 15.0
    keep_alive_check_interval_seconds: float = 5.0


settings = Settings()
app = FastAPI()
global_lock = threading.Lock()


class Command:
    def __init__(self, args: List[str]):
        self.args = args
        self.exit_code: Optional[int] = None
        self.files: Dict[str, ChunkBuffer] = {
            "stdin": ChunkBuffer(settings.chunk_size, settings.max_chunk_count),
            "stdout": ChunkBuffer(settings.chunk_size, settings.max_chunk_count),
            "stderr": ChunkBuffer(settings.chunk_size, settings.max_chunk_count),
        }
        self.last_access_time = datetime.now().timestamp()
        self.host_access_time = self.last_access_time
        self.remote_access_time = self.last_access_time

    def get_file(self, filename: str) -> ChunkBuffer:
        if filename not in self.files:
            raise HTTPException(status_code=404, detail=f"invalid filename {filename}")
        return self.files[filename]


class Session:
    def __init__(self) -> None:
        self.commands: Dict[str, Command] = {}
        self.last_access_time = datetime.now().timestamp()

    def new_command(self, command_id: str, args: List[str]) -> None:
        if command_id not in self.commands:
            self.commands[command_id] = Command(args)

    def get_command(self, command_id: str) -> Command:
        if command_id not in self.commands:
            raise HTTPException(
                status_code=404, detail=f"invalid command id {command_id}"
            )
        command = self.commands[command_id]
        command.last_access_time = datetime.now().timestamp()
        return command

    def list_commands(self) -> List[Dict[str, Any]]:
        ret = []
        for command_id, command in self.commands.items():
            ret.append(
                {
                    "command_id": command_id,
                    "args": command.args,
                    "exitCode": command.exit_code,
                }
            )
        return ret


class SessionManager:
    def __init__(self) -> None:
        self.sessions: Dict[str, Session] = {}
        self.garbage_collector: Optional[PeriodicRunner] = None
        self.keep_alive_monitor: Optional[PeriodicRunner] = None

    def keep_alive_check(self) -> None:
        with global_lock:
            now = datetime.now().timestamp()
            for session in self.sessions.values():
                for command in session.commands.values():
                    if now - command.host_access_time > settings.keep_alive_seconds:
                        command.get_file("stdin").close("host timeout")
                        command.get_file("stdout").close("host timeout")
                        command.get_file("stderr").close("host timeout")
                    if now - command.remote_access_time > settings.keep_alive_seconds:
                        command.get_file("stdin").close("remote timeout")
                        command.get_file("stdout").close("remote timeout")
                        command.get_file("stderr").close("remote timeout")

    def garbage_collect(self) -> None:
        with global_lock:
            now = datetime.now().timestamp()
            new_sessions = {}
            for session_id, session in self.sessions.items():
                if now - session.last_access_time > settings.gc_timeout_seconds:
                    console.print(f"garbage collection: remove session {session_id}")
                else:
                    new_sessions[session_id] = session
                    new_commands = {}
                    for command_id, command in session.commands.items():
                        if now - command.last_access_time > settings.gc_timeout_seconds:
                            console.print(
                                f"garbage collection: remove command {command_id}"
                            )
                        else:
                            new_commands[command_id] = command
                    session.commands = new_commands
            self.sessions = new_sessions

    def start_garbage_collector(self) -> None:
        with global_lock:
            if self.garbage_collector is None:
                self.garbage_collector = PeriodicRunner(
                    "broker-garbage-collector",
                    self.garbage_collect,
                    settings.gc_interval_seconds,
                )
                self.garbage_collector.start()
                console.print("garbage collector started")

    def stop_garbage_collector(self) -> None:
        with global_lock:
            if self.garbage_collector is not None:
                self.garbage_collector.stop()
                self.garbage_collector = None

    def start_keep_alive_monitor(self) -> None:
        with global_lock:
            if self.keep_alive_monitor is None:
                self.keep_alive_monitor = PeriodicRunner(
                    "broker-keep-alive-monitor",
                    self.keep_alive_check,
                    settings.keep_alive_check_interval_seconds,
                )
                self.keep_alive_monitor.start()
                console.print("keep alive monitor started")

    def stop_keep_alive_monitor(self) -> None:
        with global_lock:
            if self.keep_alive_monitor is not None:
                self.keep_alive_monitor.stop()
                self.keep_alive_monitor = None

    def new_session(self) -> str:
        session_id = random_id()
        self.sessions[session_id] = Session()
        return session_id

    def get_session(self, session_id: str) -> Session:
        if session_id not in self.sessions:
            raise HTTPException(
                status_code=404, detail=f"invalid session id {session_id}"
            )
        session = self.sessions[session_id]
        session.last_access_time = datetime.now().timestamp()
        return session


session_manager = SessionManager()


@app.on_event("startup")
def startup_event() -> None:
    session_manager.start_garbage_collector()
    session_manager.start_keep_alive_monitor()


@app.post("/session")
def new_session() -> Dict[str, Any]:
    with global_lock:
        session_id = session_manager.new_session()
        return {
            "hostCmd": f"swcli assistance host --broker={settings.broker_url}/session/{session_id}",
            "remoteCmd": f"swcli assistance remote --broker={settings.broker_url}/session/{session_id} [args]",
        }


@app.post("/session/{session_id}/command")
def run_command(
    session_id: str, command_id: str = Body(), args: List[str] = Body()
) -> None:
    with global_lock:
        session = session_manager.get_session(session_id)
        session.new_command(command_id, args)


@app.post("/session/{session_id}/command/{command_id}")
def update_command_exit_code(session_id: str, command_id: str, exit_code: int) -> None:
    with global_lock:
        session = session_manager.get_session(session_id)
        command = session.get_command(command_id)
        command.exit_code = exit_code


@app.post("/session/{session_id}/command/{command_id}/file/{filename}")
async def write_command_file(
    session_id: str,
    command_id: str,
    filename: str,
    offset: int,
    request: Request,
) -> Dict[str, Any]:
    data = await request.body()
    with global_lock:
        session = session_manager.get_session(session_id)
        command = session.get_command(command_id)
        f = command.get_file(filename)
        if filename == "stdin":
            command.remote_access_time = datetime.now().timestamp()
        else:
            command.host_access_time = datetime.now().timestamp()
    try:
        count = f.write(offset, data, 1)
        return {"count": count}
    except (ClosedError, InvalidOffsetError) as e:
        raise HTTPException(400, str(e)) from e


@app.post("/session/{session_id}/command/{command_id}/file/{filename}/closeRead")
def close_read_command_file(session_id: str, command_id: str, filename: str) -> None:
    with global_lock:
        session = session_manager.get_session(session_id)
        command = session.get_command(command_id)
        f = command.get_file(filename)
    f.close("read close")


@app.post("/session/{session_id}/command/{command_id}/file/{filename}/closeWrite")
def close_write_command_file(session_id: str, command_id: str, filename: str) -> None:
    with global_lock:
        session = session_manager.get_session(session_id)
        command = session.get_command(command_id)
        f = command.get_file(filename)
    f.close("")


@app.get(
    "/session/{session_id}/command/{command_id}/file/{filename}",
    response_class=Response,
    responses={200: {"content": {"application/octet-stream": {}}}},
)
def read_command_file(
    session_id: str, command_id: str, filename: str, offset: int
) -> Response:
    with global_lock:
        session = session_manager.get_session(session_id)
        command = session.get_command(command_id)
        f = command.get_file(filename)
        if filename == "stdin":
            command.host_access_time = datetime.now().timestamp()
        else:
            command.remote_access_time = datetime.now().timestamp()
    try:
        data = f.read(offset, 1)
    except (ClosedError, InvalidOffsetError) as e:
        raise HTTPException(400, str(e)) from e
    if data is None:
        return Response(
            headers={"x-wait-for-data": ""},
            content=b"",
            media_type="application/octet-stream",
        )
    return Response(content=data, media_type="application/octet-stream")


@app.post("/session/{session_id}/command/{command_id}/keepAlive")
def keep_alive(session_id: str, command_id: str, end: str) -> None:
    with global_lock:
        session = session_manager.get_session(session_id)
        command = session.get_command(command_id)
        now = datetime.now().timestamp()
        if end == "host":
            command.host_access_time = now
        else:
            command.remote_access_time = now


@app.get("/session/{session_id}/command")
def list_commands(session_id: str) -> Dict[str, Any]:
    with global_lock:
        session = session_manager.get_session(session_id)
        return {"commands": session.list_commands()}
