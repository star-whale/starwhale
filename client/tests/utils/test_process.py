import typing as t
import unittest

from starwhale.utils.process import log_check_call


class TestCheckCall(unittest.TestCase):
    def test_check_call(self) -> None:
        save_logs = []

        def _log(s: t.Any) -> None:
            save_logs.append(s)

        log_check_call(
            ["date"],
            log=_log,
        )

        assert len(save_logs) >= 1

        with self.assertRaises(FileNotFoundError):
            log_check_call(["err"])

        assert len(save_logs) >= 1

        rc = log_check_call(["date"], capture_stdout=False)
        assert rc == 0
