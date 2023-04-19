import os
import sys
import shutil
import typing as t
import logging
import subprocess
from time import sleep
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures._base import Future

from cmds import DatasetExpl
from cmds.eval_cmd import Evaluation
from cmds.base.invoke import invoke
from cmds.project_cmd import Project
from cmds.instance_cmd import Instance
from cmds.artifacts_cmd import Model, Dataset, Runtime

from starwhale import URI
from starwhale.utils import config

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
        "datasets": [
            DatasetExpl("mnist_bin", "mnist.dataset:iter_mnist_item"),
            DatasetExpl(
                "mnist_link_raw", "mnist.dataset:LinkRawDatasetProcessExecutor"
            ),
        ],
    },
    "cifar10": {
        "workdir": f"{ROOT_DIR}/example/cifar10",
        "datasets": [DatasetExpl("cifar10", "")],
    },
    "nmt": {
        "workdir": f"{ROOT_DIR}/example/nmt",
        "datasets": [DatasetExpl("nmt", "")],
    },
    "pfp": {
        "workdir": f"{ROOT_DIR}/example/PennFudanPed",
        "datasets": [DatasetExpl("pfp", "")],
        "device": "gpu",
    },
    "speech_command": {
        "workdir": f"{ROOT_DIR}/example/speech_command",
        "datasets": [
            DatasetExpl("speech_command", ""),
            DatasetExpl(
                "speech_command_link", "sc.dataset:LinkRawDatasetBuildExecutor"
            ),
        ],
        "device": "gpu",
    },
    "ag_news": {
        "workdir": f"{ROOT_DIR}/example/text_cls_AG_NEWS",
        "datasets": [DatasetExpl("ag_news", "")],
    },
    "ucf101": {
        "workdir": f"{ROOT_DIR}/example/ucf101",
        "datasets": [DatasetExpl("ucf101", "")],
    },
}
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
    evaluation_api = Evaluation()

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
        ret_uri = Dataset.build_with_api(workdir=_workdir, ds_expl=ds_expl)
        _uri = URI.capsulate_uri(
            instance=ret_uri.instance,
            project=ret_uri.project,
            obj_type=ret_uri.object.typ,
            obj_name=ret_uri.object.name,
            obj_ver=ret_uri.object.version,
        )
        if self.server_url:
            assert self.dataset_api.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://server/project/{self.server_project}",
            )
        dss_ = self.datasets.get(name, [])
        dss_.append(_uri)
        self.datasets.update({name: dss_})
        assert len(self.dataset_api.list())
        assert self.dataset_api.info(_uri.full_uri)
        return _uri

    def build_model(
        self,
        _workdir: str,
    ) -> t.Any:
        self.select_local_instance()
        _uri = Model.build_with_api(workdir=_workdir)
        if self.server_url:
            assert self.model_api.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://server/project/{self.server_project}",
                force=True,
            )
        self.models.update({_uri.object.name: _uri})
        assert len(self.model_api.list())
        assert self.model_api.info(_uri.full_uri)
        return _uri

    def build_runtime(
        self,
        _workdir: str,
        runtime_yaml: str = "runtime.yaml",
    ) -> t.Any:
        self.select_local_instance()
        runtime_cache_path = f"{_workdir}/.starwhale"
        if os.path.exists(runtime_cache_path):
            shutil.rmtree(runtime_cache_path)
        _uri = Runtime.build_with_api(workdir=_workdir, runtime_yaml=runtime_yaml)
        if self.server_url:
            assert self.runtime_api.copy(
                src_uri=_uri.full_uri,
                target_project=f"cloud://server/project/{self.server_project}",
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

    def eval(
        self,
        _model_uri: URI,
        _rt_uris: t.List[URI],
        _ds_uris: t.List[URI],
        step_spec_file: str,
        local_instance: bool = True,
    ) -> t.List[Future]:
        if not local_instance and self.server_url:
            return self.remote_eval(_ds_uris, _model_uri, _rt_uris, step_spec_file)
        else:
            self.local_evl(_ds_uris, _model_uri, _rt_uris)
            return []

    def local_evl(
        self,
        _ds_uris: t.List[URI],
        _model_uri: URI,
        _rt_uris: t.Optional[t.List[URI]] = None,
    ) -> t.Any:
        logger.info("running evaluation at local...")
        self.select_local_instance()
        jids = []
        if not _rt_uris:
            _rt_uris = [URI("")]
        for _rt_uri in _rt_uris:
            _job_id = self.evaluation_api.run(
                model=_model_uri.full_uri,
                datasets=[_ds_uri.full_uri for _ds_uri in _ds_uris],
                runtime=_rt_uri.full_uri if _rt_uri.raw else "",
            )
            assert _job_id
            assert len(self.evaluation_api.list())
            eval_info = self.evaluation_api.info(_job_id)
            assert eval_info
            assert eval_info["manifest"]["status"] in STATUS_SUCCESS
            logger.info("finish run evaluation at standalone.")
            jids.append(_job_id)
        return jids

    def remote_eval(
        self,
        _ds_uris: t.List[URI],
        _model_uri: URI,
        _rt_uris: t.List[URI],
        step_spec_file: str,
    ) -> t.List[Future]:
        self.instance_api.select(instance="server")
        self.project_api.select(project=self.server_project)
        # 8.start an evaluation
        job_status_checkers = []
        for _rt_uri in _rt_uris:
            logger.info("running evaluation at server...")
            _remote_jid = self.evaluation_api.run(
                model=_model_uri.object.version,
                datasets=[_ds_uri.object.version for _ds_uri in _ds_uris],
                runtime=_rt_uri.object.version,
                project=f"{self.server_url}/project/{self.server_project}",
                step_spec=step_spec_file,
                resource_pool=os.environ.get("RESOURCE_POOL"),
            )
            assert _remote_jid
            # 9.check job's status
            job_status_checkers.append(
                executor.submit(self.get_remote_job_status, _remote_jid)
            )
        return job_status_checkers

    def get_remote_job_status(self, job_id: str) -> t.Tuple[str, str]:
        while True:
            _remote_job = self.evaluation_api.info(
                f"{self.server_url}/project/{self.server_project}/evaluation/{job_id}"
            )
            _job_status = (
                _remote_job["manifest"]["jobStatus"]
                if _remote_job
                else next(iter(STATUS_FAIL))
            )
            print(f"checking job {job_id} status: {_job_status}")
            if _job_status in STATUS_SUCCESS.union(STATUS_FAIL):
                logger.info(
                    f"finish run evaluation at server for job {job_id}, status is:{_job_status}."
                )
                return job_id, _job_status
            sleep(15)

    def test_simple(self) -> None:
        # use local instance
        logger.info("select local")
        self.select_local_instance()

        # 1.model build
        logger.info("building model...")
        _model_uri = self.build_model(f"{self._work_dir}/scripts/example")

        # 2.dataset build
        _ds_uri = self.build_dataset(
            "simple", f"{self._work_dir}/scripts/example", DatasetExpl("", "")
        )

        # 3.runtime build
        _rt_uri = self.build_runtime(f"{self._work_dir}/scripts/example")

        remote_future_jobs = []
        if self.server_url:
            remote_future_jobs = self.remote_eval(
                [_ds_uri], _model_uri, [_rt_uri], step_spec_f("step_spec_cpu_mini.yaml")
            )

        self.local_evl([_ds_uri], _model_uri)

        for job in remote_future_jobs:
            _, status = job.result()
            assert status in STATUS_SUCCESS

    def test_all(self) -> None:
        for name, expl in EXAMPLES.items():
            logger.info(f"preparing data for {expl}")
            rc = subprocess.call(
                ["make", "CN=1", "prepare"],
                cwd=expl["workdir"],
            )
            if rc != 0:
                logger.error(f"prepare data for {expl} failed")
                raise

        for name, expl in EXAMPLES.items():
            workdir_ = expl["workdir"]
            for d_type in expl["datasets"]:
                self.build_dataset(name, workdir_, d_type)
            self.build_model(workdir_)

        for name, rt in RUNTIMES.items():
            if "yamls" not in rt:
                self.build_runtime(str(rt["workdir"]))
            else:
                for yml in list(rt["yamls"]):
                    self.build_runtime(str(rt["workdir"]), yml)

        # run evals on server
        res = [
            self.run_example(
                name,
                step_spec_f(f"step_spec_{expl.get('device', 'cpu')}_full.yaml"),
                False,
            )
            for name, expl in EXAMPLES.items()
        ]
        status_checkers: t.List[Future] = sum(res, [])

        # run evals on standalone
        for name, expl in EXAMPLES.items():
            expl.get("device", "cpu") == "cpu" and self.run_example(
                name,
                step_spec_f("step_spec_cpu_full.yaml"),
            )

        for _js in status_checkers:
            jid, status = _js.result()
            if status not in STATUS_SUCCESS:
                logger.error(f"job {jid} failed!")
                exit(1)

    def test_expl(self, expl_name: str) -> None:
        rt_ = RUNTIMES.get(expl_name) or RUNTIMES.get("pytorch")
        if not rt_:
            raise RuntimeError(f"no runtime matching for {expl_name}")
        for name, rt in RUNTIMES.items():
            if "yamls" not in rt:
                self.build_runtime(str(rt["workdir"]))
            else:
                for yml in list(rt["yamls"]):
                    self.build_runtime(str(rt["workdir"]), yml)

        expl = EXAMPLES[expl_name]
        workdir_ = str(expl["workdir"])

        rc = subprocess.Popen(
            ["make", "CN=1", "prepare"],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            cwd=workdir_,
        )
        if rc != 0:
            logger.error(f"prepare data for {expl} failed")
            raise

        #  download data
        for ds_expl in expl["datasets"]:
            self.build_dataset(expl_name, workdir_, ds_expl)
        self.build_model(workdir_)

        # run_eval
        self.run_example(
            expl_name,
            step_spec_f(f"step_spec_{expl.get('device', 'cpu')}_full.yaml"),
            expl.get("device", "cpu") == "cpu",
        )

    def run_example(
        self, name: str, step_spec: str, local_instance: bool = True
    ) -> t.List[Future]:
        datasets_ = self.datasets.get(name)
        if not datasets_:
            raise RuntimeError("datasets should not be empty")
        model_ = self.models.get(name)
        if not model_:
            raise RuntimeError("model should not be empty")
        runtimes_ = self.runtimes.get(name) or self.runtimes.get("pytorch")
        if not runtimes_:
            raise RuntimeError("runtimes should not be empty")
        return self.eval(model_, runtimes_, datasets_, step_spec, local_instance)

    def debug(self) -> None:
        for name, expl in EXAMPLES.items():
            workdir_ = expl["workdir"]
            self.build_model(str(workdir_))

    def smoke_commands(self) -> None:
        commands = [
            "timeout 2 swcli --help",
            "swcli --version",
        ]

        for cmd in commands:
            _code, _err = invoke(cmd.split())
            if _code != 0:
                raise RuntimeError(f"cmd[{cmd}] run failed, err: {_err}, code: {_code}")


if __name__ == "__main__":
    with ThreadPoolExecutor(
        max_workers=int(os.environ.get("SW_TEST_E2E_THREAD_NUM", "10"))
    ) as executor:
        # start test
        test_cli = TestCli(
            work_dir=WORK_DIR,
            thread_pool=executor,
            server_url=os.environ.get("CONTROLLER_URL"),
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

        test_cli.smoke_commands()
