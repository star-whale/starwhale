from http import HTTPStatus

import yaml
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import HTTPMethod
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import ResourceType


class TestCloudRequestMixed(TestCase):
    def setUp(self) -> None:
        super().setUp()

        sw = SWCliConfigMixed()
        sw.update_instance(
            uri="http://1.1.1.1", user_name="test", sw_token="123", alias="test"
        )

    def test_get_bundle_size_from_resp(self):
        ins = CloudRequestMixed()

        meta = {"dataset_summary": {"blobs_byte_size": 8}}
        item = {"meta": yaml.safe_dump(meta)}
        size = ins.get_bundle_size_from_resp(ResourceType.dataset, item)
        assert size == 8

        item = {"no meta": ""}
        size = ins.get_bundle_size_from_resp(ResourceType.dataset, item)
        assert size == 0

        item = {"meta": "no dataset byte size"}
        size = ins.get_bundle_size_from_resp(ResourceType.dataset, item)
        assert size == 0

    @Mocker()
    def test_bundle_list(self, rm: Mocker) -> None:
        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/sw",
            json={"data": {"id": 1}},
        )
        req = rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/1/model",
            json={
                "data": {
                    "list": [
                        {
                            "id": 2,
                            "name": "mnist",
                            "version": {
                                "alias": "v1",
                                "latest": True,
                                "tags": ["t1", "t2"],
                                "name": "version1",
                                "createdTime": 0,
                            },
                        },
                        {
                            "id": 1,
                            "name": "text_cls",
                            "version": {
                                "alias": "v2",
                                "latest": False,
                                "tags": None,
                                "name": "version2",
                                "createdTime": 0,
                                "meta": yaml.safe_dump(
                                    {
                                        "dataset_summary": {"rows": 10},
                                        "environment": {
                                            "mode": "venv",
                                            "python": "3.7",
                                        },
                                    }
                                ),
                            },
                        },
                    ],
                    "total": 2,
                    "size": 2,
                    "pageNum": 1,
                    "pageSize": 10,
                    "pages": 1,
                    "prePage": 0,
                    "nextPage": 0,
                    "hasNextPage": False,
                    "hasPreviousPage": False,
                }
            },
            status_code=HTTPStatus.OK,
        )

        cbm = CloudBundleModelMixin()
        _uri = Project("http://1.1.1.1/project/sw")
        _models, _pager = cbm._fetch_bundle_all_list(
            _uri, uri_typ=ResourceType.model, page=10, size=1
        )
        assert len(_models) == 2
        assert _models["mnist"]["id"] == 2
        assert _models["mnist"]["name"] == "mnist"
        assert set(_models["mnist"]["tags"]) == set(["t1", "t2", "v1", "latest"])
        assert _models["text_cls"]["id"] == 1
        assert _models["text_cls"]["tags"] == ["v2"]
        assert _pager["current"] == 2
        assert _pager["remain"] == 0
        assert _pager["page"]["page_num"] == 1
        assert _models["text_cls"]["mode"] == "venv"
        assert "mode" not in _models["mnist"]
        assert _models["text_cls"]["python"] == "3.7"
        assert _models["text_cls"]["rows"] == 10

        assert req.call_count == 1
        assert req.last_request.qs["pagenum"] == ["10"]
        assert req.last_request.qs["pagesize"] == ["1"]
