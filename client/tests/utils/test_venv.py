from unittest import TestCase

from starwhale.utils.venv import parse_python_version
from starwhale.utils.error import FormatError


class TestVenv(TestCase):
    def test_parse_python_version(self):
        ts = {
            "python3": {
                "major": 3,
                "minor": -1,
                "micro": -1,
            },
            "python3.7": {
                "major": 3,
                "minor": 7,
                "micro": -1,
            },
            "python3.7.1": {
                "major": 3,
                "minor": 7,
                "micro": 1,
            },
            "3.8": {
                "major": 3,
                "minor": 8,
                "micro": -1,
            },
        }

        for _k, _v in ts.items():
            _pvf = parse_python_version(_k)
            assert _pvf.major == _v["major"]
            assert _pvf.minor == _v["minor"]
            assert _pvf.micro == _v["micro"]

        self.assertRaises(ValueError, parse_python_version, "python")
        self.assertRaises(FormatError, parse_python_version, "")
