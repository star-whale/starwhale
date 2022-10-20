import sys
import json
import typing as t
from functools import wraps

from rich import box
from rich import print as rprint
from rich.panel import Panel
from rich.table import Table
from rich.pretty import Pretty
from rich.console import RenderableType

from starwhale.utils import Order, console, pretty_bytes, sort_obj_list, snake_to_camel
from starwhale.consts import UserRoleType, SHORT_VERSION_CNT, STANDALONE_INSTANCE
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.utils.error import FieldTypeOrValueError
from starwhale.utils.config import SWCliConfigMixed


class BaseTermView(SWCliConfigMixed):
    @staticmethod
    def _pager(func: t.Callable) -> t.Callable:
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
    def _header(func: t.Callable) -> t.Callable:
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
    def _only_standalone(func: t.Callable) -> t.Callable:
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            sw = SWCliConfigMixed()
            if sw.current_instance != STANDALONE_INSTANCE:
                console.print(
                    ":see_no_evil: This command only supports running in the standalone instance."
                )
                sys.exit(1)

            return func(*args, **kwargs)  # type: ignore

        return _wrapper

    @staticmethod
    def _simple_action_print(func: t.Callable) -> t.Callable:
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> t.Any:
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
    def prepare_build_bundle(project: str, bundle_name: str, typ: str) -> URI:
        console.print(f":construction: start to build {typ} bundle...")
        _project_uri = URI(project, expected_type=URIType.PROJECT)
        if not bundle_name:
            raise FieldTypeOrValueError("no bundle_name")

        _uri = URI.capsulate_uri(
            instance=_project_uri.instance,
            project=_project_uri.project,
            obj_type=typ,
            obj_name=bundle_name,
        )
        console.print(f":construction_worker: uri:{_uri}")
        return _uri

    @staticmethod
    def _print_history(
        title: str,
        history: t.List[t.Dict[str, t.Any]],
        fullname: bool = False,
    ) -> t.List[t.Dict[str, t.Any]]:
        custom_header = {0: {"justify": "left", "style": "cyan", "no_wrap": True}}
        custom_column: t.Dict[str, t.Callable[[t.Any], str]] = {
            "tags": lambda x: ",".join(x),
            "size": lambda x: pretty_bytes(x),
            "runtime": BaseTermView.place_holder_for_empty(),
        }
        data = BaseTermView.get_history_data(history, fullname)
        BaseTermView.print_table(
            title, data, custom_header, custom_column=custom_column
        )
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

    @staticmethod
    def pretty_json(data: t.Any) -> None:
        print(json.dumps(data, indent=4, sort_keys=True))

    @staticmethod
    def list_data(
        _bundles: t.Dict[str, t.Any], show_removed: bool = False, fullname: bool = False
    ) -> t.List[t.Dict[str, t.Any]]:
        result = []
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

                result.append(
                    {
                        "name": _name,
                        "version": _version,
                        "tags": _v.get("tags", []),
                        "size": _v.get("size", 0),
                        "created_at": _v.get("created_at"),
                    }
                )

        order_keys = [Order("name"), Order("created_at", True)]
        return sort_obj_list(result, order_keys)

    @staticmethod
    def place_holder_for_empty(place_holder: str = "--") -> t.Callable[[str], str]:
        return lambda x: x or place_holder

    @staticmethod
    def print_table(
        title: str,
        data: t.List[t.Dict[str, t.Any]],
        custom_header: t.Optional[t.Dict[int, t.Dict]] = None,
        custom_column: t.Optional[t.Dict[str, t.Callable[[t.Any], str]]] = None,
        custom_row: t.Optional[t.Callable] = None,
        custom_table: t.Optional[t.Dict[str, t.Any]] = None,
    ) -> None:
        default_attr = {
            "title": title,
            "box": box.SIMPLE,
            "expand": True,
        }
        if custom_table:
            default_attr = {**default_attr, **custom_table}
        table = Table(**default_attr)  # type: ignore

        def init_header(row: t.List[str]) -> None:
            for idx, field in enumerate(row):
                extra = dict()
                if custom_header and idx in custom_header:
                    extra = custom_header[idx]
                table.add_column(snake_to_camel(field), **extra)

        header_inited = False
        for row in data:
            if not header_inited:
                init_header(list(row.keys()))
                header_inited = True
            rendered_row = list()
            for field, col in row.items():
                if custom_column and field in custom_column:
                    col = custom_column[field](col)
                rendered_row.append(col)

            row_ext: t.Dict[str, t.Any] = {}
            if custom_row:
                row_ext = custom_row(row) or {}
            table.add_row(*rendered_row, **row_ext)

        if table.row_count == 0:
            console.print("empty")
        console.print(table)

    @staticmethod
    def get_info_data(
        _info: t.Dict[str, t.Any], fullname: bool = False
    ) -> t.Dict[str, t.Any]:
        if not _info:
            return dict()

        result = _info
        _history = _info.pop("history", [])

        if _history:
            result["history"] = BaseTermView.get_history_data(_history, fullname)

        return result

    @staticmethod
    def get_history_data(
        history: t.List[t.Dict[str, t.Any]],
        fullname: bool = False,
    ) -> t.List[t.Dict]:
        result = list()

        for _h in history:
            _version = _h["version"] if fullname else _h["version"][:SHORT_VERSION_CNT]
            if _h.get("id"):
                _version = f"[{_h['id']:2}] {_version}"

            result.append(
                {
                    "version": _version,
                    "tags": _h.get("tags", []),
                    "size": _h.get("size", 0),
                    "runtime": _h.get("runtime", ""),
                    "created_at": _h.get("created_at"),
                }
            )
        return result
