import click

from starwhale.mngt.user import login, logout
from starwhale.consts import DEFAULT_LOCAL_SW_CONTROLLER_ADDR


def add_mngt_command(cli: click.core.Group) -> None:
    @cli.command("login", help="Login remote StarWhale Controller")
    @click.option("--username", prompt="username")
    @click.password_option(confirmation_prompt=False)
    @click.option(
        "--starwhale",
        prompt="starwhale controller web:",
        default=DEFAULT_LOCAL_SW_CONTROLLER_ADDR,
    )
    def _login(username: str, password: str, starwhale: str) -> None:
        login(username, password, starwhale)

    @cli.command("logout", help="Logout StarWhale Controller")
    def _logout() -> None:
        click.confirm("Do you want to continue?", abort=True)
        logout()

    @cli.command("quickstart", help="StarWhale Quickstart")
    def _quickstart() -> None:
        # TODO: init git repo, add some gitignore
        pass

    @cli.command("autocomplete", help="Generate zsh/bash command auto complete")
    def _autocompete() -> None:
        pass
