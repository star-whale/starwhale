import os
from subprocess import (
    Popen, CalledProcessError, PIPE,
    STDOUT)

from select import select

from loguru import logger


def log_check_call(*args, **kwargs) -> int:
    log = kwargs.pop("log", logger.debug)
    kwargs["stdout"] = PIPE
    kwargs["stderr"] = STDOUT
    env = kwargs.get("env", os.environ.copy())
    kwargs["env"] = env
    kwargs["universal_newlines"] = True
    env["PYTHONUNBUFFERED"] = "1"

    output = []
    p = Popen(*args, **kwargs)
    log(f"cmd: {p.args}")
    while True:
        fds, _, _ = select([p.stdout], [], [], 30)  #timeout 30s
        for fd in fds:
            line = fd.readline()
            log(line.rstrip())
            output.append(line)
        else:
            if p.poll() is not None:
                break

    p.wait()

    if p.returncode != 0:
        cmd = kwargs.get('args') or args[0]
        e = CalledProcessError(p.returncode, cmd)
        e.output = "".join(output)
        raise e

    return 0

check_call = log_check_call