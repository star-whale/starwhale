from email.policy import default
from typing_extensions import Required
from loguru import logger
import click
from starwhale.consts import DEFAULT_MODEL_YAML_NAME
from starwhale.swmp.model import ModelPackage



@click.group("model", help="StarWhale Model Package(swmp) build/push/pull...")
def model_cmd():
    pass


@model_cmd.command("build", help="build starwhale model package(swmp)")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-f", "--model-yaml", default=DEFAULT_MODEL_YAML_NAME,
              help="mode yaml filename, default use ${workdir}/model.yaml file")
@click.option("--skip-gen-env", is_flag=True,
              help="does not gen conda or venv, only dump config")
def _build(workdir, model_yaml, skip_gen_env):
    ModelPackage.build(workdir, model_yaml, skip_gen_env)


@model_cmd.command("delete", help="Delete swmp from local storage")
def _delete():
    pass


@model_cmd.command("push", help="Push swmp into starwhale controller or hub.starwhale.ai")
def _push():
    pass


@model_cmd.command("pull", help="Pull swmp from starwhale controller or hub.starwhale.ai")
def _pull():
    pass


@model_cmd.command("info", help="Get more info abort local swmp")
def _info():
    pass


@model_cmd.command("list", help="List swmp from local storage")
def _list():
    pass


@model_cmd.command("smoketest", help="Run smoketest for predictor with swmp and swds")
def _smoketest():
    pass


@model_cmd.command("gendep", help="Generate venv or conda by swmp")
def _gendep():
    pass


@model_cmd.command("gc", help="GC useless model package files")
def _gc():
    pass