import json
from http import HTTPStatus

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import config as sw_config
from starwhale.consts import HTTPMethod, DEFAULT_MANIFEST_NAME
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType
from starwhale.utils.config import SWCliConfigMixed, get_swcli_config_path
from starwhale.base.bundle_copy import BundleCopy
from starwhale.core.dataset.dataset import ARCHIVE_SWDS_META

from .. import get_predefined_config_yaml

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
            "http://1.1.1.1:8182/api/v1/project/model",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/project/model/push",
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
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1:8182/api/v1/project/model",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/model/pull",
            content=b"test",
        )

        dest_dir = self._sw_config.rootdir / "self" / "model" / "mnist" / "la"
        ensure_dir(dest_dir)

        bc = BundleCopy(
            src_uri="cloud://pre-bare/project/1/model/mnist/version/latest",
            dest_uri="self",
            typ=URIType.MODEL,
        )
        bc.do()
        swmp_path = dest_dir / "latest.swmp"

        assert swmp_path.exists()
        assert swmp_path.read_bytes() == b"test"

    @Mocker()
    def test_upload_bundle_dir(self, rm: Mocker) -> None:
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1:8182/api/v1/project/dataset",
            json={"message": "already existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.POST,
            "http://1.1.1.1:8182/api/v1/project/dataset/push",
            json={"data": {"upload_id": 1}},
        )

        dataset_dir = (
            self._sw_config.rootdir / "self" / "dataset" / "mnist" / "ab" / "abcde.swds"
        )
        ensure_dir(dataset_dir)

        ensure_file(dataset_dir / DEFAULT_MANIFEST_NAME, " ")
        ensure_file(
            dataset_dir / ARCHIVE_SWDS_META, json.dumps({"signature": ["1", "2"]})
        )
        ensure_dir(dataset_dir / "data")
        ensure_file(dataset_dir / "data" / "1", " ")
        ensure_file(dataset_dir / "data" / "2", " ")

        bc = BundleCopy(
            src_uri="mnist/version/abcde",
            dest_uri="cloud://pre-bare/project/",
            typ=URIType.DATASET,
            force=True,
        )
        bc.do()

    @Mocker()
    def test_download_bundle_dir(self, rm: Mocker) -> None:
        rm.request(
            HTTPMethod.HEAD,
            "http://1.1.1.1:8182/api/v1/project/dataset",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/dataset/pull",
            json={"signature": ["1", "2"]},
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
        assert (dataset_dir / ARCHIVE_SWDS_META).exists()
        assert (dataset_dir / "data" / "1").exists()
        assert (dataset_dir / "data" / "2").exists()
