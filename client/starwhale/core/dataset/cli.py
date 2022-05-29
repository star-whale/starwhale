import click

from starwhale.consts import (
    LOCAL_FUSE_JSON_NAME,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
)
from .view import DatasetTermView


@click.group("dataset", help="StarWhale Dataset Management")
def dataset_cmd() -> None:
    pass


@dataset_cmd.command("build", help="Build swds with dataset.yaml")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-p", "--project", default="", help="Project URI")
@click.option(
    "-f",
    "--dataset-yaml",
    default=DefaultYAMLName.DATASET,
    help="dataset yaml filename, default use ${workdir}/dataset.yaml file",
)
def _build(workdir: str, project: str, dataset_yaml: str) -> None:
    # TODO: add cmd options for dataset build, another choice for dataset.yaml
    # TODO: add dryrun
    # TODO: add compress args
    DatasetTermView.build(workdir, project, dataset_yaml)


@dataset_cmd.command("list", help="List dataset")
@click.option("-p", "--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of dataset version")
@click.option("--show-removed", is_flag=True, help="Show removed dataset")
@click.option("--fullname", is_flag=True, help="show version fullname")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="page number for dataset list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for dataset list"
)
def _list(
    project: str, fullname: bool, show_removed: bool, page: int, size: int
) -> None:
    DatasetTermView.list(project, fullname, show_removed, page, size)


@dataset_cmd.command("info", help="Show dataset details")
@click.argument("dataset")
@click.option("--fullname", is_flag=True, help="show version fullname")
def _info(dataset: str, fullname: bool) -> None:
    DatasetTermView(dataset).info(fullname)


@dataset_cmd.command("remove", help="Remove dataset")
@click.argument("dataset")
@click.option("-f", "--force", is_flag=True, help="force to recover dataset")
def _remove(dataset: str, force: bool) -> None:
    DatasetTermView(dataset).remove(force)


@dataset_cmd.command("recover", help="Recover dataset")
@click.argument("dataset")
@click.option("-f", "--force", is_flag=True, help="force to recover dataset")
def _recover(dataset: str, force: bool) -> None:
    DatasetTermView(dataset).recover(force)


@dataset_cmd.command("history", help="Show dataset history")
@click.argument("dataset")
@click.option("--fullname", is_flag=True, help="show version fullname")
def _history(dataset: str, fullname: bool = False) -> None:
    DatasetTermView(dataset).history(fullname)


@dataset_cmd.command("render-fuse", help="Render fuse input.json for local swds")
@click.argument("target")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help=f"Force to render, if {LOCAL_FUSE_JSON_NAME} was already existed",
)
def _render_fuse(target: str, force: bool) -> None:
    """
    [ONLY Standalone]Render Dataset fuse input.json for standalone ppl

    TARGET: dataset uri or dataset workdir path
    """

    DatasetTermView.render_fuse_json(target, force)


@dataset_cmd.command("copy", help="Copy dataset")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="force copy dataset")
def _copy(src: str, dest: str, force: bool) -> None:
    DatasetTermView.copy(src, dest, force)
