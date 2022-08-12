import os
import json
import base64
import typing as t
import sysconfig
from unittest import skip
from unittest.mock import patch, MagicMock

import jsonlines
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.consts.env import SWEnv
from starwhale.api._impl.model import _RunConfig, PipelineHandler
from starwhale.api._impl.loader import get_data_loader, S3StorageBackend

from .. import ROOT_DIR


class SimpleHandler(PipelineHandler):
    def ppl(self, data: bytes, batch_size: int, **kw: t.Any) -> t.Any:
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

        self.status_dir = os.path.join(self.root, "status")
        self.log_dir = os.path.join(self.root, "log")
        # self.result_dir = os.path.join(self.root, "result")
        self.config_dir = os.path.join(self.root, "config")

        ensure_dir(self.config_dir)
        self.fs.add_real_directory(self.swds_dir)

    @patch("starwhale.api._impl.loader.boto3")
    def test_s3_loader(self, m_resource: MagicMock) -> None:
        swds_config = {
            "backend": "s3",
            "kind": "swds",
            "secret": {
                "access_key": "username",
                "secret_key": "password",
            },
            "service": {
                "endpoint": "127.1.1.1:1123",
                "region": "local",
            },
            "swds": [
                {
                    "bucket": "starwhale",
                    "key": {
                        "data": "data1",
                        "label": "label1",
                    },
                }
            ],
        }
        _loader = get_data_loader(swds_config)
        assert isinstance(_loader.storage, S3StorageBackend)

    def test_set_run_env(self) -> None:
        _RunConfig.set_env(
            {
                "status_dir": "status",
                "log_dir": "log",
                "input_config": "input_config",
            }
        )
        assert os.environ.get(SWEnv.input_config) == "input_config"

    @skip
    def test_default_cmp(self) -> None:
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
        os.environ[SWEnv.result_dir] = self.result_dir
        os.environ[SWEnv.input_config] = config_json_path

        with SimpleHandler() as _handler:
            ppl_result_path = os.path.join(ppl_result_dir, "current")
            with jsonlines.open(ppl_result_path, mode="w") as _jl:
                _jl.write(
                    {
                        "index": 0,
                        "ppl": base64.b64encode(
                            _handler.ppl_data_serialize([1, 2], 0.1)
                        ).decode("ascii"),
                        "batch": 10,
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
        result_file_path = os.path.join(self.result_dir, "current")
        assert os.path.exists(result_file_path)

        with jsonlines.open(result_file_path) as reader:
            lines = [_l for _l in reader]
            assert len(lines) == 1
            assert lines[0]["summary"] == {"a": 1}
            assert lines[0]["kind"] == "test"

    @skip
    def test_default_ppl(self) -> None:
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

        # datastore env
        # run ppl env
        os.environ[SWEnv.status_dir] = self.status_dir
        os.environ[SWEnv.log_dir] = self.log_dir
        os.environ[SWEnv.input_config] = config_json_path

        with SimpleHandler() as _handler:
            _handler._starwhale_internal_run_ppl()

        status_file_path = os.path.join(self.status_dir, "current")
        assert os.path.exists(status_file_path)
        assert "success" in open(status_file_path).read()
        assert os.path.exists(os.path.join(self.status_dir, "timeline"))

        result_file_path = os.path.join(self.result_dir, "current")
        assert os.path.exists(result_file_path)

        with open(result_file_path) as reader:
            lines = reader.readlines()
            assert len(lines) == 1
            with SimpleHandler() as _handler:
                line = _handler.deserialize(lines[0].encode("utf-8"))
            (result, pr) = line["ppl"]
            assert result == [1, 2]
            assert pr == 0.1
            assert line["index"] == 0
            assert line["batch"] == 10

    def test_deserializer(self) -> None:
        self.fs.add_real_directory(sysconfig.get_paths()["purelib"])
        import numpy as np
        import torch

        builtin_data = (1, "a", {"foo": 0.1})
        np_data = np.random.randn(1, 2, 3)
        tensor_data = torch.tensor([[1, 2, 3], [4, 5, 6]])
        label_data = [1, 2, 3]

        class Dummy(PipelineHandler):
            def ppl(self, data: bytes, batch_size: int, **kw: t.Any) -> t.Any:
                return builtin_data, np_data, tensor_data

            def handle_label(self, label: bytes, batch_size: int, **kw: t.Any) -> t.Any:
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
