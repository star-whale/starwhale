import dataclasses
from unittest.mock import MagicMock

from starwhale.base.client.client import Client


@dataclasses.dataclass
class MockResponse:
    status_code: int
    text: str

    def json(self) -> str:
        return self.text


def test_http_request_retry() -> None:
    c = Client("http://localhost", "token")
    c.session.request = MagicMock()
    c.session.request.side_effect = [
        MockResponse(500, "Internal Server Error"),
        MockResponse(200, "OK"),
    ]

    resp = c.http_request("GET", "/test")
    # no retry when status code is not in code_to_retry
    assert resp == "Internal Server Error"

    c.session.request.side_effect = [
        MockResponse(503, "Service Unavailable"),
        MockResponse(200, "OK"),
    ]
    resp = c.http_request("GET", "/test")
    # retry when status code is in code_to_retry
    assert resp == "OK"
