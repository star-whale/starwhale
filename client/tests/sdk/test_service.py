import os
import json
import uuid
from pathlib import Path

from openapi_spec_validator import validate_spec, openapi_v30_spec_validator

from starwhale import Image
from starwhale.api.service import Output
from starwhale.core.model.model import StandaloneModel

from .. import ROOT_DIR
from .test_base import BaseTestCase


class ServiceTestCase(BaseTestCase):
    def setUp(self):
        self.root = Path(os.path.join(ROOT_DIR, "data", "sdk", "service"))
        super().setUp()

    def test_custom_class(self):
        svc = StandaloneModel._get_service("custom_class", self.root)
        assert list(svc.apis.keys()) == ["handler_foo", "bar", "baz"]

        api_foo = svc.apis["handler_foo"]
        assert isinstance(api_foo.input, Image)
        assert isinstance(api_foo.output, Output)

        api_bar = svc.apis["bar"]
        assert api_bar.input.load("foo") == "foo"
        assert api_bar.input.load("bar") == "bar"
        assert api_bar.output.dump("foo") == "hello foo".encode("utf-8")

        api_baz = svc.apis["baz"]
        assert api_baz.input.__class__.__name__ == "CustomInput"
        assert api_baz.output.__class__.__name__ == "CustomOutput"

        spec = svc.get_spec()
        assert list(spec.paths.keys()) == ["/handler_foo", "/bar", "/baz"]
        validate_spec(spec.to_dict(), validator=openapi_v30_spec_validator)

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
