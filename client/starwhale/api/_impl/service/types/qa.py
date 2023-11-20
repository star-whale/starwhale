from __future__ import annotations

import inspect
from typing import Any, Set, List, Callable, Optional

from pydantic import BaseModel
from pydantic.dataclasses import dataclass

from .types import ServiceType, ComponentSpec


@dataclass
class Message:
    content: str
    role: str


class Query(BaseModel):
    user_input: str
    history: List[Message]
    confidence: Optional[float]
    top_k: Optional[float]
    top_p: Optional[float]
    temperature: Optional[float]
    max_length: Optional[int]


class QuestionAnswering(ServiceType):
    name = "question_answering"

    # TODO use pydantic model annotations generated arg_types
    arg_types = {
        "user_input": str,
        "history": list,  # list of Message
        "confidence": float,
        "top_k": float,
        "top_p": float,
        "temperature": float,
        "max_length": int,
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
            ComponentSpec(name=arg, type=self.arg_types[arg].__name__)
            for arg in self.args
        ]

    def router_fn(self, func: Callable) -> Callable:
        params = inspect.signature(func).parameters

        def wrapper(query: Query) -> Any:
            return func(**{k: getattr(query, k) for k in params})

        return wrapper
