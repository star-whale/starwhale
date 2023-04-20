import typing as t
from pathlib import Path

from starwhale.consts import (
    SW_AUTO_DIRNAME,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_SW_TASK_RUN_IMAGE,
)
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
    def src_dir_name(self) -> str:
        return "src"

    @property
    def src_dir(self) -> Path:
        return self.snapshot_workdir / self.src_dir_name

    @property
    def hidden_sw_dir(self) -> Path:
        return self.src_dir / SW_AUTO_DIRNAME

    @property
    def recover_loc(self) -> Path:
        return self._get_recover_loc_for_bundle()

    @property
    def manifest_path(self) -> Path:
        return self.snapshot_workdir / DEFAULT_MANIFEST_NAME

    @property
    def snapshot_workdir(self) -> Path:
        if self.building:
            return self.tmp_dir
        version = self.uri.object.version
        return (
            self.project_dir
            / URIType.MODEL
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
            / f"{version}{BundleType.MODEL}"
        )

    @property
    def packaged_runtime_snapshot_workdir(self) -> Path:
        return self.snapshot_workdir / "runtime_snapshot_workdir"

    @property
    def packaged_runtime_export_dir(self) -> Path:
        return self.packaged_runtime_snapshot_workdir / "export"

    @property
    def packaged_runtime_bundle_path(self) -> Path:
        return self.hidden_sw_dir / "runtime" / f"packaged{BundleType.RUNTIME}"

    def get_packaged_runtime_base_image(self) -> str:
        manifest = self.get_manifest_by_path(
            fpath=self.packaged_runtime_bundle_path,
            bundle_type=BundleType.RUNTIME,
            uri_type=URIType.RUNTIME,
        )
        return manifest.get("base_image", DEFAULT_SW_TASK_RUN_IMAGE)
