import os
import json
from pathlib import Path

import pytest

from tests import ROOT_DIR, BaseTestCase
from starwhale.core.model.model import StandaloneModel
from starwhale.api._impl.service.service import Service
from starwhale.base.client.models.models import (
    ComponentValueSpecInt,
    ComponentSpecValueType,
    ComponentValueSpecFloat,
)
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
        assert spec.apis[0].inference_type == "llm_chat"
        components = spec.apis[0].components
        assert len(components) == 3
        for c in [
            ComponentSpec(
                name="temperature",
                component_spec_value_type=ComponentSpecValueType.float,
                component_value_spec_float=ComponentValueSpecFloat(default_val=0.5),
            ),
            ComponentSpec(
                name="top_k",
                component_spec_value_type=ComponentSpecValueType.int,
                component_value_spec_int=ComponentValueSpecInt(default_val=1),
            ),
            ComponentSpec(
                name="max_new_tokens",
                component_spec_value_type=ComponentSpecValueType.int,
                component_value_spec_int=ComponentValueSpecInt(
                    default_val=64, max=1024
                ),
            ),
        ]:
            assert c in components

    def test_class_without_api(self):
        svc = StandaloneModel._get_service(["no_api:NoApi"], self.root)
        assert svc.get_spec() is None

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

    def test_duplicate_api(self):
        svc = Service()

        def fake_func(x):
            return x

        def another_fake_func(x):
            return x

        svc.add_api(fake_func, "foo", None, None)
        with self.assertRaises(Exception) as e:
            svc.add_api(another_fake_func, "foo", None, None)
        assert "Duplicate" in str(e.exception)
