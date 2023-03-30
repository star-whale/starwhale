from __future__ import annotations

import os
import typing as t
import inspect
import tempfile
from pathlib import Path

import yaml

from starwhale.utils import disable_progress_bar
from starwhale.consts import DefaultYAMLName
from starwhale.utils.fs import ensure_file

_path_T = t.Union[str, Path]


def build(
    evaluation_handler: t.Any,
    workdir: t.Optional[_path_T] = None,
    name: t.Optional[str] = None,
    project_uri: str = "",
    desc: str = "",
    push_to: t.Optional[str] = None,
) -> None:
    """Build Starwhale Model Package.

    In common case, you may call `build` function in your experiment scripts.`build` function is a shortcut for the `swcli model build` command.

    Arguments:
        evaluation_handler: (str, object, required) The evaluation handler supports object(function, class or module) or str(example: "to.path.module:name").
        name: (str, optional) The name of Starwhale Model Package, default is the current work dir.
        workdir: (str, Pathlib.Path, optional) The path of the rootdir. The default workdir is the current working dir.
        desc: (str, optional) The description of the Starwhale Model Package.
        project_uri: (str, optional) The project uri of the Starwhale Model Package. If the argument is not specified,
            the project_uri is the config value of `swcli project select` command.
        push_to: (str, optional) The destination project uri of the Starwhale Model Package

    Examples:
    ```python
    from starwhale import model

    # class handler
    from .user.code.evaluator import ExamplePipelineHandler
    model.build(ExamplePipelineHandler)

    # function handler
    from .user.code.evaluator import predict_image
    model.build(predict_image)

    # module handler, @step decorates function in this module
    from .user.code import evaluator
    model.build(evaluator)

    # str handler
    model.build("user.code.evaluator:ExamplePipelineHandler")
    model.build("user.code.evaluator:predict_image")
    model.build("user.code.evaluator")
    ```

    Returns:
        None.
    """
    from starwhale.core.model.view import ModelTermView

    if workdir is None:
        workdir = Path.cwd()
    else:
        workdir = Path(workdir)

    name = name or workdir.name

    yaml_path = _render_model_yaml(
        handler=evaluation_handler,
        workdir=workdir,
        name=name,
        desc=desc,
    )
    try:
        with disable_progress_bar():
            ModelTermView.build(
                workdir=workdir,
                project=project_uri,
                yaml_path=yaml_path,
            )
            if push_to:
                from starwhale import URI
                # model copy local/project/myproject/model/mnist/version/latest cloud://server/project/starwhale
                ModelTermView.copy(f"{URI(project_uri).full_uri}/model/{name}/version/latest", push_to)
    finally:
        os.unlink(yaml_path)

    if push_to:
        print("copy start1")
        from starwhale import URI
        p = URI(project_uri)
        print(f"copy start2:{p.full_uri}")
        # local/project/myproject/model/mnist-local/version/latest cloud://pre-k8s/project/starwhale
        ModelTermView.copy(f"{p.full_uri}/model/{name}/version/latest", push_to)
        print("copy end")


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


# TODO: remove _render_model_yaml function when the model.yaml is optional for model build
def _render_model_yaml(
    handler: t.Any,
    workdir: Path,
    name: str,
    desc: str,
) -> str:
    if not isinstance(handler, str):
        handler = _ingest_obj_entrypoint_name(handler, workdir)

    config = {
        "name": name,
        "run": {
            "handler": handler,
        },
        "desc": desc,
    }

    _, yaml_fpath = tempfile.mkstemp(suffix=DefaultYAMLName.MODEL)
    ensure_file(yaml_fpath, yaml.safe_dump(config, default_flow_style=False))
    return yaml_fpath
