import typing as t
from functools import wraps

from rich import print as rprint
from rich.panel import Panel
from rich.table import Table
from rich.console import RenderableType

from starwhale.consts import UserRoleType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils import console


class BaseView(SWCliConfigMixed):
    def __init__(self, swcli_config: t.Union[t.Dict[str, t.Any], None] = None) -> None:
        super().__init__(swcli_config)
        self._console = console

    @staticmethod
    def _pager(func):
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            def _print(_r: t.Dict[str, t.Any]) -> None:
                p = Panel(
                    (
                        f"Counts: [green] {_r['current']}/{_r['total']} [/] :sheep: ,"
                        f"[red] {_r['remain']} [/] items does not show."
                    ),
                    title="Count Details",
                    title_align="left",
                )
                rprint(p)

            rt = func(*args, **kwargs)  # type: ignore
            if isinstance(rt, tuple) and len(rt) == 2 and "total" in rt[1]:
                _print(rt[1])

        return _wrapper

    @staticmethod
    def _header(func):
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            sw = SWCliConfigMixed()

            def _print() -> None:
                grid = Table.grid(expand=True)
                grid.add_column(justify="center", ratio=1)
                grid.add_column(justify="right")
                grid.add_row(
                    f":star: {sw.current_instance} ({sw._current_instance_obj['uri']}) :whale:",  # type: ignore
                    f":clown_face:{sw._current_instance_obj['user_name']}@{sw._current_instance_obj.get('user_role', UserRoleType.NORMAL)}",  # type: ignore
                )
                p = Panel(grid, title="Starwhale Instance", title_align="left")
                rprint(p)

            _print()
            return func(*args, **kwargs)  # type: ignore

        return _wrapper

    @staticmethod
    def comparsion(r1: RenderableType, r2: RenderableType) -> Table:
        table = Table(show_header=False, pad_edge=False, box=None, expand=True)
        table.add_column("1")
        table.add_column("2")
        table.add_row(r1, r2)
        return table

    @staticmethod
    def pretty_status(status: str) -> t.Tuple[str, str, str]:
        style = "blue"
        icon = ":tractor:"
        if status == "SUCCESS":
            style = "green"
            icon = ":clap:"
        elif status == "FAIL":
            style = "red"
            icon = ":fearful:"
        return status, style, icon
