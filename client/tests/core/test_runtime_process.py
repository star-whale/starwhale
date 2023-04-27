import sys
import tempfile
from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.core.model.store import ModelStorage
from starwhale.core.runtime.store import RuntimeStorage
from starwhale.core.runtime.process import Process


class RuntimeProcessTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.root = Path(tempfile.mkdtemp(prefix="starwhale-ut-")).resolve()
        ensure_dir(self.root)

    def tearDown(self) -> None:
        empty_dir(self.root)

    @patch("starwhale.core.runtime.process.guess_python_env_mode")
    @patch("starwhale.core.runtime.process.check_call")
    @patch("starwhale.core.runtime.process.extract_tar")
    @patch("starwhale.core.runtime.process.StandaloneRuntime.restore")
    def test_run_with_runtime_uri(
        self,
        m_restore: MagicMock,
        m_extract: MagicMock,
        m_call: MagicMock,
        m_mode: MagicMock,
    ) -> None:
        uri = URI("runtime-test/version/1234", expected_type=URIType.RUNTIME)
        store = RuntimeStorage(uri)
        venv_dir = store.export_dir / "venv"
        ensure_dir(venv_dir)
        m_mode.return_value = "venv"
        run_argv = [
            "swcli",
            "model",
            "run",
            "--runtime",
            str(uri),
        ]

        with patch.object(sys, "argv", run_argv):
            p = Process(uri)
            p.run()

        assert m_restore.called
        assert m_extract.called

        assert m_call.call_args[0][0][0:2] == [
            "bash",
            "-c",
        ]
        assert (
            m_call.call_args[0][0][2]
            == f"source {p._prefix_path}/bin/activate && {p._prefix_path}/bin/swcli model run"
        )
        env = m_call.call_args[1]["env"]
        assert env["SW_RUNTIME_ACTIVATED_PROCESS"] == "1"
        assert env["SW_ACTIVATED_RUNTIME_URI_IN_SUBPROCESS"] == str(uri)

        m_restore.reset_mock()
        m_extract.reset_mock()
        ensure_file(venv_dir / "pyvenv.cfg", "")

        with patch.object(sys, "argv", run_argv):
            p = Process(uri, force_restore=False)
            p.run()

        assert not m_restore.called
        assert not m_extract.called

    @patch("starwhale.core.runtime.process.get_conda_bin")
    @patch("starwhale.core.runtime.process.guess_python_env_mode")
    @patch("starwhale.core.runtime.process.check_call")
    @patch("starwhale.core.runtime.process.extract_tar")
    @patch("starwhale.core.runtime.process.StandaloneRuntime.restore")
    def test_run_with_model_uri(
        self,
        m_restore: MagicMock,
        m_extract: MagicMock,
        m_call: MagicMock,
        m_mode: MagicMock,
        m_conda_bin: MagicMock,
    ) -> None:
        uri = URI("model-test/version/1234", expected_type=URIType.MODEL)
        store = ModelStorage(uri)
        conda_dir = store.packaged_runtime_export_dir / "conda"
        ensure_dir(conda_dir)
        m_mode.return_value = "conda"
        conda_bin_path = "/opt/conda/bin/conda"
        m_conda_bin.return_value = conda_bin_path

        run_argv = [
            "swcli",
            "model",
            "run",
            "--runtime=pytorch/version/latest",
            "mock",
            "another",
        ]

        with patch.object(sys, "argv", run_argv):
            p = Process(uri)
            p.run()

        assert m_restore.called
        assert m_extract.called

        assert m_call.call_args[0][0][0:2] == [
            "bash",
            "-c",
        ]
        assert (
            m_call.call_args[0][0][2]
            == f"{conda_bin_path} run --prefix {conda_dir} {p._prefix_path}/bin/swcli model run mock another"
        )
        env = m_call.call_args[1]["env"]
        assert env["SW_RUNTIME_ACTIVATED_PROCESS"] == "1"
        assert env["SW_ACTIVATED_RUNTIME_URI_IN_SUBPROCESS"] == str(uri)

        m_restore.reset_mock()
        m_extract.reset_mock()
        ensure_file(conda_dir / "conda-meta", "")

        with patch.object(sys, "argv", run_argv):
            p = Process(uri, force_restore=False)
            p.run()

        assert not m_restore.called
        assert not m_extract.called

        ensure_file(
            store.packaged_runtime_snapshot_workdir / DEFAULT_MANIFEST_NAME,
            "",
            parents=True,
        )

        with patch.object(sys, "argv", run_argv):
            p = Process(uri, force_restore=True)
            p.run()

        assert m_restore.called
        assert not m_extract.called

    @patch("starwhale.core.runtime.process.guess_python_env_mode")
    @patch("starwhale.core.runtime.process.check_call")
    @patch("starwhale.core.runtime.process.extract_tar")
    @patch("starwhale.core.runtime.process.StandaloneRuntime.restore")
    def test_run_exceptions(
        self,
        m_restore: MagicMock,
        m_extract: MagicMock,
        m_call: MagicMock,
        m_mode: MagicMock,
    ) -> None:
        with self.assertRaisesRegex(
            FieldTypeOrValueError, "is not a valid uri, only support"
        ):
            Process("dataset/mnist/version/latest").run()

        with self.assertRaisesRegex(
            FieldTypeOrValueError, "is not a valid uri, only support"
        ):
            Process(URI("mnist/version/latest", expected_type=URIType.DATASET)).run()

        uri = URI("runtime-test/version/1234", expected_type=URIType.RUNTIME)
        store = RuntimeStorage(uri)
        ensure_dir(store.export_dir / "venv")
        m_mode.return_value = "venv"

        with self.assertRaisesRegex(NoSupportError, "no cli command"):
            with patch.object(sys, "argv", []):
                Process(uri).run()

        with self.assertRaisesRegex(RuntimeError, "no runtime specified"):
            with patch.object(sys, "argv", ["swcli", "model"]):
                Process(uri).run()

        with self.assertRaisesRegex(
            NoSupportError, "run process with cloud instance uri"
        ):
            uri = "http://1.1.1.1:8081/project/self/runtime/rttest/versoin/123"
            Process(uri).run()
