from __future__ import annotations

from starwhale.base.models.base import ListFilter
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.resource import Resource
from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    ResponseMessageModelInfoVo,
    ResponseMessagePageInfoModelVo,
)


class ModelApi(Client):
    def __init__(self, instance: Instance) -> None:
        super().__init__(instance.url, instance.token)

    def list(
        self,
        project: str,
        page: int,
        size: int,
        _filter: ListFilter | None = None,
    ) -> TypeWrapper[ResponseMessagePageInfoModelVo]:
        uri = f"/api/v1/project/{project}/model"
        data = self._list(uri, page, size, _filter)
        return TypeWrapper(ResponseMessagePageInfoModelVo, data)

    def info(self, rc: Resource) -> TypeWrapper[ResponseMessageModelInfoVo]:
        uri = f"/api/v1/project/{rc.project.id}/model/{rc.name}"
        return TypeWrapper(
            ResponseMessageModelInfoVo,
            self.http_get(uri, params={"versionName": rc.version}),
        )
