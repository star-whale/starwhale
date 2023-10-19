from __future__ import annotations

import typing as t
from abc import ABCMeta, abstractmethod
from http import HTTPStatus
from pathlib import Path

import yaml

from starwhale.utils import console, load_yaml, convert_to_bytes, validate_obj_name
from starwhale.consts import (
    HTTPMethod,
    CREATED_AT_KEY,
    DefaultYAMLName,
    RECOVER_DIRNAME,
    D_ALIGNMENT_SIZE,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
    D_FILE_VOLUME_SIZE,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_STARWHALE_API_VERSION,
)
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import move_dir, empty_dir
from starwhale.base.type import (
    PathLike,
    BundleType,
    InstanceType,
    DatasetChangeMode,
    DatasetFolderSourceType,
)
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.base.mixin import ASDictMixin
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.base.models.base import ListFilter
from starwhale.base.uri.project import Project
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.copy import DatasetCopy
from starwhale.core.dataset.store import DatasetStorage
from starwhale.base.models.dataset import (
    DatasetListType,
    LocalDatasetInfo,
    LocalDatasetInfoBase,
)
from starwhale.base.client.api.dataset import DatasetApi
from starwhale.api._impl.dataset.loader import DataRow
from starwhale.base.client.models.models import DatasetInfoVo

if t.TYPE_CHECKING:
    from starwhale.api._impl.dataset.model import Dataset as SDKDataset


class DatasetSummary(ASDictMixin):
    def __init__(
        self,
        rows: int = 0,
        updated_rows: int = 0,
        deleted_rows: int = 0,
        blobs_byte_size: int = 0,
        increased_blobs_byte_size: int = 0,
        **kw: t.Any,
    ) -> None:
        self.rows = rows
        self.updated_rows = updated_rows
        self.deleted_rows = deleted_rows
        self.blobs_byte_size = blobs_byte_size
        self.increased_blobs_byte_size = increased_blobs_byte_size

    def __str__(self) -> str:
        return f"Dataset Summary: rows({self.rows})"

    def __repr__(self) -> str:
        return (
            f"Dataset Summary: rows(total: {self.rows}, updated: {self.updated_rows}, deleted: {self.deleted_rows}), "
            f"size(blobs:{self.blobs_byte_size})"
        )


_size_t = t.Union[int, str, None]


# TODO: use attr to tune code
class DatasetAttr(ASDictMixin):
    def __init__(
        self,
        volume_size: _size_t = D_FILE_VOLUME_SIZE,
        alignment_size: _size_t = D_ALIGNMENT_SIZE,
        **kw: t.Any,
    ) -> None:
        volume_size = D_FILE_VOLUME_SIZE if volume_size is None else volume_size
        alignment_size = D_ALIGNMENT_SIZE if alignment_size is None else alignment_size

        self.volume_size = convert_to_bytes(volume_size)
        self.alignment_size = convert_to_bytes(alignment_size)
        self.kw = kw

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        return super().asdict(ignore_keys=ignore_keys or ["kw"])


# TODO: abstract base class from DatasetConfig and ModelConfig
# TODO: use attr to tune code
class DatasetConfig(ASDictMixin):
    def __init__(
        self,
        name: str = "",
        handler: t.Any = "",
        pkg_data: t.List[str] | None = None,
        exclude_pkg_data: t.List[str] | None = None,
        desc: str = "",
        version: str = DEFAULT_STARWHALE_API_VERSION,
        attr: t.Dict[str, t.Any] | None = None,
        project_uri: str = "",
        runtime_uri: str = "",
        **kw: t.Any,
    ) -> None:
        self.name = name
        self.handler = handler
        self.desc = desc
        self.version = version
        self.attr = DatasetAttr(**(attr or {}))
        self.pkg_data = pkg_data or []
        self.exclude_pkg_data = exclude_pkg_data or []
        self.project_uri = project_uri
        self.runtime_uri = runtime_uri

        self.kw = kw

    def do_validate(self) -> None:
        _ok, _reason = validate_obj_name(self.name)
        if not _ok:
            raise FieldTypeOrValueError(f"name field:({self.name}) error: {_reason}")

        if isinstance(self.handler, str) and ":" not in self.handler:
            raise Exception(
                f"please use module:class format, current is: {self.handler}"
            )

        # TODO: add more validator

    def __str__(self) -> str:
        return f"Dataset Config {self.name}"

    __repr__ = __str__

    @classmethod
    def create_by_yaml(cls, fpath: t.Union[str, Path]) -> DatasetConfig:
        c = load_yaml(fpath)

        return cls(**c)

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        d = super().asdict(["handler"])
        d["handler"] = getattr(self.handler, "__name__", None) or str(self.handler)
        return d


