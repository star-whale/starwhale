import json
import dataclasses
from enum import Enum
from typing import Any

from pydantic import BaseModel


class Encoder(json.JSONEncoder):
    def default(self, o: Any) -> Any:
        if isinstance(o, bytes):
            return o.decode("utf-8")
        if isinstance(o, BaseModel):
            return json.loads(o.json(exclude_unset=True))
        if isinstance(o, Enum):
            return o.value
        if dataclasses.is_dataclass(o):
            return dataclasses.asdict(o)
        return super().default(o)
