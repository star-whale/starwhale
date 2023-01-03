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
from collections import namedtuple
from types import TracebackType
from pathlib import Path
from binascii import crc32

import jsonlines
from loguru import logger
from typing_extensions import Protocol

from starwhale.consts import (
    DEFAULT_PROJECT,
    STANDALONE_INSTANCE,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import empty_dir, ensure_dir
from starwhale.base.type import (
    InstanceType,
    DataFormatType,
    DataOriginType,
)
from starwhale.utils.error import FormatError, NoSupportError
from starwhale.core.dataset import model
from starwhale.core.dataset.type import (
    Link,
    Binary,
    MIMEType,
    BaseArtifact,
    DatasetSummary,
    D_ALIGNMENT_SIZE,
    D_FILE_VOLUME_SIZE,
    D_BIN_TO_LINK_THRESHOLD,
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
    def binary_to_link(
        self,
        row_contents: t.List[t.Dict[str, t.Any]],
    ) -> int:
        """
            Find large bytes or local fs file in row_contents. Convert them to accessible link
            return the total bytes the method processes
        """
        ...


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
        if bin_writer:
            self.bin_writer = bin_writer
        else:
            self.bin_writer = SWDSBinBuildExecutor._BinWriter(self.data_tmpdir,self.data_output_dir,self.alignment_bytes_size,self.volume_bytes_size)
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
        self.tabular_dataset.flush()

    def get_info(self) -> t.Optional[t.Dict[str, t.Any]]:
        return None

    @abstractmethod
    def iter_item(self) -> t.Generator[t.Tuple, None, None]:
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
            s.annotations = list(set(s.annotations) | set(_fs.annotations))
            s.include_link |= _fs.include_link
            s.include_user_raw |= _fs.include_user_raw

        return s

    @property
    def data_format_type(self) -> DataFormatType:
        raise NotImplementedError

    def _unpack_row_content(
        self, row_data: t.Union[t.Tuple, DataRow], append_seq_id: int
    ) -> t.Tuple[t.Union[str, int], BaseArtifact, t.Dict]:
        if isinstance(row_data, DataRow):
            idx, row = row_data.index, row_data.data
        elif isinstance(row_data, dict):
            idx, row = append_seq_id, row_data
        elif isinstance(row_data, tuple):
            if len(row_data) == 1:
                idx = append_seq_id
                row = row_data
            elif len(row_data) == 2:
                idx, row = row_data
            else:
                raise FormatError(
                    f"iter_item must return (data, annotations) or (id, data, annotations): {row_data}"
                )
        else:
            raise FormatError(
                f"row content not return tuple or DataRow type: {row_data}"
            )

        if not isinstance(row, dict):
            raise FormatError(f"content({row}) must be dict type")

        return idx, row


class SWDSBinBuildExecutor(BaseBuildExecutor):
    """
    SWDSBinBuildExecutor builds swds_bin format dataset.

    swds_bin format:
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

    # TODO: add more docstring for class

    class _BinSection(t.NamedTuple):
        offset: int
        size: int
        raw_data_offset: int
        raw_data_size: int
        fno: str

    class _BinWriter:

        def __init__(self, work_dir: str, data_output_dir: str, alignment_bytes_size: int = D_ALIGNMENT_SIZE,
                    volume_bytes_size: int = D_FILE_VOLUME_SIZE, bin2link_threshold: int = D_BIN_TO_LINK_THRESHOLD) -> None:
            self._src_path_spec = namedtuple("_src_path_spec", ["src_path", "remove_src"])
            self.work_dir = work_dir
            self.data_output_dir = data_output_dir
            self.volume_bytes_size = volume_bytes_size
            self.bin2link_threshold = bin2link_threshold
            self.alignment_bytes_size = alignment_bytes_size
            self.ds_copy_candidates: t.Dict[str, Path] = {}
            self.fno = 0
            self.wrote_size = 0
            dwriter_path = self.work_dir / str(self.fno)
            self.dwriter = dwriter_path.open("wb")
            self.ds_copy_candidates[str(self.fno)] = self._src_path_spec(dwriter_path, True)
            self.total_data_size = 0
            self.map_fno_sign: t.Dict[str, str] = {}

        def write(self, content: t.Union[bytes,str,Path]) -> SWDSBinBuildExecutor._BinSection:
            if isinstance(content, str):
                self.ds_copy_candidates[content] = self._src_path_spec(Path(content), False)
                return None
            if isinstance(content, Path):
                self.ds_copy_candidates[str(content)] = self._src_path_spec(content, False)
                return None
            _bin_section = self._write(self.dwriter, content)
            self.total_data_size += _bin_section.size
            self.wrote_size += _bin_section.size
            if self.wrote_size > self.volume_bytes_size:
                self.wrote_size = 0
                self.fno += 1
                self.dwriter.close()
                dwriter_path = self.work_dir / str(self.fno)
                self.dwriter = (dwriter_path).open("wb")
                self.ds_copy_candidates[str(self.fno)] = self._src_path_spec(dwriter_path, True)
            return _bin_section

        def _write(self, writer: t.Any, data: bytes) -> SWDSBinBuildExecutor._BinSection:
            size = len(data)
            crc = crc32(data)  # TODO: crc is right?
            start = writer.tell()
            padding_size = self._get_padding_size(size + _header_size)

            _header = _header_struct.pack(
                _header_magic, crc, 0, size, padding_size, _header_version, _data_magic
            )
            _padding = b"\0" * padding_size
            writer.write(_header + data + _padding)
            return SWDSBinBuildExecutor._BinSection(
                offset=start,
                size=_header_size + size + padding_size,
                raw_data_offset=start + _header_size,
                raw_data_size=size,
                fno=str(self.fno),

            )

        def _copy_files(
            self,
            ds_copy_candidates: t.Dict[str, Path],
        ) -> None:

            for _fno, _src_path_spec in ds_copy_candidates.items():
                _sign_name, _obj_path = DatasetStorage.save_data_file(
                    _src_path_spec.src_path, remove_src=_src_path_spec.remove_src
                )
                self.map_fno_sign[_fno] = _sign_name

                _dest_path = (
                    self.data_output_dir / _sign_name[: DatasetStorage.short_sign_cnt]
                )
                _obj_path = _obj_path.resolve().absolute()

                if _dest_path.exists():
                    if _dest_path.resolve() == _obj_path:
                        continue
                    else:
                        _dest_path.unlink()

                _dest_path.symlink_to(_obj_path)

        def close(self):
            try:
                empty = self.dwriter.tell() == 0
                self.dwriter.close()
                if empty:
                    # last file is empty
                    f = self.ds_copy_candidates[str(self.fno)].src_path
                    del self.ds_copy_candidates[str(self.fno)]
                    os.unlink(f)
            except Exception as e:
                print(f"data write close exception: {e}")

            self._copy_files(self.ds_copy_candidates)

        def _get_padding_size(self, size: int) -> int:
            remain = (size + _header_size) % self.alignment_bytes_size
            return 0 if remain == 0 else (self.alignment_bytes_size - remain)

        def binary_to_link(self, row_contents: t.List[t.Dict[str, t.Any]]) -> int:
            for row_content in row_contents:
                self._store_binary(row_content)
            self.close()
            for row_content in row_contents:
                self._update_link(row_content)
            return self.total_data_size

        def _store_binary(self, row_content: t.Dict) -> None:
            for _, v in row_content.items():
                if isinstance(v, dict):
                    self._store_binary(v)
                if not isinstance(v, BaseArtifact):
                    continue
                if not v.link and (isinstance(v.fp, bytes) or isinstance(v.fp, io.IOBase)) and len(v.to_bytes()) > self.bin2link_threshold:
                    _bin_section = self.write(v.to_bytes())
                    v.link = Link(_bin_section.fno, offset=_bin_section.raw_data_offset,
                                  size=_bin_section.raw_data_size,
                                  bin_offset=_bin_section.offset, bin_size=_bin_section.size)
                if not v.link and isinstance(v.fp, str):
                    v.link = Link(v.fp, with_local_fs_data=True)
                if v.link and v.link.with_local_fs_data:
                    self.write(v.link.uri)

        def _update_link(self, row_content: t.Dict) -> None:
            for _, v in row_content.items():
                if isinstance(v, dict):
                    self._update_link(v)
                if not isinstance(v, BaseArtifact):
                    continue
                if not v.link:
                    continue
                if not v.link.uri in self.map_fno_sign:
                    continue
                v.link.uri = self.map_fno_sign[v.link.uri]
                v.fp = None

    def make_swds(self) -> DatasetSummary:
        # TODO: add lock
        row_contents = [(append_seq_id, item_content) for append_seq_id, item_content in enumerate(
            self.iter_item(), start=self._forked_last_seq_id + 1
        )]
        total_bin_size = self.bin_writer.binary_to_link([row_content[1] for row_content in row_contents])
        increased_rows = 0
        for append_seq_id, item_content in row_contents:
            idx, row_content = self._unpack_row_content(
                item_content, append_seq_id
            )
            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_origin=DataOriginType.NEW,
                    data=row_content,
                    _append_seq_id=append_seq_id,
                )
            )
            increased_rows += 1

        self.flush()
        self.tabular_dataset.info = self.get_info()  # type: ignore

        summary = DatasetSummary(
            rows=increased_rows,
            increased_rows=increased_rows,
            data_byte_size=total_bin_size,
        )
        return self._merge_forked_summary(summary)


BuildExecutor = SWDSBinBuildExecutor


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


def create_generic_cls_by_mode(
    iter_func: t.Callable
) -> t.Type[BaseBuildExecutor]:
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
