import yaml
import os
import typing as t
from pathlib import Path
import platform
import tarfile

from loguru import logger
from fs.copy import copy_fs, copy_file
from fs.walk import Walker
from fs import open_fs

from starwhale import __version__
from starwhale.utils.error import FileTypeError, FileFormatError, NotFoundError
from starwhale.utils import gen_uniq_version, console, now_str
from starwhale.utils.fs import ensure_dir, ensure_file, ensure_link
from starwhale.utils.venv import detect_pip_req, dump_python_dep_env, SUPPORTED_PIP_REQ
from starwhale.utils.load import import_cls
from starwhale.consts import (
    DEFAULT_STARWHALE_API_VERSION,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_MODEL_YAML_NAME,
    DEFAULT_COPY_WORKERS,
    SHORT_VERSION_CNT,
)
from .store import ModelPackageLocalStore
from starwhale.utils.progress import run_with_progress_bar


class ModelRunConfig(object):

    # TODO: use attr to tune class
    def __init__(
        self,
        ppl: str,
        args: str = "",
        runtime: str = "",
        base_image: str = "",
        pkg_system: str = "",
        pip_req: str = "",
        pkg_data: t.Union[t.List[str], None] = None,
        exclude_pkg_data: t.Union[t.List[str], None] = None,
        smoketest: str = "",
        envs: t.Union[t.List[str], None] = None,
    ):
        self.ppl = ppl.strip()
        self.args = args
        self.runtime = runtime.strip()
        self.base_image = base_image.strip()
        self.pkg_system = pkg_system
        self.pip_req = pip_req
        self.pkg_data = pkg_data or []
        self.exclude_pkg_data = exclude_pkg_data or []
        self.smoketest = smoketest
        self.envs = envs or []

        self._validator()

    def _validator(self) -> None:
        if not self.ppl:
            raise FileFormatError("need ppl field")

    def __str__(self) -> str:
        return f"Model Run Config: ppl -> {self.ppl}"

    def __repr__(self) -> str:
        return f"Model Run Config: ppl -> {self.ppl}, runtime -> {self.runtime}, base_image -> {self.base_image}"


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

        self._validator()

    def _validator(self) -> None:
        # TODO: use attr validator
        if not self.model:
            raise FileFormatError("need at least one model")

        # TODO: add more validation
        # TODO: add name check

    @classmethod
    def create_by_yaml(cls, fpath: str) -> "ModelConfig":
        with open(fpath) as f:
            c = yaml.safe_load(f)

        return cls(**c)

    def __str__(self) -> str:
        return f"Model Config: {self.name}"

    def __repr__(self) -> str:
        return f"Model Config: name -> {self.name}, model-> {self.model}"


