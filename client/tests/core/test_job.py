import os
import time
import random
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import DEFAULT_EVALUATION_PIPELINE, DEFAULT_EVALUATION_JOBS_FNAME
from starwhale.utils.fs import ensure_dir
from starwhale.api._impl.job import Parser
from starwhale.core.job.model import (
    Step,
    STATUS,
    Generator,
    StepResult,
    TaskResult,
    MultiThreadProcessor,
)
from starwhale.core.job.scheduler import Scheduler


class JobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()

    def test_generate_job_yaml(self):
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
          resources:
          - cpu=1
          step_name: ppl
          task_num: 1
        - concurrency: 1
          job_name: default
          needs:
          - ppl
          resources:
          - cpu=1
          step_name: cmp
          task_num: 1
        """
        jobs = Generator.generate_job_from_yaml(_f)

        self.assertEqual("default" in jobs, True)
        self.assertEqual(len(jobs["default"]), 2)

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
                        resources=[],
                        needs=["cmp"],
                    ),
                    Step(
                        job_name="default",
                        step_name="ppl-2",
                        resources=[],
                        needs=["ppl-1"],
                    ),
                    Step(
                        job_name="default",
                        step_name="cmp",
                        resources=[],
                        needs=["ppl-2"],
                    ),
                ]
            )
        # generate successfully
        Generator.generate_dag_from_steps(
            [
                Step(
                    job_name="default",
                    step_name="ppl-1",
                    resources=[],
                    needs=[],
                ),
                Step(
                    job_name="default",
                    step_name="ppl-2",
                    resources=[],
                    needs=["ppl-1"],
                ),
                Step(
                    job_name="default",
                    step_name="cmp",
                    resources=[],
                    needs=["ppl-2"],
                ),
            ]
        )

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
                        resources=["cpu=1"],
                        concurrency=1,
                        task_num=2,
                        # cycle point
                        needs=["cmp"],
                    ),
                    Step(
                        job_name="default",
                        step_name="cmp",
                        resources=["cpu=1"],
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
                    resources=["cpu=1"],
                    concurrency=1,
                    task_num=2,
                    needs=[],
                ),
                Step(
                    job_name="default",
                    step_name="cmp",
                    resources=["cpu=1"],
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
