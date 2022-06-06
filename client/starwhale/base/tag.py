import typing as t
from pathlib import Path
from xml.dom import NotFoundErr

import yaml

from starwhale.utils import now_str, validate_obj_name
from starwhale.consts import DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import InstanceType
from starwhale.utils.error import FormatError, NoSupportError, MissingFieldError


class StandaloneTag(object):
    def __init__(self, uri: URI) -> None:
        self.uri = uri

    def _do_validate(self) -> None:
        if self.uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError(self.uri.instance_type)

    @property
    def _manifest_path(self) -> Path:
        return (
            self.uri._sw_config.rootdir
            / self.uri.project
            / self.uri.object.typ
            / self.uri.object.name
            / DEFAULT_MANIFEST_NAME
        )

    def _get_manifest(self) -> t.Dict[str, t.Any]:
        if not self._manifest_path.exists():
            _dft: t.Dict[str, t.Any] = {
                "tags": {},
                "versions": {},
            }
            ensure_file(self._manifest_path, yaml.safe_dump(_dft))
            return _dft
        else:
            return yaml.safe_load(self._manifest_path.open())

    def _save_manifest(self, _manifest: t.Dict[str, t.Any]) -> None:
        _manifest["updated_at"] = now_str()  # type: ignore
        _manifest["name"] = self.uri.object.name
        _manifest["typ"] = self.uri.object.typ

        ensure_dir(self._manifest_path.parent)
        ensure_file(
            self._manifest_path, yaml.safe_dump(_manifest, default_flow_style=False)
        )

    def add(self, tags: t.List[str], quiet: bool = False) -> None:
        _manifest = self._get_manifest()
        _version = self.uri.object.version

        if not _version and not quiet:
            raise MissingFieldError(f"uri version, {self.uri}")

        for _t in tags:
            _t = _t.strip()
            if not _t:
                continue

            _ok, _reason = validate_obj_name(_t)
            if not _ok:
                if quiet:
                    continue
                else:
                    raise FormatError(f"{_t}, reason:{_reason}")

            if _manifest["tags"].get(_t, "") != _version:
                _pre_version = _manifest["tags"][_t]
                _manifest["versions"][_pre_version].pop(_t, None)
                if not _manifest["versions"][_pre_version]:
                    _manifest["versions"].pop(_pre_version, None)

            _manifest["tags"][_t] = _version
            if _version in _manifest["versions"]:
                _manifest["versions"][_version][_t] = True
            else:
                _manifest["versions"][_version] = {_t: True}

        self._save_manifest(_manifest)

    def remove(self, tags: t.List[str], quiet: bool = False) -> None:
        _manifest = self._get_manifest()
        for _t in tags:
            _version = _manifest["tags"].pop(_t, "")

            if _version not in _manifest["versions"]:
                if quiet:
                    continue
                else:
                    raise NotFoundErr(f"tag:{_t}, version:{_version}")

            _manifest["version"][_version].pop(_t, None)
            if not _manifest["version"][_version]:
                _manifest["version"].pop(_version, None)

        self._save_manifest(_manifest)

    def list(self) -> t.List[str]:
        _manifest = self._get_manifest()
        _version = self.uri.object.version

        if _version:
            return _manifest["versions"].get(_version, {}).keys()
        else:
            return _manifest["tags"].keys()
