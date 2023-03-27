from __future__ import annotations

import io
import sys
import json
import time
import queue
import threading
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from starwhale.api import track
from starwhale.utils import now_str, load_yaml
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.log import StreamWrapper
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.process import check_call
from starwhale.core.dataset.type import Link, Audio, Image
from starwhale.api._impl.data_store import TableDesc, TableWriter
from starwhale.api._impl.track.base import (
    _TrackType,
    TrackRecord,
    _TrackSource,
    ParamsRecord,
    MetricsRecord,
    ArtifactsRecord,
    MetricsTabularRow,
    ArtifactsTabularRow,
)
from starwhale.api._impl.track.hooker import ConsoleLogHook
from starwhale.api._impl.track.handler import HandlerThread
from starwhale.api._impl.track.tracker import Tracker
from starwhale.api._impl.track.collector import CollectorThread

from .. import BaseTestCase


class TestTrackBase(BaseTestCase):
    def test_metrics_record(self) -> None:
        start_time = time.monotonic()
        mr = MetricsRecord(
            data={"loss": 0.99},
            step=1,
            start_time=start_time,
        )

        assert mr.typ == _TrackType.METRICS
        assert mr.relative_time < 100
        assert isinstance(mr.relative_time, float)
        assert mr.source == _TrackSource.USER
        assert isinstance(mr.clock_time, str)

    def test_artifacts_tabular_row(self) -> None:
        atr = ArtifactsTabularRow(
            name="test_image",
            index=1,
            created_at=now_str(),
            data=Link.from_local_artifact(
                data=Image(fp=b"123"), store_dir=Path(self.local_storage)
            ),
            link_wrapper=True,
        )
        ret_dict = atr.asdict()
        assert atr.key == ret_dict["__key"] == "test_image-0001"
        assert ret_dict["__link_wrapper"]

        atr = ArtifactsTabularRow.from_datastore(
            name="test_image",
            index=0,
            created_at=now_str(),
            data=Link(),
            __link_wrapper=False,
        )
        assert isinstance(atr, ArtifactsTabularRow)
        assert atr.key == "test_image-0000"
        assert not atr._link_wrapper

    def test_artifacts_tabular_row_exceptions(self) -> None:
        with self.assertRaisesRegex(TypeError, "is not Link type"):
            ArtifactsTabularRow(
                name="test_image",
                index=1,
                created_at=now_str(),
                data=Image(),  # type: ignore
                link_wrapper=True,
            )

        with self.assertRaisesRegex(ValueError, "must be non negative integer number"):
            ArtifactsTabularRow(
                name="test_image",
                index=-1,
                created_at=now_str(),
                data=Link(),
                link_wrapper=True,
            )

    def test_metrics_tabular_row(self) -> None:
        now = now_str()
        mtd = MetricsTabularRow(
            step=0,
            clock_time=now,
            relative_time=10,
            metrics={"loss": 0.99},
        )
        ret_dict = mtd.asdict()
        assert ret_dict == {
            "loss": 0.99,
            "__step": 0,
            "__clock_time": now,
            "__relative_time": 10.0,
        }

        mtd = MetricsTabularRow.from_record(
            record=MetricsRecord(
                step=1,
                data={"loss": 0.99, "acc": 0.98},
            )
        )
        ret_dict = mtd.asdict()
        assert ret_dict == {
            "loss": 0.99,
            "acc": 0.98,
            "__step": 1,
            "__clock_time": mtd.clock_time,
            "__relative_time": mtd.relative_time,
        }

        mtd = MetricsTabularRow.from_datastore(
            __step=1, __clock_time=now, __relative_time=1.0, loss=0.99, acc=0.98
        )
        assert mtd.step == 1
        assert mtd.clock_time == now
        assert mtd.relative_time == 1.0
        assert mtd.metrics == {"loss": 0.99, "acc": 0.98}
        assert mtd.asdict() == json.loads(json.dumps(mtd.asdict()))

    def test_metrics_tabular_row_exceptions(self) -> None:
        now = now_str()
        with self.assertRaisesRegex(TypeError, "should be non-negative integer number"):
            MetricsTabularRow(
                step=-1,
                clock_time=now,
                relative_time=10,
                metrics={"loss": 0.99},
            )

        with self.assertRaisesRegex(TypeError, "should be str key and float value"):
            MetricsTabularRow(
                step=1,
                clock_time=now,
                relative_time=10,
                metrics={"loss": "test"},  # type: ignore
            )

        with self.assertRaisesRegex(TypeError, "should be str key and float value"):
            MetricsTabularRow(
                step=1,
                clock_time=now,
                relative_time=10,
                metrics={1: 1.1},  # type: ignore
            )


