from __future__ import annotations

import io
import os
import sys
import typing as t
from abc import ABCMeta, abstractmethod
from pathlib import Path

import boto3
import loguru
from loguru import logger as _logger
from botocore.client import Config as S3Config
from typing_extensions import Protocol

from starwhale.consts import DataLoaderKind, SWDSBackendType
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.core.dataset.store import DatasetStorage

from .dataset import TabularDataset

# TODO: config chunk size
_CHUNK_SIZE = 8 * 1024 * 1024  # 8MB
_FILE_END_POS = -1


class ObjectStoreS3Connection:
    def __init__(
        self,
        endpoint: str,
        access_key: str,
        secret_key: str,
        region: str = "",
        connect_timeout: float = 10.0,
        read_timeout: float = 50.0,
        total_max_attempts: int = 6,
    ) -> None:
        self.endpoint = endpoint
        self.access_key = access_key
        self.secret_key = secret_key
        self.region = region
        self.connect_timeout = float(
            os.environ.get("SW_S3_CONNECT_TIMEOUT", connect_timeout)
        )
        self.read_timeout = float(os.environ.get("SW_S3_READ_TIMEOUT", read_timeout))
        self.total_max_attempts = int(
            os.environ.get("SW_S3_TOTAL_MAX_ATTEMPTS", total_max_attempts)
        )

    def __str__(self) -> str:
        return f"endpoint[{self.endpoint}]-region[{self.region}]"

    __repr__ = __str__

    @classmethod
    def create_from_env(cls) -> ObjectStoreS3Connection:
        # TODO: support multi s3 backend servers
        return ObjectStoreS3Connection(
            endpoint=os.environ.get("SW_S3_ENDPOINT", "127.0.0.1:9000"),
            access_key=os.environ.get("SW_S3_ACCESS_KEY", "foo"),
            secret_key=os.environ.get("SW_S3_SECRET", "bar"),
            region=os.environ.get("SW_S3_REGION", "local"),
        )


class DatasetObjectStore:
    def __init__(
        self,
        uri: URI,
        backend: str = "",
        conn: t.Optional[ObjectStoreS3Connection] = None,
        bucket: str = "",
        key_prefix: str = "",
    ) -> None:
        self.uri = uri
        _backend = backend or self._get_default_backend()

        self.conn: t.Optional[ObjectStoreS3Connection]
        self.backend: StorageBackend

        _env_bucket = os.environ.get("SW_S3_BUCKET", "")

        if _backend == SWDSBackendType.S3:
            self.conn = conn or ObjectStoreS3Connection.create_from_env()
            self.bucket = bucket or _env_bucket
            self.backend = S3StorageBackend(self.conn)
        else:
            self.conn = None
            self.bucket = bucket or _env_bucket or self._get_bucket_by_uri()
            self.backend = FuseStorageBackend()

        self.key_prefix = key_prefix or os.environ.get("SW_OBJECT_STORE_KEY_PREFIX", "")

        self._do_validate()

    def _do_validate(self) -> None:
        if self.uri.object.typ != URIType.DATASET:
            raise NoSupportError(f"{self.uri} is not dataset uri")

        if not self.bucket:
            raise FieldTypeOrValueError("no bucket field")

    def _get_default_backend(self) -> str:
        _type = self.uri.instance_type

        if _type == InstanceType.STANDALONE:
            return SWDSBackendType.FUSE
        elif _type == InstanceType.CLOUD:
            return SWDSBackendType.S3
        else:
            raise NoSupportError(
                f"get object store backend by the instance type({_type})"
            )

    def _get_bucket_by_uri(self) -> str:
        if self.uri.instance_type == InstanceType.CLOUD:
            raise NoSupportError(f"{self.uri} to fetch bucket")

        return str(DatasetStorage(self.uri).data_dir.absolute())


class DataField(t.NamedTuple):
    idx: int
    data_size: int
    data: bytes
    ext_attr: t.Dict[str, t.Any]


