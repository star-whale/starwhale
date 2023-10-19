from __future__ import annotations

import io
import sys
import typing as t
import tempfile
import threading
from pathlib import Path
from collections import defaultdict

from starwhale.consts import SW_TMP_DIR_NAME
from starwhale.utils.fs import ensure_dir, blake2b_file
from starwhale.api._impl import wrapper
from starwhale.base.cloud import CloudBundleModelMixin
from starwhale.utils.error import ParameterError
from starwhale.utils.retry import http_retry
from starwhale.base.context import Context
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.data_type import Link, Text, Binary, BaseArtifact
from starwhale.base.blob.store import LocalFileStore
from starwhale.utils.dict_util import flatten as flatten_dict
from starwhale.base.models.base import SwBaseModel
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.artifact.base import AsyncArtifactWriterBase


class _SummaryRecord(SwBaseModel):
    metrics: t.Dict[str, t.Any]


class _TableRecord(SwBaseModel):
    table: str
    id: t.Union[str, int]
    metrics: t.Dict[str, t.Any]


_TRecord = t.Union[_SummaryRecord, _TableRecord]


class Evaluation(AsyncArtifactWriterBase):
    def __init__(self, id: str, project: Project | str) -> None:
        # TODO: support cloud/server evaluation id(not hash value)
        self.id = id

        if isinstance(project, str):
            project = Project(project)

        self.project: Project = project
        self._resource = Resource(
            uri=self.id, typ=ResourceType.evaluation, project=self.project
        )
        self._in_standalone = project.instance.is_local

        self._local_file_store: LocalFileStore | None = None
        if self._in_standalone:
            self._local_file_store = LocalFileStore()

        self._datastore = wrapper.Evaluation(eval_id=self.id, project=self.project)
        self._stash_uri_records_map: t.Dict[Path, t.List[_TRecord]] = defaultdict(list)

        _root = SWCliConfigMixed().rootdir / SW_TMP_DIR_NAME
        ensure_dir(_root)
        _tmpdir = tempfile.mkdtemp(prefix=f"evaluation-{self.id}-", dir=_root)
        super().__init__(workdir=Path(_tmpdir))

    def __str__(self) -> str:
        return f"Evaluation: id({self.id}), project({self.project})"

    __repr__ = __str__

    def flush_all(self, artifacts_flush: bool = True) -> None:
        """Flush all updated tables into the Starwhale Datastore."""
        super().flush(artifacts_flush)
        self._datastore.flush_all()

    def flush(self, category: str, artifacts_flush: bool = True) -> None:  # type: ignore
        """Flush the specified table(category) into the Starwhale Datastore."""
        super().flush(artifacts_flush)
        self._datastore.flush(category)

    def flush_results(self, artifacts_flush: bool = True) -> None:
        """Flush the results table into the Starwhale Datastore."""
        super().flush(artifacts_flush)
        self._datastore.flush_results()

    def flush_summary(self, artifacts_flush: bool = True) -> None:
        """Flush the summary table into the Starwhale Datastore."""
        super().flush(artifacts_flush)
        self._datastore.flush_summary_metrics()

    def _handle_bin_sync(self, bin_path: Path) -> None:
        if self._in_standalone:
            if self._local_file_store is None:
                raise RuntimeError("local_file_store is not initialized")

            file = self._local_file_store.put(bin_path)
            uri = file.hash
        else:
            sign_name = blake2b_file(bin_path)
            crm = CloudBundleModelMixin()

            @http_retry
            def _upload() -> str:
                r = crm.do_multipart_upload_file(
                    url_path=f"/project/{self.project.id}/evaluation/{self.id}/hashedBlob/{sign_name}",
                    file_path=bin_path,
                    instance=self.project.instance,
                )
                return r.json()["data"]  # type: ignore

            uri = _upload()

        for record in self._stash_uri_records_map.get(bin_path, []):
            for k in record.metrics:
                record.metrics[k].link.uri = uri  # type: ignore

            if isinstance(record, _SummaryRecord):
                self._datastore.log_summary_metrics(record.metrics)
            elif isinstance(record, _TableRecord):
                self._datastore.log(
                    table_name=record.table, id=record.id, **record.metrics
                )

        if bin_path in self._stash_uri_records_map:
            del self._stash_uri_records_map[bin_path]

    def _flatten_dict(self, data: t.Dict) -> t.Dict:
        # [Current]: flatten dict, the artifacts list/tuple flatten, the non-artifacts list/tuple keep the original structure
        #    --> Example:
        #           origin: {"test": {"loss": 0.99, "prob": [0.98,0.99]}, "image": [Image, Image]}
        #           flatten: {"test/loss": 0.99, "test/prob": [0.98, 0.99], "image/0": Image, "image/1": Image}
        # [TODO]: flatten dict, the artifacts/non-artifacts list/tuple keep the original structure
        data = flatten_dict(data)
        ret = {}
        for k, v in data.items():
            if isinstance(v, (list, tuple)):
                _flatten_item = flatten_dict({k: v}, extract_sequence=True)
                if any([isinstance(_v, BaseArtifact) for _v in _flatten_item.values()]):
                    ret.update(_flatten_item)
                else:
                    ret[k] = v
            else:
                ret[k] = v

        return ret

    def _handle_row_put(self, record: t.Any) -> None:
        if not isinstance(record, (_SummaryRecord, _TableRecord)):
            raise ParameterError(f"record({record}) is not supported")

        metrics = self._auto_encode_types(record.metrics)
        metrics = self._flatten_dict(metrics)

        for k in list(metrics.keys()):
            v = metrics[k]
            if not isinstance(v, BaseArtifact):
                continue

            if v.link is not None:
                continue

            metrics.pop(k)
            if isinstance(v.fp, (str, Path)):
                content = Path(v.fp).read_bytes()
            elif isinstance(v.fp, (bytes, io.IOBase)):
                content = v.to_bytes()
            else:
                raise TypeError(
                    f"no support fp type for bin writer:{type(v.fp)}, {v.fp}"
                )

            _path, _meta = self._write_bin(content)
            v.link = Link(
                offset=_meta.raw_data_offset,
                size=_meta.raw_data_size,
                bin_offset=_meta.offset,
                bin_size=_meta.size,
            )
            _metrics = {k: v}

            _record: _TRecord
            if isinstance(record, _TableRecord):
                _record = _TableRecord(
                    table=record.table, id=record.id, metrics=_metrics
                )
            elif isinstance(record, _SummaryRecord):
                _record = _SummaryRecord(metrics=_metrics)

            self._stash_uri_records_map[_path].append(_record)

        if isinstance(record, _SummaryRecord):
            self._datastore.log_summary_metrics(metrics)
        elif isinstance(record, _TableRecord):
            self._datastore.log(table_name=record.table, id=record.id, **metrics)

    # TODO: refactor with TabularDataset
    def _auto_encode_types(self, data: t.Any) -> t.Any:
        """Auto Encode types for the high efficiency of the Starwhale Datastore.

        Supported types:
            string(>_ENCODE_MIN_SIZE) --> Text
            bytes(>_ENCODE_MIN_SIZE) --> Binary
        """

        if isinstance(data, str) and sys.getsizeof(data) > Text.AUTO_ENCODE_MIN_SIZE:
            return Text(content=data, auto_convert_to_str=True)
        elif (
            isinstance(data, bytes)
            and sys.getsizeof(data) > Binary.AUTO_ENCODE_MIN_SIZE
        ):
            return Binary(fp=data, auto_convert_to_bytes=True)
        elif isinstance(data, dict):
            return {k: self._auto_encode_types(v) for k, v in data.items()}
        elif isinstance(data, (list, tuple)):
            return type(data)([self._auto_encode_types(v) for v in data])
        else:
            return data

    def _auto_decode_types(self, data: t.Any) -> t.Any:
        """Auto Decode types for the high efficiency of the Starwhale Datastore.
        At the same time, it will fetch the artifacts data by the link.

        Supported types:
            Text(encoded) -> string
            Binary(encoded) -> bytes
        """
        if isinstance(data, BaseArtifact):
            data.prepare_link(self._resource.instance)

        if isinstance(data, Text) and data.auto_convert_to_str:
            return data.content
        elif isinstance(data, Binary) and data.auto_convert_to_bytes:
            return data.to_bytes()
        elif isinstance(data, dict):
            return {k: self._auto_decode_types(v) for k, v in data.items()}
        elif isinstance(data, (list, tuple)):
            return type(data)([self._auto_decode_types(v) for v in data])
        else:
            return data

    def get_tables(self) -> t.List[str]:
        """Get all table names.

        Returns:
            A list of table names.
        """
        return self._datastore.get_tables()

    def close(self) -> None:
        super().close()
        self._datastore.close()

    def log_result(self, id: t.Union[str, int], metrics: t.Dict[str, t.Any]) -> None:
        """Log metrics for results table into the Starwhale Datastore

        Arguments:
            id: [Union[str, int], required] The unique id of the record.
            metrics: [Dict, required] The metrics dict.

        Examples:
        ```python
        from starwhale import Evaluation

        evaluation_store = Evaluation(id="2ddab20df9e9430dbd73853d773a9ff6", project="self")
        evaluation_store.log_result(1, {"loss": 0.99, "accuracy": 0.98})
        evaluation_store.log_result(2, {"loss": 0.98, "accuracy": 0.99})
        ```

        Returns:
            None
        """
        self.put(
            _TableRecord(table=self._datastore._RESULTS_TABLE, id=id, metrics=metrics)
        )

    def log(
        self, category: str, id: t.Union[str, int], metrics: t.Dict[str, t.Any]
    ) -> None:
        """Log metrics for the specified table(category) into the Starwhale Datastore.

        Arguments:
            category: [str, required] The category of the log records.
            id: [Union[str, int], required] The unique id of the record.
            metrics: [Dict, required] The metrics dict.

        Examples:
        ```python
        from starwhale import Evaluation

        evaluation_store = Evaluation(id="2ddab20df9e9430dbd73853d773a9ff6", project="https://cloud.starwhale.cn/projects/349")

        evaluation_store.log("label/1", 1, {"loss": 0.99, "accuracy": 0.98})
        evaluation_store.log("ppl", "1", {"a": "test", "b": 1})

        Returns:
            None
        ```
        """
        self.put(_TableRecord(table=category, id=id, metrics=metrics))

    def log_summary(self, *args: t.Any, **kw: t.Any) -> None:
        """Log info into the Starwhale Datastore summary table.

        Arguments:
            *args: [dict, optional] use dicts to store summary info.
            **kwargs: [optional] use kv to store summary info.

        Examples:
        ```python
        from starwhale import Evaluation

        evaluation_store = Evaluation(id="2ddab20df9e9430dbd73853d773a9ff6", project="https://cloud.starwhale.cn/projects/349")

        evaluation_store.log_summary(loss=0.99)
        evaluation_store.log_summary(loss=0.99, accuracy=0.99)
        evaluation_store.log_summary({"loss": 0.99, "accuracy": 0.99})
        ```

        Returns:
            None
        """
        metrics = {}
        if len(args) == 0:
            metrics = kw
        elif len(args) == 1:
            if len(kw) > 0:
                raise ParameterError(
                    f"Args({args}) and kwargs({kw}) are specified at the same time"
                )
            if not isinstance(args[0], dict):
                raise ParameterError(f"Args({args[0]}) is not dict type")

            metrics = args[0]
        else:
            raise ParameterError(f"The number of args({args}) is greater than one")

        metrics.update(kw)
        self.put(_SummaryRecord(metrics=metrics))

    def get_summary(self) -> t.Dict:
        """Get the evaluation(id, project) related row of the summary table.

        Returns:
            Dict of the related row.
        """
        summary = self._datastore.get_summary_metrics()
        return self._auto_decode_types(summary)  # type: ignore

    def scan_results(
        self,
        start: t.Any = None,
        end: t.Any = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
    ) -> t.Iterator:
        """Get an iterator for the results table.

        Arguments:
            start: [optional] start key. Default is None.
            end: [optional] end key. Default is None.
            keep_none: [bool, optional] Whether to keep the records with None value.
            end_inclusive: [bool, optional] Whether to include the end id record.

        Examples:
        ```python
        from starwhale import Evaluation

        evaluation_store = Evaluation(id="2ddab20df9e9430dbd73853d773a9ff6", project="self")

        evaluation_store.log_result(1, {"loss": 0.99, "accuracy": 0.98})
        evaluation_store.log_result(2, {"loss": 0.98, "accuracy": 0.99})
        results = [data for data in evaluation_store.scan_results()]

        Return:
            An iterator.
        """
        return self.scan(
            self._datastore._RESULTS_TABLE, start, end, keep_none, end_inclusive
        )

    def scan(
        self,
        category: str,
        start: t.Any = None,
        end: t.Any = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
    ) -> t.Iterator:
        """Get an iterator for the log data of the specified category.

        Arguments:
            category: [str, required] The category of the log records.
            start: [optional] start key. Default is None.
            end: [optional] end key. Default is None.
            keep_none: [bool, optional] Whether to keep the records with None value.
            end_inclusive: [bool, optional] Whether to include the end id record.

        Examples:
        ```python
        from starwhale import Evaluation

        evaluation_store = Evaluation(id="2ddab20df9e9430dbd73853d773a9ff6", project="https://cloud.starwhale.cn/projects/349")
        results = [data for data in evaluation_store.scan("label/0")]
        ```

        Returns:
            An iterator.
        """
        # TODO: support batch_size
        for data in self._datastore.get(
            table_name=category,
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        ):
            yield self._auto_decode_types(data)

    @classmethod
    def from_context(cls) -> Evaluation:
        """Get the Evaluation instance from the current Context.

        Examples:
        ```python
        from starwhale import Evaluation

        with Evaluation.from_context() as e:
            e.log("label/1", 1, {"loss": 0.99, "accuracy": 0.98})
        ```
        Returns:
            The Evaluation object.
        """
        return _get_log_store_from_context()


