import os
import typing as t
from pathlib import Path
from unittest.mock import patch

import pytest
from pyfakefs.fake_filesystem import FakeFilesystem
from pyfakefs.fake_filesystem_unittest import Patcher

from starwhale.utils import load_dotenv, pretty_merge_list, validate_obj_name
from starwhale.consts import ENV_LOG_LEVEL
from starwhale.utils.debug import init_logger


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
