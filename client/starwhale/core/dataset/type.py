from __future__ import annotations

import os
import typing as t
from abc import ABCMeta, abstractmethod
from copy import deepcopy
from enum import Enum, unique
from pathlib import Path
from functools import partial

from starwhale.utils import load_yaml, convert_to_bytes
from starwhale.consts import DEFAULT_STARWHALE_API_VERSION
from starwhale.utils.fs import FilePosition
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError

D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 4 * 1024  # 4k for page cache


class DataField(t.NamedTuple):
    idx: int
    data_size: int
    data: t.Union[bytes, str]
    ext_attr: t.Dict[str, t.Any]


@unique
class LinkType(Enum):
    LocalFS = "local_fs"
    S3 = "s3"
    UNDEFINED = "undefined"
    # TODO: support hdfs, http, ssh link type


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
    _DEFAULT_USER_REGION = "local"
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
        self.region = region or self._DEFAULT_USER_REGION

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

        _secret = _env(_secret_name, "")
        _access = _env(_access_name, "")
        return cls(
            name,
            _access,
            _secret,
            endpoint=_env(cls._ENDPOINT_FMT.format(name=_name), ""),
            region=_env(cls._REGION_FMT.format(name=_name), ""),
        )


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


LocalFSLinkAuth = partial(LinkAuth, ltype=LinkType.LocalFS)
DefaultS3LinkAuth = S3LinkAuth()


class Link:
    def __init__(
        self,
        uri: str = "",
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


class DatasetSummary:
    def __init__(
        self,
        rows: int = 0,
        increased_rows: int = 0,
        label_byte_size: int = 0,
        data_byte_size: int = 0,
        include_link: bool = False,
        include_user_raw: bool = False,
        **kw: t.Any,
    ) -> None:
        self.rows = rows
        self.increased_rows = increased_rows
        self.unchanged_rows = rows - increased_rows
        self.label_byte_size = label_byte_size
        self.data_byte_size = data_byte_size
        self.include_link = include_link
        self.include_user_raw = include_user_raw

    def as_dict(self) -> t.Dict[str, t.Any]:
        d = deepcopy(self.__dict__)
        for k, v in d.items():
            if isinstance(v, Enum):
                d[k] = v.value
        return d

    def __str__(self) -> str:
        return f"Dataset Summary: rows({self.rows}), include user-raw({self.include_user_raw}), include link({self.include_link})"

    def __repr__(self) -> str:
        return (
            f"Dataset Summary: rows({self.rows}, increased: {self.increased_rows}), "
            f"include user-raw({self.include_user_raw}), include link({self.include_link}),"
            f"size(data:{self.data_byte_size}, label: {self.label_byte_size})"
        )


# TODO: use attr to tune code
class DatasetAttr:
    def __init__(
        self,
        volume_size: t.Union[int, str] = D_FILE_VOLUME_SIZE,
        alignment_size: t.Union[int, str] = D_ALIGNMENT_SIZE,
        data_mime_type: MIMEType = MIMEType.UNDEFINED,
        **kw: t.Any,
    ) -> None:
        self.volume_size = convert_to_bytes(volume_size)
        self.alignment_size = convert_to_bytes(alignment_size)
        self.data_mime_type = data_mime_type
        self.kw = kw

    def as_dict(self) -> t.Dict[str, t.Any]:
        # TODO: refactor an asdict mixin class
        _rd = deepcopy(self.__dict__)
        _rd.pop("kw", None)
        for k, v in _rd.items():
            if isinstance(v, Enum):
                _rd[k] = v.value
        return _rd


# TODO: abstract base class from DataSetConfig and ModelConfig
# TODO: use attr to tune code
class DatasetConfig:
    def __init__(
        self,
        name: str,
        data_dir: str,
        process: str,
        data_filter: str = "",
        label_filter: str = "",
        runtime: str = "",
        pkg_data: t.List[str] = [],
        exclude_pkg_data: t.List[str] = [],
        tag: t.List[str] = [],
        desc: str = "",
        version: str = DEFAULT_STARWHALE_API_VERSION,
        attr: t.Dict[str, t.Any] = {},
        **kw: t.Any,
    ) -> None:
        self.name = name
        self.data_dir = str(data_dir)
        self.data_filter = data_filter
        self.label_filter = label_filter
        self.process = process
        self.tag = tag
        self.desc = desc
        self.version = version
        self.runtime = runtime.strip()
        self.attr = DatasetAttr(**attr)
        self.pkg_data = pkg_data
        self.exclude_pkg_data = exclude_pkg_data
        self.kw = kw

        self._validator()

    def _validator(self) -> None:
        if ":" not in self.process:
            raise Exception(
                f"please use module:class format, current is: {self.process}"
            )

        # TODO: add more validator

    def __str__(self) -> str:
        return f"DataSet Config {self.name}"

    def __repr__(self) -> str:
        return f"DataSet Config {self.name}, data:{self.data_dir}"

    def as_dict(self) -> t.Dict[str, t.Any]:
        _r = deepcopy(self.__dict__)
        _r["attr"] = self.attr.as_dict()
        return _r

    @classmethod
    def create_by_yaml(cls, fpath: t.Union[str, Path]) -> DatasetConfig:
        c = load_yaml(fpath)

        return cls(**c)
