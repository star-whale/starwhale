from __future__ import annotations

import time
import typing as t
import inspect
import threading
from abc import ABCMeta
from types import TracebackType
from pathlib import Path
from functools import wraps

import dill
import jsonlines

from starwhale.utils import console, now_str
from starwhale.consts import RunStatus, CURRENT_FNAME, DecoratorInjectAttr
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.api._impl import wrapper
from starwhale.base.type import RunSubDirType, PredictLogMode
from starwhale.api.service import Input, Output, Service
from starwhale.utils.error import ParameterError, FieldTypeOrValueError
from starwhale.base.context import Context
from starwhale.core.job.store import JobStorage
from starwhale.api._impl.dataset import Dataset
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.type import JsonDict
from starwhale.core.dataset.tabular import TabularDatasetRow, TabularDatasetInfo

_jl_writer: t.Callable[[Path], jsonlines.Writer] = lambda p: jsonlines.open(
    str((p).resolve()), mode="w"
)


class PipelineHandler(metaclass=ABCMeta):
    _INPUT_PREFIX = "input/"

    def __init__(
        self,
        predict_batch_size: int = 1,
        ignore_error: bool = False,
        flush_result: bool = False,
        predict_auto_log: bool = True,
        predict_log_mode: str = PredictLogMode.PICKLE.value,
        predict_log_dataset_features: t.Optional[t.List[str]] = None,
        dataset_uris: t.Optional[t.List[str]] = None,
        **kwargs: t.Any,
    ) -> None:
        self.predict_batch_size = predict_batch_size
        self.svc = Service()
        self.context = Context.get_runtime_context()

        self.dataset_uris = self.context.dataset_uris or dataset_uris or []

        self.predict_log_dataset_features = predict_log_dataset_features
        self.ignore_error = ignore_error
        self.flush_result = flush_result
        self.predict_auto_log = predict_auto_log
        self.predict_log_mode = PredictLogMode(predict_log_mode)
        self.kwargs = kwargs

        _logdir = JobStorage.local_run_dir(self.context.project, self.context.version)
        _run_dir = (
            _logdir / RunSubDirType.RUNLOG / self.context.step / str(self.context.index)
        )
        self.status_dir = _run_dir / RunSubDirType.STATUS
        ensure_dir(self.status_dir)

        # TODO: split status/result files
        self._timeline_writer = _jl_writer(self.status_dir / "timeline")

        # TODO: use EvaluationLogStore to refactor this?
        self.evaluation_store = wrapper.Evaluation(
            eval_id=self.context.version, project=self.context.project
        )
        self._update_status(RunStatus.START)

    def __str__(self) -> str:
        return f"PipelineHandler status@{self.status_dir}"

    def __enter__(self) -> PipelineHandler:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        console.debug(f"execute {self.context.step}-{self.context.index} exit func...")
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self._timeline_writer.close()

    def _record_status(func):  # type: ignore
        @wraps(func)  # type: ignore
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            self: PipelineHandler = args[0]
            console.info(
                f"start to run {func.__name__} function@{self.context.step}-{self.context.index} ..."  # type: ignore
            )
            self._update_status(RunStatus.RUNNING)
            try:
                func(*args, **kwargs)  # type: ignore
            except Exception:
                self._update_status(RunStatus.FAILED)
                console.print_exception()
                raise
            else:
                self._update_status(RunStatus.SUCCESS)

        return _wrapper

    @_record_status  # type: ignore
    def _starwhale_internal_run_evaluate(self) -> None:
        now = now_str()
        try:
            self._do_evaluate()
        except Exception as e:
            console.exception(f"evaluate exception: {e}")
            self._timeline_writer.write(
                {"time": now, "status": False, "exception": str(e)}
            )
            raise
        else:
            self._timeline_writer.write({"time": now, "status": True, "exception": ""})

    def _do_predict(
        self,
        data: t.List[t.Dict] | t.Dict,
        index: str | int | t.List[str | int],
        index_with_dataset: str | t.List[str],
        dataset_info: TabularDatasetInfo,
        dataset_uri: Resource,
    ) -> t.Any:
        predict_func = getattr(self, "predict", None)
        ppl_func = getattr(self, "ppl", None)

        if predict_func and ppl_func:
            raise ParameterError("predict and ppl cannot be defined at the same time")

        func = predict_func or ppl_func
        if func is None:
            raise ParameterError(
                "predict or ppl must be defined, predict function is recommended"
            )
        external = {
            "index": index,
            "index_with_dataset": index_with_dataset,
            "dataset_info": dataset_info,
            "context": self.context,
            "dataset_uri": dataset_uri,
        }

        # provide the more flexible way to inject arguments for predict or ppl function
        # case1: only accept data argument
        # 1. def predict(self, data): ...
        # 2. def predict(self, data, /): ...
        # case2: accept data and external arguments
        # 1. def predict(self, *args): ...
        # 2. def predict(self, **kwargs): ...
        # 3. def predict(self, *args, **kwargs): ...
        # 4. def predict(self, data, external: t.Dict): ...
        # 5. def predict(self, data, **kwargs): ...

        kind = inspect._ParameterKind

        parameters = inspect.signature(inspect.unwrap(func)).parameters.copy()
        # Limitation: When the users use custom decorator on the class method(predict/ppl) and only data argument is defined,
        # it is assumed that the first argument related to the class object name is self. Therefore, we remove it from the list of parameters when inspecting them.
        parameters.pop("self", None)

        if len(parameters) <= 0:
            raise RuntimeError("predict/ppl function must have at least one argument")
        elif len(parameters) == 1:
            parameter: inspect.Parameter = list(parameters.values())[0]
            if parameter.kind == kind.VAR_POSITIONAL:
                return func(data, external)
            elif parameter.kind == kind.VAR_KEYWORD:
                return func(data=data, external=external)
            elif parameter.kind in (kind.POSITIONAL_ONLY, kind.POSITIONAL_OR_KEYWORD):
                return func(data)
            else:
                raise RuntimeError(
                    f"unsupported parameter kind for predict/ppl function: {parameter.kind}"
                )
        else:
            return func(data, external=external)

    def _do_evaluate(self) -> t.Any:
        evaluate_func = getattr(self, "evaluate", None)
        cmp_func = getattr(self, "cmp", None)
        if evaluate_func and cmp_func:
            raise ParameterError("evaluate and cmp cannot be defined at the same time")

        func = evaluate_func or cmp_func
        if not func:
            raise ParameterError(
                "evaluate or cmp must be defined, evaluate function is recommended"
            )

        if self.predict_auto_log:
            func(self._iter_predict_result(self.evaluation_store.get_results()))
        else:
            func()

    @_record_status  # type: ignore
    def _starwhale_internal_run_predict(self) -> None:
        if not self.dataset_uris:
            raise FieldTypeOrValueError("context.dataset_uris is empty")
        join_str = "_#@#_"
        cnt = 0
        # TODO: user custom config batch size, max_retries
        for uri_str in self.dataset_uris:
            _uri = Resource(uri_str, typ=ResourceType.dataset)
            ds = Dataset.dataset(_uri, readonly=True)
            ds.make_distributed_consumption(session_id=self.context.version)
            dataset_info = ds.info
            cnt = 0
            if _uri.instance.is_local:
                # avoid confusion with underscores in project names
                idx_prefix = f"{_uri.project.name}/{_uri.name}"
            else:
                r_id = _uri.info().get("id")
                if not r_id:
                    raise KeyError("fetch dataset id error")
                idx_prefix = str(r_id)
            for rows in ds.batch_iter(self.predict_batch_size):
                _start = time.time()
                _exception = None
                _results: t.Any = b""
                try:
                    if self.predict_batch_size > 1:
                        _results = self._do_predict(
                            data=[row.features for row in rows],
                            index=[row.index for row in rows],
                            index_with_dataset=[
                                f"{idx_prefix}{join_str}{row.index}" for row in rows
                            ],
                            dataset_info=dataset_info,
                            dataset_uri=_uri,
                        )
                    else:
                        _results = [
                            self._do_predict(
                                data=rows[0].features,
                                index=rows[0].index,
                                index_with_dataset=f"{idx_prefix}{join_str}{rows[0].index}",
                                dataset_info=dataset_info,
                                dataset_uri=_uri,
                            )
                        ]
                except Exception as e:
                    _exception = e
                    console.exception(
                        f"[{[r.index for r in rows]}] data handle -> failed"
                    )
                    if not self.ignore_error:
                        self._update_status(RunStatus.FAILED)
                        raise
                else:
                    _exception = None

                for (_idx, _features), _result in zip(rows, _results):
                    cnt += 1
                    _idx_with_ds = f"{idx_prefix}{join_str}{_idx}"
                    _duration = time.time() - _start
                    console.debug(
                        f"[{_idx_with_ds}] use {_duration:.3f}s, session-id:{self.context.version} @{self.context.step}-{self.context.index}"
                    )

                    self._timeline_writer.write(
                        {
                            "time": now_str(),
                            "status": _exception is None,
                            "exception": str(_exception),
                            "index": _idx,
                            "index_with_dataset": _idx_with_ds,
                            "duration_seconds": _duration,
                        }
                    )

                    self._log_predict_result(
                        features=_features,
                        idx_with_ds=_idx_with_ds,
                        output=_result,
                        idx=_idx,
                        duration_seconds=_duration,
                    )

        if self.flush_result and self.predict_auto_log:
            self.evaluation_store.flush_result()

        console.info(
            f"{self.context.step}-{self.context.index} handled {cnt} data items for dataset {self.dataset_uris}"
        )

    def _update_status(self, status: str) -> None:
        fpath = self.status_dir / CURRENT_FNAME
        ensure_file(fpath, status)

    def add_api(
        self, input: Input, output: Output, func: t.Callable, name: str
    ) -> None:
        self.svc.add_api(input, output, func, name)

    def serve(self, addr: str, port: int) -> None:
        self.svc.serve(addr, port)

    def _iter_predict_result(
        self, raw_results_iter: t.Iterator[t.Dict]
    ) -> t.Iterator[t.Dict]:
        for data in raw_results_iter:
            mode = data.get("_mode", PredictLogMode.PICKLE.value)
            mode = PredictLogMode(mode)

            if "output" in data and mode == PredictLogMode.PICKLE:
                data["output"] = dill.loads(data["output"])

            input_features = {}
            for k in list(data.keys()):
                if k.startswith(self._INPUT_PREFIX):
                    _, name = k.split(self._INPUT_PREFIX, 1)
                    input_features[name] = data.pop(k)

            data["input"] = input_features
            yield data

    def _log_predict_result(
        self,
        features: t.Dict,
        idx_with_ds: str,
        output: t.Any,
        idx: str | int,
        duration_seconds: float,
    ) -> None:
        if not self.predict_auto_log:
            return

        if self.predict_log_dataset_features is None:
            _log_features = features
        else:
            _log_features = {
                k: v
                for k, v in features.items()
                if k in self.predict_log_dataset_features
            }

        for artifact in TabularDatasetRow.artifacts_of(_log_features):
            if artifact.link:
                artifact.clear_cache()

        input_features = {
            f"{self._INPUT_PREFIX}{k}": JsonDict.from_data(v)
            for k, v in _log_features.items()
        }
        if self.predict_log_mode == PredictLogMode.PICKLE:
            output = dill.dumps(output)

        # for plain mode: if the output is dict type, we(datastore) will log each key-value pair as a column
        record = {
            "id": idx_with_ds,
            "_mode": self.predict_log_mode.value,
            "_index": idx,
            "output": output,
            "duration_seconds": duration_seconds,
            **input_features,
        }
        self.evaluation_store.log_result(record)


