from email.policy import default
from typing_extensions import Required
from loguru import logger
import click
from starwhale.consts import DEFAULT_MODEL_YAML_NAME
from starwhale.swmp.model import ModelPackage


@click.group("model")
def model_cmd():
    pass


@model_cmd.command("build", help="build starwhale model package(swmp)")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False),
                help="swmp build workdir")
@click.option("-f", "--model-yaml", type=click.Path(exists=True), default=DEFAULT_MODEL_YAML_NAME,
              help="mode yaml path, default use current dir model.yaml file")
@click.option()
def _build(workdir, model_yaml):
    ModelPackage.build(workdir, model_yaml)


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


@model_cmd.command("smoketest",
                   help="run smoketest for predictor with swmp and swds")
def _smoketest():
    pass