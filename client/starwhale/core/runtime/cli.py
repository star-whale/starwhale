import typing as t
from pathlib import Path

import click
from click_option_group import optgroup, MutuallyExclusiveOptionGroup

from starwhale.consts import (
    SupportArch,
    PythonRunEnv,
    DefaultYAMLName,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
)
from starwhale.base.uri import URI
from starwhale.base.type import URIType, RuntimeLockFileType
from starwhale.utils.cli import AliasedGroup
from starwhale.utils.error import MissingFieldError, ExclusiveArgsError

from .view import get_term_view, RuntimeTermView


@click.group(
    "runtime",
    cls=AliasedGroup,
    help="Runtime management, quickstart/build/copy/activate/restore...",
)
@click.pass_context
def runtime_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@click.group(
    "quickstart",
    cls=AliasedGroup,
    help="[Standalone]Quickstart your Starwhale Runtime",
)
def quickstart() -> None:
    pass


runtime_cmd.add_command(quickstart, aliases=["qs"])  # type: ignore


@quickstart.command("uri")
@click.argument("uri", required=True)
@click.argument("workdir", required=True)
@click.option("-f", "--force", is_flag=True, help="Force to quickstart")
@click.option("-n", "--name", default="", help="Runtime name")
@click.option(
    "-dr",
    "--disable-restore",
    is_flag=True,
    default=False,
    help="Create isolated python environment and restore the runtime",
)
def _quickstart_from_uri(
    uri: str, workdir: str, force: bool, name: str, disable_restore: bool
) -> None:
    """Quickstart from Starwhale Runtime URI

    Args:

        uri (str): Starwhale Dataset URI
        workdir (str): Runtime workdir
    """
    p_workdir = Path(workdir).absolute()
    name = name or p_workdir.name
    _uri = URI(uri, expected_type=URIType.RUNTIME)
    RuntimeTermView.quickstart_from_uri(
        workdir=p_workdir,
        name=name,
        uri=_uri,
        force=force,
        disable_restore=disable_restore,
    )


@quickstart.command("shell", help="Quickstart from interactive shell")
@click.argument("workdir", required=True)
@click.option("-f", "--force", is_flag=True, help="Force to quickstart")
@click.option(
    "-p",
    "--python-env",
    prompt="Choose your python env",
    type=click.Choice([PythonRunEnv.VENV, PythonRunEnv.CONDA]),
    default=PythonRunEnv.VENV,
    show_choices=True,
    show_default=True,
)
@click.option(
    "-n",
    "--name",
    default="",
    prompt="Please enter Starwhale Runtime name",
)
@click.option(
    "-dce",
    "--disable-create-env",
    prompt="Do you want to disable the isolated python environment creation automatically(NOT RECOMMENDED)?",
    is_flag=True,
    default=False,
    show_default=True,
)
@click.option(
    "-i",
    "--interactive",
    is_flag=True,
    default=False,
    help="Try entering the interactive shell at the end",
)
def _quickstart(
    workdir: str,
    force: bool,
    python_env: str,
    name: str,
    disable_create_env: bool,
    interactive: bool,
) -> None:
    """[Only Standalone]Quickstart Starwhale Runtime

    Args:

        workdir (Path): Runtime workdir
    """
    p_workdir = Path(workdir).absolute()
    name = name or p_workdir.name
    RuntimeTermView.quickstart_from_ishell(
        p_workdir, name, python_env, disable_create_env, force, interactive
    )


