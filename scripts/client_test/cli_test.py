from __future__ import annotations

import os
import sys
import typing as t
import logging
import subprocess
from time import sleep
from pathlib import Path
from concurrent.futures import as_completed, ThreadPoolExecutor

from cmds import DatasetExpl
from tenacity import retry
from cmds.job_cmd import Job
from tenacity.stop import stop_after_attempt
from tenacity.wait import wait_random
from cmds.base.invoke import check_invoke
from cmds.project_cmd import Project
from cmds.instance_cmd import Instance
from cmds.artifacts_cmd import Model, Dataset, Runtime

from starwhale import URI
from starwhale.utils import config
from starwhale.base.type import DatasetChangeMode

CURRENT_DIR = os.path.dirname(__file__)
SCRIPT_DIR = os.path.abspath(os.path.join(CURRENT_DIR, os.pardir))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, os.pardir))
WORK_DIR = os.environ.get("WORK_DIR")
if not WORK_DIR:
    raise RuntimeError("WORK_DIR NOT FOUND")
STATUS_SUCCESS = {"SUCCESS", "success"}
STATUS_FAIL = {"FAIL", "fail", "CANCELED"}

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

CPU_EXAMPLES: t.Dict[str, t.Dict[str, t.Any]] = {
    "mnist": {
        "run_handler": "mnist.evaluator:MNISTInference.cmp",
        "workdir": f"{ROOT_DIR}/example/mnist",
        "datasets": [
            DatasetExpl("mnist_bin", "mnist.dataset:iter_mnist_item"),
            DatasetExpl(
                "mnist_link_raw", "mnist.dataset:LinkRawDatasetProcessExecutor"
            ),
        ],
    },
    "cifar10": {
        "run_handler": "cifar.evaluator:CIFAR10Inference.cmp",
        "workdir": f"{ROOT_DIR}/example/cifar10",
        "datasets": [DatasetExpl("cifar10", "")],
    },
    "nmt": {
        "run_handler": "nmt.evaluator:NMTPipeline.cmp",
        "workdir": f"{ROOT_DIR}/example/nmt",
        "datasets": [DatasetExpl("nmt", "")],
    },
    "ag_news": {
        "run_handler": "tcan.evaluator:TextClassificationHandler.cmp",
        "workdir": f"{ROOT_DIR}/example/text_cls_AG_NEWS",
        "datasets": [DatasetExpl("ag_news", "")],
    },
    "ucf101": {
        "run_handler": "ucf101.evaluator:UCF101PipelineHandler.cmp",
        "workdir": f"{ROOT_DIR}/example/ucf101",
        "datasets": [DatasetExpl("ucf101", "")],
    },
}

GPU_EXAMPLES: t.Dict[str, t.Dict[str, t.Any]] = {
    "pfp": {
        "run_handler": "pfp.evaluator:cmp",
        "workdir": f"{ROOT_DIR}/example/PennFudanPed",
        "datasets": [DatasetExpl("pfp", "")],
    },
    "speech_command": {
        "run_handler": "sc.evaluator:evaluate_speech",
        "workdir": f"{ROOT_DIR}/example/speech_command",
        "datasets": [
            DatasetExpl("speech_command", ""),
            DatasetExpl(
                "speech_command_link", "sc.dataset:LinkRawDatasetBuildExecutor"
            ),
        ],
    },
}

ALL_EXAMPLES = {**CPU_EXAMPLES, **GPU_EXAMPLES}

RUNTIMES: t.Dict[str, t.Dict[str, t.Union[str, t.List[str]]]] = {
    "pytorch": {
        "workdir": f"{WORK_DIR}/example/runtime/pytorch-e2e",
        "yamls": [
            "runtime-3-7.yaml",
            "runtime-3-8.yaml",
            "runtime-3-9.yaml",
            "runtime-3-10.yaml",
        ],
    },
}


