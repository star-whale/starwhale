import click

from starwhale.consts import DEFAULT_DATASET_YAML_NAME
from starwhale.swds.dataset import DataSet
from starwhale.swds.store import DataSetLocalStore


@click.group("dataset", help="StarWhale DataSet(swds) build/push/convert...")
def dataset_cmd():
    pass


@dataset_cmd.command("build", help="Build swds with dataset.yaml")
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-f", "--dataset-yaml", default=DEFAULT_DATASET_YAML_NAME,
              help="dataset yaml filename, default use ${workdir}/dataset.yaml file")
def _build(workdir, dataset_yaml):
    #TODO: add cmd options for dataset build, another choice for dataset.yaml
    #TODO: add dryrun
    #TODO: add compress args
    DataSet.build(workdir, dataset_yaml)


@dataset_cmd.command("list", help="List local dataset")
def _list():
    DataSetLocalStore().list()


@dataset_cmd.command("push", help="Push swds into starwhale controller")
@click.argument("swds")
def _push(swds):
    DataSetLocalStore().push(swds)


@dataset_cmd.command("info", help="Show dataset details")
@click.argument("swds")
def _info(swds):
    DataSetLocalStore().info(swds)


@dataset_cmd.command("delete", help="Delete dataset in local environment")
@click.argument("swds")
def _delete(swds):
    DataSetLocalStore().delete(swds)


@dataset_cmd.command("gc", help="Delete useless dataset dir")
@click.option("--dry-run", is_flag=True, help="Dry-run swds gc")
def _gc(dry_run):
    DataSetLocalStore().gc(dry_run)