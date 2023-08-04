import os
import json
import typing as t
import pathlib
import tempfile
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
from click.testing import CliRunner
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.utils import config as sw_config
from starwhale.utils import load_yaml, gen_uniq_version
from starwhale.consts import (
    FileFlag,
    HTTPMethod,
    DefaultYAMLName,
    SW_AUTO_DIRNAME,
    VERSION_PREFIX_CNT,
    RESOURCE_FILES_NAME,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_JOBS_FILE_NAME,
    EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME,
    EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME,
)
from starwhale.utils.fs import empty_dir, ensure_dir, ensure_file, blake2b_file
from starwhale.base.type import BundleType
from starwhale.api.service import Service
from starwhale.utils.error import ExistedError, NotFoundError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.api._impl.job import Handler
from starwhale.core.job.store import JobStorage
from starwhale.core.model.cli import _list as list_cli
from starwhale.core.model.cli import _serve as serve_cli
from starwhale.core.model.cli import _prepare_model_run_args
from starwhale.core.model.view import ModelTermView, ModelTermViewJson
from starwhale.base.uri.project import Project
from starwhale.core.model.model import (
    CloudModel,
    ModelConfig,
    ModelInfoFilter,
    StandaloneModel,
    resource_to_file_node,
)
from starwhale.core.model.store import ModelStorage
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.instance.view import InstanceTermView
from starwhale.base.scheduler.step import Step
from starwhale.core.runtime.process import Process

_model_data_dir = f"{ROOT_DIR}/data/model"
_model_yaml = open(f"{_model_data_dir}/model.yaml").read()


class StandaloneModelTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

        self.sw = SWCliConfigMixed()

        self.workdir = "/home/starwhale/myproject"
        self.name = "mnist"

        self.fs.create_file(
            os.path.join(self.workdir, DefaultYAMLName.MODEL), contents=_model_yaml
        )

        ensure_dir(os.path.join(self.workdir, "models"))
        ensure_dir(os.path.join(self.workdir, "config"))
        ensure_file(os.path.join(self.workdir, "models", "mnist_cnn.pt"), " ")
        ensure_file(os.path.join(self.workdir, "config", "hyperparam.json"), " ")
        os.environ[Process.ActivatedRuntimeURI] = ""

    @patch("starwhale.base.blob.store.LocalFileStore.copy_dir")
    @patch("starwhale.api._impl.job.Handler._preload_registering_handlers")
    @patch("starwhale.core.model.model.file_stat")
    @patch("starwhale.core.model.model.StandaloneModel._get_service")
    @patch("starwhale.core.model.model.Walker.files")
    @patch("starwhale.core.model.model.blake2b_file")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch(
        "starwhale.base.uri.resource.Resource._refine_remote_rc_info",
        MagicMock(),
    )
    def test_build_workflow(
        self,
        m_blake_file: MagicMock,
        m_walker_files: MagicMock,
        m_get_service: MagicMock,
        m_stat: MagicMock,
        m_preload: MagicMock,
        m_copy_dir: MagicMock,
    ) -> None:
        Handler._registered_handlers["base"] = Handler(
            name="base",
            show_name="t1",
            func_name="test_func1",
            module_name="mock",
        )
        Handler._registered_handlers["depend"] = Handler(
            name="depend",
            show_name="t2",
            func_name="test_func2",
            module_name="mock",
            needs=["base"],
        )
        m_stat.return_value.st_size = 1
        m_blake_file.return_value = "123456"
        m_walker_files.return_value = []
        m_copy_dir.return_value = 0

        svc = MagicMock(spec=Service)
        svc.get_spec.return_value = {"foo": "bar"}
        svc.example_resources = []
        m_get_service.return_value = svc

        model_config = ModelConfig.create_by_yaml(
            Path(self.workdir) / DefaultYAMLName.MODEL
        )

        model_uri = Resource(
            self.name,
            typ=ResourceType.model,
        )
        sm = StandaloneModel(model_uri)
        sm.build(
            workdir=Path(self.workdir),
            model_config=model_config,
            tags=["test01", "test02"],
        )

        build_version = sm.uri.version

        bundle_path = (
            self.sw.rootdir
            / "self"
            / ResourceType.model.value
            / self.name
            / build_version[:VERSION_PREFIX_CNT]
            / f"{build_version}{BundleType.MODEL}"
        )
        assert m_preload.call_count == 1

        assert bundle_path.exists()
        assert (bundle_path / "src").exists()
        job_yaml_path = bundle_path / "src" / SW_AUTO_DIRNAME / DEFAULT_JOBS_FILE_NAME

        assert job_yaml_path.exists()
        job_contents = load_yaml(job_yaml_path)
        assert job_contents == {
            "base": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "test_func1",
                    "module_name": "mock",
                    "name": "base",
                    "needs": [],
                    "replicas": 1,
                    "resources": [],
                    "show_name": "t1",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": False,
                }
            ],
            "depend": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "test_func1",
                    "module_name": "mock",
                    "name": "base",
                    "needs": [],
                    "replicas": 1,
                    "resources": [],
                    "show_name": "t1",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": False,
                },
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "extra_args": [],
                    "extra_kwargs": {},
                    "func_name": "test_func2",
                    "module_name": "mock",
                    "name": "depend",
                    "needs": ["base"],
                    "replicas": 1,
                    "resources": [],
                    "show_name": "t2",
                    "expose": 0,
                    "virtual": False,
                    "require_dataset": False,
                },
            ],
            "serving": [
                {
                    "cls_name": "",
                    "concurrency": 1,
                    "expose": 8080,
                    "extra_args": [],
                    "extra_kwargs": {
                        "search_modules": ["mnist.evaluator:MNISTInference"]
                    },
                    "func_name": "StandaloneModel._serve_handler",
                    "module_name": "starwhale.core.model.model",
                    "name": "serving",
                    "needs": [],
                    "replicas": 1,
                    "resources": [],
                    "show_name": "virtual handler for model serving",
                    "virtual": True,
                    "require_dataset": False,
                }
            ],
        }

        _manifest = load_yaml(bundle_path / DEFAULT_MANIFEST_NAME)
        assert "name" not in _manifest
        assert _manifest["version"] == build_version

        _resource_files_path = (
            bundle_path / "src" / SW_AUTO_DIRNAME / RESOURCE_FILES_NAME
        )
        assert _resource_files_path.exists()
        _resource_files = load_yaml(_resource_files_path)
        assert _resource_files == []

        assert m_copy_dir.call_count == 1
        assert (
            str(m_copy_dir.call_args_list[0][1]["src_dir"])
            == "/home/starwhale/myproject"
        )
        assert str(m_copy_dir.call_args_list[0][1]["dst_dir"]).endswith("/src")

        assert bundle_path.exists()
        tags = sm.tag.list()
        assert set(tags) == {"latest", "v0", "test01", "test02"}

        model_uri = Resource(
            f"mnist/version/{build_version}",
            typ=ResourceType.model,
        )
        sm = StandaloneModel(model_uri)
        _info = sm.info()

        assert _info["basic"]["version"] == build_version
        assert _info["basic"]["name"] == self.name
        assert "size" in _info["basic"]

        _list, _ = StandaloneModel.list(Project(""))
        assert len(_list) == 1
        assert not _list[self.name][0]["is_removed"]

        model_uri = Resource(
            f"{self.name}/version/{build_version}",
            typ=ResourceType.model,
        )
        sd = StandaloneModel(model_uri)
        _ok, _ = sd.remove(False)
        assert _ok

        _list, _ = StandaloneModel.list(Project(""))
        assert _list[self.name][0]["is_removed"]

        _ok, _ = sd.recover(True)
        _list, _ = StandaloneModel.list(Project(""))
        assert not _list[self.name][0]["is_removed"]

        fname = f"{self.name}/version/{build_version}"
        for f in ModelInfoFilter:
            ModelTermView(fname).info(f)
            ModelTermViewJson(fname).info(f)

        ModelTermView(fname).diff(
            Resource(
                fname,
                ResourceType.model,
            ),
            show_details=False,
        )
        ModelTermView(fname).history()
        ModelTermView(fname).remove()
        ModelTermView(fname).recover()
        ModelTermView.list(show_removed=True)
        ModelTermView.list()

        _ok, _ = sd.remove(True)
        assert _ok
        _list, _ = StandaloneModel.list(Project(""))
        assert len(_list[self.name]) == 0

        ModelTermView.build(
            workdir=self.workdir,
            project="self",
            model_config=model_config,
            add_all=False,
        )

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.core.model.model.ModelConfig.do_validate")
    @patch("starwhale.core.model.model.StandaloneModel._make_meta_tar")
    @patch("starwhale.core.model.model.StandaloneModel._gen_model_serving")
    @patch("starwhale.core.model.model.generate_jobs_yaml")
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.core.runtime.process.Process.run", autospec=True)
    @patch("starwhale.core.runtime.process.guess_python_env_mode")
    @patch("starwhale.core.runtime.process.StandaloneRuntime.restore")
    @patch("starwhale.core.runtime.process.extract_tar")
    def test_build_with_package_runtime(
        self,
        m_extract: MagicMock,
        m_restore: MagicMock,
        m_env_mode: MagicMock,
        m_process_run: MagicMock,
        m_conf: MagicMock,
        *args: t.Any,
    ) -> None:
        m_conf.return_value = {
            "instances": {
                "foo": {"uri": "http://localhost", "sw_token": "token"},
                "local": {"uri": "local", "current_project": "self"},
            },
            "current_instance": "local",
            "storage": {"root": self.sw.rootdir},
        }
        workdir = Path("/home/test/workdir")
        ensure_dir(workdir)
        model_name = "test"
        model_config = ModelConfig(name=model_name, run={"modules": ["x.y.z"]})

        pytorch_runtime_uri = "pytorch/version/1234"

        runtime_snapshot = self.sw.rootdir / "self/workdir/runtime/pytorch/12/1234"
        ensure_file(
            runtime_snapshot / "_manifest.yaml",
            yaml.safe_dump({"name": "pytorch"}),
            parents=True,
        )
        swrt_path = self.sw.rootdir / "self/runtime/pytorch/12/1234.swrt"
        ensure_file(swrt_path, content="1", parents=True)

        swrt_hash = blake2b_file(swrt_path)

        venv_prefix = runtime_snapshot / "export/venv"
        m_env_mode.return_value = "venv"
        ensure_dir(venv_prefix)

        def _run_with_package_runtime(obj: t.Any) -> None:
            os.environ[Process.ActivatedRuntimeURI] = pytorch_runtime_uri
            ModelTermView.build(
                workdir=workdir,
                project="self",
                model_config=model_config,
                package_runtime=True,
                add_all=False,
            )

        def _run_without_package_runtime(obj: t.Any) -> None:
            os.environ[Process.ActivatedRuntimeURI] = pytorch_runtime_uri
            ModelTermView.build(
                workdir=workdir,
                project="self",
                model_config=model_config,
                package_runtime=False,
                add_all=False,
            )

        m_process_run.side_effect = _run_with_package_runtime
        version = os.environ[
            "SW_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST"
        ] = gen_uniq_version()
        packaged_uri = Resource(
            f"{model_name}/version/{version}",
            typ=ResourceType.model,
        )
        ModelTermView.build(
            workdir=workdir,
            project="self",
            model_config=model_config,
            runtime_uri="pytorch/version/1234",
            package_runtime=True,
            add_all=False,
            tags=["packaged-01"],
        )

        assert m_extract.called
        assert m_restore.called
        assert m_restore.call_args[0][0] == runtime_snapshot

        built_model_store = ModelStorage(packaged_uri)
        assert built_model_store.bundle_path.exists()
        assert built_model_store.manifest["packaged_runtime"] == {
            "hash": swrt_hash,
            "manifest": {"name": "pytorch"},
            "name": "pytorch",
            "path": "src/.starwhale/runtime/packaged.swrt",
        }
        runtime_bundle_path = built_model_store.packaged_runtime_bundle_path
        assert runtime_bundle_path.exists()
        assert runtime_bundle_path.read_text() == "1"

        m_process_run.reset_mock()
        m_process_run.side_effect = _run_without_package_runtime
        version = os.environ[
            "SW_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST"
        ] = gen_uniq_version()
        no_packaged_uri = Resource(
            f"{model_name}/version/{version}",
            typ=ResourceType.model,
        )
        ModelTermView.build(
            workdir=workdir,
            project="self",
            model_config=model_config,
            runtime_uri=pytorch_runtime_uri,
            package_runtime=False,
            add_all=False,
            tags=["packaged-01"],
        )

        built_model_store = ModelStorage(no_packaged_uri)
        assert built_model_store.bundle_path.exists()
        assert built_model_store.manifest.get("packaged_runtime") is None
        assert not built_model_store.packaged_runtime_bundle_path.exists()

        m_restore.reset_mock()

        m_env_mode.return_value = "conda"

        def _restore(workdir: Path, verbose: bool = False) -> None:
            ensure_dir(workdir / "export/conda")

        m_restore.side_effect = _restore

        m_process_run.reset_mock()
        m_process_run.side_effect = _run_with_package_runtime
        os.environ[Process.ActivatedRuntimeURI] = str(packaged_uri)
        version = os.environ[
            "SW_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST"
        ] = gen_uniq_version()
        use_model_uri = Resource(
            f"{model_name}/version/{version}",
            typ=ResourceType.model,
        )

        ModelTermView.build(
            workdir=workdir,
            project="self",
            model_config=model_config,
            runtime_uri=pytorch_runtime_uri,
            package_runtime=True,
            add_all=False,
            tags=["packaged-01"],
        )

        built_model_store = ModelStorage(use_model_uri)
        assert built_model_store.bundle_path.exists()

        assert built_model_store.manifest["packaged_runtime"] == {
            "hash": swrt_hash,
            "manifest": {"name": "pytorch"},
            "name": "pytorch",
            "path": "src/.starwhale/runtime/packaged.swrt",
        }
        runtime_bundle_path = built_model_store.packaged_runtime_bundle_path
        assert runtime_bundle_path.exists()
        assert runtime_bundle_path.read_text() == "1"

    def test_get_file_desc(self):
        _file = Path("tmp/file.txt")
        ensure_dir("tmp")
        ensure_file(_file, "123456")
        fd = resource_to_file_node(
            [{"path": "file.txt", "desc": "SRC"}], parent_path=Path("tmp")
        )
        assert "file.txt" in fd
        assert fd.get("file.txt").size == 6

    def test_diff(
        self,
    ) -> None:
        base_version = "base_mdklqrbwpyrb2s3eme63k5xqno4o4wwmd3n"
        compare_version = "compare_mdklqrbwpyrb2s3eme63k5xqno4o4ww"

        compare_resource_content = """
- duplicate_check: true
  name: empty.pt
  path: src/model/empty.pt
  size: 10
  signature: update_786a02f742015903c6c6fd852552d272912f4740e15847618a86e217f71f5419d25e1031afee585313896444934eb04b903a685b1448b755d56f701afe9be2ce
  desc: MODEL
- duplicate_check: false
  name: svc2.yaml
  path: src/svc2.yaml
  size: 10
  signature: add_035bda269db519c0bb15018359b4baf73c447696af19a05af322ab2a051084e71143e662845edaf8c591a382f18da989c6b69e8271f33e58f617a9fc67911370
  desc: SRC
- duplicate_check: false
  name: runtime.yaml
  path: src/runtime.yaml
  size: 10
  signature: 1dfcf6fbbc0044276e89f1a3e7cde63ac0e96a037e4f0303c514887f6d9c4590af1ee377ab5acbca2535a399f9425d84425d86588d7b4f474b85226ff16789b5
  desc: SRC
- duplicate_check: false
  name: model.yaml
  path: src/model.yaml
  size: 10
  signature: b0660bfbb4f0d8ac1e4e75ce8d351bebf2e80f5c79816ed2e3b28e4309b57433d6cb027414a45f27a318116a10dec7f93cdb1d4ea99ec3ebb76a7178a4044b26
  desc: SRC
        """

        base_resource_content = """
- duplicate_check: true
  name: empty.pt
  path: src/model/empty.pt
  size: 10
  signature: 786a02f742015903c6c6fd852552d272912f4740e15847618a86e217f71f5419d25e1031afee585313896444934eb04b903a685b1448b755d56f701afe9be2ce
  desc: MODEL
- duplicate_check: false
  name: svc.yaml
  path: src/svc.yaml
  size: 10
  signature: 035bda269db519c0bb15018359b4baf73c447696af19a05af322ab2a051084e71143e662845edaf8c591a382f18da989c6b69e8271f33e58f617a9fc67911370
  desc: SRC
- duplicate_check: false
  name: runtime.yaml
  path: src/runtime.yaml
  size: 10
  signature: 1dfcf6fbbc0044276e89f1a3e7cde63ac0e96a037e4f0303c514887f6d9c4590af1ee377ab5acbca2535a399f9425d84425d86588d7b4f474b85226ff16789b5
  desc: SRC
- duplicate_check: false
  name: model.yaml
  path: src/model.yaml
  size: 10
  signature: b0660bfbb4f0d8ac1e4e75ce8d351bebf2e80f5c79816ed2e3b28e4309b57433d6cb027414a45f27a318116a10dec7f93cdb1d4ea99ec3ebb76a7178a4044b26
  desc: SRC
        """

        base_bundle_path = (
            self.sw.rootdir
            / "self"
            / ResourceType.model.value
            / self.name
            / base_version[:VERSION_PREFIX_CNT]
            / f"{base_version}{BundleType.MODEL}"
        )
        ensure_file(
            base_bundle_path / "src" / SW_AUTO_DIRNAME / RESOURCE_FILES_NAME,
            base_resource_content,
            parents=True,
        )

        compare_bundle_path = (
            self.sw.rootdir
            / "self"
            / ResourceType.model.value
            / self.name
            / compare_version[:VERSION_PREFIX_CNT]
            / f"{compare_version}{BundleType.MODEL}"
        )
        ensure_file(
            compare_bundle_path / "src" / SW_AUTO_DIRNAME / RESOURCE_FILES_NAME,
            compare_resource_content,
            parents=True,
        )

        base_model_uri = Resource(
            f"{self.name}/version/{base_version}",
            typ=ResourceType.model,
        )
        sm = StandaloneModel(base_model_uri)

        diff_info = sm.diff(
            Resource(
                f"{self.name}/version/{compare_version}",
                typ=ResourceType.model,
            )
        )
        assert len(diff_info) == 3
        # 4 files and 1 added
        assert len(diff_info["all_paths"]) == 5
        assert len(diff_info["base_version"]) == 4
        assert len(diff_info["compare_version"]) == 5
        assert (
            diff_info["compare_version"]["src/model/empty.pt"].flag == FileFlag.UPDATED
        )
        assert diff_info["compare_version"]["src/svc.yaml"].flag == FileFlag.DELETED
        assert diff_info["compare_version"]["src/svc2.yaml"].flag == FileFlag.ADDED
        assert (
            diff_info["compare_version"]["src/runtime.yaml"].flag == FileFlag.UNCHANGED
        )
        assert diff_info["compare_version"]["src/model.yaml"].flag == FileFlag.UNCHANGED

    def test_extract(self) -> None:
        target = Path("/home/workdir/target_no_exist")

        with self.assertRaises(NotFoundError):
            ModelTermView("not-found/version/dummy").extract(force=False, target=target)

        bundle_path = (
            self.sw.rootdir
            / "self"
            / ResourceType.model.value
            / self.name
            / "12"
            / "1234.swmp"
        )
        ensure_dir(bundle_path)
        uri = Resource("mnist/version/1234", typ=ResourceType.model, refine=True)
        with self.assertRaises(NotFoundError):
            ModelTermView(uri).extract(force=False, target=target)

        ensure_file(bundle_path / "src" / "model.yaml", "test", parents=True)
        ensure_file(bundle_path / "src" / "inner" / "svc.yaml", "test", parents=True)
        ModelTermView(uri).extract(force=False, target=target)

        assert (target / "model.yaml").read_text() == "test"

        with self.assertRaises(ExistedError):
            ModelTermView(uri).extract(force=False, target=target)

        ModelTermView(uri).extract(force=True, target=target)
        assert (target / "model.yaml").read_text() == "test"

    @patch("starwhale.base.scheduler.Step.get_steps_from_yaml")
    @patch("starwhale.core.model.model.generate_jobs_yaml")
    @patch("starwhale.base.scheduler.Scheduler._schedule_one_task")
    @patch("starwhale.base.scheduler.Scheduler._schedule_one_step")
    @patch("starwhale.base.scheduler.Scheduler._schedule_all")
    def test_run(
        self,
        schedule_all_mock: MagicMock,
        single_step_mock: MagicMock,
        single_task_mock: MagicMock,
        gen_yaml_mock: MagicMock,
        gen_job_mock: MagicMock,
    ):
        gen_job_mock.return_value = [
            Step(
                name="ppl",
                cls_name="",
                resources=[{"type": "cpu", "limit": 1, "request": 1}],
                concurrency=1,
                task_num=1,
                needs=[],
            ),
            Step(
                name="cmp",
                cls_name="",
                resources=[{"type": "cpu", "limit": 1, "request": 1}],
                concurrency=1,
                task_num=1,
                needs=["ppl"],
            ),
        ]
        model_config = ModelConfig(name="test", run={"handlers": ["mock-module"]})
        project = "test"
        version = "qwertyuiop"
        StandaloneModel.run(
            model_src_dir=Path(self.workdir),
            model_config=model_config,
            project=project,
            version=version,
            dataset_uris=["mnist/version/latest"],
            scheduler_run_args={
                "step_name": "ppl",
                "task_index": 0,
            },
            force_generate_jobs_yaml=True,
        )
        schedule_all_mock.assert_not_called()
        single_step_mock.assert_not_called()
        single_task_mock.assert_called_once()

        job_dir = JobStorage.local_run_dir(project, version)
        job_manifest = load_yaml(job_dir / "_manifest.yaml")
        model_src_dir = job_manifest["model_src_dir"]
        assert model_src_dir != str(Path(self.workdir).resolve())
        assert model_src_dir == str(job_dir / "snapshot")
        assert not os.path.exists(model_src_dir)
        assert Path.cwd() == Path(model_src_dir)

        version = "zxcvbnm"
        StandaloneModel.run(
            model_src_dir=Path(self.workdir),
            model_config=model_config,
            project=project,
            version=version,
            dataset_uris=["mnist/version/latest"],
            scheduler_run_args={
                "step_name": "ppl",
                "task_index": -1,
            },
            forbid_snapshot=True,
        )
        single_step_mock.assert_called_once()
        job_dir = JobStorage.local_run_dir(project, version)
        job_manifest = load_yaml(job_dir / "_manifest.yaml")
        model_src_dir = job_manifest["model_src_dir"]
        assert model_src_dir == str(Path(self.workdir).resolve())
        assert model_src_dir != str(job_dir / "snapshot")
        assert os.path.exists(model_src_dir)
        assert Path.cwd() == Path(model_src_dir)

        version = "asdfghjkl"
        StandaloneModel.run(
            model_src_dir=Path(self.workdir),
            model_config=model_config,
            project=project,
            version=version,
            dataset_uris=["mnist/version/latest"],
            cleanup_snapshot=False,
        )
        schedule_all_mock.assert_called_once()
        job_dir = JobStorage.local_run_dir(project, version)
        job_manifest = load_yaml(job_dir / "_manifest.yaml")
        model_src_dir = job_manifest["model_src_dir"]
        assert model_src_dir != str(Path(self.workdir).resolve())
        assert model_src_dir == str(job_dir / "snapshot")
        assert os.path.exists(model_src_dir)
        assert Path.cwd() == Path(model_src_dir)

    @Mocker()
    @patch("starwhale.core.model.model.CloudModel.list")
    def test_list_with_project(self, req: Mocker, mock_list: MagicMock):
        base_url = "http://1.1.0.0:8182/api/v1"

        req.request(
            HTTPMethod.POST,
            f"{base_url}/login",
            json={"data": {"name": "foo", "role": {"roleName": "admin"}}},
            headers={"Authorization": "token"},
        )
        InstanceTermView().login(
            "http://1.1.0.0:8182",
            alias="remote",
            username="foo",
            password="bar",
        )
        instances = InstanceTermView().list()
        assert len(instances) == 2  # local and remote

        # default current project is local/self
        models, _ = ModelTermView.list()
        assert len(models) == 0

        mock_models = (
            {
                "proj": [
                    {"version": "foo", "is_removed": False, "created_at": 1},
                    {"version": "bar", "is_removed": False, "created_at": 2},
                ]
            },
            None,
        )
        mock_list.return_value = mock_models
        # list supports using instance/project uri which is not current_instance/current_project
        models, _ = ModelTermView.list("remote/whatever")
        assert len(models) == 2  # project foo and bar

    @patch("starwhale.utils.process.check_call")
    @patch("starwhale.utils.docker.gen_swcli_docker_cmd")
    def test_run_in_container(self, m_gencmd: MagicMock, m_call: MagicMock):
        with tempfile.TemporaryDirectory() as d:
            m_gencmd.return_value = "hi"
            m_call.return_value = 0
            ModelTermView.run_in_container(model_src_dir=Path(d), docker_image="img1")
            m_gencmd.assert_called_once_with(
                "img1", envs={"SW_INSTANCE_URI": "local"}, mounts=[d]
            )
            m_call.assert_called_once_with("hi", shell=True)

    @patch("starwhale.core.model.model.ModelConfig.do_validate")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    def test_prepare_model_run_args(self, *args: t.Any) -> None:
        user_workdir = Path("/home/user/workdir")

        ensure_file(
            user_workdir / "model.yaml",
            content=yaml.safe_dump(
                ModelConfig(name="default", run={"handler": "a.b.c"}).asdict()
            ),
            parents=True,
        )

        ensure_file(
            user_workdir / "custom_model.yaml",
            content=yaml.safe_dump(
                ModelConfig(name="custom", run={"modules": ["a.b.c", "d.e.f"]}).asdict()
            ),
            parents=True,
        )

        model_snapshot_dir = self.sw.rootdir / "self/model/model-test/12/1234.swmp"
        model_snapshot_src_dir = model_snapshot_dir / "src"
        model_snapshot_manifest_path = model_snapshot_dir / "_manifest.yaml"
        model_snapshot_model_yaml_path = model_snapshot_src_dir / "model.yaml"

        ensure_file(
            model_snapshot_model_yaml_path,
            content=yaml.safe_dump(
                ModelConfig(name="built-model", run={"modules": ["x.y.z"]}).asdict()
            ),
            parents=True,
        )

        ensure_file(
            model_snapshot_manifest_path,
            content=yaml.safe_dump({"packaged_runtime": {"name": "packaged-runtime"}}),
            parents=True,
        )

        cases = [
            (
                {
                    "model": "",
                    "runtime": "",
                    "workdir": user_workdir,
                    "modules": "",
                    "model_yaml": None,
                    "forbid_packaged_runtime": False,
                },
                {
                    "model_src_dir": user_workdir,
                    "config_name": "default",
                    "config_modules": ["a.b.c"],
                    "runtime_uri": None,
                },
            ),
            (
                {
                    "model": "",
                    "runtime": "",
                    "workdir": user_workdir / "child" / "..",
                    "modules": "",
                    "model_yaml": None,
                    "forbid_packaged_runtime": False,
                },
                {
                    "model_src_dir": user_workdir,
                    "config_name": "default",
                    "config_modules": ["a.b.c"],
                    "runtime_uri": None,
                },
            ),
            (
                {
                    "model": "",
                    "runtime": "",
                    "workdir": user_workdir,
                    "modules": "",
                    "model_yaml": "/home/user/workdir/custom_model.yaml",
                    "forbid_packaged_runtime": False,
                },
                {
                    "model_src_dir": user_workdir,
                    "config_name": "custom",
                    "config_modules": ["a.b.c", "d.e.f"],
                    "runtime_uri": None,
                },
            ),
            (
                {
                    "model": "model-test/version/1234",
                    "runtime": "",
                    "workdir": "",
                    "modules": "",
                    "model_yaml": None,
                    "forbid_packaged_runtime": False,
                },
                {
                    "model_src_dir": model_snapshot_src_dir,
                    "config_name": "built-model",
                    "config_modules": ["x.y.z"],
                    "runtime_uri": Resource(
                        "model-test/version/1234",
                        typ=ResourceType.model,
                    ),
                },
            ),
            (
                {
                    "model": "model-test/version/1234",
                    "runtime": "runtime-test/version/1234",
                    "workdir": "",
                    "modules": "",
                    "model_yaml": None,
                    "forbid_packaged_runtime": False,
                },
                {
                    "model_src_dir": model_snapshot_src_dir,
                    "config_name": "built-model",
                    "config_modules": ["x.y.z"],
                    "runtime_uri": Resource(
                        "runtime-test/version/1234",
                        typ=ResourceType.runtime,
                    ),
                },
            ),
            (
                {
                    "model": "model-test/version/1234",
                    "runtime": "",
                    "workdir": "",
                    "modules": "",
                    "model_yaml": None,
                    "forbid_packaged_runtime": True,
                },
                {
                    "model_src_dir": model_snapshot_src_dir,
                    "config_name": "built-model",
                    "config_modules": ["x.y.z"],
                    "runtime_uri": None,
                },
            ),
        ]

        for case, expect in cases:
            model_src_dir, config, runtime_uri = _prepare_model_run_args(**case)
            assert expect["model_src_dir"] == model_src_dir
            assert expect["config_name"] == config.name
            assert expect["config_modules"] == config.run.modules
            if runtime_uri is None:
                assert expect["runtime_uri"] is None
            else:
                assert expect["runtime_uri"] == runtime_uri

    @patch("starwhale.core.model.model.ModelConfig.do_validate")
    @patch("starwhale.core.model.model.StandaloneModel")
    @patch("starwhale.core.model.model.StandaloneModel.serve")
    @patch(
        "starwhale.base.uri.resource.Resource._refine_local_rc_info",
        MagicMock(),
    )
    @patch("starwhale.core.model.view.RuntimeProcess")
    def test_serve(self, *args: t.Any):
        host = "127.0.0.1"
        port = 80
        yaml_name = "model.yaml"
        runtime = "pytorch/version/latest"

        cases = [
            (
                [
                    "--uri=mnist/version/latest",
                    f"--runtime={runtime}",
                    f"--host={host}",
                    f"--port={port}",
                ],
                0,
            ),
            (
                [
                    "--uri=mnist/version/latest",
                    f"--host={host}",
                    f"--port={port}",
                ],
                0,
            ),
            (
                [
                    "--workdir=.",
                    f"--runtime={runtime}",
                    f"--host={host}",
                    f"--port={port}",
                    f"--model-yaml={yaml_name}",
                ],
                0,
            ),
            (
                [
                    "--workdir=.",
                    f"--host={host}",
                    f"--port={port}",
                    f"--model-yaml={yaml_name}",
                ],
                0,
            ),
            (
                [
                    "--uri=mnist/version/latest",
                    "--workdir=.",
                    f"--runtime={runtime}",
                    f"--host={host}",
                    f"--port={port}",
                ],
                2,
            ),
            (
                [
                    f"--runtime={runtime}",
                    f"--host={host}",
                    f"--port={port}",
                ],
                2,
            ),
        ]

        for args, exit_code in cases:
            mock_obj = MagicMock()
            runner = CliRunner()
            result = runner.invoke(serve_cli, args, obj=mock_obj)
            assert result.exit_code == exit_code


