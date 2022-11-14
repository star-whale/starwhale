from pathlib import Path

from rich.progress import Progress

from starwhale.utils import console
from starwhale.consts import STANDALONE_INSTANCE
from starwhale.base.bundle_copy import BundleCopy

from .tabular import TabularDataset


class DatasetCopy(BundleCopy):
    def _do_ubd_blobs(
        self,
        progress: Progress,
        workdir: Path,
        upload_id: str,
        url_path: str,
        add_data_uri_header: bool = False,
    ) -> None:
        with TabularDataset(
            name=self.bundle_name,
            version=self.bundle_version,
            project=self.src_uri.project,
            instance_uri=STANDALONE_INSTANCE,
        ) as local, TabularDataset(
            name=self.bundle_name,
            version=self.bundle_version,
            project=self.dest_uri.project,
            instance_uri=self.dest_uri.instance,
        ) as remote:
            console.print(
                f":bear_face: dump dataset meta from standalone to cloud({remote._ds_wrapper._meta_table_name})"
            )
            # TODO: add progressbar
            for row in local.scan():
                remote.put(row)

        super()._do_ubd_blobs(
            progress, workdir, upload_id, url_path, add_data_uri_header
        )

    def _do_download_bundle_dir(self, progress: Progress) -> None:

        with TabularDataset(
            name=self.bundle_name,
            version=self.bundle_version,
            project=self.dest_uri.project,
            instance_uri=STANDALONE_INSTANCE,
        ) as local, TabularDataset(
            name=self.bundle_name,
            version=self.bundle_version,
            project=self.src_uri.project,
            instance_uri=self.src_uri.instance,
        ) as remote:
            console.print(
                f":bird: load dataset meta from cloud({remote._ds_wrapper._meta_table_name}) to standalone"
            )
            # TODO: add progressbar
            for row in remote.scan():
                local.put(row)

        super()._do_download_bundle_dir(progress)
