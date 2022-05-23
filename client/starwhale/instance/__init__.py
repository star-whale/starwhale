import sys
from http import HTTPStatus

import requests
from rich.table import Table
from rich import box

from starwhale.utils.config import SWCliConfigMixed, update_swcli_config
from starwhale.utils import console, fmt_http_server
from starwhale.consts import (
    SW_API_VERSION,
    STANDALONE_INSTANCE,
    UserRoleType,
    DEFAULT_PROJECT,
)
from starwhale.utils.http import wrap_sw_error_resp


DEFAULT_HTTP_TIMEOUT = 90


class Instance(SWCliConfigMixed):
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

    def list(self) -> None:
        table = Table(
            title="List Starwhale Instances",
            caption=f"Current Instance: [blink]{self.current_instance}",
            box=box.SIMPLE,
        )
        table.add_column("Name")
        table.add_column("URI")
        table.add_column("UserName")
        table.add_column("UserRole")
        table.add_column("CurrentProject")
        table.add_column("Updated")

        for k, v in self._config["instances"].items():
            table.add_row(
                k,
                v["uri"],
                v["user_name"],
                v.get("user_role", "--"),
                str(v.get("current_project", "--")),
                v.get("updated_at", "--"),
            )
        self._console.print(table)
