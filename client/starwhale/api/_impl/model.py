import os
import sys
import typing as t
from abc import ABCMeta, abstractmethod
from collections import namedtuple
from pathlib import Path
import json
import logging

import loguru

from starwhale.utils.log import StreamWrapper
from starwhale.utils.error import NotFoundError
from starwhale.utils.fs import ensure_dir
from starwhale.utils import pretty_bytes

_TASK_ROOT_DIR = "/var/starwhale"

_p = lambda p, sub: Path(p) if p else Path(_TASK_ROOT_DIR) / sub
_ptype = t.Union[str, None, Path]

_LOG_TYPE = namedtuple("LOG_TYPE", ["SW", "USER"])(
    "starwhale", "user"
)

class RunConfig(object):

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
    def create_by_env(cls) -> "RunConfig":
        _env = os.environ.get
        return RunConfig(
            swds_config_path=_env("SW_TASK_SWDS_CONFIG"),
            status_dir=_env("SW_TASK_STATUS_DIR"),
            log_dir=_env("SW_TASK_LOG_DIR"),
            result_dir=_env("SW_TASK_RESULT_DIR"),
        )


class PipelineHandler(object):
    RESULT_OUTPUT_TYPE = namedtuple("OUTPUT_TYPE", ["JSONL", "PLAIN"])("jsonline", "plain")
    STATUS = namedtuple("STATUS", ["START", "RUNNING", "OK", "FAILED"])("start", "running", "ok", "failed")

    __metaclass__ = ABCMeta

    def __init__(self, merge_label: bool=False,
                 output_type: str= RESULT_OUTPUT_TYPE.JSONL,
                 ignore_error: bool=False) -> None:
        #TODO: add args for compare result and label directly
        self.merge_label = merge_label
        self.output_type = output_type
        self.ignore_error = ignore_error

        self.logger, self._sw_logger = self._init_logger()
        self.config = RunConfig.create_by_env()
        self._orig_stdout = sys.stdout
        self._orig_stderr = sys.stderr

        #TODO: find some elegant call method
        self._monkey_patch()

    def _init_logger(self) -> t.Tuple[loguru.Logger, loguru.Logger]:
        #TODO: remove logger first?
        #TODO: add custom log format, include daemonset pod name
        from loguru import logger as _logger

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

        self.logger.remove()
        self._sw_logger.remove()

    @abstractmethod
    def handle(self, data: bytes, batch_size: int, **kw) -> t.Any:
        raise NotImplementedError

    def starwhale_internal_run(self) -> None:
        #TODO: forbid inherit object override this method
        self._sw_logger.info("start to run pipeline...")

        for data, label in self._iter_dataset():
            d_idx, d_content, d_size, d_batch = data
            l_idx, l_content, l_size, l_batch = label

            self._sw_logger.info(f"[{d_idx}] data loaded, size:{pretty_bytes(d_size)}, batch:{d_batch}")
            output = ""
            try:
                #TODO: inspect profiling
                output = self.handle(d_content, d_batch,
                                     label_content=l_content, label_size=l_size,
                                     label_batch=l_batch, data_index=d_idx, label_index=l_idx)
            except Exception:
                self._sw_logger.exception(f"[{d_idx}] data handle -> failed")
                if not self.ignore_error:
                    raise
            else:
                #TODO: add more info into success
                self._sw_logger.info(f"[{d_idx} data handle -> success]")
            finally:
                self._sw_logger.info(f"[{d_idx} data handle -> finished]")

            try:
                self._do_record(output, data, label)
            except Exception:
                self._sw_logger.exception(f"{d_idx} data record")

        self._sw_logger.info("finish pipeline")

    def _do_record(self, output, data, label):
        #TODO: add status json line
        pass

    def _iter_dataset(self) -> t.Tuple[t.Any, t.Any]:
        yield None, None