from __future__ import annotations

from starwhale.base.uri.instance import Instance
from starwhale.base.client.client import Client, TypeWrapper
from starwhale.base.client.models.models import (
    ResponseMessageString,
    ResponseMessageProjectVo,
    ResponseMessagePageInfoProjectVo,
)


class ProjectApi(Client):
    def __init__(self, instance: Instance) -> None:
        super().__init__(instance.url, instance.token)

    def list(
        self,
        project_name: str | int | None = None,
        page_num: int | None = None,
        page_size: int | None = None,
    ) -> TypeWrapper[ResponseMessagePageInfoProjectVo]:
        data = self.http_get(
            "/api/v1/project",
            {
                "projectName": project_name,
                "pageNum": page_num,
                "pageSize": page_size,
            },
        )
        return TypeWrapper(ResponseMessagePageInfoProjectVo, data)

    def get(self, project_url: str | int) -> TypeWrapper[ResponseMessageProjectVo]:
        data = self.http_get(f"/api/v1/project/{project_url}")
        return TypeWrapper(ResponseMessageProjectVo, data)

    def create(self, project_name: str | int) -> TypeWrapper[ResponseMessageString]:
        data = self.http_post(
            "/api/v1/project",
            {"projectName": project_name, "privacy": "", "description": ""},
        )
        return TypeWrapper(ResponseMessageString, data)

    def recover(self, project_url: str | int) -> TypeWrapper[ResponseMessageString]:
        data = self.http_put(f"/api/v1/project/{project_url}/recover")
        return TypeWrapper(ResponseMessageString, data)

    def delete(self, project_url: str | int) -> TypeWrapper[ResponseMessageString]:
        data = self.http_delete(f"/api/v1/project/{project_url}")
        return TypeWrapper(ResponseMessageString, data)
