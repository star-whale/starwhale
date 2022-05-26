import typing as t
import sys
import os
from pathlib import Path
from starwhale.core.runtime.store import RuntimeStorage
from starwhale.utils.error import FileFormatError
import yaml

from rich.pretty import Pretty
from rich.table import Table
from rich import box

from starwhale.consts import (
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_PYTHON_VERSION,
    SHORT_VERSION_CNT,
    DefaultYAMLName,
    PythonRunEnv,
)
from starwhale.base.type import URIType
from starwhale.base.uri import URI
from starwhale.base.view import BaseView
from starwhale.utils import console, in_production, pretty_bytes

from .model import Runtime


class RuntimeTermView(BaseView):
    def __init__(self, runtime_uri: str) -> None:
        super().__init__()

        self.raw_uri = runtime_uri
        self.uri = URI(runtime_uri, expected_type=URIType.RUNTIME)
        self.runtime = Runtime.get_runtime(self.uri)

    def remove(self, force: bool = False) -> None:
        ok, reason = self.runtime.remove(force)
        if ok:
            self._console.print(f":clap: remove successfully : {self.uri}")
        else:
            self._console.print(
                f":diving_mask: failed to remove runtime({self.uri}), reason:{reason}"
            )
            sys.exit(1)

    def recover(self, force: bool = False) -> None:
        ok, reason = self.runtime.recover(force)
        if ok:
            self._console.print(f":clap: recover successfully : {self.uri}")
        else:
            self._console.print(
                f":diving_mask: failed to recover runtime({self.uri}), reason:{reason}"
            )
            sys.exit(1)

    @BaseView._header
    def history(self, fullname: bool = False) -> None:
        return self._print_history(self.runtime.history(), fullname)

    @BaseView._header
    def info(self, fullname: bool = False) -> None:
        _rt = self.runtime.info()
        if not _rt:
            self._console.print(":tea: not found info")
            return

        _history = _rt.pop("history", [])

        self._console.rule(f"[green bold]Inspect Runtime:{self.uri}")
        self._console.print(Pretty(_rt, expand_all=True))

        if _history:
            self._console.rule("[green bold] Runtime version history")
            self._print_history(_history, fullname)

    def _print_history(
        self, history: t.List[t.Dict[str, t.Any]], fullname: bool = False
    ) -> None:
        table = Table(box=box.SIMPLE, expand=True)
        table.add_column("Version", justify="left", style="cyan", no_wrap=True)
        table.add_column("Size")
        table.add_column("Created")

        for _h in history:
            table.add_row(
                _h["version"] if fullname else _h["version"][:SHORT_VERSION_CNT],
                pretty_bytes(_h["size"]),
                _h["created_at"],
            )

        console.print(table)

    @classmethod
    def build(
        cls,
        workdir: str,
        project: str = "",
        runtime_yaml_name: str = DefaultYAMLName.RUNTIME,
        gen_all_bundles: bool = False,
    ) -> None:

        console.print(":construction: start to build runtime bundle...")
        _project_uri = URI(project, expected_type=URIType.PROJECT)
        _path = os.path.join(workdir, runtime_yaml_name)
        _config = yaml.safe_load(open(_path, "r"))
        if "name" not in _config:
            raise FileFormatError(f"runtime.yaml {_path}, no name field")

        _runtime_uri = URI.capsulate_uri(
            instance=_project_uri.instance,
            project=_project_uri.project,
            obj_type=URIType.RUNTIME,
            obj_name=_config["name"],
        )
        console.print(f":construction_worker: uri:{_runtime_uri}")
        _rt = Runtime.get_runtime(_runtime_uri)
        _rt.build(workdir, runtime_yaml_name, gen_all_bundles)

    def extract(self, force: bool = False, target: t.Union[str, Path] = "") -> None:
        self._console.print(":oncoming_police_car: try to extract ...")
        path = self.runtime.extract(force, target)
        self._console.print(f":clap: extracted @ {path.resolve()} :tada:")

    @classmethod
    @BaseView._pager
    @BaseView._header
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        _uri = URI(project_uri, expected_type=URIType.PROJECT)
        _runtimes, _pager = Runtime.list(_uri, page, size)

        table = Table(title="Runtime List", box=box.SIMPLE, expand=True)
        table.add_column("Name", justify="left", style="cyan", no_wrap=True)
        table.add_column("Version")
        table.add_column("Size")
        table.add_column("Created")

        for _rt_name, _rt_versions in _runtimes.items():
            for _rt in _rt_versions:
                if show_removed ^ _rt["is_removed"]:
                    continue

                table.add_row(
                    _rt_name,
                    _rt["version"]
                    if fullname or show_removed
                    else _rt["version"][:SHORT_VERSION_CNT],
                    pretty_bytes(_rt["size"]),
                    _rt["created_at"],
                )

        console.print(table)
        return _runtimes, _pager

    @classmethod
    def create(
        cls,
        workdir: str,
        name: str,
        python_version: str = DEFAULT_PYTHON_VERSION,
        mode: str = PythonRunEnv.VENV,
        force: bool = False,
    ) -> None:
        console.print(f":construction: start to create runtime[{name}] environment...")
        Runtime.create(
            workdir, name, python_version=python_version, mode=mode, force=force
        )
        console.print(":clap: python runtime environment is ready to use :tada:")

    @classmethod
    def restore(cls, target: str) -> None:
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
        else:
            uri = URI(target, URIType.RUNTIME)
            store = RuntimeStorage(uri)
            workdir = store.snapshot_workdir

        console.print(f":golfer: try to restore python runtime environment{workdir}...")
        Runtime.restore(workdir)

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
        Runtime.copy(URI(src_uri), URI(dest_uri), force)