class ModelPackage(object):
    def __init__(
        self,
        workdir: str,
        model_yaml_fname: str = DEFAULT_MODEL_YAML_NAME,
        skip_gen_env: bool = False,
    ) -> None:
        # TODO: format workdir path?
        self.workdir = Path(workdir)
        self._skip_gen_env = skip_gen_env
        self._model_yaml_fname = model_yaml_fname
        self._swmp_config = self.load_model_config(
            os.path.join(workdir, model_yaml_fname)
        )
        self._store = ModelPackageLocalStore()

        self._swmp_store = Path()
        self._snapshot_workdir = Path()
        self._version = ""
        self._snapshot = None
        self._name = self._swmp_config.name
        self._manifest: t.Dict[str, t.Any] = {}  # TODO: use manifest classget_conda_env
        self._console = console

        self._load_config_envs()

    def __str__(self) -> str:
        return f"Model Package: {self._name}"

    def __repr__(self) -> str:
        return f"Model Package: name -> {self._name}, version-> {self._version}"

    def _load_config_envs(self) -> None:
        for _env in self._swmp_config.run.envs:
            _env = _env.strip()
            if not _env:
                continue
            _t = _env.split("=", 1)
            _k, _v = _t[0], "".join(_t[1:])

            if _k not in os.environ:
                os.environ[_k] = _v

    @classmethod
    def _load_runnable_mp(
        cls,
        swmp: str = ".",
        _model_yaml_fname: str = DEFAULT_MODEL_YAML_NAME,
        kw: t.Dict[str, t.Any] = {},
    ) -> "ModelPackage":
        if swmp.count(":") == 1:
            _name, _version = swmp.split(":")
            # TODO: tune model package local store init twice
            # TODO: guess _version?
            # TODO: model.yaml auto-detect
            _workdir = ModelPackageLocalStore().workdir / _name / _version / "src"
        else:
            _workdir = Path(swmp)
        _model_fpath = _workdir / _model_yaml_fname

        if not _model_fpath.exists():
            raise NotFoundError(f"swmp model.yaml({_model_fpath}) not found")

        mp = ModelPackage(str(_workdir.resolve()), _model_yaml_fname, skip_gen_env=True)
        mp._do_validate()
        return mp

    @classmethod
    def cmp(
        cls,
        swmp: str = ".",
        _model_yaml_fname: str = DEFAULT_MODEL_YAML_NAME,
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        mp = cls._load_runnable_mp(swmp, _model_yaml_fname, kw)
        _obj = mp._load_user_ppl_obj(kw)
        _obj._starwhale_internal_run_cmp()
        mp._console.print(f":clap: finish run cmp: {_obj}")

    @classmethod
    def ppl(
        cls,
        swmp: str = ".",
        _model_yaml_fname: str = DEFAULT_MODEL_YAML_NAME,
        kw: t.Dict[str, t.Any] = {},
    ) -> None:
        mp = cls._load_runnable_mp(swmp, _model_yaml_fname, kw)
        _obj = mp._load_user_ppl_obj(kw)
        _obj._starwhale_internal_run_ppl()
        mp._console.print(f":clap: finish run ppl: {_obj}")

    def _load_user_ppl_obj(self, kw: t.Dict[str, t.Any] = {}) -> t.Any:
        from starwhale.api._impl.model import _RunConfig

        _RunConfig.set_env(kw)

        _s = f"{self._swmp_config.run.ppl}@{self.workdir}"
        self._console.print(f"try to import {_s}...")
        _cls = import_cls(self.workdir, self._swmp_config.run.ppl)
        return _cls()

    @classmethod
    def build(cls, workdir: str, mname: str) -> None:
        mp = ModelPackage(workdir, mname)
        mp._do_validate()
        mp._do_build()

    def _do_validate(self) -> None:
        sw = self._swmp_config

        if not sw.model:
            raise FileFormatError("model yaml no model")

        for path in sw.model + sw.config:
            if not (self.workdir / path).exists():
                raise FileFormatError(f"model - {path} is not existed")

        # TODO: add more model.yaml section validation
        # TODO: add 'swcli model check' cmd

    @logger.catch
    def _do_build(self) -> None:
        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_snapshot, 5, "prepare snapshot"),
            (self._copy_src, 15, "copy src"),
            (self._dump_dep, 50, "dump python depency"),
            (self._render_manifest, 5, "render manifest"),
            (self._make_swmp_tar, 20, "build swmp"),
        ]
        run_with_progress_bar("swmp building...", operations, self._console)

    def _gen_version(self) -> None:
        logger.info("[step:version]create swmp version...")
        if not self._version:
            self._version = gen_uniq_version(self._swmp_config.name)

        self._manifest["version"] = self._version
        self._manifest["created_at"] = now_str()  # type: ignore
        logger.info(f"[step:version]swmp version is {self._version}")
        self._console.print(f":new: swmp version {self._version[:SHORT_VERSION_CNT]}")

    def _prepare_snapshot(self) -> None:
        logger.info("[step:prepare-snapshot]prepare swmp snapshot dirs...")

        self._swmp_store = self._store.pkgdir / self._name
        self._snapshot_workdir = self._store.workdir / self._name / self._version
        # TODO: graceful clear?
        if self._snapshot_workdir.exists():
            raise Exception(f"{self._snapshot_workdir} has already existed, will abort")

        ensure_dir(self._swmp_store)
        ensure_dir(self._snapshot_workdir)
        ensure_dir(self._src_dir)
        ensure_dir(self._conda_dir)
        ensure_dir(self._python_dir / "venv")

        # TODO: cleanup garbage dir
        # TODO: add lock/flag file for gc

        logger.info(
            f"[step:prepare-snapshot]swmp snapshot workdir: {self._snapshot_workdir}"
        )
        self._console.print(
            f":file_folder: swmp workdir: [underline]{self._snapshot_workdir}[/]"
        )

    @property
    def _model_pip_req(self) -> str:
        _run = self._swmp_config.run
        return detect_pip_req(self.workdir, _run.pip_req)

    @property
    def _conda_dir(self) -> Path:
        return self._snapshot_workdir / "dep" / "conda"

    @property
    def _python_dir(self) -> Path:
        return self._snapshot_workdir / "dep" / "python"

    @property
    def _venv_dir(self) -> Path:
        return self._python_dir / "venv"

    @property
    def _src_dir(self) -> Path:
        return self._snapshot_workdir / "src"

    def _dump_dep(self) -> None:
        logger.info("[step:dep]start dump python dep...")

        _dep = dump_python_dep_env(
            dep_dir=self._snapshot_workdir / "dep",
            pip_req_fpath=self._model_pip_req,
            skip_gen_env=self._skip_gen_env,
            console=self._console,
            expected_runtime=self._swmp_config.run.runtime,
        )
        self._manifest["dep"] = _dep

        logger.info("[step:dep]finish dump dep")

    def _copy_src(self) -> None:
        logger.info(
            f"[step:copy]start to copy src {self.workdir} -> {self._src_dir} ..."
        )
        self._console.print(":thumbs_up: try to copy source code files...")
        _mc = self._swmp_config
        workdir_fs = open_fs(str(self.workdir.resolve()))
        snapshot_fs = open_fs(str(self._snapshot_workdir.resolve()))
        src_fs = open_fs(str(self._src_dir.resolve()))
        # TODO: support exclude dir
        # TODO: support glob pkg_data
        # TODO: ignore some folders, such as __pycache__
        copy_file(
            workdir_fs, self._model_yaml_fname, snapshot_fs, DEFAULT_MODEL_YAML_NAME
        )
        copy_fs(
            workdir_fs,
            src_fs,
            walker=Walker(
                filter=["*.py", self._model_yaml_fname]
                + SUPPORTED_PIP_REQ
                + _mc.run.pkg_data,
                exclude_dirs=_mc.run.exclude_pkg_data,
            ),
            workers=DEFAULT_COPY_WORKERS,
        )

        for _fname in _mc.config + _mc.model:
            copy_file(workdir_fs, _fname, src_fs, _fname)

        logger.info("[step:copy]finish copy files")

    def _render_manifest(self) -> None:
        self._manifest["name"] = self._name
        self._manifest["tag"] = self._swmp_config.tag or []
        self._manifest["build"] = dict(
            os=platform.system(),
            sw_version=__version__,
        )
        # TODO: add signature for import files: model, config
        _f = self._snapshot_workdir / DEFAULT_MANIFEST_NAME
        ensure_file(_f, yaml.dump(self._manifest, default_flow_style=False))
        logger.info(f"[step:manifest]render manifest: {_f}")

    def _make_swmp_tar(self) -> None:
        out = self._swmp_store / f"{self._version}.swmp"
        logger.info(f"[step:tar]try to tar for swmp {out} ...")
        with tarfile.open(out, "w:") as tar:
            tar.add(str(self._snapshot_workdir), arcname="")

        ensure_link(out, self._swmp_store / "latest")
        logger.info("[step:tar]finish to make swmp")
        self._console.print(
            f":hibiscus: congratulation! you can run [blink red bold] swcli model info {self._name}:{self._version}[/]"
        )

    def load_model_config(self, fpath: str) -> ModelConfig:
        if not os.path.exists(fpath):
            raise FileExistsError(f"model yaml {fpath} is not existed")

        if not fpath.endswith((".yaml", ".yml")):
            raise FileTypeError(f"{fpath} file type is not yaml|yml")

        return ModelConfig.create_by_yaml(fpath)
