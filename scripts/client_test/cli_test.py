import tempfile
import pytest

from .cmds.project_cmd import Project
from .cmds.instance_cmd import Instance
from .cmds.artifacts_cmd import Model, Dataset, Runtime
from .cmds.eval_cmd import Evaluation
from .cmds.base.environment import Environment
from . import ROOT_DIR


@pytest.fixture(scope='class', name="work_dir")
def work_dir(request):
    tmp = tempfile.TemporaryDirectory()
    # set runtime var(all test case use one work_dir in this class )
    _environment = Environment(src_dir=ROOT_DIR, work_dir=tmp.name)
    _environment.prepare()

    def teardown():
        tmp.cleanup()

    request.addfinalizer(teardown)
    return tmp.name


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

        # login to cloud
        assert self.instance.login()

        # copy local artifacts to cloud
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

        # select to cloud instance
        self.instance.select(instance="cloud")
        self.project.select(project=cloud_project)

        _origin_job_list = self.evaluation.list(project=cloud_project)

        assert self.evaluation.run(model=swmp["version"], dataset=swds["version"], runtime=swrt["version"],
                                   project=cloud_project)
        _new_job_list = self.evaluation.list(project=cloud_project)
        assert len(_new_job_list) == len(_origin_job_list) + 1

        _origin_job_ids = [j["manifest"]["id"] for j in _origin_job_list]
        _new_job_ids = [j["manifest"]["id"] for j in _new_job_list]

        _new_job_id = list(set(_new_job_ids) - set(_origin_job_ids))

        assert self.evaluation.info(
            f'{cloud_uri}/project/{cloud_project}/evaluation/{_new_job_id[0]}')

    def test_mnist(self, work_dir) -> None:
        _environment = Environment(src_dir=ROOT_DIR, work_dir=work_dir)
        _environment.prepare_mnist_dir()
        self.standard_workflow(
            model_name="mnist",
            model_workdir=f"{work_dir}/example/mnist",
            ds_name="mnist",
            ds_workdir=f"{work_dir}/example/mnist",
            rt_name="pytorch",
            rt_workdir=f"{work_dir}/example/runtime/pytorch",
            cloud_uri="http://console.pre.intra.starwhale.ai",
            cloud_project="starwhale"
        )
    # TODO add more example
