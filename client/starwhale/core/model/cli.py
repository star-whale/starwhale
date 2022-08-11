import typing as t

import click

from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.type import EvalTaskType
from starwhale.consts.env import SWEnv
from starwhale.core.job.view import JobTermView

from .view import get_term_view, ModelTermView


@click.group("model", help="Model management, build/copy/ppl/cmp/eval/extract...")
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
def _build(workdir: str, project: str, model_yaml: str) -> None:
    ModelTermView.build(workdir, project, model_yaml)


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


@model_cmd.command("copy", help="Copy model, standalone <--> cloud")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force to copy model")
def _copy(src: str, dest: str, force: bool) -> None:
    ModelTermView.copy(src, dest, force)


@model_cmd.command("info", help="Show model details")
@click.argument("model")
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _info(view: t.Type[ModelTermView], model: str, fullname: bool) -> None:
    view(model).info(fullname)


@model_cmd.command("list", help="List Model")
@click.option("--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of model version")
@click.option("--show-removed", is_flag=True, help="Show removed model")
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
def _history(model: str, fullname: bool) -> None:
    ModelTermView(model).history(fullname)


@model_cmd.command("remove", help="Remove model")
@click.argument("model")
@click.option("-f", "--force", is_flag=True, help="Force to remove model")
def _remove(model: str, force: bool) -> None:
    click.confirm("continue to delete?", abort=True)
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


@model_cmd.command("ppl")
@click.argument("target")
@click.option(
    "-f",
    "--model-yaml",
    default=DefaultYAMLName.MODEL,
    help="Model yaml filename, default use ${MODEL_DIR}/model.yaml file",
)
@click.option(
    "--status-dir",
    envvar=SWEnv.status_dir,
    default="/tmp/starwhale/ppl/status",
    help=f"PPL status dir, env is {SWEnv.status_dir}",
)
@click.option(
    "--log-dir",
    envvar=SWEnv.log_dir,
    default="/tmp/starwhale/ppl/log",
    help=f"PPL log dir, env is {SWEnv.log_dir}",
)
@click.option(
    "--result-dir",
    envvar=SWEnv.result_dir,
    default="/tmp/starwhale/ppl/result",
    help=f"PPL result dir, env is {SWEnv.result_dir}",
)
@click.option("--runtime", default="", help="runtime uri")
@click.option("--runtime-restore", is_flag=True, help="Force to restore runtime")
@click.option("--dataset", envvar=SWEnv.dataset_uri, help="dataset uri")
@click.option(
    "--dataset-row-start",
    envvar=SWEnv.dataset_row_start,
    type=int,
    default=0,
    help="dataset row start index",
)
@click.option(
    "--dataset-row-end",
    envvar=SWEnv.dataset_row_end,
    type=int,
    default=-1,
    help="dataset row end index",
)
def _ppl(
    target: str,
    model_yaml: str,
    status_dir: str,
    log_dir: str,
    result_dir: str,
    runtime: str,
    runtime_restore: bool,
    dataset: str,
    dataset_row_start: int,
    dataset_row_end: int,
) -> None:
    """
    [ONLY Standalone]Run PPL

    TARGET: model uri or model workdir path, in Starwhale agent docker environment, only support workdir path.
    """
    # TODO: support render fuse json for cmp test

    ModelTermView.eval(
        target=target,
        yaml_name=model_yaml,
        typ=EvalTaskType.PPL,
        runtime_uri=runtime,
        runtime_restore=runtime_restore,
        kw={
            "status_dir": status_dir,
            "log_dir": log_dir,
            "result_dir": result_dir,
            "dataset_uri": dataset,
            "dataset_row_start": dataset_row_start,
            "dataset_row_end": dataset_row_end,
        },
    )


@model_cmd.command("cmp")
@click.argument("target")
@click.option(
    "-f",
    "--model-yaml",
    default=DefaultYAMLName.MODEL,
    help="Model yaml filename, default use ${MODEL_DIR}/model.yaml file",
)
@click.option(
    "--status-dir",
    envvar=SWEnv.status_dir,
    default="/tmp/starwhale/cmp/status",
    help=f"CMP status dir, env is {SWEnv.status_dir}",
)
@click.option(
    "--log-dir",
    envvar=SWEnv.log_dir,
    default="/tmp/starwhale/cmp/log",
    help=f"CMP log dir, env is {SWEnv.log_dir}",
)
@click.option(
    "--result-dir",
    envvar=SWEnv.result_dir,
    default="/tmp/starwhale/cmp/result",
    help=f"CMP result dir, env is {SWEnv.result_dir}",
)
@click.option("--runtime", default="", help="runtime uri")
@click.option("--runtime-restore", is_flag=True, help="Force to restore runtime")
def _cmp(
    target: str,
    model_yaml: str,
    status_dir: str,
    log_dir: str,
    result_dir: str,
    runtime: str,
    runtime_restore: bool,
) -> None:
    """
    [ONLY Standalone]Run CMP, compare inference output with label, then generate result jsonline file.

    TARGET: model uri or model workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    """
    ModelTermView.eval(
        target=target,
        yaml_name=model_yaml,
        typ=EvalTaskType.CMP,
        runtime_uri=runtime,
        runtime_restore=runtime_restore,
        kw={
            "status_dir": status_dir,
            "log_dir": log_dir,
            "result_dir": result_dir,
        },
    )


@model_cmd.command("eval")
@click.argument("model")
@click.option(
    "--dataset",
    required=True,
    multiple=True,
    help="Dataset URI, one or more",
)
@click.option("--name", help="Job name")
@click.option("--desc", help="Job description")
@click.option("-p", "--project", default="", help="Project URI")
@click.option("--runtime", default="", help="runtime uri")
@click.option("--runtime-restore", is_flag=True, help="Force to restore runtime")
def _eval(
    model: str,
    dataset: t.List[str],
    name: str,
    desc: str,
    project: str,
    runtime: str,
    runtime_restore: bool,
) -> None:
    """
    [ONLY Standalone]Create as new job for model evaluation

    MODEL: model uri or model workdir path
    """
    JobTermView.create(
        project_uri=project,
        model_uri=model,
        dataset_uris=dataset,
        runtime_uri=runtime,
        name=name,
        desc=desc,
        use_docker=False,
        gencmd=False,
        phase=EvalTaskType.ALL,
        runtime_restore=runtime_restore,
    )
