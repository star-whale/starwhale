from __future__ import annotations

import typing as t
import inspect
import tarfile
from abc import ABCMeta, abstractmethod
from pathlib import Path
from collections import defaultdict

import yaml
from fs import open_fs
from loguru import logger
from fs.copy import copy_fs

from starwhale.utils import console, load_yaml
from starwhale.consts import (
    HTTPMethod,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_COPY_WORKERS,
    DEFAULT_MANIFEST_NAME,
    ARCHIVED_SWDS_META_FNAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import move_dir, empty_dir, ensure_dir, ensure_file
from starwhale.base.type import URIType, BundleType, InstanceType
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.utils.http import ignore_error
from starwhale.utils.load import import_object
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.progress import run_with_progress_bar

from .copy import DatasetCopy
from .type import DatasetConfig, DatasetSummary
from .store import DatasetStorage
from .tabular import TabularDataset


class Dataset(BaseBundle, metaclass=ABCMeta):
    def __str__(self) -> str:
        return f"Starwhale Dataset: {self.uri}"

    @abstractmethod
    def summary(self) -> t.Optional[DatasetSummary]:
        raise NotImplementedError

    def diff(self, compare_uri: URI) -> t.Dict[str, t.Any]:
        raise NotImplementedError

    @classmethod
    def get_dataset(cls, uri: URI) -> Dataset:
        _cls = cls._get_cls(uri)
        return _cls(uri)

    @classmethod
    def copy(
        cls,
        src_uri: str,
        dest_uri: str,
        force: bool = False,
        dest_local_project_uri: str = "",
    ) -> None:
        dc = DatasetCopy(
            src_uri,
            dest_uri,
            URIType.DATASET,
            force,
            dest_local_project_uri=dest_local_project_uri,
        )
        dc.do()

    @classmethod
    def _get_cls(  # type: ignore
        cls, uri: URI
    ) -> t.Union[t.Type[StandaloneDataset], t.Type[CloudDataset]]:
        if uri.instance_type == InstanceType.STANDALONE:
            return StandaloneDataset
        elif uri.instance_type == InstanceType.CLOUD:
            return CloudDataset
        else:
            raise NoSupportError(f"dataset uri:{uri}")


class StandaloneDataset(Dataset, LocalStorageBundleMixin):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.STANDALONE
        self.store = DatasetStorage(uri)
        self.tag = StandaloneTag(uri)
        self._manifest: t.Dict[
            str, t.Any
        ] = {}  # TODO: use manifest class get_conda_env
        self.yaml_name = DefaultYAMLName.DATASET

    def add_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.add(tags, quiet)

    def remove_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.remove(tags, quiet)

    def diff(self, compare_uri: URI) -> t.Dict[str, t.Any]:
        # TODO: support cross-instance diff: standalone <--> cloud
        if compare_uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError(
                f"only support standalone uri, but compare_uri({compare_uri}) is for cloud instance"
            )

        if self.uri.object.name != compare_uri.object.name:
            raise NoSupportError(
                f"only support two versions diff in one dataset, base dataset:{self.uri}, compare dataset:{compare_uri}"
            )

        if self.uri.object.version == compare_uri.object.version:
            return {}

        compare_ds = StandaloneDataset(compare_uri)
        base_summary = self.summary()
        compare_summary = compare_ds.summary()
        base_tds_iter = TabularDataset.from_uri(self.uri).scan()
        compare_tds_iter = TabularDataset.from_uri(compare_uri).scan()

        unchanged_cnt = 0
        diff_updated = []

        # TODO: tune diff performance
        for _brow, _crow in zip(base_tds_iter, compare_tds_iter):
            if _brow == _crow:
                unchanged_cnt += 1
            else:
                diff_updated.append((_brow.asdict(), _crow.asdict()))

        # TODO: tune diff deleted and added rows like git diff
        diff_deleted_keys = [r.id for r in base_tds_iter]
        diff_added_keys = [r.id for r in compare_tds_iter]

        return {
            "version": {
                "base": self.uri.object.version,
                "compare": compare_uri.object.version,
            },
            "summary": {
                "base": base_summary.asdict() if base_summary else {},
                "compare": compare_summary.asdict() if compare_summary else {},
            },
            "diff": {
                "updated": diff_updated,
                "deleted": diff_deleted_keys,
                "added": diff_added_keys,
            },
            "diff_rows": {
                "unchanged": unchanged_cnt,
                "updated": len(diff_updated),
                "added": len(diff_added_keys),
                "deleted": len(diff_deleted_keys),
            },
        }

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
                    size=_manifest.get("dataset_byte_size", 0),
                    created_at=_manifest["created_at"],
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
        dest_path = (
            self.store.bundle_dir / f"{self.uri.object.version}{BundleType.DATASET}"
        )
        return move_dir(self.store.recover_loc, dest_path, force)

    def info(self) -> t.Dict[str, t.Any]:
        return self._get_bundle_info()

    def summary(self) -> t.Optional[DatasetSummary]:
        _manifest = self.store.manifest
        _summary = _manifest.get("dataset_summary", {})
        return DatasetSummary(**_summary) if _summary else None

    @classmethod
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        rs = defaultdict(list)

        for _bf in DatasetStorage.iter_all_bundles(
            project_uri,
            bundle_type=BundleType.DATASET,
            uri_type=URIType.DATASET,
        ):
            _mf = _bf.path / DEFAULT_MANIFEST_NAME
            if not _mf.exists():
                continue

            _manifest = load_yaml(_bf.path / DEFAULT_MANIFEST_NAME)

            rs[_bf.name].append(
                dict(
                    name=_bf.name,
                    version=_bf.version,
                    size=_manifest.get("dataset_byte_size", 0),
                    created_at=_manifest["created_at"],
                    is_removed=_bf.is_removed,
                    path=_bf.path,
                    tags=_bf.tags,
                )
            )

        return rs, {}

    def buildImpl(self, workdir: Path, **kw: t.Any) -> None:
        config = kw["config"]
        append = config.append
        if append:
            append_from_uri = URI.capsulate_uri(
                instance=self.uri.instance,
                project=self.uri.project,
                obj_type=self.uri.object.typ,
                obj_name=self.uri.object.name,
                obj_ver=config.append_from,
            )
            append_from_store = DatasetStorage(append_from_uri)
            if not append_from_store.snapshot_workdir.exists():
                raise NotFoundError(f"dataset uri: {append_from_uri}")
        else:
            append_from_uri = None
            append_from_store = None

        # TODO: design uniq build steps for model build, swmp build

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._dump_dataset_yaml,
                5,
                "dump_dataset_yaml",
                dict(
                    swds_config=config,
                ),
            ),
            (
                self._copy_src,
                15,
                "copy src",
                dict(
                    workdir=workdir,
                    pkg_data=config.pkg_data,
                    exclude_pkg_data=config.exclude_pkg_data,
                ),
            ),
            (
                self._fork_swds,
                10,
                "fork swds",
                dict(
                    append=append,
                    append_from_store=append_from_store,
                ),
            ),
            (
                self._call_make_swds,
                30,
                "make swds",
                dict(
                    workdir=workdir,
                    swds_config=config,
                    append=append,
                    append_from_uri=append_from_uri,
                    append_from_store=append_from_store,
                ),
            ),
            (self._calculate_signature, 5, "calculate signature"),
            (
                self._render_manifest,
                5,
                "render manifest",
            ),
            (self._make_swds_meta_tar, 15, "make meta tar"),
            (self._make_auto_tags, 5, "make auto tags"),
        ]
        run_with_progress_bar("swds building...", operations)

    def _fork_swds(
        self, append: bool, append_from_store: t.Optional[DatasetStorage]
    ) -> None:
        if not append or not append_from_store:
            return

        console.print(
            f":articulated_lorry: fork dataset data from {append_from_store.id}"
        )
        src_data_dir = append_from_store.data_dir
        for src in src_data_dir.rglob("*"):
            if not src.is_symlink():
                continue

            dest = self.store.data_dir / src.relative_to(src_data_dir)
            if dest.exists() and dest.resolve() == src.resolve():
                continue

            dest.symlink_to(src.resolve().absolute())

    def _call_make_swds(
        self,
        workdir: Path,
        swds_config: DatasetConfig,
        append: bool,
        append_from_uri: t.Optional[URI],
        append_from_store: t.Optional[DatasetStorage],
    ) -> None:
        from starwhale.api._impl.dataset.builder import (
            BaseBuildExecutor,
            create_generic_cls,
        )

        logger.info("[step:swds]try to gen swds...")
        append_from_version = (
            append_from_store.id if append and append_from_store else ""
        )
        self._manifest.update(
            {
                "dataset_attr": swds_config.attr.asdict(),
                "handler": swds_config.handler,
                "from": {
                    "version": append_from_version,
                    "append": append,
                },
            }
        )

        # TODO: add more import format support, current is module:class
        logger.info(f"[info:swds]try to import {swds_config.handler} @ {workdir}")
        _handler = import_object(workdir, swds_config.handler)

        _cls: t.Type[BaseBuildExecutor]
        if inspect.isclass(_handler) and issubclass(_handler, BaseBuildExecutor):
            _cls = _handler
        elif inspect.isfunction(_handler):
            _cls = create_generic_cls(_handler)
        else:
            raise RuntimeError(
                f"{swds_config.handler} not BaseBuildExecutor or generator function"
            )

        with _cls(
            dataset_name=self.uri.object.name,
            dataset_version=self._version,
            project_name=self.uri.project,
            workdir=self.store.snapshot_workdir,
            alignment_bytes_size=swds_config.attr.alignment_size,
            volume_bytes_size=swds_config.attr.volume_size,
            append=append,
            append_from_version=append_from_version,
            append_from_uri=append_from_uri,
            data_mime_type=swds_config.attr.data_mime_type,
        ) as _obj:
            console.print(
                f":ghost: import [red]{swds_config.handler}@{workdir.resolve()}[/] to make swds..."
            )
            _summary: DatasetSummary = _obj.make_swds()
            self._manifest["dataset_summary"] = _summary.asdict()

        console.print(f"[step:swds]finish gen swds @ {self.store.data_dir}")

    def _calculate_signature(self) -> None:
        algo = self.store.object_hash_algo
        sign_info = list()
        total_size = 0

        # TODO: _cal(self._snapshot_workdir / ARCHIVED_SWDS_META_FNAME) # add meta sign into _manifest.yaml
        for fpath in self.store.get_all_data_files():
            _size = fpath.stat().st_size
            total_size += _size
            sign_info.append(f"{_size}:{algo}:{fpath.name}")

        self._manifest["dataset_byte_size"] = total_size
        self._manifest["signature"] = sign_info
        console.print(
            f":robot: calculate signature with {algo} for {len(sign_info)} files"
        )

    def _make_swds_meta_tar(self) -> None:
        out = self.store.snapshot_workdir / ARCHIVED_SWDS_META_FNAME
        logger.info(f"[step:tar]try to tar for swmp meta(NOT INCLUDE DATASET){out}")
        with tarfile.open(out, "w:") as tar:
            tar.add(str(self.store.src_dir), arcname="src")
            tar.add(str(self.store.snapshot_workdir / DEFAULT_MANIFEST_NAME))
            tar.add(str(self.store.snapshot_workdir / DefaultYAMLName.DATASET))

        console.print(
            ":hibiscus: congratulation! you can run "
            f"[red bold blink] swcli dataset info {self.name}/version/{self._version}[/]"
        )

    def _prepare_snapshot(self) -> None:
        ensure_dir(self.store.data_dir)
        ensure_dir(self.store.src_dir)

        console.print(
            f":file_folder: swds workdir: [underline]{self.store.snapshot_workdir}[/]"
        )

    def _dump_dataset_yaml(self, swds_config: DatasetConfig) -> None:
        console.print(":battery: dump dataset.yaml")
        _fpath = self.store.snapshot_workdir / DefaultYAMLName.DATASET
        ensure_file(
            _fpath, yaml.safe_dump(swds_config.asdict(), default_flow_style=False)
        )

    def _copy_src(
        self,
        workdir: Path,
        pkg_data: t.List[str],
        exclude_pkg_data: t.List[str],
    ) -> None:
        logger.info(f"[step:copy]start to copy src {workdir} -> {self.store.src_dir}")
        console.print(":thumbs_up: try to copy source code files...")
        workdir_fs = open_fs(str(workdir.absolute()))
        src_fs = open_fs(str(self.store.src_dir.absolute()))
        # TODO: tune copy src
        copy_fs(
            workdir_fs,
            src_fs,
            walker=self._get_src_walker(workdir, pkg_data, exclude_pkg_data),
            workers=DEFAULT_COPY_WORKERS,
        )

        logger.info("[step:copy]finish copy files")


class CloudDataset(CloudBundleModelMixin, Dataset):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.CLOUD

    @classmethod
    @ignore_error(({}, {}))
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        crm = CloudRequestMixed()
        return crm._fetch_bundle_all_list(project_uri, URIType.DATASET, page, size)

    def summary(self) -> t.Optional[DatasetSummary]:
        r = self.do_http_request(
            f"/project/{self.uri.project}/{self.uri.object.typ}/{self.uri.object.name}",
            method=HTTPMethod.GET,
            instance_uri=self.uri,
            params={"versionUrl": self.uri.object.version},
        ).json()
        _manifest: t.Dict[str, t.Any] = yaml.safe_load(r["data"].get("versionMeta", {}))
        _summary = _manifest.get("dataset_summary", {})
        return DatasetSummary(**_summary) if _summary else None

    def build(self, workdir: Path, yaml_name: str = "", **kw: t.Any) -> None:
        raise NoSupportError("no support build dataset in the cloud instance")
