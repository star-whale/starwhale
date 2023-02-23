import unittest

from starwhale.utils.dict import flatten


class TestDict(unittest.TestCase):
    def test_flatten(self) -> None:
        cases = [
            ({"a": 1}, {"a": 1}, {"a": 1}),
            ({"a": {"b": 1, "c": 2}}, {"a/b": 1, "a/c": 2}, {"a/b": 1, "a/c": 2}),
            (
                {"a": {"b": 1, "c": 2, "d": [1, 2, 3]}},
                {"a/b": 1, "a/c": 2, "a/d/0": 1, "a/d/1": 2, "a/d/2": 3},
                {"a/b": 1, "a/c": 2, "a/d": [1, 2, 3]},
            ),
            ({"a": {"b": {"c": {"d": 1}}}}, {"a/b/c/d": 1}, {"a/b/c/d": 1}),
            (
                {"a": [1, {"b": 1}, 2]},
                {"a/0": 1, "a/b": 1, "a/2": 2},
                {"a": [1, {"b": 1}, 2]},
            ),
        ]

        for in_data, seq_out_data, out_data in cases:
            assert flatten(in_data, extract_sequence=True) == seq_out_data
            assert flatten(in_data) == out_data