class TestTracker(BaseTestCase):
    def tearDown(self) -> None:
        Tracker._instance = None
        super().tearDown()

    def test_start_with_default(self) -> None:
        code_dir = Path(self.local_storage) / "code"
        ensure_dir(code_dir)
        t = Tracker.start(saved_dir=code_dir)
        assert isinstance(t, Tracker)
        assert t == Tracker._instance
        assert t.name != ""
        assert t.id != ""

        assert t._collector_thread is not None
        assert t._collector_thread.is_alive()

        assert t._syncer_thread is not None

        assert t._handler_thread is not None
        assert t._handler_thread.is_alive()

        assert len(t._threads) == 3
        assert t._log_hook is not None
        assert isinstance(sys.stdout, StreamWrapper)
        assert isinstance(sys.stderr, StreamWrapper)

        assert (t._saved_dir / ".starwhale" / "track" / f"0-{t.id}").exists()
        t.end()

    def test_start_with_custom(self) -> None:
        with Tracker.start(
            saved_dir=self.local_storage,
            name="test_tracker",
            project_uri=URI("test_project", expected_type=URIType.PROJECT),
        ) as t:
            assert t.name == "test_tracker"
            assert t.project_uri.project == "test_project"

        with Tracker.start(
            saved_dir=self.local_storage,
            name="test_tracker",
            project_uri="test_project",
            mode="offline",
        ) as t:
            assert t.project_uri.instance == "local"
            assert t.project_uri.project == "test_project"
            assert t._syncer_thread is None
            assert t._handler_thread is not None
            assert t._handler_thread.sync_queue is None  # type: ignore

    def test_re_start_exception(self) -> None:
        t1 = Tracker.start(saved_dir=self.local_storage)
        assert Tracker._instance == t1

        with self.assertRaisesRegex(RuntimeError, "has already been initialized"):
            Tracker.start(saved_dir=self.local_storage)

        t1.end()

    @patch("os.environ", {})
    def test_start_for_cloud(self) -> None:
        with Tracker.start(
            saved_dir=self.local_storage,
            project_uri="http://1.1.1.1/project/test",
            access_token="abcd",
        ) as t:
            assert t.project_uri.project == "test"

        with self.assertRaisesRegex(
            ValueError, "access_token is required for cloud instance"
        ):
            Tracker.start(
                saved_dir=self.local_storage,
                project_uri="http://0.0.0.0/project/test",
            )

    def test_start_exceptions(self) -> None:
        with self.assertRaisesRegex(TypeError, "type is unexpected, only accepts str"):
            Tracker.start(saved_dir=self.local_storage, project_uri=1)  # type: ignore

    def test_end(self) -> None:
        assert Tracker._instance is None
        Tracker.end()

        t = Tracker.start(saved_dir=self.local_storage)
        assert t._handler_thread and t._handler_thread.is_alive()
        assert t._collector_thread and t._collector_thread.is_alive()
        assert Tracker._instance is not None
        assert isinstance(sys.stdout, StreamWrapper)

        Tracker.end()
        assert t._handler_thread and not t._handler_thread.is_alive()
        assert t._collector_thread and not t._collector_thread.is_alive()
        assert Tracker._instance is None
        assert isinstance(sys.stdout, io.TextIOWrapper)

        assert t._workdir and (t._workdir / "_manifest.yaml").exists()

        Tracker.end()

    def test_render_manifest(self) -> None:
        t = Tracker.start(saved_dir=self.local_storage)
        t.end()
        assert t._workdir
        manifest = load_yaml(t._workdir / "_manifest.yaml")

        assert manifest["auto"] == {
            "console_log_redirect": True,
            "framework_hook": True,
            "run_collect": True,
        }
        assert manifest["collector_interval"] == {"report": 30.0, "sample": 1.0}
        assert manifest["mode"] == "online"
        assert manifest["project_uri"] == "local/project/self"

    def test_log_non_init(self) -> None:
        msg = "Tracker instance has not been"
        assert Tracker._instance is None

        with self.assertRaisesRegex(RuntimeError, msg):
            track.metrics(loss=0.99)

        with self.assertRaisesRegex(RuntimeError, msg):
            track.params(mode="offline")

        with self.assertRaisesRegex(RuntimeError, msg):
            track.artifacts(image=Image())

    def test_metrics(self) -> None:
        code_dir = Path(self.local_storage) / "track"
        t = track.start(saved_dir=code_dir)

        track.metrics()
        track.metrics({})
        track.metrics(loss=0.99, commit=False)
        track.metrics(loss=0.98, acc=0.1)
        track.metrics(loss=0.97, acc=0.2)
        track.metrics({"loss": 0.96, "acc": 0.3})
        track.metrics({"loss": 0.95, "acc": 0.4}, {"gradient": 0.0})
        track.metrics(loss=0.94, acc=0.5, step=10)
        track.metrics({"indict": {"indict": {"loss": 1}}})
        track.end()

        assert isinstance(t._handler_thread, HandlerThread)
        records = list(
            t._handler_thread._data_store.scan_tables([TableDesc("metrics/user")])
        )
        assert len(records) == 6
        assert records[0]["__step"] == 0
        assert records[1]["__step"] == 1
        assert records[4]["__step"] == 10
        assert records[0]["loss"] == 0.98
        assert records[0]["acc"] == 0.1
        assert records[4]["loss"] == 0.94
        assert records[5]["indict/indict/loss"] == 1.0

    def test_metrics_exceptions(self) -> None:
        track.start(saved_dir=self.local_storage)

        with self.assertRaisesRegex(ValueError, "could not convert string to float"):
            track.metrics({"test": "test"})

        track.end()

    def test_params(self) -> None:
        code_dir = Path(self.local_storage) / "track"
        with track.start(saved_dir=code_dir) as t:
            track.params()
            track.params({})
            track.params(p1=0, p2="2", p3=3.0, p4={"k": "v"}, p5=[1, 2, 3])
            track.params(p1=1)
            track.params({"p2": "2.0"})
        assert isinstance(t._handler_thread, HandlerThread)
        user_m_path = t._handler_thread._workdir / "params" / "user.json"
        assert user_m_path.exists()
        user_m_content = json.loads(user_m_path.read_text())
        assert user_m_content == {
            "p1": 1,
            "p2": "2.0",
            "p3": 3.0,
            "p4": {"k": "v"},
            "p5": [1, 2, 3],
        }
        system_m_path = t._handler_thread._workdir / "params" / "_system.json"
        assert system_m_path.exists()

    def test_params_exceptions(self) -> None:
        track.start(saved_dir=self.local_storage)

        with self.assertRaisesRegex(TypeError, "keys should be string type"):
            track.params({1: "test"})  # type: ignore

        with self.assertRaisesRegex(
            TypeError, "Object of type bytes is not JSON serializable"
        ):
            track.params(a=b"test")

        track.end()

    def test_artifacts(self) -> None:
        code_dir = Path(self.local_storage) / "track"
        t = track.start(saved_dir=code_dir)
        t.artifacts()
        t.artifacts({})
        t.artifacts(image=Image(b"123"))
        t.artifacts(image=Image(b"345"), audio=Audio(b"123"))
        t.artifacts(
            category={"image": Image(b"567"), "audio": [Audio(b"234"), Audio(b"345")]}
        )
        t.end()

        assert isinstance(t._handler_thread, HandlerThread)
        records = list(
            t._handler_thread._data_store.scan_tables([TableDesc("artifacts/user")])
        )
        assert len(records) == 6
        assert records[0]["__key"] == "audio-0000"
        assert records[0]["data"].size == 3
        assert records[0]["data"].data_type["type"] == "audio"
        assert records[1]["__key"] == "category/audio/0-0000"
        assert records[2]["__key"] == "category/audio/1-0000"
        assert records[3]["__key"] == "category/image-0000"
        assert records[4]["__key"] == "image-0000"
        assert records[5]["__key"] == "image-0001"
        assert records[4]["index"] == 0
        assert records[5]["index"] == 1

    def test_artifacts_exceptions(self) -> None:
        t = track.start(saved_dir=self.local_storage)
        with self.assertRaisesRegex(NoSupportError, "not support for artifacts"):
            t.artifacts({"a": 1})
        t.end()

    def test_multi_track(self) -> None:
        code_dir = Path(self.local_storage) / "track"
        assert not (code_dir / ".starwhale").exists()

        t1 = track.start(saved_dir=code_dir)
        track.metrics(loss=0.99)
        track.params(model="mnist")
        track.artifacts(image=Image(b"123"))
        track.end()

        t2 = track.start(saved_dir=code_dir)
        track.metrics(loss=0.98)
        track.params(model="nn")
        track.artifacts(image=Image(b"234"))
        track.end()

        assert t1 != t2
        track_dirs = list((code_dir / ".starwhale" / "track").iterdir())
        assert len(track_dirs) == 2
        assert {"0", "1"} == {n.name.split("-")[0] for n in track_dirs}

        ensure_file(code_dir / ".starwhale" / "track" / "tmp", content="test")
        ensure_dir(code_dir / ".starwhale" / "track" / "test")

        track.start(saved_dir=code_dir)
        track.metrics(loss=0.98)
        track.params(model="nn")
        track.artifacts(image=Image(b"234"))
        track.end()


