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
from starwhale.consts import RunStatus, CURRENT_FNAME
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import RunSubDirType, PredictLogMode
from starwhale.api.service import Service
from starwhale.utils.error import ParameterError, FieldTypeOrValueError
from starwhale.base.context import Context
from starwhale.core.job.store import JobStorage
from starwhale.api._impl.dataset import Dataset
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.tabular import TabularDatasetInfo

from .log import Evaluation

_jl_writer: t.Callable[[Path], jsonlines.Writer] = lambda p: jsonlines.open(
    str((p).resolve()), mode="w"
)


class PipelineHandler(metaclass=ABCMeta):
    _INPUT_PREFIX = "input/"
    _registered_run_info: t.Dict[str, t.Dict] = {}
    _registering_lock = threading.Lock()

    def __init__(
        self,
        predict_batch_size: int = 1,
        ignore_error: bool = False,
        predict_auto_log: bool = True,
        predict_log_mode: str = PredictLogMode.PICKLE.value,
        predict_log_dataset_features: t.Optional[t.List[str]] = None,
        dataset_uris: t.Optional[t.List[str | Resource]] = None,
        **kwargs: t.Any,
    ) -> None:
        self.predict_batch_size = predict_batch_size
        self.svc = Service()
        self.context = Context.get_runtime_context()

        self.dataset_uris = self.context.dataset_uris or dataset_uris or []
        self.dataset_head = self.context.dataset_head

        self.predict_log_dataset_features = predict_log_dataset_features
        self.ignore_error = ignore_error
        self.predict_auto_log = predict_auto_log
        self.predict_log_mode = PredictLogMode(predict_log_mode)
        self.kwargs = kwargs

        # TODO: whether store to the target which point to
        _logdir = JobStorage.local_run_dir(
            self.context.run_project.id, self.context.version
        )
        _run_dir = (
            _logdir / RunSubDirType.RUNLOG / self.context.step / str(self.context.index)
        )
        self.status_dir = _run_dir / RunSubDirType.STATUS
        ensure_dir(self.status_dir)

        # TODO: split status/result files
        self._timeline_writer = _jl_writer(self.status_dir / "timeline")

        self.evaluation_store = Evaluation(
            id=self.context.version, project=self.context.log_project
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

    @classmethod
    def run(
        cls, resources: t.Optional[t.Dict[str, t.Any]] = None, replicas: int = 1
    ) -> t.Callable:
        """The decorator can be used to define the resources and replicas for the predict and evaluate function of the PipelineHandler.

        Arguments:
            resources: [Dict, optional] Resources for the predict/evaluation task, such as cpu, memory, nvidia.com/gpu etc. Current only supports
                the Server instance.
            replicas: [int, optional] The number of the predict tasks. Default is 1.
                For the evaluate function, the replicas option is not supported that the replicas is always 1.

        Returns:
            [Callable] The decorator function.

        Examples:
        ```python
        from starwhale import PipelineHandler

        class MyPipeline(PipelineHandler):

        @PipelineHandler.run(resources={"memory": 200 * 1024 * 1024, "nvidia.com/gpu": 1}, replicas=4)
        def predict(self, data):
            ...

        @PipelineHandler.run(resources={"memory": 200 * 1024 * 1024})
        def evaluate(self, ppl_result: t.Iterator):
            ...
        ```
        """

        def decorator(func: t.Callable) -> t.Callable:
            if not inspect.isfunction(func):
                raise ParameterError(
                    f"{func} is not a function, @PipelineHandler.run decorator can only be used on predict/evaluate function"
                )

            class_name, _, name = func.__qualname__.rpartition(".")
            if not class_name or "." in class_name:
                raise ParameterError(
                    f"{func} is not a class method or is an inner class method"
                )

            # TODO: add class check: is subclass of PipelineHandler

            # compatible with the old version
            if name == "ppl":
                name = "predict"
            elif name == "cmp":
                name = "evaluate"

            if name not in ("predict", "evaluate"):
                raise ParameterError(
                    f"{name} is not a valid function name, @PipelineHandler.run decorator can only be used on predict/evaluate function"
                )

            if name == "evaluate" and replicas != 1:
                raise ParameterError(
                    "evaluate function does not support replicas option, replicas always is 1"
                )

            with cls._registering_lock:
                cls._registered_run_info[f"{class_name}.{name}"] = {
                    "resources": resources,
                    "replicas": replicas,
                }

            return func

        return decorator

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
            func(self._iter_predict_result(self.evaluation_store.scan_results()))
        else:
            func()

    @_record_status  # type: ignore
    def _starwhale_internal_run_predict(self) -> None:
        if not self.dataset_uris:
            raise FieldTypeOrValueError("context.dataset_uris is empty")
        join_str = "_#@#_"

        received_rows_cnt = 0
        # TODO: user custom config batch size, max_retries
        for _uri in self.dataset_uris:
            if isinstance(_uri, str):
                _uri = Resource(_uri, typ=ResourceType.dataset)
            ds = Dataset.dataset(_uri, readonly=True)
            ds.make_distributed_consumption(session_id=self.context.version)
            dataset_info = ds.info
            if _uri.instance.is_local:
                # avoid confusion with underscores in project names
                idx_prefix = f"{_uri.project.id}/{_uri.name}"
            else:
                r_id = _uri.info().get("id")
                if not r_id:
                    raise KeyError("fetch dataset id error")
                idx_prefix = str(r_id)

            dataset_consumed_rows = 0
            for rows in ds.batch_iter(self.predict_batch_size):
                rows_cnt = len(rows)
                if self.dataset_head > 0:
                    rows_cnt = min(rows_cnt, self.dataset_head - dataset_consumed_rows)
                    if rows_cnt <= 0:
                        break

                rows = rows[:rows_cnt]
                dataset_consumed_rows += rows_cnt
                received_rows_cnt += rows_cnt
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

                if len(rows) != len(_results):
                    console.warn(
                        f"The number of results({len(_results)}) is not equal to the number of rows({len(rows)})"
                        "maybe batch predict does not return the expected results or ignore some predict exceptions"
                    )

                for (_idx, _features), _result in zip(rows, _results):
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

        self.evaluation_store.flush_all(artifacts_flush=True)

        console.info(
            f"{self.context.step}-{self.context.index} received {received_rows_cnt} data items for dataset {self.dataset_uris}"
        )

    def _update_status(self, status: str) -> None:
        fpath = self.status_dir / CURRENT_FNAME
        ensure_file(fpath, status)

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

        input_features = {
            f"{self._INPUT_PREFIX}{k}": v for k, v in _log_features.items()
        }
        if self.predict_log_mode == PredictLogMode.PICKLE:
            output = dill.dumps(output)

        # for plain mode: if the output is dict type, we(datastore) will log each key-value pair as a column
        metrics = {
            "_mode": self.predict_log_mode.value,
            "_index": idx,
            "output": output,
            "duration_seconds": duration_seconds,
            **input_features,
        }
        self.evaluation_store.log_result(id=idx_with_ds, metrics=metrics)
