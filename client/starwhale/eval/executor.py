import typing as t
import json
import os
from collections import namedtuple
from datetime import datetime
from pathlib import Path
import jsonlines

from rich.console import Console
from loguru import logger
from starwhale.utils import gen_uniq_version

from .store import EvalLocalStorage
from starwhale.consts import (
    DATA_LOADER_KIND, DEFAULT_INPUT_JSON_FNAME, FMT_DATETIME,
    JSON_INDENT, SWDS_BACKEND_TYPE,
)

from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.error import SWObjNameFormatError
from starwhale.swds.dataset import DataSet
from starwhale.swmp.store import ModelPackageLocalStore
from starwhale.utils.process import check_call

DEFAULT_SW_TASK_RUN_IMAGE = "starwhaleai/starwhale:latest"
EVAL_TASK_TYPE = namedtuple("EVAL_TASK_TYPE", ["ALL", "PPL", "CMP"])(
    "all", "ppl", "cmp"
)


class EvalExecutor(object):

    def __init__(self, model: str, datasets: t.List[str], baseimage: str=DEFAULT_SW_TASK_RUN_IMAGE,
                 name: str="", desc: str="", gencmd: bool=False, docker_verbose: bool=False) -> None:
        self.name = name
        self.desc = desc
        self.model = model
        self.datasets = datasets
        self.baseimage = baseimage
        self.gencmd = gencmd
        self.docker_verbose = docker_verbose
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

        if phase != EVAL_TASK_TYPE.PPL and not self.gencmd:
            self._render_report()

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

    def _prepare_workdir(self) -> None:
        logger.info("[step:prepare]create eval workdir...")
        self._workdir = self._store.rootdir / "run" / "eval" / self._version

        ensure_dir(self._workdir)
        for _w in (self._ppl_workdir, self._cmp_workdir):
            for _n in ("result", "log", "status", "config"):
                ensure_dir(_w / _n)

        logger.info(f"[step:prepare]eval workdir: {self._workdir}")

    def _extract_swmp(self) -> None:
        ModelPackageLocalStore().extract(self.model)

    def _gen_swds_fuse_json(self) -> Path:
        for ds in self.datasets:
            fname = DataSet.render_fuse_json(ds, force=False)
            self._fuse_jsons.append(fname)
            logger.debug(f"[gen fuse.json]{fname}")

        _base = json.load(open(self._fuse_jsons[0], "r"))
        for _f in self._fuse_jsons[0:]:
            _config = json.load(open(_f, "r"))
            _base["swds"].extend(_config["swds"])

        _f = self._workdir / EVAL_TASK_TYPE.PPL / "config" / DEFAULT_INPUT_JSON_FNAME
        ensure_file(_f, json.dumps(_base, indent=JSON_INDENT))
        return _f

    def _gen_jsonl_fuse_json(self) -> Path:
        _fuse = dict(
            backend=SWDS_BACKEND_TYPE.FUSE,
            kind=DATA_LOADER_KIND.JSONL,
            swds=[
                dict(
                    bucket= str((self._workdir / EVAL_TASK_TYPE.PPL / "result").resolve()),
                    key=dict(
                        data="current",
                    )
                )
            ]
        )
        _f = self._workdir / EVAL_TASK_TYPE.CMP / "config" / DEFAULT_INPUT_JSON_FNAME
        ensure_file(_f, json.dumps(_fuse, indent=JSON_INDENT))
        return _f

    def _do_run_cmp(self) -> None:
        self._gen_jsonl_fuse_json()
        self._do_run_cmd(EVAL_TASK_TYPE.CMP)

    def _do_run_ppl(self) -> None:
        self._do_run_cmd(EVAL_TASK_TYPE.PPL)

    def _do_run_cmd(self, typ: str) -> None:
        cmd = self._gen_docker_cmd(typ)
        logger.info(f"[run {typ}] docker run command output...")
        self._console.print(cmd)
        if not self.gencmd:
            check_call(cmd, shell=True)

    def _gen_docker_cmd(self, typ: str) -> str:
        if typ not in (EVAL_TASK_TYPE.PPL, EVAL_TASK_TYPE.CMP):
            raise Exception(f"no support {typ} to gen docker cmd")

        rootdir = "/opt/starwhale"
        rundir = self._workdir / typ

        cmd = ["docker", "run", "-it", "--net=host"]
        cmd += [
            "-v", f"{rundir}:{rootdir}",
            "-v", f"{self._model_dir}:{rootdir}/swmp",
        ]

        if self.docker_verbose:
            cmd += ["-e", "DEBUG=1"]

        _env = os.environ
        cmd += [
            "-e", f"SW_PYPI_INDEX_URL={_env.get('SW_PYPI_INDEX_URL', '')}",
            "-e", f"SW_PYPI_EXTRA_INDEX_URL={_env.get('SW_PYPI_EXTRA_INDEX_URL', '')}",
            "-e", f"SW_PYPI_TRUSTED_HOST={_env.get('SW_PYPI_TRUSTED_HOST', '')}",
            "-e", f"SW_RESET_CONDA_CONFIG={_env.get('SW_RESET_CONDA_CONFIG', '0')}",
        ]

        _mname, _mver = self.model.split(":")
        cmd += [
            "-e", f"SW_SWMP_NAME={_mname}",
            "-e", f"SW_SWMP_VERSION={_mver}",
        ]

        cmd += [typ]
        return " ".join(cmd)

    def _render_report(self) -> None:
        from starwhale.cluster.view import ClusterView
        _cv = ClusterView()
        _f = self._cmp_workdir / "result" / "current"

        with jsonlines.open(str(_f.resolve()), "r") as _reader:
            for _report in _reader:
                if not _report or not isinstance(_report, dict):
                    continue
                _cv.render_job_report(self._console, _report)