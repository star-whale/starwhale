from __future__ import annotations

import os
import json
import typing as t
import urllib
import threading
from copy import deepcopy
from enum import Enum, unique
from queue import Queue
from types import TracebackType
from functools import partial
from collections import UserDict, defaultdict

import requests
from typing_extensions import Protocol

from starwhale.utils import console, validate_obj_name
from starwhale.consts import ENV_POD_NAME
from starwhale.base.mixin import ASDictMixin, _do_asdict_convert
from starwhale.utils.error import (
    NoSupportError,
    InvalidObjectName,
    FieldTypeOrValueError,
)
from starwhale.utils.retry import http_retry
from starwhale.api._impl.wrapper import Dataset as DatastoreWrapperDataset
from starwhale.api._impl.wrapper import DatasetTableKind
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.type import Link, JsonDict, BaseArtifact
from starwhale.api._impl.data_store import TableEmptyException

DEFAULT_CONSUMPTION_BATCH_SIZE = 50

_DR_DATA_KEY = "data_datastore_revision"
_DR_INFO_KEY = "info_datastore_revision"


class DatastoreRevision(t.NamedTuple):
    data: str
    info: str

    def asdict(self) -> t.Dict:
        return {
            _DR_DATA_KEY: self.data,
            _DR_INFO_KEY: self.info,
        }

    @classmethod
    def from_manifest(cls, manifest: t.Dict | None) -> DatastoreRevision:
        manifest = manifest or {}
        return DatastoreRevision(
            data=manifest.get(_DR_DATA_KEY, ""),
            info=manifest.get(_DR_INFO_KEY, ""),
        )


class TabularDatasetInfo(UserDict):
    _ROW_ID = 0

    def __init__(self, mapping: t.Any = None, **kwargs: t.Any) -> None:
        mapping = mapping or {}
        if not isinstance(mapping, dict):
            raise TypeError(f"data is not dict type: {mapping}")

        if kwargs:
            mapping.update(kwargs)

        converted_mapping: t.Dict[str, t.Any] = {}
        for k, v in mapping.items():
            if not isinstance(k, str):
                raise TypeError(f"key:{k} is not str type")

            # TODO： add validator for value?
            converted_mapping[k] = JsonDict.from_data(v)
        super().__init__(converted_mapping)

    def __getitem__(self, k: str) -> t.Any:
        return JsonDict.to_data(super().__getitem__(k))

    def __setitem__(self, k: str, v: t.Any) -> None:
        if not isinstance(k, str):
            raise TypeError(f"key:{k} is not str type")
        super().__setitem__(k, JsonDict.from_data(v))

    @classmethod
    def load_from_datastore(
        cls, ds_wrapper: DatastoreWrapperDataset
    ) -> TabularDatasetInfo:
        try:
            rows = list(ds_wrapper.scan(cls._ROW_ID, cls._ROW_ID, end_inclusive=True))
            rows_cnt = len(rows)
            if rows_cnt == 1:
                d = rows[0]
            elif rows_cnt > 1:
                raise RuntimeError(f"fetch multi info rows: {rows}")
            else:
                d = {}
        except TableEmptyException:
            d = {}

        d.pop("id", None)
        return cls(d)

    def save_to_datastore(self, ds_wrapper: DatastoreWrapperDataset) -> str:
        ds_wrapper.put(data_id=self._ROW_ID, **self.data)
        return ds_wrapper.flush()


