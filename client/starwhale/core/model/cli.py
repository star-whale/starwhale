import typing as t

import click

from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.utils.cli import AliasedGroup
from starwhale.consts.env import SWEnv
from starwhale.core.model.view import get_term_view, ModelTermView


@click.group(
    "model",
    cls=AliasedGroup,
    help="Model management, build/copy/ppl/cmp/eval/extract...",
)
@click.pass_context
def model_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@model_cmd.command("build", help="[ONLY Standalone]Build starwhale model")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-p", "--project", default="", help="Project URI")
@click.option(
    "-f",
    "--model-yaml",
    default=DefaultYAMLName.MODEL,
    help="mode yaml filename, default use ${workdir}/model.yaml file",
)
@click.option("--runtime", default="", help="runtime uri")
def _build(workdir: str, project: str, model_yaml: str, runtime: str) -> None:
    ModelTermView.build(workdir, project, model_yaml, runtime)


@model_cmd.command("tag", help="Model Tag Management, add or remove")
@click.argument("model")
@click.argument("tags", nargs=-1)
@click.option("-r", "--remove", is_flag=True, help="Remove tags")
@click.option(
    "-q",
    "--quiet",
    is_flag=True,
    help="Ignore tag name errors like name duplication, name absence",
)
def _tag(model: str, tags: t.List[str], remove: bool, quiet: bool) -> None:
    ModelTermView(model).tag(tags, remove, quiet)


@model_cmd.command("copy", aliases=["cp"])
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force to copy model")
@click.option("-dlp", "--dest-local-project", help="dest local project uri")
def _copy(src: str, dest: str, force: bool, dest_local_project: str) -> None:
    """
    Copy Model between Standalone Instance and Cloud Instance

    SRC: model uri with version

    DEST: project uri or model uri with name.

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
        - copy standalone instance(local) project(myproject)'s mnist-local model to cloud instance(pre-k8s) mnist project with standalone instance model name 'mnist-local'
            swcli model cp local/project/myproject/model/mnist-local/version/latest cloud://pre-k8s/project/mnist
    """
    ModelTermView.copy(src, dest, force, dest_local_project)


@model_cmd.command("info", help="Show model details")
@click.argument("model")
@click.option("-f", "--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _info(view: t.Type[ModelTermView], model: str, fullname: bool) -> None:
    view(model).info(fullname)


@model_cmd.command("list", aliases=["ls"], help="List Model")
@click.option("-p", "--project", default="", help="Project URI")
@click.option("-f", "--fullname", is_flag=True, help="Show fullname of model version")
@click.option("-sr", "--show-removed", is_flag=True, help="Show removed model")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for model list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for model list"
)
@click.pass_obj
def _list(
    view: t.Type[ModelTermView],
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
) -> None:
    view.list(project, fullname, show_removed, page, size)


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


@model_cmd.command(
    "extract", help="[ONLY Standalone]Extract local model bundle tar file into workdir"
)
@click.argument("model")
@click.option("-f", "--force", is_flag=True, help="Force to extract model bundle")
@click.option(
    "--target-dir",
    default="",
    help="Extract target dir.if omitted, swcli will use starwhale default workdir",
)
def _extract(model: str, force: bool, target_dir: str) -> None:
    ModelTermView(model).extract(force, target_dir)


@model_cmd.command("eval")
@click.argument("target")
@click.option(
    "-f",
    "--model-yaml",
    default=DefaultYAMLName.MODEL,
    help="Model yaml filename, default use ${MODEL_DIR}/model.yaml file",
)
@click.option(
    "-p",
    "--project",
    envvar=SWEnv.project,
    default="",
    help=f"project name, env is {SWEnv.project}",
)
@click.option(
    "--version",
    envvar=SWEnv.eval_version,
    default=None,
    help=f"Evaluation job version, env is {SWEnv.eval_version}",
)
@click.option("--step", default="", help="Evaluation run step")
@click.option("--task-index", default=-1, help="Index of tasks in the current step")
@click.option(
    "--override-task-num",
    default=0,
    help="Total num of tasks in the current step",
)
@click.option("--runtime", default="", help="runtime uri")
@click.option(
    "datasets",
    "--dataset",
    required=True,
    envvar=SWEnv.dataset_uri,
    multiple=True,
    help=f"dataset uri, env is {SWEnv.dataset_uri}",
)
def _eval(
    project: str,
    target: str,
    model_yaml: str,
    version: str,
    datasets: list,
    step: str,
    task_index: int,
    override_task_num: int,
    runtime: str,
) -> None:
    """
    [ONLY Standalone]Run evaluation processing with root dir of {target}.

    TARGET: model uri or model workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    """
    ModelTermView.eval(
        project=project,
        target=target,
        version=version,
        yaml_name=model_yaml,
        runtime_uri=runtime,
        step=step,
        task_index=task_index,
        task_num=override_task_num,
        dataset_uris=datasets,
    )