class CloudModelTest(TestCase):
    def setUp(self) -> None:
        sw_config._config = {
            "storage": {"root": "/tmp/mock"},
            "instances": {"foo": {"uri": "https://foo.com", "user_name": "test"}},
            "current_instance": "foo",
        }

    @Mocker()
    def test_tag(self, rm: Mocker) -> None:
        uri = "cloud://foo/project/starwhale/model/mnist/version/123456a"
        rm.get(
            "https://foo.com/api/v1/project/starwhale/model/mnist",
            json={
                "data": {
                    "versionId": "100",
                    "name": "mnist",
                    "versionName": "123456a",
                }
            },
        )
        tag_url = (
            "https://foo.com/api/v1/project/starwhale/model/mnist/version/123456a/tag"
        )
        add_tag_request = rm.post(tag_url)
        ModelTermView(uri).tag(tags=["t1", "t2"], force_add=True)
        assert add_tag_request.call_count == 2
        assert add_tag_request.last_request.text == "force=True&tag=t2"

        error_message = "failed to add tags"
        add_tag_request = rm.post(
            tag_url,
            json={"data": "failed", "code": 500, "message": error_message},
            status_code=500,
        )
        ModelTermView(uri).tag(tags=["t1"], ignore_errors=True, force_add=False)
        assert add_tag_request.call_count == 1

        with self.assertRaisesRegex(RuntimeError, error_message):
            ModelTermView(uri).tag(tags=["t1"], ignore_errors=False, force_add=False)

        remove_tag_request = rm.delete(f"{tag_url}/t1")
        ModelTermView(uri).tag(tags=["t1"], remove=True)
        assert remove_tag_request.call_count == 1

        remove_tag_request = rm.delete(
            f"{tag_url}/t1",
            status_code=500,
            json={"data": "failed", "code": 500, "message": error_message},
        )
        ModelTermView(uri).tag(tags=["t1"], ignore_errors=True, remove=True)
        assert remove_tag_request.call_count == 1
        with self.assertRaisesRegex(RuntimeError, error_message):
            ModelTermView(uri).tag(tags=["t1"], ignore_errors=False, remove=True)

        list_tag_request = rm.get(tag_url, json={"data": ["t1", "t2"]})
        ModelTermView(uri).tag(tags=[])
        assert list_tag_request.call_count == 1

    @Mocker()
    def test_run(self, rm: Mocker) -> None:
        rm.get(
            "https://foo.com/api/v1/project/starwhale/model/mnist",
            json={
                "data": {
                    "versionId": "100",
                    "name": "mnist",
                    "versionName": "123456a",
                }
            },
        )
        rm.get(
            "https://foo.com/api/v1/project/starwhale/dataset/mnist",
            json={
                "data": {
                    "versionId": "200",
                    "name": "mnist",
                    "versionName": "223456a",
                }
            },
        )
        rm.get(
            "https://foo.com/api/v1/project/starwhale/runtime/mnist",
            json={
                "data": {
                    "versionId": "300",
                    "name": "mnist",
                    "versionName": "323456a",
                }
            },
        )
        rm.post(
            "https://foo.com/api/v1/project/starwhale/job", json={"data": "success"}
        )
        result, data = CloudModel.run(
            project_uri=Project("https://foo.com/project/starwhale"),
            model_uri="mnist/version/123456a",
            dataset_uris=["mnist/version/223456a"],
            runtime_uri="mnist/version/323456a",
            resource_pool="default",
            run_handler="test:predict",
        )
        assert result
        assert data == "success"
        assert rm.call_count == 4
        assert rm.request_history[0].qs == {"versionurl": ["123456a"]}
        assert rm.request_history[0].method == "GET"
        assert rm.request_history[1].qs == {"versionurl": ["223456a"]}
        assert rm.request_history[1].method == "GET"
        assert rm.request_history[2].qs == {"versionurl": ["323456a"]}
        assert rm.request_history[2].method == "GET"
        assert json.loads(rm.request_history[3].text) == {
            "modelVersionUrl": "100",
            "datasetVersionUrls": "200",
            "runtimeVersionUrl": "300",
            "resourcePool": "default",
            "handler": "test:predict",
        }
        assert rm.request_history[3].method == "POST"

    def test_cli_list(self) -> None:
        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            list_cli,
            ["--filter", "name=mn", "--filter", "owner=sw", "--filter", "latest"],
            obj=mock_obj,
        )

        assert result.exit_code == 0
        assert mock_obj.list.call_count == 1
        call_args = mock_obj.list.call_args[0]
        assert len(call_args[5]) == 3
        assert "name=mn" in call_args[5]
        assert "owner=sw" in call_args[5]
        assert "latest" in call_args[5]

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            list_cli,
            [],
            obj=mock_obj,
        )

        assert result.exit_code == 0
        assert mock_obj.list.call_count == 1
        call_args = mock_obj.list.call_args[0]
        assert len(call_args[5]) == 0


