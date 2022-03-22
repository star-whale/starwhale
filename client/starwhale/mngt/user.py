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

DEFAULT_HTTP_TIMEOUT = 5


def login(username, password, server):
    server = fmt_http_server(server)
    url = f"{server}/{SW_API_VERSION}/login"
    r = requests.post(url, timeout=DEFAULT_HTTP_TIMEOUT,
                      data={"username": username, "userpwd": password})

    if r.status_code == HTTPStatus.OK:
        rprint(f":man_cook: login {server} successfully!")
        token = r.headers.get("Authorization")
        if not token:
            rprint(f":do_not_litter: cannot get token, please contract starwhale.")
            sys.exit(1)

        update_swcli_config(
            controller=dict(
                remote_addr=server, sw_token=token
            )
        )
    else:
        rprint(f":fearful: login failed!")
        Panel(Pretty(r.text), title="Error Message")


def logout():
    update_swcli_config(
            controller=dict(
                remote_addr="", sw_token=""
            )
        )
    rprint(f":wink: bye.")