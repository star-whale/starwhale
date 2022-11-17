from typing import Optional
from dataclasses import dataclass
from unittest.mock import Mock, patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.base.uricomponents.project import Project
from starwhale.base.uricomponents.resource import Resource, ResourceType
from starwhale.base.uricomponents.exceptions import NoMatchException


class MockLocalInstance:
    is_local = True

    def __str__(self) -> str:
        return "local"


class TestResource(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def test_resource_base(self) -> None:
        # name with type and project
        p = Mock(spec=Project)
        p.instance = MockLocalInstance()
        p.name = "self"
        p.path = ""
        r = Resource(uri="mnist", typ=ResourceType.dataset, project=p)
        assert r.name == "mnist"
        assert r.typ == ResourceType.dataset
        assert r.version is None
        assert r.to_uri().raw == "local/project/self/dataset/mnist/version/latest"

        # {name}/version/{ver}
        r = Resource(uri="mnist/version/foo", typ=ResourceType.dataset, project=p)
        assert r.name == "mnist"
        assert r.typ == ResourceType.dataset
        assert r.version == "foo"
        assert r.to_uri().raw == "local/project/self/dataset/mnist/version/foo"

        r = Resource(uri="foo/bar", typ=ResourceType.dataset, project=p)
        assert r.name == "foo"
        assert r.typ == ResourceType.dataset
        assert r.version == "bar"
        assert r.to_uri().raw == "local/project/self/dataset/foo/version/bar"

        with self.assertRaises(Exception):
            Resource(uri="mnist/foo/bar", typ=ResourceType.dataset, project=p)

    @patch("starwhale.base.uricomponents.resource.Project.parse")
    def test_with_full_uri(self, mock_parse: MagicMock) -> None:
        ins = mock_parse.return_value
        ins.path = "dataset/mnist/version/foo"
        uri = "local/project/self/dataset/mnist/version/foo"
        r = Resource(uri)
        assert r.name == "mnist"
        assert r.version == "foo"
        assert r.typ == ResourceType.dataset
        assert mock_parse.call_args(uri)

    @patch("starwhale.base.uricomponents.resource.Project.parse")
    def test_with_full_uri_no_version(self, mock_parse: MagicMock) -> None:
        ins = mock_parse.return_value
        ins.path = "dataset/mnist"
        uri = "local/project/self/dataset/mnist"
        r = Resource(uri)
        assert r.name == "mnist"
        assert r.version is None
        assert r.typ == ResourceType.dataset
        assert mock_parse.call_args(uri)

    @patch("starwhale.base.uricomponents.resource.glob")
    def test_version_only_with_project(self, mock_glob: MagicMock) -> None:
        mock_glob.return_value = ["/root/project/self/runtime/mnist/fo/foo"]
        project = Mock(spec=Project)
        project.instance = MockLocalInstance()
        project.name = "self"
        r = Resource("foo", project=project)
        assert r.name == "mnist"
        assert r.version == "foo"
        assert r.typ == ResourceType.runtime
        assert r.to_uri().raw == "local/project/self/runtime/mnist/version/foo"

    @patch("starwhale.base.uricomponents.resource.glob")
    @patch("starwhale.base.uricomponents.resource.Project.parse")
    def test_version_only_no_match(
        self, mock_parse: MagicMock, mock_glob: MagicMock
    ) -> None:
        p = Mock(spec=Project)
        mock_parse.return_value = p
        p.path = ""
        p.name = "self"
        p.instance = MockLocalInstance()
        mock_glob.return_value = []
        # name only will be treated as a version
        with self.assertRaises(NoMatchException):
            Resource("mnist")

    @patch("starwhale.utils.config.load_swcli_config")
    def test_parsing_browser_url(self, load_conf: MagicMock):
        @dataclass
        class Expect:
            instance: str
            project: str
            typ: ResourceType
            name: Optional[str] = None
            version: Optional[str] = None

            def __eq__(self, other: Resource):
                return (
                    other.instance.alias == self.instance
                    and other.project.name == self.project
                    and other.typ == self.typ
                    and other.name == self.name
                    and other.version == self.version
                )

        load_conf.return_value = {
            "instances": {
                "foo": {"uri": "https://foo.com"},
                "bar": {"uri": "https://bar.com"},
            },
        }
        tests = {
            "https://foo.com/projects/1/models": Expect("foo", "1", ResourceType.model),
            "https://foo.com/projects/2/runtimes": Expect(
                "foo", "2", ResourceType.runtime
            ),
            "https://foo.com/projects/3/datasets": Expect(
                "foo", "3", ResourceType.dataset
            ),
            "https://foo.com/projects/4/evaluations": Expect(
                "foo", "4", ResourceType.evaluation
            ),
            "https://foo.com/projects/5/models/1/versions": Expect(
                "foo", "5", ResourceType.model, "1"
            ),
            "https://foo.com/projects/5/models/1/versions/2/files": Expect(
                "foo", "5", ResourceType.model, "1", "2"
            ),
        }
        for url, expect in tests.items():
            assert expect == Resource(url)

        with self.assertRaises(Exception):
            Resource("https://foo.com/projects/1/model")  # model missing the tail 's'
