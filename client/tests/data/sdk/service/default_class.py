import typing as t

from starwhale import PipelineHandler
from starwhale.api import service
from starwhale.api._impl.service.types import ServiceType


class MyDefaultClass(PipelineHandler):
    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return data

    def handler_foo(self, data: t.Any) -> t.Any:
        return

    @service.api(ServiceType.QUESTION_ANSWERING)
    def cmp(self, ppl_result: t.Iterator) -> t.Any:
        pass
