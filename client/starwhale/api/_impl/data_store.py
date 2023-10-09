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
import shutil
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
from uuid import uuid4
from typing import (
    Any,
    cast,
    Dict,
    List,
    Type,
    Tuple,
    Union,
    Iterator,
    Optional,
    Sequence,
)
from pathlib import Path
from collections import UserDict

import dill
import numpy as np
import pyarrow as pa  # type: ignore
import filelock
import requests
import tenacity
import jsonlines
from pydantic import validator
from sortedcontainers import SortedDict, SortedList, SortedValuesView
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
from starwhale.base.models.base import SwBaseModel

datastore_table_file_ext = ".sw-datastore"
datastore_manifest_file_name = "manifest.json"
datastore_max_dirty_records = int(os.getenv("DATASTORE_MAX_DIRTY_RECORDS", "10000"))


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
                return SwMapType(UNKNOWN, UNKNOWN)
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
        self.key: Any = None
        self.key_column = key_column
        self.records: SortedDict[int, Record] = SortedDict()
        if record is not None:
            self.append(record)

    def append(
        self,
        record: Record,
        tombstones: Sequence[TombstoneDesc] | None = None,
    ) -> str:
        if not self.key:
            self.key = record[self.key_column]
        seq = self.gen_seq_num()
        if len(self.records) > 0:
            last = self.get_record(tombstones=tombstones)
            # get diff of the last and record
            # let the record only contains the diff or None
            # TODO support recursive diff
            if last is None:
                diff = record.data
            else:
                diff = {
                    k: v
                    for k, v in record.items()
                    if v is None or last.get(k, None) != v
                }
            if not diff:
                return str(seq)
            record = Record(diff)
        self.records[seq] = record
        return str(seq)

    def update(self, other: InnerRecord | None) -> None:
        if other is None:
            return
        self.records.update(other.records)

    def get_record(
        self,
        revision: Optional[str] = None,
        deep_copy: bool = False,
        tombstones: Sequence[TombstoneDesc] | None = None,
    ) -> Dict[str, Any] | None:
        ret: Dict[str, Any] = dict()
        tombstones = [t for t in tombstones or [] if t.within(self.key)]
        max_tombstone_rev = tombstones and int(tombstones[-1].revision) or None
        had_value = False
        for seq, record in self.records.items():
            if revision is None or revision == "" or seq <= int(revision):
                if max_tombstone_rev is not None and seq <= max_tombstone_rev:
                    # skip the record if it is covered by the tombstone
                    continue
                if "-" in record and record["-"]:
                    ret = record.data
                else:
                    had_value = True
                    if "-" in ret:
                        ret = dict()
                    ret.update(record)
        if not had_value:
            return None
        ret.update({self.key_column: self.key})
        if deep_copy:
            ret = copy.deepcopy(ret)
        return ret

    def dumps(self) -> Dict[str, Any]:
        return {
            "key": self.key_column,
            "records": {seq: record.dumps() for seq, record in self.records.items()},
        }

    @property
    def revision_range(self) -> Tuple[int, int]:
        if len(self.records) == 0:
            return 0, 0
        keys = self.records.keys()
        return keys[0], keys[-1]

    @staticmethod
    def loads(data: Dict[str, Any]) -> InnerRecord:
        ret = InnerRecord(data["key"])
        ret.records = SortedDict(
            {int(seq): Record.loads(record) for seq, record in data["records"].items()}
        )
        first = ret.records[next(iter(ret.records))]

        ret.key = first[data["key"]]
        return ret

    @staticmethod
    def gen_seq_num() -> int:
        return time.monotonic_ns()

    def compact(self, revisions: Sequence[str]) -> None:
        """
        merge the records that between the revisions
        e.g.
        records = {1: {"a": 1}, 2: {"a": 2}, 3: {"b": 3}, 4: {"c": 4}, 5: {"d": 5}}
        revisions = ["1", "5"]
        result = {1: {"a": 1}, 5: {"a": 2, "b": 3, "c": 4, "d": 5}}
        """
        revs = sorted([int(rev) for rev in revisions])
        # add 0 for the first revision
        revs.insert(0, 0)

        rev_idx = 0
        seq_idx = 0
        prev_seq = 0
        seqs = list(self.records.keys())
        while True:
            seq = seqs[seq_idx]
            if revs[rev_idx] < seq <= revs[rev_idx + 1]:
                # merge the records between the revisions
                if prev_seq != 0:
                    self.records[prev_seq].update(self.records[seq])
                    self.records[seq] = self.records[prev_seq]
                    del self.records[prev_seq]
                prev_seq = seq
                seq_idx += 1
                if seq_idx >= len(seqs):
                    break
            else:
                rev_idx += 1
                prev_seq = 0
                if rev_idx >= len(revs):
                    break

    def __gt__(self, other: object) -> Any:
        if not isinstance(other, InnerRecord):
            return NotImplemented
        return self.key > other.key

    def __ge__(self, other: object) -> Any:
        if not isinstance(other, InnerRecord):
            return NotImplemented
        return self.key >= other.key


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
            # use the same dir as the extracted tmp parent dir
            tmp_dir = tempfile.TemporaryDirectory(dir=source.parent)
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


