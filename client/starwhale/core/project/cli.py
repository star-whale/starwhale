import typing as t

import click

from starwhale.utils.cli import AliasedGroup

from .view import get_term_view, ProjectTermView, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE


@click.group(
    "project",
    cls=AliasedGroup,
    help="Project management, for standalone and server instances",
)
@click.pass_context
def project_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@project_cmd.command(
    "list", aliases=["ls"], help="List projects in current Starwhale Instance"
)
@click.option("-i", "--instance", default="", help="instance uri")
@click.option(
    "--page",
    type=int,
    default=DEFAULT_PAGE_IDX,
    help="page number for projects list",
)
@click.option(
    "--size",
    type=int,
    default=DEFAULT_PAGE_SIZE,
    help="page size for projects list",
)
@click.pass_obj
def _list(view: t.Type[ProjectTermView], instance: str, page: int, size: int) -> None:
    view.list(instance, page, size)


@project_cmd.command(
    "create",
    aliases=["new", "add"],
    help="Create a new project in current Starwhale instance",
)
@click.argument("project", type=str)
def _create(project: str) -> None:
    ProjectTermView(project).create()


@project_cmd.command(
    "select", aliases=["use"], help="Select default project in current instance"
)
@click.argument("project", type=str)
def _select(project: str) -> None:
    ProjectTermView(project).select()


@project_cmd.command("remove", aliases=["rm"], help="Remove project")
@click.argument("project", type=str)
def _remove(project: str) -> None:
    ProjectTermView(project).remove()


@project_cmd.command("recover", help="Recover project")
@click.argument("project", type=str)
def _recover(project: str) -> None:
    ProjectTermView(project).recover()


@project_cmd.command("info", help="Inspect project")
@click.argument("project", type=str)
@click.pass_obj
def _info(view: t.Type[ProjectTermView], project: str) -> None:
    view(project).info()
