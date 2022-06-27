import sys
import typing as t

from rich import box
from rich.tree import Tree
from rich.panel import Panel
from rich.table import Table
from rich.pretty import Pretty

from starwhale.utils import console, pretty_bytes
from starwhale.consts import DEFAULT_PROJECT, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.base.view import BaseTermView

from .model import Project, ProjectObjType


class ProjectTermView(BaseTermView):
    def __init__(self, project_uri: str = "") -> None:
        super().__init__()
        self.raw_uri = project_uri
        self.uri = URI(project_uri, expected_type=URIType.PROJECT)
        self.project = Project.get_project(self.uri)

    @BaseTermView._simple_action_print
    def create(self) -> t.Tuple[bool, str]:
        return self.project.create()

    @classmethod
    @BaseTermView._pager  # type: ignore
    @BaseTermView._header  # type: ignore
    def list(
        cls,
        instance_uri: str = "",
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Any], t.Dict[str, t.Any]]:
        projects, pager = Project.list(instance_uri, page, size)

        table = Table(
            title="List Projects",
            box=box.SIMPLE,
            expand=True,
        )
        table.add_column("")
        table.add_column("Name")
        table.add_column("Location")
        table.add_column("Owner")
        table.add_column("Created")

        _current_project = URI(
            instance_uri, expected_type=URIType.INSTANCE
        ).sw_instance_config.get("current_project", DEFAULT_PROJECT)

        for _p in projects:
            _name = _p["name"]
            _is_current = _name == _current_project
            if _p.get("id"):
                _name = f"[{_p['id']}]{_name}"

            table.add_row(
                ":backhand_index_pointing_right:" if _is_current else "",
                _name,
                _p.get("location", "--"),
                _p.get("owner", "--"),
                _p["created_at"],
                style="magenta" if _is_current else "",
            )

        console.print(table)
        return projects, pager

    def select(self) -> None:
        try:
            self.select_current_default(
                instance=self.uri.instance, project=self.uri.project
            )
        except Exception as e:
            console.print(
                f":broken_heart: failed to select {self.raw_uri}, reason: {e}"
            )
            sys.exit(1)
        else:
            console.print(
                f":clap: select instance:{self.current_instance}, project:{self.current_project} successfully"
            )

    def remove(self, force: bool = False) -> None:
        ok, reason = self.project.remove(force)
        if ok:
            console.print(
                f":dog: remove project {self.project.name}, you can recover it, don't panic."
            )
        else:
            console.print(
                f":fearful: failed to remove project {self.project.name}, reason: {reason}"
            )
            sys.exit(1)

    def recover(self) -> None:
        ok, reason = self.project.recover()
        if ok:
            console.print(f":clap: recover project {self.project.name}")
        else:
            console.print(
                f":fearful: failed to recover project {self.project.name}, reason: {reason}"
            )
            sys.exit(1)

    @BaseTermView._header  # type: ignore
    def info(self, fullname: bool = False) -> None:
        _r = self.project.info()
        _models = _r.pop("models", [])
        _datasets = _r.pop("datasets", [])

        def _show_objects(objects: t.List[t.Dict[str, t.Any]], typ: str) -> Tree:
            tree = Tree(f"[red]{typ}[/]")
            for _o in objects:
                otree = tree.add(f"{_o['name']}")
                for _v in _o["latest_versions"]:
                    _k = "name" if fullname else "short_name"
                    if typ == ProjectObjType.MODEL:
                        # TODO: add model version for every version
                        _size = _o["files"][0]["size"]
                    else:
                        _size = pretty_bytes(_v["meta"]["dataset_byte_size"])

                    otree.add(
                        f"[{_v['id']}][green]{_v[_k]}[/] :timer_clock: {_v['created_at']} :dizzy:{_size}"
                    )
            return tree

        console.print(Panel(Pretty(_r), title="Project Details", title_align="left"))
        if _models or _datasets:
            _block = self.comparison(
                _show_objects(_models, ProjectObjType.MODEL),
                _show_objects(_datasets, ProjectObjType.DATASET),
            )
            console.print(_block)


# TODO: add ProjectHTTPView for http request
