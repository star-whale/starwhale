import typing as t

import click
from textual import events
from textual.app import App
from textual.widget import Widget, RenderableType
from textual.widgets import Footer, Header, ScrollView

from .widgets import Jobs, Models, Datasets, Runtimes
from .project_tree import ProjectTree, ProjectClick


class Dashboard(App):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.main_table: t.Optional[RenderableType] = None
        self.project_uri = ""
        self.key_mapping: t.Dict[str, t.Callable] = {
            "M": Models,
            "D": Datasets,
            "R": Runtimes,
            "J": Jobs,
        }

    async def on_load(self, event: events.Load) -> None:
        pass

    async def on_mount(self, event: events.Mount) -> None:
        self.body = ScrollView(fluid=False)
        await self.view.dock(Header(style=""), edge="top")
        await self.view.dock(Footer(), edge="bottom")
        await self.view.dock(
            ScrollView(ProjectTree("Starwhale", "projects")),
            edge="left",
            size=30,
            name="sidebar",
        )
        await self.view.dock(self.body, edge="top")

    async def on_key(self, event: events.Key) -> None:
        if event.key in self.key_mapping:
            await self.update_main_table(self.key_mapping[event.key](self.project_uri))

    async def update_main_table(self, w: Widget):
        self.main_table = w
        await self.body.update(self.main_table)
        await self.main_table.focus()

    async def handle_project_click(self, message: ProjectClick) -> None:
        self.project_uri = message.uri
        await self.update_main_table(Models(self.project_uri))


@click.command(
    "board", help="A terminal based UI to interact with your starwhale instances"
)
def open_board() -> None:
    Dashboard.run(title="Starwhale")
