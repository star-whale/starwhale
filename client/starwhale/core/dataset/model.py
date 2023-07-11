from __future__ import annotations

import os
import typing as t
from abc import ABCMeta, abstractmethod
from http import HTTPStatus
from pathlib import Path
from collections import defaultdict

import yaml
import requests

from starwhale.utils import console, load_yaml
from starwhale.consts import (
    HTTPMethod,
    CREATED_AT_KEY,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import move_dir, empty_dir
from starwhale.base.type import (
    BundleType,
    InstanceType,
    DatasetChangeMode,
    DatasetFolderSourceType,
)
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.utils.http import ignore_error
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import NoSupportError
from starwhale.utils.retry import http_retry
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.copy import DatasetCopy
from starwhale.api._impl.dataset.loader import DataRow

from .type import DatasetConfig, DatasetSummary, D_ALIGNMENT_SIZE, D_FILE_VOLUME_SIZE
from .store import DatasetStorage

if t.TYPE_CHECKING:
    from starwhale.api._impl.dataset.model import Dataset as SDKDataset


class Dataset(BaseBundle, metaclass=ABCMeta):
    def __str__(self) -> str:
        return f"Starwhale Dataset: {self.uri}"

    @abstractmethod
    def summary(self) -> t.Optional[DatasetSummary]:
        raise NotImplementedError

    def head(
        self, rows: int = 5, show_raw_data: bool = False
    ) -> t.List[t.Dict[str, t.Any]]:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        ret = []
        sds = SDKDataset.dataset(self.uri, readonly=True)
        for idx, row in enumerate(sds.head(n=rows, skip_fetch_data=not show_raw_data)):
            ret.append(
                {
                    "index": row.index,
                    "features": row.features,
                    "id": idx,
                }
            )

        return ret

    def diff(self, compare_uri: Resource) -> t.Dict[str, t.Any]:
        # TODO: standalone or cloud dataset diff by datastore diff feature
        raise NotImplementedError

    def build_from_folder(
        self,
        folder: Path,
        kind: DatasetFolderSourceType,
        auto_label: bool = True,
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        raise NotImplementedError

    def build_from_json_file(
        self,
        json_file_path: str,
        field_selector: str = "",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        raise NotImplementedError

    def build_from_huggingface(
        self,
        repo: str,
        subset: str | None = None,
        split: str | None = None,
        revision: str = "main",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
        cache: bool = True,
    ) -> None:
        raise NotImplementedError

    @classmethod
    def get_dataset(cls, uri: Resource) -> Dataset:
        _cls = cls._get_cls(uri)
        return _cls(uri)

    @classmethod
    def copy(
        cls,
        src_uri: Resource,
        dest_uri: str,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
        dest_local_project_uri: str = "",
        force: bool = False,
    ) -> None:
        dc = DatasetCopy(
            src_uri=src_uri,
            dest_uri=dest_uri,
            force=force,
            mode=mode,
            dest_local_project_uri=dest_local_project_uri,
        )
        dc.do()

    @classmethod
    def _get_cls(  # type: ignore
        cls, uri: Resource
    ) -> t.Union[t.Type[StandaloneDataset], t.Type[CloudDataset]]:
        if uri.instance.is_local:
            return StandaloneDataset
        elif uri.instance.is_cloud:
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

    def add_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        self.tag.add(tags, ignore_errors)

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

    def info(self) -> t.Dict[str, t.Any]:
        if not self.store.bundle_path.exists():
            return {}
        else:
            return {
                "name": self.name,
                "uri": str(self.uri),
                "project": self.uri.project.name,
                "bundle_path": str(self.store.bundle_path),
                "version": self.uri.version,
                "tags": StandaloneTag(self.uri).list(),
                "manifest": self.store.manifest,
            }

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
        filters: t.Optional[t.Union[t.Dict[str, t.Any], t.List[str]]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        filters = filters or {}
        rs = defaultdict(list)

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

            rs[_bf.name].append(
                dict(
                    name=_bf.name,
                    version=_bf.version,
                    size=_manifest.get("dataset_summary", {}).get("blobs_byte_size", 0),
                    created_at=_manifest[CREATED_AT_KEY],
                    is_removed=_bf.is_removed,
                    path=_bf.path,
                    tags=_bf.tags,
                )
            )

        return rs, {}

    def build_from_huggingface(
        self,
        repo: str,
        subset: str | None = None,
        split: str | None = None,
        revision: str = "main",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
        cache: bool = True,
    ) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        ds = SDKDataset.from_huggingface(
            name=self.name,
            repo=repo,
            subset=subset,
            split=split,
            revision=revision,
            alignment_size=alignment_size,
            volume_size=volume_size,
            mode=mode,
            cache=cache,
        )
        console.print(
            f":hibiscus: congratulation! dataset build from https://huggingface.co/datasets/{repo} has been built. You can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{ds.committed_version[:SHORT_VERSION_CNT]}[/]"
        )

    def build_from_json_file(
        self,
        json_file_path: str | Path,
        field_selector: str = "",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        json_file_path = str(json_file_path)
        if json_file_path.startswith(("http://", "https://")):

            @http_retry
            def _r(url: str) -> str:
                return requests.get(url, verify=False, timeout=90).text

            json_text = _r(json_file_path)
        elif os.path.isfile(json_file_path):
            json_text = Path(json_file_path).read_text()
        else:
            raise RuntimeError(f"json file path:{json_file_path} not exists")

        ds = SDKDataset.from_json(
            name=self.name,
            json_text=json_text,
            field_selector=field_selector,
            alignment_size=alignment_size,
            volume_size=volume_size,
            mode=mode,
        )

        console.print(
            f":hibiscus: congratulation! dataset build from {json_file_path} has been built. You can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{ds.committed_version[:SHORT_VERSION_CNT]}[/]"
        )

    def build_from_folder(
        self,
        folder: Path,
        kind: DatasetFolderSourceType,
        auto_label: bool = True,
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode = DatasetChangeMode.PATCH,
    ) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

        ds = SDKDataset.from_folder(
            folder=folder,
            kind=kind,
            name=self.uri,
            alignment_size=alignment_size,
            volume_size=volume_size,
            auto_label=auto_label,
            mode=mode,
        )

        console.print(
            ":hibiscus: congratulation! you can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{ds.committed_version[:SHORT_VERSION_CNT]}[/]"
        )

    def build(self, *args: t.Any, **kwargs: t.Any) -> None:
        from starwhale.api._impl.dataset.model import Dataset as SDKDataset

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
            version = sds.commit()
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
            # TODO: support `--append`, `--update` and `--overwrite` for dataset build from iterable handler.
            # current build only supports `--update` feature.
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

    @classmethod
    @ignore_error(({}, {}))
    def list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filter_dict: t.Optional[t.Dict[str, t.Any]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        filter_dict = filter_dict or {}
        crm = CloudRequestMixed()
        return crm._fetch_bundle_all_list(
            project_uri, ResourceType.dataset, page, size, filter_dict
        )

    def summary(self) -> t.Optional[DatasetSummary]:
        resp = self.do_http_request(
            f"/project/{self.uri.project.name}/{self.uri.typ.name}/{self.uri.name}",
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
