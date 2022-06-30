import typing as t

import click

from starwhale.consts import (
    PythonRunEnv,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_PYTHON_VERSION,
)

from .view import get_term_view, RuntimeTermView


@click.group(
    "runtime", help="Runtime management, create/build/copy/activate/restore..."
)
@click.pass_context
def runtime_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@runtime_cmd.command(
    "create",
    help="[ONLY Standalone]Create a python runtime, which help user create a easy-to-use and unambiguous environment with venv or conda",
)
@click.argument("workdir")
@click.option("-n", "--name", required=True, help="Runtime name")
@click.option(
    "-m",
    "--mode",
    type=click.Choice([PythonRunEnv.CONDA, PythonRunEnv.VENV]),
    default=PythonRunEnv.VENV,
    help="Runtime mode",
)
@click.option("--python", default=DEFAULT_PYTHON_VERSION, help="Python Version")
@click.option("-f", "--force", is_flag=True, help="Force to create runtime")
def _create(workdir: str, name: str, mode: str, python: str, force: bool) -> None:
    RuntimeTermView.create(
        workdir=workdir,
        name=name,
        mode=mode,
        python_version=python,
        force=force,
    )


@runtime_cmd.command(
    "build",
    help="[ONLY Standalone]Create and build a relocated, shareable, packaged runtime bundle. Support python and native libs.",
)
@click.argument("workdir", type=click.Path(exists=True, file_okay=False))
@click.option("-p", "--project", default="", help="Project URI")
@click.option(
    "-f",
    "--runtime-yaml",
    default=DefaultYAMLName.RUNTIME,
    help="Runtime yaml filename, default use ${workdir}/runtime.yaml file",
)
@click.option(
    "--gen-all-bundles", is_flag=True, help="Generate conda or venv files into runtime"
)
@click.option("--include-editable", is_flag=True, help="Include editable packages")
def _build(
    workdir: str,
    project: str,
    runtime_yaml: str,
    gen_all_bundles: bool,
    include_editable: bool,
) -> None:
    RuntimeTermView.build(
        workdir=workdir,
        project=project,
        yaml_name=runtime_yaml,
        gen_all_bundles=gen_all_bundles,
        include_editable=include_editable,
    )


@runtime_cmd.command("remove")
@click.argument("runtime")
@click.option("-f", "--force", is_flag=True, help="Force to remove runtime")
def _remove(runtime: str, force: bool) -> None:
    """
    Remove runtime

    You can run `swcli runtime recover` to recover the removed runtimes.

    RUNTIME: argument use the `Runtime URI` format, so you can remove the whole runtime or a specified-version runtime.
    """
    click.confirm("continue to remove?", abort=True)
    RuntimeTermView(runtime).remove(force)


@runtime_cmd.command("recover")
@click.argument("runtime")
@click.option("-f", "--force", is_flag=True, help="Force to recover runtime")
def _recover(runtime: str, force: bool) -> None:
    """
    Recover runtime

    RUNTIME: argument use the `Runtime URI` format, so you can recover the whole runtime or a specified-version runtime.
    """
    RuntimeTermView(runtime).recover(force)


@runtime_cmd.command("info", help="Show runtime details")
@click.argument("runtime")
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _info(view: t.Type[RuntimeTermView], runtime: str, fullname: bool) -> None:
    view(runtime).info(fullname)


@runtime_cmd.command("history", help="Show runtime history")
@click.argument("runtime", required=True)
@click.option("--fullname", is_flag=True, help="Show version fullname")
def _history(runtime: str, fullname: bool) -> None:
    RuntimeTermView(runtime).history(fullname)


@runtime_cmd.command("restore")
@click.argument("target")
def _restore(target: str) -> None:
    """
    [ONLY Standalone]Prepare dirs, restore python environment with virtualenv or conda and show activate command.

    TARGET: runtime uri or runtime workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    """
    RuntimeTermView.restore(target)


@runtime_cmd.command("list", help="List runtime")
@click.option("--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of runtime version")
@click.option("--show-removed", is_flag=True, help="Show removed runtime")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for tasks list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for tasks list"
)
@click.pass_obj
def _list(
    view: t.Type[RuntimeTermView],
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
) -> None:
    view.list(project, fullname, show_removed, page, size)


@runtime_cmd.command(
    "extract", help="[ONLY Standalone]Extract local runtime tar file into workdir"
)
@click.argument("runtime")
@click.option("-f", "--force", is_flag=True, help="Force to extract runtime")
@click.option(
    "--target-dir",
    default="",
    help="Extract target dir.if omitted, sw will use starwhale default workdir",
)
def _extract(runtime: str, force: bool, target_dir: str) -> None:
    RuntimeTermView(runtime).extract(force, target_dir)


@runtime_cmd.command("copy", help="Copy runtime, standalone <--> cloud")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force to copy")
def _copy(src: str, dest: str, force: bool) -> None:
    RuntimeTermView.copy(src, dest, force)


@runtime_cmd.command("tag", help="Runtime Tag Management, add or remove")
@click.argument("runtime")
@click.argument("tags", nargs=-1)
@click.option("-r", "--remove", is_flag=True, help="Remove tags")
@click.option(
    "-q",
    "--quiet",
    is_flag=True,
    help="Ignore tag name errors like name duplication, name absence",
)
def _tag(runtime: str, tags: t.List[str], remove: bool, quiet: bool) -> None:
    RuntimeTermView(runtime).tag(tags, remove, quiet)


@runtime_cmd.command(
    "activate",
    help="[Only Standalone]Activate python runtime environment for development",
)
@click.option(
    "-f",
    "--runtime-yaml",
    default=DefaultYAMLName.RUNTIME,
    help="Runtime yaml filename, default use ${WORKDIR}/runtime.yaml file",
)
@click.argument("workdir")
def _activate(workdir: str, runtime_yaml: str) -> None:
    RuntimeTermView.activate(workdir, runtime_yaml)
