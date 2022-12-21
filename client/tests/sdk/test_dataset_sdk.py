from __future__ import annotations

import io
import sys
import typing as t
from http import HTTPStatus
from pathlib import Path
from unittest.mock import MagicMock
from concurrent.futures import as_completed, ThreadPoolExecutor

import yaml
import numpy
import torch
import pytest
import torch.utils.data as tdata
from PIL import Image as PILImage
from requests_mock import Mocker

from starwhale import dataset
from starwhale.consts import HTTPMethod
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.error import ExistedError, NotFoundError, NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.dataset.type import (
    Text,
    Audio,
    Image,
    Video,
    Binary,
    BoundingBox,
    DatasetSummary,
    GrayscaleImage,
    COCOObjectAnnotation,
)
from starwhale.core.dataset.tabular import TabularDatasetInfo
from starwhale.api._impl.dataset.loader import DataRow

from .. import ROOT_DIR
from .test_base import BaseTestCase


class _DatasetSDKTestBase(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

    def _init_simple_dataset(self) -> URI:
        with dataset("mnist", create=True) as ds:
            for i in range(0, 10):
                ds.append(
                    DataRow(
                        index=i,
                        data=Binary(f"data-{i}".encode()),
                        annotations={"label": i},
                    )
                )
            ds.commit()
        return ds.uri

    def _init_simple_dataset_with_str_id(self) -> URI:
        with dataset("mnist", create=True) as ds:
            for i in range(0, 10):
                ds.append(
                    DataRow(
                        index=f"{i}",
                        data=Binary(f"data-{i}".encode()),
                        annotations={"label": i},
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


class TestDatasetSDK(_DatasetSDKTestBase):
    def test_create_from_empty(self) -> None:
        ds = dataset("mnist", create=True)
        assert ds.version != ""
        assert ds.project_uri.full_uri == "local/project/self/dataset/mnist"
        assert ds.uri.object.name == ds.name == "mnist"
        assert ds.uri.object.typ == URIType.DATASET
        assert ds.uri.object.version == ds.version
        assert ds.uri.project == "self"
        assert ds.uri.instance == "local"

        assert not ds.readonly
        assert ds._append_from_version == ""
        assert not ds._create_by_append
        assert len(ds) == 0
        assert bool(ds)

    def test_append(self) -> None:
        size = 11
        ds = dataset("mnist", create=True)
        assert len(ds) == 0
        ds.append(DataRow(index=0, data=Binary(b""), annotations={"label": 1}))
        assert len(ds) == 1
        for i in range(1, size):
            ds.append((i, Binary(), {"label": i}))
        assert len(ds) == size

        ds.append((Binary(), {"label": 1}))

        with self.assertRaises(TypeError):
            ds.append(1)

        with self.assertRaises(ValueError):
            ds.append((1, 1, 1, 1, 1))

        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert len(load_ds) == size + 1

        for i, d in enumerate(ds):
            assert d.index == i

    def test_extend(self) -> None:
        ds = dataset("mnist", create=True)
        assert len(ds) == 0
        size = 10
        ds.extend(
            [
                DataRow(index=i, data=Binary(), annotations={"label": i})
                for i in range(0, size)
            ]
        )
        ds.extend([])

        with self.assertRaises(TypeError):
            ds.extend(None)

        with self.assertRaises(TypeError):
            ds.extend([None])

        assert len(ds) == size
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert load_ds.exists()
        assert len(load_ds) == size
        assert load_ds[0].index == 0  # type: ignore
        assert load_ds[9].index == 9  # type: ignore

    def test_setitem(self) -> None:
        ds = dataset("mnist", create=True)
        assert len(ds) == 0
        assert ds._row_writer is None

        ds["index-2"] = DataRow(
            index="index-2", data=Binary(), annotations={"label": 2}
        )
        ds["index-1"] = DataRow(
            index="index-1", data=Binary(), annotations={"label": 1}
        )

        assert len(ds) == 2
        assert ds._row_writer is not None
        assert ds._row_writer._kw["dataset_name"] == ds.name
        assert ds._row_writer._kw["dataset_version"] == ds.version
        assert not ds._row_writer._kw["append"]

        ds["index-4"] = "index-4", Binary(), {"label": 4}
        ds["index-3"] = Binary(), {"label": 3}

        with self.assertRaises(ValueError):
            ds["index-5"] = (1,)

        with self.assertRaises(TypeError):
            ds["index-6"] = 1

        assert len(ds) == 4
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert len(load_ds) == 4
        index_names = [d.index for d in load_ds]
        assert index_names == ["index-1", "index-2", "index-3", "index-4"]

    def test_setitem_exceptions(self) -> None:
        ds = dataset("mnist", create=True)
        with self.assertRaises(TypeError):
            ds[1:3] = ((1, Binary(), {}), (2, Binary(), {}))

        with self.assertRaises(TypeError):
            ds[DataRow(1, Binary(), {})] = DataRow(1, Binary(), {})

    def test_parallel_setitem(self) -> None:
        ds = dataset("mnist", create=True)

        size = 100

        def _do_task(_start: int) -> None:
            for i in range(_start, size):
                ds.append(DataRow(index=i, data=Binary(), annotations={"label": i}))

        pool = ThreadPoolExecutor(max_workers=10)
        tasks = [pool.submit(_do_task, i * 10) for i in range(0, 9)]
        list(as_completed(tasks))

        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        items = list(load_ds)
        assert len(items) == size
        assert items[0].index == 0
        assert items[-1].index == 99

    def test_setitem_same_key(self) -> None:
        ds = dataset("mnist", create=True)
        ds.append(DataRow(1, Binary(b""), {"label": "1-1"}))
        assert len(ds) == 1

        for i in range(0, 10):
            ds[2] = Binary(b""), {"label": f"2-{i}"}

        # assert len(ds) == 2  TODO restore this case len(ds) after improving accuracy of _rows_cnt during building
        ds.append(DataRow(3, Binary(b""), {"label": "3-1"}))

        # assert len(ds) == 3 TODO restore this case len(ds) after improving accuracy of _rows_cnt during building
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert len(list(load_ds)) == 3
        assert load_ds[2].annotations == {"label": "2-9"}  # type: ignore
        assert len(load_ds) == 3

    def test_readonly(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)

        assert ds.readonly
        readonly_msg = "in the readonly mode"
        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds.append(DataRow(1, Binary(), {}))

        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds.extend([DataRow(1, Binary(), {})])

        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds[1] = Binary(), {}

        with self.assertRaisesRegex(RuntimeError, readonly_msg):
            ds.flush()

    def test_del_item_from_existed(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)

        with self.assertRaisesRegex(RuntimeError, "in the readonly mode"):
            del ds[1]

        ds = dataset(existed_ds_uri, create=True)
        del ds[0]
        ds.flush()

        del ds[0]
        assert len(ds) == 9
        del ds[6:]
        assert len(ds) == 5

        ds.commit()
        ds.close()

        ds = dataset(ds.uri)
        items = [d.index for d in ds]
        assert items == [1, 2, 3, 4, 5]

    def test_del_not_found(self) -> None:
        ds = dataset("mnist", create=True)
        del ds[0]
        del ds["1"]
        del ds["not-found"]

    def test_del_item_from_empty(self) -> None:
        with dataset("mnist", create=True) as ds:
            for i in range(0, 3):
                ds.append(DataRow(i, Binary(), {"label": i}))

            ds.flush()
            del ds[0]
            del ds[1]
            ds.commit()

        reopen_ds = dataset(ds.uri)
        assert len(reopen_ds) == 1
        items = list(reopen_ds)
        assert len(items) == 1
        assert items[0].index == 2

    def test_build_no_data(self) -> None:
        ds = dataset("mnist", create=True)
        msg = "no data to build dataset"
        with self.assertRaisesRegex(RuntimeError, msg):
            ds.build()

        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri, create=True)
        with self.assertRaisesRegex(RuntimeError, msg):
            ds.build()

    def test_build_from_handler_empty(self) -> None:
        def _handler() -> t.Generator:
            for i in range(0, 100):
                yield i, Binary(), {"label": i}

        ds = dataset("mnist", create=True)
        ds.build_handler = _handler
        ds.commit()
        ds.close()

        reopen_ds = dataset(ds.uri)
        assert len(reopen_ds) == 100
        assert reopen_ds[0].index == 0  # type: ignore
        assert reopen_ds[0].annotations == {"label": 0}  # type: ignore
        items = list(reopen_ds)
        assert items[-1].index == 99
        assert items[-1].annotations == {"label": 99}

    def test_build_from_handler_existed(self) -> None:
        def _handler() -> t.Generator:
            for i in range(0, 100):
                yield f"label-{i}", Binary(), {"label": i}

        existed_ds_uri = self._init_simple_dataset_with_str_id()
        with dataset(existed_ds_uri, create_from_handler=_handler) as ds:
            assert ds._create_by_append
            ds.commit()

        reopen_ds = dataset(ds.uri)
        assert len(reopen_ds) == 110
        summary = reopen_ds.summary()
        assert isinstance(summary, DatasetSummary)
        assert summary.rows == 110
        assert not summary.include_link
        assert not summary.include_user_raw
        assert summary.increased_rows == 100
        items = list(reopen_ds)
        assert len(items) == 110
        assert items[0].index == "0"
        assert items[-1].index == "label-99"

    def test_build_from_handler_with_copy_src(self) -> None:
        def _handler() -> t.Generator:
            for i in range(0, 100):
                yield DataRow(f"label-{i}", Binary(), {"label": i})

        workdir = Path(self.local_storage) / ".data"
        ensure_dir(workdir)
        ensure_file(workdir / "t.py", content="")

        ds = dataset("mnist", create_from_handler=_handler)
        ds.build_with_copy_src(workdir)
        ds.commit()
        ds.close()

        reopen_ds = dataset(ds.uri)
        assert reopen_ds.exists()

        _uri = reopen_ds.uri
        dataset_dir = (
            Path(self.local_storage)
            / _uri.project
            / "dataset"
            / reopen_ds.name
            / reopen_ds.version[:2]
            / f"{reopen_ds.version}.swds"
            / "src"
        )
        assert dataset_dir.exists()
        assert (dataset_dir / "t.py").exists()

    def test_forbid_handler(self) -> None:
        ds = dataset("mnist", create=True)
        for i in range(0, 3):
            ds.append(DataRow(i, Binary(), {"label": i}))

        assert ds._trigger_icode_build
        assert not ds._trigger_handler_build

        with self.assertRaisesRegex(
            RuntimeError, "dataset append by interactive code has already been called"
        ):
            ds.build_handler = MagicMock()

    def test_forbid_icode(self) -> None:
        ds = dataset("mnist", create=True)
        ds.build_handler = MagicMock()
        assert ds._trigger_handler_build
        assert not ds._trigger_icode_build

        msg = "no support build from handler and from cache code at the same time"
        with self.assertRaisesRegex(NoSupportError, msg):
            ds.append(DataRow(1, Binary(), {"label": 1}))

        with self.assertRaisesRegex(NoSupportError, msg):
            ds.extend([DataRow(1, Binary(), {"label": 1})])

        with self.assertRaisesRegex(NoSupportError, msg):
            ds[1] = DataRow(1, Binary(), {"label": 1})

        with self.assertRaisesRegex(NoSupportError, msg):
            del ds[1]

        ds = dataset("mnist", create_from_handler=MagicMock())
        assert ds._trigger_handler_build
        assert not ds._trigger_icode_build
        with self.assertRaisesRegex(NoSupportError, msg):
            ds.append(DataRow(1, Binary(), {"label": 1}))

        with self.assertRaisesRegex(NoSupportError, msg):
            ds.extend([DataRow(1, Binary(), {"label": 1})])

        with self.assertRaisesRegex(NoSupportError, msg):
            ds[1] = DataRow(1, Binary(), {"label": 1})

        with self.assertRaisesRegex(NoSupportError, msg):
            del ds[1]

    def test_close(self) -> None:
        ds = dataset("mnist", create=True)
        ds.close()

        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        ds.close()
        ds.close()

    def test_create_from_existed(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri, create=True)

        assert ds.version != existed_ds_uri.object.version
        assert ds.name == existed_ds_uri.object.name
        assert ds.project_uri.project == existed_ds_uri.project
        assert ds.version == ds.uri.object.version
        assert not ds.readonly
        assert not ds.exists()
        assert ds._append_from_version == existed_ds_uri.object.version
        assert ds._create_by_append
        assert len(ds) == 10
        ds.flush()

        ds.append(DataRow(index=1, data=Binary(b""), annotations={"label": 101}))
        # assert len(ds) == 10 TODO restore this case len(ds) after improving accuracy of _rows_cnt during building
        ds.append(DataRow(index=100, data=Binary(b""), annotations={"label": 100}))
        # assert len(ds) == 11 TODO restore this case len(ds) after improving accuracy of _rows_cnt during building
        ds.append(DataRow(index=101, data=Binary(b""), annotations={"label": 101}))
        ds.commit()
        ds.close()

        load_ds = dataset(ds.uri)
        assert load_ds[1].annotations == {"label": 101}  # type: ignore
        assert [d.index for d in load_ds] == list(range(0, 10)) + [100, 101]
        assert len(load_ds) == 12
        _summary = load_ds.summary()
        assert _summary is not None
        assert _summary.rows == 12

    def test_load_from_empty(self) -> None:
        with self.assertRaises(ValueError):
            dataset("mnist")

        with self.assertRaises(ExistedError):
            dataset("mnist/version/not_found")

    def test_load_from_existed(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        assert ds.version == ds.uri.object.version == existed_ds_uri.object.version
        assert ds.readonly
        assert ds.name == existed_ds_uri.object.name

        _summary = ds.summary()
        assert _summary is not None
        assert _summary.rows == len(ds) == 10
        assert ds._append_from_version == ""
        assert not ds._create_by_append

        _d = ds[0]
        assert isinstance(_d, DataRow)
        assert _d.index == 0
        assert _d.data == Binary(b"data-0")
        assert _d.annotations == {"label": 0}

    def test_load_with_tag(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        name = existed_ds_uri.object.name
        ds = dataset(f"{name}/version/latest")
        assert ds.exists()
        assert ds.version == existed_ds_uri.object.version

    def test_load_with_short_version(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        name = existed_ds_uri.object.name
        version = existed_ds_uri.object.version
        ds = dataset(f"{name}/version/{version[:7]}")
        assert ds.exists()
        assert ds.version == existed_ds_uri.object.version

    def test_iter(self) -> None:
        existed_ds_uri = self._init_simple_dataset()
        ds = dataset(existed_ds_uri)
        items = list(ds)
        assert len(items) == 10
        assert items[0].index == 0

        ds = dataset(existed_ds_uri)
        cnt = 0
        for item in ds:
            cnt += 1
            assert isinstance(item, DataRow)
        assert cnt == 10

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

    def test_tags(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        tags = list(ds.tags)
        assert tags == ["latest", "v0"]

        ds.tags.add("new_tag1")
        ds.tags.add(["new_tag2", "new_tag3"])

        tags = list(ds.tags)
        assert set(tags) == set(["latest", "v0", "new_tag1", "new_tag2", "new_tag3"])

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
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1/api/v1/project/self/dataset/not_found/version/1234",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )

        with self.assertRaisesRegex(ExistedError, "was not found fo load"):
            dataset("http://1.1.1.1/project/self/dataset/not_found/version/1234")

        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1/api/v1/project/self/dataset/mnist/version/1234",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/self/dataset/mnist",
            json={
                "data": {
                    "versionMeta": yaml.safe_dump(
                        {"dataset_summary": DatasetSummary(rows=101).asdict()}
                    )
                }
            },
            status_code=HTTPStatus.OK,
        )

        ds = dataset("http://1.1.1.1/project/self/dataset/mnist/version/1234")
        assert ds.exists()
        _summary = ds.summary()
        assert _summary is not None
        assert _summary.rows == 101

    @Mocker()
    def test_cloud_build_from_icode(self, rm: Mocker) -> None:
        sw = SWCliConfigMixed()
        sw.update_instance(
            uri="http://1.1.1.1", user_name="test", sw_token="123", alias="test"
        )

        manifest_req = rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/self/dataset/mnist",
            status_code=HTTPStatus.NOT_FOUND,
        )

        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1/api/v1/datastore/scanTable",
            json={"data": {}},
        )

        update_table_req = rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1/api/v1/datastore/updateTable",
        )

        ds = dataset("http://1.1.1.1/project/self/dataset/mnist", create=True)
        assert manifest_req.call_count == 0

        upload_file_req = rm.request(
            HTTPMethod.POST,
            f"http://1.1.1.1/api/v1/project/self/dataset/mnist/version/{ds.version}/file",
            json={"data": {"upload_id": "123"}},
        )

        _store = ds._Dataset__core_dataset.store  # type: ignore
        tmp_dir = _store.tmp_dir
        snapshot_workdir = _store.snapshot_workdir

        cnt = 10
        for i in range(0, cnt):
            ds.append(
                DataRow(
                    index=i, data=Binary(f"data-{i}".encode()), annotations={"label": i}
                )
            )

        ds.flush()
        assert tmp_dir.exists()
        assert len(list(tmp_dir.iterdir())) != 0
        assert not snapshot_workdir.exists()
        assert update_table_req.called
        assert not upload_file_req.called

        ds.commit()
        assert not tmp_dir.exists()
        assert not snapshot_workdir.exists()
        assert upload_file_req.called

        ds.close()

    @Mocker()
    def test_cloud_build_no_support(self, rm: Mocker) -> None:
        with self.assertRaisesRegex(
            NoSupportError, "no support to build cloud dataset directly"
        ):
            ds = dataset("http://1.1.1.1/project/self/dataset/mnist", create=True)
            ds.build_handler = MagicMock()
            ds.commit()

        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1/api/v1/project/self/dataset/mnist/version/1234",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )

        with self.assertRaisesRegex(
            NoSupportError, "Can't build dataset from the existed cloud dataset uri"
        ):
            dataset(
                "http://1.1.1.1/project/self/dataset/mnist/version/1234", create=True
            )

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
        ds = dataset("mnist", create=True)
        ds.append(DataRow(1, Binary(b"123"), {"label": 1}))

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
        assert isinstance(m, dict)
        assert m["name"] == ds.name
        assert m["version"] == ds.version
        assert m["tags"] == ["latest", "v0"]
        assert m["project"] == ds.project_uri.project

        empty_ds = dataset("mnist", create=True)
        m = empty_ds.manifest()
        assert m == {}

    def test_remove_recover(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        list_info, _ = ds.list(ds.project_uri, fullname=True)
        assert isinstance(list_info, list)
        assert list_info[0]["name"] == ds.name
        assert list_info[0]["version"] == ds.version
        assert list_info[0]["tags"] == ["latest", "v0"]

        ds.remove()
        with self.assertRaisesRegex(RuntimeError, "failed to remove dataset"):
            ds.remove()

        list_info, _ = ds.list(ds.project_uri, fullname=True)
        assert list_info == []

        ds.recover()
        with self.assertRaisesRegex(RuntimeError, "failed to recover dataset"):
            ds.recover()

        list_info, _ = ds.list(ds.project_uri, fullname=True)
        assert list_info[0]["version"] == ds.version

    def test_history(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        str_ds = dataset(existed_ds_uri)

        history = str_ds.history()
        assert len(history) == 1

        existed_int_ds_uri = self._init_simple_dataset()
        int_ds = dataset(existed_int_ds_uri)

        history = int_ds.history()
        assert len(history) == 2
        assert history[0]["name"] == history[1]["name"] == int_ds.name
        assert {history[0]["version"], history[1]["version"]} == {
            int_ds.version,
            str_ds.version,
        }

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
        ds = dataset(existed_ds_uri)

        head = ds.head(n=0)
        assert len(head) == 0

        head = ds.head(n=1)
        assert len(head) == 1
        assert head[0]["index"] == 0
        assert "raw" not in head[0]["data"]

        head = ds.head(n=2)
        assert len(head) == 2
        assert head[0]["index"] == 0
        assert head[1]["index"] == 1
        assert "raw" not in head[0]["data"]
        assert "raw" not in head[1]["data"]

        head = ds.head(n=2, show_raw_data=True)
        assert len(head) == 2
        assert head[0]["data"]["raw"] == b"data-0"
        assert head[1]["data"]["raw"] == b"data-1"

    @Mocker()
    def test_copy(self, rm: Mocker) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        sw = SWCliConfigMixed()
        sw.update_instance(
            uri="http://1.1.1.1", user_name="test", sw_token="123", alias="test"
        )

        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1/api/v1/project/self/dataset/mnist/version/{ds.version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )

        rm.request(
            HTTPMethod.POST,
            f"http://1.1.1.1/api/v1/project/self/dataset/mnist/version/{ds.version}/file",
            json={"data": {"upload_id": "123"}},
        )

        ds.copy("cloud://test/project/self")


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
        assert items[0][1] == ds["0"].annotations  # type: ignore
        assert isinstance(items[0][0], Binary)

    def test_skip_default_transform_with_batch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        torch_ds = ds.to_pytorch(skip_default_transform=True)
        torch_loader = tdata.DataLoader(torch_ds, batch_size=2)

        with self.assertRaisesRegex(
            TypeError,
            "default_collate: batch must contain tensors, numpy arrays, numbers, dicts or lists; found <class 'starwhale.core.dataset.type.Binary'>",
        ):
            list(torch_loader)

    def test_binary_type_without_batch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        assert isinstance(ds["0"].data, Binary)  # type: ignore

        torch_loader = tdata.DataLoader(
            ds.to_pytorch(skip_default_transform=False), batch_size=None
        )
        items = list(torch_loader)
        assert len(items) == 10

    def test_binary_type_with_batch(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)
        assert isinstance(ds["0"].data, Binary)  # type: ignore

        torch_loader = tdata.DataLoader(
            ds.to_pytorch(skip_default_transform=False), batch_size=2
        )
        items = list(torch_loader)
        assert len(items) == 5
        first_item = items[0]
        assert isinstance(first_item, list)
        assert len(first_item) == 2
        assert first_item[0] == (b"data-0", b"data-1")

        assert isinstance(first_item[1], dict)
        assert list(first_item[1].keys()) == ["label"]
        assert isinstance(first_item[1]["label"], torch.Tensor)
        assert torch.equal(torch.tensor([0, 1]), first_item[1]["label"])

    def test_keep_index(self) -> None:
        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        torch_ds = ds.to_pytorch(drop_index=False)
        torch_loader = tdata.DataLoader(torch_ds, batch_size=5)

        item = next(iter(torch_loader))
        assert isinstance(item, list)
        assert item[0] == tuple([f"{i}" for i in range(0, 5)])

    def test_use_custom_transform(self) -> None:
        with dataset("mnist", create=True) as ds:
            for i in range(0, 10):
                ds.append((Text(f"data-{i}"), {"label": i}))

            ds.commit()

        def _custom_transform(data: t.Any) -> t.Any:
            if isinstance(data, Text):
                return f"custom-{data.to_str()}"
            else:
                return data

        torch_loader = tdata.DataLoader(
            dataset(ds.uri).to_pytorch(transform=_custom_transform), batch_size=1
        )
        item = next(iter(torch_loader))
        assert isinstance(item, list) and len(item) == 2
        assert item[0][0] == "custom-data-0"

    def test_complex_transform(self) -> None:
        ds = dataset("mnist", create=True)
        for i in range(0, 10):
            annotations = {
                "int": 1,
                "float": 1.1,
                "tuple": (1, 2, 3),
                "list": [1, 2, 3],
                "map": {"key": i},
                "str": f"str-{i}",
                "bytes": f"bytes-{i}".encode(),
            }
            ds.append((Text(f"data-{i}"), annotations))
        ds.commit()
        torch_loader = tdata.DataLoader(dataset(ds.uri).to_pytorch(), batch_size=2)
        item = next(iter(torch_loader))

        assert torch.equal(item[1]["int"], torch.tensor([1, 1]))
        assert torch.equal(
            item[1]["float"], torch.tensor([1.1000, 1.1000], dtype=torch.float64)
        )
        assert torch.equal(item[1]["tuple"][0], torch.tensor([1, 1]))
        assert torch.equal(item[1]["map"]["key"], torch.tensor([0, 1]))
        assert item[1]["str"] == ["str-0", "str-1"]
        assert item[1]["bytes"] == [b"bytes-0", b"bytes-1"]

    def test_image_transform(self) -> None:
        ds = dataset("mnist", create=True)
        for i in range(1, 10):
            _img = self._create_real_image((i, i, i))
            ds.append((Image(_img), {"label": i}))

        ds.commit()

        torch_loader = tdata.DataLoader(dataset(ds.uri).to_pytorch(), batch_size=2)
        item = next(iter(torch_loader))
        assert torch.equal(
            item[0],
            torch.tensor(
                [
                    [[[1, 1, 1], [1, 1, 1]], [[1, 1, 1], [1, 1, 1]]],
                    [[[2, 2, 2], [2, 2, 2]], [[2, 2, 2], [2, 2, 2]]],
                ],
                dtype=torch.uint8,
            ),
        )

    def test_audio_transform(self) -> None:
        with dataset("mnist", create=True) as ds:
            _audio = self._create_real_audio()
            ds.append((Audio(_audio), {"label": 1}))
            ds.commit()

        torch_loader = tdata.DataLoader(dataset(ds.uri).to_pytorch(), batch_size=2)
        item = next(iter(torch_loader))
        assert isinstance(item[0], torch.Tensor)
        assert len(item[0]) != 0
        assert item[0].dtype == torch.float64


# TODO: wait for tensorflow release for python3.11
# https://github.com/tensorflow/tensorflow/issues/58032
skip_py311 = pytest.mark.skipif(
    sys.version_info > (3, 10),
    reason="skip python3.11, because tensorflow does not release the related wheel package.",
)


@skip_py311
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

    def test_tf_dataset_drop_index(self) -> None:
        import tensorflow as tf

        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        tf_ds = ds.to_tensorflow(drop_index=True)
        assert isinstance(tf_ds, tf.data.Dataset)

        items = list(tf_ds)
        assert len(items[0]) == 2
        assert isinstance(items[0][0], tf.Tensor)
        assert tf.equal(items[0][0], tf.constant(b"data-0"))
        assert tf.equal(items[0][1]["label"], tf.constant(0, dtype=tf.int64))
        assert len(items) == len(ds)

    def test_tf_dataset_with_index(self) -> None:
        import tensorflow as tf

        existed_ds_uri = self._init_simple_dataset_with_str_id()
        ds = dataset(existed_ds_uri)

        tf_ds = ds.to_tensorflow(drop_index=False)
        assert isinstance(tf_ds, tf.data.Dataset)

        items = list(tf_ds)
        assert len(items[0]) == 3
        assert tf.equal(items[0][0], tf.constant("0"))

        batch_ds = tf_ds.batch(2, drop_remainder=True)
        items = list(batch_ds.as_numpy_iterator())
        assert len(items)
        assert items[0][0].tolist() == [b"0", b"1"]
        assert items[0][1].tolist() == [b"data-0", b"data-1"]
        assert items[0][2]["label"].tolist() == [0, 1]
