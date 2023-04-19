import typing as t

import gradio

from starwhale import PipelineHandler
from starwhale.api import service


class MyDefaultClass(PipelineHandler):
    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        return data

    def handler_foo(self, data: t.Any) -> t.Any:
        return

    @service.api(gradio.Text(), gradio.Json())
    def cmp(self, ppl_result: t.Iterator) -> t.Any:
        pass
