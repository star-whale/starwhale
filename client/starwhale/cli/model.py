import click

from starwhale.consts import DEFAULT_MODEL_YAML_NAME
from starwhale.consts.env import SW_ENV
from starwhale.swmp.model import ModelPackage
from starwhale.swmp.store import ModelPackageLocalStore



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
@click.argument("swmp")
def _delete(swmp):
    ModelPackageLocalStore().delete(swmp)


@model_cmd.command("push", help="Push swmp into starwhale controller or hub.starwhale.ai")
@click.argument("swmp")
@click.option("-p", "--project", default="", help="project name, if omit, starwhale will push swmp to your default project")
@click.option("-f", "--force", is_flag=True, help="force push swmp")
def _push(swmp, project, force):
    ModelPackageLocalStore().push(swmp, project, force)


@model_cmd.command("pull", help="Pull swmp from starwhale controller or hub.starwhale.ai")
@click.argument("swmp")
@click.option("-p", "--project", default="", help="project name, if omit, starwhale will push swmp to your default project")
@click.option("-s", "--starwhale", default="", help="starwhale controller server, default is swcli config remote_addr")
@click.option("-f", "--force", is_flag=True, help="force pull swmp")
def _pull(swmp, project, starwhale, force):
    ModelPackageLocalStore().pull(swmp, project, starwhale, force)


@model_cmd.command("info", help="Get more info abort local swmp")
@click.argument("swmp")
def _info(swmp):
    ModelPackageLocalStore().info(swmp)


@model_cmd.command("list", help="List swmp from local storage")
def _list():
    ModelPackageLocalStore().list()


@model_cmd.command("smoketest", help="Run smoketest for predictor with swmp and swds")
def _smoketest():
    pass


@model_cmd.command("gendep", help="Generate venv or conda by swmp")
def _gendep():
    pass


@model_cmd.command("gc", help="GC useless model package files")
@click.option("--dry-run", is_flag=True,
              help="Dry-run swmp gc")
def _gc(dry_run):
    ModelPackageLocalStore().gc(dry_run)


@model_cmd.command("extract", help="Extract local swmp tar file into workdir")
@click.argument("swmp")
@click.option("-f", "--force", is_flag=True, help="force pull swmp")
def _extract(swmp, force):
    ModelPackageLocalStore().extract(swmp, force)


#TODO: combine click option to one func for _ppl and _cmp
@model_cmd.command("ppl", help="Run swmp pipeline")
@click.argument("swmp")
@click.option("-f", "--model-yaml", default=DEFAULT_MODEL_YAML_NAME,
              help="mode yaml filename, default use ${workdir}/model.yaml file")
@click.option("--status-dir", envvar=SW_ENV.STATUS_D, help=f"ppl status dir, env is {SW_ENV.STATUS_D}")
@click.option("--log-dir", envvar=SW_ENV.LOG_D, help=f"ppl log dir, env is {SW_ENV.LOG_D}")
@click.option("--result-dir", envvar=SW_ENV.RESULT_D, help=f"ppl result dir, env is {SW_ENV.RESULT_D}")
@click.option("--input-config", envvar=SW_ENV.INTPUT_CONFIG, help=f"ppl swds config.json path, env is {SW_ENV.INTPUT_CONFIG}")
def _ppl(swmp, model_yaml, status_dir, log_dir, result_dir, input_config):
    #TODO: add local mock input_config
    ModelPackage.ppl(swmp, model_yaml,
                     {"status_dir": status_dir,
                      "log_dir": log_dir,
                      "result_dir": result_dir,
                      "input_config": input_config})

@model_cmd.command("cmp", help="compare inference output with label, then generate result json")
@click.argument("swmp")
@click.option("-f", "--model-yaml", default=DEFAULT_MODEL_YAML_NAME,
              help="mode yaml filename, default use ${workdir}/model.yaml file")
@click.option("--status-dir", envvar=SW_ENV.STATUS_D, help=f"ppl status dir, env is {SW_ENV.STATUS_D}")
@click.option("--log-dir", envvar=SW_ENV.LOG_D, help=f"ppl log dir, env is {SW_ENV.LOG_D}")
@click.option("--result-dir", envvar=SW_ENV.RESULT_D, help=f"ppl result dir, env is {SW_ENV.RESULT_D}")
@click.option("--input-config", envvar=SW_ENV.INTPUT_CONFIG, help=f"ppl swds config.json path, env is {SW_ENV.INTPUT_CONFIG}")
def _cmp(swmp, model_yaml, status_dir, log_dir, result_dir, input_config):
    ModelPackage.cmp(swmp, model_yaml,
                     {"status_dir": status_dir,
                      "log_dir": log_dir,
                      "result_dir": result_dir,
                      "input_config": input_config})

@model_cmd.command("pre-activate", help="Prepare to restore and activate swmp runtime environment")
@click.argument("swmp")
def _pre_activate(swmp):
    #TODO: add auto decompress
    #TODO: set activate.sw path
    ModelPackageLocalStore().pre_activate(swmp)