class TestHandler(BaseTestCase):
    def test_handle_metrics(self) -> None:
        workdir = Path(self.local_storage) / "track"
        h = HandlerThread(workdir, queue.Queue())
        h._handle_metrics(
            MetricsRecord(
                data={"loss": 0.98},
                step=1,
            )
        )

        h._handle_metrics(
            MetricsRecord(
                data={"loss": 0.99, "acc": 0.99},
                step=2,
            )
        )

        assert "metrics/user" in h._table_writers
        assert isinstance(h._table_writers["metrics/user"], TableWriter)

        h.flush()
        datastore_file_path = workdir / "metrics" / "user.sw-datastore.json"
        assert datastore_file_path.exists()
        assert datastore_file_path.is_file()

        records = list(h._data_store.scan_tables([TableDesc("metrics/user")]))
        assert len(records) == 2
        assert records[0]["__step"] == 1
        assert records[1]["__step"] == 2
        assert records[0]["loss"] == 0.98
        assert "acc" not in records[0]
        assert records[1]["loss"] == 0.99
        assert records[1]["acc"] == 0.99

        assert records[0]["__relative_time"] > 0
        assert records[0]["__clock_time"] != ""

    def test_handle_params(self) -> None:
        workdir = Path(self.local_storage) / "track"
        h = HandlerThread(workdir, queue.Queue())
        assert len(h._params) == 0
        h._handle_params(ParamsRecord(data={"tag": ["test"], "model": "mnist"}))
        h._handle_params(
            ParamsRecord(data={"version": "0.1"}, source=_TrackSource.SYSTEM)
        )
        assert "user" in h._params
        assert "_system" in h._params

        h.flush()
        user_params_path = workdir / "params" / "user.json"
        system_params_path = workdir / "params" / "_system.json"
        framework_params_path = workdir / "params" / "_framework.json"

        assert user_params_path.exists()
        assert system_params_path.exists()
        assert not framework_params_path.exists()

        user_params = json.loads(user_params_path.read_text())
        system_params = json.loads(system_params_path.read_text())
        assert user_params == {"tag": ["test"], "model": "mnist"}
        assert system_params == {"version": "0.1"}

    def test_handle_artifacts(self) -> None:
        workdir = Path(self.local_storage) / "track"
        h = HandlerThread(workdir, queue.Queue())
        h._handle_artifacts(
            ArtifactsRecord(data={"image": Image(fp=b"123"), "audio": Audio(fp=b"345")})
        )
        h._handle_artifacts(ArtifactsRecord(data={"image": Image(fp=b"000")}))

        assert "artifacts/user" in h._table_writers
        assert isinstance(h._table_writers["artifacts/user"], TableWriter)
        assert h._artifacts_counter["image"] == 2
        assert h._artifacts_counter["audio"] == 1

        h.flush()

        datastore_file_path = workdir / "artifacts" / "user.sw-datastore.json"
        assert datastore_file_path.exists()
        assert datastore_file_path.is_file()

        files_dir = workdir / "artifacts" / "_files"
        assert files_dir.exists()
        records = list(h._data_store.scan_tables([TableDesc("artifacts/user")]))
        assert len(records) == 3
        assert records[0]["__key"] == "audio-0000"
        assert records[1]["__key"] == "image-0000"
        assert records[2]["__key"] == "image-0001"
        assert records[0]["__link_wrapper"]
        assert records[0]["name"] == "audio"
        assert records[0]["index"] == 0

        audio_link = records[0]["data"]
        assert isinstance(audio_link, Link)
        assert audio_link.data_type["type"] == "audio"  # type: ignore
        uri_path = Path(audio_link.uri)
        assert uri_path.exists() and uri_path.is_file()
        assert uri_path.read_bytes() == b"345"

        assert Path(records[1]["data"].uri).read_bytes() == b"123"
        assert Path(records[2]["data"].uri).read_bytes() == b"000"

    def test_run(self) -> None:
        workdir = Path(self.local_storage) / "track"
        handle_queue = queue.Queue()

        handle_queue.put(ParamsRecord(data={"params": 1}))
        handle_queue.put(
            ParamsRecord(data={"cpu_cores": 1}, source=_TrackSource.SYSTEM)
        )
        handle_queue.put(ParamsRecord(data={}, source=_TrackSource.FRAMEWORK))
        handle_queue.put(MetricsRecord(data={"metrics": 0.99}, step=0))
        handle_queue.put(MetricsRecord(data={"metrics": 0.98}, step=1))
        handle_queue.put(
            MetricsRecord(data={"cpu_freq": 0.98}, step=0, source=_TrackSource.SYSTEM)
        )
        handle_queue.put(None)
        handle_queue.put(ArtifactsRecord(data={"image": Image(fp=b"123")}))

        h = HandlerThread(workdir, handle_queue)
        h.start()
        h.close()

        assert handle_queue.qsize() == 0
        assert "metrics/user" in h._table_writers
        assert "metrics/_system" in h._table_writers
        assert "artifacts/user" in h._table_writers

        assert (workdir / "metrics" / "user.sw-datastore.json").exists()
        assert (workdir / "metrics" / "_system.sw-datastore.json").exists()
        assert (workdir / "artifacts" / "_files").exists()
        assert len(list((workdir / "artifacts" / "_files").iterdir())) != 0
        assert (workdir / "params" / "user.json").exists()
        assert (workdir / "params" / "_system.json").exists()
        assert not (workdir / "params" / "_framework.json").exists()

    def test_close(self) -> None:
        workdir = Path(self.local_storage) / "track"
        handle_queue = queue.Queue()

        handle_queue.put(MetricsRecord(data={"metrics": 0.99}, step=0))
        h = HandlerThread(workdir, handle_queue)
        assert not h.is_alive()
        h.start()
        assert h.is_alive()
        h.close()
        assert not h.is_alive()
        assert handle_queue.qsize() == 0

        assert h._table_writers["metrics/user"]._stopped

        h.close()

    @pytest.mark.filterwarnings("ignore::pytest.PytestUnhandledThreadExceptionWarning")
    def test_raise_exception(self) -> None:
        workdir = Path(self.local_storage) / "track"
        handle_queue = queue.Queue()

        handle_queue.put(TrackRecord(typ=_TrackType.METRICS, source=_TrackSource.USER))
        h = HandlerThread(workdir, handle_queue)
        h.start()

        with self.assertRaisesRegex(
            threading.ThreadError, "no support to handle TrackRecord"
        ):
            h.close()


