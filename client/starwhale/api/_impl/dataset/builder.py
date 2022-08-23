from __future__ import annotations

import shutil
import struct
import typing as t
from abc import ABCMeta, abstractmethod
from types import TracebackType
from pathlib import Path
from binascii import crc32

import jsonlines

from starwhale.consts import AUTH_ENV_FNAME, SWDS_DATA_FNAME_FMT
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import DataFormatType, DataOriginType, ObjectStoreType
from starwhale.utils.error import FormatError
from starwhale.core.dataset.type import (
    Link,
    LinkAuth,
    DatasetSummary,
    D_ALIGNMENT_SIZE,
    D_FILE_VOLUME_SIZE,
)
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
    ) -> None:
        # TODO: add more docstring for args
        # TODO: validate group upper and lower?
        self.data_dir = data_dir
        self.data_filter = data_filter
        self.label_filter = label_filter
        self.workdir = workdir
        self.data_output_dir = workdir / "data"
        self.alignment_bytes_size = alignment_bytes_size
        self.volume_bytes_size = volume_bytes_size

        self.project_name = project_name
        self.dataset_name = dataset_name
        self.dataset_version = dataset_version
        self.tabular_dataset = TabularDataset(
            dataset_name, dataset_version, project_name
        )

        self._index_writer: t.Optional[jsonlines.Writer] = None
        self._prepare()

    def _prepare(self) -> None:
        ensure_dir(self.data_output_dir)

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

        print("cleanup done.")

    @abstractmethod
    def make_swds(self) -> DatasetSummary:
        raise NotImplementedError

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
        idx             uint64  Q
        size            uint32  I
        padding_size    uint32  I
        header_version  uint32  I
        data_magic      uint32  I --> above 32 bytes
        data bytes...
        padding bytes...        --> default 4K padding
    """

    # TODO: add more docstring for class

    _DATA_FMT = SWDS_DATA_FNAME_FMT

    def _write(self, writer: t.Any, idx: int, data: bytes) -> t.Tuple[int, int]:
        size = len(data)
        crc = crc32(data)  # TODO: crc is right?
        start = writer.tell()
        padding_size = self._get_padding_size(size + _header_size)

        _header = _header_struct.pack(
            _header_magic, crc, idx, size, padding_size, _header_version, _data_magic
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
        fno, wrote_size = 0, 0
        dwriter = (self.data_output_dir / self._DATA_FMT.format(index=fno)).open("wb")
        rows, increased_rows = 0, 0
        total_label_size, total_data_size = 0, 0

        for idx, ((_, data), (_, label)) in enumerate(
            zip(self.iter_all_dataset_slice(), self.iter_all_label_slice())
        ):
            if not isinstance(data, bytes) or not isinstance(label, bytes):
                raise FormatError("data and label must be bytes type")

            # TODO: support inherit data from old dataset version
            data_origin = DataOriginType.NEW
            data_offset, data_size = self._write(dwriter, idx, data)
            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_uri=self._DATA_FMT.format(index=fno),
                    label=label,
                    data_format=self.data_format_type,
                    object_store_type=ObjectStoreType.LOCAL,
                    data_offset=data_offset,
                    data_size=data_size,
                    data_origin=data_origin,
                )
            )

            total_data_size += data_size
            total_label_size += len(label)

            wrote_size += data_size
            if wrote_size > self.volume_bytes_size:
                wrote_size = 0
                fno += 1

                dwriter.close()
                dwriter = (
                    self.data_output_dir / self._DATA_FMT.format(index=fno)
                ).open("wb")

            rows += 1
            if data_origin == DataOriginType.NEW:
                increased_rows += 1

        try:
            dwriter.close()
        except Exception as e:
            print(f"data write close exception: {e}")

        summary = DatasetSummary(
            rows=rows,
            increased_rows=increased_rows,
            label_byte_size=total_label_size,
            data_byte_size=total_data_size,
            include_user_raw=False,
            include_link=False,
        )
        return summary

    def iter_data_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        with Path(path).open() as f:
            yield f.read()

    def iter_label_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        yield Path(path).name


BuildExecutor = SWDSBinBuildExecutor


class UserRawBuildExecutor(BaseBuildExecutor):
    def make_swds(self) -> DatasetSummary:
        rows, increased_rows = 0, 0
        total_label_size, total_data_size = 0, 0
        ds_copy_candidates = {}
        auth_candidates = {}
        include_link = False

        for idx, (data, (_, label)) in enumerate(
            zip(self.iter_all_dataset_slice(), self.iter_all_label_slice())
        ):
            if isinstance(data, Link):
                data_uri = data.uri
                data_offset, data_size = data.offset, data.size
                if data.auth:
                    auth = data.auth.name
                    auth_candidates[f"{data.auth.ltype}.{data.auth.name}"] = data.auth
                else:
                    auth = ""
                object_store_type = ObjectStoreType.REMOTE
                include_link = True
            elif isinstance(data, (tuple, list)):
                data_path, (data_offset, data_size) = data
                auth = ""
                data_uri = str(Path(data_path).relative_to(self.data_dir))
                ds_copy_candidates[data_uri] = data_path
                object_store_type = ObjectStoreType.LOCAL
            else:
                raise FormatError(f"data({data}) type error, no list, tuple or Link")

            data_origin = DataOriginType.NEW

            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_uri=str(data_uri),
                    label=label,
                    data_format=self.data_format_type,
                    object_store_type=object_store_type,
                    data_offset=data_offset,
                    data_size=data_size,
                    data_origin=data_origin,
                    auth_name=auth,
                )
            )

            total_data_size += data_size
            total_label_size += len(label)

            rows += 1
            if data_origin == DataOriginType.NEW:
                increased_rows += 1

        self._copy_files(ds_copy_candidates)
        self._copy_auth(auth_candidates)

        summary = DatasetSummary(
            rows=rows,
            increased_rows=increased_rows,
            label_byte_size=total_label_size,
            data_byte_size=total_data_size,
            include_link=include_link,
            include_user_raw=True,
        )
        return summary

    def _copy_files(self, ds_copy_candidates: t.Dict[str, Path]) -> None:
        for fname, src in ds_copy_candidates.items():
            dest = self.data_output_dir / fname
            ensure_dir(dest.parent)
            shutil.copyfile(str(src.absolute()), str(dest.absolute()))

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
