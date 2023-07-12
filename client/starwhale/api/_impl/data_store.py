from __future__ import annotations

import os
import re
import abc
import sys
import copy
import json
import time
import atexit
import base64
import struct
import urllib
import inspect
import zipfile
import binascii
import tempfile
import importlib
import threading
import contextlib
from abc import ABCMeta, abstractmethod
from http import HTTPStatus
from typing import (
    Any,
    Set,
    cast,
    Dict,
    List,
    Type,
    Tuple,
    Union,
    Callable,
    Iterator,
    Optional,
)
from pathlib import Path
from collections import UserDict, OrderedDict

import dill
import numpy as np
import pyarrow as pa  # type: ignore
import requests
import tenacity
import jsonlines
from filelock import FileLock
from jsonlines import Writer
from typing_extensions import Protocol

from starwhale.utils import console
from starwhale.consts import STANDALONE_INSTANCE
from starwhale.utils.fs import ensure_dir
from starwhale.consts.env import SWEnv
from starwhale.utils.error import MissingFieldError, FieldTypeOrValueError
from starwhale.utils.retry import (
    http_retry,
    retry_if_http_exception,
    _RETRY_HTTP_STATUS_CODES,
)
from starwhale.utils.config import SWCliConfigMixed
from starwhale.utils.dict_util import flatten as flatten_dict

datastore_table_file_ext = ".sw-datastore"


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
                raise RuntimeError(f"no element type found for type {type_name}")
            return SwListType(SwType.decode_schema(element_type))
        if type_name == "TUPLE":
            element_type = schema.get("elementType", None)
            if element_type is None:
                raise RuntimeError(f"no element type found for type {type_name}")
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

    @staticmethod
    def decode_schema_from_type_encoded_value(value: Dict[str, Any]) -> "SwType":
        type_name = value.get("type", None)
        if type_name is None:
            raise RuntimeError("no type in schema")
        if type_name == "LIST" or type_name == "TUPLE":
            # {
            # 	"type": "LIST",
            # 	"value": [{
            # 	  "type": "INT8",
            # 	  "value": "01"
            # 	}]
            # }
            element_type: SwType = UNKNOWN
            v = value.get("value", [])
            # TODO: support more than one item types
            if isinstance(v, (list, tuple)) and len(v) != 0:
                element_type = SwType.decode_schema_from_type_encoded_value(v[0])
            if type_name == "LIST":
                return SwListType(element_type)
            else:
                return SwTupleType(element_type)
        if type_name == "MAP":
            # {
            # 	"type": "MAP",
            # 	"value": [{
            # 	  "key": {
            # 	     "type": "INT8",
            # 	     "value": "01"
            # 	  },
            # 	  "value": {
            # 	     "type": "INT16",
            # 	     "value": "0002"
            # 	  }
            # 	}]
            # }
            items = value.get("value", [])
            if len(items) == 0:
                raise RuntimeError("no items in map")
            # TODO: support more than one item types
            k = items[0]["key"]
            v = items[0]["value"]

            key_type = SwType.decode_schema_from_type_encoded_value(k)
            value_type = SwType.decode_schema_from_type_encoded_value(v)
            return SwMapType(key_type, value_type)
        if type_name == "OBJECT":
            # {
            # 	"type": "OBJECT",
            # 	"pythonType": "LINK",
            # 	"value": {
            # 	  "id": {
            # 	    "type": "INT64",
            # 	    "value": "0000000000000001"
            # 	  },
            # 	  "type": {
            # 	    "type": "INT8",
            # 	    "value": "01"
            # 	  }
            # 	}
            # }
            raw_type_name = value.get("pythonType", None)
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
            # try using values as attributes
            values = value.get("value", {})
            if not isinstance(values, dict):
                raise RuntimeError(
                    f"invalid schema, values should be a dict, actual {type(values)}"
                )
            for k, v in values.items():
                attrs[k] = SwType.decode_schema_from_type_encoded_value(v)
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
    def encode_type_encoded_value(self, value: Any, raw_value: bool = False) -> Any:
        ...

    @abstractmethod
    def decode_from_type_encoded_value(self, value: Any) -> Any:
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

    def encode_type_encoded_value(self, value: Any, raw_value: bool = False) -> Any:
        return {
            "type": str(self),
            "value": raw_value and str(value) or self.encode(value),
        }

    def decode_from_type_encoded_value(self, value: Any) -> Any:
        return self.decode(value["value"])

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


