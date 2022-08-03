import click

from starwhale.mngt import gc, check, open_web


def add_mngt_command(cli: click.core.Group) -> None:
    @cli.command("gc", help="Standalone garbage collection")
    @click.option("--dry-run", is_flag=True, help="Dry-run cleanup garbage collection")
    @click.option("--yes", is_flag=True, help="All confirm yes")
    def _gc(dry_run: bool, yes: bool) -> None:
        gc(dry_run, yes)

    @cli.command("ui", help="Open instance web ui")
    @click.argument("instance", default="")
    def _ui(instance: str) -> None:
        open_web(instance)

    @cli.command("check", help="Check external dependency software")
    def _check() -> None:
        check()
