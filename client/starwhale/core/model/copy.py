import os
from typing import Iterator
from pathlib import Path

from starwhale.utils import load_yaml
from starwhale.consts import FileDesc, FileNode, SWMP_SRC_FNAME, DEFAULT_MANIFEST_NAME
from starwhale.utils.fs import extract_tar
from starwhale.base.bundle_copy import BundleCopy


class ModelCopy(BundleCopy):
    def upload_files(self, workdir: Path) -> Iterator[FileNode]:
        _manifest = load_yaml(workdir / DEFAULT_MANIFEST_NAME)
        for _m in _manifest.get("resources", []):
            if _m["desc"] != FileDesc.MODEL.name:
                continue
            _path = workdir / _m["path"]
            yield FileNode(
                path=_path,
                name=_m["name"],
                size=_path.stat().st_size,
                file_desc=FileDesc.MODEL,
                signature=_m["signature"],
            )

        _meta_names = [SWMP_SRC_FNAME]

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
        _manifest = load_yaml(workdir / DEFAULT_MANIFEST_NAME)
        for _f in (SWMP_SRC_FNAME,):
            yield FileNode(
                path=workdir / _f,
                signature="",
                size=0,
                name=_f,
                file_desc=FileDesc.SRC_TAR,
            )
            extract_tar(
                tar_path=workdir / SWMP_SRC_FNAME,
                dest_dir=workdir / "src",
                force=False,
            )
        # this must after src download
        for _m in _manifest.get("resources", []):
            if _m["desc"] != FileDesc.MODEL.name:
                continue
            _sign = _m["signature"]
            _dest = workdir / _m["path"]

            yield FileNode(
                path=_dest,
                signature=_sign,
                size=0,
                name=_m["name"],
                file_desc=FileDesc.MODEL,
            )
            # TODO use unified storage
            # Path(workdir / _m["path"]).symlink_to(
            #     _dest # the unify dir
            # )

    def _do_ubd_datastore(self) -> None:
        ...
