import json
import random
import struct
import typing as t
import hashlib
import itertools
from http import HTTPStatus
from pathlib import Path
from unittest.mock import patch, MagicMock

import yaml
import httpx
import respx
import lz4.block
from requests_mock import Mocker
from google.protobuf import json_format

from tests import get_predefined_config_yaml
from starwhale.utils import config as sw_config
from starwhale.utils import NoSupportError
from starwhale.consts import (
    FileDesc,
    HTTPMethod,
    SW_BUILT_IN,
    VERSION_PREFIX_CNT,
    RESOURCE_FILES_NAME,
    DEFAULT_MANIFEST_NAME,
    ARCHIVED_SWDS_META_FNAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import DatasetChangeMode
from starwhale.proto_gen import model_package_storage_pb2 as pb2
from starwhale.utils.config import SWCliConfigMixed, get_swcli_config_path
from starwhale.base.bundle_copy import BundleCopy
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.copy import DatasetCopy
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import TabularDatasetRow

from .. import BaseTestCase

_existed_config_contents = get_predefined_config_yaml()


class _ModelServer:
    def __init__(self) -> None:
        self._models: t.Dict[str, t.Any] = {}
        self._blobs: t.Dict[str, bytes] = {}
        self._hashes: t.Dict[t.Tuple[str, int], str] = {}

    def _init_blob(self, md5: str, length: int) -> t.Tuple[str, str]:
        if (md5, length) in self._hashes:
            return "EXISTED", self._hashes[(md5, length)]
        return "OK", f"{random.getrandbits(64):016X}"

    def _upload_blob(self, blob_id: str, data: bytes) -> None:
        self._blobs[blob_id] = data

    def _complete_blob(self, blob_id: str) -> str:
        d = self._blobs[blob_id]
        return self._hashes.setdefault((hashlib.md5(d).hexdigest(), len(d)), blob_id)

    def serve(self, req: httpx.Request) -> httpx.Response:
        print(req)
        if (
            req.method == HTTPMethod.POST
            and req.headers["Content-Type"] == "application/json"
        ):
            print(req.content)
        if req.method == HTTPMethod.POST and req.url.path == "/api/v1/blob":
            data = json.loads(req.content)
            md5 = data["contentMd5"]
            length = int(data["contentLength"])
            status, blob_id = self._init_blob(md5, length)
            return httpx.Response(
                200,
                json={
                    "data": {
                        "status": status,
                        "blobId": blob_id,
                        "signedUrl": f"http://1.1.1.1:8182/uploadblob/{blob_id}",
                    }
                },
            )

        if req.method == HTTPMethod.PUT and req.url.path.startswith("/uploadblob/"):
            self._blobs[req.url.path[len("/uploadblob/") :]] = req.content
            return httpx.Response(200)

        if req.url.path.startswith("/api/v1/blob/"):
            blob_id = req.url.path[len("/api/v1/blob/") :]
            if req.method == HTTPMethod.GET:
                return httpx.Response(200, content=self._blobs[blob_id])
            if req.method == HTTPMethod.POST:
                return httpx.Response(
                    200, json={"data": {"blobId": self._complete_blob(blob_id)}}
                )

        if req.method == HTTPMethod.POST and req.url.path.endswith("/completeUpload"):
            self._models[
                req.url.path[len("/api/v1") : -len("/completeUpload")]
            ] = json.loads(req.content)
            return httpx.Response(200)

        if req.method == HTTPMethod.GET and req.url.path.endswith("/meta"):
            blob_id = req.url.params.get("blobId", "")
            if blob_id == "":
                blob_id = t.cast(
                    str,
                    self._models[req.url.path[len("/api/v1") : -len("/meta")]][
                        "metaBlobId"
                    ],
                )
            meta_blob = pb2.MetaBlob()
            meta_blob.ParseFromString(self._blobs[blob_id])
            for file in meta_blob.files:
                for id in file.blob_ids:
                    file.signed_urls.append(f"http://1.1.1.1:8182/api/v1/blob/{id}")
            return httpx.Response(
                200, json={"data": json_format.MessageToJson(meta_blob)}
            )

        raise RuntimeError(f"unhandled request: {req}")


class TestBundleCopy(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        sw_config._config = {}
        path = get_swcli_config_path()
        ensure_file(path, _existed_config_contents)
        self._sw_config = SWCliConfigMixed()
        self._sw_config.select_current_default("local", "self")
        self._model_server = _ModelServer()

    @Mocker()
    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
    def test_runtime_copy_c2l(self, rm: Mocker, *args: t.Any) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/myproject/runtime/pytorch",
            json={"data": {"id": 1, "versionName": version, "versionId": 100}},
            status_code=HTTPStatus.OK,
        )
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
        assert head_request.call_count == 1
        assert upload_request.call_count == 1

    @Mocker()
    @respx.mock
    def test_model_copy_c2l(self, rm: Mocker) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"

        cloud_uri = f"cloud://pre-bare/project/myproject/model/mnist/version/{version}"

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/myproject/model/mnist",
            json={"data": {"id": 1, "versionName": version, "versionId": 100}},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )

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

        meta_blobs = [pb2.MetaBlob()]
        meta_blobs[0].files.append(
            pb2.File(type=pb2.FILE_TYPE_DIRECTORY, from_file_index=1, to_file_index=4)
        )
        meta_blobs[0].files.append(
            pb2.File(
                type=pb2.FILE_TYPE_REGULAR,
                name="readme",
                size=6,
                permission=0o644,
                blob_ids=[""],
                blob_size=6,
                md5=bytes.fromhex("3905d7917f2b3429490b01cfb60d8f5b"),
            )
        )
        meta_blobs[0].data = b"readmet"
        meta_blobs[0].meta_blob_indexes.append(
            pb2.MetaBlobIndex(blob_id="0000000000000063", last_file_index=3)
        )
        meta_blobs.append(
            pb2.MetaBlob(
                files=[
                    pb2.File(
                        type=pb2.FILE_TYPE_HUGE,
                        name="big",
                        size=65536 * 2,
                        permission=0o600,
                        md5=hashlib.md5(
                            b"0" * 65536 + b"1" * 65536 + b"2" * 65536 + b"3" * 65536
                        ).digest(),
                        compression_algorithm=pb2.COMPRESSION_ALGORITHM_LZ4,
                        from_file_index=4,
                        to_file_index=5,
                    ),
                    pb2.File(
                        type=pb2.FILE_TYPE_DIRECTORY,
                        name="d",
                        permission=0o700,
                        from_file_index=5,
                        to_file_index=7,
                    ),
                    pb2.File(
                        blob_ids=["0000000000000064", "0000000000000065"],
                        signed_urls=["http://1.1.1.1/big1", "http://1.1.1.1/big2"],
                    ),
                    pb2.File(
                        type=pb2.FILE_TYPE_REGULAR,
                        name="readme",
                        size=6,
                        permission=0o644,
                        blob_ids=[""],
                        blob_size=6,
                        md5=bytes.fromhex("3905d7917f2b3429490b01cfb60d8f5b"),
                    ),
                    pb2.File(
                        type=pb2.FILE_TYPE_REGULAR,
                        name="t",
                        size=1,
                        permission=0o644,
                        blob_ids=[""],
                        blob_offset=6,
                        blob_size=1,
                        md5=bytes.fromhex("e358efa489f58062f10dd7316b65649e"),
                    ),
                ]
            )
        )
        respx.get(
            url__eq=f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}/meta"
        ).mock(
            return_value=httpx.Response(
                200,
                json={"data": json_format.MessageToJson(meta_blobs[0])},
            )
        )
        respx.get(
            url__eq=f"http://1.1.1.1:8182/api/v1/project/myproject/model/mnist/version/{version}/meta?blobId=0000000000000063"
        ).mock(
            return_value=httpx.Response(
                200,
                json={"data": json_format.MessageToJson(meta_blobs[1])},
            )
        )

        def compress_chunk(b: bytes) -> t.Any:
            d = lz4.block.compress(b, store_size=False)
            return struct.pack(">H", len(d)) + d

        respx.get("http://1.1.1.1/big1").mock(
            return_value=httpx.Response(
                200, content=compress_chunk(b"0" * 65536) + compress_chunk(b"1" * 65536)
            )
        )
        respx.get("http://1.1.1.1/big2").mock(
            return_value=httpx.Response(
                200, content=compress_chunk(b"2" * 65536) + compress_chunk(b"3" * 65536)
            )
        )
        for case in cases:
            swmp_path = (
                self._sw_config.rootdir / case["path"] / version[:2] / f"{version}.swmp"
            )
            swmp_manifest_path = swmp_path / "_manifest.yaml"
            assert not swmp_path.exists()
            assert not swmp_manifest_path.exists()
            BundleCopy(
                src_uri=cloud_uri,
                dest_uri=case["dest_uri"],
                typ=ResourceType.model,
                dest_local_project_uri=case["dest_local_project_uri"],
            ).do()
            assert swmp_path.exists()
            assert swmp_path.is_dir()
            with open(swmp_path / "readme", "r") as f:
                self.assertEqual("readme", f.read())
            with open(swmp_path / "d/readme", "r") as f:
                self.assertEqual("readme", f.read())
            with open(swmp_path / "d/t", "r") as f:
                self.assertEqual("t", f.read())
            with open(swmp_path / "big", "rb") as f:
                data = f.read()
                self.assertEqual(65536 * 4, len(data))
                self.assertEqual(
                    b"0" * 65536 + b"1" * 65536 + b"2" * 65536 + b"3" * 65536, data
                )
        BundleCopy(
            src_uri=cloud_uri,
            dest_uri=cases[0]["dest_uri"],
            typ=ResourceType.model,
            dest_local_project_uri=cases[0]["dest_local_project_uri"],
            force=True,
        ).do()

    @Mocker()
    @respx.mock
    def test_model_copy_l2c(self, rm: Mocker) -> None:
        version = "ge3tkylgha2tenrtmftdgyjzni3dayq"
        built_in_version = "abcdefg1234"
        swmp_path = (
            self._sw_config.rootdir
            / "self"
            / "model"
            / "mnist"
            / version[:2]
            / f"{version}.swmp"
        )
        swmp_manifest_path = swmp_path / "_manifest.yaml"
        tag_manifest_path = (
            self._sw_config.rootdir / "self" / "model" / "mnist" / "_manifest.yaml"
        )
        ensure_dir(swmp_path)
        ensure_dir(swmp_path / "src")
        ensure_file(swmp_path / "src" / "readme", "readme")
        ensure_file(
            swmp_manifest_path,
            yaml.safe_dump(
                {
                    "version": version,
                    "packaged_runtime": {
                        "manifest": {
                            "version": built_in_version,
                        },
                        "name": "other",
                        "path": "src/.starwhale/runtime/packaged.swrt",
                    },
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
            swmp_path / "src" / ".starwhale" / "runtime" / "packaged.swrt",
            "",
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

        respx.route().side_effect = self._model_server.serve

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

            rt_upload_request = rm.request(
                HTTPMethod.POST,
                f"http://1.1.1.1:8182/api/v1/project/mnist/runtime/{SW_BUILT_IN}/version/{built_in_version}/file",
                headers={"X-SW-UPLOAD-TYPE": FileDesc.MANIFEST.name},
                json={"data": {"uploadId": "126"}},
            )
            BundleCopy(
                src_uri=case["src_uri"],
                dest_uri=case["dest_uri"],
                typ=ResourceType.model,
            ).do()
            assert head_request.call_count == 1
            assert rt_upload_request.call_count == 1

        head_request = rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/mnist/model/mnist-alias/version/{version}",
            json={"message": "not found"},
            status_code=HTTPStatus.NOT_FOUND,
        )
        rt_upload_request = rm.request(
            HTTPMethod.POST,
            f"http://1.1.1.1:8182/api/v1/project/mnist/runtime/{SW_BUILT_IN}/version/{built_in_version}/file",
            headers={"X-SW-UPLOAD-TYPE": FileDesc.MANIFEST.name},
            json={"data": {"uploadId": "126"}},
        )

        def random_bytes(n: int) -> bytes:
            return bytes(bytearray(random.getrandbits(8) for _ in range(n)))

        def random_compressible_bytes(n: int) -> bytes:
            words = [random_bytes(random.randint(3, 15)) for _ in range(300)]
            ret = []
            total = 0
            while total < n:
                ret.append(words[random.randint(0, len(words) - 1)])
                total += len(ret[-1])
            return b"".join(ret)[:n]

        model_file = random_bytes(1024 * 1024 * 20 + 1024)
        ensure_file(swmp_path / "src" / "model", model_file)
        ensure_file(
            swmp_path / "src" / "mixed",
            random_compressible_bytes(1024 * 500)
            + random_bytes(1024 * 500)
            + random_compressible_bytes(1024 * 500),
        )
        file99 = random_compressible_bytes(999)
        ensure_file(
            swmp_path / "src" / "/".join([str(i) for i in range(20)]),
            file99,
            parents=True,
        )
        for i in range(500):
            ensure_file(
                swmp_path / "src" / "t" / f"f{i}",
                random_compressible_bytes(random.randint(100, 10000)),
                parents=True,
            )
        for i in range(500):
            ensure_file(
                swmp_path / "src" / "t" / f"d{i}" / "x",
                random_compressible_bytes(random.randint(100, 10000)),
                parents=True,
            )
        tzFile = random_compressible_bytes(1024 * 1024 * 20)
        ensure_file(swmp_path / "src" / "t" / "z", tzFile, parents=True)
        ensure_file(swmp_path / "src" / "empty", "")
        ensure_dir(swmp_path / "src" / "empty_dir")
        BundleCopy(
            src_uri="mnist/v1",
            dest_uri="cloud://pre-bare/project/mnist/model/mnist-alias",
            typ=ResourceType.model,
        ).do()

        rm.request(
            HTTPMethod.GET,
            "http://1.1.1.1:8182/api/v1/project/mnist/model/mnist-alias?versionUrl=v1",
            json={"data": {"id": 1, "versionName": version, "versionId": 100}},
            status_code=HTTPStatus.OK,
        )
        rm.request(
            HTTPMethod.HEAD,
            f"http://1.1.1.1:8182/api/v1/project/mnist/model/mnist-alias/version/{version}",
            json={"message": "existed"},
            status_code=HTTPStatus.OK,
        )
        BundleCopy(
            src_uri="cloud://pre-bare/project/mnist/model/mnist-alias/version/v1",
            dest_uri="mnist/v2",
            typ=ResourceType.model,
        ).do()
        dest_path = (
            self._sw_config.rootdir / "self/model/mnist" / version[:2] / "v2.swmp"
        )

        def compare(dir1: Path, dir2: Path) -> None:
            for f1, f2 in itertools.zip_longest(
                sorted(dir1.iterdir()), sorted(dir2.iterdir())
            ):
                self.assertIsNotNone(f1)
                self.assertIsNotNone(f2)
                self.assertEqual(f1.name, f2.name)
                self.assertEqual(f1.is_dir(), f2.is_dir())
                if f1.is_dir():
                    compare(f1, f2)
                else:
                    with open(f1, "rb") as a:
                        with open(f2, "rb") as b:
                            d1 = a.read()
                            d2 = b.read()
                            self.assertEqual(
                                hashlib.md5(d1).hexdigest(),
                                hashlib.md5(d2).hexdigest(),
                                f1.as_posix() + " vs " + f2.as_posix(),
                            )

        compare(swmp_path, dest_path)

    def _prepare_local_dataset(self) -> t.Tuple[str, str]:
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
    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
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

        cloud_uri = Resource(
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
    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
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
        src_uri = Resource(
            f"local/project/self/dataset/mnist/version/{version}", refine=True
        )
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
                    src_uri=Resource(case["src_uri"], typ=ResourceType.dataset),
                    dest_uri=case["dest_uri"],
                    mode=case["mode"],
                ).do()
            except Exception as e:
                print(f"case: {case}")
                raise e

            assert head_request.call_count == case["head_call_count"]
            assert upload_request.call_count == 2

        # TODO: support the flowing case
        with self.assertRaises(NoSupportError):
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
    @patch("starwhale.base.uri.resource.Resource._refine_local_rc_info")
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