class DataLoader(metaclass=ABCMeta):
    def __init__(
        self,
        storage: DatasetObjectStore,
        dataset: TabularDataset,
        logger: t.Optional[loguru.Logger] = None,
        kind: str = DataLoaderKind.SWDS,
        deserializer: t.Optional[t.Callable] = None,
    ):
        self.storage = storage
        self.dataset = dataset
        self.logger = logger or _logger
        self.kind = kind
        self.deserializer = deserializer

        self._do_validate()

    def _do_validate(self) -> None:
        if self.kind != DataLoaderKind.SWDS:
            raise Exception(f"{self.kind} no support")

    def __iter__(self) -> t.Any:
        raise NotImplementedError

    def __str__(self) -> str:
        return f"DataLoader for {self.storage.backend}"

    def __repr__(self) -> str:
        return f"DataLoader for {self.storage.backend}, extra:{self.storage.conn}"


class ResultLoader:
    def __init__(
        self,
        datas: t.List[t.Any],
        deserializer: t.Optional[t.Callable] = None,
    ) -> None:
        self.datas = datas
        self.deserializer = deserializer

    def __iter__(self) -> t.Any:
        for _data in self.datas:
            if self.deserializer:
                yield self.deserializer(_data)
                continue
            yield _data


class SWDSDataLoader(DataLoader):
    def __init__(
        self,
        storage: DatasetObjectStore,
        dataset: TabularDataset,
        logger: t.Union[loguru.Logger, None] = None,
    ) -> None:
        super().__init__(storage, dataset, logger, DataLoaderKind.SWDS)

    def __iter__(self) -> t.Generator[t.Tuple[DataField, DataField], None, None]:
        _attr = {"ds_name": self.dataset.name, "ds_version": self.dataset.version}
        for row in self.dataset.scan():
            # TODO: tune performance by fetch in batch
            # TODO: remove ext_attr field

            _data_uri = row.data_uri
            if self.storage.key_prefix:
                _data_uri = os.path.join(self.storage.key_prefix, _data_uri.lstrip("/"))

            _key_compose = (
                f"{_data_uri}:{row.data_offset}:{row.data_offset + row.data_size - 1}"
            )
            for data in self._do_iter(_key_compose, _attr):
                label = DataField(
                    idx=row.id,
                    data_size=len(row.label),
                    data=row.label,
                    ext_attr=_attr,
                )
                yield data, label

    def _do_iter(
        self, key_compose: str, attr: t.Dict[str, str]
    ) -> t.Iterator[DataField]:
        from .dataset import _header_size, _header_struct

        self.logger.info(f"@{self.storage.bucket}/{key_compose}")
        _file = self.storage.backend._make_file(self.storage.bucket, key_compose)
        while True:
            header: bytes = _file.read(_header_size)
            if not header:
                break
            _, _, idx, size, padding_size, _, _ = _header_struct.unpack(header)
            data = _file.read(size + padding_size)
            yield DataField(
                idx,
                size,
                data[:size].tobytes() if isinstance(data, memoryview) else data[:size],
                attr,
            )


class StorageBackend(metaclass=ABCMeta):
    def __init__(
        self,
        kind: str,
    ) -> None:
        self.kind = kind

    def __str__(self) -> str:
        return f"StorageBackend for {self.kind}"

    __repr__ = __str__

    def _parse_key(self, key: str) -> t.Tuple[str, int, int]:
        # TODO: some builtin method to
        # TODO: add start end normalize
        _r = key.split(":")
        if len(_r) == 1:
            return _r[0], 0, _FILE_END_POS
        elif len(_r) == 2:
            return _r[0], int(_r[1]), _FILE_END_POS
        else:
            return _r[0], int(_r[1]), int(_r[2])

    @abstractmethod
    def _make_file(self, bucket: str, key_compose: str) -> FileLikeObj:
        raise NotImplementedError


class S3StorageBackend(StorageBackend):
    def __init__(
        self,
        conn: ObjectStoreS3Connection,
    ):
        super().__init__(kind=SWDSBackendType.S3)

        self.s3 = boto3.resource(
            "s3",
            endpoint_url=conn.endpoint,
            aws_access_key_id=conn.access_key,
            aws_secret_access_key=conn.secret_key,
            config=S3Config(
                connect_timeout=conn.connect_timeout,
                read_timeout=conn.read_timeout,
                signature_version="s3v4",
                retries={
                    "total_max_attempts": conn.total_max_attempts,
                    "mode": "standard",
                },
            ),
            region_name=conn.region,
        )

    def _make_file(self, bucket: str, key_compose: str) -> FileLikeObj:
        # TODO: merge connections for s3
        _key, _start, _end = self._parse_key(key_compose)
        return S3BufferedFileLike(
            s3=self.s3,
            bucket=bucket,
            key=_key,
            start=_start,
            end=_end,
        )


