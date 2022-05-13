from __future__ import annotations
import os
import sys
import typing as t
from abc import ABCMeta, abstractmethod
from pathlib import Path
import json
import logging
from functools import wraps

import loguru
import jsonlines

from starwhale.consts.env import SWEnv
from starwhale.utils.log import StreamWrapper
from starwhale.utils.error import NotFoundError
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils import pretty_bytes, in_production, now_str
from starwhale.consts import CURRENT_FNAME
from .loader import DataField, DataLoader, get_data_loader

_TASK_ROOT_DIR = "/var/starwhale" if in_production() else "/tmp/starwhale"

_p = lambda p, sub: Path(p) if p else Path(_TASK_ROOT_DIR) / sub
_ptype = t.Union[str, None, Path]


class _LogType:
    SW = "starwhale"
    USER = "user"


_jl_writer = lambda p: jsonlines.open(str((p).resolve()), mode="w")


class _RunConfig(object):
    def __init__(
        self,
        swds_config_path: _ptype = "",
        status_dir: _ptype = "",
        log_dir: _ptype = "",
        result_dir: _ptype = "",
    ) -> None:
        self.status_dir = _p(status_dir, "status")
        self.log_dir = _p(log_dir, "log")
        self.result_dir = _p(result_dir, "result")
        self.swds_config = self.load_swds_config(swds_config_path)

        # TODO: graceful method
        self._prepare()

    def load_swds_config(self, path: _ptype) -> t.Dict[str, t.Any]:
        if not path:
            path = Path(_TASK_ROOT_DIR) / "config" / "swds.json"

        path = Path(path) if isinstance(path, str) else path
        if path.exists():
            # TODO: validate swds config
            return json.load(path.open("r"))
        else:
            raise NotFoundError(f"{path} not found")

    def _prepare(self) -> None:
        ensure_dir(self.log_dir)
        ensure_dir(self.result_dir)
        ensure_dir(self.status_dir)

    @classmethod
    def create_by_env(cls) -> "_RunConfig":
        _env = os.environ.get
        return _RunConfig(
            swds_config_path=_env(SWEnv.input_config),
            status_dir=_env(SWEnv.status_dir),
            log_dir=_env(SWEnv.log_dir),
            result_dir=_env(SWEnv.result_dir),
        )

    @classmethod
    def set_env(cls, _config: dict = {}) -> None:
        def _set(_k: str, _e: str) -> None:
            _v = _config.get(_k)
            if _v:
                os.environ[_e] = _v

        _set("status_dir", SWEnv.status_dir)
        _set("log_dir", SWEnv.log_dir)
        _set("result_dir", SWEnv.result_dir)
        _set("input_config", SWEnv.input_config)


