import os
import typing as t
from pathlib import Path

from starwhale.utils import console, load_yaml, pretty_bytes, in_production
from starwhale.consts import (
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    STANDALONE_INSTANCE,
)
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.utils.error import NoSupportError
from starwhale.utils.config import SWCliConfigMixed

from .model import Runtime, StandaloneRuntime


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
    def history(self, fullname: bool = False) -> t.List[t.Dict[str, t.Any]]:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        return self._print_history(
            title="Runtime History", history=self.runtime.history(), fullname=fullname
        )

    @BaseTermView._header
    def info(self, fullname: bool = False) -> None:
        self._print_info(self.runtime.info(), fullname=fullname)

    @classmethod
    @BaseTermView._only_standalone
    def activate(cls, path: str = "", uri: str = "") -> None:
        Runtime.activate(path, uri)

    @BaseTermView._only_standalone
    def dockerize(
        self,
        tags: t.List[str],
        push: bool,
        platforms: t.List[str],
        dry_run: bool,
        use_starwhale_builder: bool,
        reset_qemu_static: bool,
    ) -> None:
        self.runtime.dockerize(
            tags=tags,
            push=push,
            platforms=platforms,
            dry_run=dry_run,
            use_starwhale_builder=use_starwhale_builder,
            reset_qemu_static=reset_qemu_static,
        )

    @classmethod
    @BaseTermView._only_standalone
    def lock(
        cls,
        target_dir: str,
        yaml_name: str = DefaultYAMLName.RUNTIME,
        env_name: str = "",
        env_prefix_path: str = "",
        disable_auto_inject: bool = False,
        stdout: bool = False,
        include_editable: bool = False,
        emit_pip_options: bool = False,
        env_use_shell: bool = False,
    ) -> None:
        Runtime.lock(
            target_dir,
            yaml_name,
            env_name,
            env_prefix_path,
            disable_auto_inject,
            stdout,
            include_editable,
            emit_pip_options,
            env_use_shell,
        )

    @classmethod
    @BaseTermView._only_standalone
    def build(
        cls,
        workdir: str,
        project: str = "",
        yaml_name: str = DefaultYAMLName.RUNTIME,
        gen_all_bundles: bool = False,
        include_editable: bool = False,
        disable_env_lock: bool = False,
        env_prefix_path: str = "",
        env_name: str = "",
        env_use_shell: bool = False,
    ) -> None:
        _config = load_yaml(Path(workdir) / yaml_name)
        _runtime_uri = cls.prepare_build_bundle(
            project=project, bundle_name=_config.get("name"), typ=URIType.RUNTIME
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
            disable_env_lock=disable_env_lock,
            env_prefix_path=env_prefix_path,
            env_name=env_name,
            env_use_shell=env_use_shell,
        )

    @BaseTermView._only_standalone
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
    @BaseTermView._only_standalone
    def quickstart_from_uri(
        cls,
        workdir: t.Union[Path, str],
        name: str,
        uri: URI,
        force: bool = False,
        disable_restore: bool = False,
    ) -> None:
        console.print(
            f":construction: quickstart Starwhale Runtime[{name}] environment from runtime URI({uri})..."
        )
        _sw_config = SWCliConfigMixed()
        if _sw_config.current_instance != STANDALONE_INSTANCE:
            raise NoSupportError(f"{_sw_config.current_instance} quickstart")
        StandaloneRuntime.quickstart_from_uri(
            workdir, name, uri, force, disable_restore
        )
        console.print(":clap: Starwhale Runtime environment is ready to use :tada:")

    @classmethod
    @BaseTermView._only_standalone
    def quickstart_from_ishell(
        cls,
        workdir: t.Union[Path, str],
        name: str,
        mode: str,
        disable_create_env: bool = False,
        force: bool = False,
        interactive: bool = False,
    ) -> None:
        console.print(
            f":construction: quickstart Starwhale Runtime[{name}] environment..."
        )
        StandaloneRuntime.quickstart_from_ishell(
            workdir, name, mode, disable_create_env, force, interactive
        )
        console.print(":clap: Starwhale Runtime environment is ready to use :tada:")

    @classmethod
    @BaseTermView._only_standalone
    def restore(cls, target: str) -> None:
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
        else:
            _uri = URI(target, URIType.RUNTIME)
            _runtime = StandaloneRuntime(_uri)
            workdir = _runtime.store.snapshot_workdir
            if not workdir.exists():
                _runtime.extract(force=True, target=workdir)

        console.print(
            f":golfer: try to restore python runtime environment: {workdir} ..."
        )
        Runtime.restore(workdir)

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
    ) -> None:
        Runtime.copy(src_uri, dest_uri, force, dest_local_project_uri)
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
    def list(  # type: ignore
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

    def history(self, fullname: bool = False) -> None:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        _data = BaseTermView.get_history_data(
            history=self.runtime.history(), fullname=fullname
        )
        self.pretty_json(_data)


def get_term_view(ctx_obj: t.Dict) -> t.Type[RuntimeTermView]:
    return (
        RuntimeTermViewJson if ctx_obj.get("output") == "json" else RuntimeTermViewRich
    )
