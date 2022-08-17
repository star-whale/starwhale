import os
import json
import base64
import typing as t
import sysconfig
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest
import jsonlines
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import DEFAULT_PROJECT, SWDSBackendType
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.consts.env import SWEnv
from starwhale.api._impl.job import Context
from starwhale.api._impl.model import _RunConfig, PipelineHandler
from starwhale.api._impl.loader import get_data_loader, S3StorageBackend
from starwhale.api._impl.dataset import TabularDatasetRow
from starwhale.api._impl.wrapper import Evaluation

from .. import ROOT_DIR


class SimpleHandler(PipelineHandler):
    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return [1, 2], 0.1

    def cmp(self, _data_loader: t.Any) -> t.Any:
        for _data in _data_loader:
            print(_data)
        return {"summary": {"a": 1}, "kind": "test", "labels": {"1": 1}}


class TestModelPipelineHandler(TestCase):
    swds_dir = os.path.join(ROOT_DIR, "data", "dataset", "swds")

    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.root = "/home/starwhale/model_test"

        self.project = DEFAULT_PROJECT
        self.eval_id = "mm3wky3dgbqt"

        self.status_dir = os.path.join(self.root, "status")
        self.log_dir = os.path.join(self.root, "log")
        self.config_dir = os.path.join(self.root, "config")

        ensure_dir(self.config_dir)
        self.fs.add_real_directory(self.swds_dir)
        os.environ["SW_S3_BUCKET"] = "starwhale"

    def tearDown(self) -> None:
        super().tearDown()
        os.environ.pop("SW_S3_BUCKET", "")

    @patch("starwhale.api._impl.loader.boto3")
    def test_s3_loader(self, m_resource: MagicMock) -> None:
        _loader = get_data_loader(
            dataset_uri=URI("mnist/version/latest", URIType.DATASET),
            backend=SWDSBackendType.S3,
        )
        assert isinstance(_loader.storage.backend, S3StorageBackend)

    def test_set_run_env(self) -> None:
        _RunConfig.set_env(
            {
                "status_dir": "status",
                "log_dir": "log",
                "result_dir": "result",
                "dataset_uri": "mnist/version/latest",
            }
        )
        assert os.environ.get(SWEnv.status_dir) == "status"
        assert os.environ.get(SWEnv.dataset_uri) == "mnist/version/latest"

    @pytest.mark.skip(reason="wait job scheduler feature, cmp will use datastore")
    def test_cmp(self) -> None:
        ppl_result_dir = os.path.join(self.root, "ppl")
        ensure_dir(ppl_result_dir)

        config_json_path = os.path.join(self.config_dir, "input.json")
        local_ppl_result_config = {
            "backend": "fuse",
            "kind": "jsonl",
            "swds": [
                {
                    "bucket": ppl_result_dir,
                    "key": {
                        "data": "current",
                    },
                }
            ],
        }
        ensure_file(config_json_path, json.dumps(local_ppl_result_config))

        os.environ[SWEnv.status_dir] = self.status_dir
        os.environ[SWEnv.log_dir] = self.log_dir

        with SimpleHandler() as _handler:
            ppl_result_path = os.path.join(ppl_result_dir, "current")
            with jsonlines.open(ppl_result_path, mode="w") as _jl:
                _jl.write(
                    {
                        "index": 0,
                        "ppl": base64.b64encode(
                            _handler.ppl_data_serialize([1, 2], 0.1)
                        ).decode("ascii"),
                        "label": base64.b64encode(
                            _handler.label_data_serialize([3, 4])
                        ).decode("ascii"),
                    }
                )
            _handler._starwhale_internal_run_cmp()

        status_file_path = os.path.join(self.status_dir, "current")
        assert os.path.exists(status_file_path)
        assert "success" in open(status_file_path).read()
        assert os.path.exists(os.path.join(self.status_dir, "timeline"))

        # TODO: use datastore results
        # with jsonlines.open(result_file_path) as reader:
        #     lines = [_l for _l in reader]
        #     assert len(lines) == 1
        #     assert lines[0]["summary"] == {"a": 1}
        #     assert lines[0]["kind"] == "test"

    # @pytest.mark.skip(reason="wait job scheduler feature, ppl will use datastore")
    @patch("starwhale.api._impl.loader.TabularDataset.scan")
    def test_ppl(self, m_scan: MagicMock) -> None:
        os.environ[SWEnv.instance_uri] = "local"
        os.environ[SWEnv.project] = self.project
        os.environ[SWEnv.status_dir] = self.status_dir
        os.environ[SWEnv.log_dir] = self.log_dir
        os.environ[SWEnv.dataset_uri] = "mnist/version/latest"
        os.environ[SWEnv.dataset_row_start] = "0"
        os.environ[SWEnv.dataset_row_end] = "1"
        os.environ["SW_S3_BUCKET"] = self.swds_dir

        m_scan.return_value = [
            TabularDatasetRow(
                id=i,
                data_uri="data_ubyte_0.swds_bin",
                label=str(i).encode(),
                data_offset=0,
                data_size=8160,
            )
            for i in range(0, 1)
        ]
        _eval_store = Evaluation(eval_id=self.eval_id)
        with SimpleHandler(
            context=Context(
                workdir=Path(),
                src_dir=Path(),
                project=self.project,
                version=self.eval_id,
            )
        ) as _handler:
            _handler._starwhale_internal_run_ppl()

        status_file_path = os.path.join(self.status_dir, "current")
        assert os.path.exists(status_file_path)
        assert "success" in open(status_file_path).read()
        assert os.path.exists(os.path.join(self.status_dir, "timeline"))

        # TODO: use datastore results
        _ppl_results = list(_eval_store.get_results())
        assert len(_ppl_results) == 1
        with SimpleHandler(
            context=Context(
                workdir=Path(),
                src_dir=Path(),
                project=self.project,
                version=self.eval_id,
            )
        ) as _handler:
            _result = _handler.deserialize_fields(_ppl_results[0])

        (result, pr) = _result["result"]
        assert result == [1, 2]
        assert pr == 0.1
        assert result["id"] == 0

    @pytest.mark.skip(reason="wait job scheduler feature, cmp will use datastore")
    def test_deserializer(self) -> None:
        self.fs.add_real_directory(sysconfig.get_paths()["purelib"])
        import numpy as np
        import torch

        builtin_data = (1, "a", {"foo": 0.1})
        np_data = np.random.randn(1, 2, 3)
        tensor_data = torch.tensor([[1, 2, 3], [4, 5, 6]])
        label_data = [1, 2, 3]

        class Dummy(PipelineHandler):
            def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
                return builtin_data, np_data, tensor_data

            def handle_label(self, label: bytes, **kw: t.Any) -> t.Any:
                return label_data

            def cmp(self, _data_loader: t.Any) -> t.Any:
                data = [i for i in _data_loader]
                assert len(data) == 1
                (x, y, z) = data[0]["ppl"]
                assert x == builtin_data
                assert np.array_equal(y, np_data)
                assert torch.equal(z, tensor_data)

                assert label_data == data[0]["label"]

        config_json_path = os.path.join(self.config_dir, "input.json")
        local_swds_config = {
            "backend": "fuse",
            "kind": "swds",
            "swds": [
                {
                    "bucket": self.swds_dir,
                    "key": {
                        "data": "data_ubyte_0.swds_bin",
                        "label": "label_ubyte_0.swds_bin",
                    },
                }
            ],
        }
        ensure_file(config_json_path, json.dumps(local_swds_config))
        ppl_result = os.path.join(self.root, "ppl")
        os.environ[SWEnv.status_dir] = self.status_dir
        os.environ[SWEnv.log_dir] = self.log_dir
        os.environ[SWEnv.result_dir] = ppl_result
        os.environ[SWEnv.input_config] = config_json_path

        with Dummy() as _handler:
            _handler._starwhale_internal_run_ppl()

        result_file_path = os.path.join(ppl_result, "current")
        assert os.path.exists(result_file_path)

        local_swds_config = {
            "backend": "fuse",
            "kind": "jsonl",
            "swds": [
                {
                    "bucket": ppl_result,
                    "key": {
                        "data": "current",
                    },
                }
            ],
        }
        ensure_file(config_json_path, json.dumps(local_swds_config))
        os.environ[SWEnv.result_dir] = os.path.join(self.root, "cmp")
        with Dummy() as _handler:
            _handler._starwhale_internal_run_cmp()
