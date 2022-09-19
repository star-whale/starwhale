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
from starwhale.utils.error import NoSupportError, UnExpectedConfigFieldError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.runtime.view import (
    get_term_view,
    RuntimeTermView,
    RuntimeTermViewRich,
)
from starwhale.core.runtime.model import (
    Runtime,
    _TEMPLATE_DIR,
    RuntimeConfig,
    StandaloneRuntime,
)
from starwhale.core.runtime.store import RuntimeStorage
from starwhale.core.runtime.process import Process


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

        RuntimeTermView.quickstart_from_ishell(
            workdir=workdir,
            name=name,
            mode=PythonRunEnv.VENV,
            create_env=True,
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

    @patch("starwhale.core.runtime.model.StandaloneRuntime.restore")
    @patch("starwhale.base.bundle.extract_tar")
    @patch("starwhale.core.runtime.model.BundleCopy")
    def test_quickstart_from_uri(
        self, m_bundle_copy: MagicMock, m_extract: MagicMock, m_restore: MagicMock
    ) -> None:
        workdir = Path("/home/starwhale/myproject")
        name = "rttest"
        version = "112233"
        cloud_uri = URI(f"http://0.0.0.0:80/project/1/runtime/{name}/version/{version}")
        ensure_dir(workdir / ".extract")

        runtime_config = self.get_runtime_config()
        runtime_config["name"] = name
        extract_dir = workdir / ".extract"
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
        venv_dir = workdir / ".venv"
        ensure_dir(venv_dir)

        assert not (workdir / "dummy.whl").exists()
        assert not (venv_dir / ".gitignore").exists()
        assert not (workdir / "requirements.txt").exists()

        RuntimeTermView.quickstart_from_uri(
            workdir=workdir,
            name=name,
            uri=cloud_uri,
            force=True,
            restore=True,
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
        assert (venv_dir / ".gitignore").exists()
        assert (workdir / "requirements.txt").exists()

        assert m_bundle_copy.call_count == 1
        assert m_extract.call_count == 1
        assert m_restore.call_args[0] == (extract_dir, venv_dir)

    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output", return_value=b"3.7")
    def test_build_venv(
        self, m_output: MagicMock, m_check_call: MagicMock, m_py_bin: MagicMock
    ) -> None:
        name = "rttest"
        venv_dir = "/home/starwhale/venv"
        self.fs.create_file(os.path.join(venv_dir, "pyvenv.cfg"))
        venv_path = os.path.join(venv_dir, "bin/python3")
        os.environ[ENV_VENV] = venv_path
        m_py_bin.return_value = venv_path
        build_version = ""

        sw = SWCliConfigMixed()
        workdir = "/home/starwhale/myproject"
        runtime_config = self.get_runtime_config()
        runtime_config["environment"]["cuda"] = "11.5"
        runtime_config["environment"]["cudnn"] = "8"
        runtime_config["dependencies"].append(
            {
                "files": [
                    {
                        "dest": "bin/prepare.sh",
                        "name": "prepare",
                        "post": "bash bin/prepare.sh",
                        "pre": "ls bin/prepare.sh",
                        "src": "prepare.sh",
                    }
                ]
            }
        )
        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.RUNTIME),
            contents=yaml.safe_dump(runtime_config),
        )
        self.fs.create_file(os.path.join(workdir, "prepare.sh"), contents="")
        self.fs.create_file(
            os.path.join(workdir, "requirements.txt"), contents="requests==2.0.0"
        )
        self.fs.create_file(os.path.join(workdir, "dummy.whl"), contents="")

        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        sr.build(Path(workdir), enable_lock=True, env_prefix_path=venv_dir)
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
        assert os.path.exists(os.path.join(runtime_workdir, "wheels", "dummy.whl"))
        assert os.path.exists(os.path.join(runtime_workdir, "files/bin/prepare.sh"))

        assert "latest" in sr.tag.list()

        _manifest = load_yaml(os.path.join(runtime_workdir, DEFAULT_MANIFEST_NAME))

        assert _manifest["configs"] == {
            "conda": {"channels": ["conda-forge"]},
            "docker": {"image": ""},
            "pip": {"extra_index_url": [""], "index_url": "", "trusted_host": [""]},
        }

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
        assert _manifest["artifacts"]["wheels"] == ["wheels/dummy.whl"]
        assert _manifest["artifacts"]["files"][0] == {
            "_swrt_dest": "files/bin/prepare.sh",
            "dest": "bin/prepare.sh",
            "name": "prepare",
            "post": "bash bin/prepare.sh",
            "pre": "ls bin/prepare.sh",
            "src": "prepare.sh",
        }
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

        runtime_term_view = get_term_view({"output": "json"})

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

        runtime_term_view(name).history(fullname=True)
        runtime_term_view(name).info(fullname=True)
        runtime_term_view(f"{name}/version/{build_version}").info(fullname=True)

        runtime_term_view.list()
        RuntimeTermViewRich.list()
        runtime_term_view.list("myproject")
        RuntimeTermViewRich.list("myproject")
        runtime_term_view.build(workdir)
        rts = StandaloneRuntime.list(URI(""))
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
        runtime_config["dependencies"].append("conda.yaml")
        runtime_config["dependencies"].append("unparsed.xxx")
        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.RUNTIME),
            contents=yaml.safe_dump(runtime_config),
        )
        self.fs.create_file(os.path.join(workdir, "requirements.txt"), contents="")
        self.fs.create_file(os.path.join(workdir, "conda.yaml"), contents="")
        self.fs.create_file(os.path.join(workdir, "unparsed.xxx"), contents="")
        self.fs.create_file(os.path.join(workdir, "dummy.whl"), contents="")
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
                {"wheels": ["dummy.whl"]},
                {"pip": ["Pillow"]},
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
        scripts_dir = os.path.join(workdir, "files", "bin")
        wheels_dir = os.path.join(workdir, "wheels")

        ensure_dir(workdir)
        ensure_dir(scripts_dir)
        ensure_dir(wheels_dir)
        self.fs.create_file(os.path.join(scripts_dir, "prepare.sh"), contents="")
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
                        "pip_files": [
                            "requirements-sw-lock.txt",
                            "requirements-test.txt",
                        ],
                    },
                    "artifacts": {
                        "files": [
                            {
                                "_swrt_dest": "files/bin/prepare.sh",
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
        assert m_call.call_count == 4

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
            os.path.join(wheels_dir, "dummy.whl"),
        ]
        assert m_call.call_args_list[3][0][0] == [
            "/home/starwhale/myproject/export/venv/bin/pip",
            "install",
            "--exists-action",
            "w",
            "--pre",
            "starwhale",
        ]
        assert (Path(workdir) / "export/venv/bin/prepare.sh").exists()

        m_call.reset_mock()
        m_exists.return_value = True
        Runtime.restore(Path(workdir))
        assert m_call.call_count == 3

        RuntimeTermView.restore(workdir)

    @patch("starwhale.core.runtime.model.platform.machine")
    @patch("starwhale.utils.venv.tarfile.open")
    @patch("starwhale.utils.venv.check_call")
    def test_restore_conda(
        self, m_call: MagicMock, m_tar: MagicMock, m_machine: MagicMock
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

        RuntimeTermView.restore(uri)

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
        RuntimeTermView.lock(target_dir, prefix_path=conda_dir)

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

    def get_mock_manifest(self) -> t.Dict[str, t.Any]:
        return {
            "name": "rttest",
            "version": "112233",
            "base_image": "ghcr.io/star-whale/starwhale:latest-cuda11.4",
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
                        "_swrt_dest": "files/bin/prepare.sh",
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
                    "system": "Linux",
                    "use_shell_detection": True,
                },
                "mode": "venv",
                "python": "3.8",
            },
        }

    @patch("starwhale.utils.docker.check_call")
    def test_dockerize(self, m_check: MagicMock) -> None:
        self.fs.add_real_directory(_TEMPLATE_DIR)
        name = "rttest"
        version = "112233"
        image = "docker.io/t1/t2"
        uri = URI(f"{name}/version/{version}", expected_type=URIType.RUNTIME)
        manifest = self.get_mock_manifest()
        manifest["name"] = name
        manifest["version"] = version
        manifest["configs"]["docker"]["image"] = image

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
        assert f"BASE_IMAGE={manifest['base_image']}" in dockerfile_content
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

        RuntimeTermView(f"{name}/version/{version}").dockerize(
            tags=[],
            push=False,
            platforms=[SupportArch.ARM64],
            dry_run=False,
            use_starwhale_builder=False,
            reset_qemu_static=False,
        )

    @patch("shellingham.detect_shell")
    @patch("os.execl")
    def test_activate(self, m_execl: MagicMock, m_detect: MagicMock) -> None:
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
        manifest["name"] = name
        manifest["version"] = version
        ensure_dir(snapshot_dir)
        ensure_file(snapshot_dir / DEFAULT_MANIFEST_NAME, yaml.safe_dump(manifest))

        m_detect.return_value = ["zsh", "/usr/bin/zsh"]
        uri = f"{name}/version/{version}"
        StandaloneRuntime.activate(uri=uri)
        assert m_execl.call_args[0][0] == "/usr/bin/zsh"

        m_execl.reset_mock()
        runtime_config = self.get_runtime_config()
        runtime_config["mode"] = "conda"
        ensure_file(
            snapshot_dir / DefaultYAMLName.RUNTIME, yaml.safe_dump(runtime_config)
        )

        m_detect.return_value = ["bash", "/usr/bin/bash"]
        StandaloneRuntime.activate(path=str(snapshot_dir))
        assert m_execl.call_args[0][0] == "/usr/bin/bash"

        with self.assertRaises(Exception):
            RuntimeTermView.activate()

    def test_property(self) -> None:
        name = "rttest"
        version = "123"
        uri = URI(f"{name}/version/{version}", expected_type=URIType.RUNTIME)
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


class RuntimeProcessTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    @patch("starwhale.core.runtime.process.check_call")
    def test_venv_process(self, m_call: MagicMock) -> None:
        rootdir = "/home/starwhale/myproject"
        ensure_dir(rootdir)
        ensure_file(os.path.join(rootdir, "pyvenv.cfg"), "")
        process = Process(
            prefix_path=rootdir,
            target=lambda x: x,
            args=(1,),
        )
        process.run()
        assert m_call.assert_called
        assert m_call.call_args[0][0][:2] == ["bash", "-c"]

        assert m_call.call_args[0][0][2].startswith(
            f"source {rootdir}/bin/activate && {rootdir}/bin/python3"
        )
        assert "dill.load" in m_call.call_args[0][0][2]

    @patch("starwhale.core.runtime.process.check_call")
    def test_conda_process(self, m_call: MagicMock) -> None:
        rootdir = "/home/starwhale/myproject"
        ensure_dir(rootdir)
        ensure_file(os.path.join(rootdir, "conda-meta"), "")
        process = Process(
            prefix_path=rootdir,
            target=lambda x, y: print(x, y),
            args=(1, 2),
            kwargs={"a": 1},
        )
        process.run()
        assert m_call.assert_called
        assert m_call.call_args[0][0][:2] == ["bash", "-c"]

        assert m_call.call_args[0][0][2].startswith(
            f"source activate {rootdir} && {rootdir}/bin/python3"
        )
        assert "dill.load" in m_call.call_args[0][0][2]

    @patch("starwhale.core.runtime.model.StandaloneRuntime.restore")
    @patch("starwhale.core.runtime.process.check_call")
    def test_from_uri(self, m_call: MagicMock, m_restore: MagicMock) -> None:
        name = "rttest"
        version = "1234"

        sw = SWCliConfigMixed()
        snapshot_workdir = (
            sw.rootdir
            / "self"
            / "workdir"
            / "runtime"
            / name
            / version[:VERSION_PREFIX_CNT]
            / version
        )

        venv_dir = snapshot_workdir / "export" / "venv"
        ensure_dir(venv_dir)
        ensure_file(venv_dir / "pyvenv.cfg", content="")
        ensure_file(snapshot_workdir / DEFAULT_MANIFEST_NAME, content="")

        process = Process.from_runtime_uri(
            uri=f"{name}/version/{version}",
            target=lambda x: x,
            force_restore=True,
        )
        assert m_call.call_count == 0
        assert m_restore.call_args[0][0] == snapshot_workdir
        process.run()

        assert m_call.call_count == 1
        assert m_call.call_args[0][0][:2] == ["bash", "-c"]
        assert m_call.call_args[0][0][2].startswith(
            f"source {venv_dir}/bin/activate && {venv_dir}/bin/python3"
        )
        assert "dill.load" in m_call.call_args[0][0][2]

        m_call.reset_mock()
        m_restore.reset_mock()

        uri = URI(f"{name}/version/{version}", expected_type=URIType.RUNTIME)
        Process.from_runtime_uri(
            uri,
            target=lambda x: x,
        ).run()
        assert m_call.call_count == 1
        assert m_restore.call_count == 0

        uri = "http://1.1.1.1:8081/project/self/runtime/rttest/versoin/123"
        with self.assertRaises(NoSupportError):
            Process.from_runtime_uri(uri, target=lambda x: x).run()
