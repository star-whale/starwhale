import time
import random

import click

from starwhale.version import STARWHALE_VERSION
from starwhale.utils.cli import AliasedGroup
from starwhale.utils.debug import init_logger
from starwhale.utils.config import load_swcli_config
from starwhale.core.eval.cli import eval_job_cmd
from starwhale.core.model.cli import model_cmd
from starwhale.cli.board.board import open_board
from starwhale.core.dataset.cli import dataset_cmd
from starwhale.core.project.cli import project_cmd
from starwhale.core.runtime.cli import runtime_cmd
from starwhale.core.instance.cli import instance_cmd

from .mngt import add_mngt_command
from .completion import completion_cmd


def create_sw_cli() -> click.core.Group:
    @click.group(cls=AliasedGroup)
    @click.version_option(version=STARWHALE_VERSION)
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

    random.seed(time.time_ns())

    cli.add_command(instance_cmd)
    cli.add_command(project_cmd, aliases=["prj"])  # type: ignore
    cli.add_command(eval_job_cmd)
    cli.add_command(runtime_cmd, aliases=["rt"])  # type: ignore
    cli.add_command(model_cmd, aliases=["mp"])  # type: ignore
    cli.add_command(dataset_cmd, aliases=["ds"])  # type: ignore
    cli.add_command(open_board)
    cli.add_command(completion_cmd)
    add_mngt_command(cli)

    return cli


cli = create_sw_cli()

if __name__ == "__main__":
    cli()
