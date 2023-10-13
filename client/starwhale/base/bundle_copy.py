from __future__ import annotations

import os
import typing as t
from http import HTTPStatus
from pathlib import Path

import trio
from rich.progress import (
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
    FileNode,
    HTTPMethod,
    SW_BUILT_IN,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import ensure_dir
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import NotFoundError, NoSupportError, FieldTypeOrValueError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.blob.store import LocalFileStore
from starwhale.core.model.copy import upload_model, download_model
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.base.client.api.tag import TagApi

TMP_FILE_BUFSIZE = 8192

_query_param_map = {
    ResourceType.dataset: "swds",
    ResourceType.model: "swmp",
    ResourceType.runtime: "runtime",
}


class _UploadPhase:
    MANIFEST = "MANIFEST"
    BLOB = "BLOB"
    END = "END"
    CANCEL = "CANCEL"


class BundleCopy(CloudRequestMixed):
    def __init__(
        self,
        src_uri: str | Resource,
        dest_uri: str | Resource | Project,
        typ: ResourceType,
        force: bool = False,
        ignore_tags: t.List[str] | None = None,
        **kw: t.Any,
    ) -> None:
        self.src_uri: Resource = (
            Resource(src_uri, typ=typ, refine=True)
            if isinstance(src_uri, str)
            else src_uri
        )
        self.ignore_tags = ignore_tags or []
        if not self.src_uri.version:
            self.src_uri.version = "latest"

        if not self.src_uri.instance.is_local:
            p = kw.get("dest_local_project_uri")
            project = p and Project(p) or None
            dest_uri = (
                f"{self.src_uri.name}/version/{self.src_uri.version}"
                if (dest_uri == "." or dest_uri == "")
                else dest_uri
            )
        else:
            project = None

        dest = dest_uri
        if isinstance(dest_uri, str):
            try:
                dest = Resource(dest_uri, typ=typ, project=project, refine=False)
            except Exception as e:
                if str(e).startswith("invalid uri"):
                    dest = Project(dest_uri)
                else:
                    raise
        if isinstance(dest, Project):
            self.dest_uri = Resource(
                f"{self.src_uri.name}/version/{self.src_uri.version}",
                name=self.src_uri.name,
                typ=typ,
                project=dest,
                refine=False,
            )
        elif isinstance(dest, Resource):
            self.dest_uri = dest
        else:
            raise Exception("invalid dest_uri")  # this can not happen

        self.dest_uri.version = self.src_uri.version
        if self.dest_uri.version == "latest":
            self.dest_uri.version = ""

        self.typ = self.src_uri.typ
        self.force = force
        self.field_flag = _query_param_map[self.typ]
        self.field_value = f"{self.src_uri.name}:{self.src_uri.version}"
        self.kw = kw

        self._sw_config = SWCliConfigMixed()
        self._do_validate()
        self._object_store = LocalFileStore()

    def _do_validate(self) -> None:
        if self.typ not in (
            ResourceType.model,
            ResourceType.dataset,
            ResourceType.runtime,
        ):
            raise NoSupportError(f"{self.typ} copy does not work")

    def _check_cloud_obj_existed(self, rc: Resource) -> bool:
        # TODO: add more params for project
        # TODO: tune controller api, use unified params name
        ok, _ = self.do_http_request_simple_ret(
            path=self._get_remote_bundle_api_url(for_head=True),
            method=HTTPMethod.HEAD,
            instance=rc.instance,
            ignore_status_codes=[HTTPStatus.NOT_FOUND],
        )
        return ok

    def _get_versioned_resource_path(self, uri: Resource) -> Path:
        if not uri.instance.is_local:
            raise NoSupportError(f"{uri} to get target dir path")
        # TODO move it to resource uri or resource store to build up
        return (
            self._sw_config.rootdir
            / uri.project.id
            / self.typ.value
            / uri.name
            / self.src_uri.version[:VERSION_PREFIX_CNT]
            / f"{uri.version}{Resource.get_bundle_type_by_uri(uri.typ)}"
        )

    def _check_version_existed(self, uri: Resource) -> bool:
        if uri.instance.is_cloud:
            return self._check_cloud_obj_existed(uri)
        else:
            return self._get_versioned_resource_path(uri).exists()

    def _get_remote_bundle_console_url(self, with_version: bool = True) -> str:
        if self.src_uri.instance.is_cloud:
            remote = self.src_uri
            resource_name = self.src_uri.name
        else:
            remote = self.dest_uri
            resource_name = self.dest_uri.name or self.src_uri.name

        url = f"{remote.instance.url}/projects/{remote.project.id}/{self.typ.value}s/{resource_name}"
        if with_version:
            url = f"{url}/versions/{self.src_uri.version}/overview"
        return url

    def _get_remote_bundle_api_url(self, for_head: bool = False) -> str:
        version = self.src_uri.version
        if not version:
            raise FieldTypeOrValueError(
                f"cannot fetch version from src uri:{self.src_uri}"
            )

        if self.src_uri.instance.is_cloud:
            project = self.src_uri.project
            resource_name = self.src_uri.name
        else:
            project = self.dest_uri.project
            resource_name = self.dest_uri.name or self.src_uri.name

        if not resource_name:
            raise FieldTypeOrValueError(
                f"cannot fetch {self.typ} resource name from src_uri({self.src_uri}) or dest_uri({self.dest_uri})"
            )

        base = [
            f"/project/{project.id}/{self.typ.value}/{resource_name}/version/{version}"
        ]
        if not for_head:
            # uri for head request contains no 'file'
            base.append("file")

        return "/".join(base)

    def _do_upload_bundle_tar(self, progress: Progress) -> None:
        file_path = self._get_versioned_resource_path(self.src_uri)
        task_id = progress.add_task(
            f":bowling: upload {file_path.name}",
            total=file_path.stat().st_size,
        )

        self.do_multipart_upload_file(
            url_path=self._get_remote_bundle_api_url(),
            file_path=file_path,
            instance=self.dest_uri.instance,
            fields={
                self.field_flag: self.field_value,
                "project": self.dest_uri.project.id,
                "force": "1" if self.force else "0",
            },
            use_raise=True,
            progress=progress,
            task_id=task_id,
        )

    def _do_download_bundle_tar(self, progress: Progress) -> None:
        file_path = self._get_versioned_resource_path(self.dest_uri)
        ensure_dir(os.path.dirname(file_path))
        task_id = progress.add_task(f":bowling: download to {file_path}...")

        self.do_download_file(
            url_path=self._get_remote_bundle_api_url(),
            dest_path=file_path,
            instance=self.src_uri.instance,
            params={
                self.field_flag: self.field_value,
                "project": self.src_uri.project.id,
            },
            progress=progress,
            task_id=task_id,
        )

    def do(self) -> None:
        self.do_copy_bundle()
        self.do_copy_tags()

    def do_copy_tags(self) -> None:
        if self.src_uri.instance.is_local:
            candidate_tags = StandaloneTag(self.src_uri).list()
        else:
            candidate_tags = self._do_fetch_tags_from_server(self.src_uri)

        tags = []
        for tag in candidate_tags:
            if tag not in self.ignore_tags and not StandaloneTag.is_builtin_tag(tag):
                tags.append(tag)

        if not tags:
            console.print(":tea: no tags to copy")
            return

        if self.dest_uri.instance.is_local:
            # TODO: support standalone tags restrict validation
            StandaloneTag(self.dest_uri).add(tags=tags)
        else:
            self._do_upload_tags_to_server(self.dest_uri, tags)

        console.print(f":apple: tags copied: {','.join(tags)}")

    def do_copy_bundle(self) -> None:
        remote_url = self._get_remote_bundle_console_url()
        if not self.force and self._check_version_existed(self.dest_uri):
            console.print(f":tea: {self.dest_uri} was already existed, skip copy")
            return

        if not self._check_version_existed(self.src_uri):
            raise NotFoundError(str(self.src_uri))

        console.print(f":construction: start to copy {self.src_uri} -> {self.dest_uri}")

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
            if self.src_uri.instance.is_local:
                if self.typ == ResourceType.model:
                    self._do_upload_bundle_dir(progress)
                elif self.typ == ResourceType.runtime:
                    self._do_upload_bundle_tar(progress)
                else:
                    raise NoSupportError(
                        f"no support to copy {self.typ} from standalone to server"
                    )
            else:
                if self.typ == ResourceType.model:
                    self._do_download_bundle_dir(progress)
                elif self.typ == ResourceType.runtime:
                    self._do_download_bundle_tar(progress)
                else:
                    raise NoSupportError(
                        f"no support to copy {self.typ} from server to standalone"
                    )
                StandaloneTag(self.dest_uri).add_fast_tag()

        console.print(f":tea: console url of the remote bundle: {remote_url}")

    def download_files(self, workdir: Path) -> t.Iterator[FileNode]:
        raise NotImplementedError

    def _do_fetch_tags_from_server(self, rc: Resource) -> t.List[str]:
        if not rc.instance.is_cloud:
            raise RuntimeError("Only accept remote resource to fetch tags")

        return TagApi(rc.instance).list(rc).response().data

    def _do_upload_tags_to_server(self, rc: Resource, tags: t.List[str]) -> None:
        if not rc.instance.is_cloud:
            raise RuntimeError("Only accept remote resource to upload tags")

        api = TagApi(rc.instance)
        for tag in tags:
            api.add(rc, tag, force=self.force)

    def _do_download_bundle_dir(self, progress: Progress) -> None:
        workdir = self._get_versioned_resource_path(self.dest_uri)
        ensure_dir(workdir)
        trio.run(
            download_model,
            self.src_uri,
            workdir,
            self._object_store,
            progress,
            self.force,
        )

    def _do_upload_bundle_dir(
        self,
        progress: Progress,
    ) -> None:
        workdir = self._get_versioned_resource_path(self.src_uri)
        trio.run(
            upload_model,
            self.dest_uri,
            workdir,
            progress,
            self._upload_built_in_runtime,
            self.force,
        )

    def _upload_built_in_runtime(self, progress: Progress) -> str | None:
        manifest_file = (
            self._get_versioned_resource_path(self.src_uri) / DEFAULT_MANIFEST_NAME
        )

        manifest = load_yaml(manifest_file)
        packaged_runtime = manifest.get("packaged_runtime", None)
        if not packaged_runtime:
            return None
        rt_version: str = packaged_runtime["manifest"]["version"]

        dest_uri = Resource(
            f"{self.dest_uri.project}/{SW_BUILT_IN}/version/{rt_version}",
            typ=ResourceType.runtime,
            refine=False,
        )
        file_path: Path = (
            self._get_versioned_resource_path(self.src_uri) / packaged_runtime["path"]
        )

        def _check_built_in_runtime_existed(rc: Resource) -> bool:
            ok, _ = self.do_http_request_simple_ret(
                path=f"/project/{rc.project.id}/{rc.typ.value}/{rc.name}/version/{rc.version}",
                method=HTTPMethod.HEAD,
                instance=rc.instance,
                ignore_status_codes=[HTTPStatus.NOT_FOUND],
            )
            return ok

        if _check_built_in_runtime_existed(dest_uri):
            console.print("built-in runtime was already existed, skip copy it")
        else:
            task_id = progress.add_task(
                f":arrow_up: uploading the built-in runtime {file_path.name}",
                total=file_path.stat().st_size,
            )
            self.do_multipart_upload_file(
                url_path=f"/project/{dest_uri.project.id}/{ResourceType.runtime.value}/{SW_BUILT_IN}/version/{rt_version}/file",
                file_path=file_path,
                instance=dest_uri.instance,
                fields={
                    _query_param_map[
                        ResourceType.runtime
                    ]: f"{SW_BUILT_IN}:{rt_version}",
                    "project": dest_uri.project.id,
                    "force": "1" if self.force else "0",
                },
                use_raise=True,
                progress=progress,
                task_id=task_id,
            )
            progress.update(task_id, completed=file_path.stat().st_size)
        return rt_version

    @classmethod
    def download_for_cache(cls, uri: Resource) -> Resource:
        if not uri.instance.is_cloud:
            raise NoSupportError(
                f"only support download from server/cloud instance:{uri}"
            )

        _cache_project_uri = "local/project/.cache"
        cls(
            src_uri=uri,
            dest_uri=".",
            typ=uri.typ,
            dest_local_project_uri=_cache_project_uri,
        ).do()

        return Resource(
            f"{_cache_project_uri}/{uri.name}/version/{uri.version}", typ=uri.typ
        )
