import click
from rich import print as rprint
from starwhale.cluster import ClusterView


@click.group("cluster", help="starwhale cluster info and terminal ui")
def cluster_cmd() -> None:
    pass


@cluster_cmd.command("info", help="show controller cluster info")
def _info() -> None:
    ClusterView().info()


@cluster_cmd.command("open", help="open web brower for starwhale controller")
def _open() -> None:
    pass


@cluster_cmd.command("tui", help="basic terminal ui for starwhale controller")
def _tui() -> None:
    # TODO: add fullscreen terminal ui base on rich and textual lib
    rprint(":shell::clap::man_with_probing_cane: TUI(terminal ui) is comming soon...")
