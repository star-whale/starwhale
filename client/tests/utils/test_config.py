import os

from pyfakefs.fake_filesystem_unittest import TestCase

from tests import get_predefined_config_yaml
from starwhale.utils import config as sw_config
from starwhale.utils.error import NotFoundError
from starwhale.utils.config import (
    SWCliConfigMixed,
    ENV_SW_CLI_CONFIG,
    load_swcli_config,
    render_swcli_config,
    ENV_SW_LOCAL_STORAGE,
    get_swcli_config_path,
)

_existed_config_contents = get_predefined_config_yaml()


class SWCliConfigTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()
        sw_config._config = {}

    def test_config_path(self):
        path = get_swcli_config_path()
        assert path.endswith(".config/starwhale/config.yaml")

    def test_config_path_env(self):
        env_def = "foo"
        os.environ[ENV_SW_CLI_CONFIG] = env_def
        path = get_swcli_config_path()
        assert path == env_def
        os.environ.pop(ENV_SW_CLI_CONFIG)

    def test_local_storage_root(self):
        _config = load_swcli_config()
        assert _config["storage"]["root"].endswith("starwhale")

    def test_local_storage_root_env_new_conf(self):
        env_def = "bar"
        os.environ[ENV_SW_LOCAL_STORAGE] = env_def
        _config = load_swcli_config()
        assert _config["storage"]["root"] == env_def
        os.environ.pop(ENV_SW_LOCAL_STORAGE)

    def test_local_storage_root_env_exist_conf(self):
        path = get_swcli_config_path()
        render_swcli_config({"storage": {"root": "foo"}}, path)

        env_def = "bar"
        os.environ[ENV_SW_LOCAL_STORAGE] = env_def
        _config = load_swcli_config()
        assert _config["storage"]["root"] == env_def
        os.environ.pop(ENV_SW_LOCAL_STORAGE)

    def test_load_default_swcli_config(self):
        _config = load_swcli_config()

        path = get_swcli_config_path()
        assert oct(os.stat(path).st_mode & 0o777) == "0o600"

        assert "instances" in _config
        assert _config["storage"]["root"].endswith(".starwhale") is True
        assert "local" == _config["current_instance"]
        assert len(_config["instances"]) == 1
        assert "local" == _config["instances"]["local"]["uri"]
        assert "standalone" == _config["instances"]["local"]["type"]
        assert "self" == _config["instances"]["local"]["current_project"]

    def test_load_existed_swcli_config(self):
        path = get_swcli_config_path()
        self.assertFalse(os.path.exists(path))

        self.fs.create_file(path, contents=_existed_config_contents)
        _config = load_swcli_config()

        assert len(_config["instances"]) == 5
        assert "pre-bare" == _config["current_instance"]
        assert "cloud" == _config["instances"]["pre-bare"]["type"]
        assert "starwhale" == _config["instances"]["pre-bare"]["user_name"]
        assert "http://1.1.1.1:8182" == _config["instances"]["pre-bare"]["uri"]

    def test_swcli_config_mixed(self):
        path = get_swcli_config_path()
        self.fs.create_file(path, contents=_existed_config_contents)

        sw = SWCliConfigMixed()
        assert str(sw.rootdir).endswith(".cache/starwhale")

        sw.delete_instance("local")
        sw.delete_instance("xxx")
        sw.delete_instance("pre-bare")
        sw.delete_instance("pre-bare2")

        _config = load_swcli_config()
        assert "local" == sw.current_instance
        assert len(_config["instances"]) == 3

        sw.update_instance(
            uri="console.pre.intra.starwhale.ai",
            user_name="test",
            alias="pre-k8s",
        )

        _config = load_swcli_config()
        assert len(_config["instances"]) == 4
        assert "pre-k8s" in _config["instances"]
        assert (
            "http://console.pre.intra.starwhale.ai"
            == _config["instances"]["pre-k8s"]["uri"]
        )
        assert "test" == _config["instances"]["pre-k8s"]["user_name"]

    def test_select(self):
        path = get_swcli_config_path()
        self.fs.create_file(path, contents=_existed_config_contents)

        sw = SWCliConfigMixed()
        assert sw.current_instance == "pre-bare"
        assert sw.current_project == "self"
        sw.select_current_default(instance="pre-bare", project="first")
        assert sw.current_project == "first"
        sw.select_current_default(instance="local", project="self")
        assert sw.current_instance == "local"
        assert sw.current_project == "self"

        sw.select_current_default(instance="pre-bare2")
        assert sw.current_instance == "pre-bare2"
        sw.select_current_default(instance="pre-bare2", project="new")
        assert sw.current_instance == "pre-bare2"
        assert sw.current_project == "new"

        self.assertRaises(NotFoundError, sw.select_current_default, instance="notfound")
        self.assertRaises(
            NotFoundError,
            sw.select_current_default,
            instance="local",
            project="notfound",
        )
