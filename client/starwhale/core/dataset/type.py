from __future__ import annotations

import io
import os
import base64
import typing as t
from abc import ABCMeta
from enum import Enum, unique
from pathlib import Path
from functools import partial
from urllib.parse import urlparse

import requests

from starwhale.utils import load_yaml, convert_to_bytes, validate_obj_name
from starwhale.consts import (
    LATEST_TAG,
    SHORT_VERSION_CNT,
    DEFAULT_STARWHALE_API_VERSION,
)
from starwhale.base.uri import URI
from starwhale.utils.fs import FilePosition
from starwhale.base.type import URIType, InstanceType
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError
from starwhale.utils.retry import http_retry
from starwhale.api._impl.data_store import SwObject

D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 4 * 1024  # 4k for page cache


@unique
class LinkType(Enum):
    LocalFS = "local_fs"
    S3 = "s3"
    UNDEFINED = "undefined"
    # TODO: support hdfs, http, ssh link type


_LAType = t.TypeVar("_LAType", bound="LinkAuth")


class LinkAuth(SwObject):
    def __init__(
        self, name: str = "", ltype: t.Union[LinkType, str] = LinkType.UNDEFINED
    ) -> None:
        self.name = name.strip()
        self._ltype = LinkType(ltype).value
        self._do_validate()

    @property
    def ltype(self) -> LinkType:
        return LinkType(self._ltype)

    def _do_validate(self) -> None:
        if self.ltype not in LinkType:
            raise NoSupportError(f"Link Type: {self.ltype}")

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
    WEBM = "video/webm"
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
            ".webm": cls.WEBM,
            ".wav": cls.WAV,
            ".csv": cls.CSV,
            ".txt": cls.PLAIN,
        }
        return _map.get(Path(name).suffix, MIMEType.UNDEFINED)


LocalFSLinkAuth = partial(LinkAuth, ltype=LinkType.LocalFS)
DefaultS3LinkAuth = S3LinkAuth()


_T = t.TypeVar("_T")
_TupleOrList = t.Union[t.Tuple[_T, ...], t.List[_T]]
_TShape = _TupleOrList[t.Optional[int]]
_TArtifactFP = t.Union[str, bytes, Path, io.IOBase]


@unique
class ArtifactType(Enum):
    Binary = "binary"
    Image = "image"
    Video = "video"
    Audio = "audio"
    Text = "text"
    Link = "link"


_TBAType = t.TypeVar("_TBAType", bound="BaseArtifact")


class BaseArtifact(ASDictMixin, metaclass=ABCMeta):
    def __init__(
        self,
        fp: _TArtifactFP,
        type: t.Union[ArtifactType, str],
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
        encoding: str = "",
    ) -> None:
        self.fp = str(fp) if isinstance(fp, Path) else fp
        self._type = ArtifactType(type).value

        _fpath = str(fp) if isinstance(fp, (Path, str)) and fp else ""
        self.display_name = display_name or os.path.basename(_fpath)
        self._mime_type = (mime_type or MIMEType.create_by_file_suffix(_fpath)).value
        self.shape = list(shape) if shape else shape
        self.encoding = encoding
        self._do_validate()

    def _do_validate(self) -> None:
        ...

    @property
    def type(self) -> ArtifactType:
        return ArtifactType(self._type)

    @property
    def mime_type(self) -> MIMEType:
        return MIMEType(self._mime_type)

    @classmethod
    def reflect(cls, raw_data: bytes, data_type: t.Dict[str, t.Any]) -> BaseArtifact:
        if not isinstance(raw_data, bytes):
            raise NoSupportError(f"raw data type({type(raw_data)}) is not bytes")

        # TODO: support data_type reflect
        dtype = data_type.get("type")
        mime_type = MIMEType(data_type.get("mime_type", MIMEType.UNDEFINED))
        shape = data_type.get("shape", [])
        encoding = data_type.get("encoding", "")
        display_name = data_type.get("display_name", "")

        if dtype == ArtifactType.Text.value:
            _encoding = encoding or Text.DEFAULT_ENCODING
            return Text(content=raw_data.decode(_encoding), encoding=_encoding)
        elif dtype == ArtifactType.Image.value:
            return Image(
                raw_data, mime_type=mime_type, shape=shape, display_name=display_name
            )
        elif dtype == ArtifactType.Audio.value:
            return Audio(
                raw_data, mime_type=mime_type, shape=shape, display_name=display_name
            )
        elif dtype == ArtifactType.Video.value:
            return Video(
                raw_data, mime_type=mime_type, shape=shape, display_name=display_name
            )
        elif not dtype or dtype == ArtifactType.Binary.value:
            return Binary(raw_data)
        elif dtype == ArtifactType.Link.value:
            return cls.reflect(raw_data, data_type["data_type"])
        else:
            raise NoSupportError(f"Artifact reflect error: {data_type}")

    # TODO: add to_tensor, to_numpy method
    def to_bytes(self, encoding: str = "utf-8") -> bytes:
        if isinstance(self.fp, bytes):
            return self.fp
        elif isinstance(self.fp, (str, Path)):
            return Path(self.fp).read_bytes()
        elif isinstance(self.fp, io.IOBase):
            _pos = self.fp.tell()
            _content = self.fp.read()
            self.fp.seek(_pos)
            return _content.encode(encoding) if isinstance(_content, str) else _content  # type: ignore
        else:
            raise NoSupportError(f"read raw for type:{type(self.fp)}")

    def carry_raw_data(self: _TBAType) -> _TBAType:
        self._raw_base64_data = base64.b64encode(self.to_bytes()).decode()
        return self

    def astype(self) -> t.Dict[str, t.Any]:
        return {
            "type": self.type,
            "mime_type": self.mime_type,
            "shape": self.shape,
            "encoding": self.encoding,
            "display_name": self.display_name,
        }

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict[str, t.Any]:
        return super().asdict(ignore_keys or ["fp"])

    def __str__(self) -> str:
        return f"{self.type}, display:{self.display_name}, mime_type:{self.mime_type}, shape:{self.shape}, encoding: {self.encoding}"

    __repr__ = __str__


