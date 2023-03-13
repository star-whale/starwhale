import typing as t
import os.path

import pkg_resources
from fastapi import FastAPI, APIRouter
from fastapi.responses import ORJSONResponse
from typing_extensions import Protocol
from starlette.requests import Request
from starlette.responses import FileResponse
from starlette.exceptions import HTTPException
from starlette.staticfiles import StaticFiles

from starwhale.web import user, panel, system, project, data_store

STATIC_DIR_DEV = pkg_resources.resource_filename("starwhale", "web/ui")


class Component(Protocol):
    router: APIRouter
    prefix: str


class Server(FastAPI):
    def __init__(self) -> None:
        super().__init__(default_response_class=ORJSONResponse)

    def add_component(self, component: Component) -> None:
        self.include_router(component.router, prefix=f"/{component.prefix.lstrip('/')}")

    @staticmethod
    def with_components(components: t.List[Component]) -> FastAPI:
        api = Server()
        for component in components:
            api.add_component(component)

        app = FastAPI()
        app.mount("/api/v1", api)

        @app.get("/")
        def index() -> FileResponse:
            return FileResponse(os.path.join(STATIC_DIR_DEV, "index.html"))

        app.mount("/", StaticFiles(directory=STATIC_DIR_DEV), name="assets")

        @app.exception_handler(404)
        def not_found_exception_handler(
            request: Request, exc: HTTPException
        ) -> FileResponse:
            return FileResponse(os.path.join(STATIC_DIR_DEV, "index.html"))

        return app

    @staticmethod
    def default() -> FastAPI:
        return Server.with_components([panel, project, data_store, user, system])


# for testing
if __name__ == "__main__":
    import uvicorn

    server = Server.default()
    uvicorn.run("server:server", host="127.0.0.1", port=8000, reload=True)
