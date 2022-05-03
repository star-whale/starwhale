import click

from starwhale.cluster import Cluster, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE


@click.group("project", help="starwhale controller project info and operation")
def project_cmd():
    pass


@project_cmd.command("list", help="list current user projects in starwhale controller")
@click.option("-a", "--all-users", is_flag=True, help="list all users project, if not set, cli will show current user's projects")
@click.option("-p", "--page", default=DEFAULT_PAGE_NUM, help="page number for projects list")
@click.option("-s", "--size", default=DEFAULT_PAGE_SIZE, help="page size for projects list")
def _list(all_users, page, size):
    Cluster().list(all_users, page, size)


@project_cmd.command("create", help="create a new project in starwhale controller")
def _create():
    pass