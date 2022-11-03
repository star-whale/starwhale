import io
import os
import json
import math
import time
import base64
import struct
import typing as t
import threading
from pathlib import Path
from unittest.mock import patch, MagicMock
from concurrent.futures import as_completed, ThreadPoolExecutor

import jsonlines
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale import Link, MIMEType, UserRawBuildExecutor
from starwhale.consts import HTTPMethod, ENV_POD_NAME, OBJECT_STORE_DIRNAME
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file, blake2b_file
from starwhale.base.type import URIType, DataFormatType, DataOriginType, ObjectStoreType
from starwhale.consts.env import SWEnv
from starwhale.utils.error import (
    FormatError,
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
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
from starwhale.core.dataset.tabular import (
    CloudTDSC,
    StandaloneTDSC,
    TabularDataset,
    TabularDatasetRow,
    local_standalone_tdsc,
    get_dataset_consumption,
    StandaloneTabularDataset,
)
from starwhale.api._impl.dataset.builder import (
    _data_magic,
    _header_size,
    _header_magic,
    _header_struct,
    create_generic_cls,
    SWDSBinBuildExecutor,
)

from .test_base import BaseTestCase

_mnist_dir = Path(f"{ROOT_DIR}/data/dataset/mnist")
_mnist_data_path = _mnist_dir / "data"
_mnist_label_path = _mnist_dir / "label"


def iter_mnist_swds_bin_item_with_id() -> t.Generator[
    t.Tuple[t.Any, t.Any, t.Any], None, None
]:
    for data, annotations in iter_mnist_swds_bin_item():
        yield f"mnist-{data.display_name}", data, annotations


def iter_mnist_swds_bin_item() -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
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


class MNISTBuildExecutor(SWDSBinBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        return iter_mnist_swds_bin_item()


class MNISTBuildWithIDExecutor(SWDSBinBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any, t.Any], None, None]:
        return iter_mnist_swds_bin_item_with_id()


def iter_mnist_user_raw_item_with_id() -> t.Generator[
    t.Tuple[t.Any, t.Any, t.Any], None, None
]:
    for data, annotations in iter_mnist_user_raw_item():
        yield f"mnist-link-{data.data_type.display_name}", data, annotations


def iter_mnist_user_raw_item() -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
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
                data_type=GrayscaleImage(display_name=f"{i}", shape=(height, width, 1)),
                with_local_fs_data=True,
            )
            _label = struct.unpack(">B", label_file.read(1))[0]
            yield _data, {"label": _label}
            offset += image_size


class UserRawMNIST(UserRawBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        return iter_mnist_user_raw_item()


class UserRawWithIDMNIST(UserRawBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any, t.Any], None, None]:
        return iter_mnist_user_raw_item_with_id()


