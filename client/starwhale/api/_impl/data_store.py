import os
import re
import json
import atexit
import base64
import struct
import urllib
import pathlib
import binascii
import importlib
import threading
from abc import ABCMeta, abstractmethod
from http import HTTPStatus
from typing import Any, Set, cast, Dict, List, Type, Tuple, Union, Iterator, Optional

import dill
import numpy as np
import pyarrow as pa  # type: ignore
import requests
import pyarrow.parquet as pq  # type: ignore
from loguru import logger
from typing_extensions import Protocol

from starwhale.consts import STANDALONE_INSTANCE
from starwhale.utils.fs import ensure_dir
from starwhale.consts.env import SWEnv
from starwhale.utils.error import MissingFieldError, FieldTypeOrValueError
from starwhale.utils.retry import http_retry
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


class SwType(metaclass=ABCMeta):
    def __init__(self, name: str, pa_type: pa.DataType) -> None:
        self.name = name
        self.pa_type = pa_type

    @abstractmethod
    def merge(self, type: "SwType") -> "SwType":
        ...

    def serialize(self, value: Any) -> Any:
        return value

    def deserialize(self, value: Any) -> Any:
        return value

    @staticmethod
    def encode_schema(type: "SwType") -> Dict[str, Any]:
        if isinstance(type, SwScalarType):
            return {"type": str(type)}
        if isinstance(type, SwListType):
            return {
                "type": "LIST",
                "elementType": SwType.encode_schema(type.element_type),
            }
        if isinstance(type, SwTupleType):
            return {
                "type": "TUPLE",
                "elementType": SwType.encode_schema(type.element_type),
            }
        if isinstance(type, SwMapType):
            return {
                "type": "MAP",
                "keyType": SwType.encode_schema(type.key_type),
                "valueType": SwType.encode_schema(type.value_type),
            }
        if isinstance(type, SwObjectType):
            ret = {
                "type": "OBJECT",
                "attributes": [
                    dict(SwType.encode_schema(v), name=k) for k, v in type.attrs.items()
                ],
            }
            if type.raw_type is Link:
                ret["pythonType"] = "LINK"
            else:
                ret["pythonType"] = (
                    type.raw_type.__module__ + "." + type.raw_type.__name__
                )
            return ret
        raise RuntimeError(f"invalid type {type}")

    @staticmethod
    def decode_schema(schema: Dict[str, Any]) -> "SwType":
        type_name = schema.get("type", None)
        if type_name is None:
            raise RuntimeError("no type in schema")
        if type_name == "LIST":
            element_type = schema.get("elementType", None)
            if element_type is None:
                raise RuntimeError("no element type found for type LIST")
            return SwListType(SwType.decode_schema(element_type))
        if type_name == "TUPLE":
            element_type = schema.get("elementType", None)
            if element_type is None:
                raise RuntimeError("no element type found for type TUPLE")
            return SwTupleType(SwType.decode_schema(element_type))
        if type_name == "MAP":
            key_type = schema.get("keyType", None)
            value_type = schema.get("valueType", None)
            if key_type is None:
                raise RuntimeError("no key type found for type MAP")
            if value_type is None:
                raise RuntimeError("no value type found for type MAP")
            return SwMapType(
                SwType.decode_schema(key_type), SwType.decode_schema(value_type)
            )
        if type_name == "OBJECT":
            raw_type_name = schema.get("pythonType", None)
            if raw_type_name is None:
                raise RuntimeError("no python type found for type OBJECT")
            if raw_type_name == "LINK":
                raw_type = Link
            else:
                parts = raw_type_name.split(".")
                raw_type = getattr(
                    importlib.import_module(".".join(parts[:-1])), parts[-1]
                )
            attrs = {}
            attr_schemas = schema.get("attributes", None)
            if attr_schemas is not None:
                if not isinstance(attr_schemas, list):
                    raise RuntimeError("attributes should be a list")
                for attr in attr_schemas:
                    name: str = attr["name"]
                    if not isinstance(name, str):
                        raise RuntimeError(
                            f"invalid schema, attributes should use strings as names, actual {type(name)}"
                        )
                    attrs[name] = SwType.decode_schema(attr)
            return SwObjectType(raw_type, attrs)
        ret = _TYPE_NAME_DICT.get(type_name, None)
        if ret is None:
            raise RuntimeError(f"can not determine schema: {type_name}")
        return ret

    @abstractmethod
    def encode(self, value: Any) -> Optional[Any]:
        ...

    @abstractmethod
    def decode(self, value: Any) -> Any:
        ...

    @abstractmethod
    def __str__(self) -> str:
        ...

    def __repr__(self) -> str:
        return self.__str__()


