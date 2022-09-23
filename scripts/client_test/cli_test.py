import os
import sys
import tempfile
from time import sleep
from typing import Any

from cmds.eval_cmd import Evaluation
from cmds.base.common import EnvironmentPrepare
from cmds.base.invoke import invoke
from cmds.project_cmd import Project
from cmds.instance_cmd import Instance
from cmds.artifacts_cmd import Model, Dataset, Runtime

CURRENT_DIR = os.path.dirname(__file__)
SCRIPT_DIR = os.path.abspath(os.path.join(CURRENT_DIR, os.pardir))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, os.pardir))


class TestCli:
    instance = Instance()
    project = Project()
    model = Model()
    dataset = Dataset()
    runtime = Runtime()
    evaluation = Evaluation()

    def __init__(self, work_dir: str) -> None:
        self._work_dir = work_dir

    def standard_workflow(
        self,
        mode: str,
        model_name: str,
        model_workdir: str,
        ds_name: str,
        ds_workdir: str,
        rt_name: str,
        rt_workdir: str,
        cloud_uri: str,
        cloud_project: str,
    ) -> None:
        # use local instance
        self.instance.select("local")
        assert self.project.select("self")

        # 1.model build
        print("building model...")
        _model_uri = f"{model_name}/version/latest"
        assert len(self.model.list()) == 0
        assert self.model.build(workdir=model_workdir)
        assert len(self.model.list()) == 1
        swmp = self.model.info(_model_uri)
        assert swmp

        # 2.dataset build
        print("building dataset...")
        _ds_uri = f"{ds_name}/version/latest"
        assert len(self.dataset.list()) == 0
        assert self.dataset.build(workdir=ds_workdir)
        assert len(self.dataset.list()) == 1
        swds = self.dataset.info(_ds_uri)
        assert swds

        # 3.runtime build
        print("building runtime...")
        _rt_uri = f"{rt_name}/version/latest"
        assert len(self.runtime.list()) == 0
        assert self.runtime.build(workdir=rt_workdir)
        assert len(self.runtime.list()) == 1
        swrt = self.runtime.info(_rt_uri)
        assert swrt

        # 4.run evaluation on local instance
        print("running evaluation at local...")
        assert len(self.evaluation.list()) == 0
        assert self.evaluation.run(model=_model_uri, dataset=_ds_uri)
        _eval_list = self.evaluation.list()
        assert len(_eval_list) == 1

        assert self.evaluation.info(_eval_list[0]["manifest"]["version"])
        if mode != RunMode.CLOUD:
            return
        # 5.login to cloud
        print(f"login to cloud {cloud_uri} ...")
        assert self.instance.login(url=cloud_uri)

        # 6.copy local artifacts to cloud
        print("copy local artifacts to cloud...")
        assert self.model.copy(
            src_uri=_model_uri,
            target_project=f"cloud://cloud/project/{cloud_project}",
            force=True,
        )
        assert self.dataset.copy(
            src_uri=_ds_uri,
            target_project=f"cloud://cloud/project/{cloud_project}",
            with_auth=True,
            force=True,
        )
        assert self.runtime.copy(
            src_uri=_rt_uri,
            target_project=f"cloud://cloud/project/{cloud_project}",
            force=True,
        )

        # 7.select to cloud instance
        self.instance.select(instance="cloud")
        self.project.select(project=cloud_project)

        _origin_job_list = self.evaluation.list(project=cloud_project)

        # 8.start an evaluation

        print("running evaluation at cloud...")
        assert self.evaluation.run(
            model=swmp["version"],
            dataset=swds["version"],
            runtime=swrt["version"],
            project=cloud_project,
        )
        _new_job_list = self.evaluation.list(project=cloud_project)
        assert len(_new_job_list) == len(_origin_job_list) + 1

        _origin_job_ids = [j["manifest"]["id"] for j in _origin_job_list]
        _new_job_ids = [j["manifest"]["id"] for j in _new_job_list]

        _new_job_id = list(set(_new_job_ids) - set(_origin_job_ids))[0]

        # 9.check job's status
        _job_status = self.get_job_status(
            cloud_uri=cloud_uri, cloud_project=cloud_project, job_id=_new_job_id
        )

        while True:
            if _job_status == "SUCCESS" or _job_status == "FAIL":
                break
            sleep(10)
            _job_status = self.get_job_status(
                cloud_uri=cloud_uri, cloud_project=cloud_project, job_id=_new_job_id
            )
            if _job_status:
                print(f"job status is:{_job_status}")
            else:
                print("job status api occur some error!now will exit")
                break

        assert _job_status == "SUCCESS"

        # 10.reset instance to local
        self.instance.select("local")

    def get_job_status(self, cloud_uri: str, cloud_project: str, job_id: str) -> Any:
        _remote_job = self.evaluation.info(
            f"{cloud_uri}/project/{cloud_project}/evaluation/{job_id}"
        )
        return _remote_job["manifest"]["jobStatus"] if _remote_job else ""

    def test_mnist(self, mode: str) -> None:
        _environment_prepare = EnvironmentPrepare(work_dir=self._work_dir)
        _environment_prepare.prepare_mnist_data()
        _environment_prepare.prepare_mnist_requirements()
        print(f"controller url is:{os.environ.get('CONTROLLER_URL')}")
        self.standard_workflow(
            mode=mode,
            model_name="mnist",
            model_workdir=f"{self._work_dir}/example/mnist",
            ds_name="mnist",
            ds_workdir=f"{self._work_dir}/example/mnist",
            rt_name="pytorch",
            rt_workdir=f"{self._work_dir}/example/runtime/pytorch",
            cloud_uri=os.environ.get("CONTROLLER_URL") or "http://127.0.0.1:8082",
            cloud_project="starwhale",
        )

    # TODO add more example


def init_run_environment() -> str:
    # prepare environment
    _work_dir = os.environ.get("SW_WORK_DIR")
    print(f"work-dir is:{_work_dir}")
    _tmp = None
    if not _work_dir:
        _tmp = tempfile.TemporaryDirectory()
        _work_dir = _tmp.name
        print(f"use work-dir is:{_work_dir}")

        os.environ["SW_CLI_CONFIG"] = f"{_work_dir}/config.yaml"
        os.environ["SW_LOCAL_STORAGE"] = f"{_work_dir}/data"

        invoke(["cp", "-rf", f"{ROOT_DIR}/example", f"{_work_dir}/example"])
        invoke(["cp", "-rf", f"{ROOT_DIR}/client", f"{_work_dir}/client"])
        invoke(["cp", "-rf", f"{ROOT_DIR}/README.md", f"{_work_dir}/README.md"])

    # install sw at current session
    print(f"env PYPI_RELEASE_VERSION is:{os.environ.get('PYPI_RELEASE_VERSION')}")
    invoke(["pip", "install", "-e", f"{_work_dir}/client"])
    _res, _err = invoke(["swcli", "--version"])
    print(f"pytest use swcli version is:{_res}")
    return _work_dir


class RunMode:
    STANDALONE = "standalone"
    CLOUD = "cloud"


if __name__ == "__main__":
    # start test
    test_cli = TestCli(work_dir=init_run_environment())
    example = sys.argv[1]
    _mode = RunMode.CLOUD if os.environ.get("CONTROLLER_URL") else RunMode.STANDALONE
    if example == "mnist":
        test_cli.test_mnist(_mode)
    else:
        print("there is nothing to run!")
