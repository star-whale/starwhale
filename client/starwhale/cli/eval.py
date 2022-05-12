import click

from starwhale.cluster import ClusterView, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE
from starwhale.eval.executor import (
    EvalTaskType,
    EvalExecutor,
    DEFAULT_SW_TASK_RUN_IMAGE,
)
from starwhale.eval.store import EvalLocalStorage


@click.group("local", help="Local mode")
def _local_mode_cmd():
    pass


@click.group("cluster", help="Remote cluster mode with starwhale controller")
def _cluster_mode_cmd():
    pass


@click.group("eval", help="Manage Evaluation in local or cluster mode")
def eval_cmd():
    pass


eval_cmd.add_command(_local_mode_cmd)
eval_cmd.add_command(_cluster_mode_cmd)


@_local_mode_cmd.command("list", help="List evaluation result")
def _local_list():
    EvalLocalStorage().list()


@_cluster_mode_cmd.command("list", help="List remote starwhale controller job")
@click.argument("project", type=int)
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_NUM, help="page number for projects list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list"
)
def _cluster_list(project, page, size):
    ClusterView().list_jobs(project, page, size)


@_local_mode_cmd.command("info", help="Get job info with eval version")
@click.argument("version")
def _local_info(version):
    EvalLocalStorage().info(version)


@_local_mode_cmd.command("delete", help="Delete eval result")
@click.argument("version")
def _local_delete(version):
    EvalLocalStorage().delete(version)


@_cluster_mode_cmd.command("info", help="Get job info with job id")
@click.argument("project", type=int)
@click.argument("job", type=int)
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_NUM, help="page number for projects list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list"
)
def _cluster_info(project, job, page, size):
    ClusterView().info_job(project, job, page, size)


@_local_mode_cmd.command("run", help="Run evaluation in local")
@click.option("--model", required=True, help="local storage model [name]:[version]")
@click.option(
    "--dataset",
    required=True,
    multiple=True,
    help="local storeage dataset [name]:[version], one or more",
)
@click.option(
    "--baseimage", default=DEFAULT_SW_TASK_RUN_IMAGE, help="task baseimage name"
)
@click.option("--name", help="evaluation job name")
@click.option("--desc", help="evaluation job description")
@click.option(
    "--phase",
    type=click.Choice([EvalTaskType.ALL, EvalTaskType.CMP, EvalTaskType.PPL]),
    default=EvalTaskType.ALL,
    help="evalution run phase",
)
@click.option("--gencmd", is_flag=True, help="gen docker run command")
@click.option("--docker-verbose", is_flag=True, help="docker run verbose output")
def _local_run(model, dataset, baseimage, name, desc, phase, gencmd, docker_verbose):
    EvalExecutor(
        model,
        dataset,
        baseimage,
        name,
        desc,
        gencmd=gencmd,
        docker_verbose=docker_verbose,
    ).run(phase)


@_cluster_mode_cmd.command("run", help="Run evaluation in remote controller cluster")
@click.option("--model", required=True, help="model id")
@click.option("--dataset", required=True, multiple=True, help="dataset id, one or more")
@click.option("--project", help="project id")
@click.option("--baseimage", type=int, help="task baseimage id")
@click.option(
    "--resource",
    default="cpu:1",
    help="resource, fmt is resource [name]:[cnt], such as cpu:1, gpu:2",
)
@click.option("--name", help="evaluation job name")
@click.option("--desc", help="evaluation job description")
def _cluster_run(model, dataset, project, baseimage, resource, name, desc):
    ClusterView().run_job(model, dataset, project, baseimage, resource, name, desc)
