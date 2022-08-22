import os
import re
import threading
from typing import Any, Dict, List, Union, Iterator, Optional

from . import data_store
from starwhale.consts.env import SWEnv


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
            eval_id = os.getenv(SWEnv.eval_version, None)
        if eval_id is None:
            raise RuntimeError("eval id should not be None")
        if re.match(r"^[A-Za-z0-9-_]+$", eval_id) is None:
            raise RuntimeError(
                f"invalid eval id {eval_id}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
            )
        self.eval_id = eval_id
        self.project = os.getenv(SWEnv.project)
        if self.project is None:
            raise RuntimeError(f"{SWEnv.project} is not set")
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
            [data_store.TableDesc(self._results_table_name)]
        )

    def get_metrics(self) -> Dict[str, Any]:
        for metrics in self._data_store.scan_tables(
            [data_store.TableDesc(self._summary_table_name)]
        ):
            if metrics["id"] == self.eval_id:
                return metrics

        return {}


class Dataset(Logger):
    def __init__(self, dataset_id: str, project: str = "") -> None:
        if not dataset_id:
            raise RuntimeError("id should not be None")

        self.dataset_id = dataset_id
        self.project = project or os.getenv(SWEnv.project)
        if not self.project:
            raise RuntimeError("project is not set")

        self._meta_table_name = f"project/{self.project}/dataset/{self.dataset_id}/meta"
        self._data_store = data_store.get_data_store()
        self._init_writers([self._meta_table_name])

    def put(self, data_id: Union[int, str], **kwargs: Any) -> None:
        record = {"id": data_id}
        for k, v in kwargs.items():
            record[k.lower()] = v
        self._log(self._meta_table_name, record)

    def scan(self, start: Any, end: Any) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            [data_store.TableDesc(self._meta_table_name)], start=start, end=end
        )

    def __str__(self) -> str:
        return f"Dataset Wrapper, table:{self._meta_table_name}"

    __repr__ = __str__
