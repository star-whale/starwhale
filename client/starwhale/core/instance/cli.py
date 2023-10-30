import typing as t

import click

from starwhale.utils.cli import AliasedGroup

from .view import get_term_view, InstanceTermView


@click.group(
    "instance",
    cls=AliasedGroup,
    help="Starwhale Instance management, login and select standalone or cloud instance",
)
@click.pass_context
def instance_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@instance_cmd.command("select", aliases=["use"])
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
@click.option("--username")
@click.option("--password")
@click.option("--token", help="Login token")
@click.option("--alias", type=str, help="Starwhale instance alias name", required=True)
def _login(instance: str, username: str, password: str, token: str, alias: str) -> None:
    """Login Starwhale Instance

    * INSTANCE: Instance URI, if ignore it, swcli will login current selected instance.
    """
    if not bool(password and username) ^ bool(token):
        click.echo("credential or token not provided, will open browser to login")
        InstanceTermView().login_with_browser(instance, alias)
        return

    if token:
        kw = {"token": token}
    else:
        kw = {"username": username, "password": password}

    InstanceTermView().login(instance, alias, **kw)


@instance_cmd.command("logout")
@click.argument("instance", default="")
def _logout(instance: str) -> None:
    """
    Logout Starwhale instance

    * INSTANCE: instance alias name or uri, if ignore it, swcli will logout current selected instance.
    """
    click.confirm("Do you want to continue?", abort=True)
    InstanceTermView().logout(instance)


@instance_cmd.command("list", aliases=["ls"], help="List login Starwhale instances")
@click.pass_obj
def _list(view: t.Type[InstanceTermView]) -> None:
    view().list()


@instance_cmd.command("info", help="Show instance details")
@click.argument("instance", default="")
@click.pass_obj
def _info(view: t.Type[InstanceTermView], instance: str) -> None:
    view().info(instance)
