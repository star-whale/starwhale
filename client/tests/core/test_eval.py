import os
from pathlib import Path
from unittest.mock import patch, MagicMock

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR, get_predefined_config_yaml
from starwhale.utils import config as sw_config
from starwhale.consts import HTTPMethod, RECOVER_DIRNAME, DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.utils.config import load_swcli_config, get_swcli_config_path
from starwhale.core.eval.view import JobTermView, JobTermViewRich
from starwhale.core.eval.model import CloudEvaluationJob, StandaloneEvaluationJob
from starwhale.core.eval.store import EvaluationStorage

_job_data_dir = f"{ROOT_DIR}/data/job"
_job_manifest = open(f"{_job_data_dir}/job_manifest.yaml").read()
_job_list = open(f"{_job_data_dir}/job_list_resp.json").read()
_task_list = open(f"{_job_data_dir}/task_list.json").read()
_existed_config_contents = get_predefined_config_yaml()


class StandaloneEvaluationJobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()
        sw_config._config = {}

        _config = load_swcli_config()
        self.job_name = "mjrtonlfmi3gkmzxme4gkzldnz3ws4a"
        self.root = _config["storage"]["root"]
        self.job_dir = os.path.join(
            self.root, "self", URIType.EVALUATION, "mj", self.job_name
        )

        self.fs.create_dir(self.job_dir)
        self.fs.create_file(
            os.path.join(self.job_dir, DEFAULT_MANIFEST_NAME), contents=_job_manifest
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

        all_jobs = [job for job in store.iter_all_jobs(uri)]
        assert len(all_jobs) == 1
        assert all_jobs[0][0] == (Path(self.job_dir) / DEFAULT_MANIFEST_NAME).absolute()
        assert not all_jobs[0][1]

    def test_list(self):
        uri = URI("")
        jobs, _ = StandaloneEvaluationJob.list(uri)
        assert len(jobs) == 1
        assert jobs[0]["location"] == os.path.join(self.job_dir, DEFAULT_MANIFEST_NAME)
        assert jobs[0]["manifest"]["version"] == self.job_name
        assert jobs[0]["manifest"]["model"] == "mnist:meydczbrmi2g"

    @patch("starwhale.api._impl.data_store.atexit")
    @patch("starwhale.api._impl.wrapper.Evaluation.get")
    @patch("starwhale.api._impl.wrapper.Evaluation.get_metrics")
    def test_info(
        self, m_get_metrics: MagicMock, m_get: MagicMock, m_atexit: MagicMock
    ):
        m_get.return_value = {}
        m_get_metrics.return_value = {"kind": "multi_classification"}

        uri = URI(self.job_name[:5], expected_type=URIType.EVALUATION)
        job = StandaloneEvaluationJob(uri)
        info = job.info()

        assert info["manifest"]["version"] == self.job_name
        assert info["manifest"]["model"] == "mnist:meydczbrmi2g"
        assert info["report"]["kind"] == "multi_classification"

    @patch("starwhale.core.eval.view.console.print")
    @patch("starwhale.core.eval.view.Table.add_column")
    def test_render_with_limit(self, m_table_add_col: MagicMock, m_console: MagicMock):
        report = {
            "kind": "multi_classification",
            "labels": {
                "1": {"id": "1", "Precision": 1.00, "Recall": 1.00, "F1-score": 1.00},
                "2": {"id": "2", "Precision": 1.00, "Recall": 1.00, "F1-score": 1.00},
                "3": {"id": "3", "Precision": 1.00, "Recall": 1.00, "F1-score": 1.00},
            },
        }

        JobTermView(self.job_name)._render_multi_classification_job_report(
            report=report, max_report_cols=2
        )

        # 1(common col:"Label") + 2(max count) + 1(ignore col:"...")
        assert m_table_add_col.call_count == 4

    def test_remove(self):
        uri = URI(f"local/project/self/{URIType.EVALUATION}/{self.job_name[:6]}")
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

        job.remove(True)
        assert not os.path.exists(self.job_dir)
        assert not (
            Path(self.root)
            / "self"
            / URIType.EVALUATION
            / RECOVER_DIRNAME
            / self.job_name[:2]
            / self.job_name
        ).exists()

    @patch("starwhale.core.eval.model.subprocess.check_output")
    @patch("starwhale.core.eval.model.check_call")
    def test_actions(self, m_call: MagicMock, m_call_output: MagicMock):
        uri = URI(f"local/project/self/{URIType.EVALUATION}/{self.job_name}")
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


class CloudJobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()
        sw_config._config = {}
        path = get_swcli_config_path()
        self.fs.create_file(path, contents=_existed_config_contents)

        self.instance_uri = "http://1.1.1.1:8182"
        self.project_uri = f"{self.instance_uri}/project/self"
        self.job_name = "15"
        self.job_uri = f"{self.project_uri}/{URIType.EVALUATION}/{self.job_name}"

    @Mocker()
    @patch("starwhale.core.eval.view.console.print")
    def test_create(self, rm: Mocker, m_console: MagicMock):
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
            step_spec="",
        )

        assert ok
        assert reason == "11"

        JobTermView.run(
            self.project_uri,
            model_uri="1",
            dataset_uris=["1", "2"],
            runtime_uri="2",
            step_spec="",
        )
        assert m_console.call_count == 2
        assert "project/self/evaluation/11" in m_console.call_args[0][0]

    @Mocker()
    @patch("starwhale.core.eval.view.console.print")
    def test_list(self, rm: Mocker, m_console: MagicMock):
        rm.request(
            HTTPMethod.GET,
            f"{self.instance_uri}/api/v1/project/self/job",
            text=_job_list,
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
    @patch("starwhale.api._impl.wrapper.Evaluation.get")
    @patch("starwhale.api._impl.wrapper.Evaluation.get_metrics")
    @patch("starwhale.core.eval.view.console.print")
    def test_info(
        self,
        rm: Mocker,
        m_console: MagicMock,
        m_get_metrics: MagicMock,
        m_get: MagicMock,
    ):
        m_get.return_value = {}
        m_get_metrics.return_value = {
            "kind": "multi_classification",
            "accuracy": 0.9893989398939894,
        }
        rm.request(
            HTTPMethod.GET,
            f"{self.instance_uri}/api/v1/project/self/job/{self.job_name}",
            text=_job_manifest,
        )
        rm.request(
            HTTPMethod.GET,
            f"{self.instance_uri}/api/v1/project/self/job/{self.job_name}/task",
            text=_task_list,
        )

        info = CloudEvaluationJob(URI(self.job_uri)).info()
        print(f"info oo :{info}")
        assert len(info["tasks"][0]) == 3
        assert info["tasks"][0][0]["taskStatus"] == "SUCCESS"
        assert info["tasks"][0][0]["id"] == "40"
        assert "created_at" in info["tasks"][0][0]

        assert info["report"]["kind"] == "multi_classification"
        assert info["report"]["summary"]["accuracy"] == 0.9893989398939894

        JobTermView(self.job_uri).info()

    @Mocker()
    @patch("starwhale.core.eval.view.console.print")
    def test_actions(self, rm: Mocker, m_console: MagicMock):
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

    def test_utils(self):
        assert (1, 1) == CloudEvaluationJob.parse_device("cpu:1")
        assert (2, 1) == CloudEvaluationJob.parse_device("gpu:1")
        assert (1, 10) == CloudEvaluationJob.parse_device("xxx:10")
        assert (1, 1) == CloudEvaluationJob.parse_device("cpu")
        assert (2, 1) == CloudEvaluationJob.parse_device("gpu")
