import typing as t

from starwhale import PipelineHandler
from starwhale.api import service
from starwhale.api._impl.service.types.llm import LLMChat


class MyDefaultClass(PipelineHandler):
    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return data

    def handler_foo(self, data: t.Any) -> t.Any:
        return

    @service.api(inference_type=LLMChat(args={"user_input", "history", "temperature"}))
    def cmp(self, ppl_result: t.Iterator) -> t.Any:
        pass
