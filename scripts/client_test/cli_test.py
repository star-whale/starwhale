import os
import sys
import logging
import subprocess
from time import sleep
from typing import Any
from asyncio import Future
from concurrent.futures import ThreadPoolExecutor

from cmds.eval_cmd import Evaluation
from cmds.base.common import EnvironmentPrepare
from cmds.base.invoke import invoke
from cmds.project_cmd import Project
from cmds.instance_cmd import Instance
from cmds.artifacts_cmd import Model, Dataset, Runtime

from starwhale import URI

CURRENT_DIR = os.path.dirname(__file__)
step_spec_f = lambda spec: f"{os.path.abspath(CURRENT_DIR)}/step_specs/{spec}"
SCRIPT_DIR = os.path.abspath(os.path.join(CURRENT_DIR, os.pardir))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, os.pardir))
STATUS_SUCCESS = {"SUCCESS", "success"}
STATUS_FAIL = {"FAIL", "fail", "CANCELED"}

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

EXAMPLES = {
    "mnist": {
        "workdir": f"{ROOT_DIR}/example/mnist",
        "datasets": ["dataset.yaml", "dataset.yaml.raw", "dataset.yaml.link"],
    },
    "cifar10": {
        "workdir": f"{ROOT_DIR}/example/cifar10",
        "datasets": ["dataset.yaml"],
    },
    "nmt": {
        "workdir": f"{ROOT_DIR}/example/nmt",
        "datasets": ["dataset.yaml"],
    },
    # "pfp": {
    #     "workdir": f"{ROOT_DIR}/example/PennFudanPed",
    #     "datasets": ["dataset.yaml"],
    #     "device": "gpu",
    # },
    "speech_command": {
        "workdir": f"{ROOT_DIR}/example/speech_command",
        "datasets": ["dataset.yaml", "dataset.yaml.link"],
    },
    "ag_news": {
        "workdir": f"{ROOT_DIR}/example/text_cls_AG_NEWS",
        "datasets": ["dataset.yaml"],
    },
    "ucf101": {
        "workdir": f"{ROOT_DIR}/example/ucf101",
        "datasets": ["dataset.yaml"],
    },
}
RUNTIMES = {
    "pytorch": {
        "workdir": f"{ROOT_DIR}/example/runtime/pytorch-e2e",
    },
    "ucf101": {
        "workdir": f"{ROOT_DIR}/example/ucf101",
    },
}


