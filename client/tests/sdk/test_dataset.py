import io
import os
import copy
import json
import math
import time
import base64
import struct
import typing as t
import threading
from http import HTTPStatus
from types import TracebackType
from pathlib import Path
from unittest.mock import patch, MagicMock
from concurrent.futures import as_completed, ThreadPoolExecutor

import numpy
import numpy as np
import torch
import pytest
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.utils import config
from starwhale.consts import (
    HTTPMethod,
    ENV_POD_NAME,
    OBJECT_STORE_DIRNAME,
    DEFAULT_MANIFEST_NAME,
    ARCHIVED_SWDS_META_FNAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file, blake2b_file
from starwhale.base.type import URIType
from starwhale.consts.env import SWEnv
from starwhale.utils.error import (
    FormatError,
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.api._impl.wrapper import DatasetTableKind
from starwhale.core.dataset.copy import DatasetCopy
from starwhale.core.dataset.type import (
    Link,
    Text,
    Audio,
    Image,
    Video,
    Binary,
    JsonDict,
    MIMEType,
    ClassLabel,
    BoundingBox,
    NumpyBinary,
    ArtifactType,
    BoundingBox3D,
    GrayscaleImage,
    COCOObjectAnnotation,
)
from starwhale.core.dataset.store import DatasetStorage
from starwhale.api._impl.data_store import Link as DataStoreRawLink
from starwhale.api._impl.data_store import STRING, SwObject, _get_type, SwObjectType
from starwhale.core.dataset.tabular import (
    CloudTDSC,
    StandaloneTDSC,
    TabularDataset,
    TabularDatasetRow,
    TabularDatasetInfo,
    local_standalone_tdsc,
    get_dataset_consumption,
)
from starwhale.api._impl.dataset.loader import DataRow
from starwhale.api._impl.dataset.builder import (
    BinWriter,
    RowWriter,
    _data_magic,
    _header_size,
    _header_magic,
    BuildExecutor,
    _header_struct,
    create_generic_cls,
)

from .test_base import BaseTestCase

_mnist_dir = Path(f"{ROOT_DIR}/data/dataset/mnist")
_mnist_data_path = _mnist_dir / "data"
_mnist_label_path = _mnist_dir / "label"

_TGenItem = t.Generator[t.Tuple[t.Any, t.Any], None, None]


def iter_complex_annotations_swds() -> _TGenItem:
    for i in range(0, 15):
        coco = COCOObjectAnnotation(
            id=i,
            image_id=i,
            category_id=i,
            area=i * 10,
            bbox=BoundingBox(i, i, i + 1, i + 10),
            iscrowd=1,
        )
        coco.segmentation = [1, 2, 3, 4]
        data = {
            "index": i,
            "text": Text(f"data-{i}"),
            "label_str": f"label-{i}",
            "label_float": i + 0.00000092,
            "list_int": [j for j in range(0, i + 1)],
            "bytes": f"label-{i}".encode(),
            "link": DataStoreRawLink(str(i), f"display-{i}"),
            "seg": {
                "box": [1, 2, 3, 4],
                "mask": Image(
                    link=Link(
                        f"s3://admin:123@localhost:9000/users/{i}_mask.png",
                    ),
                    mime_type=MIMEType.PNG,
                    display_name=f"{i}_mask",
                    as_mask=True,
                ),
            },
            "bbox": BoundingBox(i, i, i + 1, i + 2),
            "list_bbox": [BoundingBox(j, j, i + 1, i + 2) for j in range(i + 2)],
            "coco": coco,
            "mask": Image(
                display_name=f"{i}_mask",
                mime_type=MIMEType.PNG,
                as_mask=True,
                link=Link(
                    f"s3://admin:123@localhost:9000/users/{i}_mask.png",
                ),
            ),
        }
        yield f"idx-{i}", data


def iter_mnist_swds_bin_item_with_id() -> _TGenItem:
    for data in iter_mnist_swds_bin_item():
        display_name = data["data"].display_name
        yield f"mnist-{display_name}", data


def iter_mnist_swds_bin_item() -> t.Generator[t.Dict[str, t.Any], None, None]:
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
            yield {
                "data": GrayscaleImage(
                    _data,
                    display_name=f"{i}",
                    shape=(height, width, 1),
                ),
                "label": _label,
            }


class MNISTBuildExecutor(BuildExecutor):
    def get_info(self) -> t.Optional[t.Dict[str, t.Any]]:
        return {
            "int": 1,
            "dict": {"a": 1, "b": 2},
            "list": [1, 2, 3],
            "list_dict": [{"a": 1}, {"b": 2}],
        }

    def iter_item(self) -> t.Generator[t.Dict[str, t.Any], None, None]:
        return iter_mnist_swds_bin_item()


class MNISTBuildWithIDExecutor(BuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        return iter_mnist_swds_bin_item_with_id()


def iter_mnist_user_raw_item_with_id() -> t.Generator[
    t.Tuple[t.Any, t.Any], None, None
]:
    for data in iter_mnist_user_raw_item():
        image_ = data["image"]
        yield f"mnist-link-{image_.display_name}", data


def iter_mnist_user_raw_item() -> t.Generator[t.Dict[str, t.Any], None, None]:
    with _mnist_data_path.open("rb") as data_file, _mnist_label_path.open(
        "rb"
    ) as label_file:
        _, data_number, height, width = struct.unpack(">IIII", data_file.read(16))
        _, label_number = struct.unpack(">II", label_file.read(8))

        image_size = height * width
        offset = 16

        for i in range(0, min(data_number, label_number)):
            _label = struct.unpack(">B", label_file.read(1))[0]
            _local_link = Link(
                uri=_mnist_label_path,
                with_local_fs_data=True,
            )
            yield {
                "image": GrayscaleImage(
                    display_name=f"{i}",
                    shape=(height, width, 1),
                    link=Link(
                        uri=str(_mnist_data_path.absolute()),
                        offset=offset,
                        size=image_size,
                        with_local_fs_data=True,
                    ),
                ),
                "original_data": Binary(fp=_mnist_data_path.absolute()),
                "label": _label,
                "link": _local_link,
                "list_link": [_local_link],
                "dict_link": {"key": _local_link},
            }
            offset += image_size


class TestDatasetCopy(BaseTestCase):
    def setUp(self) -> None:
        return super().setUp()

    @patch("os.environ", {})
    @Mocker()
    def test_upload(self, rm: Mocker) -> None:
        instance_uri = "http://1.1.1.1:8182"
        dataset_name = "complex_annotations"
        dataset_version = "123"
        cloud_project = "project"

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/project",
            json={"data": {"id": 1, "name": "project"}},
        )
        rm.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/version/{dataset_version}/file",
            json={"data": {"uploadId": 1}},
        )
        rm.request(
            HTTPMethod.HEAD,
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/version/{dataset_version}",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )

        m_update_req = rm.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/datastore/updateTable",
            status_code=HTTPStatus.OK,
        )

        _cls = create_generic_cls(iter_complex_annotations_swds)
        workdir = Path(self.local_storage, "user", "workdir")

        dataset_dir = (
            Path(self.local_storage)
            / "self"
            / "dataset"
            / dataset_name
            / dataset_version[:2]
            / f"{dataset_version}.swds"
        )
        ensure_dir(dataset_dir)
        ensure_file(dataset_dir / DEFAULT_MANIFEST_NAME, json.dumps({"signature": []}))
        ensure_file(dataset_dir / ARCHIVED_SWDS_META_FNAME, " ")

        project = "self"
        with _cls(
            dataset_name=dataset_name,
            dataset_version=dataset_version,
            project_name=project,
            workdir=workdir,
            alignment_bytes_size=16,
            volume_bytes_size=1000,
        ) as e:
            e.make_swds()

        os.environ[SWEnv.instance_token] = "1234"

        origin_conf = config.load_swcli_config().copy()
        # patch config to pass instance alias check
        with patch("starwhale.utils.config.load_swcli_config") as mock_conf:
            origin_conf.update(
                {
                    "current_instance": "local",
                    "instances": {
                        "foo": {"uri": "http://1.1.1.1:8182"},
                        "local": {"uri": "local", "current_project": "self"},
                    },
                }
            )
            mock_conf.return_value = origin_conf
            dc = DatasetCopy(
                src_uri=f"{dataset_name}/version/{dataset_version}",
                dest_uri=f"{instance_uri}/project/{cloud_project}",
                typ=URIType.DATASET,
            )
            dc.do()

        content = m_update_req.last_request.json()  # type: ignore
        assert {
            "type": "OBJECT",
            "attributes": [
                {"type": "STRING", "name": "_type"},
                {"type": "INT64", "name": "x"},
                {"type": "INT64", "name": "y"},
                {"type": "INT64", "name": "width"},
                {"type": "INT64", "name": "height"},
            ],
            "pythonType": "starwhale.core.dataset.type.BoundingBox",
            "name": "data/bbox",
        } in content["tableSchemaDesc"]["columnSchemaList"]

        assert {
            "type": "OBJECT",
            "attributes": [
                {"type": "LIST", "elementType": {"type": "INT64"}, "name": "box"},
                {
                    "attributes": [
                        {"name": "as_mask", "type": "BOOL"},
                        {"name": "mask_uri", "type": "STRING"},
                        {"name": "fp", "type": "STRING"},
                        {"name": "_BaseArtifact__cache_bytes", "type": "BYTES"},
                        {"name": "_type", "type": "STRING"},
                        {"name": "display_name", "type": "STRING"},
                        {"name": "_mime_type", "type": "STRING"},
                        {
                            "elementType": {"type": "INT64"},
                            "name": "shape",
                            "type": "LIST",
                        },
                        {"name": "_dtype_name", "type": "STRING"},
                        {"name": "encoding", "type": "STRING"},
                        {
                            "attributes": [
                                {"name": "_type", "type": "STRING"},
                                {"name": "owner", "type": "UNKNOWN"},
                                {"name": "uri", "type": "STRING"},
                                {"name": "scheme", "type": "STRING"},
                                {"name": "offset", "type": "INT64"},
                                {"name": "size", "type": "INT64"},
                                {"name": "data_type", "type": "UNKNOWN"},
                                {"name": "with_local_fs_data", "type": "BOOL"},
                                {"name": "_local_fs_uri", "type": "STRING"},
                                {"name": "_signed_uri", "type": "STRING"},
                                {
                                    "keyType": {"type": "UNKNOWN"},
                                    "name": "extra_info",
                                    "type": "MAP",
                                    "valueType": {"type": "UNKNOWN"},
                                },
                            ],
                            "name": "link",
                            "pythonType": "starwhale.core.dataset.type.Link",
                            "type": "OBJECT",
                        },
                        {"name": "owner", "type": "UNKNOWN"},
                    ],
                    "name": "mask",
                    "pythonType": "starwhale.core.dataset.type.Image",
                    "type": "OBJECT",
                },
            ],
            "pythonType": "starwhale.core.dataset.type.JsonDict",
            "name": "data/seg",
        } in content["tableSchemaDesc"]["columnSchemaList"]
        assert len(content["records"]) > 0

    @patch("os.environ", {})
    @Mocker()
    def test_download(self, rm: Mocker) -> None:
        instance_uri = "http://1.1.1.1:8182"
        dataset_name = "complex_annotations"
        dataset_version = "dataset-version"
        cloud_project = "project"

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/project",
            json={"data": {"id": 1, "name": "project"}},
        )
        rm.request(
            HTTPMethod.HEAD,
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/version/{dataset_version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/version/{dataset_version}/file",
            json={"signature": []},
        )

        rm.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/datastore/scanTable",
            additional_matcher=lambda x: "/info" in x.text,
            json={"data": {"records": []}},
        )

        rm.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/datastore/scanTable",
            additional_matcher=lambda x: "/meta" in x.text,
            json={
                "data": {
                    "columnTypes": [
                        {"type": "STRING", "name": "id"},
                        {
                            "name": "data/text",
                            "type": "OBJECT",
                            "pythonType": "starwhale.core.dataset.type.Text",
                            "attributes": [
                                {"name": "_BaseArtifact__cache_bytes", "type": "BYTES"},
                                {
                                    "name": "link",
                                    "type": "OBJECT",
                                    "pythonType": "starwhale.core.dataset.type.Link",
                                    "attributes": [
                                        {"name": "uri", "type": "STRING"},
                                        {"name": "offset", "type": "INT64"},
                                        {"name": "size", "type": "INT64"},
                                    ],
                                },
                            ],
                        },
                        {"type": "STRING", "name": "data_origin"},
                        {"type": "INT64", "name": "_append_seq_id"},
                        {
                            "type": "OBJECT",
                            "name": "data/bbox",
                            "attributes": [
                                {"type": "STRING", "name": "_type"},
                                {"type": "INT64", "name": "x"},
                                {"type": "INT64", "name": "y"},
                                {"type": "INT64", "name": "width"},
                                {"type": "INT64", "name": "height"},
                            ],
                            "pythonType": "starwhale.core.dataset.type.BoundingBox",
                        },
                    ],
                    "records": [
                        {
                            "id": "idx-0",
                            "data/text": {
                                "_BaseArtifact__cache_bytes": "",
                                "link": {
                                    "offset": "0000000000000080",
                                    "size": "0000000000000006",
                                    "uri": "111",
                                },
                            },
                            "data/bbox": {
                                "x": "0000000000000002",
                                "y": "0000000000000002",
                                "width": "0000000000000003",
                                "height": "0000000000000004",
                            },
                            "data_origin": "+",
                        }
                    ],
                }
            },
        )

        dataset_dir = (
            Path(self.local_storage)
            / "self"
            / "dataset"
            / dataset_name
            / dataset_version[:2]
            / f"{dataset_version}.swds"
        )

        assert not dataset_dir.exists()
        assert not (dataset_dir / DEFAULT_MANIFEST_NAME).exists()

        os.environ[SWEnv.instance_token] = "1234"
        origin_conf = config.load_swcli_config().copy()
        # patch config to pass instance alias check
        with patch("starwhale.utils.config.load_swcli_config") as mock_conf:
            origin_conf.update(
                {
                    "current_instance": "local",
                    "instances": {
                        "foo": {"uri": "http://1.1.1.1:8182"},
                        "local": {"uri": "local"},
                    },
                }
            )
            mock_conf.return_value = origin_conf
            dc = DatasetCopy(
                src_uri=f"{instance_uri}/project/{cloud_project}/dataset/{dataset_name}/version/{dataset_version}",
                dest_uri="",
                dest_local_project_uri="self",
                typ=URIType.DATASET,
            )
            dc.do()

        assert dataset_dir.exists()
        assert (dataset_dir / DEFAULT_MANIFEST_NAME).exists()

        tdb = TabularDataset(name=dataset_name, version=dataset_version, project="self")
        meta_list = list(tdb.scan())
        assert len(meta_list) == 1
        assert meta_list[0].id == "idx-0"
        assert meta_list[0].data["text"].link.uri == "111"
        bbox = meta_list[0].data["bbox"]
        assert isinstance(bbox, BoundingBox)
        assert bbox.x == 2 and bbox.y == 2
        assert bbox.width == 3 and bbox.height == 4


