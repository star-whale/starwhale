from http import HTTPStatus
import yaml
from pathlib import Path
import sys

import requests

from starwhale.consts import (
    DEFAULT_MANIFEST_NAME,
    SW_API_VERSION,
)
from starwhale.utils.http import wrap_sw_error_resp, upload_file
from starwhale.utils import console, fmt_http_server
from starwhale.utils.error import NotFoundError


TMP_FILE_BUFSIZE = 8192


class _UploadPhase:
    MANIFEST = "MANIFEST"
    BLOB = "BLOB"
    END = "END"
    CANCEL = "CANCEL"


class BundleCopy(object):
    def __init__(self) -> None:
        self.sw_remote_addr = ""
        self.sw_token = ""

    def dataset_push(
        self, sw_name: str, project: str = "", force: bool = False
    ) -> None:
        url = f"{self.sw_remote_addr}/api/{SW_API_VERSION}/project/dataset/push"

        _name = ""
        # _name, _version = self._parse_swobj(sw_name)
        # _dir, _ = self._guess(self.dataset_dir / _name, _version)
        _dir = Path()
        if not _dir.exists():
            console.print(
                f"[red]failed to push {sw_name}[/], because of {_dir} not found"
            )
            sys.exit(1)

        # TODO: refer to docker push
        console.print(" :fire: try to push swds...")
        _manifest_path = _dir / DEFAULT_MANIFEST_NAME
        _swds = f"{_name}:{_dir.name}"
        _headers = {"Authorization": self.sw_token}

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
        console.print(f"\t :arrow_up: {DEFAULT_MANIFEST_NAME} :ok:")
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
            console.print(f"\t :arrow_up: {_fp.name} :ok:")

        # TODO: parallel upload
        try:
            from starwhale.core.dataset.dataset import ARCHIVE_SWDS_META

            for p in [_dir / "data" / n for n in _manifest["signature"]] + [
                _dir / ARCHIVE_SWDS_META
            ]:
                _upload_blob(p)
        except Exception as e:
            console.print(
                f"when upload blobs, we meet Exception{e}, will cancel upload"
            )
            r = requests.post(
                url,
                data={"swds": _swds, "project": project, "phase": _UploadPhase.CANCEL},
                headers=_headers,
            )
            wrap_sw_error_resp(r, "cancel", use_raise=True)
        else:
            console.print(" :clap: :clap: all uploaded.")
            r = requests.post(
                url,
                data={"swds": _swds, "project": project, "phase": _UploadPhase.END},
                headers=_headers,
            )
            wrap_sw_error_resp(r, "end", use_raise=True)

    def push(self, swmp: str, project: str = "", force: bool = False) -> None:
        # TODO: add more restful api for project, /api/v1/project/{project_id}/model/push
        url = f"{self.sw_remote_addr}/api/{SW_API_VERSION}/project/model/push"

        _spath, _full_swmp = self._get_swmp_path(swmp)  # type: ignore
        if not _spath.exists():
            console.print(
                f"[red]failed to push {_full_swmp}[/], because of {_spath} not found"
            )
            sys.exit(1)

        console.print(f":fire: try to push swmp({_full_swmp})...")
        upload_file(
            url=url,
            fpath=_spath,
            fields={
                "swmp": _full_swmp,
                "project": project,
                "force": "1" if force else "0",
            },
            headers={"Authorization": self.sw_token},
            exit=True,
        )
        console.print(" :clap: push done.")

    def pull(
        self, swmp: str, project: str = "", server: str = "", force: bool = False
    ) -> None:
        server = server.strip() or self.sw_remote_addr
        server = fmt_http_server(server)
        url = f"{server}/api/{SW_API_VERSION}/project/model/pull"

        # _spath, _ = self._get_swmp_path(swmp)
        _spath = Path()
        if _spath.exists() and not force:
            console.print(f":ghost: {swmp} is already existed, skip pull")
            return

        # TODO: add progress bar and rich live
        # TODO: multi phase for pull swmp
        # TODO: get size in advance
        console.print(f"try to pull {swmp}")
        with requests.get(
            url,
            stream=True,
            params={"swmp": swmp, "project": project},
            headers={"Authorization": self.sw_token},
        ) as r:
            if r.status_code == HTTPStatus.OK:
                with _spath.open("wb") as f:
                    for chunk in r.iter_content(chunk_size=TMP_FILE_BUFSIZE):
                        f.write(chunk)
                console.print(":clap: pull completed")
            else:
                wrap_sw_error_resp(r, "pull failed", exit=True)
