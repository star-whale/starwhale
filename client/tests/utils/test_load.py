import os
import sys
from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import ENV_VENV
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.load import import_cls


class MockPPL:
    class Handler:
        x = 1


class ImportClsTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.workdir = "/home/starwhale/test"
        self.inject_paths = [
            "/home/starwhale/anaconda3/envs/starwhale/bin",
            "/home/starwhale/anaconda3/envs/starwhale/lib/python37.zip",
            "/home/starwhale/anaconda3/envs/starwhale/lib/python3.7",
            "/home/starwhale/anaconda3/envs/starwhale/lib/python3.7/lib-dynload",
            "",
            "/home/starwhale/anaconda3/envs/starwhale/lib/python3.7/site-packages",
        ]
        self.fs.create_dir(self.workdir)
        self.fs.create_file(os.path.join(self.workdir, "__init__.py"), contents="")
        self.fs.create_file(
            os.path.join(self.workdir, "ppl.py"), contents="class Handler: x=1"
        )

    @patch("starwhale.utils.load.importlib.import_module")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_load(
        self,
        m_output: MagicMock,
        m_import: MagicMock,
    ) -> None:
        m_import.return_value = MockPPL
        m_output.side_effect = [
            (",".join(self.inject_paths) + "\n").encode(),
            f"{sys.version_info.major}.{sys.version_info.minor}".encode(),
            (",".join(self.inject_paths) + "\n").encode(),
        ]

        venv_dir = os.path.join(self.workdir, "venv")
        py_bin = os.path.join(venv_dir, "bin", "python3")
        os.environ[ENV_VENV] = venv_dir
        ensure_dir(venv_dir)
        ensure_dir(os.path.join(venv_dir, "bin"))
        ensure_file(py_bin, " ")

        _cls = import_cls(Path(self.workdir), "ppl:Handler")
        assert _cls.__module__ == "tests.utils.test_load"
        assert _cls.x == 1
        for _p in self.inject_paths + [self.workdir]:
            assert _p in sys.path
