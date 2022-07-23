from __future__ import annotations

import os
import json
import typing as t
from abc import ABCMeta, abstractmethod
from pathlib import Path

import boto3
import loguru
from loguru import logger as _logger
from botocore.client import Config as S3Config

from starwhale.consts import DataLoaderKind, SWDSBackendType
from starwhale.utils.error import NoSupportError

# TODO: config chunk size
_CHUNK_SIZE = 8 * 1024 * 1024  # 8MB
_FILE_END_POS = -1


class DataField(t.NamedTuple):
    idx: int
    data_size: int
    batch_size: int
    data: bytes
    ext_attr: t.Dict[str, t.Any]


# TODO: use attr to simplify code


class DataLoader(metaclass=ABCMeta):
    def __init__(
        self,
        storage: StorageBackend,
        swds: t.List[t.Dict[str, t.Any]] = [],
        logger: t.Union[loguru.Logger, None] = None,
        kind: str = DataLoaderKind.SWDS,
        deserializer: t.Optional[t.Callable] = None,
    ):
        self.storage = storage
        self.swds = swds
        self.logger = logger or _logger
        self.kind = kind
        self.deserializer = deserializer

        self._do_validate()

    def _do_validate(self) -> None:
        if self.kind not in (DataLoaderKind.JSONL, DataLoaderKind.SWDS):
            raise Exception(f"{self.kind} no support")

    @abstractmethod
    def __iter__(self) -> t.Any:
        raise NotImplementedError

    def __str__(self) -> str:
        return f"DataLoader for {self.storage.backend}"

    def __repr__(self) -> str:
        return (
            f"DataLoader for {self.storage.backend}, "
            f"swds({len(self.swds)}), extra:{self.storage.service}"
        )


class JSONLineDataLoader(DataLoader):
    def __init__(
        self,
        storage: StorageBackend,
        swds: t.List[t.Dict[str, t.Any]] = [],
        logger: t.Union[loguru.Logger, None] = None,
        **kwargs: t.Any,
    ) -> None:
        super().__init__(storage, swds, logger, DataLoaderKind.JSONL, **kwargs)

    def __iter__(self) -> t.Any:
        for _swds in self.swds:
            for data in self._do_iter(_swds["bucket"], _swds["key"]["data"]):
                yield data

    def _do_iter(self, bucket: str, key_compose: str) -> t.Any:
        self.logger.info(f"@{bucket}/{key_compose}")
        _file = self.storage._make_file(bucket, key_compose)
        while True:
            line = _file.readline()
            if not line:
                break

            line = line.strip()
            if not line:
                continue

            if self.deserializer:
                yield self.deserializer(line)
                continue
            # TODO: add json exception ignore?
            yield json.loads(line)


class SWDSDataLoader(DataLoader):
    def __init__(
        self,
        storage: StorageBackend,
        swds: t.List[t.Dict[str, t.Any]] = [],
        logger: t.Union[loguru.Logger, None] = None,
    ) -> None:
        super().__init__(storage, swds, logger, DataLoaderKind.SWDS)

    def __iter__(self) -> t.Generator[t.Tuple[DataField, DataField], None, None]:
        for _swds in self.swds:
            for data, label in zip(
                self._do_iter(
                    _swds["bucket"], _swds["key"]["data"], _swds.get("ext_attr", {})
                ),
                self._do_iter(
                    _swds["bucket"], _swds["key"]["label"], _swds.get("ext_attr", {})
                ),
            ):
                yield data, label

    def _do_iter(
        self, bucket: str, key_compose: str, ext_attr: t.Dict[str, t.Any]
    ) -> t.Iterator[DataField]:
        from .dataset import _header_size, _header_struct

        self.logger.info(f"@{bucket}/{key_compose}")
        _file = self.storage._make_file(bucket, key_compose)
        while True:
            header = _file.read(_header_size)
            if not header:
                break
            _, _, idx, size, padding_size, batch, _ = _header_struct.unpack(header)
            data = _file.read(size + padding_size)
            yield DataField(
                idx,
                size,
                batch,
                data[:size].tobytes() if isinstance(data, memoryview) else data[:size],
                ext_attr,
            )


