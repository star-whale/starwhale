import typing as t
from pathlib import Path

from starwhale.consts import DEFAULT_MANIFEST_NAME, DEFAULT_SW_TASK_RUN_IMAGE
from starwhale.base.type import URIType, BundleType
from starwhale.base.store import BaseStorage


class RuntimeStorage(BaseStorage):
    def _guess(self) -> t.Tuple[Path, str]:
        return self._guess_for_bundle()

    @property
    def bundle_type(self) -> str:
        return BundleType.RUNTIME

    @property
    def uri_type(self) -> str:
        return URIType.RUNTIME

    @property
    def recover_loc(self) -> Path:
        return self._get_recover_loc_for_bundle()

    @property
    def snapshot_workdir(self) -> Path:
        return self._get_snapshot_workdir_for_bundle()

    @property
    def export_dir(self) -> Path:
        return self.snapshot_workdir / "export"

    @property
    def runtime_dir(self) -> Path:
        return self.project_dir / URIType.RUNTIME / self.uri.object.name

    @property
    def manifest_path(self) -> Path:
        return self.snapshot_workdir / DEFAULT_MANIFEST_NAME

    def get_docker_base_image(self) -> str:
        return self.manifest.get("base_image", DEFAULT_SW_TASK_RUN_IMAGE)
