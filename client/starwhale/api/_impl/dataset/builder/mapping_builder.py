from __future__ import annotations

import io
import typing as t
from pathlib import Path
from collections import defaultdict

from starwhale.consts import D_ALIGNMENT_SIZE, D_FILE_VOLUME_SIZE
from starwhale.utils.fs import blake2b_file, BLAKE2B_SIGNATURE_ALGO
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.retry import http_retry
from starwhale.base.artifact import AsyncArtifactWriterBase
from starwhale.base.data_type import Link, BaseArtifact
from starwhale.base.uri.resource import Resource
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import TabularDataset, TabularDatasetRow
from starwhale.api._impl.dataset.loader import DataRow


class MappingDatasetBuilder(AsyncArtifactWriterBase):
    _STASH_URI = "_starwhale_stash_uri"

    class _SignedBinMeta(t.NamedTuple):
        name: str
        algo: str
        size: int

        def __str__(self) -> str:
            return f"{self.name}:{self.algo}:{self.size}"

    def __init__(
        self,
        workdir: t.Union[Path, str],
        dataset_uri: Resource,
        blob_alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        blob_volume_bytes_size: int = D_FILE_VOLUME_SIZE,
    ) -> None:
        self.dataset_uri = dataset_uri
        self._in_standalone = dataset_uri.instance.is_local
        self._tabular_dataset = TabularDataset(
            name=dataset_uri.name,
            project=dataset_uri.project,
        )
        self._stash_uri_rows_map: t.Dict[
            Path, t.List[t.Tuple[BaseArtifact, TabularDatasetRow]]
        ] = defaultdict(list)
        self._signed_bins_meta: t.List[MappingDatasetBuilder._SignedBinMeta] = []
        self._last_flush_revision = ""

        super().__init__(
            workdir=workdir,
            blob_alignment_bytes_size=blob_alignment_bytes_size,
            blob_volume_bytes_size=blob_volume_bytes_size,
        )

    def put(self, row: DataRow) -> None:
        if not row or not isinstance(row, DataRow):
            raise ValueError(f"row argument must be DataRow type: {row}")
        super().put(row)

    def _handle_row_put(self, row: DataRow) -> None:
        td_row = TabularDatasetRow(id=row.index, features=row.features)

        td_row.encode_feature_types()
        for artifact in td_row.artifacts:
            # TODO: refactor BaseArtifact Type, parse link by fp, such as: fp="s3://xx/yy/zz", fp="http://xx/yy/zz"
            if not artifact.link:
                if isinstance(artifact.fp, (str, Path)):
                    content = Path(artifact.fp).read_bytes()
                elif isinstance(artifact.fp, (bytes, io.IOBase)):
                    content = artifact.to_bytes()
                else:
                    raise TypeError(
                        f"no support fp type for bin writer:{type(artifact.fp)}, {artifact.fp}"
                    )

                _path, _meta = self._write_bin(content)
                artifact.link = Link(
                    uri=self._STASH_URI,  # When bin writer is rotated, we can get the signatured uri
                    offset=_meta.raw_data_offset,
                    size=_meta.raw_data_size,
                    bin_offset=_meta.offset,
                    bin_size=_meta.size,
                )
                self._stash_uri_rows_map[_path].append((artifact, td_row))

        self._tabular_dataset.put(td_row)

    def _handle_bin_sync(self, bin_path: Path) -> None:
        size = bin_path.stat().st_size
        if self._in_standalone:
            uri, _ = DatasetStorage.save_data_file(bin_path, remove_src=True)
        else:
            sign_name = blake2b_file(bin_path)
            crm = CloudRequestMixed()
            instance_uri = self.dataset_uri.instance

            @http_retry
            def _upload() -> str:
                r = crm.do_multipart_upload_file(
                    url_path=f"/project/{self.dataset_uri.project.id}/dataset/{self.dataset_uri.name}/hashedBlob/{sign_name}",
                    file_path=bin_path,
                    instance=instance_uri,
                )
                return r.json()["data"]  # type: ignore

            uri = _upload()

        self._signed_bins_meta.append(
            MappingDatasetBuilder._SignedBinMeta(
                name=uri,
                algo=BLAKE2B_SIGNATURE_ALGO,
                size=size,
            )
        )

        for artifact, td_row in self._stash_uri_rows_map.get(bin_path, []):
            artifact.link.uri = uri  # type: ignore
            self._tabular_dataset.put(td_row)

        if bin_path in self._stash_uri_rows_map:
            del self._stash_uri_rows_map[bin_path]

    def delete(self, key: t.Union[str, int]) -> None:
        self._tabular_dataset.delete(key)

    def flush(self, artifacts_flush: bool = False) -> str:  # type: ignore
        super().flush(artifacts_flush)
        self._last_flush_revision, _ = self._tabular_dataset.flush()
        return self._last_flush_revision

    def close(self) -> None:
        super().close()
        self._tabular_dataset.close()

    @property
    def signature_bins_meta(self) -> t.List[MappingDatasetBuilder._SignedBinMeta]:
        return self._signed_bins_meta

    def calculate_rows_cnt(self) -> int:
        # TODO: tune performance by datastore
        return len(
            [
                row
                for row in self._tabular_dataset.scan(
                    revision=self._last_flush_revision
                )
            ]
        )
