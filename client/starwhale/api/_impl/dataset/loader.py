from __future__ import annotations

import os
import typing as t
from abc import ABCMeta, abstractmethod
from functools import total_ordering

import loguru
from loguru import logger as _logger

from starwhale.utils import load_dotenv
from starwhale.consts import HTTPMethod, AUTH_ENV_FNAME
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType, DataFormatType, ObjectStoreType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import ParameterError
from starwhale.core.dataset.type import Link, BaseArtifact
from starwhale.core.dataset.store import FileLikeObj, ObjectStore, DatasetStorage
from starwhale.api._impl.data_store import SwObject
from starwhale.core.dataset.tabular import (
    TabularDataset,
    TabularDatasetRow,
    DEFAULT_CONSUMPTION_BATCH_SIZE,
    TabularDatasetSessionConsumption,
)


@total_ordering
class DataRow:
    def __init__(
        self,
        index: t.Union[str, int],
        data: t.Optional[t.Union[BaseArtifact, Link]],
        annotations: t.Dict,
    ) -> None:
        self.index = index
        self.data = data
        self.annotations = annotations

        self._do_validate()

    def __str__(self) -> str:
        return f"{self.index}"

    def __repr__(self) -> str:
        return f"index:{self.index}, data:{self.data}, annotations:{self.annotations}"

    def __iter__(self) -> t.Iterator:
        return iter((self.index, self.data, self.annotations))

    def __getitem__(self, i: int) -> t.Any:
        return (self.index, self.data, self.annotations)[i]

    def __len__(self) -> int:
        return len(self.__dict__)

    def _do_validate(self) -> None:
        if not isinstance(self.index, (str, int)):
            raise TypeError(f"index({self.index}) is not int or str type")

        if self.data is not None and not isinstance(self.data, (BaseArtifact, Link)):
            raise TypeError(f"data({self.data}) is not BaseArtifact or Link type")

        if not isinstance(self.annotations, dict):
            raise TypeError(f"annotations({self.annotations}) is not dict type")

    def __lt__(self, obj: DataRow) -> bool:
        return str(self.index) < str(obj.index)

    def __eq__(self, obj: t.Any) -> bool:
        return bool(
            self.index == obj.index
            and self.data == obj.data
            and self.annotations == obj.annotations
        )


