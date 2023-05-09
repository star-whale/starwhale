from unittest import TestCase
from unittest.mock import patch, MagicMock

from starwhale.base.uri.instance import Instance, NoMatchException


class TestInstance(TestCase):
    @patch("starwhale.utils.config.load_swcli_config")
    def test_instance(self, load_conf: MagicMock) -> None:
        # invalid config
        load_conf.return_value = {"current_instance": "not exists"}
        with self.assertRaises(NoMatchException):
            Instance()

        # get default alias without params
        load_conf.return_value = {
            "current_instance": "foo",
            "instances": {
                "foo": {"uri": "https://foo.com"},
                "bar": {"uri": "https://bar.com"},
                "baz": {"uri": "https://foo.com"},
                "local": {"uri": "local"},
            },
        }

        ins = Instance()
        assert ins.alias == "foo"

        # use alias
        ins = Instance(instance_alias="bar")
        assert ins.alias == "bar"

        # use alias not exists
        with self.assertRaises(NoMatchException):
            Instance(instance_alias="alias-not-exists")

        # use url
        ins = Instance(uri="https://bar.com")
        assert ins.alias == "bar"

        # use url not exists
        with self.assertRaises(NoMatchException):
            Instance(uri="https://not-exists.com")

        # use url matches two alias
        with self.assertRaises(NoMatchException):
            Instance(uri="https://foo.com")

        # use both alias and url
        with self.assertRaises(Exception):
            Instance(instance_alias="foo", uri="https://bar.com")

        # path
        ins = Instance(uri="cloud://foo/baz")
        assert ins.alias == "foo"
        assert ins.path == "baz"

        ins = Instance(uri="local/baz/foo")
        assert ins.alias == "local"
        assert ins.path == "baz/foo"

        ins = Instance(uri="foo/baz")
        assert ins.alias == "foo"
        assert ins.path == "baz"
