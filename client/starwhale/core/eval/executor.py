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
    DefaultYAMLName,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_INPUT_JSON_FNAME,
    CNTR_DEFAULT_PIP_CACHE_DIR,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, EvalTaskType, RunSubDirType
from starwhale.consts.env import SWEnv
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
        version: str = "",
        name: str = "",
        job_name: str = "default",
        desc: str = "",
        gencmd: bool = False,
        use_docker: bool = False,
    ) -> None:
        self.name = name
        self.job_name = job_name
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

        self._version = version
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

    def run(
        self, typ: str = EvalTaskType.ALL, step: str = "", task_index: int = 0
    ) -> str:
        try:
            self._do_run(typ, step, task_index)
        except Exception as e:
            self._manifest["status"] = _STATUS.FAILED
            self._manifest["error_message"] = str(e)
            raise
        finally:
            self._render_manifest()

        return self._version

    # TODO: is it necessary to support single task at local mode??
    def _do_run(self, typ: str, step: str, task_index: int) -> None:
        self._manifest["type"] = typ
        self._manifest["status"] = _STATUS.RUNNING
        if typ is not EvalTaskType.ALL:
            if not step:
                raise FieldTypeOrValueError("step is none")
            self._manifest["step"] = step
            self._manifest["task_index"] = task_index

        operations = [
            (self._gen_version, 5, "gen version"),
            (self._prepare_workdir, 5, "prepare workdir"),
            (self._extract_swmp, 15, "extract model"),
            (self._extract_swrt, 15, "extract runtime"),
            (self._gen_swds_fuse_json, 10, "gen swds fuse json"),
            (self._init_storage, 20, "init storage"),
            (self._do_run_eval_job, 70, "run eval job"),
            (self._finally, 95, "do finally"),
        ]

        run_with_progress_bar("eval run in local...", operations)

    def _init_storage(self):
        # TODO: init storageAPI by job version 2022/07/28
        pass

    def _finally(self):
        # TODO
        pass

    def _do_run_eval_job(self):
        _type = self._manifest["type"]
        if _type is not EvalTaskType.ALL:
            _step = self._manifest["step"]
            _task_index = self._manifest["task_index"]
            self._do_run_cmd(_type, _step, _task_index)
        else:
            self._do_run_cmd(_type, "", 0)

    def _gen_version(self) -> None:
        # TODO: abstract base class or mixin class for swmp/swds/
        logger.info("[step:version]create eval job version...")
        if not self._version:
            self._version = gen_uniq_version(self.name)
        self._manifest["version"] = self._version
        self._manifest["created_at"] = now_str()  # type: ignore
        logger.info(f"[step:version]eval job version is {self._version}")

    def _prepare_workdir(self) -> None:
        logger.info("[step:prepare]create eval workdir...")
        # TODO: fix _workdir sequence-dependency issue
        self._workdir = (
            self.project_dir
            / URIType.EVALUATION
            / self._version[:VERSION_PREFIX_CNT]
            / self._version
        )

        ensure_dir(self._workdir)
        for _w in (self._workdir,):
            for _n in (
                # RunSubDirType.RESULT,
                # RunSubDirType.DATASET,
                # RunSubDirType.PPL_RESULT,
                # RunSubDirType.STATUS,
                # RunSubDirType.LOG,
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
            console.print("进入解压")
            model_uri = URI(self.model_uri, expected_type=URIType.MODEL)
            _m = StandaloneModel(model_uri)
            self._model_dir = _m.extract() / "src"

    def _extract_swrt(self) -> None:
        if self.runtime and self.use_docker:
            self._runtime_dir = self.runtime.extract()
        else:
            self._runtime_dir = Path()

    # TODO: this file and dir must exist because current ds implementation, wait replaced by datastore
    #  need new Dataset and Dataloader, so wo can instance dataloader by task index and total num.
    #  cool! this can be removed form here.
    def _gen_swds_fuse_json(self) -> Path:
        _fuse_jsons = []
        for _uri in self.dataset_uris:
            _store = DatasetStorage(_uri)
            _file_name = Dataset.render_fuse_json(_store.loc, force=False)
            _fuse_jsons.append(_file_name)
            logger.debug(f"[gen fuse input.json]{_file_name}")

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
            # / EvalTaskType.PPL
            / RunSubDirType.CONFIG
            / DEFAULT_INPUT_JSON_FNAME
        )
        ensure_file(_json_f, json.dumps(_base, indent=JSON_INDENT))
        return _json_f

    def _do_run_cmd(self, typ: str, step: str, task_index: int) -> None:
        if self.use_docker:
            # TODO
            self._do_run_cmd_in_container(typ, step, task_index)
        else:
            self._do_run_cmd_in_host(typ, step, task_index)

    def _do_run_cmd_in_host(self, typ: str, step: str, task_index: int) -> None:
        StandaloneModel.eval_user_handler(
            project=self.project_uri.project,
            version=self._version,
            typ=typ,
            src_dir=self._model_dir,
            workdir=self._workdir,
            dataset_uris=[u.full_uri for u in self.dataset_uris],
            step=step,
            task_index=task_index,
            kw={
                # TODO: replace when new dataset completed
                "input_config": self._workdir
                / RunSubDirType.CONFIG
                / DEFAULT_INPUT_JSON_FNAME,
            },
        )

    def _do_run_cmd_in_container(self, typ: str, step: str, task_index: int) -> None:
        cmd = self._gen_run_container_cmd(typ, step, task_index)
        console.rule(f":elephant: {typ} docker cmd", align="left")
        console.print(f"{cmd}\n")
        console.print(
            f":fish: eval run:{typ} dir @ [green blink]{self._workdir}/{typ}[/]"
        )
        if not self.gencmd:
            check_call(f"docker pull {self.baseimage}", shell=True)
            check_call(cmd, shell=True)

    def _gen_run_container_cmd(self, typ: str, step: str, task_index: int) -> str:
        if typ not in (EvalTaskType.ALL, EvalTaskType.SINGLE):
            raise Exception(f"no support {typ} to gen docker cmd")

        _run_dir = self._workdir

        cmd = [
            "docker",
            "run",
            "--net=host",
            "--rm",
            "--name",
            f"{self._version}-{step}-{task_index}",
            "-e",
            "DEBUG=1",
        ]

        cmd += [
            "-v",
            f"{_run_dir}:{_CNTR_WORKDIR}",
            "-v",
            f"{self.project_dir / URIType.DATASET}:{_CNTR_WORKDIR}/{RunSubDirType.DATASET}",
            "-v",
            f"{self._model_dir}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/src",
            "-v",
            f"{self._model_dir}/{DefaultYAMLName.MODEL}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/{DefaultYAMLName.MODEL}",
            "-v",
            f"{self._runtime_dir}/dep:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/dep",
            "-v",
            f"{self._runtime_dir}/{DEFAULT_MANIFEST_NAME}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/{DEFAULT_MANIFEST_NAME}",
        ]

        if typ == EvalTaskType.SINGLE:
            cmd.extend(["-e", f"SW_TASK_STEP={step}"])
            cmd.extend(["-e", f"SW_TASK_INDEX={task_index}"])

        cmd.extend(["-e", f"{SWEnv.project}={self.project_uri.project}"])
        cmd.extend(["-e", f"{SWEnv.eval_version}={self._version}"])
        # cmd.extend(["-e", f"SW_DATASETS={[u.full_uri for u in self.dataset_uris]}"])

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
        for _d in (self._workdir,):
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
