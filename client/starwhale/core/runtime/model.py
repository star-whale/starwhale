from __future__ import annotations
from copy import deepcopy

import yaml
import typing as t
from abc import ABCMeta
from pathlib import Path
from collections import defaultdict

from loguru import logger

from starwhale.base.uri import URI
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    PythonRunEnv,
    DEFAULT_PYTHON_VERSION,
    DefaultYAMLName,
    DEFAULT_SW_TASK_RUN_IMAGE,
)
from starwhale.base.type import BundleType, InstanceType, URIType
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.bundle_copy import BundleCopy
from starwhale.utils.venv import (
    create_python_env,
    activate_python_env,
    DUMP_USER_PIP_REQ_FNAME,
    detect_pip_req,
    dump_python_dep_env,
    restore_python_env,
)
from starwhale.utils.error import (
    ConfigFormatError,
    ExistedError,
    NoSupportError,
)
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils import console, validate_obj_name
from starwhale.utils.fs import (
    ensure_dir,
    ensure_file,
    get_path_created_time,
    move_dir,
)

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
        force: bool = False,
    ) -> None:
        StandaloneRuntime.create(
            workdir=workdir,
            name=name,
            python_version=python_version,
            mode=mode,
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
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest classget_conda_env

    def info(self) -> t.Dict[str, t.Any]:
        return self._get_bundle_info()

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: remove workdir
        # TODO: remove by tag
        # TODO: remove latest tag
        return move_dir(self.store.loc, self.store.recover_loc, force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support short version to recover, today only support full-version
        dest_path = (
            self.store.bundle_dir / f"{self.uri.object.version}{BundleType.RUNTIME}"
        )
        return move_dir(self.store.recover_loc, dest_path, force)

    def history(self) -> t.List[t.Dict[str, t.Any]]:
        # TODO: time order
        _r = []
        for _version, _path in self.store.iter_bundle_history():
            _r.append(
                dict(
                    version=_version,
                    path=str(_path.resolve()),
                    created_at=get_path_created_time(_path),
                    size=_path.stat().st_size,
                )
            )
        return _r

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
                self._render_manifest,
                5,
                "render manifest",
                dict(user_raw_config=_swrt_config.as_dict()),
            ),
            (self._make_tar, 20, "make runtime bundle", dict(ftype=BundleType.RUNTIME)),
        ]
        run_with_progress_bar("runtime bundle building...", operations)

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
        for (
            _rt_name,
            _rt_version,
            _path,
            _is_removed,
        ) in RuntimeStorage.iter_all_bundles(
            project_uri, bundle_type=BundleType.RUNTIME, uri_type=URIType.RUNTIME
        ):
            # TODO: add more manifest info
            rs[_rt_name].append(
                {
                    "version": _rt_version,
                    "path": str(_path.absolute()),
                    "created_at": get_path_created_time(_path),
                    "size": _path.stat().st_size,
                    "is_removed": _is_removed,
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

        config = RuntimeConfig(name=name, mode=mode, python_version=python_version)
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


class CloudRuntime(Runtime):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.CLOUD
        self.store = RuntimeStorage(uri)

    def info(self) -> t.Dict[str, t.Any]:
        raise NotImplementedError

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        raise NotImplementedError

    def history(self) -> t.List[t.Dict[str, t.Any]]:
        raise NotImplementedError

    @classmethod
    def list(
        cls,
        project_uri: URI,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.Dict[str, t.Any], t.Dict[str, t.Any]]:
        raise NotImplementedError
