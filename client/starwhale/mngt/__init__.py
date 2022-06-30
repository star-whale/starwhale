import sys
import webbrowser
from pathlib import Path

import click

from starwhale.utils import console
from starwhale.consts import (
    RECOVER_DIRNAME,
    VERSION_PREFIX_CNT,
    STANDALONE_INSTANCE,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.utils.fs import empty_dir
from starwhale.base.type import URIType, get_bundle_type_by_uri
from starwhale.utils.config import SWCliConfigMixed


def gc(dry_run: bool = False, yes: bool = False) -> None:
    sw = SWCliConfigMixed()

    for project_dir in sw.rootdir.iterdir():
        project_name = project_dir.name
        if project_name == RECOVER_DIRNAME:
            continue

        if not yes and not click.confirm(
            f"continue to garbage collection for project: {project_name}?"
        ):
            continue

        _removed_paths = []
        for typ in (URIType.DATASET, URIType.JOB, URIType.RUNTIME, URIType.MODEL):
            _bundle_type = get_bundle_type_by_uri(typ) if typ != URIType.JOB else ""
            _recover_dir = project_dir / typ / RECOVER_DIRNAME

            if typ in (URIType.RUNTIME, URIType.MODEL):
                for _path in _recover_dir.glob(f"**/*{_bundle_type}"):
                    if not (_path.is_file() and _path.name.endswith(_bundle_type)):
                        continue
                    _removed_paths.append(_path)
                    _workdir_path = _get_workdir_path(project_dir, typ, _path)
                    if _workdir_path.exists():
                        _removed_paths.append(_workdir_path)
            elif typ in (URIType.DATASET, URIType.JOB):
                for _path in _recover_dir.glob(f"**/{DEFAULT_MANIFEST_NAME}"):
                    if not _path.is_file():
                        continue
                    _removed_paths.append(_path.parent)
                    _workdir_path = _get_workdir_path(project_dir, typ, _path.parent)
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

    for project_dir in (sw.rootdir / RECOVER_DIRNAME).iterdir():
        if not dry_run and (yes or click.confirm("continue to remove?")):
            empty_dir(project_dir)

        console.print(f":no_entry_sign: [red]{project_dir}[/] removed")


def _get_workdir_path(project_dir: Path, typ: str, bundle_path: Path) -> Path:
    version = bundle_path.name.split(".")[0]
    object_prefix = f"{version[:VERSION_PREFIX_CNT]}/{version}"

    if typ != URIType.JOB:
        object_name = bundle_path.parent.parent.name
        object_prefix = f"{object_name}/{object_prefix}"
    _rpath = project_dir / "workdir" / typ / RECOVER_DIRNAME / object_prefix
    if _rpath.exists():
        return _rpath
    else:
        return project_dir / "workdir" / typ / object_prefix


def open_web(instance_uri: str = "") -> None:
    sw = SWCliConfigMixed()

    instance_uri = instance_uri.strip().strip("/")
    instance_uri = instance_uri or sw.current_instance

    if instance_uri == STANDALONE_INSTANCE:
        console.print(":see_no_evil: standalone web ui is comming soon.")
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
