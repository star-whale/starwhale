import os
import shutil
import typing as t
import tempfile
from unittest.mock import patch, MagicMock

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import ROOT_DIR
from starwhale.utils import config
from starwhale.consts import HTTPMethod, SWDSBackendType
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import URIType
from starwhale.consts.env import SWEnv
from starwhale.utils.error import ParameterError
from starwhale.core.dataset.type import Link, Image, DatasetSummary, GrayscaleImage
from starwhale.core.dataset.store import (
    ObjectStore,
    S3Connection,
    DatasetStorage,
    SignedUrlBackend,
    LocalFSStorageBackend,
)
from starwhale.api._impl.data_store import RemoteDataStore
from starwhale.core.dataset.tabular import (
    StandaloneTDSC,
    TabularDatasetRow,
    get_dataset_consumption,
)
from starwhale.api._impl.dataset.loader import DataRow, DataLoader, get_data_loader


class TestDataLoader(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.dataset_uri = URI("mnist/version/1122334455667788", URIType.DATASET)
        self.swds_dir = os.path.join(ROOT_DIR, "data", "dataset", "swds")
        self.fs.add_real_directory(self.swds_dir)

    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.wrapper.Dataset.scan_id")
    def test_range_match(self, m_scan_id: MagicMock, m_summary: MagicMock) -> None:
        m_summary.return_value = DatasetSummary(rows=1)
        m_scan_id.return_value = [{"id": "path/0"}]
        consumption = get_dataset_consumption(
            self.dataset_uri,
            session_id="10",
            session_start="path/0",
            session_end=None,
        )
        with self.assertRaises(ParameterError):
            get_data_loader(self.dataset_uri, session_consumption=consumption)

        with self.assertRaises(ParameterError):
            get_data_loader(
                self.dataset_uri, session_consumption=consumption, start="path/1"
            )

        with self.assertRaises(ParameterError):
            get_data_loader(
                self.dataset_uri, session_consumption=consumption, end="path/1"
            )

    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.wrapper.Dataset.scan_id")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_user_raw_local_store(
        self, m_scan: MagicMock, m_scan_id: MagicMock, m_summary: MagicMock
    ) -> None:
        m_summary.return_value = DatasetSummary(rows=1)
        m_scan_id.return_value = [{"id": "path/0"}]

        consumption = get_dataset_consumption(self.dataset_uri, session_id="1")
        loader = get_data_loader(self.dataset_uri, session_consumption=consumption)
        assert isinstance(loader, DataLoader)
        assert isinstance(loader.session_consumption, StandaloneTDSC)

        fname = "data"
        m_scan.return_value = [
            TabularDatasetRow(
                features={
                    "image": GrayscaleImage(
                        link=Link(
                            fname,
                            offset=32,
                            size=784,
                            _swds_bin_offset=0,
                            _swds_bin_size=8160,
                        )
                    ),
                    "label": 0,
                },
                id="path/0",
            )
        ]

        raw_data_fpath = os.path.join(ROOT_DIR, "data", "dataset", "mnist", "data")
        self.fs.add_real_file(raw_data_fpath)
        data_dir = DatasetStorage(self.dataset_uri).data_dir
        ensure_dir(data_dir)
        shutil.copy(raw_data_fpath, str(data_dir / fname))

        ObjectStore._stores = {}

        rows = list(loader)
        assert len(rows) == 1

        _idx, _data = rows[0]
        assert _idx == "path/0"
        assert _data["label"] == 0

        assert len(_data["image"].to_bytes()) == 28 * 28
        assert isinstance(_data["image"], Image)

        assert list(ObjectStore._stores.keys()) == [
            "local/project/self/dataset/mnist/version/1122334455667788."
        ]
        assert ObjectStore._stores[
            "local/project/self/dataset/mnist/version/1122334455667788."
        ].bucket == str(data_dir)
        assert (
            ObjectStore._stores[
                "local/project/self/dataset/mnist/version/1122334455667788."
            ].backend.kind
            == SWDSBackendType.LocalFS
        )
        assert not ObjectStore._stores[
            "local/project/self/dataset/mnist/version/1122334455667788."
        ].key_prefix

        loader = get_data_loader("mnist/version/1122334455667788")
        assert isinstance(loader, DataLoader)
        assert loader.session_consumption is None
        rows = list(loader)
        assert len(rows) == 1

        _idx, _data = rows[0]
        assert _idx == "path/0"
        assert _data["label"] == 0

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.store.boto3.resource")
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.wrapper.Dataset.scan_id")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_user_raw_remote_store(
        self,
        m_scan: MagicMock,
        m_scan_id: MagicMock,
        m_summary: MagicMock,
        m_boto3: MagicMock,
    ) -> None:
        with tempfile.TemporaryDirectory() as tmpdirname:
            config._config = {}
            os.environ["SW_CLI_CONFIG"] = tmpdirname + "/config.yaml"
            m_summary.return_value = DatasetSummary(rows=4)
            m_scan_id.return_value = [{"id": i} for i in range(0, 4)]

            snapshot_workdir = DatasetStorage(self.dataset_uri).snapshot_workdir
            ensure_dir(snapshot_workdir)
            config.update_swcli_config(
                **{
                    "link_auths": [
                        {
                            "type": "s3",
                            "ak": "11",
                            "sk": "11",
                            "bucket": "starwhale",
                            "endpoint": "http://127.0.0.1:9000",
                        },
                        {
                            "type": "s3",
                            "ak": "11",
                            "sk": "11",
                            "endpoint": "http://127.0.0.1:19000",
                            "bucket": "starwhale",
                        },
                        {
                            "type": "s3",
                            "ak": "11",
                            "sk": "11",
                            "endpoint": "http://127.0.0.1",
                            "bucket": "starwhale",
                        },
                    ]
                }
            )
            S3Connection.connections_config = []

            consumption = get_dataset_consumption(self.dataset_uri, session_id="2")
            loader = get_data_loader(self.dataset_uri, session_consumption=consumption)
            assert isinstance(loader, DataLoader)
            assert isinstance(loader.session_consumption, StandaloneTDSC)
            assert loader.session_consumption._todo_queue.qsize() == 1

            version = "1122334455667788"

            m_scan.return_value = [
                TabularDatasetRow(
                    features={
                        "image": GrayscaleImage(
                            link=Link(
                                f"s3://127.0.0.1:9000/starwhale/project/2/dataset/11/{version}",
                                offset=16,
                                size=784,
                            )
                        ),
                        "label": 0,
                    },
                    id=0,
                ),
                TabularDatasetRow(
                    features={
                        "image": GrayscaleImage(
                            link=Link(
                                f"s3://127.0.0.1:19000/starwhale/project/2/dataset/11/{version}",
                                offset=16,
                                size=784,
                            )
                        ),
                        "label": 1,
                    },
                    id=1,
                ),
                TabularDatasetRow(
                    features={
                        "image": GrayscaleImage(
                            link=Link(
                                f"s3://127.0.0.1/starwhale/project/2/dataset/11/{version}",
                                offset=16,
                                size=784,
                            )
                        ),
                        "label": 1,
                    },
                    id=2,
                ),
                TabularDatasetRow(
                    features={
                        "image": GrayscaleImage(
                            link=Link(
                                f"s3://username:password@127.0.0.1:29000/starwhale/project/2/dataset/11/{version}",
                                offset=16,
                                size=784,
                            )
                        ),
                        "label": 1,
                    },
                    id=3,
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

            ObjectStore._stores = {}

            rows = list(loader)
            assert len(rows) == 4
            assert {r[0] for r in rows} == set(range(4))

            _data = rows[0][1]
            assert isinstance(_data["label"], int)
            assert isinstance(_data["image"], Image)
            assert len(_data["image"].to_bytes()) == 28 * 28
            assert isinstance(_data["image"].to_bytes(), bytes)

            assert len(ObjectStore._stores) == 4
            assert (
                ObjectStore._stores[
                    "local/project/self/dataset/mnist/version/1122334455667788.s3://127.0.0.1/starwhale/"
                ].backend.kind
                == SWDSBackendType.S3
            )
            assert (
                ObjectStore._stores[
                    "local/project/self/dataset/mnist/version/1122334455667788.s3://127.0.0.1:9000/starwhale/"
                ].bucket
                == "starwhale"
            )

            loader = get_data_loader(self.dataset_uri)
            assert isinstance(loader, DataLoader)
            assert loader.session_consumption is None
            assert len(list(loader)) == 4

    @Mocker()
    @patch("starwhale.core.dataset.model.CloudDataset.summary")
    @patch("starwhale.api._impl.wrapper.Dataset.scan_id")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_swds_bin_s3(
        self,
        rm: Mocker,
        m_scan: MagicMock,
        m_scan_id: MagicMock,
        m_summary: MagicMock,
    ) -> None:
        rm.get(
            "http://127.0.0.1:1234/api/v1/project/self",
            json={"data": {"id": 1, "name": "project"}},
        )
        m_summary.return_value = DatasetSummary(rows=1)
        m_scan_id.return_value = [{"id": 0}]
        version = "1122334455667788"
        dataset_uri = URI(
            f"http://127.0.0.1:1234/project/self/dataset/mnist/version/{version}",
            expected_type=URIType.DATASET,
        )

        os.environ[SWEnv.instance_token] = "123"
        consumption = get_dataset_consumption(self.dataset_uri, session_id="5")
        loader = get_data_loader(dataset_uri, session_consumption=consumption)
        assert isinstance(loader, DataLoader)
        assert isinstance(loader.session_consumption, StandaloneTDSC)
        assert isinstance(
            loader.tabular_dataset._ds_wrapper._data_store, RemoteDataStore
        )

        fname = "data_ubyte_0.swds_bin"
        m_scan.return_value = [
            TabularDatasetRow(
                features={
                    "image": GrayscaleImage(
                        link=Link(
                            fname,
                            offset=32,
                            size=784,
                            _swds_bin_offset=0,
                            _swds_bin_size=8160,
                        )
                    ),
                    "label": 0,
                },
                id=0,
            )
        ]

        with open(os.path.join(self.swds_dir, fname), "rb") as f:
            swds_content = f.read(-1)

        signed_url = "http://minio/signed/path/file"
        rm.post(
            "http://127.0.0.1:1234/api/v1/project/self/dataset/mnist/uri/sign-links",
            json={"data": {fname: signed_url}},
        )
        rm.get(
            signed_url,
            content=swds_content,
        )

        ObjectStore._stores = {}

        rows = list(loader)
        assert len(rows) == 1
        _idx, _data = rows[0]
        assert _idx == 0
        assert _data["label"] == 0

        assert len(_data["image"].to_bytes()) == 28 * 28
        assert isinstance(_data["image"], Image)

        assert list(ObjectStore._stores.keys()) == [
            "http://127.0.0.1:1234/project/self/dataset/mnist/version/1122334455667788."
        ]
        backend = ObjectStore._stores[
            "http://127.0.0.1:1234/project/self/dataset/mnist/version/1122334455667788."
        ].backend
        assert isinstance(backend, SignedUrlBackend)
        assert backend.kind == SWDSBackendType.SignedUrl

        assert (
            ObjectStore._stores[
                "http://127.0.0.1:1234/project/self/dataset/mnist/version/1122334455667788."
            ].bucket
            == ""
        )
        assert (
            ObjectStore._stores[
                "http://127.0.0.1:1234/project/self/dataset/mnist/version/1122334455667788."
            ].key_prefix
            == ""
        )

    @patch.dict(os.environ, {})
    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_swds_bin_local_fs(self, m_scan: MagicMock, m_summary: MagicMock) -> None:
        m_summary.return_value = DatasetSummary(
            rows=2,
            increased_rows=2,
        )
        loader = get_data_loader(self.dataset_uri)
        assert isinstance(loader, DataLoader)

        fname = "data_ubyte_0.swds_bin"
        m_scan.return_value = [
            TabularDatasetRow(
                features={
                    "image": GrayscaleImage(
                        link=Link(
                            fname,
                            offset=32,
                            size=784,
                            _swds_bin_offset=0,
                            _swds_bin_size=8160,
                        )
                    ),
                    "label": 0,
                },
                id=0,
            ),
            TabularDatasetRow(
                features={
                    "image": GrayscaleImage(
                        link=Link(
                            fname,
                            offset=32,
                            size=784,
                            _swds_bin_offset=0,
                            _swds_bin_size=8160,
                        )
                    ),
                    "label": 1,
                },
                id=1,
            ),
        ]

        data_dir = DatasetStorage(self.dataset_uri).data_dir
        ensure_dir(data_dir)
        shutil.copyfile(os.path.join(self.swds_dir, fname), str(data_dir / fname))
        ObjectStore._stores = {}

        rows = list(loader)
        assert len(rows) == 2

        assert {rows[0][0], rows[1][0]} == {0, 1}

        _data = rows[0][1]
        assert isinstance(_data["label"], int)
        assert isinstance(_data["image"], Image)
        assert len(_data["image"].to_bytes()) == 784
        assert isinstance(_data["image"].to_bytes(), bytes)

        assert list(ObjectStore._stores.keys()) == [
            "local/project/self/dataset/mnist/version/1122334455667788."
        ]
        backend = ObjectStore._stores[
            "local/project/self/dataset/mnist/version/1122334455667788."
        ].backend
        assert isinstance(backend, LocalFSStorageBackend)
        assert backend.kind == SWDSBackendType.LocalFS
        assert ObjectStore._stores[
            "local/project/self/dataset/mnist/version/1122334455667788."
        ].bucket == str(data_dir)
        assert not ObjectStore._stores[
            "local/project/self/dataset/mnist/version/1122334455667788."
        ].key_prefix

    @Mocker()
    @patch.dict(os.environ, {"SW_TOKEN": "a", "SW_POD_NAME": "b"})
    @patch("starwhale.core.dataset.model.CloudDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan_batch")
    @patch("starwhale.core.dataset.tabular.TabularDatasetSessionConsumption")
    def test_remote_batch_sign(
        self,
        rm: Mocker,
        m_sc: MagicMock,
        m_scan_batch: MagicMock,
        m_summary: MagicMock,
    ) -> None:
        m_summary.return_value = DatasetSummary(rows=4)
        tdsc = m_sc()
        tdsc.get_scan_range.side_effect = [["a", "d"], None]
        tdsc.batch_size = 20
        tdsc.session_start = "a"
        tdsc.session_end = "d"
        dataset_uri = URI(
            "http://localhost/project/x/dataset/mnist/version/1122",
            URIType.DATASET,
        )
        m_scan_batch.return_value = [
            [
                TabularDatasetRow(
                    id="a",
                    features={
                        "image": Image(
                            link=Link(
                                "l11",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                        "label": Image(
                            link=Link(
                                "l1",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                    },
                ),
                TabularDatasetRow(
                    id="b",
                    features={
                        "image": Image(
                            link=Link(
                                "l12",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                        "label": Image(
                            link=Link(
                                "l2",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                    },
                ),
            ],
            [
                TabularDatasetRow(
                    id="c",
                    features={
                        "image": Image(
                            link=Link(
                                "l13",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                        "label": Image(
                            link=Link(
                                "l3",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                    },
                ),
                TabularDatasetRow(
                    id="d",
                    features={
                        "image": Image(
                            link=Link(
                                "l14",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                        "label": Image(
                            link=Link(
                                "l4",
                                offset=32,
                                size=784,
                                _swds_bin_offset=0,
                                _swds_bin_size=8160,
                            )
                        ),
                    },
                ),
            ],
        ]

        _uri_dict = {
            "l1": "http://l1/get-file",
            "l2": "http://l2/get-file",
            "l3": "http://l3/get-file",
            "l4": "http://l4/get-file",
            "l11": "http://l11/get-file",
            "l12": "http://l12/get-file",
            "l13": "http://l13/get-file",
            "l14": "http://l14/get-file",
        }

        raw_content = b"abcdefg"
        req_get_file = rm.register_uri(HTTPMethod.GET, "/get-file", content=raw_content)
        rm.post(
            "http://localhost/api/v1/project/x/dataset/mnist/uri/sign-links",
            json={"data": _uri_dict},
        )
        rm.get(
            "http://localhost/api/v1/project/x",
            json={"data": {"id": 1, "name": "x"}},
        )

        loader = get_data_loader(
            dataset_uri,
            start="a",
            end="d",
            session_consumption=tdsc,
            field_transformer={"image": "img"},
        )
        _label_uris_map = {}
        for _, data in loader:
            self.assertEqual(raw_content, data["img"].to_bytes())
            _label_uris_map[data["label"].link.uri] = data["label"].link._signed_uri
            _label_uris_map[data["img"].link.uri] = data["img"].link._signed_uri
            self.assertEqual(
                data["label"].link._signed_uri,
                _uri_dict.get(data["label"].link.uri),
            )
            self.assertEqual(
                data["img"].link._signed_uri,
                _uri_dict.get(data["img"].link.uri),
            )

        self.assertEqual(req_get_file.call_count, 8)
        self.assertEqual(len(_label_uris_map), 8)

    def test_data_row(self) -> None:
        dr = DataRow(index=1, features={"data": Image(), "label": 1})
        index, data = dr
        assert index == 1
        assert isinstance(data["data"], Image)
        assert data["label"] == 1
        assert dr[0] == 1
        assert len(dr) == 2

        dr_another = DataRow(index=2, features={"data": Image(), "label": 2})
        assert dr < dr_another
        assert dr != dr_another

        dr_third = DataRow(index=1, features={"data": Image(fp=b""), "label": 10})
        assert dr >= dr_third

    def test_data_row_exceptions(self) -> None:
        with self.assertRaises(TypeError):
            DataRow(index=b"", features=Image())  # type: ignore

        with self.assertRaises(TypeError):
            DataRow(index=1, features=b"")  # type: ignore

    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_loader_with_cache(self, m_scan: MagicMock, m_summary: MagicMock) -> None:
        rows_cnt = 100
        m_summary.return_value = DatasetSummary(rows=1)
        fname = "data_ubyte_0.swds_bin"
        m_scan.return_value = [
            TabularDatasetRow(
                id=i,
                features={
                    "l": Link(
                        fname,
                        offset=32,
                        size=784,
                        _swds_bin_offset=0,
                        _swds_bin_size=8160,
                    ),
                    "label": i,
                },
            )
            for i in range(0, rows_cnt)
        ]
        data_dir = DatasetStorage(self.dataset_uri).data_dir
        ensure_dir(data_dir)
        shutil.copyfile(os.path.join(self.swds_dir, fname), str(data_dir / fname))

        loader = get_data_loader(self.dataset_uri, cache_size=50, num_workers=4)
        assert len(list(loader)) == rows_cnt

        loader = get_data_loader(self.dataset_uri, cache_size=100, num_workers=10)
        assert len(list(loader)) == rows_cnt

        loader = get_data_loader(self.dataset_uri, cache_size=1, num_workers=1)
        assert len(list(loader)) == rows_cnt

        with self.assertRaisesRegex(ValueError, "must be a positive int number"):
            get_data_loader(self.dataset_uri, cache_size=0)

        with self.assertRaisesRegex(ValueError, "must be a positive int number"):
            get_data_loader(self.dataset_uri, num_workers=0)

    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_loader_with_scan_exception(
        self, m_scan: MagicMock, m_summary: MagicMock
    ) -> None:
        m_summary.return_value = DatasetSummary(
            rows=1,
            increased_rows=1,
        )

        def _scan_exception(*args: t.Any, **kwargs: t.Any) -> t.Any:
            raise RuntimeError("scan error")

        m_scan.side_effect = _scan_exception

        with self.assertRaisesRegex(RuntimeError, "scan error"):
            loader = get_data_loader(self.dataset_uri)
            [d.index for d in loader]

    @patch("starwhale.core.dataset.model.StandaloneDataset.summary")
    @patch("starwhale.api._impl.dataset.loader.TabularDataset.scan")
    def test_loader_with_makefile_exception(
        self, m_scan: MagicMock, m_summary: MagicMock
    ) -> None:
        m_summary.return_value = DatasetSummary(
            rows=1,
            increased_rows=1,
        )

        m_scan.return_value = [
            TabularDatasetRow(
                id=0,
                features={
                    "l": Image(
                        link=Link(
                            "not-found",
                            offset=32,
                            size=784,
                            _swds_bin_offset=0,
                            _swds_bin_size=8160,
                        )
                    ),
                    "label": 0,
                },
            )
        ]
        loader = get_data_loader(self.dataset_uri)
        with self.assertRaises(FileNotFoundError):
            [d.index for d in loader]
