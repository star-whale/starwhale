from __future__ import annotations

import os
import typing as t
from pathlib import Path

import click
from click_option_group import optgroup, RequiredMutuallyExclusiveOptionGroup

from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.utils.cli import AliasedGroup
from starwhale.consts.env import SWEnv
from starwhale.utils.error import NoSupportError
from starwhale.core.model.view import get_term_view, ModelTermView
from starwhale.base.bundle_copy import BundleCopy
from starwhale.base.uri.project import Project
from starwhale.core.model.model import ModelConfig, ModelInfoFilter
from starwhale.core.model.store import ModelStorage
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.runtime.process import Process


@click.group(
    "model",
    cls=AliasedGroup,
    help="Model management, build/copy/run/list...",
)
@click.pass_context
def model_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@model_cmd.command("build")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option(
    "-p",
    "--project",
    default="",
    help="Project URI, default is the current selected project. The model package will store in the specified project.",
)
@click.option(
    "-f",
    "--model-yaml",
    default=None,
    help="model yaml path, default use ${workdir}/model.yaml file",
)
@click.option(
    "-r",
    "--runtime",
    default="",
    help="runtime uri, build model in the runtime environment process",
)
@click.option(
    "modules",
    "-m",
    "--module",
    multiple=True,
    help="Python modules to be imported during the build process. The format is python module import path. The option supports set multiple times.",
)
@click.option("-n", "--name", default="", help="model name")
@click.option("-d", "--desc", default="", help="model description")
@click.option(
    "tags",
    "-t",
    "--tag",
    multiple=True,
    help="model tags, the option can be used multiple times. `latest` and `^v\d+$` tags are reserved tags.",
)
@click.option(
    "--package-runtime/--no-package-runtime",
    is_flag=True,
    default=True,
    show_default=True,
    help="When using the `--runtime` parameter, by default, the corresponding Starwhale runtime will become the built-in runtime for the Starwhale model. "
    "This feature can be disabled with the `--no-package-runtime` parameter.",
)
@click.option(
    "--add-all",
    is_flag=True,
    default=False,
    help="Add all files in the working directory to the model package"
    "(excludes python cache files and virtual environment files when disabled)."
    "The '.swignore' file still takes effect.",
)
def _build(
    workdir: str,
    project: str,
    model_yaml: str,
    runtime: str,
    modules: t.List[str],
    tags: t.List[str],
    package_runtime: bool,
    name: str,
    desc: str,
    add_all: bool,
) -> None:
    """Build starwhale model package.
    Only standalone instance supports model build.

    WORKDIR: model source code directory[Required].

    Example:

        \b
        # build by the model.yaml in current directory and model package will package all the files from the current directory.
        swcli model build .
        # search model run decorators from mnist.evaluate, mnist.train and mnist.predict modules, then package all the files from the current directory to model package.
        swcli model build . --module mnist.evaluate --module mnist.train --module mnist.predict
        # build model package in the Starwhale Runtime environment.
        swcli model build . --module mnist.evaluate --runtime pytorch/version/v1
        # forbid to package Starwhale Runtime into the model.
        swcli model build . --module mnist.evaluate --runtime pytorch/version/v1 --no-package-runtime
        # build model package with tags.
        swcli model build . --tag tag1 --tag tag2
    """
    if model_yaml is None:
        yaml_path = Path(workdir) / DefaultYAMLName.MODEL
    else:
        yaml_path = Path(model_yaml)

    if yaml_path.exists():
        config = ModelConfig.create_by_yaml(yaml_path)
    else:
        config = ModelConfig()

    config.name = name or config.name or Path(workdir).name
    config.run.modules = modules or config.run.modules
    config.desc = desc
    config.do_validate()

    ModelTermView.build(
        workdir=workdir,
        project=project,
        model_config=config,
        runtime_uri=runtime,
        package_runtime=package_runtime,
        add_all=add_all,
        tags=tags,
    )


