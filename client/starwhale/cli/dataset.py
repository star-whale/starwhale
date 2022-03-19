import click
from loguru import logger


@click.group("dataset", help="StarWhale DataSet(swds) build/push/convert...")
def dataset_cmd():
    pass


@dataset_cmd.command("package", help="Build swds with dataset.yaml")
def _package():
    pass


@dataset_cmd.command("push", help="Push swds into starwhale controller")
def _push():
    pass


@dataset_cmd.command("index", help="Build index for dataset")
def _index():
    pass


@dataset_cmd.command("label", help="Convert label to starwhale label format")
def _label():
    pass