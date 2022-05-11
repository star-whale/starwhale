import typing as t
import yaml
import json
import os
from collections import namedtuple
from pathlib import Path
import jsonlines

from loguru import logger

from starwhale.utils import gen_uniq_version, console, now_str
from .store import EvalLocalStorage
from starwhale.consts import (
    DATA_LOADER_KIND,
    DEFAULT_INPUT_JSON_FNAME,
    DEFAULT_MANIFEST_NAME,
    JSON_INDENT,
    SWDS_BACKEND_TYPE,
    VERSION_PREFIX_CNT,
    CURRENT_FNAME,
)
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.error import SWObjNameFormatError
from starwhale.swds.dataset import DataSet
from starwhale.swmp.store import ModelPackageLocalStore
from starwhale.utils.process import check_call
from starwhale.utils.progress import run_with_progress_bar
from starwhale.api._impl.model import PipelineHandler

DEFAULT_SW_TASK_RUN_IMAGE = "starwhaleai/starwhale:latest"
EVAL_TASK_TYPE = namedtuple("EVAL_TASK_TYPE", ["ALL", "PPL", "CMP"])(
    "all", "ppl", "cmp"
)
_CNTR_WORKDIR = "/opt/starwhale"
_RUN_SUBDIR = namedtuple(
    "_RUN_SUBDIR",
    ["RESULT", "DATASET", "PPL_RESULT", "STATUS", "LOG", "SWMP", "CONFIG"],
)("result", "dataset", "ppl_result", "status", "log", "swmp", "config")
_STATUS = PipelineHandler.STATUS


