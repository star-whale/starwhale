import os
import subprocess
from typing import Any, Dict, List, Tuple, Optional

from starwhale.utils import console


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
        console.warning(f"args:{args}, error is:{_err}")
    return p.returncode, _stdout


def check_invoke(*args: Any, **kwargs: Any) -> None:
    code = invoke_ret_code(*args, **kwargs)
    assert code == 0, f"invoke failed, code:{code}, args: {args}, kwargs: {kwargs}"


def invoke_ret_code(*args: Any, **kwargs: Any) -> int:
    kwargs.update(record_output=False)
    code, _ = _invoke(*args, **kwargs)
    return code


def invoke_output(*args: Any, **kwargs: Any) -> Tuple[int, str]:
    kwargs.update(record_output=True)
    return _invoke(*args, **kwargs)


def _invoke(
    args: List[str],
    raise_err: bool = False,
    log: bool = False,
    external_env: Optional[Dict[str, str]] = None,
    record_output: bool = False,
) -> Tuple[int, str]:
    """
    record_output: record output into a list, this may cause memory leak
    """
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

    console.debug(f"cmd: {p.args!r}, env: {external_env}")

    output = []
    while True:
        line = p.stdout.readline()  # type: ignore
        if line:
            if log:
                # remove \n at the end to avoid double \n
                console.debug(line[:-1])

            if record_output:
                output.append(line)

        if p.poll() is not None:
            break

    p.wait()
    for line in p.stdout.readlines():  # type: ignore
        if line:
            if log:
                console.debug(line)
            if record_output:
                output.append(line)

    try:
        p.stdout.close()  # type: ignore
    except Exception as ex:
        console.error(f"failed to close stdout:{ex}")

    if raise_err and p.returncode != 0:
        cmd = args[0]
        e = subprocess.CalledProcessError(p.returncode, cmd)
        e.output = "".join(output)
        raise e
    return p.returncode, "".join(output)