_log_store_instance_holder = threading.local()
_log_store_lock = threading.Lock()


def _get_log_store_from_context() -> Evaluation:
    with _log_store_lock:
        _inst: t.Optional[Evaluation]
        try:
            _inst = _log_store_instance_holder.value  # type: ignore
        except AttributeError:
            _inst = None

        if _inst is not None:
            return _inst

        context = Context.get_runtime_context()

        _inst = Evaluation(
            id=context.version,
            project=context.log_project,
        )

        _log_store_instance_holder.value = _inst
        return _inst


# TODO: only expose Evaluation for the SDK users?
def log(category: str, id: t.Union[str, int], metrics: t.Dict[str, t.Any]) -> None:
    """Log metrics into the specified category(table) for the current Context Evaluation.

    Arguments:
        category: [str, required] The category of the log records.
        id: [Union[str, int], required] The unique id of the record.
        metrics: [Dict, required] The metrics dict.

    Examples:
    ```python
    from starwhale import evaluation

    evaluation.log("label/1", 1, {"loss": 0.99, "accuracy": 0.98})
    evaluation.log("ppl", "1", {"a": "test", "b": 1})

    Returns:
        None
    ```
    """
    el = _get_log_store_from_context()
    el.log(category, id, metrics)


def log_summary(*args: t.Any, **kw: t.Any) -> None:
    """Log metrics into the summary table for the current Context Evaluation.

    Arguments:
        *args: [dict, optional] use dicts to store summary info.
        **kwargs: [optional] use kv to store summary info.

    Examples:
    ```python
    from starwhale import evaluation

    evaluation.log_summary(loss=0.99)
    evaluation.log_summary(loss=0.99, accuracy=0.99)
    evaluation.log_summary({"loss": 0.99, "accuracy": 0.99})
    ```

    Returns:
        None
    """
    el = _get_log_store_from_context()
    el.log_summary(*args, **kw)


