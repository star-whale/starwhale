from __future__ import annotations

import io
import os
import sys
import json
import math
import base64
import typing as t
import logging
from abc import ABCMeta, abstractmethod
from types import TracebackType
from pathlib import Path
from functools import wraps

import dill
import loguru
import jsonlines

from starwhale.utils import now_str
from starwhale.consts import CURRENT_FNAME
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.base.type import URIType, RunSubDirType
from starwhale.utils.log import StreamWrapper
from starwhale.consts.env import SWEnv
from starwhale.utils.error import FieldTypeOrValueError
from starwhale.api._impl.job import Context
from starwhale.core.job.model import STATUS
from starwhale.core.eval.store import EvaluationStorage
from starwhale.api._impl.dataset import DataField, get_data_loader
from starwhale.api._impl.wrapper import Evaluation
from starwhale.core.dataset.model import Dataset


class _LogType:
    SW = "starwhale"
    USER = "user"


_jl_writer: t.Callable[[Path], jsonlines.Writer] = lambda p: jsonlines.open(
    str((p).resolve()), mode="w"
)


def calculate_index(
    data_size: int, task_num: int, task_index: int
) -> t.Tuple[int, int]:
    _batch_size = 1
    if data_size > task_num:
        _batch_size = math.ceil(data_size / task_num)
    _start_index = min(_batch_size * task_index, data_size - 1)
    _end_index = min(_batch_size * (task_index + 1) - 1, data_size - 1)
    return _start_index, _end_index


class ResultLoader:
    def __init__(
        self,
        data: t.List[t.Any],
        deserializer: t.Optional[t.Callable] = None,
    ) -> None:
        self.data = data
        self.deserializer = deserializer

    def __iter__(self) -> t.Any:
        for _data in self.data:
            if self.deserializer:
                yield self.deserializer(_data)
                continue
            yield _data


