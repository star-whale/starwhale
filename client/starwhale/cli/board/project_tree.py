from dataclasses import dataclass

import rich.repr
from textual import events
from textual._types import MessageTarget
from textual.widget import Message
from textual.widgets import TreeClick, TreeControl

from starwhale.core.instance.view import InstanceTermView
from starwhale.core.project.model import Project


@dataclass
class ProjectEntry:
    path: str
    is_project: bool


@rich.repr.auto
class ProjectClick(Message, bubble=True):
    def __init__(self, sender: MessageTarget, uri: str) -> None:
        self.uri = uri
        super().__init__(sender)


class ProjectTree(TreeControl[ProjectEntry]):
    def __init__(self, uri: str, name: str):
        data = ProjectEntry("", False)
        super().__init__(uri, name=name, data=data)

    async def on_mount(self, event: events.Mount) -> None:
        ins = InstanceTermView().list()
        for i in ins:
            await self.root.add(i["name"], ProjectEntry(i["name"], False))
        await self.root.expand()
        self.refresh(layout=True)

    async def handle_tree_click(self, message: TreeClick[ProjectEntry]) -> None:
        dir_entry = message.node.data
        # root entry
        if not dir_entry.path:
            return
        if dir_entry.is_project:
            await self.emit(ProjectClick(self, dir_entry.path))
        else:
            if not message.node.loaded:
                ins = message.node.data.path
                ps, _ = Project.list(ins)
                for i in ps:
                    await message.node.add(
                        i["name"], ProjectEntry(f"{ins}/{i['name']}", True)
                    )
                await message.node.expand()
                message.node.loaded = True
            else:
                await message.node.toggle()