def log_result(id: t.Union[str, int], metrics: t.Dict[str, t.Any]) -> None:
    """Log metrics to the results table for the current Context Evaluation.

    Arguments:
        id: [Union[str, int], required] The unique id of the record.
        metrics: [Dict, required] The metrics dict.

    Examples:
    ```python
    from starwhale import evaluation

    evaluation.log_result(2, {"loss": 0.98, "accuracy": 0.99})
    ```

    Returns:
        None
    """
    el = _get_log_store_from_context()
    el.log_result(id, metrics)


def get_summary() -> t.Dict:
    """Get the evaluation(id, project) related row of the summary table for the current Context Evaluation."""
    el = _get_log_store_from_context()
    return el.get_summary()


def scan_results(
    start: t.Any = None,
    end: t.Any = None,
    keep_none: bool = False,
    end_inclusive: bool = False,
) -> t.Iterator:
    """Get an iterator from the results table for the current Context Evaluation.

    Arguments:
        start: [optional] start key. Default is None.
        end: [optional] end key. Default is None.
        keep_none: [bool, optional] Whether to keep the records with None value.
        end_inclusive: [bool, optional] Whether to include the end id record.

    Examples:
    ```python
    from starwhale import evaluation

    results = [data for data in evaluation.scan_results()]
    ```

    Returns:
        An iterator.
    """
    el = _get_log_store_from_context()
    return el.scan_results(
        start=start, end=end, keep_none=keep_none, end_inclusive=end_inclusive
    )


def scan(
    category: str,
    start: t.Any = None,
    end: t.Any = None,
    keep_none: bool = False,
    end_inclusive: bool = False,
) -> t.Iterator:
    """Get an iterator from the specified category(table) for the current Context Evaluation.

    Arguments:
        category: [str, required] The category of the log records.
        start: [optional] start key. Default is None.
        end: [optional] end key. Default is None.
        keep_none: [bool, optional] Whether to keep the records with None value.
        end_inclusive: [bool, optional] Whether to include the end id record.

    Examples:
    ```python
    from starwhale import evaluation

    results = [data for data in evaluation.scan("label/0")]
    ```

    Returns:
        An iterator.
    """
    el = _get_log_store_from_context()
    return el.scan(
        category=category,
        start=start,
        end=end,
        keep_none=keep_none,
        end_inclusive=end_inclusive,
    )
