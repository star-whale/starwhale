from __future__ import annotations

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale import login, logout
from starwhale.utils import load_yaml
from starwhale.utils.config import get_swcli_config_path


class TestInstance(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    @Mocker()
    def test_login(self, rm: Mocker) -> None:
        with self.assertRaisesRegex(
            RuntimeError, "Cannot login to the Standalone instance"
        ):
            login("local")

        with self.assertRaisesRegex(
            ValueError, "Password must be provided when username is provided"
        ):
            login("http://controller.starwhale.svc", username="starwhale")

        with self.assertRaisesRegex(ValueError, "Cannot provide both"):
            login(
                "http://controller.starwhale.svc",
                username="starwhale",
                password="abcd1234",
                token="xxx",
            )

        with self.assertRaisesRegex(ValueError, "password or token must be provided"):
            login("http://controller.starwhale.svc")

        username_request = rm.post(
            "http://controller.starwhale.svc/api/v1/login",
            json={"data": {"role": {"roleNAme": "normal"}, "name": "starwhale"}},
            headers={"Authorization": "mock-token"},
        )
        login(
            "http://controller.starwhale.svc", username="starwhale", password="abcd1234"
        )
        assert username_request.called
        conf_yaml = load_yaml(get_swcli_config_path())
        instance_conf = conf_yaml["instances"]["controller.starwhale.svc"]
        assert instance_conf["sw_token"] == "mock-token"
        assert instance_conf["user_name"] == "starwhale"
        assert instance_conf["user_role"] == "normal"
        assert instance_conf["uri"] == "http://controller.starwhale.svc"

        token_request = rm.get(
            "https://cloud.starwhale.cn/api/v1/user/current",
            json={"data": {"role": {"roleName": "admin"}, "name": "test"}},
        )
        login(
            "https://cloud.starwhale.cn",
            alias="cloud-cn",
            token="mock-token",
        )
        assert token_request.called

        conf_yaml = load_yaml(get_swcli_config_path())
        assert "cloud-cn" in conf_yaml["instances"]

    @Mocker()
    def test_logout(self, rm: Mocker) -> None:
        with self.assertRaisesRegex(
            RuntimeError, "Cannot logout from the Standalone instance"
        ):
            logout("local")

        rm.get("https://cloud.starwhale.cn/api/v1/user/current", json={"data": {}})
        login(
            "https://cloud.starwhale.cn",
            alias="cloud-cn",
            token="mock-token",
        )

        conf_yaml = load_yaml(get_swcli_config_path())
        assert "cloud-cn" in conf_yaml["instances"]

        logout("cloud-cn")

        conf_yaml = load_yaml(get_swcli_config_path())
        assert "cloud-cn" not in conf_yaml["instances"]

        login(
            "https://cloud.starwhale.cn",
            alias="cloud-cn",
            token="mock-token",
        )

        logout("https://cloud.starwhale.cn")
        assert "cloud-cn" not in conf_yaml["instances"]
