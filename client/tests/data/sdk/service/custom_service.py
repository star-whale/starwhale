import json
import typing as t

from starwhale.api.service import Input, Service, JsonOutput
from starwhale.base.spec.openapi.components import RequestBody, SpecComponent


class CustomService(Service):
    def serve(self, addr: str, port: int, handler_list: t.List[str] = None) -> None:
        raise Exception(json.dumps((addr, port, handler_list)))


svc = CustomService()


class CustomInput(Input):
    def load(self, req: t.Any) -> t.Any:
        return req

    def spec(self) -> SpecComponent:
        req = RequestBody(description="starwhale builtin model serving specification")
        return dict(requestBody=req)


class CustomOutput(JsonOutput):
    def dump(self, req: t.Any) -> bytes:
        return req


@svc.api(CustomInput(), CustomOutput())
def baz(data: t.Any) -> t.Any:
    return data
