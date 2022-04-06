from pathlib import Path
from collections import namedtuple
import yaml
import sys
import typing as t

import click
import requests
from rich.table import Table
from rich.console import Console
from rich import box
from rich.panel import Panel
from rich.pretty import Pretty
from rich import print as rprint
from fs import open_fs
from fs.tarfs import TarFS

from starwhale.utils import fmt_http_server, pretty_bytes
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME, DEFAULT_MODEL_YAML_NAME, SW_API_VERSION
)
from starwhale.base.store import LocalStorage
from starwhale.utils.error import NotFoundError

TMP_FILE_BUFSIZE = 8192


class ModelPackageLocalStore(LocalStorage):

    def list(self, filter: str = "") -> None:
        super().list(
            filter=filter,
            title="List swmp in local storage",
            caption=f"@{self.pkgdir}"
        )

    def iter_local_swobj(self) -> LocalStorage.SWobjMeta:  # type: ignore
        pkg_fs = open_fs(str(self.pkgdir.resolve()))

        for mdir in pkg_fs.scandir("."):
            if not mdir.is_dir:
                continue

            for _fname in pkg_fs.opendir(mdir.name).listdir("."):
                if _fname != self.LATEST_TAG and not _fname.endswith(".swmp"):
                    continue

                _path = self.pkgdir / mdir.name / _fname
                _manifest = self._load_swmp_manifest(str(_path.resolve()))
                _tag = _fname if _fname == self.LATEST_TAG else ""

                yield LocalStorage.SWobjMeta(
                    name=mdir.name, version=_manifest["version"], tag=_tag,
                    environment=_manifest["dep"]["env"],
                    size=pretty_bytes(_path.stat().st_size),
                    created=_manifest["created_at"]
                )

    def _load_swmp_manifest(self, fpath) -> dict:
        with TarFS(fpath) as tar:
            return yaml.safe_load(tar.open(DEFAULT_MANIFEST_NAME))

    def push(self, swmp: str) -> None:
        server= fmt_http_server(self.sw_remote_addr)
        url = f"{server}/{SW_API_VERSION}/model/push"

        _spath = self.swmp_path(swmp)
        if not _spath.exists():
            rprint(f"[red]failed to push {swmp}[/], because of {_spath} not found")
            sys.exit(1)

        #TODO: add progress bar and rich live
        #TODO: add multi-part upload
        #TODO: add more push log
        rprint("try to push swmp...")
        r = requests.post(url, data={"swmp": swmp},
                          files={"file": _spath.open("rb")},
                          headers={"Authorization": self._sw_token}
                          )
        r.raise_for_status()
        rprint(" :clap: push done.")

    def pull(self, swmp: str, server: str, force: bool) -> None:
        server = server.strip() or self.sw_remote_addr
        server = fmt_http_server(server)
        url = f"{server}/{SW_API_VERSION}/model/pull"

        _spath = self.swmp_path(swmp)
        if _spath.exists() and not force:
            rprint(f":ghost: {swmp} is already existed, skip pull")
            return

        #TODO: add progress bar and rich live
        #TODO: multi phase for pull swmp
        #TODO: get size in advance
        rprint(f"try to pull {swmp}")
        with requests.get(url, stream=True,
                         parmas={"swmp": swmp}, # type: ignore
                         headers={"Authorization": self._sw_token}) as r:
            r.raise_for_exception()
            with _spath.open("wb") as f:
                for chunk in r.iter_content(chunk_size=TMP_FILE_BUFSIZE):
                    f.write(chunk)
        rprint(f" :clap: pull completed")

    def swmp_path(self, swmp: str) -> Path:
        _model, _version = self._parse_swobj(swmp)
        return (self.pkgdir / _model / f"{_version}.swmp")

    def info(self, swmp: str) -> None:
        _manifest = self.get_swmp_info(*self._parse_swobj(swmp))
        _config_panel = Panel(Pretty(_manifest, expand_all=True), title="inspect _manifest.yaml / model.yaml info")
        Console().print(_config_panel)
        #TODO: add workdir tree

    def get_swmp_info(self, _name: str, _version: str) -> dict:
        _workdir = self._guess(self.workdir / _name, _version)
        _swmp_path = self._guess(self.pkgdir / _name, _version if _version == self.LATEST_TAG else f"{_version}.swmp")

        if _workdir.exists():
            _manifest = yaml.safe_load((_workdir / DEFAULT_MANIFEST_NAME).open())
            _model = yaml.safe_load((_workdir / DEFAULT_MODEL_YAML_NAME).open())
        elif _swmp_path.exists():
            with TarFS(str(_swmp_path.resolve())) as tar:
                _manifest = yaml.safe_load(tar.open(DEFAULT_MANIFEST_NAME))
                _model = yaml.safe_load(tar.open(DEFAULT_MODEL_YAML_NAME))
        else:
            raise NotFoundError(f"{_workdir} and {_swmp_path} are both not existed.")

        _manifest.update(_model)
        _manifest["workdir"] = str(_workdir.resolve())
        _manifest["pkg"] = str(_swmp_path.resolve())
        return _manifest

    def gc(self, dry_run: bool=False) -> None:
        ...

    def delete(self, swmp) -> None:
        _model, _version = self._parse_swobj(swmp)

        def _remove_workdir(_real_version):
            workdir_fpath = self._guess(self.workdir / _model, _real_version)
            if not (workdir_fpath.exists() and workdir_fpath.is_dir()):
                return

            click.confirm(f"continue to delete {workdir_fpath}?", abort=True)
            open_fs(str(workdir_fpath.resolve())).removetree("/")
            workdir_fpath.rmdir()
            rprint(f" :bomb: delete workdir {workdir_fpath}")

        pkg_fpath = self._guess(self.pkgdir / _model, _version)
        if pkg_fpath.exists():
            click.confirm(f"continue to delete {pkg_fpath}?", abort=True)
            pkg_fpath = pkg_fpath.resolve()
            pkg_fpath.unlink()
            rprint(f" :collision: delete swmp {pkg_fpath}")

            _remove_workdir(pkg_fpath.name)

            latest = self.pkgdir / _model / self.LATEST_TAG
            if _version == self.LATEST_TAG or latest.resolve() == pkg_fpath:
                latest.unlink()
                rprint(f" :bomb: delete swmp {latest}")

        _remove_workdir(_version)

    def extract(self, swmp: str, force: bool=False) -> None:
        #TODO: extract swmp into workdir
        ...