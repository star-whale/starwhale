from dataclasses import dataclass
from unittest.mock import Mock, patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.uri.exceptions import NoMatchException


class MockLocalInstance:
    is_local = True
    url = "local"

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
        r = Resource(
            uri="mnist", typ=ResourceType.dataset, project=p, _skip_refine=True
        )
        assert r.name == "mnist"
        assert r.typ == ResourceType.dataset
        assert r.version == ""
        assert r.full_uri == "local/project/self/dataset/mnist/version/latest"

        # {name}/version/{ver}
        r = Resource(
            uri="mnist/version/foo",
            typ=ResourceType.dataset,
            project=p,
            _skip_refine=True,
        )
        assert r.name == "mnist"
        assert r.typ == ResourceType.dataset
        assert r.version == "foo"
        assert r.full_uri == "local/project/self/dataset/mnist/version/foo"

        r = Resource(
            uri="foo/bar", typ=ResourceType.dataset, project=p, _skip_refine=True
        )
        assert r.name == "foo"
        assert r.typ == ResourceType.dataset
        assert r.version == "bar"
        assert r.full_uri == "local/project/self/dataset/foo/version/bar"

        with self.assertRaises(Exception):
            Resource(
                uri="mnist/foo/bar",
                typ=ResourceType.dataset,
                project=p,
                _skip_refine=True,
            )

    @patch("starwhale.base.uri.resource.Project.parse_from_full_uri")
    def test_with_full_uri(self, mock_parse: MagicMock) -> None:
        ins = mock_parse.return_value
        ins.path = "dataset/mnist/version/foo"
        uri = "local/project/self/dataset/mnist/version/foo"
        r = Resource(uri, _skip_refine=True)
        assert r.name == "mnist"
        assert r.version == "foo"
        assert r.typ == ResourceType.dataset
        assert mock_parse.call_args(uri)

    @patch("starwhale.utils.config.load_swcli_config")
    def test_with_full_uri_no_version(self, mock_conf: MagicMock) -> None:
        mock_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "self"},
            },
            "storage": {"root": "/root"},
        }
        uri = "local/project/self/dataset/mnist"
        r = Resource(uri, _skip_refine=True)
        assert r.name == "mnist"
        assert r.version == ""
        assert r.typ == ResourceType.dataset

    @patch("starwhale.base.uri.resource.glob")
    @patch("starwhale.utils.config.load_swcli_config")
    def test_version_only(self, mock_conf: MagicMock, mock_glob: MagicMock) -> None:
        mock_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "self"},
            },
        }
        mock_glob.return_value = ["/root/project/self/runtime/mnist/fo/foo"]

        # with project
        project = Mock(spec=Project)
        project.instance = MockLocalInstance()
        project.name = "self"
        r = Resource("foo", project=project)
        assert r.name == "mnist"
        assert r.version == "foo"
        assert r.typ == ResourceType.runtime
        assert r.full_uri == "local/project/self/runtime/mnist/version/foo"

        # without project
        r = Resource("foo")
        assert r.name == "mnist"
        assert r.version == "foo"
        assert r.typ == ResourceType.runtime
        assert r.full_uri == "local/project/self/runtime/mnist/version/foo"

    @patch("starwhale.base.uri.resource.glob")
    @patch("starwhale.base.uri.resource.Project.parse_from_full_uri")
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

    @patch("requests.get")
    @patch("starwhale.utils.config.load_swcli_config")
    def test_parsing_browser_url(self, load_conf: MagicMock, get: MagicMock) -> None:
        @dataclass
        class Expect:
            instance: str
            project: str
            typ: ResourceType
            name: str = ""
            version: str = ""

            def __eq__(self, other: Resource):
                return (
                    other.instance.alias == self.instance
                    and other.project.name == self.project
                    and other.typ == self.typ
                    and other.name == self.name
                    and other.version == self.version
                )

        token = "fake token"
        load_conf.return_value = {
            "instances": {
                "foo": {"uri": "https://foo.com", "sw_token": token},
                "bar": {"uri": "https://bar.com", "sw_token": token},
                "dev": {"uri": "http://127.0.0.1:8082", "sw_token": token},
            },
        }
        tests = {
            "http://127.0.0.1:8082/projects/1/models": Expect(
                "dev", "1", ResourceType.model
            ),
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
        get.return_value.json.return_value = {}
        for url, expect in tests.items():
            assert expect == Resource(url)

        get.return_value.json.return_value = {
            "data": {"name": "name in server", "versionName": "version in server"}
        }
        for url, expect in tests.items():
            # only the resource with name and version will be parsed
            if expect.name and expect.version:
                expect.name = "name in server"
                expect.version = "version in server"
            assert expect == Resource(url)

        with self.assertRaises(Exception):
            Resource("https://foo.com/projects/1/model")  # model missing the tail 's'

    @patch("starwhale.utils.config.load_swcli_config")
    def test_short_uri(self, load_conf: MagicMock) -> None:
        load_conf.return_value = {
            "instances": {
                "foo": {"uri": "https://foo.com"},
                "bar": {"uri": "https://bar.com"},
                "local": {"uri": "local"},
            },
        }

        tests = {
            "bar/project/self/mnist": ("mnist", "self", "bar"),
            "local/project/self/mnist": ("mnist", "self", "local"),
            "cloud://bar/project/self/mnist": ("mnist", "self", "bar"),
        }

        for uri, expect in tests.items():
            p = Resource(uri, typ=ResourceType.runtime, _skip_refine=True)
            assert p.name == expect[0]
            assert p.project.name == expect[1]
            assert p.instance.alias == expect[2]

    @patch("starwhale.utils.config.load_swcli_config")
    def test_remote_uri_with_type_part(self, m_conf: MagicMock) -> None:
        m_conf.return_value = {
            "instances": {
                "foo": {"uri": "https://foo.com"},
                "local": {"uri": "local"},
            },
        }
        uri = Resource("cloud://foo/project/starwhale/dataset/mnist", _skip_refine=True)
        assert uri.instance.alias == "foo"
        assert uri.project.name == "starwhale"
        assert uri.typ == ResourceType.dataset
        assert uri.name == "mnist"
        assert uri.version == ""

        # specify type argument
        uri = Resource(
            "cloud://foo/project/starwhale/dataset/mnist",
            typ=ResourceType.dataset,
            _skip_refine=True,
        )
        assert uri.instance.alias == "foo"
        assert uri.project.name == "starwhale"
        assert uri.typ == ResourceType.dataset
        assert uri.name == "mnist"
        assert uri.version == ""