@model_cmd.command("extract")
@click.argument("model")
@click.argument("target_dir", default=".")
@click.option("-f", "--force", is_flag=True, help="Force to extract model package")
def _extract(model: str, target_dir: str, force: bool) -> None:
    """Extract model package to target directory.

    MODEL: model uri with version.

    TARGET_DIR: target directory to extract model package.

    Example:

        \b
        - extract mnist model package to current directory
            swcli model extract mnist/version/xxxx .
        \b
        - extract mnist model package to current directory and force to overwrite the files
            swcli model extract mnist/version/xxxx . -f
    """
    ModelTermView(model).extract(target=Path(target_dir), force=force)


@model_cmd.command("tag")
@click.argument("model")
@click.argument("tags", nargs=-1)
@click.option("-r", "--remove", is_flag=True, help="Remove tags")
@click.option(
    "-q",
    "--quiet",
    is_flag=True,
    help="Ignore tag name errors like name duplication, name absence",
)
@click.option(
    "-f",
    "--force-add",
    is_flag=True,
    help="force to add tags, even the tag has been used for another version",
)
def _tag(
    model: str, tags: t.List[str], remove: bool, quiet: bool, force_add: bool
) -> None:
    """Model tag management: add, remove and list

    MODEL: argument use the `Model URI` format.

    Examples:

        \b
        - list tags of the mnist model
        swcli model tag mnist

        \b
        - add tags for the mnist model
        swcli model tag mnist t1 t2
        swcli model tag cloud://cloud.starwhale.cn/project/public:starwhale/model/mnist/version/latest t1 --force-add
        swcli model tag mnist t1 --quiet

        \b
        - remove tags for the mnist model
        swcli model tag mnist -r t1 t2
        swcli model tag cloud://cloud.starwhale.cn/project/public:starwhale/model/mnist --remove t1
    """
    ModelTermView(model).tag(
        tags=tags, remove=remove, ignore_errors=quiet, force_add=force_add
    )


@model_cmd.command("copy", aliases=["cp"])
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force to copy model")
@click.option("-dlp", "--dest-local-project", help="dest local project uri")
@click.option(
    "ignore_tags",
    "-i",
    "--ignore-tag",
    multiple=True,
    help="ignore tags to copy. The option can be used multiple times.",
)
def _copy(
    src: str, dest: str, force: bool, dest_local_project: str, ignore_tags: t.List[str]
) -> None:
    """
    Copy Model between Standalone Instance and Cloud Instance

    SRC: model uri with version

    DEST: project uri or model uri with name.

    In default, copy dataset with all user custom tags. If you want to ignore some tags, you can use `--ignore-tag` option.
    `latest` and `^v\d+$` are the system builtin tags, they are ignored automatically.

    When the tags are already used for the other model version in the dest instance, you should use `--force` option or adjust the tags.

    Example:

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud model to local project(myproject) with a new model name 'mnist-local'
            swcli model cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq local/project/myproject/mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud model to local default project(self) with the cloud instance model name 'mnist-cloud'
            swcli model cp cloud://pre-k8s/project/model/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq .

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud model to local project(myproject) with the cloud instance model name 'mnist-cloud'
            swcli model cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq . -dlp myproject

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud model to local default project(self) with a model name 'mnist-local'
            swcli model cp cloud://pre-k8s/project/model/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud model to local project(myproject) with a model name 'mnist-local'
            swcli model cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local -dlp myproject

        \b
        - copy standalone instance(local) default project(self)'s mnist-local model to cloud instance(pre-k8s) mnist project with a new model name 'mnist-cloud'
            swcli model cp mnist-local/version/latest cloud://pre-k8s/project/mnist/mnist-cloud

        \b
        - copy standalone instance(local) default project(self)'s mnist-local model to cloud instance(pre-k8s) mnist project with standalone instance model name 'mnist-local'
            swcli model cp mnist-local/version/latest cloud://pre-k8s/project/mnist

        \b
        - copy standalone instance(local) default project(self)'s mnist-local model to cloud instance(pre-k8s) mnist project without 'cloud://' prefix
            swcli model cp mnist-local/version/latest pre-k8s/project/mnist

        \b
        - copy standalone instance(local) project(myproject)'s mnist-local model to cloud instance(pre-k8s) mnist project with standalone instance model name 'mnist-local'
            swcli model cp local/project/myproject/model/mnist-local/version/latest cloud://pre-k8s/project/mnist

        \b
        - copy without some tags
           swcli model cp mnist cloud://cloud.starwhale.cn/project/starwhale:public --ignore-tag t1
    """
    ModelTermView.copy(src, dest, force, dest_local_project, ignore_tags)


