from http import HTTPStatus

import yaml
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import HTTPMethod
from starwhale.base.type import URIType
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uricomponents.project import Project


class TestCloudRequestMixed(TestCase):
    def test_get_bundle_size_from_resp(self):
        ins = CloudRequestMixed()

        item = {"size": 7}
        size = ins.get_bundle_size_from_resp("whatever", item)
        assert size == 7

        meta = {"dataset_summary": {"blobs_byte_size": 8}}
        item = {"meta": yaml.safe_dump(meta)}
        size = ins.get_bundle_size_from_resp("dataset", item)
        assert size == 8

        item = {"no meta": ""}
        size = ins.get_bundle_size_from_resp("dataset", item)
        assert size == 0

        item = {"meta": "no dataset byte size"}
        size = ins.get_bundle_size_from_resp("dataset", item)
        assert size == 0

    @Mocker()
    def test_bundle_list(self, rm: Mocker) -> None:
        sw = SWCliConfigMixed()
        sw.update_instance(
            uri="http://1.1.1.1", user_name="test", sw_token="123", alias="test"
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/sw/model?pageNum=1&pageSize=20",
            json={
                "data": {
                    "list": [
                        {"id": 2, "name": "mnist"},
                        {"id": 1, "name": "text_cls"},
                    ],
                    "total": 2,
                    "size": 2,
                }
            },
            status_code=HTTPStatus.OK,
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/sw/model?name=mnist",
            json={
                "data": {
                    "list": [
                        {"id": 2, "name": "mnist"},
                    ],
                    "total": 1,
                    "size": 1,
                }
            },
            status_code=HTTPStatus.OK,
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/sw/model/1/version",
            json={
                "data": {
                    "list": [
                        {"id": 2, "name": "tc_v2", "createdTime": 3000},
                        {"id": 1, "name": "tc_v1", "createdTime": 1000},
                    ],
                    "total": 2,
                    "size": 2,
                }
            },
            status_code=HTTPStatus.OK,
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/sw/model/2/version",
            json={
                "data": {
                    "list": [
                        {"id": 5, "name": "mnist_v3", "createdTime": 2000},
                        {"id": 4, "name": "mnist_v2", "createdTime": 2000},
                        {"id": 3, "name": "mnist_v1", "createdTime": 2000},
                    ],
                    "total": 3,
                    "size": 3,
                }
            },
            status_code=HTTPStatus.OK,
        )

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/sw/model/2/version?pageNum=1&pageSize=1",
            json={
                "data": {
                    "list": [
                        {"id": 5, "name": "mnist_v3", "createdTime": 2000},
                    ],
                    "total": 1,
                    "size": 1,
                }
            },
            status_code=HTTPStatus.OK,
        )

        cbm = CloudBundleModelMixin()
        _uri = Project("http://1.1.1.1/project/sw")
        _models, _pager = cbm._fetch_bundle_all_list(_uri, uri_typ=URIType.MODEL)

        assert len(_models.items()) == 2
        assert len(_models["[2] mnist"]) == 3
        assert len(_models["[1] text_cls"]) == 2
        assert _pager["current"] == 2
        assert _pager["remain"] == 0

        cbm = CloudBundleModelMixin()
        _uri = Project("http://1.1.1.1/project/sw")
        _models, _pager = cbm._fetch_bundle_all_list(
            _uri, uri_typ=URIType.MODEL, filter_dict={"name": "mnist"}
        )

        assert len(_models.items()) == 1
        assert len(_models["[2] mnist"]) == 3
        assert _pager["current"] == 1
        assert _pager["remain"] == 0

        cbm = CloudBundleModelMixin()
        _uri = Project("http://1.1.1.1/project/sw")
        _models, _pager = cbm._fetch_bundle_all_list(
            _uri, uri_typ=URIType.MODEL, filter_dict={"name": "mnist", "latest": True}
        )

        assert len(_models.items()) == 1
        assert len(_models["[2] mnist"]) == 1
        assert _pager["current"] == 1
        assert _pager["remain"] == 0
