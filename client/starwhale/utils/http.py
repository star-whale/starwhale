import sys

import requests
from rich import print as rprint
from rich.panel import Panel


def wrap_sw_error_resp(r :requests.Response, header: str, exit: bool=False) -> None:
    rprint(f":fearful: {header}")

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