import click
import loguru

@click.group("")
def mngt_cmd():
    pass

@mngt_cmd.command("login", help="login remote starwhale controller")
def _login():
    pass


@mngt_cmd.command("logout", help="logout, clear auth token")
def _logout():
    pass


@mngt_cmd.command("quickstart", help="quickstart for your starwhale")
def _quickstart():
    pass


@mngt_cmd.command("autocomplete", help="generate zsh/bash command auto complete")
def _autocompete():
    pass