from __future__ import annotations

import typing as t
import logging

import loguru


class StreamWrapper:
    is_wrapper = True

    def __init__(
        self,
        stream: t.TextIO,
        logger: loguru.Logger,
        level: int = logging.INFO,
    ) -> None:
        self.stream = stream
        self.logger = logger
        self.level = level

    def write(self, sb: bytes) -> None:
        s = str(sb)
        self.stream.write(s)
        # TODO: splitlines?
        self.logger.opt(depth=1).log(self.level, s.rstrip())

    def __getattr__(self, name: str) -> t.Any:
        return getattr(self.stream, name)

    def flush(self) -> None:
        ...
