import sys
import typing as t

from rich import box
from rich.panel import Panel
from rich.table import Table

from starwhale.utils import console
from starwhale.consts import STANDALONE_INSTANCE
from starwhale.base.view import BaseTermView
from starwhale.base.cloud import CloudRequestMixed
from starwhale.base.uri.instance import Instance
from starwhale.api._impl.instance import login, logout

DEFAULT_HTTP_TIMEOUT = 90


class InstanceTermView(BaseTermView, CloudRequestMixed):
    def __init__(self) -> None:
        super().__init__()

    def select(self, instance: str) -> None:
        try:
            self.select_current_default(instance=instance)
        except Exception as e:
            console.print(
                f":person_shrugging: failed to select {instance}, reason: {e}"
            )
            sys.exit(1)
        else:
            console.print(f":clap: select {self.current_instance} instance")

    def login(self, instance: str, alias: str, **kw: str) -> None:
        if instance == STANDALONE_INSTANCE:
            console.print(f":pinching_hand: skip {instance} instance login")
            return

        try:
            login(instance, alias, **kw)
            console.print(f":man_cook: login {instance} successfully!")
        except Exception as e:
            console.exception(
                f":person_shrugging: failed to login {instance}, reason: {e}"
            )
            sys.exit(1)

    def logout(self, instance: str) -> None:
        if instance == STANDALONE_INSTANCE:
            console.print(f":pinching_hand: skip {instance} instance logout")
            return

        try:
            logout(instance)
            console.print(":wink: bye.")
        except Exception as e:
            console.exception(
                f":person_shrugging: failed to logout {instance}, reason: {e}"
            )
            sys.exit(1)

    def info(self, uri: str = "") -> t.Dict:
        instance = Instance(uri or self.current_instance)

        if instance.is_local:
            return {
                "instance": instance.alias,
                "root_dir": str(self.rootdir),
            }
        else:
            return {
                "instance": instance.alias,
                "uri": instance.url,
            }

    def list(self) -> t.List[t.Dict[str, t.Any]]:
        result = list()

        for k, v in self._config["instances"].items():
            _is_current = (
                k == self.current_instance or v["uri"] == self.current_instance
            )
            result.append(
                {
                    "in_use": _is_current,
                    "name": k,
                    "uri": v.get("uri", ""),
                    "user_name": v.get("user_name", ""),
                    "user_role": v.get("user_role", ""),
                    "current_project": str(v.get("current_project", "")),
                    "updated_at": v.get("updated_at", ""),
                }
            )
        return result


class InstanceTermViewRich(InstanceTermView):
    def list(self) -> None:  # type: ignore
        title = "List Starwhale Instances"
        custom_table = {"caption": f"Current Instance: [blink]{self.current_instance}"}
        custom_column: t.Dict[str, t.Callable[[t.Any], str]] = {
            "in_use": lambda x: ":backhand_index_pointing_right:" if x else "",
            "user_role": self.place_holder_for_empty(),
            "current_project": self.place_holder_for_empty(),
            "updated_at": self.place_holder_for_empty(),
        }
        custom_row = lambda row: {"style": "magenta"} if row["in_use"] else None
        data = super().list()
        self.print_table(
            title,
            data,
            custom_table=custom_table,
            custom_column=custom_column,
            custom_row=custom_row,
        )

    @BaseTermView._header  # type: ignore
    def info(self, instance: str = "") -> None:
        _info = super().info(instance)
        instance = _info["instance"]

        if instance == STANDALONE_INSTANCE:
            console.print(f":balloon: standalone instance, root dir @ {self.rootdir}")
        else:

            def _agents_table() -> Table:
                table = Table(
                    show_edge=False,
                    show_header=True,
                    box=box.SIMPLE,
                )
                table.add_column("id")
                table.add_column("ip", style="green")
                table.add_column("status", style="blue")
                table.add_column("version")

                for i, _agent in enumerate(_info["agents"]):
                    table.add_row(
                        str(i),
                        _agent["ip"],
                        _agent["status"],
                        _agent["version"],
                    )
                return table

            def _details() -> Panel:
                grid = Table.grid(padding=1, pad_edge=True)
                grid.add_column(
                    "Category", no_wrap=True, justify="left", style="bold green"
                )
                grid.add_column("Information")
                grid.add_row("Version", _info["version"])
                grid.add_row(
                    "Agents",
                    _agents_table(),
                )

                return Panel(grid, title_align="left")

            console.print(_details())


class InstanceTermViewJson(InstanceTermView):
    def list(self) -> None:  # type: ignore
        self.pretty_json(super().list())

    def info(self, instance: str = "") -> None:  # type: ignore
        self.pretty_json(super().info(instance))


def get_term_view(ctx_obj: t.Dict) -> t.Type[InstanceTermView]:
    return (
        InstanceTermViewJson
        if ctx_obj.get("output") == "json"
        else InstanceTermViewRich
    )
