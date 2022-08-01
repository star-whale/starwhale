import os
import json
import typing as t
from pathlib import Path

import yaml
from loguru import logger

from starwhale.utils import console, now_str, is_darwin, gen_uniq_version
from starwhale.consts import (
    JSON_INDENT,
    CURRENT_FNAME,
    DataLoaderKind,
    DefaultYAMLName,
    SWDSBackendType,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_INPUT_JSON_FNAME,
    CNTR_DEFAULT_PIP_CACHE_DIR,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, EvalTaskType, RunSubDirType
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
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
class EvalExecutor:
    def __init__(
        self,
        model_uri: str,
        dataset_uris: t.List[str],
        runtime_uri: str,
        project_uri: URI,
        name: str = "",
        desc: str = "",
        gencmd: bool = False,
        use_docker: bool = False,
    ) -> None:
        self.name = name
        self.desc = desc
        self.model_uri = model_uri
        self.project_uri = project_uri
        self.dataset_uris = [
            URI(u, expected_type=URIType.DATASET) for u in dataset_uris
        ]

        self.runtime_uri = runtime_uri

        if runtime_uri:
            self.runtime: t.Optional[StandaloneRuntime] = StandaloneRuntime(
                URI(runtime_uri, expected_type=URIType.RUNTIME)
            )
            self.baseimage = self.runtime.store.get_docker_base_image()
        else:
            self.runtime = None
            self.baseimage = ""

        self.project_dir = Path(self.project_uri.real_request_uri)

        self.gencmd = gencmd
        self.use_docker = use_docker

        self._version = gen_uniq_version(self.name)
        self._manifest: t.Dict[str, t.Any] = {"status": _STATUS.START}
        self._workdir = Path()
        self._model_dir = Path()
        self._runtime_dir = Path()

        self._do_validate()

    def _do_validate(self) -> None:
        if self.use_docker:
            if not self.runtime_uri:
                raise FieldTypeOrValueError("runtime_uri is none")
            if is_darwin(arm=True):
                raise NoSupportError(
                    "use docker as the evaluation job environment in MacOSX system (Apple Silicon processor)"
                )

    def __str__(self) -> str:
        return f"Evaluation Executor: {self.name}"

    def __repr__(self) -> str:
        return f"Evaluation Executor: name -> {self.name}, version -> {self._version}"

    def run(self, phase: str = EvalTaskType.ALL) -> str:
        try:
            self._do_run(phase)
        except Exception as e:
            self._manifest["status"] = _STATUS.FAILED
            self._manifest["error_message"] = str(e)
            raise
        finally:
            self._render_manifest()

        return self._version

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
        # TODO: fix _workdir sequence-dependency issue
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
        _workdir = Path(self.model_uri)
        _model_yaml_path = _workdir / DefaultYAMLName.MODEL

        if _workdir.exists() and _model_yaml_path.exists() and not self.use_docker:
            self._model_dir = _workdir
        else:
            model_uri = URI(self.model_uri, expected_type=URIType.MODEL)
            _m = StandaloneModel(model_uri)
            self._model_dir = _m.extract() / "src"

    def _extract_swrt(self) -> None:
        if self.runtime and self.use_docker:
            self._runtime_dir = self.runtime.extract()
        else:
            self._runtime_dir = Path()

    def _gen_swds_fuse_json(self) -> Path:
        _fuse_jsons = []
        for _uri in self.dataset_uris:
            _store = DatasetStorage(_uri)
            fname = Dataset.render_fuse_json(_store.loc, force=False)
            _fuse_jsons.append(fname)
            logger.debug(f"[gen fuse input.json]{fname}")

        _base = json.load(open(_fuse_jsons[0], "r"))
        for _f in _fuse_jsons[1:]:
            _config = json.load(open(_f, "r"))
            _base["swds"].extend(_config["swds"])

        if self.use_docker:
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
        if self.use_docker:
            _base_dir = f"{_CNTR_WORKDIR}/{RunSubDirType.PPL_RESULT}"
        else:
            _base_dir = str(self._ppl_workdir / RunSubDirType.RESULT)

        _fuse = dict(
            backend=SWDSBackendType.FUSE,
            kind=DataLoaderKind.JSONL,
            swds=[
                dict(
                    bucket=_base_dir,
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
        if self.use_docker:
            self._do_run_cmd_in_container(typ)
        else:
            self._do_run_cmd_in_host(typ)

    def _do_run_cmd_in_host(self, typ: str) -> None:
        from starwhale.core.model.model import StandaloneModel

        if typ == EvalTaskType.PPL:
            _base_dir = self._ppl_workdir
        elif typ == EvalTaskType.CMP:
            _base_dir = self._cmp_workdir
        else:
            raise NoSupportError(typ)

        StandaloneModel.eval_user_handler(
            typ=typ,
            workdir=self._model_dir,
            kw={
                "status_dir": _base_dir / RunSubDirType.STATUS,
                "log_dir": _base_dir / RunSubDirType.LOG,
                "result_dir": _base_dir / RunSubDirType.RESULT,
                "input_config": _base_dir
                / RunSubDirType.CONFIG
                / DEFAULT_INPUT_JSON_FNAME,
            },
        )

    def _do_run_cmd_in_container(self, typ: str) -> None:
        cmd = self._gen_run_container_cmd(typ)
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
            "-e",
            "DEBUG=1",
        ]
        cmd += [
            "-v",
            f"{rundir}:{_CNTR_WORKDIR}",
            "-v",
            f"{self._model_dir}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/src",
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

        cntr_cache_dir = os.environ.get("SW_PIP_CACHE_DIR", CNTR_DEFAULT_PIP_CACHE_DIR)
        host_cache_dir = os.path.expanduser("~/.cache/starwhale-pip")
        cmd += ["-v", f"{host_cache_dir}:{cntr_cache_dir}"]

        _env = os.environ
        for _ee in (
            "SW_PYPI_INDEX_URL",
            "SW_PYPI_EXTRA_INDEX_URL",
            "SW_PYPI_TRUSTED_HOST",
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
                model=self.model_uri,
                model_dir=str(self._model_dir),
                datasets=[u.full_uri for u in self.dataset_uris],
                runtime=self.runtime_uri,
                status=_STATUS.SUCCESS if _status else _STATUS.FAILED,
                finished_at=now_str(),  # type: ignore
            )
        )
        _f = self._workdir / DEFAULT_MANIFEST_NAME
        ensure_file(_f, yaml.safe_dump(self._manifest, default_flow_style=False))
