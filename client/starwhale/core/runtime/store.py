import typing as t
from loguru import logger
from pathlib import Path
import platform
from starwhale.utils.config import SWCliConfigMixed
import yaml
import tarfile

from starwhale import __version__
from starwhale.base.type import URIType, BundleType
from starwhale.base.uri import URI
from starwhale.utils import gen_uniq_version, console, now_str
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME,
    RECOVER_DIRNAME,
    SHORT_VERSION_CNT,
    VERSION_PREFIX_CNT,
)
from starwhale.base.store import BaseStorage
from starwhale.utils.fs import guess_real_path, ensure_file, ensure_link, ensure_dir


class RuntimeStorage(BaseStorage):
    def _guess(self) -> t.Tuple[Path, str]:
        name = self.uri.object.name
        version = self.uri.object.version
        if version:
            _p, _v = guess_real_path(
                self.project_dir
                / URIType.RUNTIME
                / name
                / version[:VERSION_PREFIX_CNT],
                version,
            )

            if _v.endswith(BundleType.RUNTIME):
                _v = _v.split(BundleType.RUNTIME)[0]

            self.uri.object.version = _v
            return _p, _v
        else:
            return self.project_dir / URIType.RUNTIME / name, name

    @property
    def recover_loc(self) -> Path:
        version = self.uri.object.version

        loc = (
            self.project_dir / URIType.RUNTIME / RECOVER_DIRNAME / self.uri.object.name
        )

        if version:
            loc = loc / version[:VERSION_PREFIX_CNT] / f"{version}{BundleType.RUNTIME}"

        return loc

    @property
    def snapshot_workdir(self) -> Path:
        version = self.uri.object.version
        return (
            self.project_dir
            / "workdir"
            / URIType.RUNTIME
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
            / version
        )

    @property
    def conda_dir(self) -> Path:
        return self.snapshot_workdir / "dep" / "conda"

    @property
    def python_dir(self) -> Path:
        return self.snapshot_workdir / "dep" / "python"

    @property
    def venv_dir(self) -> Path:
        return self.python_dir / "venv"

    @property
    def runtime_dir(self) -> Path:
        return self.project_dir / URIType.RUNTIME / self.uri.object.name

    @property
    def bundle_dir(self) -> Path:
        version = self.uri.object.version
        return (
            self.project_dir
            / URIType.RUNTIME
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
        )

    @property
    def bundle_path(self) -> Path:
        return self.bundle_dir / f"{self.uri.object.version}{BundleType.RUNTIME}"

    @property
    def latest_bundle_dir(self) -> Path:
        return self.project_dir / URIType.RUNTIME / "latest"

    def iter_history(self) -> t.Generator[t.Tuple[str, Path], None, None]:
        for _path in self.runtime_dir.glob(f"**/*{BundleType.RUNTIME}"):
            if not _path.name.endswith(BundleType.RUNTIME):
                continue
            _rt_version = _path.name.split(BundleType.RUNTIME)[0]
            yield _rt_version, _path

    @staticmethod
    def iter_all_runtime_bundles(
        project_uri: URI,
    ) -> t.Generator[t.Tuple[str, str, Path, bool], None, None]:
        sw = SWCliConfigMixed()
        _runtime_dir = sw.rootdir / project_uri.project / URIType.RUNTIME
        for _path in _runtime_dir.glob(f"**/*{BundleType.RUNTIME}"):
            if not _path.name.endswith(BundleType.RUNTIME):
                continue

            _rt_name = _path.parent.parent.name
            _rt_version = _path.name.split(BundleType.RUNTIME)[0]
            yield _rt_name, _rt_version, _path, RECOVER_DIRNAME in _path.parts

    @property
    def mainfest(self) -> t.Dict[str, t.Any]:
        _mf = self.snapshot_workdir / DEFAULT_MANIFEST_NAME
        if not _mf.exists():
            return {}
        else:
            return yaml.safe_load(_mf.open())


class SWBundleMixin(object):
    def __init__(self) -> None:
        self._manifest: t.Dict[str, t.Any] = {}

    def _render_manifest(self, user_raw_config: t.Dict[str, t.Any] = {}) -> None:
        self._manifest["name"] = self.name  # type: ignore
        self._manifest["build"] = dict(
            os=platform.system(),
            sw_version=__version__,
        )

        # TODO: remove object type
        self._manifest["user_raw_config"] = user_raw_config

        # TODO: add signature for import files: model, config
        _f = self.store.snapshot_workdir / DEFAULT_MANIFEST_NAME  # type: ignore
        ensure_file(_f, yaml.dump(self._manifest, default_flow_style=False))
        logger.info(f"[step:manifest]render manifest: {_f}")

    def _gen_version(self) -> None:
        logger.info("[step:version]create version...")
        if not getattr(self, "_version", ""):
            self._version = gen_uniq_version(self.name)  # type: ignore

        self.uri.object.version = self._version  # type:ignore
        self._manifest["version"] = self._version  # type: ignore
        self._manifest["created_at"] = now_str()  # type: ignore
        logger.info(f"[step:version]version: {self._version}")
        console.print(f":new: version {self._version[:SHORT_VERSION_CNT]}")  # type: ignore

    def _make_tar(self, ftype: str = "") -> None:
        out = self.store.bundle_dir / f"{self._version}{ftype}"  # type: ignore
        ensure_dir(self.store.bundle_dir)  # type: ignore
        logger.info(f"[step:tar]try to tar {out} ...")

        with tarfile.open(out, "w:") as tar:
            tar.add(str(self.store.snapshot_workdir), arcname="")  # type: ignore

        ensure_link(out, self.store.latest_bundle_dir)  # type: ignore
        console.print(f":butterfly: swrt:{out}")
        logger.info("[step:tar]finish to make bundle tar")
