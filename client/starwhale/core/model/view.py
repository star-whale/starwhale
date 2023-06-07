from __future__ import annotations

import os
import typing as t
from pathlib import Path

from rich import box
from rich.text import Text
from rich.tree import Tree
from rich.panel import Panel
from rich.table import Table
from rich.pretty import Pretty
from rich.syntax import Syntax
from rich.console import Group

from starwhale.utils import docker, console, process, pretty_bytes, in_production
from starwhale.consts import (
    FileFlag,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
)
from starwhale.utils.fs import cmp_file_content
from starwhale.base.view import BaseTermView
from starwhale.consts.env import SWEnv
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.base.uri.project import Project
from starwhale.core.model.store import ModelStorage
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.runtime.model import StandaloneRuntime
from starwhale.core.runtime.process import Process as RuntimeProcess

from .model import Model, CloudModel, ModelConfig, ModelInfoFilter, StandaloneModel


class ModelTermView(BaseTermView):
    def __init__(self, model_uri: str | Resource) -> None:
        super().__init__()

        if isinstance(model_uri, Resource):
            self.uri = model_uri
        else:
            self.uri = Resource(model_uri, ResourceType.model, refine=True)
        self.model = Model.get_model(self.uri)

    @BaseTermView._simple_action_print
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.model.remove(force)

    @BaseTermView._simple_action_print
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.model.recover(force)

    def info(self, output_filter: ModelInfoFilter = ModelInfoFilter.basic) -> None:
        info = self.model.info()
        if not info:
            console.print(f":bird: model info not found: [bold red]{self.uri}[/]")
            return

        def _render_handlers() -> Table:
            table = Table(
                title="Model Package Runnable Handlers",
                box=box.MARKDOWN,
                show_lines=True,
                expand=True,
            )
            table.add_column("Handler Index", style="cyan")
            table.add_column("Handler Name", style="cyan")
            table.add_column("Steps", style="green")

            handlers = sorted(info.get("handlers", {}).items())
            for index, (name, steps) in enumerate(handlers):
                steps_content = []
                for step in steps:
                    steps_content.append(f":palm_tree: {step['name']}")
                    steps_content.append(f"\t - replicas: {step['replicas']}")
                    steps_content.append(f"\t - needs: {' '.join(step['needs'])}")
                table.add_row(str(index), name, "\n".join(steps_content))
            return table

        def _render_files_tree() -> Tree:
            files = info.get("files", [])
            unfold_files: t.Dict[str, t.Any] = {}
            for f in files:
                path = Path(f["path"])
                parent = unfold_files
                for p in path.parent.parts:
                    parent.setdefault(p, {})
                    parent = parent[p]

                parent[path.name] = f"{pretty_bytes(f['size'])}"

            root = Tree("Model Resource Files Tree", style="bold bright_blue")

            def _walk(tree: Tree, files: t.Dict) -> None:
                for k, v in files.items():
                    if isinstance(v, dict):
                        subtree = tree.add(f":file_folder: {k}")
                        _walk(subtree, v)
                    else:
                        tree.add(f":page_facing_up: {k} ({v})")

            _walk(root, unfold_files)
            return root

        basic_content = Pretty(info.get("basic", {}), expand_all=True)
        model_yaml_content = Syntax(
            info.get("model_yaml", ""), "yaml", theme="ansi_dark"
        )
        handlers_content = _render_handlers()
        files_content = _render_files_tree()

        # TODO: support files tree
        if output_filter == ModelInfoFilter.basic:
            console.print(basic_content)
        elif output_filter == ModelInfoFilter.model_yaml:
            console.print(model_yaml_content)
        elif output_filter == ModelInfoFilter.handlers:
            console.print(handlers_content)
        elif output_filter == ModelInfoFilter.files:
            console.print(files_content)
        else:
            console.rule("[green bold] Model Basic Info")
            console.print(basic_content)
            console.rule("[green bold] model.yaml")
            console.print(model_yaml_content)
            console.rule("[green bold] Model Handlers")
            console.print(handlers_content)

    @BaseTermView._only_standalone
    def extract(self, target: Path, force: bool = False) -> None:
        self.model.extract(target=target, force=force)

    @BaseTermView._only_standalone
    def diff(self, compare_uri: Resource, show_details: bool) -> None:
        r = self.model.diff(compare_uri)
        text_details: t.List[Panel] = []
        table = Table(box=box.SIMPLE, expand=False, show_lines=True)
        table.add_column("file")
        table.add_column("path")
        table.add_column(
            f"base: {self.model.version[:SHORT_VERSION_CNT]}",
            style="magenta",
            justify="left",
        )
        table.add_column(
            f"compare: {compare_uri.version[:SHORT_VERSION_CNT]}",
            style="cyan",
            justify="left",
        )
        b_files = r["base_version"]
        c_files = r["compare_version"]
        for _p in r["all_paths"]:
            _flag = c_files[_p].flag
            if _flag == FileFlag.ADDED:
                _flag_txt = f"[blue]{_flag}"
            elif _flag == FileFlag.DELETED:
                _flag_txt = f"[red]{_flag}"
            elif _flag == FileFlag.UPDATED:
                _flag_txt = f"[yellow]{_flag}"
                if show_details:
                    text_details.append(
                        Panel(
                            Text(
                                text="".join(
                                    cmp_file_content(
                                        base_path=b_files[_p].path,
                                        cmp_path=c_files[_p].path,
                                    )
                                )
                            ),
                            title=_p,
                        )
                    )
            else:
                _flag_txt = f"[green]{_flag}"
            table.add_row(
                os.path.basename(_p),
                _p,
                "existed" if _p in b_files else "-",
                f"[green]{_flag_txt}",
            )

        console.print(Panel(table, title="diff overview"))
        if show_details:
            console.print(Panel(Group(*text_details), title="diff details"))

    @BaseTermView._pager
    @BaseTermView._header
    def history(self, fullname: bool = False) -> t.List[t.Dict[str, t.Any]]:
        fullname = fullname or self.uri.instance.is_cloud
        return self._print_history(
            title="Model History List", history=self.model.history(), fullname=fullname
        )

    @classmethod
    def run_in_server(
        cls,
        project_uri: Project,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        resource_pool: str,
        run_handler: str | int,
    ) -> t.Tuple[bool, str]:
        ok, version_or_reason = CloudModel.run(
            project_uri=project_uri,
            model_uri=model_uri,
            dataset_uris=dataset_uris,
            runtime_uri=runtime_uri,
            resource_pool=resource_pool,
            run_handler=run_handler,
        )
        if ok:
            console.print(":clap: success to create job")
            console.print(
                f":bird: visit web: {project_uri.instance.url}/projects/{project_uri.name}/evaluations/{version_or_reason}"
            )
            console.print(
                f":monkey: run command: [bold green]swcli job info {project_uri.full_uri}/job/{version_or_reason} [/]"
            )
        else:
            console.print(f":bird: run failed: [bold red]{version_or_reason}[/]")

        return ok, version_or_reason

    @classmethod
    @BaseTermView._only_standalone
    def run_in_host(
        cls,
        model_src_dir: Path | str,
        model_config: ModelConfig,
        project: str = "",
        version: str = "",
        run_handler: str = "",
        dataset_uris: t.Optional[t.List[str]] = None,
        runtime_uri: t.Optional[Resource] = None,
        scheduler_run_args: t.Optional[t.Dict] = None,
        forbid_snapshot: bool = False,
        cleanup_snapshot: bool = True,
        force_generate_jobs_yaml: bool = False,
    ) -> None:
        if runtime_uri:
            RuntimeProcess(uri=runtime_uri).run()
        else:
            StandaloneModel.run(
                model_src_dir=Path(model_src_dir),
                model_config=model_config,
                project=Project(project).name,
                version=version,
                run_handler=run_handler,
                dataset_uris=dataset_uris,
                scheduler_run_args=scheduler_run_args,
                forbid_snapshot=forbid_snapshot,
                cleanup_snapshot=cleanup_snapshot,
                force_generate_jobs_yaml=force_generate_jobs_yaml,
            )

    @classmethod
    @BaseTermView._only_standalone
    def run_in_container(
        cls,
        model_src_dir: Path,
        runtime_uri: t.Optional[Resource] = None,
        docker_image: str = "",
    ) -> None:
        # TODO: support to get job version for in container
        if not runtime_uri and not docker_image:
            raise FieldTypeOrValueError("runtime_uri and docker_image both are none")

        if runtime_uri:
            if runtime_uri.typ == ResourceType.runtime:
                runtime = StandaloneRuntime(runtime_uri)
                docker_image = runtime.store.get_docker_run_image()
            elif runtime_uri.typ == ResourceType.model:
                model = StandaloneModel(runtime_uri)
                docker_image = model.store.get_packaged_runtime_docker_run_image()
            else:
                raise NoSupportError(f"not support {runtime_uri} to fetch docker image")

        mounts = [str(model_src_dir.resolve().absolute())]
        envs = {"SW_INSTANCE_URI": "local"}
        if runtime_uri:
            envs[SWEnv.runtime_version] = str(runtime_uri)
        cmd = docker.gen_swcli_docker_cmd(
            docker_image,
            envs=envs,
            mounts=mounts,
        )
        console.rule(":elephant: docker cmd", align="left")
        console.print(f"{cmd}\n")
        process.check_call(cmd, shell=True)

    @classmethod
    def _get_workdir(cls, target: str) -> Path:
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
        else:
            _uri = Resource(target, ResourceType.model)
            _store = ModelStorage(_uri)
            workdir = _store.src_dir
        return workdir

    @classmethod
    def list(
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.Union[t.Dict[str, t.Any], t.List[str]]] = None,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        filters = filters or {}
        _uri = Project(project_uri)
        cls.must_have_project(_uri)
        fullname = fullname or _uri.instance.is_cloud
        _models, _pager = Model.list(_uri, page, size, filters)
        _data = BaseTermView.list_data(_models, show_removed, fullname)
        return _data, _pager

    @classmethod
    @BaseTermView._only_standalone
    def build(
        cls,
        workdir: t.Union[str, Path],
        project: str,
        model_config: ModelConfig,
        add_all: bool,
        runtime_uri: str = "",
        package_runtime: bool = False,
    ) -> None:
        if runtime_uri:
            RuntimeProcess(uri=Resource(runtime_uri, typ=ResourceType.runtime)).run()
        else:
            model_uri = cls.prepare_build_bundle(
                project=project,
                bundle_name=model_config.name,
                typ=ResourceType.model,
            )
            m = Model.get_model(model_uri)

            if package_runtime:
                packaging_runtime_uri = os.environ.get(
                    RuntimeProcess.ActivatedRuntimeURI
                )
            else:
                packaging_runtime_uri = None

            m.build(
                Path(workdir),
                model_config=model_config,
                packaging_runtime_uri=packaging_runtime_uri,
                add_all=add_all,
            )

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
    ) -> None:
        src = Resource(src_uri, typ=ResourceType.model)
        Model.copy(src, dest_uri, force, dest_local_project_uri)
        console.print(":clap: copy done.")

    @BaseTermView._header
    def tag(
        self, tags: t.List[str], remove: bool = False, ignore_errors: bool = False
    ) -> None:
        if remove:
            console.print(f":golfer: remove tags [red]{tags}[/] @ {self.uri}...")
            self.model.remove_tags(tags, ignore_errors)
        else:
            console.print(f":surfer: add tags [red]{tags}[/] @ {self.uri}...")
            self.model.add_tags(tags, ignore_errors)

    @classmethod
    @BaseTermView._only_standalone
    def serve(
        cls,
        model_src_dir: Path,
        model_config: ModelConfig,
        runtime_uri: Resource,
        host: str,
        port: int,
    ) -> None:
        if runtime_uri:
            RuntimeProcess(uri=runtime_uri).run()
        else:
            StandaloneModel.serve(
                model_config=model_config,
                model_src_dir=model_src_dir,
                host=host,
                port=port,
            )


