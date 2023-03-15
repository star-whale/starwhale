from __future__ import annotations

import io
import os
import time
import queue
import struct
import typing as t
import inspect
import tempfile
import threading
from abc import ABCMeta, abstractmethod
from types import TracebackType
from pathlib import Path
from binascii import crc32

import jsonlines
from loguru import logger
from typing_extensions import Protocol

from starwhale.consts import DEFAULT_PROJECT, STANDALONE_INSTANCE
from starwhale.base.uri import URI
from starwhale.utils.fs import empty_dir, ensure_dir
from starwhale.base.type import InstanceType, DataFormatType, DataOriginType
from starwhale.utils.error import FormatError, NoSupportError
from starwhale.core.dataset import model
from starwhale.core.dataset.type import (
    Link,
    MIMEType,
    DatasetSummary,
    D_ALIGNMENT_SIZE,
    D_FILE_VOLUME_SIZE,
)
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import TabularDataset, TabularDatasetRow
from starwhale.api._impl.dataset.loader import DataRow

# TODO: tune header size
_header_magic = struct.unpack(">I", b"SWDS")[0]
_data_magic = struct.unpack(">I", b"SDWS")[0]
_header_struct = struct.Struct(">IIQIIII")
_header_size = _header_struct.size
_header_version = 0

_BDType = t.TypeVar("_BDType", bound="BaseBuildExecutor")


class BinWriter(Protocol):
    total_bin_size: int

    def write_row(self, row: TabularDatasetRow) -> None:
        """
        Find large bytes or local fs file in row data. Convert them to accessible link
        """
        ...

    def flush(self) -> None:
        ...

    def __enter__(self) -> BinWriter:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        ...


