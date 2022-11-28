import typing as t

import click

from starwhale.utils.cli import AliasedGroup
from starwhale.consts.env import SWEnv

from .view import (
    JobTermView,
    get_term_view,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_REPORT_COLS,
)


@click.group(
    "eval",
    cls=AliasedGroup,
    help="Evaluation management, create/list/info/compare evaluation job",
)
@click.pass_context
def eval_job_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@eval_job_cmd.command(
    "list", aliases=["ls"], help="List all jobs in the current project"
)
@click.option("-p", "--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of swmp version")
@click.option("--show-removed", is_flag=True, help="Show removed dataset")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for job list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for job list"
)
@click.pass_obj
def _list(
    view: t.Type[JobTermView],
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
) -> None:
    view.list(
        project, fullname=fullname, show_removed=show_removed, page=page, size=size
    )


@eval_job_cmd.command("run", help="Run job")
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
@click.option("--model", required=True, help="model uri or model.yaml dir path")
# TODO:support multi dataset
@click.option(
    "datasets",
    "--dataset",
    required=True,
    envvar=SWEnv.dataset_uri,
    multiple=True,
    help=f"dataset uri, env is {SWEnv.dataset_uri}",
)
@click.option("--runtime", default="", help="runtime uri")
@click.option("--name", default="default", help="job name")
@click.option("--desc", help="job description")
@click.option(
    "--step-spec",
    default="",
    type=str,
    help="[Cloud_ONLY] A file contains the specification for steps of the job",
)
@click.option(
    "--resource-pool",
    default="default",
    type=str,
    help="[Cloud_ONLY] The node group you would like to run your job on",
)
@click.option(
    "--use-docker",
    is_flag=True,
    help="[ONLY Standalone]use docker to run evaluation job",
)
@click.option("--gencmd", is_flag=True, help="[ONLY Standalone]gen docker run command")
@click.option("--step", default="", help="Evaluation run step")
@click.option("--task-index", default=-1, help="Index of tasks in the current step")
def _run(
    project: str,
    version: str,
    model: str,
    datasets: list,
    runtime: str,
    name: str,
    desc: str,
    step_spec: str,
    resource_pool: str,
    use_docker: bool,
    gencmd: bool,
    step: str,
    task_index: int,
) -> None:
    # TODO: tune so many arguments
    JobTermView.run(
        project_uri=project,
        version=version,
        model_uri=model,
        dataset_uris=datasets,
        runtime_uri=runtime,
        name=name,
        desc=desc,
        step_spec=step_spec,
        resource_pool=resource_pool,
        gencmd=gencmd,
        use_docker=use_docker,
        step=step,
        task_index=task_index,
    )


@eval_job_cmd.command("remove", aliases=["rm"], help="Remove job")
@click.argument("job")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help="Force to remove, the removed job cannot recover",
)
def _remove(job: str, force: bool) -> None:
    click.confirm("continue to remove?", abort=True)
    JobTermView(job).remove(force)


@eval_job_cmd.command("recover", help="Recover removed job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to recover")
def _recover(job: str, force: bool) -> None:
    JobTermView(job).recover(force)


@eval_job_cmd.command("pause", help="Pause job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to pause")
def _pause(job: str, force: bool) -> None:
    click.confirm("continue to pause?", abort=True)
    JobTermView(job).pause(force)


@eval_job_cmd.command("resume", help="Resume job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to resume")
def _resume(job: str, force: bool) -> None:
    JobTermView(job).resume(force)


@eval_job_cmd.command("cancel", help="Cancel job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to cancel")
def _cancel(job: str, force: bool) -> None:
    click.confirm("continue to cancel?", abort=True)
    JobTermView(job).cancel(force)


@eval_job_cmd.command("info", help="Inspect job details")
@click.argument("job")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for tasks list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for tasks list"
)
@click.option(
    "--max-report-cols",
    type=int,
    default=DEFAULT_REPORT_COLS,
    help="Max table column size for print",
)
@click.pass_obj
def _info(
    view: t.Type[JobTermView], job: str, page: int, size: int, max_report_cols: int
) -> None:
    view(job).info(page, size, max_report_cols)


@eval_job_cmd.command("compare", aliases=["cmp"])
@click.argument("base_job", nargs=1)
@click.argument("job", nargs=-1)
def _compare(base_job: str, job: t.List[str]) -> None:
    """
    [ONLY Standalone]Compare the result of evaluation job

    BASE_JOB: job uri

    JOB: job uri
    """
    JobTermView(base_job).compare(job)
