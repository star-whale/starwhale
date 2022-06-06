import os
import json
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
from starwhale.core.dataset.view import DatasetTermView
from starwhale.core.dataset.model import StandaloneDataset

from .. import ROOT_DIR

_dataset_data_dir = f"{ROOT_DIR}/data/dataset"
_dataset_yaml = open(f"{_dataset_data_dir}/dataset.yaml").read()


class StandaloneDatasetTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.core.dataset.model.copy_fs")
    @patch("starwhale.core.dataset.model.import_cls")
    def test_build_workflow(self, m_import: MagicMock, m_copy_fs: MagicMock) -> None:
        sw = SWCliConfigMixed()

        m_cls = MagicMock()
        m_import.return_value = m_cls

        workdir = "/home/starwhale/myproject"
        name = "mnist"

        self.fs.create_file(
            os.path.join(workdir, DefaultYAMLName.DATASET), contents=_dataset_yaml
        )
        ensure_dir(os.path.join(workdir, "data"))
        ensure_file(os.path.join(workdir, "mnist.py"), " ")

        dataset_uri = URI(name, expected_type=URIType.DATASET)
        sd = StandaloneDataset(dataset_uri)
        sd.build(Path(workdir))
        build_version = sd.uri.object.version

        snapshot_workdir = (
            sw.rootdir
            / "self"
            / URIType.DATASET
            / name
            / build_version[:VERSION_PREFIX_CNT]
            / f"{build_version}{BundleType.DATASET}"
        )

        assert m_import.call_count == 1
        assert m_import.call_args[0][0] == Path(workdir)
        assert m_import.call_args[0][1] == "mnist.process:DataSetProcessExecutor"

        assert m_cls.call_count == 1
        assert m_cls.call_args[1]["data_dir"] == (Path(workdir) / "data").resolve()
        assert m_cls.call_args[1]["alignment_bytes_size"] == 4096
        assert m_cls.call_args[1]["output_dir"] == (snapshot_workdir / "data").resolve()

        assert snapshot_workdir.exists()
        assert (snapshot_workdir / "data").exists()
        assert (snapshot_workdir / "src").exists()

        _manifest = yaml.safe_load((snapshot_workdir / DEFAULT_MANIFEST_NAME).open())
        assert _manifest["name"] == name

        dataset_uri = URI(
            f"mnist/version/{build_version}", expected_type=URIType.DATASET
        )
        sd = StandaloneDataset(dataset_uri)
        _info = sd.info()

        assert _info["version"] == build_version
        assert _info["name"] == name
        assert _info["bundle_path"] == str(snapshot_workdir.resolve())
        assert "history" not in _info

        dataset_uri = URI(name, expected_type=URIType.DATASET)
        sd = StandaloneDataset(dataset_uri)
        _info = sd.info()
        assert len(_info["history"][0]) == 1
        assert _info["history"][0][0]["name"] == name
        assert _info["history"][0][0]["version"] == build_version

        _history = sd.history()
        assert _info["history"] == _history

        _list, _ = StandaloneDataset.list(URI(""))
        assert len(_list) == 1
        assert not _list[name][0]["is_removed"]

        dataset_uri = URI(
            f"mnist/version/{build_version}", expected_type=URIType.DATASET
        )
        sd = StandaloneDataset(dataset_uri)
        _ok, _ = sd.remove(True)
        assert _ok

        _list, _ = StandaloneDataset.list(URI(""))
        assert _list[name][0]["is_removed"]

        _ok, _ = sd.recover(True)
        _list, _ = StandaloneDataset.list(URI(""))
        assert not _list[name][0]["is_removed"]

        _fuse_json_path = StandaloneDataset.render_fuse_json(snapshot_workdir, True)
        _fuse_json = json.load(open(_fuse_json_path))
        assert _fuse_json["backend"] == "fuse"
        assert _fuse_json["kind"] == "swds"

        DatasetTermView(name).info()
        DatasetTermView(name).history()
        fname = f"{name}/version/{build_version}"
        DatasetTermView(fname).info()
        DatasetTermView(fname).history()
        DatasetTermView(fname).remove()
        DatasetTermView(fname).recover()
        DatasetTermView.list()

        DatasetTermView.render_fuse_json(fname, force=True)
        DatasetTermView.build(workdir, "self")
