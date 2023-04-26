import time
import subprocess
from typing import Any, Dict, List

import requests

from starwhale.utils import console

from .common import (
    FileReader,
    FileWriter,
    TaskRunner,
    ChunkBuffer,
    BrokerReader,
    BrokerWriter,
    PeriodicRunner,
    check_status_code,
)


class CommandExecutor(TaskRunner):
    def __init__(self, name: str, broker_url: str, args: List[str]):
        super().__init__(name)
        self.broker_url = broker_url
        self.args = args

    def run(self) -> None:
        stdin_reader = None
        stdin_writer = None
        stdout_reader = None
        stdout_writer = None
        stderr_reader = None
        stderr_writer = None
        keep_alive = None
        try:
            keep_alive = PeriodicRunner(f"{self.name}-keep-alive", self.keep_alive, 1)
            keep_alive.start()
            with subprocess.Popen(
                self.args,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            ) as p:
                assert p.stdin is not None
                stdin_buffer = ChunkBuffer(256 * 1024, 64)
                stdin_writer = FileWriter(
                    f"{self.name}-stdin-writer", p.stdin, stdin_buffer
                )
                stdin_writer.start()
                stdin_reader = BrokerReader(
                    f"{self.name}-stdin-reader",
                    f"{self.broker_url}/file/stdin",
                    stdin_buffer,
                )
                stdin_reader.start()
                assert p.stdout is not None
                stdout_buffer = ChunkBuffer(256 * 1024, 64)
                stdout_reader = FileReader(
                    f"{self.name}-stdout-reader", p.stdout, stdout_buffer
                )
                stdout_reader.start()
                stdout_writer = BrokerWriter(
                    f"{self.name}-stdout-writer",
                    f"{self.broker_url}/file/stdout",
                    stdout_buffer,
                )
                stdout_writer.start()
                assert p.stderr is not None
                stderr_buffer = ChunkBuffer(256 * 1024, 64)
                stderr_reader = FileReader(
                    f"{self.name}-stderr-reader", p.stderr, stderr_buffer
                )
                stderr_reader.start()
                stderr_writer = BrokerWriter(
                    f"{self.name}-stderr-writer",
                    f"{self.broker_url}/file/stderr",
                    stderr_buffer,
                )
                stderr_writer.start()
                while not self.stopped:
                    try:
                        exit_code = p.wait(0.5)
                        break
                    except subprocess.TimeoutExpired:
                        pass
                if self.stopped:
                    p.terminate()
                    exit_code = p.wait()
                self.run_until_success(lambda: self.update_exit_code(exit_code))
                while not self.stopped and stdout_writer.is_alive():
                    stdout_writer.join(0.5)
                while not self.stopped and stderr_writer.is_alive():
                    stderr_writer.join(0.5)
        finally:
            if stdin_reader is not None:
                stdin_reader.stop()
                stdin_reader.join()
            if stdin_writer is not None:
                stdin_writer.stop()
                stdin_writer.join()
            if stdout_reader is not None:
                stdout_reader.stop()
                stdout_reader.join()
            if stdout_writer is not None:
                stdout_writer.stop()
                stdout_writer.join()
            if stderr_reader is not None:
                stderr_reader.stop()
                stderr_reader.join()
            if stderr_writer is not None:
                stderr_writer.stop()
                stderr_writer.join()
            if keep_alive is not None:
                keep_alive.stop()
                keep_alive.join()

    def keep_alive(self) -> None:
        requests.post(f"{self.broker_url}/keepAlive?end=host", timeout=5)

    def update_exit_code(self, exit_code: int) -> None:
        resp = requests.post(f"{self.broker_url}?exit_code={exit_code}", timeout=5)
        check_status_code(resp)
        return resp.json()  # type: ignore


class CommandRetriever(TaskRunner):
    def __init__(self, name: str, broker_url: str):
        super().__init__(name)
        self.broker_url = broker_url
        self.executions: Dict[str, CommandExecutor] = {}

    def run(self) -> None:
        try:
            while not self.stopped:
                commands = self.run_until_success(self.retrieve_commands)
                for command in commands:
                    command_id = command["command_id"]
                    args = command["args"]
                    if command_id not in self.executions:
                        console.print(f"run command {command_id}:", args)
                        executor = CommandExecutor(
                            f"command-{command_id}-executor",
                            f"{self.broker_url}/command/{command_id}",
                            args,
                        )
                        executor.start()
                        self.executions[command_id] = executor
                time.sleep(1)
        finally:
            for executor in self.executions.values():
                executor.stop()
                executor.join()

    def retrieve_commands(self) -> List[Dict[str, Any]]:
        resp = requests.get(f"{self.broker_url}/command", timeout=5)
        check_status_code(resp)
        return resp.json()["commands"]  # type: ignore
