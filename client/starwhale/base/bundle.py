from __future__ import annotations

import os
import abc
import typing as t
import tarfile
import platform
from abc import ABCMeta, abstractmethod
from pathlib import Path
from contextlib import ExitStack

import yaml
from fs.walk import Walker
from typing_extensions import Protocol

from starwhale.utils import console, now_str, gen_uniq_version
from starwhale.consts import (
    YAML_TYPES,
    CREATED_AT_KEY,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
    SW_IGNORE_FILE_NAME,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import move_dir, empty_dir, ensure_dir, ensure_file, extract_tar
from starwhale.base.store import BaseStorage, BundleField
from starwhale.utils.venv import SUPPORTED_PIP_REQ
from starwhale.utils.error import FileTypeError, NotFoundError, MissingFieldError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.models.base import ListFilter
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource


class BaseBundle(metaclass=ABCMeta):
    def __init__(self, uri: Resource) -> None:
        self.uri = uri
        self.name = self.uri.name
        self.sw_config = SWCliConfigMixed()
        self.yaml_name = ""

    @abstractmethod
    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    @abstractmethod
    def list_tags(self) -> t.List[str]:
        raise NotImplementedError

    @abstractmethod
    def add_tags(
        self, tags: t.List[str], ignore_errors: bool = False, force: bool = True
    ) -> None:
        raise NotImplementedError

    @abstractmethod
    def remove_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
        raise NotImplementedError

    @abstractmethod
    def history(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.List[t.Dict[str, t.Any]]:
        raise NotImplementedError

    @classmethod
    def _list(
        cls,
        project_uri: Project,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.List[str]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        _cls = cls._get_cls(project_uri)
        _filter = cls.get_list_filter(filters)
        return _cls.list(project_uri, page, size, _filter)  # type: ignore

    @classmethod
    def get_list_filter(
        cls,
        filters: t.Union[t.List[str], None],
    ) -> ListFilter | None:
        if filters is None:
            return None

        if isinstance(filters, dict):
            fs = filters
        else:
            fs = dict()
            for _f in filters:
                _item = _f.split("=", 1)
                fs[_item[0]] = _item[1] if len(_item) > 1 else ""
        ret = ListFilter()
        if "name" in fs:
            ret.name = fs["name"]
        if "version" in fs:
            ret.version = fs["version"]
        if "latest" in fs:
            ret.latest = True
        if "owner" in fs:
            ret.owner = fs["owner"]

        return ret

    @classmethod
    def do_bundle_filter(
        cls, bundle_field: BundleField, filters: t.Optional[ListFilter] = None
    ) -> bool:
        if filters is None:
            return True
        _name = filters.name
        if _name and not bundle_field.name.startswith(_name):
            return False
        if filters.latest and "latest" not in bundle_field.tags:
            return False

        return True

    @classmethod
    @abc.abstractmethod
    def _get_cls(cls, uri: Project) -> t.Any:
        raise NotImplementedError

    @classmethod
    def copy(cls, src_uri: Resource, dest_uri: str) -> None:
        raise NotImplementedError

    def extract(self, force: bool = False, target: t.Union[str, Path] = "") -> Path:
        raise NotImplementedError

    def build(self, *args: t.Any, **kwargs: t.Any) -> None:
        self.store.building = True  # type: ignore

        # use a temp dir to build resources
        # and mv results to dst dir to prevent leaving garbage when interrupt
        def when_exit() -> None:
            src = self.store.snapshot_workdir  # type: ignore
            self.store.building = False  # type: ignore
            dst = self.store.snapshot_workdir  # type: ignore
            ensure_dir(dst.parent)
            os.rename(src, dst)
            console.print(f":100: finish gen resource @ {dst}")

        with ExitStack() as stack:
            stack.callback(when_exit)
            kwargs["yaml_name"] = kwargs.get("yaml_name", self.yaml_name)
            self.buildImpl(*args, **kwargs)

    def buildImpl(self, *args: t.Any, **kwargs: t.Any) -> None:
        raise NotImplementedError

    @property
    def version(self) -> str:
        return getattr(self, "_version", "") or self.uri.version


# https://mypy.readthedocs.io/en/latest/more_types.html#mixin-classes
class LocalStorageBundleProtocol(Protocol):
    uri: Resource
    _version: str
    store: BaseStorage
    tag: StandaloneTag


class LocalStorageBundleMixin(LocalStorageBundleProtocol):
    def __init__(self) -> None:
        self._manifest: t.Dict[str, t.Any] = {}

    def _render_manifest(self) -> None:
        self._manifest["build"] = dict(
            os=platform.system(),
            sw_version=STARWHALE_VERSION,
        )
        # TODO: add signature for import files: model, config
        _fpath = self.store.snapshot_workdir / DEFAULT_MANIFEST_NAME
        ensure_file(_fpath, yaml.safe_dump(self._manifest, default_flow_style=False))

    def _gen_version(self) -> None:
        if not getattr(self, "_version", ""):
            self._version = gen_uniq_version()

        self.uri.version = self._version
        console.debug(f":new: version {self._version[:SHORT_VERSION_CNT]}")
        self._manifest["version"] = self._version
        self._manifest[CREATED_AT_KEY] = now_str()

    def _make_tags(self, tags: t.List[str] | None = None) -> None:
        self.tag.add(tags or [])
        self.tag.add_fast_tag()

    def _make_tar(self, ftype: str = "") -> None:
        out = self.store.bundle_dir / f"{self._version}{ftype}"
        ensure_dir(self.store.bundle_dir)

        with tarfile.open(out, "w:") as tar:
            tar.add(str(self.store.snapshot_workdir), arcname="")

        console.print(f":butterfly: {ftype} bundle:{out}")

    @classmethod
    def _do_validate_yaml(cls, path: Path) -> None:
        # add more yaml validators
        if not path.exists():
            raise NotFoundError(str(path))

        if not path.name.endswith(YAML_TYPES):
            raise FileTypeError(f"{path} file type is not yaml|yml")

    def _do_extract(self, force: bool = False, target: t.Union[str, Path] = "") -> Path:
        _store = self.store
        _uri = self.uri

        if not _uri.version:
            raise MissingFieldError("no version")

        _target: Path = Path(target) if target else _store.snapshot_workdir

        if (
            _target.exists()
            and (_target / DEFAULT_MANIFEST_NAME).exists()
            and not force
        ):
            console.print(f":joy_cat: {_target} existed, skip extract model bundle")
        else:
            extract_tar(_store.bundle_path, _target, force)

        return _target

    def _get_src_walker(
        self,
        workdir: Path,
        include_files: t.Optional[t.List[str]] = None,
        exclude_files: t.Optional[t.List[str]] = None,
    ) -> Walker:
        include_files = include_files or []
        exclude_files = exclude_files or []
        _filter = (
            ["*.py", "*.sh", "*.yaml", "*.yml"] + SUPPORTED_PIP_REQ + include_files
        )
        _exclude = exclude_files

        _sw_ignore_path = workdir / SW_IGNORE_FILE_NAME
        if _sw_ignore_path.exists():
            for _l in _sw_ignore_path.read_text().splitlines():
                _l = _l.strip()
                if not _l or _l.startswith("#"):
                    continue

                _exclude.append(_l)

        # Notice: if pass [] as exclude_dirs value, walker will fail
        _exclude = _exclude or None  # type: ignore
        return Walker(filter=_filter, exclude_dirs=_exclude)

    def _do_remove(self, force: bool = False) -> t.Tuple[bool, str]:
        store = self.store

        if force:
            empty_dir(store.loc)
            empty_dir(store.snapshot_workdir)
            return True, ""
        else:
            _ok, _reason = move_dir(store.loc, store.recover_loc, False)
            _ok2, _reason2 = True, ""
            if store.snapshot_workdir.exists():
                _ok2, _reason2 = move_dir(
                    store.snapshot_workdir,
                    store.recover_snapshot_workdir,
                    False,
                )
            return _ok and _ok2, _reason + _reason2