class Binary(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP = b"",
        mime_type: MIMEType = MIMEType.UNDEFINED,
    ) -> None:
        super().__init__(fp, ArtifactType.Binary, "", (1,), mime_type)


class Image(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
        as_mask: bool = False,
        mask_uri: str = "",
    ) -> None:
        self.as_mask = as_mask
        self.mask_uri = mask_uri
        super().__init__(
            fp,
            ArtifactType.Image,
            display_name=display_name,
            shape=shape or (None, None, 3),
            mime_type=mime_type,
        )

    def _do_validate(self) -> None:
        if self.mime_type not in (
            MIMEType.PNG,
            MIMEType.JPEG,
            MIMEType.WEBP,
            MIMEType.SVG,
            MIMEType.GIF,
            MIMEType.APNG,
            MIMEType.GRAYSCALE,
            MIMEType.UNDEFINED,
        ):
            raise NoSupportError(f"Image type: {self.mime_type}")


class GrayscaleImage(Image):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        as_mask: bool = False,
        mask_uri: str = "",
    ) -> None:
        shape = shape or (None, None)
        super().__init__(
            fp,
            display_name,
            (shape[0], shape[1], 1),
            mime_type=MIMEType.GRAYSCALE,
            as_mask=as_mask,
            mask_uri=mask_uri,
        )


class Audio(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
    ) -> None:
        shape = shape or (None,)
        super().__init__(fp, ArtifactType.Audio, display_name, shape, mime_type)

    def _do_validate(self) -> None:
        if self.mime_type not in (
            MIMEType.MP3,
            MIMEType.WAV,
            MIMEType.UNDEFINED,
        ):
            raise NoSupportError(f"Audio type: {self.mime_type}")


class Video(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
    ) -> None:
        shape = shape or (None,)
        super().__init__(fp, ArtifactType.Video, display_name, shape, mime_type)

    def _do_validate(self) -> None:
        if self.mime_type not in (
            MIMEType.MP4,
            MIMEType.AVI,
            MIMEType.WEBM,
            MIMEType.UNDEFINED,
        ):
            raise NoSupportError(f"Video type: {self.mime_type}")


class ClassLabel(ASDictMixin, SwObject):
    def __init__(
        self, names: t.Optional[t.List[t.Union[int, float, str]]] = None
    ) -> None:
        self._type = "class_label"
        self.names = names or []

    @classmethod
    def from_num_classes(cls, num: int) -> ClassLabel:
        if num < 1:
            raise FieldTypeOrValueError(f"num:{num} less than 1")
        return cls(list(range(0, num)))

    def __str__(self) -> str:
        return f"ClassLabel: {len(self.names)} classes"

    def __repr__(self) -> str:
        return f"ClassLabel: {self.names}"


