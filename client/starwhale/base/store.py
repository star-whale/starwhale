import typing as t
from pathlib import Path
from collections import namedtuple
from abc import ABCMeta, abstractmethod

from rich import box
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.pretty import Pretty
from rich import print as rprint

from starwhale.utils.config import load_swcli_config
from starwhale.utils import fmt_http_server
from starwhale.consts import (
    DEFAULT_MANIFEST_NAME, DEFAULT_MODEL_YAML_NAME, SW_API_VERSION
)


class LocalStorage(object):
    LATEST_TAG = "latest"
    SWobjMeta = namedtuple("SWobjMeta", ["name", "version", "tag", "environment", "size", "created"])

    __metaclass__ = ABCMeta

    def __init__(self, swcli_config: t.Union[dict, None]= None) -> None:
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

    @property
    def dataset_dir(self) -> Path:
        return self.rootdir / "dataset"

    @property
    def sw_remote_addr(self) -> str:
        return self._swcli_config.get("controller", {}).get("remote_addr", "")

    @property
    def _sw_token(self) -> str:
        return self._swcli_config.get("controller", {}).get("token", "")

    def _parse_swobj(self, sw_name:str) -> t.Tuple[str, str]:
        if ":" not in sw_name:
            _name, _version = sw_name, self.LATEST_TAG
        else:
            if sw_name.count(":") > 1:
                raise Exception(f"{sw_name} format wrong, use [name]:[version]")
            _name, _version = sw_name.split(":")

        return _name, _version

    def _guess(self, rootdir: Path, name: str) -> Path:
        #TODO: support more guess method, such as tag
        _path = rootdir / name
        if _path.exists():
            return _path

        for d in rootdir.iterdir():
            if d.name.startswith(name) or name.startswith(d.name):
                return d
        else:
            return _path

    @abstractmethod
    def list(self, filter: str="", title: str="", caption: str="") -> None:
        title = title or "List StarWhale obj[swmp|swds] in local storage"
        caption = caption or f"@{self.rootdir}"

        table = Table(title=title, caption=caption, box=box.SIMPLE)
        table.add_column("Name", justify="right", style="cyan", no_wrap=False)
        table.add_column("Version", style="magenta")
        table.add_column("Tag", style="magenta")
        table.add_column("Size", style="magenta")
        table.add_column("Environment", style="magenta")
        table.add_column("Created", justify="right")

        for s in self.iter_local_swobj():
            table.add_row(s.name, s.version, s.tag, s.size, s.environment, s.created)

        Console().print(table)

    @abstractmethod
    def iter_local_swobj(self) -> "LocalStorage.SWobjMeta" :
        raise NotImplementedError

    @abstractmethod
    def push(self, sw_name: str) -> None:
        raise NotImplementedError

    @abstractmethod
    def pull(self, sw_name: str) -> None:
        raise NotImplementedError

    @abstractmethod
    def info(self, sw_name: str) -> None:
        raise NotImplementedError

    @abstractmethod
    def delete(self, sw_name: str) -> None:
        raise NotImplementedError

    @abstractmethod
    def gc(self, dry_run: bool=False) -> None:
        raise NotImplementedError