from __future__ import annotations

import io
import os
import re
import csv
import json
import typing as t
from http import HTTPStatus
from pathlib import Path
from unittest.mock import patch, MagicMock
from concurrent.futures import as_completed, ThreadPoolExecutor

import yaml
import numpy
import torch
import pytest
import jsonlines
import torch.utils.data as tdata
from PIL import Image as PILImage
from requests_mock import Mocker

from tests import ROOT_DIR, BaseTestCase
from starwhale import Dataset, dataset
from starwhale.utils import load_yaml
from starwhale.consts import (
    HTTPMethod,
    D_ALIGNMENT_SIZE,
    ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST,
)
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.utils.error import FormatError, NotFoundError, NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.data_type import (
    Text,
    Audio,
    Image,
    Video,
    Binary,
    MIMEType,
    Sequence,
    BoundingBox,
    GrayscaleImage,
    COCOObjectAnnotation,
)
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.model import DatasetSummary
from starwhale.base.models.dataset import LocalDatasetInfo, LocalDatasetInfoBase
from starwhale.core.dataset.tabular import TabularDatasetInfo
from starwhale.api._impl.dataset.loader import DataRow


class _DatasetSDKTestBase(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        self._original_cwd = os.getcwd()
        os.chdir(self.local_storage)
        os.environ[ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST] = ""

    def tearDown(self) -> None:
        if hasattr(self, "_original_cwd"):
            os.chdir(self._original_cwd)
        super().tearDown()

    def _init_simple_dataset(self) -> Resource:
        with dataset("mnist") as ds:
            for i in range(0, 10):
                ds.append(
                    DataRow(
                        index=i,
                        features={"data": Binary(f"data-{i}".encode()), "label": i},
                    )
                )
            ds.commit()
        return ds.uri

    def _init_simple_dataset_with_str_id(self) -> Resource:
        with dataset("mnist") as ds:
            for i in range(0, 10):
                ds.append(
                    DataRow(
                        index=f"{i}",
                        features={"data": Binary(f"data-{i}".encode()), "label": i},
                    )
                )
            ds.commit()
        return ds.uri

    def _create_real_image(self, color: t.Tuple[int, int, int]) -> bytes:
        img = PILImage.new(mode="RGB", size=(2, 2), color=color)
        img_io = io.BytesIO()
        img.save(img_io, format="PNG")
        return img_io.getvalue()

    def _create_real_audio(self) -> bytes:
        return (Path(ROOT_DIR) / "data" / "simple.wav").read_bytes()


@patch("starwhale.base.uri.resource.Resource._refine_local_rc_info", MagicMock())
class TestDatasetSDK(_DatasetSDKTestBase):
    def test_create_from_empty(self) -> None:
        ds = dataset("mnist")
        assert not ds.exists()
        assert ds.loading_version == ds.pending_commit_version != ""
        assert ds.uri.project.name == "self"
        assert ds.uri.project.path == ""
        assert ds.uri.name == "mnist"
        assert ds.uri.typ == ResourceType.dataset
        assert ds.uri.version == ds.pending_commit_version
        assert ds.uri.project.name == "self"
        assert ds.uri.instance.alias == "local"

        assert not ds.readonly
        assert len(ds) == 0
        assert bool(ds)
        assert not ds.committed
        ds.close()

    def test_create_mode(self) -> None:
        with self.assertRaisesRegex(
            RuntimeError, "dataset doest not exist, we have already use"
        ):
            _ = dataset("mnist", create="forbid")

        ds = dataset("mnist", create="empty")
        ds.append({"label": 1})
        ds.commit()
        ds.close()

        ds = dataset("mnist", create="forbid")
        assert ds.exists()
        assert len(ds) == 1
        ds.close()

        ds = dataset("mnist", create="auto")
        assert ds.exists()
        ds.append({"label": 2})
        ds.commit()
        ds.close()

        with self.assertRaisesRegex(
            RuntimeError, "dataset already existed, failed to create"
        ):
            _ = dataset("mnist", create="empty")

        with self.assertRaisesRegex(
            ValueError, "the current create mode is not in the accept options"
        ):
            _ = dataset("mnist", create="not-option")

    def test_no_support_type(self) -> None:
        with self.assertRaisesRegex(
            RuntimeError, "json like dict shouldn't have none-str key"
        ):
            with dataset("no-support") as ds:
                ds.append({"dict": {1: "a"}})
                ds.append({"dict": {b"test": "b"}})
                ds.append({"dict": {2.0: "c"}})
                ds.flush()

    def test_append(self) -> None:
        size = 11
        ds = dataset("mnist")
        assert ds._tmpdir is None
        assert len(ds) == 0
        ds.append(DataRow(index=0, features={"data": Binary(b""), "label": 1}))
        assert len(ds) == 1
        for i in range(1, size):
            ds.append((i, {"data": Binary(), "label": i}))
        assert len(ds) == size

        ds.append(({"data": Binary(), "label": 1},))

        with self.assertRaises(TypeError):
            ds.append(1)

        with self.assertRaises(ValueError):
            ds.append((1, 1, 1, 1, 1))

        ds.commit()

        assert ds._tmpdir and ds._tmpdir.exists()
        ds.close()

        assert not ds.tmpdir.exists()

        load_ds = dataset(ds.uri)
        assert load_ds.exists()
        assert len(load_ds) == size + 1
        ids = {d.index for d in load_ds}
        assert ids == {i for i in range(0, len(load_ds))}

    def test_extend(self) -> None:
        ds = dataset("mnist")
        assert len(ds) == 0
        size = 10
        ds.extend(
            [
                DataRow(index=i, features={"data": Binary(), "label": i})
                for i in range(0, size)
            ]
        )
        ds.extend([])

        with self.assertRaises(TypeError):
            ds.extend(None)

        with self.assertRaises(TypeError):
            ds.extend([None])

        assert ds.approximate_size == size
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert load_ds.exists()
        assert ds.approximate_size == size
        assert load_ds[0].index == 0  # type: ignore
        assert load_ds[9].index == 9  # type: ignore

    def test_setitem(self) -> None:
        ds = dataset("mnist")
        assert len(ds) == 0
        assert ds._dataset_builder is None

        ds["index-2"] = DataRow(
            index="index-2", features={"data": Binary(), "label": 2}
        )
        ds["index-1"] = DataRow(
            index="index-1", features={"data": Binary(), "label": 1}
        )

        assert ds._len_may_changed
        assert len(ds) == 2
        assert ds._len_cache == 2
        assert not ds._len_may_changed
        assert ds._dataset_builder is not None
        assert ds._dataset_builder.dataset_uri.name == ds.uri.name

        ds["index-4"] = "index-4", {"data": Binary(), "label": 4}
        ds["index-3"] = {"data": Binary(), "label": 3}
        assert ds._len_may_changed

        with self.assertRaises(TypeError):
            ds["index-5"] = (1,)

        with self.assertRaises(TypeError):
            ds["index-6"] = 1

        assert len(ds) == 4
        assert ds._len_cache == 4
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert len(load_ds) == 4
        index_names = [d.index for d in load_ds]
        assert set(index_names) == {"index-1", "index-2", "index-3", "index-4"}

    def test_setitem_exceptions(self) -> None:
        ds = dataset("mnist")
        with self.assertRaises(TypeError):
            ds[1:3] = ((1, Binary(), {}), (2, Binary(), {}))

        with self.assertRaises(TypeError):
            ds[DataRow(1, Binary())] = DataRow(1, Binary())  # type: ignore

    def test_setitem_with_auto_encode(self) -> None:
        ds = dataset("ds-auto-encode")
        raw_features = {
            "str": "abc",
            "large_str": "abc" * 1000,
            "bytes": b"abc",
            "large_bytes": b"abc" * 1000,
            "one_type_list": [1, 2, 3],
            "one_type_tuple": ["a", "b", "c"],
            "mixed_types_list": [1, "2", 3.0, ["a", "b"], [1, "2", 1.1]],
            "mixed_types_tuple": (
                1,
                "2",
                3.0,
                ("a", "b"),
                ("a", 1),
                Sequence([1, 2.0, "3"]),
                {"a": 1, "b": "2", "c": ["1", 3, "abc" * 100, 1.0]},
            ),
            "mixed_dict": {
                "a": 1,
                "b": "2",
                "c": [1, 3],
                "d": {"a": 1, "b": "2", "c": [1, "3", 1.1]},
                "e": (1, "a", [1, "2"], [1, 2]),
            },
        }
        ds.append(raw_features)
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        for k, v in raw_features.items():
            assert load_ds[0].features[k] == v

    def test_parallel_setitem(self) -> None:
        ds = dataset("mnist")

        size = 100

        def _do_task(_start: int) -> None:
            for i in range(_start, size):
                ds.append(DataRow(index=i, features={"data": Binary(), "label": i}))

        pool = ThreadPoolExecutor(max_workers=10)
        tasks = [pool.submit(_do_task, i * 10) for i in range(0, 9)]
        list(as_completed(tasks))

        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        items = list(load_ds)
        assert len(items) == size
        assert 0 <= int(items[0].index) <= 99

    def test_setitem_same_key(self) -> None:
        ds = dataset("mnist")
        ds.append(DataRow(1, {"data": Binary(b""), "label": "1-1"}))
        assert len(ds) == 1

        for i in range(0, 10):
            ds[2] = {"data": Binary(b""), "label": f"2-{i}"}

        assert len(ds) == 2
        ds.append(DataRow(3, {"data": Binary(b""), "label": "3-1"}))

        assert len(ds) == 3
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert len(list(load_ds)) == 3
        assert load_ds[2].features["label"] == "2-9"  # type: ignore
        assert len(load_ds) == 3 == load_ds.approximate_size

    def test_readonly(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri, readonly=True)

        assert ds.readonly
        readonly_msg = "in the readonly mode"
        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds.append(DataRow(1, {"data": Binary(b"")}))

        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds.extend([DataRow(1, {"data": Binary(b"")})])

        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds[1] = {}

        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds.flush()

    def test_del_item_from_existed(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        del ds[0]
        del ds[0]
        assert len(ds) == 9
        del ds[6:]
        assert len(ds) == 5

        ds.commit()
        ds.close()

        ds = dataset(ds.uri)
        items = [d.index for d in ds]
        assert set(items) == {1, 2, 3, 4, 5}

    def test_del_not_found(self) -> None:
        ds = dataset("mnist")
        del ds[0]
        del ds["1"]
        del ds["not-found"]

    def test_del_item_from_empty(self) -> None:
        with dataset("mnist") as ds:
            for i in range(0, 3):
                ds.append(DataRow(i, {"data": Binary(), "label": i}))

            ds.flush(artifacts_flush=True)
            del ds[0]
            del ds[1]
            ds.commit()

        reopen_ds = dataset(ds.uri)
        items = list(reopen_ds)
        assert len(items) == 1
        assert items[0].index == 2
        assert len(reopen_ds) == 1

    def test_build_no_data(self) -> None:
        ds = dataset("mnist")
        msg = "failed to commit, because dataset builder is None"
        with self.assertRaisesRegex(RuntimeError, msg):
            ds.commit()

        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        with self.assertRaisesRegex(RuntimeError, msg):
            ds.commit()

    def test_close(self) -> None:
        ds = dataset("mnist")
        ds.close()

        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        ds.close()
        ds.close()

    def test_create_from_existed(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)

        assert ds.loading_version == existed_ds_uri.version == ds.uri.version
        assert ds.uri.name == existed_ds_uri.name
        assert ds.uri.project == existed_ds_uri.project
        assert not ds.readonly
        assert ds.exists()
        assert len(ds) == 10
        ds.flush()

        ds.append(DataRow(index=1, features={"data": Binary(b"101"), "label": 101}))
        assert len(ds) == 10
        ds.append(DataRow(index=100, features={"data": Binary(b"100"), "label": 100}))
        assert len(ds) == 11
        ds.append(DataRow(index=101, features={"data": Binary(b"101"), "label": 101}))
        version = ds.commit()
        ds.close()

        load_ds = dataset(f"{ds.uri.name}/version/{version}")
        assert load_ds[1].features["label"] == 101  # type: ignore
        assert {d.index for d in load_ds} == set(list(range(0, 10)) + [100, 101])
        assert len(load_ds) == 12

    def test_load_from_existed(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        assert ds.loading_version == ds.uri.version == existed_ds_uri.version
        assert not ds.readonly
        assert ds.uri.name == existed_ds_uri.name

        _d = ds[0]
        assert isinstance(_d, DataRow)
        assert _d.index == 0
        assert _d.features["data"].to_bytes() == b"data-0"
        assert _d.features["label"] == 0

    def test_load_with_tag(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        name = existed_ds_uri.name
        ds = dataset(f"{name}/version/latest")
        assert ds.exists()
        assert ds.loading_version == existed_ds_uri.version

    def test_load_with_short_version(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        name = existed_ds_uri.name
        version = existed_ds_uri.version
        ds = dataset(f"{name}/version/{version[:7]}")
        assert ds.exists()
        assert ds.loading_version == existed_ds_uri.version

    def test_iter(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        items = list(ds)
        assert len(items) == 10
        assert isinstance(items[0].index, int)

        ds = dataset(existed_ds_uri)
        cnt = 0
        for item in ds:
            cnt += 1
            assert isinstance(item, DataRow)
        assert cnt == 10

    def test_versioning_scan(self) -> None:
        ds = dataset("mnist")
        for i in range(0, 10):
            ds.append({"label": i})
        ds.info["version"] = "v0"
        v0 = ds.commit()
        v0_revision = ds._last_data_datastore_revision
        ds.close()

        ds = dataset("mnist")
        del ds[0:2]
        ds.info["version"] = "v1"
        v1 = ds.commit()
        v1_revision = ds._last_data_datastore_revision
        ds.close()

        ds = dataset("mnist")
        for row in ds:
            row.features.label += 10
            row.features.new_label = 1
        ds.info["version"] = "v2"
        v2 = ds.commit()
        v2_revision = ds._last_data_datastore_revision
        ds.close()

        ds_v0 = dataset(f"mnist/version/{v0}", readonly=True)
        manifest_v0 = ds_v0.manifest()
        rows = list(ds_v0)
        assert len(rows) == 10 == len(ds_v0)
        assert ds_v0.info["version"] == "v0"
        ds_v0.close()

        ds_v1 = dataset(f"mnist/version/{v1}", readonly=True)
        manifest_v1 = ds_v1.manifest()
        rows = list(ds_v1)
        assert len(rows) == 8 == len(ds_v1)
        assert {r.index for r in rows} == {i for i in range(2, 10)}
        assert {r.features.label for r in rows} == {i for i in range(2, 10)}
        assert ds_v1.info["version"] == "v1"
        ds_v1.close()

        ds_v2 = dataset(f"mnist/version/{v2}", readonly=True)
        manifest_v2 = ds_v2.manifest()
        rows = list(ds)
        assert len(rows) == 8 == len(ds_v2)
        assert {r.index for r in rows} == {i for i in range(2, 10)}
        assert {r.features.label for r in rows} == {i + 10 for i in range(2, 10)}
        assert {r.features.new_label for r in rows} == {1}
        assert ds_v2.info["version"] == "v2"
        ds_v2.close()

        assert v0_revision != v1_revision != v2_revision
        assert manifest_v0.manifest["data_datastore_revision"] == v0_revision
        assert manifest_v1.manifest["data_datastore_revision"] == v1_revision
        assert manifest_v2.manifest["data_datastore_revision"] == v2_revision

    def test_versioning_data_scan_in_one_commit(self) -> None:
        ds = dataset("mnist")
        ds.append({"label": 0})
        v0_revision = ds.flush()
        assert ds._last_data_datastore_revision == v0_revision
        assert ds[0].features.label == 0

        for i in range(1, 10):
            ds.append({"label": i})
        assert ds._Dataset__data_loaders != {}
        ds.flush()
        assert ds._Dataset__data_loaders == {}
        del ds[9]
        ds.commit()
        ds.close()

        ds = dataset("mnist", readonly=True)
        assert len(ds) == 9
        rows = list(ds)
        assert len(rows) == 9
        assert {r.features.label for r in rows} == {i for i in range(0, 9)}
        ds.close()

    def test_get_item_by_int_id(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        assert isinstance(ds[0], DataRow)
        assert ds[0].index == 0  # type: ignore

        items: t.List[DataRow] = ds[0:3]  # type: ignore
        assert isinstance(items, list)
        assert len(items) == 3
        assert items[-1].index == 2

        items: t.List[DataRow] = ds[:]  # type: ignore
        assert isinstance(items, list)
        assert len(items) == 10

        items: t.List[DataRow] = ds[8:]  # type: ignore
        assert isinstance(items, list)
        assert len(items) == 2

        items: t.List[DataRow] = ds[::2]  # type: ignore
        assert isinstance(items, list)
        assert len(items) == 5
        assert items[0].index == 0
        assert items[1].index == 2
        assert items[4].index == 8

    def test_get_item_by_str_id(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        assert isinstance(ds["0"], DataRow)
        assert ds["0"].index == "0"  # type: ignore

        items: t.List[DataRow] = ds["0":"3"]  # type: ignore
        assert isinstance(items, list)
        assert len(items) == 3
        assert items[-1].index == "2"

        items: t.List[DataRow] = ds[:]  # type: ignore
        assert isinstance(items, list)
        assert len(items) == 10

        items: t.List[DataRow] = ds["8":]  # type: ignore
        assert isinstance(items, list)
        assert len(items) == 2

    def test_features_delete(self) -> None:
        ds = dataset("mnist")
        cnt = 10
        for i in range(0, cnt):
            ds.append({"side_label": i, "inner_label": i})
        ds.flush()

        del ds[0].features.side_label
        del ds[9].features.side_label

        for row in ds[1:9]:
            del row.features.inner_label

        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert load_ds[0].features.inner_label == 0
        assert load_ds[9].features.inner_label == 9
        assert "side_label" not in load_ds[0].features
        with self.assertRaises(AttributeError):
            _ = load_ds[9].features.side_label

        for row in load_ds[1:9]:
            assert row.features.side_label == row.index
            assert "inner_label" not in row.features

    def test_features_update(self) -> None:
        ds = dataset("mnist")
        cnt = 10
        for i in range(0, cnt):
            ds.append({"update_label": i, "remove_label": i})
        ds.flush()

        assert ds[0].features.update_label == 0
        with self.assertRaisesRegex(AttributeError, "Not found attribute"):
            assert ds[0].features.add_label == 0

        assert not ds.readonly

        for row in ds:
            row.features.update_label += 10
            row.features.add_label = 100 + int(row.index)
            del row.features.remove_label

            assert row._shadow_dataset is ds
            assert row.features._starwhale_shadow_dataset is ds
            assert row.features._starwhale_index == row.index

        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        for i in range(0, cnt):
            assert load_ds[i].features.update_label == 10 + i
            assert load_ds[i].features.add_label == 100 + i

            assert not hasattr(load_ds[i].features, "remove_label")
            assert "remove_label" not in load_ds[i].features

    def test_mixed_features_update(self) -> None:
        ds = dataset("mnist")
        cnt = 10
        for i in range(0, cnt):
            ds.append({"update_label": i})
        ds.flush()

        for row in ds:
            row.features[f"label-{row.index}"] = row.index

        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        for index, row in enumerate(load_ds):
            assert set(row.features.keys()) == {"update_label", f"label-{index}"}
            assert row.features[f"label-{index}"] == index

    def test_features_update_for_readonly_dataset(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri, readonly=True)

        assert ds.readonly

        assert ds[0].features.label == 0
        assert ds[0]._shadow_dataset is ds
        assert ds[0].features._starwhale_shadow_dataset is ds
        assert ds[0].features._starwhale_index == 0

        readonly_exception_msg = "does not work in the readonly mode"

        with self.assertRaisesRegex(RuntimeError, readonly_exception_msg):
            ds[0].features.label = 1

        with self.assertRaisesRegex(RuntimeError, readonly_exception_msg):
            ds[0].features.new_label = 1

        with self.assertRaisesRegex(RuntimeError, readonly_exception_msg):
            del ds[0].features.label

    def test_get_item_features(self) -> None:
        ds = dataset("mnist")
        ds.append({"label": 0})
        ds.flush()

        features = ds[0].features
        assert isinstance(features, dict)
        assert isinstance(features, DataRow._Features)
        assert features["label"] == features.label == 0
        with self.assertRaises(AttributeError):
            _ = features.not_found

        features.new_label = 2
        assert features["new_label"] == 2

        del features.new_label
        with self.assertRaises(AttributeError):
            _ = features.new_label

        features_dict = ds[0].features.copy()
        assert isinstance(features_dict, dict)
        assert not isinstance(features_dict, DataRow._Features)
        assert hasattr(ds[0].features, "_starwhale_index")
        assert hasattr(ds[0].features, "_starwhale_shadow_dataset")
        assert not hasattr(features_dict, "_starwhale_index")
        assert not hasattr(features_dict, "_starwhale_shadow_dataset")

    def test_tags(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        tags = list(ds.tags)
        assert tags == ["latest", "v0"]

        ds.tags.add("new_tag1")
        ds.tags.add(["new_tag2", "new_tag3"])

        tags = list(ds.tags)
        assert set(tags) == {"latest", "v0", "new_tag1", "new_tag2", "new_tag3"}

        ds.tags.remove("new_tag1")
        ds.tags.remove(["new_tag3", "new_tag2"])
        tags = list(ds.tags)
        assert tags == ["latest", "v0"]

        ds.tags.remove("not_found", ignore_errors=True)
        assert len(list(ds.tags)) == 2

        with self.assertRaisesRegex(NotFoundError, "tag:not_found"):
            ds.tags.remove("not_found")

    @Mocker()
    def test_cloud_init(self, rm: Mocker) -> None:
        project_id = 1
        sw = SWCliConfigMixed()
        sw.update_instance(
            uri="http://1.1.1.1", user_name="test", sw_token="123", alias="test"
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/self",
            json={"data": {"id": project_id, "name": "self"}},
        )

        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/not_found/version/1234",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )

        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/version/1234",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )

        scan_table_mock = rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1/api/v1/datastore/scanTable",
            json={"data": {}},
        )

        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist",
            json={
                "data": {
                    "versionMeta": yaml.safe_dump(
                        {
                            "dataset_summary": DatasetSummary(rows=101).asdict(),
                            "data_datastore_revision": "data_v0",
                            "info_datastore_revision": "info_v1",
                        }
                    ),
                }
            },
            status_code=HTTPStatus.OK,
        )

        ds = dataset(f"http://1.1.1.1/project/{project_id}/dataset/mnist/version/1234")
        assert ds.exists()

        rows = list(ds)
        assert rows == []
        assert ds.info == {}

        assert scan_table_mock.call_count == 2
        assert scan_table_mock.request_history[0].json() == {
            "tables": [
                {
                    "tableName": "project/1/dataset/mnist/_current/meta",
                    "revision": "data_v0",
                }
            ],
            "encodeWithType": True,
            "limit": 1000,
        }
        assert scan_table_mock.request_history[1].json() == {
            "tables": [
                {
                    "tableName": "project/1/dataset/mnist/_current/info",
                    "revision": "info_v1",
                }
            ],
            "encodeWithType": True,
            "end": "0000000000000000",
            "start": "0000000000000000",
            "limit": 1000,
            "endInclusive": True,
        }

    @Mocker()
    def test_create_for_cloud(self, rm: Mocker) -> None:
        project_id = 1
        sw = SWCliConfigMixed()
        sw.update_instance(
            uri="http://1.1.1.1", user_name="test", sw_token="123", alias="test"
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/self",
            json={"data": {"id": project_id, "name": "self"}},
        )

        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1/api/v1/datastore/scanTable",
            json={"data": {}},
        )

        update_table_req = rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1/api/v1/datastore/updateTable",
            json={
                "code": "success",
                "message": "Success",
                "data": "fake revision",
            },
        )

        version_req = rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/version/latest",
            status_code=HTTPStatus.NOT_FOUND,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist?versionUrl=latest",
            status_code=HTTPStatus.NOT_FOUND,
        )
        ds = dataset(
            f"http://1.1.1.1/projects/{project_id}/datasets/mnist/versions/latest"
        )
        assert version_req.call_count == 1

        file_request = rm.request(
            HTTPMethod.POST,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/version/{ds.pending_commit_version}/file",
            json={"data": {"uploadId": "123"}},
        )

        rm.register_uri(
            HTTPMethod.HEAD,
            re.compile(
                f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/hashedBlob/"
            ),
            status_code=HTTPStatus.NOT_FOUND,
        )

        rm.register_uri(
            HTTPMethod.POST,
            re.compile(
                f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/hashedBlob/"
            ),
            json={"data": "uri"},
        )

        cnt = 10
        for i in range(0, cnt):
            ds.append(
                DataRow(
                    index=i, features={"data": Binary(f"data-{i}".encode()), "label": i}
                )
            )

        ds.flush()

        _dataset_builder_dir = ds._tmpdir / "builder"
        _artifacts_bin_dir = _dataset_builder_dir / "artifact_bin_tmp"

        assert _dataset_builder_dir.exists()
        assert len(list(_artifacts_bin_dir.iterdir())) == 1

        ds.commit()
        assert update_table_req.called
        assert file_request.call_count == 2

        # TODO: when sdk supports to upload blobs into cloud, remove assertRasise
        ds.close()

    def test_consumption(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        loader = ds._get_data_loader(disable_consumption=True)

        ds.make_distributed_consumption("1")
        assert ds._consumption is not None

        consumption_loader = ds._get_data_loader(disable_consumption=False)

        another_loader = ds._get_data_loader(disable_consumption=True)
        assert loader == another_loader
        assert loader is not consumption_loader
        assert loader.session_consumption is None
        assert consumption_loader.session_consumption is not None

    def test_loader_config(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        assert ds._loader_cache_size == 20
        assert ds._loader_num_workers == 2

        ds.with_loader_config()
        assert ds._loader_cache_size == 20
        assert ds._loader_num_workers == 2

        ds.with_loader_config(num_workers=3)
        assert ds._loader_cache_size == 20
        assert ds._loader_num_workers == 3

        ds.with_loader_config(cache_size=30)
        assert ds._loader_cache_size == 30
        assert ds._loader_num_workers == 3

        ds.with_loader_config(field_transformer={"label": "lbl"})

        consumption_loader = ds._get_data_loader(disable_consumption=False)
        assert consumption_loader._cache_size == 30
        assert consumption_loader._num_workers == 3
        for item in ds:
            assert "lbl" in item.features
            assert "label" in item.features
            assert item.features["lbl"] == item.features["label"]
            assert "data" in item.features

    def test_from_dict_items(self) -> None:
        ds = Dataset.from_dict_items(records=[{"a": 1, "b": 2}], name="dict-items")
        assert len(ds) == 1
        assert ds[0].features.a == 1

        ds = Dataset.from_dict_items(
            records=iter([{"a": 10, "b": 20}]), name="dict-iter"
        )
        assert len(ds) == 1
        assert ds[0].features.b == 20
        assert ds[0].index == 0

        def _iter_records() -> t.Iterable[t.Dict[str, t.Any]]:
            for i in range(10):
                yield {"a": i}

        ds = Dataset.from_dict_items(records=_iter_records(), name="dict-iter-func")
        assert len(ds) == 10
        assert ds[9].features.a == 9
        assert ds[9].index == 9

        ds = Dataset.from_dict_items(records=[("index-1", {"a": 1})], name="dict-tuple")
        assert len(ds) == 1
        assert ds["index-1"].index == "index-1"
        assert ds["index-1"].features.a == 1

    @Mocker()
    def test_from_csv(self, rm: Mocker) -> None:
        csv_folder = Path(self.local_storage) / "csv"
        ensure_dir(csv_folder)

        with (csv_folder / "1.csv").open(newline="", mode="w") as f:
            writer = csv.DictWriter(f, fieldnames=["a", "b"], dialect="excel")
            writer.writeheader()
            writer.writerow({"a": 1, "b": 2})
            writer.writerow({"a": 11, "b": 22})

        ensure_file(csv_folder / "sub" / "2.csv", "a,b\n1,2", parents=True)
        ensure_file(csv_folder / "non.txt", "")

        ds = Dataset.from_csv(path=csv_folder, name="csv-folder")
        assert len(ds) == 3
        assert ds[0].features == {"a": "1", "b": "2"}

        ds = Dataset.from_csv(
            path=csv_folder / "1.csv",
            name="csv-file",
            encoding="utf-8",
            dialect="excel",
        )
        assert len(ds) == 2

        ds = Dataset.from_csv(
            path=[csv_folder / "1.csv", csv_folder / "sub" / "2.csv"], name="csv-multi"
        )
        assert len(ds) == 3

        url = "http://example.com/1.csv"
        rm.get(
            url,
            text="a,b\nh,d".encode("utf-8-sig").decode(),
        )

        with self.assertRaises(RuntimeError):
            Dataset.from_csv(path=url, name="csv-http")

        ds = Dataset.from_csv(path=url, name="csv-http", encoding="utf-8-sig")
        assert len(ds) == 1
        assert ds[0].features == {"a": "h", "b": "d"}

    def test_from_json_text(self) -> None:
        Dataset.from_json(
            name="translation",
            text='[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]',
            tags=["t0"],
        )
        myds = dataset("translation").with_loader_config(
            field_transformer={"en": "en-us"}
        )
        assert myds[0].features["en-us"] == myds[0].features["en"]
        Dataset.from_json(
            name="translation2",
            text='[{"content":{"child_content":[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]}}]',
        )
        myds = dataset("translation2").with_loader_config(
            field_transformer={"content.child_content[0].en": "en-us"}
        )
        assert (
            myds[0].features["en-us"]
            == myds[0].features["content"]["child_content"][0]["en"]
        )

    @Mocker()
    def test_from_json_files(self, rm: Mocker) -> None:
        json_dir = Path(self.local_storage) / "json"
        ensure_file(
            json_dir / "1.json",
            json.dumps([{"a": 1, "b": 2}, {"a": 11, "b": 22}]),
            parents=True,
        )
        ensure_file(
            json_dir / "sub" / "2.json", json.dumps([{"a": 3, "b": 4}]), parents=True
        )
        ensure_file(json_dir / "non.txt", "")

        ds = Dataset.from_json(path=json_dir, name="json-folder")
        assert len(ds) == 3
        assert ds[0].features == {"a": 1, "b": 2}

        ds = Dataset.from_json(path=json_dir / "1.json", name="json-file")
        assert len(ds) == 2

        ds = Dataset.from_json(
            path=[json_dir / "1.json", json_dir / "sub" / "2.json"], name="json-multi"
        )
        assert len(ds) == 3
        assert ds[2].features == {"a": 3, "b": 4}

        selector_text = json.dumps(
            {"a": {"b": {"c": [{"a": 1, "b": 2}, {"a": 11, "b": 22}]}}}
        )
        ensure_file(json_dir / "selector.json", selector_text, parents=True)
        ds = Dataset.from_json(
            path=json_dir / "selector.json",
            name="json-selector",
            field_selector="a.b.c",
        )
        assert len(ds) == 2
        assert ds[0].features == {"a": 1, "b": 2}

        url = "http://example.com/1.json"
        rm.get(url, text=selector_text)
        ds = Dataset.from_json(path=url, name="json-http", field_selector="a.b.c")
        assert len(ds) == 2

    @Mocker()
    def test_from_jsonl_files(self, rm: Mocker) -> None:
        jsonl_dir = Path(self.local_storage) / "jsonl"
        ensure_file(
            jsonl_dir / "1.jsonl", '{"a": 1, "b": 2}\n{"a": 11, "b": 22}', parents=True
        )
        ensure_file(jsonl_dir / "sub" / "2.jsonl", '{"a": 3, "b": 4}', parents=True)

        ds = Dataset.from_json(path=jsonl_dir, name="jsonl-folder")
        assert len(ds) == 3
        assert ds[0].features == {"a": 1, "b": 2}

        ds = Dataset.from_json(path=jsonl_dir / "1.jsonl", name="jsonl-file")
        assert len(ds) == 2
        assert ds[0].features == {"a": 1, "b": 2}

        ds = Dataset.from_json(
            path=[jsonl_dir / "1.jsonl", jsonl_dir / "sub" / "2.jsonl"],
            name="jsonl-multi",
        )
        assert len(ds) == 3
        assert ds[2].features == {"a": 3, "b": 4}

        selector_text = "\n".join(
            [
                json.dumps({"a": {"b": {"c": {"a": 1, "b": 2}}}}),
                json.dumps({"a": {"b": {"c": {"a": 11, "b": 22}}}}),
            ]
        )
        url = "http://example.com/1.jsonl"
        rm.get(url, text=selector_text)
        ds = Dataset.from_json(path=url, name="jsonl-http", field_selector="a.b.c")
        assert len(ds) == 2
        assert ds[0].features == {"a": 1, "b": 2}
        assert ds[1].features == {"a": 11, "b": 22}

    def test_from_image_folder_with_mode(self) -> None:
        image_folder = Path(self.local_storage) / "images"
        ensure_file(image_folder / "1.jpg", "1", parents=True)

        dataset_name = "image-test"
        Dataset.from_folder(
            folder=image_folder,
            name=dataset_name,
            kind="image",
            mode="overwrite",
            tags=["t0", "t1"],
        )
        ds = dataset(dataset_name)
        assert len(ds) == 1
        assert ds["1.jpg"] is not None

        empty_dir(image_folder)

        ensure_file(image_folder / "2.jpg", "2", parents=True)
        ensure_file(image_folder / "2.txt", "caption 2")
        ensure_file(image_folder / "3.jpg", "3")
        Dataset.from_folder(
            folder=image_folder, name=dataset_name, kind="image", mode="overwrite"
        )
        overwrite_ds = dataset(dataset_name)
        assert len(overwrite_ds) == 2
        assert overwrite_ds["1.jpg"] is None
        assert overwrite_ds["2.jpg"] is not None
        assert overwrite_ds["3.jpg"] is not None
        assert overwrite_ds["2.jpg"].features.caption == "caption 2"

        empty_dir(image_folder)
        ensure_file(image_folder / "2.jpg", "2", parents=True)
        ensure_file(image_folder / "2.txt", "caption 2-patch")
        ensure_file(image_folder / "4.jpg", "4")

        Dataset.from_folder(
            folder=image_folder, name=dataset_name, kind="image", mode="patch"
        )
        patch_ds = dataset(dataset_name)
        assert len(patch_ds) == 3
        assert patch_ds["1.jpg"] is None
        assert patch_ds["2.jpg"] is not None
        assert patch_ds["3.jpg"] is not None
        assert patch_ds["4.jpg"] is not None
        assert patch_ds["2.jpg"].features.caption == "caption 2-patch"

    def test_from_image_folder_only_images(self) -> None:
        image_folder = Path(self.local_storage) / "images"
        ensure_file(image_folder / "1.jpg", "1", parents=True)
        ensure_file(image_folder / "2.jpg", "2")
        ensure_file(image_folder / "sub" / "3.jpg", "3", parents=True)
        ensure_file(image_folder / "4.mp3", "4")

        ds = Dataset.from_folder(folder=image_folder, kind="image")
        assert len(ds) == 3
        assert ds.uri.name == "images"
        img = ds["1.jpg"].features.file
        assert isinstance(img, Image)
        assert img.mime_type == MIMEType.JPEG
        assert ds["1.jpg"].features.file_name == "1.jpg"
        assert "label" not in ds["1.jpg"].features

        assert ds["sub/3.jpg"] is not None
        assert ds["4.mp3"] is None

    def test_from_image_folder_with_labels(self) -> None:
        image_folder = Path(self.local_storage) / "images"
        ensure_file(image_folder / "dog" / "1.jpg", "1", parents=True)
        ensure_file(image_folder / "dog" / "2.jpg", "2", parents=True)
        ensure_file(image_folder / "cat" / "3.jpg", "3", parents=True)
        ensure_file(image_folder / "4.jpg", "4", parents=True)
        ensure_file(
            image_folder / "cat" / "sub" / "mini-cat" / "5.jpg", "5", parents=True
        )

        ds = Dataset.from_folder(folder=image_folder, kind="image")
        assert len(ds) == 5
        assert ds["dog/1.jpg"].features.label == "dog"
        assert ds["dog/2.jpg"].features.label == "dog"
        assert ds["cat/3.jpg"].features.label == "cat"
        assert ds["cat/sub/mini-cat/5.jpg"].features.label == "mini-cat"
        assert "label" not in ds["4.jpg"].features

        non_label_ds = Dataset.from_folder(
            folder=image_folder, kind="image", auto_label=False, name="non_label_ds"
        )
        for row in non_label_ds:
            assert "label" not in row.features

    def test_from_image_folder_with_caption(self) -> None:
        image_folder = Path(self.local_storage) / "images"
        ensure_file(image_folder / "1.jpg", "1", parents=True)
        ensure_file(image_folder / "1.txt", "caption 1")
        ensure_file(image_folder / "sub" / "2.jpg", "2", parents=True)
        ensure_file(image_folder / "sub" / "2.txt", "caption 2")
        ensure_file(image_folder / "3.jpg", "3")

        ds = Dataset.from_folder(folder=image_folder, kind="image")
        assert len(ds) == 3
        assert ds["1.jpg"].features.caption == "caption 1"
        assert ds["sub/2.jpg"].features.caption == "caption 2"
        assert "caption" not in ds["3.jpg"].features

    def test_from_image_folder_with_metadata_csv(self) -> None:
        image_folder = Path(self.local_storage) / "images"
        csv_content = "\n".join(
            ["file_name,size,format", "1.jpg,1,JPEG", "2.jpg,2,JPEG", "4.jpg,4,JPEG"]
        )
        ensure_file(image_folder / "metadata.csv", csv_content, parents=True)
        ensure_file(image_folder / "1.jpg", "1")
        ensure_file(image_folder / "2.jpg", "2")
        ensure_file(image_folder / "3.jpg", "3")

        ds = Dataset.from_folder(folder=image_folder, kind="image")
        assert len(ds) == 3
        assert ds["1.jpg"].features.size == "1"
        assert ds["1.jpg"].features.format == "JPEG"
        assert "size" not in ds["3.jpg"].features

    def test_from_image_folder_with_metadata_jsonl(self) -> None:
        image_folder = Path(self.local_storage) / "images"
        path = image_folder / "metadata.jsonl"
        ensure_file(path, "", parents=True)
        with path.open("w+") as f:
            writer = jsonlines.Writer(f)
            writer.write_all(
                [
                    {"file_name": "1.jpg", "size": "1", "format": "JPEG"},
                    {"file_name": "2.jpg", "size": "2", "format": "JPEG"},
                ]
            )
            writer.close()

        ensure_file(image_folder / "1.jpg", "1")
        ensure_file(image_folder / "2.jpg", "2")

        ds = Dataset.from_folder(folder=image_folder, kind="image")
        assert len(ds) == 2
        assert ds["1.jpg"].features.size == "1"
        assert ds["1.jpg"].features.format == "JPEG"
        assert ds["2.jpg"].features.format == "JPEG"
        assert ds["2.jpg"].features.size == "2"

    def test_from_image_folder_with_metadata_exceptions(self) -> None:
        image_folder = Path(self.local_storage) / "images"
        ensure_file(image_folder / "metadata.csv", "", parents=True)
        ensure_file(image_folder / "metadata.jsonl", "", parents=True)
        with self.assertRaisesRegex(
            RuntimeError, "metadata.csv and metadata.jsonl are mutually exclusive"
        ):
            Dataset.from_folder(folder=image_folder, kind="image")

        new_image_folder = Path(self.local_storage) / "new_images"
        csv_content = "\n".join(["file_name2,size,format", "1.jpg,1,JPEG"])
        ensure_file(new_image_folder / "metadata.csv", csv_content, parents=True)
        with self.assertRaisesRegex(KeyError, "file_name"):
            Dataset.from_folder(folder=new_image_folder, kind="image")

    def test_from_audio_folder(self) -> None:
        audio_folder = Path(self.local_storage) / "audio"
        ensure_file(audio_folder / "1.wav", "1", parents=True)
        ensure_file(audio_folder / "2.mp3", "2")
        ensure_file(audio_folder / "3.mp4", "3")

        ds = Dataset.from_folder(folder=audio_folder, kind="audio")
        assert len(ds) == 2
        assert ds["1.wav"].features.file.mime_type == MIMEType.WAV
        assert ds["2.mp3"].features.file.mime_type == MIMEType.MP3

    def test_from_video_folder(self) -> None:
        video_folder = Path(self.local_storage) / "video"
        ensure_file(video_folder / "1.wav", "1", parents=True)
        ensure_file(video_folder / "3.mp4", "3")
        ensure_file(video_folder / "4.avi", "4")
        ensure_file(video_folder / "5.webm", "5")

        ds = Dataset.from_folder(folder=video_folder, kind="video")
        assert len(ds) == 3
        assert ds["3.mp4"].features.file.mime_type == MIMEType.MP4
        assert ds["4.avi"].features.file.mime_type == MIMEType.AVI
        assert ds["5.webm"].features.file.mime_type == MIMEType.WEBM

    def test_loader_config_exception(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        ds._get_data_loader(disable_consumption=False)

        with self.assertRaisesRegex(RuntimeError, "have already been initialized"):
            ds.with_loader_config(num_workers=1)

    def test_consumption_recreate_exception(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        ds.make_distributed_consumption("1")

        with self.assertRaisesRegex(
            RuntimeError, "distributed consumption has already been created"
        ):
            ds.make_distributed_consumption("2")

    def test_info_readonly(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        assert isinstance(ds.info, TabularDatasetInfo)
        assert list(ds.info) == []
        assert not bool(ds.info)

        ds.info["a"] = 1
        ds.close()

        ds = dataset(existed_ds_uri)
        assert list(ds.info) == []

    def test_info_update(self) -> None:
        ds = dataset("mnist")
        ds.append(DataRow(1, {"data": Binary(b"123"), "label": 1}))

        assert list(ds.info) == []
        assert not bool(ds.info)
        ds.info["a"] = 1
        ds.info["b"] = {"k": 1}
        ds.info["c"] = [1, 2, 3]
        ds.commit()

        load_ds = dataset(ds.uri)
        assert list(ds.info) == ["a", "b", "c"]
        assert load_ds.info["a"] == 1
        assert load_ds.info["b"] == {"k": 1}
        assert load_ds.info["c"] == [1, 2, 3]

    def test_manifest(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        m = ds.manifest()
        assert isinstance(m, LocalDatasetInfo)
        assert m.name == ds.uri.name
        assert m.version == ds.loading_version
        assert m.tags == ["latest", "v0"]
        assert m.project == ds.uri.project.name

        empty_ds = dataset("mnist_new")
        m = empty_ds.manifest()
        assert m is None

    def test_summary(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        summary = ds.summary()
        assert summary is not None
        assert summary.rows == len(ds)
        assert summary.updated_rows == 10
        assert summary.deleted_rows == 0
        assert (
            summary.blobs_byte_size
            == summary.increased_blobs_byte_size
            == 10 * D_ALIGNMENT_SIZE
        )

    def test_create_dataset(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()

        name = existed_ds_uri.name
        new_ds_name = f"{name}-new"

        with self.assertRaisesRegex(
            ValueError, "no support to set a non-existed dataset to the readonly mode"
        ):
            _ = dataset(new_ds_name, readonly=True)

        with self.assertRaisesRegex(
            NoSupportError,
            "no support to create a specified version dataset",
        ):
            _ = dataset(f"{new_ds_name}/version/123")

        new_ds = dataset(new_ds_name)
        assert new_ds.uri.name == new_ds_name

    def test_remove_recover(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        list_info, _ = ds.list(ds.uri.project, fullname=True)
        assert isinstance(list_info, list)
        item = list_info[0]
        assert isinstance(item, LocalDatasetInfoBase)
        assert item.name == ds.uri.name
        assert item.version == ds.loading_version
        assert item.tags == ["latest", "v0"]

        ds.remove()
        with self.assertRaisesRegex(RuntimeError, "failed to remove dataset"):
            ds.remove()

        list_info, _ = ds.list(ds.uri.project, fullname=True)
        assert list_info == []

        ds.recover()
        with self.assertRaisesRegex(RuntimeError, "failed to recover dataset"):
            ds.recover()

        list_info, _ = ds.list(ds.uri.project, fullname=True)
        item = list_info[0]
        assert isinstance(item, LocalDatasetInfoBase)
        assert item.version == ds.loading_version

    def test_history(self) -> None:
        # TODO: add more test cases after the dataset versioning refactor
        existed_int_ds_uri = self._init_simple_dataset()
        int_ds = dataset(existed_int_ds_uri)

        history = int_ds.history()
        assert len(history) == 1
        assert history[0]["name"] == int_ds.uri.name
        assert history[0]["version"] == int_ds.loading_version

    @pytest.mark.skip("enable this test when datastore supports diff")
    def test_diff(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        str_ds = dataset(existed_ds_uri)

        existed_int_ds_uri = self._init_simple_dataset()
        int_ds = dataset(existed_int_ds_uri)

        diff = str_ds.diff(int_ds)
        assert diff["diff_rows"]["updated"] == 10

        diff = str_ds.diff(str_ds)
        assert diff == {}

        diff = int_ds.diff(str_ds)
        assert diff["diff_rows"]["updated"] == 10

    def test_head(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri, readonly=True)

        head = ds.head(n=0)
        assert len(head) == 0

        head = ds.head(n=4, skip_fetch_data=True)
        assert len(head) == 4
        assert not head[0].features.data._BaseArtifact__cache_bytes
        assert not head[1].features.data._BaseArtifact__cache_bytes

        head = ds.head(n=1)
        assert len(head) == 1
        assert head[0].index == "0"
        assert "raw" not in head[0].features

        head = ds.head(n=2)
        assert len(head) == 2
        assert head[0].index == "0"
        assert head[1].index == "1"
        assert head[0].features.data._BaseArtifact__cache_bytes == b"data-0"
        assert head[1].features.data._BaseArtifact__cache_bytes == b"data-1"

        head = ds.head(1000)
        assert len(head) != 1000
        assert len(head) == len(ds)

    @Mocker()
    def test_copy(self, rm: Mocker) -> None:
        project_id = 1
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        sw = SWCliConfigMixed()
        sw.update_instance(
            uri="http://1.1.1.1", user_name="test", sw_token="123", alias="test"
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/self",
            json={"data": {"id": project_id, "name": "project"}},
        )
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )

        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1/api/v1/datastore/updateTable",
            json={"data": "datastore_revision"},
        )

        rm.register_uri(
            HTTPMethod.HEAD,
            re.compile(
                f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/hashedBlob/"
            ),
            status_code=HTTPStatus.NOT_FOUND,
        )

        upload_blob_req = rm.register_uri(
            HTTPMethod.POST,
            re.compile(
                f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/hashedBlob/"
            ),
            json={"data": "server_return_uri"},
        )

        make_version_req = rm.request(
            HTTPMethod.POST,
            f"http://1.1.1.1/api/v1/project/{project_id}/dataset/mnist/version/{ds.loading_version}/file",
            json={"data": {"uploadId": 1}},
        )

        ds.copy("cloud://test/project/self", force=True)
        assert make_version_req.call_count == 2
        assert upload_blob_req.call_count == 1

    def test_commit_from_empty(self) -> None:
        dataset_name = "mnist"
        commit_msg = "test"
        ds = dataset(dataset_name)
        assert not ds.changed
        ds.append({"label": 1})
        assert ds.changed
        assert not ds.committed
        with self.assertRaisesRegex(RuntimeError, "version has not been committed yet"):
            ds.committed_version

        version = ds.commit(message=commit_msg)
        ds.close()

        assert ds.committed
        assert ds.pending_commit_version == version
        assert ds.loading_version == version
        assert ds.committed_version == version

        manifest_path = (
            Path(self.local_storage)
            / "self"
            / "dataset"
            / dataset_name
            / version[:2]
            / f"{version}.swds"
            / "_manifest.yaml"
        )
        manifest = load_yaml(manifest_path)
        assert "created_at" in manifest
        assert "data_datastore_revision" in manifest
        assert "info_datastore_revision" in manifest
        assert manifest["dataset_summary"] == {
            "deleted_rows": 0,
            "rows": 1,
            "updated_rows": 1,
            "blobs_byte_size": 0,
            "increased_blobs_byte_size": 0,
        }
        assert manifest["message"] == commit_msg
        assert manifest["version"] == ds.loading_version

    def test_commit_from_existed(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        ds.append(DataRow(index="100", features={"data": Binary(b"100"), "label": 100}))
        assert ds.uri.version == existed_ds_uri.version
        version = ds.commit()
        assert version != existed_ds_uri.version
        assert ds.uri.version == version
        ds.close()

        assert version == ds.pending_commit_version
        assert existed_ds_uri.version == ds.loading_version
        assert version != ds.loading_version

        dataset_dir = Path(self.local_storage) / "self" / "dataset" / ds.uri.name

        v0 = existed_ds_uri.version
        v1 = version
        v0_path = dataset_dir / v0[:2] / f"{v0}.swds" / "_manifest.yaml"
        v1_path = dataset_dir / v1[:2] / f"{v1}.swds" / "_manifest.yaml"
        assert v0_path.exists()
        assert v1_path.exists()
        assert load_yaml(v0_path)["version"] == v0
        assert load_yaml(v1_path)["version"] == v1
        tag_manifest = load_yaml(dataset_dir / "_manifest.yaml")
        assert tag_manifest["fast_tag_seq"] == 1
        assert tag_manifest["tags"] == {"latest": v1, "v0": v0, "v1": v1}
        assert tag_manifest["versions"] == {
            v1: {"latest": True, "v1": True},
            v0: {"v0": True},
        }

    def test_commit_with_tags(self) -> None:
        ds = dataset("mnist")
        ds.append({"label": 1})

        with self.assertRaisesRegex(FormatError, "tag:latest is builtin"):
            ds.commit(tags=["latest"])

        with self.assertRaisesRegex(FormatError, "tag:v0 is builtin"):
            ds.commit(tags=["v0"])

        ds.commit(tags=["test1", "test2"])
        ds.close()

        load_ds = dataset("mnist")
        assert list(load_ds.tags) == ["latest", "test1", "test2", "v0"]

    def test_commit_exception(self) -> None:
        ds = dataset("mnist")
        with self.assertRaisesRegex(
            RuntimeError, "failed to commit, because dataset builder is None"
        ):
            ds.commit()

        ds.append({"label": 1})
        ds.commit()

        with self.assertRaisesRegex(RuntimeError, "Dataset has already committed"):
            ds.commit()

        ds.close()

    def test_no_commit(self) -> None:
        with dataset("mnist"):
            ...

        with self.assertRaisesRegex(
            ValueError, "no support to set a non-existed dataset to the readonly mode"
        ):
            dataset("mnist", readonly=True)

        ds = dataset("mnist")
        ds.append(("index-1", {"label": 1}))
        v0 = ds.commit()
        ds.close()

        no_commit_ds = dataset("mnist")
        no_commit_ds.append(("index-2", {"label": 2}))
        no_commit_ds.close()

        commit_ds = dataset("mnist")
        commit_ds.append(("index-3", {"label": 3}))
        v1 = commit_ds.commit()
        commit_ds.close()

        load_ds = dataset("mnist")
        assert list(load_ds.tags) == ["latest", "v1"]
        rows = {r.index for r in load_ds}
        assert rows == {"index-1", "index-2", "index-3"}
        history = load_ds.history()
        assert len(history) == 2
        assert {v0, v1} == {history[0]["version"], history[1]["version"]}

    def test_with_builder_blob_config_forbid(self) -> None:
        ds = dataset("mnist-error")
        ds.append({"label": 1})
        with self.assertRaisesRegex(
            RuntimeError, "dataset has already accept some changed rows"
        ):
            ds.with_builder_blob_config(volume_size=1, alignment_size=1)

        ds.close()

    def test_with_builder_blob_config(self) -> None:
        ds = dataset("mnist").with_builder_blob_config(
            volume_size=1024, alignment_size=48
        )
        ds.append({"bin": Binary(b"abc")})
        ds.commit()
        ds.close()

        assert (
            ds._dataset_builder._artifact_bin_writer.alignment_bytes_size
            == ds._builder_blob_alignment_size
            == 48
        )
        assert (
            ds._dataset_builder._artifact_bin_writer.volume_bytes_size
            == ds._builder_blob_volume_size
            == 1024
        )

        assert len(ds._dataset_builder.signature_bins_meta) == 1
        assert ds._dataset_builder.signature_bins_meta[0].size == 48


@patch("starwhale.base.uri.resource.Resource._refine_local_rc_info", MagicMock())
@patch("starwhale.base.uri.resource.Resource._refine_remote_rc_info", MagicMock())
class TestPytorch(_DatasetSDKTestBase):
    def test_skip_default_transform_without_batch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        torch_ds = ds.to_pytorch(skip_default_transform=True)
        assert isinstance(torch_ds, tdata.Dataset)
        assert isinstance(torch_ds, tdata.IterableDataset)

        torch_loader = tdata.DataLoader(torch_ds, batch_size=None)

        items = list(torch_loader)
        assert len(ds) == len(items)
        assert len(items[0]) == 2
        assert isinstance(items[0], dict)
        assert "label" in items[0]
        assert isinstance(items[0]["data"], Binary)

    def test_skip_default_transform_with_batch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        torch_ds = ds.to_pytorch(skip_default_transform=True)
        torch_loader = tdata.DataLoader(torch_ds, batch_size=2)

        with self.assertRaisesRegex(
            TypeError,
            "default_collate: batch must contain tensors, numpy arrays, numbers, dicts or lists",
        ):
            list(torch_loader)

    def test_binary_type_without_batch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        assert isinstance(ds["0"].features["data"], Binary)  # type: ignore

        torch_loader = tdata.DataLoader(
            ds.to_pytorch(skip_default_transform=False), batch_size=None
        )
        items = list(torch_loader)
        assert len(items) == 10

    def test_binary_type_with_batch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        assert isinstance(ds["0"].features["data"], Binary)  # type: ignore

        torch_loader = tdata.DataLoader(
            ds.to_pytorch(skip_default_transform=False), batch_size=2
        )
        items = list(torch_loader)
        assert len(items) == 5
        first_item = items[0]
        assert isinstance(first_item, dict)
        assert len(first_item["data"]) == len(
            tdata.default_collate([b"data-0", b"data-1"])
        )

        assert isinstance(first_item["label"], torch.Tensor)
        assert list(first_item["label"].size()) == [2]

    def test_binary_type_with_batch_fetch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        for rows in ds.batch_iter(2):
            assert len(rows) == 2
            assert type(rows) == list
            assert type(rows[0]) == DataRow

    def test_keep_index(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        torch_ds = ds.to_pytorch(drop_index=False)
        torch_loader = tdata.DataLoader(torch_ds, batch_size=5)

        item = next(iter(torch_loader))
        assert isinstance(item, list)
        assert len(set(item[0]) - set([d.index for d in ds])) == 0
        assert len(item[0]) == 5

    def test_use_custom_transform(self) -> None:
        with dataset("mnist") as ds:
            for i in range(0, 10):
                ds.append({"txt": Text(f"data-{i}"), "label": i})

            ds.commit()

        def _custom_transform(data: t.Any) -> t.Any:
            data = data.copy()
            txt = data["txt"].to_str()
            data["txt"] = f"custom-{txt}"
            return data

        torch_loader = tdata.DataLoader(
            dataset(ds.uri).to_pytorch(transform=_custom_transform), batch_size=1
        )
        item = next(iter(torch_loader))
        assert isinstance(item["label"], torch.Tensor)
        assert item["txt"][0] in ("custom-data-0", "custom-data-1")

    def test_complex_transform(self) -> None:
        ds = dataset("mnist")
        for i in range(0, 10):
            data = {
                "text": Text(f"data-{i}"),
                "int": 1,
                "float": 1.1,
                "tuple": (1, 2, 3),
                "list": [1, 2, 3],
                "map": {"key": i},
                "str": f"str-{i}",
                "bytes": f"bytes-{i}".encode(),
            }
            ds.append(data)
        ds.commit()
        torch_loader = tdata.DataLoader(dataset(ds.uri).to_pytorch(), batch_size=2)
        item = next(iter(torch_loader))

        assert isinstance(item["text"], list)
        assert len(item["text"]) == 2
        assert item["text"][0].startswith("data-")

        assert isinstance(item["map"], dict)
        assert item["int"].dtype == torch.int64
        assert list(item["int"].size()) == [2]
        assert torch.equal(item["int"], torch.tensor([1, 1]))
        assert torch.equal(
            item["float"], torch.tensor([1.1000, 1.1000], dtype=torch.float64)
        )
        assert torch.equal(item["tuple"][0], torch.tensor([1, 1]))
        assert isinstance(item["map"]["key"], torch.Tensor)
        assert item["map"]["key"].dtype == torch.int64
        assert list(item["map"]["key"].size()) == [2]
        assert len(item["str"]) == 2
        assert item["str"][0].startswith("str-")
        assert len(item["bytes"]) == len(
            tdata.default_collate([b"bytes-0", b"bytes-1"])
        )

    def test_image_transform(self) -> None:
        ds = dataset("mnist")
        for i in range(1, 10):
            _img = self._create_real_image((i, i, i))
            ds.append({"img": Image(_img), "label": i})

        ds.commit()

        torch_loader = tdata.DataLoader(dataset(ds.uri).to_pytorch(), batch_size=2)
        item = next(iter(torch_loader))
        assert isinstance(item["img"], torch.Tensor)
        assert item["img"].dtype == torch.uint8
        assert list(item["img"].size()) == [2, 2, 2, 3]

    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    def test_audio_transform(self) -> None:
        with dataset("mnist") as ds:
            _audio = self._create_real_audio()
            ds.append({"audio": Audio(_audio), "label": 1})
            ds.commit()

        torch_loader = tdata.DataLoader(dataset(ds.uri).to_pytorch(), batch_size=2)
        item = next(iter(torch_loader))
        assert isinstance(item["audio"], torch.Tensor)
        assert len(item["audio"]) != 0
        assert item["audio"].dtype == torch.float64


class TestTensorflow(_DatasetSDKTestBase):
    def test_simple_data(self) -> None:
        import tensorflow as tf

        import starwhale.integrations.tensorflow.dataset as tf_dataset

        mixed_data = {
            "int": 1,
            "float": 1.1,
            "bool": True,
            "str": "test",
            "bytes": b"test",
            "complex": complex(1, 2),
            "image": Image(self._create_real_image((1, 1, 1))),
            "bbox": BoundingBox(1.1, 1.1, 2.1, 2.2),
            "video": Video(b"123"),
            "audio": Audio(self._create_real_audio()),
            "text": Text("text"),
            "binary": Binary(b"binary"),
            "grayscale_image": GrayscaleImage(self._create_real_image((0, 0, 0))),
        }
        tensor_spec = tf_dataset._inspect_spec(mixed_data)
        assert tensor_spec == {
            "int": tf.TensorSpec(shape=(), dtype=tf.int64),
            "float": tf.TensorSpec(shape=(), dtype=tf.float64),
            "bool": tf.TensorSpec(shape=(), dtype=tf.bool),
            "str": tf.TensorSpec(shape=(), dtype=tf.string),
            "bytes": tf.TensorSpec(shape=(), dtype=tf.string),
            "complex": tf.TensorSpec(shape=(), dtype=tf.complex128),
            "image": tf.TensorSpec(shape=(None, None, 3), dtype=tf.uint8),
            "video": tf.TensorSpec(shape=(None,), dtype=tf.uint8),
            "audio": tf.TensorSpec(shape=(None,), dtype=tf.float64),
            "text": tf.TensorSpec(shape=(), dtype=tf.string),
            "binary": tf.TensorSpec(shape=(), dtype=tf.string),
            "grayscale_image": tf.TensorSpec(shape=(None, None, 1), dtype=tf.uint8),
            "bbox": tf.TensorSpec(shape=(4,), dtype=tf.float64),
        }

        td = tf_dataset._transform(mixed_data)
        assert td["int"] == 1
        assert td["float"] == 1.1
        assert td["image"].shape == (2, 2, 3)
        assert td["image"].dtype == numpy.uint8
        assert isinstance(td["video"], numpy.ndarray)
        assert td["video"].dtype == numpy.uint8
        assert numpy.array_equal(
            td["bbox"], numpy.array([1.1, 1.1, 2.1, 2.2], dtype=numpy.float64)
        )
        assert td["audio"].dtype == numpy.float64
        assert td["audio"].shape == (5, 1)
        assert numpy.array_equal(td["text"], numpy.array("text", dtype=numpy.str_))
        assert numpy.array_equal(
            td["binary"], numpy.array(b"binary", dtype=numpy.bytes_)
        )

    def test_compound_data(self) -> None:
        import tensorflow as tf

        import starwhale.integrations.tensorflow.dataset as tf_dataset

        mixed_data = {
            "list_int": [1, 2, 3],
            "list_float": [1.1, 1.2],
            "list_list": [[1, 2], [2, 3]],
            "list_bbox": [BoundingBox(1, 2, 3, 4), BoundingBox(1, 2, 3, 5)],
            "list_text": [[Text(), Text()], [Text(), Text()]],
            "map_map": {"a": {"a": 1}},
            "map_list": {"a": {"a": [[Binary(), Binary()], [Binary(), Binary()]]}},
        }
        tensor_spec = tf_dataset._inspect_spec(mixed_data)
        assert tensor_spec == {
            "list_int": tf.TensorSpec(shape=(3,), dtype=tf.int64),
            "list_float": tf.TensorSpec(shape=(2,), dtype=tf.float64),
            "list_list": tf.TensorSpec(shape=(2, 2), dtype=tf.int64),
            "list_bbox": tf.TensorSpec(shape=(2, 4), dtype=tf.float64),
            "list_text": tf.TensorSpec(shape=(2, 2), dtype=tf.string),
            "map_map": {"a": {"a": tf.TensorSpec(shape=(), dtype=tf.int64)}},
            "map_list": {"a": {"a": tf.TensorSpec(shape=(2, 2), dtype=tf.string)}},
        }
        td = tf_dataset._transform(mixed_data)
        assert td["list_int"] == [1, 2, 3]
        assert numpy.array_equal(
            td["list_bbox"][0], numpy.array([1, 2, 3, 4], dtype=numpy.float64)
        )
        assert isinstance(td["map_list"]["a"]["a"], list)
        assert numpy.array_equal(
            td["map_list"]["a"]["a"][0][0], numpy.array(b"", dtype=numpy.bytes_)
        )

    @pytest.mark.filterwarnings("ignore::numpy.VisibleDeprecationWarning")
    def test_inspect_exceptions(self) -> None:
        import starwhale.integrations.tensorflow.dataset as tf_dataset

        with self.assertRaisesRegex(
            NoSupportError, "inspect tensor spec does not support"
        ):
            tf_dataset._inspect_spec(COCOObjectAnnotation())

        with self.assertRaisesRegex(
            NoSupportError, "Can't convert different types in one array to tensor"
        ):
            tf_dataset._inspect_spec([[1, 1], [Binary(), Binary()]])

        ravel_err_msg = "Can't ravel to one dimension array|setting an array element with a sequence"

        with self.assertRaisesRegex(ValueError, ravel_err_msg):
            tf_dataset._inspect_spec([[1, 1], [Binary()]])

        with self.assertRaisesRegex(ValueError, ravel_err_msg):
            tf_dataset._inspect_spec([[1, 1], [1]])

        with self.assertRaisesRegex(NoSupportError, "Can't handle the compound type"):
            tf_dataset._inspect_spec([{"a": 1}, {"a": 2}])

    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    def test_tf_dataset_drop_index(self) -> None:
        import tensorflow as tf

        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        tf_ds = ds.to_tensorflow(drop_index=True)
        assert isinstance(tf_ds, tf.data.Dataset)

        items = list(tf_ds)
        assert len(items[0]) == 2
        assert isinstance(items[0]["data"], tf.Tensor)
        assert items[0]["data"].dtype == tf.string
        assert items[0]["data"].numpy().startswith(b"data-")
        assert items[0]["label"].dtype == tf.int64
        assert len(items) == len(ds)

    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    def test_tf_dataset_with_index(self) -> None:
        import tensorflow as tf

        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        tf_ds = ds.to_tensorflow(drop_index=False)
        assert isinstance(tf_ds, tf.data.Dataset)

        items = list(tf_ds)
        assert len(items[0]) == 2
        assert items[0][1]["data"].dtype == tf.string

        batch_ds = tf_ds.batch(2, drop_remainder=True)
        items = list(batch_ds.as_numpy_iterator())
        assert len(items)
        assert len(items[0][0].tolist()) == 2
        assert len(items[0][1]) == 2
        assert items[0][1]["data"][0].startswith(b"data-")
        assert len(items[0][1]["label"].tolist()) == 2


class TestHuggingface(_DatasetSDKTestBase):
    def test_simple_data(self) -> None:
        import datasets as hf_datasets

        from starwhale.integrations.huggingface.dataset import _transform_to_starwhale

        cases = [
            (1, hf_datasets.Value("int64"), 1),
            (1.0, hf_datasets.Value("float64"), 1.0),
            (b"000", hf_datasets.Value("binary"), b"000"),
            (b"000" * 1000, hf_datasets.Value("binary"), b"000" * 1000),
            (b"000", hf_datasets.Value("large_binary"), b"000"),
            (b"000" * 1001, hf_datasets.Value("large_binary"), b"000" * 1001),
            ("000", hf_datasets.Value("string"), "000"),
            ("000" * 999, hf_datasets.Value("string"), "000" * 999),
            ("000", hf_datasets.Value("large_string"), "000"),
            ("000" * 500, hf_datasets.Value("large_string"), "000" * 500),
            (1, hf_datasets.ClassLabel(num_classes=3, names=["a", "b", "c"]), 1),
            (
                [[1, 2], [11, 22]],
                hf_datasets.Array2D(shape=(1, 2), dtype="int32"),
                [[1, 2], [11, 22]],
            ),
            (
                [[[1], [2]], [[11], [22]]],
                hf_datasets.Array3D(shape=(1, 2, 1), dtype="int32"),
                [[[1], [2]], [[11], [22]]],
            ),
            (
                {"en": "the cat", "zh": "猫"},
                hf_datasets.Translation(languages=["en", "zh"]),
                {"en": "the cat", "zh": "猫"},
            ),
            (
                {"en": "the cat", "fr": ["le chat", "la chatte"]},
                hf_datasets.TranslationVariableLanguages(languages=["en", "fr"]),
                {"en": "the cat", "fr": ["le chat", "la chatte"]},
            ),
            (1, int, 1),
        ]

        for data, feature, result in cases:
            assert result == _transform_to_starwhale(data, feature)

    def test_image(self) -> None:
        import datasets as hf_datasets

        from starwhale.integrations.huggingface.dataset import _transform_to_starwhale

        img = PILImage.new(mode="RGB", size=(1, 1), color=(1, 1, 1))
        transform_img = _transform_to_starwhale(img, hf_datasets.Image(decode=True))
        assert isinstance(transform_img, Image)
        assert transform_img.shape == [1, 1, 3]
        assert transform_img.mime_type == MIMEType.PNG

        grayscale_img = PILImage.new(mode="L", size=(1, 1), color=1)
        transform_img = _transform_to_starwhale(
            grayscale_img, hf_datasets.Image(decode=True)
        )
        assert isinstance(transform_img, Image)
        assert transform_img.shape == [1, 1, 1]

        with self.assertRaisesRegex(NoSupportError, "Unknown image type"):
            _transform_to_starwhale({}, hf_datasets.Image(decode=True))

    def test_audio(self) -> None:
        import datasets as hf_datasets

        from starwhale.integrations.huggingface.dataset import _transform_to_starwhale

        audio = {
            "path": None,
            "array": numpy.array([1.0, 2.0, 3.0]),
            "sampling_rate": 16000,
        }

        transform_audio = _transform_to_starwhale(audio, hf_datasets.Audio())
        assert isinstance(transform_audio, Audio)
        assert transform_audio.mime_type == MIMEType.WAV

        audio = {"path": Path(ROOT_DIR) / "data" / "simple.wav"}
        transform_audio = _transform_to_starwhale(audio, hf_datasets.Audio())
        assert isinstance(transform_audio, Audio)
        assert transform_audio.mime_type == MIMEType.WAV

        with self.assertRaisesRegex(ValueError, "Unknown audio data fields:"):
            assert _transform_to_starwhale({}, hf_datasets.Audio())

    def test_compound_data(self) -> None:
        import datasets as hf_datasets

        from starwhale.integrations.huggingface.dataset import _transform_to_starwhale

        data = {
            "int": 1,
            "float": 1.1,
            "list_int": [1, 1, 1],
            "sequence_image": [
                PILImage.new(mode="RGB", size=(1, 1), color=(1, 1, 1)),
                PILImage.new(mode="L", size=(1, 1), color=1),
            ],
            "dict": {"int": 1},
            "sequence_dict": {
                "int": 1,
                "list_int": [1, 1, 1],
            },
        }

        features = {
            "int": hf_datasets.Value("int64"),
            "float": hf_datasets.Value("float64"),
            "list_int": hf_datasets.Sequence(feature=hf_datasets.Value("int64")),
            "sequence_image": hf_datasets.Sequence(feature=hf_datasets.Image()),
            "dict": {
                "int": hf_datasets.Value("int64"),
            },
            "sequence_dict": hf_datasets.Sequence(
                feature={
                    "int": hf_datasets.Value("int64"),
                    "list_int": hf_datasets.Sequence(
                        feature=hf_datasets.Value("int64")
                    ),
                }
            ),
        }
        transform_data = _transform_to_starwhale(data, features)

        assert transform_data["int"] == 1
        assert transform_data["float"] == 1.1
        assert transform_data["list_int"] == [1, 1, 1]
        assert isinstance(transform_data["sequence_image"][0], Image)
        assert transform_data["dict"]["int"] == 1
        assert transform_data["sequence_dict"]["int"] == 1
        assert transform_data["sequence_dict"]["list_int"] == [1, 1, 1]

    @patch(
        "starwhale.integrations.huggingface.dataset.hf_datasets.get_dataset_config_names"
    )
    @patch("starwhale.integrations.huggingface.dataset.hf_datasets.load_dataset")
    def test_build_dataset(
        self, m_load_dataset: MagicMock, m_get_config_names: MagicMock
    ) -> None:
        import datasets as hf_datasets

        complex_data = {
            "list_int": [[1, 2, 3]],
            "seq_img": [
                [
                    PILImage.new(mode="RGB", size=(1, 1), color=(1, 1, 1)),
                    PILImage.new(mode="L", size=(1, 1), color=1),
                ]
            ],
            "seq_dict": [
                [
                    {
                        "int": 1,
                        "str": "test",
                    }
                ]
            ],
            "img": [PILImage.new(mode="RGB", size=(1, 1), color=(1, 1, 1))],
            "audio": [str(Path(ROOT_DIR) / "data" / "simple.wav")],
            "class_label": [1],
        }

        complex_features = hf_datasets.Features(
            {
                "list_int": hf_datasets.Sequence(feature=hf_datasets.Value("int64")),
                "seq_img": hf_datasets.Sequence(feature=hf_datasets.Image()),
                "seq_dict": hf_datasets.Sequence(
                    feature={
                        "int": hf_datasets.Value("int64"),
                        "str": hf_datasets.Value("string"),
                    }
                ),
                "img": hf_datasets.Image(),
                "audio": hf_datasets.Audio(),
                "class_label": hf_datasets.ClassLabel(
                    num_classes=3, names=["a", "b", "c"]
                ),
            }
        )

        simple_data = {
            "int": [1, 2],
            "float": [1.0, 2.0],
            "str": ["test1", "test2"],
            "bin": [b"test1", b"test2"],
            "large_str": ["test1" * 20, "test2" * 20],
            "large_bin": [b"test1" * 20, b"test2" * 20],
        }

        simple_features = hf_datasets.Features(
            {
                "int": hf_datasets.Value("int64"),
                "float": hf_datasets.Value("float64"),
                "str": hf_datasets.Value("string"),
                "bin": hf_datasets.Value("binary"),
                "large_str": hf_datasets.Value("large_string"),
                "large_bin": hf_datasets.Value("large_binary"),
            }
        )

        hf_simple_ds = hf_datasets.Dataset.from_dict(
            simple_data, features=simple_features
        )
        hf_complex_ds = hf_datasets.Dataset.from_dict(
            complex_data, features=complex_features
        )
        hf_mixed_ds = hf_datasets.DatasetDict(
            {
                "simple": hf_simple_ds,
                "complex": hf_complex_ds,
            }
        )

        m_get_config_names.return_value = ["simple"]
        m_load_dataset.return_value = hf_simple_ds
        Dataset.from_huggingface(
            name="simple",
            repo="simple",
            split="train",
            tags=["hf-0", "hf-1"],
            add_info=True,
        )
        simple_ds = dataset("simple")
        assert len(simple_ds) == 2
        assert simple_ds["simple/train/0"].features.int == 1
        assert simple_ds["simple/train/0"].features.float == 1.0
        assert simple_ds["simple/train/0"].features.str == "test1"
        assert simple_ds["simple/train/0"].features.bin == b"test1"
        assert simple_ds["simple/train/0"].features["_hf_subset"] == "simple"
        assert simple_ds["simple/train/0"].features["_hf_split"] == "train"
        large_str = simple_ds["simple/train/0"].features["large_str"]
        large_bin = simple_ds["simple/train/0"].features["large_bin"]
        assert isinstance(large_str, str)
        assert isinstance(large_bin, bytes)
        assert large_str == "test1" * 20
        assert large_bin == b"test1" * 20

        assert simple_ds["simple/train/1"].features.int == 2
        assert simple_ds["simple/train/1"].features["large_bin"] == b"test2" * 20

        m_get_config_names.return_value = ["complex"]
        m_load_dataset.return_value = hf_complex_ds
        complex_ds = Dataset.from_huggingface(
            name="complex", repo="complex", add_info=False
        )

        assert len(complex_ds) == 1
        assert complex_ds["complex/0"].features.list_int == [1, 2, 3]
        assert complex_ds["complex/0"].features.seq_img[0].shape == [1, 1, 3]
        assert complex_ds["complex/0"].features.seq_img[1].shape == [1, 1, 1]
        assert complex_ds["complex/0"].features.seq_dict["int"] == [1]
        assert complex_ds["complex/0"].features.seq_dict["str"] == ["test"]
        assert complex_ds["complex/0"].features.img.shape == [1, 1, 3]
        assert complex_ds["complex/0"].features.class_label == 1
        assert "_hf_subset" not in complex_ds["complex/0"].features
        assert "_hf_split" not in complex_ds["complex/0"].features
        _audio = complex_ds["complex/0"].features.audio
        assert isinstance(_audio, Audio)
        assert _audio.display_name == "simple.wav"
        assert _audio.mime_type == MIMEType.WAV

        m_get_config_names.return_value = ["mixed"]
        m_load_dataset.return_value = hf_mixed_ds
        mixed_ds = Dataset.from_huggingface(name="mixed", repo="mixed")

        assert len(mixed_ds) == 3
        assert mixed_ds["mixed/complex/0"].features.list_int == [1, 2, 3]
        assert mixed_ds["mixed/simple/0"].features.int == 1
        assert mixed_ds["mixed/simple/1"].features.int == 2

        m_get_config_names.return_value = ["simple", "mixed"]
        m_load_dataset.side_effect = [hf_simple_ds, hf_mixed_ds]
        multi_subsets_ds = Dataset.from_huggingface(name="multi", repo="multi")
        assert len(multi_subsets_ds) == 5
        assert multi_subsets_ds["simple/0"].features["_hf_subset"] == "simple"
        assert "_hf_split" not in multi_subsets_ds["simple/0"].features
        assert multi_subsets_ds["mixed/simple/0"].features["_hf_subset"] == "mixed"
        assert multi_subsets_ds["mixed/simple/0"].features["_hf_split"] == "simple"
        assert multi_subsets_ds["mixed/complex/0"].features["_hf_subset"] == "mixed"
        assert multi_subsets_ds["mixed/complex/0"].features["_hf_split"] == "complex"
