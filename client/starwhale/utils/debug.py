import os

from rich import traceback

from starwhale.utils import console
from starwhale.consts import (
    ENV_LOG_LEVEL,
    ENV_LOG_VERBOSE_COUNT,
    ENV_DISABLE_PROGRESS_BAR,
)


def init_logger(verbose: int) -> None:
    if verbose == 0:
        lvl = console.ERROR
    elif verbose == 1:
        lvl = console.WARNING
    elif verbose == 2:
        lvl = console.INFO
    elif verbose == 3:
        lvl = console.DEBUG
    else:
        lvl = console.TRACE

    console.set_level(lvl)
    lvl_name = console.get_level_name(lvl)
    os.environ[ENV_LOG_LEVEL] = lvl_name
    os.environ[ENV_LOG_VERBOSE_COUNT] = str(verbose)

    if verbose > 0:
        os.environ[ENV_DISABLE_PROGRESS_BAR] = "1"
        console.print(f":space_invader: verbosity: {verbose}, log level: {lvl_name}")

    # TODO: custom debug for tb install
    traceback.install(
        console=console.rich_console, show_locals=True, max_frames=1, width=200
    )
