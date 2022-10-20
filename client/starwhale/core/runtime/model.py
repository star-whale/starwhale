from __future__ import annotations

import os
import shutil
import typing as t
import platform
import tempfile
from abc import ABCMeta
from pathlib import Path
from xml.dom import NotFoundErr
from collections import defaultdict

import yaml
import jinja2
from fs import open_fs
from loguru import logger
from fs.copy import copy_fs, copy_file

from starwhale.utils import (
    docker,
    console,
    load_yaml,
    in_container,
    validate_obj_name,
    make_dir_gitignore,
    get_downloadable_sw_version,
)
from starwhale.consts import (
    SupportOS,
    LATEST_TAG,
    SupportArch,
    PythonRunEnv,
    SW_IMAGE_FMT,
    DEFAULT_PROJECT,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    SW_PYPI_PKG_NAME,
    DEFAULT_PAGE_SIZE,
    ENV_SW_IMAGE_REPO,
    DEFAULT_IMAGE_REPO,
    STANDALONE_INSTANCE,
    SW_DEV_DUMMY_VERSION,
    DEFAULT_CONDA_CHANNEL,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import move_dir, ensure_dir, ensure_file, get_path_created_time
from starwhale.base.type import (
    URIType,
    BundleType,
    InstanceType,
    RuntimeArtifactType,
    RuntimeLockFileType,
)
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.http import ignore_error
from starwhale.utils.venv import (
    is_venv,
    is_conda,
    conda_export,
    get_base_prefix,
    get_conda_pybin,
    venv_install_req,
    conda_install_req,
    create_python_env,
    install_starwhale,
    get_python_version,
    package_python_env,
    restore_python_env,
    activate_python_env,
    pip_freeze_by_pybin,
    guess_current_py_env,
    trunc_python_version,
    check_valid_venv_prefix,
    get_user_python_version,
    check_valid_conda_prefix,
    get_python_version_by_bin,
    render_python_env_activate,
)
from starwhale.base.bundle import BaseBundle, LocalStorageBundleMixin
from starwhale.utils.error import (
    FormatError,
    ExistedError,
    NotFoundError,
    NoSupportError,
    ConfigFormatError,
    MissingFieldError,
    UnExpectedConfigFieldError,
)
from starwhale.utils.progress import run_with_progress_bar
from starwhale.base.bundle_copy import BundleCopy

from .store import RuntimeStorage

RUNTIME_API_VERSION = "1.1"
_TEMPLATE_DIR = Path(__file__).parent / "template"

_t_mixed_str_list = t.Union[t.List[str], str]
_list: t.Callable[[_t_mixed_str_list], t.List[str]] = (
    lambda _x: _x if isinstance(_x, (list, tuple)) else [_x]
)

_SUPPORT_CUDA = ["11.3", "11.4", "11.5", "11.6", "11.7"]
_SUPPORT_CUDNN = {"8": {"support_cuda_versions": ["11.3", "11.4", "11.5", "11.6"]}}
_SUPPORT_PYTHON_VERSIONS = ["3.7", "3.8", "3.9", "3.10"]


class DockerEnv(ASDictMixin):
    def __init__(self, **kwargs: t.Any):
        self.image = kwargs.get("image", "")

    def __str__(self) -> str:
        if self.image:
            return f"image:{self.image}"
        return "emtpy"

    __repr__ = __str__


class Environment(ASDictMixin):
    def __init__(
        self,
        arch: _t_mixed_str_list = "",
        os: str = SupportOS.UBUNTU,
        python: str = "",
        cuda: str = "",
        cudnn: str = "",
        **kw: t.Any,
    ) -> None:
        self.arch = _list(arch)
        self.os = os.lower()

        if not python:
            python = get_python_version()
        self.python = trunc_python_version(str(python))
        self.cuda = str(cuda).strip()
        self.cudnn = str(cudnn).strip()
        self.docker = DockerEnv(**kw.get("docker", {}))

        self._do_validate()

    def _do_validate(self) -> None:
        if self.os not in (SupportOS.UBUNTU,):
            raise NoSupportError(f"environment.os {self.os}")

        if self.python not in _SUPPORT_PYTHON_VERSIONS:
            raise ConfigFormatError(
                f"only support Python[{_SUPPORT_PYTHON_VERSIONS}], set {self.python}"
            )

        if self.cuda and self.cuda not in _SUPPORT_CUDA:
            raise NoSupportError(
                f"cuda {self.cuda} no support[supported list: {_SUPPORT_CUDA}]"
            )

        if self.cudnn and self.cudnn not in _SUPPORT_CUDNN:
            raise NoSupportError(
                f"cudnn {self.cudnn} no support[supported list: {_SUPPORT_CUDNN}]"
            )

        if self.cudnn and not self.cuda:
            raise MissingFieldError("cuda is the precondition for cudnn")

        if (
            self.cuda
            and self.cudnn
            and self.cuda not in _SUPPORT_CUDNN[self.cudnn]["support_cuda_versions"]
        ):
            raise NoSupportError(f"cuda:{self.cuda} no support cudnn:{self.cudnn}")

    def __str__(self) -> str:
        return f"Starwhale Runtime Environment: {self.os}-{self.arch}-python:{self.python}-cuda:{self.cuda}-cudnn:{self.cudnn}-docker:{self.docker}"

    __repr__ = __str__


class Dependencies(ASDictMixin):
    def __init__(self, deps: t.Optional[t.List[str]] = None) -> None:
        deps = deps or []

        self.pip_pkgs: t.List[str] = []
        self.pip_files: t.List[str] = []
        self.conda_pkgs: t.List[str] = []
        self.conda_files: t.List[str] = []
        self.wheels: t.List[str] = []
        self.files: t.List[t.Dict[str, str]] = []
        self._unparsed: t.List[t.Any] = []

        _dmap: t.Dict[str, t.List[t.Any]] = {
            "pip": self.pip_pkgs,
            "conda": self.conda_pkgs,
            "wheels": self.wheels,
            "files": self.files,
        }

        for d in deps:
            if isinstance(d, str):
                if d.endswith((".txt", ".in")):
                    self.pip_files.append(d)
                elif d.endswith((".yaml", ".yml")):
                    self.conda_files.append(d)
                else:
                    self._unparsed.append(d)
            elif isinstance(d, dict):
                for _k, _v in d.items():
                    if _k in _dmap:
                        _dmap[_k].extend(_v)
                    else:
                        self._unparsed.append(d)
            else:
                self._unparsed.append(d)

        if self._unparsed:
            logger.warning(f"unparsed dependencies:{self._unparsed}")

        self._do_validate()

    def _do_validate(self) -> None:
        for _f in self.files:
            if not _f.get("src") or not _f.get("dest"):
                raise FormatError("dependencies.file MUST include src and dest fields.")

    def __str__(self) -> str:
        return f"Starwhale Runtime Dependencies: pip:{len(self.pip_pkgs + self.pip_files)}, conda:{len(self.conda_pkgs + self.conda_files)}, wheels:{len(self.wheels)}, files:{len(self.files)}"

    __repr__ = __str__

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        return super().asdict(ignore_keys=ignore_keys or ["_unparsed"])


class Hooks(ASDictMixin):
    def __init__(self, pre: str = "", post: str = "", **kw: t.Any) -> None:
        self.pre = pre
        self.post = post


class DockerConfig(ASDictMixin):
    def __init__(
        self,
        image: str = "",
        **kw: t.Any,
    ) -> None:
        self.image = image


class PipConfig(ASDictMixin):
    def __init__(
        self,
        index_url: str = "",
        extra_index_url: _t_mixed_str_list = "",
        trusted_host: _t_mixed_str_list = "",
        **kw: t.Any,
    ) -> None:
        self.index_url = index_url
        self.extra_index_url = _list(extra_index_url)
        self.trusted_host = _list(trusted_host)


class CondaConfig(ASDictMixin):
    def __init__(self, channels: t.Optional[t.List[str]] = None, **kw: t.Any) -> None:
        self.channels = channels or [DEFAULT_CONDA_CHANNEL]


class Configs(ASDictMixin):
    def __init__(
        self,
        docker: t.Optional[t.Dict[str, str]] = None,
        conda: t.Optional[t.Dict[str, t.Any]] = None,
        pip: t.Optional[t.Dict[str, str]] = None,
        **kw: t.Any,
    ) -> None:
        self.docker = DockerConfig(**(docker or {}))
        self.conda = CondaConfig(**(conda or {}))
        self.pip = PipConfig(**(pip or {}))


class RuntimeConfig(ASDictMixin):
    def __init__(
        self,
        name: str,
        mode: str = PythonRunEnv.VENV,
        api_version: str = RUNTIME_API_VERSION,
        configs: t.Optional[t.Dict[str, t.Any]] = None,
        hooks: t.Optional[t.Dict[str, t.Any]] = None,
        dependencies: t.Optional[t.List[t.Any]] = None,
        environment: t.Optional[t.Dict[str, t.Any]] = None,
        python_version: str = "",
        pip_req: str = "",
        **kw: t.Any,
    ) -> None:
        self.name = name.strip().lower()
        self.mode = mode
        self.api_version = str(api_version)
        self.configs = Configs(**(configs or {}))
        self.hooks = Hooks(**(hooks or {}))

        environment = environment or {}
        if python_version and not environment.get("python"):
            environment["python"] = python_version
        self.environment = Environment(**environment)

        dependencies = dependencies or []
        if pip_req:
            dependencies.append(pip_req)
        self.dependencies = Dependencies(dependencies)

        self.kw = kw
        self._starwhale_version = get_downloadable_sw_version()
        self._do_validate()

    def _do_validate(self) -> None:
        ok, reason = validate_obj_name(self.name)
        if not ok:
            raise ConfigFormatError(f"name:{self.name}, reason:{reason}")

        if self.mode not in (PythonRunEnv.CONDA, PythonRunEnv.VENV):
            raise ConfigFormatError(f"{self.mode} no support")

        if self.api_version != RUNTIME_API_VERSION:
            raise NoSupportError(f"runtime api_version: {self.api_version}")

    @classmethod
    def create_by_yaml(cls, path: Path) -> RuntimeConfig:
        c = load_yaml(path)
        return cls(**c)

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        return super().asdict(ignore_keys=ignore_keys or ["kw", "_starwhale_version"])


class Runtime(BaseBundle, metaclass=ABCMeta):
    @classmethod
    def restore(cls, workdir: Path, isolated_env_dir: t.Optional[Path] = None) -> None:
        StandaloneRuntime.restore(workdir, isolated_env_dir)

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

    @classmethod
    def activate(cls, path: str = "", uri: str = "") -> None:
        StandaloneRuntime.activate(path, uri)

    @classmethod
    def lock(
        cls,
        target_dir: t.Union[str, Path],
        yaml_name: str = DefaultYAMLName.RUNTIME,
        env_name: str = "",
        env_prefix_path: str = "",
        disable_auto_inject: bool = False,
        stdout: bool = False,
        include_editable: bool = False,
        emit_pip_options: bool = False,
    ) -> None:
        StandaloneRuntime.lock(
            target_dir,
            yaml_name,
            env_name,
            env_prefix_path,
            disable_auto_inject,
            stdout,
            include_editable,
            emit_pip_options,
        )

    def dockerize(
        self,
        tags: t.Optional[t.List[str]] = None,
        platforms: t.Optional[t.List[str]] = None,
        push: bool = True,
        dry_run: bool = False,
        use_starwhale_builder: bool = False,
        reset_qemu_static: bool = False,
    ) -> None:
        raise NotImplementedError


class StandaloneRuntime(Runtime, LocalStorageBundleMixin):
    def __init__(self, uri: URI) -> None:
        super().__init__(uri)
        self.typ = InstanceType.STANDALONE
        self.store = RuntimeStorage(uri)
        self.tag = StandaloneTag(uri)
        self._manifest: t.Dict[str, t.Any] = {}

    def info(self) -> t.Dict[str, t.Any]:
        return self._get_bundle_info()

    def add_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.add(tags, quiet)

    def remove_tags(self, tags: t.List[str], quiet: bool = False) -> None:
        self.tag.remove(tags, quiet)

    def remove(self, force: bool = False) -> t.Tuple[bool, str]:
        return self._do_remove(force)

    def recover(self, force: bool = False) -> t.Tuple[bool, str]:
        # TODO: support short version to recover, today only support full-version
        dest_path = (
            self.store.bundle_dir / f"{self.uri.object.version}{BundleType.RUNTIME}"
        )
        _ok, _reason = move_dir(self.store.recover_loc, dest_path, force)
        _ok2, _reason2 = True, ""
        if self.store.recover_snapshot_workdir.exists():
            _ok2, _reason2 = move_dir(
                self.store.recover_snapshot_workdir, self.store.snapshot_workdir, force
            )
        return _ok and _ok2, _reason + _reason2

    def history(
        self,
        page: int = DEFAULT_PAGE_IDX,
        size: int = DEFAULT_PAGE_SIZE,
    ) -> t.List[t.Dict[str, t.Any]]:
        # TODO: time order
        _r = []
        for _bf in self.store.iter_bundle_history():
            if not _bf.path.is_file():
                continue

            _r.append(
                dict(
                    version=_bf.version,
                    path=str(_bf.path.resolve()),
                    created_at=get_path_created_time(_bf.path),
                    size=_bf.path.stat().st_size,
                    tags=_bf.tags,
                )
            )
        return _r

    def buildImpl(
        self,
        workdir: Path,
        yaml_name: str,
        **kw: t.Any,
    ) -> None:
        # do nothing because we override build function
        pass

    def build(
        self,
        workdir: Path,
        yaml_name: str = DefaultYAMLName.RUNTIME,
        **kw: t.Any,
    ) -> None:
        enable_lock = kw.get("enable_lock", False)
        env_name = kw.get("env_name", "")
        env_prefix_path = kw.get("env_prefix_path", "")
        include_editable = kw.get("include_editable", False)

        if enable_lock:
            console.print(
                f":alien: try to lock environment dependencies to {yaml_name}@{workdir} ..."
            )
            self.lock(
                target_dir=workdir,
                yaml_name=yaml_name,
                env_name=env_name,
                env_prefix_path=env_prefix_path,
                disable_auto_inject=False,
                stdout=False,
                include_editable=include_editable,
            )

        # TODO: tune for no runtime.yaml file
        _swrt_config = self._load_runtime_config(workdir / yaml_name)

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (
                self._dump_context,
                5,
                "dump environment and configs",
                dict(config=_swrt_config),
            ),
            (
                self._lock_environment,
                10,
                "lock environment",
                dict(
                    swrt_config=_swrt_config,
                    enable_lock=enable_lock,
                    env_prefix_path=env_prefix_path,
                    env_name=env_name,
                ),
            ),
            (
                self._dump_dependencies,
                50,
                "dump python dependencies",
                dict(
                    gen_all_bundles=kw.get("gen_all_bundles", False),
                    deps=_swrt_config.dependencies,
                    mode=_swrt_config.mode,
                    include_editable=include_editable,
                    env_prefix_path=env_prefix_path,
                    env_name=env_name,
                ),
            ),
            (
                self._dump_base_image,
                5,
                "dump base image",
                dict(config=_swrt_config),
            ),
            (
                self._copy_src,
                20,
                "dump src files:wheel, native files",
                dict(config=_swrt_config, workdir=workdir, yaml_name=yaml_name),
            ),
            (
                self._render_manifest,
                5,
                "render manifest",
            ),
            (self._make_tar, 20, "make runtime bundle", dict(ftype=BundleType.RUNTIME)),
            (self._make_auto_tags, 5, "make auto tags"),
        ]
        run_with_progress_bar("runtime bundle building...", operations)

    def _dump_context(self, config: RuntimeConfig) -> None:
        # TODO: refactor docker image in environment
        self._manifest["configs"] = config.configs.asdict()

    def _copy_src(
        self,
        config: RuntimeConfig,
        workdir: Path,
        yaml_name: str = DefaultYAMLName.RUNTIME,
    ) -> None:
        workdir_fs = open_fs(str(workdir.resolve()))
        snapshot_fs = open_fs(str(self.store.snapshot_workdir.resolve()))
        copy_file(workdir_fs, yaml_name, snapshot_fs, yaml_name)

        self._manifest["artifacts"] = {
            RuntimeArtifactType.RUNTIME: yaml_name,
            RuntimeArtifactType.WHEELS: [],
            RuntimeArtifactType.DEPEND: [],
            RuntimeArtifactType.FILES: [],
        }

        logger.info("[step:copy-wheels]start to copy wheels...")
        ensure_dir(self.store.snapshot_workdir / RuntimeArtifactType.WHEELS)
        for _fname in config.dependencies.wheels:
            _fpath = workdir / _fname
            if not _fpath.exists():
                logger.warning(f"not found wheel: {_fpath}")
                continue

            _dest = f"{RuntimeArtifactType.WHEELS}/{_fname.lstrip('/')}"
            self._manifest["artifacts"][RuntimeArtifactType.WHEELS].append(_dest)
            copy_file(
                workdir_fs,
                _fname,
                snapshot_fs,
                _dest,
            )

        logger.info("[step:copy-files]start to copy files...")
        ensure_dir(self.store.snapshot_workdir / RuntimeArtifactType.FILES)
        for _f in config.dependencies.files:
            _src = workdir / _f["src"]
            _dest = f"{RuntimeArtifactType.FILES}/{_f['dest'].lstrip('/')}"
            if not _src.exists():
                logger.warning(f"not found src-file: {_src}")
                continue

            _f["_swrt_dest"] = _dest
            self._manifest["artifacts"][RuntimeArtifactType.FILES].append(_f)
            # TODO: auto mkdir target parent dir?
            if _src.is_dir():
                # TODO: support .swignore file
                copy_fs(str(_src), str(self.store.snapshot_workdir / _dest))
            elif _src.is_file():
                ensure_dir((self.store.snapshot_workdir / _dest).parent)
                copy_file(workdir_fs, _f["src"], snapshot_fs, _dest)

        logger.info("[step:copy-deps]start to copy pip/conda requirement files")
        ensure_dir(self.store.snapshot_workdir / RuntimeArtifactType.DEPEND)
        for _fname in config.dependencies.conda_files + config.dependencies.pip_files:
            _fpath = workdir / _fname
            if not _fpath.exists():
                logger.warning(f"not found dependencies: {_fpath}")
                continue

            _dest = f"{RuntimeArtifactType.DEPEND}/{_fname.lstrip('/')}"
            self._manifest["artifacts"][RuntimeArtifactType.DEPEND].append(_dest)
            copy_file(
                workdir_fs,
                _fname,
                snapshot_fs,
                _dest,
            )

    def _dump_base_image(self, config: RuntimeConfig) -> None:
        # prefer using image configured in runtime.yaml
        base_image = config.environment.docker.image

        if not base_image:
            _repo = os.environ.get(ENV_SW_IMAGE_REPO, DEFAULT_IMAGE_REPO)
            _tag = config._starwhale_version or LATEST_TAG
            base_image = SW_IMAGE_FMT.format(repo=_repo, tag=_tag)

            _cuda = config.environment.cuda
            _cudnn = config.environment.cudnn
            _suffix = []
            if _cuda:
                _suffix.append(f"-cuda{_cuda}")

                if _cudnn:
                    _suffix.append(f"-cudnn{_cudnn}")

            base_image += "".join(_suffix)

        console.print(
            f":rainbow: runtime docker image: [red]{base_image}[/]  :rainbow:"
        )
        self._manifest["base_image"] = base_image

    def _lock_environment(
        self,
        swrt_config: RuntimeConfig,
        enable_lock: bool = False,
        env_prefix_path: str = "",
        env_name: str = "",
    ) -> None:
        console.print(":bee: dump environment info...")
        sh_py_env = guess_current_py_env()
        sh_py_ver = get_user_python_version(sh_py_env)

        self._manifest["environment"] = self._manifest.get("environment") or {}

        self._manifest["environment"].update(
            {
                "lock": {
                    "starwhale_version": STARWHALE_VERSION,
                    "system": platform.system(),
                    "shell": {
                        "python_env": sh_py_env,
                        "python_version": sh_py_ver,
                        "use_conda": is_conda(),
                        "use_venv": is_venv(),
                    },
                    "env_prefix_path": env_prefix_path,
                    "env_name": env_name,
                    "use_shell_detection": not (env_prefix_path or env_name),
                },
                "auto_lock_dependencies": enable_lock,
                "python": swrt_config.environment.python,
                "arch": swrt_config.environment.arch,
                "mode": swrt_config.mode,
            }
        )

    def _dump_dependencies(
        self,
        mode: str = PythonRunEnv.AUTO,
        gen_all_bundles: bool = False,
        deps: t.Optional[Dependencies] = None,
        include_editable: bool = False,
        env_prefix_path: str = "",
        env_name: str = "",
    ) -> None:
        console.print("dump dependencies info...")
        deps = deps or Dependencies()
        self._manifest["dependencies"] = {
            "pip_pkgs": deps.pip_pkgs,
            "conda_pkgs": deps.conda_pkgs,
            "pip_files": deps.pip_files,
            "conda_files": deps.conda_files,
            "local_packaged_env": False,
        }

        logger.info("[step:dep]finish dump dep")

        if gen_all_bundles:
            packaged = package_python_env(
                export_dir=self.store.export_dir,
                mode=mode,
                env_prefix_path=env_prefix_path,
                env_name=env_name,
                include_editable=include_editable,
            )
            self._manifest["dependencies"]["local_packaged_env"] = packaged

    def _prepare_snapshot(self) -> None:
        logger.info("[step:prepare-snapshot]prepare runtime snapshot dirs...")

        # TODO: graceful clear?
        if self.store.snapshot_workdir.exists():
            raise ExistedError(str(self.store.snapshot_workdir))

        ensure_dir(self.store.snapshot_workdir)

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
            if not _bf.path.is_file():
                continue

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
    def quickstart_from_uri(
        cls,
        workdir: t.Union[Path, str],
        name: str,
        uri: URI,
        force: bool = False,
        restore: bool = False,
    ) -> None:
        workdir = Path(workdir).absolute()
        ensure_dir(workdir)

        if uri.instance_type == InstanceType.CLOUD:
            console.print(f":cloud: copy runtime from {uri} to local")
            _dest_project_uri = f"{STANDALONE_INSTANCE}/project/{DEFAULT_PROJECT}"
            cls.copy(uri.full_uri, _dest_project_uri, force=force)
            uri = URI(
                f"{_dest_project_uri}/runtime/{uri.object.name}/version/{uri.object.version}",
                expected_type=URIType.RUNTIME,
            )

        extract_d = workdir / ".extract"
        console.print(f":package: extract swrt into {extract_d}...")
        _sr = StandaloneRuntime(uri)
        _sr.extract(force, extract_d)
        make_dir_gitignore(extract_d)

        console.print(":printer: fork runtime files...")
        cls._do_fork_runtime_bundles(workdir, extract_d, name, force)

        if restore:
            console.print(f":safety_vest: start to restore to {extract_d}...")
            _manifest = load_yaml(extract_d / DEFAULT_MANIFEST_NAME)
            isolated_env_dir = workdir / f".{_manifest['environment']['mode']}"
            if not isolated_env_dir.exists() or force:
                cls.restore(extract_d, isolated_env_dir)
                make_dir_gitignore(isolated_env_dir)
            else:
                console.print(f":sake: {isolated_env_dir} existed, skip restore")

    @staticmethod
    def _do_fork_runtime_bundles(
        workdir: Path, extract_dir: Path, name: str, force: bool = False
    ) -> None:
        # TODO: support user define runtime.yaml name
        runtime_fpath = workdir / DefaultYAMLName.RUNTIME
        if (runtime_fpath).exists() and not force:
            raise ExistedError(runtime_fpath)

        def _copy_file(src: Path, dest: Path) -> None:
            if not src.exists():
                raise NotFoundErr(src)

            if dest.exists() and not force:
                logger.warning(f"{dest} existed, skip copy")

            ensure_dir(dest.parent)
            shutil.copy(str(src), str(dest))

        rt_config = RuntimeConfig.create_by_yaml(extract_dir / DefaultYAMLName.RUNTIME)
        wheel_dir = extract_dir / RuntimeArtifactType.WHEELS
        for _w in rt_config.dependencies.wheels:
            _copy_file(wheel_dir / _w, workdir / _w)

        dep_dir = extract_dir / RuntimeArtifactType.DEPEND
        for _d in rt_config.dependencies.pip_files + rt_config.dependencies.conda_files:
            _copy_file(dep_dir / _d, workdir / _d)

        files_dir = extract_dir / RuntimeArtifactType.FILES
        for _f in rt_config.dependencies.files:
            _copy_file(files_dir / _f["dest"], files_dir / _f["src"])

        runtime_yaml = load_yaml(extract_dir / DefaultYAMLName.RUNTIME)
        runtime_yaml["name"] = name
        ensure_file(
            runtime_fpath, yaml.safe_dump(runtime_yaml, default_flow_style=False)
        )

    @classmethod
    def quickstart_from_ishell(
        cls,
        workdir: t.Union[Path, str],
        name: str,
        mode: str,
        create_env: bool = False,
        force: bool = False,
        interactive: bool = False,
    ) -> None:
        workdir = Path(workdir).absolute()
        ensure_dir(workdir)
        console.print(f":printer: render runtime.yaml @ {workdir}")
        python_version = get_python_version()

        sw_pkg = SW_PYPI_PKG_NAME
        _swcli_version = STARWHALE_VERSION
        if _swcli_version and _swcli_version != SW_DEV_DUMMY_VERSION:
            sw_pkg = f"{sw_pkg}=={_swcli_version}"

        cls.render_runtime_yaml(workdir, name, mode, python_version, [sw_pkg], force)

        if create_env:
            console.print(
                f":construction_worker: create {mode} isolated python environment..."
            )
            _id = create_python_env(
                mode=mode,
                name=name,
                workdir=workdir,
                python_version=python_version,
                force=force,
            )
            console.print(f":dog: install {sw_pkg} {mode}@{_id}...")
            if mode == PythonRunEnv.VENV:
                venv_install_req(_id, req=sw_pkg, enable_pre=True)
            elif mode == PythonRunEnv.CONDA:
                conda_install_req(env_name=_id, req=sw_pkg, enable_pre=True)

            activate_python_env(mode=mode, identity=_id, interactive=interactive)

    @classmethod
    def activate(cls, path: str = "", uri: str = "") -> None:
        if uri:
            _uri = URI(uri, expected_type=URIType.RUNTIME)
            if _uri.instance_type != InstanceType.STANDALONE:
                raise NoSupportError(f"{uri} is not the standalone instance")

            _rt = StandaloneRuntime(_uri)
            mode = load_yaml(_rt.store.manifest_path)["environment"]["mode"]
            prefix_path = _rt.store.export_dir / mode
        elif path:
            # TODO: support non-standard runtime.yaml name
            _rf = Path(path) / DefaultYAMLName.RUNTIME
            _config = RuntimeConfig.create_by_yaml(_rf)
            mode = _config.mode
            prefix_path = Path(path) / f".{mode}"
        else:
            raise Exception("No uri or path to activate")

        activate_python_env(
            mode=mode, identity=str(prefix_path.resolve()), interactive=True
        )

    @classmethod
    def lock(
        cls,
        target_dir: t.Union[str, Path],
        yaml_name: str = DefaultYAMLName.RUNTIME,
        env_name: str = "",
        env_prefix_path: str = "",
        disable_auto_inject: bool = False,
        stdout: bool = False,
        include_editable: bool = False,
        emit_pip_options: bool = False,
    ) -> None:
        runtime_fpath = Path(target_dir) / yaml_name
        if not runtime_fpath.exists():
            raise NotFoundError(runtime_fpath)
        runtime_yaml = load_yaml(runtime_fpath)
        mode = runtime_yaml.get("mode", PythonRunEnv.VENV)
        expected_pyver = str(runtime_yaml.get("environment", {}).get("python", ""))
        console.print(f":butterfly: lock dependencies at mode {mode}")

        _, temp_lock_path = tempfile.mkstemp(prefix="starwhale-lock-")
        if mode == PythonRunEnv.CONDA:
            if not env_name and not env_prefix_path:
                env_prefix_path = get_base_prefix(PythonRunEnv.CONDA)

            if env_prefix_path and not check_valid_conda_prefix(env_prefix_path):
                raise FormatError(f"conda prefix: {env_prefix_path}")

            if not env_prefix_path and not env_name:
                raise MissingFieldError("conda lock need name or prefix_path")

            _kw = {"prefix": env_prefix_path, "name": env_name}
            _pybin = get_conda_pybin(**_kw)
            _detected_pyver = get_python_version_by_bin(_pybin)
            if expected_pyver and not _detected_pyver.startswith(expected_pyver):
                raise EnvironmentError(
                    f"conda: expected python({expected_pyver}) is not equal to detected python({_detected_pyver})"
                )

            console.print(
                f":cat_face: use conda env name({env_name})/prefix({env_prefix_path}) to export environment..."
            )
            conda_export(temp_lock_path, **_kw)
        else:
            if env_name:
                raise NoSupportError(
                    f"lock environment by the env name({env_name}) in venv mode"
                )

            prefix_path = env_prefix_path or get_base_prefix(PythonRunEnv.VENV)
            if not check_valid_venv_prefix(prefix_path):
                raise FormatError(f"venv prefix: {prefix_path}")

            _pybin = os.path.join(prefix_path, "bin", "python3")
            _detected_pyver = get_python_version_by_bin(_pybin)
            if expected_pyver and not _detected_pyver.startswith(expected_pyver):
                raise EnvironmentError(
                    f"venv: expected python({expected_pyver}) is not equal to detected python({_detected_pyver})"
                )

            console.print(f":cat_face: use {_pybin} to freeze requirements...")
            pip_freeze_by_pybin(
                _pybin, temp_lock_path, include_editable, emit_pip_options
            )

        if stdout:
            console.rule("dependencies lock")
            with open(temp_lock_path) as f:
                console.print(f.read())
            os.unlink(temp_lock_path)
        else:
            dest_fname = (
                RuntimeLockFileType.CONDA
                if mode == PythonRunEnv.CONDA
                else RuntimeLockFileType.VENV
            )
            console.print(f":mouse: dump lock file: {dest_fname}")
            shutil.move(temp_lock_path, Path(target_dir) / dest_fname)

            if not disable_auto_inject and runtime_fpath.exists():
                cls._update_runtime_dep_lock_field(runtime_fpath, dest_fname)

    @staticmethod
    def _update_runtime_dep_lock_field(runtime_fpath: Path, lock_fname: str) -> None:
        content = load_yaml(runtime_fpath)
        deps = content.get("dependencies", [])
        if lock_fname in deps:
            return

        console.print(f":monkey_face: update {runtime_fpath} dependencies field")
        deps.append(lock_fname)
        content["dependencies"] = deps
        # TODO: safe_dump will change user's other yaml content format at first time
        ensure_file(runtime_fpath, yaml.safe_dump(content, default_flow_style=False))

    @staticmethod
    def render_runtime_yaml(
        workdir: Path,
        name: str,
        mode: str,
        python_version: str,
        pkgs: t.List[str],
        force: bool = False,
    ) -> None:
        _rm = workdir / DefaultYAMLName.RUNTIME

        if _rm.exists() and not force:
            raise ExistedError(f"{_rm} was already existed")

        ensure_dir(workdir)
        config = dict(
            name=name,
            mode=mode,
            environment={
                "python": python_version,
                "arch": SupportArch.NOARCH,
                "os": SupportOS.UBUNTU,
            },
            dependencies=[
                RuntimeLockFileType.CONDA
                if mode == PythonRunEnv.CONDA
                else RuntimeLockFileType.VENV,
                {"pip": pkgs},
            ],
            api_version=RUNTIME_API_VERSION,
        )
        ensure_file(_rm, yaml.safe_dump(config, default_flow_style=False))

    def dockerize(
        self,
        tags: t.Optional[t.List[str]] = None,
        platforms: t.Optional[t.List[str]] = None,
        push: bool = True,
        dry_run: bool = False,
        use_starwhale_builder: bool = False,
        reset_qemu_static: bool = False,
    ) -> None:
        docker_dir = self.store.export_dir / "docker"
        ensure_dir(docker_dir)
        dockerfile_path = docker_dir / "Dockerfile"

        def _extract() -> None:
            if (
                not self.store.snapshot_workdir.exists()
                or not self.store.manifest_path.exists()
            ):
                console.print(":unicorn_face: extract runtime swrt file")
                self.extract(force=True)

        def _render_dockerfile(_manifest: t.Dict[str, t.Any]) -> None:
            console.print(f":wolf_face: render Dockerfile @{dockerfile_path}")
            _env = jinja2.Environment(
                loader=jinja2.FileSystemLoader(searchpath=_TEMPLATE_DIR)
            )
            _template = _env.get_template("Dockerfile.tmpl")
            _pip = _manifest["configs"].get("pip", {})
            _out = _template.render(
                base_image=_manifest["base_image"],
                runtime_name=self.uri.object.name,
                runtime_version=_manifest["version"],
                pypi_index_url=_pip.get("index_url", ""),
                pypi_extra_index_url=" ".join(_pip.get("extra_index_url", [])),
                pypi_trusted_host=" ".join(_pip.get("trusted_host", [])),
                python_version=_manifest["environment"]["python"],
                mode=_manifest["environment"]["mode"],
                local_packaged_env=_manifest["dependencies"].get(
                    "local_packaged_env", False
                ),
            )
            ensure_file(dockerfile_path, _out)

        def _render_dockerignore() -> None:
            _ignores = ["export/venv", "export/conda", ".git"]
            ensure_file(
                self.store.snapshot_workdir / ".dockerignore",
                content="\n".join(_ignores),
            )

        def _build(_manifest: t.Dict[str, t.Any]) -> None:
            _tags = list(tags or [])
            _platforms = platforms or []
            _dc_image = _manifest["configs"].get("docker", {}).get("image")
            if _dc_image:
                _tags.append(f"{_dc_image.split(':', 1)[0]}:{_manifest['version']}")

            if not dry_run and reset_qemu_static:
                docker.reset_qemu_static()

            docker.buildx(
                dockerfile_path,
                self.store.snapshot_workdir,
                push=push,
                dry_run=dry_run,
                platforms=_platforms,
                tags=_tags,
                use_starwhale_builder=use_starwhale_builder,
            )

        _extract()
        _manifest = load_yaml(self.store.manifest_path)
        _render_dockerfile(_manifest)
        _render_dockerignore()
        _build(_manifest)

    @classmethod
    def restore(
        cls,
        workdir: Path,
        isolated_env_dir: t.Optional[Path] = None,
        verbose: bool = True,
    ) -> None:
        if not (workdir.exists() and (workdir / DEFAULT_MANIFEST_NAME).exists()):
            raise NoSupportError("only support swrt extract workdir")

        _manifest = load_yaml(workdir / DEFAULT_MANIFEST_NAME)
        _env = _manifest["environment"]
        _starwhale_version = (
            _manifest.get("environment", {})
            .get("lock", {})
            .get("starwhale_version", "")
        )
        isolated_env_dir = isolated_env_dir or workdir / "export" / _env["mode"]

        operations = [
            (
                cls._validate_environment,
                5,
                "validate environment condition",
                dict(
                    expected_arch=_env.get("arch", []),
                ),
            ),
            (
                restore_python_env,
                20,
                "restore python env",
                dict(
                    workdir=workdir,
                    mode=_env["mode"],
                    python_version=_env["python"],
                    deps=_manifest["dependencies"],
                    wheels=_manifest.get("artifacts", {}).get(
                        RuntimeArtifactType.WHEELS, []
                    ),
                    configs=_manifest.get("configs", {}),
                    isolated_env_dir=isolated_env_dir,
                ),
            ),
            (
                install_starwhale,
                10,
                "install starwhale",
                dict(
                    prefix_path=isolated_env_dir,
                    mode=_env["mode"],
                    version=_starwhale_version,
                    force=False,
                    configs=_manifest.get("configs", {}),
                ),
            ),
            (
                cls._setup_native_files,
                10,
                "setup native files",
                dict(
                    workdir=workdir,
                    mode=_env["mode"],
                    files=_manifest.get("artifacts", {}).get("files", []),
                ),
            ),
            (
                render_python_env_activate,
                5,
                "render python env activate scripts",
                dict(
                    mode=_env["mode"],
                    prefix_path=isolated_env_dir,
                    workdir=workdir,
                    local_packaged_env=_manifest["dependencies"].get(
                        "local_packaged_env", False
                    ),
                    verbose=verbose,
                ),
            ),
        ]

        run_with_progress_bar("runtime restore...", operations)

    @staticmethod
    def _validate_environment(expected_arch: t.List[str]) -> None:
        # TODO: add os, cuda version, python version validator
        def _validate_arch() -> None:
            machine_map = {
                "aarch64": SupportArch.ARM64,
                "arm64": SupportArch.ARM64,
                "x86_64": SupportArch.AMD64,
                "amd64": SupportArch.AMD64,
            }

            machine = platform.machine()
            machine_arch = [
                machine,
                machine_map.get(machine) or machine,
                SupportArch.NOARCH,
            ]
            _section = set([a.lower() for a in machine_arch]) & set(
                [a.lower() for a in expected_arch]
            )
            if not _section:
                raise UnExpectedConfigFieldError(
                    f"machine arch: {machine}, expected arch: {expected_arch}"
                )

        if expected_arch:
            _validate_arch()

    @staticmethod
    def _setup_native_files(
        workdir: Path, mode: str, files: t.List[t.Dict[str, str]]
    ) -> None:
        # TODO: support native file pre-hook, post-hook
        for _f in files:
            if _f["dest"].startswith("/") and in_container():
                logger.warning(f"NOTICE! dest dir: {_f['dest']}")
                _dest = Path(_f["dest"])
            else:
                if mode in (PythonRunEnv.CONDA, PythonRunEnv.VENV):
                    _root = workdir / "export" / mode
                else:
                    _root = workdir
                _dest = _root / _f["dest"].lstrip("/")

            _src = workdir / _f["_swrt_dest"]

            console.print(f":baby_chick: copy native files: {_src} -> {_dest}")
            if _src.is_dir():
                copy_fs(str(_src), str(_dest))
            else:
                ensure_dir(_dest.parent)
                shutil.copyfile(str(_src), str(_dest))


class CloudRuntime(CloudBundleModelMixin, Runtime):
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

    def build(self, workdir: Path, yaml_name: str = "", **kw: t.Any) -> None:
        raise NoSupportError("no support build runtime in the cloud instance")
