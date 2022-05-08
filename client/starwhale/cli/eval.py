import click
from loguru import logger

from starwhale.cluster import ClusterView, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE
from starwhale.eval.executor import EVAL_TASK_TYPE, DEFAULT_SW_TASK_RUN_IMAGE, EvalExecutor


@click.group("eval", help="Manage remote StarWhale Controller Job/Task/Predictor...")
def eval_cmd():
    pass


@eval_cmd.command("list", help="list remote starwhale controller job")
@click.option("--local/--remote", default=False, help="local mode or remote cluster mode, different mode will use different executor")
@click.argument("project", type=int)
@click.option("-p", "--page", type=int, default=DEFAULT_PAGE_NUM, help="page number for projects list")
@click.option("-s", "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list")
def _list(local, project, page, size):
    if local:
        pass
    else:
        ClusterView().list_jobs(project, page, size)


@eval_cmd.command("inspect", help="inspect job info with job id")
@click.argument("project", type=int)
@click.argument("job", type=int)
@click.option("-p", "--page", type=int, default=DEFAULT_PAGE_NUM, help="page number for projects list")
@click.option("-s", "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for projects list")
def _inspect(project, job, page, size):
    ClusterView().inspect_job(project, job, page, size)


@eval_cmd.command("run", help="run evaluation in local or remote controller cluster")
@click.option("--local/--remote", default=False, help="local mode or remote cluster mode, different mode will use different executor")
@click.option("--model", required=True, help="remote mode: model id, local model: model [name]:[version]")
@click.option("--dataset", required=True, multiple=True, help="remote mode: dataset id, local model: dataset [name]:[version]")
@click.option("--project", help="[remote]project id")
@click.option("--baseimage", default=DEFAULT_SW_TASK_RUN_IMAGE, help="task baseimage name or id")
@click.option("--resource", default="cpu:1", help="[remote]resource, fmt is resource [name]:[cnt], such as cpu:1, gpu:2")
@click.option("--name", help="evaluation job name")
@click.option("--desc", help="evaluation job description")
@click.option("--gencmd", is_flag=True, help="[local]gen docker run command")
@click.option("--phase", type=click.Choice(EVAL_TASK_TYPE), default=EVAL_TASK_TYPE.ALL)
def _run(local, model, dataset, project, baseimage, resource, name, desc, gencmd, phase):
    if local:
        EvalExecutor(model, dataset, baseimage, name, desc).run(phase)
    else:
        ClusterView().run_job(model, dataset, project, baseimage, resource, name, desc)