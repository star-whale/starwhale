import typing as t
from functools import wraps
from datetime import datetime

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
from starwhale.consts import FMT_DATETIME

_DEFAULT_TIMEOUT_SECS = 90
DEFAULT_PAGE_NUM = 1
DEFAULT_PAGE_SIZE = 20

#TODO: use model-view-control mode to refactor Cluster
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

    def _pager(func): #type: ignore
        @wraps(func) #type: ignore
        def _wrapper(*args, **kwargs):
            self: Cluster = args[0]

            def _print():
                pass

            func(*args, **kwargs) #type: ignore
            _print()
        return _wrapper

    def _header(func): #type: ignore
        @wraps(func) #type: ignore
        def _wrapper(*args, **kwargs):
            self: Cluster = args[0]

            def _print():
                grid = Table.grid(expand=True)
                grid.add_column(justify="center", ratio=1)
                grid.add_column(justify="right")
                grid.add_row(
                   f":star: {self.sw_remote_addr} :whale:",
                   f":clown_face:{self.user_name}@{self.user_role}",
                )
                p = Panel(grid, title=f"Starwhale Controller Cluster", title_align="left")
                rprint(p)

            _print()
            func(*args, **kwargs) #type: ignore
        return _wrapper

    @_pager #type: ignore
    @_header #type: ignore
    def list(self, all_users: bool=False, page: int=DEFAULT_PAGE_NUM, size: int=DEFAULT_PAGE_SIZE):
        user_name = "" if all_users else self.user_name
        projects, pager = self._fetch_projects(user_name, page, size)

        if not projects:
            rprint("No projects.")
            return projects, pager

        grid = Table.grid(padding=1, pad_edge=True)
        grid.add_column("Project", no_wrap=True, justify="left", style="bold green", min_width=20)
        grid.add_column("Details")

        for p in projects:
            grid.add_row(
                f"{p['id']} {p['name']}",
                f":arrow_right::arrow_right: default:{p['is_default']} :timer_clock:created:{p['created_at']} :clown_face:owner:{p['owner']} ",
            )

        p = Panel(grid, title=f"Project Details", title_align="left")
        rprint(p)
        return projects, pager

    @_header #type: ignore
    def info(self):
        #TODO: user async to get
        _baseimages = self._fetch_baseimage()
        _version = self._fetch_version()
        _agents = self._fetch_agents()

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
            grid.add_column("Category", no_wrap=True, justify="left", style="bold green")
            grid.add_column("Information")

            grid.add_row(
                "Version",
                _version,
            )

            grid.add_row(
                "BaseImage",
                "\n".join([f"- {i}" for i in _baseimages])
            )
            grid.add_row(
                "Agents",
                _agents_table(),
            )
            return Panel(grid, title_align="left")

        rprint(_details())

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
        return self.request("/system/agent", params={"pageSize": DEFAULT_PAGE_SIZE, "pageNum": DEFAULT_PAGE_NUM}).json()["data"]["list"]

    @ignore_error({})
    def _fetch_current_user(self) -> dict:
        r = self.request("/user/current").json()["data"]
        return dict(name=r["name"], role=r["role"]["roleName"])

    @ignore_error(([], {}))
    def _fetch_projects(self, user_name:str="", page: int=DEFAULT_PAGE_NUM, size: int=DEFAULT_PAGE_SIZE) -> t.Tuple[t.List[dict], dict]:
        #TODO: user params for project api
        r = self.request("/project", params={"pageNum": page, "pageSize": size, "userName": user_name}).json()
        projects = []
        for _p in r["data"]["list"]:
            owner = _p["owner"]["name"]
            if user_name != "" and user_name != owner:
                continue

            projects.append(
                dict(
                    id=_p["id"],
                    name=_p["name"],
                    created_at=datetime.fromtimestamp(float(_p["createTime"]) / 1000.0).strftime(FMT_DATETIME),
                    is_default=_p["isDefault"],
                    owner=owner,
                )
            )
        return projects, self._parse_pager(r)

    def _parse_pager(self, resp: dict) -> dict:
        _d = resp["data"]
        return dict(
            total=_d["total"],
            current=_d["size"],
            remain=_d["total"] - _d["size"],
        )
