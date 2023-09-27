from __future__ import annotations

from starwhale.base.uri.instance import Instance
from starwhale.base.uri.resource import Resource
from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.base import ResponseCode
from starwhale.base.client.models.models import (
    DatasetTagRequest,
    ResponseMessageListString,
)


class TagApi(Client):
    def __init__(self, instance: Instance) -> None:
        super().__init__(instance.url, instance.token)

    def list(self, rc: Resource) -> TypeWrapper[ResponseMessageListString]:
        uri = f"/api/v1/project/{rc.project.id}/{rc.typ.value}/{rc.name}/version/{rc.version}/tag"
        return TypeWrapper(ResponseMessageListString, self.http_get(uri))

    def add(
        self, rc: Resource, tag: str, force: bool = True
    ) -> TypeWrapper[ResponseCode]:
        uri = f"/api/v1/project/{rc.project.id}/{rc.typ.value}/{rc.name}/version/{rc.version}/tag"
        # use DatasetTagRequest for common tag request
        data = self.http_post(uri, DatasetTagRequest(tag=tag, force=force))
        return TypeWrapper(ResponseCode, data)
