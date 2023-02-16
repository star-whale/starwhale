import sys
from typing import List

import requests

from .common import (
    random_id,
    FileReader,
    FileWriter,
    TaskRunner,
    ChunkBuffer,
    BrokerReader,
    BrokerWriter,
    PeriodicRunner,
    check_status_code,
)


class CommandRunner(TaskRunner):
    def __init__(self, name: str, broker_base_url: str, args: List[str]):
        super().__init__(name)
        self.broker_base_url = broker_base_url
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
            command_id = random_id()
            self.run_until_success(
                lambda: self.new_command(
                    f"{self.broker_base_url}/command", command_id, self.args
                )
            )
            if not self.stopped:
                base_url = f"{self.broker_base_url}/command/{command_id}"
                keep_alive = PeriodicRunner(
                    f"{self.name}-keep-alive", lambda: self.keep_alive(base_url), 1
                )
                keep_alive.start()
                stdin_buffer = ChunkBuffer(256 * 1024, 64)
                stdin_reader = FileReader(
                    f"{self.name}-stdin-reader", sys.stdin, stdin_buffer
                )
                stdin_reader.start()
                stdin_writer = BrokerWriter(
                    f"{self.name}-stdin-writer", f"{base_url}/file/stdin", stdin_buffer
                )
                stdin_writer.start()
                stdout_buffer = ChunkBuffer(256 * 1024, 64)
                stdout_reader = BrokerReader(
                    f"{self.name}-stdout-reader",
                    f"{base_url}/file/stdout",
                    stdout_buffer,
                )
                stdout_reader.start()
                stdout_writer = FileWriter(
                    f"{self.name}-stdout-writer", sys.stdout, stdout_buffer
                )
                stdout_writer.start()
                stderr_buffer = ChunkBuffer(256 * 1024, 64)
                stderr_reader = BrokerReader(
                    f"{self.name}-stderr-reader",
                    f"{base_url}/file/stderr",
                    stderr_buffer,
                )
                stderr_reader.start()
                stderr_writer = FileWriter(
                    f"{self.name}-stderr-writer", sys.stderr, stderr_buffer
                )
                stderr_writer.start()
                while not self.stopped and stdout_writer.is_alive():
                    stdout_writer.join(0.5)
                while not self.stopped and stderr_writer.is_alive():
                    stderr_writer.join(0.5)
        finally:
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

    def new_command(self, broker_url: str, command_id: str, args: List[str]) -> None:
        resp = requests.post(
            broker_url, json={"command_id": command_id, "args": args}, timeout=5
        )
        check_status_code(resp)

    def keep_alive(self, url: str) -> None:
        requests.post(f"{url}/keepAlive?end=remote", timeout=5)