class StorageBackend(metaclass=ABCMeta):
    def __init__(
        self,
        backend: str,
        secret: t.Dict[str, t.Any] = {},
        service: t.Dict[str, t.Any] = {},
    ) -> None:
        self.service = service
        self.backend = backend
        self.secret = secret

        self._do_validate()

    def __str__(self) -> str:
        return f"StorageBackend for {self.backend}"

    def __repr__(self) -> str:
        return f"StorageBackend for {self.backend}, service: {self.service}"

    def _do_validate(self) -> None:
        # TODO: add more validator
        if self.backend == SWDSBackendType.S3:
            _s = self.secret
            if (
                not _s
                or not isinstance(_s, dict)
                or not _s.get("access_key")
                or not _s.get("secret_key")
            ):
                raise Exception(f"secret({_s}) format is invalid")

            _s = self.service
            if (
                not _s
                or not isinstance(_s, dict)
                or not _s.get("endpoint")
                or not _s.get("region")
            ):
                raise Exception(f"s3_service({_s} format is invalid)")

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

    # TODO: tune typing hint for FileObj
    @abstractmethod
    def _make_file(self, bucket: str, key_compose: str) -> t.Any:
        raise NotImplementedError


class S3StorageBackend(StorageBackend):
    def __init__(
        self, secret: t.Dict[str, t.Any] = {}, service: t.Dict[str, t.Any] = {}
    ):
        super().__init__(backend=SWDSBackendType.S3, secret=secret, service=service)

        _env = os.environ
        self.s3 = boto3.resource(
            "s3",
            endpoint_url=self.service["endpoint"],
            aws_access_key_id=self.secret["access_key"],
            aws_secret_access_key=self.secret["secret_key"],
            config=S3Config(
                connect_timeout=float(_env.get("SW_S3_CONNECT_TIMEOUT", 10)),
                read_timeout=float(_env.get("SW_S3_READ_TIMEOUT", 60)),
                signature_version="s3v4",
                retries={
                    "total_max_attempts": int(_env.get("SW_S3_TOTAL_MAX_ATTEMPTS", 6)),
                    "mode": "standard",
                },
            ),
            region_name=self.service["region"],
        )

    # TODO: tune return typing hint
    def _make_file(self, bucket: str, key_compose: str) -> t.Any:
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
        super().__init__(backend=SWDSBackendType.FUSE)

    def _make_file(self, bucket: str, key_compose: str) -> t.Any:
        _key, _start, _ = self._parse_key(key_compose)
        bucket_path = (
            Path(bucket).expanduser() if bucket.startswith("~/") else Path(bucket)
        )
        _file = (bucket_path / _key).open("rb")
        _file.seek(_start)
        # TODO: support end
        return _file


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

    def readline(self) -> str:
        if self._iter_lines is None:
            self._iter_lines = self.obj.get()["Body"].iter_lines(chunk_size=_CHUNK_SIZE)

        try:
            line: bytes = next(self._iter_lines)  # type: ignore
        except StopIteration:
            line = b""
        return line.decode()

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
    swds_config: t.Dict[str, t.Any],
    logger: t.Union[loguru.Logger, None] = None,
    **kwargs: t.Any,
) -> DataLoader:
    """s3 or fuse data loader

    Args:
        swds_config (dict): origin json example

    {
        "backend": "s3",  // s3 or fuse
        "kind": "swds",       // swds or jsonline
        "secret": {       // auth info
            "access_key": "username or access key",
            "secret_key": "password or secret key"
        },
        "service": {  // only for s3
            "endpoint": "s3 address",
            "region": "s3 region",
        },
        "swds": [   // swds dataset address
            {
                "bucket": "s3 bucket or rootdir",
                "key": {
                    "data":  "{name}:{start_pos}:{end_pos}", // start default is 0
                    "label": "{name}:{start_pos}:{end_pos}"  // end default is -1, which is EOF of File or Object
                },
                "ext_attr":{  //optional
                    "ds_name": "dataset name",
                    "ds_version": "dataset version"
                }
            },
            {
                "bucket": "s3 bucket or rootdir",
                "key": {
                    "data":  "{name}:{start_pos}:{end_pos}",
                    "label": "{name}:{start_pos}:{end_pos}"
                }
            }
        ]
    }

        logger (t.Union[loguru.Logger, None], optional): logger. Defaults to None.

    Raises:
        NoSupportError: _description_

    Returns:
        DataLoader: S3DataLoader or FuseDataLoader
    """
    logger = logger or _logger

    _storage: t.Union[S3StorageBackend, FuseStorageBackend]

    _backend = swds_config["backend"]
    if _backend == SWDSBackendType.S3:
        _storage = S3StorageBackend(swds_config["secret"], swds_config["service"])
    elif _backend == SWDSBackendType.FUSE:
        _storage = FuseStorageBackend()
    else:
        raise NoSupportError(f"{_backend} backend storage, no support")

    _kind = swds_config.get("kind", DataLoaderKind.SWDS)
    if _kind == DataLoaderKind.JSONL:
        return JSONLineDataLoader(_storage, swds_config["swds"], logger, **kwargs)
    elif _kind == DataLoaderKind.SWDS:
        return SWDSDataLoader(_storage, swds_config["swds"], logger)
    else:
        raise NoSupportError(f"{_kind} data loader, no support")
