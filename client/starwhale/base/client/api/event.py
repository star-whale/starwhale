from __future__ import annotations

from starwhale.base.uri.instance import Instance
from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.base import ResponseCode
from starwhale.base.client.models.models import EventRequest, ResponseMessageListEventVo


class EventApi(Client):
    def __init__(self, instance: Instance) -> None:
        super().__init__(instance.url, instance.token)

    def list(
        self, project: str, job: str, task: str = "", run: str = ""
    ) -> TypeWrapper[ResponseMessageListEventVo]:
        uri = f"/api/v1/project/{project}/job/{job}/event"
        return TypeWrapper(
            ResponseMessageListEventVo,
            self.http_get(uri, params={"taskId": task, "runId": run}),
        )

    def add(
        self, project: str, job: str, event: EventRequest
    ) -> TypeWrapper[ResponseCode]:
        uri = f"/api/v1/project/{project}/job/{job}/event"
        data = self.http_post(uri, event)
        return TypeWrapper(ResponseCode, data)
