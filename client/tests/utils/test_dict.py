import unittest

from starwhale.utils.dict_util import flatten, transform_dict


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

    def test_transform(self) -> None:
        cases = [
            (
                {"a": [{"c": {"d": "e"}}, {"c": 4}]},
                {"a": "b"},
                {"b": [{"c": {"d": "e"}}, {"c": 4}]},
            ),
            ({"a": [{"c": {"d": "e"}}, {"c": 4}]}, {"a[0].c.d": "b"}, {"b": "e"}),
            (
                {"a": [{"c": {"d": "e"}}, {"c": 4}]},
                {"a[0].c.d": "b", "a[1].c": "c1"},
                {"b": "e", "c1": 4},
            ),
            ({"a": [{"c": {"d": "e"}}, {"c": 4}]}, {"a[0].d": "b"}, {}),
        ]
        for ori_dict, key_selector, expected_dict in cases:
            assert transform_dict(ori_dict, key_selector) == expected_dict