class TabularDatasetRow(ASDictMixin):
    _FEATURES_PREFIX = "features/"

    def __init__(
        self,
        id: t.Union[str, int],
        features: t.Optional[t.Dict[str, t.Any]] = None,
        **kw: t.Union[str, int, float],
    ) -> None:
        self.id = id
        self.features = features or {}
        self.extra_kw = kw
        # TODO: add non-starwhale object store related fields, such as address, authority
        # TODO: add data uri crc for versioning
        self._do_validate()

    @classmethod
    def from_datastore(
        cls,
        id: t.Union[str, int],
        **kw: t.Any,
    ) -> TabularDatasetRow:
        _content = {}
        _extra_kw = {}
        for k, v in kw.items():
            if k.startswith(cls._FEATURES_PREFIX):
                _, name = k.split(cls._FEATURES_PREFIX, 1)
                _content[name] = JsonDict.to_data(v)
            else:
                _extra_kw[k] = v

        return cls(
            id=id,
            features=_content,
            **_extra_kw,
        )

    def __eq__(self, o: object) -> bool:
        s = deepcopy(self.__dict__)
        o = deepcopy(o.__dict__)
        return s == o

    def _do_validate(self) -> None:
        if not isinstance(self.id, (str, int)):
            raise FieldTypeOrValueError(f"id is not int or str type: {self.id}")

        if self.id == "":
            raise FieldTypeOrValueError("id is empty")

        if not isinstance(self.features, dict) or not self.features:
            raise FieldTypeOrValueError("no data field")

    def __str__(self) -> str:
        return f"row-{self.id}"

    def __repr__(self) -> str:
        return f"row-{self.id}" f"{self.features} "

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        d = super().asdict(ignore_keys=ignore_keys or ["features", "extra_kw"])
        d.update(_do_asdict_convert(self.extra_kw))
        for k, v in self.features.items():
            d[f"{self._FEATURES_PREFIX}{k}"] = JsonDict.from_data(v)
        return d

    @classmethod
    def artifacts_of(cls, features: t.Dict) -> t.List[BaseArtifact]:
        artifacts = []
        for v in features.values():
            if isinstance(v, dict):
                artifacts.extend(cls.artifacts_of(v))
            elif isinstance(v, BaseArtifact):
                artifacts.append(v)
        return artifacts

    @property
    def artifacts(self) -> t.List[BaseArtifact]:
        return TabularDatasetRow.artifacts_of(self.features)


_TDType = t.TypeVar("_TDType", bound="TabularDataset")


class TabularDataset:
    def __init__(
        self,
        name: str,
        project: str,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
        instance_name: str = "",
        token: str = "",
        data_datastore_revision: str = "",
        info_datastore_revision: str = "",
    ) -> None:
        _ok, _reason = validate_obj_name(name)
        if not _ok:
            raise InvalidObjectName(f"{name}: {_reason}")
        self.name = name

        self.project = project
        self.instance_name = instance_name

        dwd = partial(
            DatastoreWrapperDataset,
            dataset_name=name,
            project=project,
            instance_name=instance_name,
            token=token,
        )
        self._ds_wrapper = dwd(
            kind=DatasetTableKind.META, dataset_scan_revision=data_datastore_revision
        )
        self._info_ds_wrapper = dwd(
            kind=DatasetTableKind.INFO, dataset_scan_revision=info_datastore_revision
        )

        self._info_changed = False
        self._info: t.Optional[TabularDatasetInfo] = None
        self._info_lock = threading.Lock()

        self.start = start
        self.end = end

    def __str__(self) -> str:
        return f"Dataset Table: {self._ds_wrapper}"

    __repr__ = __str__

    def update(
        self, row_id: t.Union[str, int], **kw: t.Union[int, str, bytes, Link]
    ) -> None:
        self._ds_wrapper.put(row_id, **kw)

    def put(self, row: TabularDatasetRow) -> None:
        self._ds_wrapper.put(row.id, **row.asdict())

    def delete(self, row_id: t.Union[str, int]) -> None:
        self._ds_wrapper.delete(row_id)

    def flush(self) -> t.Tuple[str, str]:
        info_revision = ""
        if self._info is not None and self._info_changed:
            info_revision = self._info.save_to_datastore(self._info_ds_wrapper)

        data_revision = self._ds_wrapper.flush()
        return data_revision, info_revision

    def scan(
        self,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
        end_inclusive: bool = False,
        revision: str = "",
    ) -> t.Generator[TabularDatasetRow, None, None]:
        if start is None or (self.start is not None and start < self.start):
            start = self.start

        if end is None or (self.end is not None and end > self.end):
            end = self.end

        for _d in self._ds_wrapper.scan(start, end, end_inclusive, revision=revision):
            yield TabularDatasetRow.from_datastore(**_d)

    def scan_batch(
        self,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
        batch_size: int = 32,
        revision: str = "",
    ) -> t.Generator[t.List[TabularDatasetRow], None, None]:
        batch = []
        for r in self.scan(start, end, revision=revision):
            batch.append(r)
            if len(batch) % batch_size == 0:
                yield batch
                batch = []
        if batch:
            yield batch

    def close(self) -> None:
        self.flush()
        self._ds_wrapper.close()
        self._info_ds_wrapper.close()

    def __enter__(self: _TDType) -> _TDType:
        return self

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    @classmethod
    def from_uri(
        cls: t.Type[_TDType],
        uri: Resource,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
        data_datastore_revision: str = "",
        info_datastore_revision: str = "",
    ) -> _TDType:
        return cls(
            name=uri.name,
            project=uri.project.name,
            start=start,
            end=end,
            instance_name=uri.instance.url,
            data_datastore_revision=data_datastore_revision,
            info_datastore_revision=info_datastore_revision,
        )

    @property
    def info(self) -> TabularDatasetInfo:
        if self._info is not None:
            return self._info

        with self._info_lock:
            self._info = TabularDatasetInfo.load_from_datastore(self._info_ds_wrapper)
        return self._info

    @info.setter
    def info(self, data: t.Optional[t.Dict[str, t.Any]]) -> None:
        if data is None:
            return

        if not isinstance(data, dict):
            raise TypeError(
                f"data:{type(data)} {data} is not dict type for info update"
            )

        with self._info_lock:
            self._info_changed = True
            self._info = TabularDatasetInfo(data)