# TODO support multiple types for items
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

    def encode_type_encoded_value(self, value: Any, raw_value: bool = False) -> Any:
        if value is None:
            return {"type": str(self), "value": None}
        if isinstance(value, list):
            return {
                "type": "LIST",
                "value": [
                    self.element_type.encode_type_encoded_value(element, raw_value)
                    for element in value
                ],
            }
        raise RuntimeError(f"value should be a list: {value}")

    def decode_from_type_encoded_value(self, value: Any) -> Any:
        value = value["value"]
        if value is None:
            return None
        if isinstance(value, list):
            return [
                self.element_type.decode_from_type_encoded_value(element)
                for element in value
            ]
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

    def encode_type_encoded_value(self, value: Any, raw_value: bool = False) -> Any:
        if value is None:
            return {"type": str(self), "value": None}
        if isinstance(value, tuple):
            return {
                "type": "TUPLE",
                "value": [
                    self.element_type.encode_type_encoded_value(element, raw_value)
                    for element in value
                ],
            }
        raise RuntimeError(f"value should be a tuple: {value}")

    def decode_from_type_encoded_value(self, value: Any) -> Any:
        value = value["value"]
        if value is None:
            return None
        if isinstance(value, list):
            return tuple(
                [
                    self.element_type.decode_from_type_encoded_value(element)
                    for element in value
                ]
            )
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

    def encode_type_encoded_value(self, value: Any, raw_value: bool = False) -> Any:
        if value is None:
            return {"type": str(self), "value": None}
        if isinstance(value, dict):
            return {
                "type": "MAP",
                "value": [
                    {
                        "key": self.key_type.encode_type_encoded_value(k, raw_value),
                        "value": self.value_type.encode_type_encoded_value(
                            v, raw_value
                        ),
                    }
                    for k, v in value.items()
                ],
            }
        raise RuntimeError(f"value should be a dict: {value}")

    def decode_from_type_encoded_value(self, value: Any) -> Any:
        value = value["value"]
        if value is None:
            return None
        if isinstance(value, list):
            return {
                self.key_type.decode_from_type_encoded_value(
                    item["key"]
                ): self.value_type.decode_from_type_encoded_value(item["value"])
                for item in value
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
        if type is UNKNOWN or self is type:
            return self
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

    def encode_type_encoded_value(self, value: Any, raw_value: bool = False) -> Any:
        if value is None:
            return {"type": "OBJECT", "value": None}
        if isinstance(value, self.raw_type):
            return {
                "type": "OBJECT",
                "pythonType": "LINK"
                if self.raw_type is Link
                else self.raw_type.__module__ + "." + self.raw_type.__name__,
                "value": {
                    k: self.attrs[k].encode_type_encoded_value(v, raw_value)
                    for k, v in value.__dict__.items()
                },
            }
        raise RuntimeError(
            f"value should be of type {self.raw_type.__name__}, but is {value}"
        )

    def decode_from_type_encoded_value(self, value: Any) -> Any:
        value = value["value"]
        if value is None:
            return None
        if isinstance(value, dict):
            ret = self.raw_type()
            for k, v in value.items():
                type = self.attrs.get(k, None)
                if type is None:
                    raise RuntimeError(f"invalid attribute {k}")
                ret.__dict__[k] = type.decode_from_type_encoded_value(v)
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

    def dumps(self) -> Dict[str, Any]:
        return {"name": self.name, "type": SwType.encode_schema(self.type)}

    @staticmethod
    def loads(obj: Dict[str, Any]) -> ColumnSchema:
        return ColumnSchema(obj["name"], SwType.decode_schema(obj["type"]))


class TableEmptyException(Exception):
    pass


class Record(UserDict):
    def dumps(self) -> Dict[str, Dict]:
        return {
            "schema": {k: SwType.encode_schema(_get_type(v)) for k, v in self.items()},
            "data": {k: _get_type(v).encode(v) for k, v in self.items()},
        }

    @staticmethod
    def loads(obj: Dict[str, Dict]) -> Record:
        schema = obj["schema"]
        data = obj["data"]
        record = Record()
        for k, v in schema.items():
            record[k] = SwType.decode_schema(v).decode(data[k])
        return record


Records = List[Record]


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


def _get_table_path(root_path: str | Path, table_name: str) -> Path:
    """
    get table path from table name, return the matched file path if there is only one file match the table name
    """
    expect_prefix = Path(root_path) / (table_name.strip("/") + datastore_table_file_ext)
    paths = list(expect_prefix.parent.glob(f"{expect_prefix.name}*"))
    if len(paths) > 1:
        raise RuntimeError(f"can not find table {table_name}, get files {paths}")
    if len(paths) == 1:
        return paths[0]
    return expect_prefix


def _merge_scan(
    iters: List[Iterator[Dict[str, Any]]], keep_none: bool
) -> Iterator[dict]:
    class Node:
        def __init__(self, index: int, iter: Iterator[dict]) -> None:
            self.index = index
            self.iter = iter
            self.item: Optional[Dict[str, Any]] = None
            self.exhausted = False
            self.next_item()

        def next_item(self) -> None:
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
                nodes[i].next_item()
        if len(d) > 0:
            d["*"] = key
            if not keep_none:
                d = {k: v for k, v in d.items() if v is not None}
            yield d
        nodes = [node for node in nodes if not node.exhausted]


def _update_schema(key_column: str, record: Dict[str, Any]) -> TableSchema:
    new_schema = TableSchema(key_column, [])
    for col, value in record.items():
        value_type = _get_type(value)
        new_schema.columns[col] = ColumnSchema(col, value_type)
    return new_schema


class InnerRecord:
    def __init__(self, key_column: str, record: Optional[Record] = None) -> None:
        self.key = ""
        self.key_column = key_column
        self.records: OrderedDict[int, Record] = OrderedDict()
        self.ordered = True
        if record is not None:
            self.append(record)

    def append(self, record: Record) -> str:
        if not self.key:
            self.key = record[self.key_column]
        seq = self._get_seq_num()
        if len(self.records) > 0:
            last = self.get_record()
            # get diff of the last and record
            # let the record only contains the diff or None
            diff = {
                k: v for k, v in record.items() if v is None or last.get(k, None) != v
            }
            if not diff:
                return str(seq)
            record = Record(diff)
        self.ordered = False
        self.records[seq] = record
        return str(seq)

    def update(self, other: InnerRecord | None) -> None:
        if other is None:
            return
        self.records.update(other.records)

    def _reorder(self) -> None:
        if not self.ordered:
            self.records = OrderedDict(sorted(self.records.items()))
            self.ordered = True

    def get_record(
        self, revision: Optional[str] = None, deep_copy: Optional[bool] = None
    ) -> Dict[str, Any]:
        self._reorder()
        ret: Dict[str, Any] = dict()
        for seq, record in self.records.items():
            if revision is None or revision == "" or seq <= int(revision):
                if "-" in record and record["-"]:
                    ret = record.data
                else:
                    if "-" in ret:
                        ret = dict()
                    ret.update(record)
        ret.update({self.key_column: self.key})
        if deep_copy:
            ret = copy.deepcopy(ret)
        return ret

    def dumps(self) -> Dict[str, Any]:
        self._reorder()
        return {
            "key": self.key_column,
            "records": {seq: record.dumps() for seq, record in self.records.items()},
        }

    @staticmethod
    def loads(data: Dict[str, Any]) -> InnerRecord:
        ret = InnerRecord(data["key"])
        ret.ordered = False
        ret.records = OrderedDict(
            {int(seq): Record.loads(record) for seq, record in data["records"].items()}
        )
        first = ret.records[next(iter(ret.records))]

        ret.key = first[data["key"]]
        return ret

    @staticmethod
    def _get_seq_num() -> int:
        return time.monotonic_ns()


class Compressor(abc.ABC):
    @abc.abstractmethod
    def extension(self) -> str:
        """
        Return the extension of the compressed file.
        """
        ...

    @abc.abstractmethod
    def compress(self, source: Path) -> Path:
        """
        Compress the file and return the path to the compressed file.
        """
        ...

    @contextlib.contextmanager
    @abc.abstractmethod
    def decompress(self, source: Path) -> Iterator[Path]:
        """
        Decompress the file and return the path to the temp decompressed file.
        And the temp file will be deleted after the context manager exits.
        """
        ...


class NoCompressor(Compressor):
    def extension(self) -> str:
        return ".json"

    def compress(self, source: Path) -> Path:
        # never be called
        # we need to duplicate the file because the dump method will remove the source file
        raise RuntimeError("should not be called")

    @contextlib.contextmanager
    def decompress(self, source: Path) -> Iterator[Path]:
        # compatible with the existing json file
        yield source


class ZipCompressor(Compressor):
    def extension(self) -> str:
        return ".zip"

    def compress(self, source: Path) -> Path:
        # use the same dir as the source file
        output = source.with_suffix(self.extension())
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as zipf:
            zipf.write(source, source.name)
        return Path(output)

    @contextlib.contextmanager
    def decompress(self, source: Path) -> Iterator[Path]:
        with zipfile.ZipFile(source, "r") as zipf:
            # extract to tmp dir
            tmp_dir = tempfile.TemporaryDirectory()
            zipf.extractall(tmp_dir.name)
            file_name = zipf.namelist()[0]
            try:
                yield Path(tmp_dir.name) / file_name
            finally:
                tmp_dir.cleanup()


# get all the compressors in this module
compressors: Dict[str, Compressor] = {}
for name, obj in inspect.getmembers(sys.modules[__name__]):
    if inspect.isclass(obj) and issubclass(obj, Compressor) and obj != Compressor:
        compressors[obj.__name__] = obj()


def get_compressor(file: Path) -> Compressor:
    for compressor in compressors.values():
        if file.suffix == compressor.extension():
            return compressor
    raise ValueError(f"Unknown compressor for file {file}")


class MemoryTable:
    def __init__(self, table_name: str, key_column: ColumnSchema) -> None:
        self.table_name = table_name
        self.key_column = key_column
        self.records: Dict[Any, InnerRecord] = {}
        self.lock = threading.Lock()
        self.dirty = False
        self.compressor = ZipCompressor()

    def scan(
        self,
        columns: Optional[Dict[str, str]] = None,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
        revision: Optional[str] = None,
        deep_copy: Optional[bool] = None,
    ) -> Iterator[Dict[str, Any]]:
        _end_check: Callable = lambda x, y: x <= y if end_inclusive else x < y
        if deep_copy is None:
            env = os.getenv("SW_DATASTORE_SCAN_DEEP_COPY")
            # make deep copy to True as default if env is not set
            deep_copy = True if env is None else env.strip().upper() == "TRUE"

        with self.lock:
            records = []
            for k, v in self.records.items():
                if (start is None or k >= start) and (
                    end is None or _end_check(k, end)
                ):
                    records.append(v)
        records.sort(key=lambda x: x.key)
        for ir in records:
            r = ir.get_record(revision, deep_copy)
            if columns is None:
                d = dict(r)
            else:
                d = {columns[k]: v for k, v in r.items() if k in columns}
                if "-" in r:
                    d["-"] = r["-"]
            d["*"] = r[self.key_column.name]
            if not keep_none:
                d = {k: v for k, v in d.items() if v is not None}
            yield d

    def insert(self, record: Dict[str, Any]) -> str:
        self.dirty = True
        with self.lock:
            key = record.get(self.key_column.name)
            r = self.records.setdefault(key, InnerRecord(self.key_column.name))
            return r.append(Record(copy.deepcopy(record)))

    def delete(self, keys: List[Any]) -> str | None:
        """
        Delete records by keys. If the key is not found, it will be ignored.
        Returns the sequence number of the last delete operation, or None if no delete operation is performed.
        """
        revision = None
        with self.lock:
            for key in keys:
                r = self.records.get(key, None)
                if r is not None:
                    self.dirty = True
                    revision = r.append(Record({self.key_column.name: key, "-": True}))
        return revision

    def _dump_meta(self) -> Dict[str, Any]:
        return {
            "key_column": self.key_column.dumps(),
            "version": "0.1",
        }

    @classmethod
    def _parse_meta(cls, meta: Any) -> ColumnSchema:
        if not isinstance(meta, dict):
            raise RuntimeError(f"Invalid meta data {meta}")
        if meta["version"] != "0.1":
            raise ValueError(f"Unsupported version {meta['version']}")
        return ColumnSchema.loads(meta["key_column"])

    @classmethod
    def loads(cls, file: Path, table_name: str) -> MemoryTable:
        if not file.exists() or not file.is_file():
            raise RuntimeError(f"File {file} does not exist")

        with get_compressor(file).decompress(file) as f:
            with jsonlines.open(f) as reader:
                meta = reader.read()
                key_column = cls._parse_meta(meta)
                table = MemoryTable(table_name, key_column)
                for record in reader:
                    ir = InnerRecord.loads(record)
                    table.records[ir.key] = ir
        return table

    def dump(self, root_path: str, if_dirty: bool = True) -> None:
        root = Path(root_path)
        lock = root / ".lock"
        with FileLock(lock):
            return self._dump(root, if_dirty)

    def _dump(self, root_path: Path, if_dirty: bool = True) -> None:
        if if_dirty and not self.dirty:
            return

        dst = Path(_get_table_path(root_path, self.table_name))
        base = dst.parent
        temp_filename = base / f"temp.{os.getpid()}"
        ensure_dir(base)

        with jsonlines.open(temp_filename, mode="w") as writer:
            writer.write(self._dump_meta())
            dumped_keys = self._dump_from_local_file(dst, writer)

            for k, ir in self.records.items():
                if k in dumped_keys:
                    continue
                writer.write(ir.dumps())

        compressed = self.compressor.compress(temp_filename)
        os.unlink(temp_filename)
        if dst.suffix == datastore_table_file_ext:
            # the dst file is must not exist, we never save a table as a sw-datastore file
            # use the same extension as compressed file
            ext = datastore_table_file_ext + self.compressor.extension()
        else:
            # the dst file is a compressed file, change the extension
            ext = self.compressor.extension()

        # make dst file have the same extension as compressed file
        new_dst = dst.with_suffix(ext)
        os.rename(compressed, new_dst)
        if new_dst != dst and dst.exists():
            # remove the old file if it is not the new file name
            dst.unlink()
        self.dirty = False

    def _dump_from_local_file(self, existing: Path, output: Writer) -> Set[str]:
        dumped_keys: Set[str] = set()

        if not existing.exists():
            return dumped_keys

        with get_compressor(existing).decompress(existing) as f:
            with jsonlines.open(f, mode="r") as reader:
                self._parse_meta(reader.read())
                for i in reader:
                    ir = InnerRecord.loads(i)
                    r = self.records.get(ir.key)
                    ir.update(r)
                    dumped_keys.add(ir.key)
                    output.write(ir.dumps())

        return dumped_keys


class TableDesc:
    def __init__(
        self,
        table_name: str,
        columns: Union[Dict[str, str], List[str], None] = None,
        keep_none: bool = False,
        revision: Optional[str] = None,
    ) -> None:
        self.table_name = table_name
        self.columns: Optional[Dict[str, str]] = None
        self.keep_none = keep_none
        self.revision = revision
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
        if self.revision is not None:
            ret["revision"] = self.revision
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
        self.lock = threading.Lock()

    def update_table(
        self,
        table_name: str,
        schema: TableSchema,
        records: List[Dict[str, Any]],
    ) -> str:
        if len(records) == 0:
            return ""
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
        table = self._get_table(table_name, schema.columns[schema.key_column])
        # this will never happen, makes mypy happy
        if table is None:
            raise RuntimeError(
                f"table {table_name} does not exist and can not be created"
            )
        if schema.key_column != table.key_column.name:
            raise RuntimeError(
                f"invalid key column, expected {table.key_column}, actual {schema.key_column}"
            )

        revision = None
        for r in records:
            key = r.get(schema.key_column, None)
            if key is None:
                raise RuntimeError(
                    f"key {schema.key_column} should not be none, record: {r.keys()}"
                )
            if "-" in r:
                revision = table.delete([key])
            else:
                revision = table.insert(r)
        # revision will never be None or empty (len(records) > 0), makes mypy happy
        return revision or ""

    def _get_table(
        self, table_name: str, key_column: ColumnSchema | None, create: bool = True
    ) -> MemoryTable | None:
        with self.lock:
            table = self.tables.get(table_name, None)
            if table is None:
                file = Path(_get_table_path(self.root_path, table_name))
                if file.exists():
                    table = MemoryTable.loads(file, table_name)
                else:
                    if not create:
                        return None
                    if key_column is None:
                        raise RuntimeError(
                            f"key column is required for table {table_name}"
                        )
                    table = MemoryTable(table_name, key_column)
                self.tables[table_name] = table
        return table

    def scan_tables(
        self,
        tables: List[TableDesc],
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        class TableInfo:
            def __init__(
                self,
                name: str,
                key_column_type: SwType,
                columns: Optional[Dict[str, str]],
                keep_none: bool,
                revision: Optional[str],
            ) -> None:
                self.name = name
                self.key_column_type = key_column_type
                self.columns = columns
                self.keep_none = keep_none
                self.revision = revision

        infos: List[TableInfo] = []
        for table_desc in tables:
            table = self._get_table(table_desc.table_name, None, create=False)
            if table is None:
                continue
            key_column_type = table.key_column.type
            infos.append(
                TableInfo(
                    table_desc.table_name,
                    key_column_type,
                    table_desc.columns,
                    table_desc.keep_none,
                    table_desc.revision,
                )
            )

        # check for key type conflicts
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
            iters.append(
                self.tables[info.name].scan(
                    info.columns,
                    start,
                    end,
                    info.keep_none,
                    end_inclusive,
                    revision=info.revision,
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

    @http_retry(
        attempts=5,
        wait=tenacity.wait_fixed(1),
        retry=(
            tenacity.retry_if_exception_type(Exception)
            | retry_if_http_exception(_RETRY_HTTP_STATUS_CODES)
        ),
    )
    def update_table(
        self,
        table_name: str,
        schema: TableSchema,
        records: List[Dict[str, Any]],
    ) -> str:
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
            console.error(
                f"[update-table]Table:{table_name}, resp code:{resp.status_code}, \n resp text: {resp.text}, \n records: {records}"
            )
        resp.raise_for_status()
        return resp.json()["data"]  # type: ignore

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
        end_inclusive: bool = False,
    ) -> Iterator[Dict[str, Any]]:
        post_data: Dict[str, Any] = {
            "tables": [table.to_dict() for table in tables],
            "encodeWithType": True,
        }
        key_type = _get_type(start)
        if end is not None:
            post_data["end"] = key_type.encode(end)
        if start is not None:
            post_data["start"] = key_type.encode(start)
        post_data["limit"] = 1000
        if keep_none:
            post_data["keepNone"] = True
        if end_inclusive:
            post_data["endInclusive"] = True
        assert self.token is not None
        while True:
            resp_json = self._do_scan_table_request(post_data)
            records = resp_json.get("records", None)
            if records is None or len(records) == 0:
                break
            for record in records:
                r: Dict[str, Any] = {}
                for k, v in record.items():
                    col_type = SwType.decode_schema_from_type_encoded_value(v)
                    if col_type is None:
                        raise RuntimeError(
                            f"unknown type for column {k}, record={record}"
                        )
                    r[k] = col_type.decode_from_type_encoded_value(v)
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
    ) -> str:
        """
        Update a table with records, and return the revision of the table.
        """
        ...

    def scan_tables(
        self,
        tables: List[TableDesc],
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        keep_none: bool = False,
        end_inclusive: bool = False,
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
        self.key_column = key_column
        self.data_store = data_store or get_data_store()
        self.latest_revision = ""

        self._cond = threading.Condition()
        self._stopped = False
        self._records: List[Tuple[TableSchema, List[Dict[str, Any]]]] = []
        self._updating_records: List[Tuple[TableSchema, List[Dict[str, Any]]]] = []
        self._queue_run_exceptions: List[Exception] = []
        self._run_exceptions_limits = max(run_exceptions_limits, 0)

        self.daemon = True
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
        record = flatten_dict(record)
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
        self._insert({self.key_column: key, "-": True})

    def _insert(self, record: Dict[str, Any]) -> None:
        self._raise_run_exceptions(self._run_exceptions_limits)

        key = record.get(self.key_column, None)
        if key is None:
            raise RuntimeError(
                f"the key {self.key_column} should not be none, record:{record}"
            )
        with self._cond:
            schema = _update_schema(self.key_column, record)
            # TODO: group the records with the same schema
            self._records.append((schema, [record]))
            self._cond.notify()

    def flush(self) -> str:
        """
        Flush the records to the data store, and return the revision of the table.
        """
        while True:
            with self._cond:
                if len(self._records) == 0 and len(self._updating_records) == 0:
                    break
            time.sleep(0.1)
        return self.latest_revision

    def _batch_update_table(
        self, schema: TableSchema, records: List[Dict[str, Any]]
    ) -> None:
        max_batch_size = int(os.environ.get("SW_DATASTORE_UPDATE_MAX_BATCH_SIZE", 1000))
        for i in range(0, len(records), max_batch_size):
            chunk_records = records[i : i + max_batch_size]
            console.trace(
                f"update table {self.table_name}, {len(chunk_records)} records"
            )
            self.latest_revision = self.data_store.update_table(
                self.table_name, schema, chunk_records
            )

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
                to_submit: List[Dict[str, Any]] = []
                last_schema: TableSchema | None = None
                for schema, records in self._updating_records:
                    # group the records with the same schema
                    if last_schema is None:
                        last_schema = schema
                    else:
                        can_merge = True
                        try:
                            last_schema.merge(schema)
                        except Exception:
                            can_merge = False
                        if not can_merge:
                            console.debug(f"schema changed, {last_schema} -> {schema}")
                            self._batch_update_table(last_schema, to_submit)
                            to_submit = []
                            last_schema = schema
                    to_submit.extend(records)
                if len(to_submit) > 0 and last_schema is not None:
                    self._batch_update_table(last_schema, to_submit)
            except Exception as e:
                console.print_exception()
                self._queue_run_exceptions.append(e)
                if len(self._queue_run_exceptions) > self._run_exceptions_limits:
                    break
            finally:
                self._updating_records = []
