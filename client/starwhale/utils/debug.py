import os
import sys
import logging

from loguru import logger
from rich import traceback

from starwhale.consts import ENV_DEBUG_MODE


def set_debug_mode(is_debug: bool) -> None:
    if is_debug:
        logger.debug("set debug mode.")

    #TODO: custom debug for tb install
    traceback.install(show_locals=True, max_frames=1, width=200)

    os.environ[ENV_DEBUG_MODE] = str(is_debug)
    # TODO: tune logging basic config
    lvl = logging.DEBUG if is_debug else logging.INFO
    fmt = "<green>{time}</green> <level>{message}</level>" if is_debug else ""
    logging.basicConfig(level=lvl, format=fmt)


def get_debug_mode() -> bool:
	return os.environ.get(ENV_DEBUG_MODE, "") == "true"