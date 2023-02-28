import os
import sys
import errno
import shutil
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale import Context, get_data_loader, PipelineHandler
from starwhale.utils import gen_uniq_version
from starwhale.consts import DEFAULT_PROJECT
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, RunSubDirType, DataOriginType
from starwhale.api._impl.job import context_holder
from starwhale.core.eval.store import EvaluationStorage
from starwhale.core.dataset.type import Link, DatasetSummary, GrayscaleImage
from starwhale.core.dataset.store import ObjectStore, DatasetStorage
from starwhale.core.dataset.tabular import TabularDatasetRow, TabularDatasetInfo
from starwhale.api._impl.dataset.loader import DataRow, DataLoader


class SimpleHandler(PipelineHandler):
    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return [1, 2], 0.1

    def cmp(self, _data_loader: t.Any) -> t.Any:
        for _data in _data_loader:
            assert "result" in _data
            assert "annotations" in _data
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


class TestModelPipelineHandler(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.root = "/home/starwhale/model_test"

        self.project = DEFAULT_PROJECT
        self.eval_id = "mm3wky3dgbqt"

        self.dataset_uri_raw = f"mnist/version/{gen_uniq_version()}"
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
            dataset_uri=URI("mnist/version/latest", URIType.DATASET),
        )
        assert isinstance(_loader, DataLoader)
        assert not ObjectStore._stores

    @patch("starwhale.api._impl.evaluation.PPLResultIterator")
    @patch("starwhale.api._impl.wrapper.Evaluation.log_metrics")
    @patch("starwhale.api._impl.wrapper.Evaluation.log")
    def test_cmp(
        self,
        m_eval_log: MagicMock,
        m_eval_log_metrics: MagicMock,
        m_eval_get: MagicMock,
    ) -> None:
        _logdir = EvaluationStorage.local_run_dir(self.project, self.eval_id)
        _run_dir = _logdir / RunSubDirType.RUNLOG / "cmp" / "0"
        _status_dir = _run_dir / RunSubDirType.STATUS

        # mock datastore return results
        m_eval_get.__iter__.return_value = [
            {
                "result": "gASVaQAAAAAAAABdlEsHYV2UXZQoRz4mBBuTAu5hRz4bF5vyEiX+Rz479hi1FqrRRz5MqGToQCdARz3WYwL267cBRz3TzJIFVM1PRz1u4heY2/90Rz/wAAAAAAAARz3Kj1Gg+FBvRz5s1fMUlZZ8ZWGGlC4=",
                "data_size": "784",
                "id": "0",
                "annotations": "gASVBQAAAAAAAABLB4WULg==",
            },
            {
                "result": "gASVaQAAAAAAAABdlEsCYV2UXZQoRz7HJD9vpfz2Rz7nuBHd45K7Rz/v/95AI4woRz54jeSOtfKhRz4ydvSYTUVCRz4C6uB7EvDbRz66RdBlHOhyRz4yZGRfv61uRz6WGg/Jbfu6Rz3Qy/2xeB34ZWGGlC4=",
                "data_size": "784",
                "id": "1",
                "annotations": "gASVBQAAAAAAAABLAoWULg==",
            },
        ]

        context = Context(
            workdir=Path(),
            project=self.project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="cmp",
            index=0,
        )
        context_holder.context = context
        with SimpleHandler() as _handler:
            _handler._starwhale_internal_run_cmp()

        status_file_path = os.path.join(_status_dir, "current")
        assert os.path.exists(status_file_path)
        assert "success" in open(status_file_path).read()
        assert os.path.exists(os.path.join(_status_dir, "timeline"))

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    @patch("starwhale.api._impl.dataset.Dataset.info")
    @patch("starwhale.api._impl.dataset.Dataset.batch_iter")
    @patch("starwhale.api._impl.wrapper.Evaluation.log_result")
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    def test_ppl(
        self,
        m_summary: MagicMock,
        m_eval_log: MagicMock,
        m_ds: MagicMock,
        m_ds_info: MagicMock,
        m_scan_id: MagicMock,
    ) -> None:
        _logdir = EvaluationStorage.local_run_dir(self.project, self.eval_id)
        _run_dir = _logdir / RunSubDirType.RUNLOG / "ppl" / "0"
        _status_dir = _run_dir / RunSubDirType.STATUS

        # mock dataset
        m_summary.return_value = DatasetSummary(
            rows=1,
            increased_rows=1,
        )

        fname = "data_ubyte_0.swds_bin"
        m_scan_id.return_value = [{"id": 0}]
        m_ds.return_value = [
            [DataRow(0, {"image": GrayscaleImage(link=Link(fname)), "label": 0})]
        ]
        m_ds_info.return_value = TabularDatasetInfo(mapping={"id": 0, "value": 1})

        datastore_dir = DatasetStorage(URI(self.dataset_uri_raw, URIType.DATASET))
        data_dir = datastore_dir.data_dir
        ensure_dir(data_dir)
        shutil.copyfile(os.path.join(self.swds_dir, fname), str(data_dir / fname))
        ensure_dir(datastore_dir.loc)
        ensure_file(datastore_dir.manifest_path, "")

        context = Context(
            workdir=Path(),
            project=self.project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="ppl",
            index=0,
        )
        context_holder.context = context
        with SimpleHandler() as _handler:
            _handler._starwhale_internal_run_ppl()

        m_eval_log.assert_called_once()
        status_file_path = os.path.join(_status_dir, "current")
        assert os.path.exists(status_file_path)
        assert "success" in open(status_file_path).read()
        assert os.path.exists(os.path.join(_status_dir, "timeline"))

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan")
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
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
                assert len(data) == 1
                (x, y, z) = data[0]["result"]
                assert x == builtin_data
                assert np.array_equal(y, np_data)
                assert torch.equal(z, tensor_data)

                assert label_data == data[0]["ds_data"]["label"]

        # mock dataset
        m_summary.return_value = DatasetSummary(
            rows=1,
            increased_rows=1,
        )

        fname = "data_ubyte_0.swds_bin"
        m_scan.side_effect = [
            [{"id": 0, "value": 1}],
            [
                TabularDatasetRow(
                    data={
                        "image": GrayscaleImage(
                            link=Link(
                                fname,
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                        "label": label_data,
                    },
                    data_origin=DataOriginType.NEW,
                    id=0,
                ).asdict(),
            ],
        ]
        m_scan_id.return_value = [{"id": 0}]

        datastore_dir = DatasetStorage(URI(self.dataset_uri_raw, URIType.DATASET))
        data_dir = datastore_dir.data_dir
        ensure_dir(data_dir)
        shutil.copyfile(os.path.join(self.swds_dir, fname), str(data_dir / fname))
        ensure_dir(datastore_dir.loc)
        ensure_file(datastore_dir.manifest_path, "")

        context = Context(
            workdir=Path(),
            project=self.project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="ppl",
            index=0,
        )
        context_holder.context = context
        # mock
        with Dummy(flush_result=True) as _handler:
            _handler._starwhale_internal_run_ppl()

        context = Context(
            workdir=Path(),
            project=self.project,
            version=self.eval_id,
            dataset_uris=[self.dataset_uri_raw],
            step="cmp",
            index=0,
        )
        context_holder.context = context
        with Dummy() as _handler:
            _handler._starwhale_internal_run_cmp()
