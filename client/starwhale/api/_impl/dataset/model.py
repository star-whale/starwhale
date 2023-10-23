from __future__ import annotations

import os
import csv
import copy
import json
import typing as t
import platform
import tempfile
import threading
from http import HTTPStatus
from types import TracebackType
from pathlib import Path
from functools import wraps, lru_cache
from itertools import islice

import yaml
import jsonlines

from starwhale.utils import console, now_str, convert_to_bytes, gen_uniq_version
from starwhale.consts import (
    FileDesc,
    HTTPMethod,
    CREATED_AT_KEY,
    SW_TMP_DIR_NAME,
    D_ALIGNMENT_SIZE,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    D_FILE_VOLUME_SIZE,
    DEFAULT_MANIFEST_NAME,
    ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.base.tag import StandaloneTag
from starwhale.utils.fs import (
    copy_file,
    empty_dir,
    ensure_dir,
    ensure_file,
    iter_pathlike_io,
)
from starwhale.base.type import PathLike, DatasetChangeMode, DatasetFolderSourceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.data_type import MIMEType
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.model import Dataset as CoreDataset
from starwhale.core.dataset.model import DatasetSummary, StandaloneDataset
from starwhale.core.dataset.store import DatasetStorage
from starwhale.base.models.dataset import DatasetListType, LocalDatasetInfo
from starwhale.api._impl.data_store import TableEmptyException
from starwhale.core.dataset.tabular import (
    TabularDataset,
    DatastoreRevision,
    TabularDatasetInfo,
    DatastoreWrapperDataset,
    get_dataset_consumption,
    DEFAULT_CONSUMPTION_BATCH_SIZE,
    TabularDatasetSessionConsumption,
)
from starwhale.base.client.models.models import DatasetInfoVo

from .loader import DataRow, DataLoader, get_data_loader
from .builder import MappingDatasetBuilder

if t.TYPE_CHECKING:
    import tensorflow as tf
    from torch.utils.data import Dataset as TorchDataset

_DType = t.TypeVar("_DType", bound="Dataset")
_ItemType = t.Union[str, int, slice]
_GItemType = t.Optional[t.Union[DataRow, t.List[DataRow]]]
_IterFeatureDict = t.Iterable[t.Dict[str, t.Any]]

_DEFAULT_LOADER_WORKERS = 2
_DEFAULT_LOADER_CACHE_SIZE = 20


class _DatasetCreateMode:
    auto = "auto"
    empty = "empty"
    forbid = "forbid"


class _Tags:
    def __init__(self, core_dataset: CoreDataset) -> None:
        self.__core_dataset = core_dataset

    def add(
        self,
        tags: t.Union[str, t.List[str]],
        ignore_errors: bool = False,
        force: bool = False,
    ) -> None:
        if isinstance(tags, str):
            tags = [tags]
        self.__core_dataset.add_tags(tags, ignore_errors=ignore_errors, force=force)

    def remove(
        self, tags: t.Union[str, t.List[str]], ignore_errors: bool = False
    ) -> None:
        if isinstance(tags, str):
            tags = [tags]
        self.__core_dataset.remove_tags(tags, ignore_errors)

    def __iter__(self) -> t.Generator[str, None, None]:
        for tag in self.__core_dataset.list_tags():
            yield tag

    def __str__(self) -> str:
        return f"Dataset Tag: {self.__core_dataset}"

    __repr__ = __str__


class Dataset:
    def __init__(
        self,
        uri: Resource,
        readonly: bool = False,
        create: str = _DatasetCreateMode.auto,
    ) -> None:
        self._uri = uri
        self.__readonly = readonly
        self._pending_commit_version = (
            os.environ.get(ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST)
            or gen_uniq_version()
        )

        if create not in (
            _DatasetCreateMode.auto,
            _DatasetCreateMode.empty,
            _DatasetCreateMode.forbid,
        ):
            raise ValueError(
                f"the current create mode is not in the accept options: {create}"
            )

        _origin_uri = self._make_capsulated_uri(uri.version or "latest", refine=True)
        origin_uri_exists = self._check_uri_exists(_origin_uri)
        if origin_uri_exists:
            if create == _DatasetCreateMode.empty:
                raise RuntimeError(
                    f"dataset already existed, failed to create by the {create} create mode: {self._uri.name}"
                )

            self._loading_version = self._auto_complete_version(_origin_uri.version)
        else:
            # TODO: call server or standalone api to create an empty dataset before committing.
            if create == _DatasetCreateMode.forbid:
                raise RuntimeError(
                    "dataset doest not exist, we have already use {create} create mode to ensure dataset existence: {self.name}"
                )

            if readonly:
                raise ValueError(
                    f"no support to set a non-existed dataset to the readonly mode: {self._uri.name}"
                )

            if self._uri.version != "":
                raise NoSupportError(
                    f"no support to create a specified version dataset: {self._uri.version}"
                )

            self._loading_version = self._pending_commit_version

        self._loading_uri = self._make_capsulated_uri(
            self._loading_version, refine=False
        )
        self.__loading_core_dataset = CoreDataset.get_dataset(self._loading_uri)

        self._pending_commit_uri = self._make_capsulated_uri(
            self._pending_commit_version, refine=False
        )
        self.__pending_commit_core_dataset = CoreDataset.get_dataset(
            self._pending_commit_uri
        )

        self._consumption: t.Optional[TabularDatasetSessionConsumption] = None
        self._loader_lock = threading.Lock()
        self.__data_loaders: t.Dict[str, DataLoader] = {}
        self._loader_cache_size = _DEFAULT_LOADER_CACHE_SIZE
        self._loader_num_workers = _DEFAULT_LOADER_WORKERS
        self._loader_field_transformer: t.Optional[t.Dict] = None

        self._builder_blob_alignment_size = D_ALIGNMENT_SIZE
        self._builder_blob_volume_size = D_FILE_VOLUME_SIZE
        self._builder_lock = threading.Lock()
        self._dataset_builder: t.Optional[MappingDatasetBuilder] = None
        self._commit_lock = threading.Lock()
        self.__has_committed = False

        self._info_lock = threading.Lock()
        self._info_ds_wrapper: t.Optional[DatastoreWrapperDataset] = None
        self.__info: t.Optional[TabularDatasetInfo] = None

        self._tmpdir: t.Optional[Path] = None

        self._updated_rows_by_commit = 0
        self._deleted_rows_by_commit = 0
        if origin_uri_exists:
            _summary = self.__loading_core_dataset.summary()
            # TODO: raise none summary exception for existed dataset
            if _summary is None:
                self._approximate_total_rows = 0
                self._total_blobs_size = 0
            else:
                self._approximate_total_rows = _summary.rows
                self._total_blobs_size = _summary.blobs_byte_size
        else:
            self._approximate_total_rows = 0
            self._total_blobs_size = 0

        self._last_data_datastore_revision = ""
        self._last_info_datastore_revision = ""

        self._len_lock = threading.Lock()
        self._len_cache = self._approximate_total_rows
        self._len_may_changed = False

    def _make_capsulated_uri(self, version: str, refine: bool) -> Resource:
        return Resource(
            "/version/".join(filter(bool, [self._uri.name, version])),
            typ=ResourceType.dataset,
            project=copy.deepcopy(self._uri.project),
            refine=refine,
        )

    def _auto_complete_version(self, version: str) -> str:
        version = version.strip()
        if not version:
            return version

        # TODO: auto complete for cloud instance
        if self._uri.instance.is_cloud:
            return version

        _uri = self._make_capsulated_uri(version, refine=True)
        store = DatasetStorage(_uri)
        if not store.snapshot_workdir.exists():
            return version
        else:
            return store.id

    def __str__(self) -> str:
        return f"Dataset: {self._uri.name}, stash version: {self._pending_commit_version}, loading version: {self._loading_version}"

    def __repr__(self) -> str:
        return f"Dataset: {self._uri.name}, loading uri: {self._loading_uri}, pending commit uri: {self._pending_commit_uri}"

    def __len__(self) -> int:
        """__len__ slow but accurate"""
        with self._len_lock:
            if self._len_may_changed:
                self.flush()
                if self._dataset_builder is None:
                    raise RuntimeError("dataset builder is not initialized")
                self._len_cache = self._dataset_builder.calculate_rows_cnt()
                self._len_may_changed = False
            return self._len_cache

    @property
    def approximate_size(self) -> int:
        """approximate_size fast but maybe inaccurate"""
        return self._approximate_total_rows

    def __enter__(self: _DType) -> _DType:
        return self

    def __bool__(self) -> bool:
        return True

    def __exit__(
        self,
        type: t.Optional[t.Type[BaseException]],
        value: t.Optional[BaseException],
        trace: TracebackType,
    ) -> None:
        if value:  # pragma: no cover
            console.warning(f"type:{type}, exception:{value}, traceback:{trace}")

        self.close()

    def make_distributed_consumption(
        self, session_id: str, batch_size: int = DEFAULT_CONSUMPTION_BATCH_SIZE
    ) -> Dataset:
        if self._consumption is not None:
            raise RuntimeError(
                f"distributed consumption has already been created ({self._consumption})"
            )

        with self._loader_lock:
            self._consumption = get_dataset_consumption(
                self.uri, session_id=session_id, batch_size=batch_size
            )
        return self

    def _clear_data_loader(self) -> None:
        with self._loader_lock:
            self.__data_loaders = {}

    def with_loader_config(
        self,
        num_workers: t.Optional[int] = None,
        cache_size: t.Optional[int] = None,
        field_transformer: t.Optional[t.Dict] = None,
    ) -> Dataset:
        """Modify the default configurations when loading a dataset.
        Arguments:
            num_workers: (int, optional)
            cache_size: (int, optional)
            field_transformer: (dict, optional) transform the dataset fields to what you would like.
                a possible key_transformer: {"k1.k2.k3[2].k4":"k5"}
        Returns:
            A Dataset Object
        Examples:
        ```python
        from starwhale import Dataset, dataset
        Dataset.from_json(
            "translation",
            '[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]'
        )
        myds = dataset("translation").with_loader_config(field_transformer={"en": "en-us"})
        assert myds[0].features["en-us"] == myds[0].features["en"]
        ```
        ```python
        from starwhale import Dataset, dataset
        Dataset.from_json(
            "translation2",
            '[{"content":{"child_content":[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]}}]'
        )
        myds = dataset("translation2").with_loader_config(field_transformer={"content.child_content[0].en": "en-us"})
        assert myds[0].features["en-us"] == myds[0].features["content"]["child_content"][0]["en"]
        ```
        """
        with self._loader_lock:
            if len(self.__data_loaders) != 0:
                raise RuntimeError(
                    f"loaders({list(self.__data_loaders)}) have already been initialized"
                )

            if num_workers is not None:
                self._loader_num_workers = num_workers

            if cache_size is not None:
                self._loader_cache_size = cache_size

            if field_transformer is not None:
                self._loader_field_transformer = field_transformer

        return self

    def with_builder_blob_config(
        self,
        volume_size: int | str | None = D_FILE_VOLUME_SIZE,
        alignment_size: int | str | None = D_ALIGNMENT_SIZE,
    ) -> Dataset:
        """Config blob attributes for the dataset builder.

        If you want to config blob attributes, you should call the function before appending, updating or deleting dataset records.

        Arguments:
            volume_size: (str, int, optional) The max bytes size of the single blob file.
                When blob file size exceeds the value of volume_size argument, a new blob file is created automatically.
                The default is 64MB. The argument accepts int(bytes) or str(32K, 64MB, 1GB...) format.
            alignment_size: (str, int, optional) The alignment size of every bin section in the blob file.
                The remaining part will be filled with `\0`. Default is 64 bytes.

        Examples:
        ```python
        from starwhale import dataset, Binary

        ds = dataset("mnist").with_builder_blob_config(volume_size="32M", alignment_size=128)
        ds.append({"data": Binary(b"123")})
        ds.commit()
        ds.close()
        ```
        Returns:
            A Dataset Object
        """
        with self._builder_lock:
            if self._dataset_builder is not None:
                raise RuntimeError(
                    "dataset has already accept some changed rows, forbid to config dataset blob attributes"
                )
            volume_size = D_FILE_VOLUME_SIZE if volume_size is None else volume_size
            alignment_size = (
                D_ALIGNMENT_SIZE if alignment_size is None else alignment_size
            )
            self._builder_blob_volume_size = convert_to_bytes(volume_size)
            self._builder_blob_alignment_size = convert_to_bytes(alignment_size)

        return self

    def _get_data_loader(
        self, recreate: bool = False, disable_consumption: bool = False
    ) -> DataLoader:
        with self._loader_lock:
            key = f"consumption-{disable_consumption}-{self.uri.version}"
            _loader = self.__data_loaders.get(key)
            if _loader is None or recreate:
                if disable_consumption:
                    consumption = None
                else:
                    consumption = self._consumption

                data_revision = self._last_data_datastore_revision
                if data_revision == "":
                    data_revision = self._get_datastore_revision(self.uri).data

                _loader = get_data_loader(
                    self.uri,
                    session_consumption=consumption,
                    cache_size=self._loader_cache_size,
                    num_workers=self._loader_num_workers,
                    dataset_scan_revision=data_revision,
                    field_transformer=self._loader_field_transformer,
                )
                self.__data_loaders[key] = _loader

        return _loader

    @lru_cache(maxsize=32)  # noqa: B019
    def _get_datastore_revision(self, uri: Resource) -> DatastoreRevision:
        if uri.typ != ResourceType.dataset:
            raise NoSupportError(
                f"only support to fetch dataset datastore revision: {uri}"
            )

        if uri.version == "":
            raise RuntimeError(f"cannot get version of uri: {uri}")

        if uri.instance.is_cloud:
            crm = CloudRequestMixed()
            r = crm.do_http_request(
                path=f"/project/{uri.project.id}/dataset/{uri.name}",
                instance=uri.instance,
                params={"versionUrl": uri.version},
            ).json()
            manifest = yaml.safe_load(r["data"]["versionMeta"])
        else:
            manifest = DatasetStorage(uri).manifest

        return DatastoreRevision.from_manifest(manifest)

    def batch_iter(
        self, batch_size: int = 1, drop_not_full: bool = False
    ) -> t.Iterator[t.List[DataRow]]:
        """Batch data into lists of length n. The last batch may be shorter.

        Arguments:
            batch_size: (int, optional) The size of each batch. Default is 1.
            drop_not_full: (bool, optional) Whether to drop the last batch if it is not full.

        Returns:
            A generator of lists of length n.

        Examples:
        ```python
        from starwhale import dataset

        ds = dataset("mnist")
        for batch_rows in ds.batch_iter(batch_size=2):
            assert len(batch_rows) == 2
            print(batch_rows[0].features)
        ```
        """
        it = self.__iter__()
        while True:
            batch_data = list(islice(it, batch_size))
            if not batch_data or (drop_not_full and len(batch_data) < batch_size):
                return
            yield batch_data

    def __iter__(self) -> t.Iterator[DataRow]:
        for row in self._get_data_loader():
            row._patch_shadow_dataset(self)
            yield row

    def __getitem__(
        self,
        item: _ItemType,
    ) -> _GItemType:
        """
        Example:
            self["str_key"]  # get the DataRow by the "str_key" string key
            self[1]  # get the DataRow by the 1 int key
            self["start":"end"]  # get a slice of the dataset by the range ("start", "end")
            self[1:10:2] # get a slice of the dataset by the range (1, 10), step is 2
        """
        # TODO: tune datastore performance for getitem
        return self._getitem(item)

    def _getitem(
        self,
        item: _ItemType,
        skip_fetch_data: bool = False,
    ) -> _GItemType:
        def _run() -> _GItemType:
            loader = self._get_data_loader(disable_consumption=True)
            if isinstance(item, (int, str)):
                row = next(loader.tabular_dataset.scan(item, item, end_inclusive=True))
                return loader._unpack_row(row, skip_fetch_data, shadow_dataset=self)
            elif isinstance(item, slice):
                step = item.step or 1
                if step <= 0:
                    raise ValueError(
                        f"Dataset slice step({step}) cannot be zero or negative number"
                    )
                cnt = 0
                # TODO: batch signed urls
                rows = []
                for row in loader.tabular_dataset.scan(item.start, item.stop):
                    if cnt % step == 0:
                        rows.append(
                            loader._unpack_row(
                                row,
                                skip_fetch_data,
                                shadow_dataset=self,
                            )
                        )
                    cnt += 1
                return rows
            else:
                raise ValueError(f"{item} type is not int, str or slice")

        try:
            return _run()
        except TableEmptyException:
            self._clear_data_loader()
            return None
        except StopIteration:
            return None

    @property
    def readonly(self) -> bool:
        return self.__readonly

    def _check_readonly(func: t.Callable):  # type: ignore
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> t.Any:
            self: Dataset = args[0]
            if self.readonly:
                raise RuntimeError(f"{func} does not work in the readonly mode")
            return func(*args, **kwargs)

        return _wrapper

    @property
    def tags(self) -> _Tags:
        return _Tags(self.__loading_core_dataset)

    @staticmethod
    def _check_uri_exists(uri: t.Optional[Resource]) -> bool:
        if uri is None or uri.version == "":
            return False

        if uri.instance.is_cloud:
            crm = CloudRequestMixed()
            ok, _ = crm.do_http_request_simple_ret(
                path=f"/project/{uri.project.id}/{ResourceType.dataset.value}/{uri.name}/version/{uri.version}",
                method=HTTPMethod.HEAD,
                instance=uri.instance,
                ignore_status_codes=[HTTPStatus.NOT_FOUND],
            )
            return ok
        else:
            _store = DatasetStorage(uri)
            return _store.manifest_path.exists()

    def exists(self) -> bool:
        return self._check_uri_exists(self.uri)

    @property
    def info(self) -> TabularDatasetInfo:
        with self._info_lock:
            if self.__info is None:
                info_revision = self._last_info_datastore_revision
                if info_revision == "":
                    info_revision = self._get_datastore_revision(self.uri).info

                self._info_ds_wrapper = TabularDataset.from_uri(
                    self.uri, info_datastore_revision=info_revision
                )._info_ds_wrapper
                self.__info = TabularDatasetInfo.load_from_datastore(
                    self._info_ds_wrapper
                )

            return self.__info

    @_check_readonly
    def flush(self, artifacts_flush: bool = False) -> str:
        revision = ""
        if self._dataset_builder:
            revision = self._dataset_builder.flush(artifacts_flush)
            self._last_data_datastore_revision = revision
            self._clear_data_loader()

        return revision

    @_check_readonly
    def rehash(self) -> None:
        # TODO: rehash for swds-bin format dataset with append/delete items to reduce volumes size
        raise NotImplementedError

    def remove(self, force: bool = False) -> None:
        ok, reason = self.__loading_core_dataset.remove(force)
        if not ok:
            raise RuntimeError(f"failed to remove dataset: {reason}")

    def recover(self, force: bool = False) -> None:
        ok, reason = self.__loading_core_dataset.recover(force)
        if not ok:
            raise RuntimeError(f"failed to recover dataset: {reason}")

    def summary(self) -> t.Optional[DatasetSummary]:
        return self.__loading_core_dataset.summary()

    def history(self) -> t.List[t.Dict]:
        return self.__loading_core_dataset.history()

    def close(self) -> None:
        for _loader in self.__data_loaders.values():
            if not _loader:
                continue  # pragma: no cover
            _loader.tabular_dataset.close()

        if self._dataset_builder:
            self._dataset_builder.close()

        if self._tmpdir is not None:
            empty_dir(self._tmpdir)

    @property
    def tmpdir(self) -> Path:
        if self._tmpdir is None:
            _base_dir = SWCliConfigMixed().rootdir / SW_TMP_DIR_NAME
            ensure_dir(_base_dir)
            self._tmpdir = Path(
                tempfile.mkdtemp(prefix=f"dataset-{self._uri.name}-", dir=_base_dir)
            )
        return self._tmpdir

    def diff(self, cmp: Dataset) -> t.Dict:
        # TODO: wait for datastore diff feature
        raise NotImplementedError

    def manifest(self) -> LocalDatasetInfo | DatasetInfoVo | None:
        return self.__loading_core_dataset.info()

    def head(self, n: int = 5) -> t.List[DataRow]:
        if n <= 0:
            return []
        return list(self.scan(limit=n))

    def scan(self, limit: int | None = None) -> t.Iterator[DataRow]:
        count = 0
        # TODO: render artifact in JupyterNotebook
        loader = self._get_data_loader(disable_consumption=True)
        for td_row in loader.tabular_dataset.scan():
            if limit is not None and count >= limit:
                return
            count += 1
            data_row = loader._unpack_row(td_row, skip_fetch_data=False)
            yield data_row

    def fetch_one(self) -> DataRow:
        return next(self.scan(limit=1))

    def to_pytorch(
        self,
        transform: t.Optional[t.Callable] = None,
        drop_index: bool = True,
        skip_default_transform: bool = False,
    ) -> TorchDataset:
        """Convert Starwhale Dataset to PyTorch Dataset.

        Arguments:
            transform: (callable, optional) A transform function for input data.
            drop_index: (bool, optional) Whether to drop the index column.
            skip_default_transform: (bool, optional) If `transform` is not set,
                by default the built-in Starwhale transform function will be used to transform the data.
                This can be disabled with the `skip_default_transform` parameter.

        Returns:
            torch.utils.data.Dataset

        Examples:
        ```python
        import torch.utils.data as tdata
        from starwhale import dataset

        ds = dataset("mnist")

        torch_ds = ds.to_pytorch()
        torch_loader = tdata.DataLoader(torch_ds, batch_size=2)
        ```

        ```python
        import torch.utils.data as tdata
        from starwhale import dataset

        with dataset("mnist") as ds:
            for i in range(0, 10):
                ds.append({"txt": Text(f"data-{i}"), "label": i})

            ds.commit()

        def _custom_transform(data: t.Any) -> t.Any:
            data = data.copy()
            txt = data["txt"].to_str()
            data["txt"] = f"custom-{txt}"
            return data

        torch_loader = tdata.DataLoader(
            dataset(ds.uri).to_pytorch(transform=_custom_transform), batch_size=1
        )
        item = next(iter(torch_loader))
        assert isinstance(item["label"], torch.Tensor)
        assert item["txt"][0] in ("custom-data-0", "custom-data-1")
        ```
        """
        from starwhale.integrations.pytorch import TorchIterableDataset

        return TorchIterableDataset(
            dataset=self,
            transform=transform,
            drop_index=drop_index,
            skip_default_transform=skip_default_transform,
        )

    def to_tensorflow(
        self,
        drop_index: bool = True,
    ) -> tf.data.Dataset:
        """Convert Starwhale Dataset to Tensorflow Dataset.

        Arguments:
            drop_index: (bool, optional) Whether to drop the index column.

        Returns:
            tensorflow.data.Dataset object

        Examples:

        ```python
        from starwhale import dataset
        import tensorflow as tf

        ds = dataset("mnist")
        tf_ds = ds.to_tensorflow(drop_index=True)
        assert isinstance(tf_ds, tf.data.Dataset)
        ```
        """
        from starwhale.integrations.tensorflow import to_tf_dataset

        return to_tf_dataset(dataset=self, drop_index=drop_index)

    @_check_readonly
    def __setitem__(
        self, key: t.Union[str, int], value: t.Union[DataRow, t.Tuple, t.Dict]
    ) -> None:
        # TODO: tune the performance of getitem by cache
        if not isinstance(key, (int, str)):
            raise TypeError(f"key must be str or int type: {key}")

        if isinstance(value, DataRow):
            value.index = key
            row = value
        elif isinstance(value, dict):
            row = DataRow(index=key, features=value)
        elif isinstance(value, (tuple, list)):
            if len(value) == 1:
                features = value[0]
            elif len(value) == 2:
                _, features = value
            else:
                raise ValueError(f"{value} cannot unpack")

            row = DataRow(index=key, features=features)
        else:
            raise TypeError(f"value only supports tuple, dict or DataRow type: {value}")

        # TODO: add gc/rehash for update swds-bin format artifact features
        _ds_builder = self._get_dataset_builder()
        with self._len_lock:
            _ds_builder.put(row)
            self._len_may_changed = True

        self._approximate_total_rows += 1
        self._updated_rows_by_commit += 1

    def _get_dataset_builder(self) -> MappingDatasetBuilder:
        with self._builder_lock:
            if self._dataset_builder is None:
                # TODO: support alignment_bytes_size, volume_bytes_size arguments
                self._dataset_builder = MappingDatasetBuilder(
                    workdir=self.tmpdir / "builder",
                    dataset_uri=self._uri,
                    blob_alignment_bytes_size=self._builder_blob_alignment_size,
                    blob_volume_bytes_size=self._builder_blob_volume_size,
                )
            return self._dataset_builder

    @_check_readonly
    def __delitem__(self, key: _ItemType) -> None:
        items: t.List
        if isinstance(key, (str, int)):
            items = [self._getitem(key, skip_fetch_data=True)]
        elif isinstance(key, slice):
            items = self._getitem(key, skip_fetch_data=True)  # type: ignore
        else:
            raise TypeError(f"key({key}) is not str, int or slice type")

        # TODO: raise not-found key error?
        _ds_builder = self._get_dataset_builder()
        for item in items:
            if not item or not isinstance(item, DataRow):
                continue  # pragma: no cover

            with self._len_lock:
                _ds_builder.delete(item.index)
                self._len_may_changed = True

            self._approximate_total_rows -= 1
            self._deleted_rows_by_commit += 1

    @_check_readonly
    def append(self, item: t.Any) -> None:
        if isinstance(item, DataRow):
            self.__setitem__(item.index, item)
        elif isinstance(item, dict):
            self.__setitem__(self._approximate_total_rows, item)
        elif isinstance(item, (list, tuple)):
            if len(item) == 1:
                row = DataRow(self._approximate_total_rows, item[0])
            elif len(item) == 2:
                row = DataRow(item[0], item[1])
            else:
                raise ValueError(
                    f"cannot unpack value({item}), expected sequence is (index, data) or data"
                )

            self.__setitem__(row.index, row)
        else:
            raise TypeError(f"value({item}) is not DataRow, list or tuple type")

    @_check_readonly
    def extend(self, items: t.Sequence[t.Any]) -> None:
        for item in items:
            self.append(item)

    @property
    def loading_version(self) -> str:
        """Loaded dataset version.
        When loading an existing dataset, the loading_version is the related dataset version.
        When creating a non-existed dataset, the loading_version is equal to the pending_commit_version.

        Returns:
            A str version
        """
        return self._loading_version

    @property
    def pending_commit_version(self) -> str:
        """Next commit version.
        When you call the `commit` function, the pending_commit_version will be recorded in
        the Standalone instance ,Server instance or Cloud instance.

        Returns:
            A str version
        """
        return self._pending_commit_version

    @property
    def committed_version(self) -> str:
        """Committed version.

        After the commit function is called, the committed_version will come out, it is equal to the pending_commit_version.

        Returns:
            A str version
        """
        if self.__has_committed:
            return self._pending_commit_version
        else:
            raise RuntimeError("version has not been committed yet")

    @property
    def uri(self) -> Resource:
        """Dataset URI

        Before committing, the uri is for the loading_version.
        After committing, the uri is for the committed_version.

        Returns:
            A URI object.
        """
        if self.__has_committed:
            return self._pending_commit_uri
        else:
            return self._loading_uri

    @property
    def changed(self) -> bool:
        """For the non-readonly dataset, when the users update/delete/add records(rows) or features(columns),
        changed property will change from False to True.

        Returns:
            True or False
        """
        return self._updated_rows_by_commit != 0 or self._deleted_rows_by_commit != 0

    @_check_readonly
    def commit(
        self,
        tags: t.Optional[t.List[str]] = None,
        message: str = "",
        force_add_tags: bool = False,
        ignore_add_tags_errors: bool = False,
    ) -> str:
        """Commit into dataset
        Commit will flush and generate a version of the dataset. At the same time, commit
        operation will also generate auto-increment tag, such as v0, v1, v2. Only one commit is allowed.

        Arguments:
            tags: (list(str), optional) Specify the tags for the version. Default is None. `latest` and `^v\d+$` tags are reserved tags.
            message: (str, optional) Commit message. Default is empty str.
            force_add_tags: (bool, optional) Force to add tags. Default is False.
            ignore_add_tags_errors: (bool, optional) Ignore add tags errors. Default is False.

        Example:
        ```python
        from starwhale import dataset
        with dataset("mnist") as ds:
            ds.append({"label": 1})
            ds.commit(message="init commit")
        ```

        Return:
            A str of dataset version.
        """
        # TODO: forbid commit many times
        with self._commit_lock:
            return self._commit(
                tags or [], message, force_add_tags, ignore_add_tags_errors
            )

    def _commit(
        self,
        tags: t.List[str],
        message: str,
        force_add_tags: bool,
        ignore_add_tags_errors: bool,
    ) -> str:
        def _save_info() -> str:
            revision = ""
            if self.__info is not None and self._info_ds_wrapper is not None:
                revision = self.__info.save_to_datastore(self._info_ds_wrapper)
                self._last_info_datastore_revision = revision
            return revision

        def _dump_manifest(dataset_revision: str, info_revision: str) -> Path:
            if self._dataset_builder is None:
                raise RuntimeError("failed to commit, because dataset builder is None")

            increased_blobs_size = sum(
                [m.size for m in self._dataset_builder.signature_bins_meta]
            )

            _manifest = {
                "build": {
                    "os": platform.system(),
                    "starwhale": STARWHALE_VERSION,
                },
                "version": self._pending_commit_version,
                CREATED_AT_KEY: now_str(),
                "dataset_summary": DatasetSummary(
                    rows=self._dataset_builder.calculate_rows_cnt(),  # maybe slow
                    updated_rows=self._updated_rows_by_commit,
                    deleted_rows=self._deleted_rows_by_commit,
                    blobs_byte_size=self._total_blobs_size + increased_blobs_size,
                    increased_blobs_byte_size=increased_blobs_size,
                ).asdict(),
                "message": message,
            }

            _manifest.update(
                DatastoreRevision(data=dataset_revision, info=info_revision).asdict()
            )

            self._updated_rows_by_commit = 0
            self._deleted_rows_by_commit = 0

            _m_path = self.tmpdir / DEFAULT_MANIFEST_NAME
            ensure_file(
                _m_path,
                yaml.safe_dump(_manifest, default_flow_style=False),
                parents=True,
            )
            return _m_path

        def _submit_standalone_version(manifest_path: Path) -> None:
            pccd = self.__pending_commit_core_dataset
            if not isinstance(pccd, StandaloneDataset):
                raise RuntimeError(
                    f"core dataset is not StandaloneDataset instance: {pccd}"
                )

            _snapshot_dir = pccd.store.snapshot_workdir
            ensure_dir(_snapshot_dir)
            copy_file(
                manifest_path,
                pccd.store.snapshot_workdir / DEFAULT_MANIFEST_NAME,
            )
            pccd.tag.add_fast_tag()

        def _submit_cloud_version(manifest_path: Path) -> None:
            from starwhale.base.bundle_copy import _UploadPhase

            crm = CloudRequestMixed()
            params = {
                "swds": f"{self._uri.name}:{self._pending_commit_version}",
                "project": self._uri.project.id,
                "force": "1",  # stash version is unique, use force=1 to make http retry happy
            }
            url_path = f"/project/{self._uri.project.id}/dataset/{self._uri.name}/version/{self._pending_commit_version}/file"
            r = crm.do_multipart_upload_file(
                url_path=url_path,
                file_path=manifest_path,
                instance=self._uri.instance,
                params={
                    "phase": _UploadPhase.MANIFEST,
                    "desc": FileDesc.MANIFEST.name,
                    **params,
                },
                use_raise=True,
            )
            crm.do_http_request(
                path=url_path,
                method=HTTPMethod.POST,
                instance=self._uri.instance,
                data={
                    "phase": _UploadPhase.END,
                    "uploadId": r.json()["data"]["uploadId"],
                    **params,
                },
                use_raise=True,
                disable_default_content_type=True,
            )

        if self.__has_committed:
            raise RuntimeError("Dataset has already committed")

        StandaloneTag.check_tags_validation(tags)

        dataset_revision = self.flush(artifacts_flush=True)
        info_revision = _save_info()
        manifest_path = _dump_manifest(dataset_revision, info_revision)

        console.debug(
            f"dataset commit: revision-{dataset_revision}, info revision-{info_revision}"
        )
        if self._uri.instance.is_local:
            _submit_standalone_version(manifest_path)
        else:
            _submit_cloud_version(manifest_path)
        os.unlink(manifest_path)

        if tags:
            self.__pending_commit_core_dataset.add_tags(
                tags, ignore_errors=ignore_add_tags_errors, force=force_add_tags
            )

        self.__has_committed = True
        return self._pending_commit_version

    @property
    def committed(self) -> bool:
        """Check If the dataset is committed.

        Returns:
            True or False
        """
        return self.__has_committed

    def copy(
        self,
        dest_uri: str,
        dest_local_project_uri: str = "",
        force: bool = False,
        mode: str = DatasetChangeMode.PATCH.value,
        ignore_tags: t.List[str] | None = None,
    ) -> None:
        """Copy dataset to another instance.

        Arguments:
            dest_uri: (str, required) destination dataset uri
            dest_local_project_uri: (str, optional) destination local project uri
            force: (bool, optional) force to copy
            mode: (str, optional) copy mode, default is 'patch'. Mode choices are: 'patch', 'overwrite'.
              `patch` mode: only update the changed rows and columns for the remote dataset;
              `overwrite` mode: update records and delete extraneous rows from the remote dataset
            ignore_tags: (list(str), optional) ignore tags when copying.
              In default, copy dataset with all user custom tags. `latest` and `^v\d+$` are the system builtin tags, they are ignored automatically.
              When the tags are already used for the other dataset version in the dest instance, you should use `force` option or adjust the tags.

        Returns:
            None

        Examples:

        ```python
        from starwhale import dataset
        ds = dataset("mnist")
        ds.copy("cloud://remote-instance/project/starwhale")
        ds.copy("cloud://cloud.starwhale.cn/project/public:starwhale", ignore_tags=["t1"])
        ```
        """
        CoreDataset.copy(
            self.uri,
            dest_uri,
            dest_local_project_uri=dest_local_project_uri,
            force=force,
            mode=DatasetChangeMode(mode),
            ignore_tags=ignore_tags,
        )

    @classmethod
    def list(
        cls,
        project_uri: t.Union[str, Project] = "",
        fullname: bool = False,
        show_removed: bool = False,
        page_index: int = DEFAULT_PAGE_IDX,
        page_size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[DatasetListType, t.Dict[str, t.Any]]:
        from starwhale.core.dataset.view import DatasetTermView

        return DatasetTermView.list(
            project_uri, fullname, show_removed, page_index, page_size
        )

    @classmethod
    def dataset(
        cls,
        uri: t.Union[str, Resource],
        create: str = _DatasetCreateMode.auto,
        readonly: bool = False,
    ) -> Dataset:
        """Create or load a dataset from standalone instance or cloud instance.

        For a non-existed dataset, when you try to load it, you will get an Exception; when you try to append records and commit,
        the dataset will be created automatically.

        Arguments:
            uri: (str, Resource, required) The dataset uri str or Resource object.
            create: (str, optional) The mode of dataset creating. The options are `auto`, `empty` and `forbid`.
                `auto` mode: If the dataset already exists, creation is ignored. If it does not exist, the dataset is created automatically.
                `empty` mode: If the dataset already exists, an Exception is raised; If it does not exist, an empty dataset is created. This mode ensures the creation of a new, empty dataset.
                `forbid` mode: If the dataset already exists, nothing is done.If it does not exist, an Exception is raised. This mode ensures the existence of the dataset.
                The default is `auto`.
            readonly: (bool, optional) For an existing dataset, you can specify the readonly=True argument to ensure
                the dataset is in readonly mode. Default is False.

        Returns:
            A Dataset Object

        Examples:
        ```python
        from starwhale import dataset, Image

        # create a new dataset named mnist, and add a row into the dataset
        # dataset("mnist") is equal to dataset("mnist", create="auto")
        ds = dataset("mnist")
        ds.exists()  # return False, "mnist" dataset is not existing.
        ds.append({"img": Image(), "label": 1})
        ds.commit()
        ds.close()

        # load a cloud instance dataset in readonly mode
        ds = dataset("cloud://remote-instance/project/starwhale/dataset/mnist", readonly=True)
        labels = [row.features.label in ds]
        ds.close()

        # load a read/write dataset with a specified version
        ds = dataset("mnist/version/mrrdczdbmzsw")
        ds[0].features.label = 1
        ds.commit()
        ds.close()

        # create an empty dataset
        ds = dataset("mnist-empty", create="empty")

        # ensure the dataset existence
        ds = dataset("mnist-existed", create="forbid")
        ```

        """
        if isinstance(uri, str):
            _uri = Resource(uri, typ=ResourceType.dataset, refine=False)
        elif isinstance(uri, Resource) and uri.typ == ResourceType.dataset:
            _uri = uri
        else:
            raise TypeError(
                f"uri({uri}) argument type is not expected, dataset uri or str is ok"
            )

        ds = Dataset(
            uri=_uri,
            readonly=readonly,
            create=create or _DatasetCreateMode.auto,
        )

        return ds

    @classmethod
    def from_huggingface(
        cls,
        name: str,
        repo: str,
        subsets: t.List[str] | None = None,
        split: str | None = None,
        revision: str = "main",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
        cache: bool = True,
        tags: t.List[str] | None = None,
        add_info: bool = True,
    ) -> Dataset:
        """Create a new dataset from huggingface datasets.

        The dataset created by the huggingface will use the f"{split}/index" or str(index) as the row index.

        Arguments:
            name: (str, required) The dataset name you would like to use.
            repo: (str, required) The huggingface datasets repo name.
            subsets: (list(str), optional) The list of subset names. If the subset names are not specified, the all subsets dataset will be built.
            split: (str, optional) The split name. If the split name is not specified, the all splits dataset will be built.
            revision: (str, optional) The huggingface datasets revision. The default value is `main`. The option value accepts tag name, or branch name, or commit hash.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The maximum size of a dataset blob file. A new blob file will be generated when the size exceeds this limit.
              The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.
            cache: (bool, optional) Whether to use huggingface dataset cache(download + local hf dataset). The default value is True.
            tags: (list(str), optional) The tags for the dataset version.
            add_info: (bool, optional) Whether to add huggingface dataset info to the dataset rows,
              currently support to add subset and split into the dataset rows.
              subset uses _hf_subset field name, split uses _hf_split field name.
              The default value is True.

        Returns:
                A Dataset Object

        Examples:
        ```python
        from starwhale import Dataset
        myds = Dataset.from_huggingface("mnist", "mnist")
        print(myds[0])
        ```

        ```python
        from starwhale import Dataset
        myds = Dataset.from_huggingface("mmlu", "cais/mmlu", subsets=["anatomy"], split="auxiliary_train", revision="7456cfb")
        ```
        """
        from starwhale.integrations.huggingface import iter_dataset

        # TODO: support auto build all subset datasets
        # TODO: support huggingface dataset info
        data_items = iter_dataset(
            repo=repo,
            subsets=subsets,
            split=split,
            revision=revision,
            cache=cache,
            add_info=add_info,
        )

        return cls.from_dict_items(
            data_items,
            name=name,
            volume_size=volume_size,
            alignment_size=alignment_size,
            mode=mode,
            tags=tags,
        )

    @classmethod
    def from_json(
        cls,
        name: str,
        path: PathLike | t.List[PathLike] | None = None,
        text: str | None = None,
        field_selector: str = "",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
        tags: t.List[str] | None = None,
        encoding: str | None = None,
    ) -> Dataset:
        """Create a new dataset from a json text.

        The dataset created by the json text will use the auto increment index as the row index.

        path and text arguments are mutually exclusive, one of them must be specified.

        Arguments:
            name: (str, required) The dataset name you would like to use.
            path: (str|Path|List[str]|List[Path], optional) Json or json line files.
            text: (str, optional) The json text from which you would like to create this dataset.
            field_selector: (str, optional) The filed from which you would like to extract dataset array items.
                The default value is "" which indicates that the json object is an array contains all the items.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The blob volume size. The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.
            tags: (list(str), optional) The tags for the dataset version.`latest` and `^v\d+$` tags are reserved tags.
            encoding: (str, optional) The encoding used to decode the input file. The default is None.
                encoding does not support text parameter.

        Returns:
                A Dataset Object

        Json text format:

        ```json
        [
            {"a": 1, "b": 2},
            {"a": 10, "b": 20},
        ]
        ```
        Using field_selector: p1.p2.p3 to extract dataset array items:
        ```json
        {
            "p1": {
                "p2":{
                    "p3": [
                        {"a": 1, "b": 2},
                        {"a": 10, "b": 20},
                    ]
                }
            }
        }
        ```

        Json line text format:
        ```jsonl
        {"a": 1, "b": 2}
        {"a": 10, "b": 20}
        ```
        Using field_selector: p1.p2.p3 to extract dataset array items:
        ```jsonl
        {"p1": {"p2": {"p3": {"a": 1, "b": 2}}}}
        {"p1": {"p2": {"p3": {"a": 10, "b": 20}}}}
        ```

        Examples:
        ```python
        from starwhale import Dataset
        myds = Dataset.from_json(
            name="translation",
            text='[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]'
        )
        print(myds[0].features.en)
        ```

        ```python
        from starwhale import Dataset
        myds = Dataset.from_json(
            name="translation",
            text='{"content":{"child_content":[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]}}',
            field_selector="content.child_content"
        )
        print(myds[0].features["zh-cn"])
        ```

        ```python
        from starwhale import Dataset
        # create a dataset from /path/to/data.json file.
        Dataset.from_json(path="/path/to/data.json", name="myds"))

        # create a dataset from /path/to/dir folder.
        Dataset.from_json(path="/path/to/dir", name="myds")

        # create a dataset from /path/to/data1.json, /path/to/data2.json
        Dataset.from_json(path=["/path/to/data1.json", "/path/to/data2.json"], name="myds")

        # create a dataset from http://example.com/data.json file.
        Dataset.from_json(path="http://example.com/data.json", name="myds")
        ```
        """

        def _selector(_data: t.Sequence | t.Dict) -> t.Sequence | t.Dict:
            if field_selector:
                fields = field_selector.split(".")
                for field in fields:
                    if not isinstance(_data, dict):
                        raise NoSupportError(
                            f"field_selector only supports dict type: {_data}"
                        )
                    if field in _data:
                        _data = _data[field]
                    else:
                        raise ValueError(
                            f"The field_selector {field_selector} isn't in json text: {_data}"
                        )
            return _data

        def _decode() -> t.Iterable[t.Sequence | t.Dict]:
            if path is not None and text is not None:
                raise ValueError("paths and text arguments are mutually exclusive")
            elif path:
                for fp, suffix in iter_pathlike_io(
                    path, encoding=encoding, accepted_file_types=[".json", ".jsonl"]
                ):
                    if suffix == ".json":
                        yield _selector(json.load(fp))
                    elif suffix == ".jsonl":
                        for line in fp.readlines():
                            _r = json.loads(line)
                            _r = _selector(_r)
                            if isinstance(_r, dict):
                                _r = [_r]
                            yield _r
                    else:
                        raise ValueError(f"unsupported file type: {suffix}")
            elif text:
                yield _selector(json.loads(text))
            else:
                raise ValueError("paths or text argument must be specified")

        def _iter(_iter: t.Iterable[t.Sequence | t.Dict]) -> _IterFeatureDict:
            for i in _iter:
                if isinstance(i, (list, tuple)):
                    for j in i:
                        yield j
                elif isinstance(i, dict):
                    yield i
                else:
                    raise ValueError(f"json text:{i} must be dict, list or tuple type")

        return cls.from_dict_items(
            _iter(_decode()),
            name=name,
            volume_size=volume_size,
            alignment_size=alignment_size,
            mode=mode,
            tags=tags,
        )

    @classmethod
    def from_csv(
        cls,
        path: PathLike | t.List[PathLike],
        name: str,
        dialect: str = "excel",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
        tags: t.List[str] | None = None,
        delimiter: str = ",",
        quotechar: str = '"',
        skipinitialspace: bool = False,
        strict: bool = False,
        encoding: str | None = None,
    ) -> Dataset:
        """Create a new dataset from one and more csv files.

        The dataset created by the csv files will use the auto increment index as the row index.

        All fields in the csv file will be treated as string type.

        Arguments:
            path: (str|Path|List[str]|List[Path], required) The csv file path or a list of csv file paths from which you would like to create this dataset.
              - If the element filename ends with .csv, the function will create the specified csv file as a dataset.
              - If the element is a local directory, the function will create a dataset from all csv files in the directory and its subdirectories.
              - If the element is a http url and ends with .csv, the function will download the csv file and create a dataset from it.
            name: (str, required) The dataset name.
            dialect: (str, optional) The csv dialect name which following the design of python standard csv lib. The default value is `excel`.
              The option accepts `excel`, `excel-tab` and `unix` dialects.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The blob volume size. The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.
            tags: (list(str), optional) The tags for the dataset version.`latest` and `^v\d+$` tags are reserved tags.
            delimiter: (str, optional) A one-character string used to separate fields. It defaults to ','.
            quotechar: (str, optional) A one-character string used to quote fields containing special characters,
              such as the delimiter or quotechar, or which contain new-line characters. It defaults to '"'.
            skipinitialspace: (bool, optional) When True, whitespace immediately following the delimiter is ignored.
              The default is False.
            strict: (bool, optional) When True, raise exception Error if the csv is not well formed. The default is False.
            encoding: (str, optional) The encoding used to decode the input file. The default is None.

        Returns:
            A Dataset Object

        Examples:
        ```python
        from starwhale import Dataset

        # create a dataset from /path/to/data.csv file.
        ds = Dataset.from_csv(path="/path/to/data.csv", name="my-csv-dataset")

        # create a dataset from /path/to/data.csv file with utf-8 encoding.
        ds = Dataset.from_csv(path="/path/to/data.csv", name="my-csv-dataset", encoding="utf-8")

        # create a dataset from /path/to/dir folder.
        ds = Dataset.from_csv(path="/path/to/dir", name="my-csv-dataset")

        # create a dataset from /path/to/data1.cvs, /path/to/data2.csv
        ds = Dataset.from_csv(path=["/path/to/data1.csv", "/path/to/data2.csv"], name="my-csv-dataset")

        # create a dataset from http://example.com/data.csv file.
        ds = Dataset.from_csv(path="http://example.com/data.csv", name="my-csv-dataset")
        ```
        """

        def _iter_records() -> _IterFeatureDict:
            for fp, _ in iter_pathlike_io(
                path=path, encoding=encoding, newline="", accepted_file_types=[".csv"]
            ):
                for record in csv.DictReader(
                    fp,  # type: ignore
                    dialect=dialect,
                    delimiter=delimiter,
                    quotechar=quotechar,
                    skipinitialspace=skipinitialspace,
                    strict=strict,
                ):
                    yield record

        return cls.from_dict_items(
            _iter_records(),
            name=name,
            volume_size=volume_size,
            alignment_size=alignment_size,
            mode=mode,
            tags=tags,
        )

    @classmethod
    def from_dict_items(
        cls,
        records: t.Iterable[
            t.Dict[str, t.Any] | t.Tuple[str | int, t.Dict[str, t.Any]]
        ],
        name: str | Resource,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
        tags: t.List[str] | None = None,
    ) -> Dataset:
        """Create a new dataset from a dict iterator or a list[dict].

        The dataset created by the dict items will use the auto increment index or user custom index parsed from record[0] as the row index.

        Arguments:
            records: (Iterable[dict]|Iterable[(str|int, dict)], required) The dict or (str|int, dict) iterator from which you would like to create this dataset. )
            name: (str|Resource, required) The dataset name.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The blob volume size. The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.
            tags: (list(str), optional) The tags for the dataset version.`latest` and `^v\d+$` tags are reserved tags.

        Returns:
            A Dataset Object

        Examples:
        ```python
        from starwhale import Dataset
        # create a dataset from a list of dict
        ds = Dataset.from_dict_items([{"a": 1, "b: 2, "c": 3}], name="my-dataset")

        # create a dataset from a dict iterator
        ds = Dataset.from_dict_items(iter([{"a": 1, "b: 2, "c": 3}]), name="my-dataset")

        # create a dataset from a dict generator
        def _iter_records():
            for i in range(10):
                yield {"a": i, "b": i * 2, "c": i * 3}
        ds = Dataset.from_dict_items(_iter_records(), name="my-dataset")

        # create a dataset from a dict iterator with key
        ds = Dataset.from_dict_items(iter([(1, {"a": 1, "b: 2, "c": 3})]), name="my-dataset")
        ```
        """
        mode = DatasetChangeMode(mode)
        StandaloneTag.check_tags_validation(tags)

        with cls.dataset(name) as ds:
            console.print(f":ocean: creating dataset {ds.uri}...")
            ds = ds.with_builder_blob_config(
                volume_size=volume_size, alignment_size=alignment_size
            )

            if mode == DatasetChangeMode.OVERWRITE:
                for row in ds:
                    del ds[row.index]

            total = 0
            for record in records:
                if isinstance(record, (tuple, list)):
                    key, record = record
                    ds[key] = record
                elif isinstance(record, dict):
                    ds.append(record)
                else:
                    raise TypeError(
                        f"record type {type(record)} is not expected, dict or tuple is ok"
                    )
                total += 1

            console.print(f":butterfly: update {total} records into dataset")
            ds.commit(tags=tags)

        return ds

    @classmethod
    def from_folder(
        cls,
        folder: PathLike,
        kind: str | DatasetFolderSourceType,
        name: str | Resource = "",
        auto_label: bool = True,
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
        tags: t.List[str] | None = None,
    ) -> Dataset:
        """Create a dataset from a folder of image/video/audio files.

        The image/video/audio folder building supports the following features:

        - search the folder recursively.
        - support three kinds of folder type:
          - `image`: png/jpg/jpeg/webp/svg/apng types. The image file will be converted to Starwhale.Image type.
          - `video`: mp4/webm/avi types. The video file will be converted to Starwhale.Video type.
          - `audio`: mp3/wav types. The audio file will be converted to Starwhale.Audio type.
        - If `auto_label=True`, the name of the parent directory will be used as the label for that data item, corresponding to the `label` field. Files in the root directory will not be labeled.
        - auto fill caption with the txt file which is named the same as the image file, and located in the same folder.
        - auto import metadata.csv or metadata.jsonl (in the root dir) as the additional metadata information.

        When the auto_label is True, the function will try to ingest the label from the image folder(sub-folder) name.
        If the image file's folder is equal to the root folder, the label is empty.
        For example, the following structure will create a dataset with 2 labels: "cat" and "dog", 4 images in total.

        ```
        folder/dog/1.png
        folder/cat/2.png
        folder/dog/3.png
        folder/cat/4.png
        ```

        metadata.csv example:

        ```
        file_name, caption
        1.png, dog
        2.png, cat
        ```

        metadata.jsonl example:

        ```
        {"file_name": "1.png", "caption": "dog"}
        {"file_name": "2.png", "caption": "cat"}
        ```

        Metadata.csv and metadata.jsonl are mutually exclusive, if both exist, the exception will be raised.
        Metadata.csv or metadata.jsonl file must be located in the root folder.
        The metadata should include the file_name field which is the same as the image file path.
        Metadata.csv or metadata.jsonl file is optional for dataset.

        auto caption example: 1.txt content will be used as the caption of 1.png.

        ```
        folder/dog/1.png
        folder/dog/1.txt
        ```
        The dataset created by the folder will use the relative path name as the row index.

        Arguments:
            folder: (str|Path, required) The folder path from which you would like to create this dataset.
            kind: (str|DatasetFolderSourceType, required) The dataset source type you would like to use, the choices are: image, video and audio.
               Recursively searching for files of the specified `kind` in `folder`. Other file types will be ignored.
            name: (str|Resource, optional) The dataset name you would like to use. If not specified, the name is the folder name.
            auto_label: (bool, optional) Whether to auto label by the sub-folder name. The default value is True.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The blob volume size. The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.
            tags: (list(str), optional) The tags for the dataset version. `latest` and `^v\d+$` tags are reserved tags.

        Returns:
            A Dataset Object.

        Examples:
        ```python
        from starwhale import Dataset

        # create a my-image-dataset dataset from /path/to/image folder.
        ds = Dataset.from_folder(
            folder="/path/to/image",
            kind="image",
            name="my-image-dataset"
        )
        ```
        """
        rootdir = Path(folder)
        if not rootdir.exists():
            raise RuntimeError(f"folder {rootdir} doesn't exist")

        name = name or rootdir.name
        mode = DatasetChangeMode(mode)

        def _read_meta() -> t.Dict:
            # TODO: support multi metadata files
            _meta_csv_path = rootdir / "metadata.csv"
            _meta_jsonl_path = rootdir / "metadata.jsonl"

            if _meta_csv_path.exists() and _meta_jsonl_path.exists():
                raise RuntimeError(
                    "metadata.csv and metadata.jsonl are mutually exclusive"
                )

            _meta = {}
            if _meta_csv_path.exists():
                with _meta_csv_path.open(newline="") as f:
                    for record in csv.DictReader(f):
                        _meta[record["file_name"]] = record
            elif _meta_jsonl_path.exists():
                with jsonlines.open(_meta_jsonl_path) as reader:
                    for record in reader:
                        _meta[record["file_name"]] = record
            return _meta

        def _iter_records() -> t.Iterator[t.Tuple[str, t.Dict]]:
            from starwhale.base.data_type import Audio, Image, Video

            _dfst = DatasetFolderSourceType
            file_types_map = {
                _dfst.IMAGE: (
                    (".png", ".jpg", ".jpeg", ".webp", ".svg", ".apng"),
                    Image,
                ),
                _dfst.AUDIO: ((".mp3", ".wav"), Audio),
                _dfst.VIDEO: ((".mp4", ".webm", ".avi"), Video),
            }

            meta = _read_meta()
            accepted_file_types, file_cls = file_types_map[_dfst(kind)]

            for p in rootdir.rglob("*"):
                if not p.is_file():
                    continue

                if p.suffix not in accepted_file_types:
                    continue

                file_name = str(p.relative_to(rootdir))
                record = {
                    "file_name": file_name,
                }
                record.update(meta.get(file_name, {}))
                if auto_label:
                    # TODO: support more complicated label pattern
                    relative_name = str(p.parent.relative_to(rootdir))
                    if relative_name and relative_name != ".":
                        record["label"] = p.parent.name

                caption_path = p.parent / f"{p.stem}.txt"
                if caption_path.exists():
                    record["caption"] = caption_path.read_text().strip()

                record["file"] = file_cls(
                    fp=p,
                    display_name=p.name,
                    mime_type=MIMEType.create_by_file_suffix(p),
                )
                yield record["file_name"], record

        return cls.from_dict_items(
            _iter_records(),
            name=name,
            volume_size=volume_size,
            alignment_size=alignment_size,
            mode=mode,
            tags=tags,
        )
