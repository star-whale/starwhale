import json
import os.path
from pathlib import Path

from fastapi import APIRouter
from starlette.requests import Request

from starwhale.utils import load_yaml
from starwhale.consts import (
    SW_AUTO_DIRNAME,
    EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME,
    EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME,
)
from starwhale.utils.fs import ensure_file
from starwhale.web.response import success, SuccessResp

router = APIRouter()
prefix = "panel"


def _setting_file() -> Path:
    base = Path(os.path.join(os.getcwd(), SW_AUTO_DIRNAME))
    yaml_fmt = base / EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME
    json_fmt = base / EVALUATION_PANEL_LAYOUT_JSON_FILE_NAME
    # only one of them should exist
    # we can not decide which one to use when both exist
    if yaml_fmt.exists() and json_fmt.exists():
        raise RuntimeError("Both yaml and json file exist, please remove one of them")

    # prefer using yaml config for local development (manual edit)
    if yaml_fmt.exists():
        return yaml_fmt
    return json_fmt


def _is_yaml_fmt(file: Path) -> bool:
    return file.name == EVALUATION_PANEL_LAYOUT_YAML_FILE_NAME


@router.post("/setting/{project}/{key}")
async def setting(project: str, key: str, request: Request) -> SuccessResp:
    content = await request.body()
    f = _setting_file()
    if _is_yaml_fmt(f):
        raise RuntimeError(
            "Can not save the console layout in yaml format when the manual edit yaml is exist"
        )
    ensure_file(f, content.decode("utf-8"), parents=True)
    return success({})


@router.get("/setting/{project}/{key}")
def get_setting(project: str, key: str) -> SuccessResp:
    f = _setting_file()
    if not f.exists() or not f.is_file():
        return success("")

    if _is_yaml_fmt(f):
        # render yaml to json for console
        content = load_yaml(f)
        return success(json.dumps(content))
    with f.open("r") as fd:
        return success(fd.read())
