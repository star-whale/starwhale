import tempfile
from unittest import TestCase

from .cmds.project_cmd import Project
from .cmds.instance_cmd import Instance
from .cmds.artifacts_cmd import Model, Dataset, Runtime
from .cmds.eval_cmd import Evaluation
from .cmds.base.environment import Environment
from . import ROOT_DIR


class CloudTest(TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.work_dir = self.tmp.name
        # set runtime var
        _environment = Environment(src_dir=ROOT_DIR, work_dir=self.work_dir)
        _environment.prepare()

    def test_base_workflow(self) -> None:
        # cloud login
        instance = Instance()
        assert instance.login()

        # 1.model build
        model = Model()
        assert len(model.list()) == 0
        assert model.build(workdir=f"{self.work_dir}/example/mnist")
        assert len(model.list()) == 1
        swmp = model.info('mnist/version/latest')
        assert swmp
        assert model.copy(src_uri='mnist/version/latest', target_project="cloud://cloud/project/starwhale", force=True)

        # 2.dataset build
        dataset = Dataset()
        assert len(dataset.list()) == 0
        assert dataset.build(workdir=f"{self.work_dir}/example/mnist")
        assert len(dataset.list()) == 1
        swds = dataset.info('mnist/version/latest')
        assert swds
        assert dataset.copy(src_uri='mnist/version/latest',
                            target_project="cloud://cloud/project/starwhale",
                            with_auth=True,
                            force=True)

        # 3.runtime build
        rt = Runtime()
        assert len(rt.list()) == 0
        assert rt.build(workdir=f"{self.work_dir}/example/runtime/pytorch")
        assert len(rt.list()) == 1
        swrt = rt.info('pytorch/version/latest')
        assert swrt
        assert rt.copy(src_uri='pytorch/version/latest', target_project="cloud://cloud/project/starwhale", force=True)

        # use cloud instance
        instance.select("cloud")
        res = Project().select("starwhale")
        assert res
        print(f"project select self:{res}")

        # eval run
        _eval = Evaluation()
        _origin_job_list = _eval.list(project="starwhale")
        assert len(_origin_job_list) != 0

        assert _eval.run(model=swmp["version"], dataset=swds["version"], runtime=swrt["version"], project="starwhale")
        _new_job_list = _eval.list(project="starwhale")
        assert len(_new_job_list) == len(_origin_job_list) + 1

        _origin_job_ids = [j["manifest"]["id"] for j in _origin_job_list]
        _new_job_ids = [j["manifest"]["id"] for j in _new_job_list]

        _new_job_id = list(set(_new_job_ids) - set(_origin_job_ids))

        print(f"eval info {_new_job_id[0]}:{_eval.info(f'http://console.pre.intra.starwhale.ai/project/starwhale/evaluation/{_new_job_id[0]}')}")

    def tearDown(self) -> None:
        self.tmp.cleanup()
