import io
import os
import re
import copy
import math
import time
import typing as t
import tempfile
import threading
from http import HTTPStatus
from pathlib import Path
from unittest.mock import patch, MagicMock
from concurrent.futures import as_completed, ThreadPoolExecutor

import yaml
import pytest
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config
from starwhale.consts import (
    HTTPMethod,
    ENV_POD_NAME,
    DEFAULT_PROJECT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.consts.env import SWEnv
from starwhale.utils.error import (
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.base.data_type import (
    Link,
    Text,
    Image,
    Binary,
    MIMEType,
    Sequence,
    BoundingBox,
    COCOObjectAnnotation,
)
from starwhale.base.uri.project import Project, get_remote_project_id
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.api._impl.wrapper import DatasetTableKind
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.copy import DatasetCopy
from starwhale.core.dataset.model import DatasetConfig, StandaloneDataset
from starwhale.api._impl.data_store import Link as DataStoreRawLink
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
from starwhale.base.client.models.models import ResponseMessageListString
from starwhale.api._impl.dataset.builder.mapping_builder import MappingDatasetBuilder

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
            "mixed_types_list": [
                1,
                "2",
                3.0,
                ["a", "b", "c"],
                Sequence([1, "b", {"a": 1, "b": "str"}]),
            ],
            "mixed_types_tuple": (1, "2", 3.0, ("a", "b")),
            "mixed_dict": {
                "a": 1,
                "b": "2",
                "c": [1, 3],
                "d": {"a": 1, "b": "2", "c": [1, "3", 1.1, "abc" * 100]},
                "e": (1, "a", [1, 2], [1, "2", 1.1]),
            },
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
        cloud_project_id = 1

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
            json={"data": {"id": cloud_project_id, "name": "project"}},
        )
        rm.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}/version/{dataset_version}/file",
            json={"data": {"uploadId": 1}},
        )

        rm.request(
            HTTPMethod.HEAD,
            f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}",
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
                f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}/hashedBlob/"
            ),
            status_code=HTTPStatus.NOT_FOUND,
        )

        upload_blob_req = rm.register_uri(
            HTTPMethod.POST,
            re.compile(
                f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}/hashedBlob/"
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
            "pythonType": "starwhale.base.data_type.BoundingBox",
            "name": "features/bbox",
        } in content["tableSchemaDesc"]["columnSchemaList"]

        assert {
            "attributes": [
                {"index": 0, "type": "INT64"},
                {"index": 1, "type": "STRING"},
                {"index": 2, "type": "FLOAT64"},
            ],
            "elementType": {"elementType": {"type": "STRING"}, "type": "TUPLE"},
            "name": "features/mixed_types_tuple",
            "type": "TUPLE",
        } in content["tableSchemaDesc"]["columnSchemaList"]

        assert {
            "elementType": {"type": "INT64"},
            "name": "features/list_int",
            "type": "LIST",
        } in content["tableSchemaDesc"]["columnSchemaList"]

        assert {
            "name": "features/seg",
            "type": "MAP",
            "keyType": {"type": "STRING"},
            "valueType": {
                "attributes": [
                    {"name": "as_mask", "type": "BOOL"},
                    {"name": "mask_uri", "type": "STRING"},
                    {"name": "_type", "type": "STRING"},
                    {"name": "display_name", "type": "STRING"},
                    {"name": "_mime_type", "type": "STRING"},
                    {
                        "attributes": [{"index": 2, "type": "INT64"}],
                        "elementType": {"type": "UNKNOWN"},
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
                            {
                                "keyType": {"type": "UNKNOWN"},
                                "name": "extra_info",
                                "type": "MAP",
                                "valueType": {"type": "UNKNOWN"},
                            },
                        ],
                        "name": "link",
                        "pythonType": "starwhale.base.data_type.Link",
                        "type": "OBJECT",
                    },
                ],
                "pythonType": "starwhale.base.data_type.Image",
                "type": "OBJECT",
            },
            "sparseKeyValuePairSchema": {
                "0": {
                    "keyType": {"type": "STRING"},
                    "valueType": {"elementType": {"type": "INT64"}, "type": "LIST"},
                }
            },
        } in content["tableSchemaDesc"]["columnSchemaList"]
        assert len(content["records"]) > 0

        for v in content["records"][0]["values"]:
            if v["key"] != "features/text":
                continue
            assert "fp" not in v["value"]

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
        cloud_project_id = 1

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/project",
            json={"data": {"id": cloud_project_id, "name": "project"}},
        )
        rm.request(
            HTTPMethod.HEAD,
            f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}/version/{dataset_version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}/hashedBlob/111",
        )

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}/version/{dataset_version}/tag",
            json=ResponseMessageListString(
                code="success",
                message="success",
                data=["t1", "t2"],
            ).dict(),
        )

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/{cloud_project_id}/dataset/{dataset_name}?versionUrl={dataset_version}",
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
                            "pythonType": "starwhale.base.data_type.Text",
                            "attributes": [
                                {"name": "_BaseArtifact__cache_bytes", "type": "BYTES"},
                                {
                                    "name": "link",
                                    "type": "OBJECT",
                                    "pythonType": "starwhale.base.data_type.Link",
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
                            "pythonType": "starwhale.base.data_type.BoundingBox",
                        },
                    ],
                    "records": [
                        {
                            "id": {"value": "idx-0", "type": "STRING"},
                            "features/text": {
                                "type": "OBJECT",
                                "pythonType": "starwhale.base.data_type.Text",
                                "value": {
                                    "_BaseArtifact__cache_bytes": {
                                        "type": "STRING",
                                        "value": "",
                                    },
                                    "link": {
                                        "type": "OBJECT",
                                        "pythonType": "starwhale.base.data_type.Link",
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
                                "pythonType": "starwhale.base.data_type.BoundingBox",
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

        tdb = TabularDataset(name=dataset_name, project=Project(DEFAULT_PROJECT))
        meta_list = list(tdb.scan())
        assert len(meta_list) == 1
        assert meta_list[0].id == "idx-0"
        assert meta_list[0].features["text"].link.uri == "111"
        bbox = meta_list[0].features["bbox"]
        assert isinstance(bbox, BoundingBox)
        assert bbox.x == 1 and bbox.y == 2
        assert bbox.width == 3 and bbox.height == 4


class TestDatasetSessionConsumption(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    @Mocker()
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
    def test_get_consumption(
        self, rm: Mocker, m_scan_id: MagicMock, m_conf: MagicMock
    ) -> None:
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
        rm.request(
            HTTPMethod.GET,
            "http://127.0.0.1:8081/api/v1/project/test",
            json={"data": {"id": 1, "name": ""}},
        )
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
    def test_standalone_tdsc_multi_thread(
        self, m_scan_id: MagicMock, m_conf: MagicMock
    ) -> None:
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "self"},
            },
            "storage": {"root": "/root"},
        }
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
        instance_uri = "http://1.1.1.1:8081"
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "test": {
                    "uri": instance_uri,
                    "current_project": "p",
                    "sw_token": "token",
                },
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": "/root"},
        }

        os.environ[ENV_POD_NAME] = ""
        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/p",
            json={"data": {"id": 1, "name": "p"}},
        )
        CloudTDSC(
            Resource(
                "mnist/version/latest",
                typ=ResourceType.dataset,
                project=Project("cloud://test/project/p"),
            ),
            "",
        )

        rm.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/test",
            json={"data": {"id": 1, "name": "test"}},
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
            "http://1.1.1.1:8081/api/v1/project/1/dataset/mnist/version/123/consume",
            json={
                "data": {
                    "start": "path/1",
                    "startType": "STRING",
                    "end": "path/100",
                    "endType": "STRING",
                },
                "code": "success",
                "message": "success",
            },
        )

        range_key = tdsc.get_scan_range()
        assert range_key == ("path/1", "path/100")
        assert len(mock_request.request_history) == 1  # type: ignore
        request = mock_request.request_history[0]  # type: ignore
        assert request.path == "/api/v1/project/1/dataset/mnist/version/123/consume"
        assert request.json() == {
            "batchSize": 50,
            "sessionId": "123",
            "consumerId": "pod-1",
            "endInclusive": False,
            "startInclusive": True,
            "processedData": [],
        }

        range_key = tdsc.get_scan_range(processed_keys=[(1, 1)])
        assert len(mock_request.request_history) == 2  # type: ignore
        assert range_key == ("path/1", "path/100")
        assert mock_request.request_history[1].json() == {  # type: ignore
            "batchSize": 50,
            "sessionId": "123",
            "consumerId": "pod-1",
            "endInclusive": False,
            "startInclusive": True,
            "processedData": [
                {
                    "end": "0000000000000001",
                    "startType": "INT64",
                    "endType": "INT64",
                    "start": "0000000000000001",
                }
            ],
        }

    @Mocker()
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
    def test_standalone_tdsc(
        self, rm: Mocker, m_scan_id: MagicMock, m_conf: MagicMock
    ) -> None:
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
            rm.get(
                "http://1.1.1.1:8082/api/v1/project/starwhale", json={"data": {"id": 1}}
            )
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
        for _ in range(0, 11):
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
            {
                "int": 1,
                "dict": {"a": 1, "b": 2},
                "list_dict": [{"a": 1}, {"a": 2}],
                "dict with int key": {1: "a"},
            },
        )
        assert info["int"] == 1
        assert info["dict"] == {"a": 1, "b": 2}
        assert info["list_dict"] == [{"a": 1}, {"a": 2}]
        assert info["dict with int key"] == {1: "a"}

        assert isinstance(info.data["dict"], dict)
        assert info.data["dict"] == {"a": 1, "b": 2}
        assert isinstance(info.data["list_dict"], list)
        assert isinstance(info.data["list_dict"][0], dict)
        assert info.data["list_dict"][0] == {"a": 1}

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
            Project("self"),
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
        td = TabularDataset(name="test", project=Project(DEFAULT_PROJECT))
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

        loaded_td = TabularDataset(name="test", project=Project(DEFAULT_PROJECT))
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
            TabularDataset(name="", project=Project())

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
        assert u_row_dict["features/b"] == {"c": 1}
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


