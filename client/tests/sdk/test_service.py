import os
import json
import uuid
from pathlib import Path
from unittest import TestCase

from tests import ROOT_DIR
from starwhale import Image
from starwhale.api.service import Response
from starwhale.core.model.model import StandaloneModel


class ServiceTestCase(TestCase):
    def setUp(self):
        self.root = Path(os.path.join(ROOT_DIR, "data", "sdk", "service"))

    def test_custom_class(self):
        svc = StandaloneModel._get_service("custom_class", self.root)
        assert list(svc.apis.keys()) == ["handler_foo", "bar", "baz"]

        api_foo = svc.apis["handler_foo"]
        assert isinstance(api_foo.request, Image)
        assert isinstance(api_foo.response, Response)

        api_bar = svc.apis["bar"]
        assert api_bar.request.load("foo") == "foo"
        assert api_bar.request.load("bar") == "bar"
        assert api_bar.response.dump("foo") == "hello foo".encode("utf-8")

        api_baz = svc.apis["baz"]
        print(api_baz.request)
        assert api_baz.request.__class__.__name__ == "CustomInput"
        assert api_baz.response.__class__.__name__ == "CustomOutput"

    def test_default_class(self):
        svc = StandaloneModel._get_service("default_class:MyDefaultClass", self.root)
        assert list(svc.apis.keys()) == ["ppl", "handler_foo", "cmp"]
        req = uuid.uuid1().bytes
        assert svc.apis["ppl"].view_func(req) == req

    def test_custom_service(self):
        svc = StandaloneModel._get_service("custom_service", self.root)
        assert list(svc.apis.keys()) == ["baz"]

        with self.assertRaises(Exception) as e:
            # implementation called
            svc.serve("foo", 80, ["a", "b"])
        x, y, z = json.loads(str(e.exception))
        assert x == "foo"
        assert y == 80
        assert z == ["a", "b"]