class SwScalarType(SwType):
    def __init__(
        self, name: str, pa_type: pa.DataType, nbits: int, default_value: Any
    ) -> None:
        super().__init__(name, pa_type)
        self.nbits = nbits
        self.default_value = default_value

    def merge(self, type: SwType) -> SwType:
        if type is UNKNOWN or self is type:
            return self
        if self is UNKNOWN:
            return type
        raise RuntimeError(f"conflicting type {self} and {type}")

    def encode(self, value: Any) -> Optional[Any]:
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
            if self.nbits == 8:
                return binascii.hexlify(struct.pack(">b", value)).decode()
            if self.nbits == 16:
                return binascii.hexlify(struct.pack(">h", value)).decode()
            if self.nbits == 32:
                return binascii.hexlify(struct.pack(">i", value)).decode()
            if self.nbits == 64:
                return binascii.hexlify(struct.pack(">q", value)).decode()
        if self.name == "float":
            if self.nbits == 16:
                return binascii.hexlify(struct.pack(">e", value)).decode()
            if self.nbits == 32:
                return binascii.hexlify(struct.pack(">f", value)).decode()
            if self.nbits == 64:
                return binascii.hexlify(struct.pack(">d", value)).decode()
        raise RuntimeError("invalid type " + str(self))

    def decode(self, value: Any) -> Any:
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
            raw = binascii.unhexlify(value.zfill(int(self.nbits / 4)))
            if self.nbits == 8:
                return struct.unpack(">b", raw)[0]
            if self.nbits == 16:
                return struct.unpack(">h", raw)[0]
            if self.nbits == 32:
                return struct.unpack(">i", raw)[0]
            if self.nbits == 64:
                return struct.unpack(">q", raw)[0]
        if self.name == "float":
            raw = binascii.unhexlify(value.zfill(int(self.nbits / 4)))
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


class SwCompositeType(SwType):
    def __init__(self, name: str) -> None:
        super().__init__(name, pa.binary())

    def serialize(self, value: Any) -> Any:
        return dill.dumps(value)

    def deserialize(self, value: Any) -> Any:
        return dill.loads(value)


class SwListType(SwCompositeType):
    def __init__(self, element_type: SwType) -> None:
        super().__init__("list")
        self.element_type = element_type

    def merge(self, type: SwType) -> SwType:
        if isinstance(type, SwListType):
            t = self.element_type.merge(type.element_type)
            if t is self.element_type:
                return self
            if t is type.element_type:
                return type
            return SwListType(t)
        raise RuntimeError(f"conflicting type {self} and {type}")

    def encode(self, value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, list):
            return [self.element_type.encode(element) for element in value]
        raise RuntimeError(f"value should be a list: {value}")

    def decode(self, value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, list):
            return [self.element_type.decode(element) for element in value]
        raise RuntimeError(f"value should be a list: {value}")

    def __str__(self) -> str:
        return f"[{self.element_type}]"

    def __eq__(self, other: Any) -> bool:
        if isinstance(other, SwListType):
            return self.element_type == other.element_type
        return False


