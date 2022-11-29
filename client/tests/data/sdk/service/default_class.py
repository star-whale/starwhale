import typing as t

from starwhale import PipelineHandler, PPLResultIterator
from starwhale.api import service


class Input(service.Request):
    def load(self, req: t.Any) -> t.Any:
        return req


class Output(service.Response):
    def dump(self, resp: t.Any) -> bytes:
        return resp


class MyDefaultClass(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        for func in [self.ppl, self.handler_foo]:
            self.add_api(Input(), Output(), func, func.__name__)

    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return data

    def handler_foo(self, data: t.Any) -> t.Any:
        return

    def cmp(self, ppl_result: PPLResultIterator) -> t.Any:
        pass
