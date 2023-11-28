from __future__ import annotations

import inspect
from typing import Any, Dict, Callable, Optional

from pydantic import BaseModel

from starwhale.base.client.models.models import (
    ComponentValueSpecInt,
    ComponentSpecValueType,
)
from starwhale.api._impl.service.types.types import ServiceType


class Query(BaseModel):
    prompt: str
    negative_prompt: Optional[str] = None
    sampling_steps: Optional[int] = None
    width: Optional[int] = None
    height: Optional[int] = None
    seed: Optional[int] = None
    batch_size: Optional[int] = None
    batch_count: Optional[int] = None


class TextToImage(ServiceType):
    name = "text_to_image"
    args = {}

    arg_types: Dict[str, ComponentSpecValueType] = {
        "prompt": ComponentSpecValueType.string,
        "negative_prompt": ComponentSpecValueType.string,
        "sampling_steps": ComponentSpecValueType.int,
        "width": ComponentSpecValueType.int,
        "height": ComponentSpecValueType.int,
        "seed": ComponentSpecValueType.int,
        "batch_size": ComponentSpecValueType.int,
        "batch_count": ComponentSpecValueType.int,
    }

    def __init__(
        self,
        sampling_steps: ComponentValueSpecInt | None = None,
        width: ComponentValueSpecInt | None = None,
        height: ComponentValueSpecInt | None = None,
        seed: ComponentValueSpecInt | None = None,
        batch_size: ComponentValueSpecInt | None = None,
        batch_count: ComponentValueSpecInt | None = None,
    ) -> None:
        for k, v in locals().items():
            if k == "self":
                continue
            if v is not None:
                self.args[k] = v

    def router_fn(self, func: Callable) -> Callable:
        params = inspect.signature(func).parameters

        def wrapper(query: Query) -> Any:
            return func(**{k: getattr(query, k) for k in params})

        return wrapper
