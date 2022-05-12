from pathlib import Path
import sys
import yaml
import typing as t

import click
import requests
from rich.panel import Panel
from rich.pretty import Pretty

from fs import open_fs

from starwhale.base.store import LocalStorage
from starwhale.consts import (
    DEFAULT_DATASET_YAML_NAME,
    DEFAULT_MANIFEST_NAME,
    SHORT_VERSION_CNT,
    SW_API_VERSION,
)
from starwhale.utils.http import wrap_sw_error_resp, upload_file
from starwhale.utils.fs import empty_dir
from starwhale.utils import pretty_bytes
from starwhale.utils.error import NotFoundError

# TODO: refactor Dataset and ModelPackage LocalStorage


class _UploadPhase:
    MANIFEST = "MANIFEST"
    BLOB = "BLOB"
    END = "END"
    CANCEL = "CANCEL"


class DataSetLocalStore(LocalStorage):
    def list(self, filter: str = "", title: str = "", caption: str = "") -> None:
        super().list(
            filter=filter,
            title="List dataset(swds) in local storage",
            caption=f"@{self.dataset_dir}",
        )

    def iter_local_swobj(self) -> t.Generator[LocalStorage.SWobjMeta, None, None]:
        from .dataset import ARCHIVE_SWDS_META

        if not self.dataset_dir.exists():
            return

        _fs = open_fs(str(self.dataset_dir.resolve()))
        for name_dir in _fs.scandir("."):
            if not name_dir.is_dir:
                continue

            for ver_dir in _fs.opendir(name_dir.name).scandir("."):
                # TODO: add more validator
                if not ver_dir.is_dir:
                    continue

                _path = self.dataset_dir / name_dir.name / ver_dir.name
                if not all(
                    [
                        (_path / n).exists()
                        for n in (
                            DEFAULT_MANIFEST_NAME,
                            DEFAULT_DATASET_YAML_NAME,
                            ARCHIVE_SWDS_META,
                        )
                    ]
                ):
                    continue

                with (_path / DEFAULT_MANIFEST_NAME).open("r") as f:
                    _manifest = yaml.safe_load(f)
                # TODO: support dataset tag cmd
                _tag = ver_dir.name[:SHORT_VERSION_CNT]

                yield LocalStorage.SWobjMeta(
                    name=name_dir.name,
                    version=ver_dir.name,
                    tag=_tag,
                    environment=_manifest["dep"]["env"],
                    size=pretty_bytes(_manifest.get("dataset_byte_size", 0)),
                    generate="",
                    created=_manifest["created_at"],
                )

    def push(self, sw_name: str, project: str = "", force: bool = False) -> None:
        url = f"{self.sw_remote_addr}/api/{SW_API_VERSION}/project/dataset/push"

        _name, _version = self._parse_swobj(sw_name)
        _dir = self._guess(self.dataset_dir / _name, _version)
        if not _dir.exists():
            self._console.print(
                f"[red]failed to push {sw_name}[/], because of {_dir} not found"
            )
            sys.exit(1)

        # TODO: refer to docker push
        self._console.print(" :fire: try to push swds...")
        _manifest_path = _dir / DEFAULT_MANIFEST_NAME
        _swds = f"{_name}:{_dir.name}"
        _headers = {"Authorization": self._sw_token}

        # TODO: use rich progress
        r = upload_file(
            url=url,
            fpath=_manifest_path,
            fields={
                "swds": _swds,
                "phase": _UploadPhase.MANIFEST,
                "project": project,
                "force": "1" if force else "0",
            },
            headers=_headers,
            exit=True,
        )
        self._console.print(f"\t :arrow_up: {DEFAULT_MANIFEST_NAME} :ok:")
        upload_id = r.json().get("data", {}).get("upload_id")
        if not upload_id:
            raise Exception("get invalid upload_id")
        _headers["X-SW-UPLOAD-ID"] = upload_id
        _manifest = yaml.safe_load(_manifest_path.open())

        # TODO: add retry deco
        def _upload_blob(_fp: Path) -> None:
            if not _fp.exists():
                raise NotFoundError(f"{_fp} not found")

            upload_file(
                url=url,
                fpath=_fp,
                fields={
                    "swds": _swds,
                    "phase": _UploadPhase.BLOB,
                },
                headers=_headers,
                use_raise=True,
            )
            self._console.print(f"\t :arrow_up: {_fp.name} :ok:")

        # TODO: parallel upload
        try:
            from .dataset import ARCHIVE_SWDS_META

            for p in [_dir / "data" / n for n in _manifest["signature"]] + [
                _dir / ARCHIVE_SWDS_META
            ]:
                _upload_blob(p)
        except Exception as e:
            self._console.print(
                f"when upload blobs, we meet Exception{e}, will cancel upload"
            )
            r = requests.post(
                url,
                data={"swds": _swds, "project": project, "phase": _UploadPhase.CANCEL},
                headers=_headers,
            )
            wrap_sw_error_resp(r, "cancel", use_raise=True)
        else:
            self._console.print(" :clap: :clap: all uploaded.")
            r = requests.post(
                url,
                data={"swds": _swds, "project": project, "phase": _UploadPhase.END},
                headers=_headers,
            )
            wrap_sw_error_resp(r, "end", use_raise=True)

    def pull(self, sw_name: str) -> None:
        ...

    def info(self, sw_name: str) -> None:
        _manifest = self._do_get_info(*self._parse_swobj(sw_name))
        _config_panel = Panel(
            Pretty(_manifest, expand_all=True),
            title="inspect _manifest.yaml and dataset.yaml info",
        )
        self._console.print(_config_panel)
        # TODO: show dataset dir tree view

    def _do_get_info(self, _name: str, _version: str) -> t.Dict[t.Any, t.Any]:
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
            empty_dir(_dir)
            self._console.print(f":bomb delete dataset dir: {_dir}")
        else:
            self._console.print(
                f":diving_mask: not found or no dir for {_dir}, skip to delete it"
            )

    def gc(self, dry_run: bool = False) -> None:
        # TODO: remove intermediated dataset dir
        ...
