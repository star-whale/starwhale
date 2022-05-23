import click
import typing as t

from .view import JobTermView, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE


@click.group("job", help="Starwhale job management")
def job_cmd() -> None:
    pass


@job_cmd.command("list", help="List all jobs in current project")
@click.option("-p", "--projecct", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of swmp version")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="page number for projects list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list"
)
def _list(
    project: str,
    fullname: bool,
    page: int,
    size: int,
) -> None:
    JobTermView.list(project, fullname=fullname, page=page, size=size)


@job_cmd.command("info", help="Inspect job details")
@click.argument("job")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="page number for projects list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list"
)
def _info(job: str, page: int, size: int) -> None:
    JobTermView(job).info(page, size)


@job_cmd.command("pause", help="Pause job")
@click.argument("job")
def _pause(job: str) -> None:
    JobTermView(job).pause()


@job_cmd.command("resume", help="Resume job")
@click.argument("job")
def _resume(job: str) -> None:
    JobTermView(job).resume()


@job_cmd.command("cancel", help="Cancel job")
@click.argument("job")
def _cancel(job: str) -> None:
    JobTermView(job).cancel()


@job_cmd.command("create", help="Create job")
@click.argument("job")
@click.option("--project", help="Project URI")
@click.option("--model", required=True, help="local storage model [name]:[version]")
@click.option(
    "--dataset",
    required=True,
    multiple=True,
    help="local storeage dataset [name]:[version], one or more",
)
@click.option("--runtime", default="", help="Job Runtime")
@click.option("--name", help="evaluation job name")
@click.option("--desc", help="evaluation job description")
@click.option(
    "--resource",
    default="cpu:1",
    type=str,
    help="resource, fmt is resource [name]:[cnt], such as cpu:1, gpu:2",
)
@click.option("--gencmd", is_flag=True, help="gen docker run command")
@click.option("--docker-verbose", is_flag=True, help="docker run verbose output")
@click.option(
    "--resource",
    default="cpu:1",
    type=str,
    help="resource, fmt is resource [name]:[cnt], such as cpu:1, gpu:2",
)
def _create(
    job: str,
    project: str,
    model: str,
    dataset: t.List[str],
    runtime: str,
    name: str,
    desc: str,
    resource: str,
    gencmd: bool,
    docker_verbose: bool,
) -> None:
    JobTermView(job).create(
        project_uri=project,
        model_uri=model,
        dataset_uris=dataset,
        runtime_uri=runtime,
        name=name,
        desc=desc,
        resource=resource,
        gencmd=gencmd,
        docker_verbose=docker_verbose,
    )


@job_cmd.command("remove", help="Remove job")
@click.argument("job")
def _remove(job: str) -> None:
    JobTermView(job).remove()


@job_cmd.command("recover", help="Recover removed job")
@click.argument("job")
def _recover(job: str) -> None:
    JobTermView(job).recover()
