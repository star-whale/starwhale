from __future__ import annotations

import os
import queue
import typing as t
import threading
from functools import total_ordering

import loguru
from loguru import logger as _logger

from starwhale.consts import HTTPMethod
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import ParameterError
from starwhale.core.dataset.tabular import (
    TabularDataset,
    TabularDatasetRow,
    DEFAULT_CONSUMPTION_BATCH_SIZE,
    TabularDatasetSessionConsumption,
)

_DEFAULT_LOADER_CACHE_SIZE = 20


@total_ordering
class DataRow:
    class _Features(dict):
        def __setattr__(self, name: str, value: t.Any) -> None:
            self[name] = value

        def __getattr__(self, name: str) -> t.Any:
            if name in self:
                return self[name]
            else:
                raise AttributeError(f"No found attribute: {name}")

        def __delattr__(self, name: str) -> None:
            if name in self:
                del self[name]
            else:
                raise AttributeError(f"No found attribute: {name}")

    def __init__(
        self,
        index: t.Union[str, int],
        features: t.Dict,
    ) -> None:
        if not isinstance(index, (str, int)):
            raise TypeError(f"index({index}) is not int or str type")
        self.index = index

        if not isinstance(features, dict):
            raise TypeError(f"features({features}) is not dict type")
        self.features: t.Dict = DataRow._Features(features)

    def __str__(self) -> str:
        return f"{self.index}"

    def __repr__(self) -> str:
        return f"index:{self.index}, features:{self.features}"

    def __iter__(self) -> t.Iterator:
        return iter((self.index, self.features))

    def __getitem__(self, i: int) -> t.Any:
        return (self.index, self.features)[i]

    def __len__(self) -> int:
        return len(self.__dict__)

    def __lt__(self, obj: DataRow) -> bool:
        return str(self.index) < str(obj.index)

    def __eq__(self, obj: t.Any) -> bool:
        return bool(self.index == obj.index and self.features == obj.features)


_TMetaQItem = t.Optional[t.Union[TabularDatasetRow, Exception]]
_TRowQItem = t.Optional[t.Union[DataRow, Exception]]


