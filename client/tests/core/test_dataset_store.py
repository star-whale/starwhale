import os
import sys
import string
import typing as t
import tempfile
from pathlib import Path
from unittest import TestCase
from unittest.mock import patch, MagicMock

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import patchfs

from starwhale.utils import config
from starwhale.utils.fs import ensure_file
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.base.data_type import Link
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.store import (
    BytesBuffer,
    HttpBackend,
    S3Connection,
    DatasetStorage,
    S3StorageBackend,
    SignedUrlBackend,
    S3BufferedFileLike,
    HttpBufferedFileLike,
    LocalFSStorageBackend,
)


class TestDatasetBackend(TestCase):
    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_signed_url_backend(
        self,
        rm: Mocker,
        mock_conf: MagicMock,
    ):
        mock_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "foo"},
                "foo": {
                    "uri": "http://127.0.0.1:1234",
                    "current_project": "self",
                    "sw_token": "token",
                },
            },
            "storage": {"root": "/tmp"},
        }
        signed_url = "http://minio-io/path/to/signed/file"
        raw_content = string.ascii_lowercase.encode()
        data_uri = "12345678abcdefg"
        req_signed_url = rm.post(
            "http://127.0.0.1:1234/api/v1/project/self/dataset/mnist/uri/sign-links",
            json={"data": {data_uri: signed_url}},
        )
        req_file_download = rm.get(
            signed_url,
            content=raw_content,
        )

        dataset_uri = Resource(
            "http://127.0.0.1:1234/project/self/dataset/mnist/version/1122334455667788",
            typ=ResourceType.dataset,
            refine=False,
        )
        obj = SignedUrlBackend(dataset_uri)._make_file((Link(data_uri), 0, -1))
        assert obj.read(1) == b"a"
        assert obj.read(-1) == raw_content[1:]
        assert req_signed_url.call_count == 1
        assert req_file_download.call_count == 1
        obj.close()

    @patch("os.environ", {})
    def test_s3_conn_from_uri(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdirname:
            config._config = {}
            S3Connection.connections_config = []
            os.environ["SW_CLI_CONFIG"] = tmpdirname + "/config.yaml"
            conn = S3Connection.from_uri(
                "s3://username:password@127.0.0.1:8000/bucket/key"
            )
            assert conn.endpoint == "http://127.0.0.1:8000"
            assert conn.access_key == "username"
            assert conn.secret_key == "password"
            assert conn.bucket == "bucket"
            assert conn.region == "local"

            with self.assertRaises(NoSupportError):
                S3Connection.from_uri("s3://127.0.0.1:8000/bucket/key")

            config.update_swcli_config(
                **{
                    "link_auths": [
                        {
                            "type": "s3",
                            "ak": "access_key",
                            "sk": "secret",
                            "endpoint": "http://127.0.0.1:8000",
                            "bucket": "bucket",
                            "connect_timeout": 10.0,
                            "read_timeout": 100.0,
                        },
                    ]
                }
            )
            S3Connection.connections_config = []
            conn = S3Connection.from_uri("s3://127.0.0.1:8000/bucket/key")
            assert conn.endpoint == "http://127.0.0.1:8000"
            assert conn.access_key == "access_key"
            assert conn.secret_key == "secret"
            assert conn.bucket == "bucket"
            assert conn.region == "local"
            assert conn.connect_timeout == 10.0
            assert conn.read_timeout == 100.0
            assert conn.total_max_attempts == 6
            assert conn.extra_s3_configs == {}

            with self.assertRaises(NoSupportError):
                S3Connection.from_uri("bucket/key")

            with self.assertRaises(FieldTypeOrValueError):
                S3Connection.from_uri("s3://127.0.0.1:8000")

            with self.assertRaises(FieldTypeOrValueError):
                S3Connection.from_uri("s3://127.0.0.1:8000/bucket")

            with self.assertRaises(FieldTypeOrValueError):
                S3Connection.from_uri("s3://127.0.0.1:8000/bucket/")

            conn = S3Connection.from_uri("minio://127.0.0.1:8000/bucket/key")
            assert conn.endpoint == "http://127.0.0.1:8000"

    @patch("os.environ", {})
    def test_s3_conn_from_env(self) -> None:
        conn = S3Connection.from_env()
        assert conn.endpoint == "http://localhost:9000"
        assert conn.access_key == ""
        assert conn.secret_key == ""
        assert conn.region == "local"
        assert conn.bucket == "starwhale"

        os.environ["SW_S3_ENDPOINT"] = "localhost:9001"
        os.environ["SW_S3_ACCESS_KEY"] = "access_key"
        os.environ["SW_S3_SECRET"] = "secret"
        os.environ["SW_S3_REGION"] = "asia"
        os.environ["SW_S3_BUCKET"] = "users"
        conn = S3Connection.from_env()
        assert conn.endpoint == "http://localhost:9001"
        assert conn.access_key == "access_key"
        assert conn.secret_key == "secret"
        assert conn.region == "asia"
        assert conn.bucket == "users"

    def test_s3_backend(self) -> None:
        uri = Link("s3://username:password@127.0.0.1:8000/bucket/key")
        conn = S3Connection.from_uri("s3://username:password@127.0.0.1:8000/bucket/key")
        backend = S3StorageBackend(conn)
        s3_file: S3BufferedFileLike = backend._make_file(bucket="bucket", key_compose=(uri, 0, -1))  # type: ignore
        assert s3_file.key == "key"
        assert s3_file._current_s3_start == 0
        assert s3_file.end == -1
        s3_file.close()

        with backend._make_file(
            bucket="bucket", key_compose=(Link("/path/key2"), 0, -1)
        ) as s3_file:
            assert s3_file.key == "path/key2"

    @patchfs
    def test_local_fs_backend(self, fake_fs: t.Any) -> None:
        content = "1234"
        fpath = Path("/home/test/test.file")
        ensure_file(fpath, content, parents=True)
        uri, _ = DatasetStorage.save_data_file(fpath)

        backend = LocalFSStorageBackend()
        cases = [
            (sys.maxsize, b"1234"),
            (1, b"12"),
            (0, b"1"),
            (4, b"1234"),
        ]

        for _end, _content in cases:
            file = backend._make_file((Link(uri=uri), 0, _end))
            assert file.read(-1) == _content

            file = backend._make_file(
                (Link(uri="test.file"), 0, _end), bucket="/home/test"
            )
            assert file.read(-1) == _content


class TestBytesBuffer(TestCase):
    def setUp(self) -> None:
        self.content = string.ascii_letters.encode()
        self.content_iter = iter(
            [self.content[i : i + 10] for i in range(0, len(self.content), 10)]
        )

    def test_read(self) -> None:
        buf = BytesBuffer(10)
        buf.write_from_iter(self.content_iter)
        assert len(buf) == 10
        assert buf.read(1) == b"a"
        assert buf.read(1) == b"b"
        assert buf.read() == b"cdefghij"
        assert buf.read(1) == b""
        assert buf.read() == b""
        assert len(buf) == 0

        wrote_size = buf.write_from_iter(self.content_iter)
        assert wrote_size == 10

        assert buf.read(2) == b"kl"
        assert buf.read(2) == b"mn"
        assert len(buf) == 6
        assert buf.read(-1) == b"opqrst"

    def test_read_zero(self) -> None:
        buf = BytesBuffer(0)
        assert buf.read() == b""
        assert buf.read(1) == b""
        assert buf.read(-1) == b""

    def test_read_exhausted_buffer(self) -> None:
        buf = BytesBuffer(10)
        buf.write_from_iter([b"abcd"])
        assert len(buf) == 4
        assert buf.read(-1) == b"abcd"
        assert buf.read(-1) == b""
        assert buf.read() == b""

    def test_write(self) -> None:
        buf = BytesBuffer(10)
        buf.write_from_iter(self.content_iter)
        assert len(buf) == 10
        buf.write_from_iter(self.content_iter)
        assert len(buf) == 20
        assert buf.read(1) == b"a"
        assert len(buf) == 19
        buf.write_from_iter(self.content_iter)
        assert len(buf) == 29
        assert buf.read(1) == b"b"
        buf.write_from_iter(self.content_iter)
        assert buf.read(1) == b"c"

    def test_close(self) -> None:
        buf = BytesBuffer(10)
        buf.close()
        assert len(buf._buffer) == 0

        buf = BytesBuffer(10)
        buf.write_from_iter(self.content_iter)
        assert len(buf._buffer) == 10
        buf.close()
        assert len(buf._buffer) == 0
        # close twice test
        buf.close()


class TestBufferedFile(TestCase):
    @Mocker()
    def test_http_read(self, rm: Mocker) -> None:
        url = "http://1.1.1.1/path/to/file"
        mock_req = rm.get(
            url,
            content=string.ascii_lowercase.encode(),
        )

        buffer_size_tests = [1, 10, 26, 100]
        for size in buffer_size_tests:
            rm.reset_mock()
            obj = HttpBufferedFileLike(url=url, buffer_size=size, headers={"k": "v"})
            assert mock_req.call_count == 1
            assert obj.read(0) == b""
            assert obj.read(1) == b"a"
            assert obj.read(1) == b"b"
            assert obj.read(-1) == string.ascii_lowercase.encode()[2:]
            assert obj.read(1) == b""
            obj.close()

        with HttpBufferedFileLike(url=url, buffer_size=10, headers={"k": "v"}) as f:
            assert f.read(-1) == string.ascii_lowercase.encode()
            assert f.read(100) == b""

    def test_s3_read(self) -> None:
        content = string.ascii_lowercase.encode()
        s3 = MagicMock(
            **{
                "Object.return_value": MagicMock(
                    **{
                        "get.return_value": {
                            "Body": MagicMock(**{"read.return_value": content}),
                            "ContentLength": len(content),
                        }
                    }
                )
            }
        )

        obj = S3BufferedFileLike(s3, "bucket", "key", 0, 100)
        assert obj.read(0) == b""
        assert obj.read(1) == b"a"
        assert obj.read(3) == b"bcd"
        assert obj.read(-1) == content[4:] + content

        with S3BufferedFileLike(s3, "bucket", "key", 0, 10) as f:
            assert f.read(100) == content

    @Mocker()
    def test_http_backend(
        self,
        rm: Mocker,
    ):
        data_uri = "http://minio-io/path/to/signed/file"
        raw_content = string.ascii_lowercase.encode()
        req_file_download = rm.get(
            data_uri,
            content=raw_content,
        )

        obj = HttpBackend()._make_file((Link(data_uri), 0, -1))
        assert obj.read(1) == b"a"
        assert obj.read(-1) == raw_content[1:]
        assert req_file_download.call_count == 1
        obj.close()
