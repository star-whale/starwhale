from __future__ import annotations

import os
import typing as t
from pathlib import Path

from rich.pretty import Pretty
from rich.syntax import Syntax

from starwhale.utils import console, load_yaml, pretty_bytes, in_production
from starwhale.consts import (
    PythonRunEnv,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    STANDALONE_INSTANCE,
)
from starwhale.base.view import BaseTermView, TagViewMixin
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.venv import get_venv_env, get_conda_env, get_python_run_env
from starwhale.utils.error import NotFoundError, NoSupportError, ExclusiveArgsError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.runtime.model import Runtime, RuntimeInfoFilter, StandaloneRuntime
from starwhale.base.models.runtime import (
    RuntimeListType,
    LocalRuntimeVersion,
    LocalRuntimeVersionInfo,
)
from starwhale.base.client.models.models import RuntimeVo, RuntimeInfoVo


class RuntimeTermView(BaseTermView, TagViewMixin):
    def __init__(self, runtime_uri: str | Resource) -> None:
        super().__init__()

        if isinstance(runtime_uri, Resource):
            self.uri = runtime_uri
        else:
            self.uri = Resource(runtime_uri, typ=ResourceType.runtime)

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
        fullname = fullname or self.uri.instance.is_cloud
        return self._print_history(
            title="Runtime History", history=self.runtime.history(), fullname=fullname
        )

    def info(
        self,
        output_filter: RuntimeInfoFilter = RuntimeInfoFilter.basic,
    ) -> None:
        info = self.runtime.info()
        if info is None:
            console.print(
                f":anguished_face: No runtime info found: {self.uri}", style="red"
            )
            return

        if isinstance(info, RuntimeInfoVo):
            console.print(info)
            return

        basic_content = Pretty(info.basic, expand_all=True)
        runtime_content = Syntax(info.yaml, "yaml", theme="ansi_dark")
        manifest_content = Pretty(info.manifest, expand_all=True)
        _locks = []
        for fname, content in info.lock.items():
            _locks.append(f"#lock file: {fname}")
            _locks.append(content)
        lock_content = "\n".join(_locks)

        if output_filter == RuntimeInfoFilter.basic:
            console.print(basic_content)
        elif output_filter == RuntimeInfoFilter.runtime_yaml:
            console.print(runtime_content)
        elif output_filter == RuntimeInfoFilter.lock:
            console.print(lock_content)
        elif output_filter == RuntimeInfoFilter.manifest:
            console.print(manifest_content)
        else:
            console.rule("[green bold] Runtime Basic Info")
            console.print(basic_content)
            console.rule("[green bold] runtime.yaml")
            console.print(runtime_content)
            console.rule("[green bold] _manifest.yaml")
            console.print(manifest_content)
            console.rule("[green bold] Runtime Lock Files")
            console.print(lock_content)

    @classmethod
    @BaseTermView._only_standalone
    def activate(cls, uri: Resource, force_restore: bool = False) -> None:
        Runtime.activate(uri, force_restore)

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
        target_dir: str | Path,
        yaml_path: str | Path,
        env_name: str = "",
        env_prefix_path: str = "",
        no_cache: bool = False,
        stdout: bool = False,
        include_editable: bool = False,
        include_local_wheel: bool = False,
        dump_pip_options: bool = False,
        env_use_shell: bool = False,
    ) -> None:
        Runtime.lock(
            target_dir=target_dir,
            yaml_path=yaml_path,
            env_name=env_name,
            env_prefix_path=env_prefix_path,
            stdout=stdout,
            no_cache=no_cache,
            include_editable=include_editable,
            include_local_wheel=include_local_wheel,
            dump_pip_options=dump_pip_options,
            env_use_shell=env_use_shell,
        )

    @classmethod
    @BaseTermView._only_standalone
    def build_from_docker_image(
        cls, image: str, runtime_name: str = "", project: str = ""
    ) -> None:
        runtime_name = runtime_name or image.split(":")[0].split("/")[-1]
        runtime_uri = cls.prepare_build_bundle(
            project=project, bundle_name=runtime_name, typ=ResourceType.runtime
        )

        rt = Runtime.get_runtime(runtime_uri)
        rt.build_from_docker_image(image=image, runtime_name=runtime_name)

    @classmethod
    @BaseTermView._only_standalone
    def build_from_python_env(
        cls,
        runtime_name: str = "",
        conda_name: str = "",
        conda_prefix: str = "",
        venv_prefix: str = "",
        project: str = "",
        cuda: str = "",
        cudnn: str = "",
        arch: str = "",
        download_all_deps: bool = False,
        include_editable: bool = False,
        include_local_wheel: bool = False,
        dump_condarc: bool = False,
        dump_pip_options: bool = False,
        tags: t.List[str] | None = None,
    ) -> Resource:
        set_args = list(filter(bool, (conda_name, conda_prefix, venv_prefix)))
        if len(set_args) >= 2:
            raise ExclusiveArgsError(
                f"conda_prefix({conda_prefix}), conda_name({conda_name}), venv_prefix({venv_prefix}) are the mutex args."
            )

        if conda_name:
            mode = PythonRunEnv.CONDA
            candidate_runtime_name = conda_name
        elif conda_prefix:
            mode = PythonRunEnv.CONDA
            candidate_runtime_name = conda_prefix
        elif venv_prefix:
            mode = PythonRunEnv.VENV
            candidate_runtime_name = venv_prefix
        else:
            mode = get_python_run_env()
            candidate_runtime_name = (
                get_conda_env() if mode == PythonRunEnv.CONDA else get_venv_env()
            ) or "default"

        if runtime_name == "":
            runtime_name = candidate_runtime_name.strip("/").split("/")[-1]

        runtime_uri = cls.prepare_build_bundle(
            project=project, bundle_name=runtime_name, typ=ResourceType.runtime
        )
        rt = Runtime.get_runtime(runtime_uri)
        rt.build_from_python_env(
            runtime_name=runtime_name,
            mode=mode,
            conda_name=conda_name,
            conda_prefix=conda_prefix,
            venv_prefix=venv_prefix,
            cuda=cuda,
            cudnn=cudnn,
            arch=arch,
            download_all_deps=download_all_deps,
            include_editable=include_editable,
            include_local_wheel=include_local_wheel,
            dump_condarc=dump_condarc,
            dump_pip_options=dump_pip_options,
            tags=tags,
        )
        return runtime_uri

    @classmethod
    @BaseTermView._only_standalone
    def build_from_runtime_yaml(
        cls,
        workdir: str | Path,
        yaml_path: str | Path,
        runtime_name: str = "",
        project: str = "",
        download_all_deps: bool = False,
        include_editable: bool = False,
        include_local_wheel: bool = False,
        no_cache: bool = False,
        disable_env_lock: bool = False,
        dump_pip_options: bool = False,
        dump_condarc: bool = False,
        tags: t.List[str] | None = None,
    ) -> Resource:
        workdir = Path(workdir)
        yaml_path = Path(yaml_path)

        if not yaml_path.exists():
            raise NotFoundError(f"not found runtime yaml:{yaml_path}")
        _config = load_yaml(yaml_path)
        runtime_name = runtime_name or _config["name"]

        _runtime_uri = cls.prepare_build_bundle(
            project=project, bundle_name=runtime_name, typ=ResourceType.runtime
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
        _rt.build_from_runtime_yaml(
            workdir=workdir,
            yaml_path=yaml_path,
            download_all_deps=download_all_deps,
            include_editable=include_editable,
            include_local_wheel=include_local_wheel,
            no_cache=no_cache,
            disable_env_lock=disable_env_lock,
            dump_condarc=dump_condarc,
            dump_pip_options=dump_pip_options,
            tags=tags,
        )
        return _runtime_uri

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
        filters: t.Optional[t.List[str]] = None,
    ) -> t.Tuple[RuntimeListType, t.Dict[str, t.Any]]:
        _uri = Project(project_uri)
        cls.must_have_project(_uri)
        runtime = Runtime.get_cls(_uri.instance)
        _runtimes, _pager = runtime.list(
            _uri, page, size, runtime.get_list_filter(filters)
        )
        return _runtimes, _pager

    @classmethod
    @BaseTermView._only_standalone
    def quickstart_from_uri(
        cls,
        workdir: t.Union[Path, str],
        name: str,
        uri: Resource,
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
        console.print(
            f":golfer: try to restore python runtime environment: {target} ..."
        )
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
            Runtime.restore(workdir)
            activate_script_path = workdir / "activate.host"
            console.print(
                f":ramen: runtime(from workdir:{target}) has been restored, activate it in the current shell: \n"
                f"\t :stars: run command: [bold green]$(sh {str(activate_script_path)})[/]"
            )
        else:
            _uri = Resource(target, ResourceType.runtime)
            _runtime = StandaloneRuntime(_uri)
            workdir = _runtime.store.snapshot_workdir
            if not workdir.exists():
                _runtime.extract(force=True, target=workdir)
            Runtime.restore(workdir)
            console.print(
                f":ramen: runtime(from uri:{_uri}) has been restored, activate it in the current shell: \n"
                f"\t :stars: run command: [bold green]swcli runtime activate {target}[/]"
            )

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
        ignore_tags: t.List[str] | None = None,
    ) -> None:
        src = Resource(src_uri, typ=ResourceType.runtime)
        Runtime.copy(
            src_uri=src,
            dest_uri=dest_uri,
            force=force,
            dest_local_project_uri=dest_local_project_uri,
            ignore_tags=ignore_tags,
        )
        console.print(":clap: copy done.")


