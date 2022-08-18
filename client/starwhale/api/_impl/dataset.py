from __future__ import annotations

import os
import sys
import json
import shutil
import struct
import typing as t
from abc import ABCMeta, abstractmethod
from copy import deepcopy
from enum import Enum, unique
from types import TracebackType
from pathlib import Path
from binascii import crc32
from functools import partial

import jsonlines
from loguru import logger

from starwhale.utils import console, validate_obj_name
from starwhale.consts import (
    AUTH_ENV_FNAME,
    VERSION_PREFIX_CNT,
    STANDALONE_INSTANCE,
    SWDS_DATA_FNAME_FMT,
    DUMPED_SWDS_META_FNAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import ensure_dir, FilePosition
from starwhale.base.type import (
    URIType,
    InstanceType,
    DataFormatType,
    DataOriginType,
    ObjectStoreType,
)
from starwhale.utils.error import (
    FormatError,
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


@unique
class LinkType(Enum):
    FUSE = "fuse"
    S3 = "s3"
    UNDEFINED = "undefined"
    # TODO: support hdfs, http, ssh link type


@unique
class MIMEType(Enum):
    PNG = "image/png"
    JPEG = "image/jpeg"
    WEBP = "image/webp"
    SVG = "image/svg+xml"
    GIF = "image/gif"
    APNG = "image/apng"
    AVIF = "image/avif"
    MP4 = "video/mp4"
    AVI = "video/avi"
    WAV = "audio/wav"
    MP3 = "audio/mp3"
    PLAIN = "text/plain"
    CSV = "text/csv"
    HTML = "text/html"
    GRAYSCALE = "x/grayscale"
    UNDEFINED = "x/undefined"

    @classmethod
    def create_by_file_suffix(cls, name: str) -> MIMEType:
        # ref: https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types
        _map = {
            ".png": cls.PNG,
            ".jpeg": cls.JPEG,
            ".jpg": cls.JPEG,
            ".jfif": cls.JPEG,
            ".pjpeg": cls.JPEG,
            ".pjp": cls.JPEG,
            ".webp": cls.WEBP,
            ".svg": cls.SVG,
            ".gif": cls.GIF,
            ".apng": cls.APNG,
            ".htm": cls.HTML,
            ".html": cls.HTML,
            ".mp3": cls.MP3,
            ".mp4": cls.MP4,
            ".avif": cls.AVIF,
            ".avi": cls.AVI,
            ".wav": cls.WAV,
            ".csv": cls.CSV,
            ".txt": cls.PLAIN,
        }
        return _map.get(Path(name).suffix, MIMEType.UNDEFINED)


_LAType = t.TypeVar("_LAType", bound="LinkAuth")


class LinkAuth(metaclass=ABCMeta):
    def __init__(self, name: str = "", ltype: LinkType = LinkType.UNDEFINED) -> None:
        self.name = name.strip()
        self.ltype = ltype
        self._do_validate()

    def _do_validate(self) -> None:
        if self.ltype not in LinkType:
            raise NoSupportError(f"Link Type: {self.ltype}")

    @abstractmethod
    def dump_env(self) -> t.List[str]:
        raise NotImplementedError

    @classmethod
    def from_env(cls: t.Type[_LAType], name: str = "") -> _LAType:
        raise NotImplementedError


class S3LinkAuth(LinkAuth):
    _ENDPOINT_FMT = "USER.S3.{name}ENDPOINT"
    _REGION_FMT = "USER.S3.{name}REGION"
    _SECRET_FMT = "USER.S3.{name}SECRET"
    _ACCESS_KEY_FMT = "USER.S3.{name}ACCESS_KEY"
    _fmt: t.Callable[[str], str] = (
        lambda x: (f"{x}." if x.strip() else x).strip().upper()
    )

    def __init__(
        self,
        name: str = "",
        access_key: str = "",
        secret: str = "",
        endpoint: str = "",
        region: str = "",
    ) -> None:
        super().__init__(name, LinkType.S3)
        self.access_key = access_key
        self.secret = secret
        self.endpoint = endpoint
        self.region = region

    def dump_env(self) -> t.List[str]:
        _name = S3LinkAuth._fmt(self.name)
        _map = {
            self._SECRET_FMT: self.secret,
            self._REGION_FMT: self.region,
            self._ACCESS_KEY_FMT: self.access_key,
            self._ENDPOINT_FMT: self.endpoint,
        }
        return [f"{k.format(name=_name)}={v}" for k, v in _map.items()]

    @classmethod
    def from_env(cls, name: str = "") -> S3LinkAuth:
        _env = os.environ.get

        _name = cls._fmt(name)
        _secret_name = cls._SECRET_FMT.format(name=_name)
        _access_name = cls._ACCESS_KEY_FMT.format(name=_name)

        _secret = _env(_secret_name)
        _access = _env(_access_name)
        if not _secret or not _access:
            raise FieldTypeOrValueError(
                f"cannot find secret[{_secret_name}] or access[{_access_name}] key env"
            )

        return cls(
            name,
            _access,
            _secret,
            endpoint=_env(cls._ENDPOINT_FMT.format(name=_name), ""),
            region=_env(cls._REGION_FMT.format(name=_name), ""),
        )


FuseLinkAuth = partial(LinkAuth, ltype=LinkType.FUSE)
DefaultS3LinkAuth = S3LinkAuth()


class Link:
    def __init__(
        self,
        uri: str,
        auth: t.Optional[LinkAuth] = DefaultS3LinkAuth,
        offset: int = FilePosition.START,
        size: int = -1,
        mime_type: MIMEType = MIMEType.UNDEFINED,
    ) -> None:
        self.uri = uri.strip()
        self.offset = offset
        self.size = size
        self.auth = auth

        if mime_type == MIMEType.UNDEFINED or mime_type not in MIMEType:
            self.mime_type = MIMEType.create_by_file_suffix(self.uri)
        else:
            self.mime_type = mime_type

        self.do_validate()

    def do_validate(self) -> None:
        if self.offset < 0:
            raise FieldTypeOrValueError(f"offset({self.offset}) must be non-negative")

        if self.size < -1:
            raise FieldTypeOrValueError(f"size({self.size}) must be non-negative or -1")

    def __str__(self) -> str:
        return f"Link {self.uri}"

    def __repr__(self) -> str:
        return f"Link uri:{self.uri}, offset:{self.offset}, size:{self.size}, mime type:{self.mime_type}"


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
        data_mime_type: MIMEType = MIMEType.UNDEFINED,
        auth_name: str = "",
        **kw: t.Any,
    ) -> None:
        self.id = id
        self.data_uri = data_uri.strip()
        self.data_format = data_format
        self.data_offset = data_offset
        self.data_size = data_size
        self.data_origin = data_origin
        self.object_store_type = object_store_type
        self.data_mime_type = data_mime_type
        self.label = label.encode() if isinstance(label, str) else label
        self.auth_name = auth_name

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
        return (
            f"row-{self.id}, data-{self.data_uri}(offset:{self.data_offset}, size:{self.data_size},"
            f"format:{self.data_format}, mime type:{self.data_mime_type}), "
            f"origin-[{self.data_origin}], object store-{self.object_store_type}"
        )

    def asdict(self) -> t.Dict[str, t.Union[str, bytes, int]]:
        d = deepcopy(self.__dict__)
        for k, v in d.items():
            if isinstance(v, Enum):
                d[k] = v.value
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
        logger.debug(f"dataset table name:{self.table_name}")
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
