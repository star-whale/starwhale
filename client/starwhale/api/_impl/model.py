from __future__ import annotations

import os
import typing as t
import inspect
import threading
from pathlib import Path

from starwhale.utils import console, disable_progress_bar
from starwhale.utils.fs import blake2b_content
from starwhale.consts.env import SWEnv

from .job import Handler
from ...base.uri.project import Project

_path_T = t.Union[str, Path]
_called_build_functions: t.Dict[str, bool] = {}
_called_build_lock = threading.Lock()


def build(
    modules: t.Optional[t.List[t.Any]] = None,
    workdir: t.Optional[_path_T] = None,
    name: t.Optional[str] = None,
    project_uri: str = "",
    desc: str = "",
    remote_project_uri: t.Optional[str] = None,
    add_all: bool = False,
) -> None:
    """Build Starwhale Model Package.

    In common case, you may call `build` function in your experiment scripts.`build` function is a shortcut for the `swcli model build` command.
    Build function will search all handlers from the `modules` argument or imported modules, and then build Starwhale Model Package.

    Arguments:
        modules: (List[str|object] optional) The search modules supports object(function, class or module) or str(example: "to.path.module", "to.path.module:object").
            If the argument is not specified, the search modules are the imported modules.
        name: (str, optional) The name of Starwhale Model Package, default is the current work dir.
        workdir: (str, Pathlib.Path, optional) The path of the rootdir. The default workdir is the current working dir.
            All files in the workdir will be packaged. If you want to ignore some files, you can add `.swignore` file in the workdir.
        desc: (str, optional) The description of the Starwhale Model Package.
        project_uri: (str, optional) The project uri of the Starwhale Model Package. If the argument is not specified,
            the project_uri is the config value of `swcli project select` command.
        remote_project_uri: (str, optional) The destination project uri(cloud://remote-instance/project/starwhale) of the Starwhale Model Package
        add_all: (bool, optional) Add all files in the workdir to the Starwhale Model Package. If the argument is False, the python cache files and virtualenv files will be ignored.
            the ".swignore" file in the workdir will always take effect.

    Examples:
    ```python
    from starwhale import model

    # class search handlers
    from .user.code.evaluator import ExamplePipelineHandler
    model.build([ExamplePipelineHandler])

    # function search handlers
    from .user.code.evaluator import predict_image
    model.build([predict_image])

    # module handlers, @handler decorates function in this module
    from .user.code import evaluator
    model.build([evaluator])

    # str search handlers
    model.build(["user.code.evaluator:ExamplePipelineHandler"])
    model.build(["user.code1", "user.code2"])

    # no search handlers, use imported modules
    model.build()
    ```

    Returns:
        None.
    """
    from starwhale.core.model.view import ModelTermView
    from starwhale.core.model.model import ModelConfig

    if workdir is None:
        workdir = Path.cwd()
    else:
        workdir = Path(workdir)

    name = name or workdir.name

    search_modules_str = set()
    for h in modules or []:
        if isinstance(h, str):
            search_modules_str.add(h)
        else:
            search_modules_str.add(_ingest_obj_entrypoint_name(h, workdir))

    if not search_modules_str:
        for f in Handler._registered_functions.values():
            search_modules_str.add(_ingest_obj_entrypoint_name(f, workdir))

    if not search_modules_str:
        raise RuntimeError("no modules to search, please specify modules")

    global _called_build_functions, _called_build_lock
    with _called_build_lock:
        arguments = f"{search_modules_str}-{workdir}-{name}-{project_uri}-{desc}-{remote_project_uri}"
        key = blake2b_content(arguments.encode())
        if key in _called_build_functions:
            console.print(
                f":point_right: [bold red]cycle call model build function with the same arguments({arguments}), skip repetitive build"
            )
            return
        else:
            _called_build_functions[key] = True

    with disable_progress_bar():
        Handler.clear_registered_handlers()
        ModelTermView.build(
            workdir=workdir,
            project=project_uri,
            model_config=ModelConfig(
                name=name, run={"modules": list(search_modules_str)}, desc=desc
            ),
            add_all=add_all,
        )

    if remote_project_uri:
        remote_project_uri = Project(remote_project_uri).full_uri
    # TODO support instance and project env in uri component
    elif os.getenv(SWEnv.instance_uri) and os.getenv(SWEnv.project):
        remote_project_uri = (
            f"{os.getenv(SWEnv.instance_uri)}/project/{os.getenv(SWEnv.project)}"
        )

    if remote_project_uri:
        ModelTermView.copy(
            src_uri=f"{Project(project_uri).full_uri}/model/{name}/version/latest",
            dest_uri=remote_project_uri,
        )


def _ingest_obj_entrypoint_name(obj: t.Any, workdir: Path) -> str:
    obj = inspect.unwrap(obj)
    source_path: t.Optional[_path_T] = inspect.getsourcefile(obj)
    if source_path is None:
        raise RuntimeError(f"failed to get source path for object: {obj}")
    source_path = Path(source_path).resolve().absolute()
    workdir = workdir.resolve().absolute()

    relative_path = str(source_path.relative_to(workdir))
    _parts = relative_path.split("/")
    module_import_path = ".".join([p.rsplit(".", 1)[0] for p in _parts if p])
    return module_import_path
