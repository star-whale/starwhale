import os
from http import HTTPStatus
from unittest import TestCase
from unittest.mock import patch, MagicMock

from starwhale import URI, URIType
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.core.dataset.store import (
    S3Connection,
    S3StorageBackend,
    SignedUrlBackend,
    S3BufferedFileLike,
)


class TestDatasetBackend(TestCase):
    @patch("requests.get")
    @patch("requests.request")
    def test_signed_url_backend(
        self,
        m_request: MagicMock,
        m_get: MagicMock,
    ):
        m_request.return_value = MagicMock(
            **{"status_code": HTTPStatus.OK, "data": "a"}
        )
        _content = b"abcd"
        m_get.return_value = MagicMock(
            **{
                "content": _content,
            }
        )
        dataset_uri = URI(
            "http://127.0.0.1:1234/project/self/dataset/mnist/version/1122334455667788",
            expected_type=URIType.DATASET,
        )
        backend = SignedUrlBackend(dataset_uri)
        file = backend._make_file("", ("a", 0, -1))
        self.assertEqual(file.read(-1), _content)

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
        uri = "s3://username:password@127.0.0.1:8000/bucket/key"
        conn = S3Connection.from_uri("s3://username:password@127.0.0.1:8000/bucket/key")
        backend = S3StorageBackend(conn)
        s3_file: S3BufferedFileLike = backend._make_file("bucket", (uri, 0, -1))  # type: ignore
        assert s3_file.key == "key"
        assert s3_file.start == 0
        assert s3_file.end == -1

        s3_file: S3BufferedFileLike = backend._make_file("bucket", ("/path/key2", 0, -1))  # type: ignore
        assert s3_file.key == "path/key2"
