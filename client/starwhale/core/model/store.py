import typing as t
from pathlib import Path

from starwhale.utils import load_yaml
from starwhale.consts import (
    SW_AUTO_DIRNAME,
    DIGEST_FILE_NAME,
    VERSION_PREFIX_CNT,
    RESOURCE_FILES_NAME,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.type import BundleType
from starwhale.base.store import BaseStorage
from starwhale.base.uri.resource import ResourceType


class ModelStorage(BaseStorage):
    def _guess(self) -> t.Tuple[Path, str]:
        return self._guess_for_bundle()

    @property
    def bundle_type(self) -> str:
        return BundleType.MODEL

    @property
    def uri_type(self) -> str:
        return ResourceType.model.value

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
    def resource_files_path(self) -> Path:
        return self.hidden_sw_dir / RESOURCE_FILES_NAME

    @property
    def resource_files(self) -> t.List[t.Dict[str, t.Any]]:
        if not self.resource_files_path.exists():
            return self.manifest.get("resources", [])
        else:
            return load_yaml(self.resource_files_path)  # type: ignore

    @property
    def digest_path(self) -> Path:
        return self.hidden_sw_dir / DIGEST_FILE_NAME

    @property
    def digest(self) -> t.Dict[str, t.Any]:
        if not self.digest_path.exists():
            _manifest = self.manifest
            _manifest.pop("resources", None)
            _manifest.pop("build", None)
            return _manifest
        else:
            return load_yaml(self.digest_path)  # type: ignore

    @property
    def manifest_path(self) -> Path:
        return self.snapshot_workdir / DEFAULT_MANIFEST_NAME

    @property
    def snapshot_workdir(self) -> Path:
        if self.building:
            return self.tmp_dir
        version = self.uri.version
        return (
            self.project_dir
            / self.uri.typ.value
            / self.uri.name
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

    def get_packaged_runtime_docker_run_image(self) -> str:
        from starwhale.core.runtime.store import get_docker_run_image_by_manifest

        manifest = self.get_manifest_by_path(
            fpath=self.packaged_runtime_bundle_path,
            bundle_type=BundleType.RUNTIME,
            uri_type=ResourceType.runtime.value,
        )
        return get_docker_run_image_by_manifest(manifest)
