from __future__ import annotations

import inspect
from typing import Any, Set, Dict, List, Callable, Optional

from pydantic import BaseModel

from starwhale.base.client.models.models import ComponentSpecValueType

from .types import ServiceType, ComponentSpec


class Message(BaseModel):
    content: str
    role: str


class Query(BaseModel):
    user_input: str
    history: List[Message]
    top_k: Optional[int] = None
    top_p: Optional[float] = None
    temperature: Optional[float] = None
    max_new_tokens: Optional[int] = None


class LLMChat(ServiceType):
    name = "llm_chat"

    # TODO use pydantic model annotations generated arg_types
    arg_types: Dict[str, ComponentSpecValueType] = {
        "user_input": ComponentSpecValueType.string,
        "history": ComponentSpecValueType.list,  # list of Message
        "top_k": ComponentSpecValueType.int,
        "top_p": ComponentSpecValueType.float,
        "temperature": ComponentSpecValueType.float,
        "max_new_tokens": ComponentSpecValueType.int,
    }

    def __init__(self, args: Set | None = None) -> None:
        if args is None:
            args = set(self.arg_types.keys())
        else:
            # check if all args are in arg_types
            for arg in args:
                if arg not in self.arg_types:
                    raise ValueError(f"Argument {arg} is not in arg_types.")

        self.args = args

    def components_spec(self) -> List[ComponentSpec]:
        return [
            ComponentSpec(name=arg, component_spec_value_type=self.arg_types[arg])
            for arg in self.args
        ]

    def router_fn(self, func: Callable) -> Callable:
        params = inspect.signature(func).parameters

        def wrapper(query: Query) -> Any:
            return func(**{k: getattr(query, k) for k in params})

        return wrapper
