from __future__ import annotations

import os
import sys
import typing

import requests
from requests import exceptions
from tenacity import (
    retry,
    stop_never,
    stop_after_attempt,
    retry_if_exception_type,
    wait_random_exponential,
)
from fastapi.encoders import jsonable_encoder

from starwhale.utils import console
from starwhale.base.models.base import RespType, ListFilter, SwBaseModel, obj_to_model
from starwhale.base.client.models.base import ResponseCode

T = typing.TypeVar("T")
CLIENT_DEFAULT_RETRY_ATTEMPTS = 10


def _get_client_retry_attempts() -> int:
    """
    Get retry attempts from env, default to 10
    Set env SW_CLIENT_RETRY_ATTEMPTS to override, e.g. SW_CLIENT_RETRY_ATTEMPTS=5,
    and if you want to disable retry, set to 0 or empty string
    """
    env = os.getenv("SW_CLIENT_RETRY_ATTEMPTS")
    if env is None:
        if "pytest" in sys.modules:
            # we do not want to wait too long in pytest
            console.warn("retry limit to 2 in pytest")
            return 2
        return CLIENT_DEFAULT_RETRY_ATTEMPTS
    if env == "":
        return 0
    return int(env)


class ClientException(Exception):
    ...


class TypeWrapper(typing.Generic[T]):
    def __init__(self, type_: typing.Type[RespType], data: typing.Any) -> None:
        self._type = type_
        self._data = data
        self._raise_on_error = False
        self._base = obj_to_model(data, ResponseCode)
        self._response: T | None = None
        if self.is_success():
            self._response = obj_to_model(self._data, self._type)  # type: ignore
        else:
            console.debug(f"request failed, response msg: {self._base.message}")

    def response(self) -> T:
        if self._response is None:
            raise ClientException(self._base.message)
        return self._response

    def raw(self) -> typing.Any:
        return self._data

    def is_success(self) -> bool:
        return self._base.code == "success"

    def raise_on_error(self) -> TypeWrapper[T]:
        if not self.is_success():
            raise ClientException(self._base.message)
        return self


class RetryableException(Exception):
    ...


class Client:
    def __init__(self, base_url: str, token: str) -> None:
        self.base_url = base_url
        self.token = token
        self.session = requests.Session()
        self.session.headers.update({"Authorization": token})

    @retry(
        # https://requests.readthedocs.io/en/latest/user/quickstart/#errors-and-exceptions
        retry=retry_if_exception_type(
            (RetryableException, exceptions.ConnectionError, exceptions.Timeout)
        ),
        # retry for every 1s, 2s, 4s, 8s, 16s, 32s, 60s, 60s, ...
        wait=wait_random_exponential(multiplier=1, max=60),
        stop=_get_client_retry_attempts() > 0
        and stop_after_attempt(_get_client_retry_attempts())
        or stop_never,
        reraise=True,
    )
    def http_request(
        self,
        method: str,
        uri: str,
        json: dict | SwBaseModel | None = None,
        params: dict | None = None,
        data: typing.Any = None,
    ) -> typing.Any:
        _json: typing.Any = json
        if isinstance(json, SwBaseModel):
            # convert to dict with proper alias
            _json = jsonable_encoder(json.dict(by_alias=True, exclude_none=True))
        resp = self.session.request(
            method,
            f"{self.base_url}{uri}",
            json=_json,
            params=params,
            data=data,
            timeout=90,
        )
        code_to_retry = {408, 429, 502, 503, 504}
        if resp.status_code in code_to_retry:
            raise RetryableException(f"status code: {resp.status_code}")

        # The server will respond 500 if error happens, dead retry will not help
        return resp.json()

    def _list(
        self, uri: str, page: int, size: int, _filter: ListFilter | None
    ) -> typing.Any:
        params = {"pageNum": page, "pageSize": size}
        if _filter is not None:
            params.update(_filter.dict())
        return self.http_get(uri, params=params)

    def http_get(self, uri: str, params: dict | None = None) -> typing.Any:
        return self.http_request("GET", uri, params=params)

    def http_post(
        self,
        uri: str,
        json: dict | SwBaseModel | None = None,
        data: typing.Any = None,
    ) -> typing.Any:
        return self.http_request("POST", uri, json=json, data=data)

    def http_put(
        self,
        uri: str,
        json: dict | SwBaseModel | None = None,
        params: dict | None = None,
    ) -> typing.Any:
        return self.http_request("PUT", uri, json=json, params=params)

    def http_delete(self, uri: str, params: dict | None = None) -> typing.Any:
        return self.http_request("DELETE", uri, params=params)
