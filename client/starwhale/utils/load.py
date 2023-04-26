import sys
import typing as t
import importlib
from pathlib import Path

import pkg_resources

from starwhale.utils import console
from starwhale.utils.venv import (
    guess_current_py_env,
    get_user_python_sys_paths,
    check_python_interpreter_consistency,
)


def import_object(
    workdir: t.Union[Path, str], handler_path: str, py_env: str = ""
) -> t.Any:
    workdir_path = str(Path(workdir).absolute())
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
            sys.path.insert(0, _path)
            pkg_resources.working_set.add_entry(_path)
            sys_changed = True

    try:
        module_name, handler_name = handler_path.split(":", 1)
        console.print(
            f":speaking_head: [green]import module:{module_name}, handler:{handler_name}[/]"
        )
        _module = importlib.import_module(module_name, package=workdir_path)
        _obj = getattr(_module, handler_name, None)

        if not _obj:
            raise ModuleNotFoundError(f"{handler_path}")
    except Exception:
        console.print_exception()
        if sys_changed:
            sys.path[:] = prev_paths
        raise

    return _obj


def load_module(module: str, path: Path) -> t.Any:
    workdir_path = str(path.absolute())

    external_paths = [workdir_path]
    for _path in external_paths[::-1]:
        if _path not in sys.path:
            sys.path.insert(0, _path)
            pkg_resources.working_set.add_entry(_path)

    return importlib.import_module(module, package=workdir_path)
