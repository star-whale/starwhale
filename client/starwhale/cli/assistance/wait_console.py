from fastapi import FastAPI
from pydantic import BaseModel
from starlette.middleware.cors import CORSMiddleware

from starwhale.utils import console
from starwhale.core.model.model import Model
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.model import Dataset
from starwhale.core.runtime.model import Runtime


class Command(BaseModel):
    url: str
    token: str


def start() -> None:
    app = FastAPI()
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.on_event("startup")
    async def on_startup() -> None:
        console.print("Starwhale is ready to serve :rocket:")

    @app.get("/alive")
    async def alive() -> dict:
        return {"message": "alive"}

    @app.post("/resource")
    async def resource(request: Command) -> dict:
        rc = Resource(uri=request.url, token=request.token)
        if rc.typ == ResourceType.model:
            Model.copy(src_uri=rc, dest_uri="", dest_local_project_uri=".")
        elif rc.typ == ResourceType.dataset:
            Dataset.copy(src_uri=rc, dest_uri="", dest_local_project_uri=".")
        elif rc.typ == ResourceType.runtime:
            Runtime.copy(src_uri=rc, dest_uri="", dest_local_project_uri=".")
        else:
            raise ValueError(f"Unknown resource type: {rc.typ}")

        console.print(f"Downloaded {rc.typ.value} from {rc.full_uri}")
        return {"message": "download done"}

    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8007, log_level="error")