def revision_range_of_items(
    items: List[InnerRecord] | SortedValuesView[InnerRecord],
) -> Tuple[int, int]:
    if len(items) == 0:
        return 0, 0
    _min = sys.maxsize
    _max = 0
    for item in items:
        r = item.revision_range
        if r[0] < _min:
            _min = r[0]
        if r[1] > _max:
            _max = r[1]
    return _min, _max


class MemoryTable:
    def __init__(self, key_column: str) -> None:
        self.key_column = key_column
        self.records: SortedDict[Any, InnerRecord] = SortedDict()
        self.lock = threading.Lock()
        self.dirty = False
        self.compressor = ZipCompressor()
        self._key_type: SwType | None = None
        self._max_dirty_records = datastore_max_dirty_records
        self._min_revision = 0
        self._max_revision = 0

    @property
    def key_type(self) -> SwType | None:
        return self._key_type

    def scan(
        self,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        end_inclusive: bool = False,
    ) -> Iterator[InnerRecord]:
        for key, record in self.records.items():
            if start is not None and key < start:
                continue

            if end_inclusive:
                if end is not None and key > end:
                    break
            else:
                if end is not None and key >= end:
                    break

            yield record

    def get(self, key: Any) -> Optional[InnerRecord]:
        return self.records.get(key, None)  # type: ignore

    def _must_valid_key_type(self, key: Any) -> None:
        key_type = _get_type(key)
        if self._key_type is None:
            self._key_type = key_type
        elif self._key_type != key_type:
            raise RuntimeError(
                f"conflicting key type, expected {self._key_type}, actual {key_type}"
            )

    def insert_record(self, record: InnerRecord) -> None:
        self.dirty = True
        key = record.key
        self._must_valid_key_type(key)
        ir = self.records.get(key, None)
        if ir is None:
            self.records[key] = record
        else:
            ir.update(record)

    def insert_dict(
        self, record: Dict[str, Any], tombstones: Sequence[TombstoneDesc] | None
    ) -> str:
        self.dirty = True
        key = record[self.key_column]
        self._must_valid_key_type(key)
        ir = self.get(key)
        if ir is None:
            ir = InnerRecord(self.key_column)
            self.records[key] = ir
        return ir.append(Record(copy.deepcopy(record)), tombstones=tombstones)

    def delete(self, keys: List[Union[int, str]]) -> str:
        """
        Delete records by keys. If the key is not found, it will be ignored.
        Returns the sequence number of the last delete operation, or None if no delete operation is performed.
        """
        if len(keys) == 0:
            raise RuntimeError("keys can not be empty")

        revision = None
        self.dirty = True
        for key in keys:
            r = self.get(key)
            if r is None:
                r = InnerRecord(self.key_column)
                self.records[key] = r

            del_mark = Record({self.key_column: key, "-": True})
            revision = r.append(del_mark)
        return revision  # type: ignore

    def should_dump(self) -> bool:
        return len(self.records) >= self._max_dirty_records

    @property
    def revision_range(self) -> Tuple[int, int]:
        return revision_range_of_items(self.records.values())


class DataBlock:
    def __init__(
        self,
        min_key: Any,
        max_key: Any,
        key_column: str,
        block_id: int | None = None,
        file: Path | None = None,
    ) -> None:
        self.min_key = min_key
        self.max_key = max_key
        self.key_column = key_column
        self.file = file
        self.block_id = block_id

    @property
    def virtual(self) -> bool:
        return self.file is None

    def ahead(self, key: Any) -> bool:
        return key is not None and self.max_key < key

    def behind(self, key: Any) -> bool:
        return key is not None and self.min_key > key

    def __ge__(self, other: object) -> Any:
        if not isinstance(other, DataBlock):
            raise NotImplementedError
        return self.min_key >= other.max_key

    def __gt__(self, other: object) -> Any:
        if not isinstance(other, DataBlock):
            raise NotImplementedError
        return self.min_key > other.max_key

    def load(self) -> MemoryTable:
        if self.file is None:
            raise RuntimeError("can not load cache for virtual block")
        if not self.file.exists():
            raise RuntimeError(f"can not find file {self.file}")
        compressor = get_compressor(self.file)
        with compressor.decompress(self.file) as file:
            with jsonlines.open(file) as reader:
                # meta = reader.read()
                # TODO validate the meta
                table = MemoryTable(self.key_column)
                for record in reader:
                    table.insert_record(InnerRecord.loads(record))
                return table

    def scan(
        self,
        start: Optional[Any] = None,
        end: Optional[Any] = None,
        end_inclusive: bool = False,
    ) -> Iterator[InnerRecord]:
        cache = self.load()
        yield from cache.scan(start, end, end_inclusive)

    def dump(
        self,
        records: List[InnerRecord] | SortedValuesView[InnerRecord],
        compressor: Compressor,
        tmp_dir: str | None = None,
    ) -> None:
        if self.file is None:
            raise RuntimeError("can not dump cache for virtual block")

        with tempfile.NamedTemporaryFile(mode="w", dir=tmp_dir) as tmp:
            with jsonlines.Writer(tmp) as writer:
                for record in records:
                    writer.write(record.dumps())
            tmp.flush()
            tmp_path = compressor.compress(Path(tmp.name))
            tmp_path.rename(self.file)


