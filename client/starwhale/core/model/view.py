import os
import typing as t
from pathlib import Path

from starwhale.utils import console, load_yaml, pretty_bytes, in_production
from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.core.model.store import ModelStorage
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

    @BaseTermView._pager
    @BaseTermView._header
    def history(self, fullname: bool = False) -> t.List[t.Dict[str, t.Any]]:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        return self._print_history(
            title="Model History List", history=self.model.history(), fullname=fullname
        )

    @BaseTermView._only_standalone
    def extract(self, force: bool = False, target_dir: str = "") -> None:
        console.print(":oncoming_police_car: try to extract ...")
        path = self.model.extract(force, target_dir)
        console.print(f":clap: extracted @ {path.resolve()} :tada:")

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
    ) -> None:
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
        else:
            _uri = URI(target, URIType.MODEL)
            _store = ModelStorage(_uri)
            workdir = _store.loc

        kw = dict(
            project=project,
            version=version,
            workdir=workdir,
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
    ) -> None:
        _config = load_yaml(Path(workdir) / yaml_name)
        _model_uri = cls.prepare_build_bundle(
            project=project, bundle_name=_config.get("name"), typ=URIType.MODEL
        )
        _m = Model.get_model(_model_uri)
        if runtime_uri:
            RuntimeProcess.from_runtime_uri(
                uri=runtime_uri,
                target=_m.build,
                args=(Path(workdir), yaml_name),
            ).run()
        else:
            _m.build(Path(workdir), yaml_name)

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
    def tag(self, tags: t.List[str], remove: bool = False, quiet: bool = False) -> None:
        if remove:
            console.print(f":golfer: remove tags [red]{tags}[/] @ {self.uri}...")
            self.model.remove_tags(tags, quiet)
        else:
            console.print(f":surfer: add tags [red]{tags}[/] @ {self.uri}...")
            self.model.add_tags(tags, quiet)


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
