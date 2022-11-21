import os
import string
from unittest import TestCase
from unittest.mock import patch, MagicMock

from requests_mock import Mocker

from starwhale import URI, URIType
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.core.dataset.type import Link
from starwhale.core.dataset.store import (
    BytesBuffer,
    S3Connection,
    S3StorageBackend,
    SignedUrlBackend,
    S3BufferedFileLike,
    HttpBufferedFileLike,
)


class TestDatasetBackend(TestCase):
    @Mocker()
    def test_signed_url_backend(
        self,
        rm: Mocker,
    ):
        signed_url = "http://minio-io/path/to/signed/file"
        raw_content = string.ascii_lowercase.encode()
        data_uri = "12345678abcdefg"
        req_signed_url = rm.post(
            "http://127.0.0.1:1234/api/v1/project/self/dataset/mnist/version/1122334455667788/sign-links",
            json={"data": {data_uri: signed_url}},
        )
        req_file_download = rm.get(
            signed_url,
            content=raw_content,
        )

        dataset_uri = URI(
            "http://127.0.0.1:1234/project/self/dataset/mnist/version/1122334455667788",
            expected_type=URIType.DATASET,
        )
        obj = SignedUrlBackend(dataset_uri)._make_file("", (Link(data_uri), 0, -1))
        assert obj.read(1) == b"a"
        assert obj.read(-1) == raw_content[1:]
        assert req_signed_url.call_count == 1
        assert req_file_download.call_count == 1
        obj.close()

    @patch("os.environ", {})
    def test_s3_conn_from_uri(self) -> None:
        conn = S3Connection.from_uri("s3://username:password@127.0.0.1:8000/bucket/key")
        assert conn.endpoint == "http://127.0.0.1:8000"
        assert conn.access_key == "username"
        assert conn.secret_key == "password"
        assert conn.bucket == "bucket"
        assert conn.region == "local"

        with self.assertRaises(FieldTypeOrValueError):
            S3Connection.from_uri("s3://127.0.0.1:8000/bucket/key")

        os.environ["USER.S3.SECRET"] = "secret"
        os.environ["USER.S3.ACCESS_KEY"] = "access_key"
        os.environ["SW_S3_READ_TIMEOUT"] = "100.0"
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
        s3_file: S3BufferedFileLike = backend._make_file("bucket", (uri, 0, -1))  # type: ignore
        assert s3_file.key == "key"
        assert s3_file._current_s3_start == 0
        assert s3_file.end == -1
        s3_file.close()

        with backend._make_file("bucket", (Link("/path/key2"), 0, -1)) as s3_file:
            assert s3_file.key == "path/key2"


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
