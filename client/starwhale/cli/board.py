import typing as t
from abc import abstractmethod
from dataclasses import dataclass

import click
from rich import console
from textual import events
from rich.style import Style
from rich.table import box, Table
from textual.app import App, Reactive
from textual.widget import Widget, RenderableType
from textual.widgets import Footer, Header, ScrollView

from ..utils import pretty_bytes, snake_to_camel
from ..consts import STANDALONE_INSTANCE
from ..core.model.view import ModelTermView
from ..core.instance.view import InstanceTermView
from ..core.project.model import Project
from ..core.instance.model import Instance, CloudInstance, StandaloneInstance


@dataclass
class Column:
    key: str
    name: t.Optional[str] = ""
    render: t.Optional[t.Callable[[int, t.Any], t.Any]] = None


class TableWidget(Widget):
    """TableWidget makes an interactive rich.Table"""

    def __init__(self, **kwargs) -> None:
        super().__init__(**kwargs)
        self.table = Table(expand=True, box=box.SIMPLE)
        self.data: t.Sequence = []
        self.render_fn: t.List[Column] = []
        self._info: t.Any = None

    show_info: Reactive[bool] = Reactive(False)
    cursor_line: Reactive[int] = Reactive(0, repaint=False)

    def watch_show_info(self, show: bool) -> None:
        self._info = show and self.info(self.cursor_line) or None
        self.refresh()

    def watch_cursor_line(self, value: int) -> None:
        self.highlight_row(value)
        self.refresh()

    @abstractmethod
    def reloadImpl(self) -> None:
        raise NotImplementedError

    def info(self, idx: int) -> RenderableType:
        return console.Pretty(self.data[idx], indent_guides=True)

    def watch_data(self):
        pass

    def render(self) -> Table:
        return self._info or self.table

    def reload(self) -> None:
        self.table.columns = []
        for i in self.render_fn:
            self.table.add_column(i.name and i.name or snake_to_camel(i.key))
        self.reloadImpl()
        self.table.rows = []
        for idx, item in enumerate(self.data):

            def try_render(col: Column):
                if col.render:
                    return col.render(idx, item)
                return item.get(col.key)

            self.table.add_row(*[try_render(i) for i in self.render_fn])

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
        await self.dispatch_key(event)

    async def key_down(self) -> None:
        self.cursor_down()

    async def key_j(self):
        self.cursor_down()

    async def key_up(self) -> None:
        self.cursor_up()

    async def key_k(self):
        self.cursor_up()

    def cursor_down(self) -> None:
        if self.cursor_line < self.table.row_count - 1:
            self.cursor_line += 1

    def cursor_up(self) -> None:
        if self.cursor_line > 0:
            self.cursor_line -= 1

    async def key_i(self):
        self.show_info = True

    async def key_escape(self):
        self.show_info = False

    async def key_h(self):
        self.show_info = False


class Models(TableWidget):
    """Models represents starwhale model view"""

    def __init__(self, **kwargs) -> None:
        super().__init__(**kwargs)
        self.render_fn = [
            Column("name"),
            Column("version"),
            Column("tags", render=lambda _, x: ",".join(x["tags"])),
            Column("size", render=lambda _, x: pretty_bytes(x["size"])),
            Column("created_at", "Created At"),
        ]
        self.reload()

    def reloadImpl(self) -> None:
        self.data, _ = ModelTermView("local/self").list()


class Projects(TableWidget):
    """Projects represents starwhale project view"""

    def __init__(self, uri: str, **kwargs):
        super().__init__(**kwargs)
        self.render_fn = [
            Column("name"),
            Column("owner"),
            Column("created_at", "Created At"),
        ]
        self.uri = uri
        self.reload()

        self.current = None

    def reloadImpl(self) -> None:
        self.data, _ = Project.list(self.uri)


class Instances(TableWidget):
    """Instance represents starwhale instance view"""

    def __init__(self, **kwargs) -> None:
        super().__init__(**kwargs)
        self.render_fn = [
            Column("name"),
            Column("uri"),
            Column("user_name"),
            Column("user_role"),
            Column("current_project"),
            Column("updated_at", "Last Update"),
        ]
        self.reload()

        self.current = None

    show_projects: Reactive[bool] = Reactive(False)

    def reloadImpl(self) -> None:
        self.data = InstanceTermView().list()

    def get_current(self) -> Instance:
        row = self.data[self.cursor_line]
        # TODO refine instance detection logic
        name = row["name"]
        if name == STANDALONE_INSTANCE:
            return StandaloneInstance()
        return CloudInstance(name)

    async def key_enter(self):
        self.show_projects = True

    def key_h(self):
        self.show_projects = False

    def render(self) -> Table:
        if self.show_projects:
            return Projects(uri=self.get_current().uri.raw).render()
        return super().render()


class Dashboard(App):
    def __init__(self, **kwargs):
        self.body: t.Union[ScrollView, None] = None
        self.table = Instances()
        super().__init__(**kwargs)

    async def on_load(self, event: events.Load) -> None:
        pass

    async def on_mount(self, event: events.Mount) -> None:
        await self.view.dock(Header(style=""), edge="top")
        await self.view.dock(Footer(), edge="bottom")
        await self.view.dock(self.table, edge="right")

    async def on_key(self, event: events.Key) -> None:
        await self.table.on_key(event)


@click.command(
    "board", help="A terminal based UI to interact with your starwhale instances"
)
def open_board() -> None:
    Dashboard.run(title="Starwhale")
