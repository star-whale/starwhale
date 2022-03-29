from collections import namedtuple
from pathlib import Path
import sys
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
from starwhale.consts import (
    DEFAULT_DATASET_YAML_NAME, DEFAULT_MANIFEST_NAME, SW_API_VERSION
)
from starwhale.utils import fmt_http_server, pretty_bytes
from starwhale.utils.error import NotFoundError

#TODO: refactor Dataset and ModelPackage LocalStorage

_UPLOAD_PHASE = namedtuple("_UPLOAD_PHASE", ["MANIFEST", "BLOB", "END", "CANCEL"])(
    "manifest", "blob", "end", "cancel"
)


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
        server = fmt_http_server(self.sw_remote_addr)
        url = f"{server}/{SW_API_VERSION}/dataset/push"

        _name, _version = self._parse_swobj(sw_name)
        _dir = self._guess(self.dataset_dir / _name, _version)
        if not _dir.exists():
            rprint(f"[red]failed to push {sw_name}[/], because of {_dir} not found")
            sys.exit(1)

        #TODO: refer to docker push
        rprint("try to push swds...")
        _manifest_path = _dir / DEFAULT_MANIFEST_NAME
        _swds = f"{_name}:{_dir.name}"
        _headers = {"Authorization": self._sw_token}

        #TODO: use rich progress
        r = requests.post(
            url,
            data={"swds": _swds, "phase": _UPLOAD_PHASE.MANIFEST},
            files={"file": _manifest_path.open("rb")},
            headers=_headers,
        )
        r.raise_for_status()
        rprint(f" :climbing: {_UPLOAD_PHASE.MANIFEST}:{_manifest_path} uploaded")
        upload_id = r.json().get("upload_id")
        if not upload_id:
            raise Exception("get invalid upload_id")
        _headers["X-SW-UPLOAD-ID"] = upload_id

        _manifest = yaml.safe_load(_manifest_path.open())

        #TODO: add retry deco
        def _upload_blob(_fp: Path):
            if not _fp.exists():
                raise NotFoundError(f"{_fp} not found")

            r = requests.post(
                url,
                data={"swds": _swds, "phase": _UPLOAD_PHASE.BLOB},
                files={"file": _fp.open("rb")},
                headers=_headers
            )
            r.raise_for_status()
            rprint(f" :clap: {_fp.name} uploaded")

        #TODO: parallel upload
        try:
            for p in [_dir / "data" / n for n in _manifest["signature"]] + [_dir / ARCHIVE_SWDS_META]:
                _upload_blob(p)
        except Exception as e:
            rprint(f"when upload blobs, we meet Exception{e}, will cancel upload")
            requests.post(url, data={"swds": _swds, "phase": _UPLOAD_PHASE.CANCEL}, headers=_headers).raise_for_status()
        else:
            print(" :clap: :clap: all uploaded.")
            requests.post(url, data={"swds": _swds, "phase": _UPLOAD_PHASE.END}, headers=_headers).raise_for_status()

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
