import typing as t

import click

from starwhale.consts import (
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    LOCAL_FUSE_JSON_NAME,
)

from .view import get_term_view, DatasetTermView


@click.group("dataset", help="Dataset management, build/info/list/copy/tag...")
@click.pass_context
def dataset_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@dataset_cmd.command("build", help="[Only Standalone]Build swds with dataset.yaml")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-p", "--project", default="", help="Project URI")
@click.option(
    "-f",
    "--dataset-yaml",
    default=DefaultYAMLName.DATASET,
    help="Dataset yaml filename, default use ${WORKDIR}/dataset.yaml file",
)
@click.pass_obj
def _build(
    view: DatasetTermView, workdir: str, project: str, dataset_yaml: str
) -> None:
    # TODO: add cmd options for dataset build, another choice for dataset.yaml
    # TODO: add dry-run
    # TODO: add compress args
    view.build(workdir, project, dataset_yaml)


@dataset_cmd.command("list", help="List dataset")
@click.option("-p", "--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of dataset version")
@click.option("--show-removed", is_flag=True, help="Show removed datasets")
@click.option("--fullname", is_flag=True, help="show version fullname")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for dataset list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for dataset list"
)
@click.pass_obj
def _list(
    view: DatasetTermView,
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
) -> None:
    view.list(project, fullname, show_removed, page, size)


@dataset_cmd.command("info", help="Show dataset details")
@click.argument("dataset")
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _info(view: t.Type[DatasetTermView], dataset: str, fullname: bool) -> None:
    view(dataset).info(fullname)


@dataset_cmd.command("remove")
@click.argument("dataset")
@click.option("-f", "--force", is_flag=True, help="Force to remove dataset")
@click.pass_obj
def _remove(view: t.Type[DatasetTermView], dataset: str, force: bool) -> None:
    """
    Remove dataset

    You can run `swcli dataset recover` to recover the removed datasets.

    DATASET: argument use the `Dataset URI` format, so you can remove the whole dataset or a specified-version dataset.
    """
    click.confirm("continue to remove?", abort=True)
    view(dataset).remove(force)


@dataset_cmd.command("recover")
@click.argument("dataset")
@click.option("-f", "--force", is_flag=True, help="Force to recover dataset")
@click.pass_obj
def _recover(view: t.Type[DatasetTermView], dataset: str, force: bool) -> None:
    """
    Recover dataset

    DATASET: argument use the `Dataset URI` format, so you can recover the whole dataset or a specified-version dataset.
    """
    view(dataset).recover(force)


@dataset_cmd.command("history", help="Show dataset history")
@click.argument("dataset")
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _history(
    view: t.Type[DatasetTermView], dataset: str, fullname: bool = False
) -> None:
    view(dataset).history(fullname)


@dataset_cmd.command("render-fuse")
@click.argument("target")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help=f"Force to render, if {LOCAL_FUSE_JSON_NAME} was already existed",
)
@click.pass_obj
def _render_fuse(view: t.Type[DatasetTermView], target: str, force: bool) -> None:
    """
    [ONLY Standalone]Render Dataset fuse input.json for standalone ppl

    TARGET: dataset uri or dataset workdir path
    """

    view.render_fuse_json(target, force)


@dataset_cmd.command("copy", help="Copy dataset, standalone <--> cloud")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force copy dataset")
@click.pass_obj
def _copy(view: t.Type[DatasetTermView], src: str, dest: str, force: bool) -> None:
    view.copy(src, dest, force)


@dataset_cmd.command("tag", help="Dataset tag management, add or remove")
@click.argument("dataset")
@click.argument("tags", nargs=-1)
@click.option("-r", "--remove", is_flag=True, help="Remove tags")
@click.option(
    "-q",
    "--quiet",
    is_flag=True,
    help="Ignore tag name errors like name duplication, name absence",
)
@click.pass_obj
def _tag(
    view: t.Type[DatasetTermView],
    dataset: str,
    tags: t.List[str],
    remove: bool,
    quiet: bool,
) -> None:
    view(dataset).tag(tags, remove, quiet)
