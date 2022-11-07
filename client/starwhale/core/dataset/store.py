from __future__ import annotations

import io
import os
import sys
import json
import shutil
import typing as t
from abc import ABCMeta, abstractmethod
from pathlib import Path
from urllib.parse import urlparse

import boto3
import requests
from loguru import logger
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
from starwhale.utils.config import SWCliConfigMixed

from .type import S3LinkAuth

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


class FileLikeObj(Protocol):
    def readline(self) -> bytes:
        ...

    def read(self, size: int) -> bytes:
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
    def from_uri(cls, uri: str, auth_name: str) -> S3Connection:
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
        bucket: str,
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
    def from_data_link_uri(cls, data_uri: str, auth_name: str) -> ObjectStore:
        data_uri = data_uri.strip()
        if not data_uri:
            raise FieldTypeOrValueError("data_uri is empty")

        # TODO: support other uri type
        if data_uri.startswith("s3://"):
            backend = SWDSBackendType.S3
            conn = S3Connection.from_uri(data_uri, auth_name)
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
    def to_signed_http_backend(cls, dataset_uri: URI, auth_name: str) -> ObjectStore:
        if dataset_uri.object.typ != URIType.DATASET:
            raise NoSupportError(f"{dataset_uri} is not dataset uri")
        return cls(
            backend=SWDSBackendType.SignedUrl, bucket=auth_name, dataset_uri=dataset_uri
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

    @abstractmethod
    def _make_file(
        self, bucket: str, key_compose: t.Tuple[str, int, int]
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
        self, bucket: str, key_compose: t.Tuple[str, int, int]
    ) -> FileLikeObj:
        # TODO: merge connections for s3
        _key, _start, _end = key_compose
        return S3BufferedFileLike(
            s3=self.s3,
            bucket=bucket,
            key=_key,
            start=_start,
            end=_end,
        )


class LocalFSStorageBackend(StorageBackend):
    def __init__(self) -> None:
        super().__init__(kind=SWDSBackendType.LocalFS)

    def _make_file(
        self, bucket: str, key_compose: t.Tuple[str, int, int]
    ) -> FileLikeObj:
        _key, _start, _end = key_compose
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

    def _make_file(self, auth: str, key_compose: t.Tuple[str, int, int]) -> FileLikeObj:
        _key, _start, _end = key_compose
        url = self.sign_uri(_key, auth)
        headers = {"Range": f"bytes={_start or 0}-{_end or sys.maxsize}"}
        r = requests.get(url, headers=headers)
        return io.BytesIO(r.content)

    def sign_uri(self, uri: str, auth_name: str) -> str:
        r = self.do_http_request(
            f"/project/{self.dataset_uri.project}/{self.dataset_uri.object.typ}/{self.dataset_uri.object.name}/version/{self.dataset_uri.object.version}/sign-link",
            method=HTTPMethod.GET,
            instance_uri=self.dataset_uri,
            params={"uri": uri, "authName": auth_name, "expTimeMillis": 1000 * 60 * 30},
            use_raise=True,
        ).json()
        return r["data"]  # type: ignore


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

    def read(self, size: int) -> bytes:
        _r = self._read(size)
        if isinstance(_r, memoryview):
            return _r.tobytes()
        elif isinstance(_r, bytes):
            return _r
        else:
            raise FormatError(f"{_r}:{type(_r)} is not bytes or memoryview type")

    def _read(self, size: int) -> memoryview:
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
            logger.warning(f"skip _buffer(memoryview) release exception:{e}")

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