class Dataset(BaseBundle, metaclass=ABCMeta):
    def __str__(self) -> str:
        return f"Starwhale Dataset: {self.uri}"

    @classmethod
    @abstractmethod
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[ListFilter] = None,
    ) -> t.Tuple[DatasetListType, t.Dict[str, t.Any]]:
        raise NotImplementedError

    @abstractmethod
    def info(self) -> LocalDatasetInfo | DatasetInfoVo | None:
        raise NotImplementedError

    @abstractmethod
    def summary(self) -> t.Optional[DatasetSummary]:
        raise NotImplementedError

    def head(self, limit: int) -> t.List[DataRow]:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        return SDKDataset.dataset(self.uri, readonly=True).head(limit)

    def diff(self, compare_uri: Resource) -> t.Dict[str, t.Any]:
        # TODO: standalone or cloud dataset diff by datastore diff feature
        raise NotImplementedError

    def build_from_folder(
        self, folder: Path, kind: DatasetFolderSourceType, **kwargs: t.Any
    ) -> None:
        raise NotImplementedError

    def build_from_json_files(self, paths: t.List[PathLike], **kwargs: t.Any) -> None:
        raise NotImplementedError

    def build_from_csv_files(self, paths: t.List[PathLike], **kwargs: t.Any) -> None:
        raise NotImplementedError

    def build_from_huggingface(self, repo: str, **kwargs: t.Any) -> None:
        raise NotImplementedError

    @classmethod
    def get_dataset(cls, uri: Resource) -> Dataset:
        _cls = cls._get_cls(uri.instance)
        return _cls(uri)

    @classmethod
    def copy(
        cls,
        src_uri: Resource,
        dest_uri: str,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
        dest_local_project_uri: str = "",
        force: bool = False,
        ignore_tags: t.List[str] | None = None,
    ) -> None:
        dc = DatasetCopy(
            src_uri=src_uri,
            dest_uri=dest_uri,
            force=force,
            mode=mode,
            dest_local_project_uri=dest_local_project_uri,
            ignore_tags=ignore_tags,
        )
        dc.do()

    @classmethod
    def get_cls(
        cls, uri: Instance
    ) -> t.Union[t.Type[StandaloneDataset], t.Type[CloudDataset]]:
        return cls._get_cls(uri)

    @classmethod
    def _get_cls(  # type: ignore
        cls, uri: Instance
    ) -> t.Union[t.Type[StandaloneDataset], t.Type[CloudDataset]]:
        if uri.is_local:
            return StandaloneDataset
        elif uri.is_cloud:
            return CloudDataset
        else:
            raise NoSupportError(f"dataset uri:{uri}")


