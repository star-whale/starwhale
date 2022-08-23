import os
import json
from pathlib import Path

from starwhale.utils.fs import ensure_dir
from starwhale.api.dataset import MNISTBuildExecutor, UserRawBuildExecutor
from starwhale.core.dataset.tabular import TabularDataset
from starwhale.api._impl.dataset.builder import (
    _data_magic,
    _header_size,
    _header_magic,
    _header_struct,
)

from .. import ROOT_DIR
from .test_base import BaseTestCase

_mnist_dir = f"{ROOT_DIR}/data/dataset/mnist"
_mnist_data = open(f"{_mnist_dir}/data", "rb").read()
_mnist_label = open(f"{_mnist_dir}/label", "rb").read()


class _UserRawMNIST(UserRawBuildExecutor):
    def iter_data_slice(self, path: str):
        size = 28 * 28
        file_size = Path(path).stat().st_size
        offset = 16
        while offset < file_size:
            yield offset, size
            offset += size

    def iter_label_slice(self, path: str):
        fpath = Path(path)

        with fpath.open("rb") as f:
            f.seek(8)
            while True:
                content = f.read(1)
                if not content:
                    break
                yield content


class TestDatasetBuildExecutor(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

        self.raw_data = os.path.join(self.local_storage, ".user", "data")
        self.workdir = os.path.join(self.local_storage, ".user", "workdir")

        ensure_dir(self.raw_data)
        with open(os.path.join(self.raw_data, "mnist-data-0"), "wb") as f:
            f.write(_mnist_data)

        with open(os.path.join(self.raw_data, "mnist-label-0"), "wb") as f:
            f.write(_mnist_label)

    def test_user_raw_workflow(self) -> None:
        with _UserRawMNIST(
            dataset_name="mnist",
            dataset_version="332211",
            project_name="self",
            data_dir=Path(self.raw_data),
            workdir=Path(self.workdir),
            data_filter="mnist-data-*",
            label_filter="mnist-data-*",
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()

        assert summary.rows == 10
        assert summary.include_user_raw
        assert not summary.include_link
        data_path = Path(self.workdir, "data", "mnist-data-0")

        assert data_path.exists()
        assert data_path.stat().st_size == 28 * 28 * summary.rows + 16
        tdb = TabularDataset(name="mnist", version="332211", project="self")
        meta = list(tdb.scan(0, 1))[0]
        assert meta.id == 0
        assert meta.data_offset == 16
        assert meta.data_uri == "mnist-data-0"

    def test_swds_bin_workflow(self) -> None:
        with MNISTBuildExecutor(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            data_dir=Path(self.raw_data),
            workdir=Path(self.workdir),
            data_filter="mnist-data-*",
            label_filter="mnist-data-*",
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()

        summary_content = json.dumps(summary.as_dict())
        assert summary_content
        assert summary.rows == 10
        assert summary.increased_rows == 10
        assert summary.unchanged_rows == 0
        assert not summary.include_user_raw
        assert not summary.include_link

        data_path = Path(self.workdir, "data", "data_ubyte_0.swds_bin")

        for i in range(0, 5):
            assert Path(self.workdir) / "data" / f"data_ubyte_{i}.swds_bin"

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
