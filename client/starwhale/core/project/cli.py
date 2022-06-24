import click

from .view import ProjectTermView, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE


@click.group("project", help="Project management, for standalone and cloud instances")
def project_cmd() -> None:
    pass


@project_cmd.command("list", help="List projects in current Starwhale Instance")
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
def _list(instance: str, page: int, size: int) -> None:
    ProjectTermView.list(instance, page, size)


@project_cmd.command(
    "create", help="Create a new project in current Starwhale instance"
)
@click.argument("project", type=str)
def _create(project: str) -> None:
    ProjectTermView(project).create()


@project_cmd.command("select", help="Select default project in current instance")
@click.argument("project", type=str)
def _select(project: str) -> None:
    ProjectTermView(project).select()


@project_cmd.command("remove", help="Remove project")
@click.argument("project", type=str)
def _remove(project: str) -> None:
    ProjectTermView(project).remove()


@project_cmd.command("recover", help="Recover project")
@click.argument("project", type=str)
def _recover(project: str) -> None:
    ProjectTermView(project).recover()


@project_cmd.command("info", help="Inspect project")
@click.argument("project", type=str)
def _info(project: str) -> None:
    ProjectTermView(project).info()
