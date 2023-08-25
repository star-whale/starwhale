import typing as t

import click

from starwhale.utils.cli import AliasedGroup

from .view import (
    JobTermView,
    get_term_view,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_REPORT_COLS,
)


@click.group(
    "job",
    cls=AliasedGroup,
    help="Job management, list/info/cancel/resume job",
)
@click.pass_context
def job_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@job_cmd.command("list", aliases=["ls"])
@click.option(
    "-p",
    "--project",
    default="",
    help="Project URI, default is the current selected project.",
)
@click.option("--fullname", is_flag=True, help="Show fullname of job")
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
    page: int,
    size: int,
) -> None:
    """List jobs in the specified project.

    Examples:

        \b
        - List jobs in the current project:
        swcli job list

        \b
        - List jobs from the specified project in the server instance:
        swcli job list --project cloud://server/project/1
        swcli job list --project cloud://server/project/1 --page 1 --size 10
    """
    view.list(project, fullname=fullname, page=page, size=size)


@job_cmd.command("remove", aliases=["rm"], help="Remove job")
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


@job_cmd.command("recover", help="Recover removed job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to recover")
def _recover(job: str, force: bool) -> None:
    JobTermView(job).recover(force)


@job_cmd.command("pause")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to pause")
def _pause(job: str, force: bool) -> None:
    """Pause job.
    On Standalone instance, this command only takes effect for containerized jobs.
    """
    click.confirm("continue to pause?", abort=True)
    JobTermView(job).pause(force)


@job_cmd.command("resume")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to resume")
def _resume(job: str, force: bool) -> None:
    """Resume job.
    On Standalone instance, this command only takes effect for containerized jobs.
    """
    JobTermView(job).resume(force)


@job_cmd.command("cancel")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to cancel")
def _cancel(job: str, force: bool) -> None:
    """Cancel job.
    On Standalone instance, this command only takes effect for containerized jobs.
    """
    click.confirm("continue to cancel?", abort=True)
    JobTermView(job).cancel(force)


@job_cmd.command("info")
@click.argument("job")
@click.option(
    "--max-report-cols",
    type=int,
    default=DEFAULT_REPORT_COLS,
    help="Max table column size for print",
)
@click.option("--web", is_flag=True, help="Open job info page in browser")
@click.pass_obj
def _info(
    view: t.Type[JobTermView],
    job: str,
    max_report_cols: int,
    web: bool,
) -> None:
    """Inspect job details

    Examples:

        \b
        - Get job info from the current selected project:
        swcli job info xm5wnup

        \b
        - Get job info from the specified project
        swcli job info local/project/self/job/xm5wnup
        swcli job info cloud://cloud-cn/project/tianwei:llm-leaderboard/job/e0fa100599944951a20278b4062eb9fb
        swcli job info cloud://cloud-cn/project/257/job/e0fa100599944951a20278b4062eb9fb
    """
    # TODO: add output filter option
    view(job).info(max_report_cols, web)
