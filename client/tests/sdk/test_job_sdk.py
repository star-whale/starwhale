import json
import datetime
from unittest.mock import patch, MagicMock

import yaml

from tests import BaseTestCase
from starwhale.utils.fs import ensure_file
from starwhale.api._impl import wrapper
from starwhale.utils.error import NotFoundError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.models.job import LocalJobInfo
from starwhale.base.uri.project import Project
from starwhale.base.client.client import TypeWrapper
from starwhale.api._impl.job.model import Job
from starwhale.base.client.models.models import (
    JobVo,
    UserVo,
    ModelVo,
    JobStatus,
    RuntimeVo,
    ModelVersionVo,
    RuntimeVersionVo,
    ResponseMessagePageInfoJobVo,
)


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
        e = wrapper.Evaluation("123456", Project("self"))
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
        e = wrapper.Evaluation("1256789", Project("self"))
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
        job = jobs[0]
        assert isinstance(job, Job)
        info = job.info()
        assert info.manifest.version == "123456"
        assert info.manifest.project == "self"
        assert info.manifest.status == "failed"

        assert set(jobs[0].tables) == {"results", "table/1", "table/2"}
        assert jobs[0].summary == {"accuracy": 0.9, "id": "123456"}

        assert jobs[1].tables == ["results"]
        assert jobs[1].summary == {"accuracy": 0.91, "id": "1256789"}

    def test_get_job_from_standalone(self) -> None:
        self._prepare_standalone()
        job = Job.get("123456")
        assert set(job.tables) == {"results", "table/1", "table/2"}
        assert job.summary == {"accuracy": 0.9, "id": "123456"}
        assert str(job)
        assert repr(job)

        info = job.info()
        assert isinstance(info, LocalJobInfo)
        assert info.manifest.status == "failed"
        assert info.manifest.version == "123456"
        assert info.manifest.model == "llama2-13b-chinese"

        rows = list(job.get_table_rows("table/1"))
        assert len(rows) == 1
        assert [{"id": 1, "output": 3}]

        not_exist_rows = list(job.get_table_rows("not-exist"))
        assert not_exist_rows == []

        with self.assertRaises(NotFoundError):
            Job.get("not-exist")

    @patch("starwhale.core.job.model.CloudJob.info")
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.base.client.api.job.JobApi.list")
    def test_list_jobs_from_server(
        self, mock_list: MagicMock, load_conf: MagicMock, info: MagicMock
    ) -> None:
        load_conf.return_value = {"instances": {"foo": {"uri": "http://1.1.0.0:8182"}}}

        user = UserVo(id="1", name="foo", created_time=123, is_enabled=True)
        mock_list.return_value = TypeWrapper(
            ResponseMessagePageInfoJobVo,
            {
                "code": "success",
                "message": "",
                "data": {
                    "total": 55,
                    "page_num": 1,
                    "page_size": 10,
                    "size": 10,
                    "list": [
                        json.loads(
                            JobVo(
                                exposed_links=[],
                                id="722",
                                uuid="5c6dc44d410349829a7c6c1916a20651",
                                model_name="",
                                model_version="",
                                model=ModelVo(
                                    id="2",
                                    name="model",
                                    created_time=456,
                                    owner=user,
                                    version=ModelVersionVo(
                                        latest=True,
                                        step_specs=[],
                                        id="7",
                                        name="model",
                                        alias="v2",
                                        created_time=789,
                                        shared=False,
                                    ),
                                ),
                                runtime=RuntimeVo(
                                    id="8",
                                    name="runtime",
                                    created_time=10,
                                    owner=user,
                                    version=RuntimeVersionVo(
                                        latest=True,
                                        id="9",
                                        runtime_id="8",
                                        name="runtime",
                                        alias="v3",
                                        image="image:foo",
                                        created_time=11,
                                        shared=True,
                                    ),
                                ),
                                datasets=["cmmlu"],
                                owner=user,
                                created_time=123,
                                job_status=JobStatus.success,
                                resource_pool="pool",
                            ).json()
                        )
                    ],
                },
            },
        )

        jobs, pages = Job.list(project="http://1.1.0.0:8182/project/1")
        assert pages["total"] == 55
        assert pages["page"]["page_num"] == 1
        assert len(jobs) == 1
        job = jobs[0]
        info = job.basic_info
        assert isinstance(info, JobVo)
        assert info.id == "722"
        assert info.job_status.name == "success"
        assert info.uuid == "5c6dc44d410349829a7c6c1916a20651"
        assert info.created_time == 123
        assert info.datasets is not None
        assert info.datasets[0] == "cmmlu"
        assert info.runtime.name == "runtime"
