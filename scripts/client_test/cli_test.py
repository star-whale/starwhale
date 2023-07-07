from __future__ import annotations

import os
import sys
import time
import random
import shutil
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

from starwhale.utils import config
from starwhale.base.type import DatasetChangeMode
from starwhale.utils.debug import init_logger
from starwhale.base.uri.resource import Resource

CURRENT_DIR = os.path.dirname(__file__)
SCRIPT_DIR = os.path.abspath(os.path.join(CURRENT_DIR, os.pardir))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, os.pardir))
WORK_DIR = os.environ.get("WORK_DIR")
STATUS_SUCCESS = {"SUCCESS", "success"}
STATUS_FAIL = {"fail", "CANCELED", "FAIL"}

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

init_logger(3)

CPU_EXAMPLES: t.Dict[str, t.Dict[str, t.Any]] = {
    "mnist": {
        "run_handler": "mnist.evaluator:MNISTInference.evaluate",
        "workdir": f"{ROOT_DIR}/example/mnist",
        "datasets": [
            DatasetExpl("mnist_bin", "mnist.dataset:iter_mnist_item"),
            DatasetExpl(
                "mnist_link_raw", "mnist.dataset:LinkRawDatasetProcessExecutor"
            ),
        ],
        "runtime": "pytorch37",
    },
    "cifar10": {
        "run_handler": "cifar.evaluator:CIFAR10Inference.evaluate",
        "workdir": f"{ROOT_DIR}/example/cifar10",
        "datasets": [DatasetExpl("cifar10", "")],
        "runtime": "pytorch38",
    },
    "nmt": {
        "run_handler": "nmt.evaluator:NMTPipeline.cmp",
        "workdir": f"{ROOT_DIR}/example/nmt",
        "datasets": [DatasetExpl("nmt", "")],
        "runtime": "pytorch39",
    },
    "ag_news": {
        "run_handler": "tcan.evaluator:TextClassificationHandler.cmp",
        "workdir": f"{ROOT_DIR}/example/text_cls_AG_NEWS",
        "datasets": [DatasetExpl("ag_news", "")],
        "runtime": "pytorch310",
    },
    "ucf101": {
        "run_handler": "ucf101.evaluator:UCF101PipelineHandler.cmp",
        "workdir": f"{ROOT_DIR}/example/ucf101",
        "datasets": [DatasetExpl("ucf101", "")],
        "runtime": "pytorch310",
    },
}

GPU_EXAMPLES: t.Dict[str, t.Dict[str, t.Any]] = {
    "pfp": {
        "run_handler": "pfp.evaluator:cmp",
        "workdir": f"{ROOT_DIR}/example/PennFudanPed",
        "datasets": [DatasetExpl("pfp", "")],
        "runtime": "pytorch39",
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
        "runtime": "pytorch38",
    },
}

ALL_EXAMPLES = {**CPU_EXAMPLES, **GPU_EXAMPLES}


# TODO: add conda mode runtime


