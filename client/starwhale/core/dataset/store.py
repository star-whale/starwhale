from __future__ import annotations

import io
import os
import sys
import json
import shutil
import typing as t
from abc import ABCMeta, abstractmethod
from types import TracebackType
from pathlib import Path
from urllib.parse import urlparse

import boto3
import requests
from botocore.client import Config as S3Config
from typing_extensions import Protocol

from starwhale.consts import (
    HTTPMethod,
    SWDSBackendType,
    VERSION_PREFIX_CNT,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import (
    ensure_dir,
    blake2b_file,
    FilePosition,
    BLAKE2B_SIGNATURE_ALGO,
)
from starwhale.base.type import URIType, BundleType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.base.store import BaseStorage
from starwhale.utils.error import (
    FormatError,
    NotFoundError,
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.utils.retry import http_retry
from starwhale.utils.config import SWCliConfigMixed
from starwhale.core.dataset.type import Link

# TODO: refactor Dataset and ModelPackage LocalStorage
_DEFAULT_S3_REGION = "local"
_DEFAULT_S3_ENDPOINT = "localhost:9000"
_DEFAULT_S3_BUCKET = "starwhale"

# TODO: config chunk size
_CHUNK_SIZE = 8 * 1024 * 1024  # 8MB


class DatasetStorage(BaseStorage):
    object_hash_algo = BLAKE2B_SIGNATURE_ALGO
    short_sign_cnt = 16

    def _guess(self) -> t.Tuple[Path, str]:
        return self._guess_for_bundle()

    @property
    def recover_loc(self) -> Path:
        return self._get_recover_loc_for_bundle()

    @property
    def snapshot_workdir(self) -> Path:
        if self.building:
            return self.tmp_dir
        version = self.uri.object.version
        return (
            self.project_dir
            / URIType.DATASET
            / self.uri.object.name
            / version[:VERSION_PREFIX_CNT]
            / f"{version}{BundleType.DATASET}"
        )

    @property
    def manifest_path(self) -> Path:
        return self.loc / DEFAULT_MANIFEST_NAME

    @property
    def bundle_type(self) -> str:
        return BundleType.DATASET

    @property
    def uri_type(self) -> str:
        return URIType.DATASET

    @property
    def data_dir(self) -> Path:
        return self.snapshot_workdir / "data"

    @property
    def src_dir(self) -> Path:
        return self.snapshot_workdir / "src"

    @property
    def dataset_rootdir(self) -> Path:
        return self.project_dir / URIType.DATASET

    @classmethod
    def save_data_file(
        cls, src: t.Union[Path, str], force: bool = False, remove_src: bool = False
    ) -> t.Tuple[str, Path]:
        src = Path(src)

        if not src.exists():
            raise NotFoundError(f"data origin file: {src}")

        if not src.is_file():
            raise NoSupportError(f"{src} is not file type")

        sign_name = blake2b_file(src)
        dest = cls._get_object_store_path(sign_name)
        if dest.exists() and not force:
            return sign_name, dest

        ensure_dir(dest.parent)

        _src, _dest = str(src.absolute()), str(dest.absolute())
        if remove_src:
            shutil.move(_src, _dest)
        else:
            shutil.copyfile(_src, _dest)
        return sign_name, dest

    @classmethod
    def _get_object_store_path(cls, hash_name: str) -> Path:
        _prefix_cnt = 2
        hash_name = hash_name.strip()
        if len(hash_name) < _prefix_cnt:
            raise InvalidObjectName(f"hash name({hash_name}) is too short")

        return (
            SWCliConfigMixed().object_store_dir
            / cls.object_hash_algo
            / hash_name[:_prefix_cnt]
            / hash_name
        )

    def get_data_file(self, name: str) -> Path:
        path = self._get_object_store_path(name)
        if path.exists():
            return path

        return self.data_dir / name

    def get_all_data_files(self) -> t.List[Path]:
        files = []
        for f in self.data_dir.rglob("*"):
            if not f.is_symlink():
                continue
            files.append(f.resolve().absolute())
        return files


class S3Uri(t.NamedTuple):
    bucket: str
    key: str
    protocol: str = "s3"
    endpoint: str = _DEFAULT_S3_ENDPOINT


_TFLType = t.TypeVar("_TFLType", covariant=True)


class FileLikeObj(Protocol[_TFLType]):
    def __enter__(self) -> _TFLType:
        ...

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        ...

    def read(self, size: int) -> bytes:
        ...

    def close(self) -> None:
        ...


class S3Connection:
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
        # configs like {"addressing_style": "auto"}
        # more info in: botocore.Config._validate_s3_configuration
        # https://botocore.amazonaws.com/v1/documentation/api/latest/reference/config.html
        self.extra_s3_configs = json.loads(os.environ.get("SW_S3_EXTRA_CONFIGS", "{}"))

    def __str__(self) -> str:
        return f"endpoint[{self.endpoint}]-region[{self.region}]"

    __repr__ = __str__

    @classmethod
    def from_uri(cls, uri: str, auth_name: str = "") -> S3Connection:
        """make S3 Connection by uri

        uri:
            - s3://username:password@127.0.0.1:8000/bucket/key
            - s3://127.0.0.1:8000/bucket/key
        """
        from .type import S3LinkAuth

        uri = uri.strip()
        if not uri or not uri.startswith(("s3://", "minio://")):
            raise NoSupportError(
                f"s3 connection only support s3:// prefix, the actual uri is {uri}"
            )

        r = urlparse(uri)

        link_auth = S3LinkAuth.from_env(auth_name)
        access = r.username or link_auth.access_key
        secret = r.password or link_auth.secret
        region = link_auth.region
        endpoint = r.netloc.split("@")[-1] or link_auth.endpoint

        parts = r.path.lstrip("/").split("/", 1)
        if len(parts) != 2 or parts[0] == "" or parts[1] == "":
            raise FieldTypeOrValueError(
                f"{uri} is not a valid s3 uri for bucket and key"
            )
        bucket = parts[0]

        if not endpoint:
            raise FieldTypeOrValueError("endpoint is empty")

        if not access or not secret:
            raise FieldTypeOrValueError("no access_key or secret_key")

        return cls(
            endpoint=endpoint,
            access_key=access,
            secret_key=secret,
            region=region or _DEFAULT_S3_REGION,
            bucket=bucket,
        )

    @classmethod
    def from_env(cls) -> S3Connection:
        # TODO: support multi s3 backend servers
        _env = os.environ.get
        return S3Connection(
            endpoint=_env("SW_S3_ENDPOINT", _DEFAULT_S3_ENDPOINT),
            access_key=_env("SW_S3_ACCESS_KEY", ""),
            secret_key=_env("SW_S3_SECRET", ""),
            region=_env("SW_S3_REGION", _DEFAULT_S3_REGION),
            bucket=_env("SW_S3_BUCKET", _DEFAULT_S3_BUCKET),
        )


class ObjectStore:
    def __init__(
        self,
        backend: str,
        bucket: str = "",
        dataset_uri: URI = URI(""),
        key_prefix: str = "",
        **kw: t.Any,
    ) -> None:
        self.bucket = bucket

        self.backend: StorageBackend
        if backend == SWDSBackendType.S3:
            conn = kw.get("conn") or S3Connection.from_env()
            self.backend = S3StorageBackend(conn)
        elif backend == SWDSBackendType.SignedUrl:
            self.backend = SignedUrlBackend(dataset_uri)
        else:
            self.backend = LocalFSStorageBackend()

        self.key_prefix = key_prefix or os.environ.get("SW_OBJECT_STORE_KEY_PREFIX", "")

    def __str__(self) -> str:
        return f"ObjectStore backend:{self.backend}"

    def __repr__(self) -> str:
        return f"ObjectStored:{self.backend}, bucket:{self.bucket}, key_prefix:{self.key_prefix}"

    @classmethod
    def from_data_link_uri(cls, data_link: Link, auth_name: str) -> ObjectStore:
        if not data_link:
            raise FieldTypeOrValueError("data_link is empty")

        # TODO: support other uri type
        if data_link.uri.startswith("s3://"):
            backend = SWDSBackendType.S3
            conn = S3Connection.from_uri(data_link.uri, auth_name)
            bucket = conn.bucket
        else:
            backend = SWDSBackendType.LocalFS
            bucket = ""
            conn = None

        return cls(backend=backend, bucket=bucket, conn=conn)

    @classmethod
    def from_dataset_uri(cls, dataset_uri: URI) -> ObjectStore:
        if dataset_uri.object.typ != URIType.DATASET:
            raise NoSupportError(f"{dataset_uri} is not dataset uri")
        return cls(
            backend=SWDSBackendType.LocalFS,
            bucket=str(DatasetStorage(dataset_uri).data_dir.absolute()),
        )

    @classmethod
    def to_signed_http_backend(cls, dataset_uri: URI) -> ObjectStore:
        if dataset_uri.object.typ != URIType.DATASET:
            raise NoSupportError(f"{dataset_uri} is not dataset uri")
        return cls(backend=SWDSBackendType.SignedUrl, dataset_uri=dataset_uri)


class StorageBackend(metaclass=ABCMeta):
    def __init__(
        self,
        kind: str,
    ) -> None:
        self.kind = kind

    def __str__(self) -> str:
        return f"StorageBackend for {self.kind}"

    __repr__ = __str__

    @abstractmethod
    def _make_file(
        self, bucket: str, key_compose: t.Tuple[Link, int, int]
    ) -> FileLikeObj:
        raise NotImplementedError


class S3StorageBackend(StorageBackend):
    def __init__(
        self,
        conn: S3Connection,
    ):
        super().__init__(kind=SWDSBackendType.S3)

        self.s3 = boto3.resource(
            "s3",
            endpoint_url=conn.endpoint,
            aws_access_key_id=conn.access_key,
            aws_secret_access_key=conn.secret_key,
            config=S3Config(
                s3=conn.extra_s3_configs,
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

    def _make_file(
        self, bucket: str, key_compose: t.Tuple[Link, int, int]
    ) -> FileLikeObj:
        # TODO: merge connections for s3
        _key, _start, _end = key_compose
        return S3BufferedFileLike(
            s3=self.s3,
            bucket=bucket,
            key=_key.uri,
            start=_start,
            end=_end,
        )


class LocalFSStorageBackend(StorageBackend):
    def __init__(self) -> None:
        super().__init__(kind=SWDSBackendType.LocalFS)

    def _make_file(
        self, bucket: str, key_compose: t.Tuple[Link, int, int]
    ) -> FileLikeObj:
        _key_l, _start, _end = key_compose
        _key = _key_l.uri
        bucket_path = (
            Path(bucket).expanduser() if bucket.startswith("~/") else Path(bucket)
        )
        # TODO: tune reopen file performance, merge files
        data_path = bucket_path / _key[: DatasetStorage.short_sign_cnt]
        if not data_path.exists():
            data_path = bucket_path / _key

        with data_path.open("rb") as f:
            f.seek(_start)
            return io.BytesIO(f.read(_end - _start + 1))


class SignedUrlBackend(StorageBackend, CloudRequestMixed):
    def __init__(self, dataset_uri: URI) -> None:
        super().__init__(kind=SWDSBackendType.SignedUrl)
        self.dataset_uri = dataset_uri

    @http_retry
    def _make_file(
        self, auth: str, key_compose: t.Tuple[Link, int, int]
    ) -> FileLikeObj:
        _key, _start, _end = key_compose
        return HttpBufferedFileLike(
            url=_key.signed_uri or self.sign_uri(_key.uri),
            headers={"Range": f"bytes={_start or 0}-{_end or sys.maxsize}"},
            timeout=90,
        )

    def sign_uri(self, uri: str) -> str:
        r = self.do_http_request(
            f"/project/{self.dataset_uri.project}/{self.dataset_uri.object.typ}/{self.dataset_uri.object.name}/version/{self.dataset_uri.object.version}/sign-links",
            method=HTTPMethod.POST,
            instance_uri=self.dataset_uri,
            params={
                "expTimeMillis": int(
                    os.environ.get("SW_MODEL_PROCESS_UNIT_TIME_MILLIS", "60000")
                ),
            },
            json=[uri],
            use_raise=True,
        ).json()
        return r["data"].get(uri, "")  # type: ignore


_BFType = t.TypeVar("_BFType", bound="BaseBufferedFileLike")


class BaseBufferedFileLike(metaclass=ABCMeta):
    def __init__(self, buffer_size: int) -> None:
        self._read_buffer = BytesBuffer(buffer_size)

    def __enter__(self: _BFType) -> _BFType:
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

    def read(self, size: int) -> bytes:
        ret = self._read(size)
        if isinstance(ret, memoryview):
            return ret.tobytes()
        elif isinstance(ret, bytes):
            return ret
        else:
            raise FormatError(f"{ret}:{type(ret)} is not bytes or memoryview type")

    def _read(self, size: int) -> t.Union[memoryview, bytes]:
        if size == 0:
            return b""
        elif size < 0:
            return self._read_buffer.read() + self._read_exhausted_raw()
        else:
            while len(self._read_buffer) < size:
                fill_size = self._fill_new_bytes()
                if fill_size == 0:
                    return self._read_buffer.read()

            return self._read_buffer.read(size)

    def _read_exhausted_raw(self) -> bytes:
        raise NotImplementedError

    def _fill_new_bytes(self) -> int:
        raise NotImplementedError

    def close(self) -> None:
        self._read_buffer.close()


class HttpBufferedFileLike(BaseBufferedFileLike):
    def __init__(
        self,
        url: str,
        headers: t.Optional[t.Dict[str, str]] = None,
        timeout: int = 90,
        buffer_size: int = 256 * 1024,
    ) -> None:
        super().__init__(buffer_size)
        if headers is None:
            self.headers = {"Accept-Encoding": "identity"}
        else:
            self.headers = headers

        self.buffer_size = buffer_size
        self.resp = requests.get(
            url,
            stream=True,
            headers=self.headers,
            timeout=timeout,
        )

        if not self.resp.ok:
            self.resp.raise_for_status()

        self._read_iter = self.resp.iter_content(buffer_size)

    def read(self, size: int) -> bytes:
        if size == 0:
            return b""
        elif size < 0:
            # urllib3 resp read does not support to read -1
            return self.resp.raw.read()  # type: ignore
        else:
            return self.resp.raw.read(size)  # type: ignore

    def close(self) -> None:
        self.resp.close()
        return super().close()


class BytesBuffer:
    def __init__(self, size: int = io.DEFAULT_BUFFER_SIZE) -> None:
        self._size = size
        self._pos = 0
        self._buffer = bytearray(0)

    def __str__(self) -> str:
        return f"buffer size: {self._size}, pos: {self._pos}, length: {len(self)}"

    __repr__ = __str__

    def close(self) -> None:
        self._buffer = bytearray(0)

    def __len__(self) -> int:
        return len(self._buffer) - self._pos

    def read(self, size: int = -1) -> bytes:
        if size < 0 or size > len(self):
            size = len(self)

        ret = self._buffer[self._pos : self._pos + size]
        self._pos += size
        return bytes(ret)

    def write(self, new_bytes: bytes) -> int:
        self._buffer = self._buffer[self._pos :] + new_bytes
        self._pos = 0
        return len(new_bytes)

    def write_from_iter(self, source: t.Any) -> int:
        # TODO: add iterable check for source
        new_bytes = bytearray()
        for c in source:
            new_bytes += c
            if len(new_bytes) >= self._size:
                break
        return self.write(new_bytes)


class S3BufferedFileLike(BaseBufferedFileLike):
    # TODO: add s3 typing
    def __init__(self, s3: t.Any, bucket: str, key: str, start: int, end: int) -> None:
        super().__init__(_CHUNK_SIZE)
        self.key = self._format_key(key, bucket)
        self.obj = s3.Object(bucket, self.key)

        self.end = end
        self._s3_eof = False
        self._current_s3_start = start

    def _format_key(self, key: str, bucket: str) -> str:
        r = urlparse(key)
        path = r.path.lstrip("/")
        if r.netloc:
            _flag = f"{bucket}/"
            if not path.startswith(_flag):
                raise FormatError(f"{key} does not contain bucket({bucket}) prefix")
            return path.split(_flag, 1)[-1]
        else:
            return path

    def _read_exhausted_raw(self) -> bytes:
        return self.obj.get()["Body"].read()  # type: ignore

    def _fill_new_bytes(self) -> int:
        new_bytes, _ = self._next_data()
        return self._read_buffer.write(new_bytes)

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
