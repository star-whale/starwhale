from unittest import TestCase
from unittest.mock import patch, MagicMock

from starwhale.base import cloud_blob_cache


class TestCloudBlobCache(TestCase):
    @patch("starwhale.base.cloud_blob_cache.socket.gethostbyname_ex")
    def test_replace_url(self, gethostbyname_ex: MagicMock) -> None:
        self.assertRaises(
            AssertionError,
            lambda: next(cloud_blob_cache.replace_url("https://t", True)),
        )

        gethostbyname_ex.return_value = ("localhost", [], ["1", "2", "3"])
        cloud_blob_cache.init()
        iter = cloud_blob_cache.replace_url("https://t/other?a=1", True)
        for _ in range(10):
            results = [next(iter), next(iter), next(iter)]
            results.sort()
            self.assertListEqual(
                results,
                [
                    "http://1:18080/other?a=1",
                    "http://2:18080/other?a=1",
                    "http://3:18080/other?a=1",
                ],
            )

        gethostbyname_ex.side_effect = RuntimeError()
        cloud_blob_cache.init()
        self.assertNotEqual(
            next(cloud_blob_cache.replace_url("http://t", True)), "http://t"
        )
        self.assertEqual(
            next(cloud_blob_cache.replace_url("http://t", False)), "http://t"
        )
        cloud_blob_cache._servers = None
        cloud_blob_cache.init()
        self.assertEqual(
            next(cloud_blob_cache.replace_url("http://t", True)), "http://t"
        )
