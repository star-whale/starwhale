import yaml
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.base.cloud import CloudRequestMixed


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
