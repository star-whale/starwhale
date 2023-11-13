import os
import re
import threading
from enum import Enum, unique
from typing import Any, Set, Dict, List, Union, Callable, Iterator, Optional

from starwhale.utils import console
from starwhale.consts import VERSION_PREFIX_CNT

from . import data_store
from ...base.uri.project import Project


class Logger:
    _ID_KEY = "id"

    def _init_writers(self, tables: List[str]) -> None:
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
                    exceptions.append(e)
                    console.print_exception()

            if exceptions:
                raise Exception(*exceptions)

    def _fetch_writer(self, table_name: str) -> data_store.TableWriter:
        with self._lock:
            if table_name not in self._writers:
                self._writers.setdefault(table_name, None)
            writer = self._writers[table_name]
            if writer is None:
                _store = getattr(self, "_data_store", None)
                writer = data_store.TableWriter(
                    table_name,
                    data_store=_store,
                    key_column=self._ID_KEY,
                    run_exceptions_limits=0,
                )
                self._writers[table_name] = writer
        return writer

    def _log(self, table_name: str, record: Dict[str, Any]) -> None:
        _id = record.get(self._ID_KEY)
        if _id is None:
            raise RuntimeError(f"id is not set for table {table_name}")

        if not isinstance(_id, (str, int)):
            raise RuntimeError(
                f"id should be str or int, got {type(_id)} for table {table_name}"
            )

        writer = self._fetch_writer(table_name)
        writer.insert(record)

    def _flush(self, table_name: str) -> str:
        with self._lock:
            writer = self._writers.get(table_name)
            if writer is None:
                return ""
        return writer.flush()

    def _delete(self, table_name: str, key: Any) -> None:
        writer = self._fetch_writer(table_name)
        writer.delete(key)


table_name_formatter: Callable[
    [Union[str, int], str], str
] = lambda project, table: f"project/{project}/{table}"


