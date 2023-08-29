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
from starwhale.utils.cli import AliasedGroup
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.runtime.model import _SUPPORT_CUDA, _SUPPORT_CUDNN

from .view import get_term_view, RuntimeTermView
from .model import RuntimeInfoFilter


@click.group(
    "runtime",
    cls=AliasedGroup,
    help="Runtime management, quickstart/build/copy/activate...",
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
    _uri = Resource(uri, typ=ResourceType.runtime)
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


@runtime_cmd.command("build")
@optgroup.group(
    "\n  ** Acceptable build sources",
    cls=MutuallyExclusiveOptionGroup,
    help="The selector of the runtime build source, default is runtime.yaml source",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-c",
    "--conda",
    default="",
    help="from conda environment name",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-cp",
    "--conda-prefix",
    default="",
    help="from conda environment prefix path",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-v",
    "--venv",
    help="from virtualenv or python-venv environment prefix path",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-s",
    "--shell",
    is_flag=True,
    help="from current shell, venv or conda environment has been activated",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-y",
    "--yaml",
    help="from runtime yaml format file path.Default uses runtime.yaml in the work directory(pwd)",
    default=DefaultYAMLName.RUNTIME,
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-d",
    "--docker",
    default="",
    help="from docker image",
)
@optgroup.group(
    "\n  ** Runtime YAML Source Configuration",
    help="The configurations only work for `--from-runtime-yaml` source",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-del",
    "--disable-env-lock",
    is_flag=True,
    help="Disable virtualenv/conda environment dependencies lock",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-nc",
    "--no-cache",
    is_flag=True,
    help="Invalid the cached(installed) packages in the isolate env when env-lock is enabled, \
    only for auto-generated environments",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-dad",
    "--download-all-deps",
    is_flag=True,
    help="Download all python dependencies into the runtime bundle file, the file size of swrt maybe very large.",
    hidden=True,
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-ie",
    "--include-editable",
    is_flag=True,
    help="Include editable packages",
    hidden=True,
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-ilw",
    "--include-local-wheel",
    is_flag=True,
    help="Include local wheel packages",
    hidden=True,
)
@optgroup.group(
    "\n  ** Conda/Venv/Shell Sources Configurations",
    help="The configurations only work for `--conda`, `--conda-prefix`, `--venv` and `--shell` sources",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "--cuda",
    type=click.Choice(_SUPPORT_CUDA + [""], case_sensitive=False),
    default="",
    help="cuda version, works for shell, conda name, conda prefix path and venv prefix path sources",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "--cudnn",
    default="",
    type=click.Choice(list(_SUPPORT_CUDNN.keys()) + [""], case_sensitive=False),
    help="cudnn version, works for shell, conda name, conda prefix path and venv prefix path sources",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "--arch",
    type=click.Choice(
        [SupportArch.AMD64, SupportArch.ARM64, SupportArch.NOARCH],
        case_sensitive=False,
    ),
    default=SupportArch.NOARCH,
    help="system architecture, works for shell, conda name, conda prefix path and venv prefix path sources",
)
@optgroup.group("\n  ** Global Configurations")
@optgroup.option("-n", "--name", default="", help="runtime name")  # type: ignore[no-untyped-call]
@optgroup.option(  # type: ignore[no-untyped-call]
    "-p",
    "--project",
    default="",
    help="Project URI, default is the current selected project. The runtime package will store in the specified project.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-dad",
    "--download-all-deps",
    is_flag=True,
    help="Download all python dependencies into the runtime bundle file, the file size of swrt maybe very large.",
    hidden=True,
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-ie",
    "--include-editable",
    is_flag=True,
    help="Include editable packages",
    hidden=True,
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-dpo",
    "--dump-pip-options",
    is_flag=True,
    show_default=True,
    help="Dump pip config options from the ~/.pip/pip.conf file.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-dcc",
    "--dump-condarc",
    is_flag=True,
    show_default=True,
    help="Dump conda config from the ~/.condarc file",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-ilw",
    "--include-local-wheel",
    is_flag=True,
    help="Include local wheel packages",
    hidden=True,
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "tags",
    "-t",
    "--tag",
    multiple=True,
    help="runtime tags, the option can be used multiple times. `latest` and `^v\d+$` tags are reserved tags.",
)
def _build(
    name: str,
    project: str,
    cuda: str,
    cudnn: str,
    arch: str,
    download_all_deps: bool,
    include_editable: bool,
    include_local_wheel: bool,
    disable_env_lock: bool,
    no_cache: bool,
    conda: str,
    conda_prefix: str,
    venv: str,
    shell: bool,
    yaml: str,
    docker: str,
    dump_pip_options: bool,
    dump_condarc: bool,
    tags: t.List[str],
) -> None:
    """Create and build a relocated, shareable, packaged runtime bundle(aka `swrt` file). Support python and native libs.
    Runtime build only works in the Standalone instance.

    Acceptable sources:

        \b
        - runtime.yaml file: The most flexible and customizable way.By the runtime.yaml file,
            you can specify the runtime name, python version, conda or venv environment, and the python dependency packages etc.
        - conda name: Lock the conda environment with conda name and generate the runtime bundle.
        - conda prefix path: Lock the conda environment with conda prefix path and generate the runtime bundle.
        - venv prefix path: Lock the virtualenv or python venv environment with venv prefix path and generate the runtime bundle.
        - shell: Lock the current shell environment and generate the runtime bundle. The current shell must be conda or venv.
        - docker image: Use the docker image as the runtime directly.

    Examples:

        \b
        - from runtime.yaml:
        swcli runtime build  # use the current directory as the workdir and use the default runtime.yaml file
        swcli runtime build -y example/pytorch/runtime.yaml # use example/pytorch/runtime.yaml as the runtime.yaml file
        swcli runtime build --yaml runtime.yaml # use runtime.yaml at the current directory as the runtime.yaml file
        swcli runtime build --tag tag1 --tag tag2

        \b
        - from conda name:
        swcli runtime build -c pytorch # lock pytorch conda environment and use `pytorch` as the runtime name
        swcli runtime build --conda pytorch --name pytorch-runtime # use `pytorch-runtime` as the runtime name
        swcli runtime build --conda pytorch --cuda 11.4 # specify the cuda version
        swcli runtime build --conda pytorch --arch noarch # specify the system architecture

        \b
        - from conda prefix path:
        swcli runtime build --conda-prefix /home/starwhale/anaconda3/envs/pytorch # get conda prefix path by `conda info --envs` command

        \b
        - from venv prefix path:
        swcli runtime build -v /home/starwhale/.virtualenvs/pytorch
        swcli runtime build --venv /home/starwhale/.local/share/virtualenvs/pytorch --arch amd64

        \b
        - from docker image:
        swcli runtime build --docker pytorch/pytorch:1.9.0-cuda11.1-cudnn8-runtime  # use the docker image as the runtime directly

        \b
        - from shell:
        swcli runtime build -s --cuda 11.4 --cudnn 8 # specify the cuda and cudnn version
        swcli runtime build --shell --name pytorch-runtime # lock the current shell environment and use `pytorch-runtime` as the runtime name
    """
    if docker:
        RuntimeTermView.build_from_docker_image(
            image=docker, runtime_name=name, project=project
        )
    elif conda or conda_prefix or venv or shell:
        # TODO: support auto mode for cuda, cudnn and arch
        RuntimeTermView.build_from_python_env(
            runtime_name=name,
            conda_name=conda,
            conda_prefix=conda_prefix,
            venv_prefix=venv,
            project=project,
            cuda=cuda,
            cudnn=cudnn,
            arch=arch,
            download_all_deps=download_all_deps,
            include_editable=include_editable,
            include_local_wheel=include_local_wheel,
            dump_condarc=dump_condarc,
            dump_pip_options=dump_pip_options,
            tags=tags,
        )
    else:
        RuntimeTermView.build_from_runtime_yaml(
            workdir=Path.cwd(),
            yaml_path=Path(yaml),
            project=project,
            runtime_name=name,
            download_all_deps=download_all_deps,
            include_editable=include_editable,
            include_local_wheel=include_local_wheel,
            no_cache=no_cache,
            disable_env_lock=disable_env_lock,
            dump_condarc=dump_condarc,
            dump_pip_options=dump_pip_options,
            tags=tags,
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


@runtime_cmd.command("info")
@click.argument("runtime")
@click.option(
    "-of",
    "--output-filter",
    type=click.Choice([f.value for f in RuntimeInfoFilter], case_sensitive=False),
    default=RuntimeInfoFilter.basic.value,
    show_default=True,
    help="Filter the output content. Only standalone instance supports this option.",
)
@click.pass_obj
def _info(
    view: t.Type[RuntimeTermView],
    runtime: str,
    output_filter: str,
) -> None:
    """Show runtime details.

    RUNTIME: argument use the `Runtime URI` format. Version is optional for the Runtime URI.
    If the version is not specified, the latest version will be used.

    Example:

        \b
          swcli runtime info pytorch # show basic info from the latest version of runtime
          swcli runtime info pytorch/version/v0  # show basic info
          swcli runtime info pytorch/version/v0 --output-filter basic  # show basic info
          swcli runtime info pytorch/version/v1 -of runtime_yaml  # show runtime.yaml content
          swcli runtime info pytorch/version/v1 -of lock # show auto lock file content
          swcli runtime info pytorch/version/v1 -of manifest # show _manifest.yaml content
          swcli runtime info pytorch/version/v1 -of all # show all info of the runtime
    """
    uri = Resource(runtime, ResourceType.runtime)
    view(uri).info(RuntimeInfoFilter(output_filter))


@runtime_cmd.command("history", help="Show runtime history")
@click.argument("runtime", required=True)
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _history(view: t.Type[RuntimeTermView], runtime: str, fullname: bool) -> None:
    view(runtime).history(fullname)


# hide runtime restore command for the users in the command help output.
@runtime_cmd.command("restore", hidden=True)
@click.argument("target")
def _restore(target: str) -> None:
    """
    [Only Standalone]Prepare dirs, restore python environment with virtualenv or conda and show activate command.

    TARGET: runtime uri or runtime workdir path, in Starwhale Agent Docker Environment, only support workdir path.
    """
    RuntimeTermView.restore(target)


@runtime_cmd.command("list", aliases=["ls"])
@click.option(
    "-p",
    "--project",
    default="",
    help="Project URI, the default is the current selected project.",
)
@click.option("-f", "--fullname", is_flag=True, help="Show fullname of runtime version")
@click.option("-sr", "--show-removed", is_flag=True, help="Show removed runtime")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for tasks list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for tasks list"
)
@click.option(
    "filters",
    "-fl",
    "--filter",
    multiple=True,
    help="Filter output based on conditions provided.",
)
@click.pass_obj
def _list(
    view: t.Type[RuntimeTermView],
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
    filters: list,
) -> None:
    """
    List Runtime of the specified project

    The filtering flag (-fl or --filter) format is a key=value pair or a flag.
    If there is more than one filter, then pass multiple flags.\n
    (e.g. --filter name=mnist --filter latest)

    \b
    The currently supported filters are:
      name\tTEXT\tThe prefix of the runtime name
      owner\tTEXT\tThe name or id of the runtime owner
      latest\tFLAG\t[Cloud] Only show the latest version
            \t \t[Standalone] Only show the version with "latest" tag
    """
    view.list(project, fullname, show_removed, page, size, filters)


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
@click.option(
    "ignore_tags",
    "-i",
    "--ignore-tag",
    multiple=True,
    help="ignore tags to copy. The option can be used multiple times.",
)
def _copy(
    src: str, dest: str, force: bool, dest_local_project: str, ignore_tags: t.List[str]
) -> None:
    """
    Copy Runtime between Standalone Instance and Cloud Instance

    SRC: runtime uri with version

    DEST: project uri or runtime uri with name.

    In default, copy runtime with all user custom tags. If you want to ignore some tags, you can use `--ignore-tag` option.
    `latest` and `^v\d+$` are the system builtin tags, they are ignored automatically.

    When the tags are already used for the other runtime version in the dest instance, you should use `--force` option or adjust the tags.

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
        - copy standalone instance(local) default project(self)'s mnist-local runtime to cloud instance(pre-k8s) mnist project without 'cloud://' prefix
            swcli runtime cp mnist-local/version/latest pre-k8s/project/mnist

        \b
        - copy standalone instance(local) project(myproject)'s mnist-local runtime to cloud instance(pre-k8s) mnist project with standalone instance runtime name 'mnist-local'
            swcli runtime cp local/project/myproject/runtime/mnist-local/version/latest cloud://pre-k8s/project/mnist

        \b
        - copy without some tags
            swcli runtime cp pytorch cloud://cloud.starwhale.cn/project/starwhale:public --ignore-tag t1
    """
    RuntimeTermView.copy(src, dest, force, dest_local_project, ignore_tags)


@runtime_cmd.command("tag")
@click.argument("runtime")
@click.argument("tags", nargs=-1)
@click.option("-r", "--remove", is_flag=True, help="Remove tags")
@click.option(
    "-q",
    "--quiet",
    is_flag=True,
    help="Ignore tag name errors like name duplication, name absence",
)
@click.option(
    "-f",
    "--force-add",
    is_flag=True,
    help="force to add tags, even the tag has been used for another version",
)
def _tag(
    runtime: str, tags: t.List[str], remove: bool, quiet: bool, force_add: bool
) -> None:
    """Runtime tag management: add, remove and list

    RUNTIME: argument use the `Runtime URI` format.

    Examples:

        \b
        - list tags of the pytorch runtime
        swcli runtime tag pytorch

        \b
        - add tags for the pytorch runtime
        swcli runtime tag mnist t1 t2
        swcli runtime tag cloud://cloud.starwhale.cn/project/public:starwhale/runtime/pytorch/version/latest t1 --force-add
        swcli runtime tag mnist t1 --quiet

        \b
        - remove tags for the pytorch runtime
        swcli runtime tag mnist -r t1 t2
        swcli runtime tag cloud://cloud.starwhale.cn/project/public:starwhale/runtime/pytorch --remove t1
    """
    RuntimeTermView(runtime).tag(
        tags=tags, remove=remove, ignore_errors=quiet, force_add=force_add
    )


@runtime_cmd.command(
    "activate",
    aliases=["actv"],
    help="",
)
@click.argument("uri")
@click.option(
    "-f",
    "--force-restore",
    help="Force to restore runtime into the related snapshot workdir even the runtime has been restored",
)
def _activate(uri: str, force_restore: bool) -> None:
    """
    [Only Standalone]Activate python runtime environment for development

    When the runtime has not been restored, activate command will restore runtime automatically.

    URI: Runtime uri in the standalone instance
    """
    _uri = Resource(uri, typ=ResourceType.runtime)
    RuntimeTermView.activate(_uri, force_restore)


@runtime_cmd.command("lock")
@click.argument("target_dir", default=".")
@click.option(
    "-f",
    "--yaml-path",
    default=DefaultYAMLName.RUNTIME,
    help=f"Runtime YAML file path, default is {DefaultYAMLName.RUNTIME} at the current working directory",
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
@click.option(
    "-so", "--stdout", is_flag=True, help="Output lock file content to the stdout"
)
@click.option(
    "-ie",
    "--include-editable",
    is_flag=True,
    help="Include editable packages",
)
@click.option(
    "-ilw",
    "--include-local-wheel",
    is_flag=True,
    help="Include local wheel packages",
)
@click.option(
    "-dpo",
    "--dump-pip-options",
    is_flag=True,
    show_default=True,
    help="Dump pip config options from the ~/.pip/pip.conf file.",
)
@click.option(
    "-nc",
    "--no-cache",
    is_flag=True,
    help="Invalid the cached(installed) packages in the isolate env when env-lock is enabled, \
    only for auto-generated environments",
)
def _lock(
    target_dir: str,
    yaml_path: str,
    env_name: str,
    env_prefix_path: str,
    env_use_shell: bool,
    stdout: bool,
    include_editable: bool,
    include_local_wheel: bool,
    dump_pip_options: bool,
    no_cache: bool,
) -> None:
    """
    [Only Standalone]Lock Python venv or conda environment

    TARGET_DIR: the lock files will store in the `target_dir` , default is "."
    """

    RuntimeTermView.lock(
        target_dir,
        Path(yaml_path),
        env_name,
        env_prefix_path,
        no_cache,
        stdout,
        include_editable,
        include_local_wheel,
        dump_pip_options,
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
