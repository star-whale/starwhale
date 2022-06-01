import os
import re
import sys
import time
import uuid
import base64
import random
import string
import typing as t
import hashlib
import platform
from datetime import datetime

from rich.console import Console

from starwhale.consts import FMT_DATETIME

console = Console(soft_wrap=True)
now_str = lambda: datetime.now().astimezone().strftime(FMT_DATETIME)


def timestamp_to_datatimestr(timestamp: float) -> str:
    ts = time.localtime(timestamp)
    return time.strftime(FMT_DATETIME, ts)


def gen_uniq_version(feature: str = "") -> str:
    # version = ${timestamp:8} + ${feature:8} + ${randstr:4}
    timestamp = "".join(str(uuid.uuid1()).split("-")[0])
    feature = hashlib.sha256((feature or random_str()).encode()).hexdigest()[:7]
    randstr = random_str(cnt=4)
    bstr = base64.b32encode(f"{timestamp}{feature}{randstr}".encode()).decode("ascii")
    # TODO: add test for uniq and number
    return bstr.lower().strip("=")


def random_str(cnt: int = 8) -> str:
    return "".join(random.sample(string.ascii_lowercase + string.digits, cnt))


def in_dev() -> bool:
    return not in_production()


def in_production() -> bool:
    return os.environ.get("SW_PRODUCTION", "") == "1"


def is_windows() -> bool:
    # TODO: for windows nt?
    return platform.system() == "Windows"


def is_darwin() -> bool:
    # TODO: check m1 chip system
    return platform.system() == "Darwin"


def is_linux() -> bool:
    return platform.system() in ("Linux", "Unix")


def get_python_version() -> str:
    # TODO: check user ppl environment or starwhale-cli env? need test
    _v = sys.version_info
    return f"{_v.major}.{_v.minor}.{_v.micro}"


def fmt_http_server(server: str, https: bool = False) -> str:
    server = server.strip().strip("/")
    if not server:
        raise Exception("no server addr")

    if server.startswith(("http://", "https://")):
        return server
    else:
        prefix = "https" if https else "http"
        return f"{prefix}://{server}"


_bytes_map = {
    "k": 1024,
    "kb": 1024,
    "m": 1024 * 1024,
    "mb": 1024 * 1024,
    "g": 1024 * 1024 * 1024,
    "gb": 1024 * 1024 * 1024,
}


def convert_to_bytes(s: t.Union[str, int]) -> int:
    if isinstance(s, int):
        return s

    s = s.strip().lower()
    for f in ("k", "m", "g"):
        if s.endswith((f, f"{f}b")):
            return _bytes_map[f] * int(s.split(f)[0])
    else:
        return int(s)


_bytes_progress = ("B", "KB", "MB", "GB", "TB", "PB")


def pretty_bytes(b: t.Union[int, float]) -> str:
    def _c(b: t.Union[int, float], idx: int) -> str:
        if b < 1024 or (idx + 1 == len(_bytes_progress)):
            return f"{b:.2f}{_bytes_progress[idx]}"
        else:
            return _c(b / 1024, idx + 1)

    return _c(b, 0)


_valid_name_re = re.compile("^([a-zA-Z0-9_-])+$")


def validate_obj_name(name: str) -> t.Tuple[bool, str]:
    if len(name) < 1 or len(name) > 80:
        return (
            False,
            f"length should be between 1 and 80, but {name} has {len(name)} characters",
        )

    if not (name[0] == "_" or name[0].isalnum()):
        return (
            False,
            f"A name should always start with a letter or the _ character, current name:{name}",
        )

    if not _valid_name_re.match(name):
        return (
            False,
            f"A name MUST only consist of letters A-Z a-z, digits 0-9, the hyphen character -, and the underscore character _, current name:{name}",
        )

    return True, ""
