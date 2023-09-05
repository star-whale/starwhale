import json
import datetime

import yaml
from requests_mock import Mocker

from starwhale.consts import HTTPMethod
from starwhale.utils.fs import ensure_file
from starwhale.api._impl import wrapper
from starwhale.utils.error import NotFoundError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.instance.view import InstanceTermView
from starwhale.api._impl.job.model import Job

from .. import BaseTestCase


class TestJob(BaseTestCase):
    def _prepare_standalone(self):
        sw = SWCliConfigMixed()
        ensure_file(
            sw.rootdir / "self" / "job" / "12" / "123456" / "_manifest.yaml",
            yaml.safe_dump(
                {
                    "created_at": "2023-05-15 16:03:04 UTC",
                    "datasets": ["mnist-mini"],
                    "finished_at": "2023-05-15 16:03:07 UTC",
                    "model_src_dir": "mock1",
                    "runtime": "test",
                    "project": "self",
                    "status": "failed",
                    "version": "123456",
                    "model": "llama2-13b-chinese",
                }
            ),
            parents=True,
        )
        e = wrapper.Evaluation("123456", "self")
        e.log_summary_metrics({"accuracy": 0.9})
        e.log("table/1", id=1, output=3)
        e.log("table/2", id=2, output=4)
        e.log_result({"id": "1", "output": 3})
        e.close()
        ensure_file(
            sw.rootdir / "self" / "job" / "12" / "1256789" / "_manifest.yaml",
            yaml.safe_dump(
                {
                    "created_at": "2023-05-15 16:03:04",
                    "datasets": ["mnist-mini", "mnist-2"],
                    "finished_at": "",
                    "model_src_dir": "mock2",
                    "project": "self",
                    "status": "success",
                    "version": "1256789",
                }
            ),
            parents=True,
        )
        e = wrapper.Evaluation("1256789", "self")
        e.log_summary_metrics({"accuracy": 0.91})
        e.log_result({"id": "1", "output": 3})
        e.close()

    def test_parse_datetime(self) -> None:
        assert Job._try_parse_datetime("") is None
        assert Job._try_parse_datetime(" ") is None

        for example in ("2023-05-15 16:03:04", "2023-05-15 16:03:04 UTC", "16:03:04"):
            assert isinstance(Job._try_parse_datetime(example), datetime.datetime)

        with self.assertRaisesRegex(ValueError, "can not parse datetime string"):
            Job._try_parse_datetime("2023-05-15")

    def test_list_jobs_from_standalone(self) -> None:
        self._prepare_standalone()
        jobs, pages = Job.list()
        assert pages == {}
        assert len(jobs) == 2
        assert jobs[0].id == "123456" == jobs[0].datastore_uuid
        assert jobs[0].project.name == "self"
        assert jobs[0].status == "failed"
        assert str(jobs[0].uri) == "local/project/self/job/123456"
        assert set(jobs[0].tables) == {"results", "table/1", "table/2"}
        assert jobs[0].summary == {"accuracy": 0.9, "id": "123456"}

        assert jobs[1].tables == ["results"]
        assert jobs[1].summary == {"accuracy": 0.91, "id": "1256789"}

    def test_get_job_from_standalone(self) -> None:
        self._prepare_standalone()
        job = Job.get("123456")
        assert job.status == "failed"
        assert job.datastore_uuid == "123456" == job.id
        assert str(job.uri) == "local/project/self/job/123456"
        assert set(job.tables) == {"results", "table/1", "table/2"}
        assert job.summary == {"accuracy": 0.9, "id": "123456"}
        assert str(job)
        assert repr(job)

        dict_ret = job.asdict()
        assert json.loads(json.dumps(dict_ret)) == dict_ret
        assert dict_ret["run_info"]["resource_pool"] == ""
        assert dict_ret["input_info"]["model"] == "llama2-13b-chinese"

        rows = list(job.get_table_rows("table/1"))
        assert len(rows) == 1
        assert [{"id": 1, "output": 3}]

        not_exist_rows = list(job.get_table_rows("not-exist"))
        assert not_exist_rows == []

        with self.assertRaisesRegex(NotFoundError, "job not found:"):
            Job.get("not-exist")

    def _prepare_server(self, request_mock: Mocker) -> None:
        base_url = "http://1.1.0.0:8182/api/v1"

        request_mock.request(
            HTTPMethod.POST,
            f"{base_url}/login",
            json={"data": {"name": "foo", "role": {"roleName": "admin"}}},
            headers={"Authorization": "token"},
        )

        job_info = {
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
                    "tags": ["t1", "t2"],
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

        request_mock.request(
            HTTPMethod.GET,
            f"{base_url}/project/1/job/722",
            json={"data": job_info},
        )

        request_mock.request(
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
                    "list": [job_info],
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

        request_mock.request(
            HTTPMethod.POST,
            f"{base_url}/datastore/listTables",
            json={
                "data": {
                    "tables": [
                        "project/1/eval/5c/5c6dc44d410349829a7c6c1916a20651/table/1",
                        "project/1/eval/5c/5c6dc44d410349829a7c6c1916a20651/table/2",
                        "project/1/eval/5c/5c6dc44d410349829a7c6c1916a20651/results",
                    ]
                }
            },
        )

        request_mock.request(
            HTTPMethod.POST,
            f"{base_url}/datastore/scanTable",
            json={
                "data": {
                    "columnHints": {
                        "accuracy": {
                            "columnValueHints": [
                                "25.7700342608841",
                                "29.046544664494025",
                            ],
                            "typeHints": ["FLOAT64"],
                        },
                        "id": {"columnValueHints": [], "typeHints": ["STRING"]},
                    },
                    "lastKey": "5c6dc44d410349829a7c6c1916a20651",
                    "records": [
                        {
                            "accuracy": {
                                "type": "FLOAT64",
                                "value": "4039c520f71f4bf7",
                            },
                            "id": {
                                "type": "STRING",
                                "value": "5c6dc44d410349829a7c6c1916a20651",
                            },
                        }
                    ],
                }
            },
        )

        InstanceTermView().login(
            "http://1.1.0.0:8182",
            alias="remote",
            username="foo",
            password="bar",
        )

    @Mocker()
    def test_get_job_from_server(self, request_mock: Mocker) -> None:
        self._prepare_server(request_mock)
        job = Job.get("http://1.1.0.0:8182/project/1/job/722")
        assert job.id == "722"
        assert job.model.name == "llama2-13b-chinese"
        assert job.model.version == "2pcj3y7hnpqdmqzsl3atcsedupwedp726yrd7bec"
        assert job.model.tags == ["v2", "latest", "t1", "t2"]
        assert job.resource_pool == "A100 80G * 1"
        assert str(job)
        assert repr(job)
        assert job.tables == ["table/1", "table/2", "results"]
        assert job.summary == {
            "accuracy": 25.7700342608841,
            "id": "5c6dc44d410349829a7c6c1916a20651",
        }

        dict_ret = job.asdict()
        assert json.loads(json.dumps(dict_ret)) == dict_ret
        assert (
            dict_ret["input_info"]["model"]
            == "llama2-13b-chinese/version/2pcj3y7hnpqdmqzsl3atcsedupwedp726yrd7bec"
        )

    @Mocker()
    def test_list_jobs_from_server(self, request_mock: Mocker) -> None:
        self._prepare_server(request_mock)

        jobs, pages = Job.list(project="http://1.1.0.0:8182/project/1")
        assert pages["total"] == 55
        assert pages["page"]["page_num"] == 1
        assert len(jobs) == 1
        assert jobs[0].id == "722"
        assert str(jobs[0].uri) == "http://1.1.0.0:8182/project/1/job/722"
        assert jobs[0].handler_name == "src.evaluation:evaluation_results"
        assert jobs[0].status == "success"
        assert jobs[0].datastore_uuid == "5c6dc44d410349829a7c6c1916a20651"
        assert isinstance(jobs[0].created_at, datetime.datetime)
        assert jobs[0].datasets[0].name == "cmmlu"
        assert jobs[0].runtime.name == "test"
