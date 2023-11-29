import typing as t

from starwhale import PipelineHandler
from starwhale.api import service
from starwhale.base.client.models.models import (
    ComponentValueSpecInt,
    ComponentValueSpecFloat,
)
from starwhale.api._impl.service.types.llm import LLMChat


class MyDefaultClass(PipelineHandler):
    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return data

    def handler_foo(self, data: t.Any) -> t.Any:
        return

    @service.api(
        inference_type=LLMChat(
            temperature=ComponentValueSpecFloat(default_val=0.5),
            top_k=ComponentValueSpecInt(default_val=1),
            max_new_tokens=ComponentValueSpecInt(default_val=64, max=1024),
        )
    )
    def cmp(self, top_p: float = 0.9) -> t.Any:
        pass
