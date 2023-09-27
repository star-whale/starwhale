from __future__ import annotations

from starwhale.base.models.base import ListFilter
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.resource import Resource
from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    ResponseMessageRuntimeInfoVo,
    ResponseMessagePageInfoRuntimeVo,
)


class RuntimeApi(Client):
    def __init__(self, instance: Instance) -> None:
        super().__init__(instance.url, instance.token)

    def list(
        self,
        project: str,
        page: int,
        size: int,
        _filter: ListFilter | None = None,
    ) -> TypeWrapper[ResponseMessagePageInfoRuntimeVo]:
        uri = f"/api/v1/project/{project}/runtime"
        data = self._list(uri, page, size, _filter)
        return TypeWrapper(ResponseMessagePageInfoRuntimeVo, data)

    def info(self, rc: Resource) -> TypeWrapper[ResponseMessageRuntimeInfoVo]:
        uri = f"/api/v1/project/{rc.project.id}/runtime/{rc.name}"
        data = self.http_get(uri, params={"versionName": rc.version})
        return TypeWrapper(ResponseMessageRuntimeInfoVo, data)