class RuntimeTermViewRich(RuntimeTermView):
    @classmethod
    @BaseTermView._pager
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.List[str]] = None,
    ) -> t.Tuple[RuntimeListType, t.Dict[str, t.Any]]:
        _data, _pager = super().list(
            project_uri, fullname, show_removed, page, size, filters
        )

        custom_column: t.Dict[str, t.Callable[[t.Any], str]] = {
            "tags": lambda x: ",".join(x),
            "size": lambda x: pretty_bytes(x),
        }

        cls.print_header(project_uri)

        if all(isinstance(i, LocalRuntimeVersion) for i in _data):
            allowed_col = ["name", "version", "created_at", "size", "tags", "removed"]
            cls.print_table(
                "Runtime List",
                _data,
                custom_column=custom_column,
                allowed_keys=allowed_col,
            )
        else:
            # remote runtime list
            rows: t.List[t.Dict[str, t.Any]] = []
            for i in _data:
                if not isinstance(i, RuntimeVo):
                    # can not happen
                    continue
                owner = ""
                if i.version.owner is not None:
                    owner = i.version.owner.name
                row = {
                    "name": i.name,
                    "version": i.version.name,
                    "id": f"{i.id}/version/{i.version.id}",
                    "owner": owner,
                    "tags": [i.version.alias] + (i.version.tags or []),
                    "shared": i.version.shared != 0,
                    "image": i.version.image,
                    "size": 0,
                    "created_at": CloudRequestMixed.fmt_timestamp(
                        i.version.created_time
                    ),
                }
                rows.append(row)
            allowed_col = None
            cls.print_table(
                "Runtime List",
                rows,
                custom_column=custom_column,
                allowed_keys=allowed_col,
            )

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
        filters: t.Optional[t.List[str]] = None,
    ) -> None:
        filters = filters or []
        _data, _pager = super().list(
            project_uri, fullname, show_removed, page, size, filters
        )
        cls.pretty_json(_data)

    def info(
        self,
        output_filter: RuntimeInfoFilter = RuntimeInfoFilter.basic,
    ) -> None:
        info = self.runtime.info()
        if info is None:
            console.print(
                f":anguished_face: No runtime info found: {self.uri}", style="red"
            )
            return

        out: t.Dict[str, t.Any] = dict()
        if output_filter == RuntimeInfoFilter.basic:
            from fastapi.encoders import jsonable_encoder

            out = jsonable_encoder(info.dict(by_alias=True))
        elif output_filter == RuntimeInfoFilter.lock:
            if isinstance(info, LocalRuntimeVersionInfo):
                out = {"data": info.lock}
            else:
                # TODO: support cloud runtime lock
                out = {"data": ""}
        elif output_filter == RuntimeInfoFilter.manifest:
            if isinstance(info, LocalRuntimeVersionInfo):
                out = {"data": info.manifest}
            else:
                # TODO: support cloud runtime manifest
                out = {"data": ""}
        elif output_filter == RuntimeInfoFilter.runtime_yaml:
            if isinstance(info, LocalRuntimeVersionInfo):
                out = {"data": info.yaml}
            else:
                # TODO: support cloud runtime yaml
                out = {"data": ""}

        self.pretty_json(out)

    def history(self, fullname: bool = False) -> None:
        fullname = fullname or self.uri.instance.is_cloud
        _data = BaseTermView.get_history_data(
            history=self.runtime.history(), fullname=fullname
        )
        self.pretty_json(_data)


def get_term_view(ctx_obj: t.Dict) -> t.Type[RuntimeTermView]:
    return (
        RuntimeTermViewJson if ctx_obj.get("output") == "json" else RuntimeTermViewRich
    )