class FuseStorageBackend(StorageBackend):
    def __init__(self) -> None:
        super().__init__(kind=SWDSBackendType.FUSE)

    def _make_file(self, bucket: str, key_compose: str) -> FileLikeObj:
        _key, _start, _end = self._parse_key(key_compose)
        bucket_path = (
            Path(bucket).expanduser() if bucket.startswith("~/") else Path(bucket)
        )
        # TODO: tune reopen file performance, merge files
        with (bucket_path / _key).open("rb") as f:
            f.seek(_start)
            return io.BytesIO(f.read(_end - _start + 1))


class FileLikeObj(Protocol):
    def readline(self) -> bytes:
        ...

    def read(self, size: int) -> t.Union[bytes, memoryview]:
        ...


# TODO: add mock test
class S3BufferedFileLike:
    # TODO: add s3 typing
    def __init__(self, s3: t.Any, bucket: str, key: str, start: int, end: int) -> None:
        self.key = key
        self.obj = s3.Object(bucket, key)
        self.start = start
        self.end = end

        self._buffer = memoryview(bytearray(0))
        self._current = 0
        self._s3_eof = False
        self._current_s3_start = start
        self._iter_lines = None

    def tell(self) -> int:
        return self._current

    def readline(self) -> bytes:
        if self._iter_lines is None:
            self._iter_lines = self.obj.get()["Body"].iter_lines(chunk_size=_CHUNK_SIZE)

        try:
            line: bytes = next(self._iter_lines)  # type: ignore
        except StopIteration:
            line = b""
        return line

    def read(self, size: int) -> memoryview:
        # TODO: use smart_open 3rd lib?
        if (self._current + size) <= len(self._buffer):
            end = self._current + size
            out = self._buffer[self._current : end]
            self._current = end
            return out
        else:
            data, _ = self._next_data()
            _release_buffer = self._buffer
            self._buffer = memoryview(self._buffer[self._current :].tobytes() + data)
            _release_buffer.release()
            self._current = 0

            if len(self._buffer) == 0:
                return memoryview(bytearray(0))  # EOF
            elif (self._current + size) > len(self._buffer):
                # TODO: maybe ignore this error?
                raise Exception(
                    f"{self.key} file cannot read {size} data, error format"
                )
            else:
                end = self._current + size
                out = self._buffer[self._current : end]
                self._current = end
                return out

    def close(self) -> None:
        # TODO: cleanup stream and open
        self.obj = None
        try:
            self._buffer.release()
        except Exception as e:
            _logger.warning(f"skip _buffer(memoryview) release exception:{e}")

    def _next_data(self) -> t.Tuple[bytes, int]:
        end = _CHUNK_SIZE + self._current_s3_start - 1
        end = end if self.end == _FILE_END_POS else min(self.end, end)

        data, length = self._do_fetch_data(self._current_s3_start, end)
        self._current_s3_start += length

        return data, length

    def _do_fetch_data(self, _start: int, _end: int) -> t.Tuple[bytes, int]:
        # TODO: add more exception handle
        if self._s3_eof or (_end != _FILE_END_POS and _end < _start):
            return b"", 0

        resp = self.obj.get(Range=f"bytes={_start}-{_end}")
        body = resp["Body"]
        length = resp["ContentLength"]
        out = resp["Body"].read()
        body.close()

        self._s3_eof = _end == _FILE_END_POS or (_end - _start + 1) > length
        return out, length


def get_data_loader(
    dataset_uri: URI,
    start: int = 0,
    end: int = sys.maxsize,
    backend: str = "",
    kind: str = DataLoaderKind.SWDS,
    logger: t.Union[loguru.Logger, None] = None,
) -> DataLoader:
    logger = logger or _logger
    object_store = DatasetObjectStore(dataset_uri, backend)
    dataset = TabularDataset.from_uri(dataset_uri, start=start, end=end)

    # TODO: support user raw data format
    if kind == DataLoaderKind.SWDS:
        return SWDSDataLoader(object_store, dataset, logger)
    else:
        raise NoSupportError(f"{kind} data loader, no support")
