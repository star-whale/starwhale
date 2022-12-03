import typing as t

from starwhale import Image
from starwhale.api.service import Request, Service, Response, JsonResponse

svc = Service()


class CustomInput(Request):
    def load(self, req: t.Any) -> t.Any:
        return req


class CustomOutput(Response):
    def __init__(self, prefix: str) -> None:
        self.prefix = prefix

    def dump(self, req: str) -> bytes:
        return f"{self.prefix} {req}".encode("utf-8")


class MyCustomClass:
    @svc.api(request=Image(), response=JsonResponse())
    def handler_foo(self, data: t.Any) -> t.Any:
        return

    @svc.api(request=CustomInput(), response=CustomOutput("hello"), uri="bar")
    def handler_bar(self, data: t.Any) -> t.Any:
        return


@svc.api(request=CustomInput(), response=CustomOutput(""))
def baz(data: t.Any) -> t.Any:
    return data
