import os
import unittest
from pathlib import Path
from unittest import skip
from unittest.mock import patch, MagicMock

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.consts import HTTPMethod, RECOVER_DIRNAME, DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.utils.config import load_swcli_config
from starwhale.core.eval.view import JobTermView, JobTermViewRich
from starwhale.core.eval.model import CloudEvaluationJob, StandaloneEvaluationJob
from starwhale.core.eval.store import EvaluationStorage

from .. import ROOT_DIR

_job_data_dir = f"{ROOT_DIR}/data/job"
_job_manifest = open(f"{_job_data_dir}/job_manifest.yaml").read()
_cmp_report = open(f"{_job_data_dir}/cmp_report.jsonl").read()


class StandaloneEvaluationJobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()
        sw_config._config = {}

        _config = load_swcli_config()
        self.job_name = "mu2tgojqga3daobvmzstmytcof5goza"
        self.root = _config["storage"]["root"]
        self.job_dir = os.path.join(self.root, "self", "job", "mu", self.job_name)

        self.fs.create_dir(self.job_dir)
        self.fs.create_file(
            os.path.join(self.job_dir, "_manifest.yaml"), contents=_job_manifest
        )

        self.fs.create_file(
            os.path.join(self.job_dir, "cmp", "result", "current"), contents=_cmp_report
        )

    def test_store(self):
        uri = URI(self.job_name[:7], expected_type=URIType.EVALUATION)
        store = EvaluationStorage(uri)

        assert store.project_dir == Path(self.root) / "self"
        assert store.loc == Path(self.job_dir)
        assert store.id == self.job_name
        assert (
            store.recover_loc
            == (
                Path(self.root)
                / "self"
                / URIType.EVALUATION
                / RECOVER_DIRNAME
                / self.job_name[:2]
                / self.job_name
            ).absolute()
        )

        assert store.manifest["version"] == self.job_name
        assert "model" in store.manifest
        # assert (
        #     store.eval_report_path
        #     == (Path(self.job_dir) / "cmp" / "result" / "current").absolute()
        # )

        all_jobs = [job for job in store.iter_all_jobs(uri)]
        assert len(all_jobs) == 1
        assert all_jobs[0][0] == (Path(self.job_dir) / DEFAULT_MANIFEST_NAME).absolute()
        assert not all_jobs[0][1]

    def test_standalone_list(self):
        uri = URI("")
        jobs, _ = StandaloneEvaluationJob.list(uri)
        assert len(jobs) == 1
        assert jobs[0]["location"] == os.path.join(self.job_dir, DEFAULT_MANIFEST_NAME)
        assert jobs[0]["manifest"]["version"] == self.job_name
        assert jobs[0]["manifest"]["model"] == "mnist:meydczbrmi2g"

    def test_standalone_info(self):
        uri = URI(self.job_name[:5], expected_type=URIType.EVALUATION)
        job = StandaloneEvaluationJob(uri)
        info = job.info()

        assert info["manifest"]["version"] == self.job_name
        assert info["manifest"]["model"] == "mnist:meydczbrmi2g"
        assert info["report"]["kind"] == "multi_classification"
        assert "ppl" in info["location"]

    def test_stanalone_remove(self):
        uri = URI(f"local/project/self/job/{self.job_name[:6]}")
        job = StandaloneEvaluationJob(uri)

        ok, _ = job.remove()
        assert ok
        assert not os.path.exists(self.job_dir)
        assert (
            Path(self.root)
            / "self"
            / URIType.EVALUATION
            / RECOVER_DIRNAME
            / self.job_name[:2]
            / self.job_name
        ).exists()

        ok, _ = job.recover()
        assert ok
        assert os.path.exists(self.job_dir)
        assert not (
            Path(self.root)
            / "self"
            / RECOVER_DIRNAME
            / URIType.EVALUATION
            / self.job_name[:2]
            / self.job_name
        ).exists()

    @patch("starwhale.core.job.model.check_call")
    def test_stanalone_actions(self, m_call: MagicMock):
        uri = URI(f"local/project/self/job/{self.job_name}")
        job = StandaloneEvaluationJob(uri)

        ok, _ = job.cancel()
        assert ok
        assert m_call.call_args[0][0][1] == "rm"

        ok, _ = job.resume()
        assert ok
        assert m_call.call_args[0][0][1] == "unpause"

        ok, _ = job.pause()
        assert ok
        assert m_call.call_args[0][0][1] == "pause"

        assert m_call.call_count == 3


