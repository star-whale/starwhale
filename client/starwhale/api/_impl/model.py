from __future__ import annotations

import io
import os
import sys
import json
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

from starwhale.utils import now_str, in_production
from starwhale.consts import CURRENT_FNAME
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.log import StreamWrapper
from starwhale.consts.env import SWEnv

from .loader import DataField, DataLoader, get_data_loader, SimpleDataLoader
from .wrapper import BaseEvaluation
from ...base.type import URIType

_TASK_ROOT_DIR = "/var/starwhale" if in_production() else "/tmp/starwhale"

_p = lambda p, sub: Path(p) if p else Path(_TASK_ROOT_DIR) / sub
_ptype = t.Union[str, None, Path]


class _LogType:
    SW = "starwhale"
    USER = "user"


_jl_writer = lambda p: jsonlines.open(str((p).resolve()), mode="w")


class _RunConfig:
    def __init__(
        self,
        dataset_uri: str = "",
        dataset_row_start: int = 0,
        dataset_row_end: int = -1,
        status_dir: _ptype = "",
        log_dir: _ptype = "",
    ) -> None:
        self.status_dir = _p(status_dir, "status")  # type: ignore
        self.log_dir = _p(log_dir, "log")  # type: ignore
        self.result_dir = _p(result_dir, "result")  # type: ignore
        # TODO: refactor dataset arguments
        self.dataset_uri = URI(dataset_uri, expected_type=URIType.DATASET)
        self.dataset_row_end = dataset_row_end
        self.dataset_row_start = dataset_row_start

        # TODO: graceful method
        self._prepare()

    def _prepare(self) -> None:
        ensure_dir(self.log_dir)
        ensure_dir(self.status_dir)

    @classmethod
    def create_by_env(cls) -> _RunConfig:
        _env = os.environ.get
        return _RunConfig(
            status_dir=_env(SWEnv.status_dir),
            log_dir=_env(SWEnv.log_dir),
            dataset_uri=_env(SWEnv.dataset_uri, ""),
            dataset_row_start=int(_env(SWEnv.dataset_row_start, 0)),
            dataset_row_end=int(_env(SWEnv.dataset_row_end, -1)),
        )

    @classmethod
    def set_env(cls, _config: t.Dict[str, t.Any] = {}) -> None:
        def _set(_k: str, _e: str) -> None:
            _v = _config.get(_k)
            if _v:
                os.environ[_e] = str(_v)

        _set("status_dir", SWEnv.status_dir)
        _set("log_dir", SWEnv.log_dir)
        _set("result_dir", SWEnv.result_dir)
        _set("dataset_uri", SWEnv.dataset_uri)
        _set("dataset_row_start", SWEnv.dataset_row_start)
        _set("dataset_row_end", SWEnv.dataset_row_end)


