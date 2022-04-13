from http import HTTPStatus
import sys

import requests
from rich import print as rprint
from rich.panel import Panel


def wrap_sw_error_resp(r :requests.Response, header: str, exit: bool=False, use_raise: bool=False) -> None:
    if r.status_code == HTTPStatus.OK:
        rprint(f" :clap: {header} success")
        return

    rprint(f":fearful: {header} failed")
    msg = f"http status code: {r.status_code} \n"

    try:
        _resp = r.json()
    except Exception:
        msg += f"error message: {r.text} \n"
    else:
        msg += f"starwhale code: {_resp['code']} \n"
        msg += f"error message: {_resp['message']}"
    finally:
        rprint(Panel.fit(msg, title=":space_invader: error details"))
        if exit:
            sys.exit(1)

        if use_raise:
            r.raise_for_status()