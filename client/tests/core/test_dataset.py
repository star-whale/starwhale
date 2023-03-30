import os
import typing as t
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
import numpy
from click.testing import CliRunner
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
from starwhale.api._impl import data_store
from starwhale.base.type import URIType, BundleType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.dataset.cli import _list as list_cli
from starwhale.core.dataset.cli import _build as build_cli
from starwhale.core.dataset.type import (
    Line,
    Link,
    Point,
    Polygon,
    JsonDict,
    MIMEType,
    DatasetConfig,
    GrayscaleImage,
    D_FILE_VOLUME_SIZE,
)
from starwhale.core.dataset.view import DatasetTermView, DatasetTermViewJson
from starwhale.core.dataset.model import Dataset, StandaloneDataset

_dataset_data_dir = f"{ROOT_DIR}/data/dataset"
_dataset_yaml = open(f"{_dataset_data_dir}/dataset.yaml").read()


class StandaloneDatasetTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}

    @patch("starwhale.api._impl.dataset.model.Dataset.commit")
    @patch("starwhale.api._impl.dataset.model.Dataset.__setitem__")
    def test_function_handler_make_swds(
        self, m_setitem: MagicMock, m_commit: MagicMock
    ) -> None:
        name = "mnist"
        dataset_uri = URI(name, expected_type=URIType.DATASET)
        sd = StandaloneDataset(dataset_uri)
        sd._version = "112233"
        swds_config = DatasetConfig(name=name, handler=lambda: 1)

        with self.assertRaisesRegex(TypeError, "object is not iterable"):
            sd.build(config=swds_config)

        assert not m_commit.called
        assert not m_setitem.called

        def _iter_swds_bin_item() -> t.Generator:
            yield {"bytes": b"", "link": Link("")}

        swds_config.handler = _iter_swds_bin_item
        sd.build(config=swds_config)

        assert m_commit.call_count == 1
        assert m_setitem.call_count == 1

    @patch("starwhale.core.dataset.cli.import_object")
    def test_build_only_cli(self, m_import: MagicMock) -> None:
        workdir = "/tmp/workdir"
        ensure_dir(workdir)

        assert not (Path(workdir) / DefaultYAMLName.DATASET).exists()

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [workdir, "--name", "mnist", "--handler", "mnist:test"],
            obj=mock_obj,
        )

        assert result.exit_code == 0
        assert mock_obj.build.call_count == 1
        call_args = mock_obj.build.call_args[0]
        assert call_args[0] == workdir
        assert call_args[1].name == "mnist"
        assert m_import.call_args[0][1] == "mnist:test"

    @patch("starwhale.core.dataset.cli.import_object")
    def test_build_only_yaml(self, m_import: MagicMock) -> None:
        workdir = "/tmp/workdir"
        ensure_dir(workdir)

        config = DatasetConfig(name="mnist", handler="dataset:build")
        yaml_path = Path(workdir) / DefaultYAMLName.DATASET
        ensure_file(yaml_path, yaml.safe_dump(config.asdict()))

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                workdir,
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build.call_count == 1
        call_args = mock_obj.build.call_args[0]
        assert call_args[1].name == "mnist"
        assert m_import.call_args[0][1] == "dataset:build"

        new_workdir = "/tmp/workdir-new"
        ensure_dir(new_workdir)
        new_yaml_path = Path(new_workdir) / "dataset-new.yaml"
        yaml_path.rename(new_yaml_path)
        assert new_yaml_path.exists() and not yaml_path.exists()

        mock_obj.reset_mock()
        m_import.reset_mock()
        result = runner.invoke(
            build_cli, [new_workdir, "-f", "dataset-new.yaml"], obj=mock_obj
        )
        assert result.exit_code == 0
        assert mock_obj.build.call_count == 1
        assert call_args[1].name == "mnist"
        assert m_import.call_args[0][1] == "dataset:build"

    @patch("starwhale.core.dataset.cli.import_object")
    def test_build_mixed_cli_yaml(self, m_import: MagicMock) -> None:
        handler_func = lambda: 1
        m_import.return_value = handler_func
        workdir = "/tmp/workdir"
        ensure_dir(workdir)
        config = DatasetConfig(
            name="mnist-error",
            handler="dataset:not_found",
        )
        yaml_path = Path(workdir) / DefaultYAMLName.DATASET
        ensure_file(yaml_path, yaml.safe_dump(config.asdict()))

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                workdir,
                "--name",
                "mnist",
                "--handler",
                "dataset:buildFunction",
                "--project",
                "self",
            ],
            obj=mock_obj,
        )

        assert result.exit_code == 0
        assert mock_obj.build.call_count == 1
        call_args = mock_obj.build.call_args[0]
        assert call_args[1].name == "mnist"
        assert call_args[1].handler == handler_func
        assert call_args[1].attr.volume_size == D_FILE_VOLUME_SIZE

    def test_build_workflow(
        self,
    ) -> None:
        class _MockBuildExecutor:
            def __iter__(self) -> t.Generator:
                yield {"data": b"", "label": 1}

        sw = SWCliConfigMixed()

        workdir = "/home/starwhale/myproject"
        name = "mnist"

        ensure_dir(os.path.join(workdir, "data"))
        ensure_file(os.path.join(workdir, "mnist.py"), " ")

        config = DatasetConfig(**yaml.safe_load(_dataset_yaml))
        config.handler = _MockBuildExecutor
        dataset_uri = URI(name, expected_type=URIType.DATASET)
        sd = StandaloneDataset(dataset_uri)
        sd.build(workdir=Path(workdir), config=config)
        build_version = sd.uri.object.version

        snapshot_workdir = (
            sw.rootdir
            / "self"
            / URIType.DATASET
            / name
            / build_version[:VERSION_PREFIX_CNT]
            / f"{build_version}{BundleType.DATASET}"
        )

        assert snapshot_workdir.exists()

        _manifest = load_yaml(snapshot_workdir / DEFAULT_MANIFEST_NAME)
        assert _manifest["version"] == build_version
        assert "name" not in _manifest

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

        config.project_uri = "self"
        DatasetTermView.build(workdir, config)

        # make sure tmp dir is empty
        assert len(os.listdir(sw.rootdir / SW_TMP_DIR_NAME)) == 0

    def test_head(self) -> None:
        from starwhale.api._impl.dataset import Dataset as SDKDataset

        sds = SDKDataset.dataset("mnist-head-test")
        sds.append(
            (
                "label-0",
                {
                    "img": GrayscaleImage(fp=b"123"),
                    "label": 0,
                },
            )
        )
        sds.append(
            (
                "label-1",
                {
                    "img": GrayscaleImage(fp=b"456"),
                    "label": 1,
                },
            )
        )
        sds.commit()
        sds.close()

        dataset_uri = "mnist-head-test/version/latest"
        ds = Dataset.get_dataset(URI(dataset_uri, expected_type=URIType.DATASET))

        results = ds.head(0)
        assert len(results) == 0

        results = ds.head(1, show_raw_data=True)
        assert results[0]["id"] == 0
        assert results[0]["index"] == "label-0"
        assert results[0]["features"]["label"] == 0
        assert results[0]["features"]["img"].mime_type == MIMEType.GRAYSCALE
        assert len(results[0]["features"]["img"].to_bytes()) == 3
        assert len(results) == 1

        results = ds.head(5, show_raw_data=True)
        assert len(results) == 2
        DatasetTermView(dataset_uri).head(1, show_raw_data=True)
        DatasetTermView(dataset_uri).head(2, show_raw_data=True)
        DatasetTermViewJson(dataset_uri).head(1, show_raw_data=False)
        DatasetTermViewJson(dataset_uri).head(2, show_raw_data=True)


