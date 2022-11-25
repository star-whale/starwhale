import os
import sys
import typing as t
import logging
import subprocess
from time import sleep
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures._base import Future

from cmds.eval_cmd import Evaluation
from cmds.project_cmd import Project
from cmds.instance_cmd import Instance
from cmds.artifacts_cmd import Model, Dataset, Runtime

from starwhale import URI

CURRENT_DIR = os.path.dirname(__file__)
step_spec_f: t.Callable[
    [str], str
] = lambda spec: f"{os.path.abspath(CURRENT_DIR)}/step_specs/{spec}"
SCRIPT_DIR = os.path.abspath(os.path.join(CURRENT_DIR, os.pardir))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, os.pardir))
WORK_DIR = os.environ.get("WORK_DIR")
if not WORK_DIR:
    raise RuntimeError("WORK_DIR NOT FOUND")
STATUS_SUCCESS = {"SUCCESS", "success"}
STATUS_FAIL = {"FAIL", "fail", "CANCELED"}

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

EXAMPLES: t.Dict[str, t.Dict[str, t.Any]] = {
    "mnist": {
        "workdir": f"{ROOT_DIR}/example/mnist",
        "datasets": ["dataset.yaml", "dataset.yaml.raw", "dataset.yaml.link"],
        # "datasets": ["dataset.yaml"],
    },
    "cifar10": {
        "workdir": f"{ROOT_DIR}/example/cifar10",
        "datasets": ["dataset.yaml"],
    },
    "nmt": {
        "workdir": f"{ROOT_DIR}/example/nmt",
        "datasets": ["dataset.yaml"],
    },
    "pfp": {
        "workdir": f"{ROOT_DIR}/example/PennFudanPed",
        "datasets": ["dataset.yaml"],
        "device": "gpu",
    },
    "speech_command": {
        "workdir": f"{ROOT_DIR}/example/speech_command",
        "datasets": ["dataset.yaml", "dataset.yaml.link"],
        "device": "gpu",
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
RUNTIMES: t.Dict[str, t.Dict[str, str]] = {
    "pytorch": {
        "workdir": f"{WORK_DIR}/example/runtime/pytorch-e2e",
    },
    "ucf101": {
        "workdir": f"{WORK_DIR}/example/ucf101",
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
        cloud_url: t.Optional[str],
        cloud_project: str = "starwhale",
    ) -> None:
        self._work_dir = work_dir
        self.executor = thread_pool
        self.cloud_url = cloud_url
        self.cloud_project = cloud_project
        self.datasets: t.Dict[str, t.List[URI]] = {}
        self.runtimes: t.Dict[str, URI] = {}
        self.models: t.Dict[str, URI] = {}
        if self.cloud_url:
            logger.info(f"login to cloud {self.cloud_url} ...")
            assert self.instance.login(url=self.cloud_url)

    def build_dataset(
        self, _workdir: str, yaml: str = "dataset.yaml"
    ) -> t.Union[URI, t.Any]:
        self.select_local_instance()
        _uri = Dataset.build_with_api(workdir=_workdir, dataset_yaml=yaml)
        if self.cloud_url:
            assert self.dataset.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://cloud/project/{self.cloud_project}",
                force=True,
            )
        dss_ = self.datasets.get(_uri.object.name, [])
        dss_.append(_uri)
        self.datasets.update({_uri.object.name: dss_})
        assert len(self.dataset.list())
        assert self.dataset.info(_uri.full_uri)
        return _uri

    def build_model(
        self,
        _workdir: str,
    ) -> t.Union[URI, t.Any]:
        self.select_local_instance()
        _uri = Model.build_with_api(workdir=_workdir)
        if self.cloud_url:
            assert self.model.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://cloud/project/{self.cloud_project}",
                force=True,
            )
        self.models.update({_uri.object.name: _uri})
        assert len(self.model.list())
        assert self.model.info(_uri.full_uri)
        return _uri

    def build_runtime(
        self,
        _workdir: str,
    ) -> t.Union[URI, t.Any]:
        self.select_local_instance()
        _uri = Runtime.build_with_api(workdir=_workdir)
        if self.cloud_url:
            assert self.runtime.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://cloud/project/{self.cloud_project}",
                force=True,
            )
        self.runtimes.update({_uri.object.name: _uri})
        assert len(self.runtime.list())
        assert self.runtime.info(_uri.full_uri)
        return _uri

    def select_local_instance(self) -> None:
        self.instance.select("local")
        assert self.project.select("self")

    def eval(
        self,
        _model_uri: URI,
        _rt_uri: URI,
        _ds_uris: t.List[URI],
        step_spec_file: str,
        local_instance: bool = True,
    ) -> Future:
        if local_instance:
            _jid = self.local_evl(_ds_uris, _model_uri, _rt_uri)
            return executor.submit(lambda: (_jid, next(iter(STATUS_SUCCESS))))
        if self.cloud_url and not local_instance:
            return self.remote_eval(_ds_uris, _model_uri, _rt_uri, step_spec_file)
        return executor.submit(lambda: ("", next(iter(STATUS_SUCCESS))))

    def local_evl(
        self, _ds_uris: t.List[URI], _model_uri: URI, _rt_uri: URI
    ) -> t.Union[str, t.Any]:
        logger.info("running evaluation at local...")
        self.select_local_instance()
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
        self, _ds_uris: t.List[URI], _model_uri: URI, _rt_uri: URI, step_spec_file: str
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
        return _js

    def get_remote_job_status(self, job_id: str) -> t.Tuple[str, str]:
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
        # use local instance
        logger.info("select local")
        self.select_local_instance()

        # 1.model build
        logger.info("building model...")
        _model_uri = self.build_model(f"{self._work_dir}/scripts/example")

        # 2.dataset build
        _ds_uri = self.build_dataset(f"{self._work_dir}/scripts/example")

        # 3.runtime build
        _rt_uri = self.build_runtime(f"{self._work_dir}/scripts/example")

        self.local_evl([_ds_uri], _model_uri, _rt_uri)
        if self.cloud_url:
            _js = self.remote_eval(
                [_ds_uri], _model_uri, _rt_uri, step_spec_f("step_spec_cpu_mini.yaml")
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
            for d_type in expl["datasets"]:
                self.build_dataset(workdir_, d_type)
            self.build_model(workdir_)

        # run evals on standalone
        for name, expl in EXAMPLES.items():
            expl.get("device", "cpu") == "cpu" and self.run_example(
                name,
                step_spec_f("step_spec_cpu_full.yaml"),
            )

        # run evals on cloud
        res = [
            self.run_example(
                name,
                step_spec_f(f"step_spec_{expl.get('device', 'cpu')}_full.yaml"),
                False,
            )
            for name, expl in EXAMPLES.items()
        ]
        for _js in res:
            jid, status = _js.result()
            if status not in STATUS_SUCCESS:
                logger.error(f"job {jid} failed!")
                exit(1)

    def test_expl(self, expl_name: str) -> None:
        rt_ = RUNTIMES.get(expl_name) or RUNTIMES.get("pytorch")
        if not rt_:
            raise RuntimeError(f"no runtime matching for {expl_name}")
        self.build_runtime(str(rt_.get("workdir")))

        expl = EXAMPLES[expl_name]
        workdir_ = str(expl["workdir"])

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
            expl.get("device", "cpu") == "cpu",
        )
        jid, status = _js.result()
        if status not in STATUS_SUCCESS:
            logger.error(f"job {jid} failed!")
            exit(1)

    def run_example(
        self, name: str, step_spec: str, local_instance: bool = True
    ) -> Future:
        datasets_ = self.datasets.get(name)
        if not datasets_:
            raise RuntimeError("datasets should not be empty")
        model_ = self.models.get(name)
        if not model_:
            raise RuntimeError("model should not be empty")
        runtime_ = self.runtimes.get(name) or self.runtimes.get("pytorch")
        if not runtime_:
            raise RuntimeError("runtime should not be empty")
        return self.eval(model_, runtime_, datasets_, step_spec, local_instance)

    def debug(self) -> None:
        for name, expl in EXAMPLES.items():
            workdir_ = expl["workdir"]
            self.build_model(str(workdir_))


if __name__ == "__main__":
    with ThreadPoolExecutor(
        max_workers=int(os.environ.get("SW_TEST_E2E_THREAD_NUM", "10"))
    ) as executor:
        # start test
        test_cli = TestCli(
            work_dir=WORK_DIR,
            thread_pool=executor,
            cloud_url=os.environ.get("CONTROLLER_URL"),
        )
        example = sys.argv[1]
        if example == "simple":
            test_cli.test_simple()
        elif example == "all":
            test_cli.test_all()
        elif example == "debug":
            test_cli.debug()
        else:
            test_cli.test_expl(expl_name=example)
