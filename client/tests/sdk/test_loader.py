import os
import shutil
from unittest.mock import patch, MagicMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale import (
    MIMEType,
    S3LinkAuth,
    get_data_loader,
    SWDSBinDataLoader,
    UserRawDataLoader,
)
from starwhale.consts import AUTH_ENV_FNAME, SWDSBackendType
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, DataFormatType, DataOriginType, ObjectStoreType
from starwhale.core.dataset.type import Image, ArtifactType, DatasetSummary
from starwhale.core.dataset.store import (
    DatasetStorage,
    S3StorageBackend,
    LocalFSStorageBackend,
)
from starwhale.core.dataset.tabular import TabularDatasetRow

from .. import ROOT_DIR


class TestDataLoader(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.dataset_uri = URI("mnist/version/1122334455667788", URIType.DATASET)
        self.swds_dir = os.path.join(ROOT_DIR, "data", "dataset", "swds")
        self.fs.add_real_directory(self.swds_dir)

    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_user_raw_local_store(
        self, m_scan: MagicMock, m_summary: MagicMock
    ) -> None:
        m_summary.return_value = DatasetSummary(
            include_user_raw=True,
            include_link=False,
        )
        loader = get_data_loader(self.dataset_uri)
        assert isinstance(loader, UserRawDataLoader)

        fname = "data"
        m_scan.return_value = [
            TabularDatasetRow(
                id=0,
                object_store_type=ObjectStoreType.LOCAL,
                data_uri=fname,
                data_offset=16,
                data_size=784,
                annotations={"label": 0},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.UNDEFINED,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="",
            )
        ]

        raw_data_fpath = os.path.join(ROOT_DIR, "data", "dataset", "mnist", "data")
        self.fs.add_real_file(raw_data_fpath)
        data_dir = DatasetStorage(self.dataset_uri).data_dir
        ensure_dir(data_dir)
        shutil.copy(raw_data_fpath, str(data_dir / fname))

        assert loader._stores == {}

        rows = list(loader)
        assert len(rows) == 1

        _idx, _data, _annotations = rows[0]
        assert _idx == 0
        assert _annotations["label"] == 0

        assert len(_data.to_bytes()) == 28 * 28
        assert isinstance(_data, Image)

        assert loader.kind == DataFormatType.USER_RAW
        assert list(loader._stores.keys()) == ["local."]
        assert loader._stores["local."].bucket == str(data_dir)
        assert loader._stores["local."].backend.kind == SWDSBackendType.LocalFS
        assert not loader._stores["local."].key_prefix

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.store.boto3.resource")
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_user_raw_remote_store(
        self,
        m_scan: MagicMock,
        m_summary: MagicMock,
        m_boto3: MagicMock,
    ) -> None:
        m_summary.return_value = DatasetSummary(
            include_user_raw=True,
            include_link=True,
        )

        snapshot_workdir = DatasetStorage(self.dataset_uri).snapshot_workdir
        ensure_dir(snapshot_workdir)
        envs = {
            "USER.S3.SERVER1.SECRET": "11",
            "USER.S3.SERVER1.ACCESS_KEY": "11",
            "USER.S3.SERVER2.SECRET": "11",
            "USER.S3.SERVER2.ACCESS_KEY": "11",
            "USER.S3.SERVER2.ENDPOINT": "127.0.0.1:19000",
        }
        os.environ.update(envs)
        auth_env = S3LinkAuth.from_env(name="server1").dump_env()
        auth_env.extend(S3LinkAuth.from_env(name="server2").dump_env())
        ensure_file(
            snapshot_workdir / AUTH_ENV_FNAME,
            content="\n".join(auth_env),
        )

        for k in envs:
            os.environ.pop(k)

        loader = get_data_loader(self.dataset_uri)
        assert isinstance(loader, UserRawDataLoader)
        assert loader.kind == DataFormatType.USER_RAW
        for k in envs:
            assert k in os.environ

        version = "1122334455667788"

        m_scan.return_value = [
            TabularDatasetRow(
                id=0,
                object_store_type=ObjectStoreType.REMOTE,
                data_uri=f"s3://127.0.0.1:9000@starwhale/project/2/dataset/11/{version}",
                data_offset=16,
                data_size=784,
                annotations={"label": 0},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.USER_RAW,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="server1",
            ),
            TabularDatasetRow(
                id=1,
                object_store_type=ObjectStoreType.REMOTE,
                data_uri=f"s3://127.0.0.1:19000@starwhale/project/2/dataset/11/{version}",
                data_offset=16,
                data_size=784,
                annotations={"label": 1},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.USER_RAW,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="server2",
            ),
            TabularDatasetRow(
                id=2,
                object_store_type=ObjectStoreType.REMOTE,
                data_uri=f"s3://starwhale/project/2/dataset/11/{version}",
                data_offset=16,
                data_size=784,
                annotations={"label": 1},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.USER_RAW,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="server2",
            ),
            TabularDatasetRow(
                id=3,
                object_store_type=ObjectStoreType.REMOTE,
                data_uri=f"s3://username:password@127.0.0.1:29000@starwhale/project/2/dataset/11/{version}",
                data_offset=16,
                data_size=784,
                annotations={"label": 1},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.USER_RAW,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="server3",
            ),
        ]

        raw_data_fpath = os.path.join(ROOT_DIR, "data", "dataset", "mnist", "data")
        self.fs.add_real_file(raw_data_fpath)
        with open(raw_data_fpath, "rb") as f:
            raw_content = f.read(-1)

        m_boto3.return_value = MagicMock(
            **{
                "Object.return_value": MagicMock(
                    **{
                        "get.return_value": {
                            "Body": MagicMock(**{"read.return_value": raw_content}),
                            "ContentLength": len(raw_content),
                        }
                    }
                )
            }
        )

        assert loader.kind == DataFormatType.USER_RAW
        assert loader._stores == {}

        rows = list(loader)
        assert len(rows) == 4

        _idx, _data, _annotations = rows[0]
        assert _idx == 0
        assert _annotations["label"] == 0
        assert isinstance(_data, Image)

        assert len(_data.to_bytes()) == 28 * 28
        assert isinstance(_data.to_bytes(), bytes)
        assert len(loader._stores) == 3
        assert loader._stores["remote.server1"].backend.kind == SWDSBackendType.S3
        assert loader._stores["remote.server1"].bucket == "starwhale"

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.store.boto3.resource")
    @patch("starwhale.core.dataset.model.CloudDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_swds_bin_s3(
        self, m_scan: MagicMock, m_summary: MagicMock, m_boto3: MagicMock
    ) -> None:
        m_summary.return_value = DatasetSummary(
            include_user_raw=False,
            include_link=False,
        )
        version = "1122334455667788"
        dataset_uri = URI(
            f"http://127.0.0.1:1234/project/self/dataset/mnist/version/{version}",
            expected_type=URIType.DATASET,
        )
        loader = get_data_loader(dataset_uri)
        assert isinstance(loader, SWDSBinDataLoader)
        assert loader.kind == DataFormatType.SWDS_BIN

        fname = "data_ubyte_0.swds_bin"
        m_scan.return_value = [
            TabularDatasetRow(
                id=0,
                object_store_type=ObjectStoreType.LOCAL,
                data_uri=fname,
                data_offset=32,
                data_size=784,
                _swds_bin_offset=0,
                _swds_bin_size=8160,
                annotations={"label": 0},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.SWDS_BIN,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="",
            )
        ]
        os.environ.update(
            {
                "SW_S3_BUCKET": "starwhale",
                "SW_OBJECT_STORE_KEY_PREFIX": f"project/self/dataset/mnist/version/11/{version}",
                "SW_S3_ENDPOINT": "starwhale.mock:9000",
                "SW_S3_ACCESS_KEY": "foo",
                "SW_S3_SECRET": "bar",
            }
        )

        with open(os.path.join(self.swds_dir, fname), "rb") as f:
            swds_content = f.read(-1)

        m_boto3.return_value = MagicMock(
            **{
                "Object.return_value": MagicMock(
                    **{
                        "get.return_value": {
                            "Body": MagicMock(**{"read.return_value": swds_content}),
                            "ContentLength": len(swds_content),
                        }
                    }
                )
            }
        )
        assert loader._stores == {}

        rows = list(loader)
        assert len(rows) == 1
        _idx, _data, _annotations = rows[0]
        assert _idx == 0
        assert _annotations["label"] == 0

        assert len(_data.to_bytes()) == 10 * 28 * 28
        assert isinstance(_data, Image)

        assert list(loader._stores.keys()) == ["local."]
        backend = loader._stores["local."].backend
        assert isinstance(backend, S3StorageBackend)
        assert backend.kind == SWDSBackendType.S3
        assert backend.s3.Object.call_args[0] == (
            "starwhale",
            f"project/self/dataset/mnist/version/11/{version}/{fname}",
        )

        assert loader._stores["local."].bucket == "starwhale"
        assert (
            loader._stores["local."].key_prefix
            == f"project/self/dataset/mnist/version/11/{version}"
        )

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_swds_bin_local_fs(self, m_scan: MagicMock, m_summary: MagicMock) -> None:
        m_summary.return_value = DatasetSummary(
            include_user_raw=False,
            include_link=False,
            rows=2,
            increased_rows=2,
        )
        loader = get_data_loader(self.dataset_uri)
        assert isinstance(loader, SWDSBinDataLoader)
        assert loader.kind == DataFormatType.SWDS_BIN

        fname = "data_ubyte_0.swds_bin"
        m_scan.return_value = [
            TabularDatasetRow(
                id=0,
                object_store_type=ObjectStoreType.LOCAL,
                data_uri=fname,
                data_offset=32,
                data_size=784,
                _swds_bin_offset=0,
                _swds_bin_size=8160,
                annotations={"label": 0},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.SWDS_BIN,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="",
            ),
            TabularDatasetRow(
                id=1,
                object_store_type=ObjectStoreType.LOCAL,
                data_uri=fname,
                data_offset=32,
                data_size=784,
                _swds_bin_offset=0,
                _swds_bin_size=8160,
                annotations={"label": 1},
                data_origin=DataOriginType.NEW,
                data_format=DataFormatType.SWDS_BIN,
                data_type={
                    "type": ArtifactType.Image.value,
                    "mime_type": MIMEType.GRAYSCALE.value,
                },
                auth_name="",
            ),
        ]

        data_dir = DatasetStorage(self.dataset_uri).data_dir
        ensure_dir(data_dir)
        shutil.copyfile(os.path.join(self.swds_dir, fname), str(data_dir / fname))
        assert loader._stores == {}

        rows = list(loader)
        assert len(rows) == 2

        _idx, _data, _annotations = rows[0]

        assert _idx == 0
        assert _annotations["label"] == 0

        assert isinstance(_data, Image)
        assert len(_data.to_bytes()) == 7840
        assert isinstance(_data.to_bytes(), bytes)

        assert list(loader._stores.keys()) == ["local."]
        backend = loader._stores["local."].backend
        assert isinstance(backend, LocalFSStorageBackend)
        assert backend.kind == SWDSBackendType.LocalFS
        assert loader._stores["local."].bucket == str(data_dir)
        assert not loader._stores["local."].key_prefix
