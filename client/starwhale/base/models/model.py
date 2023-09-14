from __future__ import annotations

import typing as t

from starwhale.base.models.base import SwBaseModel
from starwhale.base.client.models.models import ModelVo, StepSpec


class StepSpecClient(StepSpec):
    concurrency = 1
    replicas = 1
    func_name: str
    module_name: str
    cls_name: t.Optional[str]
    extra_args: t.Optional[t.List]
    extra_kwargs: t.Optional[t.Dict[str, t.Any]]


class JobHandlers(SwBaseModel):
    __root__: t.Dict[str, t.List[StepSpecClient]]


class File(SwBaseModel):
    arcname: t.Optional[str]
    desc: str
    name: str
    path: str
    signature: str
    size: t.Optional[int]
    duplicate_check: bool


class Files(SwBaseModel):
    __root__: t.List[File]


class LocalModelInfoBase(SwBaseModel):
    name: str
    version: str
    project: str
    path: str
    tags: t.Optional[t.List[str]]
    created_at: str
    is_removed: bool
    size: int


class LocalModelInfo(LocalModelInfoBase):
    handlers: t.Dict[str, t.List[StepSpecClient]]
    model_yaml: str
    files: t.Optional[t.List[File]]


ModelListType = t.Union[t.List[LocalModelInfoBase], t.List[ModelVo]]
