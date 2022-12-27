from http import HTTPStatus

import yaml
from pyfakefs.fake_filesystem_unittest import TestCase
from requests_mock import Mocker
from starwhale.base.cloud import CloudRequestMixed, CloudBundleModelMixin
from starwhale.base.type import URIType
from starwhale.base.uri import URI
from starwhale.consts import HTTPMethod
from starwhale.utils.config import SWCliConfigMixed


class TestCloudRequestMixed(TestCase):
    def test_get_bundle_size_from_resp(self):
        ins = CloudRequestMixed()

        item = {"size": 7}
        size = ins.get_bundle_size_from_resp("whatever", item)
        assert size == 7

        meta = {"dataset_byte_size": 8}
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
                    "list": [],
                    "total": 10,
                    "size": 10,
                }
            },
            status_code=HTTPStatus.OK,
        )

        cbm = CloudBundleModelMixin()
        _uri = URI("http://1.1.1.1/project/sw", expected_type=URIType.PROJECT)
        _models, _pager = cbm._fetch_bundle_all_list(_uri,
                                                     uri_typ=URIType.MODEL)

        assert _models == {}
