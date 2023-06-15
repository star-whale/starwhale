from fastapi import APIRouter

from starwhale.version import STARWHALE_VERSION
from starwhale.web.response import success, SuccessResp

router = APIRouter()
prefix = "system"


@router.get("/version")
def version() -> SuccessResp:
    return success(STARWHALE_VERSION)


@router.get("/features")
def features() -> SuccessResp:
    return success({"disabled": []})
