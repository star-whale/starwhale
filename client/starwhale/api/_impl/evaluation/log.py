from __future__ import annotations

import typing as t
import threading

from starwhale.api._impl import wrapper
from starwhale.utils.error import ParameterError
from starwhale.base.context import Context
from starwhale.base.uri.project import Project


class EvaluationLogStore:
    def __init__(self, id: str, project: Project | str) -> None:
        self.id = id

        if isinstance(project, str):
            project = Project(uri=project)
        self.project: Project = project

        self._datastore = wrapper.Evaluation(eval_id=self.id, project=self.project)

    def __str__(self) -> str:
        return f"EvaluationLogStore: id({self.id}), project({self.project})"

    __repr__ = __str__

    def flush_all(self) -> None:
        """Flush all updated tables into the Starwhale Datastore."""
        self._datastore.flush_all()

    def flush(self, category: str) -> None:
        """Flush the specified table(category) into the Starwhale Datastore."""
        self._datastore.flush(category)

    def flush_results(self) -> None:
        """Flush the results table into the Starwhale Datastore."""
        self._datastore.flush_results()

    def flush_summary(self) -> None:
        """Flush the summary table into the Starwhale Datastore."""
        self._datastore.flush_summary_metrics()

    def get_tables(self) -> t.List[str]:
        """Get all table names.

        Returns:
            A list of table names.
        """
        return self._datastore.get_tables()

    def close(self) -> None:
        self._datastore.close()

    def log_result(self, id: t.Union[str, int], metrics: t.Dict[str, t.Any]) -> None:
        """Log metrics for results table into the Starwhale Datastore

        Arguments:
            id: [Union[str, int], required] The unique id of the record.
            metrics: [Dict, required] The metrics dict.

        Examples:
        ```python
        from starwhale import evaluation

        evaluation_store = evaluation.EvaluationLogStore(id="2ddab20df9e9430dbd73853d773a9ff6", project="self")
        evaluation_store.log_result(1, {"loss": 0.99, "accuracy": 0.98})
        evaluation_store.log_result(2, {"loss": 0.98, "accuracy": 0.99})
        ```

        Returns:
            None
        """
        return self._datastore.log_result(dict(id=id, **metrics))

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
        from starwhale import evaluation

        evaluation_store = evaluation.EvaluationLogStore(id="2ddab20df9e9430dbd73853d773a9ff6", project="https://cloud.starwhale.cn/projects/349")

        evaluation_store.log("label/1", 1, {"loss": 0.99, "accuracy": 0.98})
        evaluation_store.log("ppl", "1", {"a": "test", "b": 1})

        Returns:
            None
        ```
        """
        self._datastore.log(table_name=category, id=id, **metrics)

    def log_summary(self, *args: t.Any, **kw: t.Any) -> None:
        """Log info into the Starwhale Datastore summary table.

        Arguments:
            *args: [dict, optional] use dicts to store summary info.
            **kwargs: [optional] use kv to store summary info.

        Examples:
        ```python
        from starwhale import evaluation

        evaluation_store = evaluation.EvaluationLogStore(id="2ddab20df9e9430dbd73853d773a9ff6", project="https://cloud.starwhale.cn/projects/349")

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
        self._datastore.log_summary_metrics(metrics)

    def get_summary(self) -> t.Dict:
        """Get the evaluation(id, project) related row of the summary table.

        Returns:
            Dict of the related row.
        """
        return self._datastore.get_summary_metrics()

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
        from starwhale import evaluation

        evaluation_store = evaluation.EvaluationLogStore(id="2ddab20df9e9430dbd73853d773a9ff6", project="self")

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
        from starwhale import evaluation

        evaluation_store = evaluation.EvaluationLogStore(id="2ddab20df9e9430dbd73853d773a9ff6", project="https://cloud.starwhale.cn/projects/349")
        results = [data for data in evaluation_store.scan("label/0")]
        ```

        Returns:
            An iterator.
        """
        # TODO: support batch_size
        return self._datastore.get(
            table_name=category,
            start=start,
            end=end,
            keep_none=keep_none,
            end_inclusive=end_inclusive,
        )


_log_store_instance_holder = threading.local()
_log_store_lock = threading.Lock()


def get_log_store_from_context() -> EvaluationLogStore:
    with _log_store_lock:
        _inst: t.Optional[EvaluationLogStore]
        try:
            _inst = _log_store_instance_holder.value  # type: ignore
        except AttributeError:
            _inst = None

        if _inst is not None:
            return _inst

        context = Context.get_runtime_context()

        _inst = EvaluationLogStore(
            id=context.version,
            project=context.log_project,
        )

        _log_store_instance_holder.value = _inst
        return _inst


def log(category: str, id: t.Union[str, int], metrics: t.Dict[str, t.Any]) -> None:
    """Log metrics into the specified category(table) for the current Context EvaluationLogStore.

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
    el = get_log_store_from_context()
    el.log(category, id, metrics)


def log_summary(*args: t.Any, **kw: t.Any) -> None:
    """Log metrics into the summary table for the current Context EvaluationLogStore.

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
    el = get_log_store_from_context()
    el.log_summary(*args, **kw)


def log_result(id: t.Union[str, int], metrics: t.Dict[str, t.Any]) -> None:
    """Log metrics to the results table for the current Context EvaluationLogStore.

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
    el = get_log_store_from_context()
    el.log_result(id, metrics)


def get_summary() -> t.Dict:
    """Get the evaluation(id, project) related row of the summary table for the current Context EvaluationLogStore."""
    el = get_log_store_from_context()
    return el.get_summary()


def scan_results(
    start: t.Any = None,
    end: t.Any = None,
    keep_none: bool = False,
    end_inclusive: bool = False,
) -> t.Iterator:
    """Get an iterator from the results table for the current Context EvaluationLogStore.

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
    el = get_log_store_from_context()
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
    """Get an iterator from the specified category(table) for the current Context EvaluationLogStore.

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
    el = get_log_store_from_context()
    return el.scan(
        category=category,
        start=start,
        end=end,
        keep_none=keep_none,
        end_inclusive=end_inclusive,
    )
