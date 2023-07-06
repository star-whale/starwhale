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
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    DEFAULT_MANIFEST_NAME,
    ENV_BUILD_BUNDLE_FIXED_VERSION_FOR_TEST,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.utils.fs import copy_file, empty_dir, ensure_dir, ensure_file
from starwhale.base.type import DatasetChangeMode, DatasetFolderSourceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import NoSupportError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.project import Project
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.type import (
    MIMEType,
    DatasetSummary,
    D_ALIGNMENT_SIZE,
    D_FILE_VOLUME_SIZE,
)
from starwhale.core.dataset.model import Dataset as CoreDataset
from starwhale.core.dataset.model import StandaloneDataset
from starwhale.core.dataset.store import DatasetStorage
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

from .loader import DataRow, DataLoader, get_data_loader
from .builder import MappingDatasetBuilder

if t.TYPE_CHECKING:
    import tensorflow as tf
    from torch.utils.data import Dataset as TorchDataset

_DType = t.TypeVar("_DType", bound="Dataset")
_ItemType = t.Union[str, int, slice]
_GItemType = t.Optional[t.Union[DataRow, t.List[DataRow]]]

_DEFAULT_LOADER_WORKERS = 2
_DEFAULT_LOADER_CACHE_SIZE = 20


class _DatasetCreateMode:
    auto = "auto"
    empty = "empty"
    forbid = "forbid"


