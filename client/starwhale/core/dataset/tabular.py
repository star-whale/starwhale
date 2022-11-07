from __future__ import annotations

import os
import sys
import json
import typing as t
import urllib
import threading
from copy import deepcopy
from enum import Enum, unique
from queue import Queue
from types import TracebackType
from pathlib import Path
from collections import defaultdict

import requests
import jsonlines
from loguru import logger
from typing_extensions import Protocol

from starwhale.utils import console, validate_obj_name
from starwhale.consts import (
    ENV_POD_NAME,
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
from starwhale.consts.env import SWEnv
from starwhale.utils.error import (
    FormatError,
    NotFoundError,
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.utils.retry import http_retry
from starwhale.utils.config import SWCliConfigMixed
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.core.dataset.store import DatasetStorage

DEFAULT_CONSUMPTION_BATCH_SIZE = 50


class TabularDatasetRow(ASDictMixin):

    ANNOTATION_PREFIX = "_annotation_"

    def __init__(
        self,
        id: t.Union[str, int],
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
        self._do_validate()

    @classmethod
    def from_datastore(
        cls,
        id: t.Union[str, int],
        data_uri: str,
        data_format: str = DataFormatType.SWDS_BIN.value,
        object_store_type: str = ObjectStoreType.LOCAL.value,
        data_offset: int = 0,
        data_size: int = 0,
        data_origin: str = DataOriginType.NEW.value,
        data_type: str = "",
        auth_name: str = "",
        **kw: t.Any,
    ) -> TabularDatasetRow:
        _annotations = {}
        _extra_kw = {}
        for k, v in kw.items():
            if not k.startswith(cls.ANNOTATION_PREFIX):
                _extra_kw[k] = v
                continue
            _, name = k.split(cls.ANNOTATION_PREFIX, 1)
            _annotations[name] = json.loads(v)

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
            annotations=_annotations,
            **_extra_kw,
        )

    def __eq__(self, o: object) -> bool:
        s = deepcopy(self.__dict__)
        o = deepcopy(o.__dict__)

        s.pop("data_origin", None)
        o.pop("data_origin", None)
        return s == o

    def _do_validate(self) -> None:
        if not isinstance(self.id, (str, int)):
            raise FieldTypeOrValueError(f"id is not int or str type: {self.id}")

        if self.id == "":
            raise FieldTypeOrValueError("id is empty")

        if not isinstance(self.annotations, dict) or not self.annotations:
            raise FieldTypeOrValueError("no annotations field")

        if not self.data_uri:
            raise FieldTypeOrValueError("no raw_data_uri field")

        if not isinstance(self.data_format, DataFormatType):
            raise NoSupportError(f"data format: {self.data_format}")

        if not isinstance(self.data_origin, DataOriginType):
            raise NoSupportError(f"data origin: {self.data_origin}")

        if not isinstance(self.object_store_type, ObjectStoreType):
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
    _map_types = {
        "data_format": DataFormatType,
        "data_origin": DataOriginType,
        "object_store_type": ObjectStoreType,
    }

    def __init__(
        self,
        name: str,
        version: str,
        project: str,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
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

    def update(self, row_id: t.Union[str, int], **kw: t.Union[int, str, bytes]) -> None:
        self._ds_wrapper.put(row_id, **kw)

    def put(self, row: TabularDatasetRow) -> None:
        self._ds_wrapper.put(row.id, **row.asdict())

    def flush(self) -> None:
        self._ds_wrapper.flush()

    def scan(
        self,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
    ) -> t.Generator[TabularDatasetRow, None, None]:
        if start is None or (self.start is not None and start < self.start):
            start = self.start

        if end is None or (self.end is not None and end > self.end):
            end = self.end

        for _d in self._ds_wrapper.scan(start, end):
            for k, v in self._map_types.items():
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

        rows_cnt = 0
        last_append_seq_id = -1
        # TODO: tune tabular dataset fork performance
        for _row in fork_td.scan():
            rows_cnt += 1
            last_append_seq_id = max(
                int(_row.extra_kw.get("_append_seq_id", -1)), last_append_seq_id
            )
            _row.data_origin = DataOriginType.INHERIT
            self.put(_row)

        return last_append_seq_id, rows_cnt

    @classmethod
    def from_uri(
        cls: t.Type[_TDType],
        uri: URI,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
    ) -> _TDType:
        _version = uri.object.version
        if uri.instance_type == InstanceType.STANDALONE:
            _store = DatasetStorage(uri)
            _version = _store.id

        return cls(uri.object.name, _version, uri.project, start=start, end=end)


@unique
class _RunEnvType(Enum):
    POD = "pod"
    THREAD = "thread"
    PROCESS = "process"


class TabularDatasetSessionConsumption(Protocol):
    def get_scan_range(
        self, processed_keys: t.Optional[t.List[t.Tuple[t.Any, t.Any]]] = None
    ) -> t.Optional[t.Tuple[t.Any, t.Any]]:
        ...


local_standalone_tdsc: t.Dict[str, StandaloneTDSC] = {}
lock_s_tdsc = threading.Lock()


def get_dataset_consumption(
    dataset_uri: t.Union[str, URI],
    session_id: str,
    batch_size: t.Optional[int] = None,
    session_start: t.Optional[t.Any] = None,
    session_end: t.Optional[t.Any] = None,
    instance_uri: str = "",
    instance_token: str = "",
) -> TabularDatasetSessionConsumption:
    # TODO: tune factory class arguments
    _uri = instance_uri or os.environ.get(SWEnv.instance_uri)

    if isinstance(dataset_uri, str):
        dataset_uri = URI(dataset_uri, expected_type=URIType.DATASET)

    batch_size = batch_size or int(
        os.environ.get(
            "DATASET_CONSUMPTION_BATCH_SIZE",
            DEFAULT_CONSUMPTION_BATCH_SIZE,
        )
    )
    if _uri is None or _uri == "local":
        global local_standalone_tdsc
        key = f"{dataset_uri}-{session_id}-{session_start}-{session_end}-{batch_size}"
        with lock_s_tdsc:
            _obj = local_standalone_tdsc.get(key)
            if not _obj:
                _obj = StandaloneTDSC(
                    dataset_uri=dataset_uri,
                    session_id=session_id,
                    batch_size=batch_size,
                    session_start=session_start,
                    session_end=session_end,
                )
                local_standalone_tdsc[key] = _obj
            return _obj
    else:
        _token = (
            instance_token
            or SWCliConfigMixed().get_sw_token(instance=_uri)
            or os.getenv(SWEnv.instance_token, "")
        )
        return CloudTDSC(
            instance_uri=_uri,
            dataset_uri=dataset_uri,
            session_id=session_id,
            batch_size=batch_size,
            session_start=session_start,
            session_end=session_end,
            instance_token=_token,
        )


class StandaloneTDSC(TabularDatasetSessionConsumption):
    class _BatchTask:
        def __init__(self, start: t.Any, end: t.Any) -> None:
            self.start = start
            self.end = end

    def __init__(
        self,
        dataset_uri: URI,
        session_id: str,
        batch_size: int = DEFAULT_CONSUMPTION_BATCH_SIZE,
        session_start: t.Optional[t.Any] = None,
        session_end: t.Optional[t.Any] = None,
    ) -> None:
        if dataset_uri.instance_type != InstanceType.STANDALONE:
            raise NoSupportError(
                f"StandaloneTDSC only supports standalone dataset: {dataset_uri}"
            )

        self.project = dataset_uri.project
        self.dataset_name = dataset_uri.object.name
        self.dataset_version = DatasetStorage(dataset_uri).id

        self.session_id = session_id
        if batch_size <= 0:
            raise FieldTypeOrValueError(f"batch_size is invalid: {batch_size}")
        self.batch_size = batch_size
        self.session_start = session_start
        self.session_end = session_end
        self.run_env = _RunEnvType.THREAD

        self._lock = threading.Lock()
        self._todo_queue = self._init_dataset_todo_queue()
        self._doing_consumption: t.Dict[str, t.Dict[str, t.Any]] = defaultdict(dict)

        # TODO: support sidecar thread monitor
        # TODO: support max_retries

    def _init_dataset_todo_queue(self) -> Queue:
        wrapper = DatastoreWrapperDataset(
            f"{self.dataset_name}/{self.dataset_version[:VERSION_PREFIX_CNT]}/{self.dataset_version}",
            self.project,
        )
        ids = [i["id"] for i in wrapper.scan_id(self.session_start, self.session_end)]
        id_cnt = len(ids)
        queue: Queue[StandaloneTDSC._BatchTask] = Queue(maxsize=id_cnt)
        for start in range(0, id_cnt, self.batch_size):
            end = start + self.batch_size
            # TODO: add put block?
            start_key = ids[start]
            end_key = ids[end] if end < id_cnt else None
            queue.put(StandaloneTDSC._BatchTask(start=start_key, end=end_key))
        return queue

    def get_scan_range(
        self,
        processed_keys: t.Optional[t.List[t.Tuple[t.Any, t.Any]]] = None,
    ) -> t.Optional[t.Tuple[t.Any, t.Any]]:
        processed_keys = processed_keys or []
        consumer_id = self.consumer_id

        with self._lock:
            consumer: t.Dict = self._doing_consumption.get(consumer_id, {})
            if consumer:
                for k in processed_keys:
                    if not k:
                        continue
                    consumer.pop(f"{k[0]}-{k[1]}", None)

            if not self._todo_queue.empty():
                task: StandaloneTDSC._BatchTask = self._todo_queue.get()
                self._doing_consumption[consumer_id][f"{task.start}-{task.end}"] = task
                logger.info(
                    f"{consumer_id} handle scan-range: ({task.start}, {task.end})"
                )
                return task.start, task.end

            for cid in list(self._doing_consumption.keys()):
                if len(self._doing_consumption[cid]) == 0:
                    self._doing_consumption.pop(cid, None)

        return None

    def __str__(self) -> str:
        return f"[Standalone]Dataset Consumption: id-{self.session_id}, range-[{self.session_start},{self.session_end}], consumer:{self.consumer_id}@{self.run_env}"

    def __repr__(self) -> str:
        return f"[Standalone]Dataset Consumption: id-{self.session_id}, range-[{self.session_start},{self.session_end}], consumer:{self.consumer_id}@{self.run_env}, batch:{self.batch_size}"

    @property
    def consumer_id(self) -> str:
        if self.run_env == _RunEnvType.THREAD:
            return f"thread-{id(threading.current_thread())}"
        else:
            return f"process-{os.getpid()}"


class CloudTDSC(TabularDatasetSessionConsumption):
    def __init__(
        self,
        instance_uri: str,
        dataset_uri: URI,
        session_id: str,
        batch_size: int = DEFAULT_CONSUMPTION_BATCH_SIZE,
        session_start: t.Optional[t.Any] = None,
        session_end: t.Optional[t.Any] = None,
        instance_token: str = "",
    ) -> None:
        self.instance_uri = instance_uri
        self.instance_token = instance_token
        self.session_id = session_id
        self.batch_size = batch_size
        self.session_start = session_start
        self.session_end = session_end
        self.dataset_uri = dataset_uri
        self.run_env = _RunEnvType.POD
        self.consumer_id = os.environ.get(ENV_POD_NAME)

        self._do_validate()

    def _do_validate(self) -> None:
        if not self.instance_token:
            raise FieldTypeOrValueError("instance token is empty")

        if (
            not self.dataset_uri.project
            or not self.dataset_uri.object.name
            or not self.dataset_uri.object.version
        ):
            raise FormatError(f"wrong dataset uri format: {self.dataset_uri}")

        if not self.consumer_id:
            raise RuntimeError("failed to get pod name")

    @http_retry
    def get_scan_range(
        self, processed_keys: t.Optional[t.List[t.Tuple[t.Any, t.Any]]] = None
    ) -> t.Optional[t.Tuple[t.Any, t.Any]]:
        post_data = {
            "batchSize": self.batch_size,
            "sessionId": self.session_id,
            "consumerId": self.consumer_id,
        }
        if processed_keys is not None:
            post_data["processedData"] = [
                {"start": p[0], "end": p[1]} for p in processed_keys
            ]

        if self.session_start is not None:
            post_data["start"] = self.session_start

        if self.session_end is not None:
            post_data["end"] = self.session_end

        resp = requests.post(
            urllib.parse.urljoin(
                self.instance_uri,
                f"api/v1/project/{self.dataset_uri.project}/dataset/{self.dataset_uri.object.name}/version/{self.dataset_uri.object.version}/consume",
            ),
            data=json.dumps(post_data),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": self.instance_token,  # type: ignore
            },
            timeout=300,
        )
        resp.raise_for_status()
        range_data = resp.json()["data"]

        return None if range_data is None else (range_data["start"], range_data["end"])

    def __str__(self) -> str:
        return (
            f"[Cloud:{self.instance_uri}]Dataset Consumption: id-{self.session_id}, "
            f"range-[{self.session_start},{self.session_end}], consumer:{self.consumer_id}@{self.run_env}"
        )

    def __repr__(self) -> str:
        return (
            f"[Cloud:{self.instance_uri}]Dataset Consumption: id-{self.session_id}, "
            f"range-[{self.session_start},{self.session_end}], consumer:{self.consumer_id}@{self.run_env}, "
            f"batch-{self.batch_size}, dataset-{self.dataset_uri}"
        )


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
                row = TabularDatasetRow.from_datastore(**line)
                self.put(row)
