from __future__ import annotations

import io
import os
import sys
import typing as t
from abc import ABCMeta, abstractmethod
from pathlib import Path
from urllib.parse import urlparse

import boto3
import loguru
from loguru import logger as _logger
from botocore.client import Config as S3Config
from typing_extensions import Protocol

from starwhale.utils import load_dotenv
from starwhale.consts import AUTH_ENV_FNAME, SWDSBackendType
from starwhale.base.uri import URI
from starwhale.utils.fs import FilePosition
from starwhale.base.type import URIType, InstanceType, DataFormatType, ObjectStoreType
from starwhale.utils.error import FormatError, NoSupportError, FieldTypeOrValueError
from starwhale.core.dataset.store import DatasetStorage

from .dataset import S3LinkAuth, TabularDataset, TabularDatasetRow

# TODO: config chunk size
_CHUNK_SIZE = 8 * 1024 * 1024  # 8MB
_DEFAULT_S3_REGION = "local"
_DEFAULT_S3_ENDPOINT = "localhost:9000"
_DEFAULT_S3_BUCKET = "starwhale"


class FileLikeObj(Protocol):
    def readline(self) -> bytes:
        ...

    def read(self, size: int) -> t.Union[bytes, memoryview]:
        ...


class S3Uri(t.NamedTuple):
    bucket: str
    key: str
    protocol: str = "s3"
    endpoint: str = _DEFAULT_S3_ENDPOINT


class ObjectStoreS3Connection:
    def __init__(
        self,
        endpoint: str,
        access_key: str,
        secret_key: str,
        region: str = "",
        bucket: str = "",
        connect_timeout: float = 10.0,
        read_timeout: float = 50.0,
        total_max_attempts: int = 6,
    ) -> None:
        self.endpoint = endpoint.strip()
        if self.endpoint and not self.endpoint.startswith(("http://", "https://")):
            self.endpoint = f"http://{self.endpoint}"

        self.access_key = access_key
        self.secret_key = secret_key
        self.region = region
        self.bucket = bucket
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
    def from_uri(cls, uri: str, auth_name: str) -> ObjectStoreS3Connection:
        """make S3 Connection by uri

        uri:
            - s3://username:password@127.0.0.1:8000@bucket/key
            - s3://127.0.0.1:8000@bucket/key
            - s3://bucket/key
        """
        uri = uri.strip()
        if not uri or not uri.startswith("s3://"):
            raise NoSupportError(
                f"s3 connection only support s3:// prefix, the actual uri is {uri}"
            )

        r = urlparse(uri)
        netloc = r.netloc

        link_auth = S3LinkAuth.from_env(auth_name)
        access = link_auth.access_key
        secret = link_auth.secret
        region = link_auth.region

        _nl = netloc.split("@")
        if len(_nl) == 1:
            endpoint = link_auth.endpoint
            bucket = _nl[0]
        elif len(_nl) == 2:
            endpoint, bucket = _nl
        elif len(_nl) == 3:
            _key, endpoint, bucket = _nl
            access, secret = _key.split(":", 1)
        else:
            raise FormatError(netloc)

        if not endpoint:
            raise FieldTypeOrValueError("endpoint is empty")

        if not access or not secret:
            raise FieldTypeOrValueError("no access_key or secret_key")

        if not bucket:
            raise FieldTypeOrValueError("bucket is empty")

        return cls(
            endpoint=endpoint,
            access_key=access,
            secret_key=secret,
            region=region or _DEFAULT_S3_REGION,
            bucket=bucket,
        )

    @classmethod
    def from_env(cls) -> ObjectStoreS3Connection:
        # TODO: support multi s3 backend servers
        _env = os.environ.get
        return ObjectStoreS3Connection(
            endpoint=_env("SW_S3_ENDPOINT", _DEFAULT_S3_ENDPOINT),
            access_key=_env("SW_S3_ACCESS_KEY", ""),
            secret_key=_env("SW_S3_SECRET", ""),
            region=_env("SW_S3_REGION", _DEFAULT_S3_REGION),
            bucket=_env("SW_S3_BUCKET", _DEFAULT_S3_BUCKET),
        )