class Evaluation(Logger):
    _RESULTS_TABLE = "results"
    _stashing_tables: Set[str] = set()
    _stashing_tables_lock = threading.Lock()

    def __init__(self, eval_id: str, project: Project):
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
        self._tables: Dict[str, str] = {}
        self._eval_table_name: Callable[
            [str], str
        ] = (
            lambda name: f"eval/{self.eval_id[:VERSION_PREFIX_CNT]}/{self.eval_id}/{name}"
        )
        self._eval_summary_table_name = os.getenv(
            "SW_EVALUATION_SUMMARY_TABLE", "eval/summary"
        )
        self._data_store = data_store.get_data_store(
            project.instance.url, project.instance.token
        )
        self._init_writers([])

    def _get_storage_table_name(self, table: str) -> str:
        with self._lock:
            _table_name = self._tables.get(table)
            if _table_name is None:
                _table_name = table_name_formatter(self.project.id, table)
                self._tables[table] = _table_name
        return _table_name

    def _log(self, table_name: str, record: Dict[str, Any]) -> None:
        _storage_table_name = self._get_storage_table_name(table_name)
        super()._log(_storage_table_name, record)

    def _get(
        self,
        table_name: str,
        start: Any = None,
        end: Any = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            tables=[
                data_store.TableDesc(self._get_storage_table_name(table=table_name))
            ],
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )

    def _flush(self, table_name: str) -> str:
        _storage_table_name = self._get_storage_table_name(table_name)
        return super()._flush(_storage_table_name)

    def log_summary_metrics(
        self, metrics: Optional[Dict[str, Any]] = None, **kwargs: Any
    ) -> None:
        record = {self._ID_KEY: self.eval_id}
        # TODO: without if else?
        if metrics is not None:
            for k, v in metrics.items():
                k = k.lower()
                if k != self._ID_KEY:
                    record[k] = v
        else:
            for k, v in kwargs.items():
                record[k.lower()] = v

        self._log(self._eval_summary_table_name, record)

    def log(self, table_name: str, **kwargs: Any) -> None:
        record = {}
        for k, v in kwargs.items():
            record[k.lower()] = v
        with self._stashing_tables_lock:
            self._stashing_tables.add(table_name)
        self._log(self._eval_table_name(table_name), record)

    def get_results(self) -> Iterator[Dict[str, Any]]:
        return self._get(self._eval_table_name(self._RESULTS_TABLE))

    def get_summary_metrics(self) -> Dict[str, Any]:
        # TODO: tune performance by queryTable api
        for metrics in self._get(self._eval_summary_table_name, start=self.eval_id):
            if metrics[self._ID_KEY] == self.eval_id:
                return metrics
        return {}

    def get(
        self,
        table_name: str,
        start: Any = None,
        end: Any = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        return self._get(
            table_name=self._eval_table_name(table_name),
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )

    get_table_rows = get

    def flush_results(self) -> None:
        self._flush(self._eval_table_name(self._RESULTS_TABLE))

    def flush_summary_metrics(self) -> None:
        self._flush(self._eval_summary_table_name)

    def flush(self, table_name: str) -> None:
        self._flush(self._eval_table_name(table_name))

    def flush_all(self) -> None:
        self.flush_summary_metrics()
        with self._stashing_tables_lock:
            for table_name in self._stashing_tables:
                self.flush(table_name)

    def get_tables(self) -> List[str]:
        prefix = table_name_formatter(self.project.id, self._eval_table_name(""))
        prefix = prefix.strip("/")

        tables = []
        for t in self._data_store.list_tables(prefixes=[prefix]):
            if t.startswith(prefix):
                t = t[len(prefix) + 1 :]

            t = t.strip("/")
            tables.append(t)

        return tables


@unique
class DatasetTableKind(Enum):
    META = "meta"
    INFO = "info"


class Dataset(Logger):
    def __init__(
        self,
        dataset_name: str,
        project: Project,
        dataset_scan_revision: str = "",
        kind: DatasetTableKind = DatasetTableKind.META,
    ) -> None:
        if not dataset_name:
            raise RuntimeError("id should not be None")

        if not project:
            raise RuntimeError("project is not set")

        self.dataset_scan_revision = dataset_scan_revision
        self.project = project
        self.kind = kind

        # _current is only holder part of the dataset table name
        self._table_name = table_name_formatter(
            project.id,
            f"dataset/{dataset_name}/_current/{kind.value}",
        )
        self._data_store = data_store.get_data_store(
            project.instance.url, project.instance.token
        )
        self._init_writers([self._table_name])

    def put(self, data_id: Union[str, int], **kwargs: Any) -> None:
        record = {self._ID_KEY: data_id}
        for k, v in kwargs.items():
            record[k.lower()] = v
        self._log(self._table_name, record)

    def delete(self, data_id: Union[str, int]) -> None:
        self._delete(self._table_name, data_id)

    def scan(
        self,
        start: Any,
        end: Any,
        end_inclusive: bool = False,
        revision: str = "",
        keep_none: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            tables=[
                data_store.TableDesc(
                    table_name=self._table_name,
                    revision=revision or self.dataset_scan_revision,
                )
            ],
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )

    def scan_id(
        self,
        start: Any,
        end: Any,
        end_inclusive: bool = False,
        revision: str = "",
        keep_none: bool = False,
    ) -> Iterator[Any]:
        return self._data_store.scan_tables(
            tables=[
                data_store.TableDesc(
                    table_name=self._table_name,
                    columns=[self._ID_KEY],
                    revision=revision or self.dataset_scan_revision,
                )
            ],
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )

    def flush(self) -> str:
        return self._flush(self._table_name)

    def __str__(self) -> str:
        return f"Dataset Wrapper, table:{self._table_name}, store:{self._data_store}, kind: {self.kind}, revision: {self.dataset_scan_revision}"

    __repr__ = __str__