class PipelineHandler(metaclass=ABCMeta):
    def __init__(
        self,
        context: Context,
        merge_label: bool = True,
        ignore_error: bool = False,
    ) -> None:
        self.context = context
        self._init_dir()

        # TODO: add args for compare result and label directly
        self.merge_label = merge_label

        self.ignore_error = ignore_error

        self.logger, self._sw_logger = self._init_logger()
        self._stdout_changed = False
        self._stderr_changed = False
        self._orig_stdout = sys.stdout
        self._orig_stderr = sys.stderr
        # TODO: split status/result files
        self._timeline_writer = _jl_writer(self.status_dir / "timeline")

        self._ppl_data_field = "result"
        self._label_field = "label"
        self.evaluation = self._init_datastore()

        self._monkey_patch()

    def _init_dir(self) -> None:
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

    def _init_datastore(self) -> Evaluation:
        os.environ[SWEnv.project] = self.context.project
        os.environ[SWEnv.eval_version] = self.context.version
        return Evaluation()

    def _init_logger(self) -> t.Tuple[loguru.Logger, loguru.Logger]:
        # TODO: remove logger first?
        # TODO: add custom log format, include daemonset pod name
        from loguru import logger as _logger

        # TODO: configure log rotation size
        _logger.add(
            self.log_dir / "{time}.log",
            rotation="500MB",
            backtrace=True,
            diagnose=True,
            serialize=True,
        )
        _logger.bind(
            type=_LogType.USER,
            task_id=self.context.index,  # os.environ.get("SW_TASK_ID", ""),
            job_id=self.context.version,  # os.environ.get("SW_JOB_ID", ""),
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

    def __enter__(self) -> PipelineHandler:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        self._sw_logger.debug("execute ppl exit func...")
        if value:
            print(f"type:{type}, exception:{value}, traceback:{trace}")

        if self._stdout_changed:
            sys.stdout = self._orig_stdout
        if self._stderr_changed:
            sys.stderr = self._orig_stderr
        self.evaluation.close()
        self._timeline_writer.close()
        # self.logger.remove()
        # self._sw_logger.remove()

    @abstractmethod
    def ppl(self, data: bytes, **kw: t.Any) -> t.Any:
        # TODO: how to handle each element is not equal.
        raise NotImplementedError

    @abstractmethod
    def cmp(self, _data_loader: ResultLoader) -> t.Any:
        raise NotImplementedError

    def _builtin_serialize(self, *data: t.Any) -> bytes:
        return dill.dumps(data)  # type: ignore

    def ppl_data_serialize(self, *data: t.Any) -> bytes:
        return self._builtin_serialize(*data)

    def ppl_data_deserialize(self, data: bytes) -> t.Any:
        return dill.loads(base64.b64decode(data))

    def label_data_serialize(self, data: t.Any) -> bytes:
        return self._builtin_serialize(data)

    def label_data_deserialize(self, data: bytes) -> bytes:
        return dill.loads(base64.b64decode(data))[0]  # type: ignore

    # todo： waiting remove it
    def deserialize(self, data: t.Union[str, bytes]) -> t.Any:
        ret = json.loads(data)
        ret[self._ppl_data_field] = self.ppl_data_deserialize(ret[self._ppl_data_field])
        ret[self._label_field] = self.label_data_deserialize(ret[self._label_field])
        return ret

    def deserialize_fields(self, data: t.Dict[str, t.Any]) -> t.Any:
        data[self._ppl_data_field] = self.ppl_data_deserialize(
            data[self._ppl_data_field]
        )
        data[self._label_field] = self.label_data_deserialize(data[self._label_field])
        return data

    def handle_label(self, label: t.Any, **kw: t.Any) -> t.Any:
        return label

    def _record_status(func):  # type: ignore
        @wraps(func)  # type: ignore
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            self: PipelineHandler = args[0]
            self._sw_logger.info(f"start to run {func}...")
            self._update_status(STATUS.RUNNING)
            try:
                func(*args, **kwargs)  # type: ignore
            except Exception as e:
                self._update_status(STATUS.FAILED)
                self._sw_logger.exception(f"{func} abort, exception: {e}")
                raise
            else:
                self._update_status(STATUS.SUCCESS)
                self._sw_logger.info("finish.")

        return _wrapper

    @_record_status  # type: ignore
    def _starwhale_internal_run_cmp(self) -> None:
        self._sw_logger.debug("enter cmp func...")
        self._update_status(STATUS.START)
        now = now_str()
        try:
            _ppl_results = list(self.evaluation.get_results())
            self._sw_logger.debug("cmp input data size:{}", len(_ppl_results))
            _data_loader = ResultLoader(
                data=_ppl_results, deserializer=self.deserialize_fields
            )
            output = self.cmp(_data_loader)
        except Exception as e:
            self._sw_logger.exception(f"cmp exception: {e}")
            self._timeline_writer.write({"time": now, "status": False, "exception": e})
            raise
        else:
            self._timeline_writer.write({"time": now, "status": True, "exception": ""})
            self._sw_logger.debug(f"cmp result:{output}")
            self.evaluation.log_metrics(output)

    @_record_status  # type: ignore
    def _starwhale_internal_run_ppl(self) -> None:
        self._update_status(STATUS.START)

        if not self.context.dataset_uris:
            raise FieldTypeOrValueError("context.dataset_uris is empty")
        # TODO: support multi dataset uris
        _dataset_uri = URI(self.context.dataset_uris[0], expected_type=URIType.DATASET)
        _dataset = Dataset.get_dataset(_dataset_uri)
        dataset_row_start, dataset_row_end = calculate_index(
            _dataset.summary().rows, self.context.total, self.context.index
        )

        _data_loader = get_data_loader(
            dataset_uri=_dataset_uri,
            start=dataset_row_start,
            end=dataset_row_end,
            logger=self._sw_logger,
        )
        for data, label in _data_loader:
            if data.idx != label.idx:
                msg = (
                    f"data index[{data.idx}] is not equal label index [{label.idx}], "
                    f"{'ignore error' if self.ignore_error else ''}"
                )
                self._sw_logger.error(msg)
                if not self.ignore_error:
                    raise Exception(msg)

            pred: t.Any = b""
            exception = None
            try:
                # TODO: inspect profiling
                pred = self.ppl(
                    data.data if isinstance(data.data, bytes) else data.data.encode(),
                    data_index=data.idx,
                    data_size=data.data_size,
                    label_content=label.data,
                    label_size=label.data_size,
                    label_index=label.idx,
                    ds_name=data.ext_attr.get("ds_name", ""),
                    ds_version=data.ext_attr.get("ds_version", ""),
                )
            except Exception as e:
                exception = e
                self._sw_logger.exception(f"[{data.idx}] data handle -> failed")
                if not self.ignore_error:
                    self._update_status(STATUS.FAILED)
                    raise
            else:
                exception = None

            self._do_record(data, label, exception, *pred)
        self._sw_logger.debug(
            f"ppl result:{len([item for item in self.evaluation.get_results()])}"
        )

    def _do_record(
        self,
        data: DataField,
        label: DataField,
        exception: t.Union[None, Exception],
        *args: t.Any,
    ) -> None:
        _timeline = {
            "time": now_str(),
            "status": exception is None,
            "exception": str(exception),
            "index": data.idx,
            self._ppl_data_field: base64.b64encode(
                self.ppl_data_serialize(*args)
            ).decode("ascii"),
        }
        self._timeline_writer.write(_timeline)

        _label = ""
        if self.merge_label:
            try:
                label = self.handle_label(
                    label.data,
                    index=label.idx,
                    size=label.data_size,
                )
                _label = base64.b64encode(self.label_data_serialize(label)).decode(
                    "ascii"
                )
            except Exception as e:
                self._sw_logger.exception(f"{label.data!r} label handle exception:{e}")
                if not self.ignore_error:
                    self._update_status(STATUS.FAILED)
                    raise
                else:
                    _label = ""
        # self._sw_logger.debug(f"record ppl result:{data.idx}")
        self.evaluation.log_result(
            data_id=str(data.idx),
            result=base64.b64encode(self.ppl_data_serialize(*args)).decode("ascii"),
            data_size=data.data_size,
            label=_label,
        )
        self._update_status(STATUS.RUNNING)

    def _update_status(self, status: str) -> None:
        fpath = self.status_dir / CURRENT_FNAME
        ensure_file(fpath, status)
