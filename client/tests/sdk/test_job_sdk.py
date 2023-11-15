import json
import datetime
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
from requests_mock import Mocker

from tests import BaseTestCase
from starwhale.consts import DefaultYAMLName
from starwhale.utils.fs import ensure_file
from starwhale.utils.error import NotFoundError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.models.job import LocalJobInfo
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.client.client import TypeWrapper
from starwhale.api._impl.job.model import Job
from starwhale.api._impl.evaluation.log import Evaluation
from starwhale.base.client.models.models import (
    JobVo,
    UserVo,
    ModelVo,
    JobStatus,
    RuntimeVo,
    ModelVersionVo,
    RuntimeVersionVo,
    ResponseMessageJobVo,
    ResponseMessageString,
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
        e = Evaluation("123456", Project("self"))
        e.log_summary({"accuracy": 0.9})
        e.log("table/1", id=1, metrics={"output": 3})
        e.log("table/2", id=2, metrics={"output": 4})
        e.log_result(id="1", metrics={"output": 3})
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
        e = Evaluation("1256789", Project("self"))
        e.log_summary({"accuracy": 0.91})
        e.log_result(id="1", metrics={"output": 3})
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
            Job.get("not-exist").basic_info

    @patch("starwhale.base.client.api.job.JobApi.info")
    @patch("starwhale.core.job.model.CloudJob.info")
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.base.client.api.job.JobApi.list")
    def test_list_jobs_from_server(
        self,
        mock_list: MagicMock,
        load_conf: MagicMock,
        info: MagicMock,
        mock_job_info: MagicMock,
    ) -> None:
        load_conf.return_value = {"instances": {"foo": {"uri": "http://1.1.0.0:8182"}}}

        user = UserVo(id="1", name="foo", created_time=123, is_enabled=True)
        job_vo = JobVo(
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
                    draft=False,
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
        )
        mock_job_info.return_value = TypeWrapper(
            ResponseMessageJobVo,
            {
                "code": "success",
                "message": "",
                "data": job_vo,
            },
        )
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
                    "list": [json.loads(job_vo.json())],
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

    @patch("starwhale.core.model.model.StandaloneModel")
    def test_create_local_job(self, m_standalone: MagicMock) -> None:
        model_package_dir = Path(self.local_storage) / "self/model/mnist/.swmp/src"
        ensure_file(
            model_package_dir / DefaultYAMLName.MODEL,
            yaml.safe_dump({"name": "mnist", "run": {"modules": ["src"]}}),
            parents=True,
        )
        m_standalone.return_value = Resource("1122", typ=ResourceType.job)

        job = Job.create(
            project="self",
            model="mnist",
            run_handler="src:Handler",
            datasets=["mnist1", Resource("mnist2", typ=ResourceType.dataset)],
            dataset_head=10,
        )
        assert isinstance(job, Job)
        assert m_standalone.run.call_count == 1
        assert m_standalone.run.call_args[1]["dataset_uris"] == [
            "mnist1",
            "local/project/self/dataset/mnist2/version/latest",
        ]

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_create_remote_job(self, rm: Mocker, mock_load_conf: MagicMock) -> None:
        mock_load_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "foo": {
                    "uri": "https://foo.com",
                    "type": "cloud",
                    "current_project": "starwhale2",
                },
                "local": {
                    "uri": "local",
                    "type": "standalone",
                    "current_project": "self",
                },
            },
            "storage": {"root": self.local_storage},
        }
        rm.get(
            "https://foo.com/api/v1/project/starwhale2",
            json={"data": {"id": 2}},
        )
        rm.get(
            "https://foo.com/api/v1/project/2/model/mnist",
            json={
                "data": {
                    "versionId": "100",
                    "versionInfo": {
                        "latest": True,
                        "tags": ["t1"],
                        "id": "1",
                        "name": "123456a",
                        "alias": "v1",
                        "size": 4898098,
                        "createdTime": 1697042331000,
                        "owner": None,
                        "shared": True,
                        "draft": False,
                        "stepSpecs": [
                            {
                                "name": "mnist.evaluator:MNISTInference.predict",
                                "concurrency": 1,
                                "replicas": 1,
                                "needs": [],
                                "resources": [
                                    {
                                        "type": "memory",
                                        "request": 1.07374182e9,
                                        "limit": 8.5899346e9,
                                    }
                                ],
                                "expose": 0,
                                "job_name": "mnist.evaluator:MNISTInference.evaluate",
                                "show_name": "predict",
                                "require_dataset": True,
                                "ext_cmd_args": "",
                                "parameters_sig": [],
                            },
                            {
                                "name": "mnist.evaluator:MNISTInference.evaluate",
                                "concurrency": 1,
                                "replicas": 1,
                                "needs": ["mnist.evaluator:MNISTInference.predict"],
                                "resources": [
                                    {
                                        "type": "memory",
                                        "request": 1.07374182e9,
                                        "limit": 8.5899346e9,
                                    }
                                ],
                                "expose": 0,
                                "job_name": "mnist.evaluator:MNISTInference.evaluate",
                                "show_name": "evaluate",
                                "require_dataset": False,
                                "ext_cmd_args": "",
                                "parameters_sig": [],
                            },
                        ],
                    },
                    "name": "mnist",
                    "versionName": "123456a",
                    "versionAlias": "v1",
                    "versionTag": "t1",
                    "createdTime": 1697042331000,
                    "shared": 1,
                    "draft": False,
                    "id": "1",
                }
            },
        )
        rm.get(
            "https://foo.com/api/v1/project/2/dataset/mnist",
            json={
                "data": {
                    "versionId": "200",
                    "name": "mnist",
                    "versionName": "223456a",
                }
            },
        )
        mock_create_job = rm.post(
            "https://foo.com/api/v1/project/2/job",
            json=ResponseMessageString(
                code="success", message="success", data="11223344"
            ).dict(),
        )
        project = "https://foo.com/project/starwhale2"

        with self.assertRaisesRegex(
            ValueError, "project must be a server/cloud instance"
        ):
            Job.create_remote(
                project=Project("local/project/self"),
                model="mnist",
                run_handler="src:Handler",
            )

        job = Job.create(
            project=project,
            model=f"{project}/model/mnist",
            run_handler="mnist.evaluator:MNISTInference.evaluate",
            datasets=[f"{project}/dataset/mnist"],
        )
        assert isinstance(job, Job)
        assert job.uri.name == "11223344"
        assert mock_create_job.called
        assert mock_create_job.last_request.json() == {
            "modelVersionUrl": "100",
            "datasetVersionUrls": "200",
            "runtimeVersionUrl": "",
            "resourcePool": "default",
            "handler": "mnist.evaluator:MNISTInference.evaluate",
            "devMode": False,
            "devPassword": "",
        }

        with self.assertRaisesRegex(
            ValueError, "run_handler not-found-handler not found"
        ):
            Job.create(
                project=project,
                model=f"{project}/model/mnist",
                run_handler="not-found-handler",
                overwrite_specs={"not-found-handler": {"replicas": 2}},
            )

        job.create(
            project=project,
            model=f"{project}/model/mnist",
            run_handler="mnist.evaluator:MNISTInference.evaluate",
            overwrite_specs={
                "mnist.evaluator:MNISTInference.evaluate": {
                    "replicas": 2,
                    "resources": {"memory": "1GiB"},
                },
                "mnist.evaluator:MNISTInference.predict": {
                    "replicas": 3,
                    "resources": {"cpu": 2},
                },
            },
        )
        request_json = mock_create_job.last_request.json()
        assert "handler" not in request_json
        spec = yaml.safe_load(request_json["stepSpecOverWrites"])
        assert len(spec) == 2
        assert spec[0]["job_name"] == "mnist.evaluator:MNISTInference.evaluate"
        assert spec[0]["name"] == "mnist.evaluator:MNISTInference.predict"
        assert spec[0]["replicas"] == 3
        assert spec[0]["resources"] == [{"limit": 2.0, "request": 2.0, "type": "cpu"}]

        assert spec[1]["name"] == "mnist.evaluator:MNISTInference.evaluate"
        assert spec[1]["replicas"] == 2
        assert spec[1]["resources"] == [
            {"limit": 1073741824.0, "request": 1073741824.0, "type": "memory"}
        ]
