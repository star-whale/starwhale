from __future__ import annotations

import typing as t

from pydantic import BaseModel
from fastapi.encoders import jsonable_encoder


class SwBaseModel(BaseModel):
    class Config:
        arbitrary_types_allowed = True
        allow_population_by_field_name = True
        # smart_union=True is used to make sure that pydantic will not convert str to int automatically
        smart_union = True

    def to_dict(
        self, by_alias: bool = True, exclude_none: bool = True
    ) -> t.Dict[str, t.Any]:
        """
        Convert a pydantic model to a dict.
        Args:
            by_alias: use alias or not, default is True.
                e.g. if you have a field named `user_name` in your model,
                and you set `by_alias=True`, then the key in the dict will be `userName`, otherwise it will be `user_name`
            exclude_none: exclude None value or not, default is True.
        """
        return jsonable_encoder(self.dict(by_alias=by_alias, exclude_none=exclude_none))  # type: ignore


class ListFilter(SwBaseModel):
    name: t.Optional[str]
    version: t.Optional[str]
    owner: t.Optional[t.Union[str, int]]
    latest: bool = False
