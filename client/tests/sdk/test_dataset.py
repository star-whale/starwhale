import io
import os
import re
import copy
import json
import math
import time
import queue
import base64
import struct
import typing as t
import tempfile
import threading
from http import HTTPStatus
from pathlib import Path
from binascii import crc32
from unittest.mock import patch, MagicMock
from concurrent.futures import as_completed, ThreadPoolExecutor

import yaml
import numpy
import numpy as np
import torch
import pytest
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config
from starwhale.consts import HTTPMethod, ENV_POD_NAME, DEFAULT_MANIFEST_NAME
from starwhale.consts.env import SWEnv
from starwhale.utils.error import (
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.base.uri.project import Project
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.api._impl.wrapper import DatasetTableKind, _get_remote_project_id
from starwhale.base.uri.resource import Resource, ResourceType
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
    DatasetConfig,
    GrayscaleImage,
    COCOObjectAnnotation,
)
from starwhale.core.dataset.model import StandaloneDataset
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
from starwhale.api._impl.dataset.builder.mapping_builder import (
    RotatedBinWriter,
    MappingDatasetBuilder,
)

from .. import BaseTestCase

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


class TestDatasetCopy(BaseTestCase):
    @patch("os.environ", {})
    @Mocker()
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    def test_upload(self, rm: Mocker) -> None:
        instance_uri = "http://1.1.1.1:8182"
        dataset_name = "complex_annotations"
        cloud_project = "project"

        swds_config = DatasetConfig(
            name=dataset_name, handler=iter_complex_annotations_swds
        )
        dataset_uri = Resource(dataset_name, typ=ResourceType.dataset)
        sd = StandaloneDataset(dataset_uri)
        sd.build(config=swds_config)
        dataset_version = sd._version

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
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )

        m_update_req = rm.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/datastore/updateTable",
            status_code=HTTPStatus.OK,
            json={
                "code": "success",
                "message": "Success",
                "data": "fake revision",
            },
        )

        rm.register_uri(
            HTTPMethod.HEAD,
            re.compile(
                f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/hashedBlob/"
            ),
            status_code=HTTPStatus.NOT_FOUND,
        )

        upload_blob_req = rm.register_uri(
            HTTPMethod.POST,
            re.compile(
                f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/hashedBlob/"
            ),
            json={"data": "server_return_uri"},
        )

        os.environ[SWEnv.instance_token] = "1234"

        origin_conf = config.load_swcli_config().copy()
        # patch config to pass instance alias check
        with patch("starwhale.utils.config.load_swcli_config") as mock_conf:
            origin_conf.update(
                {
                    "current_instance": "local",
                    "instances": {
                        "foo": {"uri": "http://1.1.1.1:8182", "sw_token": "1234"},
                        "local": {"uri": "local", "current_project": "self"},
                    },
                }
            )
            mock_conf.return_value = origin_conf
            dc = DatasetCopy(
                src_uri=f"{dataset_name}/version/{dataset_version}",
                dest_uri=f"{instance_uri}/project/{cloud_project}",
                force=True,
            )
            dc.do()

        assert upload_blob_req.call_count == 1
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
            "name": "features/bbox",
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
                                {"name": "uri", "type": "STRING"},
                                {"name": "scheme", "type": "STRING"},
                                {"name": "offset", "type": "INT64"},
                                {"name": "size", "type": "INT64"},
                                {"name": "data_type", "type": "UNKNOWN"},
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
                    ],
                    "name": "mask",
                    "pythonType": "starwhale.core.dataset.type.Image",
                    "type": "OBJECT",
                },
            ],
            "pythonType": "starwhale.core.dataset.type.JsonDict",
            "name": "features/seg",
        } in content["tableSchemaDesc"]["columnSchemaList"]
        assert len(content["records"]) > 0

        for v in content["records"][0]["values"]:
            if v["key"] != "features/text":
                continue
            assert v["value"]["fp"] == ""

    @patch("os.environ", {})
    @Mocker()
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
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
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/hashedBlob/111",
        )

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}/version/{dataset_version}/tag",
            json={"data": ["t1", "t2"]},
        )

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/{cloud_project}/dataset/{dataset_name}?versionUrl={dataset_version}",
            json={
                "data": {"versionMeta": yaml.safe_dump({"version": dataset_version})}
            },
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
                            "name": "features/text",
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
                        {"type": "INT64", "name": "_append_seq_id"},
                        {
                            "type": "OBJECT",
                            "name": "features/bbox",
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
                            "id": {"value": "idx-0", "type": "STRING"},
                            "features/text": {
                                "type": "OBJECT",
                                "pythonType": "starwhale.core.dataset.type.Text",
                                "value": {
                                    "_BaseArtifact__cache_bytes": {
                                        "type": "STRING",
                                        "value": "",
                                    },
                                    "link": {
                                        "type": "OBJECT",
                                        "pythonType": "starwhale.core.dataset.type.Link",
                                        "value": {
                                            "offset": {
                                                "type": "INT64",
                                                "value": "0000000000000080",
                                            },
                                            "size": {
                                                "type": "INT64",
                                                "value": "0000000000000006",
                                            },
                                            "uri": {"type": "STRING", "value": "111"},
                                        },
                                    },
                                },
                            },
                            "features/bbox": {
                                "type": "OBJECT",
                                "pythonType": "starwhale.core.dataset.type.BoundingBox",
                                "value": {
                                    "x": {"type": "INT64", "value": "0000000000000001"},
                                    "y": {"type": "INT64", "value": "0000000000000002"},
                                    "width": {
                                        "type": "INT64",
                                        "value": "0000000000000003",
                                    },
                                    "height": {
                                        "type": "INT64",
                                        "value": "0000000000000004",
                                    },
                                },
                            },
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
                        "foo": {"uri": "http://1.1.1.1:8182", "sw_token": "token"},
                        "local": {"uri": "local"},
                    },
                }
            )
            mock_conf.return_value = origin_conf
            dc = DatasetCopy(
                src_uri=Resource(
                    f"{instance_uri}/project/{cloud_project}/dataset/{dataset_name}/version/{dataset_version}",
                ),
                dest_uri="",
                dest_local_project_uri="self",
            )
            dc.do()

        assert dataset_dir.exists()
        assert (dataset_dir / DEFAULT_MANIFEST_NAME).exists()

        tdb = TabularDataset(name=dataset_name, project="self")
        meta_list = list(tdb.scan())
        assert len(meta_list) == 1
        assert meta_list[0].id == "idx-0"
        assert meta_list[0].features["text"].link.uri == "111"
        bbox = meta_list[0].features["bbox"]
        assert isinstance(bbox, BoundingBox)
        assert bbox.x == 1 and bbox.y == 2
        assert bbox.width == 3 and bbox.height == 4


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
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
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
    @patch("starwhale.utils.config.load_swcli_config")
    def test_link_cloud(self, rm: Mocker, m_conf: MagicMock) -> None:
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "foo": {
                    "uri": "http://127.0.0.1:8081",
                    "current_project": "test",
                    "sw_token": "token",
                },
            },
            "storage": {"root": "/root"},
        }

        link = Link(
            uri="s3://minioadmin:minioadmin@10.131.0.1:9000/users/path/to/file",
        )

        rm.request(
            HTTPMethod.GET,
            "http://127.0.0.1:8081/api/v1/project/test/dataset/mnist",
            json={"data": {"id": 1, "versionName": "123456a", "versionId": 100}},
        )
        link.owner = Resource(
            "http://127.0.0.1:8081/project/test/dataset/mnist/version/latest"
        )

        rm.request(
            HTTPMethod.POST,
            "http://127.0.0.1:8081/api/v1/project/test/dataset/mnist/uri/sign-links?expTimeMillis=86400000",
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
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    def test_get_consumption(self, m_scan_id: MagicMock, m_conf: MagicMock) -> None:
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "self"},
                "test": {
                    "uri": "http://127.0.0.1:8081",
                    "current_project": "test",
                    "sw_token": "123",
                },
            },
            "storage": {"root": "/root"},
        }
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
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
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
    @patch("starwhale.utils.config.load_swcli_config")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    def test_cloud_tdsc(self, rm: Mocker, m_conf: MagicMock) -> None:
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "test": {
                    "uri": "http://1.1.1.1:8081",
                    "current_project": "p",
                    "sw_token": "token",
                },
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": "/root"},
        }

        os.environ[ENV_POD_NAME] = ""
        with self.assertRaises(RuntimeError):
            CloudTDSC(
                Resource(
                    "mnist/version/latest",
                    typ=ResourceType.dataset,
                    project=Project("cloud://test/project/p"),
                ),
                "",
            )

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
        assert tdsc.instance_uri == "http://1.1.1.1:8081"  # type: ignore

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

    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.core.dataset.tabular.DatastoreWrapperDataset.scan_id")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    def test_standalone_tdsc(self, m_scan_id: MagicMock, m_conf: MagicMock) -> None:
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "test": {
                    "uri": "http://1.1.1.1:8082",
                    "current_project": "p",
                    "sw_token": "token",
                },
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": "/root"},
        }

        with self.assertRaises(FieldTypeOrValueError):
            StandaloneTDSC(
                dataset_uri=Resource(
                    "mnist/version/123",
                    typ=ResourceType.dataset,
                ),
                session_id="1",
                batch_size=-1,
            )

        with self.assertRaises(NoSupportError):
            StandaloneTDSC(
                dataset_uri=Resource(
                    "http://1.1.1.1:8082/project/starwhale/dataset/mnist/version/latest",
                ),
                session_id="1",
            )

        m_scan_id.return_value = [{"id": f"{i}-{i}"} for i in range(0, 102)]

        tdsc = StandaloneTDSC(
            dataset_uri=Resource(
                "mnist/version/123",
                typ=ResourceType.dataset,
            ),
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
        td = TabularDataset(name="test", project="self")
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

        loaded_td = TabularDataset(name="test", project="self")
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
                features={
                    "bin": Binary(link=Link("abcdef")),
                    "a": 1,
                    "b": {"c": 1},
                },
                _append_seq_id=0,
            ).asdict(),
            TabularDatasetRow(
                id="path/2",
                features={
                    "l": Link("abcefg"),
                    "a": 2,
                    "b": {"c": 2},
                },
                _append_seq_id=1,
            ).asdict(),
            TabularDatasetRow(
                id="path/3",
                features={
                    "l": Link("abcefg"),
                    "a": 2,
                    "b": {"c": 2},
                },
                _append_seq_id=2,
            ).asdict(),
        ]

        m_scan.side_effect = [rows, rows, [{"id": 0, "features/value": 1}]]
        with TabularDataset.from_uri(
            Resource(
                "mnist/version/123456",
                typ=ResourceType.dataset,
            )
        ) as td:
            rs = [i for i in td.scan()]
            assert len(rs) == 3
            assert rs[0].id == "path/1"
            assert isinstance(rs[0], TabularDatasetRow)

        with self.assertRaises(InvalidObjectName):
            TabularDataset(name="", project="")

        with self.assertRaisesRegex(RuntimeError, "project is not set"):
            TabularDataset(name="a123", project="")

    def test_encode_decode_feature_types(self) -> None:
        raw_features = {
            "str": "abc",
            "large_str": "abc" * 1000,
            "bytes": b"abc",
            "large_bytes": b"abc" * 1000,
        }
        row = TabularDatasetRow(
            id=0,
            features=raw_features,
        )
        row.encode_feature_types()

        assert row.features["str"] == raw_features["str"]
        assert isinstance(row.features["large_str"], Text)
        assert row.features["large_str"].content == raw_features["large_str"]
        assert row.features["bytes"] == raw_features["bytes"]
        assert row.features["large_bytes"].to_bytes() == raw_features["large_bytes"]
        assert isinstance(row.features["large_bytes"], Binary)

        row.decode_feature_types()
        assert row.features["str"] == raw_features["str"]
        assert row.features["large_str"] == raw_features["large_str"]
        assert row.features["bytes"] == raw_features["bytes"]
        assert row.features["large_bytes"] == raw_features["large_bytes"]

    def test_row(self) -> None:
        s_row = TabularDatasetRow(
            id=0, features={"l": Image(link=Link("abcdef"), shape=[1, 2, 3]), "a": 1}
        )
        u_row = TabularDatasetRow(
            id="path/1",
            features={
                "l": Image(link=Link("abcdef"), shape=[1, 2, 3]),
                "a": 1,
                "b": {"c": 1},
            },
        )
        l_row = TabularDatasetRow(
            id="path/1",
            features={"l": Image(link=Link("s3://a/b/c"), shape=[1, 2, 3]), "a": 1},
        )
        s2_row = TabularDatasetRow(
            id=0, features={"l": Image(link=Link("abcdef"), shape=[1, 2, 3]), "a": 1}
        )

        assert s_row == s2_row
        assert s_row != u_row
        assert s_row.asdict() == {
            "id": 0,
            "features/l": Image(link=Link("abcdef"), shape=[1, 2, 3]),
            "features/a": 1,
        }

        u_row_dict = u_row.asdict()
        assert u_row_dict["features/a"] == 1
        assert u_row_dict["features/b"] == JsonDict({"c": 1})
        assert l_row.asdict()["id"] == "path/1"

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id="", features=Link(""))

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id=1.1, features={"l": Link("")})  # type: ignore

        with self.assertRaises(FieldTypeOrValueError):
            TabularDatasetRow(id="1", features=[])  # type: ignore

        for r in (s_row, u_row, l_row):
            copy_r = TabularDatasetRow.from_datastore(**r.asdict())
            assert copy_r == r