class DatasetObjectStore:
    def __init__(
        self,
        backend: str,
        bucket: str,
        key_prefix: str = "",
        **kw: t.Any,
    ) -> None:
        self.bucket = bucket
        self.conn: t.Optional[ObjectStoreS3Connection]

        self.backend: StorageBackend
        if backend == SWDSBackendType.S3:
            conn = kw.get("conn") or ObjectStoreS3Connection.from_env()
            self.backend = S3StorageBackend(conn)
        else:
            self.backend = FuseStorageBackend()

        self.key_prefix = key_prefix or os.environ.get("SW_OBJECT_STORE_KEY_PREFIX", "")

    @classmethod
    def from_data_link_uri(cls, data_uri: str, auth_name: str) -> DatasetObjectStore:
        data_uri = data_uri.strip()
        if not data_uri:
            raise FieldTypeOrValueError("data_uri is empty")

        # TODO: support other uri type
        if data_uri.startswith("s3://"):
            backend = SWDSBackendType.S3
            conn = ObjectStoreS3Connection.from_uri(data_uri, auth_name)
            bucket = conn.bucket
        else:
            backend = SWDSBackendType.FUSE
            bucket = ""
            conn = None

        return cls(backend=backend, bucket=bucket, conn=conn)

    @classmethod
    def from_dataset_uri(cls, dataset_uri: URI) -> DatasetObjectStore:
        if dataset_uri.object.typ != URIType.DATASET:
            raise NoSupportError(f"{dataset_uri} is not dataset uri")

        _type = dataset_uri.instance_type
        if _type == InstanceType.STANDALONE:
            backend = SWDSBackendType.FUSE
            bucket = str(DatasetStorage(dataset_uri).data_dir.absolute())
        else:
            backend = SWDSBackendType.S3
            bucket = os.environ.get("SW_S3_BUCKET", _DEFAULT_S3_BUCKET)

        return cls(backend=backend, bucket=bucket)


class DataField(t.NamedTuple):
    idx: int
    data_size: int
    data: bytes
    ext_attr: t.Dict[str, t.Any]


class ResultLoader:
    def __init__(
        self,
        data: t.List[t.Any],
        deserializer: t.Optional[t.Callable] = None,
    ) -> None:
        self.data = data
        self.deserializer = deserializer

    def __iter__(self) -> t.Any:
        for _data in self.data:
            if self.deserializer:
                yield self.deserializer(_data)
                continue
            yield _data


