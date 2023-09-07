import random
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
from click.testing import CliRunner
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import RunStatus
from starwhale.utils.fs import ensure_file
from starwhale.core.job.cli import _list as list_cli
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.job.view import JobTermView, JobTermViewJson, JobTermViewRich
from starwhale.base.scheduler import Step, Scheduler, StepResult, TaskResult
from starwhale.base.models.job import LocalJobInfo


class JobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()

    def test_dag_generator(self):
        # with cycle error
        with self.assertRaises(RuntimeError):
            Step.generate_dag(
                [
                    Step(
                        name="ppl-1",
                        needs=["cmp"],
                    ),
                    Step(
                        name="ppl-2",
                        needs=["ppl-1"],
                    ),
                    Step(
                        name="cmp",
                        needs=["ppl-2"],
                    ),
                ]
            )

        _dag = Step.generate_dag(
            [
                Step(
                    name="base",
                    resources=[],
                    needs=[],
                ),
                Step(
                    name="ppl-1",
                    resources=[],
                    needs=["base"],
                ),
                Step(
                    name="ppl-2",
                    resources=[],
                    needs=["base"],
                ),
                Step(
                    name="cmp",
                    resources=[],
                    needs=["ppl-1", "ppl-2"],
                ),
            ]
        )
        assert len(_dag.all_starts()) == 1
        assert len(_dag.all_terminals()) == 1
        expected_degrees = {
            "base": {"in": 0, "out": 2},
            "ppl-1": {"in": 1, "out": 1},
            "ppl-2": {"in": 1, "out": 1},
            "cmp": {"in": 2, "out": 0},
        }

        for k, v in expected_degrees.items():
            assert _dag.in_degree(k) == v["in"]
            assert _dag.out_degree(k) == v["out"]

    def test_step_result(self):
        _task_results = list(TaskResult(i, RunStatus.SUCCESS) for i in range(3))
        _step_result = StepResult("ppl-test", _task_results)
        self.assertEqual(_step_result.status, RunStatus.SUCCESS)

        _task_results.append(TaskResult(6, RunStatus.FAILED))
        _step_result = StepResult("ppl-test2", _task_results)
        self.assertEqual(_step_result.status, RunStatus.FAILED)

    def test_scheduler_cycle_exception(self):
        with self.assertRaises(RuntimeError):
            Scheduler(
                project="self",
                version="fdsie8rwe",
                workdir=Path(),
                dataset_uris=["mnist/version/tu788", "mnist/version/tu789"],
                steps=[
                    Step(
                        name="ppl",
                        resources=[{"type": "cpu", "limit": 1, "request": 1}],
                        concurrency=1,
                        task_num=2,
                        # cycle point
                        needs=["cmp"],
                    ),
                    Step(
                        name="cmp",
                        resources=[{"type": "cpu", "limit": 1, "request": 1}],
                        concurrency=1,
                        task_num=2,
                        needs=["ppl"],
                    ),
                ],
            )

    @patch("starwhale.base.scheduler.TaskExecutor.execute")
    def test_scheduler(self, m_task_execute: MagicMock):
        m_task_execute.return_value = TaskResult(
            id=random.randint(0, 10), status=RunStatus.SUCCESS
        )

        _scheduler = Scheduler(
            project="self",
            version="fdsie8rwe",
            workdir=Path(),
            dataset_uris=["mnist/version/tu788", "mnist/version/tu789"],
            steps=[
                Step(
                    name="ppl",
                    resources=[{"type": "cpu", "limit": 1, "request": 1}],
                    concurrency=1,
                    task_num=2,
                    needs=[],
                ),
                Step(
                    name="cmp",
                    resources=[{"type": "cpu", "limit": 1, "request": 1}],
                    concurrency=1,
                    task_num=1,
                    needs=["ppl"],
                ),
            ],
        )
        _results = _scheduler._schedule_all()
        assert m_task_execute.call_count == 3
        self.assertEqual(
            all([_rt.status == RunStatus.SUCCESS for _rt in _results]), True
        )

        m_task_execute.reset_mock()
        _single_result = _scheduler._schedule_one_task("ppl", 0)

        assert m_task_execute.call_count == 1
        self.assertEqual(_single_result.status, RunStatus.SUCCESS)

        m_task_execute.reset_mock()
        _single_result = _scheduler._schedule_one_step("ppl")

        assert m_task_execute.call_count == 2
        self.assertEqual(_single_result.status, RunStatus.SUCCESS)

    def test_list_for_standalone(self) -> None:
        sw = SWCliConfigMixed()
        ensure_file(
            sw.rootdir / "self" / "job" / "12" / "123456" / "_manifest.yaml",
            yaml.safe_dump(
                {
                    "created_at": "2023-05-15 16:03:04 CST",
                    "datasets": ["mnist-mini"],
                    "finished_at": "2023-05-15 16:03:07 CST",
                    "model_src_dir": "mock1",
                    "runtime": "test",
                    "project": "self",
                    "status": "failed",
                    "version": "123456",
                }
            ),
            parents=True,
        )
        ensure_file(
            sw.rootdir / "self" / "job" / "23" / "234567" / "_manifest.yaml",
            yaml.safe_dump(
                {
                    "created_at": "2023-05-15 16:03:04 CST",
                    "datasets": ["mnist-mini", "minst-2"],
                    "finished_at": "2023-05-15 16:03:07 CST",
                    "model_src_dir": "mock2",
                    "project": "self",
                    "status": "success",
                    "version": "234567",
                }
            ),
            parents=True,
        )

        jobs, pages_info = JobTermView.list()
        assert len(jobs) == 2
        assert isinstance(jobs[0], LocalJobInfo)
        assert jobs[0].manifest.project == "self"
        assert pages_info == {}

        JobTermViewRich.list(project_uri="self", fullname=True)
        JobTermViewJson.list(fullname=False)

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            list_cli,
            [
                "--project",
                "self",
                "--fullname",
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.list.call_count == 1
        assert mock_obj.list.call_args[0][0] == "self"
        assert mock_obj.list.call_args[1] == {
            "fullname": True,
            "page": 1,
            "size": 20,
        }
