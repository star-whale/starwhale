from __future__ import annotations

import typing as t

from pydantic import BaseModel
from fastapi.encoders import jsonable_encoder

from starwhale.utils.pydantic import PYDANTIC_V2

if PYDANTIC_V2:

    class SwBaseModel(BaseModel):  # pragma: no cover
        class Config:
            arbitrary_types_allowed = True
            populate_by_name = True
            protected_namespaces = ()

        def to_dict(
            self, by_alias: bool = True, exclude_none: bool = True
        ) -> t.Dict[str, t.Any]:
            return jsonable_encoder(self.model_dump(by_alias=by_alias, exclude_none=exclude_none))  # type: ignore

    from pydantic import RootModel  # type: ignore
    from pydantic.type_adapter import TypeAdapter

    class SequenceSwBaseModel(RootModel):  # type: ignore
        def __iter__(self) -> t.Iterator[t.Any]:  # type: ignore
            return iter(self.root)

        def __getitem__(self, item: t.Any) -> t.Any:
            return self.root[item]

        @property
        def data(self) -> t.Any:
            return self.root

    RespType = t.TypeVar("RespType", bound=SwBaseModel)

    def obj_to_model(obj: t.Any, model: t.Type[RespType]) -> RespType:
        return TypeAdapter(model).validate_python(obj)  # type: ignore

else:

    class SwBaseModel(BaseModel):  # type: ignore # pragma: no cover
        class Config:
            arbitrary_types_allowed = True
            allow_population_by_field_name = True
            smart_union = True

        def to_dict(
            self, by_alias: bool = True, exclude_none: bool = True
        ) -> t.Dict[str, t.Any]:
            return jsonable_encoder(self.dict(by_alias=by_alias, exclude_none=exclude_none))  # type: ignore

    class SequenceSwBaseModel(SwBaseModel):  # type: ignore
        def __iter__(self) -> t.Iterator[t.Any]:  # type: ignore
            return iter(self.__root__)  # type: ignore[attr-defined]

        def __getitem__(self, item: t.Any) -> t.Any:
            return self.__root__[item]  # type: ignore[attr-defined]

        @property
        def data(self) -> t.Any:
            return self.__root__  # type: ignore[attr-defined]

    RespType = t.TypeVar("RespType", bound=SwBaseModel)  # type: ignore[misc]

    def obj_to_model(obj: t.Any, model: t.Type[RespType]) -> RespType:
        return model.parse_obj(obj)


class ListFilter(SwBaseModel):
    name: t.Optional[str] = None
    version: t.Optional[str] = None
    owner: t.Optional[t.Union[str, int]] = None
    latest: bool = False
