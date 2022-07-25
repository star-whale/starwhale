import sys
import importlib
from pathlib import Path

import pkg_resources
from loguru import logger


def load_module(module: str, path: str):
    """
    load module from path
    :param module: module name
    :param path: abs path
    :return: module
    """
    workdir = Path(path)
    workdir_path = str(workdir.absolute())
    # add module path to sys path
    external_paths = [workdir_path]
    for _path in external_paths[::-1]:
        if _path not in sys.path:
            logger.debug(f"insert sys.path: '{_path}'")
            sys.path.insert(0, _path)
            pkg_resources.working_set.add_entry(_path)

    return importlib.import_module(module, package=workdir_path)
