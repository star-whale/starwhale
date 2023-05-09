import json
import typing as t
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
    RESOURCE_FILES_NAME,
    DEFAULT_MANIFEST_NAME,
    ARCHIVED_SWDS_META_FNAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import DatasetChangeMode
from starwhale.utils.config import SWCliConfigMixed, get_swcli_config_path
from starwhale.core.model.copy import ModelCopy
from starwhale.base.bundle_copy import FileDesc, BundleCopy
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.copy import DatasetCopy
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import TabularDatasetRow

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
    @patch("starwhale.base.uri.resource.Resource.refine_local_rc_info")
    def test_runtime_copy_c2l(self, rm: Mocker, *args: t.Any) -> None:
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
                typ=ResourceType.runtime,
                dest_local_project_uri=case["dest_local_project_uri"],
            ).do()
            assert swrt_path.is_file()

        with self.assertRaises(Exception):
            BundleCopy(
                src_uri=cloud_uri,
                dest_uri="local/project/self/pytorch-new-alias",
                typ=ResourceType.runtime,
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
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "http://1.1.1.1:8182/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"local/project/self/runtime/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": "mnist",
                "dest_uri": "cloud://pre-bare/project/mnist",
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
                "src_uri": f"mnist/{version[:5]}",
                "dest_uri": "pre-bare/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": f"mnist/{version[:5]}",
                "dest_uri": "http://1.1.1.1:8182/project/mnist",
                "dest_runtime": "mnist",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias",
                "dest_runtime": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/version/foo",
                "dest_runtime": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/foo",
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
                src_uri=case["src_uri"],
                dest_uri=case["dest_uri"],
                typ=ResourceType.runtime,
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
                typ=ResourceType.runtime,
            ).do()

    @Mocker()
    @patch("starwhale.core.model.copy.load_yaml")
    @patch("starwhale.core.model.copy.extract_tar")
    @patch("starwhale.base.uri.resource.Resource.refine_local_rc_info")
    def test_model_copy_c2l(self, rm: Mocker, *args: MagicMock) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}/file?desc=MANIFEST&partName=_manifest.yaml&signature=",
            json={"resources": []},
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}/file?desc=SRC_TAR&partName=src.tar&signature=",
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
                typ=ResourceType.model,
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
                typ=ResourceType.model,
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
                    "version": "ge3tkylgha2tenrtmftdgyjzni3dayq",
                }
            ),
            parents=True,
        )

        ensure_file(
            swmp_path / "src" / ".starwhale" / RESOURCE_FILES_NAME,
            yaml.safe_dump([]),
            parents=True,
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
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"local/project/self/model/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"local/project/self/model/mnist/version/{version}",
                "dest_uri": "pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": "mnist",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": "mnist",
                "dest_uri": "http://1.1.1.1:8182/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_model": "mnist",
            },
            {
                "src_uri": f"mnist/version/{version[:5]}",
                "dest_uri": "cloud://pre-bare/project/mnist",
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
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/version/foo",
                "dest_model": "mnist-new-alias",
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/foo",
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
                json={"data": {"uploadId": "123"}},
            )
            ModelCopy(
                src_uri=case["src_uri"],
                dest_uri=case["dest_uri"],
                typ=ResourceType.model,
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
                typ=ResourceType.model,
            ).do()

    def _prepare_local_dataset(self) -> t.Union[str, str]:
        name = "mnist"
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        swds_path = (
            self._sw_config.rootdir
            / "self"
            / "dataset"
            / name
            / version[:2]
            / f"{version}.swds"
        )
        tag_manifest_path = (
            self._sw_config.rootdir / "self" / "dataset" / name / "_manifest.yaml"
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
                    "name": name,
                    "typ": "dataset",
                    "tags": {"latest": version, "v1": version},
                    "versions": {version: {"latest": True, "v1": True}},
                }
            ),
        )
        return name, version

    @Mocker()
    @patch("starwhale.core.dataset.copy.TabularDataset.scan")
    @patch("starwhale.base.uri.resource.Resource.refine_local_rc_info")
    def test_dataset_copy_c2l(self, rm: Mocker, *args: MagicMock) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/myproject",
            json={"data": {"id": 1, "name": "myproject"}},
        )
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/myproject/dataset/mnist/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/dataset/mnist/version/{version}/file?desc=MANIFEST&partName=_manifest.yaml&signature=",
            json={
                "signature": [],
            },
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/dataset/mnist/version/{version}/file?desc=SRC_TAR&partName=archive.swds_meta&signature=",
            content=b"mnist dataset content",
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/datastore/scanTable",
            status_code=HTTPStatus.OK,
            json={"data": {"records": []}},
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/myproject/dataset/mnist?versionUrl={version}",
            json={"data": {"versionMeta": yaml.safe_dump({"version": version})}},
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
                dest_local_project_uri="myproject",
            ).do()

    @Mocker()
    @patch("starwhale.base.uri.resource.Resource.refine_local_rc_info")
    @patch("starwhale.core.dataset.copy.TabularDataset.put")
    @patch("starwhale.core.dataset.copy.TabularDataset.scan")
    @patch("starwhale.core.dataset.copy.TabularDataset.delete")
    def test_dataset_copy_mode(
        self,
        rm: Mocker,
        m_td_delete: MagicMock,
        m_td_scan: MagicMock,
        *args: MagicMock,
    ) -> None:
        name, version = self._prepare_local_dataset()

        m_td_scan.return_value = [
            TabularDatasetRow(id=1, features={"a": "1", "b": "2", "c": "3"})
        ]

        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/{name}",
            json={"data": {"id": 1, "name": name}},
        )
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{name}/version/{version}",
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.POST,
            f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{name}/version/{version}/file",
            json={"data": {"uploadId": 1}},
        )
        src_uri = f"local/project/self/mnist/version/{version}"
        dest_uri = "cloud://pre-bare/project/mnist"

        head_request = rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{name}",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )
        DatasetCopy(src_uri, dest_uri, force=True).do()
        assert head_request.call_count == 0
        assert not m_td_delete.called

        head_request = rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{name}",
            status_code=HTTPStatus.OK,
        )
        DatasetCopy(src_uri, dest_uri, mode=DatasetChangeMode.PATCH, force=True).do()

        assert head_request.call_count == 0
        assert not m_td_delete.called

        DatasetCopy(
            src_uri, dest_uri, mode=DatasetChangeMode.OVERWRITE, force=True
        ).do()
        assert head_request.call_count == 1
        assert m_td_delete.called

    @Mocker()
    @patch("starwhale.core.dataset.copy.TabularDataset.scan")
    def test_dataset_copy_l2c(self, rm: Mocker, *args: MagicMock) -> None:
        _, version = self._prepare_local_dataset()

        cases = [
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.PATCH,
                "head_call_count": 0,
            },
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "pre-bare/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.PATCH,
                "head_call_count": 0,
            },
            {
                "src_uri": f"local/project/self/mnist/version/{version}",
                "dest_uri": "http://1.1.1.1:8182/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.PATCH,
                "head_call_count": 0,
            },
            {
                "src_uri": "mnist",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.OVERWRITE,
                "head_call_count": 1,
            },
            {
                "src_uri": "mnist",
                "dest_uri": "pre-bare/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.OVERWRITE,
                "head_call_count": 1,
            },
            {
                "src_uri": f"mnist/version/{version}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.OVERWRITE,
                "head_call_count": 1,
            },
            {
                "src_uri": f"mnist/version/{version[:5]}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.OVERWRITE,
                "head_call_count": 1,
            },
            {
                "src_uri": f"mnist/{version[:5]}",
                "dest_uri": "cloud://pre-bare/project/mnist",
                "dest_dataset": "mnist",
                "mode": DatasetChangeMode.OVERWRITE,
                "head_call_count": 1,
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias",
                "dest_dataset": "mnist-new-alias",
                "mode": DatasetChangeMode.OVERWRITE,
                "head_call_count": 1,
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "pre-bare/project/mnist/mnist-new-alias",
                "dest_dataset": "mnist-new-alias",
                "mode": DatasetChangeMode.PATCH,
                "head_call_count": 0,
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "http://1.1.1.1:8182/project/mnist/mnist-new-alias",
                "dest_dataset": "mnist-new-alias",
                "mode": DatasetChangeMode.PATCH,
                "head_call_count": 0,
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/version/123",
                "dest_dataset": "mnist-new-alias",
                "mode": DatasetChangeMode.PATCH,
                "head_call_count": 0,
            },
            {
                "src_uri": "mnist/v1",
                "dest_uri": "cloud://pre-bare/project/mnist/mnist-new-alias/123",
                "dest_dataset": "mnist-new-alias",
                "mode": DatasetChangeMode.PATCH,
                "head_call_count": 0,
            },
        ]

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/mnist",
            json={"data": {"id": 1, "name": "mnist"}},
        )
        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/mnist/dataset/mnist-new-alias?versionUrl=123",
            json={"data": {"id": 2, "name": "mnist-new-alias"}},
        )

        for case in cases:
            rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{case['dest_dataset']}/version/{version}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            head_request = rm.request(
                HTTPMethod.HEAD,
                f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{case['dest_dataset']}",
                json={"message": "not found"},
                status_code=HTTPStatus.NOT_FOUND,
            )
            upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/dataset/{case['dest_dataset']}/version/{version}/file",
                json={"data": {"uploadId": 1}},
            )
            try:
                DatasetCopy(
                    src_uri=case["src_uri"],
                    dest_uri=case["dest_uri"],
                    mode=case["mode"],
                ).do()
            except Exception as e:
                print(f"case: {case}")
                raise e

            assert head_request.call_count == case["head_call_count"]
            assert upload_request.call_count == 2

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
                json={"data": {"uploadId": 1}},
            )
            BundleCopy(
                src_uri="mnist/v1",
                dest_uri="cloud://pre-bare/project/mnist/dataset/mnist-alias",
                typ=ResourceType.dataset,
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
            typ=ResourceType.runtime,
        )

        bc.do()

    @Mocker()
    @patch("starwhale.base.uri.resource.Resource.refine_local_rc_info")
    def test_download_bundle_file(self, rm: Mocker, *args: t.Any) -> None:
        version = "112233"
        version_name = "runtime-version"
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/1/runtime/mnist?versionUrl={version}",
            json={"data": {"id": 1, "name": "mnist", "versionName": version_name}},
        )
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/1/runtime/mnist/version/{version_name}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/1/runtime/mnist/version/{version_name}/file",
            content=b"test",
        )

        dest_dir = (
            self._sw_config.rootdir
            / "self"
            / "runtime"
            / "mnist"
            / f"{version_name[:VERSION_PREFIX_CNT]}"
        )
        ensure_dir(dest_dir)

        bc = BundleCopy(
            src_uri=f"cloud://pre-bare/project/1/runtime/mnist/version/{version}",
            dest_uri="",
            dest_local_project_uri="self",
            typ=ResourceType.runtime,
        )
        bc.do()
        swrt_path = dest_dir / f"{version_name}.swrt"

        assert swrt_path.exists()
        assert swrt_path.read_bytes() == b"test"
        st = StandaloneTag(
            Resource(
                f"mnist/version/{version_name}",
                typ=ResourceType.runtime,
            )
        )
        assert st.list() == ["latest", "v0"]
