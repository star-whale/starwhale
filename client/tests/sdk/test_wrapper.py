import os
from unittest.mock import patch, MagicMock

from requests_mock import Mocker

from starwhale import Text
from starwhale.consts import HTTPMethod
from starwhale.api._impl import wrapper, data_store
from starwhale.base.type import PredictLogMode
from starwhale.consts.env import SWEnv
from starwhale.base.uri.project import Project

from .. import BaseTestCase


@patch.dict(os.environ, {"SW_TOKEN": "sw_token"})
class TestEvaluation(BaseTestCase):
    def tearDown(self) -> None:
        super().tearDown()
        os.environ.pop(SWEnv.instance_uri, None)
        os.environ.pop(SWEnv.instance_token, None)

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_gen_table_name(self, request_mock: Mocker, m_conf: MagicMock) -> None:
        instance_uri = "http://1.1.1.1"
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "test": {
                    "uri": instance_uri,
                    "current_project": "p",
                    "sw_token": "abcd",
                },
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": self.local_storage},
        }
        request_mock.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/project-test",
            json={"data": {"id": 1, "name": "project-test"}},
        )

        eval = wrapper.Evaluation("123456-local", Project("project-test"))

        result_table_name = eval._eval_table_name("results")
        assert result_table_name == "eval/12/123456-local/results"

        table_name_1 = eval._get_storage_table_name("table-1")
        assert table_name_1 == "project/project-test/table-1"

        eval = wrapper.Evaluation(
            "123456-cloud", Project(name=f"{instance_uri}/project/project-test")
        )

        result_table = eval._eval_table_name("results")
        result_table_name = eval._get_storage_table_name(result_table)
        assert result_table == "eval/12/123456-cloud/results"
        assert result_table_name == "project/1/eval/12/123456-cloud/results"

        table_name_1 = eval._get_storage_table_name("table-1")
        assert table_name_1 == "project/1/table-1"

    def test_log_results_and_scan(self) -> None:
        e = wrapper.Evaluation("tt", Project("test"))
        e.log_result(dict(id="0", output=3))
        e.log_result(dict(id="1", output=4, _mode=PredictLogMode.PICKLE.value))
        e.log_result(
            dict(id="2", output=5, a="0", b="1", _mode=PredictLogMode.PLAIN.value)
        )
        e.log_result(dict(id="3", output=6, c=None, _mode="plain"))
        e.close()

        expect_result = [
            {"id": "0", "output": 3},
            {
                "id": "1",
                "output": 4,
                "_mode": "pickle",
            },
            {"id": "2", "output": 5, "a": "0", "b": "1", "_mode": "plain"},
            {"id": "3", "output": 6, "_mode": "plain"},
        ]
        self.assertEqual(expect_result, list(e.get_results()))
        self.assertEqual(expect_result, list(e.get("results")))

    def test_id_validation(self) -> None:
        e = wrapper.Evaluation("tt", Project("test"))
        with self.assertRaisesRegex(RuntimeError, "id is not set"):
            e.log("table/1", a=1)

        msg = "id should be str or int"
        with self.assertRaisesRegex(RuntimeError, msg):
            e.log("table/1", id=(1, 2))

        with self.assertRaisesRegex(RuntimeError, msg):
            e.log_result({"id": (1, 2), "a": 1})

    def test_log_summary_metrics(self) -> None:
        e = wrapper.Evaluation("tt", Project("test"))
        e.log_summary_metrics(a=0, B=1, c=None)
        e.log_summary_metrics({"a/b": 2})
        e.close()
        self.assertEqual(
            [{"id": "tt", "a": 0, "b": 1, "a/b": 2}],
            list(
                data_store.get_data_store().scan_tables(
                    [data_store.TableDesc("project/test/eval/summary")]
                )
            ),
        )

        e = wrapper.Evaluation("tt", Project("test"))
        assert e.get_summary_metrics() == {"a": 0, "b": 1, "a/b": 2, "id": "tt"}

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_exception_close(self, request_mock: Mocker, m_conf: MagicMock) -> None:
        instance_uri = "http://1.1.1.1"
        m_conf.return_value = {
            "current_instance": "test",
            "instances": {
                "test": {
                    "uri": instance_uri,
                    "current_project": "p",
                    "sw_token": "abcd",
                },
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": "/root"},
        }
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

        eval = wrapper.Evaluation("tt", Project("http://1.1.1.1/project/test"))
        eval.log_result(dict(id="0", mode=PredictLogMode.PICKLE.value, output=3))
        eval.log_summary_metrics({"a/b": 2})

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

    def test_get_tables_from_standalone(self) -> None:
        e = wrapper.Evaluation("tt", Project("test"))
        e.log("table/1", id=1, a=1)
        e.log("table/2", id=2, a=2)
        e.close()

        e = wrapper.Evaluation("tt", Project("test"))
        tables = e.get_tables()
        assert set(tables) == {"table/1", "table/2"}

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_get_tables_from_server(
        self, request_mock: Mocker, m_conf: MagicMock
    ) -> None:
        eval_id = "123456"
        project_name = "project-test"
        instance_uri = "http://1.1.1.1"
        m_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "test": {
                    "uri": instance_uri,
                    "current_project": "p",
                    "sw_token": "abcd",
                },
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": "/root"},
        }

        request_mock.request(
            HTTPMethod.GET,
            f"{instance_uri}/api/v1/project/project-test",
            json={"data": {"id": 1, "name": project_name}},
        )

        table_prefix = f"project/1/eval/{eval_id[:2]}/{eval_id}/"
        request_mock.request(
            HTTPMethod.POST,
            f"{instance_uri}/api/v1/datastore/listTables",
            json={
                "data": {
                    "tables": [f"{table_prefix}/table/1", f"{table_prefix}/table/2"]
                }
            },
        )

        e = wrapper.Evaluation(
            eval_id, Project(f"{instance_uri}/project/{project_name}")
        )
        tables = e.get_tables()
        assert set(tables) == {"table/1", "table/2"}


class TestDataset(BaseTestCase):
    def test_put_and_scan(self) -> None:
        dataset = wrapper.Dataset("dt", Project("test"))
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
        dataset = wrapper.Dataset("dt", Project("test"))
        row = {"data/text": Text("my_text")}
        dataset.put("0", **row)
        dataset.close()

        self.assertEqual(
            [{"id": "0", "data/text": Text("my_text")}],
            list(dataset.scan("0", None)),
            "scan",
        )

    def test_id_validation(self) -> None:
        dataset = wrapper.Dataset("dt", Project("test"))

        with self.assertRaisesRegex(RuntimeError, "id should be str or int"):
            dataset.put((1, 2), a=1)
