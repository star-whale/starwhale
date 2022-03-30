from loguru import logger


class FileTypeError(Exception):

    def __str__(self) -> str:
        return "file type error"


class FileFormatError(Exception):

    def __str__(self) -> str:
        return "file format error"


class NoSupportError(Exception):

    def __str__(self) -> str:
        return "no support"


class NotFoundError(Exception):

    def __str__(self) -> str:
        return "path no found"