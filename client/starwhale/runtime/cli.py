import typing as t
from pathlib import Path

import click

from starwhale.consts import YAMLType


@click.group("runtime", help="Starwhale Runtime management")
def runtime_cmd() -> None:
    pass


@runtime_cmd.command(
    "create",
    help="Create a python runtime, which help user create a easy-to-use and unambiguous environment with venv or conda",
)
@click.option("--python", default="3.7")
def _create(python: str) -> None:
    pass


@runtime_cmd.command(
    "build",
    help="Create and build a relocated, shareable, packaged runtime bundle. Support python and native libs",
)
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option(
    "-f",
    "--runtime-yaml",
    default=YAMLType.RUNTIME,
    help="runtime yaml filename, default use ${workdir}/runtime.yaml file",
)
@click.option(
    "--gen-packaged-data", is_flag=True, help="gen conda or venv files into swrt"
)
def _build(workdir: str, runtime_yaml: str, gen_packaged_data: bool) -> None:
    pass


@runtime_cmd.command("remove", help="Remove runtime")
@click.argument("runtime")
def _remove(runtime: str) -> None:
    pass


@runtime_cmd.command("recover", help="Recover runtime")
@click.argument("runtime")
def _recover(runtime: str) -> None:
    pass


@runtime_cmd.command("info", help="Inspect runtime")
@click.argument("runtime")
def _info(runtime: str) -> None:
    pass


@runtime_cmd.command("history", help="Show runtime history")
@click.argument("runtime")
def _history(runtime: str) -> None:
    pass


@runtime_cmd.command("revert", help="Revert runtime")
@click.argument("runtime")
def _revert(runtime: str) -> None:
    pass


@runtime_cmd.command("list", help="List runtime")
@click.option("--fullname", is_flag=True, help="Show fullname of swrt version")
@click.option("--project", default="", help="Project URI")
def _list(fullname: bool, project: str) -> None:
    pass


@runtime_cmd.command("extract", help="Extract local runtime tar file into workdir")
@click.argument("runtime")
@click.option("-f", "--force", is_flag=True, help="force extract swmp")
@click.option(
    "-t",
    "--target",
    type=click.Path(),
    default=None,
    help="extract target dir.if omitted, sw will use starwhale default workdir",
)
def _extract(runtime: str, force: bool, target: t.Optional[Path]) -> None:
    pass


@runtime_cmd.command("copy", help="Copy runtime")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="force push swmp")
def _copy(src: str, dest: str, force: bool) -> None:
    pass
