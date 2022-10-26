from __future__ import annotations

import io
import os
import base64
import typing as t
from abc import ABCMeta, abstractmethod
from enum import Enum, unique
from pathlib import Path
from functools import partial

from starwhale.utils import load_yaml, convert_to_bytes, validate_obj_name
from starwhale.consts import (
    LATEST_TAG,
    SHORT_VERSION_CNT,
    DEFAULT_STARWHALE_API_VERSION,
)
from starwhale.utils.fs import FilePosition
from starwhale.base.mixin import ASDictMixin
from starwhale.utils.error import NoSupportError, FieldTypeOrValueError

D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 4 * 1024  # 4k for page cache


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
        type: ArtifactType,
        display_name: str = "",
        shape: t.Optional[_TShape] = None,
        mime_type: t.Optional[MIMEType] = None,
        encoding: str = "",
    ) -> None:
        self.fp = fp
        self.type = ArtifactType(type)

        _fpath = str(fp) if isinstance(fp, (Path, str)) and fp else ""
        self.display_name = display_name or os.path.basename(_fpath)
        self.mime_type = mime_type or MIMEType.create_by_file_suffix(_fpath)
        self.shape = shape
        self.encoding = encoding
        self._do_validate()

    def _do_validate(self) -> None:
        ...

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


class Binary(BaseArtifact):
    def __init__(
        self,
        fp: _TArtifactFP,
        mime_type: MIMEType = MIMEType.UNDEFINED,
    ) -> None:
        super().__init__(fp, ArtifactType.Binary, "", (1,), mime_type)


class Image(BaseArtifact):
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


# TODO: support Video type


class Audio(BaseArtifact):
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


class ClassLabel(ASDictMixin):
    def __init__(self, names: t.List[t.Union[int, float, str]]) -> None:
        self.type = "class_label"
        self.names = names

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
class BoundingBox(ASDictMixin):
    def __init__(self, x: float, y: float, width: float, height: float) -> None:
        self.type = "bounding_box"
        self.x = x
        self.y = y
        self.width = width
        self.height = height

    def to_list(self) -> t.List[float]:
        return [self.x, self.y, self.width, self.height]

    def __str__(self) -> str:
        return f"BoundingBox: point:({self.x}, {self.y}), width: {self.width}, height: {self.height})"

    __repr__ = __str__


class Text(BaseArtifact):
    DEFAULT_ENCODING = "utf-8"

    def __init__(self, content: str, encoding: str = DEFAULT_ENCODING) -> None:
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
class COCOObjectAnnotation(ASDictMixin):
    def __init__(
        self,
        id: int,
        image_id: int,
        category_id: int,
        segmentation: t.Union[t.List, t.Dict],
        area: t.Union[float, int],
        bbox: t.Union[BoundingBox, t.List[float]],
        iscrowd: int,
    ) -> None:
        self.type = "coco_object_annotation"
        self.id = id
        self.image_id = image_id
        self.category_id = category_id
        self.bbox = bbox.to_list() if isinstance(bbox, BoundingBox) else bbox
        self.segmentation = segmentation
        self.area = area
        self.iscrowd = iscrowd

        self.do_validate()

    def do_validate(self) -> None:
        if self.iscrowd not in (0, 1):
            raise FieldTypeOrValueError(f"iscrowd({self.iscrowd}) only accepts 0 or 1")

        # TODO: iscrowd=0 -> polygons, iscrowd=1 -> RLE validate


class Link(ASDictMixin):
    def __init__(
        self,
        uri: str,
        auth: t.Optional[LinkAuth] = DefaultS3LinkAuth,
        offset: int = FilePosition.START,
        size: int = -1,
        data_type: t.Optional[BaseArtifact] = None,
        with_local_fs_data: bool = False,
    ) -> None:
        self.type = ArtifactType.Link
        self.uri = uri.strip()
        self.offset = offset
        self.size = size
        self.auth = auth
        self.data_type = data_type
        self.with_local_fs_data = with_local_fs_data

        self.do_validate()

    def do_validate(self) -> None:
        if self.offset < 0:
            raise FieldTypeOrValueError(f"offset({self.offset}) must be non-negative")

        if self.size < -1:
            raise FieldTypeOrValueError(f"size({self.size}) must be non-negative or -1")

    def astype(self) -> t.Dict[str, t.Any]:
        return {
            "type": self.type,
            "data_type": self.data_type.astype() if self.data_type else {},
        }

    def __str__(self) -> str:
        return f"Link {self.uri}"

    def __repr__(self) -> str:
        return f"Link uri:{self.uri}, offset:{self.offset}, size:{self.size}, data type:{self.data_type}, with localFS data:{self.with_local_fs_data}"


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