class TestMappingDatasetBuilder(BaseTestCase):
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    def setUp(self) -> None:
        super().setUp()
        self.workdir = Path(self.local_storage) / "test"
        self.project = Project(DEFAULT_PROJECT)
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
            project=self.project,
        )

    def tearDown(self) -> None:
        super().tearDown()
        get_remote_project_id.cache_clear()

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
        exception_msg = (
            "RowPutThread raise exception: no support fp type for bin writer"
        )
        with self.assertRaisesRegex(threading.ThreadError, exception_msg):
            mdb.flush()

        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        mdb.put(DataRow(index=5, features={"bin": Binary(1)}))  # type: ignore
        mdb._rows_put_queue.join()
        with self.assertRaisesRegex(threading.ThreadError, exception_msg):
            mdb.put(DataRow(index=5, features={}))

        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        mdb.put(DataRow(index=5, features={"bin": Binary(1)}))  # type: ignore
        with self.assertRaisesRegex(threading.ThreadError, exception_msg):
            mdb.close()

    @pytest.mark.filterwarnings("ignore::pytest.PytestUnhandledThreadExceptionWarning")
    def test_rows_put_exception(self) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
        )
        with self.assertRaisesRegex(RuntimeError, "RowPutThread raise exception"):
            for i in range(0, 5):
                mdb.put(DataRow(index=i, features={"a": type("unknown")}))
            mdb.flush()

    @pytest.mark.filterwarnings("ignore::pytest.PytestUnhandledThreadExceptionWarning")
    @patch(
        "starwhale.api._impl.dataset.builder.mapping_builder.DatasetStorage.save_data_file"
    )
    def test_abs_handler_exception(self, mock_save_data_file: MagicMock) -> None:
        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=self.uri,
            blob_volume_bytes_size=10,
        )
        mock_save_data_file.side_effect = Exception("write error")

        with self.assertRaisesRegex(RuntimeError, "raise exception"):
            for i in range(0, 10):
                mdb.put(DataRow(index=i, features={"a": Text("aaa" * 100)}))
                mdb.flush()

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
        cloud_project = "cloud_pro"
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
            f"{instance_uri}/api/v1/project/{cloud_project}",
            json={"data": {"id": 1, "name": ""}},
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
                f"{instance_uri}/api/v1/project/1/dataset/{self.dataset_name}/hashedBlob/",
            ),
            json={"data": server_return_uri},
        )

        mdb = MappingDatasetBuilder(
            workdir=self.workdir,
            dataset_uri=Resource(
                "mnist",
                project=Project(f"cloud://foo/project/{cloud_project}"),
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
