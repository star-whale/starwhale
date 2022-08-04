import os
import re
import sys
import json
import atexit
import pathlib
import threading
from typing import Any, Set, cast, Dict, List, Tuple, Iterator, Optional

import numpy as np
import pyarrow as pa  # type: ignore
import pyarrow.parquet as pq  # type: ignore
from typing_extensions import Protocol


class Type:
    def __init__(
        self, name: str, pa_type: pa.DataType, nbits: int, default_value: Any
    ) -> None:
        self.name = name
        self.pa_type = pa_type
        self.nbits = nbits
        self.default_value = default_value

    def serialize(self, value: Any) -> Any:
        return value

    def deserialize(self, value: Any) -> Any:
        return value

    def __str__(self) -> str:
        if self.name == "int" or self.name == "float":
            return f"{self.name}{self.nbits}"
        else:
            return self.name

    __repr__ = __str__


class LinkType(Type):
    def __init__(self) -> None:
        super().__init__("link", pa.string(), 32, "")

    def serialize(self, value: Any) -> Any:
        if value is None:
            return None
        assert isinstance(value, Link)
        return str(value)

    def deserialize(self, value: Any) -> Any:
        if value is None:
            return None
        d = json.loads(value)
        return Link(
            d.get("uri", None), d.get("display_text", None), d.get("mime_type", None)
        )


class Link:
    def __init__(
        self,
        uri: str,
        display_text: Optional[str] = None,
        mime_type: Optional[str] = None,
    ) -> None:
        self.uri = uri
        self.display_text = display_text
        self.mime_type = mime_type

    def __str__(self) -> str:
        return json.dumps(
            {
                "uri": self.uri,
                "display_text": self.display_text,
                "mime_type": self.mime_type,
            }
        )

    def __eq__(self, other: Any) -> bool:
        return (
            isinstance(other, Link)
            and self.uri == other.uri
            and self.display_text == other.display_text
            and self.mime_type == other.mime_type
        )


NONE = Type("none", None, 1, None)
INT8 = Type("int", pa.int8(), 8, 0)
INT16 = Type("int", pa.int16(), 16, 0)
INT32 = Type("int", pa.int32(), 32, 0)
INT64 = Type("int", pa.int64(), 64, 0)
FLOAT16 = Type("float", pa.float16(), 16, 0.0)
FLOAT32 = Type("float", pa.float32(), 32, 0.0)
FLOAT64 = Type("float", pa.float64(), 64, 0.0)
BOOL = Type("bool", pa.bool_(), 1, 0)
STRING = Type("str", pa.string(), 32, "")
BYTES = Type("bytes", pa.binary(), 32, b"")
LINK = LinkType()

_TYPE_DICT: Dict[Any, Type] = {
    type(None): NONE,
    np.byte: INT8,
    np.int8: INT8,
    np.int16: INT16,
    np.int32: INT32,
    np.int64: INT64,
    int: INT64,
    np.float16: FLOAT16,
    np.float32: FLOAT32,
    np.float_: FLOAT64,
    float: FLOAT64,
    np.bool_: BOOL,
    bool: BOOL,
    str: STRING,
    bytes: BYTES,
    Link: LINK,
}

_TYPE_NAME_DICT = {str(v): v for k, v in _TYPE_DICT.items()}


def _get_type(obj: Any) -> Optional[Type]:
    return _TYPE_DICT.get(type(obj), None)


class ColumnSchema:
    def __init__(self, name: str, type: Type) -> None:
        self.name = name
        self.type = type

    def __eq__(self, other: Any) -> bool:
        return (
            isinstance(other, ColumnSchema)
            and self.name == other.name
            and self.type is other.type
        )


