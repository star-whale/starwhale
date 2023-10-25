import os
import time
import random
import string
import threading
import traceback
from typing import IO, Any, List, Union, TextIO, Callable, Optional
from datetime import datetime

import requests

from starwhale.utils import console


def random_id() -> str:
    return "".join(
        [random.choice(string.ascii_letters + string.digits) for i in range(8)]
    )


def check_status_code(resp: requests.Response) -> None:
    if resp.status_code != 200:
        msg = f"status code={resp.status_code} body={resp.json()}"
        if resp.status_code // 100 == 4:
            raise UnrecoverableError(msg)
        raise RuntimeError(msg)


class UnrecoverableError(Exception):
    pass


class InvalidOffsetError(Exception):
    pass


class ClosedError(Exception):
    pass


class TaskRunner(threading.Thread):
    def __init__(self, name: str) -> None:
        super().__init__(name=name)
        self.stopped = False
        self.daemon = True

    def stop(self) -> None:
        self.stopped = True

    def run_until_success(
        self,
        f: Callable,
        ignore_stopped: bool = False,
        *f_args: Any,
        **f_kwargs: Any,
    ) -> Any:
        backoff = 0.1
        while not self.stopped or ignore_stopped:
            try:
                return f(*f_args, **f_kwargs)
            except UnrecoverableError:
                raise
            except Exception:
                console.print("===Retry due to exception===")
                traceback.print_exc()
                console.print("======")
            time.sleep(backoff)
            backoff *= 2
            if backoff > 1:
                backoff = 1


class PeriodicRunner(TaskRunner):
    def __init__(self, name: str, f: Callable[[], None], interval: float):
        super().__init__(name)
        self.f = f
        self.interval = interval

    def run(self) -> None:
        while not self.stopped:
            try:
                start_time = datetime.now().timestamp()
                while not self.stopped:
                    now = datetime.now().timestamp()
                    elasped = now - start_time
                    if elasped > self.interval:
                        break
                    time.sleep(min(1, self.interval - elasped))
                if not self.stopped:
                    self.f()
            except Exception:
                traceback.print_exc()


class ChunkBuffer:
    def __init__(self, chunk_size: int, max_chunk_count: int):
        self.chunk_size = chunk_size
        self.max_chunk_count = max_chunk_count
        self.chunks: List[bytes] = []
        self.offset = 0
        self.length = 0
        self.lock = threading.Condition()
        self.error: Optional[str] = None

    def write(self, offset: int, data: bytes, timeout: float) -> int:
        with self.lock:
            if self.error is not None:
                raise ClosedError(self.error)
            if offset > self.length:
                raise InvalidOffsetError(f"invalid offset {offset}")
            skip_len = self.length - offset
            if skip_len >= len(data):
                return len(data)
            data = data[skip_len:]
            index = 0
            while index < len(data):
                if self.error is not None:
                    raise ClosedError(self.error)
                if len(self.chunks) == 0 or len(self.chunks[-1]) == self.chunk_size:
                    if len(self.chunks) == self.max_chunk_count:
                        if index == 0 and self.lock.wait(timeout):
                            continue
                        break
                    self.chunks.append(b"")
                    available = self.chunk_size
                else:
                    available = self.chunk_size - len(self.chunks[-1])
                if available > len(data) - index:
                    available = len(data) - index
                self.chunks[-1] += data[index : index + available]
                index += available
            self.lock.notify_all()
            self.length += index
            return index + skip_len

    def read(self, offset: int, timeout: float) -> Optional[bytes]:
        with self.lock:
            if offset < self.offset or offset > self.length:
                raise InvalidOffsetError(f"invalid offset {offset}")
            while self.error is None and offset == self.length:
                if not self.lock.wait(timeout):
                    return None
            if self.error is not None and self.error != "":
                raise ClosedError(self.error)
            if offset == self.length:
                return b""
            index = 0
            while self.offset + len(self.chunks[index]) <= offset:
                self.offset += len(self.chunks[index])
                index += 1
            if index > 0:
                self.chunks = self.chunks[index:]
                self.lock.notify_all()
            return self.chunks[0][offset - self.offset :]

    def close(self, error: str) -> None:
        with self.lock:
            if self.error is None:
                self.error = error
                self.lock.notify_all()


class ChunkBufferReader:
    def __init__(self, chunk_buffer: ChunkBuffer):
        self.chunk_buffer = chunk_buffer
        self.offset = 0

    def read(self, timeout: float) -> Optional[bytes]:
        data = self.chunk_buffer.read(self.offset, timeout)
        if data is not None:
            self.offset += len(data)
        return data

    def close(self, error: str) -> None:
        self.chunk_buffer.close(error)


