from __future__ import annotations

import sys
import math
import struct
import typing as t
from abc import ABCMeta, abstractmethod
from copy import deepcopy
from types import TracebackType
from pathlib import Path
from binascii import crc32

import jsonlines

from starwhale.utils import validate_obj_name
from starwhale.consts import VERSION_PREFIX_CNT, SWDS_DATA_FNAME_FMT
from starwhale.base.uri import URI
from starwhale.base.type import InstanceType
from starwhale.utils.error import (
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.dataset import (
    D_ALIGNMENT_SIZE,
    D_USER_BATCH_SIZE,
    D_FILE_VOLUME_SIZE,
)

# TODO: tune header size
_header_magic = struct.unpack(">I", b"SWDS")[0]
_data_magic = struct.unpack(">I", b"SDWS")[0]
_header_struct = struct.Struct(">IIQIIII")
_header_size = _header_struct.size


class RawDataFormatType:
    SWDS_BIN = "s"
    USER = "u"


class ObjectStoreType:
    LOCAL = "l"
    REMOTE = "r"


class DataOriginType:
    NEW = "n"
    INHERIT = "i"


class TabularDatasetRow:
    def __init__(
        self,
        id: int,
        data_uri: str,
        label: t.Union[str, bytes],
        data_format: str = RawDataFormatType.SWDS_BIN,
        object_store_type: str = ObjectStoreType.LOCAL,
        data_offset: int = 0,
        data_size: int = 0,
        data_origin: str = DataOriginType.NEW,
        **kw: t.Any,
    ) -> None:
        self.id = id
        self.data_uri = data_uri.strip()
        self.data_format = data_format
        self.data_offset = data_offset
        self.data_size = data_size
        self.data_origin = data_origin
        self.object_store_type = object_store_type
        self.label = label.encode() if isinstance(label, str) else label

        # TODO: add non-starwhale object store related fields, such as address, authority
        # TODO: add data uri crc for versioning
        # TODO: support user custom annotations

    def _do_validate(self) -> None:
        if self.id < 0:
            raise FieldTypeOrValueError(
                f"id need to be greater than or equal to zero, but current id is {self.id}"
            )

        if not self.data_uri:
            raise FieldTypeOrValueError("no raw_data_uri field")

        if self.data_format not in [RawDataFormatType.SWDS_BIN, RawDataFormatType.USER]:
            raise NoSupportError(f"data format: {self.data_format}")

        if self.data_origin not in [DataOriginType.NEW, DataOriginType.INHERIT]:
            raise NoSupportError(f"data origin: {self.data_origin}")

        # TODO: support non-starwhale remote object store, for index-only feature
        if self.object_store_type != ObjectStoreType.LOCAL:
            raise NoSupportError(f"object store {self.object_store_type}")

    def __str__(self) -> str:
        return f"row-{self.id}, data-{self.data_uri}, origin-[{self.data_origin}]"

    def __repr__(self) -> str:
        return f"row-{self.id}, data-{self.data_uri}(offset:{self.data_offset}, size:{self.data_size}, format:{self.data_format}), origin-[{self.data_origin}], object store-{self.object_store_type}"

    def asdict(self) -> t.Dict[str, t.Union[str, bytes, int]]:
        return deepcopy(self.__dict__)


class TabularDataset:
    def __init__(self, name: str, version: str, project: str) -> None:
        self.name = name
        self.version = version
        self.table_name = f"{name}/{version[:VERSION_PREFIX_CNT]}/{version}"
        self._ds_wrapper = DatastoreWrapperDataset(self.table_name, project)

        self.start = -1
        self.end = sys.maxsize

        self._do_validate()

    def _do_validate(self) -> None:
        _ok, _reason = validate_obj_name(self.name)
        if not _ok:
            raise InvalidObjectName(f"{self.name}: {_reason}")

        if not self.version:
            raise FieldTypeOrValueError("no version field")

    def __str__(self) -> str:
        return f"Dataset Table: {self._ds_wrapper._meta_table_name}"

    __repr__ = __str__

    def set_range(self, start: int, end: int) -> None:
        self.start = max(start, self.start)
        self.end = min(end, self.end)

    def put(self, row: TabularDatasetRow) -> None:
        self._ds_wrapper.put(row.id, **row.asdict())

    def scan_all(self) -> t.Generator[TabularDatasetRow, None, None]:
        return self.scan(self.start, self.end)

    def scan(self, start: int, end: int) -> t.Generator[TabularDatasetRow, None, None]:
        for _d in self._ds_wrapper.scan(start, end):
            yield TabularDatasetRow(**_d)

    def close(self) -> None:
        self._ds_wrapper.close()

    @classmethod
    def from_uri(cls, uri: URI) -> TabularDataset:
        _version = uri.object.version
        if uri.instance_type == InstanceType.STANDALONE:
            _store = DatasetStorage(uri)
            _version = _store.id

        return TabularDataset(
            uri.object.name,
            _version,
            uri.project,
        )


class BuildExecutor(metaclass=ABCMeta):
    """
    BuildExecutor can build swds.

    swds_bin format:
        header_magic  uint32  I
        crc           uint32  I
        idx           uint64  Q
        size          uint32  I
        padding_size  uint32  I
        batch_size    uint32  I
        data_magic    uint32  I --> above 32 bytes
        data bytes...
        padding bytes...        --> default 4K padding
    """

    # TODO: add more docstring for class

    _DATA_FMT = SWDS_DATA_FNAME_FMT

    def __init__(
        self,
        dataset_name: str,
        dataset_version: str,
        project_name: str,
        data_dir: Path = Path("."),
        output_dir: Path = Path("./sw_output"),
        data_filter: str = "*",
        label_filter: str = "*",
        batch: int = D_USER_BATCH_SIZE,
        alignment_bytes_size: int = D_ALIGNMENT_SIZE,
        volume_bytes_size: int = D_FILE_VOLUME_SIZE,
    ) -> None:
        # TODO: add more docstring for args
        # TODO: validate group upper and lower?
        self._batch = max(batch, 1)
        self.data_dir = data_dir
        self.data_filter = data_filter
        self.label_filter = label_filter
        self.output_dir = output_dir
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
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def __enter__(self) -> BuildExecutor:
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

    def _write(self, writer: t.Any, idx: int, data: bytes) -> t.Tuple[int, int]:
        size = len(data)
        crc = crc32(data)  # TODO: crc is right?
        start = writer.tell()
        padding_size = self._get_padding_size(size + _header_size)

        _header = _header_struct.pack(
            _header_magic, crc, idx, size, padding_size, self._batch, _data_magic
        )
        _padding = b"\0" * padding_size
        writer.write(_header + data + _padding)
        return start, _header_size + size + padding_size

    def _get_padding_size(self, size: int) -> int:
        remain = (size + _header_size) % self.alignment_bytes_size
        return 0 if remain == 0 else (self.alignment_bytes_size - remain)

    def make_swds(self) -> None:
        # TODO: add lock
        fno, wrote_size = 0, 0
        dwriter = (self.output_dir / self._DATA_FMT.format(index=fno)).open("wb")

        for idx, (data, label) in enumerate(
            zip(self.iter_all_dataset_slice(), self.iter_all_label_slice())
        ):
            data_offset, data_size = self._write(dwriter, idx, data)
            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_uri=self._DATA_FMT.format(index=fno),
                    label=label,
                    data_format=RawDataFormatType.SWDS_BIN,
                    object_store_type=ObjectStoreType.LOCAL,
                    data_offset=data_offset,
                    data_size=data_size,
                    data_origin=DataOriginType.NEW,
                )
            )

            wrote_size += data_size
            if wrote_size > self.volume_bytes_size:
                wrote_size = 0
                fno += 1

                dwriter.close()
                dwriter = (self.output_dir / self._DATA_FMT.format(index=fno)).open(
                    "wb"
                )

        try:
            dwriter.close()
        except Exception as e:
            print(f"data write close exception: {e}")

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
                yield d

    def iter_all_label_slice(self) -> t.Generator[t.Any, None, None]:
        for p in self.iter_label_files():
            for d in self.iter_label_slice(str(p.absolute())):
                yield d

    @abstractmethod
    def iter_data_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        raise NotImplementedError

    @abstractmethod
    def iter_label_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        raise NotImplementedError

    def data_sort_func(self) -> t.Any:
        return None

    def label_sort_func(self) -> t.Any:
        return None


class MNISTBuildExecutor(BuildExecutor):
    def iter_data_slice(self, path: str) -> t.Generator[bytes, None, None]:
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number, height, width = struct.unpack(">IIII", f.read(16))
            print(f">data({fpath.name}) split {math.ceil(number / self._batch)} group")

            while True:
                content = f.read(self._batch * height * width)
                if not content:
                    break
                yield content

    def iter_label_slice(self, path: str) -> t.Generator[bytes, None, None]:
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number = struct.unpack(">II", f.read(8))
            print(f">label({fpath.name}) split {math.ceil(number / self._batch)} group")

            while True:
                content = f.read(self._batch)
                if not content:
                    break
                yield content


# TODO: define some open dataset class, like ImageNet, COCO
