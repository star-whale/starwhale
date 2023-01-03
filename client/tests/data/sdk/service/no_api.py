import typing as t

from starwhale import PipelineHandler, PPLResultIterator


class NoApi(PipelineHandler):
    def ppl(self, data: t.Any, **kw: t.Any) -> t.Any:
        pass

    def cmp(self, ppl_result: PPLResultIterator) -> t.Any:
        pass
