import click

from .view import InstanceTermView


@click.group(
    "instance",
    help="Starwhale Instance management, login and select standalone or cloud instance",
)
def instance_cmd() -> None:
    pass


@instance_cmd.command("select")
@click.argument("instance")
def _select(instance: str) -> None:
    """
    Select current default instance

    INSTANCE: use alias name to select
    """
    # TODO: support URI select
    InstanceTermView().select(instance)


@instance_cmd.command("login")
@click.argument("instance", default="")
@click.option("--username", prompt="username")
@click.password_option(confirmation_prompt=False)
@click.option("--alias", type=str, help="starwhale instance alias name")
def _login(instance: str, username: str, password: str, alias: str) -> None:
    """Login Starwhale Instance

    * INSTANCE: instance uri, if ignore it, swcli will login current selected instance
    """
    InstanceTermView().login(instance, username, password, alias)


@instance_cmd.command("logout")
@click.argument("instance", default="")
def _logout(instance: str) -> None:
    """
    Logout Starwhale instance

    * INSTANCE: instance alias name or uri, if ignore it, swcli will logout current selected instance
    """
    click.confirm("Do you want to continue?", abort=True)
    InstanceTermView().logout(instance)


@instance_cmd.command("list", help="List logined Starwhale instances")
def _list() -> None:
    InstanceTermView().list()


@instance_cmd.command("info", help="Inspect instance details")
@click.argument("instance", default="")
def _info(instance: str) -> None:
    InstanceTermView().info(instance)
