import re
import threading
from typing import Any, Dict, List, Union, Iterator, Optional

import dill
from loguru import logger

from starwhale.consts import VERSION_PREFIX_CNT

from . import data_store


class Logger:
    def _init_writers(
        self, tables: List[str], store: Optional[data_store.DataStore] = None
    ) -> None:
        self._writers: Dict[str, Optional[data_store.TableWriter]] = {
            table: None for table in tables
        }
        self._lock = threading.Lock()

    def close(self) -> None:
        with self._lock:
            exceptions: List[Exception] = []
            for writer in self._writers.values():
                if writer is None:
                    continue

                try:
                    writer.close()
                except Exception as e:
                    logger.exception(f"{writer} exception: {e}")
                    exceptions.append(e)

            if exceptions:
                raise Exception(*exceptions)

    def _log(self, table_name: str, record: Dict[str, Any]) -> None:
        with self._lock:
            if table_name not in self._writers:
                self._writers.setdefault(table_name, None)
            writer = self._writers[table_name]
            if writer is None:
                _store = getattr(self, "_data_store", None)
                writer = data_store.TableWriter(table_name, data_store=_store)
                self._writers[table_name] = writer

        writer.insert(record)

    def _flush(self, table_name: str) -> None:
        with self._lock:
            writer = self._writers.get(table_name)
            if writer is None:
                return
        writer.flush()


def _serialize(data: Any) -> Any:
    return dill.dumps(data)


def _deserialize(data: bytes) -> Any:
    return dill.loads(data)


class Evaluation(Logger):
    def __init__(self, eval_id: str, project: str, instance: str = ""):
        if not eval_id:
            raise RuntimeError("eval id should not be None")
        if re.match(r"^[A-Za-z0-9-_]+$", eval_id) is None:
            raise RuntimeError(
                f"invalid eval id {eval_id}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
            )
        if not project:
            raise RuntimeError("project is not set")

        self.eval_id = eval_id
        self.project = project
        self._results_table_name = self._get_datastore_table_name("results")
        self._summary_table_name = f"project/{self.project}/eval/summary"
        self._data_store = data_store.get_data_store(instance_uri=instance)
        self._init_writers([self._results_table_name, self._summary_table_name])

    def _get_datastore_table_name(self, table_name: str) -> str:
        return f"project/{self.project}/eval/{self.eval_id[:VERSION_PREFIX_CNT]}/{self.eval_id}/{table_name}"

    def log_result(
        self,
        data_id: Union[int, str],
        result: Any,
        serialize: bool = False,
        **kwargs: Any,
    ) -> None:
        record = {"id": data_id, "result": _serialize(result) if serialize else result}
        for k, v in kwargs.items():
            record[k.lower()] = _serialize(v) if serialize else v
        self._log(self._results_table_name, record)

    def log_metrics(
        self, metrics: Optional[Dict[str, Any]] = None, **kwargs: Any
    ) -> None:
        record = {"id": self.eval_id}
        # TODO: without if else?
        if metrics is not None:
            for k, v in metrics.items():
                k = k.lower()
                if k != "id":
                    record[k] = v
        else:
            for k, v in kwargs.items():
                record[k.lower()] = v
        self._log(self._summary_table_name, record)

    def log(self, table_name: str, **kwargs: Any) -> None:
        record = {}
        for k, v in kwargs.items():
            record[k.lower()] = v
        self._log(self._get_datastore_table_name(table_name), record)

    def get_results(self, deserialize: bool = False) -> Iterator[Dict[str, Any]]:
        for data in self._data_store.scan_tables(
            [data_store.TableDesc(self._results_table_name)]
        ):
            if deserialize:
                for _k, _v in data.items():
                    if _k == "id":
                        continue
                    data[_k] = _deserialize(_v)
            yield data

    def get_metrics(self) -> Dict[str, Any]:
        for metrics in self._data_store.scan_tables(
            [data_store.TableDesc(self._summary_table_name)]
        ):
            if metrics["id"] == self.eval_id:
                return metrics

        return {}

    def get(self, table_name: str) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            [data_store.TableDesc(self._get_datastore_table_name(table_name))]
        )

    def flush_result(self) -> None:
        self._flush(self._results_table_name)

    def flush_metrics(self) -> None:
        self._flush(self._summary_table_name)

    def flush(self, table_name: str) -> None:
        self._flush(table_name)


class Dataset(Logger):
    def __init__(
        self, dataset_id: str, project: str, instance_uri: str = "", token: str = ""
    ) -> None:
        if not dataset_id:
            raise RuntimeError("id should not be None")

        if not project:
            raise RuntimeError("project is not set")

        self.dataset_id = dataset_id
        self.project = project
        self._meta_table_name = f"project/{self.project}/dataset/{self.dataset_id}/meta"
        self._data_store = data_store.get_data_store(instance_uri, token)
        self._init_writers([self._meta_table_name])

    def put(self, data_id: Union[str, int], **kwargs: Any) -> None:
        record = {"id": data_id}
        for k, v in kwargs.items():
            record[k.lower()] = v
        self._log(self._meta_table_name, record)

    def scan(self, start: Any, end: Any) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            [data_store.TableDesc(self._meta_table_name)], start=start, end=end
        )

    def scan_id(self, start: Any, end: Any) -> Iterator[Any]:
        return self._data_store.scan_tables(
            [data_store.TableDesc(self._meta_table_name, columns=["id"])],
            start=start,
            end=end,
        )

    def flush(self) -> None:
        self._flush(self._meta_table_name)

    def __str__(self) -> str:
        return (
            f"Dataset Wrapper, table:{self._meta_table_name}, store:{self._data_store}"
        )

    __repr__ = __str__
