import os
import time
import unittest
import threading
from unittest.mock import call, Mock, patch

import requests
from requests.structures import CaseInsensitiveDict

from starwhale.cli.assistance import common


class TestCheckStatusCode(unittest.TestCase):
    def test_200(self) -> None:
        resp = requests.Response()
        resp.status_code = 200
        common.check_status_code(resp)

    def test_400(self) -> None:
        resp = requests.Response()
        resp.status_code = 400
        resp._content = b"{}"
        with self.assertRaises(common.UnrecoverableError):
            common.check_status_code(resp)

    def test_500(self) -> None:
        resp = requests.Response()
        resp.status_code = 500
        resp._content = b"{}"
        with self.assertRaises(RuntimeError):
            common.check_status_code(resp)


class TestTaskRunner(unittest.TestCase):
    def setUp(self) -> None:
        self.runner = common.TaskRunner("runner")

    def tearDown(self) -> None:
        if self.runner is not None:
            self.runner.stop()

    def testRunUntilSuccess(self) -> None:
        self.assertEqual(0, self.runner.run_until_success(lambda: 0))

        def error1() -> None:
            raise common.UnrecoverableError("unrecoverable")

        with self.assertRaises(common.UnrecoverableError):
            self.runner.run_until_success(error1)

        counter = [0]

        def error2() -> int:
            counter[0] += 1
            if counter[0] == 3:
                return 0
            raise RuntimeError("test")

        self.assertEqual(0, self.runner.run_until_success(error2))

        def error3() -> None:
            raise RuntimeError("test")

        def stop() -> None:
            time.sleep(1)
            self.runner.stop()

        threading.Thread(target=stop).start()
        self.assertIsNone(self.runner.run_until_success(error3))


class TestPeriodicRunner(unittest.TestCase):
    def test_run(self) -> None:
        counter = [0]

        def f() -> None:
            counter[0] += 1

        runner = common.PeriodicRunner("runner", f, 2)
        try:
            runner.start()
            time.sleep(5)
            runner.stop()
            runner.join()
            self.assertEqual(2, counter[0])
        finally:
            runner.stop()


class TestChunkBuffer(unittest.TestCase):
    def setUp(self) -> None:
        self.buffer = common.ChunkBuffer(4, 4)

    def test_read_write(self) -> None:
        self.assertEqual(4, self.buffer.write(0, b"1234", 0.5))
        self.assertEqual(3, self.buffer.write(0, b"111", 0.5))
        self.assertEqual(5, self.buffer.write(0, b"12345", 0.5))
        self.assertEqual(4, self.buffer.write(5, b"6789", 0.5))
        self.assertEqual(3, self.buffer.write(7, b"890", 0.5))
        self.assertEqual(6, self.buffer.write(10, b"abcdefg", 0.5))
        self.assertEqual(0, self.buffer.write(16, b"g", 0.5))
        with self.assertRaises(common.InvalidOffsetError):
            self.buffer.write(17, b"g", 0.5)
        self.assertEqual(b"1234", self.buffer.read(0, 0.5))
        self.assertEqual(b"234", self.buffer.read(1, 0.5))
        self.assertEqual(b"5678", self.buffer.read(4, 0.5))
        self.assertEqual(b"678", self.buffer.read(5, 0.5))
        self.assertEqual(4, self.buffer.write(16, b"ghijk", 0.5))
        self.assertEqual(b"ghij", self.buffer.read(16, 0.5))
        self.assertIsNone(self.buffer.read(20, 0.5))
        self.assertEqual(5, self.buffer.write(16, b"ghijk", 0.5))
        self.assertEqual(b"k", self.buffer.read(20, 0.5))
        self.assertEqual(15, self.buffer.write(21, b"1234567890abcdef", 0.5))
        self.assertEqual(b"123", self.buffer.read(21, 0.5))
        self.assertEqual(b"3", self.buffer.read(23, 0.5))
        self.assertEqual(b"4567", self.buffer.read(24, 0.5))
        self.buffer.close("")
        self.assertEqual(b"890a", self.buffer.read(28, 0.5))
        self.assertEqual(b"bcde", self.buffer.read(32, 0.5))
        self.assertEqual(b"", self.buffer.read(36, 0.5))
        with self.assertRaises(common.ClosedError):
            self.buffer.write(36, b"0", 0.5)
        self.buffer.error = "e"
        with self.assertRaises(common.ClosedError):
            self.buffer.read(36, 0.5)