class _Tags:
    def __init__(self, core_dataset: CoreDataset) -> None:
        self.__core_dataset = core_dataset

    def add(self, tags: t.Union[str, t.List[str]], ignore_errors: bool = False) -> None:
        if isinstance(tags, str):
            tags = [tags]
        self.__core_dataset.add_tags(tags, ignore_errors)

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
                self._total_rows = 0
                self._total_blobs_size = 0
            else:
                self._total_rows = _summary.rows
                self._total_blobs_size = _summary.blobs_byte_size
        else:
            self._total_rows = 0
            self._total_blobs_size = 0

        self._last_data_datastore_revision = ""
        self._last_info_datastore_revision = ""

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
        return self._total_rows

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

    @lru_cache(maxsize=32)
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
                path=f"/project/{uri.project.name}/dataset/{uri.name}",
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
        """Batch data into lists of length n. The last batch may be shorter."""
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
                path=f"/project/{uri.project.name}/{ResourceType.dataset.value}/{uri.name}/version/{uri.version}",
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

    def manifest(self) -> t.Dict[str, t.Any]:
        return self.__loading_core_dataset.info()

    def head(self, n: int = 5, skip_fetch_data: bool = False) -> t.List[DataRow]:
        # TODO: render artifact in JupyterNotebook
        ret = []
        loader = self._get_data_loader(disable_consumption=True)
        for idx, td_row in enumerate(loader._iter_meta()):
            if idx >= n:
                break
            data_row = loader._unpack_row(td_row, skip_fetch_data=skip_fetch_data)
            ret.append(data_row)
        return ret

    def fetch_one(self, skip_fetch_data: bool = False) -> DataRow:
        return self.head(1, skip_fetch_data)[0]

    def to_pytorch(
        self,
        transform: t.Optional[t.Callable] = None,
        drop_index: bool = True,
        skip_default_transform: bool = False,
    ) -> TorchDataset:
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
        # TODO improve accuracy of _total_rows during building
        self._total_rows += 1

        _ds_builder = self._get_dataset_builder()
        _ds_builder.put(row)
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
            _ds_builder.delete(item.index)
            self._deleted_rows_by_commit += 1
            self._total_rows -= 1

    @_check_readonly
    def append(self, item: t.Any) -> None:
        if isinstance(item, DataRow):
            self.__setitem__(item.index, item)
        elif isinstance(item, dict):
            self.__setitem__(self._total_rows, item)
        elif isinstance(item, (list, tuple)):
            if len(item) == 1:
                row = DataRow(self._total_rows, item[0])
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
        the Standalone instance ,or Cloud instance.

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
    def commit(self, tags: t.Optional[t.List[str]] = None, message: str = "") -> str:
        """Commit into dataset
        Commit will flush and generate a version of the dataset. At the same time, commit
        operation will also generate auto-increment tag, such as v0, v1, v2. Only one commit is allowed.

        Arguments:
            tags: (list(str), optional) Specify the tags for the version. Default is None.
            message: (str, optional) Commit message. Default is empty str.

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
            return self._commit(tags or [], message)

    def _commit(self, tags: t.List[str], message: str) -> str:
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
                "project": self._uri.project.name,
                "force": "1",  # stash version is unique, use force=1 to make http retry happy
            }
            url_path = f"/project/{self._uri.project.name}/dataset/{self._uri.name}/version/{self._pending_commit_version}/file"
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
            self.__pending_commit_core_dataset.add_tags(tags)

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
    ) -> None:
        """Copy dataset to another instance.

        Args:
            dest_uri: (str, required) destination dataset uri
            dest_local_project_uri: (str, optional) destination local project uri
            force: (bool, optional) force to copy
            mode: (str, optional) copy mode, default is 'patch'. Mode choices are: 'patch', 'overwrite'.
              `patch` mode: only update the changed rows and columns for the remote dataset;
              `overwrite` mode: update records and delete extraneous rows from the remote dataset
        Returns:
            None
        """
        CoreDataset.copy(
            self.uri,
            dest_uri,
            dest_local_project_uri=dest_local_project_uri,
            force=force,
            mode=DatasetChangeMode(mode),
        )

    @classmethod
    def list(
        cls,
        project_uri: t.Union[str, Project] = "",
        fullname: bool = False,
        show_removed: bool = False,
        page_index: int = DEFAULT_PAGE_IDX,
        page_size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
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
            uri: (str, Resource, required) The dataset uri.
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
        subset: str | None = None,
        split: str | None = None,
        revision: str = "main",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
        cache: bool = True,
    ) -> Dataset:
        """Create a new dataset from huggingface datasets.

        Arguments:
            name: (str, required) The dataset name you would like to use.
            repo: (str, required) The huggingface datasets repo name.
            subset: (str, optional) The subset name. If the huggingface dataset has multiple subsets, you must specify the subset name.
            split: (str, optional) The split name. If the split name is not specified, the all splits dataset will be built.
            revision: (str, optional) The huggingface datasets revision. The default value is `main`. If the split name is not specified, the all splits dataset will be built.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The blob volume size. The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.
            cache: (bool, optional) Whether to use huggingface dataset cache(download + local hf dataset). The default value is True.

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
        myds = Dataset.from_huggingface("mmlu", "cais/mmlu", subset="anatomy", split="auxiliary_train", revision="7456cfb")
        ```
        """
        from starwhale.integrations.huggingface import iter_dataset

        # TODO: support auto build all subset datasets
        # TODO: support huggingface dataset info
        data_items = iter_dataset(
            repo=repo,
            subset=subset,
            split=split,
            revision=revision,
            cache=cache,
        )

        with cls.dataset(name) as ds:
            ds = ds.with_builder_blob_config(
                volume_size=volume_size,
                alignment_size=alignment_size,
            )

            if mode == DatasetChangeMode.OVERWRITE:
                # TODO: use other high performance way to delete all records
                for row in ds:
                    del ds[row.index]

            total = 0
            for key, item in data_items:
                ds[key] = item
                total += 1

            console.print(f":butterfly: update {total} records into dataset")
            ds.commit()

        return ds

    @classmethod
    def from_json(
        cls,
        name: str,
        json_text: str,
        field_selector: str = "",
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
    ) -> Dataset:
        """Create a new dataset from a json text.

        Arguments:
            name: (str, required) The dataset name you would like to use.
            json_text: (str, required) The json text from which you would like to create this dataset.
            field_selector: (str, optional) The filed from which you would like to extract dataset array items.
                The default value is "" which indicates that the dict is an array contains all the items.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The blob volume size. The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.

        Returns:
                A Dataset Object

        Examples:
        ```python
        from starwhale import Dataset
        myds = Dataset.from_json(
            "translation",
            '[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]'
        )
        print(myds[0].features.en)
        ```

        ```python
        from starwhale import Dataset
        myds = Dataset.from_json(
            "translation",
            '{"content":{"child_content":[{"en":"hello","zh-cn":"你好"},{"en":"how are you","zh-cn":"最近怎么样"}]}}',
            "content.child_content"
        )
        print(myds[0].features["zh-cn"])
        ```
        """
        mode = DatasetChangeMode(mode)
        data_items = json.loads(json_text)

        if field_selector:
            # Split field selector by dots
            fields = field_selector.split(".")
            # Iterate over selected fields
            for field in fields:
                if field in data_items:
                    data_items = data_items[field]
                else:
                    raise ValueError(
                        f"The field_selector {field_selector} isn't in json_text: {json_text}"
                    )
        if not isinstance(data_items, list):
            raise ValueError(
                f"The field selected by field_selector {field_selector} isn't an array: {data_items}"
            )

        with cls.dataset(name) as ds:
            ds = ds.with_builder_blob_config(
                volume_size=volume_size,
                alignment_size=alignment_size,
            )

            if mode == DatasetChangeMode.OVERWRITE:
                # TODO: use other high performance way to delete all records
                for row in ds:
                    del ds[row.index]

            total = 0
            for item in data_items:
                ds.append(item)
                total += 1

            console.print(f":butterfly: update {total} records into dataset")
            ds.commit()
        return ds

    @classmethod
    def from_folder(
        cls,
        folder: str | Path,
        kind: str | DatasetFolderSourceType,
        name: str | Resource = "",
        auto_label: bool = True,
        alignment_size: int | str = D_ALIGNMENT_SIZE,
        volume_size: int | str = D_FILE_VOLUME_SIZE,
        mode: DatasetChangeMode | str = DatasetChangeMode.PATCH,
    ) -> Dataset:
        """Create a dataset from a folder of image files.

        The image folder building supports the following features:

        - search the folder recursively.
        - support three kinds of folder type:
          - `image`: png/jpg/jpeg/webp/svg/apng types. The image file will be converted to Starwhale.Image type.
          - `video`: mp4/webm/avi types. The video file will be converted to Starwhale.Video type.
          - `audio`: mp3/wav types. The audio file will be converted to Starwhale.Audio type.
        - auto labeling by the common prefix parent dir name.
        - auto fill caption with the txt file which is named the same as the image file, and located in the same folder.
        - auto import metadata.csv or metadata.jsonl as the additional metadata information.

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

        Arguments:
            folder: (str|Path, required) The folder path from which you would like to create this dataset.
            kind: (str|DatasetFolderSourceType, required) The dataset source type you would like to use, the choices are: image, video and audio.
            name: (str|Resource, optional) The dataset name you would like to use. If not specified, the name is the folder name.
            auto_label: (bool, optional) Whether to auto label by the sub-folder name. The default value is True.
            alignment_size: (int|str, optional) The blob alignment size. The default value is 128.
            volume_size: (int|str, optional) The blob volume size. The default value is 64MB.
            mode: (str|DatasetChangeMode, optional) The dataset change mode. The default value is `patch`. Mode choices are `patch` and `overwrite`.

        Returns:
            A Dataset Object.

        Examples:
        ```python
        from starwhale import Dataset

        ds = Dataset.from_folder("/path/to/image", "image", "my-image-dataset")  # create a my-image-dataset dataset from /path/to/image folder.
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
                with _meta_csv_path.open() as f:
                    for record in csv.DictReader(f):
                        _meta[record["file_name"]] = record
            elif _meta_jsonl_path.exists():
                with jsonlines.open(_meta_jsonl_path) as reader:
                    for record in reader:
                        _meta[record["file_name"]] = record
            return _meta

        def _iter_records() -> t.Iterator[t.Dict]:
            from starwhale.core.dataset.type import Audio, Image, Video

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
                yield record

        with cls.dataset(name) as ds:
            console.print(f":ocean: creating dataset {ds.uri} from folder {rootdir}...")
            ds = ds.with_builder_blob_config(
                volume_size=volume_size,
                alignment_size=alignment_size,
            )

            if mode == DatasetChangeMode.OVERWRITE:
                # TODO: use other high performance way to delete all records
                for row in ds:
                    del ds[row.index]

            total = 0
            for record in _iter_records():
                ds[record["file_name"]] = record
                total += 1

            console.print(f":butterfly: update {total} records into dataset")
            ds.commit()

        return ds
