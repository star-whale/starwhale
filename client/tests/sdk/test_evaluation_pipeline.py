from __future__ import annotations

import os
import sys
import time
import errno
import shutil
import typing as t
import tempfile
from http import HTTPStatus
from pathlib import Path
from contextlib import contextmanager
from unittest.mock import patch, MagicMock

import dill
import requests_mock
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.utils import gen_uniq_version
from starwhale.consts import HTTPMethod, ENV_POD_NAME, DEFAULT_PROJECT
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import RunSubDirType
from starwhale.utils.error import ParameterError
from starwhale.utils.retry import http_retry
from starwhale.base.context import Context, pass_context
from starwhale.base.data_type import Link, GrayscaleImage
from starwhale.core.job.store import JobStorage
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.model import DatasetSummary
from starwhale.core.dataset.store import ObjectStore, DatasetStorage
from starwhale.api._impl.evaluation import PipelineHandler
from starwhale.core.dataset.tabular import TabularDatasetRow, TabularDatasetInfo
from starwhale.api._impl.dataset.loader import DataRow, DataLoader, get_data_loader


class BatchDataHandler(PipelineHandler):
    def __init__(self) -> None:
        super().__init__(predict_batch_size=2)

    def predict(self, data: t.List[t.Dict]) -> t.Dict:
        assert isinstance(data, list)
        assert isinstance(data[0]["image"], GrayscaleImage)
        return {"result": "ok"}


class ExceptionHandler(PipelineHandler):
    def predict(self, data: t.Dict, external: t.Dict) -> t.Any:
        raise Exception("predict test exception")

    def evaluate(self, result: t.Iterator) -> None:
        raise Exception("evaluate test exception")


class NoLogHandler(PipelineHandler):
    def __init__(self) -> None:
        super().__init__(predict_auto_log=False)

    def predict(self, data: t.Dict) -> t.Dict:
        assert isinstance(data["image"], GrayscaleImage)
        return {"result": "ok"}

    def evaluate(self):
        ...


class PlainHandler(PipelineHandler):
    def __init__(self) -> None:
        super().__init__(
            predict_log_mode="plain",
            predict_log_dataset_features=["image", "label"],
        )

    def predict(self, data: t.Dict) -> t.Dict:
        assert isinstance(data["image"], GrayscaleImage)
        assert isinstance(data["label"], int)
        assert isinstance(data.annotation, str)  # type: ignore

        return {"result": "ok"}

    def evaluate(self, result: t.Iterator) -> None:
        for r in result:
            assert isinstance(r["input"]["image"], GrayscaleImage)
            assert isinstance(r["input"]["label"], int)
            assert "annotation" not in r["input"]

            assert "input/image" not in r
            assert r["output/result"] == "ok"


class SimpleHandler(PipelineHandler):
    def ppl(self, data: t.Dict) -> t.Any:
        assert isinstance(data, dict)
        return [1, 2], 0.1

    def cmp(self, _data_loader: t.Any) -> t.Any:
        for _data in _data_loader:
            assert _data["input"]["label"] == 1
            assert _data["output"] == ([1, 2], 0.1)
        return {
            "summary": {"a": 1},
            "kind": "test",
            "labels": {
                "1": {
                    "support": "980",
                    "f1-score": "0.9903699949315762",
                    "precision": "0.9838872104733132",
                    "recall": "0.996938775510204",
                },
                "2": {
                    "support": "1032",
                    "f1-score": "0.98747591522158",
                    "precision": "0.9818007662835249",
                    "recall": "0.9932170542635659",
                },
            },
            "confusion_matrix": {"binarylabel": [[0, 1, 0], [1, 0, 1]]},
            "roc_auc": {
                "1": {
                    "tpr": [1.1, 1.2],
                    "fpr": [1.1, 1.2],
                    "thresholds": [1.1, 1.2],
                    "auc": 1.01,
                },
                "2": {
                    "tpr": [1.1, 1.2],
                    "fpr": [1.1, 1.2],
                    "thresholds": [1.1, 1.2],
                    "auc": 1.02,
                },
            },
        }


