import json
import typing as t
import functools
from abc import ABC, abstractmethod
from dataclasses import dataclass

import flask

from starwhale.base.spec.openapi.components import (
    Schema,
    OpenApi,
    MediaType,
    SpecComponent,
    OpenApiResponse,
)

MIMETYPE_FIELD = "content-type"
MIMETYPE_JSON = "application/json"


class Request:
    headers: t.Mapping[str, str]
    body: bytes

    def __init__(self, headers: t.Mapping[str, str], body: bytes):
        self.headers = headers
        self.body = body

    @property
    def content_type(self) -> t.Optional[str]:
        return self.headers.get(MIMETYPE_FIELD)

    @property
    def is_json(self) -> bool:
        return self.content_type == MIMETYPE_JSON

    @classmethod
    def from_flask_request(cls, request: flask.Request) -> "Request":
        body = request.data
        if not body:
            # try uploaded files
            files = list(request.files.values())
            if len(files) > 0:
                # support only one file for now
                body = files[0].stream.read()
            else:
                # TODO: refactor request and support multiple files & form
                raise Exception("multiple files not supported")
        if not body:
            # try json
            if request.json is not None:
                body = json.dumps(request.json).encode("utf-8")

        return cls(request.headers, body)


class Response:
    body: bytes
    headers: t.Mapping[str, str]

    def __init__(
        self, body: bytes, headers: t.Optional[t.Mapping[str, str]] = None
    ) -> None:
        self.body = body
        self.headers = headers or {}


class Input(ABC):
    @abstractmethod
    def load(self, request: Request) -> t.Any:
        raise NotImplementedError

    @abstractmethod
    def spec(self) -> SpecComponent:
        """
        returns the specification of the request, used for generating the api specification
        see https://spec.openapis.org/oas/v3.0.0
        """
        raise NotImplementedError


class Output(ABC):
    @abstractmethod
    def dump(self, *args: t.Any, **kwargs: t.Any) -> Response:
        raise NotImplementedError

    @abstractmethod
    def spec(self) -> SpecComponent:
        raise NotImplementedError


class JsonOutput(Output):
    def __init__(self, resp_spec: t.Optional[OpenApiResponse] = None) -> None:
        self.resp_spec = resp_spec

    def dump(self, *args: t.Any, **kwargs: t.Any) -> Response:
        headers = {MIMETYPE_FIELD: MIMETYPE_JSON}
        return Response(json.dumps(*args, **kwargs).encode("utf-8"), headers)

    def spec(self) -> SpecComponent:
        resp = self.resp_spec or OpenApiResponse(
            description="OK",
            content={
                MIMETYPE_JSON: MediaType(
                    schema=Schema(
                        type="object",
                        properties={
                            "data": Schema(type="string"),
                        },
                    ),
                )
            },
        )
        return SpecComponent(responses={"200": resp})


# TODO: make Api virtual class
@dataclass
class Api:
    input: Input
    output: Output
    func: t.Callable
    uri: str

    def to_yaml(self) -> str:
        return self.func.__qualname__

    def view_func(self, req: Request, ins: t.Any = None) -> Response:
        i = self.input.load(req)
        func = self.func
        if ins is not None:
            func = functools.partial(func, ins)
        resp = func(i)
        return self.output.dump(resp)


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

    def get_spec(self) -> OpenApi:
        paths = dict()
        for i in self.apis.values():
            spec = i.input.spec()
            resp = i.output.spec().responses
            spec.responses = resp
            paths[i.uri if i.uri.startswith("/") else "/" + i.uri] = {"post": spec}
        return OpenApi(
            openapi="3.0.0",
            info={
                "title": "StarWhale API",
                "description": "StarWhale API",
                "version": "1.0.0",
            },
            paths=paths,
        )

    def serve(
        self, addr: str, port: int, handler_list: t.Optional[t.List[str]] = None
    ) -> None:
        """
        Default serve implementation, users can override this method
        :param addr
        :param port
        :param handler_list, use all handlers if None
        :return: None
        """
        from flask.typing import RouteCallable, ResponseReturnValue

        def _view_wrapper(func: t.Callable[..., Response]) -> RouteCallable:
            def wrapper() -> ResponseReturnValue:
                req = Request.from_flask_request(flask.request)
                resp = func(req, ins=self.api_instance)
                return resp.body, resp.headers

            return wrapper

        app = flask.Flask(__name__)
        apis = self.apis
        if handler_list:
            apis = {uri: apis[uri] for uri in apis if uri in handler_list}
        for uri, api_ in apis.items():
            app.add_url_rule(
                rule=f"/{uri}",
                endpoint=uri,
                view_func=_view_wrapper(api_.view_func),
                methods=["POST"],
            )
        app.run(addr, port)


_svc = Service()


def api(input_: Input, output: Output, uri: t.Optional[str] = None) -> t.Any:
    return _svc.api(input_, output, uri)


def internal_api_list() -> t.Dict[str, Api]:
    return _svc.apis