class SWDSBinWriter:

    """
    bin format:
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

    class _BinSection(t.NamedTuple):
        offset: int
        size: int
        raw_data_offset: int
        raw_data_size: int

    class _SrcPathSpec(t.NamedTuple):
        src_path: Path
        remove_src: bool

    def __init__(
        self,
        work_dir: Path,
        data_output_dir: Path,
        tabular_dataset: TabularDataset,
        alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        volume_bytes_size: int = D_FILE_VOLUME_SIZE,
    ) -> None:
        self.tabular_dataset = tabular_dataset
        self.work_dir = work_dir
        self.data_output_dir = data_output_dir
        self.volume_bytes_size = volume_bytes_size
        self.alignment_bytes_size = alignment_bytes_size
        self.ds_copy_candidates: t.Dict[str, SWDSBinWriter._SrcPathSpec] = {}
        self.wrote_size = 0
        _, bin_writer_path = tempfile.mkstemp(
            prefix="bin-writer-", dir=str(self.work_dir.absolute())
        )
        self.dwriter_path = Path(bin_writer_path)
        self.dwriter = self.dwriter_path.open("wb")
        self.total_bin_size = 0
        self._to_update_rows: t.List[TabularDatasetRow] = []
        self._lock = threading.Lock()

    def __enter__(self) -> SWDSBinWriter:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            logger.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    def write_row(self, row: TabularDatasetRow) -> None:
        with self._lock:
            artifacts_with_bin = False
            for v in row.artifacts:
                if not v.link and isinstance(v.fp, (str, Path)):
                    # convert user local file path to Starwhale link
                    v.link = Link(v.fp, with_local_fs_data=True)
                    #  BaseArtifact reads from BaseArtifact.fp prior to any other sources like link
                    #  When BaseArtifact.fp is user local file path , it is unreliable and should be removed.
                    v.fp = ""
                if v.link and v.link.with_local_fs_data:
                    v.link.uri = self._copy_file(v.link.uri, False)
                if (
                    not v.link
                    and v.fp
                    and (isinstance(v.fp, bytes) or isinstance(v.fp, io.IOBase))
                ):
                    artifacts_with_bin = True
                    _bin_section = self._write(v.to_bytes())
                    v.link = Link(
                        self.dwriter_path,
                        offset=_bin_section.raw_data_offset,
                        size=_bin_section.raw_data_size,
                        bin_offset=_bin_section.offset,
                        bin_size=_bin_section.size,
                    )
                    v.clear_bytes()

            if not artifacts_with_bin:
                self.tabular_dataset.put(row)
            else:
                self._to_update_rows.append(row)
            if self.wrote_size > self.volume_bytes_size:
                self._roll_bin()

    def _roll_bin(self) -> None:
        if self.wrote_size == 0:
            return
        self.wrote_size = 0
        self.dwriter.close()
        self._update_link(self.dwriter_path, self._copy_file(self.dwriter_path, True))
        _, bin_writer_path = tempfile.mkstemp(
            prefix="bin-writer-", dir=str(self.work_dir.absolute())
        )
        self.dwriter_path = Path(bin_writer_path)
        self.dwriter = self.dwriter_path.open("wb")

    def _update_link(self, old_path: Path, new_path: str) -> None:
        for row in self._to_update_rows:
            for at in row.artifacts:
                if at.link and str(at.link.uri) == str(old_path):
                    at.link.uri = new_path
            self.tabular_dataset.put(row)
        self._to_update_rows.clear()

    def _write(self, data: bytes) -> SWDSBinWriter._BinSection:
        size = len(data)
        crc = crc32(data)  # TODO: crc is right?
        start = self.dwriter.tell()
        padding_size = self._get_padding_size(size + _header_size)

        _header = _header_struct.pack(
            _header_magic, crc, 0, size, padding_size, _header_version, _data_magic
        )
        _padding = b"\0" * padding_size
        self.dwriter.write(_header + data + _padding)
        _bin_section = SWDSBinWriter._BinSection(
            offset=start,
            size=_header_size + size + padding_size,
            raw_data_offset=start + _header_size,
            raw_data_size=size,
        )
        self.total_bin_size += _bin_section.size
        self.wrote_size += _bin_section.size
        return _bin_section

    def _copy_file(
        self,
        path: t.Union[Path, str],
        remove_src: bool = False,
    ) -> str:
        _sign_name, _obj_path = DatasetStorage.save_data_file(
            path, remove_src=remove_src
        )

        _dest_path = self.data_output_dir / _sign_name[: DatasetStorage.short_sign_cnt]
        _obj_path = _obj_path.resolve().absolute()

        if _dest_path.exists():
            _dest_path.unlink()

        _dest_path.symlink_to(_obj_path)
        return _sign_name

    def close(self) -> None:
        with self._lock:
            try:
                empty = self.dwriter.tell() == 0
                self.dwriter.close()
                if empty:
                    # last file is empty
                    os.unlink(self.dwriter_path)
                else:
                    self._roll_bin()
            except Exception as e:
                print(f"data write close exception: {e}")

    def flush(self) -> None:
        with self._lock:
            self._roll_bin()

    def _get_padding_size(self, size: int) -> int:
        remain = (size + _header_size) % self.alignment_bytes_size
        return 0 if remain == 0 else (self.alignment_bytes_size - remain)


class BaseBuildExecutor(metaclass=ABCMeta):
    def __init__(
        self,
        dataset_name: str,
        dataset_version: str,
        project_name: str,
        workdir: Path = Path("./sw_output"),
        alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        volume_bytes_size: int = D_FILE_VOLUME_SIZE,
        append: bool = False,
        append_from_version: str = "",
        append_from_uri: t.Optional[URI] = None,
        data_mime_type: MIMEType = MIMEType.UNDEFINED,
        instance_name: str = STANDALONE_INSTANCE,
        bin_writer: t.Optional[BinWriter] = None,
    ) -> None:
        # TODO: add more docstring for args
        # TODO: validate group upper and lower?
        self.workdir = workdir
        self.data_output_dir = workdir / "data"
        ensure_dir(self.data_output_dir)
        _tmpdir = tempfile.mkdtemp(
            prefix=".data-tmp-", dir=str(self.workdir.absolute())
        )
        self.data_tmpdir = Path(_tmpdir)

        self.alignment_bytes_size = alignment_bytes_size
        self.volume_bytes_size = volume_bytes_size
        self.default_data_mime_type = data_mime_type

        self.project_name = project_name
        self.dataset_name = dataset_name
        self.dataset_version = dataset_version
        self.tabular_dataset = TabularDataset(
            dataset_name,
            dataset_version,
            project_name,
            instance_name=instance_name,
        )
        if bin_writer:
            self.bin_writer = bin_writer
        else:
            self.bin_writer = SWDSBinWriter(
                self.data_tmpdir,
                self.data_output_dir,
                self.tabular_dataset,
                self.alignment_bytes_size,
                self.volume_bytes_size,
            )

        self._forked_summary: t.Optional[DatasetSummary]
        if append and append_from_uri:
            # TODO： controller supports cloud dataset fork api
            if append_from_uri.instance_type == InstanceType.CLOUD:
                raise NoSupportError(
                    f"Can't build dataset from existed cloud dataset: {append_from_uri}"
                )

            self._forked_last_seq_id, self._forked_rows = self.tabular_dataset.fork(
                append_from_version
            )
            self._forked_summary = model.Dataset.get_dataset(append_from_uri).summary()
        else:
            self._forked_last_seq_id = -1
            self._forked_rows = 0
            self._forked_summary = None

        self._index_writer: t.Optional[jsonlines.Writer] = None

    def __enter__(self: _BDType) -> _BDType:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            print(f"type:{type}, exception:{value}, traceback:{trace}")
        self.close()

    def close(self) -> None:
        try:
            self.tabular_dataset.close()
        except Exception as e:
            print(f"tabular dataset close exception: {e}")

        try:
            empty_dir(self.data_tmpdir)
        except Exception as e:
            print(f"empty {self.data_tmpdir} exception: {e}")

        print("cleanup done.")

    def flush(self) -> None:
        self.bin_writer.flush()
        self.tabular_dataset.flush()

    def get_info(self) -> t.Optional[t.Dict[str, t.Any]]:
        return None

    @abstractmethod
    def iter_item(
        self,
    ) -> t.Generator[t.Union[t.Tuple, t.Dict[str, t.Any]], None, None]:
        raise NotImplementedError

    @abstractmethod
    def make_swds(self) -> DatasetSummary:
        raise NotImplementedError

    def _merge_forked_summary(self, s: DatasetSummary) -> DatasetSummary:
        _fs = self._forked_summary
        if _fs:
            s.rows += _fs.rows
            s.unchanged_rows += _fs.rows
            s.data_byte_size += _fs.data_byte_size

        return s

    @property
    def data_format_type(self) -> DataFormatType:
        raise NotImplementedError

    def _unpack_row_content(
        self, row_data: t.Union[t.Tuple, DataRow, t.Dict], append_seq_id: int
    ) -> t.Tuple[t.Union[str, int], t.Dict]:
        row: t.Dict
        if isinstance(row_data, DataRow):
            idx, row = row_data.index, row_data.features
        elif isinstance(row_data, dict):
            idx, row = append_seq_id, row_data
        elif isinstance(row_data, tuple):
            if len(row_data) == 1:
                idx = append_seq_id
                row = row_data[0]
            elif len(row_data) == 2:
                idx, row = row_data
            else:
                raise FormatError(
                    f"iter_item must return data, (data) or (id, data): {row_data}"
                )
        else:
            raise FormatError(
                f"row content not return tuple or DataRow type: {row_data}"
            )

        if not isinstance(row, dict):
            raise FormatError(f"content({row}) must be dict type")

        return idx, row


class BuildExecutor(BaseBuildExecutor):
    def make_swds(self) -> DatasetSummary:
        increased_rows = 0
        with self.bin_writer as bw:
            for append_seq_id, item_content in enumerate(
                self.iter_item(), start=self._forked_last_seq_id + 1
            ):
                idx, row_data = self._unpack_row_content(item_content, append_seq_id)
                bw.write_row(
                    TabularDatasetRow(
                        id=idx,
                        origin=DataOriginType.NEW,
                        features=row_data,
                        _append_seq_id=append_seq_id,
                    )
                )
                increased_rows += 1
        self.flush()
        self.tabular_dataset.info = self.get_info()  # type: ignore

        summary = DatasetSummary(
            rows=increased_rows,
            increased_rows=increased_rows,
            data_byte_size=self.bin_writer.total_bin_size,
        )
        return self._merge_forked_summary(summary)


def create_generic_cls(
    handler: t.Callable,
) -> t.Type[BaseBuildExecutor]:
    res = handler()

    if inspect.isgenerator(res):
        items_iter = res
    elif getattr(res, "__getitem__", None):
        items_iter = iter(res)
    else:
        raise RuntimeError(
            f"{handler} function return is not generator or iterable object"
        )

    item = next(items_iter)

    def _do_iter_item(self: t.Any) -> t.Generator:
        yield item
        for _item in items_iter:
            yield _item

    return create_generic_cls_by_mode(_do_iter_item)


def create_generic_cls_by_mode(iter_func: t.Callable) -> t.Type[BaseBuildExecutor]:
    return type(
        "GenericSWDSBinHandler",
        (BuildExecutor,),
        {"iter_item": iter_func},
    )


class RowWriter(threading.Thread):
    def __init__(
        self,
        dataset_name: str,
        dataset_version: str,
        project_name: str = DEFAULT_PROJECT,
        workdir: Path = Path(".dataset_tmp"),
        alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        volume_bytes_size: int = D_FILE_VOLUME_SIZE,
        append: bool = False,
        append_from_version: str = "",
        append_from_uri: t.Optional[URI] = None,
        instance_name: str = STANDALONE_INSTANCE,
    ) -> None:
        super().__init__(
            name=f"RowWriter-{dataset_name}-{dataset_version}-{project_name}"
        )

        self._kw = {
            "dataset_name": dataset_name,
            "dataset_version": dataset_version,
            "project_name": project_name,
            "workdir": workdir,
            "alignment_bytes_size": alignment_bytes_size,
            "volume_bytes_size": volume_bytes_size,
            "append": append,
            "append_from_version": append_from_version,
            "append_from_uri": append_from_uri,
            "instance_name": instance_name,
        }

        self._queue: queue.Queue[t.Optional[DataRow]] = queue.Queue()
        self._summary = DatasetSummary()
        self._lock = threading.Lock()

        self._run_exception: t.Optional[Exception] = None

        self.daemon = True
        self._builder: t.Optional[BaseBuildExecutor] = None
        if append and append_from_version:
            _cls = create_generic_cls_by_mode(self.__iter__)
            self._builder = _cls(**self._kw)  # type: ignore
            self.start()

    def _raise_run_exception(self) -> None:
        if self._run_exception is not None:
            _e = self._run_exception
            self._run_exception = None
            raise threading.ThreadError(f"RowWriter Thread raise exception: {_e}")

    @property
    def summary(self) -> DatasetSummary:
        return self._summary

    def __enter__(self) -> RowWriter:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            logger.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    def flush(self) -> None:
        while not self._queue.empty():
            # TODO: tune flush with thread condition
            time.sleep(0.1)

        if self._builder:
            self._builder.flush()

    def close(self) -> None:
        self._queue.put(None)

        self.join()
        if self._builder:
            self._builder.close()

        self._raise_run_exception()

    def update(self, row_item: DataRow) -> None:
        self._raise_run_exception()
        self._queue.put(row_item)

        with self._lock:
            if self._builder is None:
                _cls = create_generic_cls(self.__iter__)
                self._builder = _cls(**self._kw)  # type: ignore
                self.start()

    def __iter__(self) -> t.Generator[DataRow, None, None]:
        while True:
            item = self._queue.get(block=True, timeout=None)
            if item is None:
                if self._queue.qsize() > 0:
                    continue
                else:
                    break

            if not isinstance(item, DataRow):
                continue

            yield item

    def run(self) -> None:
        try:
            if self._builder is None:
                raise RuntimeError("dataset builder object wasn't initialized")
            self._summary = self._builder.make_swds()
        except Exception as e:
            logger.exception(e)
            self._run_exception = e
            raise
