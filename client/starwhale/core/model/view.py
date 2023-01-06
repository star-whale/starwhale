import os
import sys
import typing as t
from pathlib import Path

from rich import box
from rich.text import Text
from rich.panel import Panel
from rich.table import Table
from rich.console import Group

from starwhale.utils import (
    docker,
    console,
    process,
    load_yaml,
    pretty_bytes,
    in_production,
)
from starwhale.consts import (
    FileFlag,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import cmp_file_content
from starwhale.base.type import URIType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.consts.env import SWEnv
from starwhale.utils.error import FieldTypeOrValueError
from starwhale.core.model.store import ModelStorage
from starwhale.core.runtime.model import StandaloneRuntime
from starwhale.core.runtime.process import Process as RuntimeProcess

from .model import Model, StandaloneModel


class ModelTermView(BaseTermView):
    def __init__(self, model_uri: str) -> None:
        super().__init__()

        self.raw_uri = model_uri
        self.uri = URI(model_uri, expected_type=URIType.MODEL)
        self.model = Model.get_model(self.uri)

    @BaseTermView._simple_action_print
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.model.remove(force)

    @BaseTermView._simple_action_print
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.model.recover(force)

    @BaseTermView._header
    def info(self, fullname: bool = False) -> None:
        self._print_info(self.model.info(), fullname=fullname)

    @BaseTermView._only_standalone
    def diff(self, compare_uri: URI, show_details: bool) -> None:
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
            f"compare: {compare_uri.object.version[:SHORT_VERSION_CNT]}",
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
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        return self._print_history(
            title="Model History List", history=self.model.history(), fullname=fullname
        )

    @classmethod
    @BaseTermView._only_standalone
    def eval(
        cls,
        project: str,
        target: str,
        dataset_uris: t.List[str],
        version: str = "",
        yaml_name: str = DefaultYAMLName.MODEL,
        step: str = "",
        task_index: int = 0,
        task_num: int = 0,
        runtime_uri: str = "",
        use_docker: bool = False,
        gencmd: bool = False,
        image: str = "",
    ) -> None:
        if use_docker:
            if not runtime_uri and not image:
                raise FieldTypeOrValueError(
                    "runtime_uri and image both are none when use_docker"
                )
            if runtime_uri:
                runtime = StandaloneRuntime(
                    URI(runtime_uri, expected_type=URIType.RUNTIME)
                )
                image = runtime.store.get_docker_base_image()
            mnt_paths = (
                [os.path.abspath(target)]
                if in_production() or (os.path.exists(target) and os.path.isdir(target))
                else []
            )
            env_vars = {SWEnv.runtime_version: runtime_uri} if runtime_uri else {}
            cmd = docker.gen_swcli_docker_cmd(
                image,
                env_vars=env_vars,
                mnt_paths=mnt_paths,
            )
            console.rule(":elephant: docker cmd", align="left")
            console.print(f"{cmd}\n")
            if gencmd:
                return
            process.check_call(cmd, shell=True)
            return

        kw = dict(
            project=project,
            version=version,
            workdir=cls._get_workdir(target),
            dataset_uris=dataset_uris,
            step_name=step,
            task_index=task_index,
            task_num=task_num,
            model_yaml_name=yaml_name,
        )
        if not in_production() and runtime_uri:
            RuntimeProcess.from_runtime_uri(
                uri=runtime_uri,
                target=StandaloneModel.eval_user_handler,
                kwargs=kw,
            ).run()
        else:
            StandaloneModel.eval_user_handler(**kw)  # type: ignore

    @classmethod
    def _get_workdir(cls, target: str) -> Path:
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
        else:
            _uri = URI(target, URIType.MODEL)
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
        _uri = URI(project_uri, expected_type=URIType.PROJECT)
        fullname = fullname or (_uri.instance_type == InstanceType.CLOUD)
        _models, _pager = Model.list(_uri, page, size, filters)
        _data = BaseTermView.list_data(_models, show_removed, fullname)
        return _data, _pager

    @classmethod
    @BaseTermView._only_standalone
    def build(
        cls,
        workdir: str,
        project: str,
        yaml_name: str = DefaultYAMLName.MODEL,
        runtime_uri: str = "",
    ) -> URI:
        _config = load_yaml(Path(workdir) / yaml_name)
        _model_uri = cls.prepare_build_bundle(
            project=project, bundle_name=_config.get("name"), typ=URIType.MODEL
        )
        _m = Model.get_model(_model_uri)
        kwargs = {"yaml_name": yaml_name}
        if runtime_uri:
            RuntimeProcess.from_runtime_uri(
                uri=runtime_uri,
                target=_m.build,
                args=(Path(workdir),),
                kwargs=kwargs,
            ).run()
        else:
            _m.build(Path(workdir), **kwargs)
        return _model_uri

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
    ) -> None:
        Model.copy(src_uri, dest_uri, force, dest_local_project_uri)
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
        target: str,
        model_yaml: str,
        runtime_uri: str,
        model_uri: str,
        host: str,
        port: int,
    ) -> None:
        if target and model_uri:
            console.print("workdir and model can not both set together")
            sys.exit(1)
        if not target and not model_uri:
            console.print("workdir or model needs to be set")
            sys.exit(1)

        if target:
            workdir = cls._get_workdir(target)
        else:
            _m = StandaloneModel(URI(model_uri, expected_type=URIType.MODEL))
            workdir = _m.store.src_dir

        kw = dict(
            model_yaml=model_yaml,
            workdir=workdir,
            host=host,
            port=port,
        )

        if runtime_uri:
            RuntimeProcess.from_runtime_uri(
                uri=runtime_uri,
                target=StandaloneModel.serve,
                kwargs=kw,
            ).run()
        else:
            StandaloneModel.serve(**kw)  # type: ignore


class ModelTermViewRich(ModelTermView):
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

    def info(self, fullname: bool = False) -> None:
        self.pretty_json(self.get_info_data(self.model.info(), fullname))

    def history(self, fullname: bool = False) -> None:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        self.pretty_json(BaseTermView.get_history_data(self.model.history(), fullname))


def get_term_view(ctx_obj: t.Dict) -> t.Type[ModelTermView]:
    return ModelTermViewJson if ctx_obj.get("output") == "json" else ModelTermViewRich