class DataLoader(metaclass=ABCMeta):
    def __init__(
        self,
        dataset_uri: URI,
        start: t.Optional[t.Any] = None,
        end: t.Optional[t.Any] = None,
        logger: t.Optional[loguru.Logger] = None,
        session_consumption: t.Optional[TabularDatasetSessionConsumption] = None,
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
        self._stores: t.Dict[str, ObjectStore] = {}
        self._load_dataset_auth_env()
        self.last_processed_range: t.Optional[t.Tuple[t.Any, t.Any]] = None

    def _load_dataset_auth_env(self) -> None:
        # TODO: support multi datasets
        if self.dataset_uri.instance_type == InstanceType.STANDALONE:
            auth_env_fpath = (
                DatasetStorage(self.dataset_uri).snapshot_workdir / AUTH_ENV_FNAME
            )
            load_dotenv(auth_env_fpath)

    def _get_store(self, row: TabularDatasetRow) -> ObjectStore:
        _k = f"{self.dataset_uri}.{row.data_link.scheme}.{row.auth_name}"
        _store = self._stores.get(_k)
        if _store:
            return _store

        if self.dataset_uri.instance_type == InstanceType.CLOUD:
            _store = ObjectStore.to_signed_http_backend(self.dataset_uri)
        else:
            if row.object_store_type == ObjectStoreType.REMOTE:
                _store = ObjectStore.from_data_link_uri(row.data_link, row.auth_name)
            else:
                _store = ObjectStore.from_dataset_uri(self.dataset_uri)

        self.logger.info(f"new store backend created for key {_k}")
        self._stores[_k] = _store
        return _store

    def _get_key_compose(
        self, row: TabularDatasetRow, store: ObjectStore
    ) -> t.Tuple[Link, int, int]:
        data_link = row.data_link
        if row.object_store_type != ObjectStoreType.REMOTE and store.key_prefix:
            data_link = Link(os.path.join(store.key_prefix, data_link.uri.lstrip("/")))

        if self.kind == DataFormatType.SWDS_BIN:
            offset, size = (
                int(row.extra_kw["_swds_bin_offset"]),
                int(row.extra_kw["_swds_bin_size"]),
            )
        else:
            offset, size = row.data_offset, row.data_size

        return data_link, offset, offset + size - 1

    def _travel_link(self, obj: t.Any) -> t.List[Link]:
        _lks = []
        if isinstance(obj, Link):
            _lks.append(obj)
        elif isinstance(obj, dict):
            for v in obj.values():
                _lks.extend(self._travel_link(v))
        elif isinstance(obj, (list, tuple)):
            for v in obj:
                _lks.extend(self._travel_link(v))
        elif isinstance(obj, SwObject):
            for v in obj.__dict__.values():
                _lks.extend(self._travel_link(v))
        return _lks

    def _sign_uris(self, uris: t.List[str]) -> dict:
        _batch_size = (
            self.session_consumption.batch_size
            if self.session_consumption
            else DEFAULT_CONSUMPTION_BATCH_SIZE
        )
        r = (
            CloudRequestMixed()
            .do_http_request(
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
            )
            .json()
        )
        return r["data"]  # type: ignore

    def _iter_row(self) -> t.Generator[TabularDatasetRow, None, None]:
        if not self.session_consumption:
            # TODO: refactor for batch-signed urls
            for row in self.tabular_dataset.scan():
                yield row
        else:
            while True:
                # TODO: multithread for get meta, get data and ppl process
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
                            _links.extend(
                                [row.data_link] + self._travel_link(row.annotations)
                            )
                        uri_dict = self._sign_uris([lk.uri for lk in _links])
                        for lk in _links:
                            lk.signed_uri = uri_dict.get(lk.uri, "")

                        for row in rows:
                            yield row
                else:
                    for row in self.tabular_dataset.scan(rt[0], rt[1]):
                        yield row

    def _unpack_row(
        self, row: TabularDatasetRow, skip_fetch_data: bool = False
    ) -> DataRow:
        if skip_fetch_data:
            return DataRow(index=row.id, data=None, annotations=row.annotations)

        store = self._get_store(row)
        key_compose = self._get_key_compose(row, store)
        file = store.backend._make_file(store.bucket, key_compose)
        data_content, _ = self._read_data(file, row)
        data = BaseArtifact.reflect(data_content, row.data_type)
        return DataRow(index=row.id, data=data, annotations=row.annotations)

    def __iter__(
        self,
    ) -> t.Generator[DataRow, None, None]:
        for row in self._iter_row():
            # TODO: tune performance by fetch in batch
            yield self._unpack_row(row)

    @abstractmethod
    def _read_data(
        self, file: FileLikeObj, row: TabularDatasetRow
    ) -> t.Tuple[bytes, int]:
        raise NotImplementedError

    def __str__(self) -> str:
        return f"[{self.kind.name}]DataLoader for {self.dataset_uri}, range:[{self.start},{self.end}], use consumption:{bool(self.session_consumption)}"

    def __repr__(self) -> str:
        return f"[{self.kind.name}]DataLoader for {self.dataset_uri}, consumption:{self.session_consumption}"

    @property
    def kind(self) -> DataFormatType:
        raise NotImplementedError


class UserRawDataLoader(DataLoader):
    @property
    def kind(self) -> DataFormatType:
        return DataFormatType.USER_RAW

    def _read_data(
        self,
        file: FileLikeObj,
        row: TabularDatasetRow,
    ) -> t.Tuple[bytes, int]:
        return file.read(row.data_size), row.data_size


class SWDSBinDataLoader(DataLoader):
    @property
    def kind(self) -> DataFormatType:
        return DataFormatType.SWDS_BIN

    def _read_data(
        self, file: FileLikeObj, row: TabularDatasetRow
    ) -> t.Tuple[bytes, int]:
        from .builder import _header_size, _header_struct

        size: int
        padding_size: int
        header: bytes = file.read(_header_size)
        _, _, _, size, padding_size, _, _ = _header_struct.unpack(header)
        data: bytes = file.read(size + padding_size)
        return data[:size], size


def get_data_loader(
    dataset_uri: t.Union[str, URI],
    start: t.Optional[t.Any] = None,
    end: t.Optional[t.Any] = None,
    session_consumption: t.Optional[TabularDatasetSessionConsumption] = None,
    logger: t.Optional[loguru.Logger] = None,
) -> DataLoader:
    from starwhale.core.dataset import model

    if session_consumption:
        sc_start = session_consumption.session_start  # type: ignore
        sc_end = session_consumption.session_end  # type: ignore
        if sc_start != start or sc_end != end:
            raise ParameterError(
                f"star-end range keys not match, session_consumption:[{sc_start}, {sc_end}], loader:[{start}, {end}]"
            )

    if isinstance(dataset_uri, str):
        dataset_uri = URI(dataset_uri, expected_type=URIType.DATASET)

    summary = model.Dataset.get_dataset(dataset_uri).summary()
    include_user_raw = summary.include_user_raw if summary else False
    _cls = UserRawDataLoader if include_user_raw else SWDSBinDataLoader

    return _cls(
        dataset_uri,
        start=start,
        end=end,
        session_consumption=session_consumption,
        logger=logger or _logger,
    )
