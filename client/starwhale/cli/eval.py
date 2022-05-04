import click
from loguru import logger

from starwhale.cluster import ClusterView, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE


@click.group("eval", help="Manage remote StarWhale Controller Job/Task/Predictor...")
def eval_cmd():
    pass


@eval_cmd.command("list", help="list remote starwhale controller job")
@click.argument("project", type=int)
@click.option("-p", "--page", type=int, default=DEFAULT_PAGE_NUM, help="page number for projects list")
@click.option("-s", "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list")
def _list(project, page, size):
    ClusterView().list_jobs(project, page, size)


@eval_cmd.command("inspect", help="inspect job info with job id")
@click.argument("project", type=int)
@click.argument("job", type=int)
@click.option("-p", "--page", type=int, default=DEFAULT_PAGE_NUM, help="page number for projects list")
@click.option("-s", "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list")
def _inspect(project, job, page, size):
    ClusterView().inspect_job(project, job, page, size)


@eval_cmd.command("run", help="run evaluation in local or remote controller cluster")
def _run():
    pass