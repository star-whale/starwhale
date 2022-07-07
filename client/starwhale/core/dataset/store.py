import typing as t
from pathlib import Path

from starwhale.consts import VERSION_PREFIX_CNT, DEFAULT_MANIFEST_NAME
from starwhale.base.type import URIType, BundleType
from starwhale.base.store import BaseStorage

# TODO: refactor Dataset and ModelPackage LocalStorage


class DatasetStorage(BaseStorage):
    def _guess(self) -> t.Tuple[Path, str]:
        return self._guess_for_bundle()

    @property
    def recover_loc(self) -> Path:
        return self._get_recover_loc_for_bundle()

    @property
    def snapshot_workdir(self) -> Path:
        if self.building:
            return self.tmp_dir
        version = self.uri.object.version
        return (
            self.project_dir
            / URIType.DATASET
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
            / f"{version}{BundleType.DATASET}"
        )

    @property
    def manifest_path(self) -> Path:
        return self.loc / DEFAULT_MANIFEST_NAME

    @property
    def bundle_type(self) -> str:
        return BundleType.DATASET

    @property
    def uri_type(self) -> str:
        return URIType.DATASET

    @property
    def data_dir(self) -> Path:
        return self.snapshot_workdir / "data"

    @property
    def src_dir(self) -> Path:
        return self.snapshot_workdir / "src"

    @property
    def dataset_rootdir(self) -> Path:
        return self.project_dir / URIType.DATASET
