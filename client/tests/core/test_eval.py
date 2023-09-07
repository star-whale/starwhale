import os
import json
from pathlib import Path
from unittest.mock import patch, MagicMock

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR, get_predefined_config_yaml
from starwhale.utils import config as sw_config
from starwhale.consts import HTTPMethod, RECOVER_DIRNAME, DEFAULT_MANIFEST_NAME
from starwhale.utils.config import load_swcli_config, get_swcli_config_path
from starwhale.core.job.view import JobTermView, JobTermViewRich
from starwhale.core.job.model import CloudJob, StandaloneJob
from starwhale.core.job.store import JobStorage
from starwhale.base.models.job import LocalJobInfo
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.client.models.models import JobVo, ResponseMessagePageInfoJobVo

_job_data_dir = f"{ROOT_DIR}/data/job"
_job_manifest = open(f"{_job_data_dir}/job_manifest.yaml").read()
_job_list = ResponseMessagePageInfoJobVo(
    **json.load(open(f"{_job_data_dir}/job_list_resp.json"))
)
_existed_config_contents = get_predefined_config_yaml()


class StandaloneEvaluationJobTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()
        sw_config._config = {}

        _config = load_swcli_config()
        self.job_name = "mjrtonlfmi3gkmzxme4gkzldnz3ws4a"
        self.root = _config["storage"]["root"]
        self.job_dir = os.path.join(
            self.root, "self", ResourceType.job.value, "mj", self.job_name
        )

        self.fs.create_dir(self.job_dir)
        self.fs.create_file(
            os.path.join(self.job_dir, DEFAULT_MANIFEST_NAME), contents=_job_manifest
        )

    def test_store(self):
        uri = Resource(self.job_name[:7], project=Project("self"))
        store = JobStorage(uri)
        assert store.project_dir == Path(self.root) / "self"
        assert store.loc == Path(self.job_dir)
        assert store.id == self.job_name
        assert (
            store.recover_loc
            == (
                Path(self.root)
                / "self"
                / ResourceType.job.value
                / RECOVER_DIRNAME
                / self.job_name[:2]
                / self.job_name
            ).absolute()
        )

        assert store.manifest["version"] == self.job_name
        assert "model_src_dir" in store.manifest

        all_jobs = [job for job in store.iter_all_jobs(uri.project)]
        assert len(all_jobs) == 1
        assert all_jobs[0][0] == (Path(self.job_dir) / DEFAULT_MANIFEST_NAME).absolute()
        assert not all_jobs[0][1]

    def test_list(self):
        jobs, _ = StandaloneJob.list(Project(""))
        assert len(jobs) == 1
        job = jobs[0]
        assert job.manifest.version == self.job_name
        assert (
            job.manifest.model_src_dir
            == "/path/to/.starwhale/self/job/xx/xxyy/snapshot"
        )

    @patch("starwhale.api._impl.data_store.atexit")
    @patch("starwhale.api._impl.wrapper.Evaluation.get")
    @patch("starwhale.api._impl.wrapper.Evaluation.get_summary_metrics")
    def test_info(
        self, m_get_metrics: MagicMock, m_get: MagicMock, m_atexit: MagicMock
    ):
        m_get.return_value = {}
        m_get_metrics.return_value = {"kind": "multi_classification"}

        uri = Resource(
            self.job_name[:5],
            typ=ResourceType.job,
        )
        job = StandaloneJob(uri)
        info = job.info()

        assert isinstance(info, LocalJobInfo)
        assert info.manifest.version == self.job_name
        assert info.report.get("kind") == "multi_classification"

    @patch("starwhale.core.job.view.console.print")
    @patch("starwhale.core.job.view.Table.add_column")
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
        uri = Resource(
            f"local/project/self/{ResourceType.job.value}/{self.job_name[:6]}",
            typ=ResourceType.job,
        )
        job = StandaloneJob(uri)

        ok, _ = job.remove()
        assert ok
        assert not os.path.exists(self.job_dir)
        assert (
            Path(self.root)
            / "self"
            / ResourceType.job.value
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
            / ResourceType.job.value
            / self.job_name[:2]
            / self.job_name
        ).exists()

        job.remove(True)
        assert not os.path.exists(self.job_dir)
        assert not (
            Path(self.root)
            / "self"
            / ResourceType.job.value
            / RECOVER_DIRNAME
            / self.job_name[:2]
            / self.job_name
        ).exists()

    @patch("starwhale.core.job.model.subprocess.check_output")
    @patch("starwhale.core.job.model.check_call")
    def test_actions(self, m_call: MagicMock, m_call_output: MagicMock):
        uri = Resource(
            f"local/project/self/{ResourceType.job.value}/{self.job_name}",
            typ=ResourceType.job,
        )
        job = StandaloneJob(uri)

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
        self.project_uri = f"{self.instance_uri}/projects/self"
        self.job_name = "15"
        self.job_uri = f"{self.project_uri}/jobs/{self.job_name}"

    @Mocker()
    @patch("starwhale.core.job.view.console.print")
    def test_list(self, rm: Mocker, m_console: MagicMock):
        rm.request(
            HTTPMethod.GET,
            f"{self.instance_uri}/api/v1/project/self/job",
            text=_job_list.json(by_alias=True),
        )

        jobs, pager = CloudJob.list(
            project_uri=Project(self.project_uri),
        )

        assert len(jobs) == 1
        assert pager["total"] == 1
        assert pager["remain"] == 0

        job = jobs[0]
        assert isinstance(job, JobVo)
        assert job.id == "44"

        JobTermViewRich.list(
            self.project_uri,
            fullname=True,
        )
        assert m_console.called

    @Mocker()
    @patch("starwhale.core.job.view.console.print")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
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
