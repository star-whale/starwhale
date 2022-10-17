import os
import sys
import logging

from rich import traceback
from loguru import logger

from starwhale.consts import ENV_LOG_LEVEL, ENV_LOG_VERBOSE_COUNT


def init_logger(verbose: int) -> None:
    fmt = "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>"
    if verbose <= 0:
        lvl = logging.WARNING
    elif verbose == 1:
        lvl = logging.INFO
    else:
        lvl = logging.DEBUG

    lvl_name = logging.getLevelName(lvl)
    os.environ[ENV_LOG_LEVEL] = lvl_name
    os.environ[ENV_LOG_VERBOSE_COUNT] = str(verbose)

    # TODO: custom debug for tb install
    traceback.install(show_locals=True, max_frames=1, width=200)

    logger.remove()
    logger.add(
        sys.stderr,
        level=lvl_name,
        colorize=True,
        backtrace=True,
        diagnose=True,
        catch=True,
        format=fmt,
    )
    logger.debug(f"verbosity: {verbose}, log level: {lvl_name}")
