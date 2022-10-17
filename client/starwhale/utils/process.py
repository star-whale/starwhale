import os
import typing as t
from subprocess import PIPE, Popen, STDOUT, CalledProcessError

from loguru import logger


def log_check_call(*args: t.Any, **kwargs: t.Any) -> int:
    log = kwargs.pop("log", logger.debug)
    kwargs["bufsize"] = 1
    kwargs["stdout"] = PIPE
    kwargs["stderr"] = STDOUT
    env = os.environ.copy()
    env.update(kwargs.get("env", {}))
    env["PYTHONUNBUFFERED"] = "1"
    kwargs["env"] = env
    kwargs["universal_newlines"] = True

    output = []
    p = Popen(*args, **kwargs)
    logger.debug(f"cmd: {p.args!r}")

    def _print_log() -> None:
        line = p.stdout.readline()  # type: ignore
        if line:
            log(line.rstrip())
            output.append(line)

    while True:
        _print_log()
        if p.poll() is not None:
            break

    p.wait()
    _print_log()

    try:
        p.stdout.close()  # type: ignore
    except Exception as ex:
        log(f"failed to close stdout:{ex}")

    if p.returncode != 0:
        cmd = kwargs.get("args") or args[0]
        e = CalledProcessError(p.returncode, cmd)
        e.output = "".join(output)
        raise e

    return 0


check_call = log_check_call
