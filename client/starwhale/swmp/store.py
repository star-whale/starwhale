from pathlib import Path
from collections import namedtuple
import yaml
import sys
import typing as t

import requests
from loguru import logger
from rich.table import Table
from rich.console import Console
from rich import box
from rich.panel import Panel
from rich.pretty import Pretty
from rich import print as rprint
from fs import open_fs
from fs.tarfs import TarFS

from starwhale.utils import fmt_http_server
from starwhale.utils.config import load_swcli_config
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME, DEFAULT_MODEL_YAML_NAME, SW_API_VERSION
)

SwmpMeta = namedtuple("SwmpMeta", ["model", "version", "tag", "environment", "created"])


LATEST_TAG = "latest"
TMP_FILE_BUFSIZE = 8192


class ModelPackageLocalStore(object):

    def __init__(self, swcli_config=None) -> None:
        self._swcli_config = swcli_config or load_swcli_config()

    @property
    def rootdir(self) -> Path:
        return Path(self._swcli_config["storage"]["root"])

    @property
    def workdir(self) -> Path:
        return self.rootdir / "workdir"

    @property
    def pkgdir(self) -> Path:
        return self.rootdir / "pkg"

    def list(self, filter=None) -> None:
        #TODO: add filter for list
        #TODO: add expand option for list
        #TODO: workdir list

        table = Table(title="List swmp in local storage", caption=f"@{self.pkgdir}",
                      box=box.SIMPLE)
        table.add_column("Model", justify="right", style="cyan" ,no_wrap=False)
        table.add_column("Version", style="magenta")
        table.add_column("Tag", style="magenta")
        table.add_column("Environment", style="magenta")
        table.add_column("Created", justify="right",)

        for s in self._iter_local_swmp():
            table.add_row(s.model, s.version, s.tag, s.environment, s.created)

        Console().print(table)

    def _iter_local_swmp(self) -> SwmpMeta:  # type: ignore
        pkg_fs = open_fs(str(self.pkgdir.resolve()))

        for mdir in pkg_fs.scandir("."):
            if not mdir.is_dir:
                continue

            for _fname in pkg_fs.opendir(mdir.name).listdir("."):
                if _fname != LATEST_TAG and not _fname.endswith(".swmp"):
                    continue

                _path = self.pkgdir / mdir.name / _fname
                _manifest = self._load_swmp_manifest(str(_path.resolve()))
                _tag = _fname if _fname == LATEST_TAG else ""

                yield SwmpMeta(model=mdir.name, version=_manifest["version"], tag=_tag,
                               environment=_manifest["dep"]["env"], created=_manifest["created_at"])

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
                          files={"file": _spath.open("rb")})
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

    def _parse_swmp(self, swmp: str) -> t.Tuple[str, str]:
        if ":" not in swmp:
            _name, _version = swmp, LATEST_TAG
        else:
            if swmp.count(":") > 1:
                raise Exception(f"{swmp} format wrong, use [model]:[version]")

            _name, _version = swmp.split(":")
        return _name, _version

    def swmp_path(self, swmp: str) -> Path:
        _model, _version = self._parse_swmp(swmp)
        return (self.pkgdir / _model / f"{_version}.swmp")

    @property
    def sw_remote_addr(self) -> str:
        return self._swcli_config.get("controller", {}).get("remote_addr", "")

    @property
    def _sw_token(self) -> str:
        return self._swcli_config.get("controller", {}).get("token", "")

    def info(self, swmp: str) -> None:
        _manifest = self.get_swmp_info(*self._parse_swmp(swmp))
        _config_panel = Panel(Pretty(_manifest, expand_all=True), title="inspect _manifest.yaml / model.yaml info")
        Console().print(_config_panel)
        #TODO: add workdir tree

    def get_swmp_info(self, _name: str, _version: str) -> dict:
        _workdir = self.workdir / _name / _version
        _swmp_path = self.pkgdir / _name / (_version if _version == LATEST_TAG else f"{_version}.swmp")
        if _workdir.exists():
            _manifest = yaml.safe_load((_workdir / DEFAULT_MANIFEST_NAME).open())
            _model = yaml.safe_load((_workdir / DEFAULT_MODEL_YAML_NAME).open())
        else:
            with TarFS(str(_swmp_path.resolve())) as tar:
                _manifest = yaml.safe_load(tar.open(DEFAULT_MANIFEST_NAME))
                _model = yaml.safe_load(tar.open(DEFAULT_MODEL_YAML_NAME))
        _manifest.update(_model)
        _manifest["workdir"] = str(_workdir.resolve())
        _manifest["pkg"] = str(_swmp_path.resolve())
        return _manifest

    def gc(self, dry_run=False) -> None:
        pass

    def delete(self, swmp) -> None:
        _model, _version = self._parse_swmp(swmp)

        pkg_fpath = self.pkgdir / _model / _version
        if pkg_fpath.exists():
            if _version == LATEST_TAG:
                try:
                    pkg_fpath.unlink()
                    pkg_fpath.resolve().unlink()
                except Exception as e:
                    rprint(Pretty(e))
                finally:
                    rprint(f" :collision: delete swmp {pkg_fpath}")
                    rprint(f" :bomb: delete swmp {pkg_fpath.resolve()}")
            else:
                latest = self.pkgdir / _model / LATEST_TAG
                if latest.exists() and latest.resolve() == pkg_fpath.resolve():
                    rprint(f" :collision: delete swmp {latest}")
                rprint(f" :bomb: delete swmp {pkg_fpath}")

        workdir_fpath = self.workdir / _model / _version
        if workdir_fpath.exists() and workdir_fpath.is_dir():
            open_fs(str(workdir_fpath.resolve())).removetree("/")
            workdir_fpath.rmdir()
            rprint(f" :bomb: delete workdir {workdir_fpath}")


        pkg_fpath = self.pkgdir / _model / (_version if _version == LATEST_TAG else f"{_version}.swmp")
        if pkg_fpath.exists():
            latest = self.pkgdir / _model

            pkg_fpath.unlink()