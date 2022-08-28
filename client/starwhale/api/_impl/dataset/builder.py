from __future__ import annotations

import sys
import struct
import typing as t
import tempfile
from abc import ABCMeta, abstractmethod
from types import TracebackType
from pathlib import Path
from binascii import crc32

import jsonlines

from starwhale.consts import AUTH_ENV_FNAME, SWDS_DATA_FNAME_FMT
from starwhale.base.uri import URI
from starwhale.utils.fs import empty_dir, ensure_dir
from starwhale.base.type import DataFormatType, DataOriginType, ObjectStoreType
from starwhale.utils.error import FormatError, NoSupportError
from starwhale.core.dataset import model
from starwhale.core.dataset.type import (
    Link,
    LinkAuth,
    MIMEType,
    DatasetSummary,
    D_ALIGNMENT_SIZE,
    D_FILE_VOLUME_SIZE,
)
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import TabularDataset, TabularDatasetRow

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
        data_dir: Path = Path("."),
        workdir: Path = Path("./sw_output"),
        data_filter: str = "*",
        label_filter: str = "*",
        alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        volume_bytes_size: int = D_FILE_VOLUME_SIZE,
        append: bool = False,
        append_from_version: str = "",
        append_from_uri: t.Optional[URI] = None,
        data_mime_type: MIMEType = MIMEType.UNDEFINED,
    ) -> None:
        # TODO: add more docstring for args
        # TODO: validate group upper and lower?
        self.data_dir = data_dir
        self.data_filter = data_filter
        self.label_filter = label_filter

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
            dataset_name, dataset_version, project_name
        )

        self._forked_summary: t.Optional[DatasetSummary]
        if append and append_from_uri:
            self._forked_last_idx, self._forked_rows = self.tabular_dataset.fork(
                append_from_version
            )
            self._forked_summary = model.Dataset.get_dataset(append_from_uri).summary()
        else:
            self._forked_last_idx = -1
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
        if value:
            print(f"type:{type}, exception:{value}, traceback:{trace}")

        try:
            self.tabular_dataset.close()
        except Exception as e:
            print(f"tabular dataset close exception: {e}")

        try:
            empty_dir(self.data_tmpdir)
        except Exception as e:
            print(f"empty {self.data_tmpdir} exception: {e}")

        print("cleanup done.")

    @abstractmethod
    def make_swds(self) -> DatasetSummary:
        raise NotImplementedError

    def _merge_forked_summary(self, s: DatasetSummary) -> DatasetSummary:
        _fs = self._forked_summary
        if _fs:
            s.rows += _fs.rows
            s.unchanged_rows += _fs.rows
            s.data_byte_size += _fs.data_byte_size
            s.label_byte_size += _fs.label_byte_size
            s.include_link |= _fs.include_link
            s.include_user_raw |= _fs.include_user_raw

        return s

    def _iter_files(
        self, filter: str, sort_key: t.Optional[t.Any] = None
    ) -> t.Generator[Path, None, None]:
        _key = sort_key
        if _key is not None and not callable(_key):
            raise Exception(f"data_sort_func({_key}) is not callable.")

        _files = sorted(self.data_dir.rglob(filter), key=_key)
        for p in _files:
            if not p.is_file():
                continue
            yield p

    def iter_data_files(self) -> t.Generator[Path, None, None]:
        return self._iter_files(self.data_filter, self.data_sort_func())

    def iter_label_files(self) -> t.Generator[Path, None, None]:
        return self._iter_files(self.label_filter, self.label_sort_func())

    def iter_all_dataset_slice(self) -> t.Generator[t.Any, None, None]:
        for p in self.iter_data_files():
            for d in self.iter_data_slice(str(p.absolute())):
                yield p, d

    def iter_all_label_slice(self) -> t.Generator[t.Any, None, None]:
        for p in self.iter_label_files():
            for d in self.iter_label_slice(str(p.absolute())):
                yield p, d

    @abstractmethod
    def iter_data_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        raise NotImplementedError

    @abstractmethod
    def iter_label_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        raise NotImplementedError

    @property
    def data_format_type(self) -> DataFormatType:
        raise NotImplementedError

    def data_sort_func(self) -> t.Any:
        return None

    def label_sort_func(self) -> t.Any:
        return None