class TestCli:
    instance = Instance()
    project = Project()
    model = Model()
    dataset = Dataset()
    runtime = Runtime()
    evaluation = Evaluation()

    def __init__(
        self,
        work_dir: str,
        thread_pool: ThreadPoolExecutor,
        cloud_url: str,
        cloud_project: str = "starwhale",
    ) -> None:
        self._work_dir = work_dir
        self.executor = thread_pool
        self.cloud_url = cloud_url
        self.cloud_project = cloud_project
        self.datasets = {}
        self.runtimes = {}
        self.models = {}
        if self.cloud_url:
            logger.info(f"login to cloud {self.cloud_url} ...")
            assert self.instance.login(url=self.cloud_url)

    def build_dataset(self, _workdir: str, yaml: str) -> URI:
        self.select_local_instance()
        _uri = Dataset.build_with_api(workdir=_workdir, dataset_yaml=yaml)
        if self.cloud_url:
            assert self.dataset.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://cloud/project/{self.cloud_project}",
                force=True,
            )
        if _uri.object.name not in self.datasets:
            self.datasets[_uri.object.name] = []
        self.datasets[_uri.object.name].append(_uri)
        assert len(self.dataset.list())
        assert self.dataset.info(_uri.full_uri)
        return _uri

    def build_model(
        self,
        _workdir: str,
    ) -> URI:
        self.select_local_instance()
        _uri = Model.build_with_api(workdir=_workdir)
        if self.cloud_url:
            assert self.model.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://cloud/project/{self.cloud_project}",
                force=True,
            )
        self.models[_uri.object.name] = _uri
        assert len(self.model.list())
        assert self.model.info(_uri.full_uri)
        return _uri

    def build_runtime(
        self,
        _workdir: str,
    ) -> URI:
        self.select_local_instance()
        _uri = Runtime.build_with_api(workdir=_workdir)
        if self.cloud_url:
            assert self.runtime.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://cloud/project/{self.cloud_project}",
                force=True,
            )
        self.runtimes[_uri.object.name] = _uri
        assert len(self.runtime.list())
        assert self.runtime.info(_uri.full_uri)
        return _uri

    def select_local_instance(self):
        self.instance.select("local")
        assert self.project.select("self")

    def standard_workflow(
        self,
        model_workdir: str,
        ds_workdir: str,
        rt_workdir: str,
        step_spec_file: str,
    ) -> Future:
        # use local instance
        logger.info("select local")
        self.select_local_instance()

        # 1.model build
        logger.info("building model...")
        _model_uri = self.build_model(model_workdir)

        # 2.dataset build
        _ds_uri = self.build_dataset(ds_workdir)

        # 3.runtime build
        _rt_uri = self.build_runtime(rt_workdir)

        return self.eval(_ds_uri, _model_uri, _rt_uri, step_spec_file)

    def eval(
        self,
        _model_uri: URI,
        _rt_uri: URI,
        _ds_uris: list[URI],
        step_spec_file: str,
        remote_only: bool = False,
    ):
        # 4.run evaluation on local instance
        _job_id = "0"
        if not remote_only:
            _job_id = self.local_evl(_ds_uris, _model_uri, _rt_uri)
        if not self.cloud_url:
            return executor.submit(lambda: (_job_id, next(iter(STATUS_SUCCESS))))
        # 7.select to cloud instance
        return self.remote_eval(_ds_uris, _model_uri, _rt_uri, step_spec_file)

    def local_evl(self, _ds_uris, _model_uri, _rt_uri):
        logger.info("running evaluation at local...")
        _job_id = self.evaluation.run(
            model=_model_uri.full_uri,
            datasets=[_ds_uri.full_uri for _ds_uri in _ds_uris],
            runtime=_rt_uri.full_uri,
        )
        assert _job_id
        assert len(self.evaluation.list())
        eval_info = self.evaluation.info(_job_id)
        assert eval_info
        assert eval_info["manifest"]["status"] in STATUS_SUCCESS
        logger.info("finish run evaluation at standalone.")
        return _job_id

    def remote_eval(
        self, _ds_uris: list[URI], _model_uri: URI, _rt_uri: URI, step_spec_file: str
    ) -> Future:
        self.instance.select(instance="cloud")
        self.project.select(project=self.cloud_project)
        # 8.start an evaluation
        logger.info("running evaluation at cloud...")
        _remote_jid = self.evaluation.run(
            model=_model_uri.object.version,
            datasets=[_ds_uri.object.version for _ds_uri in _ds_uris],
            runtime=_rt_uri.object.version,
            project=f"{self.cloud_url}/project/{self.cloud_project}",
            step_spec=step_spec_file,
            resource_pool=os.environ.get("RESOURCE_POOL"),
        )
        assert _remote_jid
        # 9.check job's status
        _js = executor.submit(self.get_remote_job_status, _remote_jid)
        print("submit success-------------------")
        return _js

    def get_remote_job_status(self, job_id: str) -> Any:
        while True:
            _remote_job = self.evaluation.info(
                f"{self.cloud_url}/project/{self.cloud_project}/evaluation/{job_id}"
            )
            _job_status = (
                _remote_job["manifest"]["jobStatus"]
                if _remote_job
                else next(iter(STATUS_FAIL))
            )
            if _job_status in STATUS_SUCCESS.union(STATUS_FAIL):
                logger.info(
                    f"finish run evaluation at cloud for job {job_id}, status is:{_job_status}."
                )
                return job_id, _job_status
            sleep(10)
            logger.info(f"status for job {job_id} is:{_job_status}")

    def test_simple(self) -> None:
        _js = self.standard_workflow(
            model_workdir=f"{self._work_dir}/scripts/example",
            ds_workdir=f"{self._work_dir}/scripts/example",
            rt_workdir=f"{self._work_dir}/scripts/example",
            step_spec_file=step_spec_f("step_spec_cpu_mini.yaml")
            if os.environ.get("GITHUB_ACTION")
            else step_spec_f("step_spec_cpu_full.yaml"),
        )
        _, status = _js.result()
        assert status in STATUS_SUCCESS

    def test_all(self) -> None:

        for name, rt in RUNTIMES.items():
            self.build_runtime(rt["workdir"])

        processes = [
            subprocess.Popen(
                ["make", "prepare-e2e-data"],
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                cwd=expl["workdir"],
            )
            for name, expl in EXAMPLES.items()
        ]

        for p in processes:
            p.wait()

        for name, expl in EXAMPLES.items():
            workdir_ = expl["workdir"]
            #  download data
            for d_type in expl["datasets"]:
                self.build_dataset(workdir_, d_type)
            self.build_model(workdir_)
            # run_eval
        res = [
            self.run_example(
                name,
                step_spec_f(f"step_spec_{expl.get('device', 'cpu')}_full.yaml"),
                expl.get("device", "cpu") == "gpu",
            )
            for name, expl in EXAMPLES.items()
        ]
        for _js in res:
            jid, status = _js.result()
            if status not in STATUS_SUCCESS:
                logger.error(f"job {jid} failed!")
                exit(1)

    def test_expl(self, expl_name: str):
        rt_ = RUNTIMES.get(expl_name) or RUNTIMES.get("pytorch")
        self.build_runtime(rt_["workdir"])

        expl = EXAMPLES[expl_name]
        workdir_ = expl["workdir"]

        p = subprocess.Popen(
            ["make", "prepare-e2e-data"],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            cwd=workdir_,
        )

        p.wait()

        #  download data
        for d_type in expl["datasets"]:
            self.build_dataset(workdir_, d_type)
        self.build_model(workdir_)

        # run_eval
        _js = self.run_example(
            expl_name,
            step_spec_f(f"step_spec_{expl.get('device', 'cpu')}_full.yaml"),
            expl.get("device", "cpu") == "gpu",
        )
        jid, status = _js.result()
        if status not in STATUS_SUCCESS:
            logger.error(f"job {jid} failed!")
            exit(1)

    def run_example(
        self, name: str, step_spec: str, remote_only: bool = False
    ) -> Future:
        datasets_ = self.datasets.get(name)
        model_ = self.models.get(name)
        runtime_ = self.runtimes.get(name) or self.runtimes.get("pytorch")
        return self.eval(model_, runtime_, datasets_, step_spec, remote_only)


if __name__ == "__main__":
    with ThreadPoolExecutor(
        max_workers=int(os.environ.get("SW_TEST_E2E_THREAD_NUM", "10"))
    ) as executor:
        workdir = os.environ.get("WORK_DIR")
        # start test
        test_cli = TestCli(
            work_dir=workdir,
            thread_pool=executor,
            cloud_url=os.environ.get("CONTROLLER_URL"),
        )
        example = sys.argv[1]
        if example == "simple":
            test_cli.test_simple()
        elif example == "all":
            test_cli.test_all()
        else:
            test_cli.test_expl(expl_name=example)
