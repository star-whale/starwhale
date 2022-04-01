import os
import sys
import typing as t
from abc import ABCMeta, abstractmethod
from collections import namedtuple
from pathlib import Path
import json
import logging
from datetime import datetime

import loguru
import jsonlines
from starwhale.consts import FMT_DATETIME

from starwhale.utils.log import StreamWrapper
from starwhale.utils.error import NotFoundError
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils import pretty_bytes
from .loader import DATA_FIELD, get_data_loader

_TASK_ROOT_DIR = "/var/starwhale"

_p = lambda p, sub: Path(p) if p else Path(_TASK_ROOT_DIR) / sub
_ptype = t.Union[str, None, Path]

_LOG_TYPE = namedtuple("LOG_TYPE", ["SW", "USER"])(
    "starwhale", "user"
)
_jl_writer = lambda p: jsonlines.open(str((p).resolve()), mode="w")

class _RunConfig(object):

    def __init__(self, swds_config_path: _ptype="",
                 status_dir: _ptype="",
                 log_dir: _ptype="",
                 result_dir: _ptype="") -> None:
        self.status_dir = _p(status_dir, "status")
        self.log_dir = _p(log_dir, "log")
        self.result_dir = _p(result_dir, "result")
        self.swds_config = self.load_swds_config(swds_config_path)

        #TODO: graceful method
        self._prepare()

    def load_swds_config(self, path: _ptype) -> dict:
        if not path:
            path = Path(_TASK_ROOT_DIR) / "config" / "swds.json"

        path = Path(path) if isinstance(path, str) else path
        if path.exists():
            #TODO: validate swds config
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
            swds_config_path=_env("SW_TASK_SWDS_CONFIG"),
            status_dir=_env("SW_TASK_STATUS_DIR"),
            log_dir=_env("SW_TASK_LOG_DIR"),
            result_dir=_env("SW_TASK_RESULT_DIR"),
        )


class PipelineHandler(object):
    RESULT_OUTPUT_TYPE = namedtuple("OUTPUT_TYPE", ["JSONL", "PLAIN"])("jsonline", "plain")
    STATUS = namedtuple("STATUS", ["START", "RUNNING", "SUCCESS", "FAILED"])("start", "running", "success", "failed")

    __metaclass__ = ABCMeta

    def __init__(self, merge_label: bool=False,
                 output_type: str= RESULT_OUTPUT_TYPE.JSONL,
                 ignore_error: bool=False) -> None:
        #TODO: add args for compare result and label directly
        self.merge_label = merge_label
        self.output_type = output_type
        self.ignore_error = ignore_error

        self.logger, self._sw_logger = self._init_logger()
        self.config = _RunConfig.create_by_env()
        self._orig_stdout = sys.stdout
        self._orig_stderr = sys.stderr

        self._data_loader = get_data_loader(self.config.swds_config, self._sw_logger)
        #TODO: split status/result files
        self._result_writer = _jl_writer(self.config.result_dir / "current")
        self._status_writer = _jl_writer(self.config.status_dir / "timeline")

        #TODO: find some elegant call method
        self._monkey_patch()
        self._update_status(self.STATUS.START)

    def _init_logger(self) -> t.Tuple[loguru.Logger, loguru.Logger]:
        #TODO: remove logger first?
        #TODO: add custom log format, include daemonset pod name
        from loguru import logger as _logger

        #TODO: configure log rotation size
        _logger.add("{time}.log", rotation="500MB", backtrace=True, diagnose=True, serialize=True)
        _logger.bind(type=_LOG_TYPE.USER, task_id=os.environ.get("SW_TASK_ID", ""), job_id=os.environ.get("SW_GROUP_ID", ""))
        _sw_logger = _logger.bind(type=_LOG_TYPE.SW)
        return _logger, _sw_logger

    def _monkey_patch(self):
        if not isinstance(sys.stdout, StreamWrapper):
            sys.stdout = StreamWrapper(sys.stdout, self.logger, logging.INFO)

        if not isinstance(sys.stderr, StreamWrapper):
            sys.stderr = StreamWrapper(sys.stderr, self.logger, logging.WARN)

    def __exit__(self):
        #TODO: reset sys for stdout/stderr?
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
    def handle(self, data: bytes, batch_size: int, **kw) -> t.Any:
        raise NotImplementedError

    def starwhale_internal_run(self) -> None:
        #TODO: forbid inherit object override this method
        self._sw_logger.info("start to run pipeline...")

        self._update_status(self.STATUS.RUNNING)

        for data, label in self._data_loader:
            self._sw_logger.info(f"[{data.index}] data loaded, size:{pretty_bytes(data.data_size)}, batch:{data.batch_size}")
            output = b""
            exception = None
            try:
                #TODO: inspect profiling
                output = self.handle(
                    data.data, data.batch_size,
                    data_index=data.index, data_size=data.data_size,
                    label_content=label.data, label_size=label.data_size,
                    label_batch=label.batch_size, label_index=label.index)
            except Exception as e:
                exception = e
                self._sw_logger.exception(f"[{data.index}] data handle -> failed")
                if not self.ignore_error:
                    self._update_status(self.STATUS.FAILED)
                    raise
            else:
                exception = None
                self._sw_logger.info(f"[{data.index} data handle -> success]")
            finally:
                self._sw_logger.info(f"[{data.index} data handle -> finished]")

            try:
                self._do_record(output, data, label, exception)
            except Exception:
                self._sw_logger.exception(f"{data.index} data record")

        self._update_status(self.STATUS.SUCCESS)
        self._sw_logger.info("finish pipeline")

    def _do_record(self, output: t.Any, data: DATA_FIELD, label: DATA_FIELD, exception: t.Union[None, Exception]):
        self._status_writer.write({
            "time": datetime.now().astimezone().strftime(FMT_DATETIME),
            "status": exception is None,
            "exception": str(exception),
            "index": data.index,
            "output_size": len(output),
        })

        #TODO: output maybe cannot be jsonized
        result = {
            "index": data.index,
            "result": output,
            "batch": data.batch_size,
        }
        if self.merge_label:
            result["label"] = label.data,
        self._result_writer.write(result)

        self._update_status(self.STATUS.RUNNING)

    def _update_status(self, status: str) -> None:
        fpath = self.config.status_dir / "current"
        ensure_file(fpath, status)