from __future__ import annotations

import io
import os
import sys
import base64
import typing as t
from abc import ABCMeta
from enum import Enum, unique
from pathlib import Path
from functools import partial
from urllib.parse import urlparse

import numpy

from starwhale.utils import load_yaml, convert_to_bytes, validate_obj_name
from starwhale.consts import SHORT_VERSION_CNT, DEFAULT_STARWHALE_API_VERSION
from starwhale.utils.fs import FilePosition
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.error import (
    NoSupportError,
    FieldTypeOrValueError,
    MissingDependencyError,
)
from starwhale.utils.retry import http_retry
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.api._impl.data_store import SwObject, _TYPE_DICT

D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 128  # for page cache


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
    PPM = "image/x-portable-pixmap"
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
    def create_by_file_suffix(cls, name: str | Path) -> MIMEType:
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
            ".ppm": cls.PPM,
            ".avi": cls.AVI,
            ".webm": cls.WEBM,
            ".wav": cls.WAV,
            ".csv": cls.CSV,
            ".txt": cls.PLAIN,
        }
        return _map.get(Path(name).suffix.lower(), MIMEType.UNDEFINED)


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
    Unknown = "unknown"


class _ResourceOwnerMixin:
    @property
    def owner(self) -> t.Optional[Resource]:
        _owner = getattr(self, "_owner", None)
        return Resource(_owner, ResourceType.dataset, refine=False) if _owner else None

    @owner.setter
    def owner(self, value: t.Optional[Resource]) -> None:
        self._owner = str(value) if value else None


# TODO: support File, Model artifacts


_TBAType = t.TypeVar("_TBAType", bound="BaseArtifact")


class BaseArtifact(ASDictMixin, _ResourceOwnerMixin, metaclass=ABCMeta):
    def __init__(
        self,
        fp: _TArtifactFP,
        type: t.Union[ArtifactType, str],
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
        dtype: t.Any = numpy.int8,
        encoding: str = "",
        link: t.Optional[Link] = None,
    ) -> None:
        self.fp = str(fp) if isinstance(fp, Path) else fp
        self.__cache_bytes: bytes = self.fp if isinstance(self.fp, bytes) else bytes()
        self._type = ArtifactType(type).value

        _fpath = str(fp) if isinstance(fp, (Path, str)) and fp else ""
        self.display_name = display_name or os.path.basename(_fpath)
        self._mime_type = (mime_type or MIMEType.create_by_file_suffix(_fpath)).value
        self.shape = list(shape) if shape else shape
        self._dtype_name: str = numpy.dtype(dtype).name
        self.encoding = encoding
        self.link = link
        self._do_validate()

    def _do_validate(self) -> None:
        ...

    @property
    def dtype(self) -> numpy.dtype:
        return numpy.dtype(self._dtype_name)

    @property
    def type(self) -> ArtifactType:
        return ArtifactType(self._type)

    @property
    def mime_type(self) -> MIMEType:
        return MIMEType(self._mime_type)

    def to_bytes(self, encoding: str = "utf-8") -> bytes:
        return self.fetch_data(encoding)

    def clear_cache(self) -> None:
        self.__cache_bytes = b""
        if isinstance(self.fp, (bytes, io.IOBase, str)):
            self.fp = ""

    def fetch_data(self, encoding: str = "utf-8") -> bytes:
        if self.__cache_bytes:
            return self.__cache_bytes
        if self.fp and isinstance(self.fp, bytes):
            self.__cache_bytes = self.fp
            return self.__cache_bytes
        elif self.fp and isinstance(self.fp, (str, Path)):
            self.__cache_bytes = Path(self.fp).read_bytes()
            return self.__cache_bytes
        elif isinstance(self.fp, io.IOBase):
            _pos = self.fp.tell()
            _content = self.fp.read()
            self.fp.seek(_pos)
            self.__cache_bytes = _content.encode(encoding) if isinstance(_content, str) else _content  # type: ignore
            return self.__cache_bytes
        elif self.link:
            self.link.owner = self.owner
            self.__cache_bytes = self.link.to_bytes()
            return self.__cache_bytes
        elif not self.fp and isinstance(self.fp, bytes):
            return self.fp
        else:
            raise NoSupportError(f"read raw for type:{type(self.fp)}")

    def to_numpy(self) -> numpy.ndarray:
        raise NotImplementedError

    def to_json(self) -> str:
        raise NotImplementedError

    def to_tensor(self) -> t.Any:
        raise NotImplementedError

    to_pt_tensor = to_tensor

    def carry_raw_data(self: _TBAType) -> _TBAType:
        self._raw_base64_data = base64.b64encode(self.to_bytes()).decode()
        return self

    def drop_data(self: _TBAType) -> _TBAType:
        self.fp = b""
        if hasattr(self, "_raw_base64_data"):
            del self._raw_base64_data
        return self

    def astype(self) -> t.Dict[str, t.Any]:
        return {
            "type": self.type.value,
            "mime_type": self.mime_type.value,
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
        dtype: t.Type = numpy.bytes_,
        link: t.Optional[Link] = None,
    ) -> None:
        super().__init__(
            fp,
            ArtifactType.Binary,
            "",
            (),
            mime_type,
            dtype=dtype,
            link=link,
        )

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_bytes(), dtype=self.dtype)


