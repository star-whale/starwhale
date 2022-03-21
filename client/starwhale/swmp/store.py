from pathlib import Path
from collections import namedtuple
import yaml

from loguru import logger
from rich.table import Table
from rich.console import Console
from rich import box
from fs import open_fs
from fs.tarfs import TarFS

from starwhale.utils.config import load_swcli_config
from starwhale.consts import DEFAULT_MANIFEST_NAME

SwmpMeta = namedtuple("SwmpMeta", ["model", "version", "tag", "environment", "created"])


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

        table = Table(title="List swmp in local storage", caption=f"storage @ {self.pkgdir}",
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
                if _fname != "latest" and not _fname.endswith(".swmp"):
                    continue

                _path = self.pkgdir / mdir.name / _fname
                _manifest = self._load_swmp_manifest(str(_path.resolve()))
                _tag = _fname if _fname == "latest" else ""

                yield SwmpMeta(model=mdir.name, version=_manifest["version"], tag=_tag,
                               environment=_manifest["dep"]["env"], created=_manifest["created_at"])

    def _load_swmp_manifest(self, fpath) -> dict:
        with TarFS(fpath) as tar:

            return yaml.safe_load(tar.open(DEFAULT_MANIFEST_NAME))

    def push(self, swmp) -> None:
        pass

    def pull(self, swmp) -> None:
        pass

    def info(self, swmp) -> None:
        pass

    def gc(self, dry_run=False) -> None:
        pass

    def delete(self, swmp) -> None:
        pass