import typing as t

import requests
from loguru import logger
from rich.layout import Layout
from rich.console import Console
from rich import print as rprint
from rich.panel import Panel
from rich.table import Table
from rich import box

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
        wrap_sw_error_resp(r, path, exit=False, use_raise=False, slient=True)
        return r

    def info(self):
        #TODO: user async to get
        _baseimages = self._fetch_baseimage()
        _version = self._fetch_version()
        _agents = self._fetch_agents()
        _user = self._fetch_current_user()

        def _summary() -> Panel:
            grid = Table.grid(expand=True)
            grid.add_column(justify="center", ratio=1)
            grid.add_column(justify="right")
            grid.add_row(
               f":star: {self.sw_remote_addr} :whale:",
               f":clown_face:{_user['name']}@{_user['role']}",
            )
            return Panel(grid, title=f"Starwhale Controller Cluster({_version})", title_align="left")

        def _agents_table():
            table = Table(
                show_edge=False,
                show_header=True,
                expand=True,
                row_styles=["none", "dim"],
                box=box.SIMPLE,
            )
            table.add_column("id")
            table.add_column("ip", style="green")
            table.add_column("status", style="blue")
            table.add_column("version")
            table.add_column("connected time")

            for i, _agent in enumerate(_agents):
                table.add_row(
                    str(i), _agent["ip"], str(_agent["status"]), _agent["version"], str(_agent["connectedTime"])
                )
            return table

        def _details() -> Panel:
            grid = Table.grid(padding=1, pad_edge=True)
            grid.add_column("Category", no_wrap=True, justify="left", style="bold green", min_width=10)
            grid.add_column("Information")

            grid.add_row(
                "BaseImage",
                "\n".join([f"- {i}" for i in _baseimages])
            )
            grid.add_row(
                "Agents",
                _agents_table(),
            )
            return Panel(grid, title_align="left")

        rprint(_summary(), _details())

    @ignore_error([])
    def _fetch_baseimage(self) -> t.List[str]:
        r = self.request("/runtime/baseImage")
        return [i["name"] for i in r.json().get("data", []) if i.get("name")]

    @ignore_error("--")
    def _fetch_version(self) -> str:
        return self.request("/system/version").json()["data"]["version"]

    @ignore_error([])
    def _fetch_agents(self) -> t.List[dict]:
        #TODO: add pageSize to args
        return self.request("/system/agent", params={"pageSize": 100}).json()["data"]["list"]

    @ignore_error({})
    def _fetch_current_user(self) -> dict:
        r = self.request("/user/current").json()["data"]
        return dict(name=r["name"], role=r["role"]["roleName"])