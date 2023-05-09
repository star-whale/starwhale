from __future__ import annotations

import typing as t
from dataclasses import dataclass

import rich.repr
from rich.text import Text
from textual.widget import Style, Message
from textual.widgets import TreeControl

from starwhale.core.instance.view import InstanceTermView
from starwhale.core.project.model import Project

if t.TYPE_CHECKING:
    from textual import events
    from textual._types import MessageTarget
    from textual.widget import RenderableType
    from textual.widgets import NodeID, TreeNode, TreeClick


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
    def __init__(
        self, uri: str, name: str, current_instance: str = "", current_project: str = ""
    ):
        data = ProjectEntry("", False)
        super().__init__(uri, name=name, data=data)
        self.current_node_id: t.Optional[NodeID] = None
        self.current_instance = current_instance
        self.current_project = current_project

    async def on_mount(self, event: events.Mount) -> None:
        ins = InstanceTermView().list()
        for i in ins:
            await self.root.add(i["name"], ProjectEntry(i["name"], False))
        await self.root.expand()

        # find the node id of current instance
        if self.current_instance:
            cur_ins = None
            for nid in self.root.control.nodes:
                n = self.root.control.nodes[nid]
                if n.data.path == self.current_instance:
                    cur_ins = n
            if cur_ins is not None:
                # expand project list
                await self.add_projects(cur_ins)
                await cur_ins.expand()

                # make current project selected
                if self.current_project:
                    for nid in cur_ins.control.nodes:
                        n = cur_ins.control.nodes[nid]
                        if n.label == self.current_project:
                            await self.emit(ProjectClick(self, n.data.path))
                            self.current_node_id = nid

        self.refresh(layout=True)

    async def handle_tree_click(self, message: TreeClick[ProjectEntry]) -> None:
        dir_entry = message.node.data
        # root entry
        if not dir_entry.path:
            return

        if dir_entry.is_project:
            await self.emit(ProjectClick(self, dir_entry.path))
            self.current_node_id = message.node.id
        else:
            if not message.node.loaded:
                await self.add_projects(message.node)
                await message.node.expand()
                message.node.loaded = True
            else:
                await message.node.toggle()

    async def add_projects(self, node: TreeNode[ProjectEntry]) -> None:
        ins = node.data.path
        ps, _ = Project.list(ins)
        for i in ps:
            proj = ":".join([i["name"], i.get("owner", "")])
            path = f"{ins}/{proj}"
            await node.add(proj, ProjectEntry(path, True))

    def render_node(self, node: TreeNode[ProjectEntry]) -> RenderableType:
        label = (
            Text(node.label, no_wrap=True, overflow="ellipsis")
            if isinstance(node.label, str)
            else node.label
        )
        if node.id == self.current_node_id:
            label.stylize(Style(bgcolor="magenta"))

        if node.is_cursor:
            label.stylize("reverse")

        meta = {
            "@click": f"click_label({node.id})",
            "tree_node": node.id,
            "cursor": node.is_cursor,
        }
        label.apply_meta(meta)
        return label
