from http import HTTPStatus
from unittest import TestCase
from unittest.mock import patch, MagicMock

from starwhale import URI, URIType
from starwhale.core.dataset.store import SignedUrlBackend


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
        file = backend._make_file("", ("a", None, None))
        self.assertEqual(file.read(), _content)
