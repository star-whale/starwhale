from __future__ import annotations

import typing as t
from abc import abstractmethod
from dataclasses import dataclass

from rich import console
from rich.text import Text
from rich.style import Style
from rich.table import box, Table
from textual.app import Reactive
from textual.widget import Widget

from starwhale.utils import (
    Order,
    get_field,
    pretty_bytes,
    sort_obj_list,
    snake_to_camel,
)
from starwhale.consts import CREATED_AT_KEY, DEFAULT_PROJECT, STANDALONE_INSTANCE
from starwhale.core.job.view import JobTermView
from starwhale.core.model.view import ModelTermView
from starwhale.core.dataset.view import DatasetTermView
from starwhale.core.runtime.view import RuntimeTermView

if t.TYPE_CHECKING:
    from textual import events
    from textual.widget import RenderableType


default_project = f"{STANDALONE_INSTANCE}/{DEFAULT_PROJECT}"


@dataclass
class Column:
    key: str
    name: t.Optional[str] = ""
    render: t.Optional[t.Callable[[int, t.Any], t.Any]] = None


class OrderBy:
    def __init__(self) -> None:
        self.orderby_keys: t.Dict[str, str] = {
            "C": CREATED_AT_KEY,
            "N": "name",
            "S": "size",
        }
        self.current_order = Order("")

    def record_key(self, key: str) -> bool:
        if key not in self.orderby_keys:
            return False

        field = self.orderby_keys[key]
        if self.current_order.field == field:
            self.current_order.reverse = not self.current_order.reverse
        else:
            self.current_order.field = field
            self.current_order.reverse = False
        return True

    def sort(self, data: t.Sequence) -> t.Sequence:
        if not self.current_order.field:
            return data
        return sort_obj_list(data, [self.current_order])

    def get_order_icon(self) -> t.Tuple[str, RenderableType]:
        if not self.current_order.field:
            return "", ""
        field = self.current_order.field
        if self.current_order.reverse:
            return field, Text(" ↓", style="green")
        else:
            return field, Text(" ↑", style="red")


class TableWidget(Widget):
    """TableWidget makes an interactive rich.Table"""

    def __init__(self, **kwargs: t.Any) -> None:
        super().__init__(**kwargs)
        self.table = Table(expand=True, box=box.SIMPLE)
        self.data: t.Sequence = []
        self.render_fn: t.List[Column] = []
        self._info: t.Any = None
        self._orderby = OrderBy()

    show_info: Reactive[bool] = Reactive(False)
    cursor_line: Reactive[int] = Reactive(0, repaint=False)

    def watch_show_info(self, show: bool) -> None:
        self._info = show and self.info(self.cursor_line) or None
        self.refresh(layout=True)

    def watch_cursor_line(self, value: int) -> None:
        self.highlight_row(value)
        self.refresh()

    @abstractmethod
    def reloadImpl(self) -> None:
        raise NotImplementedError

    def info(self, idx: int) -> RenderableType:
        return console.Pretty(self.data[idx], indent_guides=True)

    def watch_data(self) -> None:
        pass

    def render(self) -> Table:
        self.app.sub_title = self.__class__.__name__
        return self._info or self.table

    def reload(self) -> None:
        self.table.columns = []
        for i in self.render_fn:
            name = Text(i.name and i.name or snake_to_camel(i.key))
            f, icon = self._orderby.get_order_icon()
            if i.key == f:
                name += icon
            self.table.add_column(name)
        self.reloadImpl()
        self.table.rows = []

        data = self._orderby.sort(self.data)
        for idx, item in enumerate(data):

            def try_render(col_: Column, idx_: int, item_: t.Any) -> t.Any:
                if col_.render:
                    return col_.render(idx_, item_)
                return get_field(item_, col_.key)

            self.table.add_row(*[try_render(i, idx, item) for i in self.render_fn])

        self.highlight_row(self.cursor_line)
        self.refresh()

    def highlight_row(self, row: int) -> None:
        self.table.row_styles = [
            Style(bgcolor="magenta") if i == row else ""
            for i in range(self.table.row_count)
        ]

    async def on_key(self, event: events.Key) -> None:
        if event.key == "r":
            self.reload()
        if self._orderby.record_key(event.key):
            self.reload()
        await self.dispatch_key(event)

    async def key_down(self) -> None:
        self.cursor_down()

    async def key_j(self) -> None:
        self.cursor_down()

    async def key_up(self) -> None:
        self.cursor_up()

    async def key_k(self) -> None:
        self.cursor_up()

    def cursor_down(self) -> None:
        if self.cursor_line < self.table.row_count - 1:
            self.cursor_line += 1

    def cursor_up(self) -> None:
        if self.cursor_line > 0:
            self.cursor_line -= 1

    async def key_i(self) -> None:
        self.show_info = True

    async def key_escape(self) -> None:
        self.show_info = False

    async def key_h(self) -> None:
        self.show_info = False


class Models(TableWidget):
    """Models represents starwhale model view"""

    # TODO use constance
    def __init__(self, uri: str = default_project, **kwargs: t.Any) -> None:
        super().__init__(**kwargs)
        self.render_fn = [
            Column("name"),
            Column("version"),
            Column("tags", render=lambda _, x: ",".join(x["tags"])),
            Column("size", render=lambda _, x: pretty_bytes(x["size"])),
            Column(CREATED_AT_KEY, "Created At"),
        ]
        self.uri = uri
        self.reload()

    def reloadImpl(self) -> None:
        self.data, _ = ModelTermView.list(self.uri)


class Datasets(TableWidget):
    """Datasets represents starwhale model view"""

    def __init__(self, uri: str = default_project, **kwargs: t.Any) -> None:
        super().__init__(**kwargs)
        self.render_fn = [
            Column("name"),
            Column("version"),
            Column("tags", render=lambda _, x: ",".join(x["tags"])),
            Column("size", render=lambda _, x: pretty_bytes(x["size"])),
            Column(CREATED_AT_KEY, "Created At"),
        ]
        self.uri = uri
        self.reload()

    def reloadImpl(self) -> None:
        self.data, _ = DatasetTermView.list(self.uri)


class Runtimes(TableWidget):
    """Runtimes represents starwhale model view"""

    def __init__(self, uri: str = default_project, **kwargs: t.Any) -> None:
        super().__init__(**kwargs)
        self.render_fn = [
            Column("name"),
            Column("version"),
            Column("tags", render=lambda _, x: ",".join(x["tags"])),
            Column("size", render=lambda _, x: pretty_bytes(x["size"])),
            Column(CREATED_AT_KEY, "Created At"),
        ]
        self.uri = uri
        self.reload()

    def reloadImpl(self) -> None:
        self.data, _ = RuntimeTermView.list(self.uri)


class Jobs(TableWidget):
    """Job represents starwhale model view"""

    def __init__(self, uri: str = default_project, **kwargs: t.Any) -> None:
        super().__init__(**kwargs)
        self.render_fn = [
            Column("manifest.version", "Name"),
            Column("manifest.model", "Model"),
            Column(
                "manifest.datasets",
                "Datasets",
                render=lambda _, x: console.Pretty(get_field(x, "manifest.datasets")),
            ),
            Column(f"manifest.{CREATED_AT_KEY}", "Created At"),
            Column("manifest.finished_at", "Finished At"),
        ]
        self.uri = uri
        self.reload()

    def reloadImpl(self) -> None:
        self.data, _ = JobTermView.list(self.uri)