class ModelTermViewRich(ModelTermView):
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
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        filters = filters or []
        _models, _pager = super().list(
            project_uri, fullname, show_removed, page, size, filters
        )
        custom_column: t.Dict[str, t.Callable[[t.Any], str]] = {
            "tags": lambda x: ",".join(x),
            "size": lambda x: pretty_bytes(x),
            "runtime": cls.place_holder_for_empty(""),
        }

        cls.print_header()
        cls.print_table("Model List", _models, custom_column=custom_column)
        return _models, _pager


class ModelTermViewJson(ModelTermView):
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
        _models, _pager = super().list(
            project_uri, fullname, show_removed, page, size, filters
        )
        cls.pretty_json(_models)

    def info(self, output_filter: ModelInfoFilter = ModelInfoFilter.basic) -> None:
        info = self.model.info()
        if output_filter == ModelInfoFilter.basic:
            info = {"basic": info.get("basic", {})}
        elif output_filter == ModelInfoFilter.model_yaml:
            info = {"model_yaml": info.get("model_yaml", "")}
        elif output_filter == ModelInfoFilter.handlers:
            info = {"handlers": info.get("handlers", {})}
        elif output_filter == ModelInfoFilter.files:
            info = {"files": info.get("files", [])}

        self.pretty_json(info)

    def history(self, fullname: bool = False) -> None:
        fullname = fullname or self.uri.instance.is_cloud
        self.pretty_json(BaseTermView.get_history_data(self.model.history(), fullname))


def get_term_view(ctx_obj: t.Dict) -> t.Type[ModelTermView]:
    return ModelTermViewJson if ctx_obj.get("output") == "json" else ModelTermViewRich
