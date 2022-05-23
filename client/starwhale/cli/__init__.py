import random
import time

import click

from starwhale import __version__
from starwhale.utils.config import load_swcli_config
from starwhale.utils.debug import init_logger

from starwhale.project.cli import project_cmd
from starwhale.job.cli import job_cmd
from starwhale.instance.cli import instance_cmd
from starwhale.runtime.cli import runtime_cmd
from starwhale.dataset.cli import dataset_cmd
from starwhale.model.cli import model_cmd

from .mngt import add_mngt_command


def create_sw_cli() -> click.core.Group:
    @click.group()
    @click.version_option(version=__version__)
    @click.option("-v", "--verbose", count=True, help="verbose for log")
    def cli(verbose: bool) -> None:
        load_swcli_config()
        init_logger(verbose)

    random.seed(time.time_ns)

    cli.add_command(instance_cmd)
    cli.add_command(project_cmd)
    cli.add_command(job_cmd)
    cli.add_command(runtime_cmd)
    cli.add_command(model_cmd)
    cli.add_command(dataset_cmd)
    add_mngt_command(cli)

    return cli


cli = create_sw_cli()

if __name__ == "__main__":
    cli()
