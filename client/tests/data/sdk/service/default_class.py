import typing as t

import gradio

from starwhale import PipelineHandler
from starwhale.api import service


class MyDefaultClass(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        for func in [self.ppl, self.handler_foo]:
            self.add_api(gradio.Text(), gradio.Json(), func, func.__name__)

    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return data

    def handler_foo(self, data: t.Any) -> t.Any:
        return

    @service.api(gradio.Text(), gradio.Json())
    def cmp(self, ppl_result: t.Iterator) -> t.Any:
        pass
