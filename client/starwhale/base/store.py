import typing as t
from pathlib import Path
from abc import ABCMeta, abstractmethod

from rich import box
from rich.table import Table

from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils import console


class LocalStorage(SWCliConfigMixed):
    LATEST_TAG = "latest"

    class SWobjMeta(t.NamedTuple):
        name: str
        version: str
        tag: str
        environment: str
        size: str
        generate: str
        created: str

    __metaclass__ = ABCMeta

    def __init__(self, swcli_config: t.Optional[t.Dict[str, t.Any]] = None) -> None:
        super().__init__(swcli_config)
        self._console = console

    def _parse_swobj(self, sw_name: str) -> t.Tuple[str, str]:
        if ":" not in sw_name:
            _name, _version = sw_name, self.LATEST_TAG
        else:
            if sw_name.count(":") > 1:
                raise Exception(f"{sw_name} format wrong, use [name]:[version]")
            _name, _version = sw_name.split(":")

        return _name, _version

    def _guess(self, rootdir: Path, name: str) -> Path:
        # TODO: support more guess method, such as tag
        _path = rootdir / name
        if _path.exists():
            return _path

        for d in rootdir.iterdir():
            if d.name.startswith(name) or name.startswith(d.name):
                return d
        else:
            return _path

    @abstractmethod
    def list(self, filter: str = "", title: str = "", caption: str = "") -> None:
        title = title or "List StarWhale obj[swmp|swds] in local storage"
        caption = caption or f"@{self.rootdir}"

        table = Table(title=title, caption=caption, box=box.SIMPLE, expand=True)
        table.add_column("Name", justify="right", style="cyan", no_wrap=False)
        table.add_column("Version", style="magenta")
        table.add_column("Tag", style="magenta")
        table.add_column("Size", style="magenta")
        table.add_column("Environment", style="magenta")
        table.add_column("Generate", style="magenta")
        table.add_column("Created", justify="right")

        for s in self.iter_local_swobj():
            table.add_row(
                s.name, s.version, s.tag, s.size, s.environment, s.generate, s.created
            )

        self._console.print(table)

    @abstractmethod
    def iter_local_swobj(self) -> t.Generator["LocalStorage.SWobjMeta", None, None]:
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
    def gc(self, dry_run: bool = False) -> None:
        raise NotImplementedError