class TestJsonDict(TestCase):
    JSON_DICT = {
        "a": 1,
        "b": [1, 2, 3],
        "c": {"ca": "1"},
        "d": Link("http://ad.c/d"),
        "e": ("a", "b"),
    }

    def test_init(self) -> None:
        _jd = JsonDict(self.JSON_DICT)
        self._do_assert(_jd)

    def _do_assert(self, _jd: JsonDict) -> None:
        self.assertEqual(1, _jd.a)
        self.assertEqual([1, 2, 3], _jd.b)
        self.assertEqual(JsonDict, type(_jd.c))
        self.assertEqual("1", _jd.c.ca)
        self.assertEqual(Link, type(_jd.d))
        self.assertEqual("http://ad.c/d", _jd.d.uri)
        self.assertEqual(("a", "b"), _jd.e)
        self.assertEqual(
            data_store.SwObjectType(
                JsonDict,
                {
                    "a": data_store.INT64,
                    "b": data_store.SwListType(data_store.INT64),
                    "c": data_store.SwObjectType(JsonDict, {"ca": data_store.STRING}),
                    "d": data_store.SwObjectType(
                        data_store.Link,
                        {
                            "_type": data_store.STRING,
                            "uri": data_store.STRING,
                            "scheme": data_store.STRING,
                            "owner": data_store.UNKNOWN,
                            "offset": data_store.INT64,
                            "size": data_store.INT64,
                            "data_type": data_store.UNKNOWN,
                            "with_local_fs_data": data_store.BOOL,
                            "_local_fs_uri": data_store.STRING,
                            "_signed_uri": data_store.STRING,
                            "extra_info": data_store.SwMapType(
                                data_store.UNKNOWN, data_store.UNKNOWN
                            ),
                        },
                    ),
                    "e": data_store.SwTupleType(data_store.STRING),
                },
            ),
            data_store._get_type(_jd),
        )

    def test_cls_method(self):
        self.assertEqual(1, JsonDict.from_data(1))
        self.assertEqual("a", JsonDict.from_data("a"))
        self.assertEqual([1, 2, 3], JsonDict.from_data([1, 2, 3]))
        _d = JsonDict.from_data({"ca": "1"})
        self.assertEqual("1", _d.ca)
        _l = Link("http://ad.c/d")
        self.assertEqual(_l, JsonDict.from_data(_l))
        tpl = ("a", "b")
        self.assertEqual(tpl, JsonDict.from_data(tpl))
        sw_j_o = JsonDict.from_data(self.JSON_DICT)
        self._do_assert(sw_j_o)

    def test_asdict(self):
        sw_j_o = JsonDict.from_data(self.JSON_DICT)
        self.assertEqual(self.JSON_DICT, sw_j_o.asdict())
        self.assertEqual({}, JsonDict().asdict())


