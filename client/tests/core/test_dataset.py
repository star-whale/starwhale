import os
import json
import typing as t
from pathlib import Path
from unittest import TestCase
from unittest.mock import patch, MagicMock

import yaml
from click.testing import CliRunner
from requests_mock import Mocker

from tests import ROOT_DIR, BaseTestCase
from starwhale.utils import config as sw_config
from starwhale.utils import load_yaml
from starwhale.consts import (
    HTTPMethod,
    DefaultYAMLName,
    SW_TMP_DIR_NAME,
    D_FILE_VOLUME_SIZE,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import BundleType, DatasetChangeMode, DatasetFolderSourceType
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.data_type import Link, MIMEType, GrayscaleImage
from starwhale.base.uri.project import Project
from starwhale.core.dataset.cli import _list as list_cli
from starwhale.core.dataset.cli import _build as build_cli
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.view import (
    DatasetTermView,
    DatasetTermViewJson,
    DatasetTermViewRich,
)
from starwhale.core.dataset.model import Dataset, DatasetConfig, StandaloneDataset
from starwhale.base.models.dataset import LocalDatasetInfo, LocalDatasetInfoBase
from starwhale.base.client.models.models import (
    DatasetVo,
    DatasetInfoVo,
    DatasetVersionVo,
)

_dataset_data_dir = f"{ROOT_DIR}/data/dataset"
_dataset_yaml = open(f"{_dataset_data_dir}/dataset.yaml").read()


class StandaloneDatasetTestCase(BaseTestCase):
    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.api._impl.dataset.model.Dataset.commit")
    @patch("starwhale.api._impl.dataset.model.Dataset.__setitem__")
    def test_function_handler_make_swds(
        self, m_setitem: MagicMock, m_commit: MagicMock, *args: t.Any
    ) -> None:
        name = "mnist"
        dataset_uri = Resource(name, typ=ResourceType.dataset)
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

    def test_no_support_type_for_build_handler(self) -> None:
        name = "no-support"
        dataset_uri = Resource(name, typ=ResourceType.dataset)

        def _iter_rows() -> t.Generator:
            for _ in range(0, 5):
                yield {"a": {1: "a", b"b": "b"}}

        sd = StandaloneDataset(dataset_uri)
        with self.assertRaisesRegex(
            RuntimeError,
            "RowPutThread raise exception: json like dict shouldn't have none-str keys 1",
        ):
            sd.build(config=DatasetConfig(name=name, handler=_iter_rows))

    @patch("starwhale.core.dataset.cli.import_object")
    def test_build_from_yaml(self, m_import: MagicMock) -> None:
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
                "--yaml",
                str(yaml_path),
                "--tag",
                "t0",
                "--tag=t2",
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build.call_count == 1
        call_args = mock_obj.build.call_args[0]
        assert call_args[1].name == "mnist"
        assert m_import.call_args[0][1] == "dataset:build"
        assert mock_obj.build.call_args[1]["tags"] == ("t0", "t2")

        new_workdir = "/tmp/workdir-new"
        ensure_dir(new_workdir)
        new_yaml_path = Path(new_workdir) / "dataset-new.yaml"
        yaml_path.rename(new_yaml_path)
        assert new_yaml_path.exists() and not yaml_path.exists()

        mock_obj.reset_mock()
        m_import.reset_mock()
        result = runner.invoke(
            build_cli,
            ["-f", os.path.join(new_workdir, "dataset-new.yaml"), "-t", "t3"],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build.call_count == 1
        assert call_args[1].name == "mnist"
        assert m_import.call_args[0][1] == "dataset:build"
        assert mock_obj.build.call_args[1]["tags"] == ("t3",)

    @patch("starwhale.api._impl.data_store.LocalDataStore.dump")
    def test_build_from_image_folder(self, m_dump: MagicMock) -> None:
        image_folder = Path("/tmp/workdir/images")
        ensure_file(image_folder / "1.jpg", "1", parents=True)
        ensure_file(image_folder / "1.txt", "1", parents=True)
        ensure_file(image_folder / "dog" / "1.jpg", "1", parents=True)
        ensure_file(image_folder / "cat" / "2.jpg", "2", parents=True)

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                "--name",
                "image-folder-test",
                "--image-folder",
                str(image_folder),
                "--auto-label",
                "--patch",
                "--tag=t0",
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build_from_folder.call_count == 1
        call_args = mock_obj.build_from_folder.call_args
        assert call_args[1]["name"] == "image-folder-test"
        assert call_args[1]["mode"] == DatasetChangeMode.PATCH
        assert call_args[0][0] == image_folder
        assert call_args[0][1] == DatasetFolderSourceType.IMAGE

        DatasetTermView.build_from_folder(
            folder=image_folder,
            kind=DatasetFolderSourceType.IMAGE,
            name="image-folder-test",
            project_uri="",
            auto_label=True,
            alignment_size="128",
            volume_size="128M",
        )
        assert m_dump.call_count == 1

    @patch("starwhale.api._impl.dataset.model.Dataset.from_csv")
    def test_build_from_csv(self, m_csv: MagicMock) -> None:
        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                "--name",
                "csv-test",
                "--csv",
                "test.csv",
                "--csv",
                "test2.csv",
                "-c",
                "/path/to/dir",
                "--csv=http://example.com/test.csv",
                "--dialect=excel-tab",
                "--strict",
                "--encoding=utf-8",
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build_from_csv_files.call_count == 1
        call_args = mock_obj.build_from_csv_files.call_args
        assert call_args
        assert len(call_args[0][0]) == 4
        assert call_args[1]["name"] == "csv-test"
        assert call_args[1]["dialect"] == "excel-tab"
        assert call_args[1]["encoding"] == "utf-8"

        DatasetTermView.build_from_csv_files(
            paths=["test.csv"], name="csv-test-file", project_uri=""
        )
        assert m_csv.call_count == 1
        assert m_csv.call_args[1]["path"] == ["test.csv"]

    @patch("starwhale.api._impl.dataset.model.Dataset.from_huggingface")
    def test_build_from_huggingface(self, m_hf: MagicMock) -> None:
        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                "--name",
                "huggingface-test",
                "--huggingface",
                "mnist",
                "--no-cache",
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build_from_huggingface.call_count == 1
        call_args = mock_obj.build_from_huggingface.call_args
        assert call_args
        assert call_args[0][0] == "mnist"
        assert call_args[1]["name"] == "huggingface-test"
        assert len(call_args[1]["subsets"]) == 0
        assert not call_args[1]["cache"]

        DatasetTermView.build_from_huggingface(
            repo="mnist",
            name="huggingface-test",
            project_uri="self",
            alignment_size="128",
            volume_size="128M",
            subsets=["sub1"],
            split="train",
            revision="main",
            cache=True,
        )
        assert m_hf.call_count == 1
        assert m_hf.call_args[1]["cache"]

    def test_build_from_json_file_local(self) -> None:
        json_file = Path("/tmp/workdir/json.json")
        content = json.dumps(
            {
                "sub": {
                    "sub": [
                        {"image": "1.jpg", "label": "dog"},
                        {"image": "2.jpg", "label": "cat"},
                    ]
                }
            }
        )
        ensure_file(json_file, content, parents=True)

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                "--name",
                "json-file-test",
                "--json",
                str(json_file),
                "--field-selector",
                "sub.sub",
                "--overwrite",
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build_from_json_files.call_count == 1
        call_args = mock_obj.build_from_json_files.call_args
        assert call_args[1]["name"] == "json-file-test"
        assert call_args[1]["mode"] == DatasetChangeMode.OVERWRITE
        assert call_args[1]["field_selector"] == "sub.sub"
        assert str(json_file) in call_args[0][0]

        DatasetTermView.build_from_json_files(
            paths=[json_file],
            name="json-file-test",
            project_uri="",
            alignment_size="128",
            volume_size="128M",
            field_selector="sub.sub",
            mode=DatasetChangeMode.OVERWRITE,
        )

    @Mocker()
    def test_build_from_json_file_http_url(self, rm: Mocker) -> None:
        url = "http://example.com/dataset.json"
        rm.request(
            HTTPMethod.GET,
            url,
            json=[
                {"image": "1.jpg", "label": "dog"},
                {"image": "2.jpg", "label": "cat"},
            ],
        )
        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                "--name",
                "json-file-test",
                "--json",
                url,
            ],
            obj=mock_obj,
        )
        assert result.exit_code == 0
        assert mock_obj.build_from_json_files.call_count == 1
        call_args = mock_obj.build_from_json_files.call_args
        assert call_args[1]["name"] == "json-file-test"
        assert call_args[1]["field_selector"] == ""
        assert url in call_args[0][0]

        DatasetTermView.build_from_json_files(
            paths=[url],
            name="json-file-test",
            project_uri="",
            alignment_size="128",
            volume_size="128M",
        )

    @patch("starwhale.core.dataset.cli.import_object")
    def test_build_from_handler(self, m_import: MagicMock) -> None:
        handler_func = lambda: 1
        m_import.return_value = handler_func
        workdir = "/tmp/workdir"
        ensure_dir(workdir)

        mock_obj = MagicMock()
        runner = CliRunner()
        result = runner.invoke(
            build_cli,
            [
                "--name",
                "mnist",
                "--python-handler",
                "dataset:buildFunction",
                "--workdir",
                workdir,
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

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.api._impl.data_store.LocalDataStore.dump")
    def test_build_workflow(self, *args: t.Any) -> None:
        class _MockBuildExecutor:
            def __iter__(self) -> t.Generator:
                yield {"data": b"", "label": 1}

        sw = SWCliConfigMixed()

        workdir = self.local_storage
        name = "mnist"

        ensure_dir(os.path.join(workdir, "data"))
        ensure_file(os.path.join(workdir, "mnist.py"), " ")

        config = DatasetConfig(**yaml.safe_load(_dataset_yaml))
        config.handler = _MockBuildExecutor
        dataset_uri = Resource(name, typ=ResourceType.dataset)
        sd = StandaloneDataset(dataset_uri)
        sd.build(workdir=Path(workdir), config=config, tags=["t0", "t1"])
        build_version = sd.uri.version

        snapshot_workdir = (
            sw.rootdir
            / "self"
            / dataset_uri.typ.value
            / name
            / build_version[:VERSION_PREFIX_CNT]
            / f"{build_version}{BundleType.DATASET}"
        )

        assert snapshot_workdir.exists()

        _manifest = load_yaml(snapshot_workdir / DEFAULT_MANIFEST_NAME)
        assert _manifest["version"] == build_version
        assert "name" not in _manifest

        dataset_uri = Resource(
            f"mnist/version/{build_version}", typ=ResourceType.dataset
        )
        sd = StandaloneDataset(dataset_uri)
        _info = sd.info()

        assert isinstance(_info, LocalDatasetInfo)
        assert _info.version == build_version
        assert _info.name == name
        assert str(Path(_info.path).resolve()) == str(snapshot_workdir.resolve())

        tags = sd.tag.list()
        assert set(tags) == {"t0", "t1", "latest", "v0"}

        _list, _ = StandaloneDataset.list(Project(""))
        assert len(_list) == 1
        item = _list[0]
        assert isinstance(item, LocalDatasetInfoBase)
        assert not item.is_removed

        dataset_uri = Resource(
            f"mnist/version/{build_version}", typ=ResourceType.dataset
        )
        sd = StandaloneDataset(dataset_uri)
        _ok, _ = sd.remove(False)
        assert _ok

        _list, _ = StandaloneDataset.list(Project(""))
        assert len(_list) == 1
        item = _list[0]
        assert isinstance(item, LocalDatasetInfoBase)
        assert item.is_removed

        _ok, _ = sd.recover(True)
        _list, _ = StandaloneDataset.list(Project(""))
        assert len(_list) == 1
        item = _list[0]
        assert isinstance(item, LocalDatasetInfoBase)
        assert not item.is_removed

        DatasetTermView(name).info()
        DatasetTermViewJson(dataset_uri).info()
        DatasetTermView(name).history()
        DatasetTermView(dataset_uri).summary()
        fname = f"{name}/version/{build_version}"
        DatasetTermView(fname).info()
        DatasetTermView(fname).history()
        DatasetTermView(fname).remove()
        DatasetTermView(fname).recover()
        DatasetTermView.list()

        sd.remove(True)
        _list, _ = StandaloneDataset.list(Project(""))
        assert len(_list) == 0

        config.project_uri = "self"
        DatasetTermView.build(workdir, config)

        # make sure tmp dir is empty
        assert len(os.listdir(sw.rootdir / SW_TMP_DIR_NAME)) == 0

    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    @patch("starwhale.api._impl.data_store.LocalDataStore.dump")
    def test_head(self, *args: t.Any) -> None:
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
        ds = Dataset.get_dataset(Resource(dataset_uri, typ=ResourceType.dataset))

        results = ds.head(0)
        assert len(results) == 0

        results = ds.head(1)
        assert results[0].index == "label-0"
        assert results[0].features["label"] == 0
        assert results[0].features["img"].mime_type == MIMEType.GRAYSCALE
        assert len(results[0].features["img"].to_bytes()) == 3
        assert len(results) == 1

        results = ds.head(5)
        assert len(results) == 2
        DatasetTermView(dataset_uri).head(1)
        DatasetTermView(dataset_uri).head(2)
        DatasetTermView(dataset_uri).head(2, show_types=True)
        DatasetTermViewJson(dataset_uri).head(1)
        DatasetTermViewJson(dataset_uri).head(2)


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

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    @patch("starwhale.core.dataset.model.CloudDataset.list")
    @patch("starwhale.core.dataset.model.CloudDataset.info")
    def test_info_list_view(
        self, rm: Mocker, mock_info: MagicMock, mock_list: MagicMock, m_conf: MagicMock
    ) -> None:
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "example": {
                    "uri": "http://example.com",
                    "current_project": "starwhale",
                    "sw_token": "token",
                },
            },
            "storage": {"root": "/root"},
        }
        rm.get(
            "http://example.com/api/v1/project/starwhale",
            json={"data": {"id": 1, "name": "starwhale"}},
        )
        dataset_uri = Resource(
            uri="cloud://example/project/starwhale/dataset/mnist/version/123",
            refine=False,
        )
        ds_info = DatasetInfoVo(
            id="1",
            name="mnist",
            version_id="2",
            version_name="123",
            shared=False,
            created_time=123,
            version_meta="meta",
        )
        mock_info.return_value = ds_info

        DatasetTermView(dataset_uri).info()
        DatasetTermViewJson(dataset_uri).info()
        ds = DatasetVo(
            id="1",
            name="mnist",
            created_time=1,
            version=DatasetVersionVo(
                latest=True,
                id="2",
                name="mnist",
                created_time=2,
                tags=[],
                shared=False,
            ),
        )
        mock_list.return_value = ([ds], {})
        project_uri = "example/project/starwhale"
        DatasetTermView.list(project_uri)
        DatasetTermViewRich.list(project_uri)
        DatasetTermViewJson.list(project_uri)