@model_cmd.command("info")
@click.argument("model")
@click.option(
    "-of",
    "--output-filter",
    type=click.Choice([f.value for f in ModelInfoFilter], case_sensitive=False),
    default=ModelInfoFilter.basic.value,
    show_default=True,
    help="Filter the output content. Only standalone instance supports this option.",
)
@click.pass_obj
def _info(view: t.Type[ModelTermView], model: str, output_filter: str) -> None:
    """Show model details.

    MODEL: argument use the `Model URI` format. Version is optional for the Model URI.
    If the version is not specified, the latest version will be used.

    Example:

        \b
        swcli model info mnist # show basic info from the latest version of model
        swcli model info mnist/version/v0 # show basic info from the v0 version of model
        swcli model info mnist/version/latest --output-filter=all # show all info
        swcli model info mnist -of basic # show basic info
        swcli model info mnist -of model_yaml  # show model.yaml
        swcli model info mnist -of handlers # show model runnable handlers info
        swcli model info mnist -of files # show model package files tree
        swcli -o json model info mnist -of all # show all info in json format
    """
    uri = Resource(model, typ=ResourceType.model)
    view(uri).info(ModelInfoFilter(output_filter))


@model_cmd.command("diff", help="model version diff")
@click.argument("base_uri", required=True)
@click.argument("compare_uri", required=True)
@click.option(
    "--show-details",
    is_flag=True,
    help="Show different detail by the model package files",
)
@click.pass_obj
def _diff(
    view: t.Type[ModelTermView], base_uri: str, compare_uri: str, show_details: bool
) -> None:
    view(base_uri).diff(Resource(compare_uri, typ=ResourceType.model), show_details)


@model_cmd.command("list", aliases=["ls"])
@click.option(
    "-p",
    "--project",
    default="",
    help="Project URI, default is the current selected project.",
)
@click.option("-f", "--fullname", is_flag=True, help="Show fullname of model version")
@click.option("-sr", "--show-removed", is_flag=True, help="Show removed model")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for model list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for model list"
)
@click.option(
    "filters",
    "-fl",
    "--filter",
    multiple=True,
    help="Filter output based on conditions provided.",
)
@click.pass_obj
def _list(
    view: t.Type[ModelTermView],
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
    filters: t.List[str],
) -> None:
    """
    List Model of the specified project.

    The filtering flag (-fl or --filter) format is a key=value pair or a flag.
    If there is more than one filter, then pass multiple flags.\n
    (e.g. --filter name=mnist --filter latest)

    \b
    The currently supported filters are:
      name\tTEXT\tThe prefix of the model name
      owner\tTEXT\tThe name or id of the model owner
      latest\tFLAG\t[Cloud] Only show the latest version
            \t \t[Standalone] Only show the version with "latest" tag
    """
    view.list(project, fullname, show_removed, page, size, filters)


@model_cmd.command("history", help="Show model history")
@click.argument("model")
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _history(view: t.Type[ModelTermView], model: str, fullname: bool) -> None:
    view(model).history(fullname)


@model_cmd.command("remove", aliases=["rm"], help="Remove model")
@click.argument("model")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help="Force to remove model, the removed model cannot recover",
)
def _remove(model: str, force: bool) -> None:
    click.confirm("continue to remove?", abort=True)
    ModelTermView(model).remove(force)


@model_cmd.command("recover", help="Recover model")
@click.argument("model")
@click.option("-f", "--force", is_flag=True, help="Force to recover model")
def _recover(model: str, force: bool) -> None:
    ModelTermView(model).recover(force)


