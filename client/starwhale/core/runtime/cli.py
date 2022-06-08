import click

from starwhale.consts import (
    PythonRunEnv,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_PYTHON_VERSION,
    DEFAULT_SW_TASK_RUN_IMAGE,
)

from .view import RuntimeTermView


@click.group("runtime", help="Starwhale Runtime management")
def runtime_cmd() -> None:
    pass


@runtime_cmd.command(
    "create",
    help="[ONLY Standalone]Create a python runtime, which help user create a easy-to-use and unambiguous environment with venv or conda",
)
@click.argument("workdir")
@click.option("-n", "--name", required=True, help="runtime name")
@click.option(
    "-m",
    "--mode",
    type=click.Choice([PythonRunEnv.CONDA, PythonRunEnv.VENV]),
    default=PythonRunEnv.VENV,
    help="runtime mode",
)
@click.option("--python", default=DEFAULT_PYTHON_VERSION, help="Python Version")
@click.option("--base-image", default=DEFAULT_SW_TASK_RUN_IMAGE, help="docker image")
@click.option("-f", "--force", is_flag=True, help="force create runtime")
def _create(
    workdir: str, name: str, mode: str, python: str, base_image: str, force: bool
) -> None:
    # TODO: add runtime argument
    RuntimeTermView.create(
        workdir=workdir,
        name=name,
        mode=mode,
        python_version=python,
        force=force,
        base_image=base_image,
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
    help="runtime yaml filename, default use ${workdir}/runtime.yaml file",
)
@click.option(
    "--gen-all-bundles", is_flag=True, help="gen conda or venv files into runtime"
)
@click.option("--include-editable", is_flag=True, help="include editable package")
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


@runtime_cmd.command("remove", help="Remove runtime")
@click.argument("runtime")
@click.option("-f", "--force", is_flag=True, help="force remove runtime")
def _remove(runtime: str, force: bool) -> None:
    click.confirm("continue to remove?", abort=True)
    RuntimeTermView(runtime).remove(force)


@runtime_cmd.command("recover", help="Recover runtime")
@click.argument("runtime")
@click.option("-f", "--force", is_flag=True, help="force recover runtime")
def _recover(runtime: str, force: bool) -> None:
    RuntimeTermView(runtime).recover(force)


@runtime_cmd.command("info", help="Inspect runtime")
@click.argument("runtime")
@click.option("--fullname", is_flag=True, help="show version fullname")
def _info(runtime: str, fullname: bool) -> None:
    RuntimeTermView(runtime).info(fullname)


@runtime_cmd.command("history", help="Show runtime history")
@click.argument("runtime", required=True)
@click.option("--fullname", is_flag=True, help="show version fullname")
def _history(runtime: str, fullname: bool) -> None:
    RuntimeTermView(runtime).history(fullname)


@runtime_cmd.command("restore")
@click.argument("target")
def _restore(target: str) -> None:
    """
    [ONLY Standalone]Prepare to restore and activate runtime environment

    TARGET: runtime uri or runtime workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    """
    RuntimeTermView.restore(target)


@runtime_cmd.command("list", help="List runtime")
@click.option("--project", default="", help="Project URI")
@click.option("--fullname", is_flag=True, help="Show fullname of runtime version")
@click.option("--show-removed", is_flag=True, help="Show removed runtime")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="page number for tasks list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="page size for tasks list"
)
def _list(
    project: str, fullname: bool, show_removed: bool, page: int, size: int
) -> None:
    RuntimeTermView.list(project, fullname, show_removed, page, size)


@runtime_cmd.command(
    "extract", help="[ONLY Standalone]Extract local runtime tar file into workdir"
)
@click.argument("runtime")
@click.option("-f", "--force", is_flag=True, help="force extract runtime")
@click.option(
    "--target-dir",
    default="",
    help="extract target dir.if omitted, sw will use starwhale default workdir",
)
def _extract(runtime: str, force: bool, target_dir: str) -> None:
    RuntimeTermView(runtime).extract(force, target_dir)


@runtime_cmd.command("copy", help="Copy runtime, standalone <--> cloud")
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="force copy")
def _copy(src: str, dest: str, force: bool) -> None:
    RuntimeTermView.copy(src, dest, force)


@runtime_cmd.command("tag", help="Runtime Tag Management, add or remove")
@click.argument("runtime")
@click.argument("tags")
@click.option("-r", "--remove", is_flag=True, help="remove tags")
@click.option(
    "-q",
    "--quiet",
    is_flag=True,
    help="ignore tag name errors like name duplication, name absence",
)
def _tag(runtime: str, tags: str, remove: bool, quiet: bool) -> None:
    RuntimeTermView(runtime).tag(tags, remove, quiet)


@runtime_cmd.command(
    "activate",
    help="[Only Standalone]Activate python runtime environment for development",
)
@click.option(
    "-f",
    "--runtime-yaml",
    default=DefaultYAMLName.RUNTIME,
    help="runtime yaml filename, default use ${workdir}/runtime.yaml file",
)
@click.argument("workdir")
def _activate(workdir: str, runtime_yaml: str) -> None:
    RuntimeTermView.activate(workdir, runtime_yaml)