BUILT_IN = "built-in"
BUILT_IN_EXAMPLES: t.List[str] = ["mnist", "pfp", "simple"]


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
        self.server_alias = "server"
        self.server_project = server_project
        self.datasets: t.Dict[str, t.List[Resource]] = {}
        self.runtimes: t.Dict[str, t.List[Resource]] = {}
        self.models: t.Dict[str, Resource] = {}
        if self.server_url:
            logger.info(f"login to server {self.server_url} ...")
            assert self.instance_api.login(url=self.server_url, alias=self.server_alias)

        _pytorch_e2e_root = f"{self._work_dir}/example/runtime/pytorch-e2e"
        self.RUNTIME_EXAMPLES: t.Dict[str, t.Dict[str, str]] = {
            "pytorch37": {"workdir": _pytorch_e2e_root, "yaml": "runtime-3-7.yaml"},
            "pytorch38": {"workdir": _pytorch_e2e_root, "yaml": "runtime-3-8.yaml"},
            "pytorch39": {"workdir": _pytorch_e2e_root, "yaml": "runtime-3-9.yaml"},
            "pytorch310": {"workdir": _pytorch_e2e_root, "yaml": "runtime-3-10.yaml"},
        }

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
        if self.server_url:
            self.dataset_api.copy(
                src_uri=ret_uri.full_uri,
                target_project=self.cloud_target_project_uri,
            )
        dss_ = self.datasets.get(name, [])
        dss_.append(ret_uri)
        self.datasets.update({name: dss_})
        assert len(self.dataset_api.list())
        assert self.dataset_api.info(ret_uri.full_uri)
        return ret_uri

    def build_model(self, workdir: str, name: str, runtime: str) -> t.Any:
        self.select_local_instance()
        _uri = Model.build(workdir=workdir, name=name, runtime=runtime)
        if self.server_url:
            self.model_api.copy(
                src_uri=_uri.full_uri,
                target_project=self.cloud_target_project_uri,
                force=True,
            )
        self.models.update({_uri.name: _uri})
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
        rts = self.runtimes.get(_uri.name, [])
        rts.append(_uri)
        self.runtimes.update({_uri.name: rts})
        assert len(self.runtime_api.list())
        assert self.runtime_api.info(_uri.full_uri)
        return _uri

    def select_local_instance(self) -> None:
        self.instance_api.select("local")
        assert self.project_api.select("self")

    def run_model_in_standalone(
        self,
        dataset_uris: t.List[Resource],
        model_uri: Resource,
        run_handler: str,
        runtime_uris: t.Optional[t.List[Resource | None]] = None,
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
        dataset_uris: t.List[Resource],
        model_uri: Resource,
        run_handler: str,
        runtime_uris: t.Optional[t.List[Resource | None]] = None,
    ) -> t.List[str]:
        self.instance_api.select(instance="server")
        self.project_api.select(project=self.server_project)

        remote_job_ids = []
        for _rt_uri in runtime_uris or [None]:
            logger.info("running evaluation at server...")
            ok, jid = self.model_api.run_in_server(
                model_uri=f"{model_uri.name}/version/{model_uri.version}",
                dataset_uris=[
                    f"{_ds_uri.name}/version/{_ds_uri.version}"
                    for _ds_uri in dataset_uris
                ],
                runtime_uri=f"{_rt_uri.name}/version/{_rt_uri.version}"
                if _rt_uri
                else "",
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
                f"{self.server_url}/projects/{self.server_project}/jobs/{job_id}"
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
        venv_runtime_uri = self.build_runtime(workdir)
        conda_runtime_uri = self.build_runtime(workdir, "runtime_conda.yaml")
        model_uri = self.build_model(workdir, "simple", "simple-test")
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
                runtime_uris=[venv_runtime_uri]
                if "simple" not in BUILT_IN_EXAMPLES
                else [None],
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
        failed_jobs = []
        for f in as_completed(futures):
            jid, status = f.result()
            if status not in STATUS_SUCCESS:
                failed_jobs.append((jid, status))

            if failed_jobs:
                msg = f"jobs failed: {failed_jobs}"
                logger.error(msg)
                raise RuntimeError(msg)

    def test_all(self) -> None:
        for name, example in ALL_EXAMPLES.items():
            logger.info(f"preparing data for {example}")
            process = subprocess.Popen(
                ["make", "CN=1", "prepare"],
                cwd=example["workdir"],
            )
            rc = process.wait()
            if rc != 0:
                logger.error(f"prepare data for {example} failed ")
                logger.error(process.stderr.read().decode("utf-8"))  # type: ignore
                raise

        for name, rt in self.RUNTIME_EXAMPLES.items():
            self.build_runtime(rt["workdir"], rt["yaml"])

        for name, example in ALL_EXAMPLES.items():
            for d_type in example["datasets"]:
                self.build_dataset(name, example["workdir"], d_type)
            self.build_model(example["workdir"], name, example["runtime"])

        if self.server_url:
            # model run on server
            res = [
                self.run_example(
                    name,
                    example["run_handler"],
                    in_standalone=False,
                    runtime=random.choice(list(self.RUNTIME_EXAMPLES.keys())),
                )
                for name, example in ALL_EXAMPLES.items()
            ]
            remote_job_ids: t.List[str] = sum(res, [])
            # remove all local artifacts
            shutil.rmtree(config._config["storage"]["root"])
            self.select_local_instance()
            # download all artifacts from server
            for ds_uris in self.datasets.values():
                for ds_uri in ds_uris:
                    self.dataset_api.copy(
                        f"cloud://{self.server_alias}/project/{self.server_project}/dataset/{ds_uri.name}/version/{ds_uri.version}",
                        ".",
                    )
            for md_uri in self.models.values():
                self.model_api.copy(
                    f"cloud://{self.server_alias}/project/{self.server_project}/model/{md_uri.name}/version/{md_uri.version}",
                    ".",
                )
            for rt_uris in self.runtimes.values():
                for rt_uri in rt_uris:
                    self.runtime_api.copy(
                        f"cloud://{self.server_alias}/project/{self.server_project}/runtime/{rt_uri.name}/version/{rt_uri.version}",
                        ".",
                    )

        # model run on standalone
        for name, example in CPU_EXAMPLES.items():
            self.run_example(
                name,
                example["run_handler"],
                in_standalone=True,
                runtime=random.choice(list(self.RUNTIME_EXAMPLES.keys())),
            )

        if self.server_url:
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

    def test_example(self, name: str) -> None:
        rt = self.RUNTIME_EXAMPLES.get(name) or self.RUNTIME_EXAMPLES.get("pytorch37")
        if not rt:
            raise RuntimeError(f"no runtime matching for {name}")

        rt_uri = self.build_runtime(rt["workdir"], rt["yaml"])

        example = ALL_EXAMPLES[name]
        workdir = str(example["workdir"])

        process = subprocess.Popen(
            ["make", "CN=1", "prepare"],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            cwd=workdir,
        )
        rc = process.wait()
        if rc != 0:
            logger.error(f"prepare data for {example} failed ")
            logger.error(process.stderr.read().decode("utf-8"))  # type: ignore
            raise

        for ds in example["datasets"]:
            self.build_dataset(name, workdir, ds)
        self.build_model(workdir, name, rt_uri.full_uri)

        self.run_example(
            name, example["run_handler"], in_standalone=True, runtime=example["runtime"]
        )

    def run_example(
        self,
        name: str,
        run_handler: str,
        in_standalone: bool = True,
        runtime: str = "pytorch37",
    ) -> t.List:
        logger.info(
            f"run example {name} using runtime {runtime} in standalone {in_standalone}"
        )
        dataset_uris = self.datasets.get(name)
        if not dataset_uris:
            raise RuntimeError("datasets should not be empty")
        model_uri = self.models.get(name)
        if not model_uri:
            raise RuntimeError("model should not be empty")

        runtime_uris = self.runtimes.get(runtime)
        if not runtime_uris:
            raise RuntimeError("runtimes should not be empty")

        if in_standalone:
            f = self.run_model_in_standalone
        else:
            runtime_uris = runtime_uris if name not in BUILT_IN_EXAMPLES else None
            f = self.run_model_in_server  # type: ignore
        return f(  # type: ignore
            dataset_uris=dataset_uris,
            model_uri=model_uri,
            runtime_uris=runtime_uris,  # type: ignore
            run_handler=run_handler,
        )

    def debug(self) -> None:
        for name, example in ALL_EXAMPLES.items():
            self.build_model(str(example["workdir"]), name, str(example["runtime"]))

    def smoke_commands(self) -> None:
        commands = [
            "swcli --help",
            "swcli --version",
        ]

        for cmd in commands:
            start = time.time()
            check_invoke(cmd.split())
            if time.time() - start > 10:
                raise RuntimeError(f"{cmd} timeout")

    def test_sdk(self) -> None:
        script_path = (
            Path(self._work_dir) / "scripts" / "example" / "src" / "sdk_model_build.py"
        )
        check_invoke([sys.executable, str(script_path)])

        self.select_local_instance()
        ctx_handle_info = self.model_api.info("ctx_handle")

        assert set(ctx_handle_info["basic"]["handlers"]) == {
            "src.evaluator:evaluate",
            "src.evaluator:predict",
            "src.sdk_model_build:context_handle",
            "src.sdk_model_build:ft",
        }, ctx_handle_info["basic"]["handlers"]

        ctx_handle_no_modules_info = self.model_api.info("ctx_handle_no_modules")
        assert set(ctx_handle_no_modules_info["basic"]["handlers"]) == {
            "src.evaluator:evaluate",
            "src.evaluator:predict",
            "src.sdk_model_build:context_handle",
            "src.sdk_model_build:ft",
        }, ctx_handle_no_modules_info["basic"]["handlers"]


def start(
    sw_repo_path,
    case="all",
    work_dir="",
    server_url="",
    client_config="",
    client_storage="",
    server_project="starwhale",
) -> None:
    wd = work_dir or WORK_DIR
    if not wd:
        raise RuntimeError("WORK_DIR NOT FOUND")

    example_sync_commands = [
        [
            "rsync",
            "-q",
            "-av",
            f"{sw_repo_path}/client",
            wd,
            "--exclude",
            "venv",
            "--exclude",
            ".venv",
        ],
        [
            "rsync",
            "-q",
            "-av",
            f"{sw_repo_path}/example/ucf101",
            f"{wd}/example",
            "--exclude",
            "venv",
            "--exclude",
            ".venv",
            "--exclude",
            ".starwhale",
            "--exclude",
            "data",
            "--exclude",
            "models",
        ],
        [
            "rsync",
            "-q",
            "-av",
            f"{sw_repo_path}/example/runtime/pytorch-e2e",
            f"{wd}/example/runtime",
            "--exclude",
            "venv",
            "--exclude",
            ".venv",
            "--exclude",
            ".starwhale",
        ],
        [
            "rsync",
            "-q",
            "-av",
            f"{sw_repo_path}/scripts/example",
            f"{wd}/scripts",
            "--exclude",
            "venv",
            "--exclude",
            ".venv",
            "--exclude",
            ".starwhale",
        ],
    ]
    processes = [
        subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        for cmd in example_sync_commands
    ]
    processes.append(
        subprocess.Popen(
            ["cp", f"{sw_repo_path}/README.md", wd],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    )
    for p in processes:
        stdout, stderr = p.communicate()
        logger.info(stdout.decode("utf-8"))
        logger.error(stderr.decode("utf-8"))

    if case == "simple":
        cmd = [
            "bash",
            f"{wd}/scripts/example/runtime_conda_init.sh",
            f"{sw_repo_path}/client",
        ]
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )
        rc = process.wait()
        if rc != 0:
            logger.error("prepare wheel for simple case failed ")
            raise

    os.environ["SW_CLI_CONFIG"] = (
        f"{wd}/{client_config}" if client_config else f"{wd}/config.yaml"
    )
    os.environ["SW_LOCAL_STORAGE"] = (
        f"{wd}/{client_storage}" if client_storage else f"{wd}/data"
    )
    with ThreadPoolExecutor(
        max_workers=int(os.environ.get("SW_TEST_E2E_THREAD_NUM", "10"))
    ) as executor:
        test_cli = TestCli(
            work_dir=wd,
            thread_pool=executor,
            server_url=server_url or os.environ.get("CONTROLLER_URL"),
            server_project=server_project,
        )

        if case == "simple":
            test_cli.test_simple()
        elif case == "all":
            test_cli.test_all()
        elif case == "debug":
            test_cli.debug()
        elif case == "sdk":
            test_cli.test_sdk()
        else:
            test_cli.test_example(name=case)

        test_cli.smoke_commands()


if __name__ == "__main__":
    import fire

    fire.Fire(start)