KeyType = Union[int, str, None]


class DataBlockDesc(SwBaseModel):
    min_key: KeyType
    max_key: KeyType
    min_revision: Optional[str]
    max_revision: Optional[str]
    row_count: int
    block_id: Optional[int]


class TombstoneDesc(SwBaseModel):
    # None means from the beginning
    start: KeyType
    # None means to the end
    end: KeyType
    end_inclusive: bool
    revision: str
    # Mark the tombstone for the key with the prefix
    # This works only if the key is a string
    key_prefix: Optional[str]

    @validator("end")
    def end_must_be_greater_than_start(
        cls, v: KeyType, values: Dict[str, Any]
    ) -> KeyType:
        if v is None:
            return None
        if values["start"] is not None:
            if not isinstance(v, type(values["start"])):
                raise ValueError("end has different type with start")
            if v <= values["start"]:
                raise ValueError("end must be greater than start")
        return v  # type: ignore

    def __gt__(self, other: object) -> Any:
        if not isinstance(other, TombstoneDesc):
            raise NotImplementedError
        return self.revision > other.revision

    def within(self, key: KeyType) -> bool:
        if self.key_prefix is not None and isinstance(key, str):
            return key.startswith(self.key_prefix)
        if self.start is not None and self.start > key:  # type: ignore
            return False
        if self.end is not None:
            if self.end_inclusive:
                if self.end < key:  # type: ignore
                    return False
            else:
                if self.end <= key:  # type: ignore
                    return False
        return True


class CheckpointDesc(SwBaseModel):
    revision: str
    created_at: int  # timestamp in ms
    count: Optional[int]

    def to_checkpoint(self) -> Checkpoint:
        return Checkpoint(
            revision=self.revision,
            created_at=self.created_at,
        )

    def __gt__(self, other: object) -> Any:
        if not isinstance(other, CheckpointDesc):
            raise NotImplementedError
        return self.revision > other.revision


class GarbageCollectionDesc(SwBaseModel):
    revision: str  # the data before this revision(contain) has been garbage collected


class DataBlockConfig(SwBaseModel):
    block_name_prefix: str = "data"
    max_block_size: int = 10000  # TODO make this configurable


class Manifest(SwBaseModel):
    """
                       ┌──────────────┐     ┌──────────────┐      ┌──────────────┐
                       │   DataBlock  │     │   DataBlock  │      │   DataBlock  │
                       └──────────────┘     └──────────────┘      └──────────────┘
    ┌─────────────┐
    │  Tombstone  ├────────────────────────────────────────────────────────────────────
    └─────────────┘
                       ┌──────────────┐     ┌──────────────┐      ┌──────────────┐
                       │   DataBlock  │     │   DataBlock  │      │   DataBlock  │
                       └──────────────┘     └──────────────┘      └──────────────┘
    ┌──────────────┐
    │  Checkpoint  ├────────────────────────────────────────────────────────────────────
    └──────────────┘
    """

    version: str = "0.1.2"
    block_config: DataBlockConfig
    blocks: List[DataBlockDesc]
    key_column: str
    key_column_type: Optional[Dict[str, Any]]  # SwType.encode_schema
    next_block_id: int = 0
    # last key is the max key in the life cycle of the table
    # last key won't change if the key is deleted and garbage collected
    # users can use the (last key + 1) as the next auto increment key
    # and note that max_key in DataBlockDesc may change if the key is deleted and garbage collected
    last_key: KeyType
    # TODO record the last revision
    last_revision = "0"
    garbage_collection: Optional[GarbageCollectionDesc]
    tombstones: List[TombstoneDesc]
    checkpoints: List[CheckpointDesc]

    def get_tombstones(self, max_revision: str | None) -> List[TombstoneDesc]:
        return sorted(
            [
                tombstone
                for tombstone in self.tombstones
                if max_revision is None or tombstone.revision <= max_revision
            ]
        )


class IterWithRangeHint(SwBaseModel):
    iter: Iterator[InnerRecord]
    min_key: KeyType  # set it to None if the iter must be touched at first, or you don't know the min key

    def __gt__(self, other: Any) -> Any:
        if not isinstance(other, IterWithRangeHint):
            raise NotImplementedError

        if self.min_key is None:
            return True  # we assume that the iter must be touched at first
        if other.min_key is None:
            return False

        # raise err if min_key has diff type
        if not isinstance(self.min_key, type(other.min_key)):
            raise RuntimeError(
                f"min_key has diff type: {type(self.min_key)} != {type(other.min_key)}"
            )

        return self.min_key > other.min_key  # type:  ignore[operator]


