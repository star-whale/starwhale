import json
from http import HTTPStatus
from unittest.mock import patch, MagicMock

import yaml
from requests_mock import Mocker
from requests_mock.exceptions import NoMockAddress
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import get_predefined_config_yaml
from starwhale.utils import config as sw_config
from starwhale.consts import (
    HTTPMethod,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
    ARCHIVED_SWDS_META_FNAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.config import SWCliConfigMixed, get_swcli_config_path
from starwhale.core.model.copy import ModelCopy
from starwhale.base.bundle_copy import FileDesc, BundleCopy
from starwhale.core.dataset.copy import DatasetCopy
from starwhale.core.dataset.store import DatasetStorage

_existed_config_contents = get_predefined_config_yaml()


class TestBundleCopy(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        sw_config._config = {}
        path = get_swcli_config_path()
        self.fs.create_file(path, contents=_existed_config_contents)
        self._sw_config = SWCliConfigMixed()
        self._sw_config.select_current_default("local", "self")

    @Mocker()
    def test_runtime_copy_c2l(self, rm: Mocker) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/myproject/runtime/pytorch/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/runtime/pytorch/version/{version}/file",
            content=b"pytorch content",
        )

        cloud_uri = (
            f"cloud://pre-bare/project/myproject/runtime/pytorch/version/{version}"
        )

        cases = [
            {
                "dest_uri": "pytorch-alias",
                "dest_local_project_uri": None,
                "path": "self/runtime/pytorch-alias",
            },
            {
                "dest_uri": "pytorch-alias",
                "dest_local_project_uri": "myproject",
                "path": "myproject/runtime/pytorch-alias",
            },
            {
                "dest_uri": ".",
                "dest_local_project_uri": None,
                "path": "self/runtime/pytorch",
            },
            {
                "dest_uri": ".",
                "dest_local_project_uri": "myproject",
                "path": "myproject/runtime/pytorch",
            },
            {
                "dest_uri": "local/project/self/pytorch-new-alias",
                "dest_local_project_uri": None,
                "path": "self/runtime/pytorch-new-alias",
            },
        ]

        for case in cases:
            swrt_path = (
                self._sw_config.rootdir / case["path"] / version[:2] / f"{version}.swrt"
            )
            assert not swrt_path.exists()
            BundleCopy(
                src_uri=cloud_uri,
                dest_uri=case["dest_uri"],
                typ=URIType.RUNTIME,
                dest_local_project_uri=case["dest_local_project_uri"],
            ).do()
            assert swrt_path.is_file()

        with self.assertRaises(Exception):
            BundleCopy(
                src_uri=cloud_uri,
                dest_uri="local/project/self/pytorch-new-alias",
                typ=URIType.RUNTIME,
                dest_local_project_uri="myproject",
            ).do()

    @Mocker()
    def test_runtime_copy_l2c(self, rm: Mocker) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        swrt_path = (
            self._sw_config.rootdir
            / "self"
            / "runtime"
            / "mnist"
            / version[:2]
            / f"{version}.swrt"
        )
        tag_manifest_path = (
            self._sw_config.rootdir / "self" / "runtime" / "mnist" / "_manifest.yaml"
        )
        ensure_dir(swrt_path.parent)
        ensure_file(swrt_path, "")
        ensure_file(
            tag_manifest_path,
            yaml.safe_dump(
                {
                    "fast_tag_seq": 0,
                    "name": "mnist",
                    "typ": "runtime",
                    "tags": {"latest": version, "v1": version},
                    "versions": {version: {"latest": True, "v1": True}},
                }
            ),
        )

        cases = [
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"local/project/self/runtime/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": "mnist",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version[:5]}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"mnist/{version[:5]}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias",
                "dest_runtime": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/version/123",
                "dest_runtime": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/123",
                "dest_runtime": "mnist-new-alias",
            },
        ]

        for case in cases:
            head_request = rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/runtime/{case['dest_runtime']}/version/{version}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/runtime/{case['dest_runtime']}/version/{version}/file",
            )
            BundleCopy(
                src_uri=case["src_uri"], dest_uri=case["dest_uri"], typ=URIType.RUNTIME
            ).do()
            assert head_request.call_count == 1
            assert upload_request.call_count == 1

        # TODO: support the flowing case
        with self.assertRaises(NoMockAddress):
            head_request = rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/runtime/mnist-alias/version/{version}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/runtime/mnist-alias/version/{version}/file",
            )
            BundleCopy(
                src_uri="mnist/v1",
                dest_uri="cloud://pre-bare/project/mnist/runtime/mnist-alias",
                typ=URIType.RUNTIME,
            ).do()

    @Mocker()
    @patch("starwhale.core.model.copy.load_yaml")
    @patch("starwhale.core.model.copy.extract_tar")
    def test_model_copy_c2l(
        self, rm: Mocker, m_load_yaml: MagicMock, m_extract: MagicMock
    ) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}/file?part_name=",
            headers={
                "X-SW-DOWNLOAD-TYPE": FileDesc.MANIFEST.name,
                "X-SW-DOWNLOAD-OBJECT-NAME": "_manifest.yaml",
                "X-SW-DOWNLOAD-OBJECT-HASH": "",
            },
            json={"resources": []},
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}/file?part_name=",
            headers={
                "X-SW-DOWNLOAD-TYPE": FileDesc.SRC_TAR.name,
                "X-SW-DOWNLOAD-OBJECT-NAME": "src.tar",
                "X-SW-DOWNLOAD-OBJECT-HASH": "",
            },
            content=b"mnist model content",
        )
        # m_load_yaml.return_value = {"resources": []}

        cloud_uri = f"cloud://pre-bare/project/myproject/model/mnist/version/{version}"

        cases = [
            {
                "dest_uri": "mnist-alias",
                "dest_local_project_uri": None,
                "path": "self/model/mnist-alias",
            },
            {
                "dest_uri": "mnist-alias",
                "dest_local_project_uri": "myproject",
                "path": "myproject/model/mnist-alias",
            },
            {
                "dest_uri": ".",
                "dest_local_project_uri": None,
                "path": "self/model/mnist",
            },
            {
                "dest_uri": ".",
                "dest_local_project_uri": "myproject",
                "path": "myproject/model/mnist",
            },
            {
                "dest_uri": "local/project/self/mnist-new-alias",
                "dest_local_project_uri": None,
                "path": "self/model/mnist-new-alias",
            },
        ]

        for case in cases:
            swmp_path = (
                self._sw_config.rootdir / case["path"] / version[:2] / f"{version}.swmp"
            )
            swmp_manifest_path = swmp_path / "_manifest.yaml"
            assert not swmp_path.exists()
            assert not swmp_manifest_path.exists()
            ModelCopy(
                src_uri=cloud_uri,
                dest_uri=case["dest_uri"],
                typ=URIType.MODEL,
                dest_local_project_uri=case["dest_local_project_uri"],
            ).do()
            assert swmp_path.exists()
            assert swmp_path.is_dir()
            assert swmp_manifest_path.exists()
            assert swmp_manifest_path.is_file()

        with self.assertRaises(Exception):
            ModelCopy(
                src_uri=cloud_uri,
                dest_uri="local/project/self/mnist-new-alias",
                typ=URIType.MODEL,
                dest_local_project_uri="myproject",
            ).do()

    @Mocker()
    def test_model_copy_l2c(self, rm: Mocker) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        swmp_path = (
            self._sw_config.rootdir
            / "self"
            / "model"
            / "mnist"
            / version[:2]
            / f"{version}.swmp"
        )
        swmp_manifest_path = swmp_path / "_manifest.yaml"
        swmp_src_tar_path = swmp_path / "src.tar"
        tag_manifest_path = (
            self._sw_config.rootdir / "self" / "model" / "mnist" / "_manifest.yaml"
        )
        ensure_dir(swmp_path)
        ensure_file(swmp_src_tar_path, "")
        ensure_file(
            swmp_manifest_path,
            yaml.safe_dump(
                {
                    "resources": [],
                    "version": "ge3tkylgha2tenrtmftdgyjzni3dayq",
                }
            ),
        )
        ensure_file(
            tag_manifest_path,
            yaml.safe_dump(
                {
                    "fast_tag_seq": 0,
                    "name": "mnist",
                    "typ": "model",
                    "tags": {"latest": version, "v1": version},
                    "versions": {version: {"latest": True, "v1": True}},
                }
            ),
        )

        cases = [
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"local/project/self/model/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": "mnist",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version[:5]}",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"mnist/{version[:5]}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias",
                "dest_model": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/version/123",
                "dest_model": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/123",
                "dest_model": "mnist-new-alias",
            },
        ]

        for case in cases:
            head_request = rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/model/{case['dest_model']}/version/{version}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/model/{case['dest_model']}/version/{version}/file",
                headers={"X-SW-UPLOAD-TYPE": FileDesc.MANIFEST.name},
                json={"data": {"upload_id": "123"}},
            )
            ModelCopy(
                src_uri=case["src_uri"], dest_uri=case["dest_uri"], typ=URIType.MODEL
            ).do()
            assert head_request.call_count == 1
            assert upload_request.call_count == 3

        # TODO: support the flowing case
        with self.assertRaises(NoMockAddress):
            head_request = rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/model/mnist-alias/version/{version}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/model/mnist-alias/version/{version}/file",
            )
            ModelCopy(
                src_uri="mnist/v1",
                dest_uri="cloud://pre-bare/project/mnist/model/mnist-alias",
                typ=URIType.MODEL,
            ).do()

    @Mocker()
    @patch("starwhale.core.dataset.copy.TabularDataset.scan")
    def test_dataset_copy_c2l(self, rm: Mocker, m_td_scan: MagicMock) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/myproject/dataset/mnist/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/dataset/mnist/version/{version}/file?part_name=",
            headers={
                "X-SW-DOWNLOAD-TYPE": FileDesc.MANIFEST.name,
                "X-SW-DOWNLOAD-OBJECT-NAME": "_manifest.yaml",
                "X-SW-DOWNLOAD-OBJECT-HASH": "",
            },
            json={
                "signature": [],
            },
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/datastore/scanTable",
            status_code=HTTPStatus.OK,
            json={"data": {"records": []}},
        )

        cloud_uri = (
            f"cloud://pre-bare/project/myproject/dataset/mnist/version/{version}"
        )

        cases = [
            {
                "dest_uri": "mnist-alias",
                "dest_local_project_uri": None,
                "path": "self/dataset/mnist-alias",
            },
            {
                "dest_uri": "mnist-alias",
                "dest_local_project_uri": "myproject",
                "path": "myproject/dataset/mnist-alias",
            },
            {
                "dest_uri": ".",
                "dest_local_project_uri": None,
                "path": "self/dataset/mnist",
            },
            {
                "dest_uri": ".",
                "dest_local_project_uri": "myproject",
                "path": "myproject/dataset/mnist",
            },
            {
                "dest_uri": "local/project/self/mnist-new-alias",
                "dest_local_project_uri": None,
                "path": "self/dataset/mnist-new-alias",
            },
        ]

        for case in cases:
            swds_path = (
                self._sw_config.rootdir / case["path"] / version[:2] / f"{version}.swds"
            )
            swds_manifest_path = swds_path / "_manifest.yaml"
            assert not swds_path.exists()
            assert not swds_manifest_path.exists()
            DatasetCopy(
                src_uri=cloud_uri,
                dest_uri=case["dest_uri"],
                typ=URIType.DATASET,
                dest_local_project_uri=case["dest_local_project_uri"],
            ).do()
            assert swds_path.exists()
            assert swds_path.is_dir()
            assert swds_manifest_path.exists()
            assert swds_manifest_path.is_file()

        with self.assertRaises(Exception):
            DatasetCopy(
                src_uri=cloud_uri,
                dest_uri="local/project/self/mnist-new-alias",
                typ=URIType.DATASET,
                dest_local_project_uri="myproject",
            ).do()

    @Mocker()
    @patch("starwhale.core.dataset.copy.TabularDataset.scan")
    def test_dataset_copy_l2c(self, rm: Mocker, m_td_scan: MagicMock) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        swds_path = (
            self._sw_config.rootdir
            / "self"
            / "dataset"
            / "mnist"
            / version[:2]
            / f"{version}.swds"
        )
        tag_manifest_path = (
            self._sw_config.rootdir / "self" / "dataset" / "mnist" / "_manifest.yaml"
        )
        hash_name = "27a43c91b7a1a9a9c8e51b1d796691dd"
        ensure_dir(swds_path)
        ensure_file(swds_path / ARCHIVED_SWDS_META_FNAME, " ")
        ensure_file(
            swds_path / DEFAULT_MANIFEST_NAME,
            json.dumps(
                {"signature": [f"1:{DatasetStorage.object_hash_algo}:{hash_name}"]}
            ),
        )
        ensure_dir(swds_path / "data")
        data_path = DatasetStorage._get_object_store_path(hash_name)
        ensure_dir(data_path.parent)
        ensure_file(data_path, "")

        ensure_file(
            tag_manifest_path,
            yaml.safe_dump(
                {
                    "fast_tag_seq": 0,
                    "name": "mnist",
                    "typ": "dataset",
                    "tags": {"latest": version, "v1": version},
                    "versions": {version: {"latest": True, "v1": True}},
                }
            ),
        )

        cases = [
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_dataset": "mnist",
            },
            {
                "src_uri": f"local/project/self/dataset/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
            },
            {
                "src_uri": "mnist",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_dataset": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version[:5]}",
                "dest_uri": "cloud://pre-bare/mnist",
                "dest_dataset": "mnist",
            },
            {
                "src_uri": f"mnist/{version[:5]}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias",
                "dest_dataset": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/version/123",
                "dest_dataset": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/123",
                "dest_dataset": "mnist-new-alias",
            },
        ]

        for case in cases:
            head_request = rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{case['dest_dataset']}/version/{version}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{case['dest_dataset']}/version/{version}/file",
                json={"data": {"upload_id": 1}},
            )
            DatasetCopy(
                src_uri=case["src_uri"], dest_uri=case["dest_uri"], typ=URIType.DATASET
            ).do()
            assert head_request.call_count == 1
            assert upload_request.call_count == 3

        # TODO: support the flowing case
        with self.assertRaises(NoMockAddress):
            head_request = rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/mnist-alias/version/{version}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/mnist-alias/version/{version}/file",
                json={"data": {"upload_id": 1}},
            )
            BundleCopy(
                src_uri="mnist/v1",
                dest_uri="cloud://pre-bare/project/mnist/dataset/mnist-alias",
                typ=URIType.DATASET,
            ).do()

    @Mocker()
    def test_upload_bundle_file(self, rm: Mocker) -> None:
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1:8182/api/v1/project/project/runtime/mnist/version/abcdefg1234",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/project/project/runtime/mnist/version/abcdefg1234/file",
        )

        runtime_dir = self._sw_config.rootdir / "self" / "runtime" / "mnist" / "ab"
        version = "abcdefg1234"
        ensure_dir(runtime_dir)
        ensure_file(runtime_dir / f"{version}.swrt", " ")

        bc = BundleCopy(
            src_uri=f"mnist/version/{version[:5]}",
            dest_uri="cloud://pre-bare/project/",
            typ=URIType.RUNTIME,
        )

        _v = bc._guess_bundle_version()
        assert _v == version
        bc.do()

    @Mocker()
    def test_download_bundle_file(self, rm: Mocker) -> None:
        version = "112233"
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/1/runtime/mnist/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/1/runtime/mnist/version/{version}/file",
            content=b"test",
        )

        dest_dir = (
            self._sw_config.rootdir
            / "self"
            / "runtime"
            / "mnist"
            / f"{version[:VERSION_PREFIX_CNT]}"
        )
        ensure_dir(dest_dir)

        bc = BundleCopy(
            src_uri=f"cloud://pre-bare/project/1/runtime/mnist/version/{version}",
            dest_uri="",
            local_project_uri="self",
            typ=URIType.RUNTIME,
        )
        bc.do()
        swrt_path = dest_dir / f"{version}.swrt"

        assert swrt_path.exists()
        assert swrt_path.read_bytes() == b"test"
        st = StandaloneTag(
            URI(
                f"mnist/version/{version}",
                expected_type=URIType.RUNTIME,
            )
        )
        assert st.list() == ["v0"]

    @Mocker()
    @patch("starwhale.core.dataset.copy.TabularDataset.scan")
    def test_upload_bundle_dir(self, rm: Mocker, m_td_scan: MagicMock) -> None:
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1:8182/api/v1/project/project/dataset/mnist/version/abcde",
            json={"message": "already existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/project/project/dataset/mnist/version/abcde/file",
            json={"data": {"upload_id": 1}},
        )

        dataset_dir = (
            self._sw_config.rootdir / "self" / "dataset" / "mnist" / "ab" / "abcde.swds"
        )
        ensure_dir(dataset_dir)

        hash_name = "27a43c91b7a1a9a9c8e51b1d796691dd"
        ensure_file(dataset_dir / ARCHIVED_SWDS_META_FNAME, " ")
        ensure_file(
            dataset_dir / DEFAULT_MANIFEST_NAME,
            json.dumps(
                {"signature": [f"1:{DatasetStorage.object_hash_algo}:{hash_name}"]}
            ),
        )
        ensure_dir(dataset_dir / "data")
        data_path = DatasetStorage._get_object_store_path(hash_name)
        ensure_dir(data_path.parent)
        ensure_file(data_path, "")

        bc = DatasetCopy(
            src_uri="mnist/version/abcde",
            dest_uri="cloud://pre-bare/project/",
            typ=URIType.DATASET,
            force=True,
        )
        bc.do()

    @Mocker()
    @patch("starwhale.core.dataset.copy.TabularDataset.scan")
    def test_download_bundle_dir(self, rm: Mocker, m_td_scan: MagicMock) -> None:
        hash_name1 = "bfa8805ddc2d43df098e43832c24e494ad"
        hash_name2 = "f954056e4324495ae5bec4e8e5e6d18f1b"
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1:8182/api/v1/project/1/dataset/mnist/version/latest",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/1/dataset/mnist/version/latest/file",
            json={
                "signature": [
                    f"1:{DatasetStorage.object_hash_algo}:{hash_name1}",
                    f"2:{DatasetStorage.object_hash_algo}:{hash_name2}",
                ]
            },
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/datastore/scanTable",
            status_code=HTTPStatus.OK,
            json={"data": {"records": []}},
        )

        bc = DatasetCopy(
            src_uri="cloud://pre-bare/project/1/dataset/mnist/version/latest",
            dest_uri="",
            local_project_uri="self",
            typ=URIType.DATASET,
        )
        bc.do()

        dataset_dir = (
            self._sw_config.rootdir
            / "self"
            / "dataset"
            / "mnist"
            / "la"
            / "latest.swds"
        )
        assert (dataset_dir / DEFAULT_MANIFEST_NAME).exists()
        assert (dataset_dir / ARCHIVED_SWDS_META_FNAME).exists()

        link1 = dataset_dir / "data" / hash_name1[: DatasetStorage.short_sign_cnt]
        link2 = dataset_dir / "data" / hash_name2[: DatasetStorage.short_sign_cnt]

        assert link1.is_symlink() and link2.is_symlink()
        assert link1.resolve() == DatasetStorage._get_object_store_path(hash_name1)
        assert link2.resolve() == DatasetStorage._get_object_store_path(hash_name2)