class DataLoader(metaclass=ABCMeta):
    def __init__(
        self,
        dataset_uri: URI,
        start: int = 0,
        end: int = sys.maxsize,
        logger: t.Union[loguru.Logger, None] = None,
    ):
        self.dataset_uri = dataset_uri
        self.start = start
        self.end = end
        self.logger = logger or _logger
        # TODO: refactor TabularDataset with dataset_uri
        # TODO: refactor dataset, tabular_dataset and standalone dataset module
        self.tabular_dataset = TabularDataset.from_uri(
            dataset_uri, start=start, end=end
        )
        self._stores: t.Dict[str, DatasetObjectStore] = {}

        self._load_dataset_auth_env()

    def _load_dataset_auth_env(self) -> None:
        # TODO: support multi datasets
        if self.dataset_uri.instance_type == InstanceType.STANDALONE:
            auth_env_fpath = (
                DatasetStorage(self.dataset_uri).snapshot_workdir / AUTH_ENV_FNAME
            )
            load_dotenv(auth_env_fpath)

    def _get_store(self, row: TabularDatasetRow) -> DatasetObjectStore:
        _k = f"{row.object_store_type.value}.{row.auth_name}"
        _store = self._stores.get(_k)
        if _store:
            return _store

        if row.object_store_type == ObjectStoreType.REMOTE:
            _store = DatasetObjectStore.from_data_link_uri(row.data_uri, row.auth_name)
        else:
            _store = DatasetObjectStore.from_dataset_uri(self.dataset_uri)

        self._stores[_k] = _store
        return _store

    def _get_key_compose(
        self, row: TabularDatasetRow, store: DatasetObjectStore
    ) -> str:
        if row.object_store_type == ObjectStoreType.REMOTE:
            data_uri = urlparse(row.data_uri).path
        else:
            data_uri = row.data_uri
            if store.key_prefix:
                data_uri = os.path.join(store.key_prefix, data_uri.lstrip("/"))

        _key_compose = (
            f"{data_uri}:{row.data_offset}:{row.data_offset + row.data_size - 1}"
        )
        return _key_compose

    def __iter__(self) -> t.Generator[t.Tuple[DataField, DataField], None, None]:
        _attr = {
            "ds_name": self.tabular_dataset.name,
            "ds_version": self.tabular_dataset.version,
        }
        for row in self.tabular_dataset.scan():
            # TODO: tune performance by fetch in batch
            # TODO: remove ext_attr field
            _store = self._get_store(row)
            _key_compose = self._get_key_compose(row, _store)

            self.logger.info(f"@{_store.bucket}/{_key_compose}")
            _file = _store.backend._make_file(_store.bucket, _key_compose)
            for data_content, data_size in self._do_iter(_file, row):
                label = DataField(
                    idx=row.id,
                    data_size=len(row.label),
                    data=row.label,
                    ext_attr=_attr,
                )
                data = DataField(
                    idx=row.id, data_size=data_size, data=data_content, ext_attr=_attr
                )

                yield data, label

    @abstractmethod
    def _do_iter(
        self, file: FileLikeObj, row: TabularDatasetRow
    ) -> t.Generator[t.Tuple[bytes, int], None, None]:
        raise NotImplementedError

    def __str__(self) -> str:
        return f"[{self.kind.name}]DataLoader for {self.dataset_uri}"

    def __repr__(self) -> str:
        return f"[{self.kind.name}]DataLoader for {self.dataset_uri}, start:{self.start}, end:{self.end}"

    @property
    def kind(self) -> DataFormatType:
        raise NotImplementedError


class UserRawDataLoader(DataLoader):
    @property
    def kind(self) -> DataFormatType:
        return DataFormatType.USER_RAW

    def _do_iter(
        self,
        file: FileLikeObj,
        row: TabularDatasetRow,
    ) -> t.Generator[t.Tuple[bytes, int], None, None]:
        data: bytes = file.read(row.data_size)
        yield data, row.data_size


class SWDSBinDataLoader(DataLoader):
    @property
    def kind(self) -> DataFormatType:
        return DataFormatType.SWDS_BIN

    def _do_iter(
        self, file: FileLikeObj, row: TabularDatasetRow
    ) -> t.Generator[t.Tuple[bytes, int], None, None]:
        from .dataset import _header_size, _header_struct

        size: int
        padding_size: int
        header: bytes = file.read(_header_size)
        _, _, _, size, padding_size, _, _ = _header_struct.unpack(header)
        data: bytes = file.read(size + padding_size)
        data = data[:size].tobytes() if isinstance(data, memoryview) else data[:size]
        yield data, size


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
            return _r[0], 0, FilePosition.END
        elif len(_r) == 2:
            return _r[0], int(_r[1]), FilePosition.END
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
        end = end if self.end == FilePosition.END else min(self.end, end)

        data, length = self._do_fetch_data(self._current_s3_start, end)
        self._current_s3_start += length

        return data, length

    def _do_fetch_data(self, _start: int, _end: int) -> t.Tuple[bytes, int]:
        # TODO: add more exception handle
        if self._s3_eof or (_end != FilePosition.END and _end < _start):
            return b"", 0

        resp = self.obj.get(Range=f"bytes={_start}-{_end}")
        body = resp["Body"]
        length = resp["ContentLength"]
        out = resp["Body"].read()
        body.close()

        self._s3_eof = _end == FilePosition.END or (_end - _start + 1) > length
        return out, length


def get_data_loader(
    dataset_uri: URI,
    start: int = 0,
    end: int = sys.maxsize,
    logger: t.Union[loguru.Logger, None] = None,
) -> DataLoader:
    from starwhale.core.dataset import model

    summary = model.Dataset.get_dataset(dataset_uri).summary()
    include_user_raw = summary.include_user_raw
    _cls = UserRawDataLoader if include_user_raw else SWDSBinDataLoader
    return _cls(dataset_uri, start, end, logger or _logger)
