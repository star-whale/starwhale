from loguru import logger
import click


@click.group("model")
def model_cmd():
    pass


@model_cmd.command("build", help="build starwhale model package(swmp)")
def _build():
    pass


@model_cmd.command("delete", help="delete swmp from local storage")
def _delete():
    pass


@model_cmd.command("push", help="push swmp into starwhale controller or hub")
def _push():
    pass


@model_cmd.command("pull", help="pull swmp from starwhale controller or hub")
def _pull():
    pass


@model_cmd.command("info", help="get more info abort local swmp")
def _info():
    pass


@model_cmd.command("list", help="list swmp from local storage")
def _list():
    pass


@model_cmd.command("smoketest", help="run smoketest for predictor with swmp and swds")
def _smoketest():
    pass