import os
import unittest
from typing import Any
from unittest.mock import Mock, patch

import requests
from requests.structures import CaseInsensitiveDict

from starwhale.cli.assistance import remote


class TestCommandRunner(unittest.TestCase):
    @patch("starwhale.cli.assistance.remote.requests.post")
    @patch("starwhale.cli.assistance.remote.requests.get")
    @patch("starwhale.cli.assistance.remote.sys")
    def test_normal(self, mock_sys: Mock, mock_get: Mock, mock_post: Mock) -> None:
        stdin_r, stdin_w = os.pipe()
        stdout_r, stdout_w = os.pipe()
        stderr_r, stderr_w = os.pipe()
        mock_sys.stdin = os.fdopen(stdin_r, "rb")
        mock_sys.stdout = os.fdopen(stdout_w, "wb")
        mock_sys.stderr = os.fdopen(stderr_w, "wb")

        input = [None]

        def on_get(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            resp._content = b"{}"
            if url.endswith("stdout") or url.endswith("stderr"):
                if kwargs["params"]["offset"] == 0:
                    nonlocal input
                    if input[0] is None:
                        resp._content = b""
                        resp.headers = CaseInsensitiveDict({"x-wait-for-data": ""})
                    else:
                        resp._content = bytes(url.split("/")[-1], "ascii") + input[0]
                else:
                    resp._content = b""
            return resp

        mock_get.side_effect = on_get

        command_id = [None]

        def on_post(url: str, **kwargs: Any) -> requests.Response:
            print(f"{url} {kwargs}\n", end="")
            resp = requests.Response()
            resp.status_code = 200
            resp._content = b"{}"
            if url.endswith("stdin"):
                data = kwargs["data"]
                resp._content = bytes(f'{{"count":{len(data)}}}', "ascii")
                nonlocal input
                input[0] = data
            elif url.endswith("command"):
                nonlocal command_id
                command_id[0] = kwargs["json"]["command_id"]
            return resp

        mock_post.side_effect = on_post

        runner = remote.CommandRunner("remote", "broker", ["1", "2"])
        try:
            runner.start()
            os.write(stdin_w, b"test")
            os.close(stdin_w)
            out = os.read(stdout_r, 1024)
            err = os.read(stderr_r, 1024)
            runner.join()
        finally:
            runner.stop()
            try:
                os.close(stdin_r)
            except OSError:
                pass
            try:
                os.close(stdin_w)
            except OSError:
                pass
            try:
                os.close(stdout_r)
            except OSError:
                pass
            try:
                os.close(stdout_w)
            except OSError:
                pass
            try:
                os.close(stderr_r)
            except OSError:
                pass
            try:
                os.close(stderr_w)
            except OSError:
                pass

        self.assertEqual(b"stdouttest", out)
        self.assertEqual(b"stderrtest", err)
        mock_get.assert_any_call(
            f"broker/command/{command_id[0]}/file/stdout",
            params={"offset": 0},
            timeout=5,
        )
        mock_get.assert_any_call(
            f"broker/command/{command_id[0]}/file/stdout",
            params={"offset": 10},
            timeout=5,
        )
        mock_get.assert_any_call(
            f"broker/command/{command_id[0]}/file/stderr",
            params={"offset": 0},
            timeout=5,
        )
        mock_get.assert_any_call(
            f"broker/command/{command_id[0]}/file/stderr",
            params={"offset": 10},
            timeout=5,
        )
        mock_post.assert_any_call(
            "broker/command",
            json={"command_id": command_id[0], "args": ["1", "2"]},
            timeout=5,
        )
        mock_post.assert_any_call(
            f"broker/command/{command_id[0]}/file/stdin",
            params={"offset": 0},
            data=b"test",
            timeout=5,
        )
        mock_post.assert_any_call(
            f"broker/command/{command_id[0]}/file/stdin/closeWrite", timeout=5
        )
