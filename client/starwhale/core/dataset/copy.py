from __future__ import annotations

import os
import copy
from typing import Iterator
from pathlib import Path

from rich.progress import Progress

from starwhale.utils import console, load_yaml, NoSupportError
from starwhale.consts import (
    FileDesc,
    FileNode,
    STANDALONE_INSTANCE,
    DEFAULT_MANIFEST_NAME,
    ARCHIVED_SWDS_META_FNAME,
)
from starwhale.utils.fs import ensure_dir
from starwhale.base.bundle_copy import BundleCopy

from .store import DatasetStorage
from .tabular import TabularDataset


class DatasetCopy(BundleCopy):
    def with_disable_datastore(self) -> DatasetCopy:
        self._disable_datastore = True
        return self

    def with_enable_datastore(self) -> DatasetCopy:
        self._disable_datastore = False
        return self

    @property
    def datastore_disabled(self) -> bool:
        return getattr(self, "_disable_datastore", False)

    def upload_files(self, workdir: Path) -> Iterator[FileNode]:
        _manifest = load_yaml(workdir / DEFAULT_MANIFEST_NAME)
        for _k in _manifest["signature"]:
            _size, _, _hash = _k.split(":")
            # TODO: head object by hash name at first
            _path = workdir / "data" / _hash[: DatasetStorage.short_sign_cnt]
            yield FileNode(
                path=_path,
                name=os.path.basename(_path),
                size=_size,
                file_desc=FileDesc.DATA,
                signature=_hash,
            )

        _meta_names = [ARCHIVED_SWDS_META_FNAME]

        for _n in _meta_names:
            _path = workdir / _n

            yield FileNode(
                path=_path,
                name=os.path.basename(_path),
                size=_path.stat().st_size,
                file_desc=FileDesc.SRC_TAR,
                signature="",
            )

    def download_files(self, workdir: Path) -> Iterator[FileNode]:
        ensure_dir(workdir / "data")
        _manifest = load_yaml(workdir / DEFAULT_MANIFEST_NAME)
        for _k in _manifest.get("signature", []):
            # TODO: parallel download
            _size, _algo, _hash = _k.split(":")
            if _algo != DatasetStorage.object_hash_algo:
                raise NoSupportError(f"download file hash algorithm {_algo}")

            _dest = DatasetStorage._get_object_store_path(_hash)

            yield FileNode(
                path=_dest,
                signature=_hash,
                size=_size,
                name=_hash[: DatasetStorage.short_sign_cnt],
                file_desc=FileDesc.DATA,
            )

            Path(workdir / "data" / _hash[: DatasetStorage.short_sign_cnt]).symlink_to(
                _dest
            )

        for _f in (ARCHIVED_SWDS_META_FNAME,):
            yield FileNode(
                path=workdir / _f,
                signature="",
                size=0,
                name=_f,
                file_desc=FileDesc.SRC_TAR,
            )

    def _do_ubd_datastore(self) -> None:
        if self.datastore_disabled:
            return

        with TabularDataset(
            name=self.bundle_name,
            version=self.bundle_version,
            project=self.src_uri.project,
            instance_name=STANDALONE_INSTANCE,
        ) as local, TabularDataset(
            name=self.bundle_name,
            version=self.bundle_version,
            project=self.dest_uri.project,
            instance_name=self.dest_uri.instance,
        ) as remote:
            console.print(
                f":bear_face: dump dataset meta from standalone to cloud({remote._ds_wrapper._table_name})"
            )
            # TODO: add progressbar
            for row in local.scan():
                remote.put(row)

            remote._info = copy.deepcopy(local.info)

    def _do_download_bundle_dir(self, progress: Progress) -> None:
        if not self.datastore_disabled:
            with TabularDataset(
                name=self.bundle_name,
                version=self.bundle_version,
                project=self.dest_uri.project,
                instance_name=STANDALONE_INSTANCE,
            ) as local, TabularDataset(
                name=self.bundle_name,
                version=self.bundle_version,
                project=self.src_uri.project,
                instance_name=self.src_uri.instance,
            ) as remote:
                console.print(
                    f":bird: load dataset meta from cloud({remote._ds_wrapper._table_name}) to standalone"
                )
                # TODO: add progressbar
                for row in remote.scan():
                    local.put(row)

                local._info = copy.deepcopy(remote.info)

        super()._do_download_bundle_dir(progress)
