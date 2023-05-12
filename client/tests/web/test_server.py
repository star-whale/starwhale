import os
import sys
from asyncio import Future
from pathlib import Path
from unittest.mock import patch, MagicMock, PropertyMock

import httpx
from fastapi.testclient import TestClient

from starwhale.utils.fs import ensure_file
from starwhale.web.server import Server
from starwhale.base.uri.instance import Instance


def test_static_faked_response():
    svr = Server.default()
    client = TestClient(svr)

    apis = [
        "user/current",
        "system/version",
        "project/1",  # project/project_id
        "project/1/job/1",  # project/project_id/job/job_id
        "project/1/role",  # project/project_id/role
    ]
    for api in apis:
        resp = client.get(f"/api/v1/{api}")
        assert resp.status_code == 200
        assert "data" in resp.json()


def test_panel(tmpdir: Path):
    os.chdir(tmpdir)
    svr = Server.default()
    client = TestClient(svr)

    resp = client.get("/api/v1/panel/setting/1/2")
    assert resp.status_code == 200
    assert resp.json()["data"] == ""

    resp = client.post("/api/v1/panel/setting/1/2", content='{"a": 1}')
    assert resp.status_code == 200

    resp = client.get("/api/v1/panel/setting/1/2")
    assert resp.status_code == 200
    assert resp.json()["data"] == '{"a": 1}'


@patch(
    "starwhale.utils.config.SWCliConfigMixed.datastore_dir", new_callable=PropertyMock
)
def test_datastore_list_tables(root: MagicMock, tmpdir: Path):
    svr = Server.default()
    client = TestClient(svr)

    root.return_value = str(tmpdir)

    resp = client.post("/api/v1/datastore/listTables", content='{"prefix": ""}')
    assert resp.status_code == 200
    assert resp.json()["data"]["tables"] == []

    ensure_file(tmpdir / "a" / "foo.sw-datastore.zip", b"", parents=True)
    ensure_file(tmpdir / "b" / "c" / "foo.sw-datastore.zip", b"", parents=True)

    resp = client.post("/api/v1/datastore/listTables", content='{"prefix": ""}')
    assert resp.status_code == 200
    assert set(resp.json()["data"]["tables"]) == {"a/foo", "b/c/foo"}


@patch("starwhale.api._impl.data_store.LocalDataStore.scan_tables")
def test_datastore_query_table(mock_scan: MagicMock):
    svr = Server.default()
    client = TestClient(svr)

    mock_scan.return_value = []
    resp = client.post(
        "/api/v1/datastore/queryTable", json={"tableName": "a/b/c", "limit": 10}
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["columnTypes"] == []
    assert resp.json()["data"]["records"] == []

    mock_scan.return_value = [
        {"a": 1, "b": 2.0, "c": "3", "d": True, "e": None, "f": [1, 2, 3]}
    ]
    resp = client.post(
        "/api/v1/datastore/queryTable", json={"tableName": "a/b/c", "limit": 10}
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["columnTypes"] == [
        {"name": "a", "type": "INT64"},
        {"name": "b", "type": "FLOAT64"},
        {"name": "c", "type": "STRING"},
        {"name": "d", "type": "BOOL"},
        {"name": "e", "type": "UNKNOWN"},
        {"name": "f", "elementType": {"type": "INT64"}, "type": "LIST"},
    ]
    assert resp.json()["data"]["records"] == [
        {"a": "1", "b": "2.0", "c": "3", "d": "True", "e": "None", "f": "[1, 2, 3]"}
    ]


@patch("starwhale.api._impl.wrapper.Evaluation.get_metrics")
def test_datastore_query_summary(mock_get_metrics: MagicMock):
    svr = Server.default()
    client = TestClient(svr)

    mock_get_metrics.return_value = {"a": 1}
    query = {
        "tableName": "a/summary",
        "limit": 10,
        "filter": {
            "operator": "EQUAL",
            "operands": [{"columnName": "sys/id"}, {"intValue": "1"}],
        },
    }
    resp = client.post("/api/v1/datastore/queryTable", json=query)
    assert resp.status_code == 200
    assert resp.json()["data"]["columnTypes"] == [{"name": "a", "type": "INT64"}]
    assert resp.json()["data"]["records"] == [{"a": "1"}]
    mock_get_metrics.assert_called_once()


@patch("starwhale.utils.config.load_swcli_config")
@patch("httpx.AsyncClient.build_request")
@patch("httpx.AsyncClient.send")
def test_proxy(m_send: MagicMock, m_ac: MagicMock, m_sw_config: MagicMock):
    m_sw_config.return_value = {
        "instances": {
            "foo": {
                "current_project": "sw",
                "type": "cloud",
                "uri": "https://example.com",
                "sw_token": "token",
            }
        },
    }

    resp = httpx.Response(200, json={"data": "ok"})

    if sys.version_info >= (3, 8):
        m_send.return_value = resp
    else:
        f = Future()
        f.set_result(resp)
        m_send.return_value = f

    svr = Server.proxy(Instance(instance_alias="foo"))
    client = TestClient(svr)

    # test proxy api
    resp = client.post("/api/v1/datastore/queryTable", json={"tableName": "a/b/c"})
    assert m_send.call_count == 1
    assert resp.status_code == 200
    assert resp.json()["data"] == "ok"

    # test panel api
    client.get("/api/v1/panel/setting/1/2")
    # m_send not call again
    assert m_send.call_count == 1
