from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.base.store import BundleField
from starwhale.base.bundle import BaseBundle
from starwhale.base.models.base import ListFilter


class TestBaseBundle(TestCase):
    def test_get_filter_dict(self):
        _cls = BaseBundle
        raw = ["name=mnist", "owner=starwhale", "latest", "other"]
        _filter = _cls.get_list_filter(filters=raw)
        assert _filter.name == "mnist"
        assert _filter.owner == "starwhale"
        assert _filter.latest

        assert _cls.get_list_filter(filters=None) is None

    def test_do_bundle_filter(self):
        _cls = BaseBundle
        _bf = BundleField(
            name="mnist",
            version="5o66ol6hj3erffoc3wnyzphymgdqjqrnwwezjesk",
            tags=["v1", "latest"],
            path=Path(),
            is_removed=False,
        )
        _filter = ListFilter(name="nmt", latest=True)
        assert not _cls.do_bundle_filter(_bf, _filter)

        _filter = ListFilter(name="mnist", latest=True)
        assert _cls.do_bundle_filter(_bf, _filter)

        _filter = ListFilter(name="mn", latest=True)
        assert _cls.do_bundle_filter(_bf, _filter)

        _bf = BundleField(
            name="mnist",
            version="5o66ol6hj3erffoc3wnyzphymgdqjqrnwwezjesk",
            tags=["v1"],
            path=Path(),
            is_removed=False,
        )

        _filter = ListFilter(name="mnist", latest=True)
        assert not _cls.do_bundle_filter(_bf, _filter)

        _filter = ListFilter(name="mnist")
        assert _cls.do_bundle_filter(_bf, _filter)
