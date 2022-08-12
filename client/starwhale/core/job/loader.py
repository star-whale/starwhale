import sys
import importlib
from pathlib import Path
from typing import Any

import pkg_resources
from loguru import logger


def load_module(module: str, path: Path) -> Any:
    """
    load module from path
    :param module: module name
    :param path: abs path
    :return: module
    """
    workdir_path = str(path.absolute())
    # add module path to sys path
    external_paths = [workdir_path]
    for _path in external_paths[::-1]:
        if _path not in sys.path:
            logger.debug(f"insert sys.path: '{_path}'")
            sys.path.insert(0, _path)
            pkg_resources.working_set.add_entry(_path)

    return importlib.import_module(module, package=workdir_path)


def load_cls(module: str, cls: str) -> Any:
    _cls = getattr(module, cls, None)
    if not _cls:
        raise RuntimeError(f"can't find class:{cls} from module:{module}")
    return _cls


def get_func_from_instance(obj: Any, func: str) -> Any:
    _func = getattr(obj, func, None)
    if not _func:
        raise RuntimeError(f"can't find function:{func} from instance:{obj}")
    return _func


def get_func_from_module(module: str, func: str) -> Any:
    _func = getattr(module, func, None)
    if not _func:
        raise RuntimeError(f"can't find function:{func} from module:{module}")
    return _func