@model_cmd.command("run")
@optgroup.group(
    "\n ** Model Selectors",
    cls=RequiredMutuallyExclusiveOptionGroup,
    help="model uri or model source code dir",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-w",
    "--workdir",
    type=click.Path(exists=True, file_okay=False),
    help="Model source dir",
)
@optgroup.option("-u", "--uri", help="Model URI")  # type: ignore[no-untyped-call]
@optgroup.group(
    "\n ** Global Options",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-h",
    "--handler",
    default="0",
    show_default=True,
    help="runnable handler index or name, default is None, will use the first handler",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "modules",
    "-m",
    "--module",
    type=str,
    multiple=True,
    help="module name, the format is python module import path, handlers will be searched in the module. The option supports set multiple times.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-f",
    "--model-yaml",
    help="Model yaml path, default use ${MODEL_DIR}/model.yaml file",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-p",
    "--run-project",
    default="",
    help="Project URI. The model run on the specified project. Default is the current selected project.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-lp",
    "--log-project",
    default="",
    help="Project URI. The model run result will store in the specified project. Default is the current selected project.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "datasets",
    "-d",
    "--dataset",
    required=False,
    envvar=SWEnv.dataset_uri,
    multiple=True,
    help=f"dataset uri, env is {SWEnv.dataset_uri}. For finetune, the argument is used for train dataset",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-dh",
    "--dataset-head",
    required=False,
    default=0,
    type=int,
    show_default=True,
    help="[ONLY STANDALONE]For debugging purpose, every prediction task will, at most, consume the first n rows from every dataset."
    "When the value is less than or equal to 0, all samples will be used. This option is useful for all datasets.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "--in-container",
    is_flag=True,
    help="[ONLY Standalone]Use docker container to run model handler, the docker image or runtime uri must be set",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-fs",
    "--forbid-snapshot",
    is_flag=True,
    help="In model URI mode, each model run uses a new snapshot directory."
    "Setting this parameter will directly use the model's workdir as the run directory."
    "In local dev mode, this parameter does not take effect, each run is in the `--workdir` specified directory.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "--cleanup-snapshot/--no-cleanup-snapshot",
    is_flag=True,
    default=True,
    show_default=True,
    help="[ONLY STANDALONE]Cleanup snapshot dir after model run. When the `--workdir` option is set, this option will be ignored.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "--resource-pool",
    default="default",
    type=str,
    help="resource pool for server side",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-r",
    "--runtime",
    default="",
    help="runtime uri, model run in the runtime environment process",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-i",
    "--image",
    default="",
    help="[ONLY Standalone]docker image, works only when --in-container  is set",
)
@click.option(
    "--version",
    default=None,
    help="Run version",
    hidden=True,
)
@click.option("--step", default="", help="Evaluation run step", hidden=True)
@click.option(
    "--task-index",
    default=-1,
    type=int,
    help="Index of tasks in the current step",
    hidden=True,
)
@click.option(
    "--override-task-num",
    default=0,
    type=int,
    help="Total num of tasks in the current step",
    hidden=True,
)
@click.option(
    "-fpr",
    "--forbid-packaged-runtime",
    is_flag=True,
    help="[ONLY STANDALONE]Forbid to use packaged runtime in the model",
    hidden=True,
)
@optgroup.group("\n  ** Finetune Options")
@optgroup.option(  # type: ignore[no-untyped-call]
    "finetune_val_datasets",
    "-vd",
    "--validation-dataset",
    "--val-dataset",
    required=False,
    envvar=SWEnv.finetune_validation_dataset_uri,
    multiple=True,
    help=f"validation dataset uri for finetune, env is {SWEnv.finetune_validation_dataset_uri}",
)
@click.argument("handler_args", nargs=-1)
def _run(
    workdir: str,
    uri: str,
    handler: int | str,
    modules: t.List[str],
    model_yaml: str,
    run_project: str,
    log_project: str,
    datasets: t.List[str],
    finetune_val_datasets: t.List[str],
    dataset_head: int,
    in_container: bool,
    runtime: str,
    image: str,
    version: str | None,
    step: str,
    task_index: int | None,
    override_task_num: int,
    resource_pool: str,
    forbid_packaged_runtime: bool,
    forbid_snapshot: bool,
    cleanup_snapshot: bool,
    handler_args: t.Tuple[str],
) -> None:
    """Run Model.
    Model Package and the model source directory are supported.

    Examples:

        \b
        # --> run by model uri
        # run the first handler from model uri
        swcli model run -u mnist/version/latest
        # run index id(1) handler from model uri
        swcli model run --uri mnist/version/latest --handler 1
        # run index fullname(mnist.evaluator:MNISTInference.cmp) handler from model uri
        swcli model run --uri mnist/version/latest --handler mnist.evaluator:MNISTInference.cmp

        \b
        # --> run by the working directory, which does not build model package yet. Make local debug happy.
        # run the first handler from the working directory, use the model.yaml in the working directory
        swcli model run -w .
        # run index id(1) handler from the working directory, search mnist.evaluator module and model.yaml handlers(if existed) to get runnable handlers
        swcli model run --workdir . --module mnist.evaluator --handler 1
        # run index fullname(mnist.evaluator:MNISTInference.cmp) handler from the working directory, search mnist.evaluator module to get runnable handlers
        swcli model run --workdir . --module mnist.evaluator --handler mnist.evaluator:MNISTInference.cmp

        \b
        # --> run with the args defined in your handler
        swcli model run --workdir . --module mnist.evaluator --handler 1 -- --your-a=1 -b 3 --your-a 4 -d dataset/version/latest

        \b
        # --> run with dataset of head 10
        swcli model run --uri mnist --dataset-head 10 --dataset mnist

        \b
        # --> run with finetune validation dataset
        swcli model run --workdir . -m mnist.finetune --dataset mnist --val-dataset mnist-val
    """
    # TODO: support run model in cluster mode
    run_project_uri = Project(run_project)
    log_project_uri = Project(log_project)
    in_server = run_project_uri.instance.is_cloud

    if in_container and in_server:
        raise RuntimeError("in-container and in-server are mutually exclusive")

    if in_server:
        if not runtime:
            raise ValueError("runtime is required in server mode")

        if not handler:
            raise ValueError("handler is required in server mode")

        if isinstance(handler, int):
            raise ValueError("handler name is required in server mode")

        if not uri:
            raise ValueError("uri is required in server mode")

        if (
            log_project_uri.instance != run_project_uri.instance
            or log_project_uri.name != run_project_uri.name
        ):
            raise RuntimeError(
                "log project must be consistent with run project in server mode"
            )

        # TODO: support finetune val dataset in server mode
        ModelTermView.run_in_server(
            project_uri=run_project_uri,
            model_uri=uri,
            dataset_uris=datasets,
            runtime_uri=runtime,
            resource_pool=resource_pool,
            run_handler=handler,
        )
        return

    model_src_dir, model_config, runtime_uri = _prepare_model_run_args(
        model=uri,
        runtime=runtime,
        workdir=workdir,
        modules=modules,
        model_yaml=model_yaml,
        forbid_packaged_runtime=forbid_packaged_runtime,
    )

    if in_container:
        ModelTermView.run_in_container(
            model_src_dir=model_src_dir,
            runtime_uri=runtime_uri,
            docker_image=image,
        )
    else:
        if workdir:
            # when workdir is set, snapshot mechanism is forbidden.
            forbid_snapshot = True
            cleanup_snapshot = False

        ModelTermView.run_in_host(
            model_src_dir=model_src_dir,
            model_config=model_config,
            run_project=run_project_uri,
            log_project=log_project_uri,
            version=version,
            run_handler=handler,
            dataset_uris=datasets,
            finetune_val_dataset_uris=finetune_val_datasets,
            dataset_head=dataset_head,
            runtime_uri=runtime_uri,
            forbid_snapshot=forbid_snapshot,
            cleanup_snapshot=cleanup_snapshot,
            scheduler_run_args={
                "step_name": step,
                "task_index": task_index,
                "task_num": override_task_num,
            },
            force_generate_jobs_yaml=uri is None,
            handler_args=list(handler_args) if handler_args else [],
        )


