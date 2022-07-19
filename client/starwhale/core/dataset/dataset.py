from __future__ import annotations

import typing as t
from copy import deepcopy
from pathlib import Path

from starwhale.utils import load_yaml, convert_to_bytes
from starwhale.consts import SWDSSubFileType, DEFAULT_STARWHALE_API_VERSION
from starwhale.utils.error import NoSupportError


class DSProcessMode:
    DEFINE = "define"
    GENERATE = "generate"


D_FILE_VOLUME_SIZE = 64 * 1024 * 1024  # 64MB
D_ALIGNMENT_SIZE = 4 * 1024  # 4k for page cache
D_USER_BATCH_SIZE = 1
ARCHIVE_SWDS_META = "archive.%s" % SWDSSubFileType.META


# TODO: use attr to tune code
class DatasetAttr:
    def __init__(
        self,
        volume_size: t.Union[int, str] = D_FILE_VOLUME_SIZE,
        alignment_size: t.Union[int, str] = D_ALIGNMENT_SIZE,
        batch_size: int = D_USER_BATCH_SIZE,
        **kw: t.Any,
    ) -> None:
        self.batch_size = batch_size
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
