from __future__ import annotations

import sys
import json
import shutil
import struct
import typing as t
from abc import ABCMeta, abstractmethod, abstractproperty
from copy import deepcopy
from types import TracebackType
from pathlib import Path
from binascii import crc32

import jsonlines
from loguru import logger

from starwhale.utils import console, validate_obj_name
from starwhale.consts import (
    VERSION_PREFIX_CNT,
    STANDALONE_INSTANCE,
    SWDS_DATA_FNAME_FMT,
    DUMPED_SWDS_META_FNAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir
from starwhale.base.type import (
    URIType,
    InstanceType,
    DataFormatType,
    DataOriginType,
    ObjectStoreType,
)
from starwhale.utils.error import (
    NotFoundError,
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.dataset import (
    DatasetSummary,
    D_ALIGNMENT_SIZE,
    D_FILE_VOLUME_SIZE,
)

# TODO: tune header size
_header_magic = struct.unpack(">I", b"SWDS")[0]
_data_magic = struct.unpack(">I", b"SDWS")[0]
_header_struct = struct.Struct(">IIQIIII")
_header_size = _header_struct.size
_header_version = 0


class TabularDatasetRow:
    def __init__(
        self,
        id: int,
        data_uri: str,
        label: t.Union[str, bytes],
        data_format: DataFormatType = DataFormatType.SWDS_BIN,
        object_store_type: ObjectStoreType = ObjectStoreType.LOCAL,
        data_offset: int = 0,
        data_size: int = 0,
        data_origin: DataOriginType = DataOriginType.NEW,
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

        if self.data_format not in DataFormatType:
            raise NoSupportError(f"data format: {self.data_format}")

        if self.data_origin not in DataOriginType:
            raise NoSupportError(f"data origin: {self.data_origin}")

        # TODO: support non-starwhale remote object store, for index-only feature
        if self.object_store_type != ObjectStoreType.LOCAL:
            raise NoSupportError(f"object store {self.object_store_type}")

    def __str__(self) -> str:
        return f"row-{self.id}, data-{self.data_uri}, origin-[{self.data_origin}]"

    def __repr__(self) -> str:
        return f"row-{self.id}, data-{self.data_uri}(offset:{self.data_offset}, size:{self.data_size}, format:{self.data_format}), origin-[{self.data_origin}], object store-{self.object_store_type}"

    def asdict(self) -> t.Dict[str, t.Union[str, bytes, int]]:
        d = deepcopy(self.__dict__)
        d["data_format"] = self.data_format.value
        d["data_origin"] = self.data_origin.value
        d["object_store_type"] = self.object_store_type.value
        return d


_TDType = t.TypeVar("_TDType", bound="TabularDataset")


class TabularDataset:
    def __init__(
        self,
        name: str,
        version: str,
        project: str,
        start: int = 0,
        end: int = sys.maxsize,
    ) -> None:
        self.name = name
        self.version = version
        self.table_name = f"{name}/{version[:VERSION_PREFIX_CNT]}/{version}"
        self._ds_wrapper = DatastoreWrapperDataset(self.table_name, project)

        self.start = start
        self.end = end

        self._do_validate()

    def _do_validate(self) -> None:
        _ok, _reason = validate_obj_name(self.name)
        if not _ok:
            raise InvalidObjectName(f"{self.name}: {_reason}")

        if not self.version:
            raise FieldTypeOrValueError("no version field")

    def __str__(self) -> str:
        return f"Dataset Table: {self._ds_wrapper}"

    __repr__ = __str__

    def put(self, row: TabularDatasetRow) -> None:
        self._ds_wrapper.put(row.id, **row.asdict())

    def scan(
        self, start: int = 0, end: int = sys.maxsize
    ) -> t.Generator[TabularDatasetRow, None, None]:
        _start = start + self.start
        _end = min(end + self.start, self.end)

        _map_types = {
            "data_format": DataFormatType,
            "data_origin": DataOriginType,
            "object_store_type": ObjectStoreType,
        }
        for _d in self._ds_wrapper.scan(_start, _end):
            for k, v in _map_types.items():
                if k not in _d:
                    continue
                _d[k] = v(_d[k])
            yield TabularDatasetRow(**_d)

    def close(self) -> None:
        self._ds_wrapper.close()

    def __enter__(self: _TDType) -> _TDType:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:
            logger.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    @classmethod
    def from_uri(
        cls: t.Type[_TDType],
        uri: URI,
        start: int = 0,
        end: int = sys.maxsize,
    ) -> _TDType:
        _version = uri.object.version
        if uri.instance_type == InstanceType.STANDALONE:
            _store = DatasetStorage(uri)
            _version = _store.id

        return cls(uri.object.name, _version, uri.project, start=start, end=end)


class StandaloneTabularDataset(TabularDataset):
    class _BytesEncoder(json.JSONEncoder):
        def default(self, obj: t.Any) -> t.Any:
            if isinstance(obj, bytes):
                return obj.decode()
            return json.JSONEncoder.default(self, obj)

    def __init__(
        self,
        name: str,
        version: str,
        project: str,
        start: int = 0,
        end: int = sys.maxsize,
    ) -> None:
        super().__init__(name, version, project, start, end)

        self.uri = URI.capsulate_uri(
            instance=STANDALONE_INSTANCE,
            project=project,
            obj_type=URIType.DATASET,
            obj_name=name,
            obj_ver=version,
        )
        self.store = DatasetStorage(self.uri)

    def dump_meta(self, force: bool = False) -> Path:
        fpath = self.store.snapshot_workdir / DUMPED_SWDS_META_FNAME

        if fpath.exists() and not force:
            console.print(f":blossom: {fpath} existed, skip dump meta")
            return fpath

        console.print(":bear_face: dump dataset meta from data store")

        with jsonlines.open(
            str(fpath), mode="w", dumps=self._BytesEncoder(separators=(",", ":")).encode
        ) as writer:
            for row in self.scan():
                writer.write(row.asdict())

        return fpath

    def load_meta(self) -> None:
        fpath = self.store.snapshot_workdir / DUMPED_SWDS_META_FNAME

        if not fpath.exists():
            raise NotFoundError(fpath)

        console.print(":bird: load dataset meta to standalone data store")
        with jsonlines.open(
            str(fpath),
            mode="r",
        ) as reader:
            for line in reader:
                row = TabularDatasetRow(**line)
                self.put(row)


_BDType = t.TypeVar("_BDType", bound="BaseBuildExecutor")


class BaseBuildExecutor(metaclass=ABCMeta):
    def __init__(
        self,
        dataset_name: str,
        dataset_version: str,
        project_name: str,
        data_dir: Path = Path("."),
        output_dir: Path = Path("./sw_output"),
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

    @abstractproperty
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
        dwriter = (self.output_dir / self._DATA_FMT.format(index=fno)).open("wb")
        object_store_type = ObjectStoreType.LOCAL
        rows, increased_rows = 0, 0
        total_label_size, total_data_size = 0, 0

        for idx, ((_, data), (_, label)) in enumerate(
            zip(self.iter_all_dataset_slice(), self.iter_all_label_slice())
        ):
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
                dwriter = (self.output_dir / self._DATA_FMT.format(index=fno)).open(
                    "wb"
                )

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
            data_format_type=self.data_format_type,
            object_store_type=object_store_type,
            label_byte_size=total_label_size,
            data_byte_size=total_data_size,
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
        object_store_type = ObjectStoreType.LOCAL
        ds_copy_candidates = {}

        for idx, ((data_path, data), (_, label)) in enumerate(
            zip(self.iter_all_dataset_slice(), self.iter_all_label_slice())
        ):
            data_origin = DataOriginType.NEW
            data_offset, data_size = data

            relative_path = str(Path(data_path).relative_to(self.data_dir))
            ds_copy_candidates[relative_path] = data_path

            self.tabular_dataset.put(
                TabularDatasetRow(
                    id=idx,
                    data_uri=str(relative_path),
                    label=label,
                    data_format=self.data_format_type,
                    object_store_type=object_store_type,
                    data_offset=data_offset,
                    data_size=data_size,
                    data_origin=data_origin,
                )
            )

            total_data_size += data_size
            total_label_size += len(label)

            rows += 1
            if data_origin == DataOriginType.NEW:
                increased_rows += 1

        for fname, src in ds_copy_candidates.items():
            dest = self.output_dir / fname
            ensure_dir(dest.parent)
            shutil.copyfile(str(src.absolute()), str(dest.absolute()))

        summary = DatasetSummary(
            rows=rows,
            increased_rows=increased_rows,
            data_format_type=self.data_format_type,
            object_store_type=object_store_type,
            label_byte_size=total_label_size,
            data_byte_size=total_data_size,
        )
        return summary

    def iter_data_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        yield 0, Path(path).stat().st_size

    def iter_label_slice(self, path: str) -> t.Generator[t.Any, None, None]:
        yield Path(path).name

    @property
    def data_format_type(self) -> DataFormatType:
        return DataFormatType.USER_RAW


class MNISTBuildExecutor(SWDSBinBuildExecutor):
    def iter_data_slice(self, path: str) -> t.Generator[bytes, None, None]:
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number, height, width = struct.unpack(">IIII", f.read(16))
            print(f">data({fpath.name}) split {number} group")

            while True:
                content = f.read(height * width)
                if not content:
                    break
                yield content

    def iter_label_slice(self, path: str) -> t.Generator[bytes, None, None]:
        fpath = Path(path)

        with fpath.open("rb") as f:
            _, number = struct.unpack(">II", f.read(8))
            print(f">label({fpath.name}) split {number} group")

            while True:
                content = f.read(1)
                if not content:
                    break
                yield content


# TODO: define some open dataset class, like ImageNet, COCO
