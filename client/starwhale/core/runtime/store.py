import os
import typing as t
from pathlib import Path

from starwhale.consts import (
    SW_IMAGE_FMT,
    ENV_SW_IMAGE_REPO,
    DEFAULT_IMAGE_REPO,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_SW_TASK_RUN_IMAGE,
)
from starwhale.base.type import BundleType
from starwhale.base.store import BaseStorage
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.resource import ResourceType


class RuntimeStorage(BaseStorage):
    def _guess(self) -> t.Tuple[Path, str]:
        return self._guess_for_bundle()

    @property
    def bundle_type(self) -> str:
        return BundleType.RUNTIME

    @property
    def uri_type(self) -> str:
        return ResourceType.runtime.value

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
        return self.project_dir / ResourceType.runtime.value / self.uri.name

    @property
    def manifest_path(self) -> Path:
        return self.snapshot_workdir / DEFAULT_MANIFEST_NAME

    @property
    def manifest(self) -> t.Dict[str, t.Any]:
        return self.get_manifest_by_path(
            fpath=self.bundle_path,
            bundle_type=self.bundle_type,
            uri_type=self.uri_type,
        )

    def get_docker_run_image(self) -> str:
        return get_docker_run_image_by_manifest(self.manifest)


def get_docker_run_image_by_manifest(manifest: t.Dict) -> str:
    if "docker" in manifest:
        custom_run_image = manifest["docker"]["custom_run_image"]
        if custom_run_image:
            image = custom_run_image
        else:
            _builtin = manifest["docker"]["builtin_run_image"]
            _repo = (
                os.environ.get(ENV_SW_IMAGE_REPO)
                or SWCliConfigMixed().docker_builtin_image_repo
                or _builtin["repo"]
                or DEFAULT_IMAGE_REPO
            )
            image = SW_IMAGE_FMT.format(
                repo=_repo, name=_builtin["name"], tag=_builtin["tag"]
            )
    else:
        image = manifest.get("base_image", DEFAULT_SW_TASK_RUN_IMAGE)

    return image  # type: ignore[no-any-return]
