import typing as t
import logging

import loguru


class StreamWrapper(object):
    is_wrapper = True

    def __init__(self, stream: t.TextIO, logger: loguru.Logger, level: int=logging.INFO) -> None:
        self.stream = stream
        self.logger = logger
        self.level = level

    def write(self, s):
        s = str(s)
        self.stream.write(s)
        self.logger.log(self.level, s.rstrip())

    def __getatt__(self, name: str) -> t.Any:
        return getattr(self.stream, name)