class TestRotatedBinWriter(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def test_bin_format(self) -> None:
        workdir = Path("/home/test")
        content = b"123456"
        alignment_size = 64

        with RotatedBinWriter(workdir, alignment_bytes_size=alignment_size) as w:
            w.write(content)

        bin_path = list(workdir.iterdir())[0]
        bin_content = bin_path.read_bytes()
        assert len(bin_content) == alignment_size

        groups = struct.unpack(">IIQIIII", bin_content[0:32])
        assert len(groups) == 7
        assert struct.pack(">I", groups[0]) == b"SWDS"  # header magic
        assert struct.pack(">I", groups[-1]) == b"SDWS"  # data magic
        assert groups[1] == crc32(content)  # data crc32
        assert groups[2] == 0  # reserved
        assert groups[3] == len(content)  # size
        assert groups[4] == alignment_size - len(content) - 32  # padding size
        assert groups[5] == 0  # header version

        c_start, c_end = 32, 32 + len(content)
        assert bin_content[c_start:c_end] == content
        assert bin_content[c_end:] == b"\0" * groups[4]

    def test_write_one_bin(self) -> None:
        workdir = Path("/home/test")
        content = b"abcdef"

        assert not workdir.exists()

        rbw = RotatedBinWriter(workdir, alignment_bytes_size=1, volume_bytes_size=100)
        assert not rbw._current_writer.closed
        bin_path, bin_section = rbw.write(content)
        assert rbw._wrote_size == bin_section.size
        assert rbw.working_path == bin_path
        rbw.close()

        assert bin_section.offset == 0
        assert bin_section.size == len(content) + RotatedBinWriter._header_size
        assert bin_section.raw_data_offset == RotatedBinWriter._header_size
        assert bin_section.raw_data_size == len(content)

        assert rbw.working_path != bin_path
        assert rbw._wrote_size == 0

        assert workdir.exists()
        assert bin_path.exists()
        assert rbw.rotated_paths == [bin_path] == list(workdir.iterdir())
        assert bin_path.parent == workdir
        assert rbw._current_writer.closed

    def test_write_multi_bins(self) -> None:
        workdir = Path("/home/test")
        rbw = RotatedBinWriter(workdir, alignment_bytes_size=1, volume_bytes_size=1)
        cnt = 10
        for _ in range(0, cnt):
            rbw.write(b"\0")
        rbw.close()
        assert len(rbw.rotated_paths) == cnt
        assert set(rbw.rotated_paths) == set(workdir.iterdir())
        assert rbw.working_path not in rbw.rotated_paths

    def test_notify(self) -> None:
        notify_queue = queue.Queue()

        cnt = 10
        assert notify_queue.qsize() == 0
        with RotatedBinWriter(
            Path("/home/test"),
            alignment_bytes_size=1,
            volume_bytes_size=1,
            rotated_bin_notify_queue=notify_queue,
        ) as w:
            for i in range(0, cnt):
                w.write(b"\0")
                assert notify_queue.qsize() == i + 1

        queue_paths = [notify_queue.get() for _ in range(0, cnt)]
        assert queue_paths == w.rotated_paths

    def test_close(self) -> None:
        rbw = RotatedBinWriter(Path("/home/test"))
        rbw.close()

        rbw = RotatedBinWriter(Path("/home/test"))
        rbw.write(b"123")
        rbw.close()

        assert rbw._current_writer.closed
        with self.assertRaisesRegex(ValueError, "I/O operation on closed file"):
            rbw.close()

    def test_alignment(self) -> None:
        class _M(t.NamedTuple):
            content_size: int
            alignment_size: int
            expected_bin_size: int

        cases = [
            _M(0, 1, 32),
            _M(0, 31, 62),
            _M(0, 64, 64),
            _M(1, 1, 33),
            _M(3, 1, 35),
            _M(32, 1, 64),
            _M(32, 63, 126),
            _M(32, 64, 64),
            _M(32, 32, 64),
            _M(32, 65, 65),
            _M(16, 16, 48),
            _M(16, 4096, 4096),
        ]

        for index, meta in enumerate(cases):
            workdir = Path(f"/home/test/{index}")
            with RotatedBinWriter(
                workdir, alignment_bytes_size=meta.alignment_size
            ) as w:
                w.write(b"\0" * meta.content_size)

            self.assertEqual(
                list(workdir.iterdir())[0].stat().st_size,
                meta.expected_bin_size,
                msg=f"content:{meta.content_size}, alignment:{meta.alignment_size}, expected:{meta.expected_bin_size}",
            )

        with self.assertRaisesRegex(
            ValueError, "alignment_bytes_size must be greater than zero"
        ):
            RotatedBinWriter(Path("."), alignment_bytes_size=0)


class TestMappingDatasetBuilder(BaseTestCase):
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    def setUp(self) -> None:
        super().setUp()
        self.workdir = Path(self.local_storage) / "test"
        self.project_name = "self"
        self.dataset_name = "mnist"
        self.holder_dataset_version = "_current"
        with patch("starwhale.utils.config.load_swcli_config") as mock_conf:
            mock_conf.return_value = {
                "current_instance": "local",
                "instances": {
                    "foo": {"uri": "http://1.1.1.1:8182", "sw_token": "token"},
                    "local": {"uri": "local"},
                },
            }
            self.uri = Resource(
                "mnist", project=Project("self"), typ=ResourceType.dataset
            )

        self.tdb = TabularDataset(
            name=self.dataset_name,
            project=self.project_name,
        )

    def tearDown(self) -> None:
        super().tearDown()
        _get_remote_project_id.cache_clear()

    def test_put(self) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        mdb.put(DataRow(index=1, features={"label": 1}))
        mdb.put(DataRow(index=1, features={"label-another": 2}))
        mdb.close()

        assert len(mdb.signature_bins_meta) == 0
        assert mdb._abs_queue.qsize() == 0
        assert mdb._rows_put_queue.qsize() == 0

        rows = list(self.tdb.scan())
        assert rows[0].id == 1
        assert rows[0].features == {"label": 1, "label-another": 2}

    def test_put_swds_artifacts(self) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        mdb.put(DataRow(index=1, features={"bin": Binary(b"123")}))
        fpath = Path(self.workdir / "test.bin")
        fpath.write_bytes(b"abc")
        mdb.put(DataRow(index=2, features={"bin": Binary(fpath)}))

        assert mdb._abs_thread.is_alive()
        assert mdb._rows_put_thread.is_alive()

        mdb.put(DataRow(index=3, features={"bin": Binary(str(fpath))}))
        mdb.put(DataRow(index=4, features={"bin": Binary(io.BytesIO(b"000"))}))

        assert mdb._artifact_bin_tmpdir.exists()

        mdb.close()

        assert not mdb._artifact_bin_tmpdir.exists()

        rows = list(self.tdb.scan())
        assert len(rows) == 4
        uris = list({r.features["bin"].link.uri for r in rows})
        assert len(uris) == 1
        sign_meta_list = mdb.signature_bins_meta
        assert len(sign_meta_list) == 1
        assert uris[0] == sign_meta_list[0].name

        stored_bin_path = (
            Path(self.local_storage)
            / ".objectstore"
            / "blake2b"
            / uris[0][:2]
            / uris[0]
        )
        assert stored_bin_path.exists()
        assert stored_bin_path.stat().st_size == sign_meta_list[0].size
        assert sign_meta_list[0].algo == "blake2b"

        assert not mdb._abs_thread.is_alive()
        assert not mdb._rows_put_thread.is_alive()
        assert mdb._abs_queue.qsize() == 0
        assert mdb._rows_put_queue.qsize() == 0
        assert len(mdb._stash_uri_rows_map) == 0

    def test_put_link_artifacts(self) -> None:
        uri = "s3://1.1.1.1/dataset/mnist/t10k-ubyte"
        with MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        ) as mdb:
            mdb.put(
                DataRow(
                    index=1,
                    features={"image": Image(link=Link(uri, offset=0, size=784))},
                )
            )

        assert len(mdb.signature_bins_meta) == 0
        rows = list(self.tdb.scan())
        assert len(rows) == 1
        image = rows[0].features["image"]
        assert image.link.uri == uri
        assert image.link.offset == 0
        assert image.link.size == 784

    def test_put_mixed_artifacts(self) -> None:
        uri = "s3://1.1.1.1/dataset/mnist/t10k-ubyte"
        with MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        ) as mdb:
            mdb.put(
                DataRow(
                    index=1,
                    features={"image": Image(link=Link(uri, offset=0, size=784))},
                )
            )

            mdb.put(DataRow(index=2, features={"image": Image(b"\0")}))

        assert len(mdb.signature_bins_meta) == 1
        rows = list(self.tdb.scan())
        assert len(rows) == 2
        assert rows[0].features["image"].link.uri == uri
        assert rows[1].features["image"].link.uri == mdb.signature_bins_meta[0].name

    @pytest.mark.filterwarnings("ignore::pytest.PytestUnhandledThreadExceptionWarning")
    def test_put_raise_exception(self) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        mdb.put(DataRow(index=5, features={"bin": Binary(1)}))  # type: ignore
        mdb.flush()
        exception_msg = (
            "RowPutThread raise exception: no support fp type for bin writer"
        )
        with self.assertRaisesRegex(threading.ThreadError, exception_msg):
            mdb.put(DataRow(index=5, features={}))

        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        mdb.put(DataRow(index=5, features={"bin": Binary(1)}))  # type: ignore
        with self.assertRaisesRegex(threading.ThreadError, exception_msg):
            mdb.close()

    def test_delete(self) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        for i in range(0, 10):
            mdb.put(DataRow(index=i, features={"label": i}))

        mdb.flush()
        for i in range(0, 9):
            mdb.delete(i)

        mdb.close()
        rows = list(self.tdb.scan())
        assert len(rows) == 1
        assert rows[0].id == 9

    def test_close(self) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        for i in range(0, 10):
            mdb.put(DataRow(index=i, features={"label": i}))

        mdb.close()
        assert mdb._abs_queue.qsize() == mdb._rows_put_queue.qsize() == 0
        assert not mdb._abs_thread.is_alive()
        assert not mdb._rows_put_thread.is_alive()
        assert mdb._artifact_bin_writer._current_writer.closed

        mdb.close()

    def test_flush_artifacts(self) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
            blob_volume_bytes_size=1024 * 1024,
        )
        mdb.put(DataRow(index=1, features={"bin": Binary(b"123")}))
        mdb.flush(artifacts_flush=False)

        mdb.put(DataRow(index=2, features={"bin": Binary(b"123")}))
        mdb.flush(artifacts_flush=True)

        mdb.put(DataRow(index=3, features={"bin": Binary(b"123")}))
        mdb.put(DataRow(index=4, features={"bin": Binary(b"123")}))
        mdb.flush(artifacts_flush=True)

        mdb.close()

        assert len(mdb.signature_bins_meta) == 2

    def test_close_empty(self) -> None:
        MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        ).close()

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_put_for_cloud(self, rm: Mocker, m_conf: MagicMock) -> None:
        instance_uri = "http://1.1.1.1"
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "self"},
                "foo": {
                    "uri": instance_uri,
                    "current_project": "test",
                    "sw_token": "token",
                },
            },
            "storage": {"root": tempfile.gettempdir()},
        }
        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/{self.project_name}",
            json={"data": {"id": 1, "name": "project"}},
        )
        update_req = rm.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/datastore/updateTable",
            json={
                "code": "success",
                "message": "Success",
                "data": "fake revision",
            },
        )

        server_return_uri = "__server-uri-path__"
        upload_req = rm.register_uri(
            HTTPMethod.POST,
            re.compile(
                f"{instance_uri}/api/v1/project/{self.project_name}/dataset/{self.dataset_name}/hashedBlob/",
            ),
            json={"data": server_return_uri},
        )

        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=Resource(
                "mnist",
                project=Project("cloud://foo/project/self"),
                typ=ResourceType.dataset,
                refine=False,
            ),
        )
        mdb.put(
            DataRow(
                index=1,
                features={
                    "bin": Binary(link=Link(uri="s3://1.1.1.1/a/b/c", offset=1, size=1))
                },
            )
        )
        mdb.flush()

        mdb.put(DataRow(index=1, features={"bin": Binary(b"abc")}))
        mdb.flush()

        mdb.put(DataRow(index=1, features={"label": 1}))
        mdb.close()

        assert update_req.called
        assert upload_req.call_count == 1

        assert mdb._signed_bins_meta[0].name == server_return_uri
        assert any(
            [
                server_return_uri in history.text
                for history in update_req.request_history
            ]
        )
