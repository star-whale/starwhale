import click

from rich import print as rprint


def add_mngt_command(cli: click.core.Group) -> None:
    @cli.command("gc", help="Garbage collection")
    @click.option("--dry-run", is_flag=False, help="dry-run cleanup garbage collection")
    def _gc(dry_run: bool) -> None:
        pass

    @cli.command("tui", help="web ui or terminal ui")
    def _ui() -> None:
        # TODO: add fullscreen terminal ui base on rich and textual lib
        rprint(
            ":shell::clap::man_with_probing_cane: TUI(terminal ui) is comming soon..."
        )