class NumpyBinary(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP,
        dtype: t.Type,
        shape: _TShape,
        link: t.Optional[Link] = None,
    ) -> None:
        super().__init__(
            fp=fp,
            type=ArtifactType.Binary,
            shape=shape,
            dtype=dtype,
            link=link,
        )

    def to_numpy(self) -> numpy.ndarray:
        return numpy.frombuffer(self.to_bytes(), dtype=self.dtype).reshape(self.shape)  # type: ignore

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_numpy_to_tensor

        return convert_numpy_to_tensor(self.to_numpy())


class Image(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: MIMEType = MIMEType.UNDEFINED,
        as_mask: bool = False,
        mask_uri: str = "",
        dtype: t.Type = numpy.uint8,
        link: t.Optional[Link] = None,
    ) -> None:
        self.as_mask = as_mask
        self.mask_uri = mask_uri
        super().__init__(
            fp,
            ArtifactType.Image,
            display_name=display_name,
            shape=shape or (None, None, 3),
            mime_type=mime_type,
            dtype=dtype,
            link=link,
        )

    def _do_validate(self) -> None:
        if self.mime_type not in (
            MIMEType.PNG,
            MIMEType.JPEG,
            MIMEType.WEBP,
            MIMEType.SVG,
            MIMEType.APNG,
            MIMEType.PPM,
            MIMEType.GRAYSCALE,
            MIMEType.UNDEFINED,
        ):
            raise NoSupportError(f"Image type: {self.mime_type}")

    def to_pil(self) -> t.Any:
        try:
            from PIL import Image as PILImage
        except ImportError:  # pragma: no cover
            raise MissingDependencyError(
                "pillow is required to convert Starwhale Image to Pillow Image, please install pillow with 'pip install pillow' or 'pip install starwhale[image]'."
            )

        return PILImage.open(io.BytesIO(self.to_bytes()))

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_pil())

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_numpy_to_tensor

        return convert_numpy_to_tensor(self.to_numpy())


class GrayscaleImage(Image):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        as_mask: bool = False,
        mask_uri: str = "",
        dtype: t.Type = numpy.uint8,
        link: t.Optional[Link] = None,
    ) -> None:
        shape = shape or (None, None)
        super().__init__(
            fp,
            display_name,
            (shape[0], shape[1], 1),
            mime_type=MIMEType.GRAYSCALE,
            as_mask=as_mask,
            mask_uri=mask_uri,
            dtype=dtype,
            link=link,
        )


class Audio(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
        dtype: t.Type = numpy.float64,
        link: t.Optional[Link] = None,
    ) -> None:
        shape = shape or (None,)
        super().__init__(
            fp,
            ArtifactType.Audio,
            display_name,
            shape,
            mime_type,
            dtype=dtype,
            link=link,
        )

    def _do_validate(self) -> None:
        if self.mime_type not in (
            MIMEType.MP3,
            MIMEType.WAV,
            MIMEType.UNDEFINED,
        ):
            raise NoSupportError(f"Audio type: {self.mime_type}")

    def to_numpy(self) -> numpy.ndarray:
        try:
            import soundfile
        except ImportError:  # pragma: no cover
            raise MissingDependencyError(
                "soundfile is required to convert Starwhale Auto to numpy ndarray, please install soundfile with 'pip install soundfile' or 'pip install starwhale[audio]'."
            )

        array, _ = soundfile.read(
            io.BytesIO(self.to_bytes()), dtype=self.dtype.name, always_2d=True
        )
        return array  # type: ignore[no-any-return]

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_numpy_to_tensor

        return convert_numpy_to_tensor(self.to_numpy())


