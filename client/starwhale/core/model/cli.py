import typing as t
from pathlib import Path

import click

from starwhale.consts import DEFAULT_MODEL_YAML_NAME
from starwhale.consts.env import SWEnv
from .model import ModelPackage
from .store import ModelPackageLocalStore


@click.group("model", help="StarWhale Model Package(swmp) build/push/pull...")
def model_cmd() -> None:
    pass


@model_cmd.command("build", help="build starwhale model package(swmp)")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option(
    "-f",
    "--model-yaml",
    default=DEFAULT_MODEL_YAML_NAME,
    help="mode yaml filename, default use ${workdir}/model.yaml file",
)
def _build(workdir: str, model_yaml: str) -> None:
    ModelPackage.build(workdir, model_yaml)


@model_cmd.command("remove", help="Remove model")
@click.argument("model")
def _remove(model: str) -> None:
    ModelPackageLocalStore().delete(model)


@model_cmd.command("recover", help="Recover model")
@click.argument("model")
def _recover(model: str) -> None:
    pass


@model_cmd.command("copy", help="Copy model")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="force copy model")
def _copy(src: str, dest: str, force: bool) -> None:
    # ModelPackageLocalStore().push(swmp, project, force)
    # ModelPackageLocalStore().pull(swmp, project, starwhale, force)
    pass


@model_cmd.command("info", help="Inspect model(swmp)")
@click.argument("model")
def _info(model: str) -> None:
    ModelPackageLocalStore().info(model)


@model_cmd.command("list", help="List model(swmp)")
@click.option("--fullname", is_flag=True, help="Show fullname of swmp version")
def _list(fullname: bool) -> None:
    ModelPackageLocalStore().list(fullname=fullname)


@model_cmd.command("eval", help="Create model(swmp) evaluation")
def _eval() -> None:
    pass


@model_cmd.command("tag", help="model(swmp) tag management")
@click.argument("model")
@click.option("-r", "--remove", is_flag=True, help="remove tag")
@click.option(
    "-t",
    "--tag",
    required=True,
    multiple=True,
    help="tag, one or more, splitted by comma",
)
def _tag(model, remove, tag) -> None:
    pass


@model_cmd.command("history", help="Show model history")
@click.argument("model")
def _history(model: str) -> None:
    pass


@model_cmd.command("revert", help="Revert model")
@click.argument("model")
def _revert(model: str) -> None:
    pass


@model_cmd.command("extract", help="Extract local swmp tar file into workdir")
@click.argument("swmp")
@click.option("-f", "--force", is_flag=True, help="force extract swmp")
@click.option(
    "-t",
    "--target",
    type=click.Path(),
    default=None,
    help="extract target dir.if omitted, sw will use starwhale default workdir",
)
def _extract(swmp: str, force: bool, target: t.Optional[Path]) -> None:
    ModelPackageLocalStore().extract(swmp, force, target)


# TODO: combine click option to one func for _ppl and _cmp
@model_cmd.command("ppl", help="Run swmp pipeline")
@click.argument("swmp")
@click.option(
    "-f",
    "--model-yaml",
    default=DEFAULT_MODEL_YAML_NAME,
    help="mode yaml filename, default use ${workdir}/model.yaml file",
)
@click.option(
    "--status-dir",
    envvar=SWEnv.status_dir,
    help=f"ppl status dir, env is {SWEnv.status_dir}",
)
@click.option(
    "--log-dir", envvar=SWEnv.log_dir, help=f"ppl log dir, env is {SWEnv.log_dir}"
)
@click.option(
    "--result-dir",
    envvar=SWEnv.result_dir,
    help=f"ppl result dir, env is {SWEnv.result_dir}",
)
@click.option(
    "--input-config",
    envvar=SWEnv.input_config,
    help=f"ppl swds config.json path, env is {SWEnv.input_config}",
)
def _ppl(
    swmp: str,
    model_yaml: str,
    status_dir: str,
    log_dir: str,
    result_dir: str,
    input_config: str,
) -> None:
    ModelPackage.ppl(
        swmp,
        model_yaml,
        {
            "status_dir": status_dir,
            "log_dir": log_dir,
            "result_dir": result_dir,
            "input_config": input_config,
        },
    )


@model_cmd.command(
    "cmp", help="compare inference output with label, then generate result json"
)
@click.argument("swmp")
@click.option(
    "-f",
    "--model-yaml",
    default=DEFAULT_MODEL_YAML_NAME,
    help="mode yaml filename, default use ${workdir}/model.yaml file",
)
@click.option(
    "--status-dir",
    envvar=SWEnv.status_dir,
    help=f"ppl status dir, env is {SWEnv.status_dir}",
)
@click.option(
    "--log-dir", envvar=SWEnv.log_dir, help=f"ppl log dir, env is {SWEnv.log_dir}"
)
@click.option(
    "--result-dir",
    envvar=SWEnv.result_dir,
    help=f"ppl result dir, env is {SWEnv.result_dir}",
)
@click.option(
    "--input-config",
    envvar=SWEnv.input_config,
    help=f"ppl swds config.json path, env is {SWEnv.input_config}",
)
def _cmp(
    swmp: str,
    model_yaml: str,
    status_dir: str,
    log_dir: str,
    result_dir: str,
    input_config: str,
) -> None:
    ModelPackage.cmp(
        swmp,
        model_yaml,
        {
            "status_dir": status_dir,
            "log_dir": log_dir,
            "result_dir": result_dir,
            "input_config": input_config,
        },
    )
