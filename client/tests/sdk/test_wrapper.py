import os
from unittest.mock import patch

from requests_mock import Mocker

from starwhale import Text
from starwhale.consts import HTTPMethod
from starwhale.api._impl import wrapper, data_store
from starwhale.consts.env import SWEnv

from .test_base import BaseTestCase


@patch.dict(os.environ, {"SW_TOKEN": "sw_token"})
class TestEvaluation(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()

    def tearDown(self) -> None:
        super().tearDown()
        os.environ.pop(SWEnv.instance_uri, None)
        os.environ.pop(SWEnv.instance_token, None)

    @Mocker()
    def test_gen_table_name(self, request_mock: Mocker) -> None:
        request_mock.request(
            HTTPMethod.GET,
            "http://localhost:80/api/v1/project/project-test",
            json={"data": {"id": 1, "name": "project-test"}},
        )

        eval = wrapper.Evaluation("123456", "project-test", instance="local")

        result_table_name = eval._eval_table_name("results")
        assert result_table_name == "eval/12/123456/results"

        table_name_1 = eval._get_storage_table_name("table-1")
        assert table_name_1 == "project/project-test/table-1"

        eval = wrapper.Evaluation(
            "123456", "project-test", instance="http://localhost:80"
        )

        result_table = eval._eval_table_name("results")
        result_table_name = eval._get_storage_table_name(result_table)
        assert result_table == "eval/12/123456/results"
        assert result_table_name == "project/1/eval/12/123456/results"

        table_name_1 = eval._get_storage_table_name("table-1")
        assert table_name_1 == "project/1/table-1"

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

    def test_dataset_update_with_sw_object(self) -> None:
        dataset = wrapper.Dataset("dt", "test")
        row = {"data/text": Text("my_text")}
        dataset.put("0", **row)
        dataset.close()

        self.assertEqual(
            [{"id": "0", "data/text": Text("my_text")}],
            list(dataset.scan("0", None)),
            "scan",
        )
