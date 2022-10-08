import os
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.utils import load_yaml
from starwhale.consts import (
    HTTPMethod,
    DefaultYAMLName,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    DEFAULT_EVALUATION_JOBS_FNAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, BundleType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.api._impl.job import Context
from starwhale.api._impl.model import PipelineHandler, PPLResultIterator
from starwhale.core.model.view import ModelTermView
from starwhale.core.model.model import StandaloneModel
from starwhale.core.instance.view import InstanceTermView

from .. import ROOT_DIR

_model_data_dir = f"{ROOT_DIR}/data/model"
_model_yaml = open(f"{_model_data_dir}/model.yaml").read()


class StandaloneModelTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.core.model.model.copy_file")
    @patch("starwhale.core.model.model.copy_fs")
    def test_build_workflow(self, m_copy_fs: MagicMock, m_copy_file: MagicMock) -> None:
        sw = SWCliConfigMixed()

        workdir = "/home/starwhale/myproject"
        name = "mnist"

        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.MODEL), contents=_model_yaml
        )

        ensure_dir(os.path.join(workdir, "models"))
        ensure_dir(os.path.join(workdir, "config"))
        ensure_file(os.path.join(workdir, "models", "mnist_cnn.pt"), " ")
        ensure_file(os.path.join(workdir, "config", "hyperparam.json"), " ")

        model_uri = URI(name, expected_type=URIType.MODEL)
        sm = StandaloneModel(model_uri)
        sm.build(Path(workdir))

        build_version = sm.uri.object.version

        bundle_path = (
            sw.rootdir
            / "self"
            / URIType.MODEL
            / name
            / build_version[:VERSION_PREFIX_CNT]
            / f"{build_version}{BundleType.MODEL}"
        )

        snapshot_workdir = (
            sw.rootdir
            / "self"
            / "workdir"
            / URIType.MODEL
            / name
            / build_version[:VERSION_PREFIX_CNT]
            / build_version
        )

        assert snapshot_workdir.exists()
        assert (snapshot_workdir / "src").exists()
        assert (snapshot_workdir / "src" / DEFAULT_EVALUATION_JOBS_FNAME).exists()

        _manifest = load_yaml(snapshot_workdir / DEFAULT_MANIFEST_NAME)
        assert _manifest["name"] == name
        assert _manifest["version"] == build_version

        assert m_copy_file.call_count == 3
        assert m_copy_file.call_args_list[0][0][1] == "model.yaml"
        assert m_copy_file.call_args_list[1][0][1] == "config/hyperparam.json"
        assert m_copy_file.call_args_list[2][0][1] == "models/mnist_cnn.pt"

        assert bundle_path.exists()
        assert "latest" in sm.tag.list()

        model_uri = URI(f"mnist/version/{build_version}", expected_type=URIType.MODEL)

        sm = StandaloneModel(model_uri)
        _info = sm.info()

        assert _info["version"] == build_version
        assert _info["name"] == name
        assert _info["config"]["build"]["os"] in ["Linux", "Darwin"]
        assert "history" not in _info

        model_uri = URI(name, expected_type=URIType.MODEL)
        sm = StandaloneModel(model_uri)
        ensure_dir(sm.store.bundle_dir / f"xx{sm.store.bundle_type}")
        _info = sm.info()

        assert len(_info["history"]) == 1
        assert _info["history"][0]["name"] == name
        assert _info["history"][0]["version"] == build_version

        _history = sm.history()
        assert _info["history"] == _history

        _list, _ = StandaloneModel.list(URI(""))
        assert len(_list) == 1
        assert not _list[name][0]["is_removed"]

        model_uri = URI(f"{name}/version/{build_version}", expected_type=URIType.MODEL)
        sd = StandaloneModel(model_uri)
        _ok, _ = sd.remove(False)
        assert _ok

        _list, _ = StandaloneModel.list(URI(""))
        assert _list[name][0]["is_removed"]

        _ok, _ = sd.recover(True)
        _list, _ = StandaloneModel.list(URI(""))
        assert not _list[name][0]["is_removed"]

        ModelTermView(name).info()
        ModelTermView(name).history()
        fname = f"{name}/version/{build_version}"
        ModelTermView(fname).info()
        ModelTermView(fname).history()
        ModelTermView(fname).remove()
        ModelTermView(fname).recover()
        ModelTermView.list(show_removed=True)
        ModelTermView.list()

        _ok, _ = sd.remove(True)
        assert _ok
        _list, _ = StandaloneModel.list(URI(""))
        assert len(_list[name]) == 0

        ModelTermView.build(workdir, "self")

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
    @patch("starwhale.core.model.default_handler.import_cls")
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

        default_handler._invoke(
            Context(
                workdir=Path(_model_data_dir),
                version="rwerwe9",
                project="self",
            ),
            "some",
        )
