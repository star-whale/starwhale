from turtle import pen
import typing as t
from collections import namedtuple
from datetime import datetime
from pathlib import Path
import tarfile

from rich.console import Console
from loguru import logger
from starwhale.utils import gen_uniq_version

from .store import EvalLocalStorage
from starwhale.consts import FMT_DATETIME, DEFAULT_MANIFEST_NAME, LOCAL_FUSE_JSON_NAME
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.error import SWObjNameFormatError
from starwhale.swds.dataset import DataSet

DEFAULT_SW_TASK_RUN_IMAGE = "starwhaleai/starwhale:latest"
EVAL_TASK_TYPE = namedtuple("EVAL_TASK_TYPE", ["ALL", "PPL", "CMP"])(
    "all", "ppl", "cmp"
)


class EvalExecutor(object):

    def __init__(self, model: str, datasets: t.List[str], baseimage: str=DEFAULT_SW_TASK_RUN_IMAGE,
                 name: str="", desc: str="", gencmd: bool=False) -> None:
        self.name = name
        self.desc = desc
        self.model = model
        self.datasets = datasets
        self.baseimage = baseimage
        self.gencmd = gencmd
        self._store = EvalLocalStorage()

        self._console = Console()
        self._version = ""
        self._manifest = {}
        self._workdir = Path()
        self._model_dir = Path()
        self._fuse_jsons = []

        self._validator()

    def __str__(self) -> str:
        return f"Evaluation Executor: {self.name}"

    def __repr__(self) -> str:
        return f"Evaluation Executor: name -> {self.name}, version -> {self._version}"

    def _validator(self):
        if self.model.count(":") != 1:
            raise SWObjNameFormatError

        for d in self.datasets:
            if d.count(":") != 1:
                raise SWObjNameFormatError

    @logger.catch
    def run(self, phase: str=EVAL_TASK_TYPE.ALL):
        self._gen_version()
        self._prepare_workdir()
        self._extract_swmp()
        self._gen_swds_fuse_json()

        if phase == EVAL_TASK_TYPE.ALL:
            self._do_run_ppl()
            self._do_run_cmp()
        elif phase == EVAL_TASK_TYPE.PPL:
            self._do_run_ppl()
        elif phase == EVAL_TASK_TYPE.CMP:
            self._do_run_cmp()

        if phase != EVAL_TASK_TYPE.PPL:
            self._render_report()

        self._render_manifest()

    def _gen_version(self) -> None:
        #TODO: abstract base class or mixin class for swmp/swds/
        logger.info("[step:version]create eval job version...")
        if not self._version:
            self._version = gen_uniq_version(self.name)
        self._manifest["version"] = self._version
        self._manifest["created_at"] = datetime.now().astimezone().strftime(FMT_DATETIME)
        logger.info(f"[step:version]eval job version is {self._version}")

    @property
    def _ppl_workdir(self) -> Path:
        return self._workdir / EVAL_TASK_TYPE.PPL

    @property
    def _cmp_workdir(self) -> Path:
        return self._workdir / EVAL_TASK_TYPE.CMP

    def _render_manifest(self) -> None:
        pass

    def _prepare_workdir(self) -> None:
        logger.info("[step:prepare]create eval workdir...")
        self._workdir = self._store.rootdir / "run" / "eval" / self._version

        ensure_dir(self._workdir)
        ensure_dir(self._ppl_workdir / "result")
        ensure_dir(self._ppl_workdir / "log")
        ensure_dir(self._ppl_workdir / "status")
        ensure_dir(self._cmp_workdir / "result")
        ensure_dir(self._cmp_workdir / "log")
        ensure_dir(self._cmp_workdir / "status")

        logger.info(f"[step:prepare]eval workdir: {self._workdir}")

    def _extract_swmp(self) -> None:
        #TODO: support to guess short version

        name, version = self.model.split(":")
        self._model_dir = self._store.workdir / name / version
        logger.info(f"[step:extract]model @ {self._model_dir}")

        if self._model_dir.exists():
            logger.info(f"[step:extract]use existed {self._model_dir}, skip extract swmp")
        else:
            #TODO: call swmp method
            _swmp_path = self._store.pkgdir / name / f"{version}.swmp"
            logger.info(f"[step:extract]try to extract {_swmp_path} -> {self._model_dir}")
            with tarfile.open(_swmp_path, "r") as tar:
                tar.extractall(path=str(self._model_dir.resolve()))

        if not (self._model_dir / DEFAULT_MANIFEST_NAME).exists():
            raise Exception("invalid swmp model dir")

    def _gen_swds_fuse_json(self) -> None:
        for ds in self.datasets:
            fname = DataSet.render_fuse_json(ds, force=False)
            self._fuse_jsons.append(fname)
            logger.debug(f"[gen fuse.json]{fname}")

    def _do_run_ppl(self) -> None:
        pass

    def _do_run_cmp(self) -> None:
        pass

    def _render_report(self) -> None:
        pass