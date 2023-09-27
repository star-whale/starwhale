import os
from pathlib import Path
from dataclasses import dataclass
from unittest.mock import patch, MagicMock

import yaml
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.uri.exceptions import NoMatchException, MultipleMatchException


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
        p = Project("self")
        r = Resource(
            uri="mnist",
            typ=ResourceType.dataset,
            project=p,
            refine=False,
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
        )
        assert r.name == "mnist"
        assert r.typ == ResourceType.dataset
        assert r.version == "foo"
        assert r.full_uri == "local/project/self/dataset/mnist/version/foo"

        r = Resource(
            uri="foo/bar",
            typ=ResourceType.dataset,
            project=p,
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
            )

    def test_refine_local_rc_info(self) -> None:
        root = SWCliConfigMixed().rootdir / "self" / "model" / "mnist"
        version = "rmnfvqjrozps2ybnwamd3bailizz2qtcqidjskhv"

        ensure_file(
            root / "_manifest.yaml",
            yaml.safe_dump({"tags": {"latest": version, "v0": version, "t1": version}}),
            parents=True,
        )
        ensure_dir(root / version[:2] / f"{version}.swmp")

        uri_cases = [
            "mnist",
            "mnist/version/latest",
            "mnist/version/v0",
            "mnist/t1",
            f"mnist/version/{version[:10]}",
            f"mnist/version/{version[:4]}",
        ]

        for uri in uri_cases:
            r = Resource(uri=uri, typ=ResourceType.model, refine=True)
            assert r.version == version

        os.unlink(root / "_manifest.yaml")
        r = Resource(uri=f"mnist/{version[:10]}", typ=ResourceType.model, refine=True)
        assert r.version == version

    @patch("starwhale.base.uri.resource.Project.parse_from_full_uri")
    def test_with_full_uri(self, mock_parse: MagicMock) -> None:
        ins = mock_parse.return_value
        ins.path = "dataset/mnist/version/foo"
        uri = "local/project/self/dataset/mnist/version/foo"
        r = Resource(uri, refine=False)
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
        r = Resource(uri, refine=False)
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
        project = Project("self")
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
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.base.uri.resource.Project.parse_from_full_uri")
    def test_version_only_no_match(
        self, mock_parse: MagicMock, mock_conf: MagicMock, mock_glob: MagicMock
    ) -> None:
        mock_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "self"},
            },
        }
        p = Project("self")
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
                    and other.project.id == self.project
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

        def response_of_get(*args, **kwargs):
            ret = type("", (), {})()
            ret.raise_for_status = lambda: None
            ver = "version in server"
            if kwargs.get("params", {}).get("versionUrl", "") == "latest":
                ver = "latest of the version"
            ret.json = lambda: {"data": {"name": "name in server", "versionName": ver}}
            return ret

        get.side_effect = response_of_get

        for url, expect in tests.items():
            # only the resource with name and version will be parsed
            if expect.name:
                expect.name = "name in server"
                if expect.version:
                    expect.version = "version in server"
                else:
                    expect.version = "latest of the version"
            assert expect == Resource(url)

        with self.assertRaises(Exception):
            Resource("https://foo.com/projects/1/model")  # model missing the tail 's'

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_short_uri(self, rm: Mocker, load_conf: MagicMock) -> None:
        load_conf.return_value = {
            "instances": {
                "foo": {"uri": "https://foo.com"},
                "bar": {"uri": "https://bar.com"},
                "local": {"uri": "local"},
            },
        }

        tests = {
            "bar/project/self/mnist": ("https://bar.com", "mnist", "1", "bar"),
            "local/project/self/mnist": ("", "mnist", "self", "local"),
            "cloud://bar/project/self/mnist": (
                "https://bar.com",
                "mnist",
                "1",
                "bar",
            ),
        }

        for uri, expect in tests.items():
            if expect[0]:
                rm.get(f"{expect[0]}/api/v1/project/self", json={"data": {"id": 1}})
            p = Resource(uri, typ=ResourceType.runtime, refine=False)
            assert p.name == expect[1]
            assert p.project.id == expect[2]
            assert p.instance.alias == expect[3]

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_remote_uri_with_type_part(self, rm: Mocker, m_conf: MagicMock) -> None:
        m_conf.return_value = {
            "instances": {
                "foo": {"uri": "https://foo.com"},
                "local": {"uri": "local"},
            },
        }
        rm.get("https://foo.com/api/v1/project/starwhale", json={"data": {"id": 1}})

        uri = Resource("cloud://foo/project/starwhale/dataset/mnist", refine=False)
        assert uri.instance.alias == "foo"
        assert uri.project.id == "1"
        assert uri.project.name == "starwhale"
        assert uri.typ == ResourceType.dataset
        assert uri.name == "mnist"
        assert uri.version == ""

        # specify type argument
        uri = Resource(
            "cloud://foo/project/starwhale/dataset/mnist",
            typ=ResourceType.dataset,
            refine=False,
        )
        assert uri.instance.alias == "foo"
        assert uri.project.id == "1"
        assert uri.project.name == "starwhale"
        assert uri.typ == ResourceType.dataset
        assert uri.name == "mnist"
        assert uri.version == ""

    @patch("starwhale.base.uri.resource.load_swcli_config")
    def test_duplicate_version(self, m_conf: MagicMock):
        root = Path("/root")
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "self"},
            },
            "storage": {"root": str(root)},
        }

        # fake two model with same version
        model1 = root / "self" / "model" / "mnist" / "fo" / "foo"
        model1.mkdir(parents=True)
        (model1 / "_manifest.yaml").touch()
        ensure_file(
            model1.parent.parent / "_manifest.yaml",
            yaml.safe_dump({"tags": {"latest": "foo"}}),
        )

        model2 = root / "self" / "model" / "mnist-dup" / "fo" / "foo"
        model2.mkdir(parents=True)
        (model2 / "_manifest.yaml").touch()
        ensure_file(
            model2.parent.parent / "_manifest.yaml",
            yaml.safe_dump({"tags": {"latest": "foo"}}),
        )

        rc = Resource("mnist", typ=ResourceType.model)
        assert rc.version == "foo"
        assert rc.name == "mnist"

        with self.assertRaises(MultipleMatchException):
            Resource("foo", typ=ResourceType.model)

        rc = Resource("mnist/foo", typ=ResourceType.model)
        assert rc.version == "foo"
        assert rc.name == "mnist"

        rc = Resource("mnist/version/foo", typ=ResourceType.model)
        assert rc.version == "foo"
        assert rc.name == "mnist"

        rc = Resource("mnist-dup/foo", typ=ResourceType.model)
        assert rc.version == "foo"
        assert rc.name == "mnist-dup"

        rc = Resource("mnist-dup/version/foo", typ=ResourceType.model)
        assert rc.version == "foo"
        assert rc.name == "mnist-dup"

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_remote_uri_with_version_and_refine(
        self, rm: Mocker, m_conf: MagicMock
    ) -> None:
        m_conf.return_value = {
            "instances": {
                "foo": {"uri": "https://foo.com"},
                "local": {"uri": "local"},
            },
        }
        rm.get("https://foo.com/api/v1/project/starwhale", json={"data": {"id": 1}})
        rm.get(
            "https://foo.com/api/v1/project/1/dataset/mnist",
            json={
                "data": {
                    "id": 1,
                    "name": "mnist",
                    "versionId": 101,
                    "versionName": "123456",
                }
            },
        )
        uri = Resource(
            "cloud://foo/project/starwhale/dataset/mnist/version/123456", refine=True
        )
        assert uri.instance.alias == "foo"
        assert uri.project.id == "1"
        assert uri.project.name == "starwhale"
        assert uri.typ == ResourceType.dataset
        assert uri.name == "mnist"
        assert uri.version == "123456"
        assert uri.info().get("id") == 1
        assert uri.info().get("versionId") == 101
