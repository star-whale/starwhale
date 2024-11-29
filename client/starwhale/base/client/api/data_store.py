from __future__ import annotations

from typing import List

from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    CheckpointVo,
    CreateCheckpointRequest,
    ResponseMessageListCheckpointVo,
)

_URI = "/api/v1/project/datastore/checkpoint"


class DataStoreApi(Client):
    def __init__(self, url: str, token: str) -> None:
        super().__init__(url, token)

    def list_checkpoints(self, table: str) -> List[CheckpointVo]:
        return (
            TypeWrapper(
                ResponseMessageListCheckpointVo,
                self.http_get(_URI, params={"table": table}),
            )
            .response()
            .data
        )

    def create_checkpoint(self, req: CreateCheckpointRequest) -> CheckpointVo:
        return TypeWrapper(CheckpointVo, self.http_post(_URI, json=req)).response().data

    def delete_checkpoint(self, table: str, revision: str) -> None:
        self.http_delete(_URI, params={"table": table, "id": revision})
