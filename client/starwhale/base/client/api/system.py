from __future__ import annotations

from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    SystemVersionVo,
    ResponseMessageSystemVersionVo,
)


class SystemApi(Client):
    def __init__(self, url: str, token: str) -> None:
        super().__init__(url, token)

    def version(self) -> SystemVersionVo:
        data = self.http_get("/api/v1/system/version")
        return TypeWrapper(ResponseMessageSystemVersionVo, data).response().data  # type: ignore
