import click

from starwhale.utils import config
from starwhale.utils.cli import AliasedGroup


@click.group(
    "config",
    cls=AliasedGroup,
    help="Configuration management",
)
def config_cmd() -> None:
    pass


@config_cmd.command("edit", aliases=["e"], help="edit the configuration of swcli")
def _edit() -> None:
    config.edit_from_shell()
