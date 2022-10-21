import os
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.utils import config as sw_config
from starwhale.utils import load_yaml
from starwhale.consts import (
    DefaultYAMLName,
    SW_TMP_DIR_NAME,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, BundleType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.dataset.type import Link, DatasetConfig, DatasetSummary
from starwhale.core.dataset.view import DatasetTermView
from starwhale.core.dataset.model import StandaloneDataset
from starwhale.api._impl.dataset.builder import BaseBuildExecutor

_dataset_data_dir = f"{ROOT_DIR}/data/dataset"
_dataset_yaml = open(f"{_dataset_data_dir}/dataset.yaml").read()


class MockBuildExecutor(BaseBuildExecutor):
    def iter_item(self) -> t.Generator[t.Tuple[t.Any, t.Any], None, None]:
        yield b"", ""

    def make_swds(self) -> DatasetSummary:
        return DatasetSummary()


class StandaloneDatasetTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.api._impl.dataset.builder.UserRawBuildExecutor.make_swds")
    @patch("starwhale.api._impl.dataset.builder.SWDSBinBuildExecutor.make_swds")
    @patch("starwhale.core.dataset.model.import_object")
    def test_function_handler_make_swds(
        self, m_import: MagicMock, m_swds_bin: MagicMock, m_user_raw: MagicMock
    ) -> None:
        name = "mnist"
        dataset_uri = URI(name, expected_type=URIType.DATASET)
        sd = StandaloneDataset(dataset_uri)
        sd._version = "112233"
        workdir = "/home/starwhale/myproject"
        config = DatasetConfig(name, handler="mnist:handler")

        kwargs = dict(
            workdir=Path(workdir),
            swds_config=config,
            append=False,
            append_from_uri=None,
            append_from_store=None,
        )

        m_import.return_value = lambda: 1
        with self.assertRaises(RuntimeError):
            sd._call_make_swds(**kwargs)  # type: ignore

        m_import.reset_mock()

        def _iter_swds_bin_item() -> t.Generator:
            yield b"", {}

        def _iter_user_raw_item() -> t.Generator:
            yield Link(""), {}

        m_import.return_value = _iter_swds_bin_item
        sd._call_make_swds(**kwargs)  # type: ignore
        assert m_swds_bin.call_count == 1

        m_import.return_value = _iter_user_raw_item
        sd._call_make_swds(**kwargs)  # type: ignore
        assert m_user_raw.call_count == 1

    @patch("starwhale.core.dataset.model.copy_fs")
    @patch("starwhale.core.dataset.model.import_object")
    def test_build_workflow(
        self,
        m_import: MagicMock,
        m_copy_fs: MagicMock,
    ) -> None:
        sw = SWCliConfigMixed()

        m_import.return_value = MockBuildExecutor

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
        assert m_import.call_args[0][1] == "mnist.dataset:DatasetProcessExecutor"

        assert snapshot_workdir.exists()
        assert (snapshot_workdir / "data").exists()
        assert (snapshot_workdir / "src").exists()

        _manifest = load_yaml(snapshot_workdir / DEFAULT_MANIFEST_NAME)
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
        ensure_dir(sd.store.bundle_dir / sd.store.bundle_type)
        _info = sd.info()
        assert len(_info["history"]) == 1
        assert _info["history"][0]["name"] == name
        assert _info["history"][0]["version"] == build_version

        _history = sd.history()
        assert _info["history"] == _history

        _list, _ = StandaloneDataset.list(URI(""))
        assert len(_list) == 1
        assert not _list[name][0]["is_removed"]

        dataset_uri = URI(
            f"mnist/version/{build_version}", expected_type=URIType.DATASET
        )
        sd = StandaloneDataset(dataset_uri)
        _ok, _ = sd.remove(False)
        assert _ok

        _list, _ = StandaloneDataset.list(URI(""))
        assert _list[name][0]["is_removed"]

        _ok, _ = sd.recover(True)
        _list, _ = StandaloneDataset.list(URI(""))
        assert not _list[name][0]["is_removed"]

        DatasetTermView(name).info()
        DatasetTermView(name).history()
        fname = f"{name}/version/{build_version}"
        DatasetTermView(fname).info()
        DatasetTermView(fname).history()
        DatasetTermView(fname).remove()
        DatasetTermView(fname).recover()
        DatasetTermView.list()

        sd.remove(True)
        _list, _ = StandaloneDataset.list(URI(""))
        assert len(_list[name]) == 0

        DatasetTermView.build(workdir, "self")

        # make sure tmp dir is empty
        assert len(os.listdir(sw.rootdir / SW_TMP_DIR_NAME)) == 0
