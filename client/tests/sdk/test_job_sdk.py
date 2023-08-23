import datetime

import yaml

from tests import BaseTestCase
from starwhale.utils.fs import ensure_file
from starwhale.api._impl import wrapper
from starwhale.utils.error import NotFoundError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.models.job import LocalJobInfo
from starwhale.api._impl.job.model import Job


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
