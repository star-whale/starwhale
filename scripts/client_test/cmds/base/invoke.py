import os
import subprocess
from typing import Dict, List, Tuple, Optional

from loguru import logger


def invoke_with_react(args: List[str], input_content: str = "yes") -> Tuple[int, str]:
    p = subprocess.Popen(
        args,
        stdout=subprocess.PIPE,
        stdin=subprocess.PIPE,
        stderr=subprocess.PIPE,
        universal_newlines=True,
    )
    _stdout, _err = p.communicate(input=input_content)
    if _err:
        logger.warning(f"args:{args}, error is:{_err}")
    return p.returncode, _stdout


def invoke(
    args: List[str],
    raise_err: bool = False,
    log: bool = False,
    external_env: Optional[Dict[str, str]] = None,
) -> Tuple[int, str]:
    env = os.environ.copy()
    env["PYTHONUNBUFFERED"] = "1"
    env.update(external_env or {})
    p = subprocess.Popen(
        args,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        env=env,
        universal_newlines=True,
    )

    logger.debug(f"cmd: {p.args!r}, env: {external_env}")

    output = []
    while True:
        line = p.stdout.readline()  # type: ignore
        if line:
            if log:
                logger.debug(line)
            output.append(line)

        if p.poll() is not None:
            break

    p.wait()
    for line in p.stdout.readlines():  # type: ignore
        if line:
            if log:
                logger.debug(line)
            output.append(line)

    try:
        p.stdout.close()  # type: ignore
    except Exception as ex:
        logger.error(f"failed to close stdout:{ex}")

    if raise_err and p.returncode != 0:
        cmd = args[0]
        e = subprocess.CalledProcessError(p.returncode, cmd)
        e.output = "".join(output)
        raise e
    return p.returncode, "".join(output)