@unique
class _RunEnvType(Enum):
    POD = "pod"
    THREAD = "thread"
    PROCESS = "process"


class TabularDatasetSessionConsumption(Protocol):
    batch_size: int

    def get_scan_range(
        self, processed_keys: t.Optional[t.List[t.Tuple[t.Any, t.Any]]] = None
    ) -> t.Optional[t.Tuple[t.Any, t.Any]]:
        ...  # pragma: no cover


local_standalone_tdsc: t.Dict[str, StandaloneTDSC] = {}
lock_s_tdsc = threading.Lock()


def get_dataset_consumption(
    dataset_uri: t.Union[str, Resource],
    session_id: str,
    batch_size: t.Optional[int] = None,
    session_start: t.Optional[t.Any] = None,
    session_end: t.Optional[t.Any] = None,
) -> TabularDatasetSessionConsumption:
    if isinstance(dataset_uri, str):
        dataset_uri = Resource(dataset_uri, typ=ResourceType.dataset)

    batch_size = batch_size or int(
        os.environ.get(
            "DATASET_CONSUMPTION_BATCH_SIZE",
            DEFAULT_CONSUMPTION_BATCH_SIZE,
        )
    )
    if dataset_uri.instance.is_local:
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
        return CloudTDSC(
            dataset_uri=dataset_uri,
            session_id=session_id,
            batch_size=batch_size,
            session_start=session_start,
            session_end=session_end,
        )


class StandaloneTDSC(TabularDatasetSessionConsumption):
    class _BatchTask:
        def __init__(self, start: t.Any, end: t.Any) -> None:
            self.start = start
            self.end = end

    def __init__(
        self,
        dataset_uri: Resource,
        session_id: str,
        batch_size: int = DEFAULT_CONSUMPTION_BATCH_SIZE,
        session_start: t.Optional[t.Any] = None,
        session_end: t.Optional[t.Any] = None,
    ) -> None:
        if not dataset_uri.instance.is_local:
            raise NoSupportError(
                f"StandaloneTDSC only supports standalone dataset: {dataset_uri}"
            )

        self.project = dataset_uri.project
        self.dataset_name = dataset_uri.name
        self.dataset_version = dataset_uri.version

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
        # TODO: support datastore revision
        wrapper = DatastoreWrapperDataset(
            dataset_name=self.dataset_name,
            project=self.project.name,
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
                console.info(
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
            return f"process-{os.getpid()}"  # pragma: no cover


class CloudTDSC(TabularDatasetSessionConsumption):
    def __init__(
        self,
        dataset_uri: Resource,
        session_id: str,
        batch_size: int = DEFAULT_CONSUMPTION_BATCH_SIZE,
        session_start: t.Optional[t.Any] = None,
        session_end: t.Optional[t.Any] = None,
    ) -> None:
        self.instance_uri = dataset_uri.instance.url
        self.instance_token = dataset_uri.instance.token
        self.session_id = session_id
        self.batch_size = batch_size
        self.session_start = session_start
        self.session_end = session_end
        self.dataset_uri = dataset_uri
        self.run_env = _RunEnvType.POD
        self.consumer_id = os.environ.get(ENV_POD_NAME)

        self._do_validate()

    def _do_validate(self) -> None:
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
                f"api/v1/project/{self.dataset_uri.project.name}/dataset/{self.dataset_uri.name}/version/{self.dataset_uri.version}/consume",
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
