import os
import json
import struct
from pathlib import Path

from starwhale.utils.fs import ensure_dir, blake2b_file
from starwhale.api.dataset import (
    Link,
    MIMEType,
    MNISTBuildExecutor,
    UserRawBuildExecutor,
)
from starwhale.core.dataset.store import DatasetStorage
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
            yield Link(offset=offset, size=size, mime_type=MIMEType.GRAYSCALE)
            offset += size

    def iter_label_slice(self, path: str):
        fpath = Path(path)

        with fpath.open("rb") as f:
            f.seek(8)
            while True:
                content = f.read(1)
                if not content:
                    break
                yield struct.unpack(">B", content)[0]


class TestDatasetBuildExecutor(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

        self.object_store_dir = os.path.join(
            self.local_storage, ".objectstore", DatasetStorage.object_hash_algo
        )
        self.raw_data = os.path.join(self.local_storage, ".user", "data")
        self.workdir = os.path.join(self.local_storage, ".user", "workdir")

        self.data_fpath = os.path.join(self.raw_data, "mnist-data-0")
        ensure_dir(self.raw_data)
        with open(self.data_fpath, "wb") as f:
            f.write(_mnist_data)

        self.data_file_sign = blake2b_file(self.data_fpath)

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

        link_path = (
            Path(self.workdir)
            / "data"
            / self.data_file_sign[: DatasetStorage.short_sign_cnt]
        )
        assert link_path.exists()

        data_path = (
            Path(self.object_store_dir) / self.data_file_sign[:2] / self.data_file_sign
        )

        assert link_path.resolve() == data_path
        assert data_path.exists()
        assert data_path.stat().st_size == 28 * 28 * summary.rows + 16
        tdb = TabularDataset(name="mnist", version="332211", project="self")
        meta = list(tdb.scan(0, 1))[0]
        assert meta.id == 0
        assert meta.data_offset == 16
        assert meta.data_uri == self.data_file_sign

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
            data_mime_type=MIMEType.GRAYSCALE,
        ) as e:
            assert e.data_tmpdir.exists()
            summary = e.make_swds()

        assert not e.data_tmpdir.exists()

        data_files_sign = []
        for f in e.data_output_dir.iterdir():
            if not f.is_symlink():
                continue
            data_files_sign.append(f.resolve().name)

        summary_content = json.dumps(summary.as_dict())
        assert summary_content
        assert summary.rows == 10
        assert summary.increased_rows == 10
        assert summary.unchanged_rows == 0
        assert not summary.include_user_raw
        assert not summary.include_link

        assert len(data_files_sign) == 11

        for _sign in data_files_sign:
            _sign_fpath = Path(self.object_store_dir) / _sign[:2] / _sign
            assert _sign_fpath.exists()
            assert _sign == blake2b_file(_sign_fpath)
            assert (
                _sign_fpath
                == (
                    e.data_output_dir / _sign[: DatasetStorage.short_sign_cnt]
                ).resolve()
            )

        data_path = (
            Path(self.object_store_dir) / data_files_sign[0][:2] / data_files_sign[0]
        )
        data_content = data_path.read_bytes()
        _parser = _header_struct.unpack(data_content[:_header_size])
        assert _parser[0] == _header_magic
        assert _parser[3] == 28 * 28
        assert _parser[6] == _data_magic
        assert len(data_content) == _header_size + _parser[3] + _parser[4]

        tdb = TabularDataset(name="mnist", version="112233", project="self")
        meta = list(tdb.scan(0, 1))[0]
        assert meta.id == 0
        assert meta.data_offset == 32
        assert meta.extra_kw["_swds_bin_offset"] == 0
        assert meta.data_uri in data_files_sign
        assert meta.data_mime_type == MIMEType.GRAYSCALE