# TODO: support other bounding box format
class BoundingBox(ASDictMixin, SwObject):
    def __init__(
        self, x: float = 0, y: float = 0, width: float = 0, height: float = 0
    ) -> None:
        self._type = "bounding_box"
        self.x = x
        self.y = y
        self.width = width
        self.height = height

    def to_list(self) -> t.List[float]:
        return [self.x, self.y, self.width, self.height]

    def __str__(self) -> str:
        return f"BoundingBox: point:({self.x}, {self.y}), width: {self.width}, height: {self.height})"

    __repr__ = __str__


class Text(BaseArtifact, SwObject):
    DEFAULT_ENCODING = "utf-8"

    def __init__(self, content: str = "", encoding: str = DEFAULT_ENCODING) -> None:
        # TODO: add encoding validate
        self.content = content
        super().__init__(
            fp=b"",
            type=ArtifactType.Text,
            display_name=f"{content[:SHORT_VERSION_CNT]}...",
            shape=(1,),
            mime_type=MIMEType.PLAIN,
            encoding=encoding,
        )

    def to_bytes(self, encoding: str = "") -> bytes:
        return self.content.encode(encoding or self.encoding)

    def to_str(self) -> str:
        return self.content


# https://cocodataset.org/#format-data
class COCOObjectAnnotation(ASDictMixin, SwObject):
    def __init__(
        self,
        id: int = 0,
        image_id: t.Union[int, str] = 0,
        category_id: int = 0,
        area: t.Union[float, int] = 0,
        bbox: t.Optional[t.Union[BoundingBox, t.List[float]]] = None,
        iscrowd: int = 0,
    ) -> None:
        self._type = "coco_object_annotation"
        self.id = id
        self.image_id = image_id
        self.category_id = category_id
        self.bbox = bbox.to_list() if isinstance(bbox, BoundingBox) else bbox
        self.area = area
        self.iscrowd = iscrowd

        self.do_validate()

    def do_validate(self) -> None:
        if self.iscrowd not in (0, 1):
            raise FieldTypeOrValueError(f"iscrowd({self.iscrowd}) only accepts 0 or 1")

    @property
    def segmentation(self) -> t.Optional[t.Union[t.List, t.Dict]]:
        if getattr(self, "_segmentation_polygon", None):
            return self._segmentation_polygon
        elif getattr(self, "_segmentation_rle_size", None) and getattr(
            self, "_segmentation_rle_counts", None
        ):
            return {
                "size": self._segmentation_rle_size,
                "counts": self._segmentation_rle_counts,
            }
        else:
            return None

    @segmentation.setter
    def segmentation(self, value: t.Union[t.List, t.Dict]) -> None:
        # hack for datastore dict type unify value type
        # TODO: datastore support pythonic dict type
        if isinstance(value, list):
            self._segmentation_polygon = value
        elif isinstance(value, dict):
            self._segmentation_rle_size = value["size"]
            self._segmentation_rle_counts = value["counts"]
        else:
            raise NoSupportError(
                f"segmentation only supports list(polygon) and dict(rle) format: {value}"
            )


