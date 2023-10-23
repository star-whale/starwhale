import time
import unittest
from typing import Optional

from fastapi.testclient import TestClient

from starwhale.cli.assistance import broker


class TestApi(unittest.TestCase):
    def setUp(self) -> None:
        broker.settings.broker_url = "broker"
        broker.settings.chunk_size = 4
        broker.settings.max_chunk_count = 4

        self.client = TestClient(broker.app)
        self.session_id = self.client.post("/session").json()["hostCmd"].split("/")[-1]
        resp = self.client.post(
            f"/session/{self.session_id}/command",
            json={"command_id": "test", "args": ["a", "b", "c"]},
        )
        if resp.status_code != 200:
            self.fail(resp.json())

    def tearDown(self) -> None:
        broker.session_manager.stop_garbage_collector()
        broker.session_manager.stop_keep_alive_monitor()

    def test_command(self) -> None:
        resp = self.client.get(f"/session/{self.session_id}/command")
        if resp.status_code != 200:
            self.fail(resp.json())
        self.assertEqual(
            {
                "commands": [
                    {"command_id": "test", "args": ["a", "b", "c"], "exitCode": None}
                ]
            },
            resp.json(),
        )
        command_id = resp.json()["commands"][0]["command_id"]
        resp = self.client.post(
            f"/session/{self.session_id}/command/{command_id}?exit_code=0"
        )
        if resp.status_code != 200:
            self.fail(resp.json())
        resp = self.client.get(f"/session/{self.session_id}/command")
        if resp.status_code != 200:
            self.fail(resp.json())
        self.assertEqual(
            {
                "commands": [
                    {"command_id": "test", "args": ["a", "b", "c"], "exitCode": 0}
                ]
            },
            resp.json(),
        )

    def _read(self, offset: int) -> Optional[bytes]:
        resp = self.client.get(
            f"/session/{self.session_id}/command/test/file/stdin?offset={offset}"
        )
        if resp.status_code != 200:
            self.fail(resp.json())
        if "x-wait-for-data" in resp.headers:
            return None
        return resp.content

    def _write(self, data: bytes, offset: int) -> Optional[int]:
        resp = self.client.post(
            f"/session/{self.session_id}/command/test/file/stdin?offset={offset}",
            content=data,
        )
        if resp.status_code != 200:
            self.fail(resp.json())
        return resp.json()["count"]  # type: ignore

    def test_read_write(self) -> None:
        self.assertEqual(4, self._write(b"1234", 0))
        self.assertEqual(3, self._write(b"111", 0))
        self.assertEqual(5, self._write(b"12345", 0))
        self.assertEqual(4, self._write(b"6789", 5))
        self.assertEqual(3, self._write(b"890", 7))
        self.assertEqual(6, self._write(b"abcdefg", 10))
        self.assertEqual(0, self._write(b"g", 16))
        resp = self.client.post(
            f"/session/{self.session_id}/command/test/file/stdin?offset=17",
            content=b"g",
        )
        if resp.status_code != 400:
            self.fail(resp.json())
        self.assertEqual(b"1234", self._read(0))
        self.assertEqual(b"234", self._read(1))
        self.assertEqual(b"678", self._read(5))
        self.assertEqual(4, self._write(b"ghijk", 16))
        self.assertEqual(b"ghij", self._read(16))
        self.assertIsNone(self._read(20))
        self.assertEqual(5, self._write(b"ghijk", 16))
        self.assertEqual(b"k", self._read(20))
        self.assertEqual(4, self._write(b"1234", 21))
        self.assertEqual(4, self._write(b"5678", 25))
        self.assertEqual(4, self._write(b"90ab", 29))
        self.assertEqual(3, self._write(b"cdef", 33))
        self.assertEqual(b"123", self._read(21))
        resp = self.client.post(
            f"/session/{self.session_id}/command/test/file/stdin/closeWrite"
        )
        if resp.status_code != 200:
            self.fail(resp.json())
        self.assertEqual(b"", self._read(36))

    def test_read_close(self) -> None:
        resp = self.client.post(
            f"/session/{self.session_id}/command/test/file/stdin/closeRead"
        )
        if resp.status_code != 200:
            self.fail(resp.json())
        resp = self.client.get(
            f"/session/{self.session_id}/command/test/file/stdin?offset=0"
        )
        if resp.status_code != 400:
            self.fail(resp.json())
        resp = self.client.post(
            f"/session/{self.session_id}/command/test/file/stdin?offset=0", content=b""
        )
        if resp.status_code != 400:
            self.fail(resp.json())

    def test_garbage_collection(self) -> None:
        broker.settings.gc_interval_seconds = 0.5
        broker.settings.gc_timeout_seconds = 2
        broker.session_manager.start_garbage_collector()
        resp = self.client.post(
            f"/session/{self.session_id}/command",
            json={"command_id": "tt", "args": ["a", "b", "c"]},
        )
        if resp.status_code != 200:
            self.fail(resp.json())
        resp = self.client.get(f"/session/{self.session_id}/command")
        if resp.status_code != 200:
            self.fail(resp.json())
        self.assertEqual(
            {
                "commands": [
                    {"command_id": "test", "args": ["a", "b", "c"], "exitCode": None},
                    {"command_id": "tt", "args": ["a", "b", "c"], "exitCode": None},
                ]
            },
            resp.json(),
        )
        for _ in range(5):
            self._read(0)
            self._write(b"", 0)
            time.sleep(0.5)
        resp = self.client.get(f"/session/{self.session_id}/command")
        if resp.status_code != 200:
            self.fail(resp.json())
        self.assertEqual(
            {
                "commands": [
                    {"command_id": "test", "args": ["a", "b", "c"], "exitCode": None},
                ]
            },
            resp.json(),
        )
        time.sleep(3)
        resp = self.client.get(f"/session/{self.session_id}/command")
        if resp.status_code != 404:
            self.fail(resp.json())

    def test_keep_alive_monitor(self) -> None:
        broker.settings.keep_alive_seconds = 2
        broker.settings.keep_alive_check_interval_seconds = 1
        broker.session_manager.start_keep_alive_monitor()
        resp = self.client.get(
            f"/session/{self.session_id}/command/test/file/stdin?offset=0"
        )
        if resp.status_code != 200:
            self.fail(resp.json())
        time.sleep(3)
        resp = self.client.get(
            f"/session/{self.session_id}/command/test/file/stdin?offset=0"
        )
        if resp.status_code != 400:
            self.fail(resp.json())