class PipelineHandler(metaclass=ABCMeta):
    class ResultOutputType:
        JSONL = "jsonline"
        PLAIN = "plain"

    class STATUS:
        START = "start"
        RUNNING = "running"
        SUCCESS = "success"
        FAILED = "failed"

    def __init__(
        self,
        evaluation: BaseEvaluation,
        merge_label: bool = True,
        output_type: str = ResultOutputType.JSONL,
        ignore_error: bool = False,
    ) -> None:
        # TODO: add args for compare result and label directly
        self.merge_label = merge_label
        self.output_type = output_type
        self.ignore_error = ignore_error
        # TODO: use datastore when dataset complete
        self.config = _RunConfig.create_by_env()

        self.logger, self._sw_logger = self._init_logger()
        self._stdout_changed = False
        self._stderr_changed = False
        self._orig_stdout = sys.stdout
        self._orig_stderr = sys.stderr

        self._data_loader = get_data_loader(
            dataset_uri=self.config.dataset_uri,
            start=self.config.dataset_row_start,
            end=self.config.dataset_row_end,
            logger=self._sw_logger,
        )

        # TODO: split status/result files
        self._status_writer = _jl_writer(self.config.status_dir / "timeline")  # type: ignore

        self._ppl_data_field = "result"
        self._label_field = "label"
        self.evaluation = evaluation
        self._simple_step_name = ""
        self._monkey_patch()

    def _init_logger(self) -> t.Tuple[loguru.Logger, loguru.Logger]:
        # TODO: remove logger first?
        # TODO: add custom log format, include daemonset pod name
        from loguru import logger as _logger

        # TODO: configure log rotation size
        _logger.add(
            self.config.log_dir / "{time}.log",
            rotation="500MB",
            backtrace=True,
            diagnose=True,
            serialize=True,
        )
        _logger.bind(
            type=_LogType.USER,
            task_id=os.environ.get("SW_TASK_ID", ""),
            job_id=os.environ.get("SW_JOB_ID", ""),
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
        return (
            f"PipelineHandler status@{self.config.status_dir}, "
            f"log@{self.config.log_dir}"
        )

    def __enter__(self) -> PipelineHandler:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:
            print(f"type:{type}, exception:{value}, traceback:{trace}")

        if self._stdout_changed:
            sys.stdout = self._orig_stdout
        if self._stderr_changed:
            sys.stderr = self._orig_stderr

        self.logger.remove()
        self._sw_logger.remove()

    @abstractmethod
    def ppl(self, data: bytes, batch_size: int, **kw: t.Any) -> t.Any:
        # TODO: how to handle each batch element is not equal.
        raise NotImplementedError

    @abstractmethod
    def cmp(self, _data_loader: DataLoader) -> t.Any:
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

    def handle_label(self, label: bytes, batch_size: int, **kw: t.Any) -> t.Any:
        return label.decode()

    def _record_status(func):  # type: ignore
        @wraps(func)  # type: ignore
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> None:
            self: PipelineHandler = args[0]
            self._sw_logger.info(f"start to run {func}...")
            self._update_status(self.STATUS.RUNNING)
            try:
                func(*args, **kwargs)  # type: ignore
            except Exception as e:
                self._update_status(self.STATUS.FAILED)
                self._sw_logger.exception(f"{func} abort, exception: {e}")
                raise
            else:
                self._update_status(self.STATUS.SUCCESS)
                self._sw_logger.info("finish.")

        return _wrapper

    @_record_status  # type: ignore
    def _starwhale_internal_run_cmp(self) -> None:
        self._simple_step_name = "cmp"
        self._update_status(self.STATUS.START)
        now = now_str()  # type: ignore
        try:
            _ppl_results = [result for result in self.evaluation.get_results()]
            self._sw_logger.debug("cmp input data size:{}", len(_ppl_results))
            _data_loader = SimpleDataLoader(
                _ppl_results, self._sw_logger, deserializer=self.deserialize_fields
            )
            output = self.cmp(_data_loader)
        except Exception as e:
            self._sw_logger.exception(f"cmp exception: {e}")
            self._status_writer.write({"time": now, "status": False, "exception": e})
            raise
        else:
            self._status_writer.write({"time": now, "status": True, "exception": ""})
            self._sw_logger.debug(f"cmp result:{output}")
            self.evaluation.log_metrics(output)

    @_record_status  # type: ignore
    def _starwhale_internal_run_ppl(self) -> None:
        self._simple_step_name = "ppl"
        self._update_status(self.STATUS.START)

        # TODO: use datastore when dataset complete
        _data_loader = get_data_loader(
            self.config.load_swds_config(),
            self._sw_logger,
            deserializer=self.deserialize,
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
                    data.data,
                    data.batch_size,
                    data_index=data.idx,
                    data_size=data.data_size,
                    label_content=label.data,
                    label_size=label.data_size,
                    label_batch=label.batch_size,
                    label_index=label.idx,
                    ds_name=data.ext_attr.get("ds_name", ""),
                    ds_version=data.ext_attr.get("ds_version", ""),
                )
            except Exception as e:
                exception = e
                self._sw_logger.exception(f"[{data.idx}] data handle -> failed")
                if not self.ignore_error:
                    self._update_status(self.STATUS.FAILED)
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
            "time": now_str(),  # type: ignore
            "status": exception is None,
            "exception": str(exception),
            "index": data.idx,
            "output_tuple_len": len(args),
        }
        self._status_writer.write(_timeline)

        _label = ""
        if self.merge_label:
            try:
                label = self.handle_label(
                    label.data,
                    label.batch_size,
                    index=label.idx,
                    size=label.data_size,
                )
                _label = base64.b64encode(self.label_data_serialize(label)).decode(
                    "ascii"
                )
            except Exception as e:
                self._sw_logger.exception(f"{label.data!r} label handle exception:{e}")
                if not self.ignore_error:
                    self._update_status(self.STATUS.FAILED)
                    raise
                else:
                    _label = ""

        self._sw_logger.debug(f"record ppl result:{data.idx}")
        self.evaluation.log_result(
            data_id=str(data.idx),
            result=base64.b64encode(self.ppl_data_serialize(*args)).decode("ascii"),
            batch=data.batch_size,
            label=_label,
        )
        self._update_status(self.STATUS.RUNNING)

    def _update_status(self, status: str) -> None:
        fpath = self.config.status_dir / CURRENT_FNAME
        ensure_file(fpath, status)