class TableSchema:
    def __init__(self, key_column: str, columns: List[ColumnSchema]) -> None:
        self.key_column = key_column
        self.columns = {col.name: col for col in columns}

    def copy(self) -> "TableSchema":
        return TableSchema(self.key_column, list(self.columns.values()))

    @staticmethod
    def parse(json_str: str) -> "TableSchema":
        d = json.loads(json_str)
        return TableSchema(
            d["key"],
            [
                ColumnSchema(col["name"], _TYPE_NAME_DICT[col["type"]])
                for col in d["columns"]
            ],
        )

    def __str__(self) -> str:
        return json.dumps(
            {
                "key": self.key_column,
                "columns": [
                    {"name": col.name, "type": str(col.type)}
                    for col in self.columns.values()
                ],
            }
        )

    __rept__ = __str__

    def __eq__(self, other: Any) -> bool:
        return (
            isinstance(other, TableSchema)
            and self.key_column == other.key_column
            and self.columns == other.columns
        )


def _get_table_path(root_path: str, table_name: str) -> str:
    return str(pathlib.Path(root_path) / table_name)


def _parse_parquet_name(name: str) -> Tuple[str, int]:
    try:
        if name.endswith(".parquet"):
            if name.startswith("base-"):
                return "base", int(name[5:-8])
            elif name.startswith("patch-"):
                return "patch", int(name[6:-8])
    except ValueError:
        # ignore invalid filename
        pass
    return "", 0


def _write_parquet_file(filename: str, table: pa.Table) -> None:
    with pq.ParquetWriter(filename, table.schema) as writer:
        writer.write_table(table)


def _scan_parquet_file(
    path: str,
    columns: Optional[Dict[str, str]] = None,
    start: Optional[Any] = None,
    end: Optional[Any] = None,
) -> Iterator[dict]:
    f = pq.ParquetFile(path)
    schema_arrow = f.schema_arrow
    if columns is None:
        columns = {
            name: name
            for name in schema_arrow.names
            if name != "-" and not name.startswith("~")
        }
    schema = TableSchema.parse(f.metadata.metadata[b"schema"].decode("utf-8"))
    key_index = schema_arrow.get_field_index(schema.key_column)
    if key_index < 0:
        raise RuntimeError(
            f"key {schema.key_column} is not found in names: {schema_arrow.names}"
        )
    key_alias = columns.get(schema.key_column, None)
    all_cols = [schema.key_column]
    if schema_arrow.get_field_index("-") >= 0:
        all_cols.append("-")
    for name, alias in columns.items():
        if (
            name != schema.key_column
            and name != "-"
            and schema_arrow.get_field_index(name) >= 0
        ):
            all_cols.append(name)
    for name in schema_arrow.names:
        if name.startswith("~") and name[1:] in columns:
            all_cols.append(name)

    for i in range(f.num_row_groups):
        stats = f.metadata.row_group(i).column(key_index).statistics
        if (end is not None and stats.min >= end) or (
            start is not None and stats.max < start
        ):
            continue
        table = f.read_row_group(i, all_cols)
        names = table.schema.names
        types = [schema.columns[name].type for name in names]
        n_rows = table[0].length()
        n_cols = len(names)
        for j in range(n_rows):
            key = types[0].deserialize(table[0][j].as_py())
            if (start is not None and key < start) or (end is not None and key >= end):
                continue
            d = {"*": key}
            if key_alias is not None:
                d[key_alias] = key
            for k in range(1, n_cols):
                name = names[k]
                value = types[k].deserialize(table[k][j].as_py())
                if name == "-":
                    if value is not None:
                        d["-"] = value
                elif name.startswith("~") and value:
                    d.pop(columns.get(name[1:], ""), "")
                else:
                    alias = columns.get(name, "")
                    if alias != "" and value is not None:
                        d[alias] = value
            yield d


