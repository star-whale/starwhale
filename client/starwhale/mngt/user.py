from http import HTTPStatus
import sys

import requests
from loguru import logger
from rich import print as rprint
from rich.pretty import Pretty
from rich.panel import Panel

from starwhale.utils.config import update_swcli_config
from starwhale.utils import fmt_http_server
from starwhale.consts import SW_API_VERSION
from starwhale.utils.http import wrap_sw_error_resp

DEFAULT_HTTP_TIMEOUT = 90


def login(username, password, server):
    server = fmt_http_server(server)
    url = f"{server}/api/{SW_API_VERSION}/login"
    r = requests.post(url, timeout=DEFAULT_HTTP_TIMEOUT,
                      data={"userName": username, "userPwd": password})

    if r.status_code == HTTPStatus.OK:
        rprint(f":man_cook: login {server} successfully!")
        token = r.headers.get("Authorization")
        if not token:
            rprint(f":do_not_litter: cannot get token, please contract starwhale.")
            sys.exit(1)

        _d = r.json()["data"]
        role = _d.get("role") if isinstance(_d, dict) else None

        update_swcli_config(
            controller=dict(
                remote_addr=server, sw_token=token,
                user_name=username,
                user_role=role or "normal",
            )
        )
    else:
        wrap_sw_error_resp(r, "login failed!", exit=True)


def logout():
    update_swcli_config(
            controller=dict(
                remote_addr="", sw_token="", user_name="", user_role="",
            )
        )
    rprint(f":wink: bye.")