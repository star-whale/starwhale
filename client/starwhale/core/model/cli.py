import os

import click

from starwhale.consts import (
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    LOCAL_FUSE_JSON_NAME,
)
from starwhale.base.uri import URI
from starwhale.base.type import URIType, EvalTaskType
from starwhale.consts.env import SWEnv
from starwhale.core.dataset.store import DatasetStorage

from .view import ModelTermView


@click.group("model", help="StarWhale Model Management")
def model_cmd() -> None:
    pass


@model_cmd.command("build", help="[ONLY Standalone]Build starwhale model")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-p", "--project", default="", help="Project URI")
@click.option(
    "-f",
    "--model-yaml",
    default=DefaultYAMLName.MODEL,
    help="mode yaml filename, default use ${workdir}/model.yaml file",
)
def _build(workdir: str, project: str, model_yaml: str) -> None:
    ModelTermView.build(workdir, project, model_yaml)


@model_cmd.command("copy", help="Copy model, stanalone <--> cloud")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="force copy model")
def _copy(src: str, dest: str, force: bool) -> None:
    ModelTermView.copy(src, dest, force)


@model_cmd.command("info", help="Inspect model")
@click.argument("model")
@click.option("--fullname", is_flag=True, help="show version fullname")
def _info(model: str, fullname: bool) -> None:
    ModelTermView(model).info(fullname)


@model_cmd.command("list", help="List Model")
@click.option("--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of model version")
@click.option("--show-removed", is_flag=True, help="Show removed model")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="page number for model list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for model list"
)
def _list(
    project: str, fullname: bool, show_removed: bool, page: int, size: int
) -> None:
    ModelTermView.list(project, fullname, show_removed, page, size)


@model_cmd.command("history", help="Show model history")
@click.argument("model")
@click.option("--fullname", is_flag=True, help="show version fullname")
def _history(model: str, fullname: bool) -> None:
    ModelTermView(model).history(fullname)


@model_cmd.command("remove", help="Remove model")
@click.argument("model")
@click.option("-f", "--force", is_flag=True, help="force to remove model")
def _remove(model: str, force: bool) -> None:
    click.confirm("continue to delete?", abort=True)
    ModelTermView(model).remove(force)


@model_cmd.command("recover", help="Recover model")
@click.argument("model")
@click.option("-f", "--force", is_flag=True, help="force to recover model")
def _recover(model: str, force: bool) -> None:
    ModelTermView(model).recover(force)


@model_cmd.command(
    "extract", help="[ONLY Standalone]Extract local model bundle tar file into workdir"
)
@click.argument("model")
@click.option("-f", "--force", is_flag=True, help="force extract model bundle")
@click.option(
    "--target-dir",
    default="",
    help="extract target dir.if omitted, sw will use starwhale default workdir",
)
def _extract(model: str, force: bool, target_dir: str) -> None:
    ModelTermView(model).extract(force, target_dir)


# TODO: combine click option to one func for _ppl and _cmp
@model_cmd.command("ppl")
@click.argument("target")
@click.option(
    "-f",
    "--model-yaml",
    default=DefaultYAMLName.MODEL,
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
    target: str,
    model_yaml: str,
    status_dir: str,
    log_dir: str,
    result_dir: str,
    input_config: str,
) -> None:
    """
    [ONLY Standalone]Run PPL

    TARGET: model uri or model workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    INPUT_JSON: dataset uri or dataset/input.json uri
    """
    if not os.path.exists(input_config):
        uri = URI(input_config, expected_type=URIType.DATASET)
        store = DatasetStorage(uri)
        input_config = str((store.snapshot_workdir / LOCAL_FUSE_JSON_NAME).absolute())

    # TODO: support render fuse json for cmp test

    ModelTermView.eval(
        target=target,
        yaml_name=model_yaml,
        typ=EvalTaskType.PPL,
        kw={
            "status_dir": status_dir,
            "log_dir": log_dir,
            "result_dir": result_dir,
            "input_config": input_config,
        },
    )


@model_cmd.command("cmp")
@click.argument("target")
@click.option(
    "-f",
    "--model-yaml",
    default=DefaultYAMLName.MODEL,
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
    target: str,
    model_yaml: str,
    status_dir: str,
    log_dir: str,
    result_dir: str,
    input_config: str,
) -> None:
    """
    [ONLY Standalone]Run CMP, compare inference output with label, then generate result json

    TARGET: model uri or model workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    """
    ModelTermView.eval(
        target=target,
        yaml_name=model_yaml,
        typ=EvalTaskType.CMP,
        kw={
            "status_dir": status_dir,
            "log_dir": log_dir,
            "result_dir": result_dir,
            "input_config": input_config,
        },
    )