class TestDatasetBuildExecutor(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

        self.object_store_dir = os.path.join(
            self.local_storage, OBJECT_STORE_DIRNAME, DatasetStorage.object_hash_algo
        )
        self.raw_data = os.path.join(self.local_storage, ".user", "data")
        self.workdir = os.path.join(self.local_storage, ".user", "workdir")
        self.data_file_sign = blake2b_file(_mnist_data_path)

    def test_user_raw_with_id_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_user_raw_item_with_id)
        assert issubclass(_cls, UserRawBuildExecutor)
        with _cls(
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

    def test_user_raw_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_user_raw_item)
        assert issubclass(_cls, UserRawBuildExecutor)
        with _cls(
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

    def test_swds_bin_with_id_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_swds_bin_item_with_id)
        assert issubclass(_cls, SWDSBinBuildExecutor)
        with _cls(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()

        assert summary.rows == 10
        assert not summary.include_user_raw
        assert not summary.include_link

    def test_swds_bin_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_swds_bin_item)
        assert issubclass(_cls, SWDSBinBuildExecutor)
        with _cls(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()

        assert summary.rows == 10
        assert not summary.include_user_raw
        assert not summary.include_link

    def test_abnormal_function_handler(self) -> None:
        non_generator_f = lambda: 1
        with self.assertRaises(RuntimeError):
            _cls = create_generic_cls(non_generator_f)  # type: ignore

        list_f = lambda: [(b"1", {"a": 1}), (b"2", {"a": 2}), (b"3", {"a": 3})]
        _cls = create_generic_cls(list_f)  # type: ignore
        assert issubclass(_cls, SWDSBinBuildExecutor)
        with _cls(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()
        assert summary.rows == 3

        def _gen_only_one() -> t.Generator:
            yield b"", {"a": 1}

        _cls = create_generic_cls(_gen_only_one)
        assert issubclass(_cls, SWDSBinBuildExecutor)
        with _cls(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()

        assert summary.rows == 1
        assert not summary.include_user_raw
        assert not summary.include_link

    def test_user_raw_with_id_workflow(self) -> None:
        with UserRawWithIDMNIST(
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

        tdb = TabularDataset(name="mnist", version="332211", project="self")
        meta = list(tdb.scan())
        assert len(meta) == 10
        assert meta[0].id == "mnist-link-0"
        assert meta[1].id == "mnist-link-1"
        ids = list(tdb._ds_wrapper.scan_id(None, None))
        assert len(ids) == 10
        assert isinstance(ids[9], dict)
        assert ids[9]["id"] == "mnist-link-9"

    def test_user_raw_workflow(self) -> None:
        with UserRawMNIST(
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
        ).resolve()

        assert link_path.resolve() == data_path
        assert data_path.exists()
        assert data_path.stat().st_size == 28 * 28 * summary.rows + 16
        tdb = TabularDataset(name="mnist", version="332211", project="self")
        meta = list(tdb.scan(start=0, end=1))[0]
        assert meta.id == 0
        assert meta.data_offset == 16
        assert meta.data_uri == self.data_file_sign

    def test_swds_bin_id_workflow(self) -> None:
        with MNISTBuildWithIDExecutor(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            assert e.data_tmpdir.exists()
            summary = e.make_swds()

        summary_content = json.dumps(summary.asdict())
        assert summary_content
        assert summary.rows == 10
        assert summary.increased_rows == 10
        assert summary.unchanged_rows == 0
        assert not summary.include_user_raw
        assert not summary.include_link

        tdb = TabularDataset(name="mnist", version="112233", project="self")
        meta = list(tdb.scan())
        assert len(meta) == 10
        assert meta[0].id == "mnist-0"
        assert meta[1].id == "mnist-1"
        ids = list(tdb._ds_wrapper.scan_id(None, None))
        assert len(ids) == 10
        assert isinstance(ids[9], dict)
        assert ids[9]["id"] == "mnist-9"

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

        assert len(data_files_sign) == 10

        for _sign in data_files_sign:
            _sign_fpath = (Path(self.object_store_dir) / _sign[:2] / _sign).resolve()
            assert _sign_fpath.exists()
            assert _sign == blake2b_file(_sign_fpath)
            assert (
                _sign_fpath
                == (
                    e.data_output_dir / _sign[: DatasetStorage.short_sign_cnt]
                ).resolve()
            )

        src_data_path = (
            Path(self.object_store_dir) / data_files_sign[0][:2] / data_files_sign[0]
        )
        data_content = src_data_path.read_bytes()
        _parser = _header_struct.unpack(data_content[:_header_size])
        assert _parser[0] == _header_magic
        assert _parser[3] == 28 * 28
        assert _parser[6] == _data_magic
        assert len(data_content) == _header_size + _parser[3] + _parser[4]

        tdb = TabularDataset(name="mnist", version="112233", project="self")
        meta = list(tdb.scan(start=0, end=1))[0]
        assert meta.id == 0
        assert meta.data_offset == 32
        assert meta.extra_kw["_swds_bin_offset"] == 0
        assert meta.data_uri in data_files_sign
        assert meta.data_type["type"] == ArtifactType.Image.value
        assert meta.data_type["mime_type"] == MIMEType.GRAYSCALE.value
        assert meta.data_type["shape"] == [28, 28, 1]

        link_data_path = (
            Path(self.workdir)
            / "data"
            / data_files_sign[0][: DatasetStorage.short_sign_cnt]
        )
        assert link_data_path.exists()
        link_data_path.unlink()
        dummy_path = Path(self.workdir) / "dummy"
        ensure_file(dummy_path, "")
        link_data_path.symlink_to(dummy_path)
        assert link_data_path.exists()

        with MNISTBuildExecutor(
            dataset_name="mnist",
            dataset_version="112233",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()

        assert link_data_path.resolve() != dummy_path
        assert link_data_path.resolve() == src_data_path.resolve()


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


class TestDatasetSessionConsumption(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    def test_get_consumption(self, m_scan_id: MagicMock) -> None:
        m_scan_id.return_value = [{"id": 0}]
        os.environ["DATASET_CONSUMPTION_BATCH_SIZE"] = "10"
        consumption = get_dataset_consumption(
            dataset_uri="mnist/version/123", session_id="123"
        )
        assert isinstance(consumption, StandaloneTDSC)
        assert consumption.batch_size == 10
        assert len(local_standalone_tdsc) == 1

        consumption_another = get_dataset_consumption(
            dataset_uri="mnist/version/123", session_id="123"
        )
        assert consumption == consumption_another
        assert len(local_standalone_tdsc) == 1

        consumption_new = get_dataset_consumption(
            dataset_uri="mnist/version/456", session_id="456"
        )
        assert consumption != consumption_new
        assert len(local_standalone_tdsc) == 2

        os.environ[SWEnv.instance_uri] = "cloud://test"
        os.environ[SWEnv.instance_token] = "123"
        os.environ[ENV_POD_NAME] = "pod-1"
        consumption_cloud = get_dataset_consumption(
            dataset_uri="cloud://test/project/test/dataset/mnist/version/123",
            session_id="123",
        )
        assert consumption != consumption_cloud
        assert len(local_standalone_tdsc) == 2
        assert isinstance(consumption_cloud, CloudTDSC)
        assert consumption_cloud.instance_token == "123"

    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    def test_standalone_tdsc_multi_thread(self, m_scan_id: MagicMock) -> None:
        total = 1002
        batch_size = 10
        m_scan_id.return_value = [{"id": f"{i}-{i}"} for i in range(0, total)]

        def _do_task() -> t.Tuple:
            consumption = get_dataset_consumption(
                dataset_uri="mnist/version/thread",
                session_id="multi-thread-test",
                batch_size=batch_size,
            )

            r = []
            last_processed = None
            while True:
                rk = consumption.get_scan_range([last_processed])  # type: ignore
                if rk is None:
                    break

                time.sleep(0.01)

                r.append(rk)
                last_processed = rk
            return r, consumption

        pool = ThreadPoolExecutor(max_workers=5)
        tasks = [pool.submit(_do_task) for i in range(0, 4)]

        range_keys = []
        consumptions = []
        for task in as_completed(tasks):
            r = task.result()
            range_keys.append(r[0])
            consumptions.append(r[1])

        assert len(set(consumptions)) == 1
        assert len(range_keys) == 4
        for rk in range_keys:
            assert len(rk) != 0

        assert range_keys[0] != range_keys[1] != range_keys[2] != range_keys[3]
        merged_keys = sorted(sum(range_keys, []))
        assert len(merged_keys) == math.ceil(total / batch_size)
        assert ("0-0", "10-10") in merged_keys
        assert ("1000-1000", None) in merged_keys
        assert ("990-990", "1000-1000") in merged_keys

    @patch.dict(os.environ, {})
    @Mocker()
    def test_cloud_tdsc(self, rm: Mocker) -> None:
        with self.assertRaises(FieldTypeOrValueError):
            CloudTDSC(
                "", URI("mnist/version/latest", expected_type=URIType.DATASET), ""
            )

        os.environ[ENV_POD_NAME] = ""
        with self.assertRaises(RuntimeError):
            CloudTDSC(
                "",
                URI("mnist/version/latest", expected_type=URIType.DATASET),
                "",
                instance_token="1122",
            )

        os.environ[ENV_POD_NAME] = "pod-1"
        with self.assertRaises(FormatError):
            CloudTDSC(
                "",
                URI("mnist", expected_type=URIType.DATASET),
                "",
                instance_token="1122",
            )

        instance_uri = "http://1.1.1.1:8081"
        os.environ[SWEnv.instance_uri] = instance_uri
        os.environ[SWEnv.instance_token] = "123"
        os.environ[ENV_POD_NAME] = "pod-1"
        tdsc = get_dataset_consumption(
            dataset_uri="cloud://test/project/test/dataset/mnist/version/123",
            session_id="123",
        )
        tdsc_new = get_dataset_consumption(
            dataset_uri="cloud://test/project/test/dataset/mnist/version/123",
            session_id="123",
        )
        assert tdsc != tdsc_new
        assert tdsc.instance_uri == instance_uri  # type: ignore

        mock_request = rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8081/api/v1/project/test/dataset/mnist/version/123/nextRange",
            json={"data": {"start": "path/1", "end": "path/100"}},
        )

        range_key = tdsc.get_scan_range()
        assert range_key == ("path/1", "path/100")
        assert len(mock_request.request_history) == 1  # type: ignore
        request = mock_request.request_history[0]  # type: ignore
        assert (
            request.path == "/api/v1/project/test/dataset/mnist/version/123/nextrange"
        )
        assert request.json() == {
            "batchSize": 50,
            "maxRetries": 5,
            "sessionId": "123",
            "runEnv": "pod",
            "consumerId": "pod-1",
        }

        range_key = tdsc.get_scan_range(processed_keys=[(1, 1)])
        assert len(mock_request.request_history) == 2  # type: ignore
        assert range_key == ("path/1", "path/100")
        assert mock_request.request_history[1].json() == {  # type: ignore
            "batchSize": 50,
            "maxRetries": 5,
            "sessionId": "123",
            "runEnv": "pod",
            "consumerId": "pod-1",
            "processedData": [{"end": 1, "start": 1}],
        }

    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    def test_standalone_tdsc(self, m_scan_id: MagicMock) -> None:
        with self.assertRaises(FieldTypeOrValueError):
            StandaloneTDSC(
                dataset_uri=URI("mnist/version/123", expected_type=URIType.DATASET),
                session_id="1",
                batch_size=-1,
            )

        with self.assertRaises(NoSupportError):
            StandaloneTDSC(
                dataset_uri=URI(
                    "http://1.1.1.1:8082/project/starwhale/dataset/mnist/version/latest"
                ),
                session_id="1",
            )

        m_scan_id.return_value = [{"id": f"{i}-{i}"} for i in range(0, 102)]

        tdsc = StandaloneTDSC(
            dataset_uri=URI("mnist/version/123", expected_type=URIType.DATASET),
            session_id="1",
            batch_size=10,
        )
        current_tid = id(threading.current_thread())
        assert tdsc.consumer_id == f"thread-{current_tid}"
        assert tdsc._todo_queue.maxsize == 102
        assert tdsc._todo_queue.qsize() == 11

        rt_tasks = []
        for i in range(0, 11):
            task = tdsc._todo_queue.get()
            rt_tasks.append(task)
            tdsc._todo_queue.put(task)

        assert rt_tasks[0].start == "0-0"
        assert rt_tasks[0].end == "10-10"
        assert rt_tasks[1].start == "10-10"
        assert rt_tasks[1].end == "20-20"

        assert rt_tasks[-2].start == "90-90"
        assert rt_tasks[-2].end == "100-100"

        assert rt_tasks[-1].start == "100-100"
        assert rt_tasks[-1].end is None

        assert tdsc._todo_queue.qsize() == 11

        key_range = tdsc.get_scan_range()
        assert key_range == ("0-0", "10-10")
        assert tdsc.consumer_id in tdsc._doing_consumption
        assert len(tdsc._doing_consumption[tdsc.consumer_id]) == 1
        assert "0-0-10-10" in tdsc._doing_consumption[tdsc.consumer_id]

        key_range = tdsc.get_scan_range()
        assert key_range == ("10-10", "20-20")
        assert len(tdsc._doing_consumption[tdsc.consumer_id]) == 2
        assert "0-0-10-10" in tdsc._doing_consumption[tdsc.consumer_id]
        assert "10-10-20-20" in tdsc._doing_consumption[tdsc.consumer_id]

        key_range = tdsc.get_scan_range(processed_keys=[None, (), ("0-0", "10-10"), ("10-10", "20-20")])  # type: ignore
        assert key_range == ("20-20", "30-30")
        assert len(tdsc._doing_consumption[tdsc.consumer_id]) == 1
        assert "0-0-10-10" not in tdsc._doing_consumption[tdsc.consumer_id]
        assert "10-10-20-20" not in tdsc._doing_consumption[tdsc.consumer_id]
        assert "20-20-30-30" in tdsc._doing_consumption[tdsc.consumer_id]

        tdsc.get_scan_range(processed_keys=[("20-20", "30-30")])
        tdsc.get_scan_range()
        tdsc.get_scan_range()
        tdsc.get_scan_range()
        tdsc.get_scan_range(
            processed_keys=[
                ("30-30", "40-40"),
                ("40-40", "50-50"),
                ("50-50", "60-60"),
                ("60-60", "70-70"),
                ("0-0", "0-0"),
            ]
        )
        tdsc.get_scan_range(processed_keys=[("70-70", "80-80")])
        tdsc.get_scan_range(processed_keys=[("80-80", "90-90")])

        key_range = tdsc.get_scan_range(processed_keys=[("90-90", "100-100")])
        assert key_range == ("100-100", None)
        assert tdsc._todo_queue.qsize() == 0
        assert len(tdsc._doing_consumption[tdsc.consumer_id]) == 1
        assert "100-100-None" in tdsc._doing_consumption[tdsc.consumer_id]

        key_range = tdsc.get_scan_range(processed_keys=[("100-100", None)])
        assert key_range is None
        assert len(tdsc._doing_consumption) == 0

        key_range = tdsc.get_scan_range()
        assert key_range is None


class TestTabularDataset(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset")
    def test_tabular_dataset(self, m_ds_wrapper: MagicMock) -> None:
        m_ds_wrapper.return_value.scan.return_value = [
            TabularDatasetRow(
                id="path/1",
                data_uri="abcdef",
                data_format=DataFormatType.SWDS_BIN,
                annotations={"a": 1, "b": {"c": 1}},
                _append_seq_id=0,
            ).asdict(),
            TabularDatasetRow(
                id="path/2",
                data_uri="abcefg",
                annotations={"a": 2, "b": {"c": 2}},
                _append_seq_id=1,
            ).asdict(),
            TabularDatasetRow(
                id="path/3",
                data_uri="abcefg",
                annotations={"a": 2, "b": {"c": 2}},
                _append_seq_id=2,
            ).asdict(),
        ]
        with TabularDataset.from_uri(
            URI("mnist/version/123456", expected_type=URIType.DATASET)
        ) as td:
            rs = [i for i in td.scan()]
            assert len(rs) == 3
            assert rs[0].id == "path/1"
            assert isinstance(rs[0], TabularDatasetRow)
            assert rs[0].data_format == DataFormatType.SWDS_BIN

            last_append_seq_id, rows_cnt = td.fork("123")
            assert last_append_seq_id == 2
            assert rows_cnt == 3

        with self.assertRaises(InvalidObjectName):
            TabularDataset("", "", "")

        with self.assertRaises(FieldTypeOrValueError):
            TabularDataset("a123", "", "")

    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset")
    def test_standalone_tabular_dataset(self, m_ds_wrapper: MagicMock) -> None:
        m_ds_wrapper.return_value.scan.return_value = [
            TabularDatasetRow(
                id="path/1",
                data_uri="abcdef",
                data_format=DataFormatType.SWDS_BIN,
                annotations={"a": 1, "b": {"c": 1}},
                _append_seq_id=0,
            ).asdict(),
            TabularDatasetRow(
                id="path/2",
                data_uri="abcdef",
                data_format=DataFormatType.SWDS_BIN,
                annotations={"a": 1, "b": {"c": 1}},
                _append_seq_id=0,
            ).asdict(),
        ]

        with StandaloneTabularDataset.from_uri(
            URI("mnist/version/123456", expected_type=URIType.DATASET)
        ) as std:
            ensure_dir(std.store.snapshot_workdir)
            path = std.dump_meta(True)
            assert path.exists()

            with jsonlines.open(str(path), "r") as reader:
                lines = [line for line in reader]
                assert len(lines) == 2
                assert lines[0]["id"] == "path/1"
                assert lines[1]["id"] == "path/2"
                std.load_meta()

                m_put = m_ds_wrapper.return_value.put
                assert m_put.call_count == 2
                assert m_put.call_args_list[0][0][0] == "path/1"
                assert m_put.call_args_list[1][0][0] == "path/2"
                assert isinstance(m_put.call_args_list[0][1], dict)
                assert m_put.call_args_list[0][1]["id"] == "path/1"

    def test_row(self) -> None:
        s_row = TabularDatasetRow(id=0, data_uri="abcdef", annotations={"a": 1})
        data_type = {"type": "image", "shape": [1, 2, 3]}
        u_row = TabularDatasetRow(
            id="path/1",
            data_uri="abcdef",
            data_format=DataFormatType.USER_RAW,
            data_type=data_type,
            annotations={"a": 1, "b": {"c": 1}},
        )
        l_row = TabularDatasetRow(
            id="path/1",
            data_uri="s3://a/b/c",
            data_format=DataFormatType.USER_RAW,
            object_store_type=ObjectStoreType.REMOTE,
            annotations={"a": 1},
        )
        s2_row = TabularDatasetRow(
            id=0,
            data_uri="abcdef",
            data_origin=DataOriginType.INHERIT,
            annotations={"a": 1},
        )

        assert s_row == s2_row
        assert s_row != u_row
        assert s_row.asdict() == {
            "id": 0,
            "data_uri": "abcdef",
            "data_format": "swds_bin",
            "data_offset": 0,
            "data_size": 0,
            "data_origin": "+",
            "object_store_type": "local",
            "auth_name": "",
            "data_type": "{}",
            "_annotation_a": "1",
        }

        u_row_dict = u_row.asdict()
        assert u_row_dict["_annotation_a"] == "1"
        assert u_row_dict["_annotation_b"] == '{"c":1}'
        assert u_row_dict["data_type"] == json.dumps(data_type, separators=(",", ":"))
        assert l_row.asdict()["id"] == "path/1"

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id="", data_uri="")

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id=1.1, data_uri="")  # type: ignore

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id="1", data_uri="", annotations=[])  # type: ignore

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id=1, data_uri="")

        with self.assertRaises(NoSupportError):
            TabularDatasetRow(id="1", data_uri="123", annotations={"a": 1}, data_format="1")  # type: ignore

        with self.assertRaises(NoSupportError):
            TabularDatasetRow(id="1", data_uri="123", annotations={"a": 1}, data_origin="1")  # type: ignore

        with self.assertRaises(NoSupportError):
            TabularDatasetRow(id="1", data_uri="123", annotations={"a": 1}, object_store_type="1")  # type: ignore

        for r in (s_row, u_row, l_row):
            copy_r = TabularDatasetRow.from_datastore(**r.asdict())
            assert copy_r == r
