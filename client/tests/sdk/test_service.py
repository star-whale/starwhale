import os
import json
from pathlib import Path

import pytest

from tests import ROOT_DIR, BaseTestCase
from starwhale.core.model.model import StandaloneModel
from starwhale.api._impl.service.types.types import ComponentSpec


class ServiceTestCase(BaseTestCase):
    def setUp(self):
        self.root = Path(os.path.join(ROOT_DIR, "data", "sdk", "service"))
        super().setUp()

    @pytest.mark.skip("enable this test when handler supports custom service class")
    def test_custom_class(self):
        svc = StandaloneModel._get_service(["custom_class"], self.root)
        assert list(svc.apis.keys()) == ["foo", "bar"]

        for i in svc.apis.values():
            assert i.input.__class__.__name__ == "list"
            assert i.input[0].__class__.__name__ == "CustomInput"
            assert i.output.__class__.__name__ == "list"
            assert i.output[0].__class__.__name__ == "CustomOutput"

        spec = svc.get_spec()
        assert len(spec["dependencies"]) == 2

    def test_default_class(self):
        svc = StandaloneModel._get_service(
            ["default_class:MyDefaultClass"],
            self.root,
        )
        assert list(svc.apis.keys()) == ["cmp"]
        spec = svc.get_spec()
        assert len(spec.apis) == 1
        assert spec.apis[0].uri == "cmp"
        assert spec.apis[0].inference_type == "question_answering"
        components = spec.apis[0].components_hint
        assert set(components) == {
            ComponentSpec(name="user_input", type="str"),
            ComponentSpec(name="history", type="list"),
            ComponentSpec(name="temperature", type="float"),
        }

    def test_class_without_api(self):
        svc = StandaloneModel._get_service(["no_api:NoApi"], self.root)
        assert len(svc.get_spec().apis) == 0

    @pytest.mark.skip("enable this test when handler supports custom service class")
    def test_custom_service(self):
        svc = StandaloneModel._get_service(["custom_service"], self.root)
        assert list(svc.apis.keys()) == ["baz"]

        with self.assertRaises(Exception) as e:
            # implementation called
            svc.serve("foo", 80)
        x, y = json.loads(str(e.exception))
        assert x == "foo"
        assert y == 80
