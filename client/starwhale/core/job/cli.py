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


@job_cmd.command("list", aliases=["ls"], help="List all jobs in the specified project")
@click.option(
    "-p",
    "--project",
    default="",
    help="Project URI, default is the current selected project.",
)
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


@job_cmd.command("pause", help="Pause job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to pause")
def _pause(job: str, force: bool) -> None:
    click.confirm("continue to pause?", abort=True)
    JobTermView(job).pause(force)


@job_cmd.command("resume", help="Resume job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to resume")
def _resume(job: str, force: bool) -> None:
    JobTermView(job).resume(force)


@job_cmd.command("cancel", help="Cancel job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="Force to cancel")
def _cancel(job: str, force: bool) -> None:
    click.confirm("continue to cancel?", abort=True)
    JobTermView(job).cancel(force)


@job_cmd.command("info", help="Inspect job details")
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
@click.option("--web", is_flag=True, help="Open job info page in browser")
@click.pass_obj
def _info(
    view: t.Type[JobTermView],
    job: str,
    page: int,
    size: int,
    max_report_cols: int,
    web: bool,
) -> None:
    view(job).info(page, size, max_report_cols, web)
