import os
import shutil
import tempfile
import unittest


class BaseTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.root = os.path.join(tempfile.gettempdir(), "datastore_test")
        os.makedirs(self.root, exist_ok=True)
        os.environ["SW_ROOT_PATH"] = self.root

    def tearDown(self) -> None:
        shutil.rmtree(self.root)