class SwTupleType(SwCompositeType):
    def __init__(self, element_type: SwType) -> None:
        super().__init__("tuple")
        self.element_type = element_type

    def merge(self, type: SwType) -> SwType:
        if isinstance(type, SwTupleType):
            t = self.element_type.merge(type.element_type)
            if t is self.element_type:
                return self
            if t is type.element_type:
                return type
            return SwTupleType(t)
        raise RuntimeError(f"conflicting type {self} and {type}")

    def encode(self, value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, tuple):
            return tuple([self.element_type.encode(element) for element in value])
        raise RuntimeError(f"value should be a tuple: {value}")

    def decode(self, value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, list):
            return tuple([self.element_type.decode(element) for element in value])
        raise RuntimeError(f"value should be a list: {value}")

    def __str__(self) -> str:
        return f"({self.element_type})"

    def __eq__(self, other: Any) -> bool:
        if isinstance(other, SwTupleType):
            return self.element_type == other.element_type
        return False


class SwMapType(SwCompositeType):
    def __init__(self, key_type: SwType, value_type: SwType) -> None:
        super().__init__("map")
        self.key_type = key_type
        self.value_type = value_type

    def merge(self, type: SwType) -> SwType:
        if isinstance(type, SwMapType):
            kt = self.key_type.merge(type.key_type)
            vt = self.value_type.merge(type.value_type)
            if kt is self.key_type and vt is self.value_type:
                return self
            if kt is type.key_type and vt is type.value_type:
                return type
            return SwMapType(kt, vt)
        raise RuntimeError(f"conflicting type {self} and {type}")

    def encode(self, value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, dict):
            return {
                self.key_type.encode(k): self.value_type.encode(v)
                for k, v in value.items()
            }
        raise RuntimeError(f"value should be a dict: {value}")

    def decode(self, value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, dict):
            return {
                self.key_type.decode(k): self.value_type.decode(v)
                for k, v in value.items()
            }
        raise RuntimeError(f"value should be a dict: {value}")

    def __str__(self) -> str:
        return f"{{{self.key_type}:{self.value_type}}}"

    def __eq__(self, other: Any) -> bool:
        if isinstance(other, SwMapType):
            return (
                self.key_type == other.key_type and self.value_type == other.value_type
            )
        return False


class SwObjectType(SwCompositeType):
    def __init__(self, raw_type: Type, attrs: Dict[str, SwType]) -> None:
        super().__init__("object")
        self.raw_type = raw_type
        self.attrs = attrs

    def merge(self, type: SwType) -> SwType:
        if isinstance(type, SwObjectType) and self.raw_type is type.raw_type:
            new_attrs: Dict[str, SwType] = {}
            for attr_name, attr_type in self.attrs.items():
                t = type.attrs.get(attr_name, None)
                if t is not None:
                    new_type = attr_type.merge(t)
                    if new_type is not attr_type:
                        new_attrs[attr_name] = new_type
            for attr_name, attr_type in type.attrs.items():
                if attr_name not in self.attrs:
                    new_attrs[attr_name] = attr_type
            if len(new_attrs) == 0:
                return self
            attrs = dict(self.attrs)
            attrs.update(new_attrs)
            return SwObjectType(self.raw_type, attrs)
        raise RuntimeError(f"conflicting type {str(self)} and {str(type)}")

    def encode(self, value: Any) -> Optional[Any]:
        if value is None:
            return None
        if isinstance(value, self.raw_type):
            ret: Dict[str, Any] = {}
            for k, v in value.__dict__.items():
                type = self.attrs.get(k, None)
                if type is None:
                    raise RuntimeError(f"invalid attribute {k}")
                ret[k] = type.encode(v)
            return ret
        raise RuntimeError(
            f"value should be of type {self.raw_type.__name__}, but is {value}"
        )

    def decode(self, value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, dict):
            ret = self.raw_type()
            for k, v in value.items():
                type = self.attrs.get(k, None)
                if type is None:
                    raise RuntimeError(f"invalid attribute {k}")
                ret.__dict__[k] = type.decode(v)
            return ret
        raise RuntimeError(f"value should be a dict: {value}")

    def __str__(self) -> str:
        return (
            self.raw_type.__name__
            + "{"
            + ",".join([f"{k}:{v}" for k, v in self.attrs.items()])
            + "}"
        )

    def __eq__(self, other: Any) -> bool:
        if isinstance(other, SwObjectType):
            return self.attrs == other.attrs
        return False