@model_cmd.command("serve")
@optgroup.group(
    "\n ** Model Selectors",
    cls=RequiredMutuallyExclusiveOptionGroup,
    help="model uri or model source code dir",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-w",
    "--workdir",
    type=click.Path(exists=True, file_okay=False),
    help="Model source dir",
)
@optgroup.option("-u", "--uri", help="Model URI")  # type: ignore[no-untyped-call]
@click.option(  # type: ignore[no-untyped-call]
    "-f",
    "--model-yaml",
    help="Model yaml path, default use ${MODEL_DIR}/model.yaml file",
)
@click.option("-r", "--runtime", default="", help="runtime uri")
@click.option(
    "--host", default="127.0.0.1", show_default=True, help="The host to listen on"
)
@click.option("--port", default=8080, show_default=True, help="The port of the server")
@click.option(
    "-fpr",
    "--forbid-packaged-runtime",
    is_flag=True,
    help="[ONLY STANDALONE]Forbid to use packaged runtime in the model",
    hidden=True,
)
@click.option(
    "modules",
    "-m",
    "--module",
    multiple=True,
    help="Python modules to be imported during the build process. The format is python module import path. The option supports set multiple times.",
)
def _serve(
    workdir: str,
    uri: str,
    model_yaml: str,
    runtime: str,
    host: str,
    port: int,
    forbid_packaged_runtime: bool,
    modules: t.List[str],
) -> None:
    """Serve Model.

    Examples:

        \b
        swcli model serve -u mnist/version/latest
        swcli model serve --uri mnist/version/latest --runtime pytorch/version/latest

        \b
        swcli model serve --workdir . --runtime pytorch/version/latest
        swcli model serve --workdir . --runtime pytorch/version/latest --host 0.0.0.0 --port 8080
        swcli model serve --workdir . --runtime pytorch/version/latest --module mnist.evaluator
    """
    model_src_dir, config, runtime_uri = _prepare_model_run_args(
        model=uri,
        runtime=runtime,
        workdir=workdir,
        modules=modules,
        model_yaml=model_yaml,
        forbid_packaged_runtime=forbid_packaged_runtime,
    )

    ModelTermView.serve(
        model_src_dir=model_src_dir,
        model_config=config,
        host=host,
        port=port,
        runtime_uri=runtime_uri,
    )


