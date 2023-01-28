import typing as t
import functools
from dataclasses import dataclass

import gradio
from gradio.components import Component

from starwhale.utils import in_production

Input = t.Union[Component, t.List[Component]]
Output = t.Union[Component, t.List[Component]]


@dataclass
class Api:
    input: Input
    output: Output
    func: t.Callable
    uri: str

    def view_func(self, ins: t.Any = None) -> t.Callable:
        func = self.func
        if ins is not None:
            func = functools.partial(func, ins)
        return func


class Service:
    def __init__(self) -> None:
        self.apis: t.Dict[str, Api] = {}
        self.api_instance: t.Any = None

    # TODO: support function as input and output
    def api(self, input_: Input, output: Output, uri: t.Optional[str] = None) -> t.Any:
        def decorator(func: t.Any) -> t.Any:
            self.add_api(input_, output, func, uri or func.__name__)
            return func

        return decorator

    # TODO: support checking duplication
    def add_api(
        self, input_: Input, output: Output, func: t.Callable, uri: str
    ) -> None:
        if not isinstance(input_, list):
            input_ = [input_]
        if not isinstance(output, list):
            output = [output]
        _api = Api(input_, output, func, uri)
        self.apis[uri] = _api

    def add_api_instance(self, api_: Api) -> None:
        self.apis[api_.uri] = api_

    def get_spec(self) -> t.Any:
        # fast path
        if not self.apis:
            return {}
        # hijack_submit set to True for generating config for console (On-Premises)
        server = self._gen_gradio_server(hijack_submit=True)
        return server.get_config_file()

    def get_openapi_spec(self) -> t.Any:
        server = self._gen_gradio_server(hijack_submit=True)
        return server.app.openapi()

    def _render_api(self, _api: Api, hijack_submit: bool) -> None:
        js_func: t.Optional[str] = None
        if hijack_submit:
            js_func = "async(...x) => { typeof wait === 'function' && await wait(); return x; }"
        with gradio.Row():
            with gradio.Column():
                for i in _api.input:
                    comp = gradio.components.get_component_instance(
                        i, render=False
                    ).render()
                    if isinstance(comp, gradio.components.Changeable):
                        comp.change(
                            _api.view_func(self.api_instance),
                            i,
                            _api.output,
                            _js=js_func,
                        )
            with gradio.Column():
                for i in _api.output:
                    gradio.components.get_component_instance(i, render=False).render()

    def _gen_gradio_server(
        self, hijack_submit: bool, title: t.Optional[str] = None
    ) -> gradio.Blocks:
        apis = self.apis.values()
        with gradio.Blocks() as app:
            with gradio.Tabs():
                for _api in apis:
                    with gradio.TabItem(label=_api.uri):
                        self._render_api(_api, hijack_submit)
        app.title = title or "starwhale"
        return app

    def serve(self, addr: str, port: int, title: t.Optional[str] = None) -> None:
        """
        Default serve implementation, users can override this method
        :param addr
        :param port
        :param title webpage title
        :return: None
        """
        server = self._gen_gradio_server(hijack_submit=in_production(), title=title)
        server.launch(server_name=addr, server_port=port)


_svc = Service()


def api(input_: Input, output: Output, uri: t.Optional[str] = None) -> t.Any:
    return _svc.api(input_, output, uri)


def internal_api_list() -> t.Dict[str, Api]:
    return _svc.apis
