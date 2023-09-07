import typing

from pydantic import BaseModel
from pydantic.generics import GenericModel

Model = typing.TypeVar("Model")


class ResponseCode(BaseModel):
    code: str
    message: str


class ResponseMessage(GenericModel, typing.Generic[Model]):
    code: str
    message: str
    data: Model
