import typing as t

import gradio

from starwhale.api.service import Service

svc = Service()


class CustomInput(gradio.Text):
    ...


class CustomOutput(gradio.Json):
    ...


class MyCustomClass:
    @svc.api(CustomInput(), CustomOutput(), uri="foo")
    def handler_foo(self, data: t.Any) -> t.Any:
        return


@svc.api(CustomInput(), CustomOutput())
def bar(data: t.Any) -> t.Any:
    return data
