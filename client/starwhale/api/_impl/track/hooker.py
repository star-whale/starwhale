from __future__ import annotations

import io
import sys
import typing as t
import logging
from pathlib import Path

import loguru

from starwhale.utils.fs import ensure_dir
from starwhale.utils.log import StreamWrapper

from .base import _TrackSource


def hook_frameworks() -> None:
    ...


def hook_python_libs() -> None:
    ...


class ConsoleLogHook:
    def __init__(self, log_dir: Path, track_id: str, rotation: str = "100 MB") -> None:
        self.log_dir = log_dir

        self._stdout_changed: bool = False
        self._stderr_changed: bool = False
        self._original_stdout: t.Optional[io.TextIOWrapper] = None
        self._original_stderr: t.Optional[io.TextIOWrapper] = None

        self._rotation = rotation
        self._track_id = track_id
        self._logger: t.Optional[loguru.Logger] = None
        self._log_handler_id: t.Optional[int] = None

    def _setup_logger(self) -> loguru.Logger:
        ensure_dir(self.log_dir)
        loguru.logger.remove()
        self._log_handler_id = loguru.logger.add(
            self.log_dir / "console-{time}.log",
            rotation=self._rotation,
            backtrace=True,
            diagnose=True,
            serialize=True,
        )
        _logger = loguru.logger.bind(
            source=_TrackSource.USER.value,
            track_id=self._track_id,
        )
        return _logger

    def install(self) -> None:
        if self._logger is None:
            self._logger = self._setup_logger()

        if not isinstance(sys.stdout, StreamWrapper) and isinstance(
            sys.stdout, io.TextIOWrapper
        ):
            self._original_stdout = sys.stdout
            sys.stdout = StreamWrapper(sys.stdout, self._logger, logging.INFO)  # type: ignore
            self._stdout_changed = True

        if not isinstance(sys.stderr, StreamWrapper) and isinstance(
            sys.stderr, io.TextIOWrapper
        ):
            self._original_stderr = sys.stderr
            sys.stderr = StreamWrapper(sys.stderr, self._logger, logging.WARN)  # type: ignore
            self._stderr_changed = True

        # TODO: redirect python standard logger

    def uninstall(self) -> None:
        if self._log_handler_id is not None and self._logger is not None:
            self._logger.remove(self._log_handler_id)
            self._logger = None
            self._log_handler_id = None

        if self._stdout_changed and self._original_stdout is not None:
            sys.stdout = self._original_stdout
            self._stdout_changed = False
            self._original_stdout = None

        if self._stderr_changed and self._original_stderr is not None:
            sys.stderr = self._original_stderr
            self._stderr_changed = False
            self._original_stderr = None
