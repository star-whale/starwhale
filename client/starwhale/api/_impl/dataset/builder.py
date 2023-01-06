from __future__ import annotations

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

from starwhale.consts import DEFAULT_PROJECT, STANDALONE_INSTANCE, SWDS_DATA_FNAME_FMT
from starwhale.base.uri import URI
from starwhale.utils.fs import empty_dir, ensure_dir
from starwhale.base.type import (
    InstanceType,
    DataFormatType,
    DataOriginType,
    ObjectStoreType,
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
)
from starwhale.core.dataset.store import DatasetStorage
from starwhale.api._impl.data_store import SwObject
from starwhale.core.dataset.tabular import TabularDataset, TabularDatasetRow
from starwhale.api._impl.dataset.loader import DataRow

# TODO: tune header size
_header_magic = struct.unpack(">I", b"SWDS")[0]
_data_magic = struct.unpack(">I", b"SDWS")[0]
_header_struct = struct.Struct(">IIQIIII")
_header_size = _header_struct.size
_header_version = 0


_BDType = t.TypeVar("_BDType", bound="BaseBuildExecutor")


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
        self, row_content: t.Union[t.Tuple, DataRow], append_seq_id: int
    ) -> t.Tuple[t.Union[str, int], BaseArtifact, t.Dict]:
        if isinstance(row_content, DataRow):
            idx, row_data, row_annotations = row_content
        elif isinstance(row_content, tuple):
            if len(row_content) == 2:
                idx = append_seq_id
                row_data, row_annotations = row_content
            elif len(row_content) == 3:
                idx, row_data, row_annotations = row_content
            else:
                raise FormatError(
                    f"iter_item must return (data, annotations) or (id, data, annotations): {row_content}"
                )
        else:
            raise FormatError(
                f"row content not return tuple or DataRow type: {row_content}"
            )

        if not isinstance(row_annotations, dict):
            raise FormatError(f"annotations({row_annotations}) must be dict type")

        return idx, row_data, row_annotations


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

    _DATA_FMT = SWDS_DATA_FNAME_FMT

    class _BinSection(t.NamedTuple):
        offset: int
        size: int
        raw_data_offset: int
        raw_data_size: int

    def _write(self, writer: t.Any, data: bytes) -> _BinSection:
        size = len(data)
        crc = crc32(data)  # TODO: crc is right?
        start = writer.tell()
        padding_size = self._get_padding_size(size + _header_size)

        _header = _header_struct.pack(
            _header_magic, crc, 0, size, padding_size, _header_version, _data_magic
        )
        _padding = b"\0" * padding_size
        writer.write(_header + data + _padding)
        return self._BinSection(
            offset=start,
            size=_header_size + size + padding_size,
            raw_data_offset=start + _header_size,
            raw_data_size=size,
        )

    def _get_padding_size(self, size: int) -> int:
        remain = (size + _header_size) % self.alignment_bytes_size
        return 0 if remain == 0 else (self.alignment_bytes_size - remain)

    @property
    def data_format_type(self) -> DataFormatType:
        return DataFormatType.SWDS_BIN

    def make_swds(self) -> DatasetSummary:
        # TODO: add lock
        ds_copy_candidates: t.Dict[str, Path] = {}
        fno, wrote_size = 0, 0

        dwriter_path = self.data_tmpdir / str(fno)
        dwriter = dwriter_path.open("wb")
        ds_copy_candidates[str(fno)] = dwriter_path

        increased_rows = 0
        total_data_size = 0
        dataset_annotations: t.Dict[str, t.Any] = {}

        for append_seq_id, item_content in enumerate(
            self.iter_item(), start=self._forked_last_seq_id + 1
        ):
            idx, row_data, row_annotations = self._unpack_row_content(
                item_content, append_seq_id
            )

            _artifact: BaseArtifact
            if isinstance(row_data, bytes):
                _artifact = Binary(row_data, self.default_data_mime_type)
            elif isinstance(row_data, BaseArtifact):
                _artifact = row_data
            else:
                raise NoSupportError(f"data type {type(row_data)}")

            if not dataset_annotations:
                # TODO: check annotations type and name
                dataset_annotations = row_annotations

            _bin_section = self._write(dwriter, _artifact.to_bytes())
            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_link=Link(str(fno)),
                    data_format=self.data_format_type,
                    object_store_type=ObjectStoreType.LOCAL,
                    data_offset=_bin_section.raw_data_offset,
                    data_size=_bin_section.raw_data_size,
                    _swds_bin_offset=_bin_section.offset,
                    _swds_bin_size=_bin_section.size,
                    data_origin=DataOriginType.NEW,
                    data_type=_artifact.astype(),
                    annotations=row_annotations,
                    _append_seq_id=append_seq_id,
                )
            )

            total_data_size += _bin_section.size

            wrote_size += _bin_section.size
            if wrote_size > self.volume_bytes_size:
                wrote_size = 0
                fno += 1

                dwriter.close()
                dwriter_path = self.data_tmpdir / str(fno)
                dwriter = (dwriter_path).open("wb")
                ds_copy_candidates[str(fno)] = dwriter_path

            increased_rows += 1

        self.flush()

        try:
            empty = dwriter.tell() == 0
            dwriter.close()
            if empty:
                # last file is empty
                f = ds_copy_candidates[str(fno)]
                del ds_copy_candidates[str(fno)]
                os.unlink(f)
        except Exception as e:
            print(f"data write close exception: {e}")

        self._copy_files(ds_copy_candidates)
        self.tabular_dataset.info = self.get_info()  # type: ignore

        summary = DatasetSummary(
            rows=increased_rows,
            increased_rows=increased_rows,
            data_byte_size=total_data_size,
            include_user_raw=False,
            include_link=False,
            annotations=list(dataset_annotations.keys()),
        )
        return self._merge_forked_summary(summary)

    def _copy_files(
        self,
        ds_copy_candidates: t.Dict[str, Path],
    ) -> None:
        map_fno_sign: t.Dict[str, str] = {}
        for _fno, _src_path in ds_copy_candidates.items():
            _sign_name, _obj_path = DatasetStorage.save_data_file(
                _src_path, remove_src=True
            )
            map_fno_sign[_fno] = _sign_name

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

        # TODO: tune performance scan after put in a second
        for row in self.tabular_dataset.scan():
            if row.data_link.uri not in map_fno_sign:
                continue

            self.tabular_dataset.update(
                row_id=row.id, data_link=Link(map_fno_sign[row.data_link.uri])
            )


