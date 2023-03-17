import typing as t

from starwhale import PipelineHandler


class NoApi(PipelineHandler):
    def ppl(self, data: t.Any, **kw: t.Any) -> t.Any:
        pass

    def cmp(self, ppl_result: t.Iterator) -> t.Any:
        pass