@patch("starwhale.core.model.model.generate_jobs_yaml")
@patch("starwhale.core.model.model.StandaloneModel._get_service")
@patch("starwhale.utils.config.load_swcli_config")
def test_build_with_custom_config_file(
    m_sw_config: MagicMock,
    m_get_service: MagicMock,
    m_generate_yaml: MagicMock,
    tmp_path: pathlib.Path,
):
    m_sw_config.return_value = {
        "storage": {
            "root": tmp_path.absolute(),
        },
        "instances": {
            "local": {
                "current_project": "self",
                "type": "standalone",
                "uri": "local",
            }
        },
        "current_instance": "local",
    }

    # generate a fake file as the example
    example = tmp_path / "example.png"
    ensure_file(example, "fake image content")

    svc = MagicMock(spec=Service)
    svc.get_spec.return_value = {"foo": "bar"}
    svc.example_resources = [example]
    m_get_service.return_value = svc

    name = "foo"
    workdir = tmp_path / "bar"
    cfg = "my_custom_config.yaml"
    ensure_dir(workdir)
    ensure_dir(workdir / "models")
    ensure_file(workdir / "models/mnist_cnn.pt", "baz")
    ensure_dir(workdir / "config")
    ensure_file(workdir / "config/hyperparam.json", "baz")
    ensure_file(workdir / cfg, _model_yaml)

    model_config = ModelConfig.create_by_yaml(workdir / cfg)

    model_uri = Resource(
        name,
        typ=ResourceType.model,
    )
    sm = StandaloneModel(model_uri)
    sm.build(workdir=workdir, model_config=model_config)

    build_version = sm.uri.version

    bundle_path = (
        tmp_path
        / "self"
        / ResourceType.model.value
        / name
        / build_version[:VERSION_PREFIX_CNT]
        / f"{build_version}{BundleType.MODEL}"
    )

    assert bundle_path.exists()
    assert (bundle_path / "src").exists()
    assert (bundle_path / "src" / DefaultYAMLName.MODEL).exists()
    assert (bundle_path / "src" / ".starwhale" / "examples" / "example.png").exists()


