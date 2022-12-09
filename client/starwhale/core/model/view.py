import os
import typing as t
from pathlib import Path

from starwhale.utils import console, load_yaml, pretty_bytes, in_production, docker
from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.core.model.store import ModelStorage
from starwhale.core.runtime.process import Process as RuntimeProcess

from .model import Model, StandaloneModel
from ..runtime.model import StandaloneRuntime
from ...consts.env import SWEnv
from ...utils.error import FieldTypeOrValueError
from ...utils.process import check_call


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
                raise FieldTypeOrValueError("runtime_uri and image both are none when use_docker")
            if runtime_uri:
                runtime: t.Optional[StandaloneRuntime] = StandaloneRuntime(
                    URI(runtime_uri, expected_type=URIType.RUNTIME)
                )
                image = runtime.store.get_docker_base_image()
            mnt_paths = [os.path.abspath(target)] if in_production() or (os.path.exists(target) and os.path.isdir(target)) else []
            cmd = docker.gen_docker_cmd(image, env_vars={SWEnv.runtime_version: runtime_uri},
                                        mnt_paths=mnt_paths,
                                        name=f"sw-{version}-{step}-{task_index}")
            console.rule(f":elephant: docker cmd", align="left")
            console.print(f"{cmd}\n")
            if gencmd:
                return
            check_call(f"docker pull {image}", shell=True)
            check_call(cmd, shell=True)
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
            workdir = _store.loc
        return workdir

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
        _models, _pager = Model.list(_uri, page, size)
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
        host: str,
        port: int,
        handlers: t.Optional[t.List[str]] = None,
    ) -> None:
        workdir = cls._get_workdir(target)
        StandaloneModel.serve(model_yaml, workdir, host, port, handlers)


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
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _models, _pager = super().list(project_uri, fullname, show_removed, page, size)
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
    ) -> None:
        _models, _pager = super().list(project_uri, fullname, show_removed, page, size)
        cls.pretty_json(_models)

    def info(self, fullname: bool = False) -> None:
        self.pretty_json(self.get_info_data(self.model.info(), fullname))

    def history(self, fullname: bool = False) -> None:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        self.pretty_json(BaseTermView.get_history_data(self.model.history(), fullname))


def get_term_view(ctx_obj: t.Dict) -> t.Type[ModelTermView]:
    return ModelTermViewJson if ctx_obj.get("output") == "json" else ModelTermViewRich