def _merge_scan(iters: List[Iterator[Dict[str, Any]]]) -> Iterator[dict]:
    class Node:
        def __init__(self, index: int, iter: Iterator[dict]) -> None:
            self.index = index
            self.iter = iter
            self.item: Optional[Dict[str, Any]] = None
            self.exhausted = False
            self.nextItem()

        def nextItem(self) -> None:
            try:
                self.item = next(self.iter)
                self.exhausted = False
                self.key = cast(str, self.item["*"])
            except StopIteration:
                self.exhausted = True
                self.item = None
                self.key = ""

    n = len(iters)
    nodes = []
    for i in range(n):
        node = Node(i, iters[i])
        if not node.exhausted:
            nodes.append(node)
    while len(nodes) > 0:
        key = min(nodes, key=lambda x: x.key).key
        d: Dict[str, Any] = {}
        for i in range(len(nodes)):
            while nodes[i].key == key:
                item = nodes[i].item
                assert item is not None
                removal = item.pop("-", False)
                item.pop("*", None)
                if removal:
                    d.clear()
                else:
                    d.update(item)
                nodes[i].nextItem()
        if len(d) > 0:
            d["*"] = key
            yield d
        nodes = [node for node in nodes if not node.exhausted]


def _get_table_files(path: str) -> List[str]:
    if not os.path.exists(path):
        return []
    patches = []
    base_index = -1
    for file in os.listdir(path):
        type, index = _parse_parquet_name(file)
        if type == "base" and index > base_index:
            base_index = index
        elif type == "patch":
            patches.append(index)
    if base_index >= 0:
        ret = [os.path.join(path, f"base-{base_index}.parquet")]
    else:
        ret = []
    patches.sort()
    for i in patches:
        if i > base_index:
            ret.append(os.path.join(path, f"patch-{i}.parquet"))
    return ret


def _read_table_schema(path: str) -> TableSchema:
    if not os.path.exists(path):
        raise RuntimeError(f"path not found: {path}")
    if not os.path.isdir(path):
        raise RuntimeError(f"{path} is not a directory")
    files = _get_table_files(path)
    if len(files) == 0:
        raise RuntimeError(f"table is empty, path:{path}")
    schema = pq.read_schema(files[-1])
    if schema.metadata is None:
        raise RuntimeError(f"no metadata for file {files[-1]}")
    schema_data = schema.metadata.get(b"schema", None)
    if schema_data is None:
        raise RuntimeError(f"no schema for file {files[-1]}")
    return TableSchema.parse(schema_data.decode())


def _scan_table(
    path: str,
    columns: Optional[Dict[str, str]] = None,
    start: Optional[Any] = None,
    end: Optional[Any] = None,
    explicit_none: bool = False,
) -> Iterator[dict]:
    iters = []
    for file in _get_table_files(path):
        iters.append(_scan_parquet_file(file, columns, start, end))
    if len(iters) > 0:
        schema = _read_table_schema(path)
        column_names = [
            col.name
            for col in schema.columns.values()
            if col.name != "-" and not col.name.startswith("~")
        ]
    for record in _merge_scan(iters):
        if explicit_none:
            for col in column_names:
                record.setdefault(col, None)
        yield record


def _records_to_table(
    schema: TableSchema, records: List[Dict[str, Any]], deletes: List[Any]
) -> pa.Table:
    if len(records) == 0:
        return
    schema = schema.copy()
    if len(deletes) > 0:
        schema.columns["-"] = ColumnSchema("-", BOOL)
        for key in deletes:
            records.append({schema.key_column: key, "-": True})
    records.sort(key=lambda x: cast(str, x.get(schema.key_column)), reverse=True)
    d: Dict[str, Any] = {}
    nulls: Dict[str, List[int]] = {}
    for i in range(len(records)):
        record = records[len(records) - 1 - i]
        for col, col_schema in schema.columns.items():
            if col in record:
                value = record.get(col)
                if value is None:
                    nulls.setdefault(col, []).append(i)
            else:
                value = None
            d.setdefault(col, []).append(col_schema.type.serialize(value))
    for col, indexes in nulls.items():
        schema.columns["~" + col] = ColumnSchema("~" + col, BOOL)
        data = [False] * len(records)
        for i in indexes:
            data[i] = True
        d["~" + col] = data
    pa_schema = pa.schema(
        [(k, v.type.pa_type) for k, v in schema.columns.items()],
        {"schema": str(schema)},
    )
    return pa.Table.from_pydict(d, schema=pa_schema)