def _prepare_model_run_args(
    model: str,
    runtime: str,
    workdir: str,
    modules: t.List[str],
    model_yaml: t.Optional[str],
    forbid_packaged_runtime: bool,
) -> t.Tuple[Path, ModelConfig, t.Optional[Resource]]:
    runtime_uri: Resource | None = None
    if runtime:
        try:
            runtime_uri = Resource(runtime, typ=ResourceType.runtime)
        except Exception:
            pass

    if model:
        model_uri = Resource(model, typ=ResourceType.model)
        if model_uri.instance.is_cloud:
            model_uri = BundleCopy.download_for_cache(model_uri)

        model_store = ModelStorage(model_uri)
        model_src_dir = model_store.src_dir

        if modules:
            raise NoSupportError("module is not supported in model uri mode")

        if (
            os.environ.get(Process.EnvInActivatedProcess, "0") == "0"
            and model_store.digest.get("packaged_runtime")
            and not forbid_packaged_runtime
            and runtime_uri is None
        ):
            runtime_uri = model_uri
    else:
        model_src_dir = Path(workdir)

    if runtime_uri and runtime_uri.instance.is_cloud:
        runtime_uri = BundleCopy.download_for_cache(runtime_uri)

    model_src_dir = model_src_dir.absolute().resolve()
    if model_yaml is None:
        yaml_path = model_src_dir / DefaultYAMLName.MODEL
    else:
        yaml_path = Path(model_yaml)

    if yaml_path.exists():
        config = ModelConfig.create_by_yaml(yaml_path)
    else:
        config = ModelConfig()

    config.name = config.name or model_src_dir.name
    config.run.modules = modules or config.run.modules
    config.do_validate()

    return model_src_dir, config, runtime_uri
