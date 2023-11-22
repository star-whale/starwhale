from __future__ import annotations

import io
import os
import sys
import base64
import typing as t
from abc import ABCMeta
from enum import Enum, unique
from pathlib import Path
from urllib.parse import urlparse

import numpy

from starwhale.consts import SHORT_VERSION_CNT
from starwhale.utils.fs import DIGEST_SIZE, FilePosition
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.error import (
    NoSupportError,
    FieldTypeOrValueError,
    MissingDependencyError,
)
from starwhale.utils.retry import http_retry
from starwhale.base.uri.instance import Instance
from starwhale.api._impl.data_store import SwObject, _TYPE_DICT

_T = t.TypeVar("_T")
_TupleOrList = t.Union[t.Tuple[_T, ...], t.List[_T]]
_TShape = _TupleOrList[t.Optional[int]]
_TArtifactFP = t.Union[str, bytes, Path, io.IOBase]
_TBAType = t.TypeVar("_TBAType", bound="BaseArtifact")


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


@unique
class ArtifactType(Enum):
    Binary = "binary"
    Image = "image"
    Video = "video"
    Audio = "audio"
    Text = "text"
    Link = "link"
    Unknown = "unknown"


class BaseArtifact(ASDictMixin, metaclass=ABCMeta):
    # "fp" and "__cache_bytes" are the runtime attributes that are not saved in the datastore
    __slots__ = ("fp", "__cache_bytes", "_content")

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

    def prepare_link(self, instance: Instance) -> None:
        if self.link:
            self.link.instance = instance

    __repr__ = __str__


class Binary(BaseArtifact, SwObject):
    # TODO: use the better way to calculate the min size
    # Detect if the bytes is too long to encode to Binary for the datastore efficiency
    # size = DIGEST_SIZE + Binary Struct size + Link Object Struct size
    AUTO_ENCODE_MIN_SIZE = sys.getsizeof(DIGEST_SIZE) + 1024

    def __init__(
        self,
        fp: _TArtifactFP = b"",
        mime_type: MIMEType = MIMEType.UNDEFINED,
        dtype: t.Type = numpy.bytes_,
        link: t.Optional[Link] = None,
        auto_convert_to_bytes: bool = False,
    ) -> None:
        self.auto_convert_to_bytes = auto_convert_to_bytes
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
        fp: _TArtifactFP = "",
        dtype: t.Type = numpy.uint8,
        shape: _TShape | None = None,
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

    def to_pil(self, mode: str | None = None) -> t.Any:
        """Convert Starwhale Image to Pillow Image.

        Arguments:
            mode: (str, optional) The mode of Pillow Image, default is None. If the argument is None, the mode is the same as Image original mode.
                If the argument is not None, we will call Pillow convert method to convert the mode.
                The mode list is https://pillow.readthedocs.io/en/stable/handbook/concepts.html#concept-modes.

        Returns:
            Pillow Image.
        """
        try:
            from PIL import Image as PILImage
        except ImportError as e:  # pragma: no cover
            raise MissingDependencyError(
                "pillow is required to convert Starwhale Image to Pillow Image, please install pillow with 'pip install pillow' or 'pip install starwhale[image]'."
            ) from e

        img = PILImage.open(io.BytesIO(self.to_bytes()))
        if mode is not None:
            img = img.convert(mode)
        return img

    def to_numpy(self, mode: str | None = None) -> numpy.ndarray:
        """Convert Starwhale Image to numpy ndarray.

        Starwhale Image -> Pillow Image -> Numpy ndarray

        Arguments:
            mode: (str, optional) The mode of Pillow Image, default is None.
                The argument is the same as `to_pil` method.

        """
        return numpy.array(self.to_pil(mode))

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
        except ImportError as e:  # pragma: no cover
            raise MissingDependencyError(
                "soundfile is required to convert Starwhale Auto to numpy ndarray, please install soundfile with 'pip install soundfile' or 'pip install starwhale[audio]'."
            ) from e

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