class TestLine(TestCase):
    def test_to_list(self):
        p = Line([Point(3.9, 4.5), Point(5.9, 6.5), Point(7.9, 9.5)])
        self.assertEqual([[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]], p.to_list())
        self.assertEqual("Line: [[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]]", str(p))
        self.assertEqual(numpy.float64, p.dtype)
        self.assertEqual(
            data_store.SwObjectType(
                Line,
                {
                    "_type": data_store.STRING,
                    "points": data_store.SwListType(
                        data_store.SwObjectType(
                            Point,
                            {
                                "_type": data_store.STRING,
                                "x": data_store.FLOAT64,
                                "y": data_store.FLOAT64,
                            },
                        )
                    ),
                },
            ),
            data_store._get_type(p),
        )


class TestPoint(TestCase):
    def test_to_list(self):
        p = Point(3.9, 4.5)
        self.assertEqual([3.9, 4.5], p.to_list())
        self.assertEqual(numpy.float64, p.dtype)
        self.assertEqual(
            data_store.SwObjectType(
                Point,
                {
                    "_type": data_store.STRING,
                    "x": data_store.FLOAT64,
                    "y": data_store.FLOAT64,
                },
            ),
            data_store._get_type(p),
        ),


class TestPolygon(TestCase):
    def test_to_list(self):
        p = Polygon([Point(3.9, 4.5), Point(5.9, 6.5), Point(7.9, 9.5)])
        self.assertEqual([[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]], p.to_list())
        self.assertEqual("Polygon: [[3.9, 4.5], [5.9, 6.5], [7.9, 9.5]]", str(p))
        self.assertEqual(numpy.float64, p.dtype)
        self.assertEqual(
            data_store.SwObjectType(
                Polygon,
                {
                    "_type": data_store.STRING,
                    "points": data_store.SwListType(
                        data_store.SwObjectType(
                            Point,
                            {
                                "_type": data_store.STRING,
                                "x": data_store.FLOAT64,
                                "y": data_store.FLOAT64,
                            },
                        )
                    ),
                },
            ),
            data_store._get_type(p),
        ),


class CloudDatasetTest(TestCase):
    def setUp(self) -> None:
        sw_config._config = {}

    def test_cli_list(self) -> None:
        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            list_cli,
            [
                "--filter",
                "name=mnist",
                "--filter",
                "owner=starwhale",
                "--filter",
                "latest",
            ],
            obj=mock_obj,
        )

        assert result.exit_code == 0
        assert mock_obj.list.call_count == 1
        call_args = mock_obj.list.call_args[0]
        assert len(call_args[5]) == 3
        assert "name=mnist" in call_args[5]
        assert "owner=starwhale" in call_args[5]
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
