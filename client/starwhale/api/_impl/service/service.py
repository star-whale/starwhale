from __future__ import annotations

import os
import typing as t
import functools

import pkg_resources
from fastapi import FastAPI
from pydantic import BaseModel
from starlette.responses import FileResponse
from starlette.staticfiles import StaticFiles

from starwhale.base.models.base import SwBaseModel

from .types import ServiceType

STATIC_DIR_DEV = os.getenv("SW_SERVE_STATIC_DIR") or pkg_resources.resource_filename(
    "starwhale", "web/ui"
)


class Query(BaseModel):
    content: str


class Api(SwBaseModel):
    func: t.Callable
    uri: str
    inference_type: ServiceType

    @staticmethod
    def question_answering(func: t.Callable) -> t.Callable:
        def inter(query: Query) -> str:
            return func(query.content)  # type: ignore

        return inter

    def view_func(self, ins: t.Any = None) -> t.Callable:
        func = self.func
        if ins is not None:
            func = functools.partial(func, ins)
        return getattr(self, self.inference_type.value)(func)  # type: ignore


class ServiceSpec(SwBaseModel):
    title: t.Optional[str]
    description: t.Optional[str]
    version: str
    apis: t.List[Api]


class Service:
    def __init__(self) -> None:
        self.apis: t.Dict[str, Api] = {}
        self.api_within_instance_map: t.Dict[str, t.Any] = {}

    def api(self, inference_type: ServiceType) -> t.Any:
        def decorator(func: t.Any) -> t.Any:
            self.add_api(func, func.__name__, inference_type=inference_type)
            return func

        return decorator

    def get_spec(self) -> ServiceSpec:
        return ServiceSpec(version="0.0.1", apis=list(self.apis.values()))

    def add_api(self, func: t.Callable, uri: str, inference_type: ServiceType) -> None:
        if uri in self.apis:
            raise ValueError(f"Duplicate api uri: {uri}")

        _api = Api(func=func, uri=uri, inference_type=inference_type)
        self.apis[uri] = _api

    def add_api_instance(self, _api: Api) -> None:
        self.apis[_api.uri] = _api

    def serve(self, addr: str, port: int, title: t.Optional[str] = None) -> None:
        """
        Default serve implementation, users can override this method
        :param addr
        :param port
        :param title webpage title
        :return: None
        """
        app = FastAPI(title=title or "Starwhale Model Serving")

        @app.get("/api/spec")
        def spec() -> ServiceSpec:
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


_svc = Service()


def api(inference_type: ServiceType) -> t.Any:
    return _svc.api(inference_type=inference_type)


def internal_api_list() -> t.Dict[str, Api]:
    return _svc.apis
