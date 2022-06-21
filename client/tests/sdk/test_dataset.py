import os
from pathlib import Path

import jsonlines
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils.fs import ensure_dir
from starwhale.api._impl.dataset import (
    _data_magic,
    _header_size,
    _header_magic,
    _header_struct,
    MNISTBuildExecutor,
)

from .. import ROOT_DIR

_mnist_dir = f"{ROOT_DIR}/data/dataset/mnist"
_mnist_data = open(f"{_mnist_dir}/data", "rb").read()
_mnist_label = open(f"{_mnist_dir}/label", "rb").read()


class TestDatasetBuildExecutor(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.root = "/home/starwhale/dataset_test"
        self.raw_data = os.path.join(self.root, "data")
        self.output_data = os.path.join(self.root, "output")

        ensure_dir(self.raw_data)
        with open(os.path.join(self.raw_data, "mnist-data-0"), "wb") as f:
            f.write(_mnist_data)

        with open(os.path.join(self.raw_data, "mnist-label-0"), "wb") as f:
            f.write(_mnist_label)

    def test_workflow(self) -> None:
        batch_size = 2
        with MNISTBuildExecutor(
            data_dir=Path(self.raw_data),
            output_dir=Path(self.output_data),
            data_filter="mnist-data-*",
            label_filter="mnist-data-*",
            batch=batch_size,
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            e.make_swds()

        data_path = Path(self.output_data, "data_ubyte_0.swds_bin")
        label_path = Path(self.output_data, "label_ubyte_0.swds_bin")
        index_path = Path(self.output_data, "index.jsonl")

        for i in range(0, 5):
            assert Path(self.output_data) / f"data_ubyte_{i}.swds_bin"
            assert Path(self.output_data) / f"label_ubyte_{i}.swds_bin"

        assert index_path.exists()

        data_content = data_path.read_bytes()
        _parser = _header_struct.unpack(data_content[:_header_size])
        assert _parser[0] == _header_magic
        assert _parser[2] == 0
        assert _parser[3] == 28 * 28 * batch_size
        assert _parser[5] == batch_size
        assert _parser[6] == _data_magic
        assert len(data_content) == _header_size + _parser[3] + _parser[4]

        label_content = label_path.read_bytes()
        _label_parser = _header_struct.unpack(label_content[:_header_size])

        assert _label_parser[0] == _header_magic
        assert _label_parser[2] == 0
        assert _label_parser[3] == 2
        assert len(label_content) == _header_size + _label_parser[3] + _label_parser[4]

        with jsonlines.open(str(index_path)) as reader:
            cnt = 0
            for _line in reader:
                cnt += 1
                assert _line["batch"] == 2
                assert "data" in _line and "label" in _line

            assert cnt == 5
