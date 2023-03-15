import typing

SuccessResp = typing.Dict[str, typing.Any]


def success(data: typing.Any) -> SuccessResp:
    return {
        "code": "success",
        "message": "Success",
        "data": data,
    }
