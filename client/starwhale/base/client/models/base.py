import typing

from pydantic import BaseModel

Model = typing.TypeVar("Model")


class ResponseCode(BaseModel):
    code: str
    message: str
