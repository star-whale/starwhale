import os
import re
import sys
import json
import atexit
import base64
import struct
import urllib
import pathlib
import binascii
import threading
from http import HTTPStatus
from typing import Any, Set, cast, Dict, List, Tuple, Union, Iterator, Optional

import numpy as np
import pyarrow as pa  # type: ignore
import requests
import pyarrow.parquet as pq  # type: ignore
from loguru import logger
from typing_extensions import Protocol

from starwhale.utils.fs import ensure_dir
from starwhale.consts.env import SWEnv
from starwhale.utils.error import MissingFieldError
from starwhale.utils.config import SWCliConfigMixed

try:
    import fcntl

    has_fcntl = True
except ImportError:
    has_fcntl = False


def _check_move(src: str, dest: str) -> bool:
    if has_fcntl:
        with open(os.path.join(os.path.dirname(src), ".lock"), "w") as f:
            try:
                fcntl.flock(f, fcntl.LOCK_EX)  # type: ignore
            except OSError:
                return False
            try:
                os.rename(src, dest)
                return True
            finally:
                fcntl.flock(f, fcntl.LOCK_UN)  # type: ignore
    else:
        # windows
        try:
            os.rename(src, dest)
            return True
        except FileExistsError:
            return False


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

    def encode(self, value: Any) -> Optional[str]:
        if value is None:
            return None
        if self is UNKNOWN:
            return None
        if self is BOOL:
            if value:
                return "1"
            else:
                return "0"
        if self is STRING:
            return cast(str, value)
        if self is BYTES:
            return base64.b64encode(value).decode()
        if self.name == "int":
            return f"{value:x}"
        if self.name == "float":
            if self.nbits == 16:
                return binascii.hexlify(struct.pack(">e", value)).decode()
            if self.nbits == 32:
                return binascii.hexlify(struct.pack(">f", value)).decode()
            if self.nbits == 64:
                return binascii.hexlify(struct.pack(">d", value)).decode()
        raise RuntimeError("invalid type " + str(self))

    def decode(self, value: str) -> Any:
        if value is None:
            return None
        if self is UNKNOWN:
            return None
        if self is BOOL:
            return value == "1"
        if self is STRING:
            return value
        if self is BYTES:
            return base64.b64decode(value)
        if self.name == "int":
            return int(value, 16)
        if self.name == "float":
            raw = binascii.unhexlify(value)
            if self.nbits == 16:
                return struct.unpack(">e", raw)[0]
            if self.nbits == 32:
                return struct.unpack(">f", raw)[0]
            if self.nbits == 64:
                return struct.unpack(">d", raw)[0]
        raise RuntimeError("invalid type " + str(self))

    def __str__(self) -> str:
        if self.name == "int" or self.name == "float":
            return f"{self.name}{self.nbits}".upper()
        else:
            return self.name.upper()

    __repr__ = __str__


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


UNKNOWN = Type("unknown", None, 1, None)
INT8 = Type("int", pa.int8(), 8, 0)
INT16 = Type("int", pa.int16(), 16, 0)
INT32 = Type("int", pa.int32(), 32, 0)
INT64 = Type("int", pa.int64(), 64, 0)
FLOAT16 = Type("float", pa.float16(), 16, 0.0)
FLOAT32 = Type("float", pa.float32(), 32, 0.0)
FLOAT64 = Type("float", pa.float64(), 64, 0.0)
BOOL = Type("bool", pa.bool_(), 1, 0)
STRING = Type("string", pa.string(), 32, "")
BYTES = Type("bytes", pa.binary(), 32, b"")

