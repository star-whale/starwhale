import os
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.utils import load_yaml
from starwhale.consts import (
    ENV_VENV,
    SupportArch,
    PythonRunEnv,
    DefaultYAMLName,
    ENV_CONDA_PREFIX,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.base.type import URIType, BundleType, RuntimeLockFileType
from starwhale.utils.venv import EnvTarType, get_python_version
from starwhale.utils.error import UnExpectedConfigFieldError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.runtime.view import RuntimeTermView
from starwhale.core.runtime.model import Runtime, RuntimeConfig, StandaloneRuntime


class StandaloneRuntimeTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.virtualenv.cli_run")
    def test_quickstart_from_ishell_venv(
        self, m_venv: MagicMock, m_call: MagicMock
    ) -> None:
        workdir = "/home/starwhale/myproject"
        venv_dir = os.path.join(workdir, ".venv")
        runtime_path = os.path.join(workdir, DefaultYAMLName.RUNTIME)
        name = "test-venv"

        StandaloneRuntime.quickstart_from_ishell(
            workdir=workdir,
            name=name,
            mode=PythonRunEnv.VENV,
            create_env=True,
        )

        assert os.path.exists(os.path.join(workdir, runtime_path))
        assert m_venv.call_count == 1
        assert m_venv.call_args[0][0] == [
            venv_dir,
            "--prompt",
            name,
            "--python",
            get_python_version(),
        ]

        assert " ".join(m_call.call_args[0][0]).startswith(
            " ".join(
                [
                    os.path.join(venv_dir, "bin", "pip"),
                    "install",
                ]
            )
        )

        _rt_config = load_yaml(runtime_path)

        assert _rt_config["name"] == name
        assert _rt_config["mode"] == "venv"
        assert _rt_config["environment"]["python"] == get_python_version()
        assert "_starwhale_version" not in _rt_config
        assert "base_image" not in _rt_config

        empty_dir(workdir)
        assert not os.path.exists(os.path.join(workdir, runtime_path))

        m_venv.reset_mock()

        StandaloneRuntime.quickstart_from_ishell(
            workdir=workdir,
            name=name,
            mode=PythonRunEnv.VENV,
            create_env=False,
        )
        assert m_venv.call_count == 0

        _rt_config = RuntimeConfig.create_by_yaml(Path(workdir) / runtime_path)
        assert _rt_config.name == name
        assert _rt_config.mode == PythonRunEnv.VENV
        assert _rt_config.environment.arch == [SupportArch.NOARCH]
        assert _rt_config.dependencies.pip_pkgs[0] == "starwhale"
        assert _rt_config.dependencies.pip_files[0] == RuntimeLockFileType.VENV

    @patch("starwhale.utils.venv.check_call")
    def test_quickstart_from_ishell_conda(self, m_call: MagicMock) -> None:
        workdir = "/home/starwhale/myproject"
        runtime_path = os.path.join(workdir, DefaultYAMLName.RUNTIME)
        name = "test-conda"

        StandaloneRuntime.quickstart_from_ishell(
            workdir=workdir,
            name=name,
            mode=PythonRunEnv.CONDA,
            create_env=True,
        )
        assert os.path.exists(os.path.join(workdir, runtime_path))
        _rt_config = load_yaml(runtime_path)
        assert _rt_config["mode"] == "conda"
        assert m_call.call_args_list[0][0][0] == [
            "conda",
            "create",
            "--yes",
            "--name",
            name,
            f"python={get_python_version()}",
        ]
        assert " ".join(m_call.call_args_list[1][0][0]).startswith(
            " ".join(
                [
                    "conda",
                    "run",
                    "--name",
                    name,
                    "python3",
                    "-m",
                    "pip",
                    "install",
                ]
            )
        )

    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output", return_value=b"3.7")
    def test_build_venv(
        self, m_output: MagicMock, m_check_call: MagicMock, m_py_bin: MagicMock
    ) -> None:
        name = "rttest"
        venv_path = "/home/starwhale/venv/bin/python3"
        os.environ[ENV_VENV] = venv_path
        m_py_bin.return_value = venv_path
        build_version = ""

        sw = SWCliConfigMixed()
        workdir = "/home/starwhale/myproject"
        runtime_config = self.get_runtime_config()
        runtime_config["environment"]["cuda"] = "11.5"
        runtime_config["environment"]["cudnn"] = "8"
        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.RUNTIME),
            contents=yaml.safe_dump(runtime_config),
        )
        self.fs.create_file(
            os.path.join(workdir, "requirements.txt"), contents="requests==2.0.0"
        )

        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        sr.build(Path(workdir))
        assert sr.uri.object.version != ""
        assert len(sr._version) == 31
        build_version = sr._version

        runtime_dir = os.path.join(sw.rootdir, "self", "runtime")
        bundle_path = os.path.join(
            runtime_dir,
            name,
            sr._version[:VERSION_PREFIX_CNT],
            f"{sr._version}{BundleType.RUNTIME}",
        )
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            name,
            sr._version[:VERSION_PREFIX_CNT],
            sr._version,
        )

        assert os.path.exists(bundle_path)
        assert os.path.exists(runtime_workdir)
        assert "latest" in sr.tag.list()

        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))

        assert (
            _manifest["base_image"]
            == "ghcr.io/star-whale/starwhale:latest-cuda11.5-cudnn8"
        )

        assert (
            _manifest["environment"]["python"]
            == runtime_config["environment"]["python"]
        )
        assert _manifest["version"] == sr.uri.object.version
        assert _manifest["environment"]["mode"] == "venv"
        assert _manifest["environment"]["lock"]["shell"]["use_venv"]
        assert not _manifest["dependencies"]["local_packaged_env"]

        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        info = sr.info()
        assert info["project"] == "self"
        assert "version" not in info
        assert len(info["history"][0]) == 1
        assert info["history"][0][0]["version"] == build_version

        uri = URI(f"{name}/version/{build_version[:6]}", expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        info = sr.info()
        assert "history" not in info
        assert info["version"] == build_version

        rts = StandaloneRuntime.list(URI(""))
        assert len(rts[0]) == 1
        assert len(rts[0][name]) == 1
        assert rts[0][name][0]["version"] == build_version

        RuntimeTermView(name).history(fullname=True)
        RuntimeTermView(name).info(fullname=True)
        RuntimeTermView(f"{name}/version/{build_version}").info(fullname=True)
        RuntimeTermView.list()
        RuntimeTermView.list("myproject")
        RuntimeTermView.build(workdir)
        rts = StandaloneRuntime.list(URI(""))
        assert len(rts[0][name]) == 2

        uri = URI(f"{name}/version/{build_version[:8]}", expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        ok, _ = sr.remove()
        assert ok
        recover_path = os.path.join(
            sw.rootdir,
            "self",
            "runtime",
            ".recover",
            name,
            build_version[:VERSION_PREFIX_CNT],
            f"{build_version}{BundleType.RUNTIME}",
        )
        recover_snapshot_path = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            ".recover",
            name,
            build_version[:VERSION_PREFIX_CNT],
            build_version,
        )
        swrt_path = os.path.join(
            sw.rootdir,
            "self",
            "runtime",
            name,
            build_version[:VERSION_PREFIX_CNT],
            f"{build_version}{BundleType.RUNTIME}",
        )
        swrt_snapshot_path = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            name,
            build_version[:VERSION_PREFIX_CNT],
            build_version,
        )
        assert os.path.exists(recover_path)
        assert not os.path.exists(swrt_path)
        assert os.path.exists(recover_snapshot_path)
        assert not os.path.exists(swrt_snapshot_path)

        uri = URI(f"{name}/version/{build_version}", expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        ok, _ = sr.recover()
        assert ok
        assert not os.path.exists(recover_path)
        assert os.path.exists(swrt_path)
        assert not os.path.exists(recover_snapshot_path)
        assert os.path.exists(swrt_snapshot_path)

    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.is_venv")
    @patch("starwhale.utils.venv.is_conda")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_build_conda(
        self,
        m_call: MagicMock,
        m_check_call: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
    ) -> None:
        m_py_bin.return_value = "/home/starwhale/anaconda3/envs/starwhale/bin/python3"
        m_conda.return_value = True
        m_venv.return_value = False
        m_call.return_value = b"3.7.13"

        os.environ["CONDA_DEFAULT_ENV"] = "1"

        name = "rttest"
        workdir = "/home/starwhale/myproject"

        runtime_config = self.get_runtime_config()
        runtime_config["mode"] = "conda"
        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.RUNTIME),
            contents=yaml.safe_dump(runtime_config),
        )
        self.fs.create_file(os.path.join(workdir, "requirements.txt"), contents="")
        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        sr.build(Path(workdir))
        sr.info()
        sr.history()

    def get_runtime_config(self) -> t.Dict[str, t.Any]:
        return {
            "name": "rttest",
            "mode": "venv",
            "environment": {
                "python": "3.7",
            },
            "dependencies": [
                "requirements.txt",
            ],
        }

    @patch("starwhale.utils.venv.check_user_python_pkg_exists")
    @patch("starwhale.utils.venv.virtualenv.cli_run")
    @patch("starwhale.utils.venv.check_call")
    def test_restore_venv(
        self, m_call: MagicMock, m_venv: MagicMock, m_exists: MagicMock
    ):
        workdir = "/home/starwhale/myproject"
        export_dir = os.path.join(workdir, "export")
        venv_dir = os.path.join(export_dir, "venv")
        dep_dir = os.path.join(workdir, "dependencies")

        ensure_dir(workdir)
        self.fs.create_file(
            os.path.join(workdir, DEFAULT_MANIFEST_NAME),
            contents=yaml.safe_dump(
                {
                    "environment": {
                        "mode": "venv",
                        "python": "3.7",
                        "arch": [SupportArch.AMD64],
                    },
                    "dependencies": {
                        "local_packaged_env": False,
                        "pip_files": [
                            "requirements-sw-lock.txt",
                            "requirements-test.txt",
                        ],
                    },
                }
            ),
        )
        ensure_dir(export_dir)

        req_fpath = os.path.join(dep_dir, "requirements-test.txt")
        req_lock_fpath = os.path.join(dep_dir, "requirements-sw-lock.txt")
        self.fs.create_file(req_fpath, contents="test1==0.0.1")
        self.fs.create_file(req_lock_fpath, contents="test2==0.0.1")

        m_exists.return_value = False
        Runtime.restore(Path(workdir))
        assert m_call.call_count == 3
        pip_cmds = [
            m_call.call_args_list[0][0][0][-1],
            m_call.call_args_list[1][0][0][-1],
        ]
        assert m_venv.call_args[0][0] == [
            venv_dir,
            "--python",
            "3.7",
        ]
        assert req_fpath in pip_cmds
        assert req_lock_fpath in pip_cmds
        assert m_call.call_args_list[2][0][0] == [
            "/home/starwhale/myproject/export/venv/bin/pip",
            "install",
            "--exists-action",
            "w",
            "--pre",
            "starwhale",
        ]

        m_call.reset_mock()
        m_exists.return_value = True
        Runtime.restore(Path(workdir))
        assert m_call.call_count == 2

        RuntimeTermView.restore(workdir)

    @patch("starwhale.core.runtime.model.platform.machine")
    @patch("starwhale.utils.venv.tarfile.open")
    @patch("starwhale.utils.venv.check_call")
    def test_restore_conda(
        self, m_call: MagicMock, m_tar: MagicMock, m_machine: MagicMock
    ):
        workdir = "/home/starwhale/myproject"
        export_dir = os.path.join(workdir, "export")
        ensure_dir(workdir)

        self.fs.create_file(
            os.path.join(workdir, DEFAULT_MANIFEST_NAME),
            contents=yaml.safe_dump(
                {
                    "environment": {
                        "python": "3.7",
                        "mode": "conda",
                        "arch": [SupportArch.ARM64],
                    },
                    "dependencies": {
                        "local_packaged_env": False,
                    },
                }
            ),
        )
        ensure_dir(export_dir)

        with self.assertRaises(UnExpectedConfigFieldError):
            Runtime.restore(Path(workdir))

        m_machine.return_value = "arm64"
        Runtime.restore(Path(workdir))

        assert m_call.call_args_list[0][0][0] == [
            "conda",
            "create",
            "--yes",
            "--prefix",
            os.path.join(export_dir, "conda"),
            "python=3.7",
        ]
        RuntimeTermView.restore(workdir)

        ensure_file(
            os.path.join(workdir, DEFAULT_MANIFEST_NAME),
            yaml.safe_dump(
                {
                    "environment": {"mode": "conda", "python": "3.7"},
                    "dependencies": {"local_packaged_env": True},
                }
            ),
        )
        tar_path = os.path.join(export_dir, EnvTarType.CONDA)
        ensure_file(tar_path, "test")

        Runtime.restore(Path(workdir))
        assert m_tar.call_count == 1
        assert m_tar.call_args[0][0] == tar_path
        RuntimeTermView.restore(workdir)

    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_venv(self, m_output: MagicMock, m_call: MagicMock) -> None:
        target_dir = "/home/starwhale/workdir"
        ensure_dir(target_dir)
        runtime_fname = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        lock_fname = os.path.join(target_dir, RuntimeLockFileType.VENV)
        self.fs.create_file(
            runtime_fname, contents=yaml.safe_dump({"name": "test", "mode": "venv"})
        )

        venv_dir = "/tmp/venv"
        ensure_dir(venv_dir)
        self.fs.create_file(os.path.join(venv_dir, "pyvenv.cfg"))
        os.environ[ENV_VENV] = venv_dir

        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.VENV not in content.get("dependencies", [])
        StandaloneRuntime.lock(target_dir)

        assert m_call.call_args[0][0].startswith(
            " ".join(
                [
                    f"{venv_dir}/bin/python3",
                    "-m",
                    "pip",
                    "freeze",
                    "--require-virtualenv",
                    "--exclude-editable",
                    ">>",
                    "/tmp/starwhale-lock-",
                ]
            )
        )
        assert os.path.exists(lock_fname)
        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.VENV == content["dependencies"][-1]
        del os.environ[ENV_VENV]

        os.unlink(lock_fname)
        StandaloneRuntime.lock(target_dir, env_prefix_path=venv_dir)
        assert m_call.call_args[0][0].startswith(
            " ".join(
                [
                    f"{venv_dir}/bin/python3",
                    "-m",
                    "pip",
                    "freeze",
                    "--require-virtualenv",
                    "--exclude-editable",
                    ">>",
                    "/tmp/starwhale-lock-",
                ]
            )
        )
        assert os.path.exists(lock_fname)

    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_conda(self, m_output: MagicMock, m_call: MagicMock) -> None:
        target_dir = "/home/starwhale/workdir"
        ensure_dir(target_dir)
        runtime_fname = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        self.fs.create_file(
            runtime_fname, contents=yaml.safe_dump({"name": "test", "mode": "conda"})
        )

        conda_dir = "/tmp/conda"
        ensure_dir(conda_dir)
        ensure_dir(os.path.join(conda_dir, "conda-meta"))

        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.CONDA not in content.get("dependencies", [])
        StandaloneRuntime.lock(target_dir, env_prefix_path=conda_dir)

        assert m_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            "/tmp/conda",
            "--file",
        ]

        assert os.path.exists(os.path.join(target_dir, RuntimeLockFileType.CONDA))
        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.CONDA == content["dependencies"][-1]

        StandaloneRuntime.lock(target_dir, env_name="conda-env-name")
        assert m_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--name",
            "conda-env-name",
            "--file",
        ]

        os.environ[ENV_CONDA_PREFIX] = conda_dir
        StandaloneRuntime.lock(target_dir)
        assert m_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            "/tmp/conda",
            "--file",
        ]
