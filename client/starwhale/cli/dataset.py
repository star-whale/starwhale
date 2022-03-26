import click
from loguru import logger

from starwhale.consts import DEFAULT_DATASET_YAML_NAME
from starwhale.swds.dataset import DataSet


@click.group("dataset", help="StarWhale DataSet(swds) build/push/convert...")
def dataset_cmd():
    pass


@dataset_cmd.command("build", help="Build swds with dataset.yaml")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-f", "--dataset-yaml", default=DEFAULT_DATASET_YAML_NAME,
              help="dataset yaml filename, default use ${workdir}/dataset.yaml file")
@click.option("--dry-run", is_flag=True, help="Dry-run swds build")
def _build(workdir, dataset_yaml, dry_run):
    #TODO: add cmd options for dataset build, another choice for dataset.yaml
    DataSet.build(workdir, dataset_yaml, dry_run)


@dataset_cmd.command("list", help="List local dataset")
def _list():
    DataSet.list()

@dataset_cmd.command("push", help="Push swds into starwhale controller")
@click.argument("swds")
def _push(swds):
    DataSet.push(swds)

@dataset_cmd.command("info", help="Show dataset details")
@click.argument("swds")
def _info(swds):
    DataSet.info(swds)