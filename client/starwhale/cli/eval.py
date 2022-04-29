import click
from loguru import logger


@click.group("eval", help="Manage remote StarWhale Controller Job/Task/Predictor...")
def eval_cmd():
    pass


@eval_cmd.command("list", help="list remote starwhale controller job")
def _list():
    pass


@eval_cmd.command("info", help="inspect job info with job id")
def _info():
    pass


@eval_cmd.command("run", help="run evaluation in local or remote controller cluster")
def _run():
    pass