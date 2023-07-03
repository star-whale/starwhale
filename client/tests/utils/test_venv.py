import os
from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils.venv import (
    conda_install_req,
    _do_pip_install_req,
    parse_python_version,
)
from starwhale.utils.error import FormatError


class TestVenv(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def test_parse_python_version(self) -> None:
        ts = {
            "python3": {
                "major": 3,
                "minor": -1,
                "micro": -1,
            },
            "python3.7": {
                "major": 3,
                "minor": 7,
                "micro": -1,
            },
            "python3.7.1": {
                "major": 3,
                "minor": 7,
                "micro": 1,
            },
            "3.8": {
                "major": 3,
                "minor": 8,
                "micro": -1,
            },
        }

        for _k, _v in ts.items():
            _pvf = parse_python_version(_k)
            assert _pvf.major == _v["major"]
            assert _pvf.minor == _v["minor"]
            assert _pvf.micro == _v["micro"]

        self.assertRaises(ValueError, parse_python_version, "python")
        self.assertRaises(FormatError, parse_python_version, "")

    @patch("starwhale.utils.venv.check_call")
    def test_conda_install(self, m_call: MagicMock) -> None:
        conda_install_req(
            req=["starwhale==0.2.0", "numpy"],
            env_name="conda-env",
            use_pip_install=False,
            configs={"conda": {"channels": ["nvidia", "pytorch"]}},
        )
        assert m_call.call_args[0][0] == [
            "conda",
            "install",
            "--name",
            "conda-env",
            "--channel",
            "nvidia",
            "--channel",
            "pytorch",
            "--yes",
            "--override-channels",
            "starwhale==0.2.0",
            "numpy",
        ]

        conda_install_req(
            req="starwhale==0.2.0",
            prefix_path=Path("/tmp/conda-env"),
            use_pip_install=False,
        )
        assert m_call.call_args[0][0] == [
            "conda",
            "install",
            "--prefix",
            "/tmp/conda-env",
            "--channel",
            "conda-forge",
            "--yes",
            "--override-channels",
            "starwhale==0.2.0",
        ]

        req_fname = "/tmp/requirements.txt"
        self.fs.create_file(req_fname, contents="")

        conda_install_req(
            req=["starwhale==0.2.0", Path(req_fname), "numpy", req_fname],
            prefix_path=Path("/tmp/conda-env"),
        )
        assert m_call.call_args[0][0] == [
            "conda",
            "run",
            "--live-stream",
            "--prefix",
            "/tmp/conda-env",
            "python3",
            "-m",
            "pip",
            "install",
            "--exists-action",
            "w",
            "--timeout=90",
            "--retries=10",
            "starwhale==0.2.0",
            "-r",
            req_fname,
            "numpy",
            "-r",
            req_fname,
        ]

    @patch("starwhale.utils.venv.check_call")
    def test_pip_install(self, m_call: MagicMock) -> None:
        _do_pip_install_req(
            prefix_cmd=["venv/bin/pip"],
            req="starwhale==0.2.0",
            enable_pre=True,
            pip_config={
                "index_url": "https://e1.com",
                "trusted_host": ["e1.com"],
            },
        )
        assert m_call.call_args[0][0] == [
            "venv/bin/pip",
            "install",
            "--exists-action",
            "w",
            "--index-url",
            "https://e1.com",
            "--trusted-host",
            "e1.com",
            "--timeout=90",
            "--retries=10",
            "--pre",
            "starwhale==0.2.0",
        ]

        os.environ["SW_PYPI_INDEX_URL"] = "https://e2.com"
        os.environ["SW_PYPI_EXTRA_INDEX_URL"] = "https://e3.com https://e4.com"
        os.environ["SW_PYPI_TRUSTED_HOST"] = "e2.com e3.com e4.com"

        req_fname = "/tmp/requirements.txt"
        self.fs.create_file(req_fname, contents="")

        _do_pip_install_req(
            prefix_cmd=["venv/bin/pip"],
            req=Path(req_fname),
            pip_config={
                "index_url": "https://e1.com",
                "trusted_host": ["e1.com"],
            },
        )

        assert m_call.call_args[0][0] == [
            "venv/bin/pip",
            "install",
            "--exists-action",
            "w",
            "--index-url",
            "https://e2.com",
            "--extra-index-url",
            "https://e3.com https://e4.com https://e1.com",
            "--trusted-host",
            "e2.com e3.com e4.com e1.com",
            "--timeout=90",
            "--retries=10",
            "-r",
            req_fname,
        ]

        _do_pip_install_req(
            prefix_cmd=["venv/bin/pip"],
            req=["starwhale==0.2.0", Path(req_fname), "numpy", req_fname],
            enable_pre=True,
            pip_config={
                "index_url": "https://e1.com",
                "trusted_host": ["e1.com"],
            },
        )
        assert m_call.call_args[0][0] == [
            "venv/bin/pip",
            "install",
            "--exists-action",
            "w",
            "--index-url",
            "https://e2.com",
            "--extra-index-url",
            "https://e3.com https://e4.com https://e1.com",
            "--trusted-host",
            "e2.com e3.com e4.com e1.com",
            "--timeout=90",
            "--retries=10",
            "--pre",
            "starwhale==0.2.0",
            "-r",
            req_fname,
            "numpy",
            "-r",
            req_fname,
        ]
