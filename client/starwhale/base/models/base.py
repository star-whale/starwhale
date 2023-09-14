from __future__ import annotations

import typing as t

from pydantic import BaseModel


class SwBaseModel(BaseModel):
    class Config:
        arbitrary_types_allowed = True
        allow_population_by_field_name = True

    ...


class ListFilter(SwBaseModel):
    name: t.Optional[str]
    version: t.Optional[str]
    owner: t.Optional[t.Union[str, int]]
    latest: bool = False
