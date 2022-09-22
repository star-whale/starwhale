import typing as t
from pathlib import Path

from starwhale.consts import DEFAULT_MANIFEST_NAME
from starwhale.base.type import URIType, BundleType
from starwhale.base.store import BaseStorage


class ModelStorage(BaseStorage):
    def _guess(self) -> t.Tuple[Path, str]:
        return self._guess_for_bundle()

    @property
    def bundle_type(self) -> str:
        return BundleType.MODEL

    @property
    def uri_type(self) -> str:
        return URIType.MODEL

    @property
    def src_dir(self) -> Path:
        return self.snapshot_workdir / "src"

    @property
    def recover_loc(self) -> Path:
        return self._get_recover_loc_for_bundle()

    @property
    def manifest_path(self) -> Path:
        return self.snapshot_workdir / DEFAULT_MANIFEST_NAME

    @property
    def snapshot_workdir(self) -> Path:
        return self._get_snapshot_workdir_for_bundle()
