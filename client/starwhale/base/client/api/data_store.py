from __future__ import annotations

from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    ResponseMessageString,
    UpdateTableEmbeddedRequest,
)


class DataStoreApi(Client):
    def __init__(self, url: str, token: str) -> None:
        super().__init__(url, token)

    def update(self, req: UpdateTableEmbeddedRequest) -> str:
        data = self.http_post("/api/v1/datastore/updateTable/embedded", json=req)
        return TypeWrapper(ResponseMessageString, data).response().data  # type: ignore
