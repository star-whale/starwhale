import json
import typing as t

from starwhale.api.service import Request, Service, Response


class CustomService(Service):
    def serve(self, addr: str, port: int, handler_list: t.List[str] = None) -> None:
        raise Exception(json.dumps((addr, port, handler_list)))


svc = CustomService()


class CustomInput(Request):
    def load(self, req: t.Any) -> t.Any:
        return req


class CustomOutput(Response):
    def dump(self, req: t.Any) -> bytes:
        return req


@svc.api(request=CustomInput(), response=CustomOutput())
def baz(data: t.Any) -> t.Any:
    return data
