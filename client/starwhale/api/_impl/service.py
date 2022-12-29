import typing as t
import functools
from dataclasses import dataclass

import gradio
from gradio.components import Component

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
        _api = Api(input_, output, func, uri)
        self.apis[uri] = _api

    def add_api_instance(self, api_: Api) -> None:
        self.apis[api_.uri] = api_

    def get_spec(self) -> t.Any:
        server = self._gen_gradio_server()
        return server.get_config_file()

    def get_openapi_spec(self) -> t.Any:
        server = self._gen_gradio_server()
        return server.app.openapi()

    def _gen_gradio_server(self) -> gradio.Blocks:
        apis = self.apis.values()
        return gradio.TabbedInterface(
            interface_list=[
                gradio.Interface(
                    fn=api_.view_func(self.api_instance),
                    inputs=api_.input,
                    outputs=api_.output,
                )
                for api_ in apis
            ],
            tab_names=[api_.uri for api_ in apis],
        )

    def serve(self, addr: str, port: int) -> None:
        """
        Default serve implementation, users can override this method
        :param addr
        :param port
        :return: None
        """
        server = self._gen_gradio_server()
        server.launch(server_name=addr, server_port=port)


_svc = Service()


def api(input_: Input, output: Output, uri: t.Optional[str] = None) -> t.Any:
    return _svc.api(input_, output, uri)


def internal_api_list() -> t.Dict[str, Api]:
    return _svc.apis
