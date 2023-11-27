import click

from starwhale.utils.cli import AliasedGroup
from starwhale.cli.debug.datastore import datastore


@click.group("debug", cls=AliasedGroup, help="debug cmds")
def debug_cmd() -> None:
    ...


debug_cmd.add_command(datastore)
