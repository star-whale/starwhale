from __future__ import annotations

import io
import sys
import time
import typing as t
import logging
from abc import ABCMeta, abstractmethod
from types import TracebackType
from pathlib import Path
from functools import wraps

import loguru
import jsonlines

from starwhale import URI
from starwhale.utils import now_str
from starwhale.consts import CURRENT_FNAME
from starwhale.api.job import Context
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.api._impl import wrapper
from starwhale.base.type import URIType, RunSubDirType
from starwhale.utils.log import StreamWrapper
from starwhale.utils.error import FieldTypeOrValueError
from starwhale.api._impl.job import context_holder
from starwhale.core.job.model import STATUS
from starwhale.core.eval.store import EvaluationStorage
from starwhale.core.dataset.tabular import get_dataset_consumption
from starwhale.api._impl.dataset.loader import get_data_loader


class _LogType:
    SW = "starwhale"
    USER = "user"


_jl_writer: t.Callable[[Path], jsonlines.Writer] = lambda p: jsonlines.open(
    str((p).resolve()), mode="w"
)


class PPLResultStorage:
    def __init__(self, context: Context) -> None:
        self.evaluation = wrapper.Evaluation(
            eval_id=context.version, project=context.project
        )

    def save(self, data_id: t.Union[int, str], result: t.Any, **kwargs: t.Any) -> None:
        self.evaluation.log_result(
            data_id=data_id, result=result, **kwargs, serialize=True
        )

    def flush(self) -> None:
        self.evaluation.flush_result()

    def __exit__(self) -> None:
        self.evaluation.close()


class PPLResultIterator:
    def __init__(self, context: Context) -> None:
        self.evaluation = wrapper.Evaluation(
            eval_id=context.version, project=context.project
        )

    def __iter__(self) -> t.Iterator[t.Dict[str, t.Any]]:
        # TODO: use class to refactor data
        return self.evaluation.get_results(deserialize=True)

    def __exit__(self) -> None:
        self.evaluation.close()


