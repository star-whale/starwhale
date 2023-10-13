import dataclasses
from typing import Any
from unittest.mock import MagicMock

import pytest
import requests.exceptions

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

    request_count = 0

    def request_side_effect_with_exception(*args, **kwargs) -> Any:
        nonlocal request_count
        if request_count > 0:
            return MockResponse(200, "OK")
        request_count += 1
        raise requests.exceptions.Timeout()

    c.session.request = request_side_effect_with_exception
    resp = c.http_request("GET", "/test")
    # retry when exception is raised
    assert resp == "OK"

    request_count = 0

    def request_side_effect_with_exception(*args, **kwargs) -> Any:
        nonlocal request_count
        request_count += 1
        raise requests.exceptions.ConnectionError()

    c.session.request = request_side_effect_with_exception
    with pytest.raises(requests.exceptions.ConnectionError):
        c.http_request("GET", "/test")

    assert request_count == 2
