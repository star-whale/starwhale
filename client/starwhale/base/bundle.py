import os
import typing as t
import tarfile
import platform
from abc import ABCMeta, abstractmethod, abstractclassmethod
from pathlib import Path
from contextlib import ExitStack

import yaml
from loguru import logger
from fs.walk import Walker
from fs.tarfs import TarFS

from starwhale.utils import console, now_str, gen_uniq_version
from starwhale.consts import (
    LATEST_TAG,
    YAML_TYPES,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    SHORT_VERSION_CNT,
    SW_IGNORE_FILE_NAME,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import move_dir, empty_dir, ensure_dir, ensure_file, extract_tar
from starwhale.utils.venv import SUPPORTED_PIP_REQ
from starwhale.utils.error import FileTypeError, NotFoundError, MissingFieldError
from starwhale.utils.config import SWCliConfigMixed

from .uri import URI
from .store import BundleField


class BaseBundle(metaclass=ABCMeta):
    def __init__(self, uri: URI) -> None:
        self.uri = uri
        self.name = self.uri.object.name
        self.sw_config = SWCliConfigMixed()
        self.yaml_name = ""

    @abstractmethod
    def info(self) -> t.Dict[str, t.Any]:
        raise NotImplementedError

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
    def add_tags(self, tags: t.List[str], ignore_errors: bool = False) -> None:
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
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
        filters: t.Optional[t.Union[t.Dict[str, t.Any], t.List[str]]] = None,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        filters = filters or {}
        _cls = cls._get_cls(project_uri)
        _filter = cls.get_filter_dict(filters, cls.get_filter_fields())
        return _cls.list(project_uri, page, size, _filter)  # type: ignore

    @classmethod
    def get_filter_dict(
        cls,
        filters: t.Union[t.Dict[str, t.Any], t.List[str]],
        fields: t.Optional[t.List[str]] = None,
    ) -> t.Dict[str, t.Any]:
        fields = fields or []
        if isinstance(filters, t.Dict):
            return {k: v for k, v in filters.items() if k in fields}

        _filter_dict: t.Dict[str, t.Any] = {}
        for _f in filters:
            _item = _f.split("=", 1)
            if _item[0] in fields:
                _filter_dict[_item[0]] = _item[1] if len(_item) > 1 else ""
        return _filter_dict

    @classmethod
    def get_filter_fields(cls) -> t.List[str]:
        return ["name", "owner", "latest"]

    @classmethod
    def do_bundle_filter(
        cls,
        bundle_field: BundleField,
        filters: t.Union[t.Dict[str, t.Any], t.List[str]],
    ) -> bool:
        filter_dict = cls.get_filter_dict(filters, cls.get_filter_fields())
        _name = filter_dict.get("name")
        if _name and not bundle_field.name.startswith(_name):
            return False
        _latest = filter_dict.get("latest") is not None
        if _latest and "latest" not in bundle_field.tags:
            return False

        return True

    @abstractclassmethod
    def _get_cls(cls, uri: URI) -> t.Any:
        raise NotImplementedError

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
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
            console.print(f"finish gen resource @ {dst}")

        with ExitStack() as stack:
            stack.callback(when_exit)
            kwargs["yaml_name"] = kwargs.get("yaml_name", self.yaml_name)
            self.buildImpl(*args, **kwargs)

    def buildImpl(self, *args: t.Any, **kwargs: t.Any) -> None:
        raise NotImplementedError

    @property
    def version(self) -> str:
        return getattr(self, "_version", "") or self.uri.object.version


class LocalStorageBundleMixin:
    def __init__(self) -> None:
        self._manifest: t.Dict[str, t.Any] = {}

    def _render_manifest(self) -> None:
        self._manifest["build"] = dict(
            os=platform.system(),
            sw_version=STARWHALE_VERSION,
        )
        self._manifest["version"] = self._version  # type: ignore
        self._manifest["created_at"] = now_str()

        # TODO: add signature for import files: model, config
        _fpath = self.store.snapshot_workdir / DEFAULT_MANIFEST_NAME  # type: ignore
        ensure_file(_fpath, yaml.safe_dump(self._manifest, default_flow_style=False))
        logger.info(f"[step:manifest]render manifest: {_fpath}")

    def _gen_version(self) -> None:
        logger.info("[step:version]create version...")
        if not getattr(self, "_version", ""):
            self._version = gen_uniq_version()

        self.uri.object.version = self._version  # type:ignore
        logger.info(f"[step:version]version: {self._version}")
        console.print(f":new: version {self._version[:SHORT_VERSION_CNT]}")  # type: ignore

    def _make_auto_tags(self) -> None:
        self.tag.add([LATEST_TAG], ignore_errors=True)  # type: ignore
        self.tag.add_fast_tag()  # type: ignore

    def _make_tar(self, ftype: str = "") -> None:
        out = self.store.bundle_dir / f"{self._version}{ftype}"  # type: ignore
        ensure_dir(self.store.bundle_dir)  # type: ignore
        logger.info(f"[step:tar]try to tar {out} ...")

        with tarfile.open(out, "w:") as tar:
            tar.add(str(self.store.snapshot_workdir), arcname="")  # type: ignore

        console.print(f":butterfly: {ftype} bundle:{out}")
        logger.info("[step:tar]finish to make bundle tar")

    @classmethod
    def _do_validate_yaml(cls, path: Path) -> None:
        # add more yaml validators
        if not path.exists():
            raise NotFoundError(str(path))

        if not path.name.endswith(YAML_TYPES):
            raise FileTypeError(f"{path} file type is not yaml|yml")

    def _get_bundle_info(self) -> t.Dict[str, t.Any]:
        _uri = self.uri  # type: ignore
        _store = self.store  # type: ignore

        if not _store.bundle_path.exists():
            return {}

        _manifest: t.Dict[str, t.Any] = {
            "uri": _uri.full_uri,
            "project": _uri.project,
            "name": _uri.object.name,
            "snapshot_workdir": str(_store.snapshot_workdir),
            "bundle_path": str(_store.bundle_path),
        }

        if _uri.object.version:
            _tag = StandaloneTag(_uri)
            _manifest["version"] = _uri.object.version
            _manifest["config"] = {}
            _manifest["tags"] = _tag.list()

            if _store.bundle_path.is_dir():
                _manifest["config"].update(_store.manifest)
            else:
                if _store.snapshot_workdir.exists():
                    _manifest["config"].update(_store.manifest)
                elif _store.bundle_path.exists():
                    with TarFS(str(_store.bundle_path)) as tar:
                        with tar.open(DEFAULT_MANIFEST_NAME) as f:
                            _om = yaml.safe_load(f)
                        _manifest["config"].update(_om)
                else:
                    raise NotFoundError(
                        f"{_store.bundle_path} and {_store.snapshot_workdir}"
                    )
        else:
            _manifest["history"] = self.history()  # type: ignore
        return _manifest

    def _do_extract(self, force: bool = False, target: t.Union[str, Path] = "") -> Path:
        _store = self.store  # type: ignore
        _uri = self.uri  # type: ignore

        if not _uri.object.version:
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

        # Notice: if pass [] as exclude_dirs value, walker will failed
        _exclude = _exclude or None  # type: ignore
        return Walker(filter=_filter, exclude_dirs=_exclude)

    def _do_remove(self, force: bool = False) -> t.Tuple[bool, str]:
        from .store import BaseStorage

        store: BaseStorage = self.store  # type: ignore

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
