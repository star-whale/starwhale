import os
import re
import urllib
import threading
from enum import Enum, unique
from typing import Any, Dict, List, Union, Callable, Iterator, Optional
from functools import lru_cache

import requests

from starwhale.utils import console
from starwhale.consts import VERSION_PREFIX_CNT, STANDALONE_INSTANCE
from starwhale.consts.env import SWEnv
from starwhale.utils.retry import http_retry
from starwhale.utils.config import SWCliConfigMixed

from . import data_store


class Logger:
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
                writer = data_store.TableWriter(table_name, data_store=_store)
                self._writers[table_name] = writer
        return writer

    def _log(self, table_name: str, record: Dict[str, Any]) -> None:
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


def _gen_storage_table_name(
    project: Union[str, int], table: str, instance_uri: str = ""
) -> str:
    _instance_uri = instance_uri or os.getenv(SWEnv.instance_uri)
    if (
        _instance_uri is None
        or _instance_uri == STANDALONE_INSTANCE
        or isinstance(project, int)
        or (isinstance(project, str) and project.isnumeric())
    ):
        return table_name_formatter(project, table)
    else:
        return table_name_formatter(
            _get_remote_project_id(_instance_uri, project), table
        )


@lru_cache(maxsize=None)
@http_retry
def _get_remote_project_id(instance_uri: str, project: str) -> Any:
    resp = requests.get(
        urllib.parse.urljoin(instance_uri, f"/api/v1/project/{project}"),
        headers={
            "Content-Type": "application/json; charset=utf-8",
            "Authorization": (
                SWCliConfigMixed().get_sw_token(instance=instance_uri)
                or os.getenv(SWEnv.instance_token, "")
            ),
        },
        timeout=60,
    )
    resp.raise_for_status()
    return resp.json().get("data", {})["id"]


class Evaluation(Logger):
    _ID_KEY = "id"

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
        self.instance = instance
        self._tables: Dict[str, str] = {}
        self._eval_table_name: Callable[
            [str], str
        ] = (
            lambda name: f"eval/{self.eval_id[:VERSION_PREFIX_CNT]}/{self.eval_id}/{name}"
        )
        self._eval_summary_table_name = "eval/summary"
        self._data_store = data_store.get_data_store(instance_uri=instance)
        self._init_writers([])

    def _get_storage_table_name(self, table: str) -> str:
        with self._lock:
            _table_name = self._tables.get(table)
            if _table_name is None:
                _table_name = _gen_storage_table_name(
                    project=self.project,
                    table=table,
                    instance_uri=self.instance,
                )
                self._tables[table] = _table_name
        return _table_name

    def _log(self, table_name: str, record: Dict[str, Any]) -> None:
        _storage_table_name = self._get_storage_table_name(table_name)
        super()._log(_storage_table_name, record)

    def _get(self, table_name: str) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            [data_store.TableDesc(self._get_storage_table_name(table=table_name))]
        )

    def _flush(self, table_name: str) -> str:
        _storage_table_name = self._get_storage_table_name(table_name)
        return super()._flush(_storage_table_name)

    def log_result(self, record: Dict) -> None:
        self._log(self._eval_table_name("results"), record)

    def log_metrics(
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
        self._log(self._eval_table_name(table_name), record)

    def get_results(self) -> Iterator[Dict[str, Any]]:
        return self._get(self._eval_table_name("results"))

    def get_metrics(self) -> Dict[str, Any]:
        for metrics in self._get(self._eval_summary_table_name):
            if metrics[self._ID_KEY] == self.eval_id:
                return metrics

        return {}

    def get(self, table_name: str) -> Iterator[Dict[str, Any]]:
        return self._get(self._eval_table_name(table_name))

    def flush_result(self) -> None:
        self._flush(self._eval_table_name("results"))

    def flush_metrics(self) -> None:
        self._flush(self._eval_summary_table_name)

    def flush(self, table_name: str) -> None:
        self._flush(self._eval_table_name(table_name))


@unique
class DatasetTableKind(Enum):
    META = "meta"
    INFO = "info"


class Dataset(Logger):
    def __init__(
        self,
        dataset_name: str,
        project: str,
        dataset_scan_revision: str = "",
        instance_name: str = "",
        token: str = "",
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
        self._table_name = _gen_storage_table_name(
            project=project,
            table=f"dataset/{dataset_name}/_current/{kind.value}",
            instance_uri=instance_name,
        )
        self._data_store = data_store.get_data_store(instance_name, token)
        self._init_writers([self._table_name])

    def put(self, data_id: Union[str, int], **kwargs: Any) -> None:
        record = {"id": data_id}
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
    ) -> Iterator[Dict[str, Any]]:
        return self._data_store.scan_tables(
            [
                data_store.TableDesc(
                    table_name=self._table_name,
                    revision=revision or self.dataset_scan_revision,
                )
            ],
            start=start,
            end=end,
            end_inclusive=end_inclusive,
        )

    def scan_id(
        self,
        start: Any,
        end: Any,
        end_inclusive: bool = False,
        revision: str = "",
    ) -> Iterator[Any]:
        return self._data_store.scan_tables(
            [
                data_store.TableDesc(
                    table_name=self._table_name,
                    columns=["id"],
                    revision=revision or self.dataset_scan_revision,
                )
            ],
            start=start,
            end=end,
            end_inclusive=end_inclusive,
        )

    def flush(self) -> str:
        return self._flush(self._table_name)

    def __str__(self) -> str:
        return f"Dataset Wrapper, table:{self._table_name}, store:{self._data_store}, kind: {self.kind}, revision: {self.dataset_scan_revision}"

    __repr__ = __str__
