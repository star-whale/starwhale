from __future__ import annotations

import time
import queue
import typing as t
import threading
from functools import total_ordering

from starwhale.utils import console
from starwhale.utils.error import ParameterError
from starwhale.utils.dict_util import transform_dict
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.store import get_signed_urls
from starwhale.core.dataset.tabular import (
    TabularDataset,
    TabularDatasetRow,
    TabularDatasetSessionConsumption,
)

_DEFAULT_LOADER_CACHE_SIZE = 20

if t.TYPE_CHECKING:
    from .model import Dataset


@total_ordering
class DataRow:
    class _Features(dict):
        _PROTECTED_PREFIX = "_starwhale_"

        def __setitem__(self, key: t.Any, value: t.Any) -> None:
            super().__setitem__(key, value)
            self._patch_shadow_dataset(key, value)

        def __delitem__(self, key: t.Any) -> None:
            super().__delitem__(key)
            # datastore will ignore none column for scanning by default
            self._patch_shadow_dataset(key, None)

        def __setattr__(self, name: str, value: t.Any) -> None:
            if name.startswith(self._PROTECTED_PREFIX):
                super().__setattr__(name, value)
            else:
                self[name] = value

        def __getattr__(self, name: str) -> t.Any:
            if name.startswith(self._PROTECTED_PREFIX):
                return super().__getattribute__(name)
            elif name in self:
                return self[name]
            else:
                raise AttributeError(f"Not found attribute: {name}")

        def __delattr__(self, name: str) -> None:
            if name.startswith(self._PROTECTED_PREFIX):
                raise RuntimeError(f"cannot delete internal attribute: {name}")
            elif name in self:
                del self[name]
            else:
                raise AttributeError(f"Not found attribute: {name}")

        def _patch_shadow_dataset(self, key: t.Any, value: t.Any) -> None:
            # TODO: merge batch update
            ds = getattr(self, "_starwhale_shadow_dataset", None)
            if ds is not None:
                # pass dict type into dataset __setitem__, not the DataRow._Features type
                ds.__setitem__(self._starwhale_index, {key: value})

        def _with_shadow_dataset(
            self, dataset: Dataset, index: t.Union[str, int]
        ) -> DataRow._Features:
            from .model import Dataset

            if not isinstance(dataset, Dataset):
                raise TypeError(
                    f"shadow dataset only supports starwhale.Dataset type: {dataset}"
                )

            self._starwhale_shadow_dataset = dataset
            self._starwhale_index = index
            return self

    def __init__(
        self,
        index: t.Union[str, int],
        features: t.Dict,
        shadow_dataset: t.Optional[Dataset] = None,
    ) -> None:
        if not isinstance(index, (str, int)):
            raise TypeError(f"index({index}) is not int or str type")
        self.index = index

        if not isinstance(features, dict):
            raise TypeError(f"features({features}) is not dict type")
        self.features = DataRow._Features(features)
        if shadow_dataset is not None:
            self.features._with_shadow_dataset(shadow_dataset, index)
        self._shadow_dataset = shadow_dataset

    def _patch_shadow_dataset(self, dataset: Dataset) -> None:
        if self._shadow_dataset is not None and self._shadow_dataset is not dataset:
            raise RuntimeError("shadow dataset has already been set")

        if dataset is not None:
            self._shadow_dataset = dataset
            self.features._with_shadow_dataset(dataset, self.index)

    def __str__(self) -> str:
        return f"{self.index}"

    def __repr__(self) -> str:
        return f"index:{self.index}, features:{self.features}, shadow dataset: {self._shadow_dataset}"

    def __iter__(self) -> t.Iterator:
        return iter(self._get_items())

    def __getitem__(self, i: int) -> t.Any:
        return self._get_items()[i]

    def __len__(self) -> int:
        return len(self._get_items())

    def _get_items(self) -> t.Tuple:
        return (self.index, self.features)

    def __lt__(self, obj: DataRow) -> bool:
        return str(self.index) < str(obj.index)

    def __eq__(self, obj: t.Any) -> bool:
        return bool(self.index == obj.index and self.features == obj.features)


_TMetaQItem = t.Optional[t.Union[TabularDatasetRow, Exception]]
_TRowQItem = t.Optional[t.Union[DataRow, Exception]]
_TProcessedQItem = t.Optional[t.Union[str, int]]