def _merge_iters_with_hint(iters: List[IterWithRangeHint]) -> Iterator[InnerRecord]:
    """
    Merge multiple iterators into one iterator.
    This method will
    - merge the records with the same key.
    - keep the order of the records.
    - do not touch the iterator until it is needed.
    """

    class Node:
        def __init__(self, iter: Iterator[InnerRecord]) -> None:
            self.key: Any = None
            self.iter = iter
            self.item: Optional[InnerRecord] = None
            self.exhausted = False
            self.next_item()

        def next_item(self) -> None:
            try:
                self.item = next(self.iter)
                self.exhausted = False
                self.key = self.item.key
            except StopIteration:
                self.exhausted = True
                self.item = None
                self.key = None

    iters = sorted(iters)
    nodes: List[Node] = []
    # find the first non-empty iterator and get the global min key
    while len(iters) > 0 and len(nodes) == 0:
        n = Node(iters.pop(0).iter)
        if not n.exhausted:
            nodes.append(n)

    def add_iters_to_node_if_needed(
        _nodes: List[Node], _iters: List[IterWithRangeHint]
    ) -> Tuple[List[Node], List[IterWithRangeHint]]:
        """
        Add the iterators to the nodes if the min key of the iterator is less than the global min key.
        This method have no side effect to the input parameters.
        """
        ret_nodes = _nodes[:]
        min_key = len(ret_nodes) > 0 and min(ret_nodes, key=lambda x: x.key).key or None
        filtered: List[IterWithRangeHint] = []
        for _iter in _iters:
            if (
                _iter.min_key is None
                or min_key is None
                or _iter.min_key <= min_key
                or len(ret_nodes) == 0
            ):
                _n = Node(_iter.iter)
                if not _n.exhausted:
                    ret_nodes.append(_n)
                    min_key = min(ret_nodes, key=lambda x: x.key).key
            else:
                filtered.append(_iter)
        return ret_nodes, sorted(filtered)

    # note that there may be multiple iterators with the same min key or None
    # we need to merge them together
    nodes, iters = add_iters_to_node_if_needed(nodes, iters)

    if len(nodes) == 0:
        return

    while len(nodes) > 0:
        key = min(nodes, key=lambda x: x.key).key
        ret: InnerRecord | None = None
        for i in range(len(nodes)):
            while nodes[i].key == key:
                item = nodes[i].item
                if ret is None:
                    ret = item
                else:
                    ret.update(item)
                nodes[i].next_item()
        if ret is not None:
            yield ret

        nodes = [node for node in nodes if not node.exhausted]

        # touch the iterator if the min key of the iterator is less than the global min key
        nodes, iters = add_iters_to_node_if_needed(nodes, iters)


