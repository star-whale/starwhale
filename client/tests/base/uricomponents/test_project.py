from typing import Any
from unittest import TestCase
from unittest.mock import patch, MagicMock

from starwhale.base.uricomponents.project import Project


class MockInstance:
    def __init__(self, **kwargs: Any) -> None:
        ...

    info = {"current_project": "foo"}
    path = ""


class TestProject(TestCase):
    @patch("starwhale.base.uricomponents.project.Instance", MockInstance)
    def test_project(self) -> None:
        p = Project()
        assert p.name == "foo"

        p = Project("bar")
        assert p.name == "bar"

    @patch("starwhale.utils.config.load_swcli_config")
    def test_project_with_url_only(self, load_conf: MagicMock) -> None:
        load_conf.return_value = {"instances": {"foo": {"uri": "https://foo.com"}}}
        p = Project(uri="https://foo.com/project/bar")
        assert p.name == "bar"
        assert p.path == ""

        p = Project(uri="https://foo.com/project/bar/dataset/mnist/version/baz")
        assert p.name == "bar"
        assert p.path == "dataset/mnist/version/baz"