class Video(BaseArtifact, SwObject):
    def __init__(
        self,
        fp: _TArtifactFP = "",
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
        dtype: t.Type = numpy.uint8,
        link: t.Optional[Link] = None,
    ) -> None:
        shape = shape or (None,)
        super().__init__(
            fp,
            ArtifactType.Video,
            display_name,
            shape,
            mime_type,
            dtype=dtype,
            link=link,
        )

    # TODO： support to_tensor methods

    def to_numpy(self) -> numpy.ndarray:
        # TODO: support video encode/decode
        return numpy.array(self.to_bytes(), dtype=self.dtype)

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

    @property
    def shape(self) -> t.Tuple[int]:
        return (4,)

    @property
    def dtype(self) -> numpy.dtype:
        return numpy.dtype(numpy.float64)

    def to_list(self) -> t.List[float]:
        return [self.x, self.y, self.width, self.height]

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_list(), self.dtype)

    def to_bytes(self) -> bytes:
        return self.to_numpy().tobytes()

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_list_to_tensor

        return convert_list_to_tensor(self.to_list())

    def __str__(self) -> str:
        return f"BoundingBox: point:({self.x}, {self.y}), width: {self.width}, height: {self.height})"

    __repr__ = __str__


class BoundingBox3D(ASDictMixin, SwObject):
    """
    This is a 3d bounding box viewer helper class for two-dimensional UI. Two BoundingBox are needed to show it.
    bbox_a: the box that is facing user on the two-dimensional UI
    bbox_b: the box that is facing bbox_a on the two-dimensional UI
    """

    SHAPE = 2, 4

    def __init__(self, bbox_a: BoundingBox, bbox_b: BoundingBox) -> None:
        self._type = "bounding_box3D"
        self.bbox_a = bbox_a
        self.bbox_b = bbox_b

    @property
    def shape(self) -> tuple[int, int]:
        return BoundingBox3D.SHAPE

    @property
    def dtype(self) -> numpy.dtype:
        return numpy.dtype(numpy.float64)

    def to_list(self) -> t.List[t.List[float]]:
        return [self.bbox_a.to_list(), self.bbox_b.to_list()]

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_list(), self.dtype)

    def to_bytes(self) -> bytes:
        return self.to_numpy().tobytes()

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_list_to_tensor

        return convert_list_to_tensor(self.to_list())

    def __str__(self) -> str:
        return f"BoundingBox A: {str(self.bbox_a)} ; BoundingBox B: {str(self.bbox_b)} "

    __repr__ = __str__


class Line(ASDictMixin, SwObject):
    def __init__(self, points: t.List[Point]) -> None:
        self._type = "line"
        self.points = points

    @property
    def dtype(self) -> numpy.dtype:
        return numpy.dtype(numpy.float64)

    def to_list(self) -> t.List[t.List[float]]:
        return [p.to_list() for p in self.points]

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_list(), self.dtype)

    def to_bytes(self) -> bytes:
        return self.to_numpy().tobytes()

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_list_to_tensor

        return convert_list_to_tensor(self.to_list())

    def __str__(self) -> str:
        return f"Line: {self.to_list()}"

    __repr__ = __str__


class Point(ASDictMixin, SwObject):
    def __init__(self, x: float = 0, y: float = 0) -> None:
        self._type = "point"
        self.x = x
        self.y = y

    @property
    def dtype(self) -> numpy.dtype:
        return numpy.dtype(numpy.float64)

    def to_list(self) -> t.List[float]:
        return [self.x, self.y]

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_list(), self.dtype)

    def to_bytes(self) -> bytes:
        return self.to_numpy().tobytes()

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_list_to_tensor

        return convert_list_to_tensor(self.to_list())

    def __str__(self) -> str:
        return f"Point: ({self.x}, {self.y})"

    __repr__ = __str__


