from __future__ import annotations

import os
import typing as t
import platform
import threading
from http import HTTPStatus
from types import TracebackType
from pathlib import Path
from functools import wraps
from itertools import islice

import yaml
from loguru import logger

from starwhale.utils import now_str, gen_uniq_version
from starwhale.consts import (
    FileDesc,
    HTTPMethod,
    CREATED_AT_KEY,
    SW_AUTO_DIRNAME,
    DEFAULT_PAGE_IDX,
    DEFAULT_PAGE_SIZE,
    STANDALONE_INSTANCE,
    DEFAULT_MANIFEST_NAME,
)
from starwhale.version import STARWHALE_VERSION
from starwhale.base.uri import URI, URIType
from starwhale.utils.fs import copy_file, ensure_dir, ensure_file
from starwhale.base.type import InstanceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import NoSupportError
from starwhale.utils.retry import http_retry
from starwhale.core.dataset.type import DatasetSummary
from starwhale.core.dataset.model import Dataset as CoreDataset
from starwhale.core.dataset.model import StandaloneDataset
from starwhale.core.dataset.store import DatasetStorage
from starwhale.api._impl.data_store import TableEmptyException
from starwhale.core.dataset.tabular import (
    TabularDataset,
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
        name: str,
        version: str,
        project_uri: URI,
        create: bool = False,
        readonly: bool = False,
    ) -> None:
        self.name = name
        self.project_uri = project_uri
        self.__readonly = readonly

        _origin_uri = URI.capsulate_uri(
            self.project_uri.instance,
            self.project_uri.project,
            URIType.DATASET,
            self.name,
            version or "latest",
        )
        self._pending_commit_version = gen_uniq_version()

        origin_uri_exists = self._check_uri_exists(_origin_uri)
        if origin_uri_exists:
            if create:
                raise RuntimeError(
                    f"dataset already existed, failed to create: {self.name}"
                )
            self._loading_version = self._auto_complete_version(version)
        else:
            if readonly:
                raise ValueError(
                    f"no support to set a non-existed dataset to the readonly mode: {self.name}"
                )

            if not create:
                raise RuntimeError(
                    "for the non-existed dataset, you should set create=True to create dataset automatically"
                )

            if version != "":
                raise NoSupportError(
                    f"no support to create a specified version dataset: {version}"
                )
            self._loading_version = self._pending_commit_version

        self.uri = URI.capsulate_uri(
            self.project_uri.instance,
            self.project_uri.project,
            URIType.DATASET,
            self.name,
            self._loading_version,
        )
        self.__core_dataset = CoreDataset.get_dataset(self.uri)

        self._pending_commit_uri = URI.capsulate_uri(
            self.project_uri.instance,
            self.project_uri.project,
            URIType.DATASET,
            self.name,
            self._pending_commit_version,
        )
        self.__pending_commit_core_dataset = CoreDataset.get_dataset(
            self._pending_commit_uri
        )

        self._consumption: t.Optional[TabularDatasetSessionConsumption] = None
        self._loader_lock = threading.Lock()
        self.__data_loaders: t.Dict[str, DataLoader] = {}
        self._loader_cache_size = _DEFAULT_LOADER_CACHE_SIZE
        self._loader_num_workers = _DEFAULT_LOADER_WORKERS

        self._builder_lock = threading.Lock()
        self._dataset_builder: t.Optional[MappingDatasetBuilder] = None
        self._commit_lock = threading.Lock()
        self.__has_committed = False

        self._info_lock = threading.Lock()
        self._info_ds_wrapper: t.Optional[DatastoreWrapperDataset] = None
        self.__info: t.Optional[TabularDatasetInfo] = None

        self._workdir = Path(f"{SW_AUTO_DIRNAME}/dataset")
        self._updated_rows_by_commit = 0
        self._deleted_rows_by_commit = 0
        if create:
            self._total_rows = 0
        else:
            _summary = self.__core_dataset.summary()
            # TODO: raise none summary exception for existed dataset
            self._total_rows = 0 if _summary is None else _summary.rows

    def _auto_complete_version(self, version: str) -> str:
        version = version.strip()
        if not version:
            return version

        if self.project_uri.instance_type == InstanceType.CLOUD:
            return version

        _uri = URI.capsulate_uri(
            instance=self.project_uri.instance,
            project=self.project_uri.project,
            obj_type=URIType.DATASET,
            obj_name=self.name,
            obj_ver=version,
        )
        store = DatasetStorage(_uri)
        if not store.snapshot_workdir.exists():
            return version
        else:
            return store.id

    def __str__(self) -> str:
        return f"Dataset: {self.name}, stash version: {self._pending_commit_version}, loading version: {self._loading_version}"

    def __repr__(self) -> str:
        return f"Dataset: {self.uri}"

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
            logger.warning(f"type:{type}, exception:{value}, traceback:{trace}")

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

    # TODO： remove this function after datastore supports timestamp
    def _use_loading_version_for_loader(self) -> Dataset:
        self._use_loading_version = True
        return self

    def _clear_data_loader(self) -> None:
        with self._loader_lock:
            self.__data_loaders = {}

    def with_loader_config(
        self, num_workers: t.Optional[int] = None, cache_size: t.Optional[int] = None
    ) -> Dataset:
        if len(self.__data_loaders) != 0:
            raise RuntimeError(
                f"loaders({list(self.__data_loaders)}) have already been initialized"
            )

        with self._loader_lock:
            if num_workers is not None:
                self._loader_num_workers = num_workers

            if cache_size is not None:
                self._loader_cache_size = cache_size

        return self

    def _get_data_loader(
        self, recreate: bool = False, disable_consumption: bool = False
    ) -> DataLoader:
        with self._loader_lock:
            # TODO: use datastore timestamp to load specified version data
            if hasattr(self, "_use_loading_version"):
                version = self._loading_version
            else:
                version = MappingDatasetBuilder._HOLDER_VERSION

            key = f"consumption-{disable_consumption}-{version}"

            _loader = self.__data_loaders.get(key)
            if _loader is None or recreate:
                if disable_consumption:
                    consumption = None
                else:
                    consumption = self._consumption

                _uri = URI.capsulate_uri(
                    self.project_uri.instance,
                    self.project_uri.project,
                    URIType.DATASET,
                    self.name,
                    version,
                )

                _loader = get_data_loader(
                    _uri,
                    session_consumption=consumption,
                    cache_size=self._loader_cache_size,
                    num_workers=self._loader_num_workers,
                )
                self.__data_loaders[key] = _loader

        return _loader

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
        return _Tags(self.__core_dataset)

    @staticmethod
    def _check_uri_exists(uri: t.Optional[URI]) -> bool:
        if uri is None or uri.object.version == "":
            return False

        if uri.instance_type == InstanceType.CLOUD:
            crm = CloudRequestMixed()
            ok, _ = crm.do_http_request_simple_ret(
                path=f"/project/{uri.project}/{URIType.DATASET}/{uri.object.name}/version/{uri.object.version}",
                method=HTTPMethod.HEAD,
                instance_uri=uri,
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
        if self.__info is not None:
            return self.__info

        with self._info_lock:
            # TODO: use uniform dataset table to store info
            self._info_ds_wrapper = TabularDataset.from_uri(self.uri)._info_ds_wrapper
            self.__info = TabularDatasetInfo.load_from_datastore(self._info_ds_wrapper)

        return self.__info

    @_check_readonly
    def flush(self, artifacts_flush: bool = False) -> None:
        if self._dataset_builder:
            self._dataset_builder.flush(artifacts_flush)

        loader = self._get_data_loader(disable_consumption=True)
        loader.tabular_dataset.flush()

    @_check_readonly
    def rehash(self) -> None:
        # TODO: rehash for swds-bin format dataset with append/delete items to reduce volumes size
        raise NotImplementedError

    def remove(self, force: bool = False) -> None:
        ok, reason = self.__core_dataset.remove(force)
        if not ok:
            raise RuntimeError(f"failed to remove dataset: {reason}")

    def recover(self, force: bool = False) -> None:
        ok, reason = self.__core_dataset.recover(force)
        if not ok:
            raise RuntimeError(f"failed to recover dataset: {reason}")

    def summary(self) -> t.Optional[DatasetSummary]:
        return self.__core_dataset.summary()

    def history(self) -> t.List[t.Dict]:
        return self.__core_dataset.history()

    def close(self) -> None:
        for _loader in self.__data_loaders.values():
            if not _loader:
                continue  # pragma: no cover
            _loader.tabular_dataset.close()

        if self._dataset_builder:
            self._dataset_builder.close()

    def diff(self, cmp: Dataset) -> t.Dict:
        # TODO: wait for datastore diff feature
        raise NotImplementedError

    def manifest(self) -> t.Dict[str, t.Any]:
        return self.__core_dataset.info()

    def head(self, n: int = 3, show_raw_data: bool = False) -> t.List[t.Dict]:
        # TODO: render artifact in JupyterNotebook
        return self.__core_dataset.head(n, show_raw_data)

    def fetch_one(self, skip_fetch_data: bool = False) -> DataRow:
        loader = self._get_data_loader(disable_consumption=True)
        row = next(loader.tabular_dataset.scan())
        return loader._unpack_row(row, skip_fetch_data, shadow_dataset=self)

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
                    workdir=self._workdir / "builder",
                    dataset_name=self.name,
                    project_name=self.project_uri.project,
                    instance_name=self.project_uri.instance,
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
        def _save_info() -> None:
            if self.__info is not None and self._info_ds_wrapper is not None:
                self.__info.save_to_datastore(self._info_ds_wrapper)

        def _dump_manifest() -> Path:
            if self._dataset_builder is None:
                raise RuntimeError("failed to commit, because dataset builder is None")

            _signs = [str(m) for m in self._dataset_builder.signature_bins_meta]

            _manifest = {
                "build": {
                    "os": platform.system(),
                    "starwhale": STARWHALE_VERSION,
                },
                "version": self._pending_commit_version,
                "related_datastore_timestamp": "",  # TODO: get timestamp from datastore
                CREATED_AT_KEY: now_str(),
                "append_signs": _signs,
                "dataset_summary": {
                    "rows": self._dataset_builder.calculate_rows_cnt(),  # maybe slow
                    "updated_rows": self._updated_rows_by_commit,
                    "deleted_rows": self._deleted_rows_by_commit,
                },
                "message": message,
            }

            self._updated_rows_by_commit = 0
            self._deleted_rows_by_commit = 0

            _m_path = self._workdir / DEFAULT_MANIFEST_NAME
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

        @http_retry
        def _submit_cloud_version(manifest_path: Path) -> None:
            from starwhale.base.bundle_copy import _UploadPhase

            crm = CloudRequestMixed()
            params = {
                "swds": f"{self.name}:{self._pending_commit_version}",
                "project": self.project_uri.project,
                "force": "1",  # stash version is unique, use force=1 to make http retry happy
            }
            url_path = f"/project/{self.project_uri.project}/dataset/{self.name}/version/{self._pending_commit_version}/file"
            r = crm.do_multipart_upload_file(
                url_path=url_path,
                file_path=manifest_path,
                instance_uri=self.project_uri,
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
                instance_uri=self.project_uri,
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

        self.flush(artifacts_flush=True)
        _save_info()
        manifest_path = _dump_manifest()
        if self.project_uri.instance == STANDALONE_INSTANCE:
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
        self, dest_uri: str, force: bool = False, dest_local_project_uri: str = ""
    ) -> None:
        CoreDataset.copy(
            str(self.uri),
            dest_uri,
            force=force,
            dest_local_project_uri=dest_local_project_uri,
        )

    @staticmethod
    def list(
        project_uri: t.Union[str, URI] = "",
        fullname: bool = False,
        show_removed: bool = False,
        page_index: int = DEFAULT_PAGE_IDX,
        page_size: int = DEFAULT_PAGE_SIZE,
    ) -> t.Tuple[t.List[t.Dict[str, t.Any]], t.Dict[str, t.Any]]:
        from starwhale.core.dataset.view import DatasetTermView

        return DatasetTermView.list(
            project_uri, fullname, show_removed, page_index, page_size
        )

    @staticmethod
    def dataset(
        uri: t.Union[str, URI],
        create: bool = False,
        readonly: bool = False,
    ) -> Dataset:
        """Create or load a dataset from standalone instance or cloud instance.

        Arguments:
            uri: (str, URI, required) The dataset uri.
            create: (bool, optional) Create a new dataset automatically.If create is False, the dataset must exist.
                Default is False.
            readonly: (bool, optional) For an existing dataset, you can specify the readonly=True argument to ensure
                the dataset is in readonly mode. Default is False.

        Returns:
            A Dataset Object

        Examples:
        ```python
        from starwhale import dataset, Image

        # create a new dataset named mnist, and add a row into the dataset
        ds = dataset("mnist", create=True)
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
        ```

        """
        if isinstance(uri, str):
            _uri = URI(uri, expected_type=URIType.DATASET)
        elif isinstance(uri, URI) and uri.object.typ == URIType.DATASET:
            _uri = uri
        else:
            raise TypeError(
                f"uri({uri}) argument type is not expected, dataset uri or str is ok"
            )

        if create and readonly:
            raise ValueError(
                "create and readonly arguments cannot be set to True at the same time"
            )

        ds = Dataset(
            name=_uri.object.name,
            version=_uri.object.version,
            project_uri=_uri,  # TODO: cut off dataset resource info?
            create=create,
            readonly=readonly,
        )

        return ds