class Link(ASDictMixin, SwObject):
    def __init__(
        self,
        uri: t.Union[str, Path] = "",
        auth: t.Optional[LinkAuth] = None,
        offset: int = FilePosition.START.value,
        size: int = -1,
        data_type: t.Optional[BaseArtifact] = None,
        with_local_fs_data: bool = False,
    ) -> None:
        self._type = "link"
        self.uri = (str(uri)).strip()
        self.offset = offset
        self.size = size
        self.auth = auth
        self.data_type = data_type
        self.with_local_fs_data = with_local_fs_data
        self._local_fs_uri = ""
        self._signed_uri = ""

        self.do_validate()

    @property
    def local_fs_uri(self) -> str:
        return self._local_fs_uri

    @local_fs_uri.setter
    def local_fs_uri(self, value: str) -> None:
        self._local_fs_uri = value

    @property
    def signed_uri(self) -> str:
        return self._signed_uri

    @signed_uri.setter
    def signed_uri(self, value: str) -> None:
        self._signed_uri = value

    def do_validate(self) -> None:
        if self.offset < 0:
            raise FieldTypeOrValueError(f"offset({self.offset}) must be non-negative")

    def astype(self) -> t.Dict[str, t.Any]:
        return {
            "type": self._type,
            "data_type": self.data_type.astype() if self.data_type else {},
        }

    def __str__(self) -> str:
        return f"Link {self.uri}"

    def __repr__(self) -> str:
        return f"Link uri:{self.uri}, offset:{self.offset}, size:{self.size}, data type:{self.data_type}, with localFS data:{self.with_local_fs_data}"

    @http_retry
    def to_bytes(self, dataset_uri: t.Union[str, URI]) -> bytes:
        from .store import ObjectStore

        if self.signed_uri:
            r = requests.get(self.signed_uri, timeout=10)
            return r.content
        # TODO: auto inject dataset_uri in the loader process
        if isinstance(dataset_uri, str):
            dataset_uri = URI(dataset_uri, expected_type=URIType.DATASET)

        auth_name = self.auth.name if self.auth else ""
        if dataset_uri.instance_type == InstanceType.CLOUD:
            key_compose = self, 0, 0
            store = ObjectStore.to_signed_http_backend(dataset_uri)
        else:
            _up = urlparse(self.uri)
            if _up.scheme:
                key_compose = (
                    Link(self.local_fs_uri) if self.local_fs_uri else self,
                    0,
                    0,
                )
                store = ObjectStore.from_data_link_uri(key_compose[0], auth_name)
            else:
                key_compose = (
                    Link(self.local_fs_uri) if self.local_fs_uri else self,
                    0,
                    -2,
                )
                store = ObjectStore.from_dataset_uri(dataset_uri)

        with store.backend._make_file(store.bucket, key_compose) as f:
            return f.read(-1)  # type: ignore


class DatasetSummary(ASDictMixin):
    def __init__(
        self,
        rows: int = 0,
        increased_rows: int = 0,
        data_byte_size: int = 0,
        include_link: bool = False,
        include_user_raw: bool = False,
        annotations: t.Optional[t.List[str]] = None,
        **kw: t.Any,
    ) -> None:
        self.rows = rows
        self.increased_rows = increased_rows
        self.unchanged_rows = rows - increased_rows
        self.data_byte_size = data_byte_size
        self.include_link = include_link
        self.include_user_raw = include_user_raw
        self.annotations = annotations or []

    def __str__(self) -> str:
        return f"Dataset Summary: rows({self.rows}), include user-raw({self.include_user_raw}), include link({self.include_link})"

    def __repr__(self) -> str:
        return (
            f"Dataset Summary: rows({self.rows}, increased: {self.increased_rows}), "
            f"include user-raw({self.include_user_raw}), include link({self.include_link}),"
            f"size(data:{self.data_byte_size}, annotations: {self.annotations})"
        )


# TODO: use attr to tune code
class DatasetAttr(ASDictMixin):
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

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        return super().asdict(ignore_keys=ignore_keys or ["kw"])


# TODO: abstract base class from DatasetConfig and ModelConfig
# TODO: use attr to tune code
class DatasetConfig(ASDictMixin):
    def __init__(
        self,
        name: str = "",
        handler: str = "",
        pkg_data: t.List[str] = [],
        exclude_pkg_data: t.List[str] = [],
        desc: str = "",
        version: str = DEFAULT_STARWHALE_API_VERSION,
        attr: t.Dict[str, t.Any] = {},
        project_uri: str = "",
        runtime_uri: str = "",
        append: bool = False,
        append_from: str = LATEST_TAG,
        **kw: t.Any,
    ) -> None:
        self.name = name
        self.handler = handler
        self.desc = desc
        self.version = version
        self.attr = DatasetAttr(**attr)
        self.pkg_data = pkg_data
        self.exclude_pkg_data = exclude_pkg_data
        self.project_uri = project_uri
        self.runtime_uri = runtime_uri
        self.append = append
        self.append_from = append_from

        self.kw = kw

    def do_validate(self) -> None:
        _ok, _reason = validate_obj_name(self.name)
        if not _ok:
            raise FieldTypeOrValueError(f"name field:({self.name}) error: {_reason}")

        if ":" not in self.handler:
            raise Exception(
                f"please use module:class format, current is: {self.handler}"
            )

        # TODO: add more validator

    def __str__(self) -> str:
        return f"Dataset Config {self.name}"

    __repr__ = __str__

    @classmethod
    def create_by_yaml(cls, fpath: t.Union[str, Path]) -> DatasetConfig:
        c = load_yaml(fpath)

        return cls(**c)
