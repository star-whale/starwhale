import click
from loguru import logger


@click.group("predictor", help="Manage remote StarWhale Controller Job/Task/Predictor...")
def predictor_cmd():
    pass


@predictor_cmd.command("list", help="list remote starwhale controller job")
def _list():
    pass


@predictor_cmd.command("info", help="inspect job info with job id")
def _info():
    pass


@predictor_cmd.command("task", help="inspect task info with task id")
def _task_info():
    pass