class CloudJobTestCase(unittest.TestCase):
    def setUp(self):
        self.instance_uri = "http://1.1.1.1:8888"
        self.project_uri = f"{self.instance_uri}/project/self"
        self.job_name = "15"
        self.job_uri = f"{self.project_uri}/job/{self.job_name}"

    @Mocker()
    @patch("starwhale.core.job.view.console.print")
    def test_cloud_create(self, rm: Mocker, m_console: MagicMock):
        rm.request(
            HTTPMethod.POST,
            f"{self.instance_uri}/api/v1/project/self/job",
            json={"code": 1, "message": "ok", "data": "11"},
        )

        ok, reason = CloudEvaluationJob.run(
            project_uri=URI(self.project_uri),
            model_uri="1",
            dataset_uris=["1", "2"],
            runtime_uri="2",
            resource="gpu:1",
        )

        assert ok
        assert reason == "11"

        JobTermView.run(
            self.project_uri,
            model_uri="1",
            dataset_uris=["1", "2"],
            runtime_uri="2",
            resource="gpu:1",
        )
        assert m_console.call_count == 2
        assert "project/self/job/11" in m_console.call_args[0][0]

    @Mocker()
    @patch("starwhale.core.job.view.console.print")
    def test_cloud_list(self, rm: Mocker, m_console: MagicMock):
        rm.request(
            HTTPMethod.GET,
            f"{self.instance_uri}/api/v1/project/self/job",
            text=open(f"{_job_data_dir}/job_list_resp.json").read(),
        )

        jobs, pager = CloudEvaluationJob.list(
            project_uri=URI(self.project_uri),
        )

        assert len(jobs) == 10
        assert pager["total"] == 15
        assert pager["remain"] == 5

        _manifest = jobs[0]["manifest"]
        assert "owner" not in _manifest
        assert _manifest["modelVersion"] == "my2dgzbumfsgcnrtmftdgyjzgf4wizi"
        assert _manifest["id"] == "15"
        assert "created_at" in _manifest
        assert "finished_at" in _manifest

        JobTermViewRich.list(
            self.project_uri,
            fullname=True,
        )
        assert m_console.call_count == 1

    @Mocker()
    @patch("starwhale.core.job.view.console.print")
    def test_cloud_info(self, rm: Mocker, m_console: MagicMock):
        rm.request(
            HTTPMethod.GET,
            f"{self.instance_uri}/api/v1/project/self/job/{self.job_name}/result",
            text=open(f"{_job_data_dir}/report.json").read(),
        )
        rm.request(
            HTTPMethod.GET,
            f"{self.instance_uri}/api/v1/project/self/job/{self.job_name}/task",
            text=open(f"{_job_data_dir}/task_list.json").read(),
        )

        info = CloudEvaluationJob(URI(self.job_uri)).info()
        assert len(info["tasks"][0]) == 3
        assert info["tasks"][0][0]["taskStatus"] == "SUCCESS"
        assert info["tasks"][0][0]["id"] == "40"
        assert "created_at" in info["tasks"][0][0]

        assert info["report"]["kind"] == "multi_classification"
        assert info["report"]["summary"]["accuracy"] == 0.9894

        JobTermView(self.job_uri).info()

    @Mocker()
    @patch("starwhale.core.job.view.console.print")
    def test_cloud_actions(self, rm: Mocker, m_console: MagicMock):
        rm.request(
            HTTPMethod.POST,
            f"{self.instance_uri}/api/v1/project/self/job/{self.job_name}/resume",
            json={"code": 1, "message": "ok", "data": "resume"},
        )
        rm.request(
            HTTPMethod.POST,
            f"{self.instance_uri}/api/v1/project/self/job/{self.job_name}/pause",
            json={"code": 1, "message": "ok", "data": "pause"},
        )
        rm.request(
            HTTPMethod.POST,
            f"{self.instance_uri}/api/v1/project/self/job/{self.job_name}/cancel",
            json={"code": 1, "message": "ok", "data": "cancel"},
        )

        JobTermView(self.job_uri).pause()
        JobTermView(self.job_uri).cancel()
        JobTermView(self.job_uri).resume()

    def test_cloud_utils(self):
        assert (1, 1) == CloudEvaluationJob.parse_device("cpu:1")
        assert (2, 1) == CloudEvaluationJob.parse_device("gpu:1")
        assert (1, 10) == CloudEvaluationJob.parse_device("xxx:10")
        assert (1, 1) == CloudEvaluationJob.parse_device("cpu")
        assert (2, 1) == CloudEvaluationJob.parse_device("gpu")
