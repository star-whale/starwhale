from __future__ import annotations

from http import HTTPStatus
from urllib.parse import urlparse

import requests

from starwhale.utils import fmt_http_server
from starwhale.consts import HTTPMethod, UserRoleType, STANDALONE_INSTANCE
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.instance import Instance

DEFAULT_HTTP_TIMEOUT = 90


def login(
    instance: str,
    alias: str = "",
    username: str = "",
    password: str = "",
    token: str = "",
) -> None:
    """Login to a Starwhale Server instance or Cloud instance.

    Provide either username + password or token to login. Login information will be saved in `~/.config/starwhale/config.yaml`.
    login function is equivalent to `swcli instance login` command.

    We cannot login to a Standalone instance.

    Arguments:
        instance: (str, required) Starwhale Server instance or Cloud instance server http url.
        alias: (str, optional) Alias for the instance. If not provided, the alias will be the same as the hostname of the instance url.
          Alias is used to identify the instance in the Instance URI.
        username: (str, optional) Username for login.
        password: (str, optional) Password for login.
        token: (str, optional) Token for login.  Get token from Starwhale Server instance or Cloud instance Web UI.

    Returns:
        None

    Examples:

    ```python
    from starwhale import login

    # login to Starwhale Cloud instance by token
    login(instance="https://cloud.starwhale.cn", alias="cloud-cn", token="xxx")

    # login to Starwhale Server instance by username and password
    login(instance="http://controller.starwhale.svc", alias="dev", username="starwhale", password="abcd1234")
    ```
    """
    if instance == STANDALONE_INSTANCE:
        raise RuntimeError("Cannot login to the Standalone instance")

    if username:
        if not password:
            raise ValueError("Password must be provided when username is provided")
        if token:
            raise ValueError("Cannot provide both username + password and token")
    elif not token:
        raise ValueError("Either username + password or token must be provided")

    server = fmt_http_server(instance)
    alias_name = alias or urlparse(server).hostname
    if not alias_name:
        raise ValueError("Cannot parse alias name from instance url")

    if token:
        r = _login_request_by_token(server, token)
    else:
        r = _login_request_by_username(server, username, password)

    if r.status_code == HTTPStatus.OK:
        token = r.headers.get("Authorization") or token
        if not token:
            raise RuntimeError("Cannot get token, please contract starwhale")

        _d = r.json()["data"]
        _role = _d.get("role", {}).get("roleName") if isinstance(_d, dict) else None

        SWCliConfigMixed().update_instance(
            uri=server,
            user_name=_d.get("name", ""),
            user_role=_role or UserRoleType.NORMAL,
            sw_token=token,
            alias=alias_name,
        )
    else:
        r.raise_for_status()


def _login_request_by_token(server: str, token: str) -> requests.Response:
    return CloudRequestMixed.do_http_request(  # type: ignore[no-any-return]
        path="/user/current",
        instance=Instance(uri=server, token=token),
        method=HTTPMethod.GET,
        timeout=DEFAULT_HTTP_TIMEOUT,
    )


def _login_request_by_username(
    server: str, username: str, password: str
) -> requests.Response:
    return CloudRequestMixed.do_http_request(  # type: ignore[no-any-return]
        path="/login",
        instance=server,
        method=HTTPMethod.POST,
        timeout=DEFAULT_HTTP_TIMEOUT,
        disable_default_content_type=True,
        data={
            "userName": username,
            "userPwd": password,
        },
    )


def logout(instance: str) -> None:
    """Logout from a Starwhale Server instance or Cloud instance.

    When logout from a instance, the corresponding login information will be removed from `~/.config/starwhale/config.yaml`.

    We cannot logout from a Standalone instance.

    Arguments:
        instance: (str, required) Instance URI

    Returns:
        None

    Examples:

    ```python
    from starwhale import login, logout

    login(instance="https://cloud.starwhale.cn", alias="cloud-cn", token="xxx")
    # logout by the alias
    logout("cloud-cn")

    login(instance="http://controller.starwhale.svc", alias="dev", username="starwhale", password="abcd1234")
    # logout by the instance http url
    logout("http://controller.starwhale.svc")
    ```
    """
    if instance == STANDALONE_INSTANCE:
        raise RuntimeError("Cannot logout from the Standalone instance")

    SWCliConfigMixed().delete_instance(instance)
