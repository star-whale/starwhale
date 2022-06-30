from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.consts import HTTPMethod
from starwhale.utils.config import SWCliConfigMixed, get_swcli_config_path
from starwhale.core.instance.view import InstanceTermView

from .. import get_predefined_config_yaml

_existed_config_contents = get_predefined_config_yaml()


class InstanceTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}
        path = get_swcli_config_path()
        self.fs.create_file(path, contents=_existed_config_contents)

    def test_select(self):
        InstanceTermView().select("local")
        assert SWCliConfigMixed().current_instance == "local"

        InstanceTermView().select("pre-bare")
        assert SWCliConfigMixed().current_instance == "pre-bare"

        with self.assertRaises(SystemExit):
            InstanceTermView().select("not-found")

    @Mocker()
    def test_workflow(self, rm: Mocker):
        InstanceTermView().select("local")
        InstanceTermView().info()

        InstanceTermView().info("pre-bare")
        InstanceTermView().list()

    @Mocker()
    def test_login(self, rm: Mocker):
        InstanceTermView().login("local", "abc", "123", alias="local")

        with self.assertRaises(SystemExit):
            InstanceTermView().select("pre-k8s")

        rm.request(
            HTTPMethod.POST,
            "http://1.1.0.0:8182/api/v1/login",
            json={"data": {"role": {"roleName": "admin"}}},
            headers={"Authorization": "123"},
        )
        InstanceTermView().login("http://1.1.0.0:8182", "abc", "123", alias="pre-k8s")
        InstanceTermView().select("pre-k8s")
        InstanceTermView().logout("pre-k8s")

        with self.assertRaises(SystemExit):
            InstanceTermView().select("pre-k8s")
