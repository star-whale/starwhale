import sys
import typing as t
import importlib
from pathlib import Path

import pkg_resources
from loguru import logger

from starwhale.utils import console
from starwhale.utils.venv import (
    guess_current_py_env,
    get_user_python_sys_paths,
    check_python_interpreter_consistency,
)


def import_object(workdir: Path, handler_path: str, py_env: str = "") -> t.Any:
    workdir_path = str(workdir.absolute())
    external_paths = [workdir_path]
    py_env = py_env or guess_current_py_env()
    _ok, _cur_py, _ex_py = check_python_interpreter_consistency(py_env)
    if not _ok:
        console.print(
            f":speaking_head: [red]swcli python prefix:{_cur_py}, runtime env python prefix:{_ex_py}[/], swcli will inject sys.path"
        )
        external_paths.extend(get_user_python_sys_paths(py_env))

    prev_paths = sys.path[:]
    sys_changed = False

    for _path in external_paths[::-1]:
        if _path not in sys.path:
            logger.debug(f"insert sys.path: '{_path}'")
            sys.path.insert(0, _path)
            pkg_resources.working_set.add_entry(_path)
            sys_changed = True

    try:
        module_name, handler_name = handler_path.split(":", 1)
        logger.debug(f"import module:{module_name}, handler:{handler_name}")
        _module = importlib.import_module(module_name, package=workdir_path)
        _obj = getattr(_module, handler_name, None)

        if not _obj:
            raise ModuleNotFoundError(f"{handler_path}")
    except Exception as e:
        logger.exception(e)
        if sys_changed:
            sys.path[:] = prev_paths
        raise

    return _obj


def load_module(module: str, path: Path) -> t.Any:
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


def load_cls(module: str, cls_str: str) -> t.Any:
    _cls = getattr(module, cls_str, None)
    if not _cls:
        raise ModuleNotFoundError(f"class:{cls_str} from module:{module}")
    return _cls


def get_func_from_object(obj: t.Any, func_str: str) -> t.Any:
    _func = getattr(obj, func_str, None)
    if not _func:
        raise ModuleNotFoundError(f"function:{func_str} from object:{obj}")
    return _func


def get_func_from_module(module: str, func_str: str) -> t.Any:
    _func = getattr(module, func_str, None)
    if not _func:
        raise ModuleNotFoundError(f"function:{func_str} from module:{module}")
    return _func
