from __future__ import annotations

import queue
import atexit
import typing as t
import threading
from abc import ABC, abstractmethod
from types import TracebackType
from pathlib import Path

from starwhale.utils import console
from starwhale.consts import D_ALIGNMENT_SIZE, D_FILE_VOLUME_SIZE
from starwhale.utils.fs import empty_dir
from starwhale.api._impl.data_store import datastore_max_dirty_records

from .rotated_bin import RotatedBinWriter


class AsyncArtifactWriterBase(ABC):
    def __init__(
        self,
        workdir: Path | str,
        blob_alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        blob_volume_bytes_size: int = D_FILE_VOLUME_SIZE,
    ) -> None:
        self.workdir = Path(workdir)

        self._blob_alignment_bytes_size = blob_alignment_bytes_size
        self._blob_volume_bytes_size = blob_volume_bytes_size

        self._rows_put_queue: queue.Queue[t.Any] = queue.Queue(
            maxsize=datastore_max_dirty_records
        )
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
        atexit.register(self.close)

    def __enter__(self) -> AsyncArtifactWriterBase:
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

    def _raise_thread_daemons_exception(self) -> None:
        if self._abs_exception:
            _e = self._abs_exception
            self._abs_exception = None
            raise threading.ThreadError(f"ArtifactsBinSyncThread raise exception: {_e}")

        if self._rows_put_exception:
            _e = self._rows_put_exception
            self._rows_put_exception = None
            raise threading.ThreadError(f"RowPutThread raise exception: {_e}")

    @abstractmethod
    def _handle_row_put(self, row: t.Any) -> None:
        ...

    @abstractmethod
    def _handle_bin_sync(self, bin_path: Path) -> None:
        ...

    def _rows_put_worker(self) -> None:
        try:
            while True:
                row = self._rows_put_queue.get(block=True, timeout=None)
                if row is None:
                    self._rows_put_queue.task_done()
                    break
                elif isinstance(
                    row, Exception
                ):  # receive exception from ArtifactsBinSyncThread
                    self._rows_put_queue.task_done()
                    raise row
                else:
                    try:
                        self._handle_row_put(row)
                    finally:
                        self._rows_put_queue.task_done()
        except Exception as e:
            self._rows_put_exception = e
            # clear queued rows that was put into queue before exception occurred, because we will queue.join in flush.
            while True:
                try:
                    self._rows_put_queue.get_nowait()
                    self._rows_put_queue.task_done()
                except queue.Empty:
                    break
            raise
        finally:
            # If exceptions are raised in the artifact's close function, we should still enqueue `None` to the abs queue
            # in order to terminate the abs thread. Failing to do so cloud result in the abs thread becoming blocked indefinitely.
            try:
                self._artifact_bin_writer.close()
            finally:
                self._abs_queue.put(None)

    def _abs_worker(self) -> None:
        try:
            while True:
                bin_path = self._abs_queue.get(block=True, timeout=None)
                if bin_path is None:
                    self._abs_queue.task_done()
                    break
                else:
                    try:
                        self._handle_bin_sync(bin_path)
                    finally:
                        self._abs_queue.task_done()
        except Exception as e:
            self._abs_exception = e
            self._rows_put_queue.put(e)

            # clear queued rows that was put into queue before exception occurred, because we will queue.join in flush.
            while True:
                try:
                    self._abs_queue.get_nowait()
                    self._abs_queue.task_done()
                except queue.Empty:
                    break
            raise

    def _threads_join(self) -> None:
        self._abs_thread.join()
        self._rows_put_thread.join()

    def close(self) -> None:
        atexit.unregister(self.close)
        self._rows_put_queue.put(None)
        self._threads_join()

        empty_dir(self._artifact_bin_tmpdir)
        self._raise_thread_daemons_exception()

    def flush(self, artifacts_flush: bool = False) -> None:
        self._rows_put_queue.join()

        if artifacts_flush:
            self._artifact_bin_writer._rotate()
            self._abs_queue.join()

        self._raise_thread_daemons_exception()

    def _write_bin(self, content: bytes) -> t.Tuple[Path, RotatedBinWriter._BinSection]:
        return self._artifact_bin_writer.write(content)

    def put(self, row: t.Any) -> None:
        self._raise_thread_daemons_exception()
        self._rows_put_queue.put(row)
