import click

from starwhale.utils import config
from starwhale.utils.cli import AliasedGroup


@click.group(
    "config",
    cls=AliasedGroup,
    help="Configuration management, edit is supported now",
)
def config_cmd() -> None:
    pass


@config_cmd.command("edit", aliases=["e"], help="edit the configuration of swlci")
def __edit() -> None:
    config.edit_from_shell()
