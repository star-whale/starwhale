from __future__ import annotations

import typing as t
import webbrowser
from pathlib import Path

import click
from jinja2 import Environment, FileSystemLoader

from starwhale.version import STARWHALE_VERSION
from starwhale.utils.fs import ensure_dir, ensure_file
from starwhale.utils.cli import AliasedGroup
from starwhale.utils.debug import console
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.process import check_call


@click.group("server", cls=AliasedGroup, help="Manage Starwhale Local Server")
def server_cmd() -> None:
    pass  # pragma: no cover


@server_cmd.command("start")
@click.option(
    "-h",
    "--host",
    default="127.0.0.1",
    show_default=True,
    help="bind Starwhale Server to the host",
)
@click.option(
    "-p",
    "--port",
    default=8082,
    show_default=True,
    help="publish Starwhale Server port to the host",
)
@click.option(
    "envs",
    "-e",
    "--env",
    multiple=True,
    help="set environment variables for Starwhale Server",
)
@click.option(
    "-i",
    "--server-image",
    default="",
    help="set Starwhale Server image. If not set, Starwhale will use the swcli version related server image.",
)
@click.option(
    "--detach/--no-detach",
    is_flag=True,
    default=True,
    show_default=True,
    help="run Starwhale Server containers in the background",
)
@click.option(
    "--dry-run",
    is_flag=True,
    default=False,
    show_default=True,
    help="render compose yaml file and dry run docker compose",
)
@click.option(
    "--db-image",
    default="docker-registry.starwhale.cn/bitnami/mysql:8.0.29-debian-10-r2",
    hidden=True,
    help="set Starwhale Server database image",
)
def start(
    host: str,
    port: int,
    envs: t.List[str],
    server_image: str,
    detach: bool,
    dry_run: bool,
    db_image: str,
) -> None:
    """Start Starwhale Server in the local machine with Docker.

    You need to install Docker(>=19.03) and Docker-Compose(v2) first.
    When you run this command, a docker compose yaml file will be generated in the ~/.starwhale/.server directory.

    Examples:

    \b
    # Start Starwhale Server with default settings, then you can visit http://127.0.0.1:8082 to use Starwhale Server.
    swcli server start

    \b
    # Start Starwhale Server with custom Server image.
    swcli server start -i docker-registry.starwhale.cn/star-whale/server:latest

    \b
    # Start Starwhale Server with custom host and port.
    swcli server start --port 18082 --host 0.0.0.0

    \b
    # Start Starwhale Server in the foreground and custom environment variables for pypi.
    swcli server start --no-detach -e SW_PYPI_INDEX_URL=https://pypi.tuna.tsinghua.edu.cn/simple -e SW_PYPI_EXTRA_INDEX_URL=https://mirrors.aliyun.com/pypi/simple
    """
    server_compose_path = SWCliConfigMixed().server_compose
    ensure_dir(server_compose_path.parent)

    console.print(f":flying_saucer: render compose yaml file: {server_compose_path}")
    render_compose_yaml(
        path=server_compose_path,
        host=host,
        port=port,
        envs=envs,
        server_image=server_image,
        db_image=db_image,
    )

    console.print(":ping_pong: start Starwhale Server by docker compose")

    cmd = ["docker", "compose", "-f", str(server_compose_path), "up"]
    if detach:
        cmd.append("--detach")

    if dry_run:
        cmd.append("--dry-run")

    check_call(cmd, log=console.print)

    if detach:
        url = f"http://{host}:{port}"
        console.print(
            "Starwhale Server is running in the background. \n"
            "\t :apple: stop: [bold green]swcli server stop[/] \n"
            "\t :banana: check status: [bold green]swcli server status[/] \n"
            f"\t :watermelon: more compose command: [bold green]docker compose -f {server_compose_path} sub-command[/] \n"
            f"\t :carrot: visit web: {url}"
        )
        if not dry_run:
            webbrowser.open(url)  # pragma: no cover


@server_cmd.command(
    "status", aliases=["ps"], help="Show Starwhale Server containers status"
)
def status() -> None:
    # TODO: add more status info
    check_call(
        ["docker", "compose", "-f", str(SWCliConfigMixed().server_compose), "ps"],
        log=console.print,
    )


@server_cmd.command("stop", help="Stop Starwhale Server containers")
def stop() -> None:
    check_call(
        ["docker", "compose", "-f", str(SWCliConfigMixed().server_compose), "stop"],
        log=console.print,
    )


_TEMPLATE_DIR = Path(__file__).parent


def render_compose_yaml(
    path: Path,
    host: str,
    port: int,
    envs: t.List[str],
    server_image: str,
    db_image: str,
) -> None:
    jinja2_env = Environment(loader=FileSystemLoader(searchpath=_TEMPLATE_DIR))
    template = jinja2_env.get_template("compose.tmpl")

    starwhale_version = STARWHALE_VERSION
    if starwhale_version.startswith("0.0.0"):
        starwhale_version = ""

    if not server_image:
        server_image = f"docker-registry.starwhale.cn/star-whale/server:{starwhale_version or 'latest'}"

    content = template.render(
        host_ip=host,
        host_port=port,
        envs=envs,
        server_image=server_image,
        db_image=db_image,
        starwhale_version=starwhale_version,
    )
    ensure_file(path, content=content, parents=True)