class EvalExecutor(object):
    def __init__(
        self,
        model: str,
        datasets: t.List[str],
        baseimage: str = DEFAULT_SW_TASK_RUN_IMAGE,
        name: str = "",
        desc: str = "",
        gencmd: bool = False,
        docker_verbose: bool = False,
    ) -> None:
        self.name = name
        self.desc = desc
        self.model = model
        self.datasets = list(datasets)
        self.baseimage = baseimage
        self.gencmd = gencmd
        self.docker_verbose = docker_verbose
        self._store = EvalLocalStorage()

        self._console = console
        self._version = ""
        self._manifest = {"status": _STATUS.START}
        self._workdir = Path()
        self._model_dir = Path()

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
    def run(self, phase: str = EVAL_TASK_TYPE.ALL):
        try:
            self._do_run(phase)
        except Exception as e:
            self._manifest["status"] = _STATUS.FAILED
            self._manifest["error_message"] = str(e)
            raise
        finally:
            self._render_manifest()

    def _do_run(self, phase: str = EVAL_TASK_TYPE.ALL):
        self._manifest["phase"] = phase
        self._manifest["status"] = _STATUS.RUNNING

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_workdir, 5, "prepare workdir"),
            (self._extract_swmp, 15, "extract swmp"),
            (self._gen_swds_fuse_json, 10, "gen swds fuse json"),
        ]

        _ppl = (self._do_run_ppl, 70, "run ppl")
        _cmp = (self._do_run_cmp, 70, "run cmp")

        if phase == EVAL_TASK_TYPE.ALL:
            operations.extend([_ppl, _cmp])
        elif phase == EVAL_TASK_TYPE.PPL:
            operations.append(_ppl)
        elif phase == EVAL_TASK_TYPE.CMP:
            operations.append(_cmp)

        if phase != EVAL_TASK_TYPE.PPL and not self.gencmd:
            operations.append((self._render_report, 15, "render report"))

        run_with_progress_bar("eval run in local...", operations, self._console)

    def _gen_version(self) -> None:
        # TODO: abstract base class or mixin class for swmp/swds/
        logger.info("[step:version]create eval job version...")
        if not self._version:
            self._version = gen_uniq_version(self.name)
        self._manifest["version"] = self._version
        self._manifest["created_at"] = now_str()
        logger.info(f"[step:version]eval job version is {self._version}")

    @property
    def _ppl_workdir(self) -> Path:
        return self._workdir / EVAL_TASK_TYPE.PPL

    @property
    def _cmp_workdir(self) -> Path:
        return self._workdir / EVAL_TASK_TYPE.CMP

    def _prepare_workdir(self) -> None:
        logger.info("[step:prepare]create eval workdir...")
        # TODO: fix _workdir sequence-depent issue
        self._workdir = (
            self._store.eval_run_dir
            / self._version[:VERSION_PREFIX_CNT]
            / self._version
        )

        ensure_dir(self._workdir)
        for _w in (self._ppl_workdir, self._cmp_workdir):
            for _n in _RUN_SUBDIR:
                ensure_dir(_w / _n)

        logger.info(f"[step:prepare]eval workdir: {self._workdir}")

    def _extract_swmp(self) -> None:
        self._model_dir = ModelPackageLocalStore().extract(self.model)

    def _gen_swds_fuse_json(self) -> Path:
        _fuse_jsons = []
        for ds in self.datasets:
            fname = DataSet.render_fuse_json(ds, force=False)
            _fuse_jsons.append(fname)
            logger.debug(f"[gen fuse input.json]{fname}")

        _base = json.load(open(_fuse_jsons[0], "r"))
        for _f in _fuse_jsons[0:]:
            _config = json.load(open(_f, "r"))
            _base["swds"].extend(_config["swds"])

        _bucket = f"{_CNTR_WORKDIR}/{_RUN_SUBDIR.DATASET}"
        for i in range(len(_base["swds"])):
            _base["swds"][i]["bucket"] = _bucket

        _f = (
            self._workdir
            / EVAL_TASK_TYPE.PPL
            / _RUN_SUBDIR.CONFIG
            / DEFAULT_INPUT_JSON_FNAME
        )
        ensure_file(_f, json.dumps(_base, indent=JSON_INDENT))
        return _f

    def _gen_jsonl_fuse_json(self) -> Path:
        _fuse = dict(
            backend=SWDS_BACKEND_TYPE.FUSE,
            kind=DATA_LOADER_KIND.JSONL,
            swds=[
                dict(
                    bucket=f"{_CNTR_WORKDIR}/{_RUN_SUBDIR.PPL_RESULT}",
                    key=dict(
                        data=CURRENT_FNAME,
                    ),
                )
            ],
        )
        _f = (
            self._workdir
            / EVAL_TASK_TYPE.CMP
            / _RUN_SUBDIR.CONFIG
            / DEFAULT_INPUT_JSON_FNAME
        )
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
        self._console.rule(f":elephant: {typ} docker cmd", align="left")
        self._console.print(f"{cmd}\n")
        self._console.print(
            f":fish: eval run:{typ} dir @ [green blink]{self._workdir}/{typ}[/]"
        )
        if not self.gencmd:
            check_call(cmd, shell=True)

    def _gen_docker_cmd(self, typ: str) -> str:
        if typ not in (EVAL_TASK_TYPE.PPL, EVAL_TASK_TYPE.CMP):
            raise Exception(f"no support {typ} to gen docker cmd")

        rundir = self._workdir / typ

        cmd = ["docker", "run", "--net=host"]
        cmd += [
            "-v",
            f"{rundir}:{_CNTR_WORKDIR}",
            "-v",
            f"{self._model_dir}:{_CNTR_WORKDIR}/{_RUN_SUBDIR.SWMP}",
        ]

        if typ == EVAL_TASK_TYPE.PPL:
            cmd += [
                "-v",
                f"{self._store.dataset_dir}:{_CNTR_WORKDIR}/{_RUN_SUBDIR.DATASET}",
            ]
        elif typ == EVAL_TASK_TYPE.CMP:
            cmd += [
                "-v",
                f"{self._ppl_workdir / _RUN_SUBDIR.RESULT }:{_CNTR_WORKDIR}/{_RUN_SUBDIR.PPL_RESULT}",
            ]

        if self.docker_verbose:
            cmd += ["-e", "DEBUG=1"]

        _env = os.environ
        for _ee in (
            "SW_PYPI_INDEX_URL",
            "SW_PYPI_EXTRA_INDEX_URL",
            "SW_PYPI_TRUSTED_HOST",
            "SW_RESET_CONDA_CONFIG",
        ):
            if _ee not in _env:
                continue
            cmd.extend(["-e", f"{_ee}={_env[_ee]}"])

        _mname, _mver = self.model.split(":")
        cmd += [
            "-e",
            f"SW_SWMP_NAME={_mname}",
            "-e",
            f"SW_SWMP_VERSION={_mver}",
        ]

        cmd += [self.baseimage, typ]
        return " ".join(cmd)

    def _render_report(self) -> None:
        _f = self._cmp_workdir / _RUN_SUBDIR.RESULT / CURRENT_FNAME
        render_cmp_report(_f)

        self._console.rule("[bold green]More Details[/]")
        self._console.print(
            f":helicopter: eval version: [green]{self._version}[/], :hedgehog: workdir: {self._workdir.resolve()} \n"
        )

    def _render_manifest(self):
        _status = True
        for _d in (self._ppl_workdir, self._cmp_workdir):
            _f = _d / _RUN_SUBDIR.STATUS / CURRENT_FNAME
            if not _f.exists():
                continue
            _status = _status and (_f.open().read().strip() == _STATUS.SUCCESS)

        self._manifest.update(
            dict(
                name=self.name,
                desc=self.desc,
                model=self.model,
                status=_STATUS.SUCCESS if _status else _STATUS.FAILED,
                datasets=self.datasets,
                baseimage=self.baseimage,
                finished_at=now_str(),
            )
        )
        _f = self._workdir / DEFAULT_MANIFEST_NAME
        ensure_file(_f, yaml.dump(self._manifest, default_flow_style=False))


def render_cmp_report(rpath: Path) -> None:
    from starwhale.cluster.view import ClusterView

    _cv = ClusterView()

    with jsonlines.open(str(rpath.resolve()), "r") as _reader:
        for _report in _reader:
            if not _report or not isinstance(_report, dict):
                continue
            _cv.render_job_report(_report)
