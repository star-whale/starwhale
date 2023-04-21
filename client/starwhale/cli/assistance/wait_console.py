from fastapi import FastAPI
from pydantic import BaseModel
from starlette.middleware.cors import CORSMiddleware

from starwhale.base.uri.resource import Resource, ResourceType


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

    @app.get("/alive")
    async def alive() -> dict:
        return {"message": "alive"}

    @app.post("/resource")
    async def resource(request: Command) -> dict:
        rc = Resource(uri=request.url, token=request.token)
        if rc.typ == ResourceType.model:
            from starwhale.core.model.copy import ModelCopy

            ModelCopy(
                src_uri=rc, dest_uri="local/project/self", typ=ResourceType.model
            ).do()

        return {"message": "download done"}

    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8007, log_level="error")
