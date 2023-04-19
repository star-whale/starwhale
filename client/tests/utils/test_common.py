import os
import typing as t
import urllib.error
from pathlib import Path
from unittest import TestCase
from unittest.mock import patch

import pytest
import requests
import tenacity
from requests_mock import Mocker
from pyfakefs.fake_filesystem import FakeFilesystem
from pyfakefs.fake_filesystem_unittest import Patcher

from starwhale import version
from starwhale.utils import (
    load_dotenv,
    gen_uniq_version,
    pretty_merge_list,
    validate_obj_name,
)
from starwhale.consts import HTTPMethod, ENV_LOG_LEVEL
from starwhale.utils.debug import init_logger
from starwhale.utils.retry import http_retry


def test_valid_object_name() -> None:
    assert validate_obj_name("a")[0]
    assert validate_obj_name("_")[0]
    assert not validate_obj_name("1")[0]
    assert validate_obj_name("abc")[0]
    assert not validate_obj_name("a" * 81)[0]
    assert validate_obj_name("_adtest")[0]
    assert validate_obj_name("_.adtest")[0]
    assert not validate_obj_name(".adtest")[0]
    assert validate_obj_name("v1.0")[0]
    assert validate_obj_name("v1-alpha1")[0]


def test_logger() -> None:
    init_logger(0)
    assert os.environ[ENV_LOG_LEVEL] == "WARNING"

    init_logger(3)
    assert os.environ[ENV_LOG_LEVEL] == "DEBUG"


@pytest.fixture
def fake_fs() -> t.Generator[t.Optional[FakeFilesystem], None, None]:
    with Patcher() as patcher:
        yield patcher.fs


@patch.dict(os.environ, {"TEST_ENV": "1"}, clear=True)
def test_load_dotenv(fake_fs: FakeFilesystem) -> None:
    content = """
    # this is a comment line
    A=1
    B = 2
    c =
    ddd
    """
    fpath = "/home/starwhale/test/.auth_env"
    fake_fs.create_file(fpath, contents=content)
    assert os.environ["TEST_ENV"] == "1"
    load_dotenv(Path(fpath))
    assert os.environ["A"] == "1"
    assert os.environ["B"] == "2"
    assert not os.environ["c"]
    assert "ddd" not in os.environ
    assert len(os.environ) == 4


def test_pretty_merge_list() -> None:
    _cases = (
        ([1, 2, 3, 4, 5, 7, 8, 9, 10, 100, 101, 102], "1-5,7-10,100-102"),
        ([1, 3, 5, 7], "1,3,5,7"),
        ([1, 3, 4, 5, 7], "1,3-5,7"),
        ([], ""),
        ([1, 3, 3, 4, 4, 5, 7], "1,3-5,7"),
    )

    for in_lst, expected_str in _cases:
        assert pretty_merge_list(in_lst) == expected_str


def test_gen_uniq_version() -> None:
    cnt = 10000
    versions = [gen_uniq_version() for i in range(cnt)]
    assert len(versions[0]) == 40
    assert len(set(versions)) == cnt
    len_versions = [len(v) for v in versions]
    assert len(set(len_versions)) == 1
    short_versions = [v[:5] for v in versions]
    assert len(set(short_versions)) >= cnt * 0.99


def test_version() -> None:
    v = version.STARWHALE_VERSION
    assert v != ""
    assert os.environ["SW_VERSION"] == v


class TestRetry(TestCase):
    @http_retry
    def _do_request(self, url: str) -> None:
        _r = requests.get(url, timeout=1)
        _r.raise_for_status()
        raise Exception("dummy")

    @http_retry(attempts=6)
    def _do_urllib_raise(self):
        raise urllib.error.HTTPError("http://1.1.1.1", 500, "dummy", None, None)  # type: ignore

    @http_retry(attempts=2, retry=tenacity.retry_always, wait=tenacity.wait_fixed(1))
    def _do_raise(self):
        raise Exception("dummy")

    @Mocker()
    def test_http_retry(self, request_mock: Mocker) -> None:
        _cases = [
            (200, 1, Exception),
            (400, 1, requests.exceptions.HTTPError),
            (500, 3, requests.exceptions.HTTPError),
            (503, 3, requests.exceptions.HTTPError),
        ]

        for status_code, expected_attempts, exception in _cases:
            url = f"http://1.1.1.1/{status_code}"
            request_mock.request(HTTPMethod.GET, url, status_code=status_code)

            with self.assertRaises(exception):
                self._do_request(url)

            assert (
                self._do_request.retry.statistics["attempt_number"] == expected_attempts
            ), url

        with self.assertRaises(urllib.error.HTTPError):
            self._do_urllib_raise()

        assert self._do_urllib_raise.retry.statistics["attempt_number"] == 6

        with self.assertRaises(Exception):
            self._do_raise()
        assert self._do_raise.retry.statistics["attempt_number"] == 2
        assert self._do_raise.retry.statistics["idle_for"] == 1.0
