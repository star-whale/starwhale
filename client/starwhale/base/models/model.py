from __future__ import annotations

import typing as t

from pydantic import validator

from starwhale.utils.pydantic import PYDANTIC_V2
from starwhale.base.models.base import SwBaseModel, SequenceSwBaseModel
from starwhale.base.client.models.models import (
    ModelVo,
    StepSpec,
    OptionType,
    OptionField,
)


class StepSpecClient(StepSpec):
    concurrency: t.Optional[int] = 1  # concurrency is deprecated in the sdk side
    replicas: int = 1
    func_name: str
    module_name: str
    cls_name: t.Optional[str] = None
    extra_args: t.Optional[t.List] = None
    extra_kwargs: t.Optional[t.Dict[str, t.Any]] = None


# Current supported types(ref: (click types)[https://github.com/pallets/click/blob/main/src/click/types.py]):
# 1. primitive types: INT,FLOAT,BOOL,STRING
# 2. Func: FuncParamType, such as: debug: t.Union[str, t.List[DebugOption]] = dataclasses.field(default="", metadata={"help": "debug mode"})
#       we will convert FuncParamType to STRING type to simplify the input implementation. We ignore `func` field.
# 3. Choice: click.Choice type, add choices and case_sensitive options.
class OptionTypeClient(OptionType):
    case_sensitive: bool = False

    @validator("param_type", pre=True)
    def parse_param_type(cls, value: str) -> str:
        value = value.upper()
        return "STRING" if value == "FUNC" else value


class OptionFieldClient(OptionField):
    type: OptionTypeClient
    required: bool = False
    multiple: bool = False
    is_flag: bool = False

    @validator("default", pre=True)
    def parse_default(cls, value: str) -> str:
        return str(value)


class File(SwBaseModel):
    arcname: t.Optional[str] = None
    desc: str
    name: str
    path: str
    signature: str
    size: t.Optional[int] = None
    duplicate_check: bool


class LocalModelInfoBase(SwBaseModel):
    name: str
    version: str
    project: str
    path: str
    tags: t.Optional[t.List[str]] = None
    created_at: str
    is_removed: bool
    size: int


class LocalModelInfo(LocalModelInfoBase):
    handlers: t.Dict[str, t.List[StepSpecClient]]
    model_yaml: str
    files: t.Optional[t.List[File]] = None


ModelListType = t.Union[t.List[LocalModelInfoBase], t.List[ModelVo]]

if PYDANTIC_V2:

    class JobHandlers(SequenceSwBaseModel):
        root: t.Dict[str, t.List[StepSpecClient]]

    class Files(SequenceSwBaseModel):
        root: t.List[File]

else:

    class JobHandlers(SequenceSwBaseModel):  # type: ignore
        __root__: t.Dict[str, t.List[StepSpecClient]]

    class Files(SequenceSwBaseModel):  # type: ignore
        __root__: t.List[File]