class ChunkBufferWriter:
    def __init__(self, chunk_buffer: ChunkBuffer):
        self.chunk_buffer = chunk_buffer
        self.offset = 0

    def write(self, data: bytes, timeout: float) -> int:
        count = self.chunk_buffer.write(self.offset, data, timeout)
        self.offset += count
        return count

    def close(self, error: str) -> None:
        self.chunk_buffer.close(error)


class FileReader(TaskRunner):
    def __init__(
        self,
        name: str,
        file: Union[TextIO, IO[bytes]],
        chunk_buffer: ChunkBuffer,
    ):
        super().__init__(name)
        self.file = file
        self.chunk_buffer_writer = ChunkBufferWriter(chunk_buffer)

    def run(self) -> None:
        try:
            fd = self.file.fileno()
            while not self.stopped:
                data = os.read(fd, 16384)
                if len(data) == 0:
                    self.chunk_buffer_writer.close("")
                    return
                while not self.stopped and len(data) > 0:
                    count = self.chunk_buffer_writer.write(data, 0.5)
                    data = data[count:]
        except Exception as e:
            console.print("===Stop read from file===")
            traceback.print_exc()
            console.print("======")
            self.chunk_buffer_writer.close(str(e))
        finally:
            self.file.close()


class FileWriter(TaskRunner):
    def __init__(
        self,
        name: str,
        file: Union[TextIO, IO[bytes]],
        chunk_buffer: ChunkBuffer,
    ):
        super().__init__(name)
        self.file = file
        self.chunk_buffer_reader = ChunkBufferReader(chunk_buffer)
        self.offset = 0

    def run(self) -> None:
        try:
            fd = self.file.fileno()
            while not self.stopped:
                data = self.chunk_buffer_reader.read(0.5)
                if data is not None:
                    if self.stopped or len(data) == 0:
                        return
                    self.offset += len(data)
                    while not self.stopped and len(data) > 0:
                        count = os.write(fd, data)
                        data = data[count:]
        except Exception as e:
            console.print("===Stop write to file===")
            traceback.print_exc()
            console.print("======")
            self.chunk_buffer_reader.close(str(e))
        finally:
            self.file.close()


class BrokerReader(TaskRunner):
    def __init__(
        self,
        name: str,
        broker_url: str,
        chunk_buffer: ChunkBuffer,
    ):
        super().__init__(name)
        self.broker_url = broker_url
        self.chunk_buffer_writer = ChunkBufferWriter(chunk_buffer)
        self.offset = 0

    def run(self) -> None:
        try:
            while not self.stopped:
                data = self.run_until_success(self._read_from_broker)
                if data is not None:
                    if len(data) == 0:
                        self.chunk_buffer_writer.close("")
                        return
                    self.offset += len(data)
                    while not self.stopped and len(data) > 0:
                        count = self.chunk_buffer_writer.write(data, 0.5)
                        data = data[count:]
        except Exception as e:
            console.print("===Stop read from broker===")
            traceback.print_exc()
            console.print("======")
            self.chunk_buffer_writer.close(str(e))
        finally:
            self.run_until_success(self._close_read, True)

    def _read_from_broker(self) -> Optional[bytes]:
        resp = requests.get(self.broker_url, params={"offset": self.offset}, timeout=5)
        check_status_code(resp)
        if "x-wait-for-data" in resp.headers:
            return None
        return resp.content

    def _close_read(self) -> None:
        resp = requests.post(f"{self.broker_url}/closeRead", timeout=5)
        check_status_code(resp)


class BrokerWriter(TaskRunner):
    def __init__(
        self,
        name: str,
        broker_url: str,
        chunk_buffer: ChunkBuffer,
    ):
        super().__init__(name)
        self.broker_url = broker_url
        self.chunk_buffer_reader = ChunkBufferReader(chunk_buffer)
        self.offset = 0

    def run(self) -> None:
        try:
            while not self.stopped:
                data = self.chunk_buffer_reader.read(0.5)
                if data is not None:
                    if self.stopped or len(data) == 0:
                        return
                    d = data
                    while not self.stopped and len(d) > 0:
                        count = self.run_until_success(self._write_to_broker, False, d)
                        self.offset += count
                        d = d[count:]
        except Exception as e:
            console.print("===Stop write to broker===")
            traceback.print_exc()
            console.print("======")
            self.chunk_buffer_reader.close(str(e))
        finally:
            self.run_until_success(self._close_write, True)

    def _write_to_broker(self, data: bytes) -> int:
        resp = requests.post(
            self.broker_url, params={"offset": self.offset}, data=data, timeout=5
        )
        check_status_code(resp)
        return resp.json()["count"]  # type: ignore

    def _close_write(self) -> None:
        resp = requests.post(f"{self.broker_url}/closeWrite", timeout=5)
        check_status_code(resp)
