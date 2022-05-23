import sys
from http import HTTPStatus

import requests
from rich.table import Table
from rich import box
from rich.panel import Panel

from starwhale.utils.config import SWCliConfigMixed, update_swcli_config
from starwhale.utils import console, fmt_http_server
from starwhale.consts import (
    SW_API_VERSION,
    STANDALONE_INSTANCE,
    UserRoleType,
    DEFAULT_PROJECT,
)
from starwhale.utils.http import wrap_sw_error_resp
from starwhale.base.view import BaseView
from starwhale.cluster.model import ClusterModel


DEFAULT_HTTP_TIMEOUT = 90


class InstanceTermView(SWCliConfigMixed, BaseView):
    def __init__(self) -> None:
        super().__init__()
        self._console = console

    def select(self, instance: str) -> None:
        # TODO: add login check for current instance
        if instance not in self._config["instances"]:
            self._console.print(
                f":person_shrugging: not found {instance} to select, please login first"
            )
            sys.exit(1)

        update_swcli_config(current_instance=instance)
        self._console.print(f":clap: select {self.current_instance} instance")

    def login(self, instance: str, username: str, password: str, alias: str) -> None:
        if instance == STANDALONE_INSTANCE:
            self._console.print(f":pinching_hand: skip {instance} instance login")
            return

        server = fmt_http_server(instance)
        url = f"{server}/api/{SW_API_VERSION}/login"
        r = requests.post(
            url,
            timeout=DEFAULT_HTTP_TIMEOUT,
            data={"userName": username, "userPwd": password},
        )

        if r.status_code == HTTPStatus.OK:
            self._console.print(f":man_cook: login {server} successfully!")
            token = r.headers.get("Authorization")
            if not token:
                self._console.print(
                    ":do_not_litter: cannot get token, please contract starwhale."
                )
                sys.exit(1)

            _d = r.json()["data"]
            _role = _d.get("role", {}).get("roleName") if isinstance(_d, dict) else None
            _project = _d.get("current_project", DEFAULT_PROJECT)

            self.update_instance(
                uri=server,
                user_name=username,
                user_role=_role or UserRoleType.NORMAL,
                sw_token=token,
                alias=alias,
                current_project=_project,
            )
        else:
            wrap_sw_error_resp(r, "login failed!", exit=True)

    def logout(self, instance: str = "") -> None:
        # TODO: do real logout request
        instance = instance or self.current_instance

        if instance == STANDALONE_INSTANCE:
            self._console.print(f":pinching_hand: skip {instance} instance logout")
            return

        self.delete_instance(instance)
        self._console.print(":wink: bye.")

    @BaseView._header  # type: ignore
    def info(self, instance: str = "") -> None:
        instance = instance or self.current_instance

        if instance == STANDALONE_INSTANCE:
            self._console.print(
                f":balloon: standalone instance, root dir @ {self.rootdir}"
            )
        else:
            # TODO: support use uri directly
            # TODO: user async to get
            cm = ClusterModel()
            _baseimages = cm._fetch_baseimage()
            _version = cm._fetch_version()
            _agents = cm._fetch_agents()

            def _agents_table() -> Table:
                table = Table(
                    show_edge=False,
                    show_header=True,
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
                        str(i),
                        _agent["ip"],
                        str(_agent["status"]),
                        _agent["version"],
                        str(_agent["connectedTime"]),
                    )
                return table

            def _details() -> Panel:
                grid = Table.grid(padding=1, pad_edge=True)
                grid.add_column(
                    "Category", no_wrap=True, justify="left", style="bold green"
                )
                grid.add_column("Information")

                grid.add_row(
                    "Version",
                    _version,
                )

                grid.add_row("BaseImage", "\n".join([f"- {i}" for i in _baseimages]))
                grid.add_row(
                    "Agents",
                    _agents_table(),
                )
                return Panel(grid, title_align="left")

            self._console.print(_details())

    def list(self) -> None:
        table = Table(
            title="List Starwhale Instances",
            caption=f"Current Instance: [blink]{self.current_instance}",
            box=box.SIMPLE,
        )
        table.add_column("")
        table.add_column("Name")
        table.add_column("URI")
        table.add_column("UserName")
        table.add_column("UserRole")
        table.add_column("CurrentProject")
        table.add_column("Updated")

        for k, v in self._config["instances"].items():
            _is_current = (
                k == self.current_instance or v["uri"] == self.current_instance
            )
            table.add_row(
                ":backhand_index_pointing_right:" if _is_current else "",
                k,
                v["uri"],
                v["user_name"],
                v.get("user_role", "--"),
                str(v.get("current_project", "--")),
                v.get("updated_at", "--"),
                style="magenta" if _is_current else "",
            )
        self._console.print(table)
