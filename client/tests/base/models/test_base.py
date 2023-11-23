import typing as t

from pydantic import Field

from starwhale.base.models.base import SwBaseModel


def test_to_dict():
    class Foo(SwBaseModel):
        int_field: int
        str_field: str
        bool_field: bool
        float_field: float = Field(0.0, alias="floatField")
        list_field: t.List[int]
        dict_field: t.Optional[t.Dict[str, int]] = Field(None, alias="dictField")
        opt_field: t.Optional[str] = None

    foo = Foo(
        int_field=1,
        str_field="foo",
        bool_field=True,
        float_field=1.0,
        list_field=[1, 2, 3],
        dict_field={"foo": 1, "bar": 2},
    )
    assert foo.to_dict() == {
        "int_field": 1,
        "str_field": "foo",
        "bool_field": True,
        "floatField": 1.0,
        "list_field": [1, 2, 3],
        "dictField": {"foo": 1, "bar": 2},
    }

    assert foo.to_dict(by_alias=False) == {
        "int_field": 1,
        "str_field": "foo",
        "bool_field": True,
        "float_field": 1.0,
        "list_field": [1, 2, 3],
        "dict_field": {"foo": 1, "bar": 2},
    }

    assert foo.to_dict(exclude_none=False) == {
        "int_field": 1,
        "str_field": "foo",
        "bool_field": True,
        "floatField": 1.0,
        "list_field": [1, 2, 3],
        "dictField": {"foo": 1, "bar": 2},
        "opt_field": None,
    }
