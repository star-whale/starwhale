import os
import json
import typing as t
from pathlib import Path

import yaml
from loguru import logger

from starwhale.utils import console, now_str, gen_uniq_version
from starwhale.consts import (
    JSON_INDENT,
    CURRENT_FNAME,
    DataLoaderKind,
    DefaultYAMLName,
    SWDSBackendType,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_INPUT_JSON_FNAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, EvalTaskType, RunSubDirType
from starwhale.utils.process import check_call
from starwhale.utils.progress import run_with_progress_bar
from starwhale.api._impl.model import PipelineHandler
from starwhale.core.model.model import StandaloneModel
from starwhale.core.dataset.model import Dataset
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.runtime.model import StandaloneRuntime

_CNTR_WORKDIR = "/opt/starwhale"
_STATUS = PipelineHandler.STATUS


# TODO: add DAG
class EvalExecutor(object):
    def __init__(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        project_uri: URI,
        name: str = "",
        desc: str = "",
        gencmd: bool = False,
        docker_verbose: bool = False,
    ) -> None:
        self.name = name
        self.desc = desc

        self.model_uri = URI(model_uri, expected_type=URIType.MODEL)
        self.project_uri = project_uri
        self.dataset_uris = [
            URI(u, expected_type=URIType.DATASET) for u in dataset_uris
        ]
        self.runtime_uri = URI(runtime_uri, expected_type=URIType.RUNTIME)
        self.runtime = StandaloneRuntime(self.runtime_uri)
        self.baseimage = self.runtime.store.get_docker_base_image()
        self.project_dir = Path(self.project_uri.real_request_uri)

        self.gencmd = gencmd
        self.docker_verbose = docker_verbose
        self._version = ""
        self._manifest: t.Dict[str, t.Any] = {"status": _STATUS.START}
        self._workdir = Path()
        self._model_dir = Path()
        self._runtime_dir = Path()

    def __str__(self) -> str:
        return f"Evaluation Executor: {self.name}"

    def __repr__(self) -> str:
        return f"Evaluation Executor: name -> {self.name}, version -> {self._version}"

    @logger.catch
    def run(self, phase: str = EvalTaskType.ALL) -> None:
        try:
            self._do_run(phase)
        except Exception as e:
            self._manifest["status"] = _STATUS.FAILED
            self._manifest["error_message"] = str(e)
            raise
        finally:
            self._render_manifest()

    def _do_run(self, phase: str = EvalTaskType.ALL) -> None:
        self._manifest["phase"] = phase
        self._manifest["status"] = _STATUS.RUNNING

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_workdir, 5, "prepare workdir"),
            (self._extract_swmp, 15, "extract model"),
            (self._extract_swrt, 15, "extract runtime"),
            (self._gen_swds_fuse_json, 10, "gen swds fuse json"),
        ]

        _ppl = (self._do_run_ppl, 70, "run ppl")
        _cmp = (self._do_run_cmp, 70, "run cmp")

        if phase == EvalTaskType.ALL:
            operations.extend([_ppl, _cmp])
        elif phase == EvalTaskType.PPL:
            operations.append(_ppl)
        elif phase == EvalTaskType.CMP:
            operations.append(_cmp)

        run_with_progress_bar("eval run in local...", operations)

    def _gen_version(self) -> None:
        # TODO: abstract base class or mixin class for swmp/swds/
        logger.info("[step:version]create eval job version...")
        if not self._version:
            self._version = gen_uniq_version(self.name)
        self._manifest["version"] = self._version
        self._manifest["created_at"] = now_str()  # type: ignore
        logger.info(f"[step:version]eval job version is {self._version}")

    @property
    def _ppl_workdir(self) -> Path:
        return self._workdir / EvalTaskType.PPL

    @property
    def _cmp_workdir(self) -> Path:
        return self._workdir / EvalTaskType.CMP

    def _prepare_workdir(self) -> None:
        logger.info("[step:prepare]create eval workdir...")
        # TODO: fix _workdir sequence-depent issue
        self._workdir = (
            self.project_dir
            / URIType.JOB
            / self._version[:VERSION_PREFIX_CNT]
            / self._version
        )

        ensure_dir(self._workdir)
        for _w in (self._ppl_workdir, self._cmp_workdir):
            for _n in (
                RunSubDirType.RESULT,
                RunSubDirType.DATASET,
                RunSubDirType.PPL_RESULT,
                RunSubDirType.STATUS,
                RunSubDirType.LOG,
                RunSubDirType.SWMP,
                RunSubDirType.CONFIG,
            ):
                ensure_dir(_w / _n)

        logger.info(f"[step:prepare]eval workdir: {self._workdir}")

    def _extract_swmp(self) -> None:
        _m = StandaloneModel(self.model_uri)
        self._model_dir = _m.extract()

    def _extract_swrt(self) -> None:
        self._runtime_dir = self.runtime.extract()

    def _gen_swds_fuse_json(self) -> Path:
        _fuse_jsons = []
        for _uri in self.dataset_uris:
            _store = DatasetStorage(_uri)
            fname = Dataset.render_fuse_json(_store.loc, force=False)
            _fuse_jsons.append(fname)
            logger.debug(f"[gen fuse input.json]{fname}")

        _base = json.load(open(_fuse_jsons[0], "r"))
        for _f in _fuse_jsons[0:]:
            _config = json.load(open(_f, "r"))
            _base["swds"].extend(_config["swds"])

        _bucket = f"{_CNTR_WORKDIR}/{RunSubDirType.DATASET}"
        for i in range(len(_base["swds"])):
            _base["swds"][i]["bucket"] = _bucket

        _json_f: Path = (
            self._workdir
            / EvalTaskType.PPL
            / RunSubDirType.CONFIG
            / DEFAULT_INPUT_JSON_FNAME
        )
        ensure_file(_json_f, json.dumps(_base, indent=JSON_INDENT))
        return _json_f

    def _gen_jsonl_fuse_json(self) -> Path:
        _fuse = dict(
            backend=SWDSBackendType.FUSE,
            kind=DataLoaderKind.JSONL,
            swds=[
                dict(
                    bucket=f"{_CNTR_WORKDIR}/{RunSubDirType.PPL_RESULT}",
                    key=dict(
                        data=CURRENT_FNAME,
                    ),
                )
            ],
        )
        _f = (
            self._workdir
            / EvalTaskType.CMP
            / RunSubDirType.CONFIG
            / DEFAULT_INPUT_JSON_FNAME
        )
        ensure_file(_f, json.dumps(_fuse, indent=JSON_INDENT))
        return _f

    def _do_run_cmp(self) -> None:
        self._gen_jsonl_fuse_json()
        self._do_run_cmd(EvalTaskType.CMP)

    def _do_run_ppl(self) -> None:
        self._do_run_cmd(EvalTaskType.PPL)

    def _do_run_cmd(self, typ: str) -> None:
        cmd = self._gen_run_container_cmd(typ)
        logger.info(f"[run {typ}] docker run command output...")
        console.rule(f":elephant: {typ} docker cmd", align="left")
        console.print(f"{cmd}\n")
        console.print(
            f":fish: eval run:{typ} dir @ [green blink]{self._workdir}/{typ}[/]"
        )
        if not self.gencmd:
            check_call(f"docker pull {self.baseimage}", shell=True)
            check_call(cmd, shell=True)

    def _gen_run_container_cmd(self, typ: str) -> str:
        if typ not in (EvalTaskType.PPL, EvalTaskType.CMP):
            raise Exception(f"no support {typ} to gen docker cmd")

        rundir = self._workdir / typ

        cmd = [
            "docker",
            "run",
            "--net=host",
            "--rm",
            "--name",
            f"{self._version}-{typ}",
        ]
        cmd += [
            "-v",
            f"{rundir}:{_CNTR_WORKDIR}",
            "-v",
            f"{self._model_dir}/src:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/src",
            "-v",
            f"{self._model_dir}/{DefaultYAMLName.MODEL}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/{DefaultYAMLName.MODEL}",
            "-v",
            f"{self._runtime_dir}/dep:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/dep",
            "-v",
            f"{self._runtime_dir}/{DEFAULT_MANIFEST_NAME}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/{DEFAULT_MANIFEST_NAME}",
        ]

        if typ == EvalTaskType.PPL:
            cmd += [
                "-v",
                f"{self.project_dir / URIType.DATASET}:{_CNTR_WORKDIR}/{RunSubDirType.DATASET}",
            ]
        elif typ == EvalTaskType.CMP:
            cmd += [
                "-v",
                f"{self._ppl_workdir / RunSubDirType.RESULT}:{_CNTR_WORKDIR}/{RunSubDirType.PPL_RESULT}",
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

        cmd += [self.baseimage, typ]
        return " ".join(cmd)

    def _render_manifest(self) -> None:
        _status = True
        for _d in (self._ppl_workdir, self._cmp_workdir):
            _f = _d / RunSubDirType.STATUS / CURRENT_FNAME
            if not _f.exists():
                continue
            _status = _status and (_f.open().read().strip() == _STATUS.SUCCESS)

        self._manifest.update(
            dict(
                name=self.name,
                desc=self.desc,
                model=self.model_uri.full_uri,
                datasets=[u.full_uri for u in self.dataset_uris],
                runtime=self.runtime_uri.full_uri,
                status=_STATUS.SUCCESS if _status else _STATUS.FAILED,
                finished_at=now_str(),  # type: ignore
            )
        )
        _f = self._workdir / DEFAULT_MANIFEST_NAME
        ensure_file(_f, yaml.safe_dump(self._manifest, default_flow_style=False))