class DataLoader:
    def __init__(
        self,
        dataset_uri: Resource,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
        session_consumption: t.Optional[TabularDatasetSessionConsumption] = None,
        cache_size: int = _DEFAULT_LOADER_CACHE_SIZE,
        num_workers: int = 2,
        dataset_scan_revision: str = "",
        field_transformer: t.Optional[t.Dict] = None,
    ):
        self.dataset_uri = dataset_uri
        self.start = start
        self.end = end
        self.dataset_scan_revision = dataset_scan_revision

        # TODO: refactor TabularDataset with dataset_uri
        # TODO: refactor dataset, tabular_dataset and standalone dataset module
        self.tabular_dataset = TabularDataset.from_uri(
            dataset_uri,
            start=start,
            end=end,
            data_datastore_revision=self.dataset_scan_revision,
        )
        self.session_consumption = session_consumption

        if num_workers <= 0:
            raise ValueError(
                f"num_workers({num_workers}) must be a positive int number"
            )
        self._num_workers = num_workers
        self._field_transformer = field_transformer

        if cache_size <= 0:
            raise ValueError(f"cache_size({cache_size}) must be a positive int number")
        self._cache_size = cache_size

        self._meta_fetched_queue: queue.Queue[_TMetaQItem] | None = None
        self._row_unpacked_queue: queue.Queue[_TRowQItem] | None = None
        self._key_processed_queue: queue.Queue[_TProcessedQItem] | None = None

        self._lock = threading.Lock()
        self._expected_rows_cnt = 0
        self._processed_rows_cnt = 0
        self._key_range_dict: t.Dict[t.Tuple[t.Any, t.Any], t.Dict[str, int]] = {}

    def _get_processed_key_range(self) -> t.Optional[t.List[t.Tuple[t.Any, t.Any]]]:
        if self._key_processed_queue is None:
            raise RuntimeError("key processed queue is not initialized")

        # Current server side implementation only supports the original key range as the processedData parameter,
        # so we need to wait for all the keys in the original key range to be processed.
        while True:
            try:
                key = self._key_processed_queue.get(block=False)
            except queue.Empty:
                break

            # TODO: tune performance for find key range
            for rk in self._key_range_dict:
                if (rk[0] is None or rk[0] <= key) and (rk[1] is None or key < rk[1]):
                    self._key_range_dict[rk]["processed_cnt"] += 1
                    break
            else:
                raise RuntimeError(
                    f"key({key}) not found in key range dict:{self._key_range_dict}"
                )

        processed_range_keys = []
        for rk in list(self._key_range_dict.keys()):
            if (
                self._key_range_dict[rk]["processed_cnt"]
                == self._key_range_dict[rk]["rows_cnt"]
            ):
                processed_range_keys.append(rk)
                del self._key_range_dict[rk]

        return processed_range_keys

    def _check_all_processed_done(self) -> bool:
        unfinished = self._expected_rows_cnt - self._processed_rows_cnt
        if unfinished < 0:
            raise ValueError(
                f"unfinished rows cnt({unfinished}) < 0, processed rows cnt has been called more than expected"
            )
        else:
            return unfinished == 0

    def _iter_meta(self) -> t.Generator[TabularDatasetRow, None, None]:
        if not self.session_consumption:
            # TODO: refactor for batch-signed urls
            for row in self.tabular_dataset.scan():
                yield row
        else:
            while True:
                with self._lock:
                    pk = self._get_processed_key_range()
                    rt = self.session_consumption.get_scan_range(pk)
                    if rt is None and self._check_all_processed_done():
                        break

                if rt is None:
                    time.sleep(1)
                    continue

                rows_cnt = 0
                if self.dataset_uri.instance.is_cloud:
                    for rows in self.tabular_dataset.scan_batch(
                        rt[0], rt[1], self.session_consumption.batch_size
                    ):
                        _links = [
                            a.link for row in rows for a in row.artifacts if a.link
                        ]
                        _signed_uris_map = get_signed_urls(
                            self.dataset_uri.instance, [lk.uri for lk in _links]
                        )
                        for lk in _links:
                            lk.signed_uri = _signed_uris_map.get(lk.uri, "")

                        for row in rows:
                            rows_cnt += 1
                            yield row
                else:
                    for row in self.tabular_dataset.scan(rt[0], rt[1]):
                        rows_cnt += 1
                        yield row

                with self._lock:
                    self._expected_rows_cnt += rows_cnt
                    self._key_range_dict[(rt[0], rt[1])] = {
                        "rows_cnt": rows_cnt,
                        "processed_cnt": 0,
                    }

    def _iter_meta_for_queue(self) -> None:
        out_mq = self._meta_fetched_queue
        if out_mq is None:
            raise RuntimeError("queue not initialized for iter meta")
        try:
            for meta in self._iter_meta():
                if meta and isinstance(meta, TabularDatasetRow):
                    out_mq.put(meta)
                else:
                    console.warn(
                        f"meta is not TabularDatasetRow type: {meta} - {type(meta)}"
                    )
        except Exception as e:
            out_mq.put(e)
            raise

        for _ in range(0, self._num_workers):
            out_mq.put(None)

    def _unpack_row(
        self,
        row: TabularDatasetRow,
        skip_fetch_data: bool = False,
        shadow_dataset: t.Optional[Dataset] = None,
    ) -> DataRow:
        for artifact in row.artifacts:
            artifact.prepare_link(self.dataset_uri.instance)
            if not skip_fetch_data:
                artifact.fetch_data()

        row.decode_feature_types()
        if self._field_transformer is not None:
            _features = transform_dict(row.features, self._field_transformer)
            row.features.update(_features)
        return DataRow(
            index=row.id, features=row.features, shadow_dataset=shadow_dataset
        )

    def _unpack_row_for_queue(
        self, skip_fetch_data: bool = False, shadow_dataset: t.Optional[Dataset] = None
    ) -> None:
        in_mq = self._meta_fetched_queue
        out_mq = self._row_unpacked_queue
        if in_mq is None or out_mq is None:
            raise RuntimeError(
                f"queue not initialized for unpack row: in({in_mq}), out({out_mq})"
            )

        while True:
            meta = in_mq.get(block=True, timeout=None)
            if meta is None:
                break
            elif isinstance(meta, Exception):
                out_mq.put(meta)
                raise meta
            else:
                try:
                    row = self._unpack_row(
                        meta, skip_fetch_data, shadow_dataset=shadow_dataset
                    )
                    if row and isinstance(row, DataRow):
                        out_mq.put(row)
                except Exception as e:
                    out_mq.put(e)
                    raise

        out_mq.put(None)

    def __iter__(
        self,
    ) -> t.Generator[DataRow, None, None]:
        self._meta_fetched_queue = queue.Queue(4 * self._cache_size)
        self._row_unpacked_queue = queue.Queue(self._cache_size)
        if self.session_consumption:
            self._key_processed_queue = queue.Queue()

        meta_fetcher = threading.Thread(
            name="meta-fetcher",
            target=self._iter_meta_for_queue,
            daemon=True,
        )
        meta_fetcher.start()

        rows_unpackers = []
        for i in range(0, self._num_workers):
            _t = threading.Thread(
                name=f"row-unpacker-{i}",
                target=self._unpack_row_for_queue,
                daemon=True,
            )
            _t.start()
            rows_unpackers.append(_t)

        done_unpacker_cnt = 0
        while True:
            row = self._row_unpacked_queue.get(block=True, timeout=None)
            if row is None:
                done_unpacker_cnt += 1
                if done_unpacker_cnt == self._num_workers:
                    break
            elif isinstance(row, Exception):
                raise row
            else:
                yield row
                with self._lock:
                    if self._key_processed_queue is not None:
                        self._key_processed_queue.put(row.index)
                    self._processed_rows_cnt += 1

        console.debug(
            "queue details:"
            f"meta fetcher(qsize:{self._meta_fetched_queue.qsize()}, alive: {meta_fetcher.is_alive()}), "
            f"row unpackers(qsize:{self._row_unpacked_queue.qsize()}, alive: {[t.is_alive() for t in rows_unpackers]})"
        )

    def __str__(self) -> str:
        return f"DataLoader for {self.dataset_uri}, range:[{self.start},{self.end}], use consumption:{bool(self.session_consumption)}"

    def __repr__(self) -> str:
        return (
            f"DataLoader for {self.dataset_uri}, consumption:{self.session_consumption}"
        )


def get_data_loader(
    dataset_uri: t.Union[str, Resource],
    start: t.Optional[t.Any] = None,
    end: t.Optional[t.Any] = None,
    session_consumption: t.Optional[TabularDatasetSessionConsumption] = None,
    cache_size: int = _DEFAULT_LOADER_CACHE_SIZE,
    num_workers: int = 2,
    dataset_scan_revision: str = "",
    field_transformer: t.Optional[t.Dict] = None,
) -> DataLoader:
    if session_consumption:
        sc_start = session_consumption.session_start  # type: ignore
        sc_end = session_consumption.session_end  # type: ignore
        if sc_start != start or sc_end != end:
            raise ParameterError(
                f"star-end range keys not match, session_consumption:[{sc_start}, {sc_end}], loader:[{start}, {end}]"
            )

    if isinstance(dataset_uri, str):
        dataset_uri = Resource(dataset_uri, ResourceType.dataset)

    return DataLoader(
        dataset_uri,
        start=start,
        end=end,
        session_consumption=session_consumption,
        cache_size=cache_size,
        num_workers=num_workers,
        dataset_scan_revision=dataset_scan_revision,
        field_transformer=field_transformer,
    )