class MockBinWriter:
    def __init__(self) -> None:
        self.total_bin_size = 0

    def write_row(self, row: TabularDatasetRow) -> None:
        """
        Find large bytes or local fs file in row data. Convert them to accessible link
        """
        print("write_row")

    def flush(self) -> None:
        print("flush")

    def __enter__(self) -> BinWriter:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        print("exit")


class TestDatasetBuildExecutor(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

        self.object_store_dir = os.path.join(
            self.local_storage, OBJECT_STORE_DIRNAME, DatasetStorage.object_hash_algo
        )
        self.raw_data = os.path.join(self.local_storage, ".user", "data")
        self.workdir = os.path.join(self.local_storage, ".user", "workdir")
        self.data_file_sign = blake2b_file(_mnist_data_path)
        self.label_file_sign = blake2b_file(_mnist_label_path)

    def test_user_raw_with_id_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_user_raw_item_with_id)
        assert issubclass(_cls, BuildExecutor)
        with _cls(
            dataset_name="mnist",
            dataset_version="332211",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
            bin_writer=MockBinWriter(),
        ) as e:
            summary = e.make_swds()

        assert summary.rows == 10

    def test_user_raw_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_user_raw_item)
        assert issubclass(_cls, BuildExecutor)
        with _cls(
            dataset_name="mnist",
            dataset_version="332211",
            project_name="self",
            workdir=Path(self.workdir),
            alignment_bytes_size=64,
            volume_bytes_size=100,
        ) as e:
            summary = e.make_swds()
            scan = e.tabular_dataset.scan()
            for row in scan:
                assert isinstance(row.data.get("original_data"), Binary)
                assert not row.data.get("original_data").fp
                assert row.data.get("original_data").link.uri

        assert summary.rows == 10

    def test_swds_bin_with_id_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_swds_bin_item_with_id)
        assert issubclass(_cls, BuildExecutor)
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

    def test_swds_bin_function_handler(self) -> None:
        _cls = create_generic_cls(iter_mnist_swds_bin_item)
        assert issubclass(_cls, BuildExecutor)
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

    def test_abnormal_function_handler(self) -> None:
        non_generator_f = lambda: 1
        with self.assertRaises(RuntimeError):
            _cls = create_generic_cls(non_generator_f)  # type: ignore

        list_f = lambda: [{"d": b"1", "a": 1}, {"d": b"2", "a": 2}, {"d": b"2", "a": 2}]
        _cls = create_generic_cls(list_f)  # type: ignore
        assert issubclass(_cls, BuildExecutor)
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
            yield {"d": b"1", "a": 1}

        _cls = create_generic_cls(_gen_only_one)
        assert issubclass(_cls, BuildExecutor)
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

    def test_complex_annotation(self) -> None:
        _cls = create_generic_cls(iter_complex_annotations_swds)
        name = "complex_annotations"
        version = "123"
        project = "self"
        with _cls(
            dataset_name=name,
            dataset_version=version,
            project_name=project,
            workdir=Path(self.workdir),
            alignment_bytes_size=16,
            volume_bytes_size=1000,
        ) as e:
            summary = e.make_swds()

        assert summary.rows == 15
        tdb = TabularDataset(name=name, version=version, project=project)
        meta_list = list(tdb.scan("idx-0", "idx-1"))
        assert len(meta_list) > 0
        data = meta_list[0].data
        assert isinstance(data["link"], DataStoreRawLink)
        assert isinstance(data["coco"], COCOObjectAnnotation)
        assert data["coco"].bbox == [0, 0, 1, 10]
        assert isinstance(data["list_bbox"][0], BoundingBox)
        assert isinstance(data["mask"], Image)
        assert isinstance(data["mask"].link, Link)

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
        assert meta.data["data"].link.extra_info["bin_offset"] == 0
        assert meta.data["data"].link.offset == 32
        assert meta.data["data"].link.extra_info["bin_size"] == 864
        assert meta.data["data"].link.uri in data_files_sign
        assert meta.data["data"].type == ArtifactType.Image
        assert meta.data["data"].mime_type == MIMEType.GRAYSCALE

        assert list(tdb.info) == ["int", "dict", "list", "list_dict"]
        assert tdb.info["list_dict"] == [{"a": 1}, {"b": 2}]
        assert tdb.info["list"] == [1, 2, 3]
        assert tdb.info["dict"] == {"a": 1, "b": 2}

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

    def test_annotation_swobj(self) -> None:
        objs = [
            ClassLabel([1, 2, 3]),
            Binary(b"test"),
            Image(
                "path/to/file",
                display_name="t",
                shape=[28, 28, 3],
                mime_type=MIMEType.PNG,
            ),
            GrayscaleImage(Path("path/to/file"), shape=[28, 28, 1]),
            Audio("test/1.wav"),
            Video("test/1.avi"),
            BoundingBox(1, 2, 3, 4),
            Text("test"),
            Link(
                "path/to/file",
                with_local_fs_data=True,
                data_type=Image(display_name="image"),
            ),
            COCOObjectAnnotation(
                id=1,
                image_id=1,
                category_id=1,
                area=100,
                bbox=BoundingBox(1, 2, 3, 4),
                iscrowd=1,
            ),
        ]

        for obj in objs:
            typ = _get_type(obj)
            assert isinstance(typ, SwObjectType)
            assert typ.attrs["_type"] == STRING

    def test_binary(self) -> None:
        b = Binary(b"test")
        assert b.to_bytes() == b"test"
        assert b.astype() == {
            "type": ArtifactType.Binary.value,
            "mime_type": MIMEType.UNDEFINED.value,
            "encoding": "",
            "display_name": "",
        }

    def test_numpy_binary(self) -> None:
        np_array = np.array([[1.008, 6.94, 22.990], [39.098, 85.468, 132.91]])
        b = NumpyBinary(np_array.tobytes(), np_array.dtype, np_array.shape)
        assert b.to_bytes() == np_array.tobytes()
        np.testing.assert_array_equal(b.to_numpy(), np_array)
        assert torch.equal(torch.from_numpy(np_array), b.to_tensor())

    def test_image(self) -> None:
        fp = io.StringIO("test")
        img = Image(fp, display_name="t", shape=[28, 28, 3], mime_type=MIMEType.PNG)
        assert img.to_bytes() == b"test"
        _asdict = img.asdict()
        assert not _asdict["as_mask"]
        assert "fp" not in _asdict
        assert "_raw_base64_data" not in _asdict
        assert _asdict["_type"] == "image"
        assert _asdict["display_name"] == "t"
        assert _asdict["shape"] == [28, 28, 3]
        assert json.loads(json.dumps(_asdict))["_type"] == "image"

        with self.assertRaises(RuntimeError):
            _get_type(img)

        img = Image(
            "path/to/file", display_name="t", shape=[28, 28, 3], mime_type=MIMEType.PNG
        )
        typ = _get_type(img)
        assert isinstance(typ, SwObjectType)
        assert typ.attrs["mask_uri"] == STRING

        fp = io.BytesIO(b"test")
        img = GrayscaleImage(fp, shape=[28, 28, 1]).carry_raw_data()
        assert img.to_bytes() == b"test"
        _asdict = json.loads(json.dumps(img.asdict()))
        assert _asdict["_type"] == "image"
        assert _asdict["_mime_type"] == MIMEType.GRAYSCALE.value
        assert _asdict["shape"] == [28, 28, 1]
        assert _asdict["_raw_base64_data"] == base64.b64encode(b"test").decode()
        with self.assertRaises(RuntimeError):
            _get_type(img)

        self.fs.create_file("path/to/file", contents="")
        img = GrayscaleImage(Path("path/to/file"), shape=[28, 28, 1]).carry_raw_data()
        typ = _get_type(img)
        assert isinstance(typ, SwObjectType)
        assert typ.attrs["_raw_base64_data"] == STRING

    def test_audio(self) -> None:
        fp = "/test/1.wav"
        self.fs.create_file(fp, contents="test")
        audio = Audio(fp)
        _asdict = json.loads(json.dumps(audio.asdict()))
        assert _asdict["_mime_type"] == MIMEType.WAV.value
        assert _asdict["_type"] == "audio"
        assert audio.to_bytes() == b"test"
        typ = _get_type(audio)
        assert isinstance(typ, SwObjectType)

    def test_video(self) -> None:
        fp = "/test/1.avi"
        self.fs.create_file(fp, contents="test")
        video = Video(fp)
        _asdict = json.loads(json.dumps(video.asdict()))
        assert _asdict["_mime_type"] == MIMEType.AVI.value
        assert _asdict["_type"] == "video"
        assert video.to_bytes() == b"test"

    def test_bbox(self) -> None:
        bbox = BoundingBox(1, 2, 3, 4)
        assert bbox.to_list() == [1, 2, 3, 4]
        _asdict = json.loads(json.dumps(bbox.asdict()))
        assert _asdict["_type"] == "bounding_box"
        assert _asdict["x"] == 1
        assert _asdict["y"] == 2
        assert _asdict["width"] == 3
        assert _asdict["height"] == 4
        assert torch.equal(bbox.to_tensor(), torch.Tensor([1, 2, 3, 4]))
        _bout = bbox.to_bytes()
        assert isinstance(_bout, bytes)
        _array = numpy.frombuffer(_bout, dtype=numpy.float64)
        assert numpy.array_equal(_array, numpy.array([1, 2, 3, 4], dtype=numpy.float64))

    def test_bbox3d(self) -> None:
        bbox_a = BoundingBox(1, 2, 3, 4)
        bbox_b = BoundingBox(3, 4, 3, 4)
        bbox = BoundingBox3D(bbox_a, bbox_b)
        assert bbox.to_list() == [[1, 2, 3, 4], [3, 4, 3, 4]]
        _asdict = json.loads(json.dumps(bbox.asdict()))
        assert _asdict["_type"] == "bounding_box3D"
        assert _asdict["bbox_a"]["x"] == 1
        assert _asdict["bbox_a"]["y"] == 2
        assert _asdict["bbox_a"]["width"] == 3
        assert _asdict["bbox_a"]["height"] == 4
        assert _asdict["bbox_b"]["x"] == 3
        assert _asdict["bbox_b"]["y"] == 4
        assert _asdict["bbox_b"]["width"] == 3
        assert _asdict["bbox_b"]["height"] == 4
        assert torch.equal(bbox.to_tensor(), torch.Tensor([[1, 2, 3, 4], [3, 4, 3, 4]]))
        _bout = bbox.to_bytes()
        assert isinstance(_bout, bytes)
        _array = numpy.frombuffer(_bout, dtype=numpy.float64).reshape(
            BoundingBox3D.SHAPE
        )
        assert numpy.array_equal(
            _array, numpy.array([[1, 2, 3, 4], [3, 4, 3, 4]], dtype=numpy.float64)
        )

    def test_text(self) -> None:
        text = Text("test")
        _asdict = json.loads(json.dumps(text.asdict()))
        assert text.to_bytes() == b"test"
        assert "fp" not in _asdict
        assert _asdict["_content"] == "test"
        assert _asdict["_type"] == "text"
        assert _asdict["_mime_type"] == MIMEType.PLAIN.value
        assert text.to_str() == "test"

    def test_coco(self) -> None:
        coco = COCOObjectAnnotation(
            id=1,
            image_id=1,
            category_id=1,
            area=100,
            bbox=BoundingBox(1, 2, 3, 4),
            iscrowd=1,
        )
        polygon = ["1", "2", "3", "4"]
        assert coco.segmentation is None
        coco.segmentation = polygon
        _asdict = json.loads(json.dumps(coco.asdict()))
        assert _asdict["_type"] == "coco_object_annotation"
        assert coco.segmentation == coco._segmentation_polygon == polygon

        coco_dict = COCOObjectAnnotation(
            id=2,
            image_id=2,
            category_id=2,
            area=100,
            bbox=BoundingBox(1, 2, 3, 4),
            iscrowd=1,
        )
        rle = {"size": [100, 200], "counts": "abcd"}
        assert coco_dict.segmentation is None
        coco_dict.segmentation = rle
        _asdict = json.loads(json.dumps(coco.asdict()))
        assert _asdict["_type"] == "coco_object_annotation"
        assert coco_dict.segmentation == rle
        assert coco_dict._segmentation_rle_counts == rle["counts"]
        assert coco_dict._segmentation_rle_size == rle["size"]

        with self.assertRaises(FieldTypeOrValueError):
            coco = COCOObjectAnnotation(
                id=1,
                image_id=1,
                category_id=1,
                area=100,
                bbox=BoundingBox(1, 2, 3, 4),
                iscrowd=3,
            )

    def test_class_label(self) -> None:
        cl = ClassLabel([1, 2, 3])
        _asdict = json.loads(json.dumps(cl.asdict()))
        assert _asdict["_type"] == "class_label"
        assert _asdict["names"] == [1, 2, 3]

        cl = ClassLabel.from_num_classes(3)
        assert cl.names == [0, 1, 2]

        with self.assertRaises(FieldTypeOrValueError):
            ClassLabel.from_num_classes(0)

    @patch("starwhale.core.dataset.store.boto3.resource")
    def test_link_standalone(self, m_boto3: MagicMock) -> None:
        link = Link(
            uri="s3://minioadmin:minioadmin@10.131.0.1:9000/users/path/to/file",
            owner="mnist/version/latest",
            data_type=Image(display_name="test"),
        )
        as_type = link.astype()
        assert as_type["type"] == "link"
        assert as_type["data_type"]["type"] == ArtifactType.Image.value
        assert as_type["data_type"]["display_name"] == "test"
        raw_content = b"123"

        m_boto3.return_value = MagicMock(
            **{
                "Object.return_value": MagicMock(
                    **{
                        "get.return_value": {
                            "Body": MagicMock(**{"read.return_value": raw_content}),
                            "ContentLength": len(raw_content),
                        }
                    }
                )
            }
        )

        content = link.to_bytes()
        assert content == raw_content

        b = Binary(link=link)
        assert b.to_bytes() == raw_content

    @Mocker()
    def test_link_cloud(self, rm: Mocker) -> None:
        link = Link(
            uri="s3://minioadmin:minioadmin@10.131.0.1:9000/users/path/to/file",
            owner="http://127.0.0.1:8081/project/test/dataset/mnist/version/latest",
        )

        rm.request(
            HTTPMethod.POST,
            "http://127.0.0.1:8081/api/v1/project/test/dataset/mnist/version/latest/sign-links?expTimeMillis=60000",
            json={
                "data": {
                    "s3://minioadmin:minioadmin@10.131.0.1:9000/users/path/to/file": "http://127.0.0.1:9001/signed_url"
                }
            },
        )

        raw_content = b"123"

        rm.request(
            HTTPMethod.GET,
            "http://127.0.0.1:9001/signed_url",
            content=raw_content,
        )

        content = link.to_bytes()
        assert content == raw_content

        link2 = Link(uri="http://127.0.0.1:9001/signed_url")
        content = link2.to_bytes()
        assert content == raw_content


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

    @Mocker()
    @patch.dict(os.environ, {})
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
            "http://1.1.1.1:8081/api/v1/project/test/dataset/mnist/version/123/consume",
            json={"data": {"start": "path/1", "end": "path/100"}},
        )

        range_key = tdsc.get_scan_range()
        assert range_key == ("path/1", "path/100")
        assert len(mock_request.request_history) == 1  # type: ignore
        request = mock_request.request_history[0]  # type: ignore
        assert request.path == "/api/v1/project/test/dataset/mnist/version/123/consume"
        assert request.json() == {
            "batchSize": 50,
            "sessionId": "123",
            "consumerId": "pod-1",
        }

        range_key = tdsc.get_scan_range(processed_keys=[(1, 1)])
        assert len(mock_request.request_history) == 2  # type: ignore
        assert range_key == ("path/1", "path/100")
        assert mock_request.request_history[1].json() == {  # type: ignore
            "batchSize": 50,
            "sessionId": "123",
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


class TestTabularDatasetInfo(BaseTestCase):
    def test_dict_behavior(self) -> None:
        info = TabularDatasetInfo(
            {"int": 1, "list": [1, 2, 3], "dict": {"a": 1, "b": 2}}, kw_str="str"
        )

        assert list(info) == list(info.keys()) == ["int", "list", "dict", "kw_str"]
        assert list(info.values()) == [1, [1, 2, 3], {"a": 1, "b": 2}, "str"]
        assert info["int"] == 1
        info["int"] = 2
        assert info["int"] == 2
        del info["int"]
        assert list(info.keys()) == ["list", "dict", "kw_str"]

        info.update(a=1, b=2)
        assert info["a"] == 1
        assert info["b"] == 2

    def test_copy(self) -> None:
        src_info = TabularDatasetInfo(
            {"int": 1, "list": [1, 2, 3], "dict": {"a": 1, "b": 2}}, kw_str="str"
        )

        dest_info = copy.deepcopy(src_info)
        assert dest_info["dict"] == {"a": 1, "b": 2}
        assert id(dest_info["dict"]) != id(src_info["dict"])
        assert id(dest_info.data["dict"]) != id(src_info.data["dict"])

    def test_inner_json_dict(self) -> None:
        info = TabularDatasetInfo(
            {"int": 1, "dict": {"a": 1, "b": 2}, "list_dict": [{"a": 1}, {"a": 2}]}
        )
        assert info["int"] == 1
        assert info["dict"] == {"a": 1, "b": 2}
        assert info["list_dict"] == [{"a": 1}, {"a": 2}]

        assert isinstance(info.data["dict"], SwObject)
        assert isinstance(info.data["dict"], JsonDict)
        assert info.data["dict"].__dict__ == {"a": 1, "b": 2}
        assert isinstance(info.data["list_dict"], list)
        assert isinstance(info.data["list_dict"][0], JsonDict)
        assert info.data["list_dict"][0].__dict__ == {"a": 1}

    def test_exceptions(self) -> None:
        info = TabularDatasetInfo()
        assert not bool(info)
        assert list(info) == []

        with self.assertRaisesRegex(TypeError, "is not str type"):
            info[1] = "2"  # type: ignore

        with self.assertRaisesRegex(TypeError, "data is not dict type"):
            TabularDatasetInfo(1)

        with self.assertRaisesRegex(TypeError, "is not str type"):
            TabularDatasetInfo({1: "test"})

    def test_datastore(self) -> None:
        ds_wrapper = DatastoreWrapperDataset(
            "test",
            "self",
            kind=DatasetTableKind.INFO,
        )
        info = TabularDatasetInfo(
            {
                "int": 1,
                "dict": {"a": 1, "b": 2},
                "list": ["s", "a"],
                "list_dict": [{"a": 1}, {"a": 2}],
                "image": Image(),
            }
        )
        info.save_to_datastore(ds_wrapper)

        load_info = TabularDatasetInfo.load_from_datastore(ds_wrapper)
        assert list(load_info) == ["int", "dict", "list", "list_dict", "image"]
        assert load_info["int"] == 1
        assert load_info["dict"] == {"a": 1, "b": 2}
        assert load_info["list"] == ["s", "a"]
        assert load_info["list_dict"] == [{"a": 1}, {"a": 2}]
        assert isinstance(load_info["image"], Image)

    def test_tabular_dataset_property(self) -> None:
        td = TabularDataset(name="test", version="123", project="self")
        assert td._info is None
        assert isinstance(td.info, TabularDatasetInfo)
        assert not bool(td.info)
        assert list(td.info) == []
        assert td._info is not None
        assert not td._info_changed

        td.info = None
        assert not td._info_changed

        td.info = {"a": 1, "b": 2, "dict": {"k": 1}}
        assert td._info_changed
        assert list(td._info) == ["a", "b", "dict"]

        with self.assertRaisesRegex(TypeError, "is not dict type for info update"):
            td.info = 1  # type: ignore

        td.close()

        loaded_td = TabularDataset(name="test", version="123", project="self")
        assert loaded_td.info["a"] == 1
        assert list(loaded_td.info) == ["a", "b", "dict"]
        assert not loaded_td._info_changed
        loaded_td.close()


class TestTabularDataset(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan")
    def test_tabular_dataset(self, m_scan: MagicMock) -> None:
        rows = [
            TabularDatasetRow(
                id="path/1",
                data={
                    "bin": Binary(link=Link("abcdef")),
                    "a": 1,
                    "b": {"c": 1},
                },
                _append_seq_id=0,
            ).asdict(),
            TabularDatasetRow(
                id="path/2",
                data={
                    "l": Link("abcefg"),
                    "a": 2,
                    "b": {"c": 2},
                },
                _append_seq_id=1,
            ).asdict(),
            TabularDatasetRow(
                id="path/3",
                data={
                    "l": Link("abcefg"),
                    "a": 2,
                    "b": {"c": 2},
                },
                _append_seq_id=2,
            ).asdict(),
        ]

        m_scan.side_effect = [rows, rows, [{"id": 0, "data/value": 1}]]
        with TabularDataset.from_uri(
            URI("mnist/version/123456", expected_type=URIType.DATASET)
        ) as td:
            rs = [i for i in td.scan()]
            assert len(rs) == 3
            assert rs[0].id == "path/1"
            assert isinstance(rs[0], TabularDatasetRow)

            last_append_seq_id, rows_cnt = td.fork("123")
            assert last_append_seq_id == 2
            assert rows_cnt == 3

        with self.assertRaises(InvalidObjectName):
            TabularDataset("", "", "")

        with self.assertRaises(FieldTypeOrValueError):
            TabularDataset("a123", "", "")

    def test_row(self) -> None:
        s_row = TabularDatasetRow(
            id=0, data={"l": Image(link=Link("abcdef"), shape=[1, 2, 3]), "a": 1}
        )
        u_row = TabularDatasetRow(
            id="path/1",
            data={
                "l": Image(link=Link("abcdef"), shape=[1, 2, 3]),
                "a": 1,
                "b": {"c": 1},
            },
        )
        l_row = TabularDatasetRow(
            id="path/1",
            data={"l": Image(link=Link("s3://a/b/c"), shape=[1, 2, 3]), "a": 1},
        )
        s2_row = TabularDatasetRow(
            id=0, data={"l": Image(link=Link("abcdef"), shape=[1, 2, 3]), "a": 1}
        )

        assert s_row == s2_row
        assert s_row != u_row
        assert s_row.asdict() == {
            "id": 0,
            "data/l": Image(link=Link("abcdef"), shape=[1, 2, 3]),
            "data/a": 1,
            "data_origin": "+",
        }

        u_row_dict = u_row.asdict()
        assert u_row_dict["data/a"] == 1
        assert u_row_dict["data/b"] == JsonDict({"c": 1})
        assert l_row.asdict()["id"] == "path/1"

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id="", data=Link(""))

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id=1.1, data={"l": Link("")})  # type: ignore

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id="1", data=[])  # type: ignore

        for r in (s_row, u_row, l_row):
            copy_r = TabularDatasetRow.from_datastore(**r.asdict())
            assert copy_r == r


