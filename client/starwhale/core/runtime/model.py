from __future__ import annotations

import typing as t
from abc import ABCMeta
from copy import deepcopy
from pathlib import Path
from collections import defaultdict

import yaml
from loguru import logger

from starwhale.utils import console, validate_obj_name
from starwhale.consts import (
    PythonRunEnv,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_PYTHON_VERSION,
    DEFAULT_SW_TASK_RUN_IMAGE,
)
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import move_dir, ensure_dir, ensure_file, get_path_created_time
from starwhale.base.type import URIType, BundleType, InstanceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.http import ignore_error
from starwhale.utils.venv import (
    detect_pip_req,
    create_python_env,
    restore_python_env,
    activate_python_env,
    dump_python_dep_env,
    DUMP_USER_PIP_REQ_FNAME,
)
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import ExistedError, NoSupportError, ConfigFormatError
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.bundle_copy import BundleCopy

from .store import RuntimeStorage


class RuntimeConfig(object):
    def __init__(
        self,
        name: str,
        mode: str,
        python_version: str,
        pip_req: str = DUMP_USER_PIP_REQ_FNAME,
        base_image: str = DEFAULT_SW_TASK_RUN_IMAGE,
        **kw: t.Any,
    ) -> None:
        self.name = name.strip().lower()
        self.mode = mode
        self.python_version = python_version.strip()
        self.pip_req = pip_req
        self.base_image = base_image
        self.kw = kw

        self._do_validate()

    def _do_validate(self) -> None:
        ok, reason = validate_obj_name(self.name)
        if not ok:
            raise ConfigFormatError(f"name:{self.name}, reason:{reason}")

        if self.mode not in (PythonRunEnv.CONDA, PythonRunEnv.VENV):
            raise ConfigFormatError(f"{self.mode} no support")

        if not self.python_version.startswith("3."):
            raise ConfigFormatError(f"only support Python3, set {self.python_version}")

        # TODO: add more validators

    @classmethod
    def create_by_yaml(cls, path: Path) -> RuntimeConfig:
        c = yaml.safe_load(path.open())
        return cls(**c)

    def as_dict(self) -> t.Dict[str, t.Any]:
        return deepcopy(self.__dict__)


class Runtime(BaseBundle):
    __metaclass__ = ABCMeta

    @classmethod
    def restore(cls, workdir: Path) -> None:
        StandaloneRuntime.restore(workdir)

    @classmethod
    def create(
        cls,
        workdir: t.Union[str, Path],
        name: str,
        python_version: str = DEFAULT_PYTHON_VERSION,
        mode: str = PythonRunEnv.VENV,
        base_image: str = DEFAULT_SW_TASK_RUN_IMAGE,
        force: bool = False,
    ) -> None:
        StandaloneRuntime.create(
            workdir=workdir,
            name=name,
            python_version=python_version,
            mode=mode,
            base_image=base_image,
            force=force,
        )

    @classmethod
    def get_runtime(cls, uri: URI) -> Runtime:
        _cls = cls._get_cls(uri)
        return _cls(uri)

    @classmethod
    def _get_cls(  # type: ignore
        cls,
        uri: URI,
    ) -> t.Union[t.Type[StandaloneRuntime], t.Type[CloudRuntime]]:
        if uri.instance_type == InstanceType.STANDALONE:
            return StandaloneRuntime
        elif uri.instance_type == InstanceType.CLOUD:
            return CloudRuntime
        else:
            raise NoSupportError(f"runtime uri:{uri}")

    def __str__(self) -> str:
        return f"Starwhale Runtime: {self.uri}"

    @classmethod
    def copy(cls, src_uri: str, dest_uri: str, force: bool = False) -> None:
        bc = BundleCopy(src_uri, dest_uri, URIType.RUNTIME, force)
        bc.do()


