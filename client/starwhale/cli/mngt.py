import click


def add_mngt_command(cli: click.core.Group) -> None:
    @cli.command("quickstart", help="StarWhale Quickstart")
    def _quickstart() -> None:
        # TODO: init git repo, add some gitignore
        pass

    @cli.command("autocomplete", help="Generate zsh/bash command auto complete")
    def _autocompete() -> None:
        pass

    @cli.command("gc", help="Garbage collection")
    @click.option("--dry-run", is_flag=False, help="dry-run cleanup garbage collection")
    def _gc(dry_run: bool) -> None:
        pass
