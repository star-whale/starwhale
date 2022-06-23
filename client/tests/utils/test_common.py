import os

from starwhale.utils import validate_obj_name
from starwhale.consts import ENV_LOG_LEVEL
from starwhale.utils.debug import init_logger


def test_valid_object_name() -> None:
    assert validate_obj_name("a")[0]
    assert validate_obj_name("1")[0]
    assert validate_obj_name("abc")[0]
    assert not validate_obj_name("a" * 81)[0]
    assert validate_obj_name("_adtest")[0]
    assert not validate_obj_name("_.adtest")[0]


def test_logger() -> None:
    init_logger(0)
    assert os.environ[ENV_LOG_LEVEL] == "WARNING"

    init_logger(3)
    assert os.environ[ENV_LOG_LEVEL] == "DEBUG"