class TestCollector(BaseTestCase):
    def test_init_exceptions(self) -> None:
        workdir = Path(self.local_storage) / "track"

        with self.assertRaisesRegex(NotFoundError, "Collector workdir"):
            CollectorThread(tracker=MagicMock(), workdir=workdir)

        ensure_dir(workdir)
        with self.assertRaisesRegex(ValueError, "sample interval seconds"):
            CollectorThread(tracker=MagicMock(), workdir=workdir, sample_interval=0)

        with self.assertRaisesRegex(ValueError, "sample interval seconds"):
            CollectorThread(
                tracker=MagicMock(),
                workdir=workdir,
                sample_interval=100,
                report_interval=300,
            )

        with self.assertRaisesRegex(ValueError, "should be less than report interval"):
            CollectorThread(
                tracker=MagicMock(),
                workdir=workdir,
                sample_interval=10,
                report_interval=1,
            )

        with self.assertRaisesRegex(ValueError, "report interval seconds"):
            CollectorThread(
                tracker=MagicMock(),
                workdir=workdir,
                sample_interval=1,
                report_interval=500,
            )

    def test_inspect_code_without_git(self) -> None:
        workdir = Path(self.local_storage) / "track"
        ensure_dir(workdir)
        ret = CollectorThread(
            tracker=MagicMock(),
            workdir=workdir,
        )._inspect_code()

        assert ret == {"code": {"git": {}, "workdir": str(workdir)}}

    def test_inspect_code_with_git(self) -> None:
        workdir = Path(self.local_storage) / "track"
        ensure_dir(workdir)
        ensure_file(workdir / "code.file", content="test")
        commands = [
            "git init -b main",
            "git add code.file",
            'git commit -m "test"',
            "git remote add origin http://remote.git",
        ]

        [check_call(cmd, shell=True, cwd=str(workdir)) for cmd in commands]

        ret = CollectorThread(
            tracker=MagicMock(),
            workdir=workdir,
        )._inspect_code()

        code_info = ret["code"]
        git_info = code_info["git"]
        assert code_info["workdir"] == str(workdir)
        assert git_info["commit_sha"] != ""
        assert git_info["message"] == "test\n"
        assert not git_info["is_dirty"]
        assert git_info["branch"] == "main"
        assert git_info["remote_urls"] == ["http://remote.git"]

    def test_inspect_python_run(self) -> None:
        ret = CollectorThread(
            tracker=MagicMock(),
            workdir=Path(self.local_storage),
        )._inspect_python_run()
        assert ret["python"]["version"].startswith("3.")
        assert "python" in ret["python"]["bin_path"]
        assert ret["python"]["env_mode"] in ("conda", "venv", "system")

    def test_inspect_system_specs(self) -> None:
        ret = CollectorThread(
            tracker=MagicMock(),
            workdir=Path(self.local_storage),
        )._inspect_system_specs()
        assert ret["system"]["hostname"] != ""
        assert ret["hardware"]["cpu"]["cores"] > 0
        assert (
            ret["hardware"]["cpu"]["cores_logical"] >= ret["hardware"]["cpu"]["cores"]
        )
        assert ret["hardware"]["memory"]["total_bytes"] > 0
        assert (
            ret["hardware"]["disk"]["total_bytes"]
            > ret["hardware"]["disk"]["used_bytes"]
            > 0
        )

    def test_inspect_process_specs(self) -> None:
        ret = CollectorThread(
            tracker=MagicMock(),
            workdir=Path(self.local_storage),
        )._inspect_process_specs()
        assert ret["process"]["pid"] != 0
        assert ret["process"]["cwd"] != ""
        cmdline = ret["process"]["cmdline"]
        assert isinstance(cmdline, str)
        assert "pytest" in cmdline or "python" in cmdline

    def test_inspect_environments(self) -> None:
        ret = CollectorThread(
            tracker=MagicMock(),
            workdir=Path(self.local_storage),
        )._inspect_environments()
        assert "PATH" in ret["environments"]
        assert "SW_VERSION" in ret["environments"]

    def test_inspect_metrics(self) -> None:
        ret = CollectorThread(
            tracker=MagicMock(),
            workdir=Path(self.local_storage),
        )._inspect_metrics()
        assert ret["system"]["cpu"]["usage_percent"] >= 0
        assert ret["process"]["cpu"]["usage_percent"] >= 0
        assert ret["process"]["num_threads"] >= 1

    def test_run(self) -> None:
        workdir = Path(self.local_storage) / "track"
        ensure_dir(workdir)

        sample_interval = 0.1
        report_interval = 0.5

        mock_tracker = MagicMock()
        c = CollectorThread(
            tracker=mock_tracker,
            workdir=workdir,
            sample_interval=sample_interval,
            report_interval=report_interval,
        )
        c.start()
        time.sleep(1.5)
        c.close()

        assert mock_tracker._log_params.call_count == 5
        log_p_call = mock_tracker._log_params.call_args_list
        assert log_p_call[0][1]["source"] == _TrackSource.SYSTEM
        subjects = {k for args in log_p_call for k in args[0][0]}
        assert subjects == {
            "code",
            "python",
            "system",
            "hardware",
            "process",
            "environments",
        }

        assert mock_tracker._log_metrics.call_count == c._metrics_step
        assert c._metrics_step >= 1

        log_m_call = mock_tracker._log_metrics.call_args_list[0][1]
        assert set(log_m_call["data"].keys()) == {
            "system/cpu/usage_percent/last",
            "system/cpu/usage_percent/avg",
            "process/cpu/usage_percent/last",
            "process/cpu/usage_percent/avg",
            "process/num_threads/last",
            "process/num_threads/avg",
        }

        for v in log_m_call["data"].values():
            assert isinstance(v, float)

        assert log_m_call["step"] == 0
        assert log_m_call["commit"]
        assert log_m_call["source"] == _TrackSource.SYSTEM
        assert len(c._staging_metrics) == 0

        assert (
            c._metrics_inspect_cnt / (sample_interval / report_interval)
            >= c._metrics_step
        )

    def test_close(self) -> None:
        workdir = Path(self.local_storage) / "track"
        ensure_dir(workdir)
        c = CollectorThread(
            tracker=MagicMock(),
            workdir=workdir,
            sample_interval=0.1,
            report_interval=0.2,
        )
        assert c._stopped
        assert not c._stop_event.is_set()
        c.start()
        assert not c._stopped
        assert c.is_alive()
        time.sleep(0.2)
        c.close()

        assert c._stopped
        assert not c.is_alive()
        assert c._stop_event.is_set()

        c.close()

    @pytest.mark.filterwarnings("ignore::pytest.PytestUnhandledThreadExceptionWarning")
    def test_raise_exceptions(self) -> None:
        workdir = Path(self.local_storage) / "track"
        ensure_dir(workdir)

        mock_tracker = MagicMock()
        mock_tracker._log_params.side_effect = RuntimeError("mock log params exception")
        mock_tracker._log_metrics.side_effect = RuntimeError(
            "mock log metrics exception"
        )

        c = CollectorThread(
            tracker=mock_tracker,
            workdir=workdir,
            sample_interval=0.1,
            report_interval=0.1,
            run_exceptions_limits=1,
        )
        c.start()

        cnt = 0
        while not c._run_exceptions and cnt < 10:
            time.sleep(0.5)
            cnt += 1

        with self.assertRaisesRegex(threading.ThreadError, "run raise"):
            c.close()
        assert len(c._run_exceptions) != 0

    def test_report_metrics(self) -> None:
        workdir = Path(self.local_storage) / "track"
        ensure_dir(workdir)

        mock_tracker = MagicMock()

        c = CollectorThread(
            tracker=mock_tracker,
            workdir=workdir,
        )
        c._staging_metrics = {}
        c._report_metrics()
        assert mock_tracker._log_metrics.call_count == 0
        assert c._metrics_step == 0

        c._staging_metrics = {
            "cpu": [1, 2, 3],
            "mem": [1, 2, 3],
            "none": [],
        }
        c._report_metrics()
        assert mock_tracker._log_metrics.call_count == 1
        assert c._metrics_step == 1
        assert mock_tracker._log_metrics.call_args[1]["data"] == {
            "cpu/last": 3.0,
            "cpu/avg": 2.0,
            "mem/last": 3.0,
            "mem/avg": 2.0,
        }


