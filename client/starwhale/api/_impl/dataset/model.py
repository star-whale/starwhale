from __future__ import annotations

import typing as t
import threading
from http import HTTPStatus
from types import TracebackType
from pathlib import Path
from functools import wraps

from loguru import logger

from starwhale.utils import gen_uniq_version
from starwhale.consts import HTTPMethod, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.uri import URI, URIType
from starwhale.base.type import InstanceType
from starwhale.base.cloud import CloudRequestMixed
from starwhale.utils.error import ExistedError, NotFoundError, NoSupportError
from starwhale.core.dataset.type import DatasetConfig, DatasetSummary
from starwhale.core.dataset.model import Dataset as CoreDataset
from starwhale.core.dataset.model import StandaloneDataset
from starwhale.core.dataset.store import DatasetStorage
from starwhale.core.dataset.tabular import (
    get_dataset_consumption,
    DEFAULT_CONSUMPTION_BATCH_SIZE,
    TabularDatasetSessionConsumption,
)

from .loader import DataRow, DataLoader, get_data_loader
from .builder import RowWriter, BaseBuildExecutor

_DType = t.TypeVar("_DType", bound="Dataset")
_ItemType = t.Union[str, int, slice]
_HandlerType = t.Optional[t.Union[t.Callable, BaseBuildExecutor]]
_GItemType = t.Optional[t.Union[DataRow, t.List[DataRow]]]


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
    ) -> None:
        self.name = name
        self.project_uri = project_uri

        _origin_uri = URI.capsulate_uri(
            self.project_uri.instance,
            self.project_uri.project,
            URIType.DATASET,
            self.name,
            version,
        )

        if create:
            self.version = gen_uniq_version()
        else:
            self.version = self._auto_complete_version(version)

        if not self.version:
            raise ValueError("version field is empty")

        self.uri = URI.capsulate_uri(
            self.project_uri.instance,
            self.project_uri.project,
            URIType.DATASET,
            self.name,
            self.version,
        )

        self.__readonly = not create
        self.__core_dataset = CoreDataset.get_dataset(self.uri)
        if create:
            setattr(self.__core_dataset, "_version", self.version)

        self._append_use_swds_bin = False
        _summary = None
        if self._check_uri_exists(_origin_uri):
            if create:
                self._append_from_version = version
                self._create_by_append = True
                self._fork_dataset()
                _summary = CoreDataset.get_dataset(_origin_uri).summary()
                if _summary:
                    self._append_use_swds_bin = not (
                        _summary.include_link or _summary.include_user_raw
                    )
            else:
                self._append_from_version = ""
                self._create_by_append = False
        else:
            if create:
                self._append_from_version = ""
                self._create_by_append = False
            else:
                raise ExistedError(f"{self.uri} was not found fo load")

        self._summary = _summary or self.__core_dataset.summary()

        self._rows_cnt = self._summary.rows if self._summary else 0
        self._consumption: t.Optional[TabularDatasetSessionConsumption] = None
        self._lock = threading.Lock()
        self.__data_loaders: t.Dict[str, DataLoader] = {}
        self.__build_handler: _HandlerType = None
        self._trigger_handler_build = False
        self._trigger_icode_build = False
        self._writer_lock = threading.Lock()
        self._row_writer: t.Optional[RowWriter] = None
        self.__keys_cache: t.Set[t.Union[int, str]] = set()
        self._enable_copy_src = False

    def _fork_dataset(self) -> None:
        # TODO: support cloud dataset prepare in the tmp dir
        # TODO: lazy fork dataset
        self.__core_dataset._prepare_snapshot()
        self.__core_dataset._fork_swds(
            self._create_by_append, self._append_from_version
        )

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
        return f"Dataset: {self.name}-{self.version}"

    def __repr__(self) -> str:
        return f"Dataset: uri-{self.uri}"

    def __len__(self) -> int:
        return self._rows_cnt

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

        with self._lock:
            self._consumption = get_dataset_consumption(
                self.uri, session_id=session_id, batch_size=batch_size
            )
        return self

    def _get_data_loader(
        self, recreate: bool = False, disable_consumption: bool = False
    ) -> DataLoader:
        with self._lock:
            key = f"consumption-{disable_consumption}"

            _loader = self.__data_loaders.get(key)
            if _loader is None or recreate:
                if disable_consumption:
                    consumption = None
                else:
                    consumption = self._consumption

                _loader = get_data_loader(self.uri, session_consumption=consumption)
                self.__data_loaders[key] = _loader

        return _loader

    def __iter__(self) -> t.Iterator[DataRow]:
        for row in self._get_data_loader():
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
                return loader._unpack_row(row, skip_fetch_data)
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
                        rows.append(loader._unpack_row(row, skip_fetch_data))
                    cnt += 1
                return rows
            else:
                raise ValueError(f"{item} type is not int, str or slice")

        try:
            return _run()
        except RuntimeError as e:
            if str(e).startswith("table is empty"):
                return None
            raise
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

    def _forbid_handler_build(func: t.Callable):  # type: ignore
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> t.Any:
            self: Dataset = args[0]
            if self._trigger_handler_build:
                raise NoSupportError(
                    "no support build from handler and from cache code at the same time, build from handler has already been activated"
                )
            return func(*args, **kwargs)

        return _wrapper

    def _forbid_icode_build(func: t.Callable):  # type: ignore
        @wraps(func)
        def _wrapper(*args: t.Any, **kwargs: t.Any) -> t.Any:
            self: Dataset = args[0]
            if self._trigger_icode_build:
                raise NoSupportError(
                    "no support build from handler and from cache code at the same time, build from interactive code has already been activated"
                )
            return func(*args, **kwargs)

        return _wrapper

    @property
    def build_handler(self) -> _HandlerType:
        return self.__build_handler

    @build_handler.setter
    def build_handler(self, handler: _HandlerType) -> None:
        if self._trigger_icode_build:
            raise RuntimeError(
                "dataset append by interactive code has already been called"
            )
        self._trigger_handler_build = True
        self.__build_handler = handler

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
                path=f"/project/{uri.project}/{URIType.DATASET}/{uri.object.name}/version/{uri.object.version}/file",
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

    @_check_readonly
    def flush(self) -> None:
        loader = self._get_data_loader(disable_consumption=True)
        loader.tabular_dataset.flush()
        self.__keys_cache = set()

        if self._row_writer:
            self._row_writer.flush()

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
        return self._summary

    def history(self) -> t.List[t.Dict]:
        return self.__core_dataset.history()

    def close(self) -> None:
        self.__keys_cache = set()
        for _loader in self.__data_loaders.values():
            if not _loader:
                continue  # pragma: no cover

            _loader.tabular_dataset.close()

        if self._row_writer:
            self._row_writer.close()

        # TODO: flush raw data into disk

    def diff(self, cmp: Dataset) -> t.Dict:
        return self.__core_dataset.diff(cmp.uri)

    def info(self) -> t.Dict[str, t.Any]:
        return self.__core_dataset.info()

    def head(self, n: int = 3, show_raw_data: bool = False) -> t.List[t.Dict]:
        # TODO: render artifact in JupyterNotebook
        return self.__core_dataset.head(n, show_raw_data)

    def to_pytorch(self) -> t.Any:
        raise NotImplementedError

    def to_tensorflow(self) -> t.Any:
        raise NotImplementedError

    @_check_readonly
    @_forbid_handler_build
    def __setitem__(
        self, key: t.Union[str, int], value: t.Union[DataRow, t.Tuple]
    ) -> None:
        # TODO: tune the performance of getitem by cache
        self._trigger_icode_build = True
        if not isinstance(self.__core_dataset, StandaloneDataset):
            raise NoSupportError(
                f"setitem only supports for standalone dataset: {self.__core_dataset}"
            )

        _row_writer = self._get_row_writer()

        if not isinstance(key, (int, str)):
            raise TypeError(f"key must be str or int type: {key}")

        if isinstance(value, DataRow):
            value.index = key
            row = value
        elif isinstance(value, (tuple, list)):
            if len(value) == 2:
                data, annotations = value
            elif len(value) == 3:
                _, data, annotations = value
            else:
                raise ValueError(f"{value} cannot unpack")

            row = DataRow(index=key, data=data, annotations=annotations)
        else:
            raise TypeError(f"value only supports tuple or DataRow type: {value}")

        if key not in self.__keys_cache:
            self.__keys_cache.add(key)
            _item = self._getitem(key, skip_fetch_data=True)
            if _item is None or len(_item) == 0:
                self._rows_cnt += 1

        _row_writer.update(row)

    def _get_row_writer(self) -> RowWriter:
        if self._row_writer is not None:
            return self._row_writer

        with self._writer_lock:
            if self._row_writer is None:
                if self._create_by_append and self._append_from_version:
                    append_from_uri = URI.capsulate_uri(
                        instance=self.project_uri.instance,
                        project=self.project_uri.project,
                        obj_type=URIType.DATASET,
                        obj_name=self.name,
                        obj_ver=self._append_from_version,
                    )
                    store = DatasetStorage(append_from_uri)
                    if not store.snapshot_workdir.exists():
                        raise NotFoundError(f"dataset uri: {append_from_uri}")
                    append_from_version = store.id
                else:
                    append_from_uri = None
                    append_from_version = ""

                # TODO: support alignment_bytes_size, volume_bytes_size arguments
                if not isinstance(self.__core_dataset, StandaloneDataset):
                    raise NoSupportError(
                        f"setitem only supports for standalone dataset: {self.__core_dataset}"
                    )

                self._row_writer = RowWriter(
                    dataset_name=self.name,
                    dataset_version=self.version,
                    project_name=self.project_uri.project,
                    workdir=self.__core_dataset.store.snapshot_workdir,  # TODO: use tmpdir which is same as dataset build command
                    append=self._create_by_append,
                    append_from_version=append_from_version,
                    append_from_uri=append_from_uri,
                    append_with_swds_bin=self._append_use_swds_bin,
                )
        return self._row_writer

    _init_row_writer = _get_row_writer

    @_check_readonly
    @_forbid_handler_build
    def __delitem__(self, key: _ItemType) -> None:
        self._trigger_icode_build = True
        self._init_row_writer()  # hack for del item as the first operation

        items: t.List
        if isinstance(key, (str, int)):
            items = [self._getitem(key, skip_fetch_data=True)]
        elif isinstance(key, slice):
            items = self._getitem(key, skip_fetch_data=True)  # type: ignore
        else:
            raise TypeError(f"key({key}) is not str, int or slice type")

        # TODO: raise not-found key error?
        loader = self._get_data_loader(disable_consumption=True)
        for item in items:
            if not item or not isinstance(item, DataRow):
                continue  # pragma: no cover
            if item.index in self.__keys_cache:
                self.__keys_cache.remove(item.index)
            loader.tabular_dataset.delete(item.index)
            self._rows_cnt -= 1

    @_check_readonly
    @_forbid_handler_build
    def append(self, item: t.Any) -> None:
        if isinstance(item, DataRow):
            self.__setitem__(item.index, item)
        elif isinstance(item, (list, tuple)):
            if len(item) == 2:
                row = DataRow(self._rows_cnt, item[0], item[1])
            elif len(item) == 3:
                row = DataRow(item[0], item[1], item[2])
            else:
                raise ValueError(
                    f"cannot unpack value({item}), expected sequence is (index, data, annotations) or (data, annotations)"
                )

            self.__setitem__(row.index, row)
        else:
            raise TypeError(f"value({item}) is not DataRow, list or tuple type")

    @_check_readonly
    @_forbid_handler_build
    def extend(self, items: t.Sequence[t.Any]) -> None:
        for item in items:
            self.append(item)

    @_check_readonly
    def build_with_copy_src(
        self,
        src_dir: t.Union[str, Path],
        include_files: t.Optional[t.List[str]] = None,
        exclude_files: t.Optional[t.List[str]] = None,
    ) -> Dataset:
        self._enable_copy_src = True
        self._build_src_dir = Path(src_dir)
        self._build_include_files = include_files or []
        self._build_exclude_files = exclude_files or []
        return self

    commit_with_copy_src = build_with_copy_src

    @_check_readonly
    @_forbid_handler_build
    def _do_build_from_interactive_code(self) -> None:
        ds = self.__core_dataset
        if isinstance(ds, StandaloneDataset):
            if self._row_writer is None:
                raise RuntimeError("row writer is none, no data was written")

            self.flush()
            self._row_writer.close()
            # TODO: use the elegant method to refactor manifest update
            self._summary = self._row_writer.summary
            self._summary.rows = len(self)
            ds._manifest["dataset_summary"] = self._summary.asdict()
            ds._calculate_signature()
            ds._render_manifest()
            ds._make_swds_meta_tar()
            ds._make_auto_tags()
        else:
            # TODO: support cloud dataset build
            raise NoSupportError("only support standalone dataset build")

    @_check_readonly
    @_forbid_icode_build
    def _do_build_from_handler(self) -> None:
        self._trigger_icode_build = True
        config = DatasetConfig(
            name=self.name,
            handler=self.build_handler,
            project_uri=self.project_uri.full_uri,
            append=self._create_by_append,
            append_from=self._append_from_version,
        )

        kw: t.Dict[str, t.Any] = {"disable_copy_src": not self._enable_copy_src}
        if self._enable_copy_src:
            config.pkg_data = self._build_include_files
            config.exclude_pkg_data = self._build_exclude_files
            kw["workdir"] = self._build_src_dir

        # TODO: support DatasetAttr config for SDK
        config.do_validate()
        kw["config"] = config
        # TODO: support build tmpdir, follow the swcli dataset build command behavior
        self.__core_dataset.buildImpl(**kw)
        _summary = self.__core_dataset.summary()
        self._rows_cnt = _summary.rows if _summary else 0

    @_check_readonly
    def build(self) -> None:
        # TODO: support build dataset for cloud uri directly
        if self.project_uri.instance_type == InstanceType.CLOUD:
            raise NoSupportError("no support to build cloud dataset directly")

        if self._trigger_icode_build:
            self._do_build_from_interactive_code()
        elif self._trigger_handler_build and self.build_handler:
            self._do_build_from_handler()
        else:
            raise RuntimeError("no data to build dataset")

    commit = build

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
        create_from_handler: t.Optional[_HandlerType] = None,
    ) -> Dataset:
        if isinstance(uri, str):
            _uri = URI(uri, expected_type=URIType.DATASET)
        elif isinstance(uri, URI) and uri.object.typ == URIType.DATASET:
            _uri = uri
        else:
            raise TypeError(
                f"uri({uri}) argument type is not expected, dataset uri or str is ok"
            )

        ds = Dataset(
            name=_uri.object.name,
            version=_uri.object.version,
            project_uri=_uri,  # TODO: cut off dataset resource info?
            create=create or bool(create_from_handler),
        )

        if create_from_handler:
            ds.build_handler = create_from_handler

        return ds
