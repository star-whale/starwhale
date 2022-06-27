import time
import random

import click

from starwhale import __version__
from starwhale.utils.debug import init_logger
from starwhale.core.job.cli import job_cmd
from starwhale.utils.config import load_swcli_config
from starwhale.core.model.cli import model_cmd
from starwhale.core.dataset.cli import dataset_cmd
from starwhale.core.project.cli import project_cmd
from starwhale.core.runtime.cli import runtime_cmd
from starwhale.core.instance.cli import instance_cmd

from .mngt import add_mngt_command


def create_sw_cli() -> click.core.Group:
    @click.group()
    @click.version_option(version=__version__)
    @click.option(
        "-v",
        "--verbose",
        count=True,
        help="Show verbose log, support multi counts for v args. More v args, more logs.",
    )
    @click.option("-o", "--output", help="Output format", type=click.Choice(["json"]))
    @click.pass_context
    def cli(ctx: click.Context, verbose: bool, output: str) -> None:
        load_swcli_config()
        init_logger(verbose)
        ctx.ensure_object(dict)
        ctx.obj["output"] = output

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
