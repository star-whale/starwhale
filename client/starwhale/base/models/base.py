from __future__ import annotations

import typing as t

from pydantic import BaseModel
from fastapi.encoders import jsonable_encoder


class SwBaseModel(BaseModel):
    class Config:
        arbitrary_types_allowed = True
        populate_by_name = True
        # https://docs.pydantic.dev/latest/api/config/#pydantic.config.ConfigDict.protected_namespaces
        protected_namespaces = ()

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
    name: t.Optional[str] = None
    version: t.Optional[str] = None
    owner: t.Optional[t.Union[str, int]] = None
    latest: bool = False