@patch("starwhale.core.model.model.generate_jobs_yaml")
@patch("starwhale.utils.config.load_swcli_config")
def test_render_eval_layout(m_sw_config: MagicMock, m_g: MagicMock, tmp_path: Path):
    m_sw_config.return_value = {
        "storage": {
            "root": tmp_path.absolute(),
        },
        "instances": {
            "local": {
                "current_project": "self",
                "type": "standalone",
                "uri": "local",
            }
        },
        "current_instance": "local",
    }
    name = "bar"
    model_uri = Resource(
        name,
        typ=ResourceType.model,
    )
    sm = StandaloneModel(model_uri)

    workdir = tmp_path / name
    model_config = ModelConfig(name=name, run={"handler": "baz:main"})
    ensure_file(workdir / "baz.py", "def main(): pass", parents=True)

    # test with no layout
    # no error should be raised
    sm.build(workdir=workdir, model_config=model_config)

    build_version = sm.uri.version
    bundle_path = (
        tmp_path
        / "self"
        / ResourceType.model.value
        / name
        / build_version[:VERSION_PREFIX_CNT]
        / f"{build_version}{BundleType.MODEL}"
    )
    empty_dir(bundle_path)

    # test with json layout
    d = workdir / SW_AUTO_DIRNAME / EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME
    ensure_file(d, '{"foo": "bar"}', parents=True)
    sm.build(workdir=workdir, model_config=model_config)

    layout = (
        bundle_path / "src" / SW_AUTO_DIRNAME / EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME
    )
    assert layout.exists()
    assert layout.read_text() == '{"foo": "bar"}'
    empty_dir(bundle_path)

    # test with yaml layout
    d = workdir / SW_AUTO_DIRNAME / EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME
    ensure_file(d, "bar: baz", parents=True)
    sm.build(workdir=workdir, model_config=model_config)
    assert layout.exists()
    assert layout.read_text() == '{"bar": "baz"}'
