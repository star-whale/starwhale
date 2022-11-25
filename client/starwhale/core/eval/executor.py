import os
import typing as t
from pathlib import Path

from loguru import logger

from starwhale.utils import console, now_str, is_darwin, gen_uniq_version
from starwhale.consts import DefaultYAMLName, CNTR_DEFAULT_PIP_CACHE_DIR
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import URIType, EvalTaskType, RunSubDirType
from starwhale.consts.env import SWEnv
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.process import check_call
from starwhale.utils.progress import run_with_progress_bar
from starwhale.core.eval.store import EvaluationStorage
from starwhale.core.model.model import StandaloneModel
from starwhale.core.runtime.model import StandaloneRuntime

_CNTR_WORKDIR = "/opt/starwhale"


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
        step: str = "",
        task_index: int = 0,
        gencmd: bool = False,
        use_docker: bool = False,
    ) -> None:
        self.name = name
        self.job_name = job_name

        if step:
            self.type = EvalTaskType.SINGLE
        else:
            self.type = EvalTaskType.ALL
        self.step = step
        self.task_index = task_index

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

        if not version:
            logger.info("[step:init]create eval job version...")
            self._version = gen_uniq_version()
            logger.info(f"[step:init]eval job version is {self._version}")
        else:
            self._version = version

        self.sw_config = SWCliConfigMixed()

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
                    "use docker as the evaluation job environment in macOS system (Apple Silicon processor)"
                )

    def __str__(self) -> str:
        return f"Evaluation Executor: {self.name}"

    def __repr__(self) -> str:
        return f"Evaluation Executor: name -> {self.name}, version -> {self._version}"

    def run(self) -> str:
        try:
            self._do_run()
        except Exception as e:
            logger.error(f"execute evaluation error, error is:{str(e)}")
            raise e
        return self._version

    def _do_run(self) -> None:
        operations = [
            (self._prepare_workdir, 5, "prepare workdir"),
            (self._extract_swmp, 15, "extract model"),
            (self._extract_swrt, 15, "extract runtime"),
            (self._do_run_cmd, 70, "run eval job"),
        ]

        run_with_progress_bar("eval run in local...", operations)

    def _prepare_workdir(self) -> None:
        logger.info("[step:prepare]create eval workdir...")
        # TODO: fix _workdir sequence-dependency issue
        self._workdir = EvaluationStorage.local_run_dir(
            self.project_uri.project, self._version
        )

        ensure_dir(self._workdir)
        ensure_dir(self._workdir / RunSubDirType.SWMP)

        logger.info(f"[step:prepare]eval workdir: {self._workdir}")

    def _extract_swmp(self) -> None:
        _workdir = Path(self.model_uri)
        _model_yaml_path = _workdir / DefaultYAMLName.MODEL

        if _workdir.exists() and _model_yaml_path.exists() and not self.use_docker:
            self._model_dir = _workdir
        else:
            console.print("start to uncompress swmp...")
            model_uri = URI(self.model_uri, expected_type=URIType.MODEL)
            _m = StandaloneModel(model_uri)
            self._model_dir = _m.extract() / "src"

    def _extract_swrt(self) -> None:
        if self.runtime and self.use_docker:
            self._runtime_dir = self.runtime.extract()
        else:
            self._runtime_dir = Path()

    def _do_run_cmd(self) -> None:
        if self.use_docker:
            self._do_run_cmd_in_container()
        else:
            self._do_run_cmd_in_host()

    def _do_run_cmd_in_host(self) -> None:
        StandaloneModel.eval_user_handler(
            project=self.project_uri.project,
            version=self._version,
            workdir=self._model_dir,
            dataset_uris=[u.full_uri for u in self.dataset_uris],
            step_name=self.step,
            task_index=self.task_index,
            # other runtime info
            base_info=dict(
                name=self.name,
                desc=self.desc,
                model=self.model_uri,
                model_dir=str(self._model_dir),
                datasets=[u.full_uri for u in self.dataset_uris],
                runtime=self.runtime_uri,
                created_at=now_str(),
            ),
        )

    def _do_run_cmd_in_container(self) -> None:
        cmd = self._gen_run_container_cmd(self.type, self.step, self.task_index)
        console.rule(f":elephant: {self.type} docker cmd", align="left")
        console.print(f"{cmd}\n")
        console.print(
            f":fish: eval run:{self.type} dir @ [green blink]{self._workdir}/{self.type}[/]"
        )
        if not self.gencmd:
            check_call(f"docker pull {self.baseimage}", shell=True)
            check_call(cmd, shell=True)

    def _gen_run_container_cmd(self, typ: str, step: str, task_index: int) -> str:
        if typ not in (EvalTaskType.ALL, EvalTaskType.SINGLE):
            raise Exception(f"no support {typ} to gen docker cmd")
        _entrypoint = "run"
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
            "-l",
            f"version={self._version}",
        ]

        cmd += [
            "-v",
            f"{_run_dir}:{_CNTR_WORKDIR}",
            "-v",
            f"{self.sw_config.rootdir}:/root/.starwhale",
            "-v",
            f"{self.sw_config.object_store_dir}:{self.sw_config.object_store_dir}",
            "-v",
            f"{self._model_dir}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/src",
            "-v",
            f"{self._model_dir}/{DefaultYAMLName.MODEL}:{_CNTR_WORKDIR}/{RunSubDirType.SWMP}/{DefaultYAMLName.MODEL}",
            "-v",
            f"{self._runtime_dir}:{_CNTR_WORKDIR}/{RunSubDirType.SWRT}",
        ]

        if typ == EvalTaskType.SINGLE:
            cmd.extend(["-e", f"SW_TASK_STEP={step}"])
            cmd.extend(["-e", f"SW_TASK_INDEX={task_index}"])

        logger.debug(f"config:{self.sw_config._current_instance_obj}")

        cmd.extend(["-e", f"{SWEnv.project}={self.project_uri.project}"])
        cmd.extend(["-e", f"{SWEnv.eval_version}={self._version}"])
        cmd.extend(
            [
                "-e",
                f"{SWEnv.instance_uri}={self.sw_config._current_instance_obj['uri']}",
                "-e",
                f"{SWEnv.instance_token}={self.sw_config._current_instance_obj.get('sw_token', '')}",
            ]
        )
        # TODO: support multi dataset
        cmd.extend(
            [
                "-e",
                f"{SWEnv.dataset_uri}={' '.join([ds.full_uri for ds in self.dataset_uris])}",
            ]
        )

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

        cmd += [self.baseimage, _entrypoint]
        return " ".join(cmd)
