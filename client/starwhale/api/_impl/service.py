import json
import base64
import typing as t
from abc import ABC, abstractmethod
from dataclasses import dataclass

from starwhale.core.dataset.type import MIMEType, ArtifactType, BaseArtifact


class Request(ABC):
    @abstractmethod
    def load(self, req: t.Any) -> t.Any:
        raise NotImplementedError()


class Response(ABC):
    # TODO: support mime_type and body
    @abstractmethod
    def dump(self, resp: t.Any) -> bytes:
        raise NotImplementedError()


class GrayscaleImageRequest(Request):
    def __init__(
        self, shape: t.Optional[t.List] = None, image_key: t.Optional[str] = None
    ):
        self.shape = shape
        self.image_key = image_key or "img"

    def load(self, req: t.Any) -> BaseArtifact:
        if isinstance(req, (str, bytes)):
            raw = base64.b64decode(req)
        elif isinstance(req, dict) and self.image_key:
            if self.image_key not in req:
                raise Exception("can not get image")
            raw = base64.b64decode(req[self.image_key])
        else:
            raise Exception("can not get image from unknown request type")
        return BaseArtifact.reflect(
            raw,
            {
                "type": ArtifactType.Image.value,
                "mime_type": MIMEType.GRAYSCALE,
                "shape": self.shape,
            },
        )


class JsonResponse(Response):
    def dump(self, resp: t.Any) -> bytes:
        return json.dumps(resp).encode("utf8")


# TODO: make Api virtual class
@dataclass
class Api:
    request: Request
    response: Response
    func: t.Callable
    uri: str

    def to_yaml(self) -> str:
        return self.func.__qualname__

    def view_func(self, req: t.Any) -> bytes:
        i = self.request.load(req)
        resp = self.func(i)
        return self.response.dump(resp)


class Service:
    def __init__(self) -> None:
        self.apis: t.Dict[str, Api] = {}

    def api(
        self, request: Request, response: Response, uri: t.Optional[str] = None
    ) -> t.Any:
        def decorator(func: t.Any) -> t.Any:
            self.add_api(request, response, func, uri or func.__name__)
            return func

        return decorator

    # TODO: support checking duplication
    def add_api(
        self, request: Request, response: Response, func: t.Callable, uri: str
    ) -> None:
        _api = Api(request, response, func, uri)
        self.apis[uri] = _api

    def add_api_instance(self, api: Api) -> None:
        self.apis[api.uri] = api

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
        import flask
        from flask.typing import RouteCallable

        def _view_wrapper(func: t.Callable[..., bytes]) -> RouteCallable:
            def wrapper() -> bytes:
                mime = flask.request.content_type.split(";")[0]
                if mime == "application/json":
                    data = json.loads(flask.request.data.decode("utf-8"))
                else:
                    data = flask.request.data
                return func(data)

            return wrapper

        app = flask.Flask(__name__)
        apis = self.apis
        if handler_list:
            apis = {uri: apis[uri] for uri in apis if uri in handler_list}
        for uri, api in apis.items():
            app.add_url_rule(
                rule=f"/{uri}",
                view_func=_view_wrapper(api.view_func),
                methods=["POST"],
            )
        app.run(addr, port)