class StandaloneDataset(Dataset, LocalStorageBundleMixin):
    def __init__(self, uri: Resource) -> None:
        super().__init__(uri)
        self.store = DatasetStorage(uri)
        self.typ = InstanceType.STANDALONE
        self.tag = StandaloneTag(uri)
        self._manifest: t.Dict[
            str, t.Any
        ] = {}  # TODO: use manifest class get_conda_env
        self.yaml_name = DefaultYAMLName.DATASET
        self._version = uri.version

    def list_tags(self) -> t.List[str]:
        return self.tag.list()

    def add_tags(
        self, tags: t.List[str], ignore_errors: bool = False, force: bool = False
    ) -> None:
        self.tag.add(tags, ignore_errors, force=force)

    def remove_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.remove(tags, ignore_errors)

    def history(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.List[t.Dict[str, t.Any]]:
        _r = []

        for _bf in self.store.iter_bundle_history():
            _manifest_path = _bf.path / DEFAULT_MANIFEST_NAME
            if not _manifest_path.exists():
                continue

            _manifest = load_yaml(_bf.path / DEFAULT_MANIFEST_NAME)
            _r.append(
                dict(
                    name=self.name,
                    version=_bf.version,
                    size=_manifest.get("blobs_byte_size", 0),
                    created_at=_manifest[CREATED_AT_KEY],
                    tags=_bf.tags,
                    path=_bf.path,
                )
            )

        return _r

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: remove by tag
        if force:
            empty_dir(self.store.snapshot_workdir)
            return True, ""
        else:
            return move_dir(self.store.snapshot_workdir, self.store.recover_loc, False)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        dest_path = self.store.bundle_dir / f"{self.uri.version}{BundleType.DATASET}"
        return move_dir(self.store.recover_loc, dest_path, force)

    def info(self) -> LocalDatasetInfo | DatasetInfoVo | None:
        if not self.store.bundle_path.exists():
            return None
        else:
            return LocalDatasetInfo(
                name=self.name,
                uri=str(self.uri),
                project=self.uri.project.id,
                path=str(self.store.bundle_path),
                version=self.uri.version,
                tags=StandaloneTag(self.uri).list(),
                manifest=self.store.manifest,
                created_at=self.store.manifest.get(CREATED_AT_KEY, ""),
                is_removed=RECOVER_DIRNAME in str(self.store.bundle_path),
                size=self.store.manifest.get("dataset_summary", {}).get(
                    "blobs_byte_size", 0
                ),
            )

    def summary(self) -> t.Optional[DatasetSummary]:
        _manifest = self.store.manifest
        _summary = _manifest.get("dataset_summary", {})
        return DatasetSummary(**_summary) if _summary else None

    @classmethod
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[ListFilter] = None,
    ) -> t.Tuple[DatasetListType, t.Dict[str, t.Any]]:
        rs: t.List[LocalDatasetInfoBase] = []

        for _bf in DatasetStorage.iter_all_bundles(
            project_uri,
            bundle_type=BundleType.DATASET,
            uri_type=ResourceType.dataset,
        ):
            if not cls.do_bundle_filter(_bf, filters):
                continue

            _mf = _bf.path / DEFAULT_MANIFEST_NAME
            if not _mf.exists():
                continue

            _manifest = load_yaml(_bf.path / DEFAULT_MANIFEST_NAME)

            rs.append(
                LocalDatasetInfoBase(
                    name=_bf.name,
                    version=_bf.version,
                    size=_manifest.get("dataset_summary", {}).get("blobs_byte_size", 0),
                    created_at=_manifest[CREATED_AT_KEY],
                    rows=_manifest.get("dataset_summary", {}).get("rows", 0),
                    is_removed=_bf.is_removed,
                    path=str(_bf.path),
                    tags=_bf.tags,
                    project=project_uri.id,
                )
            )

        return rs, {}

    def build_from_csv_files(self, paths: t.List[PathLike], **kwargs: t.Any) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        ds = SDKDataset.from_csv(
            path=paths,
            name=self.name,
            **kwargs,
        )
        console.print(
            f":hibiscus: congratulation! dataset build from csv files({paths}) has been built. You can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{ds.committed_version[:SHORT_VERSION_CNT]}[/]"
        )

    def build_from_huggingface(self, repo: str, **kwargs: t.Any) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        ds = SDKDataset.from_huggingface(name=self.name, repo=repo, **kwargs)
        console.print(
            f":hibiscus: congratulation! dataset build from https://huggingface.co/datasets/{repo} has been built. You can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{ds.committed_version[:SHORT_VERSION_CNT]}[/]"
        )

    def build_from_json_files(self, paths: t.List[PathLike], **kwargs: t.Any) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        ds = SDKDataset.from_json(name=self.name, path=paths, **kwargs)
        console.print(
            f":hibiscus: congratulation! dataset build from {paths} has been built. You can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{ds.committed_version[:SHORT_VERSION_CNT]}[/]"
        )

    def build_from_folder(
        self, folder: Path, kind: DatasetFolderSourceType, **kwargs: t.Any
    ) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        ds = SDKDataset.from_folder(folder=folder, kind=kind, name=self.uri, **kwargs)

        console.print(
            ":hibiscus: congratulation! you can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{ds.committed_version[:SHORT_VERSION_CNT]}[/]"
        )

    def build(self, *args: t.Any, **kwargs: t.Any) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        tags = kwargs.get("tags", [])
        StandaloneTag.check_tags_validation(tags)

        mode = DatasetChangeMode(kwargs.get("mode", DatasetChangeMode.PATCH))
        dataset_config: DatasetConfig = kwargs["config"]
        with SDKDataset.dataset(self.uri) as sds:
            sds = sds.with_builder_blob_config(
                volume_size=dataset_config.attr.volume_size,
                alignment_size=dataset_config.attr.alignment_size,
            )

            if mode == DatasetChangeMode.OVERWRITE:
                # TODO: use other high performance way to delete all records
                for row in sds:
                    del sds[row.index]

            console.print(
                f":new: pending commit version: {sds.pending_commit_version[:SHORT_VERSION_CNT]}"
            )
            self._build_from_iterable_handler(sds, dataset_config)
            version = sds.commit(tags=tags)
            self._version = self.uri.version = version

            console.print(
                ":hibiscus: congratulation! you can run "
                f"[red bold blink] swcli dataset info {self.name}/version/{version[:SHORT_VERSION_CNT]}[/]"
            )

    def _build_from_iterable_handler(
        self, sds: SDKDataset, dataset_config: DatasetConfig
    ) -> None:
        _handler_name = getattr(dataset_config.handler, "__name__", None) or str(
            dataset_config.handler
        )

        console.print(
            f":ghost: iterate records from python iterable handler: {_handler_name}"
        )
        total = 0
        for index, item in enumerate(dataset_config.handler()):
            if isinstance(item, DataRow):
                key = item.index
            elif isinstance(item, (tuple, list)) and len(item) == 2:
                key = item[0]
            else:
                key = index  # auto-incr index
            sds[key] = item
            total += 1

        console.print(f":butterfly: update {total} records into {self.name} dataset")


