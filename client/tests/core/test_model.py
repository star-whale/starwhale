import os
import typing as t
import pathlib
import tempfile
from pathlib import Path
from unittest.mock import patch, MagicMock

from click.testing import CliRunner
from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.utils import config as sw_config
from starwhale.utils import load_yaml
from starwhale.consts import (
    FileFlag,
    HTTPMethod,
    DefaultYAMLName,
    SW_AUTO_DIRNAME,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_EVALUATION_JOBS_FNAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, BundleType
from starwhale.api.service import Service
from starwhale.utils.config import SWCliConfigMixed
from starwhale.api._impl.job import Context, context_holder
from starwhale.core.job.model import Step
from starwhale.core.model.cli import _list as list_cli
from starwhale.core.model.view import ModelTermView
from starwhale.core.model.model import StandaloneModel, resource_to_file_node
from starwhale.core.instance.view import InstanceTermView
from starwhale.api._impl.evaluation import PipelineHandler, PPLResultIterator

_model_data_dir = f"{ROOT_DIR}/data/model"
_model_yaml = open(f"{_model_data_dir}/model.yaml").read()
_base_model_yaml = open(f"{_model_data_dir}/base_manifest.yaml").read()
_compare_model_yaml = open(f"{_model_data_dir}/compare_manifest.yaml").read()


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

    @patch("starwhale.base.blob.store.LocalFileStore.copy_dir")
    @patch("starwhale.core.model.model.file_stat")
    @patch("starwhale.core.model.model.StandaloneModel._get_service")
    @patch("starwhale.core.model.model.Walker.files")
    @patch("starwhale.core.model.model.blake2b_file")
    def test_build_workflow(
        self,
        m_blake_file: MagicMock,
        m_walker_files: MagicMock,
        m_get_service: MagicMock,
        m_stat: MagicMock,
        m_copy_dir: MagicMock,
    ) -> None:
        m_stat.return_value.st_size = 1
        m_blake_file.return_value = "123456"
        m_walker_files.return_value = []

        svc = MagicMock(spec=Service)
        svc.get_spec.return_value = {}
        svc.example_resources = []
        m_get_service.return_value = svc

        model_uri = URI(self.name, expected_type=URIType.MODEL)
        sm = StandaloneModel(model_uri)
        sm.build(workdir=Path(self.workdir))

        build_version = sm.uri.object.version

        bundle_path = (
            self.sw.rootdir
            / "self"
            / URIType.MODEL
            / self.name
            / build_version[:VERSION_PREFIX_CNT]
            / f"{build_version}{BundleType.MODEL}"
        )

        assert bundle_path.exists()
        assert (bundle_path / "src").exists()
        assert (
            bundle_path / "src" / SW_AUTO_DIRNAME / DEFAULT_EVALUATION_JOBS_FNAME
        ).exists()

        _manifest = load_yaml(bundle_path / DEFAULT_MANIFEST_NAME)
        assert "name" not in _manifest
        assert _manifest["version"] == build_version

        assert m_copy_dir.call_count == 1
        assert m_copy_dir.call_args_list[0][0][0] == "/home/starwhale/myproject"
        assert m_copy_dir.call_args_list[0][0][1].endswith("/src")

        assert bundle_path.exists()
        assert "latest" in sm.tag.list()

        model_uri = URI(f"mnist/version/{build_version}", expected_type=URIType.MODEL)

        sm = StandaloneModel(model_uri)
        _info = sm.info()

        assert _info["version"] == build_version
        assert _info["name"] == self.name
        assert _info["config"]["build"]["os"] in ["Linux", "Darwin"]
        assert "history" not in _info

        model_uri = URI(self.name, expected_type=URIType.MODEL)
        sm = StandaloneModel(model_uri)
        ensure_dir(sm.store.bundle_dir / f"{sm.store.bundle_type}")
        _info = sm.info()

        assert len(_info["history"]) == 1
        assert _info["history"][0]["name"] == self.name
        assert _info["history"][0]["version"] == build_version

        _history = sm.history()
        assert _info["history"] == _history

        _list, _ = StandaloneModel.list(URI(""))
        assert len(_list) == 1
        assert not _list[self.name][0]["is_removed"]

        model_uri = URI(
            f"{self.name}/version/{build_version}", expected_type=URIType.MODEL
        )
        sd = StandaloneModel(model_uri)
        _ok, _ = sd.remove(False)
        assert _ok

        _list, _ = StandaloneModel.list(URI(""))
        assert _list[self.name][0]["is_removed"]

        _ok, _ = sd.recover(True)
        _list, _ = StandaloneModel.list(URI(""))
        assert not _list[self.name][0]["is_removed"]

        ModelTermView(self.name).info()
        ModelTermView(self.name).history()
        fname = f"{self.name}/version/{build_version}"
        ModelTermView(fname).info()
        ModelTermView(fname).diff(
            URI(fname, expected_type=URIType.MODEL), show_details=False
        )
        ModelTermView(fname).history()
        ModelTermView(fname).remove()
        ModelTermView(fname).recover()
        ModelTermView.list(show_removed=True)
        ModelTermView.list()

        _ok, _ = sd.remove(True)
        assert _ok
        _list, _ = StandaloneModel.list(URI(""))
        assert len(_list[self.name]) == 0

        ModelTermView.build(self.workdir, "self", Path(self.workdir) / "model.yaml")

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

        base_bundle_path = (
            self.sw.rootdir
            / "self"
            / URIType.MODEL
            / self.name
            / base_version[:VERSION_PREFIX_CNT]
            / f"{base_version}{BundleType.MODEL}"
        )
        ensure_dir(base_bundle_path)
        ensure_file(base_bundle_path / DEFAULT_MANIFEST_NAME, _base_model_yaml)

        compare_bundle_path = (
            self.sw.rootdir
            / "self"
            / URIType.MODEL
            / self.name
            / compare_version[:VERSION_PREFIX_CNT]
            / f"{compare_version}{BundleType.MODEL}"
        )
        ensure_dir(compare_bundle_path)
        ensure_file(compare_bundle_path / DEFAULT_MANIFEST_NAME, _compare_model_yaml)

        base_model_uri = URI(
            f"{self.name}/version/{base_version}", expected_type=URIType.MODEL
        )
        sm = StandaloneModel(base_model_uri)

        diff_info = sm.diff(
            URI(f"{self.name}/version/{compare_version}", expected_type=URIType.MODEL)
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

    @patch("starwhale.core.job.model.Generator.generate_job_from_yaml")
    @patch("starwhale.api._impl.job.Parser.generate_job_yaml")
    @patch("starwhale.core.job.scheduler.Scheduler.schedule_single_task")
    @patch("starwhale.core.job.scheduler.Scheduler.schedule_single_step")
    @patch("starwhale.core.job.scheduler.Scheduler.schedule")
    def test_eval(
        self,
        schedule_mock: MagicMock,
        single_step_mock: MagicMock,
        single_task_mock: MagicMock,
        gen_yaml_mock: MagicMock,
        gen_job_mock: MagicMock,
    ):
        gen_job_mock.return_value = {
            "default": [
                Step(
                    job_name="default",
                    step_name="ppl",
                    cls_name="",
                    resources=[{"type": "cpu", "limit": 1, "request": 1}],
                    concurrency=1,
                    task_num=1,
                    needs=[],
                ),
                Step(
                    job_name="default",
                    step_name="cmp",
                    cls_name="",
                    resources=[{"type": "cpu", "limit": 1, "request": 1}],
                    concurrency=1,
                    task_num=1,
                    needs=["ppl"],
                ),
            ]
        }
        StandaloneModel.eval_user_handler(
            project="test",
            version="qwertyuiop",
            workdir=Path(self.workdir),
            dataset_uris=["mnist/version/latest"],
            step_name="ppl",
            task_index=0,
        )
        schedule_mock.assert_not_called()
        single_step_mock.assert_not_called()
        single_task_mock.assert_called_once()

        StandaloneModel.eval_user_handler(
            project="test",
            version="qwertyuiop",
            workdir=Path(self.workdir),
            dataset_uris=["mnist/version/latest"],
            step_name="ppl",
            task_index=-1,
        )
        single_step_mock.assert_called_once()

        StandaloneModel.eval_user_handler(
            project="test",
            version="qwertyuiop",
            workdir=Path(self.workdir),
            dataset_uris=["mnist/version/latest"],
        )
        schedule_mock.assert_called_once()

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

    def test_get_cls_from_model_yaml(self):
        from starwhale.core.model import default_handler

        with self.assertRaises(Exception):
            default_handler._get_cls(Path(_model_data_dir))

    @patch("starwhale.api._impl.data_store.atexit")
    @patch("starwhale.core.model.default_handler.StandaloneModel")
    @patch("starwhale.core.model.default_handler.import_object")
    def test_default_handler(
        self, m_import: MagicMock, m_model: MagicMock, m_atexit: MagicMock
    ):
        from starwhale.core.model import default_handler

        class SimpleHandler(PipelineHandler):
            def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
                pass

            def cmp(self, _iter: PPLResultIterator) -> t.Any:
                pass

            def some(self):
                assert self.context.version == "rwerwe9"
                return "success"

        m_model.load_model_config().return_value = {"run": {"ppl": "test"}}
        m_import.return_value = SimpleHandler
        context = Context(
            workdir=Path(_model_data_dir),
            version="rwerwe9",
            project="self",
        )
        context_holder.context = context
        default_handler._invoke(context, "some")

    @patch("starwhale.utils.process.check_call")
    @patch("starwhale.utils.docker.gen_swcli_docker_cmd")
    def test_use_docker(self, m_gencmd: MagicMock, m_call: MagicMock):
        with tempfile.TemporaryDirectory() as tmpdirname:
            m_gencmd.return_value = "hi"
            m_call.return_value = 0
            ModelTermView.eval("", tmpdirname, [], use_docker=True, image="img1")
            m_gencmd.assert_called_once_with(
                "img1", env_vars={}, mnt_paths=[tmpdirname]
            )
            m_call.assert_called_once_with("hi", shell=True)

    @patch("starwhale.core.model.model.StandaloneModel")
    @patch("starwhale.core.model.model.StandaloneModel.serve")
    @patch("starwhale.core.runtime.process.Process.from_runtime_uri")
    def test_serve(self, *args: t.Any):
        host = "127.0.0.1"
        port = 80
        yaml = "model.yaml"
        runtime = "pytorch/version/latest"
        ModelTermView.serve("", yaml, runtime, "mnist/version/latest", host, port)
        ModelTermView.serve(".", yaml, runtime, "", host, port)
        ModelTermView.serve(".", yaml, "", "", host, port)

        with self.assertRaises(SystemExit):
            ModelTermView.serve("", yaml, runtime, "", host, port)

        with self.assertRaises(SystemExit):
            ModelTermView.serve("set", yaml, runtime, "set", host, port)


class CloudModelTest(TestCase):
    def setUp(self) -> None:
        sw_config._config = {}

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


@patch("starwhale.core.model.model.StandaloneModel._get_service")
def test_build_with_custom_config_file(
    m_get_service: MagicMock, tmp_path: pathlib.Path
):
    sw_config._config = {
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
    svc.get_spec.return_value = {}
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

    model_uri = URI(name, expected_type=URIType.MODEL)
    sm = StandaloneModel(model_uri)
    sm.build(workdir=workdir, yaml_path=workdir / cfg)

    build_version = sm.uri.object.version

    bundle_path = (
        tmp_path
        / "self"
        / URIType.MODEL
        / name
        / build_version[:VERSION_PREFIX_CNT]
        / f"{build_version}{BundleType.MODEL}"
    )

    assert bundle_path.exists()
    assert (bundle_path / "src").exists()
    assert (bundle_path / "src" / DefaultYAMLName.MODEL).exists()
    assert (bundle_path / "src" / ".starwhale" / "examples" / "example.png").exists()
