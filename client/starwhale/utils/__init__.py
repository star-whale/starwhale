import uuid
import hashlib
import base64
import random
import string
import os
import sys
import platform
import subprocess
import typing as t
from pathlib import Path

from loguru import logger

from starwhale.consts import ENV_CONDA, ENV_CONDA_PREFIX


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


def get_external_python_version():
    return subprocess.check_output(["python3", "-c",
        "import sys; _v = sys.version_info; print(f'{_v,major}.{_v.minor}.{_v.micro}')"
        ], stderr=sys.stderr
    )

def in_dev() -> bool:
    return not in_production()

def in_production() -> bool:
    return os.environ.get("SW_PRODUCTION", "") == "1"

def is_venv() -> bool:
    #TODO: refactor for get external venv attr
    output = subprocess.check_output(["python3", "-c",
         "import sys; print(sys.prefix != (getattr(sys, 'base_prefix', None) or (getattr(sys, 'real_prefix', None) or sys.prefix)))"
        ],stderr=sys.stdout)
    return "True" in str(output)


def is_conda() -> bool:
    return get_conda_env() != "" and get_conda_env_prefix() != ""


def get_python_run_env() -> str:
    if is_conda():
        return "conda"
    elif is_venv():
        return "venv"
    else:
        return "system"


def get_conda_env() -> str:
    return os.environ.get(ENV_CONDA, "")


def get_conda_env_prefix() -> str:
    return os.environ.get(ENV_CONDA_PREFIX, "")


def is_windows() -> bool:
    #TODO: for windows nt?
    return platform.system() == "Windows"


def is_darwin() -> bool:
    #TODO: check m1 chip system
    return platform.system() == "Darwin"


def is_linux() -> bool:
    return platform.system() in ("Linux", "Unix")


def get_python_version():
    #TODO: check user ppl environment or starwhale-cli env? need test
    _v = sys.version_info
    return f"{_v.major}.{_v.minor}.{_v.micro}"


def fmt_http_server(server: str, https: bool=False) -> str:
    server = server.strip().strip("/")
    if not server:
        raise Exception(f"no server addr")

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

    def _c(b: t.Union[int, float], idx:int) -> str:
        if b < 1024 or (idx + 1 == len(_bytes_progress)):
            return f"{b:.2f}{_bytes_progress[idx]}"
        else:
            return _c(b / 1024, idx+1)

    return _c(b, 0)