# TODO: add flush, get_summary functions?
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
        self._datastore.log_metrics(metrics)

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


def predict(*args: t.Any, **kw: t.Any) -> t.Any:
    """Defines a predict function.

    This function can be used as a decorator to define an evaluation-predict function that maps the dataset data into
    the different predict workers.

    Arguments:
        datasets: [Union[List[str], None], optional] Use the datasets rows as the input data for the predict function.
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.
        concurrency: [int, optional] The concurrency of the predict tasks. Default is 1.
        replicas: [int, optional] The number of the predict tasks. Default is 1.
        batch_size: [int, optional] Number of samples per batch. Default is 1.
        fail_on_error: [bool, optional] Fast fail on the exceptions in the predict function. Default is True.
        auto_log: [bool, optional] Auto log the return values of the predict function and the according dataset rows. Default is True.
        log_mode: [str, optional] When auto_log=True, the log_mode can be specified to control the log behavior. Options are `pickle` and `plain`. Default is `pickle`.
        log_dataset_features: [List[str], optional] When auto_log=True, the log_dataset_features can be specified to control the log dataset features behavior.
            Default is None, all dataset features will be logged. If the list is empty, no dataset features will be logged.
        needs: [List[Callable], optional] The list of the functions that need to be executed before the predict function.

    Examples:
    ```python

    from starwhale import evaluation

    @evaluation.predict
    def predict_image(data):
        ...

    @evaluation.predict(
        dataset="mnist/version/latest",
        batch_size=32,
        replicas=4,
    )
    def predict_batch_images(batch_data)
        ...
    ```

    Returns:
        The decorated function.
    """

    # TODO: support runtime

    if len(args) == 1 and len(kw) == 0 and callable(args[0]):
        return predict()(args[0])
    else:

        def _wrap(func: t.Callable) -> t.Any:
            _register_predict(func, **kw)
            setattr(func, DecoratorInjectAttr.Predict, True)
            return func

        return _wrap


