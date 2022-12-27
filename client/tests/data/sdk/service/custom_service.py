import json
import typing as t

import gradio

from starwhale.api.service import Service


class CustomService(Service):
    def serve(self, addr: str, port: int) -> None:
        raise Exception(json.dumps((addr, port)))


svc = CustomService()


class CustomInput(gradio.Text):
    ...


class CustomOutput(gradio.Json):
    ...


@svc.api(CustomInput(), CustomOutput())
def baz(data: t.Any) -> t.Any:
    return data
