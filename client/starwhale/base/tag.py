from __future__ import annotations

import re
import typing as t
from pathlib import Path

import yaml

from starwhale.utils import now_str, load_yaml, validate_obj_name
from starwhale.consts import LATEST_TAG, DEFAULT_MANIFEST_NAME
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.error import (
    FormatError,
    NotFoundError,
    NoSupportError,
    MissingFieldError,
)
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.resource import Resource


class StandaloneTag:
    AUTO_INCR_TAG_RE = re.compile(r"^v\d+$")

    def __init__(self, uri: Resource) -> None:
        self.uri = uri
        self._do_validate()

    def _do_validate(self) -> None:
        if not self.uri.instance.is_local:
            raise NoSupportError(self.uri.instance.type)

    @property
    def _manifest_path(self) -> Path:
        return (
            SWCliConfigMixed().rootdir
            / self.uri.project.id
            / self.uri.typ.value
            / self.uri.name
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
        _manifest["name"] = self.uri.name
        _manifest["typ"] = self.uri.typ.value

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
        self.add(tags=[f"v{_tag}", LATEST_TAG], manifest=_manifest)

    def add(
        self,
        tags: t.List[str],
        ignore_errors: bool = False,
        manifest: t.Optional[t.Dict] = None,
        force: bool = False,
    ) -> None:
        _manifest = manifest or self._get_manifest()
        _version = self.uri.version

        # TODO: support to force add tag for the used by the other version, current skip the used validation for the Standalone instance.

        if not _version and not ignore_errors:
            raise MissingFieldError(f"uri version, {self.uri}")

        for _t in tags:
            _t = _t.strip()
            if not _t:
                continue

            _ok, _reason = validate_obj_name(_t)
            if not _ok:
                if ignore_errors:
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

    def remove(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        _manifest = self._get_manifest()
        for _t in tags:
            _version = _manifest["tags"].pop(_t, "")

            if _version not in _manifest["versions"]:
                if ignore_errors:
                    continue
                else:
                    raise NotFoundError(f"tag:{_t}, version:{_version}")

            _manifest["versions"][_version].pop(_t, None)
            if not _manifest["versions"][_version]:
                _manifest["versions"].pop(_version, None)

        self._save_manifest(_manifest)

    def __iter__(self) -> t.Generator[str, None, None]:
        for tag in self.list():
            yield tag

    def list(self) -> t.List[str]:
        _manifest = self._get_manifest()
        _version = self.uri.version

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

    @classmethod
    def is_auto_incr_tag(cls, tag: str) -> bool:
        return bool(cls.AUTO_INCR_TAG_RE.match(tag))

    @classmethod
    def is_builtin_tag(cls, tag: str) -> bool:
        return tag == LATEST_TAG or cls.is_auto_incr_tag(tag)

    @classmethod
    def check_tags_validation(
        cls, tags: t.List[str] | None, forbid_builtin_tags: bool = True
    ) -> None:
        tags = tags or []
        for _t in tags:
            _t = _t.strip()
            _ok, _reason = validate_obj_name(_t)
            if not _ok:
                raise FormatError(f"{_t}, reason:{_reason}")

            if forbid_builtin_tags and cls.is_builtin_tag(_t):
                raise FormatError(f"tag:{_t} is builtin, can not be used")
