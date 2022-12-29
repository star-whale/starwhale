import os
import json
from pathlib import Path

from starwhale.core.model.model import StandaloneModel

from .. import ROOT_DIR
from .test_base import BaseTestCase


class ServiceTestCase(BaseTestCase):
    def setUp(self):
        self.root = Path(os.path.join(ROOT_DIR, "data", "sdk", "service"))
        super().setUp()

    def test_custom_class(self):
        svc = StandaloneModel._get_service("custom_class", self.root)
        assert list(svc.apis.keys()) == ["foo", "bar"]

        for i in svc.apis.values():
            assert i.input.__class__.__name__ == "CustomInput"
            assert i.output.__class__.__name__ == "CustomOutput"

        spec = svc.get_spec()
        assert list(filter(bool, [i["api_name"] for i in spec["dependencies"]])) == [
            "predict",
            "predict_1",
        ]

    def test_default_class(self):
        svc = StandaloneModel._get_service("default_class:MyDefaultClass", self.root)
        assert list(svc.apis.keys()) == ["ppl", "handler_foo", "cmp"]

    def test_custom_service(self):
        svc = StandaloneModel._get_service("custom_service", self.root)
        assert list(svc.apis.keys()) == ["baz"]

        with self.assertRaises(Exception) as e:
            # implementation called
            svc.serve("foo", 80)
        x, y = json.loads(str(e.exception))
        assert x == "foo"
        assert y == 80