class PipelineHandler(metaclass=ABCMeta):
    def __init__(
        self,
        ignore_annotations: bool = False,
        ignore_error: bool = False,
        flush_result: bool = False,
    ) -> None:
        self.context: Context = context_holder.context

        # TODO: add args for compare result and label directly
        self.ignore_annotations = ignore_annotations
        self.ignore_error = ignore_error
        self.flush_result = flush_result

        _logdir = EvaluationStorage.local_run_dir(
            self.context.project, self.context.version
        )
        _run_dir = (
            _logdir / RunSubDirType.RUNLOG / self.context.step / str(self.context.index)
        )
        self.status_dir = _run_dir / RunSubDirType.STATUS
        self.log_dir = _run_dir / RunSubDirType.LOG
        ensure_dir(self.status_dir)
        ensure_dir(self.log_dir)

        self.logger, self._sw_logger = self._init_logger(self.log_dir)
        self._stdout_changed = False
        self._stderr_changed = False
        self._orig_stdout = sys.stdout
        self._orig_stderr = sys.stderr
        # TODO: split status/result files
        self._timeline_writer = _jl_writer(self.status_dir / "timeline")

        self.evaluation = wrapper.Evaluation(
            eval_id=self.context.version, project=self.context.project
        )
        self._monkey_patch()
        self._update_status(STATUS.START)

    def _init_logger(
        self, log_dir: Path, rotation: str = "500MB"
    ) -> t.Tuple[loguru.Logger, loguru.Logger]:
        # TODO: remove logger first?
        # TODO: add custom log format, include daemonset pod name
        from loguru import logger as _logger

        # TODO: configure log rotation size
        _logger.add(
            log_dir / "{time}.log",
            rotation=rotation,
            backtrace=True,
            diagnose=True,
            serialize=True,
        )
        _logger.bind(
            type=_LogType.USER,
            task_id=self.context.index,
            job_id=self.context.version,
        )
        _sw_logger = _logger.bind(type=_LogType.SW)
        return _logger, _sw_logger

    def _monkey_patch(self) -> None:
        if not isinstance(sys.stdout, StreamWrapper) and isinstance(
            sys.stdout, io.TextIOWrapper
        ):
            sys.stdout = StreamWrapper(sys.stdout, self.logger, logging.INFO)  # type: ignore
            self._stdout_changed = True

        if not isinstance(sys.stderr, StreamWrapper) and isinstance(
            sys.stderr, io.TextIOWrapper
        ):
            sys.stderr = StreamWrapper(sys.stderr, self.logger, logging.WARN)  # type: ignore
            self._stderr_changed = True

    def __str__(self) -> str:
        return f"PipelineHandler status@{self.status_dir}, " f"log@{self.log_dir}"

    def __enter__(self) -> "PipelineHandler":
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        self._sw_logger.debug(
            f"execute {self.context.step}-{self.context.index} exit func..."
        )
        if value:
            print(f"type:{type}, exception:{value}, traceback:{trace}")

        if self._stdout_changed:
            sys.stdout = self._orig_stdout
        if self._stderr_changed:
            sys.stderr = self._orig_stderr
        self._timeline_writer.close()

    @abstractmethod
    def ppl(self, data: t.Any, **kw: t.Any) -> t.Any:
        # TODO: how to handle each element is not equal.
        raise NotImplementedError

    @abstractmethod
    def cmp(self, ppl_result: PPLResultIterator) -> t.Any:
        raise NotImplementedError

    def _record_status(func):  # type: ignore
        @wraps(func)  # type: ignore
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            self: PipelineHandler = args[0]
            self._sw_logger.info(
                f"start to run {func.__name__} function@{self.context.step}-{self.context.index} ..."  # type: ignore
            )
            self._update_status(STATUS.RUNNING)
            try:
                func(*args, **kwargs)  # type: ignore
            except Exception as e:
                self._update_status(STATUS.FAILED)
                self._sw_logger.exception(f"{func} abort, exception: {e}")
                raise
            else:
                self._update_status(STATUS.SUCCESS)

        return _wrapper

    @_record_status  # type: ignore
    def _starwhale_internal_run_cmp(self) -> None:
        now = now_str()
        try:
            ppl_result_loader = PPLResultIterator(self.context)
            self.cmp(ppl_result_loader)
        except Exception as e:
            self._sw_logger.exception(f"cmp exception: {e}")
            self._timeline_writer.write(
                {"time": now, "status": False, "exception": str(e)}
            )
            raise
        else:
            self._timeline_writer.write({"time": now, "status": True, "exception": ""})

    @_record_status  # type: ignore
    def _starwhale_internal_run_ppl(self) -> None:
        result_storage = PPLResultStorage(self.context)

        if not self.context.dataset_uris:
            raise FieldTypeOrValueError("context.dataset_uris is empty")

        # TODO: user custom config batch size, max_retries
        for ds_uri in self.context.dataset_uris:
            _uri = URI(ds_uri, expected_type=URIType.DATASET)
            consumption = get_dataset_consumption(
                dataset_uri=_uri, session_id=self.context.version
            )
            loader = get_data_loader(_uri, session_consumption=consumption)

            cnt = 0
            for _idx, _data, _annotations in loader:
                cnt += 1
                _start = time.time()
                result: t.Any = b""
                exception = None
                _unique_id = f"{_uri.object}_{_idx}"
                try:
                    # TODO: inspect profiling
                    result = self.ppl(_data, annotations=_annotations, index=_unique_id)
                except Exception as e:
                    exception = e
                    self._sw_logger.exception(f"[{_unique_id}] data handle -> failed")
                    if not self.ignore_error:
                        self._update_status(STATUS.FAILED)
                        raise
                else:
                    exception = None

                self._sw_logger.debug(
                    f"[{_unique_id}] use {time.time() - _start:.3f}s, session-id:{self.context.version} @{self.context.step}-{self.context.index}"
                )

                self._timeline_writer.write(
                    {
                        "time": now_str(),
                        "status": exception is None,
                        "exception": str(exception),
                        "index": f"{_unique_id}",
                    }
                )

                result_storage.save(
                    data_id=f"{_unique_id}",
                    result=result,
                    annotations={} if self.ignore_annotations else _annotations,
                )

        if self.flush_result:
            result_storage.flush()

        self._sw_logger.info(
            f"{self.context.step}-{self.context.index} handled {cnt} data items for dataset {self.context.dataset_uris}"
        )

    def _update_status(self, status: str) -> None:
        fpath = self.status_dir / CURRENT_FNAME
        ensure_file(fpath, status)
