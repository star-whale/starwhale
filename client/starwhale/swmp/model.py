import yaml
import os
import typing as t
from pathlib import Path
import platform
from datetime import datetime
import tarfile

import conda_pack
from loguru import logger
from fs.copy import copy_fs, copy_file
from fs.walk import Walker
from fs import open_fs

from starwhale import __version__
from starwhale.utils.error import FileTypeError, FileFormatError, NoSupportError
from starwhale.utils import (
    gen_uniq_version, get_conda_env, is_windows, is_linux, is_darwin,
    is_venv, is_conda, get_python_run_env,
    pip_freeze, conda_export, get_python_version,
)
from starwhale.utils.fs import ensure_dir, ensure_file, ensure_link
from starwhale.utils.venv import setup_venv, install_req
from starwhale.consts import (
    CONDA_ENV_TAR, DEFAULT_STARWHALE_API_VERSION, DUMP_CONDA_ENV_FNAME, DUMP_PIP_REQ_FNAME, FMT_DATETIME,
    DEFAULT_MANIFEST_NAME, DEFAULT_MODEL_YAML_NAME, SUPPORTED_PIP_REQ, DUMP_USER_PIP_REQ_FNAME,
    DUMP_CONDA_ENV_FNAME, DUMP_PIP_REQ_FNAME, DEFAULT_COPY_WORKERS,
)
from .store import ModelPackageLocalStore


class ModelRunConfig(object):

    #TODO: use attr to tune class
    def __init__(self, ppl: str, args: str="", runtime: str="",
                 base_image: str="", env: str="", pip_req: str="",
                 pkg_data: t.Union[list, None]=None, exclude_pkg_data: t.Union[list, None]=None,
                 smoketest: str=""):
        self.ppl = ppl.strip()
        self.args = args
        self.runtime = runtime.strip()
        self.base_image = base_image.strip()
        self.env = env
        self.pip_req = pip_req
        self.pkg_data = pkg_data or []
        self.exclude_pkg_data = exclude_pkg_data or []
        self.smoketest = smoketest

        self._validator()

    def _validator(self) -> None:
        if not self.ppl:
            raise FileFormatError("need ppl field")

        if not(self.runtime or self.base_image):
            raise FileFormatError("runtime or base_image must set at least one field")

    def __str__(self) -> str:
        return f"Model Run Config: ppl -> {self.ppl}"

    def __repr__(self) -> str:
        return f"Model Run Config: ppl -> {self.ppl}, runtime -> {self.runtime}, base_image -> {self.base_image}"


class ModelConfig(object):

    #TODO: use attr to tune class
    def __init__(self, name: str, model: t.List[str],
        config: t.List[str], run: dict, desc: str = "",
        tag: t.List[str] = [], version: str=DEFAULT_STARWHALE_API_VERSION):

        #TODO: format model name
        self.name = name
        self.model = model or []
        self.config = config or []
        #TODO: support artifacts: local or remote
        self.run = ModelRunConfig(**run)
        self.desc = desc
        self.tag = tag
        self.version = version

        self._validator()

    def _validator(self):
        #TODO: use attr validator
        if not self.model:
            raise FileFormatError("need at least one model")

        #TODO: add more validation
        #TODO: add name check

    @classmethod
    def create_by_yaml(cls, fpath: str) -> "ModelConfig":
        with open(fpath) as f:
             c = yaml.safe_load(f)

        return ModelConfig(**c)

    def __str__(self) -> str:
        return f"Model Config: {self.name}"

    def __repr__(self) -> str:
        return f"Model Config: name -> {self.name}, model-> {self.model}"