class Polygon(ASDictMixin, SwObject):
    def __init__(self, points: t.List[Point]) -> None:
        self._type = "polygon"
        self.points = points

    @property
    def dtype(self) -> numpy.dtype:
        return numpy.dtype(numpy.float64)

    def to_list(self) -> t.List[t.List[float]]:
        return [p.to_list() for p in self.points]

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_list(), self.dtype)

    def to_bytes(self) -> bytes:
        return self.to_numpy().tobytes()

    def to_tensor(self) -> t.Any:
        from starwhale.integrations.pytorch import convert_list_to_tensor

        return convert_list_to_tensor(self.to_list())

    def __str__(self) -> str:
        return f"Polygon: {self.to_list()}"

    __repr__ = __str__


class Text(BaseArtifact, SwObject):
    DEFAULT_ENCODING = "utf-8"

    def __init__(
        self,
        content: str = "",
        encoding: str = DEFAULT_ENCODING,
        link: t.Optional[Link] = None,
    ) -> None:
        # TODO: add encoding validate
        self._content = content
        super().__init__(
            fp=b"",
            type=ArtifactType.Text,
            display_name=f"{content[:SHORT_VERSION_CNT]}...",
            shape=(),
            mime_type=MIMEType.PLAIN,
            encoding=encoding,
            dtype=numpy.str_,
            link=link,
        )

    @property
    def content(self) -> str:
        if not self._content:
            self._content = self.link_to_content()
        return self._content

    def clear_cache(self) -> None:
        super().clear_cache()
        self._content = ""

    def to_bytes(self, encoding: str = "") -> bytes:
        return self.content.encode(encoding or self.encoding)

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_str(), dtype=self.dtype)

    def to_str(self) -> str:
        return self.content

    def link_to_content(self, encoding: str = "") -> str:
        if self.link:
            return str(self.link.to_bytes(), encoding or self.encoding)
        else:
            return ""


# TODO: support tensorflow transform
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


# TODO: support tensorflow transform
class Link(ASDictMixin, _ResourceOwnerMixin, SwObject):
    def __init__(
        self,
        uri: t.Union[str, Path] = "",
        offset: int = FilePosition.START.value,
        size: int = -1,
        data_type: t.Optional[t.Union[BaseArtifact, t.Dict]] = None,
        use_plain_type: bool = False,
        **kwargs: t.Any,
    ) -> None:
        self._type = "link"
        self.uri = str(uri).strip()
        _up = urlparse(self.uri)
        self.scheme = _up.scheme
        if offset < 0:
            raise FieldTypeOrValueError(f"offset({offset}) must be non-negative")

        self.offset = offset
        self.size = size

        if data_type is not None and not isinstance(data_type, (BaseArtifact, dict)):
            raise TypeError(
                f"data_type({data_type}) is not None or BaseArtifact or dict type"
            )

        if isinstance(data_type, BaseArtifact):
            data_type = data_type.drop_data()
            if use_plain_type:
                data_type = data_type.astype()

        self.data_type = data_type

        self._signed_uri = ""
        self.extra_info = kwargs

    @property
    def signed_uri(self) -> str:
        return self._signed_uri

    @signed_uri.setter
    def signed_uri(self, value: str) -> None:
        self._signed_uri = value

    def astype(self) -> t.Dict[str, t.Any]:
        data_type: t.Dict
        if isinstance(self.data_type, dict):
            data_type = self.data_type
        elif isinstance(self.data_type, BaseArtifact):
            data_type = self.data_type.astype()
        else:
            data_type = {}
        return {
            "type": self._type,
            "data_type": data_type,
        }

    def __str__(self) -> str:
        return f"Link {self.uri}"

    def __repr__(self) -> str:
        return f"Link uri:{self.uri}, offset:{self.offset}, size:{self.size}"

    @http_retry
    def to_bytes(self) -> bytes:
        # TODO: cache store
        from .store import ObjectStore

        key_compose = (
            self,
            self.offset or 0,
            self.size + self.offset - 1 if self.size != -1 else sys.maxsize,
        )
        store = ObjectStore.get_store(self, self.owner)
        with store.backend._make_file(
            key_compose=key_compose, bucket=store.bucket
        ) as f:
            return f.read(self.size)  # type: ignore


