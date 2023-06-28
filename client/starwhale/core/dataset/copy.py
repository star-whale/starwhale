from __future__ import annotations

import os
import copy
import typing as t
import tempfile
from http import HTTPStatus
from concurrent.futures import as_completed, ThreadPoolExecutor

import yaml
from rich.progress import (
    Progress,
    BarColumn,
    TextColumn,
    SpinnerColumn,
    TimeElapsedColumn,
    TotalFileSizeColumn,
    TransferSpeedColumn,
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

from .tabular import TabularDataset, DatastoreRevision

# local storage scheme: no-scheme or file://
_LOCAL_STORAGE_SCHEMES = ("", "file")


class DatasetCopy(BundleCopy):
    def __init__(self, src_uri: Resource, dest_uri: str, **kw: t.Any) -> None:
        super().__init__(
            src_uri,
            dest_uri,
            typ=ResourceType.dataset,
            **kw,
        )
        self._max_workers = int(os.environ.get("SW_BUNDLE_COPY_THREAD_NUM", "5"))
        self._copy_mode = kw.get("mode", DatasetChangeMode.PATCH)

    def _check_dataset_existed(self, uri: Resource) -> bool:
        dataset_name = uri.name
        if uri.instance.is_cloud:
            # TODO simplify remote resource request without join uri manually
            ok, _ = self.do_http_request_simple_ret(
                path=f"/project/{uri.project.name}/dataset/{dataset_name}",
                method=HTTPMethod.HEAD,
                instance=uri.instance,
                ignore_status_codes=[HTTPStatus.NOT_FOUND],
            )
            return ok
        else:
            dataset_dir = (
                self._sw_config.rootdir / uri.project.name / "dataset" / dataset_name
            )
            return (dataset_dir / DEFAULT_MANIFEST_NAME).exists()

    def do(self) -> None:
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

        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            TextColumn("[progress.percentage]{task.percentage:>3.0f}%"),
            TimeElapsedColumn(),
            TotalFileSizeColumn(),
            TransferSpeedColumn(),
            console=console.rich_console,
            refresh_per_second=0.2,
        ) as progress:
            src = TabularDataset(
                name=self.src_uri.name,
                project=self.src_uri.project.name,
                instance_name=self.src_uri.instance.url,
                token=self.src_uri.instance.token,
            )

            dest = TabularDataset(
                name=self.dest_uri.name or self.src_uri.name,
                project=self.dest_uri.project.name,
                instance_name=self.dest_uri.instance.url,
                token=self.dest_uri.instance.token,
            )
            try:
                self._do_dataset_copy(src=src, dest=dest, progress=progress)
            finally:
                src.close()
                dest.close()

        remote_url = self._get_remote_bundle_console_url()
        console.print(f":tea: console url of the remote bundle: {remote_url}")

    def _do_dataset_copy(
        self,
        src: TabularDataset,
        dest: TabularDataset,
        progress: Progress,
    ) -> None:
        console.print(":bird: preprocess artifacts link")
        _artifacts_uri_map: t.Dict[str, str] = {}

        for row in src.scan():
            for artifact in row.artifacts:
                link = artifact.link
                if (
                    not link
                    or link.scheme not in _LOCAL_STORAGE_SCHEMES
                    or link.uri in _artifacts_uri_map
                ):
                    continue

                _artifacts_uri_map[link.uri] = ""

        if dest.instance_name == STANDALONE_INSTANCE:
            console.print(f":cat: try to download {len(_artifacts_uri_map)} blobs")
            self._do_download_blobs(_artifacts_uri_map, progress)
        else:
            console.print(f":cat: try to upload {len(_artifacts_uri_map)} blobs")
            self._do_upload_blobs(_artifacts_uri_map, progress)

        console.print(":horse: dump dataset meta from src to dest")

        if (
            self._copy_mode == DatasetChangeMode.OVERWRITE
            and self._check_dataset_existed(self.dest_uri)
        ):
            # TODO: use datastore high performance api to delete all rows
            for row in dest.scan():
                dest.delete(row.id)

        for row in src.scan():
            for artifact in row.artifacts:
                link = artifact.link
                if (
                    not link
                    or link.scheme not in _LOCAL_STORAGE_SCHEMES
                    or link.uri not in _artifacts_uri_map
                ):
                    continue
                link.uri = _artifacts_uri_map[link.uri]
            dest.put(row)

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
            path=f"/project/{self.src_uri.project.name}/dataset/{self.src_uri.name}",
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
            "project": self.dest_uri.project.name,
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

    def _do_download_blobs(
        self,
        artifacts_uri_map: t.Dict[str, str],
        progress: Progress,
    ) -> None:
        with ThreadPoolExecutor(max_workers=self._max_workers) as executor:
            futures = [
                executor.submit(
                    self._do_download_blob_to_object_store,
                    progress,
                    k,
                )
                for k, v in artifacts_uri_map.items()
            ]

            results = [t.result() for t in as_completed(futures)]

            for src_uri, dest_uri in results:
                artifacts_uri_map[src_uri] = dest_uri

    def _do_download_blob_to_object_store(
        self,
        progress: Progress,
        remote_uri: str,
    ) -> t.Tuple[str, str]:
        hash_name = remote_uri.strip().strip("/").split("/")[-1]

        # TODO: get size by head api
        task_id = progress.add_task(
            f":arrow_down: {hash_name[:SHORT_VERSION_CNT]}",
            visible=True,
        )

        local_blob_path = DatasetStorage._get_object_store_path(hash_name)
        if not local_blob_path.exists():
            self.do_download_file(
                url_path=f"/project/{self.src_uri.project.name}/dataset/{self.src_uri.name}/hashedBlob/{hash_name}",
                dest_path=local_blob_path,
                instance=self.src_uri.instance,
                progress=progress,
                task_id=task_id,
            )
        else:
            progress.update(task_id, completed=local_blob_path.stat().st_size)

        return remote_uri, hash_name

    def _do_upload_blob_from_object_store(
        self, progress: Progress, local_hashed_uri: str
    ) -> t.Tuple[str, str]:
        local_blob_path = DatasetStorage._get_object_store_path(local_hashed_uri)
        url_path = f"/project/{self.dest_uri.project.name}/dataset/{self.src_uri.name}/hashedBlob/{local_hashed_uri}"
        blob_size = local_blob_path.stat().st_size

        task_id = progress.add_task(
            f":arrow_up: {local_hashed_uri[:SHORT_VERSION_CNT]}",
            total=blob_size,
            visible=True,
        )

        r = self.do_http_request(
            path=url_path,
            instance=self.dest_uri.instance,
            method=HTTPMethod.HEAD,
            ignore_status_codes=[HTTPStatus.NOT_FOUND],
        )
        if r.status_code == HTTPStatus.OK:
            progress.update(task_id, completed=blob_size)
            return local_hashed_uri, r.headers["X-SW-LOCAL-STORAGE-URI"]

        remote_uri = self.do_multipart_upload_file(
            url_path=url_path,
            file_path=local_blob_path,
            instance=self.dest_uri.instance,
            use_raise=True,
            progress=progress,
            task_id=task_id,
        ).json()["data"]
        return local_hashed_uri, remote_uri

    def _do_upload_blobs(
        self,
        artifacts_uri_map: t.Dict[str, str],
        progress: Progress,
    ) -> None:
        with ThreadPoolExecutor(max_workers=self._max_workers) as executor:
            futures = [
                executor.submit(
                    self._do_upload_blob_from_object_store,
                    progress,
                    k,
                )
                for k, v in artifacts_uri_map.items()
            ]

            results = [t.result() for t in as_completed(futures)]

            for src_uri, dest_uri in results:
                artifacts_uri_map[src_uri] = dest_uri
