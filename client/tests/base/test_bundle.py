from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.base.store import BundleField
from starwhale.base.bundle import BaseBundle


class TestBaseBundle(TestCase):
    def test_get_filter_dict(self):
        _cls = BaseBundle
        _fields = ["name", "owner", "latest"]
        _filter = ["name=mnist", "owner=starwhale", "latest", "other"]
        f_dict = _cls.get_filter_dict(filters=_filter, fields=_fields)
        print(f_dict)
        assert f_dict.get("name") == "mnist"
        assert f_dict.get("owner") == "starwhale"
        assert f_dict.get("latest") is not None
        assert not f_dict.get("other")

        _filter = {"name": "nmt", "latest": True, "other": True}
        f_dict = _cls.get_filter_dict(filters=_filter, fields=_fields)
        assert f_dict.get("name") == "nmt"
        assert f_dict.get("latest") is not None
        assert not f_dict.get("owner")
        assert not f_dict.get("other")

    def test_do_bundle_filter(self):
        _cls = BaseBundle
        _bf = BundleField(
            name="mnist",
            version="5o66ol6hj3erffoc3wnyzphymgdqjqrnwwezjesk",
            tags=["v1", "latest"],
            path=Path(),
            is_removed=False,
        )
        _filter = {"name": "nmt", "latest": True}
        assert not _cls.do_bundle_filter(_bf, _filter)

        _filter = {"name": "mnist", "latest": True}
        assert _cls.do_bundle_filter(_bf, _filter)

        _filter = {"name": "mn", "latest": True}
        assert _cls.do_bundle_filter(_bf, _filter)

        _bf = BundleField(
            name="mnist",
            version="5o66ol6hj3erffoc3wnyzphymgdqjqrnwwezjesk",
            tags=["v1"],
            path=Path(),
            is_removed=False,
        )

        _filter = {"name": "mnist", "latest": True}
        assert not _cls.do_bundle_filter(_bf, _filter)

        _filter = {"name": "mnist"}
        assert _cls.do_bundle_filter(_bf, _filter)
