import os
import tempfile
from time import sleep

import pytest

from .cmds.base.invoke import invoke
from .cmds.project_cmd import Project
from .cmds.instance_cmd import Instance
from .cmds.artifacts_cmd import Model, Dataset, Runtime
from .cmds.eval_cmd import Evaluation
from .cmds.base.common import EnvironmentPrepare
from . import ROOT_DIR


# all test case use one work_dir in this class
@pytest.fixture(scope='class', name="work_dir")
def work_dir(request):
    _work_dir = os.environ.get("SW_WORK_DIR")
    print(f"work-dir is:{_work_dir}")
    if not _work_dir:
        tmp = tempfile.TemporaryDirectory()
        _work_dir = tmp.name

        os.environ["SW_CLI_CONFIG"] = f"{_work_dir}/config.yaml"
        os.environ["SW_LOCAL_STORAGE"] = f"{_work_dir}/data"

        invoke(["cp", "-rf", f"{ROOT_DIR}/example", f"{_work_dir}/example"])
        invoke(["cp", "-rf", f"{ROOT_DIR}/client", f"{_work_dir}/client"])
        invoke(["cp", "-rf", f"{ROOT_DIR}/README.md", f"{_work_dir}/README.md"])

        def teardown():
            tmp.cleanup()

        request.addfinalizer(teardown)
    # install sw at
    invoke(["pip", "install", "-e", f"{_work_dir}/client"])
    _res, _err = invoke(["swcli", "--version"])
    print(f"pytest use swcli version is:{_res}")
    return _work_dir


class TestCli:
    instance = Instance()
    project = Project()
    model = Model()
    dataset = Dataset()
    runtime = Runtime()
    evaluation = Evaluation()

    def standard_workflow(self, model_name: str, model_workdir: str,
                          ds_name: str, ds_workdir: str,
                          rt_name: str, rt_workdir: str,
                          cloud_uri: str, cloud_project: str):
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
        print("building dataset")
        _ds_uri = f"{ds_name}/version/latest"
        assert len(self.dataset.list()) == 0
        assert self.dataset.build(workdir=ds_workdir)
        assert len(self.dataset.list()) == 1
        swds = self.dataset.info(_ds_uri)
        assert swds

        # 3.runtime build
        print("building runtime")
        _rt_uri = f"{rt_name}/version/latest"
        assert len(self.runtime.list()) == 0
        assert self.runtime.build(workdir=rt_workdir)
        assert len(self.runtime.list()) == 1
        swrt = self.runtime.info(_rt_uri)
        assert swrt

        # 4.run evaluation on local instance
        print("running evaluation")
        assert len(self.evaluation.list()) == 0
        assert self.evaluation.run(model=_model_uri, dataset=_ds_uri)
        _eval_list = self.evaluation.list()
        assert len(_eval_list) == 1

        assert self.evaluation.info(_eval_list[0]['manifest']['version'])

        # 5.login to cloud
        assert self.instance.login(url=cloud_uri)

        # 6.copy local artifacts to cloud
        assert self.model.copy(src_uri=_model_uri,
                               target_project=f"cloud://cloud/project/{cloud_project}",
                               force=True)
        assert self.dataset.copy(src_uri=_ds_uri,
                                 target_project=f"cloud://cloud/project/{cloud_project}",
                                 with_auth=True,
                                 force=True)
        assert self.runtime.copy(src_uri=_rt_uri,
                                 target_project=f"cloud://cloud/project/{cloud_project}",
                                 force=True)

        # 7.select to cloud instance
        self.instance.select(instance="cloud")
        self.project.select(project=cloud_project)

        _origin_job_list = self.evaluation.list(project=cloud_project)

        # 8.start an evaluation
        assert self.evaluation.run(model=swmp["version"], dataset=swds["version"], runtime=swrt["version"],
                                   project=cloud_project)
        _new_job_list = self.evaluation.list(project=cloud_project)
        assert len(_new_job_list) == len(_origin_job_list) + 1

        _origin_job_ids = [j["manifest"]["id"] for j in _origin_job_list]
        _new_job_ids = [j["manifest"]["id"] for j in _new_job_list]

        _new_job_id = list(set(_new_job_ids) - set(_origin_job_ids))[0]

        # 9.check job's status
        _job_status = self.get_job_status(cloud_uri=cloud_uri, cloud_project=cloud_project, job_id=_new_job_id)

        while True:
            if _job_status == "SUCCESS" or _job_status == "FAIL":
                break
            sleep(2)
            _job_status = self.get_job_status(cloud_uri=cloud_uri, cloud_project=cloud_project, job_id=_new_job_id)

        assert _job_status == "SUCCESS"

        # 10.reset instance to local
        self.instance.select("local")

    def get_job_status(self, cloud_uri: str, cloud_project: str, job_id: str) -> str:
        _remote_job = self.evaluation.info(f'{cloud_uri}/project/{cloud_project}/evaluation/{job_id}')
        return _remote_job["manifest"]["jobStatus"] if _remote_job else ""

    def test_mnist(self, work_dir) -> None:
        _environment_prepare = EnvironmentPrepare(work_dir=work_dir)
        _environment_prepare.prepare_mnist_data()
        _environment_prepare.prepare_mnist_requirements()
        print(f"controller url is:{os.environ.get('CONTROLLER_URL')}")
        self.standard_workflow(
            model_name="mnist",
            model_workdir=f"{work_dir}/example/mnist",
            ds_name="mnist",
            ds_workdir=f"{work_dir}/example/mnist",
            rt_name="pytorch",
            rt_workdir=f"{work_dir}/example/runtime/pytorch",
            cloud_uri=os.environ.get("CONTROLLER_URL") or "http://console.pre.intra.starwhale.ai",
            cloud_project="starwhale"
        )
    # TODO add more example