class LocalTable:
    """
    LocalTable is a table that stores data in local file system. it contains a memory table and multiple data blocks.
    The memory table is used to store the latest data. The data blocks are used to store the historical data.

    The architecture diagram:

    +-----------------+  +-----------------+  +-----------------+
    |   MemoryTable   |  |   DataBlock 1   |  |   DataBlock 2   |
    +-----------------+  +-----------------+  +-----------------+
    |  key1:  record7 |  |  key1: record1  |  |  key11: record4 |
    |  key12: record8 |  |  key2: record2  |  |  key12: record5 |
    |  key33: record9 |  |  key3: record3  |  |  key13: record6 |
    +-----------------+  +-----------------+  +-----------------+

    """

    def __init__(
        self,
        table_name: str,
        root_path: str | Path,
        key_column: str | None,
        create_if_missing: bool = True,
    ) -> None:
        self.table_name = table_name
        self.root_path = Path(root_path)
        self.key_column = key_column
        self.lock = threading.Lock()
        self._dump_lock = threading.Lock()
        self._data_block_lock = threading.Lock()
        self.compressor = ZipCompressor()
        self.create_if_missing = create_if_missing
        self._key_column_type: SwType | None = None
        self._load_manifest()
        self.memory_table: MemoryTable | None = None
        self._immutable_memory_table: MemoryTable | None = None
        if self.key_column is not None:
            self.memory_table = MemoryTable(self.key_column)

        # tombstones cache
        self._tombstones: List[TombstoneDesc] = []

        # ensure the root path exists
        self.root_path.mkdir(parents=True, exist_ok=True)

    @property
    def key_column_type(self) -> SwType | None:
        return self.memory_table and self.memory_table.key_type or self._key_column_type

    @property
    def tombstones(self) -> List[TombstoneDesc]:
        if not self._tombstones:
            self._tombstones = self._load_manifest().get_tombstones(None)
        return self._tombstones

    def _must_get_mem_table(self) -> MemoryTable:
        """
        Get the memory table. If the memory table is None, it will create a new memory table.
        If the memory table is full, it will create a new memory table and dump the old memory table to a data block.
        """
        if self.memory_table is None and self.key_column is not None:
            self.memory_table = MemoryTable(self.key_column)
        assert self.memory_table is not None

        if self.memory_table.should_dump():
            # check if there is an immutable memory table
            if self._immutable_memory_table is not None:
                # wait for the dump thread
                while self._immutable_memory_table.should_dump():
                    console.trace("wait for the immutable memory table dump thread")
                    time.sleep(0.5)

            self._immutable_memory_table = self.memory_table
            assert self.key_column is not None
            self.memory_table = MemoryTable(self.key_column)
            # start a new thread to dump the data
            # no need to join the thread, the thread will be waited when the dump method is called
            threading.Thread(
                target=self._dump,
                args=(self.root_path, self._immutable_memory_table),
                name="immutable_memory_table_dump",
            ).start()

        return self.memory_table

    def insert(self, record: Dict[str, Any]) -> str:
        with self.lock:
            mem = self._must_get_mem_table()
            rv = mem.insert_dict(record, tombstones=self.tombstones)
            if self._key_column_type is None:
                self._key_column_type = mem.key_type
            return rv

    def delete(self, keys: List[Union[int, str]]) -> str:
        with self.lock:
            return self._must_get_mem_table().delete(keys)

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
        if deep_copy is None:
            env = os.getenv("SW_DATASTORE_SCAN_DEEP_COPY")
            # make deep copy to True as default if env is not set
            deep_copy = True if env is None else env.strip().upper() == "TRUE"

        with self.lock:
            mem_tables = [self._must_get_mem_table()]
            if self._immutable_memory_table is not None:
                mem_tables.append(self._immutable_memory_table)

            iters = [
                IterWithRangeHint(
                    iter=mem.scan(start, end, end_inclusive),
                )
                for mem in mem_tables
            ]
            with self._data_block_lock:
                data_blocks = self._load_data_blocks()
                for block in data_blocks:
                    if block.virtual:
                        continue
                    if block.ahead(start):
                        continue
                    if block.behind(end):
                        break
                    iters.append(
                        IterWithRangeHint(
                            iter=block.scan(start, end, end_inclusive),
                            min_key=block.min_key,
                        )
                    )
                tombstones = self._load_manifest().get_tombstones(revision)
                for item in _merge_iters_with_hint(iters):
                    if item is None:
                        continue
                    r = item.get_record(
                        revision=revision, deep_copy=deep_copy, tombstones=tombstones
                    )
                    if r is None:
                        continue
                    if columns is None:
                        d = dict(r)
                    else:
                        d = {columns[k]: v for k, v in r.items() if k in columns}
                        if "-" in r:
                            d["-"] = r["-"]
                    assert (
                        self.key_column is not None
                    )  # load_manifest will set key_column
                    d["*"] = r[self.key_column]
                    if not keep_none:
                        d = {k: v for k, v in d.items() if v is not None}
                    yield d

    def _load_data_blocks(self) -> SortedList[DataBlock]:
        manifest = self._load_manifest()
        ret: SortedList[DataBlock] = SortedList()
        for block in manifest.blocks:
            if block.block_id is None:
                raise RuntimeError("block_id can not be None")
            file = self._get_block_file_name(
                self.root_path,
                manifest.block_config.block_name_prefix,
                block.block_id,
            )
            assert self.key_column is not None  # load_manifest will set key_column
            db = DataBlock(
                min_key=block.min_key,
                max_key=block.max_key,
                key_column=self.key_column,
                block_id=block.block_id,
                file=file,
            )
            ret.add(db)
        return ret

    def _load_manifest(self) -> Manifest:
        manifest_file = self.root_path / datastore_manifest_file_name
        if manifest_file.exists():
            with manifest_file.open() as f:
                manifest = Manifest.parse_raw(f.read())
                if self.key_column is None:
                    self.key_column = manifest.key_column
                if manifest.key_column != self.key_column:
                    raise RuntimeError(
                        f"conflicting key column, expected {self.key_column}, actual {manifest.key_column}"
                    )
                if manifest.key_column_type is not None:
                    self._key_column_type = SwType.decode_schema(
                        manifest.key_column_type
                    )
                return manifest

        if self.create_if_missing:
            return Manifest(
                block_config=DataBlockConfig(),
                blocks=[],
                key_column=self.key_column or "",
                next_block_id=1,
                tombstones=[],
                checkpoints=[],
            )

        raise TableEmptyException(f"can not find table {self.table_name}")

    def _get_block_file_name(self, root_path: Path, prefix: str, block_id: int) -> Path:
        return root_path / f"{prefix}.{block_id:07d}{self.compressor.extension()}"

    def _dump(self, root_path: Path, mem_table: MemoryTable) -> None:
        with self._dump_lock:
            self._dump_mem_table(root_path, mem_table)

    def _dump_mem_table(self, root_path: Path, mem_table: MemoryTable) -> None:
        if len(mem_table.records) == 0:
            return

        root_path.mkdir(parents=True, exist_ok=True)

        # reload the manifest file
        manifest = self._load_manifest()
        if manifest.key_column_type is None and mem_table.key_type is not None:
            key_type_str = SwType.encode_schema(mem_table.key_type)
            manifest.key_column_type = key_type_str

        data_blocks = self._load_data_blocks()
        console.trace(f"load {len(data_blocks)} data blocks")

        key_column = self.key_column
        if key_column is None:
            raise RuntimeError("key_column can not be None")

        blocks_to_rm = []
        # dump the memory table
        dest_blocks: Dict[DataBlock, List[InnerRecord]] = {}
        records = list(mem_table.scan())
        key_range_not_found: List[Tuple[Any, Any]] = []
        last_range_broken = True
        for record in records:
            key = record.key
            block_found = False
            for block in data_blocks:
                if block.block_id is None:
                    continue
                if block.min_key <= key <= block.max_key:
                    dest_blocks.setdefault(block, []).append(record)
                    blocks_to_rm = [block.file]
                    block_found = True
                    last_range_broken = True
            if not block_found:
                if last_range_broken:
                    key_range_not_found.append((key, key))
                else:
                    key_range_not_found[-1] = (
                        key_range_not_found[-1][0],
                        key,
                    )
                last_range_broken = False

        for start, end in key_range_not_found:
            console.trace(f"create new block for key range {start} - {end}")
            block_id = manifest.next_block_id
            file = self._get_block_file_name(
                root_path, manifest.block_config.block_name_prefix, block_id
            )
            block_records = [record for record in records if start <= record.key <= end]
            dest_blocks[
                DataBlock(start, end, key_column, block_id, file)
            ] = block_records

        # update the data block records and check if the block reaches the max size
        for block, records in dest_blocks.items():
            if not block.file.exists():
                mem = MemoryTable(key_column)
            else:
                mem = block.load()
            for record in records:
                mem.insert_record(record)
            total = len(mem.records)
            if total > manifest.block_config.max_block_size:
                # split the block into more blocks
                size = total / (total // manifest.block_config.max_block_size + 1)
            else:
                size = total

            chunks = list(
                zip(
                    range(0, total, int(size)),
                    range(int(size), total + int(size), int(size)),
                )
            )
            # remove the block desc from manifest
            manifest.blocks = [
                b for b in manifest.blocks if b.block_id != block.block_id
            ]

            view = mem.records.values()
            for i, (start, end) in enumerate(chunks):
                if i == len(chunks) - 1:
                    end = total
                items = list(view[start:end])
                block = DataBlock(
                    min_key=items[0].key,
                    max_key=items[-1].key,
                    key_column=key_column,
                    file=self._get_block_file_name(
                        root_path,
                        manifest.block_config.block_name_prefix,
                        manifest.next_block_id,
                    ),
                )
                console.trace(f"dump block {block.file}")
                block.dump(items, self.compressor, str(root_path))
                min_rev, max_rev = revision_range_of_items(items)
                manifest.blocks.append(
                    DataBlockDesc(
                        min_key=block.min_key,
                        max_key=block.max_key,
                        min_revision=min_rev and str(min_rev) or None,
                        max_revision=max_rev and str(max_rev) or None,
                        row_count=len(items),
                        block_id=manifest.next_block_id,
                    )
                )
                manifest.next_block_id += 1

        with self._data_block_lock:
            # regenerate the manifest file and replace the old one
            with tempfile.NamedTemporaryFile(mode="w", delete=False) as tmp:
                tmp.write(manifest.json())
                tmp.flush()
                shutil.move(tmp.name, root_path / datastore_manifest_file_name)

            # remove the old data files
            for f in blocks_to_rm:
                console.trace(f"remove block {f}")
                f.unlink()

        # release the memory
        mem_table.records.clear()

    def dump(self) -> None:
        if self._immutable_memory_table is not None:
            self._dump(self.root_path, self._immutable_memory_table)
            self._immutable_memory_table = None
        if self.memory_table is not None:
            self._dump(self.root_path, self.memory_table)
            self.memory_table = None

    def _dump_manifest(self, manifest: Manifest) -> None:
        with tempfile.NamedTemporaryFile(mode="w", delete=False) as tmp:
            tmp.write(manifest.json())
            tmp.flush()
            shutil.move(tmp.name, self.root_path / datastore_manifest_file_name)

    def delete_by_range(
        self, start: KeyType, end: KeyType, end_inclusive: bool = False
    ) -> str:
        rev = str(InnerRecord.gen_seq_num())
        t = TombstoneDesc(
            start=start,
            end=end,
            end_inclusive=end_inclusive,
            revision=rev,
        )
        with filelock.FileLock(self.root_path / ".lock"):
            manifest = self._load_manifest()
            manifest.tombstones.append(t)
            self._tombstones = manifest.get_tombstones(None)
            self._dump_manifest(manifest)
        return rev

    def list_checkpoints(self) -> List[Checkpoint]:
        manifest = self._load_manifest()
        return [cp.to_checkpoint() for cp in manifest.checkpoints]

    def add_checkpoint(self, revision: str) -> None:
        with filelock.FileLock(self.root_path / ".lock"):
            cp = CheckpointDesc(revision=revision, created_at=int(time.time() * 1000))
            manifest = self._load_manifest()
            # check if checkpoint exists
            for c in manifest.checkpoints:
                if c.revision == revision:
                    return
            # TODO: do not allow to add checkpoint if the revision is between two checkpoints
            # because the revisions between two checkpoints may be garbage collected
            cp.count = self._get_size(revision=revision)
            manifest.checkpoints.append(cp)
            self._dump_manifest(manifest)

    def remove_checkpoint(self, cp: Checkpoint) -> None:
        with filelock.FileLock(self.root_path / ".lock"):
            manifest = self._load_manifest()
            manifest.checkpoints = [
                c for c in manifest.checkpoints if c.revision != cp.revision
            ]
            self._dump_manifest(manifest)

    def _get_size(self, revision: str | None) -> int:
        return sum(
            1 for _ in self.scan(revision=revision, keep_none=True, deep_copy=False)
        )

    def get_size(self, cp: Checkpoint | None) -> int:
        manifest = self._load_manifest()
        if cp is not None:
            # find the checkpoint in manifest
            for c in manifest.checkpoints:
                if c.revision == cp.revision and c.count is not None:
                    return c.count
        return self._get_size(revision=None)

    def _dump_items(
        self,
        items: List[InnerRecord] | SortedValuesView[InnerRecord],
        dest_manifest: Manifest,
    ) -> None:
        assert self.key_column is not None
        block = DataBlock(
            min_key=items[0].key,
            max_key=items[-1].key,
            key_column=self.key_column,
            block_id=dest_manifest.next_block_id,
            file=self._get_block_file_name(
                self.root_path,
                dest_manifest.block_config.block_name_prefix,
                dest_manifest.next_block_id,
            ),
        )
        dest_manifest.next_block_id += 1
        console.trace(f"dump block {block.file}")
        tmp_path = self.root_path / ".tmp"
        tmp_path.mkdir(parents=True, exist_ok=True)
        block.dump(items, self.compressor, str(tmp_path))
        min_rev, max_rev = revision_range_of_items(items)
        dest_manifest.blocks.append(
            DataBlockDesc(
                min_key=block.min_key,
                max_key=block.max_key,
                min_revision=min_rev and str(min_rev) or None,
                max_revision=max_rev and str(max_rev) or None,
                row_count=len(items),
                block_id=block.block_id,
            )
        )

    def gc(self) -> None:
        """
        Garbage collect the revisions and tombstones.
        It does the following things:
        - remove the data blocks that are not referenced by the manifest
        - merge the tombstones that between two checkpoints
        - compact the revisions between two checkpoints
        """

        # we do not support do gc for tables with memory table
        if self.memory_table is not None or self._immutable_memory_table is not None:
            console.error("can not do gc for tables with memory table, dump first")
            return

        manifest = self._load_manifest()
        last_gc_revision = (
            manifest.garbage_collection and manifest.garbage_collection.revision or None
        )
        if last_gc_revision == manifest.last_revision:
            console.trace("no need to do gc")
            return

        revisions = [cp.revision for cp in sorted(manifest.checkpoints)]

        new_manifest = Manifest(
            version=manifest.version,
            block_config=manifest.block_config,
            blocks=[],
            key_column=manifest.key_column,
            key_column_type=manifest.key_column_type,
            next_block_id=manifest.next_block_id,
            last_key=None,
            last_revision="0",
            garbage_collection=GarbageCollectionDesc(revision=manifest.last_revision),
            tombstones=manifest.tombstones,
            checkpoints=manifest.checkpoints,
        )

        with self._data_block_lock:
            data_blocks = self._load_data_blocks()
            iters: List[IterWithRangeHint] = [
                IterWithRangeHint(
                    iter=block.scan(start=None, end=None),
                    min_key=block.min_key,
                )
                for block in data_blocks
            ]

            assert self.key_column is not None
            mem_table = MemoryTable(self.key_column)
            for item in _merge_iters_with_hint(iters):
                if item is None:
                    continue
                item.compact(revisions=revisions)
                mem_table.insert_record(item)
                if item.key is not None:
                    new_manifest.last_key = item.key
                if mem_table.should_dump():
                    items: SortedValuesView[InnerRecord] = mem_table.records.values()  # type: ignore[assignment]
                    self._dump_items(items, new_manifest)
                    mem_table.records.clear()

            if len(mem_table.records) > 0:
                items = mem_table.records.values()  # type: ignore[assignment]
                self._dump_items(items, new_manifest)
                mem_table.records.clear()

            self._dump_manifest(new_manifest)
            # remove the old data files
            for f in data_blocks:
                console.trace(f"remove block {f.file}")
                f.file.unlink()


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


class LocalTableDesc(SwBaseModel):
    name: str
    dir: str
    created_at: int  # timestamp in milliseconds


class LocalDataStoreManifest(SwBaseModel):
    version: str = "0.1"
    tables: List[LocalTableDesc]


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
        self.tables: Dict[str, LocalTable] = {}
        self.lock = threading.Lock()

    def list_tables(
        self,
        prefixes: List[str],
    ) -> List[str]:
        manifest = self._load_manifest()
        return [
            table.name
            for table in manifest.tables
            if any(table.name.startswith(prefix) for prefix in prefixes)
        ]

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
        if schema.key_column != table.key_column:
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

    def _load_manifest(self) -> LocalDataStoreManifest:
        manifest_file = Path(self.root_path) / datastore_manifest_file_name
        if manifest_file.exists():
            with manifest_file.open() as f:
                return LocalDataStoreManifest.parse_raw(f.read())
        return LocalDataStoreManifest(tables=[])

    def _dump_manifest(self, manifest: LocalDataStoreManifest) -> None:
        manifest_file = Path(self.root_path) / datastore_manifest_file_name
        with filelock.FileLock(str(Path(self.root_path) / ".lock")):
            with tempfile.NamedTemporaryFile(mode="w", delete=False) as tmp:
                tmp.write(manifest.json(indent=2))
                tmp.flush()
                shutil.move(tmp.name, manifest_file)

    def _get_table(
        self, table_name: str, key_column: ColumnSchema | None, create: bool = True
    ) -> LocalTable | None:
        with self.lock:
            table = self.tables.get(table_name, None)
            if table is not None:
                return table
            # open or create
            manifest = self._load_manifest()
            table_root: Path | None = None
            for t in manifest.tables:
                if t.name == table_name:
                    table_root = Path(self.root_path) / t.dir
                    break
            existing = table_root is not None
            if table_root is None:  # make mypy happy
                if not create:
                    return None
                uuid = uuid4().hex
                table_root = Path(self.root_path) / uuid[:2] / uuid[2:-1]

            table_root.mkdir(parents=True, exist_ok=True)
            # no try, let it raise
            table = LocalTable(
                table_name=table_name,
                root_path=table_root,
                key_column=key_column and key_column.name or None,
                create_if_missing=create,
            )
            if not existing:
                manifest.tables.append(
                    LocalTableDesc(
                        name=table_name,
                        dir=str(table_root.relative_to(self.root_path)),
                        created_at=int(time.time() * 1000),
                    )
                )
            self._dump_manifest(manifest)
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
            key_column_type = table.key_column_type
            if key_column_type is None:
                raise RuntimeError(
                    f"key column type not found for table {table_desc.table_name}"
                )
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
            table.dump()

    def _must_get_table(self, table_name: str) -> LocalTable:
        table = self._get_table(table_name, None, create=False)
        if table is None:
            raise RuntimeError(f"table {table_name} does not exist")
        return table

    def delete_by_range(
        self, table_name: str, start: KeyType, end: KeyType, end_inclusive: bool = False
    ) -> str:
        return self._must_get_table(table_name).delete_by_range(
            start, end, end_inclusive
        )

    def list_table_checkpoints(self, table_name: str) -> List[Checkpoint]:
        return self._must_get_table(table_name).list_checkpoints()

    def add_checkpoint(self, table_name: str, revision: str) -> None:
        self._must_get_table(table_name).add_checkpoint(revision)

    def remove_checkpoint(self, table_name: str, cp: Checkpoint) -> None:
        self._must_get_table(table_name).remove_checkpoint(cp)

    def get_table_size(self, table_name: str, cp: Checkpoint | None = None) -> int:
        return self._must_get_table(table_name).get_size(cp)


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

        return self._do_request(data, "/api/v1/datastore/updateTable")  # type: ignore

    def list_tables(self, prefixes: List[str]) -> List[str]:
        prefixes = [prefix.strip("/") for prefix in prefixes]
        resp = self._do_request({"prefixes": prefixes}, "/api/v1/datastore/listTables")
        return resp["tables"]  # type: ignore

    @http_retry(
        attempts=10,
        wait=tenacity.wait_fixed(1),
        retry=(
            tenacity.retry_if_exception_type(Exception)
            | retry_if_http_exception(_RETRY_HTTP_STATUS_CODES)
        ),
    )
    def _do_request(self, post_data: Dict[str, Any], path: str) -> Any:
        resp = requests.post(
            urllib.parse.urljoin(self.instance_uri, path),
            data=json.dumps(post_data, separators=(",", ":")),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": self.token,  # type: ignore
            },
            timeout=90,
        )
        if resp.status_code != HTTPStatus.OK:
            console.error(
                f"path:{path} resp code:{resp.status_code}, \n resp text: {resp.text}, \n post_data: {post_data}"
            )
        resp.raise_for_status()
        return resp.json()["data"]

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
            resp_json = self._do_request(post_data, "/api/v1/datastore/scanTable")
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

    def delete_by_range(
        self, table_name: str, start: KeyType, end: KeyType, end_inclusive: bool = False
    ) -> str:
        return ""

    def list_table_checkpoints(self, table_name: str) -> List[Checkpoint]:
        return []

    def add_checkpoint(self, table_name: str, revision: str) -> None:
        ...

    def remove_checkpoint(self, table_name: str, cp: Checkpoint) -> None:
        ...

    def get_table_size(self, table_name: str, cp: Checkpoint | None = None) -> int:
        return 0


