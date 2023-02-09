import os

from requests_mock import Mocker

from starwhale.consts import HTTPMethod
from starwhale.api._impl import wrapper, data_store
from starwhale.consts.env import SWEnv

from .test_base import BaseTestCase


class TestEvaluation(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

    def tearDown(self) -> None:
        super().tearDown()
        os.environ.pop(SWEnv.instance_uri, None)
        os.environ.pop(SWEnv.instance_token, None)

    def test_log_results_and_scan(self) -> None:
        eval = wrapper.Evaluation("tt", "test")
        eval.log_result("0", 3)
        eval.log_result("1", 4)
        eval.log_result("2", 5, a="0", B="1")
        eval.log_result("3", 6, c=None)
        eval.close()
        self.assertEqual(
            [
                {"id": "0", "result": 3},
                {"id": "1", "result": 4},
                {"id": "2", "result": 5, "a": "0", "b": "1"},
                {"id": "3", "result": 6},
            ],
            list(eval.get_results()),
        )

    def test_log_metrics(self) -> None:
        eval = wrapper.Evaluation("tt", "test")
        eval.log_metrics(a=0, B=1, c=None)
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

    @Mocker()
    def test_exception_close(self, request_mock: Mocker) -> None:
        request_mock.request(
            HTTPMethod.GET,
            "http://1.1.1.1/api/v1/project/test",
            json={"data": {"id": 1, "name": "self"}},
        )

        request_mock.request(
            HTTPMethod.POST,
            url="http://1.1.1.1/api/v1/datastore/updateTable",
            status_code=400,
        )

        os.environ[SWEnv.instance_token] = "abcd"
        os.environ[SWEnv.instance_uri] = "http://1.1.1.1"
        eval = wrapper.Evaluation("tt", "test")
        eval.log_result("0", 3)
        eval.log_metrics({"a/b": 2})

        assert len(eval._writers) == 2
        with self.assertRaises(Exception) as twe:
            eval.close()

        assert len(twe.exception.args) == 2
        for e in twe.exception.args:
            assert isinstance(e, data_store.TableWriterException)

        for _writer in eval._writers.values():
            assert _writer is not None
            assert not _writer.is_alive()
            assert _writer._stopped
            assert len(_writer._queue_run_exceptions) == 0


class TestDataset(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

    def test_put_and_scan(self) -> None:
        dataset = wrapper.Dataset("dt", "test")
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

        self.assertEqual(
            [{"id": f"{i}"} for i in range(0, 4)],
            list(dataset.scan_id(None, None)),
            "scan_id",
        )
