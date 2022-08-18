from __future__ import annotations

import typing as t
from copy import deepcopy
from enum import Enum
from pathlib import Path

from starwhale.utils import load_yaml, convert_to_bytes
from starwhale.consts import DEFAULT_STARWHALE_API_VERSION
from starwhale.base.type import DataFormatType, ObjectStoreType
from starwhale.utils.error import NoSupportError


class DSProcessMode:
    DEFINE = "define"
    GENERATE = "generate"


D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 4 * 1024  # 4k for page cache


class DatasetSummary:
    def __init__(
        self,
        rows: int = 0,
        increased_rows: int = 0,
        data_format_type: t.Union[DataFormatType, str] = DataFormatType.UNDEFINED,
        object_store_type: t.Union[ObjectStoreType, str] = ObjectStoreType.UNDEFINED,
        label_byte_size: int = 0,
        data_byte_size: int = 0,
        **kw: t.Any,
    ) -> None:
        self.rows = rows
        self.increased_rows = increased_rows
        self.unchanged_rows = rows - increased_rows
        self.data_format_type: DataFormatType = (
            DataFormatType(data_format_type)
            if isinstance(data_format_type, str)
            else data_format_type
        )
        self.object_store_type: ObjectStoreType = (
            ObjectStoreType(object_store_type)
            if isinstance(object_store_type, str)
            else object_store_type
        )
        self.label_byte_size = label_byte_size
        self.data_byte_size = data_byte_size

    def as_dict(self) -> t.Dict[str, t.Any]:
        d = deepcopy(self.__dict__)
        for k, v in d.items():
            if isinstance(v, Enum):
                d[k] = v.value
        return d

    def __str__(self) -> str:
        return f"Dataset Summary: rows({self.rows}), data_format({self.data_format_type}), object_store({self.object_store_type})"

    def __repr__(self) -> str:
        return (
            f"Dataset Summary: rows({self.rows}, increased: {self.increased_rows}), "
            f"data_format({self.data_format_type}), object_store({self.object_store_type}),"
            f"size(data:{self.data_byte_size}, label: {self.label_byte_size})"
        )


# TODO: use attr to tune code
class DatasetAttr:
    def __init__(
        self,
        volume_size: t.Union[int, str] = D_FILE_VOLUME_SIZE,
        alignment_size: t.Union[int, str] = D_ALIGNMENT_SIZE,
        **kw: t.Any,
    ) -> None:
        self.volume_size = convert_to_bytes(volume_size)
        self.alignment_size = convert_to_bytes(alignment_size)
        self.kw = kw

    def as_dict(self) -> t.Dict[str, t.Any]:
        _rd = deepcopy(self.__dict__)
        _rd.pop("kw")
        return _rd


# TODO: abstract base class from DataSetConfig and ModelConfig
# TODO: use attr to tune code
class DatasetConfig:
    def __init__(
        self,
        name: str,
        data_dir: str,
        process: str,
        mode: str = DSProcessMode.GENERATE,
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
        self.mode = mode
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
        if self.mode not in (DSProcessMode.DEFINE, DSProcessMode.GENERATE):
            raise NoSupportError(f"{self.mode} mode no support")

        if ":" not in self.process:
            raise Exception(
                f"please use module:class format, current is: {self.process}"
            )

        # TODO: add more validator

    def __str__(self) -> str:
        return f"DataSet Config {self.name}"

    def __repr__(self) -> str:
        return f"DataSet Config {self.name}, mode:{self.mode}, data:{self.data_dir}"

    def as_dict(self) -> t.Dict[str, t.Any]:
        _r = deepcopy(self.__dict__)
        _r["attr"] = self.attr.as_dict()
        return _r

    @classmethod
    def create_by_yaml(cls, fpath: t.Union[str, Path]) -> DatasetConfig:
        c = load_yaml(fpath)

        return cls(**c)