UNKNOWN = SwScalarType("unknown", None, 1, None)
INT8 = SwScalarType("int", pa.int8(), 8, 0)
INT16 = SwScalarType("int", pa.int16(), 16, 0)
INT32 = SwScalarType("int", pa.int32(), 32, 0)
INT64 = SwScalarType("int", pa.int64(), 64, 0)
FLOAT16 = SwScalarType("float", pa.float16(), 16, 0.0)
FLOAT32 = SwScalarType("float", pa.float32(), 32, 0.0)
FLOAT64 = SwScalarType("float", pa.float64(), 64, 0.0)
BOOL = SwScalarType("bool", pa.bool_(), 1, 0)
STRING = SwScalarType("string", pa.string(), 32, "")
BYTES = SwScalarType("bytes", pa.binary(), 32, b"")

_TYPE_DICT: Dict[Any, SwScalarType] = {
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

_TYPE_NAME_DICT = {str(v): v for v in _TYPE_DICT.values()}


def _get_type(obj: Any) -> SwType:
    element_type: SwType = UNKNOWN
    if isinstance(obj, list):
        for element in obj:
            element_type = element_type.merge(_get_type(element))
        return SwListType(element_type)
    if isinstance(obj, tuple):
        for element in obj:
            element_type = element_type.merge(_get_type(element))
        return SwTupleType(element_type)
    if isinstance(obj, dict):
        key_type: SwType = UNKNOWN
        value_type: SwType = UNKNOWN
        for k, v in obj.items():
            key_type = key_type.merge(_get_type(k))
            value_type = value_type.merge(_get_type(v))
        return SwMapType(key_type, value_type)
    if isinstance(obj, SwObject):
        attrs = {}
        for k, v in obj.__dict__.items():
            attrs[k] = _get_type(v)
        return SwObjectType(type(obj), attrs)
    ret = _TYPE_DICT.get(type(obj), None)
    if ret is None:
        raise RuntimeError(f"unsupported type {type(obj)}")
    return ret


class SwObject:
    def __str__(self) -> str:
        return json.dumps(self._to_dict())

    def __eq__(self, other: Any) -> bool:
        return type(other) is type(self) and self.__dict__ == other.__dict__

    def _to_dict(self) -> Dict[str, Any]:
        ret: Dict[str, Any] = {}
        for k, v in self.__dict__.items():
            if isinstance(v, SwObject):
                ret[k] = v._to_dict()
            elif isinstance(v, bytes):
                ret[k] = BYTES.encode(v)
            else:
                ret[k] = v
        return ret


class Link(SwObject):
    def __init__(
        self,
        uri: Optional[str] = None,
        display_text: Optional[str] = None,
        mime_type: Optional[str] = None,
    ) -> None:
        self.uri = uri
        self.display_text = display_text
        self.mime_type = mime_type


class ColumnSchema:
    def __init__(self, name: str, type: SwType) -> None:
        self.name = name
        self.type = type

    def __eq__(self, other: Any) -> bool:
        return (
            isinstance(other, ColumnSchema)
            and self.name == other.name
            and self.type == other.type
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
            if column_schema is None:
                new_schema[col.name] = col
            else:
                try:
                    column_schema.type = column_schema.type.merge(col.type)
                except RuntimeError as e:
                    raise RuntimeError(f"can not update column {col.name}") from e
        self.columns.update(new_schema)

    @staticmethod
    def parse(json_str: str) -> "TableSchema":
        d = json.loads(json_str)
        return TableSchema(
            d["key"],
            [
                ColumnSchema(col["name"], SwType.decode_schema(col))
                for col in d["columns"]
            ],
        )

    def __str__(self) -> str:
        columns = []
        for col in self.columns.values():
            d = SwType.encode_schema(col.type)
            d["name"] = col.name
            columns.append(d)
        return json.dumps(
            {
                "key": self.key_column,
                "columns": columns,
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


def _update_schema(schema: TableSchema, record: Dict[str, Any]) -> TableSchema:
    new_schema = schema.copy()
    for col, value in record.items():
        value_type = _get_type(value)
        column_schema = schema.columns.get(col, None)
        if column_schema is None:
            new_schema.columns[col] = ColumnSchema(col, value_type)
        else:
            try:
                new_schema.columns[col].type = new_schema.columns[col].type.merge(
                    value_type
                )
            except RuntimeError as e:
                raise RuntimeError(f"can not insert a record with field {col}") from e

    return new_schema


class MemoryTable:
    def __init__(self, table_name: str, schema: TableSchema) -> None:
        self.table_name = table_name
        self.schema = schema.copy()
        self.records: Dict[Any, Dict[str, Any]] = {}
        self.deletes: Set[Any] = set()
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
            if r is not record:
                r.update(record)

    def delete(self, keys: List[Any]) -> None:
        with self.lock:
            for key in keys:
                self.deletes.add(key)
                self.records.pop(key, None)

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

    def __str__(self) -> str:
        return f"LocalDataStore, root:{self.root_path}"

    __repr__ = __str__

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
            r: Dict[str, Any] = {}
            for k, v in record.items():
                if k != "*":
                    r[k] = v
            yield r

    def dump(self) -> None:
        for table in list(self.tables.values()):
            table.dump(self.root_path)


class RemoteDataStore:
    def __init__(self, instance_uri: str, token: str) -> None:
        if not instance_uri:
            raise FieldTypeOrValueError("instance_uri not set")

        if not token:
            raise FieldTypeOrValueError("token not set")

        self.instance_uri = instance_uri
        self.token = token

    def __str__(self) -> str:
        return f"RemoteDataStore for {self.instance_uri}"

    __repr__ = __str__

    @http_retry
    def update_table(
        self,
        table_name: str,
        schema: TableSchema,
        records: List[Dict[str, Any]],
    ) -> None:
        data: Dict[str, Any] = {"tableName": table_name}
        column_schemas = []
        for col in schema.columns.values():
            d = SwType.encode_schema(col.type)
            d["name"] = col.name
            column_schemas.append(d)
        data["tableSchemaDesc"] = {
            "keyColumn": schema.key_column,
            "columnSchemaList": column_schemas,
        }
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

    @http_retry
    def _do_scan_table_request(self, post_data: Dict[str, Any]) -> Dict[str, Any]:
        resp = requests.post(
            urllib.parse.urljoin(self.instance_uri, "/api/v1/datastore/scanTable"),
            data=json.dumps(post_data, separators=(",", ":")),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": self.token,  # type: ignore
            },
            timeout=60,
        )
        resp.raise_for_status()
        return resp.json()["data"]  # type: ignore

    def scan_tables(
        self,
        tables: List[TableDesc],
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        post_data: Dict[str, Any] = {"tables": [table.to_dict() for table in tables]}
        key_type = _get_type(start)
        if end is not None:
            post_data["end"] = key_type.encode(end)
        if start is not None:
            post_data["start"] = key_type.encode(start)
        post_data["limit"] = 1000
        if keep_none:
            post_data["keepNone"] = True
        assert self.token is not None
        while True:
            resp_json = self._do_scan_table_request(post_data)
            records = resp_json.get("records", None)
            if records is None or len(records) == 0:
                break
            column_types_list = resp_json.get("columnTypes", None)
            if column_types_list is None:
                raise RuntimeError("no column types in response")
            column_types = {
                col["name"]: SwType.decode_schema(col) for col in column_types_list
            }
            for record in records:
                r: Dict[str, Any] = {}
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


def get_data_store(instance_uri: str = "", token: str = "") -> DataStore:
    _instance_uri = instance_uri or os.getenv(SWEnv.instance_uri)
    if _instance_uri is None or _instance_uri == STANDALONE_INSTANCE:
        return LocalDataStore.get_instance()
    else:
        token = (
            token
            or SWCliConfigMixed().get_sw_token(instance=instance_uri)
            or os.getenv(SWEnv.instance_token, "")
        )
        return RemoteDataStore(
            instance_uri=_instance_uri,
            token=token,
        )


def _flatten(record: Dict[str, Any]) -> Dict[str, Any]:
    def _new(key_prefix: str, src: Dict[str, Any], dest: Dict[str, Any]) -> None:
        for k, v in src.items():
            k = key_prefix + str(k)
            if isinstance(v, dict):
                _new(k + "/", v, dest)
            else:
                dest[k] = v

    ret: Dict[str, Any] = {}
    _new("", record, ret)
    return ret


class TableWriterException(Exception):
    pass


class TableWriter(threading.Thread):
    def __init__(
        self,
        table_name: str,
        key_column: str = "id",
        data_store: Optional[DataStore] = None,
        run_exceptions_limits: int = 100,
    ) -> None:
        super().__init__(name=f"TableWriter-{table_name}")
        self.table_name = table_name
        self.schema = TableSchema(key_column, [])
        self.data_store = data_store or get_data_store()

        self._cond = threading.Condition()
        self._stopped = False
        self._records: List[Dict[str, Any]] = []
        self._updating_records: List[Dict[str, Any]] = []
        self._queue_run_exceptions: List[Exception] = []
        self._run_exceptions_limits = max(run_exceptions_limits, 0)

        self.setDaemon(True)
        atexit.register(self.close)
        self.start()

    def __enter__(self) -> Any:
        return self

    def __exit__(self, type: Any, value: Any, tb: Any) -> None:
        self.close()

    def close(self) -> None:
        with self._cond:
            if not self._stopped:
                atexit.unregister(self.close)
                self._stopped = True
                self._cond.notify()
        self.join()
        self._raise_run_exceptions(0)

    def _raise_run_exceptions(self, limits: int) -> None:
        if len(self._queue_run_exceptions) > limits:
            _es = self._queue_run_exceptions
            self._queue_run_exceptions = []
            raise TableWriterException(f"{self} run raise {len(_es)} exceptions: {_es}")

    def insert(self, record: Dict[str, Any]) -> None:
        record = _flatten(record)
        for k in record:
            for ch in k:
                if (
                    not ch.isalnum()
                    and ch != "-"
                    and ch != "_"
                    and ch != "/"
                    and ch != ":"
                    and not ch.isspace()
                ):
                    raise RuntimeError(f"invalid field {k}")
        self._insert(record)

    def delete(self, key: Any) -> None:
        self._insert({self.schema.key_column: key, "-": True})

    def _insert(self, record: Dict[str, Any]) -> None:
        self._raise_run_exceptions(self._run_exceptions_limits)

        key = record.get(self.schema.key_column, None)
        if key is None:
            raise RuntimeError(
                f"the key {self.schema.key_column} should not be none, record:{record}"
            )
        with self._cond:
            self.schema = _update_schema(self.schema, record)
            self._records.append(record)
            self._cond.notify()

    def flush(self) -> None:
        while True:
            with self._cond:
                if len(self._records) == 0 and len(self._updating_records) == 0:
                    break

    def run(self) -> None:
        while True:
            with self._cond:
                while not self._stopped and len(self._records) == 0:
                    self._cond.wait()
                if len(self._records) == 0:
                    break
                self._updating_records = self._records
                self._records = []

            try:
                self.data_store.update_table(
                    self.table_name, self.schema, self._updating_records
                )
            except Exception as e:
                logger.exception(e)
                self._queue_run_exceptions.append(e)
                if len(self._queue_run_exceptions) > self._run_exceptions_limits:
                    break
            finally:
                self._updating_records = []
