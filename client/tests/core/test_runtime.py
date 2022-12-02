import os
import typing as t
import tempfile
from pathlib import Path
from unittest.mock import call, patch, MagicMock

import yaml
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.utils import is_linux, load_yaml
from starwhale.consts import (
    ENV_VENV,
    ENV_CONDA,
    SupportArch,
    PythonRunEnv,
    DefaultYAMLName,
    SW_AUTO_DIRNAME,
    ENV_CONDA_PREFIX,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file
from starwhale.base.type import URIType, BundleType, DependencyType, RuntimeLockFileType
from starwhale.utils.venv import EnvTarType, get_python_version
from starwhale.utils.error import (
    FormatError,
    NotFoundError,
    NoSupportError,
    ConfigFormatError,
    ExclusiveArgsError,
    UnExpectedConfigFieldError,
)
from starwhale.utils.config import SWCliConfigMixed
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
        assert _rt_config.dependencies._pip_files[0] == RuntimeLockFileType.VENV

    @patch("starwhale.utils.venv.check_call")
    def test_quickstart_from_ishell_conda(self, m_call: MagicMock) -> None:
        workdir = "/home/starwhale/myproject"
        runtime_path = os.path.join(workdir, DefaultYAMLName.RUNTIME)
        conda_prefix_dir = os.path.join(workdir, SW_AUTO_DIRNAME, "conda")
        name = "test-conda"

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
            "conda",
            "create",
            "--yes",
            "--prefix",
            conda_prefix_dir,
            f"python={get_python_version()}",
        ]
        assert " ".join(m_call.call_args_list[1][0][0]).startswith(
            " ".join(
                [
                    "conda",
                    "run",
                    "--prefix",
                    conda_prefix_dir,
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

    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_build_venv(
        self,
        m_output: MagicMock,
        m_check_call: MagicMock,
        m_py_bin: MagicMock,
    ) -> None:
        m_output.return_value = b"3.7"
        name = "rttest"
        venv_dir = "/home/starwhale/venv"
        ensure_dir(venv_dir)
        self.fs.create_file(os.path.join(venv_dir, "pyvenv.cfg"))
        venv_path = os.path.join(venv_dir, "bin/python3")
        os.environ[ENV_VENV] = venv_dir
        m_py_bin.return_value = venv_path
        build_version = ""

        sw = SWCliConfigMixed()
        workdir = "/home/starwhale/myproject"
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
        sr.build(workdir=Path(workdir), enable_lock=True, env_prefix_path=venv_dir)
        assert sr.uri.object.version != ""
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
        assert _deps["pip_files"] == ["requirements.txt", "requirements-sw-lock.txt"]
        assert _deps["pip_pkgs"] == ["Pillow"]
        _raw_deps = _deps["raw_deps"]
        assert len(_raw_deps) == 7
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
            {"deps": "requirements-sw-lock.txt", "kind": "pip_req_file"},
        ]

        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        ensure_dir(sr.store.bundle_dir / f"xx{sr.store.bundle_type}")
        info = sr.info()
        assert info["project"] == "self"
        assert "version" not in info
        assert len(info["history"]) == 1
        assert info["history"][0]["version"] == build_version

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
        runtime_term_view.build(workdir, env_use_shell=True)

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

        rtv = RuntimeTermView(f"{name}/version/{build_version}")
        ok, _ = rtv.remove(True)
        assert ok
        assert not os.path.exists(recover_path)
        assert not os.path.exists(swrt_path)
        assert not os.path.exists(recover_snapshot_path)
        assert not os.path.exists(swrt_snapshot_path)

        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        sr.build(
            workdir=Path(workdir),
            enable_lock=True,
            env_prefix_path=venv_dir,
            gen_all_bundles=True,
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
    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.utils.venv.is_venv")
    @patch("starwhale.utils.venv.is_conda")
    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_build_conda(
        self,
        m_call_output: MagicMock,
        m_check_call: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
    ) -> None:
        conda_prefix = "/home/starwhale/anaconda3/envs/starwhale"
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))
        m_py_bin.return_value = os.path.join(conda_prefix, "bin/python3")
        m_conda.return_value = True
        m_venv.return_value = False
        m_call_output.side_effect = [b"3.7.13", conda_prefix.encode(), b"False"]

        os.environ[ENV_CONDA] = "1"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix

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
        sr.build(workdir=Path(workdir), env_use_shell=True)
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
        _deps = _manifest["dependencies"]
        assert _deps["conda_files"] == ["conda.yaml", "conda-sw-lock.yaml"]
        assert len(_deps["conda_pkgs"]) == 0
        assert _deps["pip_files"] == ["requirements.txt"]
        assert _deps["pip_pkgs"] == ["Pillow"]
        assert _deps["raw_deps"] == [
            {"deps": "requirements.txt", "kind": "pip_req_file"},
            {"deps": ["dummy.whl"], "kind": "wheel"},
            {"deps": ["Pillow"], "kind": "pip_pkg"},
            {"deps": "conda.yaml", "kind": "conda_env_file"},
            {"deps": "conda-sw-lock.yaml", "kind": "conda_env_file"},
        ]

    @patch("os.environ", {})
    @patch("starwhale.core.runtime.model.get_python_version")
    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.utils.venv.subprocess.check_output")
    @patch("starwhale.utils.venv.check_call")
    def test_build_without_python_version(
        self,
        m_check_call: MagicMock,
        m_call_output: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
        m_py_ver: MagicMock,
    ) -> None:
        conda_prefix = "/home/starwhale/anaconda3/envs/starwhale"
        os.environ[ENV_CONDA_PREFIX] = conda_prefix
        ensure_dir(conda_prefix)
        ensure_dir(os.path.join(conda_prefix, "conda-meta"))

        m_py_bin.return_value = os.path.join(conda_prefix, "bin/python3")
        m_venv.return_value = False
        m_conda.return_value = True
        m_call_output.side_effect = [b"3.7.13", conda_prefix.encode(), b"False"]
        m_py_ver.return_value = "fake.ver"

        name = "demo_runtime"
        workdir = "/home/starwhale/myproject"
        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.RUNTIME),
            contents=yaml.safe_dump({"name": name, "mode": "conda"}),
        )

        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        with self.assertRaises(ConfigFormatError):
            sr.build(workdir=Path(workdir), env_use_shell=True)

        assert m_check_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            "/home/starwhale/anaconda3/envs/starwhale",
            "--file",
        ]

        m_check_call.reset_mock()
        m_call_output.reset_mock()
        m_call_output.side_effect = [b"3.7.13", conda_prefix.encode(), b"False"]

        m_py_ver.assert_called_once()

        m_py_ver.return_value = "3.10"
        sr.build(workdir=Path(workdir), env_use_shell=True)
        m_py_ver.assert_has_calls([call(), call()])

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

    @patch("os.environ", {})
    @patch("starwhale.utils.venv.get_user_runtime_python_bin")
    @patch("starwhale.core.runtime.model.is_venv")
    @patch("starwhale.core.runtime.model.is_conda")
    @patch("starwhale.utils.venv.subprocess.check_output")
    @patch("starwhale.utils.venv.check_call")
    def test_build_with_docker_image_specified(
        self,
        m_check_call: MagicMock,
        m_call_output: MagicMock,
        m_conda: MagicMock,
        m_venv: MagicMock,
        m_py_bin: MagicMock,
    ) -> None:
        conda_dir = "/home/starwhale/anaconda3/envs/starwhale"
        ensure_dir(conda_dir)
        ensure_dir(os.path.join(conda_dir, "conda-meta"))
        os.environ[ENV_CONDA_PREFIX] = conda_dir
        m_py_bin.return_value = "/home/starwhale/anaconda3/envs/starwhale/bin/python3"
        m_venv.return_value = False
        m_conda.return_value = True
        m_call_output.return_value = b"3.7.13"

        docker_image = "foo.com/bar:latest"
        name = "demo_runtime"
        workdir = "/home/starwhale/myproject"

        yaml_content = {
            "name": name,
            "mode": "conda",
            "environment": {},
        }
        yaml_file = os.path.join(workdir, DefaultYAMLName.RUNTIME)
        self.fs.create_file(yaml_file, contents=yaml.safe_dump(yaml_content))

        uri = URI(name, expected_type=URIType.RUNTIME)
        sr = StandaloneRuntime(uri)
        sr.build(workdir=Path(workdir), env_use_shell=True)

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
        assert _manifest["base_image"] != docker_image

        yaml_content["environment"] = {"docker": {"image": docker_image}}

        self.fs.remove_object(yaml_file)
        self.fs.create_file(yaml_file, contents=yaml.safe_dump(yaml_content))
        sr = StandaloneRuntime(URI(name, expected_type=URIType.RUNTIME))
        sr.build(workdir=Path(workdir), env_use_shell=True)
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
            ["--pre", "starwhale"],
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
            "--pre",
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
        Runtime.restore(Path(workdir))

        assert m_call.call_count == 8
        conda_cmds = [cm[0][0] for cm in m_call.call_args_list]
        conda_prefix_dir = os.path.join(export_dir, "conda")
        assert conda_cmds == [
            [
                "conda",
                "create",
                "--yes",
                "--prefix",
                conda_prefix_dir,
                "python=3.7",
            ],
            [
                "conda",
                "run",
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
                "--prefix",
                conda_prefix_dir,
                "--channel",
                "conda-forge",
                "--yes",
                "--override-channels",
                "'c' 'd'",
            ],
            [
                "conda",
                "env",
                "update",
                "--file",
                conda_env_fpath,
                "--prefix",
                conda_prefix_dir,
            ],
            [
                f"{conda_prefix_dir}/bin/python3",
                "-c",
                "import pkg_resources; pkg_resources.get_distribution('starwhale')",
            ],
        ]

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
        runtime_fname = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        lock_fname = os.path.join(target_dir, RuntimeLockFileType.VENV)
        self.fs.create_file(os.path.join(target_dir, "dummy.whl"))
        self.fs.create_file(
            runtime_fname,
            contents=yaml.safe_dump(
                {
                    "name": "test",
                    "mode": "venv",
                    "dependencies": [{"pip": ["a", "b"]}, {"wheels": ["dummy.whl"]}],
                }
            ),
        )

        venv_dir = "/tmp/venv"
        ensure_dir(venv_dir)
        self.fs.create_file(os.path.join(venv_dir, "pyvenv.cfg"))

        os.environ[ENV_VENV] = venv_dir
        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.VENV not in content.get("dependencies", [])
        StandaloneRuntime.lock(target_dir, env_use_shell=True)

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

        assert os.path.exists(lock_fname)
        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.VENV == content["dependencies"][-1]
        del os.environ[ENV_VENV]

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
                    f"{tempfile.gettempdir()}/starwhale-lock-",
                ]
            )
        )
        assert os.path.exists(lock_fname)

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
        StandaloneRuntime.lock(target_dir)

        assert m_call.call_count == 5
        assert m_call.call_args_list[3][0][0] == [
            f"{sw_venv_dir}/bin/pip",
            "install",
            "--exists-action",
            "w",
            "-r",
            f"{target_dir}/requirements-sw-lock.txt",
        ]
        assert m_output.call_count == 2

        assert os.path.exists(sw_venv_dir)
        assert os.path.exists(venv_cfg)

    def test_abnormal_lock(self) -> None:
        with self.assertRaises(NotFoundError):
            RuntimeTermView.lock("not-found")

        target_dir = "/home/starwhale/workdir"
        ensure_dir(target_dir)
        runtime_fname = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        self.fs.create_file(
            runtime_fname, contents=yaml.safe_dump({"name": "test", "mode": "no-mode"})
        )

        with self.assertRaises(ExclusiveArgsError):
            RuntimeTermView.lock(
                target_dir, env_name="test", env_prefix_path="1", env_use_shell=True
            )

        with self.assertRaises(ExclusiveArgsError):
            RuntimeTermView.lock(target_dir, env_name="test", env_prefix_path="1")

        with self.assertRaises(NoSupportError):
            RuntimeTermView.lock(target_dir, env_prefix_path="test")

        os.unlink(runtime_fname)
        self.fs.create_file(
            runtime_fname, contents=yaml.safe_dump({"name": "test", "mode": "venv"})
        )

        with self.assertRaises(NoSupportError):
            RuntimeTermView.lock(target_dir, env_name="test")

        with self.assertRaises(FormatError):
            RuntimeTermView.lock(target_dir, env_prefix_path="1")

        os.unlink(runtime_fname)
        self.fs.create_file(
            runtime_fname, contents=yaml.safe_dump({"name": "test", "mode": "conda"})
        )

        with self.assertRaises(FormatError):
            RuntimeTermView.lock(target_dir, env_prefix_path="1")

    @patch("starwhale.utils.venv.check_call")
    @patch("starwhale.utils.venv.subprocess.check_output")
    def test_lock_conda(self, m_output: MagicMock, m_call: MagicMock) -> None:
        target_dir = "/home/starwhale/workdir"
        ensure_dir(target_dir)
        runtime_fname = os.path.join(target_dir, DefaultYAMLName.RUNTIME)
        self.fs.create_file(
            runtime_fname,
            contents=yaml.safe_dump(
                {
                    "name": "test",
                    "mode": "conda",
                    "dependencies": [{"pip": ["a"]}, {"conda": ["b"]}],
                }
            ),
        )

        conda_dir = "/tmp/conda"
        ensure_dir(conda_dir)
        ensure_dir(os.path.join(conda_dir, "conda-meta"))
        m_output.return_value = conda_dir.encode()

        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.CONDA not in content.get("dependencies", {})
        RuntimeTermView.lock(target_dir, env_prefix_path=conda_dir)

        assert m_call.call_args_list[0][0][0] == [
            "conda",
            "run",
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
            "install",
            "--prefix",
            conda_dir,
            "--channel",
            "conda-forge",
            "--yes",
            "--override-channels",
            "'b'",
        ]

        assert m_call.call_args_list[2][0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            conda_dir,
            "--file",
        ]

        assert m_output.call_count == 1
        assert m_output.call_args_list[0][0][0] == [
            f"{conda_dir}/bin/python3",
            "-c",
            "import sys; _v=sys.version_info;print(f'{_v.major}.{_v.minor}.{_v.micro}')",
        ]

        assert os.path.exists(os.path.join(target_dir, RuntimeLockFileType.CONDA))
        content = load_yaml(runtime_fname)
        assert RuntimeLockFileType.CONDA == content["dependencies"][-1]

        StandaloneRuntime.lock(target_dir, env_name="conda-env-name")

        assert m_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            conda_dir,
            "--file",
        ]

        os.environ[ENV_CONDA_PREFIX] = conda_dir
        StandaloneRuntime.lock(target_dir, env_use_shell=True)
        assert m_call.call_args[0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            conda_dir,
            "--file",
        ]

        del os.environ[ENV_CONDA_PREFIX]
        m_output.reset_mock()
        m_call.reset_mock()

        sw_conda_dir = os.path.join(target_dir, SW_AUTO_DIRNAME, "conda")
        ensure_dir(sw_conda_dir)
        ensure_dir(os.path.join(sw_conda_dir, "conda-meta"))
        StandaloneRuntime.lock(target_dir)

        assert m_call.call_count == 4
        assert m_call.call_args_list[0][0][0] == [
            "conda",
            "run",
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
            "install",
            "--prefix",
            sw_conda_dir,
            "--channel",
            "conda-forge",
            "--yes",
            "--override-channels",
            "'b'",
        ]
        assert m_call.call_args_list[2][0][0] == [
            "conda",
            "env",
            "update",
            "--file",
            f"{target_dir}/conda-sw-lock.yaml",
            "--prefix",
            sw_conda_dir,
        ]

        assert m_call.call_args_list[3][0][0][:6] == [
            "conda",
            "env",
            "export",
            "--prefix",
            sw_conda_dir,
            "--file",
        ]
        assert m_output.call_count == 1

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
            tags=("t1", "t2", "t3"),  # type: ignore
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
