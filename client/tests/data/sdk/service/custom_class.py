import typing as t

from starwhale import Image
from starwhale.api.service import Input, Service, JsonOutput
from starwhale.base.spec.openapi.components import (
    Schema,
    MediaType,
    RequestBody,
    SpecComponent,
)

svc = Service()


class CustomInput(Input):
    def load(self, req: t.Any) -> t.Any:
        return req

    def spec(self) -> SpecComponent:
        req = RequestBody(
            description="starwhale API",
            content={
                "multipart/form-data": MediaType(
                    schema=Schema(
                        type="object",
                        required=["data"],
                        properties={
                            "data": Schema(type="string", format="binary"),
                        },
                    ),
                )
            },
        )
        return SpecComponent(requestBody=req)


class CustomOutput(JsonOutput):
    def __init__(self, prefix: str) -> None:
        super().__init__()
        self.prefix = prefix

    def dump(self, req: str) -> bytes:
        return f"{self.prefix} {req}".encode("utf-8")


class MyCustomClass:
    @svc.api(Image(), JsonOutput())
    def handler_foo(self, data: t.Any) -> t.Any:
        return

    @svc.api(CustomInput(), CustomOutput("hello"), uri="bar")
    def handler_bar(self, data: t.Any) -> t.Any:
        return


@svc.api(CustomInput(), CustomOutput(""))
def baz(data: t.Any) -> t.Any:
    return data
