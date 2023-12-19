import os
import typing as t
from subprocess import PIPE, Popen, STDOUT, CalledProcessError

from starwhale.utils import console


def log_check_call(*args: t.Any, **kwargs: t.Any) -> int:
    log = kwargs.pop("log", console.debug)
    capture_stdout = kwargs.pop("capture_stdout", True)

    kwargs["bufsize"] = 1
    kwargs["stdout"] = PIPE if capture_stdout else None
    kwargs["stderr"] = STDOUT if capture_stdout else None
    env = os.environ.copy()
    env.update(kwargs.get("env", {}))
    env["PYTHONUNBUFFERED"] = "1"
    kwargs["env"] = env
    kwargs["universal_newlines"] = True

    output = []
    p = Popen(*args, **kwargs)
    console.debug(f"cmd: {p.args!r}")

    if capture_stdout:
        while True:
            line = p.stdout.readline()  # type: ignore
            if line:
                log(line.rstrip())
                output.append(line)

            if p.poll() is not None:
                break

        p.wait()
        for line in p.stdout.readlines():  # type: ignore
            if line:
                log(line.rstrip())
                output.append(line)

        try:
            p.stdout.close()  # type: ignore
        except Exception as ex:
            log(f"failed to close stdout:{ex}")
    else:
        _stdout, _stderr = p.communicate()
        output.append(f"stdout: {_stdout} \nstderr: {_stderr}")

    if p.returncode != 0:
        cmd = kwargs.get("args") or args[0]
        e = CalledProcessError(p.returncode, cmd)
        e.output = "".join(output)
        raise e

    return 0


check_call = log_check_call
