import typing

from starwhale.base.models.base import SwBaseModel

Model = typing.TypeVar("Model")


class ResponseCode(SwBaseModel):
    code: str
    message: str
