from __future__ import annotations

import typing as t

from pydantic import BaseModel

from starwhale.base.client.models.models import StepSpec


class StepSpecClient(StepSpec):
    concurrency = 1
    replicas = 1
    func_name: str
    module_name: str
    cls_name: t.Optional[str]
    extra_args: t.Optional[t.List]
    extra_kwargs: t.Optional[t.Dict[str, t.Any]]


class JobHandlers(BaseModel):
    __root__: t.Dict[str, t.List[StepSpecClient]]


class File(BaseModel):
    arcname: t.Optional[str]
    desc: str
    name: str
    path: str
    signature: str
    size: t.Optional[int]
    duplicate_check: bool


class Files(BaseModel):
    __root__: t.List[File]


class LocalModelInfo(BaseModel):
    name: str
    version: str
    project: str
    path: str
    tags: t.Optional[t.List[str]]
    handlers: t.Dict[str, t.List[StepSpecClient]]
    model_yaml: str
    files: t.Optional[t.List[File]]
