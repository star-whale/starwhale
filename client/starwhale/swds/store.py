from pathlib import Path
import yaml

import click
import requests
from rich.console import Console
from rich import box
from rich.table import Table
from rich.panel import Panel
from rich.pretty import Pretty
from rich import print as rprint

from fs import open_fs

from .dataset import ARCHIVE_SWDS_META
from starwhale.base.store import LocalStorage
from starwhale.consts import DEFAULT_DATASET_YAML_NAME, DEFAULT_MANIFEST_NAME
from starwhale.utils import pretty_bytes
from starwhale.utils.error import NotFoundError

#TODO: refactor Dataset and ModelPackage LocalStorage

class DataSetLocalStore(LocalStorage):

    def list(self, filter: str="") -> None:
        super().list(
            filter=filter,
            title="List dataset(swds) in local storage",
            caption=f"@{self.dataset_dir}",
        )

    def iter_local_swobj(self):
        _fs = open_fs(str(self.dataset_dir.resolve()))

        for name_dir in _fs.scandir("."):
            if not name_dir.is_dir:
                continue

            for ver_dir in _fs.opendir(name_dir.name).scandir("."):
                #TODO: add more validator
                if not ver_dir.is_dir:
                    continue

                _path = self.dataset_dir / name_dir.name / ver_dir.name
                if not all([(_path / n).exists() for n in (DEFAULT_MANIFEST_NAME, DEFAULT_DATASET_YAML_NAME, ARCHIVE_SWDS_META)]):
                    continue

                with (_path / DEFAULT_MANIFEST_NAME).open("r") as f:
                    _manifest = yaml.safe_load(f)
                #TODO: support dataset tag cmd
                _tag = ver_dir.name[:7]

                yield LocalStorage.SWobjMeta(
                    name=name_dir.name, version=ver_dir.name,
                    tag=_tag, environment=_manifest["dep"]["env"],
                    size=pretty_bytes(_manifest.get("dataset_byte_size", 0)),
                    created=_manifest["created_at"],
                )


    def push(self, sw_name: str) -> None:
        ...

    def pull(self, sw_name: str) -> None:
        ...

    def info(self, sw_name: str) -> None:
        _manifest = self._do_get_info(*self._parse_swobj(sw_name))
        _config_panel = Panel(Pretty(_manifest, expand_all=True), title="inspect _manifest.yaml and dataset.yaml info")
        Console().print(_config_panel)
        #TODO: show dataset dir tree view

    def _do_get_info(self, _name: str, _version: str) -> dict:
        _dir = self._guess(self.dataset_dir / _name, _version)
        if not _dir.exists():
            raise NotFoundError(f"{_dir} is not existed")

        _manifest = yaml.safe_load((_dir / DEFAULT_MANIFEST_NAME).open())
        _dataset = yaml.safe_load((_dir / DEFAULT_DATASET_YAML_NAME).open())

        _manifest.update(_dataset)
        _manifest["dataset_dir"] = str(_dir.resolve())
        return _manifest

    def delete(self, sw_name: str) -> None:
        _name, _version = self._parse_swobj(sw_name)
        _dir = self._guess(self.dataset_dir / _name, _version)

        if _dir.exists() and _dir.is_dir():
            click.confirm(f"continue to delete {_dir}?", abort=True)
            open_fs(str(_dir.resolve())).removetree("/")
            _dir.rmdir()
            rprint(f" :bomb: delete dataset dir {_dir}")
        else:
            rprint(f" :diving_mask: {_dir} is not dir, please check, we will not delete it")

    def gc(self, dry_run: bool=False) -> None:
        #TODO: remove intermediated dataset dir
        ...
