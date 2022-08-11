import os
import tempfile
import unittest

from starwhale.utils import config as sw_config
from starwhale.consts import ENV_SW_CLI_CONFIG, ENV_SW_LOCAL_STORAGE
from starwhale.utils.fs import empty_dir, ensure_dir


class BaseTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self._test_local_storage = tempfile.mkdtemp(prefix="sw-test-mock-")
        os.environ[ENV_SW_CLI_CONFIG] = os.path.join(
            self._test_local_storage, "config.yaml"
        )
        os.environ[ENV_SW_LOCAL_STORAGE] = self._test_local_storage
        sw_config._config = {}

        self.root = str(sw_config.SWCliConfigMixed().datastore_dir)
        ensure_dir(self.root)

    def tearDown(self) -> None:
        empty_dir(self._test_local_storage)
        os.environ.pop(ENV_SW_CLI_CONFIG, "")
        os.environ.pop(ENV_SW_LOCAL_STORAGE, "")