def _register_predict(
    func: t.Callable,
    datasets: t.Optional[t.List[str]] = None,
    resources: t.Optional[t.Dict[str, t.Any]] = None,
    needs: t.Optional[t.List[t.Callable]] = None,
    concurrency: int = 1,
    replicas: int = 1,
    batch_size: int = 1,
    fail_on_error: bool = True,
    auto_log: bool = True,
    log_mode: str = PredictLogMode.PICKLE.value,
    log_dataset_features: t.Optional[t.List[str]] = None,
) -> None:
    from .job import Handler

    Handler.register(
        name="predict",
        resources=resources,
        concurrency=concurrency,
        needs=needs,
        replicas=replicas,
        require_dataset=True,
        extra_kwargs=dict(
            predict_batch_size=batch_size,
            ignore_error=not fail_on_error,
            predict_auto_log=auto_log,
            predict_log_mode=log_mode,
            predict_log_dataset_features=log_dataset_features,
            dataset_uris=datasets,
        ),
    )(func)


def evaluate(*args: t.Any, **kw: t.Any) -> t.Any:
    """Defines an evaluate function.

    This function can be used as a decorator to define an evaluation-evaluate function that reduces the results of the
    predict function.

    Argument:
        needs: [List[Callable], required] The list of the functions that need to be executed before the evaluate function.
        use_predict_auto_log: [bool, optional] Passing the iterator of the predict auto-log results into the evaluate function.
            Default is True.
        resources: [Dict, optional] Resources for the predict task, such as memory, gpu etc. Current only supports
            the cloud instance.

    Examples:
    ```python
    from starwhale import evaluation

    @evaluation.evaluate(needs=[predict_image])
    def evaluate_results(predict_result_iter):
        ...

    @evaluation.evaluate(
        use_predict_auto_log=False,
        needs=[predict_image],
    )
    def evaluate_results():
        ...
    ```

    Returns:
        The decorated function.
    """

    def _wrap(func: t.Callable) -> t.Any:
        _register_evaluate(func, **kw)
        setattr(func, DecoratorInjectAttr.Evaluate, True)
        return func

    return _wrap


def _register_evaluate(
    func: t.Callable,
    needs: t.Optional[t.List[t.Callable]] = None,
    resources: t.Optional[t.Dict[str, t.Any]] = None,
    use_predict_auto_log: bool = True,
) -> None:
    from .job import Handler

    if not needs:
        raise ValueError("needs is required for evaluate function")

    Handler.register(
        name="evaluate",
        resources=resources,
        concurrency=1,
        replicas=1,
        needs=needs,
        extra_kwargs=dict(
            predict_auto_log=use_predict_auto_log,
        ),
    )(func)
