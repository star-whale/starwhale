from importlib.resources import path
import click
from loguru import logger

from starwhale import __version__
from starwhale.consts import SW_CLI_CONFIG, ENV_SW_CLI_CONFIG
from starwhale.consts.config import load_global_config
from starwhale.utils.debug import set_debug_mode

from .model import model_cmd
from .dataset import dataset_cmd
from .mngt import mngt_cmd
from .predictor import predictor_cmd


def create_sw_cli():
    @click.group()
    @click.version_option(version=__version__)
    @click.option(
        "--debug",
        default=False,
        help="output more debug info."
    )
    @click.option(
        "--config",
        default=SW_CLI_CONFIG,
        type=click.Path(exists=True),
        help="specify custom config path",
        envvar=ENV_SW_CLI_CONFIG
    )
    def cli(debug, config):
        load_global_config(config)
        set_debug_mode(debug)

    cli.add_command(model_cmd)
    cli.add_command(dataset_cmd)
    cli.add_command(mngt_cmd)
    cli.add_command(predictor_cmd)

    return cli

cli = create_sw_cli()

if __name__ == "__main__":
    cli()