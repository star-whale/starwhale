import os
from pathlib import Path

from starwhale.utils.fs import ensure_dir
from starwhale.api._impl.dataset import (
    _data_magic,
    _header_size,
    _header_magic,
    _header_struct,
    TabularDataset,
    MNISTBuildExecutor,
)

from .. import ROOT_DIR
from .test_base import BaseTestCase

_mnist_dir = f"{ROOT_DIR}/data/dataset/mnist"
_mnist_data = open(f"{_mnist_dir}/data", "rb").read()
_mnist_label = open(f"{_mnist_dir}/label", "rb").read()


class TestDatasetBuildExecutor(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

        self.raw_data = os.path.join(self.local_storage, ".user", "data")
        self.output_data = os.path.join(self.local_storage, ".user", "output")

        ensure_dir(self.raw_data)
        with open(os.path.join(self.raw_data, "mnist-data-0"), "wb") as f:
            f.write(_mnist_data)

        with open(os.path.join(self.raw_data, "mnist-label-0"), "wb") as f:
            f.write(_mnist_label)

    def test_workflow(self) -> None:
        with MNISTBuildExecutor(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            data_dir=Path(self.raw_data),
            output_dir=Path(self.output_data),
            data_filter="mnist-data-*",
            label_filter="mnist-data-*",
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            e.make_swds()

        data_path = Path(self.output_data, "data_ubyte_0.swds_bin")

        for i in range(0, 5):
            assert Path(self.output_data) / f"data_ubyte_{i}.swds_bin"

        data_content = data_path.read_bytes()
        _parser = _header_struct.unpack(data_content[:_header_size])
        assert _parser[0] == _header_magic
        assert _parser[2] == 0
        assert _parser[3] == 28 * 28
        assert _parser[6] == _data_magic
        assert len(data_content) == _header_size + _parser[3] + _parser[4]

        tdb = TabularDataset(name="mnist", version="112233", project="self")
        meta = list(tdb.scan(0, 1))[0]
        assert meta.id == 0
        assert meta.data_offset == 0
        assert meta.data_uri == "data_ubyte_0.swds_bin"
