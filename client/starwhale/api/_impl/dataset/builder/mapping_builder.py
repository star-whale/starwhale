from __future__ import annotations

import io
import os
import queue
import struct
import typing as t
import tempfile
import threading
from types import TracebackType
from pathlib import Path
from binascii import crc32
from collections import defaultdict

from starwhale.utils import console
from starwhale.utils.fs import (
    empty_dir,
    ensure_dir,
    blake2b_file,
    BLAKE2B_SIGNATURE_ALGO,
)
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.retry import http_retry
from starwhale.base.uri.resource import Resource
from starwhale.core.dataset.type import (
    Link,
    BaseArtifact,
    D_ALIGNMENT_SIZE,
    D_FILE_VOLUME_SIZE,
)
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import TabularDataset, TabularDatasetRow
from starwhale.api._impl.dataset.loader import DataRow


class RotatedBinWriter:
    """
    format:
        header_magic    uint32  I
        crc             uint32  I
        _reserved       uint64  Q
        size            uint32  I
        padding_size    uint32  I
        header_version  uint32  I
        data_magic      uint32  I --> above 32 bytes
        data bytes...
        padding bytes...        --> default 4K padding
    """

    _header_magic = struct.unpack(">I", b"SWDS")[0]
    _data_magic = struct.unpack(">I", b"SDWS")[0]
    _header_struct = struct.Struct(">IIQIIII")
    _header_size = _header_struct.size
    _header_version = 0

    class _BinSection(t.NamedTuple):
        offset: int
        size: int
        raw_data_offset: int
        raw_data_size: int

    def __init__(
        self,
        workdir: Path,
        rotated_bin_notify_queue: t.Optional[queue.Queue[t.Optional[Path]]] = None,
        alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        volume_bytes_size: int = D_FILE_VOLUME_SIZE,
    ) -> None:
        self.workdir = workdir

        if alignment_bytes_size <= 0:
            raise ValueError(
                f"alignment_bytes_size must be greater than zero: {alignment_bytes_size}"
            )

        self.alignment_bytes_size = alignment_bytes_size

        if volume_bytes_size < 0:
            raise ValueError(
                f"volume_bytes_size must be greater than zero: {volume_bytes_size}"
            )
        self.volume_bytes_size = volume_bytes_size

        self._rotated_bin_notify_queue = rotated_bin_notify_queue
        self._wrote_size = 0

        ensure_dir(self.workdir)
        _, bin_writer_path = tempfile.mkstemp(
            prefix="bin-writer-", dir=str(self.workdir.absolute())
        )
        self._current_writer_path = Path(bin_writer_path)
        self._current_writer = self._current_writer_path.open("wb")
        self._rotated_bin_paths: t.List[Path] = []

    def __enter__(self) -> RotatedBinWriter:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    @property
    def working_path(self) -> Path:
        return self._current_writer_path

    @property
    def rotated_paths(self) -> t.List[Path]:
        return self._rotated_bin_paths

    def close(self) -> None:
        self._rotate()

        empty = self._current_writer.tell() == 0
        self._current_writer.close()
        if empty and self._current_writer_path.exists():
            # last file is empty
            os.unlink(self._current_writer_path)

    def _rotate(self) -> None:
        if self._wrote_size == 0:
            return
        self._current_writer.close()
        self._wrote_size = 0

        self._rotated_bin_paths.append(self._current_writer_path)
        if self._rotated_bin_notify_queue is not None:
            self._rotated_bin_notify_queue.put(self._current_writer_path)

        _, bin_writer_path = tempfile.mkstemp(
            prefix="bin-writer-", dir=str(self.workdir.absolute())
        )
        self._current_writer_path = Path(bin_writer_path)
        self._current_writer = self._current_writer_path.open("wb")

    def write(self, data: bytes) -> t.Tuple[Path, RotatedBinWriter._BinSection]:
        _cls = RotatedBinWriter
        size = len(data)
        crc = crc32(data)  # TODO: crc is right?
        start = self._current_writer.tell()
        padding_size = self._get_padding_size(size)

        _header = _cls._header_struct.pack(
            _cls._header_magic,
            crc,
            0,
            size,
            padding_size,
            _cls._header_version,
            _cls._data_magic,
        )
        _padding = b"\0" * padding_size
        self._current_writer.write(_header + data + _padding)
        _bin_path = self._current_writer_path
        _bin_section = _cls._BinSection(
            offset=start,
            size=_cls._header_size + size + padding_size,
            raw_data_offset=start + _cls._header_size,
            raw_data_size=size,
        )
        self._wrote_size += _bin_section.size

        if self._wrote_size > self.volume_bytes_size:
            self._rotate()

        return _bin_path, _bin_section

    def _get_padding_size(self, size: int) -> int:
        remain = (size + RotatedBinWriter._header_size) % self.alignment_bytes_size
        return 0 if remain == 0 else (self.alignment_bytes_size - remain)


