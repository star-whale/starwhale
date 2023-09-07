from __future__ import annotations

import typing

import requests
from pydantic import BaseModel
from tenacity import retry, retry_if_exception_type, wait_random_exponential
from pydantic.tools import parse_obj_as

from starwhale.utils import console
from starwhale.base.client.models.base import ResponseCode

T = typing.TypeVar("T")
RespType = typing.TypeVar("RespType", bound=BaseModel)


class TypeWrapper(typing.Generic[T]):
    def __init__(self, type_: typing.Type[RespType], data: typing.Any) -> None:
        self._type = type_
        self._data = data
        self._raise_on_error = False

    def data(self) -> T:
        base = self._parse_base()
        if not self._raise_on_error and not self._is_success(base):
            console.debug(base.message)
            return base  # type: ignore[return-value]

        return parse_obj_as(self._type, self._data)  # type: ignore[return-value]

    def raw(self) -> typing.Any:
        return self._data

    def _parse_base(self) -> ResponseCode:
        return parse_obj_as(ResponseCode, self.raw())

    def is_success(self) -> bool:
        return self._is_success(self._parse_base())

    @staticmethod
    def _is_success(base: ResponseCode) -> bool:
        return base.code == "success"

    def raise_on_error(self) -> TypeWrapper[T]:
        self._raise_on_error = True
        parsed = self._parse_base()
        if not self._is_success(parsed):
            raise Exception(parsed.message)
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
        retry=retry_if_exception_type(RetryableException),
        # retry for every 1s, 2s, 4s, 8s, 16s, 32s, 60s, 60s, ...
        wait=wait_random_exponential(multiplier=1, max=60),
    )
    def http_request(
        self,
        method: str,
        uri: str,
        json: dict | BaseModel | None = None,
        params: dict | None = None,
        data: typing.Any = None,
    ) -> typing.Any:
        if isinstance(json, BaseModel):
            # convert to dict with proper alias
            json = json.dict(by_alias=True)
        resp = self.session.request(
            method,
            f"{self.base_url}{uri}",
            json=json,
            params=params,
            data=data,
            timeout=90,
        )
        code_to_retry = {408, 429, 502, 503, 504}
        if resp.status_code in code_to_retry:
            raise RetryableException(f"status code: {resp.status_code}")

        # The server will respond 500 if error happens, dead retry will not help
        return resp.json()

    def http_get(self, uri: str, params: dict | None = None) -> typing.Any:
        return self.http_request("GET", uri, params=params)

    def http_post(
        self, uri: str, json: dict | BaseModel | None = None, data: typing.Any = None
    ) -> typing.Any:
        return self.http_request("POST", uri, json=json, data=data)

    def http_put(
        self, uri: str, json: dict | BaseModel | None = None, params: dict | None = None
    ) -> typing.Any:
        return self.http_request("PUT", uri, json=json, params=params)

    def http_delete(self, uri: str, params: dict | None = None) -> typing.Any:
        return self.http_request("DELETE", uri, params=params)
