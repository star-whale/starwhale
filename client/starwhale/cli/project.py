import click

@click.group("project", help="starwhale controller project info and operation")
def project_cmd():
    pass


@project_cmd.command("list", help="list current user projects in starwhale controller")
def _list():
    pass


@project_cmd.command("create", help="create a new project in starwhale controller")
def _create():
    pass