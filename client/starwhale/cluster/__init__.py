from http import HTTPStatus
import typing as t

import requests
from loguru import logger

from starwhale.utils.config import SWCliConfigMixed
from starwhale.consts import SW_API_VERSION, HTTP_METHOD
from starwhale.utils.http import wrap_sw_error_resp, ignore_error

_DEFAULT_TIMEOUT_SECS = 90


class Cluster(SWCliConfigMixed):

    def __init__(self, swcli_config: t.Union[dict, None] = None) -> None:
        super().__init__(swcli_config)

    def request(self, path: str, method: str=HTTP_METHOD.GET, **kw: dict) -> requests.Response:
        _url = f"{self.sw_remote_addr}/api/{SW_API_VERSION}/{path.lstrip('/')}"
        r = requests.request(method, _url,
                             timeout=_DEFAULT_TIMEOUT_SECS,
                             verify=False,
                             headers={"Authorization": self._sw_token},
                             **kw)
        wrap_sw_error_resp(r, path, exit=False, use_raise=False)
        return r

    def info(self):
        #TODO: user async to get
        pass

    @ignore_error([])
    def _fetch_baseimage(self) -> t.List[str]:
        r = self.request("/runtime/baseImage")
        return [i["name"] for i in r.json().get("data", []) if i.get("name")]

    @ignore_error("--")
    def _fetch_version(self) -> str:
        return self.request("/system/version").json()["data"]["version"]

    @ignore_error([])
    def _fetch_agents(self) -> t.List[dict]:
        return self.request("/system/agent").json()["data"]["list"]

    @ignore_error({})
    def _fetch_current_user(self) -> dict:
        r = self.request("/user/current").json()["data"]
        return dict(username=r["name"], role=r["role"]["roleName"])