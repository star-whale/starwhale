from fastapi import APIRouter

from starwhale.web.user import user
from starwhale.web.response import success, SuccessResp

router = APIRouter()
prefix = "project"

project = {
    "id": "self",
    "name": "self",
    "privacy": "PRIVATE",
    "createdTime": 0,
    "owner": user,
    "statistics": {},
}


@router.get("/{project_id}")
def get_project(project_id: str) -> SuccessResp:
    return success(project)


@router.get("/{project_id}/job/{job}")
def get_job(project_id: str, job: str) -> SuccessResp:
    return success({"id": "1", "uuid": job, "modelName": "mock-model"})


@router.get("/{project_id}/role")
def get_role(project_id: str) -> SuccessResp:
    return success(
        [
            {
                "id": "1",
                "user": user,
                "project": project,
                "role": {
                    "id": "1",
                    "name": "Owner",
                    "code": "OWNER",
                },
            },
        ]
    )
