import io
import json
import base64
import inspect
from pathlib import Path
from unittest.mock import patch, MagicMock

import numpy
import numpy as np
import torch
from PIL import Image as PILImage
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import HTTPMethod
from starwhale.api._impl import data_store
from starwhale.utils.error import FieldTypeOrValueError
from starwhale.base.data_type import (
    Line,
    Link,
    Text,
    Audio,
    Image,
    Point,
    Video,
    Binary,
    Polygon,
    JsonDict,
    MIMEType,
    Sequence,
    ClassLabel,
    BoundingBox,
    NumpyBinary,
    ArtifactType,
    BoundingBox3D,
    GrayscaleImage,
    COCOObjectAnnotation,
)
from starwhale.base.uri.instance import Instance


class TestDataType(TestCase):
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
            Sequence([]),
            Sequence((1, "2", 3.0)),
            Sequence(
                [
                    1,
                    "2",
                    3.0,
                    ["a", "b", "c"],
                    Sequence([1, "b", {"a": 1, "b": "str"}]),
                ]
            ),
        ]

        for obj in objs:
            typ = data_store._get_type(obj)
            assert isinstance(typ, data_store.SwObjectType)
            assert typ.attrs["_type"] == data_store.STRING

    def test_binary(self) -> None:
        b = Binary(b"test")
        assert b.to_bytes() == b"test"
        assert b.astype() == {
            "type": ArtifactType.Binary.value,
            "mime_type": MIMEType.UNDEFINED.value,
            "encoding": "",
            "display_name": "",
        }

    def test_sequence(self) -> None:
        raw_data = [
            1,
            "2",
            3.0,
            ["a", "b", "c"],
            Sequence([1, "b", {"a": 1, "b": "str"}]),
        ]
        s = Sequence(raw_data)
        assert str(s)
        assert repr(s)
        assert len(s) == 5
        assert bool(s)

        assert s.sequence_type == "list"
        assert s.to_raw_data() == raw_data

        s = Sequence([])
        assert not bool(s)
        assert len(s) == 0
        assert s.to_raw_data() == []

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
        t = data_store._get_type(img)
        assert t.raw_type == Image
        assert t.name == "object"

        img = Image(
            "path/to/file", display_name="t", shape=[28, 28, 3], mime_type=MIMEType.PNG
        )
        typ = data_store._get_type(img)
        assert isinstance(typ, data_store.SwObjectType)
        assert typ.attrs["mask_uri"] == data_store.STRING

        fp = io.BytesIO(b"test")
        img = GrayscaleImage(fp, shape=[28, 28, 1]).carry_raw_data()
        assert img.to_bytes() == b"test"
        _asdict = json.loads(json.dumps(img.asdict()))
        assert _asdict["_type"] == "image"
        assert _asdict["_mime_type"] == MIMEType.GRAYSCALE.value
        assert _asdict["shape"] == [28, 28, 1]
        assert _asdict["_raw_base64_data"] == base64.b64encode(b"test").decode()

        self.fs.create_file("path/to/file", contents="")
        img = GrayscaleImage(Path("path/to/file"), shape=[28, 28, 1]).carry_raw_data()
        typ = data_store._get_type(img)
        assert isinstance(typ, data_store.SwObjectType)
        assert typ.attrs["_raw_base64_data"] == data_store.STRING

        pixels = numpy.random.randint(
            low=0, high=256, size=(100, 100, 3), dtype=numpy.uint8
        )
        image_bytes = io.BytesIO()
        PILImage.fromarray(pixels, mode="RGB").save(image_bytes, format="PNG")
        img = Image(image_bytes.getvalue())
        pil_img = img.to_pil()
        assert isinstance(pil_img, PILImage.Image)
        assert pil_img.mode == "RGB"
        l_pil_img = img.to_pil("L")
        assert l_pil_img.mode == "L"
        array = img.to_numpy()
        assert isinstance(array, numpy.ndarray)
        assert array.shape == (100, 100, 3)
        l_array = img.to_numpy("L")
        assert l_array.shape == (100, 100)

    def test_swobject_subclass_init(self) -> None:
        from starwhale.base import data_type

        for v in data_type.__dict__.values():
            if inspect.isclass(v) and issubclass(v, data_store.SwObject):
                self.assertIsInstance(v()._to_dict(), dict, f"class: {v}")

    def test_audio(self) -> None:
        fp = "/test/1.wav"
        self.fs.create_file(fp, contents="test")
        audio = Audio(fp)
        _asdict = json.loads(json.dumps(audio.asdict()))
        assert _asdict["_mime_type"] == MIMEType.WAV.value
        assert _asdict["_type"] == "audio"
        assert audio.to_bytes() == b"test"
        typ = data_store._get_type(audio)
        assert isinstance(typ, data_store.SwObjectType)

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
        assert "_content" not in _asdict
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
            "http://127.0.0.1:8081/api/v1/project/test",
            json={"data": {"id": 1, "name": ""}},
        )
        rm.request(
            HTTPMethod.GET,
            "http://127.0.0.1:8081/api/v1/project/1/dataset/mnist",
            json={"data": {"id": 1, "versionName": "123456a", "versionId": 100}},
        )
        link.instance = Instance("http://127.0.0.1:8081")

        rm.request(
            HTTPMethod.POST,
            "http://127.0.0.1:8081/api/v1/filestorage/sign-links?expTimeMillis=86400000",
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


class TestJsonDict(TestCase):
    JSON_DICT = {
        "a": 1,
        "b": [1, 2, 3],
        "c": {"ca": "1"},
        "d": Link("http://ad.c/d"),
        "e": ("a", "b"),
    }

    def test_init(self) -> None:
        _jd = JsonDict(self.JSON_DICT)
        self._do_assert(_jd)

    def _do_assert(self, _jd: JsonDict) -> None:
        self.assertEqual(1, _jd.a)
        self.assertEqual([1, 2, 3], _jd.b)
        self.assertEqual(JsonDict, type(_jd.c))
        self.assertEqual("1", _jd.c.ca)
        self.assertEqual(Link, type(_jd.d))
        self.assertEqual("http://ad.c/d", _jd.d.uri)
        self.assertEqual(("a", "b"), _jd.e)

        self.assertEqual(
            data_store.SwObjectType(
                JsonDict,
                {
                    "a": data_store.INT64,
                    "b": data_store.SwListType(data_store.INT64),
                    "c": data_store.SwObjectType(JsonDict, {"ca": data_store.STRING}),
                    "d": data_store.SwObjectType(
                        data_store.Link,
                        {
                            "_type": data_store.STRING,
                            "uri": data_store.STRING,
                            "scheme": data_store.STRING,
                            "offset": data_store.INT64,
                            "size": data_store.INT64,
                            "data_type": data_store.UNKNOWN,
                            "extra_info": data_store.SwMapType(
                                data_store.UNKNOWN, data_store.UNKNOWN
                            ),
                        },
                    ),
                    "e": data_store.SwTupleType(data_store.STRING),
                },
            ),
            data_store._get_type(_jd),
        )

    def test_cls_method(self):
        self.assertEqual(1, JsonDict.from_data(1))
        self.assertEqual("a", JsonDict.from_data("a"))
        self.assertEqual([1, 2, 3], JsonDict.from_data([1, 2, 3]))
        _d = JsonDict.from_data({"ca": "1"})
        self.assertEqual("1", _d.ca)
        _l = Link("http://ad.c/d")
        self.assertEqual(_l, JsonDict.from_data(_l))
        tpl = ("a", "b")
        self.assertEqual(tpl, JsonDict.from_data(tpl))
        sw_j_o = JsonDict.from_data(self.JSON_DICT)
        self._do_assert(sw_j_o)

    def test_asdict(self):
        sw_j_o = JsonDict.from_data(self.JSON_DICT)
        self.assertEqual(self.JSON_DICT, sw_j_o.asdict())
        self.assertEqual({}, JsonDict().asdict())

    def test_exceptions(self):
        class _MockStr(str):
            ...

        cases = [{1: "int"}, {b"a": "bytes"}, {_MockStr("test"): "obj"}]
        for case in cases:
            with self.assertRaises(ValueError):
                JsonDict.from_data(case)


class TestLine(TestCase):
    def test_to_list(self):
        p = Line([Point(3.9, 4.5), Point(5.9, 6.5), Point(7.9, 9.5)])
        self.assertEqual([[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]], p.to_list())
        self.assertEqual("Line: [[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]]", str(p))
        self.assertEqual(numpy.float64, p.dtype)
        self.assertEqual(
            data_store.SwObjectType(
                Line,
                {
                    "_type": data_store.STRING,
                    "points": data_store.SwListType(
                        data_store.SwObjectType(
                            Point,
                            {
                                "_type": data_store.STRING,
                                "x": data_store.FLOAT64,
                                "y": data_store.FLOAT64,
                            },
                        )
                    ),
                },
            ),
            data_store._get_type(p),
        )


class TestPoint(TestCase):
    def test_to_list(self):
        p = Point(3.9, 4.5)
        self.assertEqual([3.9, 4.5], p.to_list())
        self.assertEqual(numpy.float64, p.dtype)
        self.assertEqual(
            data_store.SwObjectType(
                Point,
                {
                    "_type": data_store.STRING,
                    "x": data_store.FLOAT64,
                    "y": data_store.FLOAT64,
                },
            ),
            data_store._get_type(p),
        ),


class TestPolygon(TestCase):
    def test_to_list(self):
        p = Polygon([Point(3.9, 4.5), Point(5.9, 6.5), Point(7.9, 9.5)])
        self.assertEqual([[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]], p.to_list())
        self.assertEqual("Polygon: [[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]]", str(p))
        self.assertEqual(numpy.float64, p.dtype)
        self.assertEqual(
            data_store.SwObjectType(
                Polygon,
                {
                    "_type": data_store.STRING,
                    "points": data_store.SwListType(
                        data_store.SwObjectType(
                            Point,
                            {
                                "_type": data_store.STRING,
                                "x": data_store.FLOAT64,
                                "y": data_store.FLOAT64,
                            },
                        )
                    ),
                },
            ),
            data_store._get_type(p),
        ),
