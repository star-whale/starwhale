
import tempfile
from unittest import TestCase

from .cmds.project_cmd import Project
from .cmds.instance_cmd import Instance
from .cmds.artifacts_cmd import Model, Dataset, Runtime
from .cmds.eval_cmd import Evaluation
from .cmds.base.environment import Environment
from . import ROOT_DIR


class StandaloneTest(TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.work_dir = self.tmp.name
        # set runtime var
        _environment = Environment(src_dir=ROOT_DIR, work_dir=self.work_dir)
        _environment.prepare()

    def test_base_workflow(self):
        # use local instance
        print("login...")
        instance = Instance()
        instance.select("local")
        assert Project().select("self")

        # 1.model build
        print("building model...")
        model = Model()
        assert len(model.list()) == 0
        assert model.build(workdir=f"{self.work_dir}/example/mnist")
        assert len(model.list()) == 1
        assert model.info('mnist/version/latest')

        # 2.dataset build
        print("building dataset")
        dataset = Dataset()
        assert len(dataset.list()) == 0
        assert dataset.build(workdir=f"{self.work_dir}/example/mnist")
        assert len(dataset.list()) == 1
        assert dataset.info('mnist/version/latest')

        # 3.runtime build
        print("building runtime")
        rt = Runtime()
        assert len(rt.list()) == 0
        assert rt.build(workdir=f"{self.work_dir}/example/runtime/pytorch")
        assert len(rt.list()) == 1
        assert rt.info('pytorch/version/latest')

        # 4.eval run
        print("running evaluation")
        _eval = Evaluation()
        assert len(_eval.list()) == 0
        assert _eval.run(model="mnist/version/latest", dataset="mnist/version/latest")
        _eval_list = _eval.list()
        assert len(_eval_list) == 1

        assert _eval.info(_eval_list[0]['manifest']['version'])

    def tearDown(self) -> None:
        self.tmp.cleanup()
