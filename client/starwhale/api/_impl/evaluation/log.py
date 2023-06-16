from __future__ import annotations

import typing as t
import threading

from starwhale.api._impl import wrapper
from starwhale.utils.error import ParameterError
from starwhale.base.context import Context


class EvaluationLogStore:
    _instance_holder = threading.local()
    _lock = threading.Lock()

    def __init__(self, id: str, project: str) -> None:
        self.id = id
        self.project = project

        self._datastore = wrapper.Evaluation(eval_id=self.id, project=self.project)

    def __str__(self) -> str:
        return f"Evaluation: id({self.id}), project({self.project})"

    __repr__ = __str__

    @classmethod
    def _get_instance(cls) -> EvaluationLogStore:
        with cls._lock:
            _inst: t.Optional[EvaluationLogStore]
            try:
                _inst = cls._instance_holder.value  # type: ignore
            except AttributeError:
                _inst = None

            if _inst is not None:
                return _inst

            context = Context.get_runtime_context()

            _inst = cls(
                id=context.version,
                project=context.project,
            )

            cls._instance_holder.value = _inst
            return _inst

    @classmethod
    def log(
        cls, category: str, id: t.Union[str, int], metrics: t.Dict[str, t.Any]
    ) -> None:
        """Log metrics into the Starwhale Datastore.

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
        # TODO: support pickle serialize
        el = cls._get_instance()
        el._log(category, id, metrics)

    def _log(
        self, category: str, id: t.Union[str, int], metrics: t.Dict[str, t.Any]
    ) -> None:
        self._datastore.log(table_name=category, id=id, **metrics)

    @classmethod
    def log_summary(cls, *args: t.Any, **kw: t.Any) -> None:
        """Log info into the Starwhale Datastore summary table.

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
        el = cls._get_instance()
        el._log_summary(metrics)

    def _log_summary(self, metrics: t.Dict) -> None:
        self._datastore.log_summary_metrics(metrics)

    def _iter_results(self) -> t.Iterator:
        return self._datastore.get_results()

    def _flush_result(self) -> None:
        return self._datastore.flush_results()

    def log_result(self, record: t.Dict) -> None:
        self._datastore.log_result(record)

    @classmethod
    def iter(
        cls,
        category: str,
    ) -> t.Iterator:
        """Get an iterator for the log data of the specified category.

        Arguments:
            category: [str, required] The category of the log records.

        Examples:
        ```python
        from starwhale import evaluation

        results = [data for data in evaluation.iter("label/0")]
        ```

        Returns:
            An iterator.
        """
        # TODO: support batch_size
        el = cls._get_instance()
        return el._iter(category)

    def _iter(self, category: str) -> t.Iterator:
        return self._datastore.get(category)
