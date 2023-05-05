import os
import re
import time
import uuid
import base64
import random
import string
import typing as t
import platform
from pathlib import Path
from datetime import datetime
from functools import partial, cmp_to_key
from contextlib import contextmanager

import yaml

from starwhale.consts import (
    FMT_DATETIME,
    MINI_FMT_DATETIME,
    SW_DEV_DUMMY_VERSION,
    ENV_DISABLE_PROGRESS_BAR,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.utils.error import NoSupportError

now: t.Callable[[str], str] = lambda x: datetime.now().astimezone().strftime(x)
now_str: t.Callable[[], str] = partial(now, FMT_DATETIME)
now_mini_str: t.Callable[[], str] = partial(now, MINI_FMT_DATETIME)


def timestamp_to_datatimestr(timestamp: float) -> str:
    ts = time.localtime(timestamp)
    return time.strftime(FMT_DATETIME, ts)


def gen_uniq_version() -> str:
    uuid_bytes = bytearray(uuid.uuid1().bytes)
    rand_bytes = os.urandom(17)
    # ref: https://docs.python.org/3.9/library/uuid.html
    # uuid1: 32-bit time_low, 16-bit time_mid, 16-bit time_hi_version, 8-bit clock_seq_hi_variant, 8-bit clock_seq_low, 48-bit node
    # uuid only keeps time_low, time_mid, clock_seq_hi_variant and clock_seq_low
    ver_bytes = uuid_bytes[:6] + uuid_bytes[8:10] + rand_bytes
    random.shuffle(ver_bytes)
    return base64.b32encode(ver_bytes).decode("ascii").lower().strip("=")


def random_str(cnt: int = 8) -> str:
    return "".join(random.sample(string.ascii_lowercase + string.digits, cnt))


def in_dev() -> bool:
    return not in_production()


def in_production() -> bool:
    return os.environ.get("SW_PRODUCTION", "") == "1"


def in_container() -> bool:
    return os.environ.get("SW_CONTAINER", "") == "1"


def is_windows() -> bool:
    # TODO: for windows nt?
    return platform.system() == "Windows"


def is_darwin(arm: bool = False) -> bool:
    if platform.system() != "Darwin":
        return False
    return not arm or platform.processor() == "arm"


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
    _v = STARWHALE_VERSION
    return "" if _v == SW_DEV_DUMMY_VERSION else _v


def get_field(item: t.Dict, field: str):  # type:ignore
    for k in field.split("."):
        item = item.get(k)  # type:ignore
    return item


def snake_to_camel(snake: str) -> str:
    parts = snake.split("_")
    return "".join(i.title() for i in parts)


class Order:
    def __init__(self, field: str, reverse: bool = False):
        self.field = field
        self.reverse = reverse


def sort_obj_list(
    data: t.Sequence, orders: t.List[Order]
) -> t.List[t.Dict[str, t.Any]]:
    def cmp(a: t.Any, b: t.Any) -> t.Any:
        return (a > b) - (a < b)

    def compare(lhs: t.Dict, rhs: t.Dict) -> int:
        m = 0
        for o in orders:
            m = cmp(get_field(lhs, o.field), get_field(rhs, o.field))
            m = o.reverse and -m or m
            if m != 0:
                return m
        return m

    return sorted(data, key=cmp_to_key(compare))


def load_yaml(path: t.Union[str, Path]) -> t.Any:
    """load_yaml loads yaml from path, it may raise exception such as yaml.YAMLError"""
    with open(path) as f:
        return yaml.safe_load(f)


def get_current_shell() -> str:
    import shellingham

    return str(shellingham.detect_shell()[0])


@contextmanager
def disable_progress_bar() -> t.Generator[None, None, None]:
    old_flag = os.environ.get(ENV_DISABLE_PROGRESS_BAR, "")

    os.environ[ENV_DISABLE_PROGRESS_BAR] = "1"
    try:
        yield
    finally:
        os.environ[ENV_DISABLE_PROGRESS_BAR] = old_flag


def make_dir_gitignore(d: Path) -> None:
    from starwhale.utils.fs import ensure_dir, ensure_file

    if d.exists() and not d.is_dir():
        raise NoSupportError(f"{d} is not dir")

    ensure_dir(d)
    ensure_file(d / ".gitignore", "*")


def load_dotenv(fpath: Path) -> None:
    if not fpath.exists():
        return

    with fpath.open("r") as f:
        for line in f.readlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue

            k, v = line.split("=", 1)
            os.environ[k.strip()] = v.strip()


def pretty_merge_list(lst: t.List[int]) -> str:
    _r = []
    lst = sorted(lst)
    if not lst:
        return ""

    _start, _end = lst[0], lst[0]
    for _cur in lst[1:]:
        if _end + 1 == _cur or _end == _cur:
            _end = _cur
        else:
            _r.append(str(_start) if _start == _end else f"{_start}-{_end}")
            _start = _end = _cur

    _r.append(str(_start) if _start == _end else f"{_start}-{_end}")
    return ",".join(_r)