class TestChunkBufferReader(unittest.TestCase):
    def test(self) -> None:
        buffer = common.ChunkBuffer(4, 4)
        reader = common.ChunkBufferReader(buffer)
        self.assertEqual(3, buffer.write(0, b"123", 0.5))
        self.assertEqual(b"123", reader.read(0.5))
        self.assertEqual(6, buffer.write(0, b"123456", 0.5))
        self.assertEqual(b"4", reader.read(0.5))
        self.assertEqual(b"56", reader.read(0.5))
        self.assertIsNone(reader.read(0.5))
        self.assertEqual(14, buffer.write(6, b"1234567890abcd", 0.5))
        buffer.close("")
        self.assertEqual(b"12", reader.read(0.5))
        self.assertEqual(b"3456", reader.read(0.5))
        self.assertEqual(b"7890", reader.read(0.5))
        self.assertEqual(b"abcd", reader.read(0.5))
        self.assertEqual(b"", reader.read(0.5))


class TestChunkBufferWriter(unittest.TestCase):
    def test(self) -> None:
        buffer = common.ChunkBuffer(4, 4)
        writer = common.ChunkBufferWriter(buffer)
        self.assertEqual(3, writer.write(b"123", 0.5))
        self.assertEqual(3, writer.write(b"456", 0.5))
        self.assertEqual(10, writer.write(b"7890abcdefg", 0.5))
        self.assertEqual(b"1234", buffer.read(0, 0.5))
        self.assertEqual(b"5678", buffer.read(4, 0.5))
        self.assertEqual(4, writer.write(b"123456", 0.5))
        self.assertEqual(0, writer.write(b"7", 0.5))
        buffer.close("")
        with self.assertRaises(common.ClosedError):
            writer.write(b"7", 0.5)


class TestFileReader(unittest.TestCase):
    def test(self) -> None:
        r, w = os.pipe()
        f = os.fdopen(r, "rb")
        buffer = common.ChunkBuffer(4, 4)
        reader = common.FileReader("reader", f, buffer)
        try:
            reader.start()
            os.write(w, b"123")
            self.assertEqual(b"123", buffer.read(0, 0.5))
            os.write(w, b"4567890abcdef")
            time.sleep(0.5)
            self.assertEqual(b"1234", buffer.read(0, 0.5))
            os.write(w, b"ghi")
            os.close(w)
            time.sleep(0.5)
            self.assertEqual(b"1234", buffer.read(0, 0.5))
            self.assertEqual(b"5678", buffer.read(4, 0.5))
            self.assertEqual(b"90ab", buffer.read(8, 0.5))
            self.assertEqual(b"cdef", buffer.read(12, 0.5))
            self.assertEqual(b"ghi", buffer.read(16, 0.5))
            self.assertEqual(b"", buffer.read(19, 0.5))
        finally:
            reader.stop()
            try:
                os.close(r)
            except OSError:
                pass
            try:
                os.close(w)
            except OSError:
                pass


class TestFileWriter(unittest.TestCase):
    def test(self) -> None:
        r, w = os.pipe()
        writer = None
        try:
            fin = os.fdopen(r, "rb")
            fout = os.fdopen(w, "wb")
            buffer = common.ChunkBuffer(4, 4)
            writer = common.FileWriter("writer", fout, buffer)
            writer.start()
            self.assertEqual(3, buffer.write(0, b"123", 0.5))
            self.assertEqual(b"123", fin.read(3))
            self.assertEqual(3, buffer.write(3, b"456", 0.5))
            self.assertEqual(b"456", fin.read(3))
            self.assertEqual(14, buffer.write(6, b"1234567890abcd", 0.5))
            buffer.close("")
            self.assertEqual(b"1234567890abcd", fin.read())
        finally:
            if writer is not None:
                writer.stop()
            try:
                os.close(r)
            except OSError:
                pass
            try:
                os.close(w)
            except OSError:
                pass


