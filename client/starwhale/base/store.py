import typing as t
from abc import ABCMeta, abstractmethod, abstractproperty
from pathlib import Path

import yaml
from fs.tarfs import TarFS

from starwhale.consts import (
    RECOVER_DIRNAME,
    SHORT_VERSION_CNT,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import guess_real_path
from starwhale.utils.config import SWCliConfigMixed


class BundleField(t.NamedTuple):
    name: str = ""
    version: str = ""
    tags: t.List[str] = []
    path: Path = Path()
    is_removed: bool = False


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
    def manifest(self) -> t.Dict[str, t.Any]:
        if not self.manifest_path.exists():
            return {}
        else:
            return yaml.safe_load(self.manifest_path.open())

    def _get_recover_snapshot_workdir_for_bundle(self) -> Path:
        version = self.uri.object.version
        return (
            self.project_dir
            / "workdir"
            / self.uri_type
            / RECOVER_DIRNAME
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
            / version
        )

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
        rootdir = self.project_dir / self.uri_type / name
        if version:
            _p, _v, _ok = guess_real_path(
                rootdir / version[:VERSION_PREFIX_CNT],
                version,
            )

            if not _ok:
                _manifest = StandaloneTag.get_manifest_by_dir(rootdir)
                _tag_version = _manifest.get("tags", {}).get(version, "")
                if _tag_version:
                    _p, _v, _ok = guess_real_path(
                        rootdir / _tag_version[:VERSION_PREFIX_CNT], _tag_version
                    )

            if _v.endswith(self.bundle_type):
                _v = _v.split(self.bundle_type)[0]

            self.uri.object.version = _v
            return _p, _v
        else:
            return self.project_dir / self.uri_type / name, name

    def iter_bundle_history(self) -> t.Generator[BundleField, None, None]:
        rootdir = self.project_dir / self.uri_type / self.uri.object.name
        _manifest = StandaloneTag.get_manifest_by_dir(rootdir)
        tags_map: t.Dict[str, t.Any] = _manifest.get("versions", {})

        for _path in rootdir.glob(f"**/*{self.bundle_type}"):
            if not _path.name.endswith(self.bundle_type):
                continue
            _rt_version = _path.name.split(self.bundle_type)[0]
            _tags = tags_map.get(_rt_version, {}).keys()
            yield BundleField(
                name=_path.name,
                version=_rt_version,
                tags=list(_tags),
                path=_path,
                is_removed=False,
            )

    @classmethod
    def iter_all_bundles(
        cls,
        project_uri: URI,
        bundle_type: str,
        uri_type: str,
    ) -> t.Generator[BundleField, None, None]:
        sw = SWCliConfigMixed()
        _obj_dir = sw.rootdir / project_uri.project / uri_type
        _tags_map = {}
        for _path in _obj_dir.glob(f"**/*{bundle_type}"):
            if not _path.name.endswith(bundle_type):
                continue

            _rt_name = _path.parent.parent.name
            if _rt_name not in _tags_map:
                _manifest = StandaloneTag.get_manifest_by_dir(_obj_dir / _rt_name)
                _tags_map[_rt_name] = _manifest.get("versions", {})

            _rt_version = _path.name.split(bundle_type)[0]
            _tags = _tags_map.get(_rt_name, {}).get(_rt_version, {}).keys()
            yield BundleField(
                name=_rt_name,
                version=_rt_version,
                tags=list(_tags),
                path=_path,
                is_removed=RECOVER_DIRNAME in _path.parts,
            )

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
