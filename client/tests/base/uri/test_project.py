from typing import Any
from unittest import TestCase
from unittest.mock import patch, MagicMock

from starwhale.base.uri.project import Project
from starwhale.base.uri.exceptions import UriTooShortException


class MockInstance:
    def __init__(self, **kwargs: Any) -> None:
        ...

    info = {"current_project": "foo"}
    path = ""


class TestProject(TestCase):
    @patch("starwhale.base.uri.project.Instance", MockInstance)
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

    @patch("starwhale.utils.config.load_swcli_config")
    def test_parse_from_full_uri(self, load_conf: MagicMock) -> None:
        load_conf.return_value = {
            "instances": {"foo": {"uri": "https://foo.com"}, "local": {"uri": "local"}}
        }

        tests = {
            "https://foo.com/project/myproject/dataset/mnist": "myproject",
            "cloud://foo/project/myproject/dataset/mnist": "myproject",
            "foo/project/myproject/dataset/mnist": "myproject",
        }

        for uri, project in tests.items():
            p = Project.parse_from_full_uri(uri, ignore_rc_type=False)
            assert p.name == project

        p = Project.parse_from_full_uri(
            "foo/project/myproject/mnist", ignore_rc_type=True
        )
        assert p.name == "myproject"

    def test_parse_from_full_uri_exceptions(self) -> None:
        tests = (
            "foo//project/myproject/dataset/mnist",
            "https://foo.com/project/myproject/dataset//mnist",
        )

        for uri in tests:
            with self.assertRaisesRegex(Exception, "wrong format uri"):
                Project.parse_from_full_uri(uri, ignore_rc_type=False)

        tests = (
            "http://foo.com/project/self",
            "foo/project/self",
            "cloud://foo/project/self",
        )
        for uri in tests:
            with self.assertRaises(UriTooShortException):
                Project.parse_from_full_uri(uri, ignore_rc_type=True)