@patch("atexit.register", MagicMock())
class TestModelPipelineHandler(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.root = "/home/starwhale/model_test"

        self.local_project = Project(DEFAULT_PROJECT)
        self.eval_id = "mm3wky3dgbqt"

        self.dataset_version = gen_uniq_version()
        self.dataset_uri_raw = f"mnist/version/{self.dataset_version}"
        self.swds_dir = os.path.join(ROOT_DIR, "data", "dataset", "swds")
        self.fs.add_real_directory(self.swds_dir)

    def tearDown(self) -> None:
        super().tearDown()
        os.environ.pop("SW_S3_BUCKET", "")

    @patch("starwhale.core.dataset.store.boto3")
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    def test_s3_loader(self, m_summary: MagicMock, m_resource: MagicMock) -> None:
        os.environ["SW_S3_BUCKET"] = "starwhale"
        m_summary.return_value = DatasetSummary()
        ObjectStore._stores = {}

        _loader = get_data_loader(
            dataset_uri=Resource(
                "mnist/version/latest",
                typ=ResourceType.dataset,
            ),
        )
        assert isinstance(_loader, DataLoader)
        assert not ObjectStore._stores

    @patch("starwhale.api._impl.wrapper.Evaluation.get_results")
    @patch("starwhale.api._impl.wrapper.Evaluation.log_summary_metrics")
    @patch("starwhale.api._impl.wrapper.Evaluation.log")
    def test_cmp_with_plain_log_mode(
        self,
        m_eval_log: MagicMock,
        m_eval_log_metrics: MagicMock,
        m_eval_get: MagicMock,
    ) -> None:
        m_eval_get.return_value = [
            {
                "_mode": "plain",
                "id": "mnist-1",
                "output/result": "ok",
                "input/image": GrayscaleImage(),
                "input/label": 1,
            }
        ]

        context = Context(
            workdir=Path(),
            run_project=self.local_project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="cmp",
            index=0,
        )
        Context.set_runtime_context(context)
        with PlainHandler() as _handler:
            _handler._starwhale_internal_run_evaluate()

    @patch("starwhale.api._impl.wrapper.Evaluation.get_results")
    @patch("starwhale.api._impl.wrapper.Evaluation.log_summary_metrics")
    @patch("starwhale.api._impl.wrapper.Evaluation.log")
    def test_cmp(
        self,
        m_eval_log: MagicMock,
        m_eval_log_metrics: MagicMock,
        m_eval_get: MagicMock,
    ) -> None:
        _logdir = JobStorage.local_run_dir(self.local_project.id, self.eval_id)
        _run_dir = _logdir / RunSubDirType.RUNLOG / "cmp" / "0"
        _status_dir = _run_dir / RunSubDirType.STATUS

        # mock datastore return results
        m_eval_get.return_value = [
            {
                "output": dill.dumps(([1, 2], 0.1)),
                "id": "0",
                "input/label": 1,
            }
        ]

        context = Context(
            workdir=Path(),
            run_project=self.local_project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="cmp",
            index=0,
        )
        Context.set_runtime_context(context)
        with SimpleHandler() as _handler:
            _handler._starwhale_internal_run_evaluate()

        status_file_path = os.path.join(_status_dir, "current")
        assert os.path.exists(status_file_path)
        assert "success" in open(status_file_path).read()

        with self.assertRaisesRegex(Exception, "evaluate test exception"):
            with ExceptionHandler() as handler:
                handler._starwhale_internal_run_evaluate()

        with NoLogHandler() as handler:
            handler._starwhale_internal_run_evaluate()

    @contextmanager
    def _mock_ppl_prepare_data_in_cloud(self) -> t.Any:
        with patch(
            "starwhale.base.uri.resource.Resource._refine_local_rc_info"
        ) as _, patch(
            "starwhale.core.dataset.model.CloudDataset.summary"
        ) as m_summary, patch(
            "starwhale.api._impl.dataset.Dataset.batch_iter"
        ) as m_ds, patch(
            "starwhale.api._impl.dataset.Dataset.info"
        ) as m_ds_info, patch(
            "starwhale.api._impl.evaluation.log.Evaluation.log_result",
        ) as m_log_result, patch(
            "starwhale.utils.config.load_swcli_config"
        ) as m_config, patch.dict(
            os.environ, {ENV_POD_NAME: "test-pod-1"}
        ), requests_mock.Mocker() as rm:
            m_config.return_value = {
                "current_instance": "cloud",
                "instances": {
                    "cloud": {"uri": "https://localhost:80", "sw_token": "bar"},
                    "local": {"uri": "local"},
                },
                "storage": {"root": tempfile.gettempdir()},
            }

            _logdir = JobStorage.local_run_dir(self.local_project.id, self.eval_id)
            _run_dir = _logdir / RunSubDirType.RUNLOG / "ppl" / "0"
            _status_dir = _run_dir / RunSubDirType.STATUS

            m_summary.return_value = DatasetSummary(
                rows=1,
            )

            m_ds.return_value = [
                [
                    DataRow(
                        0,
                        {
                            "image": GrayscaleImage(link=Link("")),
                            "label": 0,
                            "annotation": "a",
                        },
                    )
                ]
            ]
            m_ds_info.return_value = TabularDatasetInfo(mapping={"id": 0, "value": 1})

            project_id = 1
            rm.request(
                HTTPMethod.GET,
                "https://localhost:80/api/v1/project/starwhale",
                json={"data": {"id": project_id, "name": "starwhale"}},
            )

            rm.request(
                HTTPMethod.HEAD,
                f"https://localhost:80/api/v1/project/{project_id}/dataset/mnist/version/{self.dataset_version}",
                json={"message": "found"},
                status_code=HTTPStatus.OK,
            )
            rm.get(
                f"https://localhost:80/api/v1/project/{project_id}/dataset/mnist",
                json={
                    "data": {
                        "id": 11,
                        "versionId": 22,
                        "name": "mnist",
                        "versionName": self.dataset_version,
                    }
                },
            )
            context = Context(
                workdir=Path(),
                run_project=self.local_project,
                log_project=Project("https://localhost:80/project/starwhale"),
                version=self.eval_id,
                dataset_uris=[
                    f"https://localhost:80/project/starwhale/dataset/{self.dataset_uri_raw}"
                ],
                step="ppl",
                index=0,
            )
            Context.set_runtime_context(context)

            yield _status_dir, m_log_result

    @contextmanager
    def _mock_ppl_prepare_data(self, dataset_head: int = 0) -> t.Any:
        with patch(
            "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        ) as _, patch(
            "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        ) as _, patch(
            "starwhale.core.dataset.model.StandaloneDataset.summary"
        ) as m_summary, patch(
            "starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id"
        ) as m_scan_id, patch(
            "starwhale.api._impl.dataset.Dataset.batch_iter"
        ) as m_ds, patch(
            "starwhale.api._impl.dataset.Dataset.info"
        ) as m_ds_info, patch(
            "starwhale.api._impl.evaluation.log.Evaluation.log_result",
        ) as m_log_result:
            _logdir = JobStorage.local_run_dir(self.local_project.id, self.eval_id)
            _run_dir = _logdir / RunSubDirType.RUNLOG / "ppl" / "0"
            _status_dir = _run_dir / RunSubDirType.STATUS

            rows = 100
            m_summary.return_value = DatasetSummary(
                rows=rows,
            )

            fname = "data_ubyte_0.swds_bin"
            m_scan_id.return_value = [{"id": i} for i in range(0, rows)]
            m_ds.return_value = [
                [
                    DataRow(
                        i,
                        {
                            "image": GrayscaleImage(link=Link(fname)),
                            "label": i,
                            "annotation": "a",
                        },
                    )
                ]
                for i in range(0, rows)
            ]
            m_ds_info.return_value = TabularDatasetInfo(mapping={"id": 0, "value": 1})

            datastore_dir = DatasetStorage(
                Resource(
                    self.dataset_uri_raw,
                    typ=ResourceType.dataset,
                )
            )
            data_dir = datastore_dir.data_dir
            ensure_dir(data_dir)
            shutil.copyfile(os.path.join(self.swds_dir, fname), str(data_dir / fname))
            ensure_dir(datastore_dir.loc)
            ensure_file(datastore_dir.manifest_path, "")

            context = Context(
                workdir=Path(),
                run_project=self.local_project,
                version=self.eval_id,
                dataset_uris=[self.dataset_uri_raw],
                step="ppl",
                index=0,
                dataset_head=dataset_head,
            )
            Context.set_runtime_context(context)

            yield _status_dir, m_log_result

    def test_ppl_with_dataset_head(self) -> None:
        with self._mock_ppl_prepare_data(dataset_head=10) as (status_dir, m_log_result):
            with SimpleHandler() as _handler:
                _handler._starwhale_internal_run_predict()

            assert m_log_result.call_count == 10

    def test_ppl_with_batch_input(self) -> None:
        with self._mock_ppl_prepare_data() as (status_dir, m_log_result):
            with BatchDataHandler() as _handler:
                _handler._starwhale_internal_run_predict()

    def test_ppl_with_no_predict_log(self) -> None:
        with self._mock_ppl_prepare_data() as (status_dir, m_log_result):
            with NoLogHandler() as _handler:
                _handler._starwhale_internal_run_predict()

            m_log_result.assert_not_called()

    def test_ppl_with_exception(self) -> None:
        with self._mock_ppl_prepare_data() as (status_dir, m_log_result):
            with self.assertRaisesRegex(Exception, "predict test exception"):
                with ExceptionHandler() as _handler:
                    _handler._starwhale_internal_run_predict()

            status_file_path = os.path.join(status_dir, "current")
            assert os.path.exists(status_file_path)
            assert "failed" in open(status_file_path).read()

    def test_ppl_with_plain_mode(self) -> None:
        with self._mock_ppl_prepare_data() as (status_dir, m_log_result):
            with PlainHandler() as _handler:
                _handler._starwhale_internal_run_predict()

            log_result = m_log_result.call_args[1]
            assert log_result["id"].startswith("self/mnist_")
            assert log_result["metrics"]["_mode"] == "plain"
            assert log_result["metrics"]["_index"] == 99
            assert log_result["metrics"]["output"] == {"result": "ok"}
            assert isinstance(log_result["metrics"]["input/image"], GrayscaleImage)
            assert log_result["metrics"]["input/label"] == 99
            assert "input/annotation" not in log_result
            assert log_result["metrics"]["duration_seconds"] >= 0

            status_file_path = os.path.join(status_dir, "current")
            assert os.path.exists(status_file_path)
            assert "success" in open(status_file_path).read()

    def test_ppl_with_plain_mode_in_cloud(self) -> None:
        with self._mock_ppl_prepare_data_in_cloud() as (status_dir, m_log_result):
            with PlainHandler() as _handler:
                _handler._starwhale_internal_run_predict()

            log_result = m_log_result.call_args[1]
            assert log_result["id"].startswith("11_")
            assert log_result["metrics"]["_mode"] == "plain"
            assert log_result["metrics"]["_index"] == 0
            assert log_result["metrics"]["output"] == {"result": "ok"}
            assert isinstance(log_result["metrics"]["input/image"], GrayscaleImage)
            assert log_result["metrics"]["input/label"] == 0
            assert "input/annotation" not in log_result["metrics"]
            assert log_result["metrics"]["duration_seconds"] >= 0

    def test_ppl(self) -> None:
        with self._mock_ppl_prepare_data() as (status_dir, m_log_result):
            with SimpleHandler() as _handler:
                _handler._starwhale_internal_run_predict()

            assert m_log_result.call_count == 100
            status_file_path = os.path.join(status_dir, "current")
            assert os.path.exists(status_file_path)
            assert "success" in open(status_file_path).read()

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan")
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    def test_deserializer(
        self, m_summary: MagicMock, m_scan: MagicMock, m_scan_id: MagicMock
    ) -> None:
        # make torch happy
        for i in sys.path:
            if not i:
                continue
            try:
                self.fs.add_real_directory(i)
            except OSError as e:
                if e.errno not in [errno.EEXIST, errno.ENOENT]:
                    raise e
        import numpy as np
        import torch

        builtin_data = (1, "a", {"foo": 0.1})
        np_data = np.random.randn(1, 2, 3)
        tensor_data = torch.tensor([[1, 2, 3], [4, 5, 6]])
        label_data = [1, 2, 3]

        class Dummy(PipelineHandler):
            def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
                return builtin_data, np_data, tensor_data

            def cmp(self, _data_loader: t.Any) -> t.Any:
                data = [i for i in _data_loader]
                assert len(data) == 10
                (x, y, z) = data[0]["output"]
                assert x == builtin_data
                assert np.array_equal(y, np_data)
                assert torch.equal(z, tensor_data)
                assert label_data == data[0]["input"]["label"]

        m_summary.return_value = DatasetSummary(rows=1)

        fname = "data_ubyte_0.swds_bin"
        sign, _ = DatasetStorage.save_data_file(Path(self.swds_dir) / fname)
        m_scan.side_effect = [
            [{"id": 0, "value": 0}],
            [
                TabularDatasetRow(
                    features={
                        "image": GrayscaleImage(
                            link=Link(
                                uri=sign,
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                        "label": label_data,
                    },
                    id=i,
                ).asdict()
                for i in range(10)
            ],
        ]
        m_scan_id.return_value = [{"id": i} for i in range(10)]

        datastore_dir = DatasetStorage(
            Resource(
                self.dataset_uri_raw,
                typ=ResourceType.dataset,
            )
        )
        ensure_file(datastore_dir.manifest_path, "", parents=True)

        context = Context(
            workdir=Path(),
            run_project=self.local_project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="ppl",
            index=0,
        )
        Context.set_runtime_context(context)
        with Dummy() as _handler:
            _handler._starwhale_internal_run_predict()

        time.sleep(1)
        context = Context(
            workdir=Path(),
            run_project=self.local_project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="cmp",
            index=0,
        )
        Context.set_runtime_context(context)
        with Dummy() as _handler:
            _handler._starwhale_internal_run_evaluate()

    def test_predict_ingest(self) -> None:
        class DummyWithVarKeyword(PipelineHandler):
            def ppl(self, data: t.Any, **kw: t.Any) -> t.Any:
                assert data.label == 1
                assert isinstance(data, dict)
                assert "index" in kw["external"]
                assert kw["external"]["dataset_uri"].name == "mnist"
                assert kw["external"]["dataset_uri"].version == "123456"

        class DummyWithVarPositional(PipelineHandler):
            def ppl(self, *args: t.Any, **kw: t.Any) -> t.Any:
                assert args[0].label == 1
                assert "index" in kw["external"]
                assert "context" in kw["external"]

        class DummyWithOnlyData(PipelineHandler):
            def predict(self, data: t.Any) -> t.Any:
                assert data.label == 1

        class DummyWithOnlyVarKeyword(PipelineHandler):
            def predict(self, **kw: t.Any) -> t.Any:
                assert kw["data"].label == 1
                assert "index" in kw["external"]

        class DummyWithOnlyVarPositional(PipelineHandler):
            def predict(self, *args: t.Any) -> t.Any:
                assert args[0].label == 1
                assert "index" in args[1]

        class DummyWithKeyword(PipelineHandler):
            def predict(self, data: t.Any, external: t.Any) -> t.Any:
                assert data.label == 1
                assert "index" in external

        class DummyWithDecoratorOnlyData(PipelineHandler):
            @http_retry(attempts=3)
            def predict(self, data: t.Any) -> t.Any:
                assert data.label == 1

        class DummyWithDecoratorKeyword(PipelineHandler):
            @http_retry(attempts=3)
            @pass_context
            def predict(self, data: t.Any, **kw: t.Any) -> t.Any:
                assert data.label == 1
                assert "index" in kw["external"]
                assert isinstance(kw["context"], Context)

        class DummyWithoutAny(PipelineHandler):
            ...

        class DummyWithTwoHandlers(PipelineHandler):
            def ppl(self, data: t.Any, **kw: t.Any) -> t.Any:
                ...

            def predict(self, data: t.Any, external: t.Any) -> t.Any:
                ...

        handlers = [
            DummyWithDecoratorOnlyData,
            DummyWithDecoratorKeyword,
            DummyWithVarKeyword,
            DummyWithOnlyVarKeyword,
            DummyWithKeyword,
            DummyWithVarPositional,
            DummyWithOnlyData,
            DummyWithOnlyVarPositional,
        ]
        Context.set_runtime_context(Context(version="123", run_project=Project("self")))
        uri = Resource("mnist/version/123456", typ=ResourceType.dataset, refine=False)
        for h in handlers:
            h()._do_predict(
                data=DataRow._Features({"label": 1}),
                index=0,
                index_with_dataset="0",
                dataset_info=TabularDatasetInfo(),
                dataset_uri=uri,
            )

        with self.assertRaisesRegex(
            ParameterError, "predict and ppl cannot be defined at the same time"
        ):
            DummyWithTwoHandlers()._do_predict({}, 1, "1", TabularDatasetInfo(), uri)

        with self.assertRaisesRegex(
            ParameterError,
            "predict or ppl must be defined, predict function is recommended",
        ):
            DummyWithoutAny()._do_predict({}, 1, "1", TabularDatasetInfo(), uri)
