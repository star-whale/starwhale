from __future__ import annotations

from starwhale.base.models.base import ListFilter
from starwhale.base.uri.instance import Instance
from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    JobRequest,
    ResponseMessageJobVo,
    ResponseMessageString,
    ResponseMessagePageInfoJobVo,
    ResponseMessagePageInfoTaskVo,
)


class JobApi(Client):
    def __init__(self, instance: Instance) -> None:
        super().__init__(instance.url, instance.token)

    def list(
        self,
        project: str,
        page: int,
        size: int,
        _filter: ListFilter | None = None,
    ) -> TypeWrapper[ResponseMessagePageInfoJobVo]:
        uri = f"/api/v1/project/{project}/job"
        data = self._list(uri, page, size, _filter)
        return TypeWrapper(ResponseMessagePageInfoJobVo, data)

    def create(
        self, project: str, job: JobRequest
    ) -> TypeWrapper[ResponseMessageString]:
        uri = f"/api/v1/project/{project}/job"
        data = self.http_post(uri, json=job)
        return TypeWrapper(ResponseMessageString, data)

    def info(self, project: str, job: str) -> TypeWrapper[ResponseMessageJobVo]:
        uri = f"/api/v1/project/{project}/job/{job}"
        data = self.http_get(uri)
        return TypeWrapper(ResponseMessageJobVo, data)

    def tasks(
        self, project: str, job: str
    ) -> TypeWrapper[ResponseMessagePageInfoTaskVo]:
        uri = f"/api/v1/project/{project}/job/{job}/task"
        long_enough_size = 100000
        data = self.http_get(uri, params={"pageNum": 1, "pageSize": long_enough_size})
        return TypeWrapper(ResponseMessagePageInfoTaskVo, data)
