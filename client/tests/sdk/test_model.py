import os
import json
import typing as t
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
        self.result_dir = os.path.join(self.root, "result")
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
                "result_dir": "result",
                "input_config": "input_config",
            }
        )
        assert os.environ.get(SWEnv.input_config) == "input_config"

    def test_cmp(self) -> None:
        ppl_result_dir = os.path.join(self.root, "ppl")
        ensure_dir(ppl_result_dir)
        ppl_result_path = os.path.join(ppl_result_dir, "current")
        with jsonlines.open(ppl_result_path, mode="w") as _jl:
            _jl.write(
                {
                    "index": 0,
                    "result": [1, 2],
                    "pr": 0.1,
                    "batch": 10,
                    "label": "\\u0007\\u0002\\u0001\\u0000\\u0004\\u0001\\u0004\\t\\u0005\\t",
                }
            )

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

    def test_ppl(self) -> None:
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

        os.environ[SWEnv.status_dir] = self.status_dir
        os.environ[SWEnv.log_dir] = self.log_dir
        os.environ[SWEnv.result_dir] = self.result_dir
        os.environ[SWEnv.input_config] = config_json_path

        with SimpleHandler() as _handler:
            _handler._starwhale_internal_run_ppl()

        status_file_path = os.path.join(self.status_dir, "current")
        assert os.path.exists(status_file_path)
        assert "success" in open(status_file_path).read()
        assert os.path.exists(os.path.join(self.status_dir, "timeline"))

        result_file_path = os.path.join(self.result_dir, "current")
        assert os.path.exists(result_file_path)

        with jsonlines.open(result_file_path) as reader:
            lines = [_l for _l in reader]
            assert len(lines) == 1
            assert lines[0]["index"] == 0
            assert lines[0]["result"] == [1, 2]
            assert "pr" in lines[0]
            assert lines[0]["batch"] == 10
