from http import HTTPStatus
import sys
import typing as t
from functools import wraps

from loguru import logger
import requests

from rich import print as rprint
from rich.panel import Panel


def wrap_sw_error_resp(
    r: requests.Response,
    header: str,
    exit: bool = False,
    use_raise: bool = False,
    silent: bool = False,
    ignore_status_codes: t.List[int] = [],
) -> None:

    if silent:
        _rprint = lambda x: x
    else:
        _rprint = rprint

    if r.status_code == HTTPStatus.OK:
        _rprint(f":clap: {header} success")  # type: ignore
        return

    msg = f":disappointed_face: url:{r.url}\n:dog: http status code: {r.status_code} \n"

    try:
        _resp = r.json()
    except Exception:
        msg += f":dragon:error message: {r.text} \n"
    else:
        msg += f":falafel: starwhale code: {_resp['code']} \n"
        msg += f":dragon: error message: {_resp['message']}"
    finally:
        if r.status_code in ignore_status_codes:
            return

        rprint(Panel.fit(msg, title=":space_invader: error details"))  # type: ignore
        if exit:
            sys.exit(1)

        if use_raise:
            r.raise_for_status()


def ignore_error(default_ret: t.Any = ""):
    def _decorator(func):
        @wraps(func)
        def _wrapper(*args, **kwargs):
            try:
                return func(*args, **kwargs)
            except Exception as e:
                logger.warning(f"{func} error: {e}")
                return default_ret

        return _wrapper

    return _decorator
