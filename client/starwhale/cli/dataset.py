import click
from loguru import logger


@click.group("ds", help="dataset")
def dataset_cmd():
    pass


@dataset_cmd.command("package", help="build swds with dataset.yaml")
def _package():
    pass


@dataset_cmd.command("push", help="push swds into starwhale controller")
def _push():
    pass


@dataset_cmd.command("index", help="build index for dataset")
def _index():
    pass


@dataset_cmd.command("label", help="try to convert label to starwhale label format")
def _label():
    pass