class CloudDataset(CloudBundleModelMixin, Dataset):
    def __init__(self, uri: Resource) -> None:
        super().__init__(uri)
        self.typ = InstanceType.CLOUD

    def info(self) -> LocalDatasetInfo | DatasetInfoVo | None:
        uri: Resource = self.uri
        r = DatasetApi(uri.instance).info(uri).raise_on_error().response()
        return r.data

    @classmethod
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filter: t.Optional[ListFilter] = None,
    ) -> t.Tuple[DatasetListType, t.Dict[str, t.Any]]:
        crm = CloudRequestMixed()
        r = (
            DatasetApi(project_uri.instance)
            .list(project_uri.name, page, size, filter)
            .raise_on_error()
            .response()
        )
        return r.data.list or [], crm.parse_pager(r.dict())

    def summary(self) -> t.Optional[DatasetSummary]:
        resp = self.do_http_request(
            f"/project/{self.uri.project.id}/{self.uri.typ.name}/{self.uri.name}",
            method=HTTPMethod.GET,
            instance=self.uri.instance,
            params={"versionUrl": self.uri.version},
            ignore_status_codes=[
                HTTPStatus.NOT_FOUND,
                HTTPStatus.INTERNAL_SERVER_ERROR,
            ],
        )
        if resp.status_code != HTTPStatus.OK:
            return None

        content = resp.json()
        _manifest: t.Dict[str, t.Any] = yaml.safe_load(
            content["data"].get("versionMeta", {})
        )
        _summary = _manifest.get("dataset_summary", {})
        return DatasetSummary(**_summary) if _summary else None

    def build(self, *args: t.Any, **kwargs: t.Any) -> None:
        raise NoSupportError("no support build dataset in the cloud instance")
