import json
from http import HTTPStatus

from requests_mock import Mocker
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
from starwhale.base.bundle_copy import BundleCopy
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
    def test_upload_bundle_file(self, rm: Mocker) -> None:
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1:8182/api/v1/project/project/model/mnist/version/abcdefg1234",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/project/project/model/mnist/version/abcdefg1234/file",
        )

        model_dir = self._sw_config.rootdir / "self" / "model" / "mnist" / "ab"
        version = "abcdefg1234"
        ensure_dir(model_dir)
        ensure_file(model_dir / f"{version}.swmp", " ")

        bc = BundleCopy(
            src_uri=f"mnist/version/{version[:5]}",
            dest_uri="cloud://pre-bare/project/",
            typ=URIType.MODEL,
        )

        _v = bc._guess_bundle_version()
        assert _v == version
        bc.do()

    @Mocker()
    def test_download_bundle_file(self, rm: Mocker) -> None:
        version = "112233"
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/1/model/mnist/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            f"http://1.1.1.1:8182/api/v1/project/1/model/mnist/version/{version}/file",
            content=b"test",
        )

        dest_dir = (
            self._sw_config.rootdir
            / "self"
            / "model"
            / "mnist"
            / f"{version[:VERSION_PREFIX_CNT]}"
        )
        ensure_dir(dest_dir)

        bc = BundleCopy(
            src_uri=f"cloud://pre-bare/project/1/model/mnist/version/{version}",
            dest_uri="self",
            typ=URIType.MODEL,
        )
        bc.do()
        swmp_path = dest_dir / f"{version}.swmp"

        assert swmp_path.exists()
        assert swmp_path.read_bytes() == b"test"
        st = StandaloneTag(
            URI(
                f"mnist/version/{version}",
                expected_type=URIType.MODEL,
            )
        )
        assert st.list() == ["v0"]

    @Mocker()
    def test_upload_bundle_dir(self, rm: Mocker) -> None:
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

        bc = BundleCopy(
            src_uri="mnist/version/abcde",
            dest_uri="cloud://pre-bare/project/",
            typ=URIType.DATASET,
            force=True,
        )
        bc.do()

    @Mocker()
    def test_download_bundle_dir(self, rm: Mocker) -> None:
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
        bc = BundleCopy(
            src_uri="cloud://pre-bare/project/1/dataset/mnist/version/latest",
            dest_uri="self",
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
