import typing as t
import os.path
from copy import deepcopy
from http import HTTPStatus
from pathlib import Path
from concurrent.futures import wait, ThreadPoolExecutor

from rich.progress import (
    TaskID,
    Progress,
    BarColumn,
    TextColumn,
    SpinnerColumn,
    TimeElapsedColumn,
    TotalFileSizeColumn,
    TransferSpeedColumn,
)

from starwhale.utils import console, load_yaml
from starwhale.consts import (
    HTTPMethod,
    AUTH_ENV_FNAME,
    VERSION_PREFIX_CNT,
    STANDALONE_INSTANCE,
    DEFAULT_MANIFEST_NAME,
    DUMPED_SWDS_META_FNAME,
    ARCHIVED_SWDS_META_FNAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import URIType, InstanceType, get_bundle_type_by_uri
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import NotFoundError, NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.model.store import ModelStorage
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.runtime.store import RuntimeStorage

TMP_FILE_BUFSIZE = 8192

_query_param_map = {
    URIType.DATASET: "swds",
    URIType.MODEL: "swmp",
    URIType.RUNTIME: "runtime",
}


class _UploadPhase:
    MANIFEST = "MANIFEST"
    BLOB = "BLOB"
    END = "END"
    CANCEL = "CANCEL"


class BundleCopy(CloudRequestMixed):
    def __init__(
        self,
        src_uri: str,
        dest_uri: str,
        typ: str,
        force: bool = False,
        **kw: t.Any,
    ) -> None:
        self.src_uri = URI(src_uri, expected_type=typ)
        self.dest_uri = URI(dest_uri, expected_type=URIType.PROJECT)
        self.typ = typ
        self.force = force

        self.bundle_name = self.src_uri.object.name
        self.bundle_version = self._guess_bundle_version()

        self.kw = kw

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
        url = self._get_remote_instance_rc_url(True)
        ok, _ = self.do_http_request_simple_ret(
            path=url,
            method=HTTPMethod.HEAD,
            instance_uri=uri,
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

    def _get_remote_instance_rc_url(self, for_head: bool = False) -> str:
        _obj = self.src_uri.object
        project = self.dest_uri.project
        if self.src_uri.instance_type == InstanceType.CLOUD:
            project = self.src_uri.project
        base = [f"/project/{project}/{self.typ}/{_obj.name}/version/{_obj.version}"]
        if not for_head:
            # uri for head request contains no 'file'
            base.append("file")

        return "/".join(base)

    def _do_upload_bundle_tar(self, progress: Progress) -> None:
        file_path = self._get_target_path(self.src_uri)
        task_id = progress.add_task(
            f":bowling: upload {file_path.name}",
            total=file_path.stat().st_size,
        )

        self.do_multipart_upload_file(
            url_path=self._get_remote_instance_rc_url(),
            file_path=file_path,
            instance_uri=self.dest_uri,
            fields={
                _query_param_map[self.typ]: f"{self.bundle_name}:{self.bundle_version}",
                "project": self.dest_uri.project,
                "force": "1" if self.force else "0",
            },
            use_raise=True,
            progress=progress,
            task_id=task_id,
        )

    def _do_download_bundle_tar(self, progress: Progress) -> None:
        file_path = self._get_target_path(self.dest_uri)
        ensure_dir(os.path.dirname(file_path))
        task_id = progress.add_task(f":bowling: download to {file_path}...")

        self.do_download_file(
            url_path=self._get_remote_instance_rc_url(),
            dest_path=file_path,
            instance_uri=self.src_uri,
            params={
                _query_param_map[self.typ]: f"{self.bundle_name}:{self.bundle_version}",
                "project": self.src_uri.project,
            },
            progress=progress,
            task_id=task_id,
        )

    def do(self) -> None:
        if self._is_existed(self.dest_uri) and not self.force:
            console.print(
                f":tea: {self.dest_uri}-{self.bundle_name}-{self.bundle_version} was already existed, skip copy"
            )
            return

        # TODO: when controller api support dataset head, remove dataset type check
        if self.typ != URIType.DATASET and not self._is_existed(self.src_uri):
            raise NotFoundError(str(self.src_uri))

        console.print(
            f":construction: start to copy {self.src_uri} -> {self.dest_uri}..."
        )

        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            TextColumn("[progress.percentage]{task.percentage:>3.0f}%"),
            TimeElapsedColumn(),
            TotalFileSizeColumn(),
            TransferSpeedColumn(),
            console=console,
            refresh_per_second=0.2,
        ) as progress:
            if self.src_uri.instance_type == InstanceType.STANDALONE:
                if self.typ == URIType.DATASET:
                    self._do_upload_bundle_dir(progress, add_data_uri_header=True)
                else:
                    self._do_upload_bundle_tar(progress)
            else:
                if self.typ == URIType.DATASET:
                    self._do_download_bundle_dir(progress)
                else:
                    self._do_download_bundle_tar(progress)

                _dest_uri = URI.capsulate_uri(
                    instance=STANDALONE_INSTANCE,
                    project=self.dest_uri.project,
                    obj_type=self.typ,
                    obj_name=self.bundle_name,
                    obj_ver=self.bundle_version,
                )
                StandaloneTag(_dest_uri).add_fast_tag()

    def _do_upload_bundle_dir(
        self,
        progress: Progress,
        add_data_uri_header: bool = False,
    ) -> None:
        workdir: Path = self._get_target_path(self.src_uri)
        manifest_path = workdir / DEFAULT_MANIFEST_NAME
        key = _query_param_map[self.typ]
        name = f"{self.bundle_name}:{self.bundle_version}"
        url_path = self._get_remote_instance_rc_url()

        task_id = progress.add_task(
            f":arrow_up: {manifest_path.name}",
            total=manifest_path.stat().st_size,
        )

        # TODO: use rich progress
        r = self.do_multipart_upload_file(
            url_path=url_path,
            file_path=manifest_path,
            instance_uri=self.dest_uri,
            fields={
                key: name,
                "phase": _UploadPhase.MANIFEST,
                "project": self.dest_uri.project,
                "force": "1" if self.force else "0",
            },
            use_raise=True,
            progress=progress,
            task_id=task_id,
        )
        upload_id = r.json().get("data", {}).get("upload_id")
        if not upload_id:
            raise Exception("get invalid upload_id")
        _headers = {"X-SW-UPLOAD-ID": str(upload_id)}
        _manifest = load_yaml(manifest_path)

        # TODO: add retry deco
        def _upload_blob(_fp: Path, _tid: TaskID, _data_uri: str = "") -> None:
            if not _fp.exists():
                raise NotFoundError(f"{_fp} not found")

            _upload_headers = deepcopy(_headers)
            if add_data_uri_header and _data_uri:
                _upload_headers["X-SW-UPLOAD-DATA-URI"] = _data_uri
                _upload_headers["X-SW-UPLOAD-OBJECT-HASH"] = _data_uri

            progress.update(_tid, visible=True)
            self.do_multipart_upload_file(
                url_path=url_path,
                file_path=_fp,
                instance_uri=self.dest_uri,
                fields={
                    key: name,
                    "phase": _UploadPhase.BLOB,
                },
                headers=_upload_headers,
                use_raise=True,
                progress=progress,
                task_id=_tid,
            )

        try:
            _p_map = {}

            for _k in _manifest["signature"]:
                _size, _, _hash = _k.split(":")

                # TODO: head object by hash name at first
                _path = workdir / "data" / _hash[: DatasetStorage.short_sign_cnt]
                _tid = progress.add_task(
                    f":arrow_up: {_path.name}",
                    total=float(_size),
                    visible=False,
                )
                _p_map[_tid] = (_path, _hash)

            _meta_names = [ARCHIVED_SWDS_META_FNAME, DUMPED_SWDS_META_FNAME]
            if self.kw.get("with_auth") and (workdir / AUTH_ENV_FNAME).exists():
                _meta_names.append(AUTH_ENV_FNAME)

            for _n in _meta_names:
                _path = workdir / _n
                _tid = progress.add_task(
                    f":arrow_up: {_path.name}",
                    total=_path.stat().st_size,
                    visible=False,
                )
                _p_map[_tid] = (_path, "")

            with ThreadPoolExecutor(
                max_workers=int(os.environ.get("SW_BUNDLE_COPY_THREAD_NUM", "5"))
            ) as executor:
                futures = [
                    executor.submit(_upload_blob, _p, _tid, _data_uri)
                    for _tid, (_p, _data_uri) in _p_map.items()
                ]
                wait(futures)

        except Exception as e:
            console.print(
                f":confused_face: when upload blobs, we meet Exception{e}, will cancel upload"
            )
            self.do_http_request(
                path=url_path,
                method=HTTPMethod.POST,
                instance_uri=self.dest_uri,
                headers=_headers,
                data={
                    key: name,
                    "project": self.dest_uri.project,
                    "phase": _UploadPhase.CANCEL,
                },
                use_raise=True,
                disable_default_content_type=True,
            )
        else:
            self.do_http_request(
                path=url_path,
                method=HTTPMethod.POST,
                instance_uri=self.dest_uri,
                headers=_headers,
                data={
                    key: name,
                    "project": self.dest_uri.project,
                    "phase": _UploadPhase.END,
                },
                use_raise=True,
                disable_default_content_type=True,
            )

    def _do_download_bundle_dir(self, progress: Progress) -> None:
        _workdir = self._get_target_path(self.dest_uri)
        ensure_dir(_workdir / "data")

        def _download(_target: Path, _part: str, _tid: TaskID) -> None:
            self.do_download_file(
                # TODO: use /project/{self.typ}/pull api
                url_path=self._get_remote_instance_rc_url(),
                dest_path=_target,
                instance_uri=self.src_uri,
                params={
                    "name": self.bundle_name,
                    "version": self.bundle_version,
                    "part_name": _part,
                },
                progress=progress,
                task_id=_tid,
            )

        _manifest_path = _workdir / DEFAULT_MANIFEST_NAME
        _tid = progress.add_task(f":arrow_down: {DEFAULT_MANIFEST_NAME}")
        _download(_manifest_path, DEFAULT_MANIFEST_NAME, _tid)
        _manifest = load_yaml(_manifest_path)

        for _k in _manifest.get("signature", []):
            # TODO: parallel download
            _size, _algo, _hash = _k.split(":")
            if _algo != DatasetStorage.object_hash_algo:
                raise NoSupportError(f"download file hash algorithm {_algo}")

            _tid = progress.add_task(
                f":arrow_down: {_hash[:DatasetStorage.short_sign_cnt]}",
                total=float(_size),
            )

            _dest = DatasetStorage._get_object_store_path(_hash)
            if not _dest.exists() or self.force:
                _download(_dest, _hash, _tid)
            Path(_workdir / "data" / _hash[: DatasetStorage.short_sign_cnt]).symlink_to(
                _dest
            )

        # FIXME: copy .auth_env from controller? security issue
        for _f in [ARCHIVED_SWDS_META_FNAME, DUMPED_SWDS_META_FNAME]:
            _tid = progress.add_task(f":arrow_down: {_f}")
            _download(_workdir / _f, _f, _tid)
