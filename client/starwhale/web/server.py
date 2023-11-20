import typing as t
import os.path
from urllib.parse import urlparse

import httpx
from fastapi import FastAPI, APIRouter
from fastapi.responses import ORJSONResponse
from typing_extensions import Protocol
from starlette.requests import Request
from starlette.responses import Response, FileResponse, StreamingResponse
from starlette.background import BackgroundTask
from starlette.exceptions import HTTPException
from starlette.staticfiles import StaticFiles

from starwhale.web import user, panel, system, project, data_store
from starwhale.base.uri.instance import Instance
from starwhale.api._impl.service.service import STATIC_DIR_DEV


class Component(Protocol):
    router: APIRouter
    prefix: str


class Server(FastAPI):
    def __init__(self) -> None:
        super().__init__(default_response_class=ORJSONResponse)

    def add_component(self, component: Component) -> None:
        self.include_router(component.router, prefix=f"/{component.prefix.lstrip('/')}")

    @classmethod
    def mount_static(cls, app: FastAPI) -> FastAPI:
        def index() -> FileResponse:
            return FileResponse(os.path.join(STATIC_DIR_DEV, "index.html"))

        app.add_route("/", index, methods=["GET"])
        app.mount("/", StaticFiles(directory=STATIC_DIR_DEV), name="assets")

        def not_found_exception_handler(
            request: Request, exc: HTTPException
        ) -> FileResponse:
            return FileResponse(os.path.join(STATIC_DIR_DEV, "index.html"))

        app.add_exception_handler(404, not_found_exception_handler)

        return app

    @classmethod
    def with_components(cls, components: t.List[Component]) -> FastAPI:
        api = Server()
        for component in components:
            api.add_component(component)

        app = FastAPI()
        app.mount("/api/v1", api)
        return cls.mount_static(app)

    @classmethod
    def default(cls) -> FastAPI:
        return cls.with_components([panel, project, data_store, user, system])

    @classmethod
    def proxy(cls, instance: Instance) -> FastAPI:
        app = FastAPI()
        client = httpx.AsyncClient(base_url=instance.url)
        host = urlparse(instance.url).netloc

        async def proxy(request: Request) -> Response:
            url = httpx.URL(path=request.url.path, params=request.query_params)
            headers = request.headers.mutablecopy()
            headers.update({"Authorization": instance.token, "Host": host})
            content = await request.body()
            req = client.build_request(
                request.method, url, content=content, headers=headers
            )
            resp = await client.send(req, stream=True)
            return StreamingResponse(
                resp.aiter_bytes(),
                status_code=resp.status_code,
                headers=resp.headers,
                background=BackgroundTask(resp.aclose),
            )

        # serve panel related api in local mode for customizing panel
        app.include_router(panel.router, prefix="/api/v1/panel")
        app.add_route(
            "/api/v1/{path:path}",
            proxy,
            methods=["GET", "POST", "PUT", "DELETE", "PATCH", "HEAD"],
        )

        return cls.mount_static(app)


# for testing
if __name__ == "__main__":
    import uvicorn

    server = Server.default()
    uvicorn.run("server:server", host="127.0.0.1", port=8000, reload=True)
