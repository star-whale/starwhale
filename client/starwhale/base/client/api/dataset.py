from __future__ import annotations

from starwhale.base.models.base import ListFilter
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.resource import Resource
from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    DataConsumptionRequest,
    ResponseMessageDatasetInfoVo,
    ResponseMessagePageInfoDatasetVo,
    NullableResponseMessageDataIndexDesc,
)


class DatasetApi(Client):
    def __init__(self, instance: Instance) -> None:
        super().__init__(instance.url, instance.token)

    def list(
        self,
        project: str,
        page: int,
        size: int,
        _filter: ListFilter | None = None,
    ) -> TypeWrapper[ResponseMessagePageInfoDatasetVo]:
        uri = f"/api/v1/project/{project}/dataset"
        data = self._list(uri, page, size, _filter)
        return TypeWrapper(ResponseMessagePageInfoDatasetVo, data)

    def info(self, rc: Resource) -> TypeWrapper[ResponseMessageDatasetInfoVo]:
        uri = f"/api/v1/project/{rc.project.id}/dataset/{rc.name}"
        return TypeWrapper(
            ResponseMessageDatasetInfoVo,
            self.http_get(uri, params={"versionName": rc.version}),
        )

    def consume(
        self, rc: Resource, req: DataConsumptionRequest
    ) -> TypeWrapper[NullableResponseMessageDataIndexDesc]:
        uri = f"/api/v1/project/{rc.project.id}/dataset/{rc.name}/version/{rc.version}/consume"
        return TypeWrapper(
            NullableResponseMessageDataIndexDesc, self.http_post(uri, json=req)
        )