class StandaloneRuntime(Runtime, LocalStorageBundleMixin):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.STANDALONE
        self.store = RuntimeStorage(uri)
        self.tag = StandaloneTag(uri)
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest classget_conda_env

    def info(self) -> t.Dict[str, t.Any]:
        return self._get_bundle_info()

    def add_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.add(tags, quiet)

    def remove_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.remove(tags, quiet)

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return move_dir(self.store.loc, self.store.recover_loc, force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support short version to recover, today only support full-version
        dest_path = (
            self.store.bundle_dir / f"{self.uri.object.version}{BundleType.RUNTIME}"
        )
        return move_dir(self.store.recover_loc, dest_path, force)

    def history(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:

        # TODO: time order
        _r = []
        for _bf in self.store.iter_bundle_history():
            _r.append(
                dict(
                    version=_bf.version,
                    path=str(_bf.path.resolve()),
                    created_at=get_path_created_time(_bf.path),
                    size=_bf.path.stat().st_size,
                    tags=_bf.tags,
                )
            )
        return _r, {}

    def build(
        self,
        workdir: Path,
        yaml_name: str = DefaultYAMLName.RUNTIME,
        **kw: t.Any,
    ) -> None:
        # TODO: tune for no runtime.yaml file
        _mp = workdir / yaml_name
        _swrt_config = self._load_runtime_config(_mp)
        _pip_req_path = detect_pip_req(workdir, _swrt_config.pip_req)
        _python_version = _swrt_config.python_version

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._dump_dep,
                50,
                "dump python depency",
                dict(
                    gen_all_bundles=kw.get("gen_all_bundles", False),
                    pip_req_path=_pip_req_path,
                    python_version=_python_version,
                    mode=_swrt_config.mode,
                ),
            ),
            (
                self._dump_base_image,
                5,
                "dump base image",
                dict(config=_swrt_config),
            ),
            (
                self._render_manifest,
                5,
                "render manifest",
                dict(user_raw_config=_swrt_config.as_dict()),
            ),
            (self._make_tar, 20, "make runtime bundle", dict(ftype=BundleType.RUNTIME)),
            (self._make_latest_tag, 5, "make latest tag"),
        ]
        run_with_progress_bar("runtime bundle building...", operations)

    def _dump_base_image(self, config: RuntimeConfig) -> None:
        base_image = config.base_image or DEFAULT_SW_TASK_RUN_IMAGE
        console.print(
            f":rainbow: runtime docker image: [red]{base_image}[/]  :rainbow:"
        )
        self._manifest["base_image"] = base_image

    def _dump_dep(
        self,
        gen_all_bundles: bool = False,
        pip_req_path: str = DUMP_USER_PIP_REQ_FNAME,
        python_version: str = DEFAULT_PYTHON_VERSION,
        mode: str = PythonRunEnv.AUTO,
    ) -> None:
        logger.info("[step:dep]start dump python dep...")
        _dep = dump_python_dep_env(
            dep_dir=self.store.snapshot_workdir / "dep",
            pip_req_fpath=pip_req_path,
            gen_all_bundles=gen_all_bundles,
            expected_runtime=python_version,
            mode=mode,
        )
        self._manifest["dep"] = _dep
        logger.info("[step:dep]finish dump dep")

    def _prepare_snapshot(self) -> None:
        logger.info("[step:prepare-snapshot]prepare runtime snapshort dirs...")

        # TODO: graceful clear?
        if self.store.snapshot_workdir.exists():
            raise ExistedError(str(self.store.snapshot_workdir))

        ensure_dir(self.store.snapshot_workdir)
        ensure_dir(self.store.venv_dir)
        ensure_dir(self.store.conda_dir)

        console.print(
            f":file_folder: workdir: [underline]{self.store.snapshot_workdir}[/]"
        )

    def _load_runtime_config(self, path: Path) -> RuntimeConfig:
        self._do_validate_yaml(path)
        return RuntimeConfig.create_by_yaml(path)

    def extract(self, force: bool = False, target: t.Union[str, Path] = "") -> Path:
        return self._do_extract(force, target)

    @classmethod
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        rs = defaultdict(list)
        for _bf in RuntimeStorage.iter_all_bundles(
            project_uri, bundle_type=BundleType.RUNTIME, uri_type=URIType.RUNTIME
        ):
            # TODO: add more manifest info
            rs[_bf.name].append(
                {
                    "version": _bf.version,
                    "path": str(_bf.path.absolute()),
                    "created_at": get_path_created_time(_bf.path),
                    "size": _bf.path.stat().st_size,
                    "is_removed": _bf.is_removed,
                    "tags": _bf.tags,
                }
            )
        return rs, {}

    @classmethod
    def create(
        cls,
        workdir: t.Union[str, Path],
        name: str,
        python_version: str = DEFAULT_PYTHON_VERSION,
        mode: str = PythonRunEnv.VENV,
        base_image: str = DEFAULT_SW_TASK_RUN_IMAGE,
        force: bool = False,
    ) -> None:
        workdir = Path(workdir).absolute()
        ensure_dir(workdir)
        # TODO: auto install requirements.txt
        _id = create_python_env(
            mode=mode,
            name=name,
            workdir=workdir,
            python_version=python_version,
            force=force,
        )

        config = RuntimeConfig(
            name=name, mode=mode, python_version=python_version, base_image=base_image
        )
        cls.render_runtime_yaml(config, workdir, force)
        activate_python_env(mode=mode, identity=_id)

    @staticmethod
    def render_runtime_yaml(
        config: RuntimeConfig, workdir: Path, force: bool = False
    ) -> None:
        _rm = workdir / DefaultYAMLName.RUNTIME

        if _rm.exists() and not force:
            raise ExistedError(f"{_rm} was already existed")

        ensure_dir(workdir)
        ensure_file(_rm, yaml.safe_dump(config.as_dict(), default_flow_style=False))

    @classmethod
    def restore(cls, workdir: Path) -> None:
        if not (
            workdir.exists()
            and (workdir / DEFAULT_MANIFEST_NAME).exists()
            and (workdir / "dep").exists()
        ):
            raise NoSupportError("only support swrt extract workdir")

        _manifest = yaml.safe_load((workdir / DEFAULT_MANIFEST_NAME).open())
        restore_python_env(
            workdir, _manifest["dep"]["env"], _manifest["dep"]["local_gen_env"]
        )


class CloudRuntime(CloudRequestMixed, Runtime):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.CLOUD

    @classmethod
    @ignore_error(({}, {}))
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        crm = CloudRequestMixed()
        return crm._fetch_bundle_all_list(project_uri, URIType.RUNTIME, page, size)
