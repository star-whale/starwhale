from __future__ import annotations

import os
import typing as t
from abc import ABCMeta
from copy import deepcopy
from pathlib import Path
from collections import defaultdict

import yaml
from fs import open_fs
from loguru import logger
from fs.copy import copy_fs, copy_file
from fs.walk import Walker

from starwhale.utils import console
from starwhale.consts import (
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_COPY_WORKERS,
    DEFAULT_STARWHALE_API_VERSION,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import move_dir, ensure_dir
from starwhale.base.type import URIType, BundleType, EvalTaskType, InstanceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.load import import_cls
from starwhale.utils.venv import SUPPORTED_PIP_REQ
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import ExistedError, NoSupportError, FileFormatError
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.bundle_copy import BundleCopy

from .store import ModelStorage


class ModelRunConfig(object):

    # TODO: use attr to tune class
    def __init__(
        self,
        ppl: str,
        runtime: str = "",
        pkg_data: t.Union[t.List[str], None] = None,
        exclude_pkg_data: t.Union[t.List[str], None] = None,
        envs: t.Union[t.List[str], None] = None,
        **kw: t.Any,
    ):
        self.ppl = ppl.strip()
        self.runtime = runtime.strip()
        self.pkg_data = pkg_data or []
        self.exclude_pkg_data = exclude_pkg_data or []
        self.envs = envs or []
        self.kw = kw

        self._do_validate()

    def _do_validate(self) -> None:
        if not self.ppl:
            raise FileFormatError("need ppl field")

    def __str__(self) -> str:
        return f"Model Run Config: ppl -> {self.ppl}"

    def __repr__(self) -> str:
        return f"Model Run Config: ppl -> {self.ppl}, runtime -> {self.runtime}"

    def as_dict(self) -> t.Dict[str, t.Any]:
        return deepcopy(self.__dict__)


class ModelConfig(object):

    # TODO: use attr to tune class
    def __init__(
        self,
        name: str,
        model: t.List[str],
        config: t.List[str],
        run: t.Dict[str, t.Any],
        desc: str = "",
        tag: t.List[str] = [],
        version: str = DEFAULT_STARWHALE_API_VERSION,
        **kw: t.Any,
    ):

        # TODO: format model name
        self.name = name
        self.model = model or []
        self.config = config or []
        # TODO: support artifacts: local or remote
        self.run = ModelRunConfig(**run)
        self.desc = desc
        self.tag = tag
        self.version = version
        self.kw = kw

        self._do_validate()

    def _do_validate(self) -> None:
        # TODO: use attr validator
        if not self.model:
            raise FileFormatError("need at least one model")

        # TODO: add more validation
        # TODO: add name check

    @classmethod
    def create_by_yaml(cls, path: Path) -> ModelConfig:
        c = yaml.safe_load(path.open())
        return cls(**c)

    def __str__(self) -> str:
        return f"Model Config: {self.name}"

    def __repr__(self) -> str:
        return f"Model Config: name -> {self.name}, model-> {self.model}"

    def as_dict(self) -> t.Dict[str, t.Any]:
        _r = deepcopy(self.__dict__)
        _r["run"] = self.run.as_dict()
        return _r


class Model(BaseBundle):
    __metaclass__ = ABCMeta

    def __str__(self) -> str:
        return f"Starwhale Model: {self.uri}"

    def eval(self) -> None:
        pass

    def ppl(
        self, workdir: Path, yaml_name: str = DefaultYAMLName.MODEL, **kw: t.Any
    ) -> None:
        pass

    def cmp(
        self, workdir: Path, yaml_name: str = DefaultYAMLName.MODEL, **kw: t.Any
    ) -> None:
        pass

    @classmethod
    def get_model(cls, uri: URI) -> Model:
        _cls = cls._get_cls(uri)
        return _cls(uri)

    @classmethod
    def _get_cls(cls, uri: URI) -> t.Union[t.Type[StandaloneModel], t.Type[CloudModel]]:  # type: ignore
        if uri.instance_type == InstanceType.STANDALONE:
            return StandaloneModel
        elif uri.instance_type == InstanceType.CLOUD:
            return CloudModel
        else:
            raise NoSupportError(f"model uri:{uri}")

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
        bc = BundleCopy(src_uri, dest_uri, URIType.MODEL, force)
        bc.do()


class StandaloneModel(Model, LocalStorageBundleMixin):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.STANDALONE
        self.store = ModelStorage(uri)
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest classget_conda_env

    @classmethod
    def eval_user_handler(
        cls,
        typ: str,
        workdir: Path,
        yaml_name: str = DefaultYAMLName.MODEL,
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        from starwhale.api._impl.model import _RunConfig

        _mp = workdir / yaml_name
        _model_config = cls._load_model_config(_mp)
        _handler = _model_config.run.ppl

        _RunConfig.set_env(kw)
        console.print(f"try to import {_handler}@{workdir}...")
        _cls = import_cls(workdir, _handler)
        _obj = _cls()

        if typ == EvalTaskType.CMP:
            _obj._starwhale_internal_run_cmp()
        else:
            _obj._starwhale_internal_run_ppl()

        console.print(f":clap: finish run {typ}: {_obj}")

    def info(self) -> t.Dict[str, t.Any]:
        return self._get_bundle_info()

    def history(self) -> t.List[t.Dict[str, t.Any]]:
        _r = []
        for _version, _path in self.store.iter_bundle_history():
            _manifest = ModelStorage.get_manifest_by_path(
                _path, BundleType.MODEL, URIType.MODEL
            )

            _r.append(
                dict(
                    name=self.name,
                    version=_version,
                    path=str(_path.resolve()),
                    created_at=_manifest["created_at"],
                    size=_path.stat().st_size,
                    runtime=_manifest["user_raw_config"]
                    .get("run", {})
                    .get("runtime", "--"),
                )
            )
        return _r

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: remove workdir
        # TODO: remove by tag
        # TODO: remove latest tag
        return move_dir(self.store.loc, self.store.recover_loc, force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support short version to recover, today only support full-version
        dest_path = (
            self.store.bundle_dir / f"{self.uri.object.version}{BundleType.MODEL}"
        )
        return move_dir(self.store.recover_loc, dest_path, force)

    @classmethod
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        rs = defaultdict(list)
        for (
            _rt_name,
            _rt_version,
            _path,
            _is_removed,
        ) in ModelStorage.iter_all_bundles(
            project_uri,
            bundle_type=BundleType.MODEL,
            uri_type=URIType.MODEL,
        ):
            _manifest = ModelStorage.get_manifest_by_path(
                _path, BundleType.MODEL, URIType.MODEL
            )

            rs[_rt_name].append(
                {
                    "name": _manifest["name"],
                    "version": _rt_version,
                    "path": str(_path.absolute()),
                    "size": _path.stat().st_size,
                    "is_removed": _is_removed,
                    "runtime": _manifest["user_raw_config"]
                    .get("run", {})
                    .get("runtime", "--"),
                    "created_at": _manifest["created_at"],
                }
            )
        return rs, {}

    def build(
        self, workdir: Path, yaml_name: str = DefaultYAMLName.MODEL, **kw: t.Any
    ) -> None:
        _mp = workdir / yaml_name
        _model_config = self._load_model_config(_mp)

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._copy_src,
                15,
                "copy src",
                dict(workdir=workdir, yaml_name=yaml_name, model_config=_model_config),
            ),
            (
                self._render_manifest,
                5,
                "render manifest",
                dict(user_raw_config=_model_config.as_dict()),
            ),
            (self._make_tar, 20, "build model bundle", dict(ftype=BundleType.MODEL)),
        ]
        run_with_progress_bar("model bundle building...", operations)

    @classmethod
    def _load_model_config(cls, yaml_path: Path) -> ModelConfig:
        cls._do_validate_yaml(yaml_path)
        _config = ModelConfig.create_by_yaml(yaml_path)

        if not _config.model:
            raise FileFormatError("model yaml no model")

        for _fpath in _config.model + _config.config:
            if not (yaml_path.parent / _fpath).exists():
                raise FileFormatError(f"model - {_fpath} is not existed")

        # TODO: add more model.yaml section validation
        # TODO: add 'swcli model check' cmd

        cls._load_config_envs(_config)
        return _config

    def _prepare_snapshot(self) -> None:
        logger.info("[step:prepare-snapshot]prepare model snapshot dirs...")

        if self.store.snapshot_workdir.exists():
            raise ExistedError(str(self.store.snapshot_workdir))

        ensure_dir(self.store.snapshot_workdir)
        ensure_dir(self.store.src_dir)

        # TODO: cleanup garbage dir
        # TODO: add lock/flag file for gc

        console.print(
            f":file_folder: workdir: [underline]{self.store.snapshot_workdir}[/]"
        )

    def _copy_src(
        self, workdir: Path, yaml_name: str, model_config: ModelConfig
    ) -> None:
        logger.info(
            f"[step:copy]start to copy src {workdir} -> {self.store.src_dir} ..."
        )
        console.print(":thumbs_up: try to copy source code files...")
        _mc = model_config

        workdir_fs = open_fs(str(workdir.resolve()))
        snapshot_fs = open_fs(str(self.store.snapshot_workdir.resolve()))
        src_fs = open_fs(str(self.store.src_dir.resolve()))
        # TODO: support exclude dir
        # TODO: support glob pkg_data
        # TODO: ignore some folders, such as __pycache__
        copy_file(workdir_fs, yaml_name, snapshot_fs, DefaultYAMLName.MODEL)
        copy_fs(
            workdir_fs,
            src_fs,
            walker=Walker(
                filter=["*.py", yaml_name] + SUPPORTED_PIP_REQ + _mc.run.pkg_data,
                exclude_dirs=_mc.run.exclude_pkg_data,
            ),
            workers=DEFAULT_COPY_WORKERS,
        )

        for _fname in _mc.config + _mc.model:
            copy_file(workdir_fs, _fname, src_fs, _fname)

        logger.info("[step:copy]finish copy files")

    @classmethod
    def _load_config_envs(cls, _config: ModelConfig) -> None:
        for _env in _config.run.envs:
            _env = _env.strip()
            if not _env:
                continue
            _t = _env.split("=", 1)
            _k, _v = _t[0], "".join(_t[1:])

            if _k not in os.environ:
                os.environ[_k] = _v

    def extract(self, force: bool = False, target: t.Union[str, Path] = "") -> Path:
        return self._do_extract(force, target)


class CloudModel(Model, CloudRequestMixed):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.CLOUD