class DataLoader:
    def __init__(
        self,
        dataset_uri: URI,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
        logger: t.Optional[loguru.Logger] = None,
        session_consumption: t.Optional[TabularDatasetSessionConsumption] = None,
        cache_size: int = _DEFAULT_LOADER_CACHE_SIZE,
        num_workers: int = 2,
    ):
        self.dataset_uri = dataset_uri
        self.logger = logger or _logger
        self.start = start
        self.end = end

        # TODO: refactor TabularDataset with dataset_uri
        # TODO: refactor dataset, tabular_dataset and standalone dataset module
        self.tabular_dataset = TabularDataset.from_uri(
            dataset_uri, start=start, end=end
        )
        self.session_consumption = session_consumption
        self.last_processed_range: t.Optional[t.Tuple[t.Any, t.Any]] = None

        if num_workers <= 0:
            raise ValueError(
                f"num_workers({num_workers}) must be a positive int number"
            )
        self._num_workers = num_workers

        if cache_size <= 0:
            raise ValueError(f"cache_size({cache_size}) must be a positive int number")
        self._cache_size = cache_size

    def _sign_uris(self, uris: t.List[str]) -> dict:
        _batch_size = (
            self.session_consumption.batch_size
            if self.session_consumption
            else DEFAULT_CONSUMPTION_BATCH_SIZE
        )
        r = CloudRequestMixed.do_http_request(
            f"/project/{self.dataset_uri.project}/{self.dataset_uri.object.typ}/{self.dataset_uri.object.name}/version/{self.dataset_uri.object.version}/sign-links",
            method=HTTPMethod.POST,
            instance_uri=self.dataset_uri,
            params={
                "expTimeMillis": int(
                    os.environ.get("SW_MODEL_PROCESS_UNIT_TIME_MILLIS", "60000")
                )
                * _batch_size,
            },
            json=uris,
            use_raise=True,
        ).json()
        return r["data"]  # type: ignore

    def _iter_meta(self) -> t.Generator[TabularDatasetRow, None, None]:
        if not self.session_consumption:
            # TODO: refactor for batch-signed urls
            for row in self.tabular_dataset.scan():
                yield row
        else:
            while True:
                # TODO: tune last processed range for multithread
                pk = [self.last_processed_range] if self.last_processed_range else None
                rt = self.session_consumption.get_scan_range(pk)
                self.last_processed_range = rt
                if rt is None:
                    break

                if self.dataset_uri.instance_type == InstanceType.CLOUD:
                    for rows in self.tabular_dataset.scan_batch(
                        rt[0], rt[1], self.session_consumption.batch_size
                    ):
                        _links = []
                        for row in rows:
                            for at in row.artifacts():
                                at.owner = self.dataset_uri
                                if at.link:
                                    _links.append(at.link)
                        uri_dict = self._sign_uris([lk.uri for lk in _links])
                        for lk in _links:
                            lk.signed_uri = uri_dict.get(lk.uri, "")

                        for row in rows:
                            yield row
                else:
                    for row in self.tabular_dataset.scan(rt[0], rt[1]):
                        yield row

    def _iter_meta_with_queue(self, mq: queue.Queue[_TMetaQItem]) -> None:
        # TODO: tune last processed range
        try:
            for meta in self._iter_meta():
                if meta and isinstance(meta, TabularDatasetRow):
                    mq.put(meta)
        except Exception as e:
            mq.put(e)
            raise

        for _ in range(0, self._num_workers):
            mq.put(None)

    def _unpack_row(
        self, row: TabularDatasetRow, skip_fetch_data: bool = False
    ) -> DataRow:
        for at in row.artifacts():
            at.owner = self.dataset_uri
            if skip_fetch_data:
                continue
            at.fetch_data()
        return DataRow(index=row.id, features=row.data)

    def _unpack_row_with_queue(
        self,
        in_mq: queue.Queue[_TMetaQItem],
        out_mq: queue.Queue[_TRowQItem],
        skip_fetch_data: bool = False,
    ) -> None:
        while True:
            meta = in_mq.get(block=True, timeout=None)
            if meta is None:
                break
            elif isinstance(meta, Exception):
                out_mq.put(meta)
                raise meta
            else:
                try:
                    row = self._unpack_row(meta, skip_fetch_data)
                    if row and isinstance(row, DataRow):
                        out_mq.put(row)
                except Exception as e:
                    out_mq.put(e)
                    raise

        out_mq.put(None)

    def __iter__(
        self,
    ) -> t.Generator[DataRow, None, None]:
        meta_fetched_queue: queue.Queue[_TMetaQItem] = queue.Queue(4 * self._cache_size)
        row_unpacked_queue: queue.Queue[_TRowQItem] = queue.Queue(self._cache_size)

        meta_fetcher = threading.Thread(
            name="meta-fetcher",
            target=self._iter_meta_with_queue,
            args=(meta_fetched_queue,),
            daemon=True,
        )
        meta_fetcher.start()

        rows_unpackers = []
        for i in range(0, self._num_workers):
            _t = threading.Thread(
                name=f"row-unpacker-{i}",
                target=self._unpack_row_with_queue,
                args=(meta_fetched_queue, row_unpacked_queue),
                daemon=True,
            )
            _t.start()
            rows_unpackers.append(_t)

        done_unpacker_cnt = 0
        while True:
            row = row_unpacked_queue.get(block=True, timeout=None)
            if row is None:
                done_unpacker_cnt += 1
                if done_unpacker_cnt == self._num_workers:
                    break
            elif isinstance(row, Exception):
                raise row
            else:
                yield row

        self.logger.debug(
            "queue details:"
            f"meta fetcher(qsize:{meta_fetched_queue.qsize()}, alive: {meta_fetcher.is_alive()}), "
            f"row unpackers(qsize:{row_unpacked_queue.qsize()}, alive: {[t.is_alive() for t in rows_unpackers]})"
        )

    def __str__(self) -> str:
        return f"DataLoader for {self.dataset_uri}, range:[{self.start},{self.end}], use consumption:{bool(self.session_consumption)}"

    def __repr__(self) -> str:
        return (
            f"DataLoader for {self.dataset_uri}, consumption:{self.session_consumption}"
        )


def get_data_loader(
    dataset_uri: t.Union[str, URI],
    start: t.Optional[t.Any] = None,
    end: t.Optional[t.Any] = None,
    session_consumption: t.Optional[TabularDatasetSessionConsumption] = None,
    logger: t.Optional[loguru.Logger] = None,
    cache_size: int = _DEFAULT_LOADER_CACHE_SIZE,
    num_workers: int = 2,
) -> DataLoader:

    if session_consumption:
        sc_start = session_consumption.session_start  # type: ignore
        sc_end = session_consumption.session_end  # type: ignore
        if sc_start != start or sc_end != end:
            raise ParameterError(
                f"star-end range keys not match, session_consumption:[{sc_start}, {sc_end}], loader:[{start}, {end}]"
            )

    if isinstance(dataset_uri, str):
        dataset_uri = URI(dataset_uri, expected_type=URIType.DATASET)

    return DataLoader(
        dataset_uri,
        start=start,
        end=end,
        session_consumption=session_consumption,
        logger=logger or _logger,
        cache_size=cache_size,
        num_workers=num_workers,
    )