class PipelineHandler(object):
    class ResultOutputType:
        JSONL = "jsonline"
        PLAIN = "plain"

    class STATUS:
        START = "start"
        RUNNING = "running"
        SUCCESS = "success"
        FAILED = "failed"

    __metaclass__ = ABCMeta

    def __init__(
        self,
        merge_label: bool = True,
        output_type: str = ResultOutputType.JSONL,
        ignore_error: bool = False,
    ) -> None:
        # TODO: add args for compare result and label directly
        self.merge_label = merge_label
        self.output_type = output_type
        self.ignore_error = ignore_error
        self.config = _RunConfig.create_by_env()

        self.logger, self._sw_logger = self._init_logger()
        self._orig_stdout = sys.stdout
        self._orig_stderr = sys.stderr

        self._data_loader = get_data_loader(self.config.swds_config, self._sw_logger)
        # TODO: split status/result files
        self._result_writer = _jl_writer(self.config.result_dir / CURRENT_FNAME)
        self._status_writer = _jl_writer(self.config.status_dir / "timeline")

        # TODO: find some elegant call method
        self._monkey_patch()
        self._update_status(self.STATUS.START)

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
        if not isinstance(sys.stdout, StreamWrapper):
            sys.stdout = StreamWrapper(sys.stdout, self.logger, logging.INFO)  # type: ignore

        if not isinstance(sys.stderr, StreamWrapper):
            sys.stderr = StreamWrapper(sys.stderr, self.logger, logging.WARN)  # type: ignore

    def __str__(self) -> str:
        return (
            f"PipelineHandler status@{self.config.status_dir}, "
            f"log@{self.config.log_dir}, result@{self.config.result_dir}"
        )

    def __exit__(self) -> None:
        # TODO: reset sys for stdout/stderr?
        sys.stdout = self._orig_stdout
        sys.stderr = self._orig_stderr

        try:
            self._result_writer.close()
        except Exception as e:
            self._sw_logger.exception(f"result writer close exception: {e}")

        try:
            self._status_writer.close()
        except Exception as e:
            self._sw_logger.exception(f"status writer close exception: {e}")

        self.logger.remove()
        self._sw_logger.remove()

    @abstractmethod
    def ppl(self, data: bytes, batch_size: int, **kw: t.Any) -> t.Any:
        # TODO: how to handle each batch element is not equal.
        raise NotImplementedError

    @abstractmethod
    def cmp(self, _data_loader: DataLoader) -> t.Any:
        raise NotImplementedError

    def handle_label(self, label: bytes, batch_size: int, **kw: t.Any) -> t.Any:
        return label.decode()

    def _record_status(func):  # type: ignore
        @wraps(func)  # type: ignore
        def _wrapper(*args, **kwargs):
            self: PipelineHandler = args[0]
            self._sw_logger.info(f"start to run {func}...")
            self._update_status(self.STATUS.RUNNING)
            try:
                func(*args, **kwargs)  # type: ignore
            except Exception as e:
                self._update_status(self.STATUS.FAILED)
                self._sw_logger.exception(f"{func} abort, exception: {e}")
            else:
                self._update_status(self.STATUS.SUCCESS)
                self._sw_logger.info("finish.")

        return _wrapper

    @_record_status  # type: ignore
    def _starwhale_internal_run_cmp(self) -> None:
        ex = None
        try:
            output = self.cmp(self._data_loader)
        except Exception as e:
            ex = e
            self._sw_logger.exception(f"cmp exception: {e}")
            raise

        try:
            self._status_writer.write(
                {"time": now_str(), "status": ex is None, "exception": ex}  # type: ignore
            )
            self._result_writer.write(output)
        except Exception as e:
            self._sw_logger.exception(f"cmp record exception: {e}")

    @_record_status  # type: ignore
    def _starwhale_internal_run_ppl(self) -> None:
        for data, label in self._data_loader:
            self._sw_logger.info(
                f"[{data.idx}]data-label loaded, data size:{pretty_bytes(data.data_size)}, "
                f"label size:{pretty_bytes(label.data_size)} ,batch:{data.batch_size}"
            )

            if data.idx != label.idx:
                msg = (
                    f"data index[{data.idx}] is not equal label index [{label.idx}], "
                    f"{'ignore error' if self.ignore_error else ''}"
                )
                self._sw_logger.error(msg)
                if not self.ignore_error:
                    raise Exception(msg)

            output = b""
            exception = None
            try:
                # TODO: inspect profiling
                output = self.ppl(
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
                self._sw_logger.info(f"[{data.idx}] data handle -> success")

            try:
                self._do_record(output, data, label, exception)
            except Exception as e:
                self._sw_logger.exception(f"{data.idx} data record exception: {e}")

    def _do_record(
        self,
        output: t.Any,
        data: DataField,
        label: DataField,
        exception: t.Union[None, Exception],
    ) -> None:
        self._status_writer.write(
            {
                "time": now_str(),  # type: ignore
                "status": exception is None,
                "exception": str(exception),
                "index": data.idx,
                "output_size": len(output),
            }
        )

        # TODO: output maybe cannot be jsonized
        result = {
            "index": data.idx,
            "result": output,
            "batch": data.batch_size,
        }
        if self.merge_label:
            try:
                result["label"] = self.handle_label(
                    label.data,
                    label.batch_size,
                    index=label.idx,
                    size=label.data_size,
                )
            except Exception as e:
                self._sw_logger.exception(f"{label.data!r} label handle exception:{e}")
                if not self.ignore_error:
                    self._update_status(self.STATUS.FAILED)
                    raise
                else:
                    result["label"] = ""

        self._result_writer.write(result)
        self._update_status(self.STATUS.RUNNING)

    def _update_status(self, status: str) -> None:
        fpath = self.config.status_dir / CURRENT_FNAME
        ensure_file(fpath, status)
