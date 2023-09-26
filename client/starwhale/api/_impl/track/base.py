from __future__ import annotations

import copy
import time
import typing as t
from enum import Enum, unique

from starwhale.utils import now_str
from starwhale.base.mixin import ASDictMixin
from starwhale.base.data_type import Link


@unique
class _TrackMode(Enum):
    OFFLINE = "offline"
    ONLINE = "online"


@unique
class _TrackType(Enum):
    METRICS = "metrics"
    PARAMETERS = "parameters"
    ARTIFACTS = "artifacts"


@unique
class _TrackSource(Enum):
    USER = "user"
    SYSTEM = "_system"
    FRAMEWORK = "_framework"


class TrackRecord:
    def __init__(
        self, typ: _TrackType, source: _TrackSource, start_time: float = 0
    ) -> None:
        self.typ = typ
        self.source = source
        self.clock_time = now_str()
        self.relative_time = time.monotonic() - start_time

    def __str__(self) -> str:
        return f"TrackRecord[{self.typ}] from {self.source}"


class MetricsRecord(TrackRecord):
    def __init__(
        self,
        data: t.Dict[str, float],
        step: int,
        source: _TrackSource = _TrackSource.USER,
        start_time: float = 0,
    ) -> None:
        super().__init__(_TrackType.METRICS, source, start_time)
        self.data = data
        self.step = step

    def __str__(self) -> str:
        return f"MetricsRecord[{self.step}] {self.data}"

    __repr__ = __str__


class ParamsRecord(TrackRecord):
    def __init__(
        self,
        data: t.Dict[str, t.Any],
        source: _TrackSource = _TrackSource.USER,
        start_time: float = 0,
    ) -> None:
        super().__init__(_TrackType.PARAMETERS, source, start_time)
        self.data = data

    def __str__(self) -> str:
        return f"ParamsRecord-{self.data}"

    __repr__ = __str__


class ArtifactsRecord(TrackRecord):
    def __init__(
        self,
        data: t.Dict[str, t.Any],
        source: _TrackSource = _TrackSource.USER,
        start_time: float = 0,
    ) -> None:
        super().__init__(_TrackType.ARTIFACTS, source, start_time)
        self.data = data

    def __str__(self) -> str:
        return f"ArtifactsRecord-{self.data}"

    __repr__ = __str__


class ArtifactsTabularRow(ASDictMixin):
    def __init__(
        self,
        name: str,
        index: int,
        created_at: str,
        data: Link,
        link_wrapper: bool = True,
    ) -> None:
        self.name = name
        self.index = index
        self.key = f"{name}-{index:04}"
        self.created_at = created_at
        self.data = data
        self._link_wrapper = link_wrapper

        self._do_validate()

    def _do_validate(self) -> None:
        if not isinstance(self.data, Link):
            raise TypeError(f"data:{self.data} is not Link type")

        if self.index < 0 or self.index >= 10000:
            raise ValueError(
                f"index({self.index}) must be non negative integer number and less than 10000"
            )

    def __str__(self) -> str:
        return f"ArtifactsTabularRow-{self.key}"

    def __repr__(self) -> str:
        return f"ArtifactsTabularRow-{self.key}-{self.created_at}-{self.data}"

    @classmethod
    def from_datastore(cls, **kw: t.Any) -> ArtifactsTabularRow:
        return cls(
            name=kw["name"],
            index=kw["index"],
            created_at=kw["created_at"],
            data=kw["data"],
            link_wrapper=kw.get("__link_wrapper", True),
        )

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        return {
            "__key": self.key,
            "__link_wrapper": self._link_wrapper,
            "name": self.name,
            "index": self.index,
            "data": self.data,
        }


class MetricsTabularRow(ASDictMixin):
    def __init__(
        self,
        step: int,
        clock_time: str,
        relative_time: float,
        metrics: t.Dict[str, float],
    ) -> None:
        self.step = step
        self.clock_time = clock_time
        self.relative_time = float(relative_time)
        self.metrics = metrics
        # TODO: add synced flag for syncer thread?
        self._do_validate()

    def _do_validate(self) -> None:
        if self.step < 0:
            raise TypeError(f"step({self.step}) should be non-negative integer number")

        for k, v in self.metrics.items():
            if not isinstance(k, str) or not isinstance(v, float):
                raise TypeError(
                    f"metric(k:{k}, v:{v}) should be str key and float value."
                )

    def __str__(self) -> str:
        return f"MetricsTabularRow[{self.step}] {self.metrics}"

    def __repr__(self) -> str:
        return f"MetricsTabularRow[{self.step}] {self.metrics} at {self.clock_time}, relative time({self.relative_time}s)"

    @classmethod
    def from_datastore(cls, **kw: t.Any) -> MetricsTabularRow:
        metrics: t.Dict[str, float] = {}

        for k, v in kw.items():
            if k.startswith("__"):
                continue

            if isinstance(k, str) and isinstance(v, (int, float)):
                metrics[k] = float(v)

        return cls(
            step=kw["__step"],
            clock_time=kw["__clock_time"],
            relative_time=kw["__relative_time"],
            metrics=metrics,
        )

    @classmethod
    def from_record(cls, record: MetricsRecord) -> MetricsTabularRow:
        return cls(
            step=record.step,
            clock_time=record.clock_time,
            relative_time=record.relative_time,
            metrics=record.data,
        )

    def asdict(self, ignore_keys: t.Optional[t.List[str]] = None) -> t.Dict:
        ret: t.Dict[str, t.Any] = copy.deepcopy(self.metrics)
        ret.update(
            {
                "__step": self.step,
                "__clock_time": self.clock_time,
                "__relative_time": self.relative_time,
            }
        )
        return ret


HandleQueueElementType = t.Optional[
    t.Union[TrackRecord, MetricsRecord, ArtifactsRecord, ParamsRecord]
]
