import os
import sys
import typing as t
import subprocess
import webbrowser
from pathlib import Path

import click
from packaging.version import parse as version_parse

from starwhale.utils import console
from starwhale.consts import (
    RECOVER_DIRNAME,
    SW_TMP_DIR_NAME,
    DATA_STORE_DIRNAME,
    VERSION_PREFIX_CNT,
    STANDALONE_INSTANCE,
    OBJECT_STORE_DIRNAME,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.utils.fs import empty_dir
from starwhale.base.type import get_bundle_type_by_uri
from starwhale.utils.venv import get_conda_bin
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.resource import ResourceType


def gc(dry_run: bool = False, yes: bool = False) -> None:
    sw = SWCliConfigMixed()

    for project_dir in sw.rootdir.iterdir():
        project_name = project_dir.name
        if project_name in (
            RECOVER_DIRNAME,
            SW_TMP_DIR_NAME,
            OBJECT_STORE_DIRNAME,
            DATA_STORE_DIRNAME,
        ):
            continue

        if not yes and not click.confirm(
            f"continue to garbage collection for project: {project_name}?"
        ):
            continue

        _removed_paths = []
        for typ in (
            ResourceType.model,
            ResourceType.dataset,
            ResourceType.runtime,
            ResourceType.job,
        ):
            _bundle_type = (
                get_bundle_type_by_uri(typ) if typ != ResourceType.job else ""
            )
            _recover_dir = project_dir / typ.value / RECOVER_DIRNAME

            if typ in (ResourceType.model, ResourceType.runtime):
                for _path in _recover_dir.glob(f"**/*{_bundle_type}"):
                    if not (_path.is_file() and _path.name.endswith(_bundle_type)):
                        continue
                    _removed_paths.append(_path)
                    _workdir_path = _get_workdir_path(project_dir, typ.value, _path)
                    if _workdir_path.exists():
                        _removed_paths.append(_workdir_path)
            elif typ in (ResourceType.dataset, ResourceType.job):
                for _path in _recover_dir.glob(f"**/{DEFAULT_MANIFEST_NAME}"):
                    if not _path.is_file():
                        continue
                    _removed_paths.append(_path.parent)
                    _workdir_path = _get_workdir_path(
                        project_dir, _bundle_type, _path.parent
                    )
                    if _workdir_path.exists():
                        _removed_paths.append(_workdir_path)

        _rcnt = len(_removed_paths)
        if _rcnt == 0:
            console.print(f":deer: project:[red]{project_name}[/], no objects to gc")
            continue

        console.print(
            f":zap: project:[red]{project_name}[/], find {len(_removed_paths)} objects to cleanup..."
        )
        console.print("\n".join([f":no_entry_sign: {_p}" for _p in _removed_paths]))

        if not dry_run and (yes or click.confirm("continue to remove?")):
            for _p in _removed_paths:
                empty_dir(_p)

    _gc_special_dirs(sw.rootdir, dry_run, yes)


def _get_workdir_path(project_dir: Path, typ: str, bundle_path: Path) -> Path:
    version = bundle_path.name.split(".")[0]
    object_prefix = f"{version[:VERSION_PREFIX_CNT]}/{version}"

    if typ != ResourceType.job.value:
        object_name = bundle_path.parent.parent.name
        object_prefix = f"{object_name}/{object_prefix}"
    _rpath = project_dir / "workdir" / typ / RECOVER_DIRNAME / object_prefix
    if _rpath.exists():
        return _rpath
    else:
        return project_dir / "workdir" / typ / object_prefix


def _gc_special_dirs(root: Path, dry_run: bool, yes: bool) -> None:
    if os.path.isdir(root / RECOVER_DIRNAME):
        for project_dir in (root / RECOVER_DIRNAME).iterdir():
            if not dry_run and (yes or click.confirm("continue to remove?")):
                empty_dir(project_dir)

            console.print(f":no_entry_sign: [red]{project_dir}[/] removed")
    if os.path.isdir(root / SW_TMP_DIR_NAME) and not dry_run:
        # no need to confirm without dry_run, they are really garbage
        empty_dir(root / SW_TMP_DIR_NAME)


def open_web(instance_uri: str = "") -> None:
    sw = SWCliConfigMixed()

    instance_uri = instance_uri.strip().strip("/")
    instance_uri = instance_uri or sw.current_instance

    if instance_uri == STANDALONE_INSTANCE:
        console.print(":see_no_evil: standalone web ui is coming soon.")
    else:
        _config = sw.get_sw_instance_config(instance_uri)
        _uri = _config.get("uri", "")
        if not _uri:
            console.print(
                f":rotating_light: sw cannot find instance [red]{instance_uri}[/], please [bold red]swcli instance login[/] first."
            )
            sys.exit(1)
        else:
            console.print(f":clap: try to open {_uri}")
            webbrowser.open(_uri)


class _CheckLevel:
    WARN = "warn"
    CRITICAL = "critical"


class _Dependency(t.NamedTuple):
    title: str
    min_version: str
    level: str
    help: str
    checker: t.Callable[..., str]


def check() -> None:
    def _check_docker() -> str:
        out = subprocess.check_output(
            ["docker", "version", "--format", "{{.Client.Version}}"],
            stderr=subprocess.STDOUT,
        )
        return out.decode().strip()

    def _check_conda() -> str:
        out = subprocess.check_output(
            [get_conda_bin(), "--version"], stderr=subprocess.STDOUT
        )
        return out.decode().strip().split()[-1]

    dependencies: t.List[_Dependency] = [
        _Dependency(
            title="Docker",
            min_version="19.03",
            level=_CheckLevel.WARN,
            help=(
                "Docker is an open platform for developing, shipping, and running applications."
                "Starwhale uses Docker to run jobs. You can visit https://docs.docker.com/get-docker/ for more details."
            ),
            checker=_check_docker,
        ),
        _Dependency(
            title="Conda",
            min_version="4.0.0",
            level=_CheckLevel.WARN,
            help=(
                "Conda is an open-source package management system and environment management system."
                "Starwhale uses Conda to build runtime. You can download it from https://docs.conda.io/en/latest/miniconda.html."
            ),
            checker=_check_conda,
        ),
    ]

    for d in dependencies:
        ok, reason = True, ""
        _check_version = ""
        try:
            _check_version = d.checker()
        except subprocess.CalledProcessError as e:
            ok, reason = False, f"exit code:{e.returncode}, command:{e.output}"
        except Exception as e:
            ok, reason = False, str(e)

        if _check_version and version_parse(_check_version) < version_parse(
            d.min_version
        ):
            ok, reason = (
                False,
                f"version: {_check_version}, expected min version: {d.min_version}",
            )

        if ok:
            console.print(f":white_check_mark: {d.title} {_check_version}")
        else:
            console.print(f":x: {d.title} [{d.level}]")
            console.print(f"\t * :point_right: Reason: [red]{reason}[/]")
            console.print(f"\t * :blue_book: Min version: {d.min_version}")
            console.print(f"\t * :information_desk_person: Advice: {d.help} \n")