class TestCli:
    instance_api = Instance()
    project_api = Project()
    model_api = Model()
    dataset_api = Dataset()
    runtime_api = Runtime()
    job_api = Job()

    def __init__(
        self,
        work_dir: str,
        thread_pool: ThreadPoolExecutor,
        server_url: t.Optional[str],
        server_project: str = "starwhale",
    ) -> None:
        self._work_dir = work_dir
        self.executor = thread_pool
        self.server_url = server_url
        self.server_project = server_project
        self.datasets: t.Dict[str, t.List[URI]] = {}
        self.runtimes: t.Dict[str, t.List[URI]] = {}
        self.models: t.Dict[str, URI] = {}
        if self.server_url:
            logger.info(f"login to server {self.server_url} ...")
            assert self.instance_api.login(url=self.server_url)

        self.cloud_target_project_uri = f"cloud://server/project/{self.server_project}"
        config.update_swcli_config(
            **{
                "link_auths": [
                    {
                        "type": "s3",
                        "ak": "starwhale",
                        "sk": "starwhale",
                        "endpoint": "http://10.131.0.1:9000",
                        "bucket": "users",
                        "region": "local",
                        "connect_timeout": 10.0,
                        "read_timeout": 100.0,
                    },
                ]
            }
        )

    def build_dataset(self, name: str, _workdir: str, ds_expl: DatasetExpl) -> t.Any:
        self.select_local_instance()
        ret_uri = Dataset.build(workdir=_workdir, name=ds_expl.name)
        _uri = URI.capsulate_uri(
            instance=ret_uri.instance,
            project=ret_uri.project,
            obj_type=ret_uri.object.typ,
            obj_name=ret_uri.object.name,
            obj_ver=ret_uri.object.version,
        )
        if self.server_url:
            self.dataset_api.copy(
                src_uri=_uri.full_uri,
                target_project=self.cloud_target_project_uri,
            )
        dss_ = self.datasets.get(name, [])
        dss_.append(_uri)
        self.datasets.update({name: dss_})
        assert len(self.dataset_api.list())
        assert self.dataset_api.info(_uri.full_uri)
        return _uri

    def build_model(self, workdir: str, name: str) -> t.Any:
        self.select_local_instance()
        _uri = Model.build(workdir=workdir, name=name)
        if self.server_url:
            self.model_api.copy(
                src_uri=_uri.full_uri,
                target_project=self.cloud_target_project_uri,
                force=True,
            )
        self.models.update({_uri.object.name: _uri})
        assert len(self.model_api.list())
        assert self.model_api.info(_uri.full_uri)
        return _uri

    def build_runtime(
        self,
        workdir: str,
        runtime_yaml: str = "runtime.yaml",
    ) -> t.Any:
        self.select_local_instance()
        _uri = Runtime.build(workdir=workdir, runtime_yaml=runtime_yaml)
        if self.server_url:
            self.runtime_api.copy(
                src_uri=_uri.full_uri,
                target_project=self.cloud_target_project_uri,
                force=True,
            )
        rts = self.runtimes.get(_uri.object.name, [])
        rts.append(_uri)
        self.runtimes.update({_uri.object.name: rts})
        assert len(self.runtime_api.list())
        assert self.runtime_api.info(_uri.full_uri)
        return _uri

    def select_local_instance(self) -> None:
        self.instance_api.select("local")
        assert self.project_api.select("self")

    def run_model_in_standalone(
        self,
        dataset_uris: t.List[URI],
        model_uri: URI,
        run_handler: str,
        runtime_uris: t.Optional[t.List[URI | None]] = None,
    ) -> t.List[str]:
        logger.info("running evaluation at local...")
        self.select_local_instance()

        job_ids = []
        for runtime_uri in runtime_uris or [None]:
            job_id = self.model_api.run_in_host(
                model_uri=model_uri.full_uri,
                dataset_uris=[_ds_uri.full_uri for _ds_uri in dataset_uris],
                runtime_uri=runtime_uri,
                run_handler=run_handler,
            )
            assert job_id
            assert len(self.job_api.list())
            eval_info = self.job_api.info(job_id)
            assert eval_info
            assert eval_info["manifest"]["status"] in STATUS_SUCCESS
            logger.info("finish run evaluation at standalone.")
            job_ids.append(job_id)
        return job_ids

    def run_model_in_server(
        self,
        dataset_uris: t.List[URI],
        model_uri: URI,
        runtime_uris: t.List[URI],
        run_handler: str,
    ) -> t.List[str]:
        self.instance_api.select(instance="server")
        self.project_api.select(project=self.server_project)

        remote_job_ids = []
        for _rt_uri in runtime_uris:
            logger.info("running evaluation at server...")
            ok, jid = self.model_api.run_in_server(
                model_uri=model_uri.object.version,
                dataset_uris=[_ds_uri.object.version for _ds_uri in dataset_uris],
                runtime_uri=_rt_uri.object.version,
                project=f"{self.server_url}/project/{self.server_project}",
                run_handler=run_handler,
            )
            assert (
                ok
            ), f"submit evaluation to server failed, model: {model_uri}, dataset: {dataset_uris}, runtime: {_rt_uri}"
            remote_job_ids.append(jid)
        return remote_job_ids

    @retry(stop=stop_after_attempt(20), wait=wait_random(min=2, max=20))
    def get_remote_job_status(self, job_id: str) -> t.Tuple[str, str]:
        while True:
            _remote_job = self.job_api.info(
                f"{self.server_url}/project/{self.server_project}/job/{job_id}"
            )
            _job_status = (
                _remote_job["manifest"]["jobStatus"]
                if _remote_job
                else next(iter(STATUS_FAIL))
            )
            if _job_status in STATUS_SUCCESS.union(STATUS_FAIL):
                logger.info(
                    f"finish run evaluation at server for job {job_id}, status is:{_job_status}."
                )
                return job_id, _job_status
            sleep(5)

    def test_simple(self) -> None:
        self.select_local_instance()

        run_handler = "src.evaluator:evaluate"
        workdir = f"{self._work_dir}/scripts/example"
        model_uri = self.build_model(workdir, "simple")
        venv_runtime_uri = self.build_runtime(workdir)
        conda_runtime_uri = self.build_runtime(workdir, "runtime_conda.yaml")
        dataset_uri = self.build_dataset("simple", workdir, DatasetExpl("", ""))

        if self.server_url:
            self.dataset_api.copy(
                src_uri=dataset_uri.full_uri,
                target_project=self.cloud_target_project_uri,
                force=True,
                mode=DatasetChangeMode.OVERWRITE,
            )

        remote_job_ids = []
        if self.server_url:
            remote_job_ids = self.run_model_in_server(
                dataset_uris=[dataset_uri],
                model_uri=model_uri,
                runtime_uris=[venv_runtime_uri],
                run_handler=run_handler,
            )

        self.run_model_in_standalone(
            dataset_uris=[dataset_uri],
            model_uri=model_uri,
            run_handler=run_handler,
            runtime_uris=[conda_runtime_uri],
        )

        futures = [
            self.executor.submit(self.get_remote_job_status, jid)
            for jid in remote_job_ids
        ]
        for f in as_completed(futures):
            _, status = f.result()
            assert status in STATUS_SUCCESS

    def test_all(self) -> None:
        for name, example in ALL_EXAMPLES.items():
            logger.info(f"preparing data for {example}")
            rc = subprocess.call(
                ["make", "CN=1", "prepare"],
                cwd=example["workdir"],
            )
            if rc != 0:
                logger.error(f"prepare data for {example} failed")
                raise

        for name, example in ALL_EXAMPLES.items():
            for d_type in example["datasets"]:
                self.build_dataset(name, example["workdir"], d_type)
            self.build_model(example["workdir"], name)

        for name, rt in RUNTIMES.items():
            if "yamls" not in rt:
                self.build_runtime(str(rt["workdir"]))
            else:
                for yml in list(rt["yamls"]):
                    self.build_runtime(str(rt["workdir"]), yml)

        # model run on server
        res = [
            self.run_example(name, example["run_handler"], in_standalone=False)
            for name, example in ALL_EXAMPLES.items()
        ]
        remote_job_ids: t.List[str] = sum(res, [])

        # model run on standalone
        for name, example in CPU_EXAMPLES.items():
            self.run_example(name, example["run_handler"], in_standalone=True)

        failed_jobs = []
        futures = [
            self.executor.submit(self.get_remote_job_status, jid)
            for jid in remote_job_ids
        ]
        for f in as_completed(futures):
            jid, status = f.result()
            if status not in STATUS_SUCCESS:
                failed_jobs.append((jid, status))

        if failed_jobs:
            msg = f"failed jobs: {failed_jobs}"
            logger.error(msg)
            raise RuntimeError(msg)
        else:
            logger.info("all jobs finished successfully")

    def test_example(self, name: str, run_handler: str) -> None:
        rt_ = RUNTIMES.get(name) or RUNTIMES.get("pytorch")
        if not rt_:
            raise RuntimeError(f"no runtime matching for {name}")
        for name, rt in RUNTIMES.items():
            if "yamls" not in rt:
                self.build_runtime(str(rt["workdir"]))
            else:
                for yml in list(rt["yamls"]):
                    self.build_runtime(str(rt["workdir"]), yml)

        example = ALL_EXAMPLES[name]
        workdir = str(example["workdir"])

        rc = subprocess.Popen(
            ["make", "CN=1", "prepare"],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            cwd=workdir,
        )
        if rc != 0:
            logger.error(f"prepare data for {example} failed")
            raise

        for ds in example["datasets"]:
            self.build_dataset(name, workdir, ds)
        self.build_model(workdir, name)

        self.run_example(name, run_handler, in_standalone=True)

    def run_example(
        self, name: str, run_handler: str, in_standalone: bool = True
    ) -> t.List:
        dataset_uris = self.datasets.get(name)
        if not dataset_uris:
            raise RuntimeError("datasets should not be empty")
        model_uri = self.models.get(name)
        if not model_uri:
            raise RuntimeError("model should not be empty")
        runtime_uris = self.runtimes.get(name) or self.runtimes.get("pytorch")
        if not runtime_uris:
            raise RuntimeError("runtimes should not be empty")

        if in_standalone:
            f = self.run_model_in_standalone
        else:
            f = self.run_model_in_server  # type: ignore
        return f(  # type: ignore
            dataset_uris=dataset_uris,
            model_uri=model_uri,
            runtime_uris=runtime_uris,  # type: ignore
            run_handler=run_handler,
        )

    def debug(self) -> None:
        for name, example in ALL_EXAMPLES.items():
            self.build_model(str(example["workdir"]), name)

    def smoke_commands(self) -> None:
        commands = [
            "timeout 2 swcli --help",
            "swcli --version",
        ]

        for cmd in commands:
            check_invoke(cmd.split())

    def test_sdk(self) -> None:
        script_path = (
            Path(self._work_dir) / "scripts" / "example" / "src" / "sdk_model_build.py"
        )
        check_invoke([sys.executable, str(script_path)])

        self.select_local_instance()
        ctx_handle_info = self.model_api.info("ctx_handle")

        assert set(ctx_handle_info["basic"]["handlers"]) == set(
            [
                "src.evaluator:evaluate",
                "src.evaluator:predict",
                "src.sdk_model_build:context_handle",
            ]
        ), ctx_handle_info["basic"]["handlers"]

        ctx_handle_no_modules_info = self.model_api.info("ctx_handle_no_modules")
        assert set(ctx_handle_no_modules_info["basic"]["handlers"]) == set(
            [
                "src.evaluator:evaluate",
                "src.evaluator:predict",
                "src.sdk_model_build:context_handle",
            ]
        ), ctx_handle_no_modules_info["basic"]["handlers"]


if __name__ == "__main__":
    with ThreadPoolExecutor(
        max_workers=int(os.environ.get("SW_TEST_E2E_THREAD_NUM", "10"))
    ) as executor:
        test_cli = TestCli(
            work_dir=WORK_DIR,
            thread_pool=executor,
            server_url=os.environ.get("CONTROLLER_URL"),
        )

        case = sys.argv[1]
        if case == "simple":
            test_cli.test_simple()
        elif case == "all":
            test_cli.test_all()
        elif case == "debug":
            test_cli.debug()
        elif case == "sdk":
            test_cli.test_sdk()
        else:
            test_cli.test_example(name=case, run_handler=sys.argv[2])

        test_cli.smoke_commands()
