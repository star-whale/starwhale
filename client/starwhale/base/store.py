import typing as t
from pathlib import Path
from abc import ABCMeta, abstractmethod, abstractproperty
import yaml

from fs.tarfs import TarFS

from starwhale.base.uri import URI
from starwhale.utils.config import SWCliConfigMixed
from starwhale.consts import (
    SHORT_VERSION_CNT,
    VERSION_PREFIX_CNT,
    RECOVER_DIRNAME,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.utils.fs import guess_real_path


class BaseStorage(object):
    __metaclass__ = ABCMeta

    def __init__(self, uri: URI) -> None:
        self.uri = uri
        self.sw_config = SWCliConfigMixed()
        self.project_dir = self.sw_config.rootdir / self.uri.project
        self.loc, self.id = self._guess()

    @abstractmethod
    def _guess(self) -> t.Tuple[Path, str]:
        raise NotImplementedError

    @abstractproperty
    def recover_loc(self) -> Path:
        raise NotImplementedError

    @property
    def snapshot_workdir(self) -> Path:
        raise NotImplementedError

    @property
    def manifest_path(self) -> Path:
        raise NotImplementedError

    @property
    def bundle_type(self) -> str:
        raise NotImplementedError

    @property
    def uri_type(self) -> str:
        raise NotImplementedError

    @property
    def bundle_dir(self) -> Path:
        version = self.uri.object.version
        return (
            self.project_dir
            / self.uri_type
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
        )

    @property
    def bundle_path(self) -> Path:
        if self.uri.object.version:
            return self.bundle_dir / f"{self.uri.object.version}{self.bundle_type}"
        else:
            return self.bundle_dir

    @property
    def latest_bundle_dir(self) -> Path:
        return self.project_dir / self.uri_type / "latest"

    @property
    def mainfest(self) -> t.Dict[str, t.Any]:
        if not self.manifest_path.exists():
            return {}
        else:
            return yaml.safe_load(self.manifest_path.open())

    def _get_snapshot_workdir_for_bundle(self) -> Path:
        version = self.uri.object.version
        return (
            self.project_dir
            / "workdir"
            / self.uri_type
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
            / version
        )

    def _get_recover_loc_for_bundle(self) -> Path:
        loc = self.project_dir / self.uri_type / RECOVER_DIRNAME / self.uri.object.name

        version = self.uri.object.version
        if version:
            loc = loc / version[:VERSION_PREFIX_CNT] / f"{version}{self.bundle_type}"

        return loc

    def _guess_for_bundle(self) -> t.Tuple[Path, str]:
        name = self.uri.object.name
        version = self.uri.object.version
        if version:
            _p, _v = guess_real_path(
                self.project_dir / self.uri_type / name / version[:VERSION_PREFIX_CNT],
                version,
            )

            if _v.endswith(self.bundle_type):
                _v = _v.split(self.bundle_type)[0]

            self.uri.object.version = _v
            return _p, _v
        else:
            return self.project_dir / self.uri_type / name, name

    def iter_bundle_history(self) -> t.Generator[t.Tuple[str, Path], None, None]:
        rootdir = self.project_dir / self.uri_type / self.uri.object.name
        for _path in rootdir.glob(f"**/*{self.bundle_type}"):
            if not _path.name.endswith(self.bundle_type):
                continue
            _rt_version = _path.name.split(self.bundle_type)[0]
            yield _rt_version, _path

    @classmethod
    def iter_all_bundles(
        cls,
        project_uri: URI,
        bundle_type: str,
        uri_type: str,
    ) -> t.Generator[t.Tuple[str, str, Path, bool], None, None]:
        sw = SWCliConfigMixed()
        _runtime_dir = sw.rootdir / project_uri.project / uri_type
        for _path in _runtime_dir.glob(f"**/*{bundle_type}"):
            if not _path.name.endswith(bundle_type):
                continue

            _rt_name = _path.parent.parent.name
            _rt_version = _path.name.split(bundle_type)[0]
            yield _rt_name, _rt_version, _path, RECOVER_DIRNAME in _path.parts

    @classmethod
    def get_manifest_by_path(
        cls, fpath: Path, bundle_type: str, uri_type: str, direct: bool = False
    ) -> t.Any:
        if not direct and fpath.name.endswith(bundle_type):
            _model_dir = fpath.parent.parent
            _project_dir = _model_dir.parent.parent

            _mname = _model_dir.name
            _mversion = fpath.name.split(bundle_type)[0]

            _extracted_dir = (
                _project_dir
                / "workdir"
                / uri_type
                / _mname
                / _mversion[:SHORT_VERSION_CNT]
                / _mversion
            )
            _extracted_manifest = _extracted_dir / DEFAULT_MANIFEST_NAME

            if _extracted_manifest.exists():
                return yaml.safe_load(_extracted_manifest.open())

        with TarFS(str(fpath)) as tar:
            return yaml.safe_load(tar.open(DEFAULT_MANIFEST_NAME))
