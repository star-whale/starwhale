import os
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.consts import DefaultYAMLName, VERSION_PREFIX_CNT, DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, BundleType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.model.view import ModelTermView
from starwhale.core.model.model import StandaloneModel

from .. import ROOT_DIR

_model_data_dir = f"{ROOT_DIR}/data/model"
_model_yaml = open(f"{_model_data_dir}/model.yaml").read()


class StandaloneModelTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.core.model.model.copy_file")
    @patch("starwhale.core.model.model.copy_fs")
    @patch("starwhale.core.model.model.import_cls")
    def test_build_workflow(
        self, m_import: MagicMock, m_copy_fs: MagicMock, m_copy_file: MagicMock
    ) -> None:
        sw = SWCliConfigMixed()

        m_cls = MagicMock()
        m_import.return_value = m_cls

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

        _manifest = yaml.safe_load((snapshot_workdir / DEFAULT_MANIFEST_NAME).open())
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
        assert _info["config"]["build"]["os"] == "Linux"
        assert "history" not in _info

        model_uri = URI(name, expected_type=URIType.MODEL)
        sm = StandaloneModel(model_uri)
        _info = sm.info()

        assert len(_info["history"][0]) == 1
        assert _info["history"][0][0]["name"] == name
        assert _info["history"][0][0]["version"] == build_version

        _history = sm.history()
        assert _info["history"] == _history

        _list, _ = StandaloneModel.list(URI(""))
        assert len(_list) == 1
        assert not _list[name][0]["is_removed"]

        model_uri = URI(f"{name}/version/{build_version}", expected_type=URIType.MODEL)
        sd = StandaloneModel(model_uri)
        _ok, _ = sd.remove(True)
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

        ModelTermView.build(workdir, "self")
