import click

from starwhale.utils import console
from starwhale.utils.cli import AliasedGroup
from starwhale.base.uri.instance import Instance
from starwhale.api._impl.data_store import (
    DataStore,
    TableDesc,
    LocalDataStore,
    RemoteDataStore,
)


@click.group("ds", cls=AliasedGroup, help="data store debug cmds")
def datastore() -> None:
    ...


def _get_datastore() -> DataStore:
    ins = Instance()
    if ins.is_cloud:
        return RemoteDataStore(ins.url, ins.token)
    else:
        return LocalDataStore.get_instance()


@datastore.command("tables", help="list tables")
def _tables() -> None:
    for table in _get_datastore().list_tables([""]):
        click.echo(table)


@datastore.command("scan", help="get table")
@click.argument("table")
@click.option("-l", "--limit", default=10, help="limit of rows to get, 0 for all")
def _get(table: str, limit: int) -> None:
    ds = _get_datastore()
    for row in ds.scan_tables(tables=[TableDesc(table_name=table)]):
        console.print(row)
        if limit > 0:
            limit -= 1
            if limit <= 0:
                break