BuildExecutor = SWDSBinBuildExecutor


class UserRawBuildExecutor(BaseBuildExecutor):
    def make_swds(self) -> DatasetSummary:
        increased_rows = 0
        total_data_size = 0
        include_link = False

        map_path_sign: t.Dict[str, t.Tuple[str, Path]] = {}
        dataset_annotations: t.Dict[str, t.Any] = {}

        for append_seq_id, item_content in enumerate(
            self.iter_item(),
            start=self._forked_last_seq_id + 1,
        ):
            idx, row_data, row_annotations = self._unpack_row_content(
                item_content, append_seq_id
            )

            if not dataset_annotations:
                # TODO: check annotations type and name
                dataset_annotations = row_annotations

            if not isinstance(row_data, Link):
                raise FormatError(f"data({row_data}) must be Link type")

            if row_data.with_local_fs_data:
                _local_link = row_data
                _data_fpath = _local_link.uri
                if _data_fpath not in map_path_sign:
                    map_path_sign[_data_fpath] = DatasetStorage.save_data_file(
                        _data_fpath
                    )
                data_uri, _ = map_path_sign[_data_fpath]
                object_store_type = ObjectStoreType.LOCAL

                def _travel_link(obj: t.Any) -> None:
                    if isinstance(obj, Link):
                        if not obj.with_local_fs_data:
                            raise NoSupportError(
                                f"Local Link only supports local link annotations: {obj}"
                            )
                        if obj.uri not in map_path_sign:
                            map_path_sign[obj.uri] = DatasetStorage.save_data_file(
                                obj.uri
                            )
                        obj.local_fs_uri, _ = map_path_sign[obj.uri]
                    elif isinstance(obj, dict):
                        for v in obj.values():
                            _travel_link(v)
                    elif isinstance(obj, (list, tuple)):
                        for v in obj:
                            _travel_link(v)
                    elif isinstance(obj, SwObject):
                        for v in obj.__dict__.values():
                            _travel_link(v)

                _travel_link(row_annotations)
            else:
                _remote_link = row_data
                data_uri = _remote_link.uri
                object_store_type = ObjectStoreType.REMOTE
                include_link = True

            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_link=Link(data_uri),
                    data_format=self.data_format_type,
                    object_store_type=object_store_type,
                    data_offset=row_data.offset,
                    data_size=row_data.size,
                    data_origin=DataOriginType.NEW,
                    data_type=row_data.astype(),
                    annotations=row_annotations,
                    _append_seq_id=append_seq_id,
                )
            )

            total_data_size += row_data.size
            increased_rows += 1

        self._copy_files(map_path_sign)
        self.tabular_dataset.info = self.get_info()  # type: ignore

        # TODO: provide fine-grained rows/increased rows by dataset pythonic api
        summary = DatasetSummary(
            rows=increased_rows,
            increased_rows=increased_rows,
            data_byte_size=total_data_size,
            include_link=include_link,
            include_user_raw=True,
            annotations=list(dataset_annotations.keys()),
        )
        return self._merge_forked_summary(summary)

    def _copy_files(self, map_path_sign: t.Dict[str, t.Tuple[str, Path]]) -> None:
        for sign, obj_path in map_path_sign.values():
            # TODO: use relative symlink or fix link command for datastore dir moving
            (self.data_output_dir / sign[: DatasetStorage.short_sign_cnt]).symlink_to(
                obj_path.absolute()
            )

    @property
    def data_format_type(self) -> DataFormatType:
        return DataFormatType.USER_RAW


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

    if isinstance(item, DataRow):
        data = item.data
    elif isinstance(item, (tuple, list)):
        if len(item) == 2:
            data = item[0]
        elif len(item) == 3:
            data = item[1]
        else:
            raise FormatError(f"wrong item format: {item}")
    else:
        raise TypeError(f"item only supports tuple, list or DataRow type: {item}")

    use_swds_bin = not isinstance(data, Link)
    return create_generic_cls_by_mode(use_swds_bin, _do_iter_item)


def create_generic_cls_by_mode(
    use_swds_bin: bool, iter_func: t.Callable
) -> t.Type[BaseBuildExecutor]:
    attrs = {"iter_item": iter_func}
    if use_swds_bin:
        _cls = type(
            "GenericSWDSBinHandler",
            (SWDSBinBuildExecutor,),
            attrs,
        )
    else:
        _cls = type(
            "GenericUserRawHandler",
            (UserRawBuildExecutor,),
            attrs,
        )
    return _cls


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
        append_with_swds_bin: bool = True,
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
            _cls = create_generic_cls_by_mode(append_with_swds_bin, self.__iter__)
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
