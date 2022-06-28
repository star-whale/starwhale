import os
import typing as t
from pathlib import Path

from starwhale.utils import console, pretty_bytes, in_production
from starwhale.consts import (
    PythonRunEnv,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_PYTHON_VERSION,
)
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.core.runtime.store import RuntimeStorage

from .model import Runtime


class RuntimeTermView(BaseTermView):
    def __init__(self, runtime_uri: str) -> None:
        super().__init__()

        self.raw_uri = runtime_uri
        self.uri = URI(runtime_uri, expected_type=URIType.RUNTIME)
        self.runtime = Runtime.get_runtime(self.uri)

    @BaseTermView._simple_action_print
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.runtime.remove(force)

    @BaseTermView._simple_action_print
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.runtime.recover(force)

    @BaseTermView._pager
    @BaseTermView._header
    def history(
        self, fullname: bool = False
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        return self._print_history(
            title="Runtime History", history=self.runtime.history(), fullname=fullname
        )

    @BaseTermView._header
    def info(self, fullname: bool = False) -> None:
        self._print_info(self.runtime.info(), fullname=fullname)

    @classmethod
    def activate(cls, workdir: str, yaml_name: str = DefaultYAMLName.RUNTIME) -> None:
        Runtime.activate(workdir, yaml_name)

    @classmethod
    def build(
        cls,
        workdir: str,
        project: str = "",
        yaml_name: str = DefaultYAMLName.RUNTIME,
        gen_all_bundles: bool = False,
        include_editable: bool = False,
    ) -> None:
        _runtime_uri = cls.prepare_build_bundle(
            workdir, project, yaml_name, URIType.RUNTIME
        )
        if include_editable:
            console.print(
                ":bell: [red bold]runtime will include pypi editable package[/] :bell:"
            )
        else:
            console.print(
                ":bird: [red bold]runtime will ignore pypi editable package[/]"
            )

        _rt = Runtime.get_runtime(_runtime_uri)
        _rt.build(
            Path(workdir),
            yaml_name,
            gen_all_bundles=gen_all_bundles,
            include_editable=include_editable,
        )

    def extract(self, force: bool = False, target: t.Union[str, Path] = "") -> None:
        console.print(":oncoming_police_car: try to extract ...")
        path = self.runtime.extract(force, target)
        console.print(f":clap: extracted @ {path.resolve()} :tada:")

    @classmethod
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _uri = URI(project_uri, expected_type=URIType.PROJECT)
        fullname = fullname or (_uri.instance_type == InstanceType.CLOUD)
        _runtimes, _pager = Runtime.list(_uri, page, size)
        _data = BaseTermView.list_data(_runtimes, show_removed, fullname)
        return _data, _pager

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
            workdir,
            name,
            python_version=python_version,
            mode=mode,
            force=force,
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
        Runtime.copy(src_uri, dest_uri, force)
        console.print(":clap: copy done.")

    @BaseTermView._header
    def tag(self, tags: t.List[str], remove: bool = False, quiet: bool = False) -> None:
        # TODO: refactor model/runtime/dataset tag view-model
        if remove:
            console.print(f":golfer: remove tags [red]{tags}[/] @ {self.uri}...")
            self.runtime.remove_tags(tags, quiet)
        else:
            console.print(f":surfer: add tags [red]{tags}[/] @ {self.uri}...")
            self.runtime.add_tags(tags, quiet)


class RuntimeTermViewRich(RuntimeTermView):
    @classmethod
    @BaseTermView._pager
    @BaseTermView._header
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _data, _pager = super().list(project_uri, fullname, show_removed, page, size)

        custom_column: t.Dict[str, t.Callable[[t.Any], str]] = {
            "tags": lambda x: ",".join(x),
            "size": lambda x: pretty_bytes(x),
            "runtime": cls.place_holder_for_empty(),
        }

        cls.print_table("Runtime List", _data, custom_column=custom_column)
        return _data, _pager


class RuntimeTermViewJson(RuntimeTermView):
    @classmethod
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> None:
        _data, _pager = super().list(project_uri, fullname, show_removed, page, size)
        cls.pretty_json(_data)

    def info(self, fullname: bool = False) -> None:
        _data = self.get_info_data(self.runtime.info(), fullname=fullname)
        self.pretty_json(_data)


def get_term_view(ctx_obj: t.Dict) -> t.Type[RuntimeTermView]:
    return (
        RuntimeTermViewJson if ctx_obj.get("output") == "json" else RuntimeTermViewRich
    )
