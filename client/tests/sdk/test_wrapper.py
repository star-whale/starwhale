import os

from starwhale.api._impl import wrapper, data_store
from starwhale.consts.env import SWEnv

from .test_base import BaseTestCase


class TestEvaluation(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        os.environ[SWEnv.project] = "test"
        os.environ[SWEnv.eval_version] = "tt"

    def test_log_results_and_scan(self) -> None:
        eval = wrapper.Evaluation("test")
        eval.log_result("0", 3)
        eval.log_result("1", 4)
        eval.log_result("2", 5, a="0", B="1")
        eval.close()
        self.assertEqual(
            [
                {"id": "0", "result": 3},
                {"id": "1", "result": 4},
                {"id": "2", "result": 5, "a": "0", "b": "1"},
            ],
            list(eval.get_results()),
        )

    def test_log_metrics(self) -> None:
        eval = wrapper.Evaluation()
        eval.log_metrics(a=0, B=1)
        eval.log_metrics({"a/b": 2})
        eval.close()
        self.assertEqual(
            [{"id": "tt", "a": 0, "b": 1, "a/b": 2}],
            list(
                data_store.get_data_store().scan_tables(
                    [data_store.TableDesc("project/test/eval/summary")]
                )
            ),
        )


class TestDataset(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        os.environ[SWEnv.project] = "test"

    def test_put_and_scan(self) -> None:
        dataset = wrapper.Dataset("dt")
        dataset.put("0", a=1, b=2)
        dataset.put("1", a=2, b=3)
        dataset.put("2", a=3, b=4)
        dataset.put("3", a=4, b=5)
        dataset.close()
        self.assertEqual(
            [
                {"id": "1", "a": 2, "b": 3},
                {"id": "2", "a": 3, "b": 4},
                {"id": "3", "a": 4, "b": 5},
            ],
            list(dataset.scan("1", "4")),
            "scan",
        )
