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


def pip_freeze(path: t.Union[str, Path]) -> bytes:
    #TODO: add cmd timeout and error log
    return subprocess.check_output(f"pip freeze > {path}", shell=True, stderr=subprocess.STDOUT)


def conda_export(path: t.Union[str, Path], env:str="") -> bytes:
    #TODO: add cmd timeout
    cmd = "conda env export"
    env = f"-n {env}" if env else ""
    return subprocess.check_output(f"{cmd} {env} > {path}", shell=True, stderr=subprocess.STDOUT)

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