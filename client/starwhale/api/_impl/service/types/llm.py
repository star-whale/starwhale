from __future__ import annotations

import inspect
from typing import Any, Dict, List, Callable, Optional

from pydantic import BaseModel

from starwhale.base.client.models.models import (
    ComponentValueSpecInt,
    ComponentSpecValueType,
    ComponentValueSpecFloat,
)

from .types import ServiceType


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

    # TODO use pydantic model annotations generated arg_types
    arg_types: Dict[str, ComponentSpecValueType] = {
        "user_input": ComponentSpecValueType.string,
        "history": ComponentSpecValueType.list,  # list of Message
        "top_k": ComponentSpecValueType.int,
        "top_p": ComponentSpecValueType.float,
        "temperature": ComponentSpecValueType.float,
        "max_new_tokens": ComponentSpecValueType.int,
    }

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
