import os
import typing as t
from pathlib import Path

from starwhale.utils import load_yaml
from starwhale.consts import (
    FileDesc,
    FileNode,
    SWMP_SRC_FNAME,
    SW_AUTO_DIRNAME,
    RESOURCE_FILES_NAME,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.utils.fs import extract_tar
from starwhale.base.bundle_copy import BundleCopy


class ModelCopy(BundleCopy):
    def upload_files(self, workdir: Path) -> t.Iterator[FileNode]:
        for _m in self._get_resource_files(workdir):
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

    def _get_resource_files(self, workdir: Path) -> t.List[t.Dict[str, t.Any]]:
        resource_files_path = workdir / "src" / SW_AUTO_DIRNAME / RESOURCE_FILES_NAME
        if resource_files_path.exists():
            resources = load_yaml(resource_files_path)
        else:
            resources = load_yaml(workdir / DEFAULT_MANIFEST_NAME).get("resources", [])
        return resources  # type: ignore

    def download_files(self, workdir: Path) -> t.Iterator[FileNode]:
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
        for _m in self._get_resource_files(workdir):
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
