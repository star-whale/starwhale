import os
import typing as t
from pathlib import Path

import click

from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI
from starwhale.base.type import URIType
from starwhale.utils.cli import AliasedGroup
from starwhale.utils.load import import_object
from starwhale.utils.error import NotFoundError
from starwhale.core.dataset.type import MIMEType, DatasetAttr, DatasetConfig

from .view import get_term_view, DatasetTermView


@click.group(
    "dataset",
    cls=AliasedGroup,
    help="Dataset management, build/info/list/copy/tag...",
)
@click.pass_context
def dataset_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@dataset_cmd.command("build", help="[Only Standalone]Build swds with dataset.yaml")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option(
    "-h",
    "--handler",
    help="Dataset build executor handler: [module path]:[class or function name]",
)
@click.option("-n", "--name", help="Dataset name")
@click.option("-p", "--project", help="Project URI")
@click.option("--desc", help="Dataset description")
@click.option(
    "-as",
    "--alignment-size",
    help="swds-bin format dataset: alignment size",
)
@click.option(
    "-vs",
    "--volume-size",
    help="swds-bin format dataset: volume size",
)
@click.option("-dmt", "--data-mime-type", help="Dataset global default data mime type")
@click.option(
    "-f",
    "--dataset-yaml",
    default=DefaultYAMLName.DATASET,
    help="Dataset yaml filename, default use ${WORKDIR}/dataset.yaml file",
)
@click.option("-a", "--append", is_flag=True, default=None, help="Only append new data")
@click.option("-af", "--append-from", help="Append from dataset version")
@click.option("-r", "--runtime", help="runtime uri")
@click.option("-dcs", "--disable-copy-src", help="disable copy src dir")
@click.pass_obj
def _build(
    view: DatasetTermView,
    workdir: str,
    handler: str,
    name: str,
    project: str,
    desc: str,
    dataset_yaml: str,
    alignment_size: str,
    volume_size: str,
    data_mime_type: str,
    append: bool,
    append_from: str,
    runtime: str,
    disable_copy_src: bool,
) -> None:
    # TODO: add dry-run
    # TODO: add compress args
    if not os.path.exists(workdir):
        raise NotFoundError(workdir)

    yaml_path = Path(workdir) / dataset_yaml
    config = DatasetConfig()
    if yaml_path.exists():
        config = DatasetConfig.create_by_yaml(yaml_path)

    config.name = name or config.name or Path(workdir).absolute().name
    handler = handler or config.handler
    config.handler = import_object(workdir, handler)
    config.runtime_uri = runtime or config.runtime_uri
    config.project_uri = project or config.project_uri
    # TODO: support README.md as the default desc
    config.desc = desc or config.desc
    config.append_from = append_from or config.append_from

    config.attr = DatasetAttr(
        volume_size=volume_size or config.attr.volume_size,
        alignment_size=alignment_size or config.attr.alignment_size,
        data_mime_type=MIMEType(data_mime_type or config.attr.data_mime_type),
    )

    if append is not None:
        config.append = append

    config.do_validate()
    view.build(workdir, config, disable_copy_src)


@dataset_cmd.command("diff", help="Dataset version diff")
@click.argument("base_uri", required=True)
@click.argument("compare_uri", required=True)
@click.option(
    "--show-details", is_flag=True, help="Show data different detail by the row"
)
@click.pass_obj
def _diff(
    view: t.Type[DatasetTermView], base_uri: str, compare_uri: str, show_details: bool
) -> None:
    view(base_uri).diff(URI(compare_uri, expected_type=URIType.DATASET), show_details)


@dataset_cmd.command("list", aliases=["ls"])
@click.option("-p", "--project", default="", help="Project URI")
@click.option("-f", "--fullname", is_flag=True, help="Show fullname of dataset version")
@click.option("-sr", "--show-removed", is_flag=True, help="Show removed datasets")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for dataset list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for dataset list"
)
@click.option(
    "filters",
    "-fl",
    "--filter",
    multiple=True,
    help="Filter output based on conditions provided.",
)
@click.pass_obj
def _list(
    view: DatasetTermView,
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
    filters: list,
) -> None:
    """
    List Dataset

    The filtering flag (-fl or --filter) format is a key=value pair or a flag.
    If there is more than one filter, then pass multiple flags.\n
    (e.g. --filter name=mnist --filter latest)

    \b
    The currently supported filters are:
      name\tTEXT\tThe prefix of the dataset name
      owner\tTEXT\tThe name or id of the dataset owner
      latest\tFLAG\t[Cloud] Only show the latest version
            \t \t[Standalone] Only show the version with "latest" tag
    """
    view.list(project, fullname, show_removed, page, size, filters)


@dataset_cmd.command("info", help="Show dataset details")
@click.argument("dataset")
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _info(view: t.Type[DatasetTermView], dataset: str, fullname: bool) -> None:
    view(dataset).info(fullname)


@dataset_cmd.command("remove", aliases=["rm"])
@click.argument("dataset")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help="Force to remove dataset, the removed dataset cannot recover",
)
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


@dataset_cmd.command("summary", help="Show dataset summary")
@click.argument("dataset")
@click.pass_obj
def _summary(view: t.Type[DatasetTermView], dataset: str) -> None:
    view(dataset).summary()


@dataset_cmd.command("copy", aliases=["cp"])
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force copy dataset")
@click.option("-dlp", "--dest-local-project", help="dest local project uri")
def _copy(src: str, dest: str, force: bool, dest_local_project: str) -> None:
    """
    Copy Dataset between Standalone Instance and Cloud Instance

    SRC: dataset uri with version

    DEST: project uri or dataset uri with name.

    Example:

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local project(myproject) with a new dataset name 'mnist-local'
            swcli dataset cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq local/project/myproject/mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local default project(self) with the cloud instance dataset name 'mnist-cloud'
            swcli dataset cp cloud://pre-k8s/project/dataset/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq .

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local project(myproject) with the cloud instance dataset name 'mnist-cloud'
            swcli dataset cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq . -dlp myproject

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local default project(self) with a dataset name 'mnist-local'
            swcli dataset cp cloud://pre-k8s/project/dataset/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local project(myproject) with a dataset name 'mnist-local'
            swcli dataset cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local -dlp myproject

        \b
        - copy standalone instance(local) default project(self)'s mnist-local dataset to cloud instance(pre-k8s) mnist project with a new dataset name 'mnist-cloud'
            swcli dataset cp mnist-local/version/latest cloud://pre-k8s/project/mnist/mnist-cloud

        \b
        - copy standalone instance(local) default project(self)'s mnist-local dataset to cloud instance(pre-k8s) mnist project with standalone instance dataset name 'mnist-local'
            swcli dataset cp mnist-local/version/latest cloud://pre-k8s/project/mnist

        \b
        - copy standalone instance(local) project(myproject)'s mnist-local dataset to cloud instance(pre-k8s) mnist project with standalone instance dataset name 'mnist-local'
            swcli dataset cp local/project/myproject/dataset/mnist-local/version/latest cloud://pre-k8s/project/mnist
    """
    DatasetTermView.copy(src, dest, force, dest_local_project)


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


@dataset_cmd.command("head", help="Print the first 10 rows of the dataset")
@click.argument("dataset")
@click.option("-n", "--rows", default=5, help="Print the first NUM rows of the dataset")
@click.option(
    "-d",
    "--show-raw-data",
    is_flag=True,
    help="Fetch raw data content",
)
@click.pass_obj
def _head(
    view: t.Type[DatasetTermView],
    dataset: str,
    rows: int,
    show_raw_data: bool,
) -> None:
    view(dataset).head(rows, show_raw_data)