class Sequence(ASDictMixin, SwObject):
    """Datastore does not support mixed type list or tuple, so we need to wrap it with Sequence type."""

    _supported_types = {
        "list": list,
        "tuple": tuple,
    }
    _prefix = "i"

    def __init__(
        self, data: list | tuple | None = None, auto_convert: bool = False
    ) -> None:
        data = [] if data is None else data

        self._type = "sequence"

        self.sequence_type = type(data).__name__
        if self.sequence_type not in self._supported_types:
            raise NoSupportError(f"Sequence type: {self.sequence_type}")

        _dict_data = {f"{self._prefix}{idx}": item for idx, item in enumerate(data)}
        self._cnt = len(_dict_data)
        self.data = JsonDict.from_data(_dict_data)
        self.auto_convert = auto_convert

    def __str__(self) -> str:
        return f"Sequence[{self.sequence_type}] with {self._cnt} items"

    def __repr__(self) -> str:
        return f"Sequence: {self.to_raw_data()}"

    def __len__(self) -> int:
        return self._cnt

    def __bool__(self) -> bool:
        return self._cnt != 0

    def to_raw_data(self) -> t.Any:
        return self._supported_types[self.sequence_type](self)

    def __iter__(self) -> t.Iterator[t.Any]:
        _unwrap_data = self.data.asdict()
        for i in range(0, self._cnt):
            yield _unwrap_data[f"{self._prefix}{i}"]


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

    def __init__(
        self, bbox_a: BoundingBox | None = None, bbox_b: BoundingBox | None = None
    ) -> None:
        bbox_a = bbox_a or BoundingBox()
        bbox_b = bbox_b or BoundingBox()

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
    def __init__(self, points: t.List[Point] | None = None) -> None:
        points = points or []
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
    def __init__(self, points: t.List[Point] | None = None) -> None:
        points = points or []
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
    # TODO: use the better way to calculate the min size
    # Detect if the str is too long to encode to Text for the datastore efficiency
    # size = DIGEST_SIZE + Text Struct size + Link Object Struct size
    AUTO_ENCODE_MIN_SIZE = sys.getsizeof(DIGEST_SIZE) + 1024

    # "_content" is the runtime attributes that is not saved in the datastore
    __slots__ = ("_content",)

    def __init__(
        self,
        content: str = "",
        encoding: str = DEFAULT_ENCODING,
        link: t.Optional[Link] = None,
        auto_convert_to_str: bool = False,
    ) -> None:
        # TODO: add encoding validate
        self._content = content
        self.auto_convert_to_str = auto_convert_to_str
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
            self._content = str(self.fetch_data(), self.encoding) if self.link else ""
        return self._content

    def to_bytes(self, encoding: str = "") -> bytes:
        return self.content.encode(encoding or self.encoding)

    def to_numpy(self) -> numpy.ndarray:
        return numpy.array(self.to_str(), dtype=self.dtype)

    def to_str(self) -> str:
        return self.content

    def __str__(self) -> str:
        return self.content

    __repr__ = __str__


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
class Link(ASDictMixin, SwObject):
    # "_signed_uri" and "_instance" are the runtime attributes that are not saved in the datastore
    __slots__ = ("_signed_uri", "_instance")

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
        self.extra_info = kwargs

    @property
    def signed_uri(self) -> str:
        return getattr(self, "_signed_uri", "")

    @signed_uri.setter
    def signed_uri(self, value: str) -> None:
        self._signed_uri = value

    @property
    def instance(self) -> Instance | None:
        return getattr(self, "_instance", None)

    @instance.setter
    def instance(self, value: Instance | None) -> None:
        self._instance = value

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
        from starwhale.core.dataset.store import ObjectStore

        key_compose = (
            self,
            self.offset or 0,
            self.size + self.offset - 1 if self.size != -1 else sys.maxsize,
        )
        store = ObjectStore.get_store(self)
        with store.backend._make_file(
            key_compose=key_compose, bucket=store.bucket
        ) as f:
            return f.read(self.size)  # type: ignore


class JsonDict(SwObject):
    """
    JsonDict takes json like dict(https://www.json.org/json-en.html) as init parameter
    Besides the standard value types, SwObject is an extra value type that is allowed to be passed in
    """

    def __init__(self, d: dict | None = None) -> None:
        d = d or {}
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
            f"json like dict shouldn't have values who's type is not in [SwObject, list, dict, scalar], Type: {type(d)}"
        )

    def asdict(self) -> t.Dict:
        ret: t.Dict[str, t.Any] = {}
        for k, v in self.__dict__.items():
            ret[k] = JsonDict.to_data(v)
        return ret

    @classmethod
    def to_data(cls, d: t.Any) -> t.Any:
        """unwrap JsonDict to dict"""
        if isinstance(d, JsonDict):
            return d.asdict()
        if isinstance(d, list):
            return [JsonDict.to_data(_d) for _d in d]
        if isinstance(d, tuple):
            return tuple(JsonDict.to_data(_d) for _d in d)
        return d
