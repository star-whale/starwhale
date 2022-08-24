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


def import_cls(
    workdir: Path, mc: str, parentClass: t.Any = object, py_env: str = ""
) -> t.Any:
    logger.debug("enter import cls func...")
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
        module_name, cls_name = mc.split(":", 1)
        logger.debug(f"import module:{module_name}, cls:{cls_name}")
        _module = importlib.import_module(module_name, package=workdir_path)
        _cls = getattr(_module, cls_name, None)
        if not _cls or not issubclass(_cls, parentClass):
            raise Exception(f"{mc} is not subclass of {parentClass}")
    except Exception as e:
        logger.exception(e)
        if sys_changed:
            sys.path[:] = prev_paths
        raise

    return _cls


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


def load_cls(module: str, cls: str) -> t.Any:
    _cls = getattr(module, cls, None)
    if not _cls:
        raise RuntimeError(f"can't find class:{cls} from module:{module}")
    return _cls


def get_func_from_object(obj: t.Any, func: str) -> t.Any:
    _func = getattr(obj, func, None)
    if not _func:
        raise RuntimeError(f"can't find function:{func} from instance:{obj}")
    return _func


def get_func_from_module(module: str, func: str) -> t.Any:
    _func = getattr(module, func, None)
    if not _func:
        raise RuntimeError(f"can't find function:{func} from module:{module}")
    return _func
