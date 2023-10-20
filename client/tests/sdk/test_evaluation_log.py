from __future__ import annotations

import io
import re
import uuid
from pathlib import Path

import numpy
from PIL import Image as PILImage
from requests_mock import Mocker

from tests import BaseTestCase
from starwhale.consts import HTTPMethod, DEFAULT_PROJECT
from starwhale.utils.fs import ensure_file
from starwhale.utils.error import ParameterError
from starwhale.base.context import Context
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.data_type import Image
from starwhale.base.uri.project import Project
from starwhale.api._impl.evaluation import log as evaluation_log_module
from starwhale.api._impl.evaluation.log import Evaluation, _get_log_store_from_context


class TestEvaluation(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        self.version = str(uuid.uuid4())
        self.local_project = Project(DEFAULT_PROJECT)
        self._cleanup_context()

    def tearDown(self) -> None:
        super().tearDown()
        self._cleanup_context()

    def _cleanup_context(self) -> None:
        try:
            Context._context_holder.__delattr__("value")
        except AttributeError:
            ...

        try:
            evaluation_log_module._log_store_instance_holder.__delattr__("value")
        except AttributeError:
            ...

    def test_without_context(self) -> None:
        raise_msg = "Starwhale does not set Context yet"
        with self.assertRaisesRegex(RuntimeError, raise_msg):
            evaluation_log_module.log("test", 1, {})

        with self.assertRaisesRegex(RuntimeError, raise_msg):
            evaluation_log_module.log_summary(loss=0.99)

        with self.assertRaisesRegex(RuntimeError, raise_msg):
            evaluation_log_module.scan("test")

        with self.assertRaisesRegex(RuntimeError, raise_msg):
            _get_log_store_from_context()

    def test_log_for_singleton_instance(self) -> None:
        _log = evaluation_log_module.log
        _log_result = evaluation_log_module.log_result

        Context.set_runtime_context(
            Context(version=self.version, log_project=self.local_project)
        )
        category = "test"

        _log(category=category, id=1, metrics={"a": 1, "b": 2})
        _log(category=category, id=2, metrics={"a": 2, "b": 3})
        _log_result(id="id-1", metrics={"text": "ttt" * 1000})
        _log_result(id="id-2", metrics={"binary": b"bbb" * 1000})

        _els = _get_log_store_from_context()
        _els.flush_all(artifacts_flush=True)

        rt = list(evaluation_log_module.scan(category))
        assert rt == [{"id": 1, "a": 1, "b": 2}, {"id": 2, "a": 2, "b": 3}]

        rt = list(evaluation_log_module.scan_results())
        assert rt == [
            {"id": "id-1", "text": "ttt" * 1000},
            {"id": "id-2", "binary": b"bbb" * 1000},
        ]

    def test_log_summary_for_singleton_instance(self) -> None:
        _log_s = evaluation_log_module.log_summary

        Context.set_runtime_context(
            Context(version=self.version, log_project=self.local_project)
        )
        _log_s(loss=0.98)
        _log_s(loss=0.99, accuracy=0.98)
        _log_s({"label": "log_summary", "accuracy": 0.99})

        _els = _get_log_store_from_context()
        _els.flush_all()

        summary = evaluation_log_module.get_summary()
        assert summary == {
            "id": self.version,
            "loss": 0.99,
            "accuracy": 0.99,
            "label": "log_summary",
        }

    def test_log_summary_no_support(self) -> None:
        _log_s = evaluation_log_module.log_summary

        Context.set_runtime_context(
            Context(version=self.version, log_project=self.local_project)
        )

        with self.assertRaisesRegex(ParameterError, "are specified at the same time"):
            _log_s({"a": 1}, b=2)

        with self.assertRaisesRegex(ParameterError, "is greater than one"):
            _log_s({"a": 1}, {"b": 2})

        with self.assertRaisesRegex(ParameterError, "is not dict type"):
            _log_s(1)

    def test_get_instance(self) -> None:
        Context.set_runtime_context(
            Context(version=self.version, log_project=self.local_project)
        )
        inst = _get_log_store_from_context()
        assert inst.id == self.version
        assert inst.project.id == "self"
        assert inst.project.name == "self"

        inst_another = _get_log_store_from_context()
        assert inst == inst_another
        assert inst == Evaluation.from_context()

    @Mocker()
    def test_log_for_server(self, req_mock: Mocker) -> None:
        instance = "http://1.1.1.1"
        sw = SWCliConfigMixed()
        sw.update_instance(
            uri=instance, user_name="test", sw_token="test", alias="test"
        )

        blob_upload_req = req_mock.register_uri(
            HTTPMethod.POST,
            re.compile(
                f"{instance}/api/v1/project/1/evaluation/{self.version}/hashedBlob/"
            ),
            json={"data": "mock-remote-uri-link"},
        )
        update_table_req = req_mock.request(
            HTTPMethod.POST,
            f"{instance}/api/v1/datastore/updateTable",
            json={
                "code": "success",
                "message": "success",
                "data": "fake-version",
            },
        )

        store = Evaluation(id=self.version, project=f"{instance}/project/1")
        store.log(
            "image",
            1,
            {
                "image-list": [
                    self._generate_random_image(),
                    1,
                    self._generate_random_image(),
                ]
            },
        )
        store.flush_all(artifacts_flush=True)
        store.close()

        assert blob_upload_req.call_count == 1
        assert update_table_req.call_count >= 1

        table_name = update_table_req.last_request.json()["tableName"]
        assert table_name == f"project/1/eval/{self.version[:2]}/{self.version}/image"
        uri_link_count = 0
        for h in update_table_req.request_history:
            uri_link_count += h.text.count("mock-remote-uri-link")
        assert uri_link_count == 2

    def test_log_and_scan_for_standalone(self) -> None:
        mock_image_fpath = Path(self.local_storage) / "text_file"
        ensure_file(mock_image_fpath, "000", parents=True)

        store = Evaluation(self.version, DEFAULT_PROJECT)
        store.log("image", 1, {"image": self._generate_random_image(), "width": 100})
        store.log(
            "image",
            2,
            {
                "image": [self._generate_random_image() for i in range(3)],
                "wight": 101.1,
                "path_image": Image(mock_image_fpath),
            },
        )
        store.log_result(
            id="id-1",
            metrics={
                "text": "aaa" * 2000,
                "binary": b"bbb" * 2000,
                "image": self._generate_random_image(),
            },
        )
        store.log_result(
            id="id-2", metrics=dict(text="aaa", binary=b"bbb", items=[1, 2, 3])
        )
        store.log_summary(
            {
                "test": {"loss": 0.99, "accuracy": 0.98, "prob": [1.0, 2.0, 3.0]},
                "artifacts": {
                    "image": self._generate_random_image(),
                    "image-list": [1, 2, self._generate_random_image()],
                },
            }
        )
        store.flush_all(artifacts_flush=True)

        summary = store.get_summary()
        assert summary["id"] == self.version
        assert summary["test/loss"] == 0.99
        assert summary["test/accuracy"] == 0.98
        assert summary["test/prob"] == [1.0, 2.0, 3.0]
        assert summary["artifacts/image-list/0"] == 1
        img = summary["artifacts/image"]
        assert isinstance(img, Image)
        assert len(img.to_bytes()) > 0
        assert isinstance(summary["artifacts/image-list/2"], Image)

        results = list(store.scan_results())
        assert len(results) == 2
        assert results[1] == {
            "id": "id-2",
            "text": "aaa",
            "binary": b"bbb",
            "items": [1, 2, 3],
        }
        assert results[0]["text"] == "aaa" * 2000
        assert results[0]["binary"] == b"bbb" * 2000
        img = results[0]["image"]
        assert isinstance(img, Image)
        assert len(img.to_bytes()) > 0

        image_items = list(store.scan("image"))
        assert len(image_items) == 2
        assert image_items[0]["width"] == 100
        assert isinstance(image_items[0]["image"], Image)
        for i in range(3):
            assert isinstance(image_items[1][f"image/{i}"], Image)
        assert image_items[1]["path_image"].to_bytes() == b"000"

        tables = store.get_tables()
        assert set(tables) == {"image", "results"}

    def test_scan_empty(self) -> None:
        store = Evaluation(self.version, DEFAULT_PROJECT)
        assert list(store.scan("image")) == []
        assert list(store.scan_results()) == []
        assert store.get_summary() == {}

    def test_flush(self) -> None:
        store = Evaluation(self.version, DEFAULT_PROJECT)
        store.flush("not-found", artifacts_flush=True)
        store.flush("not-found", artifacts_flush=False)
        store.flush_results()
        store.flush_summary()
        store.flush_all(artifacts_flush=True)

        store.log("image", 1, {"image": self._generate_random_image(), "width": 100})
        store.flush("image", artifacts_flush=True)
        assert len(list(store.scan("image"))) == 1
        store.log_result(id="id-1", metrics={"text": "aaa" * 200})
        store.flush_results(artifacts_flush=True)
        store.log_summary({"test": {"loss": 0.99, "accuracy": 0.98}})
        store.flush_summary()
        assert store.get_summary() == {
            "id": self.version,
            "test/loss": 0.99,
            "test/accuracy": 0.98,
        }

    def test_close(self) -> None:
        e = Evaluation(self.version, DEFAULT_PROJECT)
        e.close()

        with Evaluation(self.version, self.local_project) as e:
            e.log_summary({"test": {"loss": 0.99}})

        e = Evaluation(self.version, DEFAULT_PROJECT)
        assert e.get_summary()["test/loss"] == 0.99

    def _generate_random_image(self) -> Image:
        pixels = numpy.random.randint(
            low=0, high=256, size=(100, 100, 3), dtype=numpy.uint8
        )
        image_bytes = io.BytesIO()
        PILImage.fromarray(pixels, mode="RGB").save(image_bytes, format="PNG")
        return Image(image_bytes.getvalue())
