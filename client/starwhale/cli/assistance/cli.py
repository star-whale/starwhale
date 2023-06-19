from typing import Tuple

import click

from starwhale.utils import console
from starwhale.utils.cli import AliasedGroup

from .host import CommandRetriever
from .remote import CommandRunner
from .wait_console import start


@click.group("assistance", cls=AliasedGroup, help="Remote assistance")
def assistance_cmd() -> None:
    pass


@assistance_cmd.command(
    "host", help="Run a remote assistance host to accept remote commands"
)
@click.option("--broker", help="Broker URL", required=True)
def host(broker: str) -> None:
    retriever = CommandRetriever("host", broker)
    retriever.start()
    try:
        retriever.join()
    except KeyboardInterrupt:
        console.print("stopping...")
        retriever.stop()
        retriever.join()


@assistance_cmd.command(
    "remote",
    context_settings=dict(
        ignore_unknown_options=True,
    ),
    help="Send commands to remote assistance host",
)
@click.option("--broker", help="Broker URL", required=True)
@click.argument("args", nargs=-1, required=True)
def remote(broker: str, args: Tuple[str]) -> None:
    runner = CommandRunner("remote", broker, list(args))
    runner.start()
    try:
        runner.join()
    except KeyboardInterrupt:
        console.print("stopping...")
        runner.stop()
        runner.join()


@assistance_cmd.command(
    "wait-console",
    help="Wait for Starwhale Console to connect to the standalone instance",
)
def wait_console() -> None:
    start()