class SWDSBinBuildExecutor(BaseBuildExecutor):
    """
    SWDSBinBuildExecutor can build swds_bin.

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

    def _write(self, writer: t.Any, data: bytes) -> t.Tuple[int, int]:
        size = len(data)
        crc = crc32(data)  # TODO: crc is right?
        start = writer.tell()
        padding_size = self._get_padding_size(size + _header_size)

        # TODO: remove idx field
        _header = _header_struct.pack(
            _header_magic, crc, 0, size, padding_size, _header_version, _data_magic
        )
        _padding = b"\0" * padding_size
        writer.write(_header + data + _padding)
        return start, _header_size + size + padding_size

    def _get_padding_size(self, size: int) -> int:
        remain = (size + _header_size) % self.alignment_bytes_size
        return 0 if remain == 0 else (self.alignment_bytes_size - remain)

    @property
    def data_format_type(self) -> DataFormatType:
        return DataFormatType.SWDS_BIN

    def make_swds(self) -> DatasetSummary:
        # TODO: add lock
        ds_copy_candidates: t.Dict[int, Path] = {}
        fno, wrote_size = 0, 0

        dwriter_path = self.data_tmpdir / str(fno)
        dwriter = dwriter_path.open("wb")
        ds_copy_candidates[fno] = dwriter_path

        increased_rows = 0
        total_label_size, total_data_size = 0, 0

        for idx, ((_, data), (_, label)) in enumerate(
            zip(self.iter_all_dataset_slice(), self.iter_all_label_slice()),
            start=self._forked_last_idx + 1,
        ):
            if isinstance(data, (tuple, list)):
                _data_content, _data_mime_type = data
            else:
                _data_content, _data_mime_type = data, self.default_data_mime_type

            if not isinstance(_data_content, bytes):
                raise FormatError("data content must be bytes type")

            data_offset, data_size = self._write(dwriter, _data_content)
            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_uri=str(fno),
                    label=label,
                    data_format=self.data_format_type,
                    object_store_type=ObjectStoreType.LOCAL,
                    data_offset=data_offset,
                    data_size=data_size,
                    data_origin=DataOriginType.NEW,
                    data_mime_type=_data_mime_type or self.default_data_mime_type,
                )
            )

            total_data_size += data_size
            total_label_size += sys.getsizeof(label)

            wrote_size += data_size
            if wrote_size > self.volume_bytes_size:
                wrote_size = 0
                fno += 1

                dwriter.close()
                dwriter_path = self.data_tmpdir / str(fno)
                dwriter = (dwriter_path).open("wb")
                ds_copy_candidates[fno] = dwriter_path

            increased_rows += 1

        try:
            dwriter.close()
        except Exception as e:
            print(f"data write close exception: {e}")

        self._copy_files(
            ds_copy_candidates,
            (self._forked_last_idx + 1, self._forked_last_idx + 1 + increased_rows),
        )

        summary = DatasetSummary(
            rows=increased_rows,
            increased_rows=increased_rows,
            label_byte_size=total_label_size,
            data_byte_size=total_data_size,
            include_user_raw=False,
            include_link=False,
        )
        return self._merge_forked_summary(summary)

    def _copy_files(
        self,
        ds_copy_candidates: t.Dict[int, Path],
        row_pos: t.Tuple[int, int],
    ) -> None:
        map_fno_sign: t.Dict[int, str] = {}
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
                    _dest_path.unlink(missing_ok=True)

            _dest_path.symlink_to(_obj_path)

        for row in self.tabular_dataset.scan(*row_pos):
            self.tabular_dataset.update(
                row_id=row.id, data_uri=map_fno_sign[int(row.data_uri)]
            )

    def iter_data_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        with Path(path).open() as f:
            yield f.read()

    def iter_label_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        yield Path(path).name


BuildExecutor = SWDSBinBuildExecutor


class UserRawBuildExecutor(BaseBuildExecutor):
    def make_swds(self) -> DatasetSummary:
        increased_rows = 0
        total_label_size, total_data_size = 0, 0
        auth_candidates = {}
        include_link = False

        map_path_sign: t.Dict[str, t.Tuple[str, Path]] = {}

        for idx, (data, (_, label)) in enumerate(
            zip(self.iter_all_dataset_slice(), self.iter_all_label_slice()),
            start=self._forked_last_idx + 1,
        ):
            if isinstance(data, Link):
                _remote_link = data
                data_uri = _remote_link.uri
                data_offset, data_size = _remote_link.offset, _remote_link.size
                if _remote_link.auth:
                    auth = _remote_link.auth.name
                    auth_candidates[
                        f"{_remote_link.auth.ltype}.{_remote_link.auth.name}"
                    ] = _remote_link.auth
                else:
                    auth = ""
                object_store_type = ObjectStoreType.REMOTE
                include_link = True
                data_mime_type = _remote_link.mime_type
            elif isinstance(data, (tuple, list)):
                _data_fpath, _local_link = data
                if _data_fpath not in map_path_sign:
                    map_path_sign[_data_fpath] = DatasetStorage.save_data_file(
                        _data_fpath
                    )

                if not isinstance(_local_link, Link):
                    raise NoSupportError("data only support Link type")

                data_mime_type = _local_link.mime_type
                data_offset, data_size = _local_link.offset, _local_link.size
                data_uri, _ = map_path_sign[_data_fpath]
                auth = ""
                object_store_type = ObjectStoreType.LOCAL
            else:
                raise FormatError(f"data({data}) type error, no list, tuple or Link")

            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_uri=data_uri,
                    label=label,
                    data_format=self.data_format_type,
                    object_store_type=object_store_type,
                    data_offset=data_offset,
                    data_size=data_size,
                    data_origin=DataOriginType.NEW,
                    auth_name=auth,
                    data_mime_type=data_mime_type,
                )
            )

            total_data_size += data_size
            total_label_size += sys.getsizeof(label)
            increased_rows += 1

        self._copy_files(map_path_sign)
        self._copy_auth(auth_candidates)

        # TODO: provide fine-grained rows/increased rows by dataset pythonic api
        summary = DatasetSummary(
            rows=increased_rows,
            increased_rows=increased_rows,
            label_byte_size=total_label_size,
            data_byte_size=total_data_size,
            include_link=include_link,
            include_user_raw=True,
        )
        return self._merge_forked_summary(summary)

    def _copy_files(self, map_path_sign: t.Dict[str, t.Tuple[str, Path]]) -> None:
        for sign, obj_path in map_path_sign.values():
            # TODO: use relative symlink or fix link command for datastore dir moving
            (self.data_output_dir / sign[: DatasetStorage.short_sign_cnt]).symlink_to(
                obj_path.absolute()
            )

    def _copy_auth(self, auth_candidates: t.Dict[str, LinkAuth]) -> None:
        if not auth_candidates:
            return

        with (self.workdir / AUTH_ENV_FNAME).open("w") as f:
            for auth in auth_candidates.values():
                f.write("\n".join(auth.dump_env()))

    def iter_data_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        yield 0, Path(path).stat().st_size

    def iter_label_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        yield Path(path).name

    @property
    def data_format_type(self) -> DataFormatType:
        return DataFormatType.USER_RAW
