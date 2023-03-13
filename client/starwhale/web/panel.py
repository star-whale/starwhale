import os.path
from pathlib import Path

from fastapi import APIRouter
from starlette.requests import Request

from starwhale.consts import SW_AUTO_DIRNAME
from starwhale.utils.fs import ensure_file
from starwhale.web.response import success, SuccessResp

router = APIRouter()
prefix = "panel"


def _setting_path() -> str:
    return os.path.join(os.getcwd(), SW_AUTO_DIRNAME, "panel.json")


@router.post("/setting/{project}/{key}")
async def setting(project: str, key: str, request: Request) -> SuccessResp:
    content = await request.body()
    ensure_file(_setting_path(), content.decode("utf-8"), parents=True)
    return success({})


@router.get("/setting/{project}/{key}")
def get_setting(project: str, key: str) -> SuccessResp:
    file = Path(_setting_path())
    if not file.exists() or not file.is_file():
        return success("")
    with file.open("r") as f:
        return success(f.read())
