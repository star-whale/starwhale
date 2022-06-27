import os
import sys
import typing as t
from functools import wraps

import yaml
from rich import box
from rich import print as rprint
from rich.panel import Panel
from rich.table import Table
from rich.pretty import Pretty
from rich.console import RenderableType

from starwhale.utils import console, pretty_bytes
from starwhale.consts import UserRoleType, SHORT_VERSION_CNT
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.utils.error import FileFormatError
from starwhale.utils.config import SWCliConfigMixed


class BaseTermView(SWCliConfigMixed):
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
    def _simple_action_print(func):
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            rt = func(*args, **kwargs)

            if isinstance(rt, tuple) and len(rt) == 2:
                if rt[0]:
                    console.print(":clap: do successfully")
                else:
                    console.print(f":diving_mask: failed to run, reason:{rt[1]}")
                    sys.exit(1)
            return rt

        return _wrapper

    @staticmethod
    def comparison(r1: RenderableType, r2: RenderableType) -> Table:
        table = Table(show_header=False, pad_edge=False, box=None, expand=True)
        table.add_column("1")
        table.add_column("2")
        table.add_row(r1, r2)
        return table

    @staticmethod
    def pretty_status(status: str) -> t.Tuple[str, str, str]:
        status = status.lower()
        style = "blue"
        icon = ":tractor:"
        if status == "success":
            style = "green"
            icon = ":clap:"
        elif status in ("fail", "failed"):
            style = "red"
            icon = ":fearful:"
        return status, style, icon

    @staticmethod
    def prepare_build_bundle(
        workdir: str, project: str, yaml_name: str, typ: str
    ) -> URI:
        console.print(f":construction: start to build {typ} bundle...")
        _project_uri = URI(project, expected_type=URIType.PROJECT)
        _path = os.path.join(workdir, yaml_name)
        _config = yaml.safe_load(open(_path, "r"))
        if "name" not in _config:
            raise FileFormatError(f"{_path}, no name field")

        _uri = URI.capsulate_uri(
            instance=_project_uri.instance,
            project=_project_uri.project,
            obj_type=typ,
            obj_name=_config["name"],
        )
        console.print(f":construction_worker: uri:{_uri}")
        return _uri

    @staticmethod
    def _print_history(
        title: str,
        history: t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]],
        fullname: bool = False,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        table = Table(title=title, box=box.SIMPLE, expand=True)
        table.add_column("Version", justify="left", style="cyan", no_wrap=True)
        table.add_column("Tags")
        table.add_column("Size")
        table.add_column("Runtime")
        table.add_column("Created")

        for _h in history[0]:
            _version = _h["version"] if fullname else _h["version"][:SHORT_VERSION_CNT]
            if _h.get("id"):
                _version = f"[{_h['id']:2}] {_version}"

            table.add_row(
                _version,
                ",".join(_h.get("tags", [])),
                pretty_bytes(_h["size"]),
                _h.get("runtime", "--"),
                _h["created_at"],
            )
        console.print(table)
        return history

    @staticmethod
    def _print_info(_info: t.Dict[str, t.Any], fullname: bool = False) -> None:
        if not _info:
            console.print(":tea: not found info")
            return

        _history = _info.pop("history", [])

        console.rule("[green bold]Inspect Details")
        console.print(Pretty(_info, expand_all=True))

        if _history:
            console.rule("[green bold] Version History")
            BaseTermView._print_history(
                title="History List", history=_history, fullname=fullname
            )

    @staticmethod
    def _print_list(
        _bundles: t.Dict[str, t.Any], show_removed: bool = False, fullname: bool = False
    ) -> None:
        table = Table(title="Bundle List", box=box.SIMPLE, expand=True)

        table.add_column("Name")
        table.add_column("Version")
        table.add_column("Tags")
        table.add_column("Size")
        table.add_column("Runtime")
        table.add_column("Created")

        for _name, _versions in _bundles.items():
            for _v in _versions:
                if show_removed ^ _v["is_removed"]:
                    continue

                _version = (
                    _v["version"]
                    if fullname or show_removed
                    else _v["version"][:SHORT_VERSION_CNT]
                )
                if _v.get("id"):
                    _version = f"[{_v['id']:2}] {_version}"

                table.add_row(
                    _name,
                    _version,
                    ",".join(_v.get("tags", [])),
                    pretty_bytes(_v["size"]),
                    _v.get("runtime", "--"),
                    _v["created_at"],
                )

        console.print(table)