class Checkpoint(SwBaseModel):
    revision: str
    created_at: int  # timestamp in milliseconds


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
        """
        Scan tables with the given tables, and return the iterator of the records.
        """
        ...

    def list_tables(
        self,
        prefixes: List[str],
    ) -> List[str]:
        """
        List table names with the given prefixes.
        """
        ...

    def delete_by_range(
        self, table_name: str, start: KeyType, end: KeyType, end_inclusive: bool = False
    ) -> str:
        """
        Delete rows by range.
        - It deletes all rows with key >= start and key < end if end_inclusive is False,
            or key <= end if end_inclusive is True.
        - It will delete all rows if start is None and end is None.

        Return the revision of the table.
        """
        ...

    def list_table_checkpoints(self, table_name: str) -> List[Checkpoint]:
        ...

    def add_checkpoint(self, table_name: str, revision: str) -> None:
        ...

    def remove_checkpoint(self, table_name: str, cp: Checkpoint) -> None:
        ...

    def get_table_size(self, table_name: str, cp: Checkpoint | None = None) -> int:
        """
        Get the size of the table.
        Provide a checkpoint to get the size of the table at the checkpoint.
        """
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
        try:
            if isinstance(self.data_store, LocalDataStore):
                self.data_store.dump()
        finally:
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
                    raise RuntimeError(f"invalid field {k!r}")
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
        # block until the records are flushed
        while True:
            if len(self._records) < datastore_max_dirty_records * 2:
                break
            time.sleep(0.1)

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
                            console.trace(f"schema changed, {last_schema} -> {schema}")
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
