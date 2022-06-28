import os
import typing as t
from pathlib import Path

from starwhale.utils import console, pretty_bytes, in_production
from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.base.type import URIType, EvalTaskType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.core.model.store import ModelStorage

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
    def history(
        self, fullname: bool = False
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        return self._print_history(
            title="Model History List", history=self.model.history(), fullname=fullname
        )

    def extract(self, force: bool = False, target_dir: str = "") -> None:
        console.print(":oncoming_police_car: try to extract ...")
        path = self.model.extract(force, target_dir)
        console.print(f":clap: extracted @ {path.resolve()} :tada:")

    @classmethod
    def eval(
        cls,
        target: str,
        yaml_name: str = DefaultYAMLName.MODEL,
        typ: str = "",
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        if in_production() or (os.path.exists(target) and os.path.isdir(target)):
            workdir = Path(target)
        else:
            uri = URI(target, URIType.MODEL)
            store = ModelStorage(uri)
            workdir = store.loc

        if typ in (EvalTaskType.CMP, EvalTaskType.PPL):
            console.print(f":golfer: try to eval {typ} @ {workdir}...")
            StandaloneModel.eval_user_handler(
                typ,
                workdir,
                yaml_name=yaml_name,
                kw=kw,
            )
        else:
            pass

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
    def build(
        cls, workdir: str, project: str, yaml_name: str = DefaultYAMLName.MODEL
    ) -> None:
        _model_uri = cls.prepare_build_bundle(
            workdir, project, yaml_name, URIType.MODEL
        )
        _m = Model.get_model(_model_uri)
        _m.build(Path(workdir), yaml_name)

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
        Model.copy(src_uri, dest_uri, force)
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
    def list(
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


def get_term_view(ctx_obj: t.Dict) -> t.Type[ModelTermView]:
    return ModelTermViewJson if ctx_obj.get("output") == "json" else ModelTermViewRich