_TYPE_DICT: Dict[Any, Type] = {
    type(None): UNKNOWN,
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


class TableSchemaDesc:
    def __init__(
        self, key_column: Optional[str], columns: Optional[List[ColumnSchema]]
    ) -> None:
        self.key_column = key_column
        self.columns = columns


class TableSchema:
    def __init__(self, key_column: str, columns: List[ColumnSchema]) -> None:
        self.key_column = key_column
        self.columns = {col.name: col for col in columns}

    def copy(self) -> "TableSchema":
        return TableSchema(self.key_column, list(self.columns.values()))

    def merge(self, other: "TableSchema") -> None:
        if self.key_column != other.key_column:
            raise RuntimeError(
                f"conflicting key column, expected {self.key_column}, acutal {other.key_column}"
            )
        new_schema = {}
        for col in other.columns.values():
            column_schema = self.columns.get(col.name, None)
            if (
                column_schema is not None
                and column_schema.type is not UNKNOWN
                and col.type is not UNKNOWN
                and col.type is not column_schema.type
                and col.type.name != column_schema.type.name
            ):
                raise RuntimeError(
                    f"conflicting column type, name {col.name}, expected {column_schema.type}, actual {col.type}"
                )
            if (
                column_schema is None
                or column_schema.type is UNKNOWN
                or (
                    col.type is not UNKNOWN
                    and col.type.nbits > column_schema.type.nbits
                )
            ):
                new_schema[col.name] = col
        self.columns.update(new_schema)

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

    __repr__ = __str__

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
    keep_none: bool = False,
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
                    alias = columns.get(name[1:], "")
                    if alias != "":
                        if keep_none:
                            d[alias] = None
                        else:
                            d.pop(alias, "")
                else:
                    alias = columns.get(name, "")
                    if alias != "" and value is not None:
                        d[alias] = value
            yield d


def _merge_scan(
    iters: List[Iterator[Dict[str, Any]]], keep_none: bool
) -> Iterator[dict]:
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

    nodes = []
    for i, iter in enumerate(iters):
        node = Node(i, iter)
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
            if not keep_none:
                d = {k: v for k, v in d.items() if v is not None}
            yield d
        nodes = [node for node in nodes if not node.exhausted]


def _get_table_files(path: str) -> List[str]:
    ensure_dir(path)

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
    ensure_dir(path)

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
    keep_none: bool = False,
) -> Iterator[dict]:
    iters = []
    for file in _get_table_files(path):
        if os.path.basename(file).startswith("patch"):
            keep = True
        else:
            keep = keep_none
        iters.append(_scan_parquet_file(file, columns, start, end, keep))
    return _merge_scan(iters, keep_none)


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
    if isinstance(d, dict):
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
            and column_schema.type is not UNKNOWN
            and value_type is not UNKNOWN
            and value_type is not column_schema.type
            and value_type.name != column_schema.type.name
        ):
            raise RuntimeError(
                f"can not insert a record with field {col} of type {value_type}, {column_schema.type} expected"
            )
        if column_schema is None:
            new_schema.columns[col] = ColumnSchema(col, value_type)
        elif column_schema.type is UNKNOWN:
            new_schema.columns[col].type = value_type
        elif value_type is not UNKNOWN and value_type.nbits > column_schema.type.nbits:
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

    def scan(
        self,
        columns: Optional[Dict[str, str]] = None,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
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
                d = dict(r)
            else:
                d = {columns[k]: v for k, v in r.items() if k in columns}
                if "-" in r:
                    d["-"] = r["-"]
            d["*"] = r[schema.key_column]
            if not keep_none:
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
        with self.lock:
            schema = self.schema.copy()
        path = _get_table_path(root_path, self.table_name)
        ensure_dir(path)
        while True:
            max_index = -1
            for file in os.listdir(path):
                type, index = _parse_parquet_name(file)
                if type != "" and index > max_index:
                    max_index = index
            if max_index < 0:
                filename = "base-0.parquet"
            else:
                filename = f"base-{max_index + 1}.parquet"
            temp_filename = f"temp.{os.getpid()}"
            if max_index >= 0:
                s = _read_table_schema(os.path.join(path))
                s.merge(schema)
                schema = s
            _write_parquet_file(
                os.path.join(path, temp_filename),
                _records_to_table(
                    schema,
                    list(
                        _merge_scan(
                            [
                                _scan_table(path, keep_none=True),
                                self.scan(keep_none=True),
                            ],
                            True,
                        )
                    ),
                    [],
                ),
            )
            if _check_move(
                os.path.join(path, temp_filename), os.path.join(path, filename)
            ):
                break


class TableDesc:
    def __init__(
        self,
        table_name: str,
        columns: Union[Dict[str, str], List[str], None] = None,
        keep_none: bool = False,
    ) -> None:
        self.table_name = table_name
        self.columns: Optional[Dict[str, str]] = None
        self.keep_none = keep_none
        if columns is not None:
            self.columns = {}
            if isinstance(columns, dict):
                alias_map: Dict[str, str] = {}
                for col, alias in columns.items():
                    key = alias_map.setdefault(alias, col)
                    if key != col:
                        raise RuntimeError(
                            f"duplicate alias {alias} for column {col} and {key}"
                        )
                self.columns = columns
            else:
                for col in columns:
                    if col in self.columns:
                        raise RuntimeError(f"duplicate column name {col}")
                    self.columns[col] = col

    def to_dict(self) -> Dict[str, Any]:
        ret: Dict[str, Any] = {
            "tableName": self.table_name,
        }
        if self.columns is not None:
            ret["columns"] = [
                {"columnName": col, "alias": alias}
                for col, alias in self.columns.items()
            ]
        if self.keep_none:
            ret["keepNone"] = True
        return ret


class LocalDataStore:
    _instance = None
    _lock = threading.Lock()

    @staticmethod
    def get_instance() -> "LocalDataStore":
        with LocalDataStore._lock:
            if LocalDataStore._instance is None:

                ds_path = SWCliConfigMixed().datastore_dir
                ensure_dir(ds_path)

                LocalDataStore._instance = LocalDataStore(str(ds_path))
                atexit.register(LocalDataStore._instance.dump)
            return LocalDataStore._instance

    def __init__(self, root_path: str) -> None:
        self.root_path = root_path
        self.name_pattern = re.compile(r"^[A-Za-z0-9-_/: ]+$")
        self.tables: Dict[str, MemoryTable] = {}

    def update_table(
        self,
        table_name: str,
        schema: TableSchema,
        records: List[Dict[str, Any]],
    ) -> None:
        if self.name_pattern.match(table_name) is None:
            raise RuntimeError(
                f"invalid table name {table_name}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
            )
        for r in records:
            for k in r.keys():
                if k != "-" and self.name_pattern.match(k) is None:
                    raise RuntimeError(
                        f"invalid column name {k}, only letters(A-Z, a-z), digits(0-9), hyphen('-'), and underscore('_') are allowed"
                    )
        table = self.tables.get(table_name, None)
        if table is None:
            table_path = _get_table_path(self.root_path, table_name)
            if _get_table_files(table_path):
                table_schema = _read_table_schema(table_path)
                table_schema.merge(schema)
            else:
                table_schema = schema
            table = MemoryTable(table_name, table_schema)
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
            if "-" in r:
                table.delete([key])
            else:
                table.insert(r)

    def scan_tables(
        self,
        tables: List[TableDesc],
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        class TableInfo:
            def __init__(
                self,
                name: str,
                key_column_type: pa.DataType,
                columns: Optional[Dict[str, str]],
                keep_none: bool,
            ) -> None:
                self.name = name
                self.key_column_type = key_column_type
                self.columns = columns
                self.keep_none = keep_none

        logger.debug(f"scan enter, table size:{len(tables)}")
        infos: List[TableInfo] = []
        for table_desc in tables:
            table = self.tables.get(table_desc.table_name, None)
            if table is not None:
                schema = table.get_schema()
            else:
                schema = _read_table_schema(
                    _get_table_path(self.root_path, table_desc.table_name)
                )
            key_column_type = schema.columns[schema.key_column].type.pa_type
            infos.append(
                TableInfo(
                    table_desc.table_name,
                    key_column_type,
                    table_desc.columns,
                    table_desc.keep_none,
                )
            )

        # check for key type conflictions
        for info in infos:
            if info is infos[0]:
                continue
            if info.key_column_type != infos[0].key_column_type:
                raise RuntimeError(
                    "conflicting key field type. "
                    f"{info.name} has a key of type {info.key_column_type},"
                    f" while {infos[0].name} has a key of type {infos[0].key_column_type}"
                )

        iters = []
        for info in infos:
            table_path = _get_table_path(self.root_path, info.name)
            if info.name in self.tables:
                if _get_table_files(table_path):
                    iters.append(
                        _merge_scan(
                            [
                                _scan_table(
                                    table_path,
                                    info.columns,
                                    start,
                                    end,
                                    info.keep_none,
                                ),
                                self.tables[info.name].scan(
                                    info.columns, start, end, True
                                ),
                            ],
                            info.keep_none,
                        )
                    )
                else:
                    iters.append(
                        self.tables[info.name].scan(
                            info.columns, start, end, info.keep_none
                        )
                    )
            else:
                logger.debug(f"scan by disk table{info.name}")
                iters.append(
                    _scan_table(
                        table_path,
                        info.columns,
                        start,
                        end,
                        info.keep_none,
                    )
                )
        for record in _merge_scan(iters, keep_none):
            record.pop("*", None)
            yield record

    def dump(self) -> None:
        logger.debug(f"start dump, tables size:{len(self.tables.values())}")
        for table in list(self.tables.values()):
            logger.debug(f"dump {table.table_name} to {self.root_path}")
            table.dump(self.root_path)


class RemoteDataStore:
    def __init__(self, instance_uri: str) -> None:
        self.instance_uri = instance_uri
        self.token = os.getenv(SWEnv.instance_token)
        if self.token is None:
            raise RuntimeError("SW_TOKEN is not found in environment")

    def update_table(
        self,
        table_name: str,
        schema: TableSchema,
        records: List[Dict[str, Any]],
    ) -> None:
        data: Dict[str, Any] = {"tableName": table_name}
        schema_data: Dict[str, Any] = {
            "keyColumn": schema.key_column,
            "columnSchemaList": [
                {"name": col.name, "type": str(col.type)}
                for col in schema.columns.values()
            ],
        }
        data["tableSchemaDesc"] = schema_data
        if records is not None:
            encoded: List[Dict[str, List[Dict[str, Optional[str]]]]] = []
            for record in records:
                r: List[Dict[str, Optional[str]]] = []
                for k, v in record.items():
                    if k == "-":
                        r.append({"key": "-", "value": "1"})
                    else:
                        r.append({"key": k, "value": schema.columns[k].type.encode(v)})
                encoded.append({"values": r})
            data["records"] = encoded

        if self.token is None:
            raise MissingFieldError("no authorization token")

        assert self.token is not None
        resp = requests.post(
            urllib.parse.urljoin(self.instance_uri, "/api/v1/datastore/updateTable"),
            data=json.dumps(data, separators=(",", ":")),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": self.token,
            },
            timeout=60,
        )

        if resp.status_code != HTTPStatus.OK:
            logger.error(
                f"[update-table]Table:{table_name}, resp code:{resp.status_code}, \n resp text: {resp.text}, \n records: {records}"
            )
        resp.raise_for_status()

    def scan_tables(
        self,
        tables: List[TableDesc],
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        post_data: Dict[str, Any] = {"tables": [table.to_dict() for table in tables]}
        key_type = _get_type(start)
        assert key_type is not None
        if end is not None:
            post_data["end"] = key_type.encode(end)
        if start is not None:
            post_data["start"] = key_type.encode(start)
        post_data["limit"] = 1000
        if keep_none:
            post_data["keepNone"] = True
        assert self.token is not None
        while True:
            resp = requests.post(
                urllib.parse.urljoin(self.instance_uri, "/api/v1/datastore/scanTable"),
                data=json.dumps(post_data, separators=(",", ":")),
                headers={
                    "Content-Type": "application/json; charset=utf-8",
                    "Authorization": self.token,
                },
                timeout=60,
            )
            resp.raise_for_status()
            resp_json: Dict[str, Any] = resp.json()["data"]
            records = resp_json.get("records", None)
            if records is None or len(records) == 0:
                break
            if "columnTypes" not in resp_json:
                raise RuntimeError("no column types in response")
            column_types = {
                col: _TYPE_NAME_DICT[type]
                for col, type in resp_json["columnTypes"].items()
            }
            for record in records:
                r = {}
                for k, v in record.items():
                    col_type = column_types.get(k, None)
                    if col_type is None:
                        raise RuntimeError(
                            f"unknown type for column {k}, record={record}"
                        )
                    r[k] = col_type.decode(v)
                yield r
            if len(records) == 1000:
                post_data["start"] = resp_json["lastKey"]
                post_data["startInclusive"] = False
            else:
                break


class DataStore(Protocol):
    def update_table(
        self,
        table_name: str,
        schema: TableSchema,
        records: List[Dict[str, Any]],
    ) -> None:
        ...

    def scan_tables(
        self,
        tables: List[TableDesc],
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        ...


def get_data_store() -> DataStore:
    instance_uri = os.getenv(SWEnv.instance_uri)
    if instance_uri is None or instance_uri == "local":
        return LocalDataStore.get_instance()
    else:
        return RemoteDataStore(instance_uri)


def _flatten(record: Dict[str, Any]) -> Dict[str, Any]:
    def _new(key_prefix: str, src: Dict[str, Any], dest: Dict[str, Any]) -> None:
        for k, v in src.items():
            k = key_prefix + str(k)
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
                if (
                    not ch.isalnum()
                    and ch != "-"
                    and ch != "_"
                    and ch != "/"
                    and not ch.isspace()
                ):
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
            self.data_store.update_table(self.table_name, self.schema, records)
