import click

from starwhale.cluster import ClusterView, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE


@click.group("project", help="starwhale controller project info and operation")
def project_cmd():
    pass


@project_cmd.command("list", help="list current user projects in starwhale controller")
@click.option(
    "-a",
    "--all-users",
    is_flag=True,
    help="list all users project, if not set, cli will show current user's projects",
)
@click.option(
    "-p",
    "--page",
    type=int,
    default=DEFAULT_PAGE_NUM,
    help="page number for projects list",
)
@click.option(
    "-s",
    "--size",
    type=int,
    default=DEFAULT_PAGE_SIZE,
    help="page size for projects list",
)
@click.option("--fullname", is_flag=True, help="show version fullname")
def _list(all_users, page, size, fullname):
    ClusterView().list_projects(all_users, page, size, fullname)


@project_cmd.command("create", help="create a new project in starwhale controller")
def _create():
    pass
