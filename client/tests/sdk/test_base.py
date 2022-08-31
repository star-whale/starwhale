import os
import tempfile
import unittest

from starwhale.utils import config as sw_config
from starwhale.consts import ENV_SW_CLI_CONFIG, ENV_SW_LOCAL_STORAGE
from starwhale.utils.fs import empty_dir, ensure_dir


class BaseTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.local_storage = tempfile.mkdtemp(prefix="sw-test-mock-")
        os.environ[ENV_SW_CLI_CONFIG] = os.path.join(self.local_storage, "config.yaml")
        os.environ[ENV_SW_LOCAL_STORAGE] = self.local_storage
        sw_config._config = {}

        self.datastore_root = str(sw_config.SWCliConfigMixed().datastore_dir)
        ensure_dir(self.datastore_root)

    def tearDown(self) -> None:
        empty_dir(self.local_storage)
        os.environ.pop(ENV_SW_CLI_CONFIG, "")
        os.environ.pop(ENV_SW_LOCAL_STORAGE, "")
