import click
import random
import time

from starwhale import __version__
from starwhale.consts import SW_CLI_CONFIG, ENV_SW_CLI_CONFIG
from starwhale.utils.config import load_swcli_config, load_swcli_config
from starwhale.utils.debug import init_logger

from .model import model_cmd
from .dataset import dataset_cmd
from .mngt import add_mngt_command
from .eval import eval_cmd
from .cluster import cluster_cmd
from .project import project_cmd


def create_sw_cli():
    @click.group()
    @click.version_option(version=__version__)
    @click.option(
        "-v",
        "--verbose",
        count=True,
        help="verbose for log"
    )
    def cli(verbose):
        load_swcli_config()
        init_logger(verbose)

    random.seed(time.time_ns)

    cli.add_command(model_cmd)
    cli.add_command(dataset_cmd)
    cli.add_command(eval_cmd)
    cli.add_command(cluster_cmd)
    cli.add_command(project_cmd)
    add_mngt_command(cli)

    return cli


cli = create_sw_cli()

if __name__ == "__main__":
    cli()