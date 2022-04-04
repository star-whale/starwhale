import click
import codecs

from starwhale.mngt.user import login, logout
from starwhale.consts import DEFAULT_LOCAL_SW_CONTROLLER_ADDR


def add_mngt_command(cli):
    @cli.command("login", help="Login remote StarWhale Controller")
    @click.option("--username", prompt="username")
    @click.password_option(confirmation_prompt=False)
    @click.option("--starwhale", prompt="starwhale controller web:", default=DEFAULT_LOCAL_SW_CONTROLLER_ADDR)
    def _login(username, password, starwhale):
        password = codecs.encode(password, "rot13")
        login(username, password, starwhale)

    @cli.command("logout", help="Logout StarWhale Controller")
    def _logout():
        click.confirm("Do you want to continue?", abort=True)
        logout()

    @cli.command("quickstart", help="StarWhale Quickstart")
    def _quickstart():
        #TODO: init git repo, add some gitignore
        pass

    @cli.command("autocomplete", help="Generate zsh/bash command auto complete")
    def _autocompete():
        pass

