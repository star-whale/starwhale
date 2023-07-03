import os

from rich import traceback

from starwhale.utils import console
from starwhale.consts import (
    ENV_LOG_LEVEL,
    ENV_LOG_VERBOSE_COUNT,
    ENV_DISABLE_PROGRESS_BAR,
)


def init_logger(verbose: int = 0) -> None:
    """Initialize Starwhale logger and traceback.

    Arguments:
        verbose: (int, optional) verbosity level. Defaults to 0.
          - 0: show only errors, traceback only shows 1 frame.
          - 1: show errors + warnings, traceback shows 5 frames.
          - 2: show errors + warnings + info, traceback shows 10 frames.
          - 3: show errors + warnings + info + debug, traceback shows 100 frames.
          - >=4: show errors + warnings + info + debug + trace, traceback shows 1000 frames.

    Returns: None
    """
    verbose_env = os.environ.get(ENV_LOG_VERBOSE_COUNT)
    if verbose_env is not None:
        verbose = int(verbose_env)

    show_locals = False
    if verbose == 0:
        lvl = console.ERROR
        max_frames = 1
    elif verbose == 1:
        lvl = console.WARNING
        max_frames = 5
    elif verbose == 2:
        lvl = console.INFO
        max_frames = 10
    elif verbose == 3:
        lvl = console.DEBUG
        max_frames = 100
    elif verbose >= 4:
        lvl = console.TRACE
        max_frames = 1000
        show_locals = True
    else:
        raise ValueError(f"Invalid verbose level: {verbose}")

    console.set_level(lvl)
    lvl_name = console.get_level_name(lvl)
    os.environ[ENV_LOG_LEVEL] = lvl_name

    if verbose > 0:
        os.environ[ENV_DISABLE_PROGRESS_BAR] = "1"
        console.print(f":space_invader: verbosity: {verbose}, log level: {lvl_name}")

    # TODO: custom debug for tb install
    traceback.install(
        console=console.rich_console,
        show_locals=show_locals,
        max_frames=max_frames,
        width=200,
    )
