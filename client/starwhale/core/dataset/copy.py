from __future__ import annotations

import os
import copy
import queue
import typing as t
import tempfile
import threading
from http import HTTPStatus

import yaml
from rich.progress import (
    TaskID,
    Progress,
    BarColumn,
    TextColumn,
    SpinnerColumn,
    TimeElapsedColumn,
)

from starwhale.utils import console, now_str, load_yaml
from starwhale.consts import (
    FileDesc,
    HTTPMethod,
    CREATED_AT_KEY,
    SHORT_VERSION_CNT,
    STANDALONE_INSTANCE,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import ensure_file
from starwhale.base.type import DatasetChangeMode
from starwhale.utils.error import NotFoundError
from starwhale.base.bundle_copy import BundleCopy, _UploadPhase
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.store import DatasetStorage

from .tabular import TabularDataset, DatastoreRevision, TabularDatasetRow

# local storage scheme: no-scheme or file://
_LOCAL_STORAGE_SCHEMES = ("", "file")


class DatasetCopy(BundleCopy):
    def __init__(
        self,
        src_uri: str | Resource,
        dest_uri: str,
        cache_size: int = 200,
        workers: int = 12,
        **kw: t.Any,
    ) -> None:
        super().__init__(src_uri, dest_uri, typ=ResourceType.dataset, **kw)

        self._workers = workers
        self._copy_mode = kw.get("mode", DatasetChangeMode.PATCH)

        self._processing_queue: queue.Queue[TabularDatasetRow | None] = queue.Queue(
            maxsize=cache_size
        )
        self._lock = threading.Lock()
        self._uris_lock_map: t.Dict[str, threading.Lock] = {}
        self._blobs_uri_map: t.Dict[str, str] = {}

    def _check_dataset_existed(self, uri: Resource) -> bool:
        dataset_name = uri.name
        if uri.instance.is_cloud:
            # TODO simplify remote resource request without join uri manually
            ok, _ = self.do_http_request_simple_ret(
                path=f"/project/{uri.project.id}/dataset/{dataset_name}",
                method=HTTPMethod.HEAD,
                instance=uri.instance,
                ignore_status_codes=[HTTPStatus.NOT_FOUND],
            )
            return ok
        else:
            dataset_dir = (
                self._sw_config.rootdir / uri.project.id / "dataset" / dataset_name
            )
            return (dataset_dir / DEFAULT_MANIFEST_NAME).exists()

    def do_copy_bundle(self) -> None:
        if (
            not self.force
            and self.dest_uri.version
            and self._check_version_existed(self.dest_uri)
        ):
            console.print(f":tea: {self.dest_uri} was already existed, skip copy")
            return

        if not self._check_version_existed(self.src_uri):
            raise NotFoundError(f"src dataset not found: {self.src_uri}")

        console.print(
            f":construction: start to copy[{self._copy_mode.value}] {self.src_uri} -> {self.dest_uri}"
        )

        src = TabularDataset(
            name=self.src_uri.name,
            project=self.src_uri.project,
        )
        dest = TabularDataset(
            name=self.dest_uri.name or self.src_uri.name,
            project=self.dest_uri.project,
        )

        try:
            self._do_dataset_copy(src=src, dest=dest)
        finally:
            src.close()
            dest.close()

        remote_url = self._get_remote_bundle_console_url()
        console.print(f":tea: console url of the remote bundle: {remote_url}")

    def _do_row_update(
        self,
        progress: Progress,
        task_id: TaskID,
        dest: TabularDataset,
        exceptions: t.List[Exception],
    ) -> None:
        try:
            while True:
                row = self._processing_queue.get(block=True)
                if row is None:
                    break

                if not isinstance(row, TabularDatasetRow):
                    console.warn(f"row is not TabularDatasetRow: {row} - {type(row)}")
                    continue

                for artifact in row.artifacts:
                    link = artifact.link
                    if not link or link.scheme not in _LOCAL_STORAGE_SCHEMES:
                        continue

                    link.uri = self._do_copy_blob(link.uri)

                dest.put(row)
                progress.update(task_id, advance=1, refresh=True)
        except Exception as e:
            console.print_exception()
            exceptions.append(e)
            raise

    def _do_dataset_copy(
        self,
        src: TabularDataset,
        dest: TabularDataset,
    ) -> None:
        if (
            self._copy_mode == DatasetChangeMode.OVERWRITE
            and self._check_dataset_existed(self.dest_uri)
        ):
            console.print(":horse: prepare to overwrite dest dataset")
            # TODO: use datastore high performance api to delete all rows
            for row in dest.scan():
                dest.delete(row.id)

        console.print(":bird: copy dataset rows")
        with Progress(
            SpinnerColumn(),
            BarColumn(),
            TextColumn("{task.completed}"),
            TimeElapsedColumn(),
            console=console.rich_console,
            refresh_per_second=0.2,
        ) as progress:
            # TODO: get total rows as progress total
            task_id = progress.add_task(
                ":cookie: update rows and copy artifacts...", total=None
            )

            row_updater_exceptions: t.List[Exception] = []
            row_updater_threads = []
            for i in range(0, self._workers):
                _t = threading.Thread(
                    name=f"row-updater-{i}",
                    target=self._do_row_update,
                    args=(progress, task_id, dest, row_updater_exceptions),
                    daemon=True,
                )
                _t.start()
                row_updater_threads.append(_t)

            src_rows = 0
            for row in src.scan():
                src_rows += 1
                if row is not None:
                    self._processing_queue.put(row)

            for _ in range(0, self._workers):
                self._processing_queue.put(None)

            for _t in row_updater_threads:
                _t.join()

            if row_updater_exceptions:
                raise RuntimeError(
                    f"row updater threads raise exceptions({len(row_updater_exceptions)}): {row_updater_exceptions}"
                )

            progress.update(
                task_id,
                description=":white_check_mark: copy rows done",
                refresh=True,
                completed=src_rows,
            )

        console.print(":kangaroo: update dataset info")
        dest._info = copy.deepcopy(src.info)

        dataset_revision, info_revision = dest.flush()
        console.print(":tiger: make version for dest instance")
        if dest.instance_name == STANDALONE_INSTANCE:
            self._make_standalone_version(dataset_revision, info_revision)
        else:
            self._make_cloud_version(dataset_revision, info_revision)

    def _make_standalone_version(
        self, dataset_revision: str, info_revision: str
    ) -> None:
        r = self.do_http_request(
            path=f"/project/{self.src_uri.project.id}/dataset/{self.src_uri.name}",
            instance=self.src_uri.instance,
            params={"versionUrl": self.src_uri.version},
        ).json()

        manifest = yaml.safe_load(r["data"]["versionMeta"])
        manifest[CREATED_AT_KEY] = now_str()
        manifest.update(
            DatastoreRevision(data=dataset_revision, info=info_revision).asdict()
        )

        uri = self.dest_uri
        snapshot_dir = self._get_versioned_resource_path(uri)
        ensure_file(
            snapshot_dir / DEFAULT_MANIFEST_NAME, yaml.safe_dump(manifest), parents=True
        )

        StandaloneTag(uri).add_fast_tag()

    def _make_cloud_version(self, dataset_revision: str, info_revision: str) -> None:
        dataset_name = self.dest_uri.name or self.src_uri.name
        params = {
            "swds": f"{dataset_name}:{self.src_uri.name}",
            "project": self.dest_uri.project.id,
            "force": "1",  # use force=1 to make http retry happy, we check dataset existence in advance
        }
        url_path = self._get_remote_bundle_api_url()
        snapshot_dir = self._get_versioned_resource_path(self.src_uri)
        manifest = load_yaml(snapshot_dir / DEFAULT_MANIFEST_NAME)
        manifest[CREATED_AT_KEY] = now_str()
        manifest.update(
            DatastoreRevision(data=dataset_revision, info=info_revision).asdict()
        )
        _, tmp_path = tempfile.mkstemp()
        try:
            ensure_file(tmp_path, yaml.dump(manifest), parents=True)

            # TODO: use dataset create api
            r = self.do_multipart_upload_file(
                url_path=url_path,
                file_path=tmp_path,
                instance=self.dest_uri.instance,
                params={
                    "phase": _UploadPhase.MANIFEST,
                    "desc": FileDesc.MANIFEST.name,
                    **params,
                },
                use_raise=True,
            )
            self.do_http_request(
                path=url_path,
                method=HTTPMethod.POST,
                instance=self.dest_uri.instance,
                data={
                    "phase": _UploadPhase.END,
                    "uploadId": r.json()["data"]["uploadId"],
                    **params,
                },
                use_raise=True,
                disable_default_content_type=True,
            )
        finally:
            os.unlink(tmp_path)

    def _do_download_blob_to_object_store(self, remote_uri: str) -> str:
        hash_name = remote_uri.strip().strip("/").split("/")[-1]
        local_blob_path = DatasetStorage._get_object_store_path(hash_name)
        console.debug(f":down_arrow: download blob {hash_name[:SHORT_VERSION_CNT]}")

        if not local_blob_path.exists():
            self.do_download_file(
                url_path=f"/project/{self.src_uri.project.id}/dataset/{self.src_uri.name}/hashedBlob/{hash_name}",
                dest_path=local_blob_path,
                instance=self.src_uri.instance,
            )
        return hash_name

    def _do_upload_blob_from_object_store(self, local_hashed_uri: str) -> str:
        local_blob_path = DatasetStorage._get_object_store_path(local_hashed_uri)
        url_path = f"/project/{self.dest_uri.project.id}/dataset/{self.src_uri.name}/hashedBlob/{local_hashed_uri}"
        console.debug(f":up_arrow: upload blob {local_hashed_uri[:SHORT_VERSION_CNT]}")

        r = self.do_http_request(
            path=url_path,
            instance=self.dest_uri.instance,
            method=HTTPMethod.HEAD,
            ignore_status_codes=[HTTPStatus.NOT_FOUND],
        )
        if r.status_code == HTTPStatus.OK:
            return r.headers["X-SW-LOCAL-STORAGE-URI"]  # type: ignore

        remote_uri = self.do_multipart_upload_file(
            url_path=url_path,
            file_path=local_blob_path,
            instance=self.dest_uri.instance,
            use_raise=True,
        ).json()["data"]
        return remote_uri  # type: ignore

    def _do_copy_blob(self, uri: str) -> str:
        with self._lock:
            if self._uris_lock_map.get(uri) is None:
                self._uris_lock_map[uri] = threading.Lock()

        with self._uris_lock_map[uri]:
            if self._blobs_uri_map.get(uri) is not None:
                return self._blobs_uri_map[uri]

            if self.dest_uri.instance.is_cloud:
                dest_uri = self._do_upload_blob_from_object_store(uri)
            else:
                dest_uri = self._do_download_blob_to_object_store(uri)

            self._blobs_uri_map[uri] = dest_uri
            return dest_uri
