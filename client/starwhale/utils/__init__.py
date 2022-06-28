import os
import re
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

from starwhale import __version__
from starwhale.consts import FMT_DATETIME, SW_DEV_DUMMY_VERSION

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


_valid_name_re = re.compile("^[a-zA-Z_][a-zA-Z0-9_.-]*$")


def validate_obj_name(name: str) -> t.Tuple[bool, str]:
    if len(name) < 1 or len(name) > 80:
        return (
            False,
            f"length should be between 1 and 80, but {name} has {len(name)} characters",
        )

    if not _valid_name_re.match(name):
        return (
            False,
            f"A name MUST only consist of letters A-Z a-z, digits 0-9, the hyphen character -, the underscore character _ and the dot character ., current name:{name}",
        )

    return True, ""


def get_downloadable_sw_version() -> str:
    _v = __version__
    return "" if _v == SW_DEV_DUMMY_VERSION else _v


def snake_to_camel(snake: str) -> str:
    parts = snake.split("_")
    return "".join(i.title() for i in parts)
