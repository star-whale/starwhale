import click
import loguru


@click.command("login", help="login remote starwhale controller")
def _login():
    pass


@click.command("logout", help="logout, clear auth token")
def _logout():
    pass


@click.command("quickstart", help="quickstart for your starwhale")
def _quickstart():
    pass


@click.command("autocomplete", help="generate zsh/bash command auto complete")
def _autocompete():
    pass