class MappingDatasetBuilder:
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
        self.workdir = Path(workdir)
        self.dataset_uri = dataset_uri
        self._in_standalone = dataset_uri.instance.is_local

        self._blob_alignment_bytes_size = blob_alignment_bytes_size
        self._blob_volume_bytes_size = blob_volume_bytes_size
        self._tabular_dataset = TabularDataset(
            name=dataset_uri.name,
            project=dataset_uri.project.name,
            instance_name=dataset_uri.instance.url,
        )
        self._rows_put_queue: queue.Queue[
            t.Optional[t.Union[DataRow, Exception]]
        ] = queue.Queue()
        self._rows_put_thread = threading.Thread(
            target=self._rows_put_worker, daemon=True, name="RowPutThread"
        )
        self._rows_put_thread.start()
        self._rows_put_exception: t.Optional[Exception] = None

        # abs = artifacts bin sync
        self._abs_queue: queue.Queue[t.Optional[Path]] = queue.Queue()
        self._abs_exception: t.Optional[Exception] = None
        self._abs_thread = threading.Thread(
            target=self._abs_worker, daemon=True, name="ArtifactsBinSyncThread"
        )
        self._abs_thread.start()

        self._artifact_bin_tmpdir = self.workdir / "artifact_bin_tmp"
        self._artifact_bin_writer = RotatedBinWriter(
            workdir=self._artifact_bin_tmpdir,
            rotated_bin_notify_queue=self._abs_queue,
            alignment_bytes_size=self._blob_alignment_bytes_size,
            volume_bytes_size=self._blob_volume_bytes_size,
        )

        self._stash_uri_rows_map: t.Dict[
            Path, t.List[t.Tuple[BaseArtifact, TabularDatasetRow]]
        ] = defaultdict(list)
        self._signed_bins_meta: t.List[MappingDatasetBuilder._SignedBinMeta] = []
        self._last_flush_revision = ""

    def __enter__(self) -> MappingDatasetBuilder:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    def put(self, row: DataRow) -> None:
        if not row or not isinstance(row, DataRow):
            raise ValueError(f"row argument must be DataRow type: {row}")

        self._raise_thread_daemons_exception()
        self._rows_put_queue.put(row)

    def _raise_thread_daemons_exception(self) -> None:
        if self._abs_exception:
            _e = self._abs_exception
            self._abs_exception = None
            raise threading.ThreadError(f"ArtifactsBinSyncThread raise exception: {_e}")

        if self._rows_put_exception:
            _e = self._rows_put_exception
            self._rows_put_exception = None
            raise threading.ThreadError(f"RowPutThread raise exception: {_e}")

    def _handle_row_put(self, row: DataRow) -> None:
        td_row = TabularDatasetRow(id=row.index, features=row.features)

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

                _path, _meta = self._artifact_bin_writer.write(content)
                artifact.link = Link(
                    uri=self._STASH_URI,  # When bin writer is rotated, we can get the signatured uri
                    offset=_meta.raw_data_offset,
                    size=_meta.raw_data_size,
                    bin_offset=_meta.offset,
                    bin_size=_meta.size,
                )
                self._stash_uri_rows_map[_path].append((artifact, td_row))

            # TODO: find a graceful cleanup method
            # forbid to write cache and fp contents into datastore table
            artifact.clear_cache()

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
                    url_path=f"/project/{self.dataset_uri.project.name}/dataset/{self.dataset_uri.name}/hashedBlob/{sign_name}",
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

    def _rows_put_worker(self) -> None:
        try:
            while True:
                row = self._rows_put_queue.get(block=True, timeout=None)
                if row is None:
                    break
                elif isinstance(
                    row, Exception
                ):  # receive exception from ArtifactsBinSyncThread
                    raise row
                else:
                    try:
                        self._handle_row_put(row)
                    finally:
                        self._rows_put_queue.task_done()
        except Exception as e:
            self._rows_put_exception = e
            raise
        finally:
            self._artifact_bin_writer.close()
            self._abs_queue.put(None)

    def _abs_worker(self) -> None:
        try:
            while True:
                bin_path = self._abs_queue.get(block=True, timeout=None)
                if bin_path is None:
                    break
                else:
                    try:
                        self._handle_bin_sync(bin_path)
                    finally:
                        self._abs_queue.task_done()
        except Exception as e:
            self._abs_exception = e
            self._rows_put_queue.put(e)
            raise

    def delete(self, key: t.Union[str, int]) -> None:
        self._tabular_dataset.delete(key)

    def flush(self, artifacts_flush: bool = False) -> str:
        self._rows_put_queue.join()

        if artifacts_flush:
            self._artifact_bin_writer._rotate()
            self._abs_queue.join()

        self._last_flush_revision, _ = self._tabular_dataset.flush()
        return self._last_flush_revision

    def _threads_join(self) -> None:
        self._abs_thread.join()
        self._rows_put_thread.join()

    def close(self) -> None:
        self._rows_put_queue.put(None)
        self._threads_join()
        self._tabular_dataset.close()

        empty_dir(self._artifact_bin_tmpdir)
        self._raise_thread_daemons_exception()

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
