import os
import json
import typing as t
from pathlib import Path

from rich.progress import TaskID, Progress

from starwhale.utils import load_yaml
from starwhale.consts import (
    FileDesc,
    FileNode,
    HTTPMethod,
    SW_BUILT_IN,
    SWMP_SRC_FNAME,
    SW_AUTO_DIRNAME,
    RESOURCE_FILES_NAME,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.utils.fs import extract_tar
from starwhale.utils.retry import http_retry
from starwhale.base.bundle_copy import BundleCopy, _query_param_map
from starwhale.base.uri.instance import Instance
from starwhale.base.uri.resource import Resource, ResourceType


class ModelCopy(BundleCopy):
    def upload_files(self, workdir: Path) -> t.Iterator[FileNode]:
        for _m in self._get_resource_files(workdir):
            if _m["desc"] != FileDesc.MODEL.name:
                continue
            _path = workdir / _m["path"]
            yield FileNode(
                path=_path,
                name=_m["name"],
                size=_path.stat().st_size,
                file_desc=FileDesc.MODEL,
                signature=_m["signature"],
            )

        _meta_names = [SWMP_SRC_FNAME]

        for _n in _meta_names:
            _path = workdir / _n
            yield FileNode(
                path=_path,
                name=os.path.basename(_path),
                size=_path.stat().st_size,
                file_desc=FileDesc.SRC_TAR,
                signature="",
            )

    def _get_resource_files(self, workdir: Path) -> t.List[t.Dict[str, t.Any]]:
        resource_files_path = workdir / "src" / SW_AUTO_DIRNAME / RESOURCE_FILES_NAME
        if resource_files_path.exists():
            resources = load_yaml(resource_files_path)
        else:
            resources = load_yaml(workdir / DEFAULT_MANIFEST_NAME).get("resources", [])
        return resources  # type: ignore

    def download_files(self, workdir: Path) -> t.Iterator[FileNode]:
        for _f in (SWMP_SRC_FNAME,):
            yield FileNode(
                path=workdir / _f,
                signature="",
                size=0,
                name=_f,
                file_desc=FileDesc.SRC_TAR,
            )
            extract_tar(
                tar_path=workdir / SWMP_SRC_FNAME,
                dest_dir=workdir / "src",
                force=False,
            )
        # this must after src download
        for _m in self._get_resource_files(workdir):
            if _m["desc"] != FileDesc.MODEL.name:
                continue
            _sign = _m["signature"]
            _dest = workdir / _m["path"]

            yield FileNode(
                path=_dest,
                signature=_sign,
                size=0,
                name=_m["name"],
                file_desc=FileDesc.MODEL,
            )
            # TODO use unified storage
            # Path(workdir / _m["path"]).symlink_to(
            #     _dest # the unify dir
            # )

    def final_steps(self, progress: Progress) -> None:
        if self.src_uri.instance.is_local:
            manifest_file = (
                self._get_versioned_resource_path(self.src_uri) / DEFAULT_MANIFEST_NAME
            )

            manifest = load_yaml(manifest_file)
            packaged_runtime = manifest.get("packaged_runtime", None)
            if packaged_runtime:
                rt_version = packaged_runtime["manifest"]["version"]
                rt_file_path = (
                    self._get_versioned_resource_path(self.src_uri)
                    / packaged_runtime["path"]
                )
                _tid = progress.add_task(
                    f":arrow_up: synchronize the built-in runtime:{rt_version}..."
                )

                dest_uri = Resource(
                    f"{self.dest_uri.project}/{SW_BUILT_IN}/version/{rt_version}",
                    typ=ResourceType.runtime,
                    refine=True,
                )

                def upload_runtime_tar(file_path: Path, progress: Progress) -> None:
                    task_id = progress.add_task(
                        f":synchronize the built-in runtime {file_path.name}",
                        total=file_path.stat().st_size,
                    )
                    self.do_multipart_upload_file(
                        url_path=f"/project/{dest_uri.project.name}/{ResourceType.runtime.value}/{SW_BUILT_IN}/version/{rt_version}/file",
                        file_path=file_path,
                        instance=dest_uri.instance,
                        fields={
                            _query_param_map[
                                ResourceType.runtime
                            ]: f"{SW_BUILT_IN}:{rt_version}",
                            "project": dest_uri.project.name,
                            "force": "1" if self.force else "0",
                        },
                        use_raise=True,
                        progress=progress,
                        task_id=task_id,
                    )

                upload_runtime_tar(rt_file_path, progress)

                @http_retry
                def sync_built_in_runtime(
                    path: str,
                    instance: Instance,
                    progress: t.Optional[Progress] = None,
                    task_id: TaskID = TaskID(0),
                ) -> None:
                    self.do_http_request(
                        path=path,
                        method=HTTPMethod.PUT,
                        instance=instance,
                        data=json.dumps(
                            {
                                "built_in_runtime": rt_version,
                            }
                        ),
                        use_raise=True,
                        disable_default_content_type=False,
                    )
                    progress.update(task_id, completed=100)

                print(f"final link:{self._get_remote_bundle_api_url(for_head=True)}")
                sync_built_in_runtime(
                    path=self._get_remote_bundle_api_url(for_head=True),
                    instance=self.dest_uri.instance,
                    progress=progress,
                    task_id=_tid,
                )