class TestBrokerReader(unittest.TestCase):
    @patch("starwhale.cli.assistance.common.requests.post")
    @patch("starwhale.cli.assistance.common.requests.get")
    def test_normal(self, mock_get: Mock, mock_post: Mock) -> None:
        responses = [
            requests.Response(),
            requests.Response(),
            requests.Response(),
            requests.Response(),
            requests.Response(),
        ]
        responses[0].status_code = 200
        responses[0]._content = b"123"
        responses[1].status_code = 500
        responses[1]._content = b"{}"
        responses[2].status_code = 200
        responses[2]._content = b""
        responses[2].headers = CaseInsensitiveDict({"x-wait-for-data": ""})
        responses[3].status_code = 200
        responses[3]._content = b"45"
        responses[4].status_code = 200
        responses[4]._content = b""
        mock_get.side_effect = responses
        post_resp = requests.Response()
        post_resp.status_code = 200
        post_resp._content = b"{}"
        mock_post.return_value = post_resp
        buffer = common.ChunkBuffer(4, 4)
        reader = common.BrokerReader("reader", "broker", buffer)
        try:
            reader.start()
            reader.join()
            self.assertEqual(b"1234", buffer.read(0, 0.5))
            self.assertEqual(b"5", buffer.read(4, 0.5))
            mock_get.assert_has_calls(
                [
                    call("broker", params={"offset": 0}, timeout=5),
                    call("broker", params={"offset": 3}, timeout=5),
                    call("broker", params={"offset": 3}, timeout=5),
                    call("broker", params={"offset": 3}, timeout=5),
                    call("broker", params={"offset": 5}, timeout=5),
                ]
            )
        finally:
            reader.stop()

    @patch("starwhale.cli.assistance.common.requests.post")
    @patch("starwhale.cli.assistance.common.requests.get")
    def test_read_fail(self, mock_get: Mock, mock_post: Mock) -> None:
        get_resp = requests.Response()
        get_resp.status_code = 400
        get_resp._content = b"{}"
        mock_get.return_value = get_resp
        post_resp = requests.Response()
        post_resp.status_code = 200
        post_resp._content = b"{}"
        mock_post.return_value = post_resp
        buffer = common.ChunkBuffer(4, 4)
        reader = common.BrokerReader("reader", "broker", buffer)
        try:
            reader.start()
            reader.join()
            with self.assertRaises(common.ClosedError):
                buffer.read(0, 0.5)
            mock_get.assert_called_once_with("broker", params={"offset": 0}, timeout=5)
            mock_post.assert_called_once_with("broker/closeRead", timeout=5)
        finally:
            reader.stop()

    @patch("starwhale.cli.assistance.common.requests.post")
    @patch("starwhale.cli.assistance.common.requests.get")
    def test_write_fail(self, mock_get: Mock, mock_post: Mock) -> None:
        get_resp = requests.Response()
        get_resp.status_code = 200
        get_resp._content = b"123"
        mock_get.return_value = get_resp
        post_resp = requests.Response()
        post_resp.status_code = 200
        post_resp._content = b"{}"
        mock_post.return_value = post_resp
        buffer = common.ChunkBuffer(4, 4)
        buffer.close("read close")
        reader = common.BrokerReader("reader", "broker", buffer)
        try:
            reader.start()
            reader.join()
            mock_get.assert_called_once_with("broker", params={"offset": 0}, timeout=5)
            mock_post.assert_called_once_with("broker/closeRead", timeout=5)
        finally:
            reader.stop()


class TestBrokerWriter(unittest.TestCase):
    @patch("starwhale.cli.assistance.common.requests.post")
    def test_normal(self, mock_post: Mock) -> None:
        responses = [
            requests.Response(),
            requests.Response(),
            requests.Response(),
            requests.Response(),
            requests.Response(),
            requests.Response(),
        ]
        responses[0].status_code = 200
        responses[0]._content = b'{"count":3}'
        responses[1].status_code = 500
        responses[1]._content = b"{}"
        responses[2].status_code = 200
        responses[2]._content = b'{"count":0}'
        responses[3].status_code = 200
        responses[3]._content = b'{"count":1}'
        responses[4].status_code = 200
        responses[4]._content = b'{"count":1}'
        responses[5].status_code = 200
        responses[5]._content = b"{}"
        mock_post.side_effect = responses
        buffer = common.ChunkBuffer(4, 4)
        writer = common.BrokerWriter("writer", "broker", buffer)
        try:
            writer.start()
            buffer.write(0, b"123", 0.5)
            time.sleep(0.5)
            buffer.write(3, b"45", 0.5)
            buffer.close("")
            writer.join()
            mock_post.assert_has_calls(
                [
                    call("broker", params={"offset": 0}, data=b"123", timeout=5),
                    call("broker", params={"offset": 3}, data=b"4", timeout=5),
                    call("broker", params={"offset": 3}, data=b"4", timeout=5),
                    call("broker", params={"offset": 3}, data=b"4", timeout=5),
                    call("broker", params={"offset": 4}, data=b"5", timeout=5),
                    call("broker/closeWrite", timeout=5),
                ]
            )
        finally:
            writer.stop()

    @patch("starwhale.cli.assistance.common.requests.post")
    def test_write_fail(self, mock_post: Mock) -> None:
        resp = requests.Response()
        resp.status_code = 400
        resp._content = b"{}"
        mock_post.return_value = resp
        buffer = common.ChunkBuffer(4, 4)
        writer = common.BrokerWriter("writer", "broker", buffer)
        try:
            writer.start()
            buffer.write(0, b"123", 0.5)
            buffer.close("")
            writer.join()
            mock_post.assert_any_call(
                "broker", params={"offset": 0}, data=b"123", timeout=5
            )
            mock_post.assert_any_call("broker/closeWrite", timeout=5)
        finally:
            writer.stop()

    @patch("starwhale.cli.assistance.common.requests.post")
    def test_read_fail(self, mock_post: Mock) -> None:
        resp = requests.Response()
        resp.status_code = 200
        resp._content = b"{}"
        mock_post.return_value = resp
        buffer = common.ChunkBuffer(4, 4)
        buffer.close("fail")
        writer = common.BrokerWriter("writer", "broker", buffer)
        try:
            writer.start()
            writer.join()
            mock_post.assert_called_once_with("broker/closeWrite", timeout=5)
        finally:
            writer.stop()
