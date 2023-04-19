from __future__ import annotations

import os
import typing as t
import functools
from dataclasses import dataclass

from starwhale.utils import in_production

if t.TYPE_CHECKING:
    from gradio import Blocks
    from gradio.components import Component

Input = t.Union["Component", t.List["Component"]]
Output = t.Union["Component", t.List["Component"]]
Examples = t.Union[t.List[t.Any], str]


@dataclass
class Api:
    input: Input
    output: Output
    func: t.Callable
    uri: str
    examples: t.Optional[Examples]

    def view_func(self, ins: t.Any = None) -> t.Callable:
        func = self.func
        if ins is not None:
            func = functools.partial(func, ins)
        return func


@dataclass
class Hijack:
    """
    Hijack options for online evaluation, useless for local usage.
    """

    # if hijack the submit button js logic
    submit: bool = False
    # the resource path serving on the server side
    # used for example resource render for console
    resource_path: t.Optional[str] = None


class Service:
    def __init__(self, hijack: t.Optional[Hijack] = None) -> None:
        self.apis: t.Dict[str, Api] = {}
        self.api_within_instance_map: t.Dict[str, t.Any] = {}
        self.example_resources: t.List[str] = []
        self.hijack = hijack

    # TODO: support function as input and output
    def api(
        self,
        _input: Input,
        output: Output,
        uri: t.Optional[str] = None,
        examples: t.Optional[Examples] = None,
    ) -> t.Any:
        def decorator(func: t.Any) -> t.Any:
            self.add_api(_input, output, func, uri or func.__name__, examples)
            return func

        return decorator

    # TODO: support checking duplication
    def add_api(
        self,
        _input: Input,
        output: Output,
        func: t.Callable,
        uri: str,
        examples: t.Optional[Examples] = None,
    ) -> None:
        if not isinstance(_input, list):
            _input = [_input]
        if not isinstance(output, list):
            output = [output]
        _api = Api(_input, output, func, uri, examples)
        self.apis[uri] = _api

    def add_api_instance(self, _api: Api) -> None:
        self.apis[_api.uri] = _api

    def get_spec(self) -> t.Any:
        # fast path
        if not self.apis:
            return {}
        server = self._gen_gradio_server()
        return server.get_config_file()

    def get_openapi_spec(self) -> t.Any:
        server = self._gen_gradio_server()
        return server.app.openapi()

    def _render_api(self, _api: Api, _inst: t.Any) -> None:
        import gradio
        from gradio.components import File, Image, Video, IOComponent

        js_func: t.Optional[str] = None
        if self.hijack and self.hijack.submit:
            js_func = "async(...x) => { typeof wait === 'function' && await wait(); return x; }"
        with gradio.Row():
            with gradio.Column():
                fn = _api.view_func(_inst)
                for i in _api.input:
                    gradio.components.get_component_instance(i, render=False).render()
                submit = gradio.Button("Submit")
                submit.click(fn, inputs=_api.input, outputs=_api.output, _js=js_func)
                # do not serve the useless examples in server instances
                # the console will render them even the models are not serving
                if _api.examples and not in_production():
                    example = gradio.Examples(
                        examples=_api.examples,
                        inputs=[i for i in _api.input if isinstance(i, IOComponent)],
                    )
                    if any(
                        isinstance(i, (File, Image, Video))
                        for i in example.dataset.components
                    ):
                        # examples should be a list of file path
                        # use flatten list
                        to_copy = [i for j in example.examples for i in j]
                        self.example_resources.extend(to_copy)
                        # change example resource path for online evaluation
                        # e.g. /path/to/example.png -> /workdir/src/.starwhale/examples/example.png
                        if self.hijack and self.hijack.resource_path:
                            for i in range(len(example.dataset.samples)):
                                for j in range(len(example.dataset.samples[i])):
                                    origin = example.dataset.samples[i][j]
                                    if origin in to_copy:
                                        name = os.path.basename(origin)
                                        example.dataset.samples[i][j] = os.path.join(
                                            self.hijack.resource_path, name
                                        )
            with gradio.Column():
                for i in _api.output:
                    gradio.components.get_component_instance(i, render=False).render()

    def _gen_gradio_server(self, title: t.Optional[str] = None) -> Blocks:
        import gradio

        with gradio.Blocks() as app:
            with gradio.Tabs():
                for name, api in self.apis.items():
                    with gradio.TabItem(label=api.uri):
                        self._render_api(api, self.api_within_instance_map.get(name))
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
        server = self._gen_gradio_server(title=title)
        server.launch(server_name=addr, server_port=port)


_svc = Service()


def api(
    _input: Input,
    output: Output,
    uri: t.Optional[str] = None,
    examples: t.Optional[Examples] = None,
) -> t.Any:
    return _svc.api(_input, output, uri, examples)


def internal_api_list() -> t.Dict[str, Api]:
    return _svc.apis