class ModelPackage(object):

    def __init__(self, workdir: str, model_yaml_fname: str, skip_gen_env: bool) -> None:
        #TODO: format workdir path?
        self.workdir = Path(workdir)
        self._skip_gen_env = skip_gen_env
        self._model_yaml_fname = model_yaml_fname
        self._swmp_config = self.load_model_config(os.path.join(workdir, model_yaml_fname))
        self._store = ModelPackageLocalStore()

        self._swmp_store = Path()
        self._snapshot_workdir = Path()
        self._version = ""
        self._snapshot = None
        self._name = self._swmp_config.name
        self._manifest = {} #TODO: use manifest classget_conda_env

    def __str__(self) -> str:
        return f"Model Package: {self._name}"

    def __repr__(self) -> str:
        return f"Model Package: name -> {self._name}, version-> {self._version}"

    @classmethod
    def build(cls, workdir: str, mname: str, skip_gen_env: bool):
        mp = ModelPackage(workdir, mname, skip_gen_env)
        mp._do_validate()
        mp._do_build()

    def _do_validate(self):
        sw = self._swmp_config

        if not sw.model:
            raise FileFormatError("model yaml no model")

        for path in sw.model:
            if not (self.workdir / path).exists():
                raise FileFormatError(f"model - {path} not existed")

        if not (self.workdir / sw.run.ppl).exists():
            raise FileExistsError(f"run ppl - {sw.run.ppl} not existed")

        #TODO: add more model.yaml section validation
        #TODO: add 'swcli model check' cmd

    @logger.catch
    def _do_build(self):
        #TODO: add progress bar
        self._gen_version()
        self._prepare_snapshot()
        self._copy_src()
        self._dump_dep()

        self._render_docker_script()
        self._render_manifest()
        self._make_swmp_tar()

    def _gen_version(self) -> None:
        logger.info("[step:version]create swmp version...")
        if not self._version:
            self._version = gen_uniq_version(self._swmp_config.name)

        self._manifest["version"] = self._version
        self._manifest["created_at"] = datetime.now().astimezone().strftime(FMT_DATETIME)
        logger.info(f"[step:version]swmp version is {self._version}")

    def _prepare_snapshot(self):
        logger.info("[step:prepare-snapshot]prepare swmp snapshot dirs...")

        self._swmp_store = self._store.pkgdir / self._name
        self._snapshot_workdir = self._store.workdir / self._name / self._version
        #TODO: graceful clear?
        if self._snapshot_workdir.exists():
            raise Exception(f"{self._snapshot_workdir} has already existed, will abort")

        ensure_dir(self._swmp_store)
        ensure_dir(self._snapshot_workdir)
        ensure_dir(self._src_dir)
        ensure_dir(self._conda_dir)
        ensure_dir(self._python_dir / "venv")

        #TODO: cleanup garbage dir
        #TODO: add lock/flag file for gc

        logger.info(f"[step:prepare-snapshot]swmp snapshot workdir: {self._snapshot_workdir}")

    @property
    def _model_pip_req(self) -> str:
        _run = self._swmp_config.run

        if _run.pip_req and (self.workdir / _run.pip_req).exists():
            return str(self.workdir / _run.pip_req)
        else:
            for p in SUPPORTED_PIP_REQ:
                if (self.workdir / p).exists():
                    return str(self.workdir / p)
            else:
                return ""

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
        logger.info(f"[step:dep]dump conda or venv environment...")

        pr_env = get_python_run_env()
        sys_name = platform.system()
        py_ver = get_python_version()

        #TODO: add python version into manifest
        #TODO: add size into manifest
        self._manifest["dep"] = dict(
            env=pr_env,
            system=sys_name,
            python=py_ver,
            local_gen_env=False,
            venv=dict(use=not is_conda()),
            conda=dict(use=is_conda())
        )

        pip_lock_req = self._python_dir / DUMP_PIP_REQ_FNAME
        conda_lock_env = self._conda_dir / DUMP_CONDA_ENV_FNAME

        #TODO: use model.yaml run-env field
        logger.info(f"[info:dep]python env({pr_env}), os({sys_name}, python({py_ver}))")
        if is_conda():
            #TODO: environment.yaml prefix removed?
            logger.info(f"[info:dep]dump conda environment yaml: {conda_lock_env}")
            conda_export(conda_lock_env)
        elif is_venv():
            logger.info(f"[info:dep]dump pip-req with freeze: {pip_lock_req}")
            pip_freeze(pip_lock_req)
        else:
            # TODO: add other env tools
            logger.warning("detect use system python, swcli does not pip freeze, only use custom pip-req")

        if is_windows() or is_darwin() or self._skip_gen_env:
            #TODO: win/osx will produce env in controller agent with task
            logger.info(f"[info:dep]{sys_name} will skip conda/venv dump or generate")
        elif is_linux():
            #TODO: more design local or remote build venv
            #TODO: ignore some pkg when dump, like notebook?
            self._manifest["dep"]["local_gen_env"] = True  # type: ignore

            if is_conda():
                cenv = get_conda_env()
                dest = str(self._conda_dir / CONDA_ENV_TAR)
                if not cenv:
                    raise Exception(f"cannot get conda env value")

                #TODO: add env/env-name into model.yaml, user can set custom vars.
                logger.info("[info:dep]try to pack conda...")
                conda_pack.pack(name=cenv, force=True, output=dest, ignore_editable_packages=True)
                logger.info(f"[info:dep]finish conda pack {dest})")
            else:
                #TODO: tune venv create performance, use clone?
                logger.info(f"[info:dep]build venv dir: {self._venv_dir}")
                setup_venv(self._venv_dir)
                logger.info(f"[info:dep]install pip freeze({pip_lock_req}) to venv: {self._venv_dir}")
                install_req(self._venv_dir, pip_lock_req)
                if self._model_pip_req:
                    logger.info(f"[info:dep]install custom pip({self._model_pip_req}) to venv: {self._venv_dir}")
                    install_req(self._venv_dir, self._model_pip_req)
                    copy_fs(self._model_pip_req, str(self._python_dir / DUMP_USER_PIP_REQ_FNAME))
        else:
            raise NoSupportError(f"no support {sys_name} system")

        logger.info(f"[step:dep]finish dump dep")

    def _copy_src(self) -> None:
        logger.info(f"[step:copy]start to copy src {self.workdir} -> {self._src_dir} ...")
        _mc = self._swmp_config
        workdir_fs = open_fs(str(self.workdir.resolve()))
        snapshot_fs = open_fs(str(self._snapshot_workdir.resolve()))
        src_fs = open_fs(str(self._src_dir.resolve()))
        #TODO: support exclude dir
        copy_file(workdir_fs, self._model_yaml_fname, snapshot_fs, DEFAULT_MODEL_YAML_NAME)
        copy_fs(workdir_fs, src_fs,
                walker=Walker(
                    filter=["*.py", self._model_yaml_fname] + SUPPORTED_PIP_REQ + _mc.model + _mc.config + _mc.run.pkg_data,
                ), workers=DEFAULT_COPY_WORKERS)
        logger.info("[step:copy]finish copy files")

    def _render_manifest(self):
        self._manifest["tag"] = self._swmp_config.tag or []
        self._manifest["build"] = dict(
            os=platform.system(),
            swctl_version=__version__,
        )
        #TODO: add signature for import files: model, config
        _manifest = self._snapshot_workdir / DEFAULT_MANIFEST_NAME
        ensure_file(_manifest, yaml.dump(self._manifest, default_flow_style=False))
        logger.info(f"[step:manifest]render manifest: {_manifest}")

    def _make_swmp_tar(self):
        out = self._swmp_store / f"{self._version}.swmp"
        logger.info(f"[step:tar]try to tar for swmp {out} ...")
        with tarfile.open(out, "w:") as tar:
            tar.add(str(self._snapshot_workdir), arcname="")

        ensure_link(out, self._swmp_store / "latest")
        logger.info(f"[step:tar]finish to make swmp")

    def _render_docker_script(self):
        #TODO: agent run and smoketest step
        pass

    def load_model_config(self, fpath: str) -> ModelConfig:
        if not os.path.exists(fpath):
            raise Exception(f"model.yaml {fpath} is not existed")

        if not fpath.endswith((".yaml", ".yml")):
            raise FileTypeError(f"{fpath} file type is not yaml|yml")

        return ModelConfig.create_by_yaml(fpath)