class TestHooker(BaseTestCase):
    def test_install(self) -> None:
        workdir = Path(self.local_storage) / "track"
        h = ConsoleLogHook(log_dir=workdir, track_id="track_id_test")
        _stdout = sys.stdout
        _stderr = sys.stderr
        assert isinstance(_stdout, io.TextIOWrapper)
        assert isinstance(_stderr, io.TextIOWrapper)
        assert not h._stderr_changed
        assert not h._stdout_changed
        h.install()
        assert isinstance(h._log_handler_id, int)
        assert h._logger is not None

        assert isinstance(sys.stdout, StreamWrapper)
        assert isinstance(sys.stderr, StreamWrapper)
        assert sys.stdout != _stdout
        assert sys.stderr != _stderr
        assert h._original_stdout == _stdout
        assert h._original_stderr == _stderr
        assert h._stderr_changed
        assert h._stdout_changed

        print("stdout test", file=sys.stdout)
        print("stderr test", file=sys.stderr)

        logs_path = [f for f in workdir.iterdir() if f.name.startswith("console-")]
        assert len(logs_path) == 1
        log_content = logs_path[0].read_text()
        assert "stdout test" in log_content
        assert "stderr test" in log_content
        assert "track_id_test" in log_content

        h.uninstall()

    def test_uninstall(self) -> None:
        workdir = Path(self.local_storage) / "track"
        h = ConsoleLogHook(log_dir=workdir, track_id="track_id_test")
        h.uninstall()

        h.install()

        assert not isinstance(sys.stderr, io.TextIOWrapper)
        assert not isinstance(sys.stdout, io.TextIOWrapper)

        h.uninstall()
        assert not h._stdout_changed
        assert not h._stderr_changed
        assert h._original_stdout is None
        assert h._original_stderr is None
        assert isinstance(sys.stdout, io.TextIOWrapper)
        assert isinstance(sys.stderr, io.TextIOWrapper)

        h.uninstall()
