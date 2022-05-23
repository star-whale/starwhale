import click

from starwhale.consts import DEFAULT_DATASET_YAML_NAME, LOCAL_FUSE_JSON_NAME
from .dataset import DataSet
from .store import DataSetLocalStore


@click.group("dataset", help="StarWhale DataSet(swds) build/push/convert...")
def dataset_cmd() -> None:
    pass


@dataset_cmd.command("build", help="Build swds with dataset.yaml")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option(
    "-f",
    "--dataset-yaml",
    default=DEFAULT_DATASET_YAML_NAME,
    help="dataset yaml filename, default use ${workdir}/dataset.yaml file",
)
def _build(workdir: str, dataset_yaml: str) -> None:
    # TODO: add cmd options for dataset build, another choice for dataset.yaml
    # TODO: add dryrun
    # TODO: add compress args
    DataSet.build(workdir, dataset_yaml)


@dataset_cmd.command("list", help="List local dataset")
@click.option("--fullname", is_flag=True, help="Show fullname of swmp version")
@click.option("-p", "--project", default="", help="Project URI")
def _list(fullname: bool, project: str) -> None:
    DataSetLocalStore().list(fullname=fullname)


@dataset_cmd.command("info", help="Show dataset details")
@click.argument("swds")
def _info(swds: str) -> None:
    DataSetLocalStore().info(swds)


@dataset_cmd.command("remove", help="Remove dataset")
@click.argument("dataset")
def _remove(dataset: str) -> None:
    DataSetLocalStore().delete(dataset)


@dataset_cmd.command("recover", help="Recover dataset")
@click.argument("dataset")
def _recover(dataset: str) -> None:
    pass


@dataset_cmd.command("tag", help="dataset(swds) tag management")
@click.argument("dataset")
@click.option("-r", "--remove", is_flag=True, help="remove tag")
@click.option(
    "-t",
    "--tag",
    required=True,
    multiple=True,
    help="tag, one or more, splitted by comma",
)
def _tag(dataset, remove, tag) -> None:
    pass


@dataset_cmd.command("history", help="Show dataset history")
@click.argument("dataset")
def _history(dataset: str) -> None:
    pass


@dataset_cmd.command("revert", help="Revert dataset")
@click.argument("model")
def _revert(model: str) -> None:
    pass


@dataset_cmd.command("render-fuse", help="Render fuse input.json for local swds")
@click.argument("swds")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help=f"Force to render, if {LOCAL_FUSE_JSON_NAME} was already existed",
)
def _render_fuse(swds: str, force: bool) -> None:
    DataSet.render_fuse_json(swds, force)


@dataset_cmd.command("copy", help="Copy dataset")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="force copy dataset")
def _copy(src: str, dest: str, force: bool) -> None:
    # DataSetLocalStore().push(swds, project, force)
    pass
