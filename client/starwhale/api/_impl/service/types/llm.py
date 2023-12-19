from __future__ import annotations

import inspect
from typing import Any, List, Callable, Optional

from pydantic import BaseModel

from starwhale.base.client.models.models import (
    ComponentValueSpecInt,
    ComponentValueSpecFloat,
)

from .types import ServiceType, generate_type_definition


class MessageItem(BaseModel):
    content: str
    bot: bool = False


class Query(BaseModel):
    user_input: str
    history: List[MessageItem]
    top_k: Optional[int] = None
    top_p: Optional[float] = None
    temperature: Optional[float] = None
    max_new_tokens: Optional[int] = None


class LLMChat(ServiceType):
    name = "llm_chat"
    args = {}
    arg_types = generate_type_definition(Query)
    Message = MessageItem

    def __init__(
        self,
        top_k: ComponentValueSpecInt | None = None,
        top_p: ComponentValueSpecFloat | None = None,
        temperature: ComponentValueSpecFloat | None = None,
        max_new_tokens: ComponentValueSpecInt | None = None,
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
