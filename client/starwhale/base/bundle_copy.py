from http import HTTPStatus
from pathlib import Path

import yaml

from starwhale.utils import console
from starwhale.consts import HTTPMethod, VERSION_PREFIX_CNT, DEFAULT_MANIFEST_NAME
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import URIType, InstanceType, get_bundle_type_by_uri
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.model.store import ModelStorage
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.runtime.store import RuntimeStorage
from starwhale.core.dataset.dataset import ARCHIVE_SWDS_META

TMP_FILE_BUFSIZE = 8192

_query_param_map = {
    URIType.DATASET: "swds",
    URIType.MODEL: "swmp",
    URIType.RUNTIME: "swrt",
}


class _UploadPhase:
    MANIFEST = "MANIFEST"
    BLOB = "BLOB"
    END = "END"
    CANCEL = "CANCEL"


class BundleCopy(CloudRequestMixed):
    def __init__(
        self, src_uri: str, dest_uri: str, typ: str, force: bool = False
    ) -> None:
        self.src_uri = URI(src_uri, expected_type=typ)
        self.dest_uri = URI(dest_uri, expected_type=URIType.PROJECT)
        self.typ = typ
        self.force = force

        self.bundle_name = self.src_uri.object.name
        self.bundle_version = self._guess_bundle_version()

        self._sw_config = SWCliConfigMixed()
        self._do_validate()

    def _guess_bundle_version(self) -> str:
        if self.src_uri.instance_type == InstanceType.CLOUD:
            return self.src_uri.object.version
        else:
            if self.typ == URIType.DATASET:
                _v = DatasetStorage(self.src_uri).id
            elif self.typ == URIType.MODEL:
                _v = ModelStorage(self.src_uri).id
            elif self.typ == URIType.RUNTIME:
                _v = RuntimeStorage(self.src_uri).id
            else:
                raise NoSupportError(self.typ)

            return _v or self.src_uri.object.version

    def _do_validate(self) -> None:
        if self.typ not in (URIType.DATASET, URIType.MODEL, URIType.RUNTIME):
            raise NoSupportError(f"{self.typ} copy does not work")

        if self.bundle_version == "":
            raise Exception(f"must specify version src:({self.bundle_version})")

    def _check_cloud_obj_existed(self, uri: URI) -> bool:
        # TODO: add more params for project
        # TODO: tune controller api, use unified params name
        ok, _ = self.do_http_request_simple_ret(
            path=f"/project/{self.typ}",
            method=HTTPMethod.HEAD,
            instance_uri=uri,
            params={
                _query_param_map[self.typ]: f"{self.bundle_name}:{self.bundle_version}"
            },
            ignore_status_codes=[HTTPStatus.NOT_FOUND],
        )
        return ok

    def _get_target_path(self, uri: URI) -> Path:
        if uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError(f"{uri} to get target dir path")

        return (
            self._sw_config.rootdir
            / uri.project
            / self.typ
            / self.bundle_name
            / self.bundle_version[:VERSION_PREFIX_CNT]
            / f"{self.bundle_version}{get_bundle_type_by_uri(self.typ)}"
        )

    def _is_existed(self, uri: URI) -> bool:
        if uri.instance_type == InstanceType.CLOUD:
            return self._check_cloud_obj_existed(uri)
        else:
            return self._get_target_path(uri).exists()

    def _do_upload_bundle_tar(self) -> None:
        file_path = self._get_target_path(self.src_uri)
        console.print(f":bowling: upload {file_path}...")

        self.do_multipart_upload_file(
            url_path=f"/project/{self.typ}/push",
            file_path=file_path,
            instance_uri=self.dest_uri,
            fields={
                _query_param_map[self.typ]: f"{self.bundle_name}:{self.bundle_version}",
                "project": self.dest_uri.project,
                "force": "1" if self.force else "0",
            },
            use_raise=True,
        )

    def _do_download_bundle_tar(self) -> None:
        file_path = self._get_target_path(self.dest_uri)
        console.print(f":bowling: download to {file_path}...")

        self.do_download_file(
            url_path=f"/project/{self.typ}/pull",
            dest_path=self._get_target_path(self.dest_uri),
            instance_uri=self.src_uri,
            params={
                _query_param_map[self.typ]: f"{self.bundle_name}:{self.bundle_version}",
                "project": self.src_uri.project,
            },
        )

    def do(self) -> None:
        if self._is_existed(self.dest_uri) and not self.force:
            console.print(
                f":tea: {self.dest_uri}-{self.bundle_name}-{self.bundle_version} was already existed, skip copy"
            )
            return

        # TODO: when controller api support dataset head, remove dataset type check
        if self.typ != URIType.DATASET and self._is_existed(self.src_uri):
            raise NotFoundError(str(self.src_uri))

        console.print(
            f":construction: start to copy {self.src_uri} -> {self.dest_uri}..."
        )

        if self.src_uri.instance_type == InstanceType.STANDALONE:
            if self.typ == URIType.DATASET:
                self._do_upload_bundle_dir()
            else:
                self._do_upload_bundle_tar()
        else:
            if self.typ == URIType.DATASET:
                self._do_download_bundle_dir()
            else:
                self._do_download_bundle_tar()

    def _do_upload_bundle_dir(self) -> None:
        _workdir: Path = self._get_target_path(self.src_uri)
        _manifest_path = _workdir / DEFAULT_MANIFEST_NAME
        _key = _query_param_map[self.typ]
        _name = f"{self.bundle_name}:{self.bundle_version}"
        _url_path = f"/project/{self.typ}/push"

        # TODO: use rich progress
        r = self.do_multipart_upload_file(
            url_path=_url_path,
            file_path=_manifest_path,
            instance_uri=self.dest_uri,
            fields={
                _key: _name,
                "phase": _UploadPhase.MANIFEST,
                "project": self.dest_uri.project,
                "force": "1" if self.force else "0",
            },
            use_raise=True,
        )
        console.print(f"\t :arrow_up: {DEFAULT_MANIFEST_NAME} :ok:")
        upload_id = r.json().get("data", {}).get("upload_id")
        if not upload_id:
            raise Exception("get invalid upload_id")
        _headers = {"X-SW-UPLOAD-ID": upload_id}
        _manifest = yaml.safe_load(_manifest_path.open())

        # TODO: add retry deco
        def _upload_blob(_fp: Path) -> None:
            if not _fp.exists():
                raise NotFoundError(f"{_fp} not found")

            self.do_multipart_upload_file(
                url_path=_url_path,
                file_path=_fp,
                instance_uri=self.dest_uri,
                fields={
                    _key: _name,
                    "phase": _UploadPhase.BLOB,
                },
                headers=_headers,
                use_raise=True,
            )
            console.print(f"\t :arrow_up: {_fp.name} :ok:")

        try:
            from starwhale.core.dataset.dataset import ARCHIVE_SWDS_META

            for p in [_workdir / "data" / n for n in _manifest["signature"]] + [
                _workdir / ARCHIVE_SWDS_META
            ]:
                _upload_blob(p)
        except Exception as e:
            console.print(
                f":confused_face: when upload blobs, we meet Exception{e}, will cancel upload"
            )
            self.do_http_request(
                path=_url_path,
                method=HTTPMethod.POST,
                instance_uri=self.dest_uri,
                headers=_headers,
                data={
                    _key: _name,
                    "project": self.dest_uri.project,
                    "phase": _UploadPhase.CANCEL,
                },
                use_raise=True,
                disable_default_content_type=True,
            )
        else:
            console.print(":cow: end upload")
            self.do_http_request(
                path=_url_path,
                method=HTTPMethod.POST,
                instance_uri=self.dest_uri,
                headers=_headers,
                data={
                    _key: _name,
                    "project": self.dest_uri.project,
                    "phase": _UploadPhase.END,
                },
                use_raise=True,
                disable_default_content_type=True,
            )

    def _do_download_bundle_dir(self) -> None:
        _workdir = self._get_target_path(self.dest_uri)
        console.print(f":bowling: download to {_workdir}...")
        ensure_dir(_workdir)
        ensure_dir(_workdir / "data")

        def _download(_target: Path, _part: str) -> None:
            self.do_download_file(
                # TODO: use /project/{self.typ}/pull api
                url_path=f"/project/{self.typ}",
                dest_path=_target,
                instance_uri=self.src_uri,
                params={
                    "name": self.bundle_name,
                    "version": self.bundle_version,
                    "part_name": _part,
                },
            )
            console.print(f"\t :arrow_down: {_part} :ok:")

        _manifest_path = _workdir / DEFAULT_MANIFEST_NAME
        _download(_manifest_path, DEFAULT_MANIFEST_NAME)
        _download(_workdir / ARCHIVE_SWDS_META, ARCHIVE_SWDS_META)

        _manifest = yaml.safe_load(_manifest_path.open())
        for _k in _manifest.get("signature", {}):
            # TODO: parallel download
            _download(_workdir / "data" / _k, _k)