class DatasetSummary(ASDictMixin):
    def __init__(
        self,
        rows: int = 0,
        updated_rows: int = 0,
        deleted_rows: int = 0,
        blobs_byte_size: int = 0,
        increased_blobs_byte_size: int = 0,
        **kw: t.Any,
    ) -> None:
        self.rows = rows
        self.updated_rows = updated_rows
        self.deleted_rows = deleted_rows
        self.blobs_byte_size = blobs_byte_size
        self.increased_blobs_byte_size = increased_blobs_byte_size

    def __str__(self) -> str:
        return f"Dataset Summary: rows({self.rows})"

    def __repr__(self) -> str:
        return (
            f"Dataset Summary: rows(total: {self.rows}, updated: {self.updated_rows}, deleted: {self.deleted_rows}), "
            f"size(blobs:{self.blobs_byte_size})"
        )


_size_t = t.Union[int, str, None]


# TODO: use attr to tune code
class DatasetAttr(ASDictMixin):
    def __init__(
        self,
        volume_size: _size_t = D_FILE_VOLUME_SIZE,
        alignment_size: _size_t = D_ALIGNMENT_SIZE,
        **kw: t.Any,
    ) -> None:
        volume_size = D_FILE_VOLUME_SIZE if volume_size is None else volume_size
        alignment_size = D_ALIGNMENT_SIZE if alignment_size is None else alignment_size

        self.volume_size = convert_to_bytes(volume_size)
        self.alignment_size = convert_to_bytes(alignment_size)
        self.kw = kw

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        return super().asdict(ignore_keys=ignore_keys or ["kw"])


# TODO: abstract base class from DatasetConfig and ModelConfig
# TODO: use attr to tune code
class DatasetConfig(ASDictMixin):
    def __init__(
        self,
        name: str = "",
        handler: t.Any = "",
        pkg_data: t.List[str] | None = None,
        exclude_pkg_data: t.List[str] | None = None,
        desc: str = "",
        version: str = DEFAULT_STARWHALE_API_VERSION,
        attr: t.Dict[str, t.Any] | None = None,
        project_uri: str = "",
        runtime_uri: str = "",
        **kw: t.Any,
    ) -> None:
        self.name = name
        self.handler = handler
        self.desc = desc
        self.version = version
        self.attr = DatasetAttr(**(attr or {}))
        self.pkg_data = pkg_data or []
        self.exclude_pkg_data = exclude_pkg_data or []
        self.project_uri = project_uri
        self.runtime_uri = runtime_uri

        self.kw = kw

    def do_validate(self) -> None:
        _ok, _reason = validate_obj_name(self.name)
        if not _ok:
            raise FieldTypeOrValueError(f"name field:({self.name}) error: {_reason}")

        if isinstance(self.handler, str) and ":" not in self.handler:
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

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        d = super().asdict(["handler"])
        d["handler"] = getattr(self.handler, "__name__", None) or str(self.handler)
        return d


class JsonDict(SwObject):
    """
    JsonDict takes json like dict(https://www.json.org/json-en.html) as init parameter
    Besides the standard value types, SwObject is an extra value type that is allowed to be passed in
    """

    def __init__(self, d: dict = {}) -> None:
        for _k, _v in d.items():
            if type(_k) != str:
                raise ValueError(f"json like dict shouldn't have none-str keys {_k}")
            self.__dict__[_k] = JsonDict.from_data(_v)

    @classmethod
    def from_data(cls, d: t.Any) -> t.Any:
        """
        returns JsonDict or primitive values
        SwObject is an extra primitive value
        """
        if isinstance(d, dict):
            return cls(d)
        for _t, _ in _TYPE_DICT.items():
            if isinstance(d, _t):
                return d
        if isinstance(d, SwObject):
            return d
        if type(d) == list:
            return [cls.from_data(_d) for _d in d]
        if type(d) == tuple:
            return tuple(cls.from_data(_d) for _d in d)

        raise ValueError(
            f"json like dict shouldn't have values who's type is not in [SwObject, list, dict, str, int, bool, None], Type: {type(d)}"
        )

    def asdict(self) -> t.Dict:
        ret: t.Dict[str, t.Any] = {}
        for k, v in self.__dict__.items():
            ret[k] = JsonDict.to_data(v)
        return ret

    @classmethod
    def to_data(cls, d: t.Any) -> t.Any:
        """
        unwrap JsonDict to dict
        """
        if isinstance(d, JsonDict):
            return d.asdict()
        if isinstance(d, list):
            return [JsonDict.to_data(_d) for _d in d]
        if isinstance(d, tuple):
            return tuple(JsonDict.to_data(_d) for _d in d)
        return d
