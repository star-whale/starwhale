from __future__ import annotations

import json
import typing as t
import tarfile
from abc import ABCMeta
from pathlib import Path
from collections import defaultdict

from fs import open_fs
from loguru import logger
from fs.copy import copy_fs, copy_file

from starwhale.utils import console, load_yaml
from starwhale.consts import (
    JSON_INDENT,
    DataLoaderKind,
    DefaultYAMLName,
    SWDSBackendType,
    SWDSSubFileType,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    VERSION_PREFIX_CNT,
    SWDS_DATA_FNAME_FMT,
    DEFAULT_COPY_WORKERS,
    LOCAL_FUSE_JSON_NAME,
    SWDS_LABEL_FNAME_FMT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import (
    move_dir,
    ensure_dir,
    ensure_file,
    blake2b_file,
    BLAKE2B_SIGNATURE_ALGO,
)
from starwhale.base.type import URIType, BundleType, InstanceType
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.utils.http import ignore_error
from starwhale.utils.load import import_cls
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import NoSupportError
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.bundle_copy import BundleCopy

from .store import DatasetStorage
from .dataset import DatasetConfig, DSProcessMode, ARCHIVE_SWDS_META


class Dataset(BaseBundle, metaclass=ABCMeta):
    def __str__(self) -> str:
        return f"Starwhale Dataset: {self.uri}"

    @classmethod
    def render_fuse_json(cls, workdir: Path, force: bool = False) -> str:
        return StandaloneDataset.render_fuse_json(workdir, force)

    @classmethod
    def get_dataset(cls, uri: URI) -> Dataset:
        _cls = cls._get_cls(uri)
        return _cls(uri)

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
        bc = BundleCopy(src_uri, dest_uri, URIType.DATASET, force)
        bc.do()

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
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest classget_conda_env
        self.yaml_name = DefaultYAMLName.DATASET

    def add_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.add(tags, quiet)

    def remove_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.remove(tags, quiet)

    @classmethod
    def render_fuse_json(cls, workdir: Path, force: bool = False) -> str:
        _mf = workdir / DEFAULT_MANIFEST_NAME
        if not _mf.exists():
            raise Exception(f"need {DEFAULT_MANIFEST_NAME} @ {workdir}")

        _manifest = load_yaml(_mf)
        _fuse = dict(
            backend=SWDSBackendType.FUSE,
            kind=DataLoaderKind.SWDS,
            swds=[],
        )

        ds_name = _manifest["name"]
        ds_version = _manifest["version"]
        swds_bins = [
            _k
            for _k in _manifest["signature"].keys()
            if _k.startswith("data_") and _k.endswith(SWDSSubFileType.BIN)
        ]

        bucket = workdir.parent.parent.parent
        path_prefix = f"{ds_name}/{ds_version[:VERSION_PREFIX_CNT]}/{ds_version}{BundleType.DATASET}/data"
        for idx in range(0, len(swds_bins)):
            _fuse["swds"].append(  # type: ignore
                dict(
                    bucket=str(bucket.resolve()),
                    key=dict(
                        data=f"{path_prefix}/{SWDS_DATA_FNAME_FMT.format(index=idx)}",
                        label=f"{path_prefix}/{SWDS_LABEL_FNAME_FMT.format(index=idx)}",
                        # TODO: add extra_attr ds_name, ds_versoin
                    ),
                    ext_attr=dict(
                        ds_name=ds_name,
                        ds_version=ds_version,
                    ),
                )
            )

        _f = workdir / LOCAL_FUSE_JSON_NAME
        if _f.exists() and not force:
            console.print(f":joy_cat: {LOCAL_FUSE_JSON_NAME} existed, skip render")
        else:
            ensure_file(_f, json.dumps(_fuse, indent=JSON_INDENT))
            console.print(
                f":clap: render swds {ds_name}:{ds_version} {LOCAL_FUSE_JSON_NAME}"
            )

        console.print(f":mag: {_f}")
        return str(_f.resolve())

    def history(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        _r = []

        for _bf in self.store.iter_bundle_history():
            _manifest = load_yaml(_bf.path / DEFAULT_MANIFEST_NAME)

            _r.append(
                dict(
                    name=_manifest["name"],
                    version=_bf.version,
                    size=_manifest.get("dataset_byte_size", 0),
                    created_at=_manifest["created_at"],
                    tags=_bf.tags,
                    path=_bf.path,
                )
            )

        return _r, {}

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: remove by tag
        return move_dir(self.store.snapshot_workdir, self.store.recover_loc, force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        dest_path = (
            self.store.bundle_dir / f"{self.uri.object.version}{BundleType.DATASET}"
        )
        return move_dir(self.store.recover_loc, dest_path, force)

    def info(self) -> t.Dict[str, t.Any]:
        return self._get_bundle_info()

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
            _manifest = load_yaml(_bf.path / DEFAULT_MANIFEST_NAME)

            rs[_bf.name].append(
                dict(
                    name=_manifest["name"],
                    version=_bf.version,
                    size=_manifest.get("dataset_byte_size", 0),
                    created_at=_manifest["created_at"],
                    is_removed=_bf.is_removed,
                    path=_bf.path,
                    tags=_bf.tags,
                )
            )

        return rs, {}

    def buildImpl(
        self, workdir: Path, yaml_name: str = DefaultYAMLName.DATASET, **kw: t.Any
    ) -> None:
        _dp = workdir / yaml_name
        _dataset_config = self._load_dataset_config(_dp)

        # TODO: design dataset layer mechanism
        # TODO: design append some new data into existed dataset
        # TODO: design uniq build steps for model build, swmp build

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._copy_src,
                15,
                "copy src",
                dict(
                    workdir=workdir,
                    yaml_name=yaml_name,
                    pkg_data=_dataset_config.pkg_data,
                    exclude_pkg_data=_dataset_config.exclude_pkg_data,
                ),
            ),
            (
                self._call_make_swds,
                30,
                "make swds",
                dict(workdir=workdir, swds_config=_dataset_config),
            ),
            (self._calculate_signature, 5, "calculate signature"),
            (
                self._render_manifest,
                5,
                "render manifest",
            ),
            (self._make_swds_meta_tar, 15, "make meta tar"),
            (self._make_latest_tag, 5, "make latest tag"),
        ]
        run_with_progress_bar("swds building...", operations)

    def _call_make_swds(self, workdir: Path, swds_config: DatasetConfig) -> None:
        from starwhale.api._impl.dataset import BuildExecutor

        logger.info("[step:swds]try to gen swds...")
        self._manifest["dataset_attr"] = swds_config.attr.as_dict()
        self._manifest["mode"] = swds_config.mode
        self._manifest["process"] = swds_config.process

        # TODO: add more import format support, current is module:class
        logger.info(f"[info:swds]try to import {swds_config.process} @ {workdir}")
        _cls = import_cls(workdir, swds_config.process, BuildExecutor)

        with _cls(
            data_dir=workdir / swds_config.data_dir,
            output_dir=self.store.data_dir,
            data_filter=swds_config.data_filter,
            label_filter=swds_config.label_filter,
            batch=swds_config.attr.batch_size,
            alignment_bytes_size=swds_config.attr.alignment_size,
            volume_bytes_size=swds_config.attr.volume_size,
        ) as _obj:
            console.print(
                f":ghost: import [red]{swds_config.process}@{workdir.resolve()}[/] to make swds..."
            )
            if swds_config.mode == DSProcessMode.GENERATE:
                logger.info("[info:swds]do make swds_bin job...")
                _obj.make_swds()
            else:
                logger.info("[info:swds]skip make swds_bin")

        console.print(f"[step:swds]finish gen swds @ {self.store.data_dir}")

    def _calculate_signature(self) -> None:
        _algo = BLAKE2B_SIGNATURE_ALGO
        _sign = dict()
        total_size = 0

        logger.info(
            f"[step:signature]try to calculate signature with {_algo} @ {self.store.data_dir}"
        )
        console.print(":robot: calculate signature...")

        # TODO: _cal(self._snapshot_workdir / ARCHIVE_SWDS_META) # add meta sign into _manifest.yaml
        for f in self.store.data_dir.iterdir():
            if not f.is_file():
                continue

            _size = f.stat().st_size
            total_size += _size
            _sign[f.name] = f"{_size}:{_algo}:{blake2b_file(f)}"

        self._manifest["dataset_byte_size"] = total_size
        self._manifest["signature"] = _sign
        logger.info(
            f"[step:signature]finish calculate signature with {_algo} for {len(_sign)} files"
        )

    def _make_swds_meta_tar(self) -> None:
        out = self.store.snapshot_workdir / ARCHIVE_SWDS_META
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

    def _copy_src(
        self,
        workdir: Path,
        yaml_name: str,
        pkg_data: t.List[str],
        exclude_pkg_data: t.List[str],
    ) -> None:
        logger.info(f"[step:copy]start to copy src {workdir} -> {self.store.src_dir}")
        console.print(":thumbs_up: try to copy source code files...")
        workdir_fs = open_fs(str(workdir.absolute()))
        src_fs = open_fs(str(self.store.src_dir.absolute()))
        snapshot_fs = open_fs(str(self.store.snapshot_workdir.absolute()))

        copy_file(workdir_fs, yaml_name, src_fs, DefaultYAMLName.DATASET)
        copy_file(workdir_fs, yaml_name, snapshot_fs, DefaultYAMLName.DATASET)
        # TODO: tune copy src
        copy_fs(
            workdir_fs,
            src_fs,
            walker=self._get_src_walker(workdir, pkg_data, exclude_pkg_data),
            workers=DEFAULT_COPY_WORKERS,
        )

        logger.info("[step:copy]finish copy files")

    def _load_dataset_config(self, yaml_path: Path) -> DatasetConfig:
        self._do_validate_yaml(yaml_path)
        _config = DatasetConfig.create_by_yaml(yaml_path)

        if not (yaml_path.parent / _config.data_dir).exists():
            raise FileNotFoundError(f"dataset datadir:{_config.data_dir}")

        return _config


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