@runtime_cmd.command(
    "build",
    help="[Only Standalone]Create and build a relocated, shareable, packaged runtime bundle. Support python and native libs.",
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
    "-gab",
    "--gen-all-bundles",
    is_flag=True,
    help="Generate conda or venv files into runtime",
)
@click.option(
    "-ie", "--include-editable", is_flag=True, help="Include editable packages"
)
@click.option(
    "-del",
    "--disable-env-lock",
    is_flag=True,
    help="Disable virtualenv/conda environment dependencies lock, and the cli supports three methods to lock environment that are shell(auto-detect), prefix_path or env_name",
)
@optgroup.group(  # type: ignore
    "Python environment selectors",
    cls=MutuallyExclusiveOptionGroup,
    help="The selector of the python environment, default is the starwhale auto create env prefix path",
)
@optgroup.option(  # type: ignore
    "-ep", "--env-prefix-path", default="", help="Conda or virtualenv prefix path"
)
@optgroup.option(  # type: ignore
    "-en",
    "--env-name",
    default="",
    help="conda name in lock or gen all bundles process",
)
@optgroup.option(  # type: ignore
    "-es", "--env-use-shell", is_flag=True, default=False, help="use current shell"
)
def _build(
    workdir: str,
    project: str,
    runtime_yaml: str,
    gen_all_bundles: bool,
    include_editable: bool,
    disable_env_lock: bool,
    env_prefix_path: str,
    env_name: str,
    env_use_shell: bool,
) -> None:
    RuntimeTermView.build(
        workdir=workdir,
        project=project,
        yaml_name=runtime_yaml,
        gen_all_bundles=gen_all_bundles,
        include_editable=include_editable,
        disable_env_lock=disable_env_lock,
        env_prefix_path=env_prefix_path,
        env_name=env_name,
        env_use_shell=env_use_shell,
    )


@runtime_cmd.command("remove", aliases=["rm"])
@click.argument("runtime")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help="Force to remove runtime, the removed runtime cannot recover",
)
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
@click.pass_obj
def _history(view: t.Type[RuntimeTermView], runtime: str, fullname: bool) -> None:
    view(runtime).history(fullname)


@runtime_cmd.command("restore")
@click.argument("target")
def _restore(target: str) -> None:
    """
    [Only Standalone]Prepare dirs, restore python environment with virtualenv or conda and show activate command.

    TARGET: runtime uri or runtime workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    """
    RuntimeTermView.restore(target)


@runtime_cmd.command("list", aliases=["ls"], help="List runtime")
@click.option("-p", "--project", default="", help="Project URI")
@click.option("-f", "--fullname", is_flag=True, help="Show fullname of runtime version")
@click.option("-sr", "--show-removed", is_flag=True, help="Show removed runtime")
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
    "extract", help="[Only Standalone]Extract local runtime tar file into workdir"
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


@runtime_cmd.command("copy", aliases=["cp"])
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force to copy")
@click.option("-dlp", "--dest-local-project", help="dest local project uri")
def _copy(src: str, dest: str, force: bool, dest_local_project: str) -> None:
    """
    Copy Runtime between Standalone Instance and Cloud Instance

    SRC: runtime uri with version

    DEST: project uri or runtime uri with name.

    Example:

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud runtime to local project(myproject) with a new runtime name 'mnist-local'
            swcli runtime cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq local/project/myproject/mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud runtime to local default project(self) with the cloud instance runtime name 'mnist-cloud'
            swcli runtime cp cloud://pre-k8s/project/runtime/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq .

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud runtime to local project(myproject) with the cloud instance runtime name 'mnist-cloud'
            swcli runtime cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq . -dlp myproject

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud runtime to local default project(self) with a runtime name 'mnist-local'
            swcli runtime cp cloud://pre-k8s/project/runtime/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud runtime to local project(myproject) with a runtime name 'mnist-local'
            swcli runtime cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local -dlp myproject

        \b
        - copy standalone instance(local) default project(self)'s mnist-local runtime to cloud instance(pre-k8s) mnist project with a new runtime name 'mnist-cloud'
            swcli runtime cp mnist-local/version/latest cloud://pre-k8s/project/mnist/mnist-cloud

        \b
        - copy standalone instance(local) default project(self)'s mnist-local runtime to cloud instance(pre-k8s) mnist project with standalone instance runtime name 'mnist-local'
            swcli runtime cp mnist-local/version/latest cloud://pre-k8s/project/mnist

        \b
        - copy standalone instance(local) project(myproject)'s mnist-local runtime to cloud instance(pre-k8s) mnist project with standalone instance runtime name 'mnist-local'
            swcli runtime cp local/project/myproject/runtime/mnist-local/version/latest cloud://pre-k8s/project/mnist
    """
    RuntimeTermView.copy(src, dest, force, dest_local_project)


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
    aliases=["actv"],
    help="[Only Standalone]Activate python runtime environment for development",
)
@click.option("-u", "--uri", help="Runtime uri which has already been restored")
@click.option("-p", "--path", help="User's runtime workdir")
def _activate(uri: str, path: str) -> None:
    if uri and path:
        raise ExclusiveArgsError(f"only uri({uri}) or path({path}) can take effect")

    if not uri and not path:
        raise MissingFieldError("uri or path is required.")

    RuntimeTermView.activate(path, uri)


