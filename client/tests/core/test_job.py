import os
import time
import random
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.consts import DEFAULT_EVALUATION_PIPELINE, DEFAULT_EVALUATION_JOBS_FNAME
from starwhale.utils.fs import ensure_dir
from starwhale.api._impl.job import Parser, Context
from starwhale.core.job.model import (
    Step,
    STATUS,
    Generator,
    StepResult,
    TaskResult,
    TaskExecutor,
    MultiThreadProcessor,
)
from starwhale.core.job.scheduler import Scheduler

_job_data_dir = f"{ROOT_DIR}/data/job"


class JobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()

    def test_generate_default_job_yaml(self):
        Parser.clear_config()
        # create a temporary directory
        root = "home/starwhale/job"
        ensure_dir(root)
        _f = os.path.join(root, DEFAULT_EVALUATION_JOBS_FNAME)

        Parser.generate_job_yaml(DEFAULT_EVALUATION_PIPELINE, Path(root), Path(_f))

        """
        default:
        - concurrency: 1
          job_name: default
          needs: []
          resources: {}
          step_name: ppl
          task_num: 1
        - concurrency: 1
          job_name: default
          needs:
          - ppl
          resources: {}
          step_name: cmp
          task_num: 1
        """
        jobs = Generator.generate_job_from_yaml(_f)
        _steps = jobs["default"]
        self.assertEqual("default" in jobs, True)
        self.assertEqual(len(_steps), 2)
        self.assertEqual(_steps[0].step_name, "ppl")
        self.assertEqual(_steps[1].step_name, "cmp")

    def test_generate_job_yaml_with_error(self):
        Parser.clear_config()
        # create a temporary directory
        root = "home/starwhale/job"
        ensure_dir(root)
        _f = os.path.join(root, DEFAULT_EVALUATION_JOBS_FNAME)

        with self.assertRaises(RuntimeError) as context:
            Parser.generate_job_yaml(
                "job_steps_with_error", Path(_job_data_dir), Path(_f)
            )
            self.assertTrue("resources value is illegal2" in context.exception)

    def test_generate_custom_job_yaml(self):
        Parser.clear_config()
        # create a temporary directory
        root = "home/starwhale/job"
        ensure_dir(root)
        _f = os.path.join(root, DEFAULT_EVALUATION_JOBS_FNAME)

        Parser.generate_job_yaml("job_steps_with_cls", Path(_job_data_dir), Path(_f))

        """
        default:
        - concurrency: 1
          job_name: default
          needs: []
          resources:
            cpu:
              request: 1
              limit: 1
          step_name: custom_ppl
          task_num: 1
        - concurrency: 1
          job_name: default
          needs:
          - custom_ppl
          resources:
            cpu:
              request: 1
              limit: 2
          step_name: custom_cmp
          task_num: 1
        """
        jobs = Generator.generate_job_from_yaml(_f)
        _steps = jobs["default"]
        self.assertEqual("default" in jobs, True)
        self.assertEqual(len(_steps), 2)
        self.assertEqual(_steps[0].step_name, "custom_ppl")
        self.assertEqual(_steps[0].cls_name, "CustomPipeline")
        self.assertEqual(_steps[1].step_name, "custom_cmp")
        self.assertEqual(_steps[0].resources["cpu"], {"limit": 1, "request": 1})
        self.assertEqual(_steps[1].resources["cpu"], {"limit": 2, "request": 1})

    def test_job_check(self):
        self.assertEqual(
            Parser.check(
                {
                    "default": [
                        {"step_name": "ppl", "needs": [""]},
                        {"step_name": "cmp", "needs": ["ppl2"]},
                    ]
                }
            ),
            False,
        )

        self.assertEqual(
            Parser.check(
                {
                    "default": [
                        {"step_name": "ppl", "needs": [""]},
                        {"step_name": "cmp", "needs": ["ppl"]},
                    ]
                }
            ),
            True,
        )

    def test_dag_generator(self):
        # with cycle error
        with self.assertRaises(RuntimeError):
            Generator.generate_dag_from_steps(
                [
                    Step(
                        job_name="default",
                        step_name="ppl-1",
                        resources={},
                        needs=["cmp"],
                    ),
                    Step(
                        job_name="default",
                        step_name="ppl-2",
                        resources={},
                        needs=["ppl-1"],
                    ),
                    Step(
                        job_name="default",
                        step_name="cmp",
                        resources={},
                        needs=["ppl-2"],
                    ),
                ]
            )
        # generate successfully
        _dag = Generator.generate_dag_from_steps(
            [
                Step(
                    job_name="default",
                    step_name="ppl-1",
                    resources={},
                    needs=[],
                ),
                Step(
                    job_name="default",
                    step_name="ppl-2",
                    resources={},
                    needs=["ppl-1"],
                ),
                Step(
                    job_name="default",
                    step_name="cmp",
                    resources={},
                    needs=["ppl-2"],
                ),
            ]
        )
        assert len(_dag.all_starts()) == 1
        assert len(_dag.all_terminals()) == 1
        assert _dag.in_degree("ppl-1") == 0
        assert _dag.in_degree("ppl-2") == 1
        assert _dag.in_degree("cmp") == 1

    @patch("starwhale.core.job.model.load_cls")
    @patch("starwhale.core.job.model.get_func_from_module")
    @patch("starwhale.core.job.model.get_func_from_object")
    def test_task_executor_with_class(
        self,
        m_get_from_object: MagicMock,
        m_get_from_module: MagicMock,
        m_load_cls: MagicMock,
    ):
        _task_executor = TaskExecutor(
            index=0,
            context=Context(workdir=Path()),
            status=STATUS.START,
            func="custom_ppl",
            cls_name="CustomPipeline",
            module="job_steps_with_cls",
            workdir=Path(_job_data_dir),
        )
        _result = _task_executor.execute()
        assert _result.status == STATUS.SUCCESS
        m_get_from_object.assert_called_once()
        m_load_cls.assert_called_once()
        m_get_from_module.assert_not_called()

    @patch("starwhale.core.job.model.load_cls")
    @patch("starwhale.core.job.model.get_func_from_module")
    @patch("starwhale.core.job.model.get_func_from_object")
    def test_task_executor_without_class(
        self,
        m_get_from_object: MagicMock,
        m_get_from_module: MagicMock,
        m_load_cls: MagicMock,
    ):
        _task_executor = TaskExecutor(
            index=0,
            context=Context(workdir=Path()),
            status=STATUS.START,
            func="custom_ppl",
            module="job_steps_without_cls",
            workdir=Path(_job_data_dir),
        )
        _result = _task_executor.execute()
        assert _result.status == STATUS.SUCCESS
        m_get_from_object.assert_not_called()
        m_load_cls.assert_not_called()
        m_get_from_module.assert_called_once()

    def test_multithread_processor(self):
        class SimpleExecutor:
            def __init__(self):
                self.exec_time = 0.0

            def execute(self) -> t.Any:
                _start = time.time()
                time.sleep(1)
                self.exec_time = time.time() - _start
                return "success"

        _s1 = SimpleExecutor()
        _s2 = SimpleExecutor()
        _m = MultiThreadProcessor("test", 2, [_s1, _s2])
        _results = _m.execute()
        self.assertEqual(all(_r == "success" for _r in _results), True)

    def test_step_result(self):
        _task_results = list(TaskResult(i, STATUS.SUCCESS) for i in range(3))
        _step_result = StepResult("ppl-test", _task_results)
        self.assertEqual(_step_result.status, STATUS.SUCCESS)

        _task_results.append(TaskResult(6, STATUS.FAILED))
        _step_result = StepResult("ppl-test2", _task_results)
        self.assertEqual(_step_result.status, STATUS.FAILED)

    def test_scheduler_cycle_exception(self):
        with self.assertRaises(RuntimeError):
            Scheduler(
                project="self",
                version="fdsie8rwe",
                module="test",
                workdir=Path(),
                dataset_uris=["mnist/version/tu788", "mnist/version/tu789"],
                steps=[
                    Step(
                        job_name="default",
                        step_name="ppl",
                        resources={"cpu": 1},
                        concurrency=1,
                        task_num=2,
                        # cycle point
                        needs=["cmp"],
                    ),
                    Step(
                        job_name="default",
                        step_name="cmp",
                        resources={"cpu": 1},
                        concurrency=1,
                        task_num=2,
                        needs=["ppl"],
                    ),
                ],
            )

    @patch("starwhale.core.job.model.TaskExecutor.execute")
    def test_scheduler(self, m_task_execute: MagicMock):
        m_task_execute.return_value = TaskResult(
            task_id=random.randint(0, 10), status=STATUS.SUCCESS
        )

        _scheduler = Scheduler(
            project="self",
            version="fdsie8rwe",
            module="test",
            workdir=Path(),
            dataset_uris=["mnist/version/tu788", "mnist/version/tu789"],
            steps=[
                Step(
                    job_name="default",
                    step_name="ppl",
                    resources={"cpu": 1},
                    concurrency=1,
                    task_num=2,
                    needs=[],
                ),
                Step(
                    job_name="default",
                    step_name="cmp",
                    resources={"cpu": 1},
                    concurrency=1,
                    task_num=1,
                    needs=["ppl"],
                ),
            ],
        )
        _results = _scheduler.schedule()
        # total task num is 3
        assert m_task_execute.call_count == 3
        self.assertEqual(all([_rt.status == STATUS.SUCCESS for _rt in _results]), True)

        _single_result = _scheduler.schedule_single_task("ppl", 0)

        # total task num is 3 + 1 = 4
        assert m_task_execute.call_count == 4
        self.assertEqual(_single_result.status, STATUS.SUCCESS)

        _single_result = _scheduler.schedule_single_step("ppl")

        # total task num is 3 + 1 + 2 = 6
        assert m_task_execute.call_count == 6
        self.assertEqual(_single_result.status, STATUS.SUCCESS)
