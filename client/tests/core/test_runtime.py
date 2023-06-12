import os
import typing as t
import platform
import tempfile
import subprocess
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
from click.testing import CliRunner
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.utils import config as sw_config
from starwhale.utils import is_linux, load_yaml
from starwhale.consts import (
    ENV_VENV,
    ENV_CONDA,
    SupportArch,
    PythonRunEnv,
    ENV_LOG_LEVEL,
    DefaultYAMLName,
    SW_AUTO_DIRNAME,
    ENV_CONDA_PREFIX,
    ENV_SW_IMAGE_REPO,
    DEFAULT_IMAGE_REPO,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_SW_TASK_RUN_IMAGE,
)
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.base.type import BundleType, DependencyType, RuntimeLockFileType
from starwhale.utils.venv import EnvTarType, get_conda_bin, get_python_version
from starwhale.utils.error import (
    FormatError,
    NotFoundError,
    NoSupportError,
    ConfigFormatError,
    ExclusiveArgsError,
    UnExpectedConfigFieldError,
)
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.project import Project
from starwhale.core.runtime.cli import _list as runtime_list_cli
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.runtime.view import (
    get_term_view,
    RuntimeTermView,
    RuntimeTermViewRich,
)
from starwhale.core.runtime.model import (
    Runtime,
    Dependencies,
    _TEMPLATE_DIR,
    RuntimeConfig,
    WheelDependency,
    RuntimeInfoFilter,
    StandaloneRuntime,
)
from starwhale.core.runtime.store import (
    RuntimeStorage,
    get_docker_run_image_by_manifest,
)

_runtime_data_dir = f"{ROOT_DIR}/data/runtime"
_swrt = open(f"{_runtime_data_dir}/pytorch.swrt").read()


class StandaloneRuntimeTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self._clear_cache()

    def tearDown(self) -> None:
        self._clear_cache()

    def _clear_cache(self) -> None:
        os.environ.pop(ENV_LOG_LEVEL, None)
        sw_config._config = {}
        get_conda_bin.cache_clear()
        os.environ.pop("CONDARC", None)

    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.virtualenv.cli_run")
    def test_quickstart_from_ishell_venv(
        self, m_venv: MagicMock, m_call: MagicMock
    ) -> None:
        workdir = "/home/starwhale/myproject"
        venv_dir = os.path.join(workdir, SW_AUTO_DIRNAME, "venv")
        runtime_path = os.path.join(workdir, DefaultYAMLName.RUNTIME)
        name = "test-venv"

        RuntimeTermView.quickstart_from_ishell(
            workdir=workdir,
            name=name,
            mode=PythonRunEnv.VENV,
            disable_create_env=False,
            force=True,
            interactive=True,
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
            disable_create_env=True,
        )
        assert m_venv.call_count == 0

        _rt_config = RuntimeConfig.create_by_yaml(Path(workdir) / runtime_path)
        assert _rt_config.name == name
        assert _rt_config.mode == PythonRunEnv.VENV
        assert _rt_config.environment.arch == [SupportArch.NOARCH]

        assert _rt_config.dependencies._pip_pkgs[0] == "starwhale"
        assert _rt_config.dependencies._pip_files == []

    @patch("os.environ", {})
    @patch("starwhale.utils.venv.check_call")
    def test_quickstart_from_ishell_conda(self, m_call: MagicMock) -> None:
        workdir = "/home/starwhale/myproject"
        runtime_path = os.path.join(workdir, DefaultYAMLName.RUNTIME)
        conda_prefix_dir = os.path.join(workdir, SW_AUTO_DIRNAME, "conda")
        name = "test-conda"

        conda_bin_path = "/home/starwhale/miniconda3/bin/conda"
        os.environ["CONDA_EXE"] = conda_bin_path
        ensure_file(conda_bin_path, "", parents=True)

        StandaloneRuntime.quickstart_from_ishell(
            workdir=workdir,
            name=name,
            mode=PythonRunEnv.CONDA,
            disable_create_env=False,
        )
        assert os.path.exists(os.path.join(workdir, runtime_path))
        _rt_config = load_yaml(runtime_path)
        assert _rt_config["mode"] == "conda"
        assert m_call.call_args_list[0][0][0] == [
            conda_bin_path,
            "create",
            "--yes",
            "--quiet",
            "--prefix",
            conda_prefix_dir,
            f"python={get_python_version()}",
        ]
        assert " ".join(m_call.call_args_list[1][0][0]).startswith(
            " ".join(
                [
                    conda_bin_path,
                    "run",
                    "--live-stream",
                    "--prefix",
                    conda_prefix_dir,
                    "python3",
                    "-m",
                    "pip",
                    "install",
                ]
            )
        )

    @patch("starwhale.base.uri.resource.Resource._refine_remote_rc_info")
    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.core.runtime.model.StandaloneRuntime.restore")
    @patch("starwhale.base.bundle.extract_tar")
    @patch("starwhale.core.runtime.model.BundleCopy")
    def test_quickstart_from_uri(
        self,
        m_bundle_copy: MagicMock,
        m_extract: MagicMock,
        m_restore: MagicMock,
        m_config: MagicMock,
        *args: t.Any,
    ) -> None:
        m_config.return_value = {
            "current_instance": "local",
            "instances": {
                "foo": {"uri": "http://0.0.0.0:80", "sw_token": "bar"},
                "local": {"uri": "local"},
            },
            "storage": {"root": tempfile.gettempdir()},
        }
        workdir = Path("/home/starwhale/myproject")
        name = "rttest"
        version = "112233"
        cloud_uri = Resource(
            f"http://0.0.0.0:80/project/1/runtime/{name}/version/{version}"
        )
        extract_dir = workdir / SW_AUTO_DIRNAME / "fork-runtime-extract"
        ensure_dir(extract_dir)

        runtime_config = self.get_runtime_config()
        runtime_config["name"] = name
        ensure_file(
            extract_dir / DefaultYAMLName.RUNTIME,
            content=yaml.safe_dump(runtime_config),
        )
        ensure_dir(extract_dir / "wheels")
        ensure_dir(extract_dir / "dependencies")
        ensure_file(extract_dir / "wheels" / "dummy.whl", "")
        ensure_file(extract_dir / "dependencies" / "requirements.txt", "numpy")
        ensure_file(
            extract_dir / "_manifest.yaml",
            yaml.safe_dump({"environment": {"mode": "venv"}}),
        )

        sw = SWCliConfigMixed()
        runtime_dir = os.path.join(sw.rootdir, "self", "runtime")
        bundle_path = os.path.join(
            runtime_dir, name, f"{version[:VERSION_PREFIX_CNT]}", f"{version}.swrt"
        )
        ensure_dir(os.path.dirname(bundle_path))
        ensure_file(bundle_path, "")
        auto_dir = workdir / SW_AUTO_DIRNAME
        venv_dir = auto_dir / "venv"
        ensure_dir(venv_dir)

        assert not (workdir / "dummy.whl").exists()
        assert not (auto_dir / ".gitignore").exists()
        assert not (workdir / "requirements.txt").exists()

        RuntimeTermView.quickstart_from_uri(
            workdir=workdir,
            name=name,
            uri=cloud_uri,
            force=True,
            disable_restore=False,
        )

        runtime_path = workdir / DefaultYAMLName.RUNTIME
        assert runtime_path.exists()
        runtime_config = yaml.safe_load(runtime_path.read_text())
        assert runtime_config["name"] == name
        assert runtime_config["mode"] == "venv"
        assert runtime_config["dependencies"][0] == "requirements.txt"
        assert runtime_config["dependencies"][1]["wheels"] == ["dummy.whl"]
        assert runtime_config["dependencies"][2]["pip"] == ["Pillow"]
        assert (workdir / "dummy.whl").exists()
        assert (auto_dir / ".gitignore").exists()
        assert (workdir / "requirements.txt").exists()

        assert m_bundle_copy.call_count == 1
        assert m_extract.call_count == 1
        assert m_restore.call_args[0] == (extract_dir, venv_dir)

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.core.runtime.model.guess_current_py_env")
    @patch("starwhale.core.runtime.model.get_user_python_version")
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.core.runtime.model.StandaloneRuntime.lock")
    @patch("starwhale.core.runtime.model.get_python_version_by_bin")
    def test_build_from_conda_prefix(
        self,
        m_py_ver_bin: MagicMock,
        m_lock: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_user_py_ver: MagicMock,
        m_py_env: MagicMock,
        *args: t.Any,
    ) -> None:
        conda_prefix = "/home/starwhale/anaconda3/envs/starwhale"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))

        m_py_ver_bin.return_value = "3.8.10"
        lock_fpath = Path("/tmp/conda-sw-lock.yaml")
        lock_content = "numpy==1.19.5\nPillow==8.3.1"
        ensure_file(lock_fpath, content=lock_content, parents=True)
        m_lock.return_value = lock_content, lock_fpath
        m_conda.return_value = True
        m_venv.return_value = False
        m_user_py_ver.return_value = "3.9"
        m_py_env.return_value = "conda"

        uri = RuntimeTermView.build_from_python_env(
            conda_prefix=conda_prefix,
            cuda="11.4",
            cudnn="8",
            arch="amd64",
        )

        assert m_py_ver_bin.call_args[0][0] == os.path.join(conda_prefix, "bin/python3")
        assert m_lock.call_args[1]["env_name"] == ""
        assert m_lock.call_args[1]["env_prefix_path"] == conda_prefix
        assert not m_lock.call_args[1]["env_use_shell"]

        sw = SWCliConfigMixed()
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            uri.name,
            uri.version[:VERSION_PREFIX_CNT],
            uri.version,
        )
        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))
        assert _manifest["artifacts"] == {
            "dependencies": ["dependencies/.starwhale/lock/conda-sw-lock.yaml"],
            "files": [],
            "runtime_yaml": "runtime.yaml",
            "wheels": [],
        }
        assert (
            _manifest["base_image"]
            == "docker-registry.starwhale.cn/star-whale/starwhale:latest-cuda11.4-cudnn8"
        )
        assert _manifest["docker"] == {
            "builtin_run_image": {
                "fullname": "docker-registry.starwhale.cn/star-whale/starwhale:latest-cuda11.4-cudnn8",
                "name": "starwhale",
                "repo": "docker-registry.starwhale.cn/star-whale",
                "tag": "latest-cuda11.4-cudnn8",
            },
            "custom_run_image": "",
        }

        assert _manifest["dependencies"] == {
            "conda_files": [],
            "conda_pkgs": [],
            "local_packaged_env": False,
            "pip_files": [],
            "pip_pkgs": ["starwhale"],
            "raw_deps": [{"deps": ["starwhale"], "kind": "pip_pkg"}],
        }
        assert _manifest["environment"] == {
            "arch": ["amd64"],
            "auto_lock_dependencies": True,
            "lock": {
                "env_name": "",
                "env_prefix_path": "/home/starwhale/anaconda3/envs/starwhale",
                "env_use_shell": False,
                "files": [".starwhale/lock/conda-sw-lock.yaml"],
                "shell": {
                    "python_env": "conda",
                    "python_version": "3.9",
                    "use_conda": True,
                    "use_venv": False,
                },
                "starwhale_version": "0.0.0.dev0",
                "system": platform.system(),
            },
            "mode": "conda",
            "python": "3.8",
        }
        assert _manifest["version"] == uri.version

        _runtime_yaml = load_yaml(os.path.join(runtime_workdir, "runtime.yaml"))
        assert _runtime_yaml == {
            "api_version": "1.1",
            "dependencies": [{"pip": ["starwhale"]}],
            "environment": {
                "arch": "amd64",
                "cuda": "11.4",
                "cudnn": "8",
                "os": "ubuntu:20.04",
                "python": "3.8.10",
            },
            "mode": "conda",
            "name": "starwhale",
        }

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.core.runtime.model.guess_current_py_env")
    @patch("starwhale.core.runtime.model.get_user_python_version")
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.core.runtime.model.StandaloneRuntime.lock")
    @patch("starwhale.core.runtime.model.get_python_version_by_bin")
    @patch("starwhale.core.runtime.model.get_conda_prefix_path")
    def test_build_from_conda_name(
        self,
        m_conda_prefix: MagicMock,
        m_py_ver_bin: MagicMock,
        m_lock: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_user_py_ver: MagicMock,
        m_py_env: MagicMock,
        m_check_call: MagicMock,
        *args: t.Any,
    ) -> None:
        conda_prefix = "/home/starwhale/anaconda3/envs/starwhale"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))
        conda_name = "starwhale"

        self.fs.create_file(
            Path.home() / ".condarc",
            contents=yaml.safe_dump({"ssl_verify": False}, default_flow_style=False),
        )

        lock_fpath = Path("/tmp/conda-sw-lock.yaml")
        lock_content = "numpy==1.19.5\nPillow==8.3.1"
        ensure_file(lock_fpath, content=lock_content, parents=True)
        m_lock.return_value = lock_content, lock_fpath
        m_conda_prefix.return_value = conda_prefix
        m_py_ver_bin.return_value = "3.8.10"
        m_conda.return_value = True
        m_venv.return_value = False
        m_user_py_ver.return_value = "3.9"
        m_py_env.return_value = "conda"

        uri = RuntimeTermView.build_from_python_env(
            runtime_name="test",
            conda_name=conda_name,
            cuda="11.4",
        )

        assert m_py_ver_bin.call_args[0][0] == os.path.join(conda_prefix, "bin/python3")
        assert m_lock.call_args[1]["env_name"] == conda_name
        assert m_lock.call_args[1]["env_prefix_path"] == ""
        assert not m_lock.call_args[1]["env_use_shell"]
        assert m_check_call.call_args[0][0] == [
            f"{conda_prefix}/bin/python3",
            "-m",
            "pip",
            "check",
        ]

        sw = SWCliConfigMixed()
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            uri.name,
            uri.version[:VERSION_PREFIX_CNT],
            uri.version,
        )
        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))
        assert _manifest["artifacts"] == {
            "dependencies": ["dependencies/.starwhale/lock/conda-sw-lock.yaml"],
            "files": [],
            "runtime_yaml": "runtime.yaml",
            "wheels": [],
        }
        assert (
            _manifest["base_image"]
            == "docker-registry.starwhale.cn/star-whale/starwhale:latest-cuda11.4"
        )
        assert _manifest["dependencies"] == {
            "conda_files": [],
            "conda_pkgs": [],
            "local_packaged_env": False,
            "pip_files": [],
            "pip_pkgs": ["starwhale"],
            "raw_deps": [{"deps": ["starwhale"], "kind": "pip_pkg"}],
        }
        assert _manifest["environment"] == {
            "arch": ["noarch"],
            "auto_lock_dependencies": True,
            "lock": {
                "env_name": conda_name,
                "env_prefix_path": "",
                "env_use_shell": False,
                "files": [".starwhale/lock/conda-sw-lock.yaml"],
                "shell": {
                    "python_env": "conda",
                    "python_version": "3.9",
                    "use_conda": True,
                    "use_venv": False,
                },
                "starwhale_version": "0.0.0.dev0",
                "system": platform.system(),
            },
            "mode": "conda",
            "python": "3.8",
        }
        assert _manifest["configs"] == {
            "conda": {"channels": ["conda-forge"], "condarc": {"ssl_verify": False}},
            "docker": {"image": ""},
            "pip": {"extra_index_url": [""], "index_url": "", "trusted_host": [""]},
        }
        assert _manifest["version"] == uri.version

        _runtime_yaml = load_yaml(os.path.join(runtime_workdir, "runtime.yaml"))
        assert _runtime_yaml == {
            "api_version": "1.1",
            "dependencies": [{"pip": ["starwhale"]}],
            "environment": {
                "arch": "noarch",
                "cuda": "11.4",
                "cudnn": "",
                "os": "ubuntu:20.04",
                "python": "3.8.10",
            },
            "mode": "conda",
            "name": "test",
        }
        _condarc = load_yaml(os.path.join(runtime_workdir, "configs", "condarc"))
        assert _condarc == {"ssl_verify": False}

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.core.runtime.model.guess_current_py_env")
    @patch("starwhale.core.runtime.model.get_user_python_version")
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.core.runtime.model.StandaloneRuntime.lock")
    @patch("starwhale.core.runtime.model.get_python_version_by_bin")
    def test_build_from_venv_prefix(
        self,
        m_py_ver_bin: MagicMock,
        m_lock: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_user_py_ver: MagicMock,
        m_py_env: MagicMock,
        *args: t.Any,
    ) -> None:
        venv_prefix = "/home/starwhale/.venv/starwhale"
        os.environ[ENV_VENV] = venv_prefix
        ensure_dir(venv_prefix)
        self.fs.create_file(os.path.join(venv_prefix, "pyvenv.cfg"))

        m_py_ver_bin.return_value = "3.8.10"
        lock_fpath = Path("/tmp/requirements-sw-lock.txt")
        lock_content = "numpy==1.19.5\nPillow==8.3.1"
        ensure_file(lock_fpath, content=lock_content, parents=True)
        m_lock.return_value = lock_content, lock_fpath
        m_conda.return_value = False
        m_venv.return_value = True
        m_user_py_ver.return_value = "3.9"
        m_py_env.return_value = "venv"

        uri = RuntimeTermView.build_from_python_env(
            venv_prefix=venv_prefix,
        )

        assert m_py_ver_bin.call_args[0][0] == os.path.join(venv_prefix, "bin/python3")
        assert m_lock.call_args[1]["env_name"] == ""
        assert m_lock.call_args[1]["env_prefix_path"] == venv_prefix
        assert not m_lock.call_args[1]["env_use_shell"]

        sw = SWCliConfigMixed()
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            uri.name,
            uri.version[:VERSION_PREFIX_CNT],
            uri.version,
        )
        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))
        assert _manifest["artifacts"] == {
            "dependencies": ["dependencies/.starwhale/lock/requirements-sw-lock.txt"],
            "files": [],
            "runtime_yaml": "runtime.yaml",
            "wheels": [],
        }
        assert (
            _manifest["base_image"]
            == "docker-registry.starwhale.cn/star-whale/starwhale:latest"
        )
        assert _manifest["docker"] == {
            "builtin_run_image": {
                "fullname": "docker-registry.starwhale.cn/star-whale/starwhale:latest",
                "name": "starwhale",
                "repo": "docker-registry.starwhale.cn/star-whale",
                "tag": "latest",
            },
            "custom_run_image": "",
        }
        assert _manifest["dependencies"] == {
            "conda_files": [],
            "conda_pkgs": [],
            "local_packaged_env": False,
            "pip_files": [],
            "pip_pkgs": ["starwhale"],
            "raw_deps": [{"deps": ["starwhale"], "kind": "pip_pkg"}],
        }
        assert _manifest["environment"] == {
            "arch": ["noarch"],
            "auto_lock_dependencies": True,
            "lock": {
                "env_name": "",
                "env_prefix_path": venv_prefix,
                "env_use_shell": False,
                "files": [".starwhale/lock/requirements-sw-lock.txt"],
                "shell": {
                    "python_env": "venv",
                    "python_version": "3.9",
                    "use_conda": False,
                    "use_venv": True,
                },
                "starwhale_version": "0.0.0.dev0",
                "system": platform.system(),
            },
            "mode": "venv",
            "python": "3.8",
        }
        assert _manifest["version"] == uri.version

        _runtime_yaml = load_yaml(os.path.join(runtime_workdir, "runtime.yaml"))
        assert _runtime_yaml == {
            "api_version": "1.1",
            "dependencies": [{"pip": ["starwhale"]}],
            "environment": {
                "arch": "noarch",
                "cuda": "",
                "cudnn": "",
                "os": "ubuntu:20.04",
                "python": "3.8.10",
            },
            "mode": "venv",
            "name": "starwhale",
        }

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.utils.venv.subprocess.check_output", autospec=True)
    def test_build_from_env_exceptions(
        self, m_check_output: MagicMock, *args: t.Any
    ) -> None:
        with self.assertRaisesRegex(ExclusiveArgsError, "conda_prefix"):
            RuntimeTermView.build_from_python_env(
                runtime_name="test",
                conda_prefix="/home/starwhale/anaconda3/envs/starwhale",
                venv_prefix="/home/starwhale/venv",
                conda_name="starwhale",
            )

        with self.assertRaisesRegex(RuntimeError, "Invalid conda prefix"):
            RuntimeTermView.build_from_python_env(
                runtime_name="test",
                conda_prefix="not-found-path",
            )

        with self.assertRaisesRegex(RuntimeError, "Invalid venv prefix"):
            RuntimeTermView.build_from_python_env(
                runtime_name="test",
                venv_prefix="not-found-path",
            )

        m_check_output.side_effect = subprocess.CalledProcessError(
            returncode=1,
            cmd=[],
        )
        with self.assertRaises(subprocess.CalledProcessError):
            RuntimeTermView.build_from_python_env(
                runtime_name="test",
                conda_name="not-found-name",
            )

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_build_venv_exceptions(
        self,
        m_output: MagicMock,
        m_py_bin: MagicMock,
        *args: t.Any,
    ):
        workdir = "/home/starwhale/myproject"
        venv_dir = os.path.join(workdir, ".starwhale", "venv")

        m_output.return_value = b"3.7"
        name = "rttest"
        ensure_dir(venv_dir)
        self.fs.create_file(os.path.join(venv_dir, "pyvenv.cfg"))
        venv_path = os.path.join(venv_dir, "bin/python3")
        os.environ[ENV_VENV] = venv_dir
        m_py_bin.return_value = venv_path

        runtime_config = self.get_runtime_config()
        runtime_config["dependencies"] = [
            "req.txt",
            ".starwhale/lock/requirements-sw-lock.txt",
        ]
        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.RUNTIME),
            contents=yaml.safe_dump(runtime_config),
        )
        self.fs.create_file(
            os.path.join(workdir, "req.txt"), contents="requests==2.0.0"
        )
        self.fs.create_file(
            os.path.join(workdir, ".starwhale/lock/requirements-sw-lock.txt"),
            contents="requests==2.0.0",
        )

        uri = Resource(name, typ=ResourceType.runtime)
        sr = StandaloneRuntime(uri)
        with self.assertRaisesRegex(
            RuntimeError, "has already been added into the runtime.yaml"
        ):
            sr.build_from_runtime_yaml(
                workdir=workdir, yaml_path=os.path.join(workdir, "runtime.yaml")
            )

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.conda_export")
    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.is_venv")
    @patch("starwhale.utils.venv.is_conda")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_build_conda_exceptions(
        self,
        m_call_output: MagicMock,
        m_check_call: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
        m_conda_export: MagicMock,
    ) -> None:
        m_conda.return_value = True
        m_venv.return_value = False

        name = "rttest"
        workdir = "/home/starwhale/myproject"
        conda_prefix = os.path.join(workdir, ".starwhale/conda")
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))

        m_call_output.side_effect = [b"3.7.13", conda_prefix.encode(), b"False"]
        m_py_bin.return_value = os.path.join(conda_prefix, "bin/python3")
        os.environ[ENV_CONDA] = "1"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix

        runtime_config = self.get_runtime_config()
        runtime_config["mode"] = "conda"
        runtime_config["dependencies"] = [".starwhale/lock/conda-sw-lock.yaml"]
        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.RUNTIME),
            contents=yaml.safe_dump(runtime_config),
        )
        self.fs.create_file(
            os.path.join(workdir, ".starwhale/lock/conda-sw-lock.yaml"), contents=""
        )
        uri = Resource(name, typ=ResourceType.runtime)
        sr = StandaloneRuntime(uri)
        with self.assertRaisesRegex(
            RuntimeError, "has already been added into the runtime.yaml"
        ):
            sr.build_from_runtime_yaml(
                workdir=workdir, yaml_path=os.path.join(workdir, "runtime.yaml")
            )

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_build_from_runtime_yaml_in_venv_mode(
        self, m_output: MagicMock, m_py_bin: MagicMock, *args: t.Any
    ) -> None:
        sw = SWCliConfigMixed()
        workdir = "/home/starwhale/myproject"

        m_output.return_value = b"3.7"
        name = "rttest"

        venv_dir = os.path.join(workdir, ".starwhale", "venv")
        ensure_dir(venv_dir)
        self.fs.create_file(os.path.join(venv_dir, "pyvenv.cfg"))
        venv_path = os.path.join(venv_dir, "bin/python3")
        os.environ[ENV_VENV] = venv_dir
        m_py_bin.return_value = venv_path

        runtime_config = self.get_runtime_config()
        runtime_config["environment"]["cuda"] = "11.5"
        runtime_config["environment"]["cudnn"] = "8"
        runtime_config["dependencies"].extend(
            [
                {
                    "files": [
                        {
                            "dest": "bin/../bin/prepare.sh",
                            "name": "prepare",
                            "post": "bash bin/prepare.sh",
                            "pre": "ls bin/prepare.sh",
                            "src": "prepare.sh",
                        }
                    ]
                },
                "conda-env.yaml",
                {"conda": ["pkg"]},
            ]
        )
        runtime_yaml_name = "non-runtime.yaml"
        self.fs.create_file(
            os.path.join(workdir, runtime_yaml_name),
            contents=yaml.safe_dump(runtime_config),
        )
        self.fs.create_file(os.path.join(workdir, "prepare.sh"), contents="")
        self.fs.create_file(
            os.path.join(workdir, "requirements.txt"), contents="requests==2.0.0"
        )
        self.fs.create_file(os.path.join(workdir, "dummy.whl"), contents="")

        uri = Resource(name, typ=ResourceType.runtime)
        sr = StandaloneRuntime(uri)
        sr.build_from_runtime_yaml(
            workdir=workdir, yaml_path=os.path.join(workdir, runtime_yaml_name)
        )

        assert sr.uri.version != ""
        assert len(sr._version) == 40
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
        assert os.path.exists(os.path.join(runtime_workdir, "wheels", "dummy.whl"))
        assert os.path.exists(os.path.join(runtime_workdir, "files/prepare.sh"))
        assert os.path.exists(os.path.join(runtime_workdir, DefaultYAMLName.RUNTIME))

        assert "latest" in sr.tag.list()

        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))

        assert _manifest["configs"] == {
            "conda": {"channels": ["conda-forge"], "condarc": {}},
            "docker": {"image": ""},
            "pip": {"extra_index_url": [""], "index_url": "", "trusted_host": [""]},
        }

        assert (
            _manifest["base_image"]
            == "docker-registry.starwhale.cn/star-whale/starwhale:latest-cuda11.5-cudnn8"
        )

        assert (
            _manifest["environment"]["python"]
            == runtime_config["environment"]["python"]
        )
        assert _manifest["version"] == sr.uri.version
        assert _manifest["environment"]["mode"] == "venv"
        assert _manifest["environment"]["lock"]["shell"]["use_venv"]
        assert _manifest["artifacts"]["wheels"] == ["wheels/dummy.whl"]
        assert _manifest["artifacts"]["files"][0] == {
            "dest": "bin/../bin/prepare.sh",
            "name": "prepare",
            "post": "bash bin/prepare.sh",
            "pre": "ls bin/prepare.sh",
            "src": "prepare.sh",
        }
        _deps = _manifest["dependencies"]
        assert not _deps["local_packaged_env"]
        assert _deps["conda_files"] == ["conda-env.yaml"]
        assert _deps["conda_pkgs"] == ["pkg"]
        assert _deps["pip_files"] == ["requirements.txt"]
        assert _deps["pip_pkgs"] == ["Pillow"]
        _raw_deps = _deps["raw_deps"]
        assert len(_raw_deps) == 6
        assert _raw_deps == [
            {"deps": "requirements.txt", "kind": "pip_req_file"},
            {"deps": ["dummy.whl"], "kind": "wheel"},
            {"deps": ["Pillow"], "kind": "pip_pkg"},
            {
                "deps": [
                    {
                        "dest": "bin/../bin/prepare.sh",
                        "name": "prepare",
                        "post": "bash bin/prepare.sh",
                        "pre": "ls bin/prepare.sh",
                        "src": "prepare.sh",
                    }
                ],
                "kind": "native_file",
            },
            {"deps": "conda-env.yaml", "kind": "conda_env_file"},
            {"deps": ["pkg"], "kind": "conda_pkg"},
        ]
        assert _manifest["artifacts"]["dependencies"] == [
            "dependencies/requirements.txt",
            "dependencies/.starwhale/lock/requirements-sw-lock.txt",
        ]
        assert _manifest["environment"]["auto_lock_dependencies"]
        assert _manifest["environment"]["lock"]["files"] == [
            ".starwhale/lock/requirements-sw-lock.txt"
        ]
        for p in _manifest["artifacts"]["dependencies"]:
            assert os.path.exists(os.path.join(runtime_workdir, p))

        uri = Resource(
            f"{name}/version/{build_version[:6]}",
            typ=ResourceType.runtime,
        )
        sr = StandaloneRuntime(uri)
        info = sr.info()
        assert "history" not in info
        assert info["basic"]["version"] == build_version
        assert "manifest" in info
        assert "runtime_yaml" in info
        assert "requirements-sw-lock.txt" in info["lock"]

        rts = StandaloneRuntime.list(Project(""))
        assert len(rts[0]) == 1
        assert len(rts[0][name]) == 1
        assert rts[0][name][0]["version"] == build_version

        runtime_json_view = get_term_view({"output": "json"})
        runtime_term_view = get_term_view({"output": "terminal"})

        build_uri = f"{name}/version/{build_version}"
        tag_manifest_path = (
            sw.rootdir / "self" / "runtime" / name / DEFAULT_MANIFEST_NAME
        )
        runtime_term_view(build_uri).tag(["t1", "t2"])
        tag_content = yaml.safe_load(tag_manifest_path.read_text())
        assert "t1" in tag_content["tags"]
        assert "t2" in tag_content["tags"]
        runtime_term_view(build_uri).tag(["t1"], remove=True)
        tag_content = yaml.safe_load(tag_manifest_path.read_text())
        assert "t1" not in tag_content["tags"]
        assert "t2" in tag_content["tags"]

        uri = f"{name}/version/{build_version}"
        for f in RuntimeInfoFilter:
            runtime_term_view(uri).info(output_filter=f)
            runtime_json_view(uri).info(output_filter=f)

        runtime_term_view.list()
        RuntimeTermViewRich.list()
        runtime_term_view.list("myproject")
        RuntimeTermViewRich.list("myproject")
        runtime_term_view.build_from_runtime_yaml(
            workdir=workdir, yaml_path=os.path.join(workdir, runtime_yaml_name)
        )

        rts = StandaloneRuntime.list(Project(""))
        assert len(rts[0][name]) == 2

        rtv = runtime_term_view(f"{name}/version/{build_version[:8]}")
        ok, _ = rtv.remove()
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

        rtv = RuntimeTermView(f"{name}/version/{build_version}")
        ok, _ = rtv.recover()
        assert ok
        assert not os.path.exists(recover_path)
        assert os.path.exists(swrt_path)
        assert not os.path.exists(recover_snapshot_path)
        assert os.path.exists(swrt_snapshot_path)

        rtv = RuntimeTermView(f"{name}/version/{build_version}")
        ok, _ = rtv.remove(True)
        assert ok
        assert not os.path.exists(recover_path)
        assert not os.path.exists(swrt_path)
        assert not os.path.exists(recover_snapshot_path)
        assert not os.path.exists(swrt_snapshot_path)

        uri = Resource(name, typ=ResourceType.runtime)
        sr = StandaloneRuntime(uri)
        sr.build_from_runtime_yaml(
            workdir=workdir,
            yaml_path=os.path.join(workdir, runtime_yaml_name),
            download_all_deps=True,
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

        if is_linux():
            export_dir = Path(runtime_workdir) / "export"
            venv_tar_path = export_dir / "venv_env.tar.gz"
            assert export_dir.exists()
            assert venv_tar_path.exists(), list(export_dir.iterdir())

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.conda_export")
    @patch("starwhale.core.runtime.model.get_user_python_version")
    @patch("starwhale.core.runtime.model.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.is_venv")
    @patch("starwhale.utils.venv.is_conda")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_build_from_conda_shell(
        self,
        m_call_output: MagicMock,
        m_check_call: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
        m_py_ver: MagicMock,
        m_conda_export: MagicMock,
    ) -> None:
        conda_prefix = "/home/starwhale/anaconda3/envs/starwhale"
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))
        m_py_bin.return_value = os.path.join(conda_prefix, "bin/python3")
        m_py_ver.return_value = b"3.7.13"
        m_conda.return_value = True
        m_venv.return_value = False
        m_call_output.side_effect = [
            b"3.7.13",
            b"3.7.13",
            conda_prefix.encode(),
            b"False",
        ]

        os.environ[ENV_CONDA] = "1"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix

        name = "rttest"

        uri = Resource(name, typ=ResourceType.runtime)
        sr = StandaloneRuntime(uri)
        sr.build_from_python_env(mode="conda", runtime_name=name)
        sr.info()
        sr.history()

        sw = SWCliConfigMixed()
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            name,
            sr._version[:VERSION_PREFIX_CNT],
            sr._version,
        )
        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))
        assert {
            "dependencies": ["dependencies/.starwhale/lock/conda-sw-lock.yaml"],
            "files": [],
            "runtime_yaml": "runtime.yaml",
            "wheels": [],
        } == _manifest["artifacts"]

        assert _manifest["dependencies"] == {
            "conda_files": [],
            "conda_pkgs": [],
            "local_packaged_env": False,
            "pip_files": [],
            "pip_pkgs": ["starwhale"],
            "raw_deps": [{"deps": ["starwhale"], "kind": "pip_pkg"}],
        }
        assert _manifest["configs"] == {
            "conda": {"channels": ["conda-forge"], "condarc": {}},
            "docker": {"image": ""},
            "pip": {"extra_index_url": [""], "index_url": "", "trusted_host": [""]},
        }

        _env = _manifest["environment"]
        assert _env["auto_lock_dependencies"]
        assert _env["lock"]["env_use_shell"]
        assert _env["lock"]["files"] == [".starwhale/lock/conda-sw-lock.yaml"]
        assert _env["python"] == "3.7"

        assert m_conda_export.called

        for p in _manifest["artifacts"]["dependencies"]:
            assert os.path.exists(os.path.join(runtime_workdir, p))

        assert not os.path.exists(os.path.join(runtime_workdir, "configs", "condarc"))

    @patch("starwhale.core.runtime.model.conda_export")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    @patch("starwhale.core.runtime.model.check_valid_conda_prefix")
    def test_build_with_no_cache(self, m_check: MagicMock, *args: t.Any):
        target_dir = "/home/starwhale/workdir"
        yaml_path = f"{target_dir}/{DefaultYAMLName.RUNTIME}"
        ensure_file(
            yaml_path,
            yaml.safe_dump(
                {
                    "name": "test",
                    "mode": "conda",
                }
            ),
            parents=True,
        )
        # make sure the dir is not deleted by "recreate_env_if_broken"
        m_check.return_value = True
        env_dir = f"{target_dir}/{SW_AUTO_DIRNAME}/conda"
        ensure_dir(env_dir)
        my_garbage = f"{env_dir}/garbage"
        ensure_dir(my_garbage)

        StandaloneRuntime.lock(target_dir, yaml_path)
        assert Path(my_garbage).exists()

        StandaloneRuntime.lock(target_dir, yaml_path, no_cache=True)
        assert not Path(my_garbage).exists()

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.get_user_runtime_python_bin")
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.utils.venv.subprocess.check_output")
    @patch("starwhale.utils.venv.check_call")
    def test_build_from_shell_with_check_error(
        self,
        m_check_call: MagicMock,
        m_call_output: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
    ) -> None:
        conda_prefix = "/home/starwhale/anaconda3/envs/starwhale"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))

        m_py_bin.return_value = os.path.join(conda_prefix, "bin/python3")
        m_venv.return_value = False
        m_conda.return_value = True
        m_call_output.side_effect = [
            b"3.7.13",
            conda_prefix.encode(),
            b"False",
        ]

        name = "error_demo_runtime"
        uri = Resource(name, typ=ResourceType.runtime, refine=False)
        sr = StandaloneRuntime(uri)

        m_check_call.side_effect = subprocess.CalledProcessError(
            1, "cmd", output="test"
        )
        with self.assertRaises(SystemExit):
            sr.build_from_python_env(runtime_name=name, mode="conda")

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.conda_export")
    @patch("starwhale.core.runtime.model.get_user_python_version")
    @patch("starwhale.core.runtime.model.get_python_version_by_bin")
    @patch("starwhale.core.runtime.model.get_user_runtime_python_bin")
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.utils.venv.subprocess.check_output")
    @patch("starwhale.utils.venv.check_call")
    def test_build_from_shell(
        self,
        m_check_call: MagicMock,
        m_call_output: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
        m_py_ver: MagicMock,
        m_user_py_ver: MagicMock,
        m_conda_export: MagicMock,
    ) -> None:
        conda_prefix = "/home/starwhale/anaconda3/envs/starwhale"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))

        m_py_bin.return_value = os.path.join(conda_prefix, "bin/python3")
        m_venv.return_value = False
        m_conda.return_value = True
        m_call_output.side_effect = [
            b"3.7.13",
            conda_prefix.encode(),
            b"False",
        ]
        m_py_ver.return_value = "fake.ver"

        name = "demo_runtime"
        uri = Resource(name, typ=ResourceType.runtime)
        sr = StandaloneRuntime(uri)
        with self.assertRaisesRegex(ConfigFormatError, "only support Python"):
            sr.build_from_python_env(runtime_name=name, mode="conda")

        assert m_check_call.call_args[0][0] == [
            f"{conda_prefix}/bin/python3",
            "-m",
            "pip",
            "check",
        ]

        m_check_call.reset_mock()
        m_call_output.reset_mock()
        m_call_output.side_effect = [b"3.7.13", conda_prefix.encode(), b"False"]

        m_py_ver.assert_called_once()
        m_user_py_ver.return_value = m_py_ver.return_value = "3.10"

        sr.build_from_python_env(runtime_name=name, mode="conda")
        assert m_py_ver.call_count == 3
        assert m_conda_export.called

        sw = SWCliConfigMixed()
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            name,
            sr._version[:VERSION_PREFIX_CNT],
            sr._version,
        )
        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))
        assert _manifest["environment"]["python"] == m_py_ver.return_value

    @patch("starwhale.core.runtime.model.conda_export")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    @patch("starwhale.core.runtime.model.check_valid_conda_prefix")
    @patch("tempfile.mkstemp")
    def test_lock_ret_val(self, m_mkstemp: MagicMock, m_check: MagicMock, *args: t.Any):
        target_dir = "/home/starwhale/workdir"
        yaml_path = f"{target_dir}/{DefaultYAMLName.RUNTIME}"
        ensure_dir(target_dir)
        ensure_file(
            yaml_path,
            yaml.safe_dump(
                {
                    "name": "test",
                    "mode": "conda",
                }
            ),
        )
        m_check.return_value = True
        lock_file = "/sw-dep-lock"
        m_mkstemp.return_value = (None, lock_file)
        ensure_file(lock_file, "foo")
        content, path = StandaloneRuntime.lock(target_dir, yaml_path)
        assert content == "foo"
        assert (
            path == Path(target_dir) / SW_AUTO_DIRNAME / "lock" / "conda-sw-lock.yaml"
        )

    @patch("starwhale.base.bundle.LocalStorageBundleMixin._gen_version")
    @patch("starwhale.core.runtime.model.StandaloneRuntime._prepare_snapshot")
    @patch("starwhale.core.runtime.model.StandaloneRuntime._dump_configs")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    @patch("starwhale.core.runtime.model.StandaloneRuntime._dump_dependencies")
    @patch("starwhale.core.runtime.model.StandaloneRuntime._dump_docker_image")
    @patch("starwhale.core.runtime.model.StandaloneRuntime._copy_src")
    @patch("starwhale.base.bundle.LocalStorageBundleMixin._make_tar")
    @patch("starwhale.base.bundle.LocalStorageBundleMixin._make_auto_tags")
    @patch("starwhale.core.runtime.model.get_user_python_version")
    @patch("starwhale.core.runtime.model.StandaloneRuntime._load_runtime_config")
    @patch("starwhale.core.runtime.model.StandaloneRuntime.lock")
    @patch("starwhale.core.runtime.model.get_python_version_by_bin")
    def test_sw_ver_detection_when_build(
        self,
        m_bin_py_ver: MagicMock,
        m_lock: MagicMock,
        m_rt_conf: MagicMock,
        m_py_env: MagicMock,
        *args: t.Any,
    ):
        name = "foo"
        m_rt_conf.return_value = RuntimeConfig(name)
        m_py_env.return_value = "venv"
        m_bin_py_ver.return_value = "0.0.1"

        sw = SWCliConfigMixed()
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            name,
        )

        lock_fpath = Path("/tmp/requirements-sw-lock.txt")
        lock_content = "#foo\nbar==1.1\nstarwhale==a.b.c\nbaz"
        ensure_file(lock_fpath, content=lock_content, parents=True)
        m_lock.return_value = lock_content, lock_fpath
        ensure_dir(runtime_workdir)

        venv_prefix = "/bar"
        ensure_dir(venv_prefix)
        self.fs.create_file(os.path.join(venv_prefix, "pyvenv.cfg"))
        uri = Resource(name, typ=ResourceType.runtime, refine=False)
        sr = StandaloneRuntime(uri)
        sr.build_from_python_env(
            runtime_name=name, mode="venv", venv_prefix=venv_prefix
        )
        assert sr._detected_sw_version == "a.b.c"
        runtime_workdir = os.path.join(
            runtime_workdir,
            sr._version[:VERSION_PREFIX_CNT],
            sr._version,
        )
        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))
        assert _manifest["environment"]["lock"]["starwhale_version"] == "a.b.c"

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.core.runtime.model.guess_current_py_env")
    @patch("starwhale.core.runtime.model.get_user_python_version")
    def test_build_from_docker_image(
        self,
        m_py_ver: MagicMock,
        m_py_env: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
    ) -> None:
        m_py_ver.return_value = "3.7"
        m_py_env.return_value = "system"
        m_venv.return_value = False
        m_conda.return_value = False

        name = "demo_runtime"
        docker_image = "user-defined-image:latest"
        uri = Resource(name, typ=ResourceType.runtime)
        sr = StandaloneRuntime(uri)
        sr.build_from_docker_image(image=docker_image, runtime_name=name)

        sw = SWCliConfigMixed()
        runtime_workdir = os.path.join(
            sw.rootdir,
            "self",
            "workdir",
            "runtime",
            name,
            sr._version[:VERSION_PREFIX_CNT],
            sr._version,
        )
        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))
        assert _manifest["base_image"] == docker_image
        assert _manifest["docker"] == {
            "builtin_run_image": {
                "fullname": "docker-registry.starwhale.cn/star-whale/starwhale:latest",
                "name": "starwhale",
                "repo": "docker-registry.starwhale.cn/star-whale",
                "tag": "latest",
            },
            "custom_run_image": "user-defined-image:latest",
        }
        assert _manifest["artifacts"] == {
            "dependencies": [],
            "files": [],
            "runtime_yaml": "runtime.yaml",
            "wheels": [],
        }
        assert _manifest["environment"]["lock"]["env_name"] == ""
        assert _manifest["environment"]["lock"]["env_prefix_path"] == ""
        assert not _manifest["environment"]["lock"]["env_use_shell"]

    def get_runtime_config(self) -> t.Dict[str, t.Any]:
        return {
            "name": "rttest",
            "mode": "venv",
            "environment": {
                "python": "3.7",
            },
            "dependencies": [
                "requirements.txt",
                {"wheels": ["dummy.whl"]},
                {"pip": ["Pillow"]},
            ],
        }

    @patch("starwhale.utils.venv.check_user_python_pkg_exists")
    @patch("starwhale.utils.venv.virtualenv.cli_run")
    @patch("starwhale.utils.venv.check_call")
    def test_restore_venv_with_auto_lock(
        self, m_call: MagicMock, m_venv: MagicMock, m_exists: MagicMock
    ):
        workdir = "/home/starwhale/myproject"
        export_dir = os.path.join(workdir, "export")
        dep_dir = os.path.join(workdir, "dependencies")
        scripts_dir = os.path.join(workdir, "files")
        wheels_dir = os.path.join(workdir, "wheels")

        ensure_dir(workdir)
        ensure_dir(scripts_dir)
        ensure_dir(wheels_dir)
        wheel_fpath = os.path.join(wheels_dir, "dummy.whl")
        self.fs.create_file(
            os.path.join(scripts_dir, "scripts", "prepare.sh"), contents=""
        )
        self.fs.create_file(wheel_fpath, contents="")

        self.fs.create_file(
            os.path.join(workdir, DEFAULT_MANIFEST_NAME),
            contents=yaml.safe_dump(
                {
                    "environment": {
                        "mode": "venv",
                        "python": "3.7",
                        "arch": [SupportArch.AMD64],
                        "lock": {
                            "files": [".starwhale/lock/requirements-sw-lock.txt"],
                        },
                    },
                    "dependencies": {
                        "local_packaged_env": False,
                        "raw_deps": [
                            {
                                "kind": "pip_req_file",
                                "deps": "requirements-sw-lock.txt",
                            },
                            {
                                "kind": "pip_pkg",
                                "deps": ["a", "b"],
                            },
                            {
                                "kind": "pip_req_file",
                                "deps": "requirements-test.txt",
                            },
                            {
                                "kind": "wheel",
                                "deps": ["dummy.whl"],
                            },
                            {
                                "kind": "pip_pkg",
                                "deps": ["c", "d"],
                            },
                            {
                                "kind": "native_file",
                                "deps": [
                                    {
                                        "dest": "bin/prepare.sh",
                                        "name": "prepare",
                                        "post": "bash bin/prepare.sh",
                                        "pre": "ls bin/prepare.sh",
                                        "src": "scripts/prepare.sh",
                                    }
                                ],
                            },
                        ],
                        "pip_files": [
                            "requirements-sw-lock.txt",
                            "requirements-test.txt",
                        ],
                    },
                    "artifacts": {
                        "files": [
                            {
                                "dest": "bin/prepare.sh",
                                "name": "prepare",
                                "post": "bash bin/prepare.sh",
                                "pre": "ls bin/prepare.sh",
                                "src": "scripts/prepare.sh",
                            }
                        ],
                        "wheels": ["wheels/dummy.whl"],
                        "dependencies": [
                            "dependencies/requirements-test.txt",
                            "dependencies/requirements-sw-lock.txt",
                        ],
                    },
                }
            ),
        )
        ensure_dir(export_dir)

        lock_dir = os.path.join(dep_dir, ".starwhale", "lock")
        ensure_dir(lock_dir)
        req_lock_fpath = os.path.join(lock_dir, "requirements-sw-lock.txt")
        self.fs.create_file(req_lock_fpath, contents="test2==0.0.1")

        m_exists.return_value = True
        Runtime.restore(Path(workdir))

        assert m_call.call_count == 2
        pip_cmds = [mc[0][0][4:] for mc in m_call.call_args_list]
        assert pip_cmds == [["-r", req_lock_fpath], [wheel_fpath]]

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
        scripts_dir = os.path.join(workdir, "files")
        wheels_dir = os.path.join(workdir, "wheels")

        ensure_dir(workdir)
        ensure_dir(scripts_dir)
        ensure_dir(wheels_dir)
        self.fs.create_file(
            os.path.join(scripts_dir, "scripts", "prepare.sh"), contents=""
        )
        self.fs.create_file(os.path.join(wheels_dir, "dummy.whl"), contents="")

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
                        "raw_deps": [
                            {
                                "kind": "pip_req_file",
                                "deps": "requirements-sw-lock.txt",
                            },
                            {
                                "kind": "pip_pkg",
                                "deps": ["a", "b"],
                            },
                            {
                                "kind": "pip_req_file",
                                "deps": "requirements-test.txt",
                            },
                            {
                                "kind": "wheel",
                                "deps": ["dummy.whl"],
                            },
                            {
                                "kind": "pip_pkg",
                                "deps": ["c", "d"],
                            },
                            {
                                "kind": "native_file",
                                "deps": [
                                    {
                                        "dest": "bin/prepare.sh",
                                        "name": "prepare",
                                        "post": "bash bin/prepare.sh",
                                        "pre": "ls bin/prepare.sh",
                                        "src": "scripts/prepare.sh",
                                    }
                                ],
                            },
                        ],
                        "pip_files": [
                            "requirements-sw-lock.txt",
                            "requirements-test.txt",
                        ],
                    },
                    "artifacts": {
                        "files": [
                            {
                                "dest": "bin/prepare.sh",
                                "name": "prepare",
                                "post": "bash bin/prepare.sh",
                                "pre": "ls bin/prepare.sh",
                                "src": "scripts/prepare.sh",
                            }
                        ],
                        "wheels": ["wheels/dummy.whl"],
                        "dependencies": [
                            "dependencies/requirements-test.txt",
                            "dependencies/requirements-sw-lock.txt",
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

        assert m_call.call_count == 8
        pip_cmds = [mc[0][0][4:] for mc in m_call.call_args_list]
        assert pip_cmds == [
            ["-r", req_lock_fpath],
            ["a"],
            ["b"],
            ["-r", req_fpath],
            [f"{workdir}/wheels/dummy.whl"],
            ["c"],
            ["d"],
            ["starwhale"],
        ]

        assert m_venv.call_args[0][0] == [
            venv_dir,
            "--python",
            "3.7",
        ]

        assert m_call.call_args_list[0][0][0] == [
            "/home/starwhale/myproject/export/venv/bin/pip",
            "install",
            "--exists-action",
            "w",
            "-r",
            req_lock_fpath,
        ]
        assert m_call.call_args_list[-1][0][0] == [
            "/home/starwhale/myproject/export/venv/bin/pip",
            "install",
            "--exists-action",
            "w",
            "starwhale",
        ]
        assert (Path(workdir) / "export/venv/bin/prepare.sh").exists()

        m_call.reset_mock()
        m_exists.return_value = True
        Runtime.restore(Path(workdir))
        assert m_call.call_count == 7

        RuntimeTermView.restore(workdir)

    @patch("starwhale.core.runtime.model.platform.machine")
    @patch("starwhale.utils.fs.tarfile.open")
    @patch("starwhale.utils.venv.check_call")
    def test_restore_conda_with_auto_lock(
        self, m_call: MagicMock, m_tar: MagicMock, m_machine: MagicMock
    ):
        name = "rttest"
        version = "1234"
        sw = SWCliConfigMixed()
        workdir = str(
            sw.rootdir
            / "self"
            / "workdir"
            / "runtime"
            / name
            / version[:VERSION_PREFIX_CNT]
            / version
        )
        ensure_dir(workdir)

        self.fs.create_dir(os.path.join(workdir, "configs"))
        condarc_path = os.path.join(workdir, "configs", "condarc")
        self.fs.create_file(
            condarc_path,
            contents=yaml.safe_dump({"verbosity": 3}, default_flow_style=False),
        )

        self.fs.create_file(
            os.path.join(workdir, DEFAULT_MANIFEST_NAME),
            contents=yaml.safe_dump(
                {
                    "environment": {
                        "python": "3.7",
                        "mode": "conda",
                        "arch": [SupportArch.ARM64],
                        "lock": {"files": [".starwhale/lock/conda-sw-lock.yaml"]},
                    },
                    "dependencies": {
                        "local_packaged_env": False,
                        "raw_deps": [
                            {
                                "kind": "pip_req_file",
                                "deps": "requirements-sw-lock.txt",
                            },
                            {
                                "kind": "pip_pkg",
                                "deps": ["a", "b"],
                            },
                            {
                                "kind": "wheel",
                                "deps": ["dummy.whl"],
                            },
                            {
                                "kind": "conda_pkg",
                                "deps": ["c", "d"],
                            },
                            {
                                "kind": "conda_env_file",
                                "deps": "conda-env.yaml",
                            },
                        ],
                    },
                }
            ),
        )

        export_dir = os.path.join(workdir, "export")
        ensure_dir(export_dir)
        dep_dir = os.path.join(workdir, "dependencies")
        lock_dir = os.path.join(dep_dir, ".starwhale", "lock")
        ensure_dir(lock_dir)
        req_lock_fpath = os.path.join(lock_dir, "conda-sw-lock.yaml")
        wheels_dir = os.path.join(workdir, "wheels")
        ensure_dir(wheels_dir)
        wheel_fpath = os.path.join(wheels_dir, "dummy.whl")

        self.fs.create_file(req_lock_fpath, contents="fake content")
        self.fs.create_file(wheel_fpath, contents="")

        assert "CONDARC" not in os.environ
        m_machine.return_value = "arm64"
        Runtime.restore(Path(workdir))

        assert os.environ["CONDARC"] == condarc_path
        assert m_call.call_count == 4
        conda_cmds = [cm[0][0] for cm in m_call.call_args_list]
        conda_prefix_dir = os.path.join(export_dir, "conda")
        assert conda_cmds == [
            [
                "conda",
                "create",
                "--yes",
                "--quiet",
                "--prefix",
                conda_prefix_dir,
                "python=3.7",
            ],
            [
                "conda",
                "env",
                "update",
                "--quiet",
                "--file",
                req_lock_fpath,
                "--prefix",
                conda_prefix_dir,
            ],
            [
                "conda",
                "run",
                "--live-stream",
                "--prefix",
                conda_prefix_dir,
                "python3",
                "-m",
                "pip",
                "install",
                "--exists-action",
                "w",
                wheel_fpath,
            ],
            [
                f"{conda_prefix_dir}/bin/python3",
                "-c",
                "import pkg_resources; pkg_resources.get_distribution('starwhale')",
            ],
        ]

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.core.runtime.model.platform.machine")
    @patch("starwhale.utils.fs.tarfile.open")
    @patch("starwhale.utils.venv.check_call")
    def test_restore_conda(
        self,
        m_call: MagicMock,
        m_tar: MagicMock,
        m_machine: MagicMock,
        *args: t.Any,
    ):
        name = "rttest"
        version = "1234"
        uri = "rttest/version/1234"
        sw = SWCliConfigMixed()
        workdir = str(
            sw.rootdir
            / "self"
            / "workdir"
            / "runtime"
            / name
            / version[:VERSION_PREFIX_CNT]
            / version
        )
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
                        "raw_deps": [
                            {
                                "kind": "conda_env_file",
                                "deps": "conda-env.yaml",
                            },
                            {
                                "kind": "pip_req_file",
                                "deps": "requirements-sw-lock.txt",
                            },
                            {
                                "kind": "pip_pkg",
                                "deps": ["a", "b"],
                            },
                            {
                                "kind": "wheel",
                                "deps": ["dummy.whl"],
                            },
                            {
                                "kind": "conda_pkg",
                                "deps": ["c", "d"],
                            },
                        ],
                    },
                }
            ),
        )
        ensure_dir(export_dir)

        dep_dir = os.path.join(workdir, "dependencies")
        req_lock_fpath = os.path.join(dep_dir, "requirements-sw-lock.txt")
        conda_env_fpath = os.path.join(dep_dir, "conda-env.yaml")
        wheels_dir = os.path.join(workdir, "wheels")
        ensure_dir(wheels_dir)
        wheel_fpath = os.path.join(wheels_dir, "dummy.whl")
        self.fs.create_file(req_lock_fpath, contents="test2==0.0.1")
        self.fs.create_file(wheel_fpath, contents="")
        self.fs.create_file(conda_env_fpath, contents="conda")

        with self.assertRaises(UnExpectedConfigFieldError):
            Runtime.restore(Path(workdir))

        m_machine.return_value = "arm64"
        os.environ[ENV_LOG_LEVEL] = "TRACE"
        Runtime.restore(Path(workdir))

        assert "CONDARC" not in os.environ
        assert m_call.call_count == 8
        conda_cmds = [cm[0][0] for cm in m_call.call_args_list]
        conda_prefix_dir = os.path.join(export_dir, "conda")
        assert conda_cmds == [
            [
                "conda",
                "create",
                "--yes",
                "--quiet",
                "--prefix",
                conda_prefix_dir,
                "-vvv",
                "python=3.7",
            ],
            [
                "conda",
                "env",
                "update",
                "-vvv",
                "--quiet",
                "--file",
                conda_env_fpath,
                "--prefix",
                conda_prefix_dir,
            ],
            [
                "conda",
                "run",
                "--live-stream",
                "-vvv",
                "--prefix",
                conda_prefix_dir,
                "python3",
                "-m",
                "pip",
                "install",
                "--exists-action",
                "w",
                "-r",
                req_lock_fpath,
            ],
            [
                "conda",
                "run",
                "--live-stream",
                "-vvv",
                "--prefix",
                conda_prefix_dir,
                "python3",
                "-m",
                "pip",
                "install",
                "--exists-action",
                "w",
                "a",
            ],
            [
                "conda",
                "run",
                "--live-stream",
                "-vvv",
                "--prefix",
                conda_prefix_dir,
                "python3",
                "-m",
                "pip",
                "install",
                "--exists-action",
                "w",
                "b",
            ],
            [
                "conda",
                "run",
                "--live-stream",
                "-vvv",
                "--prefix",
                conda_prefix_dir,
                "python3",
                "-m",
                "pip",
                "install",
                "--exists-action",
                "w",
                wheel_fpath,
            ],
            [
                "conda",
                "install",
                "-vvv",
                "--prefix",
                conda_prefix_dir,
                "--channel",
                "conda-forge",
                "--yes",
                "--override-channels",
                "c",
                "d",
            ],
            [
                f"{conda_prefix_dir}/bin/python3",
                "-c",
                "import pkg_resources; pkg_resources.get_distribution('starwhale')",
            ],
        ]

        RuntimeTermView.restore(workdir)

        ensure_file(
            os.path.join(workdir, DEFAULT_MANIFEST_NAME),
            yaml.safe_dump(
                {
                    "environment": {"mode": "conda", "python": "3.7"},
                    "dependencies": {"local_packaged_env": True, "raw_deps": []},
                }
            ),
        )
        tar_path = os.path.join(export_dir, EnvTarType.CONDA)
        ensure_file(tar_path, "test")

        Runtime.restore(Path(workdir))
        assert m_tar.call_count == 1
        assert str(m_tar.call_args[0][0]) == tar_path

        RuntimeTermView.restore(uri)

    @patch("starwhale.utils.venv.virtualenv")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_venv(
        self, m_output: MagicMock, m_call: MagicMock, m_venv: MagicMock
    ) -> None:
        target_dir = "/home/starwhale/workdir"
        ensure_dir(target_dir)
        yaml_path = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        lock_fpath = Path(target_dir) / ".starwhale" / "lock" / RuntimeLockFileType.VENV
        self.fs.create_file(os.path.join(target_dir, "dummy.whl"))
        self.fs.create_file(
            yaml_path,
            contents=yaml.safe_dump(
                {
                    "name": "test",
                    "mode": "venv",
                    "dependencies": [{"pip": ["a", "b"]}, {"wheels": ["dummy.whl"]}],
                }
            ),
        )

        def _patch_pip_freeze(*args: t.Any, **kwargs: t.Any) -> t.Any:
            if "pip freeze" in args[0]:
                lock_file = args[0].split(">>")[1].strip()
                ensure_file(
                    lock_file,
                    "\n".join(
                        [
                            "a==0.0.1",
                            "b @ file:///home/starwhale/workdir/dummy.whl",
                            "c @ file:///home/starwhale/workdir/dummy.whl#sha=1234567890",
                        ]
                    ),
                )
            return MagicMock()

        m_call.side_effect = _patch_pip_freeze

        venv_dir = "/tmp/venv"
        ensure_dir(venv_dir)
        self.fs.create_file(os.path.join(venv_dir, "pyvenv.cfg"))

        os.environ[ENV_VENV] = venv_dir
        content = load_yaml(yaml_path)
        assert RuntimeLockFileType.VENV not in content.get("dependencies", [])
        StandaloneRuntime.lock(target_dir, yaml_path, env_use_shell=True)

        assert m_call.call_args_list[0][0][0] == [
            f"{venv_dir}/bin/pip",
            "install",
            "--exists-action",
            "w",
            "a",
        ]
        assert m_call.call_args_list[1][0][0] == [
            f"{venv_dir}/bin/pip",
            "install",
            "--exists-action",
            "w",
            "b",
        ]

        assert m_call.call_args_list[2][0][0] == [
            f"{venv_dir}/bin/pip",
            "install",
            "--exists-action",
            "w",
            f"{target_dir}/dummy.whl",
        ]
        assert m_call.call_args_list[3][0][0].startswith(
            " ".join(
                [
                    f"{venv_dir}/bin/python3",
                    "-m",
                    "pip",
                    "freeze",
                    "--require-virtualenv",
                    "--exclude-editable",
                    ">>",
                    f"{tempfile.gettempdir()}/starwhale-lock-",
                ]
            )
        )
        assert m_output.call_count == 2
        assert m_output.call_args_list[0][0][0] == [
            f"{venv_dir}/bin/python3",
            "-m",
            "pip",
            "config",
            "list",
            "--user",
        ]
        assert m_output.call_args_list[1][0][0] == [
            f"{venv_dir}/bin/python3",
            "-c",
            "import sys; _v=sys.version_info;print(f'{_v.major}.{_v.minor}.{_v.micro}')",
        ]

        assert lock_fpath.exists()
        content = load_yaml(yaml_path)
        assert lock_fpath.read_text() == "a==0.0.1"
        assert RuntimeLockFileType.VENV not in content["dependencies"]
        del os.environ[ENV_VENV]

        StandaloneRuntime.lock(
            target_dir,
            yaml_path,
            env_prefix_path=venv_dir,
            include_editable=True,
            include_local_wheel=True,
        )
        assert m_call.call_args[0][0].startswith(
            " ".join(
                [
                    f"{venv_dir}/bin/python3",
                    "-m",
                    "pip",
                    "freeze",
                    "--require-virtualenv",
                    ">>",
                    f"{tempfile.gettempdir()}/starwhale-lock-",
                ]
            )
        )
        assert lock_fpath.exists()
        assert "dummy.whl" in lock_fpath.read_text()

        m_output.reset_mock()
        m_call.reset_mock()

        sw_venv_dir = os.path.join(target_dir, SW_AUTO_DIRNAME, "venv")
        venv_cfg = os.path.join(sw_venv_dir, "pyvenv.cfg")
        assert not os.path.exists(sw_venv_dir)
        assert not os.path.exists(venv_cfg)

        def _mock_cli_run(args: t.Any) -> t.Any:
            ensure_dir(sw_venv_dir)
            ensure_file(venv_cfg, content="")
            return MagicMock()

        m_venv.cli_run = _mock_cli_run
        StandaloneRuntime.lock(target_dir, yaml_path)

        assert m_call.call_count == 4
        assert m_output.call_count == 2

        assert os.path.exists(sw_venv_dir)
        assert os.path.exists(venv_cfg)
        assert lock_fpath.exists()

    def test_abnormal_lock(self) -> None:
        with self.assertRaises(NotFoundError):
            RuntimeTermView.lock("not-found", "not-found")

        target_dir = "/home/starwhale/workdir"
        ensure_dir(target_dir)
        yaml_path = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        self.fs.create_file(
            yaml_path, contents=yaml.safe_dump({"name": "test", "mode": "no-mode"})
        )

        with self.assertRaises(ExclusiveArgsError):
            RuntimeTermView.lock(
                target_dir,
                yaml_path,
                env_name="test",
                env_prefix_path="1",
                env_use_shell=True,
            )

        with self.assertRaises(ExclusiveArgsError):
            RuntimeTermView.lock(
                target_dir, yaml_path, env_name="test", env_prefix_path="1"
            )

        with self.assertRaises(NoSupportError):
            RuntimeTermView.lock(target_dir, yaml_path, env_prefix_path="test")

        os.unlink(yaml_path)
        self.fs.create_file(
            yaml_path, contents=yaml.safe_dump({"name": "test", "mode": "venv"})
        )

        with self.assertRaises(NoSupportError):
            RuntimeTermView.lock(target_dir, yaml_path, env_name="test")

        with self.assertRaises(FormatError):
            RuntimeTermView.lock(target_dir, yaml_path, env_prefix_path="1")

        os.unlink(yaml_path)
        self.fs.create_file(
            yaml_path, contents=yaml.safe_dump({"name": "test", "mode": "conda"})
        )

        with self.assertRaises(FormatError):
            RuntimeTermView.lock(target_dir, yaml_path, env_prefix_path="1")

    def _render_conda_export(self, cmd: t.Any) -> None:
        if cmd[:3] == ["conda", "env", "export"]:
            path = cmd[-1]
            ensure_file(
                path,
                content=yaml.safe_dump(
                    {"name": "test", "dependencies": ["b", "d", {"pip": ["a", "c"]}]}
                ),
            )

    def _prepare_conda_runtime_workdir(self) -> t.Tuple[str, str, str]:
        target_dir = "/home/starwhale/workdir"
        ensure_dir(target_dir)
        yaml_path = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        self.fs.create_file(
            yaml_path,
            contents=yaml.safe_dump(
                {
                    "name": "test",
                    "mode": "conda",
                    "dependencies": [{"pip": ["a", "c"]}, {"conda": ["b", "d"]}],
                }
            ),
        )
        conda_dir = "/tmp/conda"
        ensure_dir(conda_dir)
        ensure_dir(os.path.join(conda_dir, "conda-meta"))

        return target_dir, yaml_path, conda_dir

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.get_conda_prefix_path")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_conda_with_yaml(
        self, m_output: MagicMock, m_call: MagicMock, m_get_prefix: MagicMock
    ) -> None:
        target_dir, yaml_path, _ = self._prepare_conda_runtime_workdir()
        m_call.side_effect = self._render_conda_export

        m_output.side_effect = [b"a", b"3.7.13"]

        sw_conda_dir = os.path.join(target_dir, SW_AUTO_DIRNAME, "conda")
        ensure_dir(sw_conda_dir)
        ensure_dir(os.path.join(sw_conda_dir, "conda-meta"))

        StandaloneRuntime.lock(target_dir, yaml_path)

        assert m_call.call_count == 4
        assert m_call.call_args_list[0][0][0] == [
            "conda",
            "run",
            "--live-stream",
            "--prefix",
            sw_conda_dir,
            "python3",
            "-m",
            "pip",
            "install",
            "--exists-action",
            "w",
            "a",
        ]
        assert m_call.call_args_list[1][0][0] == [
            "conda",
            "run",
            "--live-stream",
            "--prefix",
            sw_conda_dir,
            "python3",
            "-m",
            "pip",
            "install",
            "--exists-action",
            "w",
            "c",
        ]
        assert m_call.call_args_list[2][0][0] == [
            "conda",
            "install",
            "--prefix",
            sw_conda_dir,
            "--channel",
            "conda-forge",
            "--yes",
            "--override-channels",
            "b",
            "d",
        ]

        assert m_call.call_args_list[3][0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            sw_conda_dir,
            "--file",
        ]

        lock_yaml_path = os.path.join(
            target_dir, ".starwhale", "lock", RuntimeLockFileType.CONDA
        )
        content = load_yaml(lock_yaml_path)
        assert content == {
            "dependencies": ["b", "d", {"pip": ["a"]}],
            "name": "test",
        }

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.get_conda_prefix_path")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_conda_with_shell(
        self, m_output: MagicMock, m_call: MagicMock, m_get_prefix: MagicMock
    ) -> None:
        target_dir, yaml_path, conda_dir = self._prepare_conda_runtime_workdir()
        m_get_prefix.return_value = conda_dir
        m_call.side_effect = self._render_conda_export
        pip_freeze_content = "\n".join(
            [
                "#",
                " ",
                "a",
                "xxx.whl",
                "@ file://xxx",
                "mock @ git+https://abc.com/mock.git@main",
            ]
        )
        m_output.side_effect = [pip_freeze_content.encode(), conda_dir.encode()]

        os.environ[ENV_CONDA_PREFIX] = conda_dir
        StandaloneRuntime.lock(
            target_dir, yaml_path, env_use_shell=True, include_local_wheel=True
        )
        assert m_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            conda_dir,
            "--file",
        ]
        lock_yaml_path = os.path.join(
            target_dir, ".starwhale", "lock", RuntimeLockFileType.CONDA
        )
        content = load_yaml(lock_yaml_path)
        assert content == {
            "dependencies": [
                "b",
                "d",
                {
                    "pip": [
                        "a",
                        "xxx.whl",
                        "@ file://xxx",
                        "mock @ git+https://abc.com/mock.git@main",
                    ]
                },
            ],
            "name": "test",
        }

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.get_conda_prefix_path")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_conda_with_name(
        self, m_output: MagicMock, m_call: MagicMock, m_get_prefix: MagicMock
    ) -> None:
        target_dir, yaml_path, conda_dir = self._prepare_conda_runtime_workdir()
        m_call.side_effect = self._render_conda_export
        m_output.side_effect = [b"", conda_dir.encode()]
        m_get_prefix.return_value = conda_dir

        StandaloneRuntime.lock(
            target_dir, yaml_path, env_name="conda-env-name", include_editable=True
        )
        assert m_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            conda_dir,
            "--file",
        ]
        assert m_output.call_args_list[0][0][0] == [
            "conda",
            "run",
            "--prefix",
            conda_dir,
            "pip",
            "freeze",
        ]
        lock_yaml_path = os.path.join(
            target_dir, ".starwhale", "lock", RuntimeLockFileType.CONDA
        )
        content = load_yaml(lock_yaml_path)
        assert content == {"dependencies": ["b", "d"], "name": "test"}

    @patch("os.environ", {})
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_conda_with_prefix(
        self, m_output: MagicMock, m_call: MagicMock
    ) -> None:
        target_dir, yaml_path, conda_dir = self._prepare_conda_runtime_workdir()
        m_call.side_effect = self._render_conda_export

        pip_freeze_content = "\n".join(
            [
                "#",
                " ",
                "a",
                "xxx.whl",
                "@ file://xxx",
                "mock @ git+https://abc.com/mock.git@main",
            ]
        )
        m_output.side_effect = [pip_freeze_content.encode(), conda_dir.encode()]

        content = load_yaml(yaml_path)
        assert RuntimeLockFileType.CONDA not in content.get("dependencies", {})
        RuntimeTermView.lock(target_dir, yaml_path, env_prefix_path=conda_dir)

        assert m_call.call_args_list[0][0][0] == [
            "conda",
            "run",
            "--live-stream",
            "--prefix",
            conda_dir,
            "python3",
            "-m",
            "pip",
            "install",
            "--exists-action",
            "w",
            "a",
        ]
        assert m_call.call_args_list[1][0][0] == [
            "conda",
            "run",
            "--live-stream",
            "--prefix",
            conda_dir,
            "python3",
            "-m",
            "pip",
            "install",
            "--exists-action",
            "w",
            "c",
        ]
        assert m_call.call_args_list[2][0][0] == [
            "conda",
            "install",
            "--prefix",
            conda_dir,
            "--channel",
            "conda-forge",
            "--yes",
            "--override-channels",
            "b",
            "d",
        ]

        assert m_call.call_args_list[3][0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            conda_dir,
            "--file",
        ]

        assert m_output.call_args_list[0][0][0] == [
            "conda",
            "run",
            "--prefix",
            conda_dir,
            "pip",
            "freeze",
            "--exclude-editable",
        ]
        assert m_output.call_args_list[1][0][0] == [
            f"{conda_dir}/bin/python3",
            "-c",
            "import sys; _v=sys.version_info;print(f'{_v.major}.{_v.minor}.{_v.micro}')",
        ]

        lock_yaml_path = os.path.join(
            target_dir, ".starwhale", "lock", RuntimeLockFileType.CONDA
        )
        assert os.path.exists(lock_yaml_path)
        content = load_yaml(lock_yaml_path)
        assert content["dependencies"] == [
            "b",
            "d",
            {"pip": ["a", "mock @ git+https://abc.com/mock.git@main"]},
        ]

    def get_mock_manifest(self) -> t.Dict[str, t.Any]:
        return {
            "name": "rttest",
            "version": "112233",
            "base_image": "docker-registry.starwhale.cn/star-whale/starwhale:latest-cuda11.4",
            "dependencies": {
                "conda_files": [],
                "conda_pkgs": [],
                "pip_pkgs": ["numpy"],
                "pip_files": ["requirements-sw-lock.txt"],
                "local_packaged_env": False,
            },
            "configs": {
                "conda": {"channels": ["conda-forge"]},
                "docker": {"image": "ghcr.io/star-whale/runtime/pytorch"},
                "pip": {
                    "extra_index_url": ["https://pypi.doubanio.com/simple"],
                    "index_url": "https://pypi.tuna.tsinghua.edu.cn/simple",
                    "trusted_host": ["pypi.tuna.tsinghua.edu.cn", "pypi.doubanio.com"],
                },
            },
            "artifacts": {
                "dependencies": ["dependencies/requirements-sw-lock.txt"],
                "files": [
                    {
                        "dest": "bin/prepare.sh",
                        "name": "prepare",
                        "post": "bash bin/prepare.sh",
                        "pre": "ls bin/prepare.sh",
                        "src": "scripts/prepare.sh",
                    }
                ],
                "runtime_yaml": "runtime.yaml",
                "wheels": ["wheels/dummy-0.0.0-py3-none-any.whl"],
            },
            "environment": {
                "arch": ["noarch"],
                "auto_lock_dependencies": False,
                "lock": {
                    "env_name": "",
                    "env_prefix_path": "",
                    "shell": {
                        "python_env": "conda",
                        "python_version": "3.8.13",
                        "use_conda": True,
                        "use_venv": False,
                    },
                    "starwhale_version": "0.0.0.dev0",
                    "system": platform.system(),
                    "use_shell_detection": True,
                },
                "mode": "venv",
                "python": "3.8",
            },
        }

    def test_dockerize_with_extract(self) -> None:
        self.fs.add_real_directory(_TEMPLATE_DIR)
        name = "rttest"
        version = "112233"
        image = "docker.io/t1/t2"
        uri = Resource(f"{name}/version/{version}", typ=ResourceType.runtime)
        manifest = self.get_mock_manifest()
        manifest["version"] = version
        manifest["configs"]["docker"]["image"] = image

        sr = StandaloneRuntime(uri)

        ensure_dir(sr.store.runtime_dir / "11")
        ensure_file(sr.store.runtime_dir / "11" / "112233.swrt", content=_swrt)
        sr.dockerize(
            tags=["t1", "t2"],
            platforms=[SupportArch.AMD64],
            push=True,
            dry_run=True,
            use_starwhale_builder=True,
            reset_qemu_static=True,
        )

        dockerfile_path = sr.store.export_dir / "docker" / "Dockerfile"
        dockerignore_path = sr.store.snapshot_workdir / ".dockerignore"
        assert dockerfile_path.exists()
        assert dockerignore_path.exists()

    @patch("starwhale.utils.docker.check_call")
    def test_dockerize(self, m_check: MagicMock) -> None:
        self.fs.add_real_directory(_TEMPLATE_DIR)
        name = "rttest"
        version = "112233"
        image = "docker.io/t1/t2"
        uri = Resource(f"{name}/version/{version}", typ=ResourceType.runtime)
        manifest = self.get_mock_manifest()
        manifest["version"] = version
        manifest["configs"]["docker"]["image"] = image

        custom_image = "docker.io/sw/base:v1"
        manifest["docker"] = {
            "custom_run_image": custom_image,
            "builtin_run_image": {
                "repo": "self-registry/sw",
                "name": "starwhale",
                "tag": "v2-cuda11.7",
                "fullname": "self-registry/sw/starwhale:v2",
            },
        }

        sr = StandaloneRuntime(uri)

        ensure_dir(sr.store.snapshot_workdir)
        ensure_file(sr.store.manifest_path, content=yaml.safe_dump(manifest))
        sr.dockerize(
            tags=["t1", "t2"],
            platforms=[SupportArch.AMD64],
            push=True,
            dry_run=False,
            use_starwhale_builder=True,
            reset_qemu_static=True,
        )

        dockerfile_path = sr.store.export_dir / "docker" / "Dockerfile"
        dockerignore_path = sr.store.snapshot_workdir / ".dockerignore"
        assert dockerfile_path.exists()
        assert dockerignore_path.exists()
        dockerfile_content = dockerfile_path.read_text()
        assert f"BASE_IMAGE={custom_image}" in dockerfile_content
        assert f"starwhale_runtime_version={version}" in dockerfile_content

        assert m_check.call_count == 3
        assert m_check.call_args_list[0][0][0] == [
            "docker",
            "run",
            "--rm",
            "--privileged",
            "multiarch/qemu-user-static",
            "--reset",
            "-p",
            "-yes",
        ]
        assert m_check.call_args_list[1][0][0] == [
            "docker",
            "buildx",
            "inspect",
            "--builder",
            "starwhale-multiarch-runtime-builder",
        ]

        build_cmd = " ".join(m_check.call_args_list[2][0][0])
        assert "--builder starwhale-multiarch-runtime-builder" in build_cmd
        assert "--platform linux/amd64" in build_cmd
        assert "--tag t1" in build_cmd
        assert "--tag t2" in build_cmd
        assert f"--tag {image}:{version}" in build_cmd
        assert "--push" in build_cmd
        assert f"--file {dockerfile_path}" in build_cmd

        uri = Resource(f"{name}/version/{version}", typ=ResourceType.runtime)
        RuntimeTermView(uri).dockerize(
            tags=("t1", "t2", "t3"),  # type: ignore
            push=False,
            platforms=[SupportArch.ARM64],
            dry_run=False,
            use_starwhale_builder=False,
            reset_qemu_static=False,
        )

    @patch("starwhale.core.runtime.model.StandaloneRuntime.restore")
    @patch("starwhale.core.runtime.model.StandaloneRuntime.extract")
    @patch("shellingham.detect_shell")
    @patch("os.execl")
    def test_activate(
        self,
        m_execl: MagicMock,
        m_detect: MagicMock,
        m_extract: MagicMock,
        m_restore: MagicMock,
    ) -> None:
        sw = SWCliConfigMixed()
        name = "rttest"
        version = "123"
        snapshot_dir = (
            sw.rootdir
            / "self"
            / "workdir"
            / "runtime"
            / name
            / f"{version[:VERSION_PREFIX_CNT]}"
            / version
        )
        manifest = self.get_mock_manifest()
        manifest["version"] = version
        ensure_dir(snapshot_dir)
        ensure_file(snapshot_dir / DEFAULT_MANIFEST_NAME, yaml.safe_dump(manifest))

        venv_dir = snapshot_dir / "export" / "venv"
        ensure_dir(venv_dir)

        m_detect.return_value = ["zsh", "/usr/bin/zsh"]
        uri = Resource(f"{name}/version/{version}", typ=ResourceType.runtime)
        StandaloneRuntime.activate(uri=uri)
        assert m_execl.call_args[0][0] == "/usr/bin/zsh"
        assert not m_extract.called
        assert not m_restore.called

        ensure_file(venv_dir / ".runtime_restore_status", "success", parents=True)
        StandaloneRuntime.activate(uri=uri, force_restore=False)
        assert not m_restore.called

        ensure_file(venv_dir / ".runtime_restore_status", "failed", parents=True)
        StandaloneRuntime.activate(uri=uri, force_restore=False)
        assert m_restore.called

        ensure_file(venv_dir / ".runtime_restore_status", "restoring", parents=True)
        StandaloneRuntime.activate(uri=uri, force_restore=False)
        assert m_restore.called

        ensure_file(
            venv_dir / ".runtime_restore_status", "wrong-word-xxx", parents=True
        )
        StandaloneRuntime.activate(uri=uri, force_restore=False)
        assert m_restore.called

        m_execl.reset_mock()
        runtime_config = self.get_runtime_config()
        runtime_config["mode"] = "conda"
        ensure_file(
            snapshot_dir / DefaultYAMLName.RUNTIME, yaml.safe_dump(runtime_config)
        )

        m_execl.reset_mock()
        m_detect.return_value = ["bash", "/usr/bin/bash"]
        StandaloneRuntime.activate(uri=uri, force_restore=True)
        assert not m_extract.called
        assert m_restore.called
        assert m_restore.call_args[0][0] == snapshot_dir

    @patch("starwhale.core.runtime.store.SWCliConfigMixed")
    def test_docker_run_image(self, m_config: MagicMock) -> None:
        manifest = {
            "docker": {
                "custom_run_image": "user-faked-image:v1",
                "builtin_run_image": {
                    "repo": "docker-registry.starwhale.cn",
                    "name": "starwhale",
                    "tag": "latest",
                },
            }
        }
        assert manifest["docker"][
            "custom_run_image"
        ] == get_docker_run_image_by_manifest(manifest)

        manifest = {
            "docker": {
                "custom_run_image": "",
                "builtin_run_image": {
                    "repo": "test-registry.starwhale.cn",
                    "name": "starwhale",
                    "tag": "latest",
                },
            }
        }

        m_config.return_value.docker_builtin_image_repo = ""
        assert (
            "test-registry.starwhale.cn/starwhale:latest"
            == get_docker_run_image_by_manifest(manifest)
        )

        repo_from_env = "ghcr.io/repo-from-env"
        os.environ[ENV_SW_IMAGE_REPO] = repo_from_env
        image = get_docker_run_image_by_manifest(manifest)
        assert image == f"{repo_from_env}/starwhale:latest"

        repo_from_config = "docker.io/image-from-config"
        m_config.return_value.docker_builtin_image_repo = repo_from_config
        os.environ.pop(ENV_SW_IMAGE_REPO, None)
        image = get_docker_run_image_by_manifest(manifest)
        assert image == f"{repo_from_config}/starwhale:latest"

        m_config.return_value.docker_builtin_image_repo = ""
        manifest["docker"]["builtin_run_image"]["repo"] = ""
        image = get_docker_run_image_by_manifest(manifest)
        assert image.startswith(DEFAULT_IMAGE_REPO)

        manifest = {"base_image": "old_version_faked_image"}
        assert manifest["base_image"] == get_docker_run_image_by_manifest(manifest)

        manifest = {}
        assert DEFAULT_SW_TASK_RUN_IMAGE == get_docker_run_image_by_manifest(manifest)

    def test_property(self) -> None:
        name = "rttest"
        version = "123"
        uri = Resource(f"{name}/version/{version}", typ=ResourceType.runtime)
        rs = RuntimeStorage(uri)

        sw = SWCliConfigMixed()

        project_dir = sw.rootdir / "self"
        runtime_dir = project_dir / "runtime" / name
        snapshot_dir = (
            project_dir
            / "workdir"
            / "runtime"
            / name
            / f"{version[:VERSION_PREFIX_CNT]}"
            / version
        )

        assert rs.bundle_type == ".swrt"
        assert rs.uri_type == "runtime"
        assert rs.runtime_dir == runtime_dir
        assert rs.manifest_path == snapshot_dir / DEFAULT_MANIFEST_NAME
        assert rs.export_dir == snapshot_dir / "export"
        assert (
            rs.recover_loc
            == sw.rootdir
            / "self"
            / "runtime"
            / ".recover"
            / name
            / version[:VERSION_PREFIX_CNT]
            / f"{version}.swrt"
        )
        assert rs.snapshot_workdir == snapshot_dir
        assert (
            rs.recover_snapshot_workdir
            == sw.rootdir
            / "self"
            / "workdir"
            / "runtime"
            / ".recover"
            / name
            / version[:VERSION_PREFIX_CNT]
            / version
        )


class DependenciesTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    def test_create_dependencies(self) -> None:
        deps_config = [
            "requirements.txt",
            {"pip": ["p1", "p2", "p3"]},
            {"wheels": ["dummy.whl"]},
            {"conda": ["cp1", "cp2"]},
            {
                "files": [
                    {
                        "dest": "bin/prepare.sh",
                        "name": "prepare",
                        "post": "bash bin/prepare.sh",
                        "pre": "ls bin/prepare.sh",
                        "src": "scripts/prepare.sh",
                    },
                    {
                        "dest": "bin/prepare2.sh",
                        "name": "prepare2",
                        "src": "scripts/prepare2.sh",
                    },
                ]
            },
            {"pip": ["p4", "p5", "p6"]},
            {"conda": ["cp3", "cp4"]},
            "conda-env.yaml",
            "requirements.in",
            "conda-env.yml",
        ]

        dep = Dependencies(deps_config)
        assert len(dep._unparsed) == 0
        assert len(dep.deps) == 10
        assert dep._pip_files == ["requirements.txt", "requirements.in"]
        assert dep._pip_pkgs == ["p1", "p2", "p3", "p4", "p5", "p6"]
        assert dep._conda_pkgs == ["cp1", "cp2", "cp3", "cp4"]
        assert dep._conda_files == ["conda-env.yaml", "conda-env.yml"]
        assert len(dep._files) == 2
        assert dep._wheels == ["dummy.whl"]
        expected_kinds = [
            DependencyType.PIP_REQ_FILE,
            DependencyType.PIP_PKG,
            DependencyType.WHEEL,
            DependencyType.CONDA_PKG,
            DependencyType.NATIVE_FILE,
            DependencyType.PIP_PKG,
            DependencyType.CONDA_PKG,
            DependencyType.CONDA_ENV_FILE,
            DependencyType.PIP_REQ_FILE,
            DependencyType.CONDA_ENV_FILE,
        ]

        flat_raw_deps = dep.flatten_raw_deps()
        for idx, (kind, data) in enumerate(zip(expected_kinds, deps_config)):
            assert kind == dep.deps[idx].kind
            if isinstance(data, dict):
                data = list(data.values())[0]
            assert data == dep.deps[idx].deps  # type: ignore
            assert flat_raw_deps[idx]["kind"] == kind.value
            assert flat_raw_deps[idx]["deps"] == data

        r_dict = dep.asdict()
        assert isinstance(r_dict, dict)
        assert "_unparsed" not in r_dict

    def test_create_abnormal_dependencies(self) -> None:
        deps_config = [
            "requirements",
            {"not_found": ["p4", "p5", "p6"]},
            "conda-env.y",
            "requirements.out",
        ]

        dep = Dependencies(deps_config)
        assert len(dep.deps) == 0
        assert len(dep._unparsed) == 4
        assert "requirements" in dep._unparsed

        with self.assertRaises(NoSupportError):
            Dependencies([{"pip": "test"}])  # type: ignore

        with self.assertRaises(FormatError):
            Dependencies([{"pip": [None, None]}])  # type: ignore

        with self.assertRaises(NoSupportError):
            Dependencies([{"conda": "test"}])  # type: ignore

        with self.assertRaises(FormatError):
            Dependencies([{"conda": [None, None]}])  # type: ignore

        with self.assertRaises(NoSupportError):
            Dependencies([{"wheels": "fsdf"}])  # type: ignore

        with self.assertRaises(FormatError):
            Dependencies([{"wheels": [None, None]}])  # type: ignore

        with self.assertRaises(NoSupportError):
            Dependencies([{"files": "fsdf"}])  # type: ignore

        with self.assertRaises(FormatError):
            Dependencies([{"files": [None, None]}])  # type: ignore

        with self.assertRaises(FormatError):
            Dependencies([{"files": [{}, {}]}])  # type: ignore

        with self.assertRaises(FormatError):
            WheelDependency(["d.d"])


class CloudRuntimeTest(TestCase):
    def setUp(self) -> None:
        sw_config._config = {}

    def test_cli_list(self) -> None:
        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            runtime_list_cli,
            [
                "--filter",
                "name=pytorch",
                "--filter",
                "owner=test",
                "--filter",
                "latest",
            ],
            obj=mock_obj,
        )

        assert result.exit_code == 0
        assert mock_obj.list.call_count == 1
        call_args = mock_obj.list.call_args[0]
        assert len(call_args[5]) == 3
        assert "name=pytorch" in call_args[5]
        assert "owner=test" in call_args[5]
        assert "latest" in call_args[5]

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            runtime_list_cli,
            [],
            obj=mock_obj,
        )

        assert result.exit_code == 0
        assert mock_obj.list.call_count == 1
        call_args = mock_obj.list.call_args[0]
        assert len(call_args[5]) == 0
