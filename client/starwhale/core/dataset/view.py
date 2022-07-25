import os
import typing as t
from pathlib import Path

from starwhale.utils import console, pretty_bytes
from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.base.view import BaseTermView
from starwhale.core.dataset.store import DatasetStorage

from .model import Dataset


class DatasetTermView(BaseTermView):
    def __init__(self, dataset_uri: str) -> None:
        super().__init__()

        self.raw_uri = dataset_uri
        self.uri = URI(dataset_uri, expected_type=URIType.DATASET)
        self.dataset = Dataset.get_dataset(self.uri)

    @BaseTermView._simple_action_print
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.dataset.remove(force)

    @BaseTermView._simple_action_print
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        return self.dataset.recover(force)

    @BaseTermView._pager
    @BaseTermView._header
    def history(
        self, fullname: bool = False
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        fullname = fullname or self.uri.instance_type == InstanceType.CLOUD
        return self._print_history(
            title="Dataset History List",
            history=self.dataset.history(),
            fullname=fullname,
        )

    @BaseTermView._header
    def info(self, fullname: bool = False) -> None:
        self._print_info(self.dataset.info(), fullname=fullname)

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
        _datasets, _pager = Dataset.list(_uri, page, size)
        _data = BaseTermView.list_data(_datasets, show_removed, fullname)
        return _data, _pager

    @classmethod
    def build(
        cls, workdir: str, project: str, yaml_name: str = DefaultYAMLName.DATASET
    ) -> None:
        _dataset_uri = cls.prepare_build_bundle(
            workdir, project, yaml_name, URIType.DATASET
        )
        _ds = Dataset.get_dataset(_dataset_uri)
        _ds.build(Path(workdir), yaml_name)

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
        Dataset.copy(src_uri, dest_uri, force)
        console.print(":clap: copy done")

    @classmethod
    def render_fuse_json(cls, target: str, force: bool = False) -> None:
        if os.path.exists(target) and os.path.isdir(target):
            workdir = Path(target)
        else:
            uri = URI(target, URIType.DATASET)
            store = DatasetStorage(uri)
            workdir = store.loc

        console.print(f":crown: try to render fuse json@{workdir}...")
        Dataset.render_fuse_json(workdir, force)

    @BaseTermView._header
    def tag(self, tags: t.List[str], remove: bool = False, quiet: bool = False) -> None:
        if remove:
            console.print(f":golfer: remove tags {tags} @ {self.uri}...")
            self.dataset.remove_tags(tags, quiet)
        else:
            console.print(f":surfer: add tags {tags} @ {self.uri}...")
            self.dataset.add_tags(tags, quiet)


class DatasetTermViewRich(DatasetTermView):
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
        _datasets, _pager = super().list(
            project_uri, fullname, show_removed, page, size
        )
        custom_column: t.Dict[str, t.Callable[[t.Any], str]] = {
            "tags": lambda x: ",".join(x),
            "size": lambda x: pretty_bytes(x),
            "runtime": cls.place_holder_for_empty(),
        }

        cls.print_table("Dataset List", _datasets, custom_column=custom_column)
        return _datasets, _pager


class DatasetTermViewJson(DatasetTermView):
    @classmethod
    def list(  # type: ignore
        cls,
        project_uri: str = "",
        fullname: bool = False,
        show_removed: bool = False,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> None:
        _datasets, _pager = super().list(
            project_uri, fullname, show_removed, page, size
        )
        cls.pretty_json(_datasets)

    def info(self, fullname: bool = False) -> None:
        self.pretty_json(self.get_info_data(self.dataset.info(), fullname))


def get_term_view(ctx_obj: t.Dict) -> t.Type[DatasetTermView]:
    return (
        DatasetTermViewJson if ctx_obj.get("output") == "json" else DatasetTermViewRich
    )
