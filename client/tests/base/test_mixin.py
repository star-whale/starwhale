import typing as t
from enum import Enum
from unittest import TestCase
from collections import namedtuple

from starwhale.base.mixin import ASDictMixin, _do_asdict_convert


class TestASDictMixin(TestCase):

    t_namedtuple = namedtuple("nt", ["a", "b", "c"])

    class TEnum(Enum):
        A_KEY = "a_value"
        B_KEY = "b_value"

    class TUsed(ASDictMixin):
        def __init__(self) -> None:
            self.a_key = "a_value"
            self.b_key = "b_value"

    class TUsedIgnore(TUsed):
        def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
            return super().asdict(ignore_keys=["b_key", "no_existed_key"])

    def test_mixin(self) -> None:
        _cases = [
            (self.TUsed().asdict(), {"a_key": "a_value", "b_key": "b_value"}),
            (self.TUsedIgnore().asdict(), {"a_key": "a_value"}),
        ]

        for _result, _expected in _cases:
            assert _result == _expected

    def test_convert(self) -> None:
        _cases = [
            ({"1": 2}, {"1": 2}),
            (
                {"1": [self.t_namedtuple(1, 2, 3), self.t_namedtuple(4, 5, 6)]},
                {"1": [{"a": 1, "b": 2, "c": 3}, {"a": 4, "b": 5, "c": 6}]},
            ),
            (
                {"1": self.TEnum.A_KEY},
                {"1": "a_value"},
            ),
            (1, 1),
            (self.TUsedIgnore(), {"a_key": "a_value"}),
            (
                {
                    "a": {"b": [self.TUsed(), self.TUsedIgnore()]},
                    "b": self.TUsedIgnore(),
                    "c": (1, 2, 3),
                },
                {
                    "a": {
                        "b": [
                            {"a_key": "a_value", "b_key": "b_value"},
                            {"a_key": "a_value"},
                        ]
                    },
                    "b": {"a_key": "a_value"},
                    "c": (1, 2, 3),
                },
            ),
        ]
        for _obj, _expected in _cases:
            assert _expected == _do_asdict_convert(_obj)
