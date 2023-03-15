from fastapi import APIRouter

from starwhale.web.response import success, SuccessResp

router = APIRouter()
prefix = "user"

user = {
    "id": "1",
    "name": "starwhale",
    "createdTime": 0,
    "isEnabled": True,
    "systemRole": "OWNER",
    "projectRoles": {"1": "OWNER"},
}


@router.get("/current")
def current() -> SuccessResp:
    return success(user)
