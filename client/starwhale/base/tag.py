import typing as t
from pathlib import Path

import yaml

from starwhale.utils import now_str, load_yaml, validate_obj_name
from starwhale.consts import DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import InstanceType
from starwhale.utils.error import (
    FormatError,
    NotFoundError,
    NoSupportError,
    MissingFieldError,
)


class StandaloneTag:
    def __init__(self, uri: URI) -> None:
        self.uri = uri
        self._do_validate()

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
                "fast_tag_seq": -1,
                "tags": {},
                "versions": {},
            }
            ensure_dir(self._manifest_path.parent)
            ensure_file(self._manifest_path, yaml.safe_dump(_dft))
            return _dft
        else:
            return load_yaml(self._manifest_path)  # type: ignore

    def _save_manifest(self, _manifest: t.Dict[str, t.Any]) -> None:
        _manifest["updated_at"] = now_str()
        _manifest["name"] = self.uri.object.name
        _manifest["typ"] = self.uri.object.typ

        ensure_dir(self._manifest_path.parent)
        ensure_file(
            self._manifest_path, yaml.safe_dump(_manifest, default_flow_style=False)
        )

    def add_fast_tag(self) -> None:
        _manifest = self._get_manifest()
        _seq = int(_manifest.get("fast_tag_seq", -1))
        _tag = _seq + 1
        while True:
            if f"v{_tag}" in _manifest["tags"]:
                _tag = _tag + 1
            else:
                break

        _manifest["fast_tag_seq"] = _tag
        self.add(tags=[f"v{_tag}"], manifest=_manifest)

    def add(
        self,
        tags: t.List[str],
        quiet: bool = False,
        manifest: t.Optional[t.Dict] = None,
    ) -> None:
        _manifest = manifest or self._get_manifest()
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

            _pre_version = _manifest["tags"].get(_t, "")
            if _pre_version and _pre_version != _version:
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
                    raise NotFoundError(f"tag:{_t}, version:{_version}")

            _manifest["versions"][_version].pop(_t, None)
            if not _manifest["versions"][_version]:
                _manifest["versions"].pop(_version, None)

        self._save_manifest(_manifest)

    def list(self) -> t.List[str]:
        _manifest = self._get_manifest()
        _version = self.uri.object.version

        if _version:
            _tags = _manifest["versions"].get(_version, {}).keys()
        else:
            _tags = _manifest["tags"].keys()
        return list(_tags)

    @classmethod
    def get_manifest_by_dir(cls, dir: Path) -> t.Dict[str, t.Any]:
        _mf = dir / DEFAULT_MANIFEST_NAME
        if _mf.exists():
            return load_yaml(_mf) or {}
        else:
            return {}