@runtime_cmd.command("lock")
@click.argument("target_dir", default=".")
@click.option(
    "--yaml-name",
    default=DefaultYAMLName.RUNTIME,
    help=f"Runtime YAML file name, default is {DefaultYAMLName.RUNTIME}",
)
@click.option(
    "--disable-auto-inject",
    is_flag=True,
    help="Disable auto update runtime.yaml dependencies field with lock file name, only render the lock file",
)
@optgroup.group(  # type: ignore
    "Python environment selectors",
    cls=MutuallyExclusiveOptionGroup,
    help="The selector of the python environment, default is the starwhale auto create env prefix path",
)
@optgroup.option("-n", "--env-name", default="", help="conda name")  # type: ignore
@optgroup.option(  # type: ignore
    "-p", "--env-prefix-path", default="", help="conda or virtualenv prefix path"
)
@optgroup.option(  # type: ignore
    "-s", "--env-use-shell", is_flag=True, default=False, help="use current shell"
)
@click.option("--stdout", is_flag=True, help="Output lock file content to the stdout")
@click.option(
    "--include-editable",
    is_flag=True,
    help="Include editable packages, only for venv mode",
)
@click.option(
    "--emit-pip-options",
    is_flag=True,
    help=f"Emit pip config options when the command dumps {RuntimeLockFileType.VENV}",
)
def _lock(
    target_dir: str,
    yaml_name: str,
    disable_auto_inject: bool,
    env_name: str,
    env_prefix_path: str,
    env_use_shell: bool,
    stdout: bool,
    include_editable: bool,
    emit_pip_options: bool,
) -> None:
    """
    [Only Standalone]Lock Python venv or conda environment

    TARGET_DIR: the runtime.yaml and local file dir, default is "."
    """

    RuntimeTermView.lock(
        target_dir,
        yaml_name,
        env_name,
        env_prefix_path,
        disable_auto_inject,
        stdout,
        include_editable,
        emit_pip_options,
        env_use_shell,
    )


@runtime_cmd.command("dockerize")
@click.argument("uri", required=True)
@click.option("-t", "--tag", multiple=True, help="Image tag")
@click.option("--push", is_flag=True, help="Push image to the registry")
@click.option(
    "--platform",
    multiple=True,
    default=[SupportArch.AMD64],
    type=click.Choice([SupportArch.AMD64, SupportArch.ARM64]),
    help="Target platform for docker build",
)
@click.option(
    "--dry-run",
    is_flag=True,
    help="Only render Dockerfile and build command",
)
@click.option(
    "--use-starwhale-builder",
    is_flag=True,
    help="Starwhale will create buildx builder for multi-arch",
)
@click.option(
    "--reset-qemu-static",
    is_flag=True,
    help="Reset qemu static, then fix multiarch build issue",
)
def _dockerize(
    uri: str,
    tag: t.Tuple[str],
    push: bool,
    platform: t.Tuple[str],
    dry_run: bool,
    use_starwhale_builder: bool,
    reset_qemu_static: bool,
) -> None:
    """[Only Standalone]Starwhale runtime dockerize, only for standalone instance

    URI (str): Starwhale Runtime URI in the standalone instance
    """
    RuntimeTermView(uri).dockerize(
        list(tag),
        push,
        list(platform),
        dry_run,
        use_starwhale_builder,
        reset_qemu_static,
    )
