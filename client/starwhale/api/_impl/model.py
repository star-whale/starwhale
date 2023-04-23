from __future__ import annotations

import os
import typing as t
import inspect
from pathlib import Path

from starwhale.utils import disable_progress_bar
from starwhale.consts.env import SWEnv

_path_T = t.Union[str, Path]


def build(
    modules: t.Optional[t.List[t.Any]] = None,
    workdir: t.Optional[_path_T] = None,
    name: t.Optional[str] = None,
    project_uri: str = "",
    desc: str = "",
    remote_project_uri: t.Optional[str] = None,
) -> None:
    """Build Starwhale Model Package.

    In common case, you may call `build` function in your experiment scripts.`build` function is a shortcut for the `swcli model build` command.

    Arguments:
        modules: (List[str|object] optional) The search modules supports object(function, class or module) or str(example: "to.path.module:name").
        name: (str, optional) The name of Starwhale Model Package, default is the current work dir.
        workdir: (str, Pathlib.Path, optional) The path of the rootdir. The default workdir is the current working dir.
        desc: (str, optional) The description of the Starwhale Model Package.
        project_uri: (str, optional) The project uri of the Starwhale Model Package. If the argument is not specified,
            the project_uri is the config value of `swcli project select` command.
        remote_project_uri: (str, optional) The destination project uri(cloud://remote-instance/project/starwhale) of the Starwhale Model Package

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

    search_modules_str = []
    for h in modules or []:
        if isinstance(h, str):
            search_modules_str.append(h)
        else:
            search_modules_str.append(_ingest_obj_entrypoint_name(h, workdir))

    config = ModelConfig(name=name, run={"modules": search_modules_str}, desc=desc)

    with disable_progress_bar():
        ModelTermView.build(
            workdir=workdir,
            project=project_uri,
            model_config=config,
        )

    from starwhale import URI, URIType

    if remote_project_uri:
        remote_project_uri = URI(
            remote_project_uri, expected_type=URIType.PROJECT
        ).full_uri
    elif os.getenv(SWEnv.instance_uri) and os.getenv(SWEnv.project):
        remote_project_uri = (
            f"{os.getenv(SWEnv.instance_uri)}/project/{os.getenv(SWEnv.project)}"
        )

    if remote_project_uri:
        ModelTermView.copy(
            src_uri=f"{URI(project_uri).full_uri}/model/{name}/version/latest",
            dest_uri=remote_project_uri,
        )


def _ingest_obj_entrypoint_name(
    obj: t.Any,
    workdir: Path,
) -> str:
    source_path: t.Optional[_path_T] = inspect.getsourcefile(obj)
    if source_path is None:
        raise RuntimeError(f"failed to get source path for object: {obj}")
    source_path = Path(source_path).resolve().absolute()
    workdir = workdir.resolve().absolute()

    relative_path = str(source_path.relative_to(workdir))
    _parts = relative_path.split("/")
    module_import_path = ".".join([p.rsplit(".", 1)[0] for p in _parts if p])

    if inspect.ismodule(obj):
        return module_import_path
    else:
        return f"{module_import_path}:{obj.__qualname__}"
