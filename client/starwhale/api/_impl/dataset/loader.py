from __future__ import annotations

import os
import typing as t
from abc import ABCMeta, abstractmethod

import loguru
from loguru import logger as _logger

from starwhale.utils import load_dotenv
from starwhale.consts import AUTH_ENV_FNAME
from starwhale.base.uri import URI
from starwhale.base.type import URIType, InstanceType, DataFormatType, ObjectStoreType
from starwhale.utils.error import ParameterError
from starwhale.core.dataset.type import BaseArtifact
from starwhale.core.dataset.store import FileLikeObj, ObjectStore, DatasetStorage
from starwhale.core.dataset.tabular import (
    TabularDataset,
    TabularDatasetRow,
    TabularDatasetSessionConsumption,
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
        _k = f"{self.dataset_uri}.{row.auth_name}"
        _store = self._stores.get(_k)
        if _store:
            return _store

        _type = self.dataset_uri.instance_type
        if _type == InstanceType.CLOUD:
            _store = ObjectStore.to_signed_http_backend(self.dataset_uri, row.auth_name)
        else:
            if row.object_store_type == ObjectStoreType.REMOTE:
                _store = ObjectStore.from_data_link_uri(row.data_uri, row.auth_name)
            else:
                _store = ObjectStore.from_dataset_uri(self.dataset_uri)

        self._stores[_k] = _store
        return _store

    def _get_key_compose(
        self, row: TabularDatasetRow, store: ObjectStore
    ) -> t.Tuple[str, int, int]:
        data_uri = row.data_uri
        if row.object_store_type != ObjectStoreType.REMOTE and store.key_prefix:
            data_uri = os.path.join(store.key_prefix, data_uri.lstrip("/"))

        if self.kind == DataFormatType.SWDS_BIN:
            offset, size = (
                int(row.extra_kw["_swds_bin_offset"]),
                int(row.extra_kw["_swds_bin_size"]),
            )
        else:
            offset, size = row.data_offset, row.data_size

        return data_uri, offset, offset + size - 1

    def _do_iter_row(self) -> t.Generator[TabularDatasetRow, None, None]:
        if not self.session_consumption:
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

                for row in self.tabular_dataset.scan(rt[0], rt[1]):
                    yield row

    def __iter__(
        self,
    ) -> t.Generator[t.Tuple[t.Union[str, int], t.Any, t.Dict], None, None]:
        for row in self._do_iter_row():
            # TODO: tune performance by fetch in batch
            _store = self._get_store(row)
            _key_compose = self._get_key_compose(row, _store)
            _file = _store.backend._make_file(_store.bucket, _key_compose)
            for data_content, _ in self._do_iter_data(_file, row):
                data = BaseArtifact.reflect(data_content, row.data_type)
                # TODO: refactor annotation origin type
                yield row.id, data, row.annotations

    @abstractmethod
    def _do_iter_data(
        self, file: FileLikeObj, row: TabularDatasetRow
    ) -> t.Generator[t.Tuple[bytes, int], None, None]:
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

    def _do_iter_data(
        self,
        file: FileLikeObj,
        row: TabularDatasetRow,
    ) -> t.Generator[t.Tuple[bytes, int], None, None]:
        yield file.read(row.data_size), row.data_size


class SWDSBinDataLoader(DataLoader):
    @property
    def kind(self) -> DataFormatType:
        return DataFormatType.SWDS_BIN

    def _do_iter_data(
        self, file: FileLikeObj, row: TabularDatasetRow
    ) -> t.Generator[t.Tuple[bytes, int], None, None]:
        from .builder import _header_size, _header_struct

        size: int
        padding_size: int
        header: bytes = file.read(_header_size)
        _, _, _, size, padding_size, _, _ = _header_struct.unpack(header)
        data: bytes = file.read(size + padding_size)
        yield data[:size], size


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
