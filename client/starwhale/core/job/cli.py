import typing as t

import click

from starwhale.base.type import EvalTaskType

from .view import JobTermView, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE


@click.group("job", help="Starwhale job management")
def job_cmd() -> None:
    pass


@job_cmd.command("list", help="List all jobs in current project")
@click.option("-p", "--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of swmp version")
@click.option("--show-removed", is_flag=True, help="Show removed dataset")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="page number for projects list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list"
)
def _list(
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
) -> None:
    JobTermView.list(
        project, fullname=fullname, show_removed=show_removed, page=page, size=size
    )


@job_cmd.command("create", help="Create job")
@click.argument("project")
@click.option("--model", required=True, help="model uri")
@click.option(
    "--dataset",
    required=True,
    multiple=True,
    help="dataset uri, one or more",
)
@click.option("--runtime", default="", help="runtime uri")
@click.option("--name", help="job name")
@click.option("--desc", help="job description")
@click.option(
    "--resource",
    default="cpu:1",
    type=str,
    help="[ONLY Cloud]resource, fmt is resource [name]:[cnt], such as cpu:1, gpu:2",
)
@click.option("--gencmd", is_flag=True, help="[ONLY Standalone]gen docker run command")
@click.option(
    "--docker-verbose", is_flag=True, help="[ONLY Standalone]docker run verbose output"
)
@click.option(
    "--phase",
    type=click.Choice([EvalTaskType.ALL, EvalTaskType.CMP, EvalTaskType.PPL]),
    default=EvalTaskType.ALL,
    help="[ONLY Standalone]evalution run phase",
)
def _create(
    project: str,
    model: str,
    dataset: t.List[str],
    runtime: str,
    name: str,
    desc: str,
    resource: str,
    gencmd: bool,
    docker_verbose: bool,
    phase: str,
) -> None:
    JobTermView.create(
        project_uri=project,
        model_uri=model,
        dataset_uris=dataset,
        runtime_uri=runtime,
        name=name,
        desc=desc,
        resource=resource,
        gencmd=gencmd,
        docker_verbose=docker_verbose,
        phase=phase,
    )


@job_cmd.command("remove", help="Remove job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="force to remove")
def _remove(job: str, force: bool) -> None:
    click.confirm("continue to remove?", abort=True)
    JobTermView(job).remove(force)


@job_cmd.command("recover", help="Recover removed job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="force to recover")
def _recover(job: str, force: bool) -> None:
    JobTermView(job).recover(force)


@job_cmd.command("pause", help="Pause job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="force to pause")
def _pause(job: str, force: bool) -> None:
    click.confirm("continue to pause?", abort=True)
    JobTermView(job).pause(force)


@job_cmd.command("resume", help="Resume job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="force to resume")
def _resume(job: str, force: bool) -> None:
    JobTermView(job).resume(force)


@job_cmd.command("cancel", help="Cancel job")
@click.argument("job")
@click.option("-f", "--force", is_flag=True, help="force to cancel")
def _cancel(job: str, force: bool) -> None:
    click.confirm("continue to cancel?", abort=True)
    JobTermView(job).cancel(force)


@job_cmd.command("info", help="Inspect job details")
@click.argument("job")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="page number for tasks list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for tasks list"
)
def _info(job: str, page: int, size: int) -> None:
    JobTermView(job).info(page, size)