class TestRowWriter(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        self._original_cwd = os.getcwd()
        os.chdir(self.local_storage)

    def tearDown(self) -> None:
        if hasattr(self, "_original_cwd"):
            os.chdir(self._original_cwd)
        super().tearDown()

    @patch("starwhale.api._impl.dataset.builder.BuildExecutor.make_swds")
    def test_row_update(self, m_make_swds: MagicMock) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")

        assert rw._builder is None
        assert not rw.is_alive()
        assert rw._queue.empty()

        rw._builder = MagicMock()

        rw.update(DataRow(index=1, data={"data": Binary(b"test"), "label": 1}))
        first_builder = rw._builder
        assert rw._builder is not None
        assert rw._queue.qsize() == 1

        rw.update(DataRow(index=2, data={"data": Binary(b"test"), "label": 2}))
        second_builder = rw._builder
        assert first_builder == second_builder
        assert rw._queue.qsize() == 2

        rw._builder = None
        rw.update(DataRow(index=3, data={"data": Binary(b"test"), "label": 3}))
        assert rw._builder is not None
        assert rw.daemon
        assert isinstance(rw._builder, BuildExecutor)
        assert m_make_swds.call_count == 1

    @pytest.mark.filterwarnings("ignore::pytest.PytestUnhandledThreadExceptionWarning")
    @patch("starwhale.api._impl.dataset.builder.BuildExecutor.make_swds")
    def test_update_exception(self, m_make_swds: MagicMock) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")

        assert rw._raise_run_exception() is None
        rw._builder = MagicMock()
        rw.update(DataRow(index=1, data={"data": Binary(b"test"), "label": 1}))
        assert rw._run_exception is None

        rw._run_exception = ValueError("test")
        with self.assertRaises(threading.ThreadError):
            rw.update(DataRow(index=1, data={"data": Binary(b"test"), "label": 1}))

        rw._run_exception = None
        rw._builder = None
        m_make_swds.side_effect = TypeError("thread test")
        with self.assertRaises(threading.ThreadError):
            rw.update(DataRow(index=2, data={"data": Binary(b"test"), "label": 2}))
            rw.join()
            rw.update(DataRow(index=3, data={"data": Binary(b"test"), "label": 3}))

    def test_iter(self) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")
        rw._builder = MagicMock()
        size = 10
        for i in range(0, size):
            rw.update(DataRow(index=i, data={"data": Binary(b"test"), "label": i}))

        rw.update(None)  # type: ignore
        assert not rw.is_alive()
        assert rw._queue.qsize() == size + 1

        items = list(rw)
        assert len(items) == size
        assert items[0].index == 0
        assert items[9].index == 9

    def test_iter_block(self) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")
        rw._builder = MagicMock()
        rw.update(DataRow(index=1, data={"data": Binary(b"test"), "label": 1}))

        thread = threading.Thread(target=lambda: list(rw), daemon=True)
        thread.start()
        assert thread.is_alive()
        time.sleep(1)
        assert thread.is_alive()

        rw.update(None)  # type: ignore
        time.sleep(0.1)
        assert not thread.is_alive()

    def test_iter_none(self) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")
        rw._builder = MagicMock()
        size = 10
        for _ in range(0, size):
            rw.update(None)  # type: ignore

        assert rw._queue.qsize() == size
        assert len(list(rw)) == 0

    def test_iter_merge_none(self) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")
        rw._builder = MagicMock()
        size = 10
        for _ in range(0, size):
            rw.update(None)  # type: ignore

        rw.update(DataRow(index=1, data={"data": Binary(b"test"), "label": 1}))
        rw.update(None)  # type: ignore

        assert rw._queue.qsize() == size + 2
        items = list(rw)
        assert len(items) == 1
        assert items[0].index == 1

    def test_close(self) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")
        rw.update(DataRow(index=1, data={"data": Binary(b"test"), "label": 1}))
        rw.close()
        assert not rw.is_alive()

        with RowWriter(dataset_name="mnist", dataset_version="123456") as context_rw:
            context_rw.update(
                DataRow(index=1, data={"data": Binary(b"test"), "label": 1})
            )
        assert not rw.is_alive()

    def test_make_swds_bin(self) -> None:
        workdir = Path(self.local_storage) / ".user" / "workdir"

        assert not workdir.exists()
        rw = RowWriter(dataset_name="mnist", dataset_version="123456", workdir=workdir)
        assert rw._builder is None
        size = 100
        for i in range(0, size):
            rw.update(DataRow(index=i, data={"data": Binary(b"test"), "label": i}))
        rw.close()

        assert isinstance(rw._builder, BuildExecutor)
        assert rw._queue.qsize() == 0
        assert rw.summary.rows == size

        data_dir = workdir / "data"
        assert data_dir.exists()
        files = list(data_dir.iterdir())
        assert len(files) == 1
        assert files[0].is_symlink()

    def test_make_link(self) -> None:
        user_dir = Path(self.local_storage) / ".user"
        raw_data_file = user_dir / "data_file"
        raw_content = "123"
        ensure_dir(user_dir)
        ensure_file(raw_data_file, content=raw_content)

        workdir = user_dir / "workdir"
        assert not workdir.exists()
        rw = RowWriter(dataset_name="mnist", dataset_version="123456", workdir=workdir)
        assert rw._builder is None
        size = 100
        for i in range(0, size):
            rw.update(
                DataRow(
                    index=i,
                    data={"data": Link(uri="minio://1/1/1/"), "label": i, "label2": 2},
                )
            )
        rw.close()

        assert rw._queue.qsize() == 0
        assert rw.summary.rows == size

        data_dir = workdir / "data"
        assert data_dir.exists()
        files = list(data_dir.iterdir())
        assert len(files) == 0

    @patch("starwhale.api._impl.dataset.builder.BuildExecutor.make_swds")
    def test_append_swds_bin(self, m_make_swds: MagicMock) -> None:
        rw = RowWriter(
            dataset_name="mnist",
            dataset_version="123456",
            append=True,
            append_from_version="abcdefg",
        )
        assert isinstance(rw._builder, BuildExecutor)

    def test_flush(self) -> None:
        rw = RowWriter(dataset_name="mnist", dataset_version="123456")
        rw._builder = MagicMock()
        rw.flush()

        rw.update(DataRow(index=1, data={"data": Binary(b"test"), "label": 1}))
        thread = threading.Thread(target=rw.flush, daemon=True)
        thread.start()
        time.sleep(0.2)
        assert thread.is_alive()

        item = rw._queue.get(block=True)
        assert item.index == 1  # type: ignore
        rw.flush()
