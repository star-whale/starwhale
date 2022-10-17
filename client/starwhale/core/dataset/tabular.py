import sys
import json
import typing as t
from copy import deepcopy
from types import TracebackType
from pathlib import Path

import jsonlines
from loguru import logger

from starwhale.utils import console, validate_obj_name
from starwhale.consts import (
    VERSION_PREFIX_CNT,
    STANDALONE_INSTANCE,
    DUMPED_SWDS_META_FNAME,
)
from starwhale.base.uri import URI
from starwhale.base.type import (
    URIType,
    InstanceType,
    DataFormatType,
    DataOriginType,
    ObjectStoreType,
)
from starwhale.base.mixin import ASDictMixin, _do_asdict_convert
from starwhale.utils.error import (
    NotFoundError,
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.core.dataset.store import DatasetStorage


class TabularDatasetRow(ASDictMixin):

    ANNOTATION_PREFIX = "_annotation_"

    def __init__(
        self,
        id: int,
        data_uri: str,
        data_format: DataFormatType = DataFormatType.SWDS_BIN,
        object_store_type: ObjectStoreType = ObjectStoreType.LOCAL,
        data_offset: int = 0,
        data_size: int = 0,
        data_origin: DataOriginType = DataOriginType.NEW,
        data_type: t.Optional[t.Dict[str, t.Any]] = None,
        auth_name: str = "",
        annotations: t.Optional[t.Dict[str, t.Any]] = None,
        **kw: t.Union[str, int, float],
    ) -> None:
        self.id = id
        self.data_uri = data_uri.strip()
        self.data_format = data_format
        self.data_offset = data_offset
        self.data_size = data_size
        self.data_origin = data_origin
        self.object_store_type = object_store_type
        self.auth_name = auth_name
        self.data_type = data_type or {}
        self.annotations = annotations or {}
        self.extra_kw = kw
        # TODO: add non-starwhale object store related fields, such as address, authority
        # TODO: add data uri crc for versioning

    @classmethod
    def from_datastore(
        cls,
        id: int,
        data_uri: str,
        data_format: str = DataFormatType.SWDS_BIN.value,
        object_store_type: str = ObjectStoreType.LOCAL.value,
        data_offset: int = 0,
        data_size: int = 0,
        data_origin: str = DataOriginType.NEW.value,
        data_type: str = "",
        auth_name: str = "",
        **kw: t.Any,
    ) -> "TabularDatasetRow":
        annotations = {}
        extra_kw = {}
        for k, v in kw.items():
            if not k.startswith(cls.ANNOTATION_PREFIX):
                extra_kw[k] = v
                continue
            _, name = k.split(cls.ANNOTATION_PREFIX, 1)
            annotations[name] = json.loads(v)

        return cls(
            id=id,
            data_uri=data_uri,
            data_format=DataFormatType(data_format),
            object_store_type=ObjectStoreType(object_store_type),
            data_offset=data_offset,
            data_size=data_size,
            data_origin=DataOriginType(data_origin),
            auth_name=auth_name,
            # TODO: use protobuf format to store and reflect annotation
            data_type=json.loads(data_type),
            annotations=annotations,
            **extra_kw,
        )

    def __eq__(self, o: object) -> bool:
        s = deepcopy(self.__dict__)
        o = deepcopy(o.__dict__)

        s.pop("data_origin", None)
        o.pop("data_origin", None)
        return s == o

    def _do_validate(self) -> None:
        if self.id < 0:
            raise FieldTypeOrValueError(
                f"id need to be greater than or equal to zero, but current id is {self.id}"
            )

        if not isinstance(self.annotations, dict) or not self.annotations:
            raise FieldTypeOrValueError("no annotations field")

        if not self.data_uri:
            raise FieldTypeOrValueError("no raw_data_uri field")

        if self.data_format not in DataFormatType:
            raise NoSupportError(f"data format: {self.data_format}")

        if self.data_origin not in DataOriginType:
            raise NoSupportError(f"data origin: {self.data_origin}")

        if self.object_store_type not in ObjectStoreType:
            raise NoSupportError(f"object store {self.object_store_type}")

    def __str__(self) -> str:
        return f"row-{self.id}, data-{self.data_uri}, origin-[{self.data_origin}]"

    def __repr__(self) -> str:
        return (
            f"row-{self.id}, data-{self.data_uri}(offset:{self.data_offset}, size:{self.data_size},"
            f"format:{self.data_format}, meta type:{self.data_type}), "
            f"origin-[{self.data_origin}], object store-{self.object_store_type}"
        )

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        d = super().asdict(
            ignore_keys=ignore_keys or ["annotations", "extra_kw", "data_type"]
        )
        d.update(_do_asdict_convert(self.extra_kw))
        # TODO: use protobuf format to store and reflect annotation
        for k, v in self.annotations.items():
            v = _do_asdict_convert(v)
            if getattr(v, "jsonify", None):
                v = v.jsonify()
            else:
                v = json.dumps(v, separators=(",", ":"))
            d[f"{self.ANNOTATION_PREFIX}{k}"] = v
        d["data_type"] = json.dumps(
            _do_asdict_convert(self.data_type), separators=(",", ":")
        )
        return d


_TDType = t.TypeVar("_TDType", bound="TabularDataset")


class TabularDataset:
    def __init__(
        self,
        name: str,
        version: str,
        project: str,
        start: int = 0,
        end: int = sys.maxsize,
    ) -> None:
        self.name = name
        self.version = version
        self.project = project
        self.table_name = f"{name}/{version[:VERSION_PREFIX_CNT]}/{version}"
        self._ds_wrapper = DatastoreWrapperDataset(self.table_name, project)

        self.start = start
        self.end = end

        self._do_validate()

    def _do_validate(self) -> None:
        _ok, _reason = validate_obj_name(self.name)
        if not _ok:
            raise InvalidObjectName(f"{self.name}: {_reason}")

        if not self.version:
            raise FieldTypeOrValueError("no version field")

    def __str__(self) -> str:
        return f"Dataset Table: {self._ds_wrapper}"

    __repr__ = __str__

    def update(self, row_id: int, **kw: t.Union[int, str, bytes]) -> None:
        self._ds_wrapper.put(row_id, **kw)

    def put(self, row: TabularDatasetRow) -> None:
        self._ds_wrapper.put(row.id, **row.asdict())

    def flush(self) -> None:
        self._ds_wrapper.flush()

    def scan(
        self, start: int = 0, end: int = sys.maxsize
    ) -> t.Generator[TabularDatasetRow, None, None]:
        _start = start + self.start
        _end = min(end + self.start, self.end)

        _map_types = {
            "data_format": DataFormatType,
            "data_origin": DataOriginType,
            "object_store_type": ObjectStoreType,
        }
        for _d in self._ds_wrapper.scan(_start, _end):
            for k, v in _map_types.items():
                if k not in _d:
                    continue
                _d[k] = v(_d[k])
            yield TabularDatasetRow.from_datastore(**_d)

    def close(self) -> None:
        self._ds_wrapper.close()

    def __enter__(self: _TDType) -> _TDType:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:
            logger.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    def fork(self, version: str) -> t.Tuple[int, int]:
        fork_td = TabularDataset(name=self.name, version=version, project=self.project)

        rows = 0
        last_row_idx = -1
        # TODO: tune tabular dataset fork performance
        for _row in fork_td.scan():
            rows += 1
            last_row_idx = max(_row.id, last_row_idx)
            _row.data_origin = DataOriginType.INHERIT
            self.put(_row)

        return last_row_idx, rows

    @classmethod
    def from_uri(
        cls: t.Type[_TDType],
        uri: URI,
        start: int = 0,
        end: int = sys.maxsize,
    ) -> _TDType:
        _version = uri.object.version
        if uri.instance_type == InstanceType.STANDALONE:
            _store = DatasetStorage(uri)
            _version = _store.id

        return cls(uri.object.name, _version, uri.project, start=start, end=end)


class StandaloneTabularDataset(TabularDataset):
    class _BytesEncoder(json.JSONEncoder):
        def default(self, obj: t.Any) -> t.Any:
            if isinstance(obj, bytes):
                return obj.decode()
            return json.JSONEncoder.default(self, obj)

    def __init__(
        self,
        name: str,
        version: str,
        project: str,
        start: int = 0,
        end: int = sys.maxsize,
    ) -> None:
        super().__init__(name, version, project, start, end)

        self.uri = URI.capsulate_uri(
            instance=STANDALONE_INSTANCE,
            project=project,
            obj_type=URIType.DATASET,
            obj_name=name,
            obj_ver=version,
        )
        self.store = DatasetStorage(self.uri)

    def dump_meta(self, force: bool = False) -> Path:
        fpath = self.store.snapshot_workdir / DUMPED_SWDS_META_FNAME

        if fpath.exists() and not force:
            console.print(f":blossom: {fpath} existed, skip dump meta")
            return fpath

        console.print(":bear_face: dump dataset meta from data store")

        with jsonlines.open(
            str(fpath), mode="w", dumps=self._BytesEncoder(separators=(",", ":")).encode
        ) as writer:
            for row in self.scan():
                writer.write(row.asdict())

        return fpath

    def load_meta(self) -> None:
        fpath = self.store.snapshot_workdir / DUMPED_SWDS_META_FNAME

        if not fpath.exists():
            raise NotFoundError(fpath)

        console.print(":bird: load dataset meta to standalone data store")
        with jsonlines.open(
            str(fpath),
            mode="r",
        ) as reader:
            for line in reader:
                row = TabularDatasetRow(**line)
                self.put(row)
