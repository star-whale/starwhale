from __future__ import annotations

import os
import sys
import typing as t
import functools

from starwhale.base.client.models.models import ApiSpec, ServiceSpec

if sys.version_info >= (3, 9):
    from importlib.resources import files
else:
    from importlib_resources import files

from fastapi import FastAPI
from pydantic import Field
from starlette.responses import FileResponse
from starlette.staticfiles import StaticFiles

from starwhale.utils import console
from starwhale.base.models.base import SwBaseModel

from .types import Inputs, Outputs, ServiceType, all_components_are_gradio

STATIC_DIR_DEV = os.getenv("SW_SERVE_STATIC_DIR") or str(
    files("starwhale").joinpath("web/ui")
)


class Api(SwBaseModel):
    func: t.Callable
    uri: str
    inference_type: t.Optional[ServiceType] = None
    # do not export inputs and outputs to spec for now
    inputs: Inputs = Field(exclude=True)
    outputs: Outputs = Field(exclude=True)

    def view_func(self, ins: t.Any = None) -> t.Callable:
        func = self.func
        if ins is not None:
            func = functools.partial(func, ins)
        if self.inference_type is None:
            return func
        return self.inference_type.router_fn(func)

    def all_gradio_components(self) -> bool:
        if self.inference_type is not None:
            return False
        return all_components_are_gradio(inputs=self.inputs, outputs=self.outputs)

    def to_spec(self) -> ApiSpec | None:
        if self.inference_type is None:
            return None

        return ApiSpec(
            uri=self.uri,
            inference_type=self.inference_type.name,
            components=self.inference_type.components_spec(),
        )


class Service:
    def __init__(self) -> None:
        self.apis: t.Dict[str, Api] = {}
        self.api_within_instance_map: t.Dict[str, t.Any] = {}

    def api(
        self,
        inference_type: ServiceType | None = None,
        inputs: Inputs = None,
        outputs: Outputs = None,
    ) -> t.Any:
        def decorator(func: t.Any) -> t.Any:
            self.add_api(
                func,
                func.__name__,
                inference_type=inference_type,
                inputs=inputs,
                outputs=outputs,
            )
            return func

        return decorator

    def get_spec(self) -> ServiceSpec | None:
        if not self.apis:
            return None
        return ServiceSpec(
            version="0.0.2",
            apis=list(filter(None, [_api.to_spec() for _api in self.apis.values()])),
        )

    def add_api(
        self,
        func: t.Callable,
        uri: str,
        inference_type: ServiceType | None = None,
        inputs: Inputs = None,
        outputs: Outputs = None,
    ) -> None:
        console.debug(f"add api {uri}")
        if uri in self.apis:
            old = self.apis[uri].func
            # the dest module will be force unloaded and reload when generating job yaml,
            # so we need to check if the module and function name are the same
            if old.__module__ != func.__module__ or old.__name__ != func.__name__:
                raise ValueError(f"Duplicate api uri: {uri}")

        if inference_type is not None:
            inference_type.update_from_func(func)

        _api = Api(
            func=func,
            uri=f"{uri}",
            inference_type=inference_type,
            inputs=inputs,
            outputs=outputs,
        )
        self.apis[uri] = _api

    def add_api_instance(self, _api: Api) -> None:
        self.apis[_api.uri] = _api

    def serve(
        self, addr: str, port: int, title: t.Optional[str] = None
    ) -> None:  # pragma: no cover
        title = title or "Starwhale Model Serving"
        # check if all the api uses gradio components
        # if so, use gradio to serve
        # otherwise, use fastapi to serve
        if all([_api.all_gradio_components() for _api in self.apis.values()]):
            self._serve_gradio(addr, port, title=title)
        else:
            self._serve_builtin(addr, port, title=title)

    def _serve_builtin(
        self, addr: str, port: int, title: str
    ) -> None:  # pragma: no cover
        app = FastAPI(title=title)

        @app.get("/api/spec")
        def spec() -> t.Union[ServiceSpec, None]:
            return self.get_spec()

        for _api in self.apis.values():
            app.add_api_route(
                f"/api/{_api.uri}",
                _api.view_func(self.api_within_instance_map.get(_api.uri)),
                methods=["POST"],
            )

        def index(opt: t.Any) -> FileResponse:
            return FileResponse(os.path.join(STATIC_DIR_DEV, "client/index.html"))

        app.add_route("/", index, methods=["GET"])
        app.mount("/", StaticFiles(directory=STATIC_DIR_DEV), name="assets")

        import uvicorn

        uvicorn.run(app, host=addr, port=port)

    def _serve_gradio(
        self, addr: str, port: int, title: str
    ) -> None:  # pragma: no cover
        import gradio

        def api_to_component(_api: Api) -> gradio.Interface:
            return gradio.Interface(
                fn=_api.view_func(self.api_within_instance_map.get(_api.uri)),
                inputs=_api.inputs,
                outputs=_api.outputs,
                title=_api.uri,
            )

        with gradio.blocks.Blocks(title=title) as app:
            if len(self.apis) == 1:
                # if only one api, use the main page
                api_to_component(list(self.apis.values())[0])
            else:
                # one tab for each api
                for _api in self.apis.values():
                    with gradio.Tab(label=_api.uri):
                        api_to_component(_api)
        app.launch(server_name=addr, server_port=port)


_svc = Service()


def api(
    inputs: Inputs = None,
    outputs: Outputs = None,
    inference_type: ServiceType | None = None,
) -> t.Any:
    return _svc.api(inference_type=inference_type, inputs=inputs, outputs=outputs)


def internal_api_list() -> t.Dict[str, Api]:
    return _svc.apis
