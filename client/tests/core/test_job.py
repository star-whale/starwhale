import random
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
from click.testing import CliRunner
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import RunStatus, HTTPMethod
from starwhale.utils.fs import ensure_file
from starwhale.core.job.cli import _list as list_cli
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.job.view import JobTermView, JobTermViewJson, JobTermViewRich
from starwhale.base.scheduler import Step, Scheduler, StepResult, TaskResult
from starwhale.core.instance.view import InstanceTermView


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
        assert jobs[0]["manifest"]["project"] == "self"
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

    @Mocker()
    def test_list_for_server(self, rm: Mocker) -> None:
        base_url = "http://1.1.0.0:8182/api/v1"

        rm.request(
            HTTPMethod.POST,
            f"{base_url}/login",
            json={"data": {"name": "foo", "role": {"roleName": "admin"}}},
            headers={"Authorization": "token"},
        )

        rm.request(
            HTTPMethod.GET,
            f"{base_url}/project/1/job",
            json={
                "code": "success",
                "data": {
                    "endRow": 1,
                    "hasNextPage": True,
                    "hasPreviousPage": False,
                    "isFirstPage": True,
                    "isLastPage": False,
                    "list": [
                        {
                            "comment": None,
                            "createdTime": 1692790991000,
                            "datasetList": [
                                {
                                    "createdTime": 1692172104000,
                                    "id": "133",
                                    "name": "cmmlu",
                                    "owner": None,
                                    "version": {
                                        "alias": "v1",
                                        "createdTime": 1692172104000,
                                        "id": "190",
                                        "indexTable": "project/257/dataset/cmmlu/_current/meta",
                                        "latest": True,
                                        "name": "kiwtxaz7h3a4atp3rjhhymp3mgbxvjtuip7cklzc",
                                        "owner": None,
                                        "shared": 0,
                                        "tags": None,
                                    },
                                }
                            ],
                            "duration": 20290782,
                            "exposedLinks": [],
                            "id": "722",
                            "jobName": "src.evaluation:evaluation_results",
                            "jobStatus": "SUCCESS",
                            "runtime": {
                                "name": "test",
                                "version": {
                                    "name": "123",
                                },
                            },
                            "model": {
                                "createdTime": 1692777636000,
                                "id": "162",
                                "name": "llama2-13b-chinese",
                                "version": {
                                    "alias": "v2",
                                    "builtInRuntime": None,
                                    "createdTime": 1692790804000,
                                    "id": "196",
                                    "latest": True,
                                    "name": "2pcj3y7hnpqdmqzsl3atcsedupwedp726yrd7bec",
                                    "owner": None,
                                    "shared": 0,
                                    "size": 26509991838,
                                },
                            },
                            "modelName": "llama2-13b-chinese",
                            "modelVersion": "2pcj3y7hnpqdmqzsl3atcsedupwedp726yrd7bec",
                            "resourcePool": "A100 80G * 1",
                            "stopTime": 1692811282000,
                            "uuid": "5c6dc44d410349829a7c6c1916a20651",
                        }
                    ],
                    "nextPage": 2,
                    "pageNum": 1,
                    "pageSize": 1,
                    "pages": 55,
                    "prePage": 0,
                    "size": 1,
                    "startRow": 0,
                    "total": 55,
                },
                "message": "Success",
            },
        )

        InstanceTermView().login(
            "http://1.1.0.0:8182",
            alias="remote",
            username="foo",
            password="bar",
        )

        jobs, pages_info = JobTermView.list(
            project_uri="cloud://remote/project/1",
            fullname=True,
            page=2,
            size=10,
        )
        assert len(jobs) == 1
        assert pages_info["current"] == 1
        assert pages_info["total"] == 55
        assert pages_info["remain"] == 54
        assert pages_info["page"]["page_num"] == 1
        assert jobs[0]["manifest"]["id"] == "722"

        JobTermViewRich.list(project_uri="cloud://remote/project/1", fullname=False)
        JobTermViewJson.list(project_uri="cloud://remote/project/1")

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            list_cli,
            [
                "--project",
                "cloud://remote/project/1",
                "--fullname",
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.list.call_count == 1
        assert mock_obj.list.call_args[0][0] == "cloud://remote/project/1"
        assert mock_obj.list.call_args[1] == {
            "fullname": True,
            "page": 1,
            "size": 20,
        }
