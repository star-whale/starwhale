import io
import os
import json
import base64
import struct
import typing as t
from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale import Link, MIMEType, UserRawBuildExecutor
from starwhale.utils.fs import blake2b_file
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.core.dataset.type import (
    Text,
    Audio,
    Image,
    Binary,
    ClassLabel,
    BoundingBox,
    ArtifactType,
    BaseArtifact,
    GrayscaleImage,
    COCOObjectAnnotation,
)
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import TabularDataset
from starwhale.api._impl.dataset.loader import calculate_index
from starwhale.api._impl.dataset.builder import (
    _data_magic,
    _header_size,
    _header_magic,
    _header_struct,
    SWDSBinBuildExecutor,
)

from .. import ROOT_DIR
from .test_base import BaseTestCase

_mnist_dir = Path(f"{ROOT_DIR}/data/dataset/mnist")
_mnist_data_path = _mnist_dir / "data"
_mnist_label_path = _mnist_dir / "label"


class MNISTBuildExecutor(SWDSBinBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        with _mnist_data_path.open("rb") as data_file, _mnist_label_path.open(
            "rb"
        ) as label_file:
            _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
            _, label_number = struct.unpack(">II", label_file.read(8))
            print(
                f">data({data_file.name}) split data:{data_number}, label:{label_number} group"
            )
            image_size = height * width

            for i in range(0, min(data_number, label_number)):
                _data = data_file.read(image_size)
                _label = struct.unpack(">B", label_file.read(1))[0]
                yield GrayscaleImage(
                    _data,
                    display_name=f"{i}",
                    shape=(height, width, 1),
                ), {"label": _label}


class _UserRawMNIST(UserRawBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        with _mnist_data_path.open("rb") as data_file, _mnist_label_path.open(
            "rb"
        ) as label_file:
            _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
            _, label_number = struct.unpack(">II", label_file.read(8))

            image_size = height * width
            offset = 16

            for i in range(0, min(data_number, label_number)):
                _data = Link(
                    uri=str(_mnist_data_path.absolute()),
                    offset=offset,
                    size=image_size,
                    data_type=GrayscaleImage(
                        display_name=f"{i}", shape=(height, width, 1)
                    ),
                    with_local_fs_data=True,
                )
                _label = struct.unpack(">B", label_file.read(1))[0]
                yield _data, {"label": _label}
                offset += image_size


class TestDatasetBuildExecutor(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

        self.object_store_dir = os.path.join(
            self.local_storage, ".objectstore", DatasetStorage.object_hash_algo
        )
        self.raw_data = os.path.join(self.local_storage, ".user", "data")
        self.workdir = os.path.join(self.local_storage, ".user", "workdir")
        self.data_file_sign = blake2b_file(_mnist_data_path)

    def test_user_raw_workflow(self) -> None:
        with _UserRawMNIST(
            dataset_name="mnist",
            dataset_version="332211",
            project_name="self",
            workdir=Path(self.workdir),
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
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            assert e.data_tmpdir.exists()
            summary = e.make_swds()

        assert not e.data_tmpdir.exists()

        data_files_sign = []
        for f in e.data_output_dir.iterdir():
            if not f.is_symlink():
                continue
            data_files_sign.append(f.resolve().name)

        summary_content = json.dumps(summary.asdict())
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
        assert meta.data_type["type"] == ArtifactType.Image.value
        assert meta.data_type["mime_type"] == MIMEType.GRAYSCALE.value
        assert meta.data_type["shape"] == [28, 28, 1]


class TestDatasetType(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def test_binary(self) -> None:
        b = Binary(b"test")
        assert b.to_bytes() == b"test"
        assert b.astype() == {
            "type": ArtifactType.Binary,
            "mime_type": MIMEType.UNDEFINED,
            "shape": (1,),
            "encoding": "",
            "display_name": "",
        }

    def test_image(self) -> None:
        fp = io.StringIO("test")
        img = Image(fp, display_name="t", shape=(28, 28, 3), mime_type=MIMEType.PNG)
        assert img.to_bytes() == b"test"
        _asdict = img.asdict()
        assert not _asdict["as_mask"]
        assert "fp" not in _asdict
        assert "_raw_base64_data" not in _asdict
        assert _asdict["type"] == "image"
        assert _asdict["display_name"] == "t"
        assert _asdict["shape"] == (28, 28, 3)
        assert json.loads(json.dumps(_asdict))["type"] == "image"

        fp = io.BytesIO(b"test")
        img = GrayscaleImage(fp, shape=(28, 28, 1)).carry_raw_data()
        assert img.to_bytes() == b"test"
        _asdict = json.loads(json.dumps(img.asdict()))
        assert _asdict["type"] == "image"
        assert _asdict["mime_type"] == MIMEType.GRAYSCALE.value
        assert _asdict["shape"] == [28, 28, 1]
        assert _asdict["_raw_base64_data"] == base64.b64encode(b"test").decode()

    def test_audio(self) -> None:
        fp = "/test/1.wav"
        self.fs.create_file(fp, contents="test")
        audio = Audio(fp)
        _asdict = json.loads(json.dumps(audio.asdict()))
        assert _asdict["mime_type"] == MIMEType.WAV.value
        assert _asdict["type"] == "audio"
        assert audio.to_bytes() == b"test"

    def test_bbox(self) -> None:
        bbox = BoundingBox(1, 2, 3, 4)
        assert bbox.to_list() == [1, 2, 3, 4]
        _asdict = json.loads(json.dumps(bbox.asdict()))
        assert _asdict["type"] == "bounding_box"
        assert _asdict["x"] == 1
        assert _asdict["y"] == 2
        assert _asdict["width"] == 3
        assert _asdict["height"] == 4

    def test_text(self) -> None:
        text = Text("test")
        _asdict = json.loads(json.dumps(text.asdict()))
        assert text.to_bytes() == b"test"
        assert "fp" not in _asdict
        assert _asdict["content"] == "test"
        assert _asdict["type"] == "text"
        assert _asdict["mime_type"] == MIMEType.PLAIN.value

    def test_coco(self) -> None:
        coco = COCOObjectAnnotation(
            id=1,
            image_id=1,
            category_id=1,
            segmentation={"counts": "abcd"},
            area=100,
            bbox=BoundingBox(1, 2, 3, 4),
            iscrowd=1,
        )
        _asdict = json.loads(json.dumps(coco.asdict()))
        assert _asdict["type"] == "coco_object_annotation"

        with self.assertRaises(FieldTypeOrValueError):
            coco = COCOObjectAnnotation(
                id=1,
                image_id=1,
                category_id=1,
                segmentation={"counts": "abcd"},
                area=100,
                bbox=BoundingBox(1, 2, 3, 4),
                iscrowd=3,
            )

    def test_class_label(self) -> None:
        cl = ClassLabel([1, 2, 3])
        _asdict = json.loads(json.dumps(cl.asdict()))
        assert _asdict["type"] == "class_label"
        assert _asdict["names"] == [1, 2, 3]

        cl = ClassLabel.from_num_classes(3)
        assert cl.names == [0, 1, 2]

        with self.assertRaises(FieldTypeOrValueError):
            ClassLabel.from_num_classes(0)

    def test_reflect(self) -> None:
        img = BaseArtifact.reflect(b"test", data_type={"type": "image"})
        assert isinstance(img, Image)
        assert img.type.value == "image"

        text = BaseArtifact.reflect(
            b"text", data_type={"type": "text", "encoding": "utf-8"}
        )
        assert isinstance(text, Text)
        assert text.content == "text"

        audio = BaseArtifact.reflect(b"audio", data_type={"type": "audio"})
        assert isinstance(audio, Audio)

        b = BaseArtifact.reflect(b"fsdf", data_type={})
        assert isinstance(b, Binary)

        with self.assertRaises(NoSupportError):
            BaseArtifact.reflect(b"", data_type={"type": 1})

        link_audio = BaseArtifact.reflect(
            b"link", data_type={"type": "link", "data_type": {"type": "audio"}}
        )
        assert isinstance(link_audio, Audio)


class TestLoader(TestCase):
    def test_calculate_index(self):
        _start, _end = calculate_index(data_size=2, sharding_num=5, sharding_index=0)
        assert _start == 0 and _end == 1
        _start, _end = calculate_index(data_size=2, sharding_num=5, sharding_index=1)
        assert _start == 1 and _end == 2
        _start, _end = calculate_index(data_size=2, sharding_num=5, sharding_index=2)
        assert _start == -1 and _end == -1
        _start, _end = calculate_index(data_size=2, sharding_num=5, sharding_index=3)
        assert _start == -1 and _end == -1

        _start, _end = calculate_index(data_size=100, sharding_num=1, sharding_index=0)
        assert _start == 0 and _end == 100

        _start, _end = calculate_index(data_size=100, sharding_num=2, sharding_index=0)
        assert _start == 0 and _end == 50
        _start, _end = calculate_index(data_size=100, sharding_num=2, sharding_index=1)
        assert _start == 50 and _end == 100

        _start, _end = calculate_index(data_size=101, sharding_num=2, sharding_index=0)
        assert _start == 0 and _end == 51
        _start, _end = calculate_index(data_size=101, sharding_num=2, sharding_index=1)
        assert _start == 51 and _end == 101

        _start, _end = calculate_index(data_size=110, sharding_num=3, sharding_index=0)
        assert _start == 0 and _end == 37
        _start, _end = calculate_index(data_size=110, sharding_num=3, sharding_index=1)
        assert _start == 37 and _end == 74
        _start, _end = calculate_index(data_size=110, sharding_num=3, sharding_index=2)
        assert _start == 74 and _end == 110
