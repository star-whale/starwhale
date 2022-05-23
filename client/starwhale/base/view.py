import typing as t
from functools import wraps

from rich import print as rprint
from rich.panel import Panel
from rich.table import Table

from rich.console import RenderableType
from starwhale.consts import UserRoleType


class BaseView(object):
    def _pager(func):  # type: ignore
        @wraps(func)  # type: ignore
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
                rprint("\n", p)

            rt = func(*args, **kwargs)  # type: ignore
            if isinstance(rt, tuple) and len(rt) == 2 and "total" in rt[1]:
                _print(rt[1])

        return _wrapper

    def _header(func):  # type: ignore
        @wraps(func)  # type: ignore
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            self: BaseView = args[0]

            def _print() -> None:
                grid = Table.grid(expand=True)
                grid.add_column(justify="center", ratio=1)
                grid.add_column(justify="right")
                # TODO: tune self for current_instance
                grid.add_row(
                    f":star: {self.current_instance } ({self._current_instance_obj['uri']}) :whale:",  # type: ignore
                    f":clown_face:{self._current_instance_obj['user_name']}@{self._current_instance_obj.get('user_role', UserRoleType.NORMAL)}",  # type: ignore
                )
                p = Panel(grid, title="Starwhale Instance", title_align="left")
                rprint(p)

            _print()
            return func(*args, **kwargs)  # type: ignore

        return _wrapper

    def comparsion(self, r1: RenderableType, r2: RenderableType) -> Table:
        table = Table(show_header=False, pad_edge=False, box=None, expand=True)
        table.add_column("1", ratio=1)
        table.add_column("2", ratio=1)
        table.add_row(r1, r2)
        return table
