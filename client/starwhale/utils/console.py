from __future__ import annotations

import typing as t
import logging
import threading

from rich.console import Console

from . import now

rich_console = Console(soft_wrap=True)

_min_level = logging.ERROR
_min_level_lock = threading.Lock()

CRITICAL = logging.CRITICAL
FATAL = CRITICAL
ERROR = logging.ERROR
WARNING = logging.WARNING
WARN = WARNING
INFO = logging.INFO
DEBUG = logging.DEBUG
TRACE = 5


def set_level(level: int) -> None:
    global _min_level
    with _min_level_lock:
        if level not in _levels:
            raise ValueError(f"invalid log level: {level}")
        _min_level = level


class _LevelInfo(t.NamedTuple):
    number: int
    name: str
    color: str
    emoji: str
    style: str


_levels: t.Dict[int, _LevelInfo] = {
    TRACE: _LevelInfo(TRACE, "TRACE", "white", ":bird:", "default"),
    DEBUG: _LevelInfo(DEBUG, "DEBUG", "blue", ":speaker:", "default"),
    INFO: _LevelInfo(INFO, "INFO", "green", ":bulb:", "default"),
    WARN: _LevelInfo(WARN, "WARN", "yellow", ":question:", "bold magenta"),
    ERROR: _LevelInfo(ERROR, "ERROR", "red", ":x:", "bold red"),
    FATAL: _LevelInfo(
        FATAL,
        "FATAL",
        "bold red",
        ":x:",
        "red on white",
    ),
}


def get_level_name(level: int) -> str:
    if level not in _levels:
        return f"LEVEL {level}"
    else:
        return _levels[level].name


def print(*args: t.Any, **kwargs: t.Any) -> None:
    extend_args = list(args or [])
    global _min_level
    without_timestamp = kwargs.pop("without_timestamp", False)
    if _min_level <= logging.DEBUG and not without_timestamp:
        extend_args.insert(0, f"[{_get_datetime_str()}]")

    rich_console.print(*extend_args, **kwargs)


def print_exception(*args: t.Any, **kwargs: t.Any) -> None:
    rich_console.print_exception(*args, **kwargs)


def trace(*args: t.Any) -> None:
    _log(TRACE, *args)


def debug(*args: t.Any) -> None:
    _log(logging.DEBUG, *args)


def info(*args: t.Any) -> None:
    _log(logging.INFO, *args)


def warn(*args: t.Any) -> None:
    _log(logging.WARN, *args)


warning = warn


def error(*args: t.Any) -> None:
    _log(logging.ERROR, *args)


def exception(*args: t.Any) -> None:
    _log(logging.ERROR, *args)
    print_exception()


def fatal(*args: t.Any) -> None:
    _log(logging.FATAL, *args)


def log(*args: t.Any) -> None:
    _log(logging.INFO, *args)


def _log(level: int, *args: t.Any) -> None:
    level_info = _levels.get(level) or _levels[logging.INFO]

    global _min_level
    if level_info.number < _min_level:
        return

    datetime_str = _get_datetime_str()
    rich_console.print(
        f"[{datetime_str}] {level_info.emoji} [{level_info.color}]|{level_info.name}|[/]",
        *args,
        style=level_info.style,
    )


def rule(*args: t.Any, **kwargs: t.Any) -> None:
    rich_console.rule(*args, **kwargs)


def _get_datetime_str() -> str:
    global _min_level
    if _min_level <= logging.DEBUG:
        return now("%Y-%m-%d %H:%M:%S.%f")
    else:
        return now("%H:%M:%S")