def _get_size(d: Any) -> int:
    ret = sys.getsizeof(d)
    if type(d) is dict:
        for v in d.values():
            ret += sys.getsizeof(v)
    return ret


def _update_schema(schema: TableSchema, record: Dict[str, Any]) -> TableSchema:
    new_schema = schema.copy()
    for col, value in record.items():
        value_type = _get_type(value)
        if value_type is None:
            raise RuntimeError(f"unsupported type {type(value)} for field {col}")
        column_schema = schema.columns.get(col, None)
        if (
            column_schema is not None
            and column_schema.type is not NONE
            and value_type is not NONE
            and value_type is not column_schema.type
            and value_type.name != column_schema.type.name
        ):
            raise RuntimeError(
                f"can not insert a record with field {col} of type {value_type}, {column_schema.type} expected"
            )
        if column_schema is None:
            new_schema.columns[col] = ColumnSchema(col, value_type)
        elif column_schema.type is NONE:
            new_schema.columns[col].type = value_type
        elif value_type is not NONE and value_type.nbits > column_schema.type.nbits:
            new_schema.columns[col].type = value_type
    return new_schema


class MemoryTable:
    def __init__(self, table_name: str, schema: TableSchema) -> None:
        self.table_name = table_name
        self.schema = schema.copy()
        self.records: Dict[Any, Dict[str, Any]] = {}
        self.deletes: Set[Any] = set()
        self.nbytes = 0
        self.lock = threading.Lock()

    def get_schema(self) -> TableSchema:
        with self.lock:
            return self.schema.copy()

    def load(self, root_path: str) -> None:
        for record in _scan_table(_get_table_path(root_path, self.table_name)):
            key = record.get(self.schema.key_column, None)
            actual = record.pop("*")
            if record.get(self.schema.key_column, None) != key:
                raise RuntimeError(
                    f"failed to load table {self.table_name}: key column={self.schema.key_column}, expected key:{key}, actual key:{actual}"
                )
            record.pop("-", None)
            self.records[key] = record

    def scan(
        self,
        columns: Optional[Dict[str, str]] = None,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        explicit_none: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        with self.lock:
            schema = self.schema.copy()
            records = [
                {self.schema.key_column: key, "-": True}
                for key in self.deletes
                if (start is None or key >= start) and (end is None or key < end)
            ]
            for k, v in self.records.items():
                if (start is None or k >= start) and (end is None or k < end):
                    records.append(v)
        records.sort(key=lambda x: cast(str, x[self.schema.key_column]))
        for r in records:
            if columns is None:
                d = r
            else:
                d = {columns[k]: v for k, v in r.items() if k in columns}
                if "-" in r:
                    d["-"] = r["-"]
            d["*"] = r[self.schema.key_column]
            if explicit_none:
                for col in schema.columns.values():
                    d.setdefault(col.name, None)
            else:
                d = {k: v for k, v in d.items() if v is not None}
            yield d

    def insert(self, record: Dict[str, Any]) -> None:
        with self.lock:
            self.schema = _update_schema(self.schema, record)
            key = record.get(self.schema.key_column)
            r = self.records.setdefault(key, record)
            if r is record:
                self.nbytes += _get_size(record)
            else:
                self.nbytes -= _get_size(r)
                r.update(record)
                self.nbytes += _get_size(r)

    def delete(self, keys: List[Any]) -> None:
        with self.lock:
            for key in keys:
                self.deletes.add(key)
                self.nbytes += _get_size(key)
                r = self.records.pop(key, None)
                if r is not None:
                    self.nbytes -= _get_size(r)

    def dump(self, root_path: str) -> None:
        path = _get_table_path(root_path, self.table_name)
        if not os.path.exists(path):
            os.mkdir(path)
        if not os.path.isdir(path):
            raise RuntimeError(f"{path} is not a directory")
        max_index = -1
        for file in os.listdir(path):
            type, index = _parse_parquet_name(file)
            if type != "" and index > max_index:
                max_index = index
        if max_index < 0:
            filename = "base-0.parquet"
        else:
            filename = f"base-{max_index + 1}.parquet"
        _write_parquet_file(
            os.path.join(path, filename),
            _records_to_table(
                self.schema,
                list(self.records.values()),
                list(self.deletes),
            ),
        )


class LocalDataStore:
    _instance = None
    _lock = threading.Lock()

    @staticmethod
    def get_instance() -> "LocalDataStore":
        with LocalDataStore._lock:
            if LocalDataStore._instance is None:
                root_path = os.getenv("SW_ROOT_PATH", None)
                if root_path is None:
                    raise RuntimeError(
                        "data store root path is not defined for standalone instance"
                    )
                LocalDataStore._instance = LocalDataStore(root_path)
                atexit.register(LocalDataStore._instance.dump)
            return LocalDataStore._instance

    def __init__(self, root_path: str) -> None:
        self.root_path = root_path
        self.name_pattern = re.compile(r"^[A-Za-z0-9-_/]+$")
        self.tables: Dict[str, MemoryTable] = {}

    def put(
        self, table_name: str, schema: TableSchema, records: List[Dict[str, Any]]
    ) -> None:
        if self.name_pattern.match(table_name) is None:
            raise RuntimeError(
                f"invalid table name {table_name}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
            )
        for r in records:
            for k in r.keys():
                if self.name_pattern.match(k) is None:
                    raise RuntimeError(
                        f"invalid column name {k}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
                    )
        table = self.tables.get(table_name, None)
        if table is None:
            table = MemoryTable(table_name, schema)
            table.load(self.root_path)
            self.tables[table_name] = table
        if schema.key_column != table.schema.key_column:
            raise RuntimeError(
                f"invalid key column, expected {table.schema.key_column}, actual {schema.key_column}"
            )
        for r in records:
            key = r.get(schema.key_column, None)
            if key is None:
                raise RuntimeError(
                    f"key {schema.key_column} should not be none, record: {r.keys()}"
                )
            for name in r.keys():
                if self.name_pattern.match(name) is None:
                    raise RuntimeError(
                        f"invalid column name {name}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
                    )
            if "-" in r:
                table.delete([key])
            else:
                table.insert(r)

    def scan_tables(
        self,
        tables: List[Tuple[str, str, bool]],
        columns: Optional[Dict[str, str]] = None,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
    ) -> Iterator[Dict[str, Any]]:
        # check for alias duplications
        if columns is not None:
            alias_map: Dict[str, str] = {}
            for col, alias in columns.items():
                key = alias_map.setdefault(alias, col)
                if key != col:
                    raise RuntimeError(
                        f"duplicate alias {alias} for column {col} and {key}"
                    )

        class TableInfo:
            def __init__(
                self,
                name: str,
                key_column_type: pa.DataType,
                columns: Dict[str, str],
                explicit_none: bool,
            ) -> None:
                self.name = name
                self.key_column_type = key_column_type
                self.columns = columns
                self.explicit_none = explicit_none

        infos: List[TableInfo] = []
        for table_name, table_alias, explicit_none in tables:
            table = self.tables.get(table_name, None)
            if table is None:
                schema = _read_table_schema(_get_table_path(self.root_path, table_name))
            else:
                schema = table.get_schema()
            key_column_type = schema.columns[schema.key_column].type.pa_type
            column_names = schema.columns.keys()
            col_prefix = table_alias + "."
            if columns is None or col_prefix + "*" in columns:
                cols = None
            else:
                cols = {}
                for name in column_names:
                    alias = columns.get(name, "")
                    alias = columns.get(col_prefix + name, alias)
                    if alias != "":
                        cols[name] = alias
            infos.append(TableInfo(table_name, key_column_type, cols, explicit_none))

        # check for key type conflication
        for info in infos:
            if info is infos[0]:
                continue
            if info.key_column_type != infos[0].key_column_type:
                raise RuntimeError(
                    "conflicting key field type. "
                    + f"{info.name} has a key of type {info.key_column_type},"
                    + f" while {infos[0].name} has a key of type {infos[0].key_column_type}"
                )
        iters = []
        for info in infos:
            if info.name in self.tables:
                iters.append(
                    self.tables[info.name].scan(
                        info.columns, start, end, info.explicit_none
                    )
                )
            else:
                iters.append(
                    _scan_table(info.name, info.columns, start, end, info.explicit_none)
                )

        for record in _merge_scan(iters):
            record.pop("*", None)
            yield record

    def dump(self) -> None:
        for table in self.tables.values():
            table.dump(self.root_path)


class RemoteDataStore:
    def put(
        self, table_name: str, schema: TableSchema, records: List[Dict[str, Any]]
    ) -> None:
        ...

    def scan_tables(
        self,
        tables: List[Tuple[str, str, bool]],
        columns: Optional[Dict[str, str]] = None,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
    ) -> Iterator[Dict[str, Any]]:
        ...


class DataStore(Protocol):
    def put(
        self, table_name: str, schema: TableSchema, records: List[Dict[str, Any]]
    ) -> None:
        ...

    def scan_tables(
        self,
        tables: List[Tuple[str, str, bool]],
        columns: Optional[Dict[str, str]] = None,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
    ) -> Iterator[Dict[str, Any]]:
        ...


def get_data_store() -> DataStore:
    instance = os.getenv("SW_INSTANCE")
    if instance is None or instance == "local":
        return LocalDataStore.get_instance()
    else:
        return RemoteDataStore()


def _flatten(record: Dict[str, Any]) -> Dict[str, Any]:
    def _new(key_prefix: str, src: Dict[str, Any], dest: Dict[str, Any]) -> None:
        for k, v in src.items():
            k = key_prefix + k
            if type(v) is dict:
                _new(k + "/", v, dest)
            dest[k] = v

    for v in record.values():
        if type(v) is dict:
            ret: Dict[str, Any] = {}
            _new("", record, ret)
            return ret
    return record


class TableWriter(threading.Thread):
    def __init__(self, table_name: str, key_column: str = "id") -> None:
        super().__init__()
        self.table_name = table_name
        self.schema = TableSchema(key_column, [])
        self.records: List[Dict[str, Any]] = []
        self.stopped = False
        self.data_store = get_data_store()
        self.cond = threading.Condition()
        self.setDaemon(True)
        atexit.register(self.close)
        self.start()

    def __enter__(self) -> Any:
        return self

    def __exit__(self, type: Any, value: Any, tb: Any) -> None:
        self.close()

    def close(self) -> None:
        with self.cond:
            if not self.stopped:
                atexit.unregister(self.close)
                self.stopped = True
                self.cond.notify()
        self.join()

    def insert(self, record: Dict[str, Any]) -> None:
        record = _flatten(record)
        for k in record:
            for ch in k:
                if not ch.isalnum() and ch != "-" and ch != "_" and ch != "/":
                    raise RuntimeError(f"invalid field {k}")
        self._insert(record)

    def delete(self, key: Any) -> None:
        self._insert({self.schema.key_column: key, "-": True})

    def _insert(self, record: Dict[str, Any]) -> None:
        key = record.get(self.schema.key_column, None)
        if key is None:
            raise RuntimeError(
                f"the key {self.schema.key_column} should not be none, record:{record}"
            )
        with self.cond:
            self.schema = _update_schema(self.schema, record)
            self.records.append(record)
            self.cond.notify()

    def run(self) -> None:
        while True:
            with self.cond:
                while not self.stopped and len(self.records) == 0:
                    self.cond.wait()
                if len(self.records) == 0:
                    break
                records = self.records
                self.records = []
            self.data_store.put(self.table_name, self.schema, records)
