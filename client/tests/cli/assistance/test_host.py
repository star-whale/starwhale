import sys
import json
import time
import unittest
from typing import Any, Dict
from unittest.mock import Mock, patch

import requests
from requests.structures import CaseInsensitiveDict

from starwhale.cli.assistance import host


class TestCommandExecutor(unittest.TestCase):
    @patch("starwhale.cli.assistance.host.requests.post")
    @patch("starwhale.cli.assistance.host.requests.get")
    def test_normal(self, mock_get: Mock, mock_post: Mock) -> None:
        def on_get(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            resp._content = b"{}"
            if "params" in kwargs:
                offset = kwargs["params"]["offset"]
                if offset == 0:
                    resp._content = b"1\n22\n333"
                else:
                    resp._content = b""
            return resp

        mock_get.side_effect = on_get
        stdout = b""
        stderr = b""

        def on_post(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            if "data" in kwargs:
                data = kwargs["data"]
                resp._content = bytes(f'{{"count":{len(data)}}}', "ascii")
            else:
                resp._content = b"{}"
            if url == "broker/file/stdout":
                nonlocal stdout
                stdout += data
            elif url == "broker/file/stderr":
                nonlocal stderr
                stderr += data
            return resp

        mock_post.side_effect = on_post

        executor = host.CommandExecutor(
            "host",
            "broker",
            [
                sys.executable,
                "-c",
                "import sys\nfor line in sys.stdin:\n print(line.strip(), end='')\nprint('err', file=sys.stderr, end='')",
            ],
        )
        try:
            executor.start()
            executor.join()
        finally:
            executor.stop()
        self.assertEqual(b"122333", stdout)
        self.assertEqual(b"err", stderr)
        mock_post.assert_any_call("broker?exit_code=0", timeout=5)

    @patch("starwhale.cli.assistance.host.requests.post")
    @patch("starwhale.cli.assistance.host.requests.get")
    def test_write_fail(self, mock_get: Mock, mock_post: Mock) -> None:
        get_resp = requests.Response()
        get_resp.status_code = 200
        get_resp._content = b""
        get_resp.headers = CaseInsensitiveDict({"x-wait-for-data": ""})
        mock_get.return_value = get_resp

        def on_post(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            resp._content = b"{}"
            if url == "broker/file/stdout":
                resp.status_code = 400
            elif url == "broker/file/stderr":
                data = kwargs["data"]
                resp._content = bytes(f'{{"count":{len(data)}}}', "ascii")
            return resp

        mock_post.side_effect = on_post

        executor = host.CommandExecutor(
            "host",
            "broker",
            [
                sys.executable,
                "-c",
                "import time\nprint('test', end='')\ntime.sleep(1)\nprint('test', end='')",
            ],
        )
        try:
            executor.start()
            executor.join()
        finally:
            executor.stop()
        mock_post.assert_any_call("broker?exit_code=0", timeout=5)

    @patch("starwhale.cli.assistance.host.requests.post")
    @patch("starwhale.cli.assistance.host.requests.get")
    def test_read_fail(self, mock_get: Mock, mock_post: Mock) -> None:
        get_resp = requests.Response()
        get_resp.status_code = 400
        get_resp._content = b"{}"
        mock_get.return_value = get_resp

        def on_post(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            resp._content = b"{}"
            if url == "broker/file/stdout" or url == "broker/file/stderr":
                data = kwargs["data"]
                resp._content = bytes(f'{{"count":{len(data)}}}', "ascii")
            return resp

        mock_post.side_effect = on_post

        executor = host.CommandExecutor(
            "host",
            "broker",
            [
                sys.executable,
                "-c",
                "import sys\nprint(sys.stdin.readline(), end='')",
            ],
        )
        try:
            executor.start()
            executor.join()
        finally:
            executor.stop()
        mock_post.assert_any_call("broker?exit_code=0", timeout=5)


class TestCommandRetriever(unittest.TestCase):
    @patch("starwhale.cli.assistance.host.requests.post")
    @patch("starwhale.cli.assistance.host.requests.get")
    def test_normal(self, mock_get: Mock, mock_post: Mock) -> None:
        commands = [
            {
                "command_id": "1",
                "args": [
                    sys.executable,
                    "-c",
                    "import sys\nprint('1'+sys.stdin.readline().strip(), end='')",
                ],
            },
            {
                "command_id": "2",
                "args": [
                    sys.executable,
                    "-c",
                    "import sys\nprint('2'+sys.stdin.readline().strip(), end='')",
                ],
            },
            {
                "command_id": "3",
                "args": [
                    sys.executable,
                    "-c",
                    "import sys\nprint('3'+sys.stdin.readline().strip(), end='')",
                ],
            },
        ]

        counter = [0]

        def on_get(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            resp._content = b"{}"
            if url == "broker/command":
                nonlocal counter
                nonlocal commands
                if counter[0] == 0:
                    c = commands[:2]
                elif counter[0] == 1:
                    c = commands[1:]
                else:
                    c = commands
                counter[0] += 1
                resp._content = bytes(
                    f'{{"commands":{json.dumps(c)}}}',
                    "ascii",
                )
            elif url.endswith("stdin"):
                if kwargs["params"]["offset"] == 0:
                    if url == "broker/command/1/file/stdin":
                        resp._content = b"1"
                    elif url == "broker/command/2/file/stdin":
                        resp._content = b"2"
                    elif url == "broker/command/3/file/stdin":
                        resp._content = b"3"
                else:
                    resp._content = b""
            return resp

        mock_get.side_effect = on_get

        out: Dict[str, Dict[str, bytes]] = {}

        def on_post(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            resp._content = b"{}"
            if url.endswith("stdout") or url.endswith("stderr"):
                nonlocal out
                command_id = url.split("/")[2]
                file = url.split("/")[4]
                d = out.setdefault(command_id, {})
                data = kwargs["data"]
                resp._content = bytes(f'{{"count":{len(data)}}}', "ascii")
                if file not in d:
                    d[file] = b""
                d[file] = d[file] + data
            return resp

        mock_post.side_effect = on_post

        retriver = host.CommandRetriever("host", "broker")
        try:
            retriver.start()
            time.sleep(5)
        finally:
            retriver.stop()
        self.assertEqual(
            {"1": {"stdout": b"11"}, "2": {"stdout": b"22"}, "3": {"stdout": b"33"}},
            out,
        )
