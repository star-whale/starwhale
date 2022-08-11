import os
import re
import threading
from typing import Any, Dict, List, Iterator, Optional
from multiprocessing.connection import Connection

from starwhale.consts import EvaluationResultKind

from . import data_store


class Logger:
    def _init_writers(self, tables: List[str]) -> None:
        self._writers: Dict[str, Optional[data_store.TableWriter]] = {
            table: None for table in tables
        }
        self._lock = threading.Lock()

    def close(self) -> None:
        with self._lock:
            for writer in self._writers.values():
                if writer is not None:
                    writer.close()

    def _log(self, table_name: str, record: Dict[str, Any]) -> None:
        with self._lock:
            writer = self._writers[table_name]
            if writer is None:
                writer = data_store.TableWriter(table_name)
                self._writers[table_name] = writer
        writer.insert(record)


class Evaluation(Logger):
    def __init__(self, eval_id: Optional[str] = None):
        if eval_id is None:
            eval_id = os.getenv("SW_EVAL_ID", None)
        if eval_id is None:
            raise RuntimeError("eval id should not be None")
        if re.match(r"^[A-Za-z0-9-_]+$", eval_id) is None:
            raise RuntimeError(
                f"invalid eval id {eval_id}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
            )
        self.eval_id = eval_id
        self.project = os.getenv("SW_PROJECT")
        if self.project is None:
            raise RuntimeError("SW_PROJECT is not set")
        self._results_table_name = f"project/{self.project}/eval/{self.eval_id}/results"
        self._summary_table_name = f"project/{self.project}/eval/summary"
        self._init_writers([self._results_table_name, self._summary_table_name])
        self._data_store = data_store.get_data_store()

    def log_result(self, data_id: str, result: Any, **kwargs: Any) -> None:
        record = {"id": data_id, "result": result}
        for k, v in kwargs.items():
            record[k.lower()] = v
        self._log(self._results_table_name, record)

    def log_metrics(
        self, metrics: Optional[Dict[str, Any]] = None, **kwargs: Any
    ) -> None:
        record = {"id": self.eval_id}
        if metrics is not None:
            for k, v in metrics.items():
                k = k.lower()
                if k != "id":
                    record[k] = v
        else:
            for k, v in kwargs.items():
                record[k.lower()] = v
        self._log(self._summary_table_name, record)

    def get_results(self) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            [(self._results_table_name, "result", False)]
        )

    def get_metrics(self):
        _m = [
            metrics
            for metrics in self._data_store.scan_tables(
                [(self._summary_table_name, "", False)]
            )
            if metrics["id"] == self.eval_id
        ]
        return _m[0]


class EvaluationResult:
    def __init__(self, data_id: str, result: Any, **kwargs: Any):
        self.data_id = data_id
        self.result = result
        self.kwargs = kwargs


class EvaluationMetric:
    def __init__(self, metrics: Optional[Dict[str, Any]] = None, **kwargs: Any):
        self.metrics = metrics
        self.kwargs = kwargs


# TODO: rich query params
class EvaluationQuery:
    def __init__(self, kind: EvaluationResultKind):
        self.kind = kind


class EvaluationForSubProcess:
    def __init__(self, sub_conn: Connection):
        self.sub_conn = sub_conn

    def log_result(self, data_id: str, result: Any, **kwargs: Any) -> None:
        self.sub_conn.send(EvaluationResult(data_id=data_id, result=result, **kwargs))

    def log_metrics(
        self, metrics: Optional[Dict[str, Any]] = None, **kwargs: Any
    ) -> None:
        self.sub_conn.send(EvaluationMetric(metrics=metrics, **kwargs))

    def get_results(self) -> Iterator[Dict[str, Any]]:
        self.sub_conn.send(EvaluationQuery(EvaluationResultKind.RESULT))
        return self.sub_conn.recv()

    def get_metrics(self):
        self.sub_conn.send(EvaluationQuery(EvaluationResultKind.METRIC))
        return self.sub_conn.recv()


class Dataset(Logger):
    def __init__(self, dataset_id: str) -> None:
        if dataset_id is None:
            raise RuntimeError("id should not be None")
        if re.match(r"^[A-Za-z0-9-_]+$", dataset_id) is None:
            raise RuntimeError(
                f"invalid id {id}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
            )
        self.dataset_id = dataset_id
        self.project = os.getenv("SW_PROJECT")
        if self.project is None:
            raise RuntimeError("SW_PROJECT is not set")
        self._meta_table_name = f"project/{self.project}/dataset/{self.dataset_id}/meta"
        self._data_store = data_store.get_data_store()
        self._init_writers([self._meta_table_name])

    def put(self, data_id: str, **kwargs: Any) -> None:
        record = {"id": data_id}
        for k, v in kwargs.items():
            record[k.lower()] = v
        self._log(self._meta_table_name, record)

    def scan(self, start: str, end: str) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            [(self._meta_table_name, "meta", False)], start=start, end=end
        )
