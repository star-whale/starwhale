import os
import json
import time
import unittest
import concurrent.futures
from typing import Dict, List
from unittest.mock import Mock, patch

import numpy as np
import pyarrow as pa  # type: ignore
import requests
from requests_mock import Mocker

from starwhale import Text
from starwhale.consts import HTTPMethod
from starwhale.api._impl import data_store
from starwhale.api._impl.data_store import (
    SwType,
    TableSchema,
    TableEmptyException,
    TableWriterException,
)

from .test_base import BaseTestCase


class TestBasicFunctions(BaseTestCase):
    def test_get_table_path(self) -> None:
        self.assertEqual(os.path.join("a", "b"), data_store._get_table_path("a", "b"))
        self.assertEqual(
            os.path.join("a", "b", "c"), data_store._get_table_path("a", "b/c")
        )
        self.assertEqual(
            os.path.join("a", "b", "c", "d"), data_store._get_table_path("a", "b/c/d")
        )

    def test_parse_parquet_name(self) -> None:
        self.assertEqual(
            ("", 0),
            data_store._parse_parquet_name("base-123.txt"),
            "invalid extension",
        )
        self.assertEqual(
            ("", 0),
            data_store._parse_parquet_name("base_1.parquet"),
            "invalid prefix",
        )
        self.assertEqual(
            ("", 0),
            data_store._parse_parquet_name("base-i.parquet"),
            "invalid index",
        )
        self.assertEqual(
            ("base", 123), data_store._parse_parquet_name("base-123.parquet"), "base"
        )
        self.assertEqual(
            ("patch", 123), data_store._parse_parquet_name("patch-123.parquet"), "patch"
        )

    def test_write_and_scan(self) -> None:
        path = os.path.join(self.datastore_root, "base-0.parquet")
        data_store._write_parquet_file(
            path,
            pa.Table.from_pydict(
                {
                    "a": [0, 1, 2],
                    "b": ["x", "y", "z"],
                    "c": [10, 11, 12],
                    "-": [None, True, None],
                    "~c": [False, False, True],
                },
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "a",
                            [
                                data_store.ColumnSchema("a", data_store.INT64),
                                data_store.ColumnSchema("b", data_store.STRING),
                                data_store.ColumnSchema("c", data_store.INT64),
                                data_store.ColumnSchema("-", data_store.BOOL),
                                data_store.ColumnSchema("~c", data_store.BOOL),
                            ],
                        )
                    )
                },
            ),
        )
        self.assertEqual(
            [
                {"*": 0, "a": 0, "b": "x", "c": 10},
                {"*": 1, "a": 1, "b": "y", "c": 11, "-": True},
                {"*": 2, "a": 2, "b": "z"},
            ],
            list(data_store._scan_parquet_file(path)),
            "scan all",
        )
        self.assertEqual(
            [
                {"*": 0, "i": "x", "j": 10},
                {"*": 1, "i": "y", "j": 11, "-": True},
                {"*": 2, "i": "z"},
            ],
            list(data_store._scan_parquet_file(path, columns={"b": "i", "c": "j"})),
            "some columns",
        )
        self.assertEqual(
            [
                {"*": 0, "i": "x", "j": 10},
                {"*": 1, "i": "y", "j": 11, "-": True},
                {"*": 2, "i": "z"},
            ],
            list(
                data_store._scan_parquet_file(
                    path, columns={"b": "i", "c": "j", "x": "x"}
                )
            ),
            "extra column",
        )
        self.assertEqual(
            [
                {"*": 0, "i": "x", "j": 10},
                {"*": 1, "i": "y", "j": 11, "-": True},
                {"*": 2, "i": "z"},
            ],
            list(
                data_store._scan_parquet_file(
                    path, columns={"b": "i", "c": "j", "-": "x"}
                )
            ),
            "'-' column",
        )
        self.assertEqual(
            [{"*": 1, "i": "y", "j": 11, "-": True}, {"*": 2, "i": "z"}],
            list(
                data_store._scan_parquet_file(
                    path, columns={"b": "i", "c": "j"}, start=1
                )
            ),
            "with start",
        )
        self.assertEqual(
            [{"*": 0, "i": "x", "j": 10}],
            list(
                data_store._scan_parquet_file(path, columns={"b": "i", "c": "j"}, end=1)
            ),
            "with end",
        )
        self.assertEqual(
            [{"*": 1, "i": "y", "j": 11, "-": True}],
            list(
                data_store._scan_parquet_file(
                    path, columns={"b": "i", "c": "j"}, start=1, end=2
                )
            ),
            "with start and end",
        )

        self.assertEqual(
            [{"*": 1, "-": True, "i": "y", "j": 11}, {"*": 2, "i": "z"}],
            list(
                data_store._scan_parquet_file(
                    path,
                    columns={"b": "i", "c": "j"},
                    start=1,
                    end=2,
                    end_inclusive=True,
                )
            ),
            "with start and end, with end inclusive",
        )
        self.assertEqual(
            [
                {"*": 0, "a": 0, "b": "x", "c": 10},
                {"*": 1, "a": 1, "b": "y", "c": 11, "-": True},
                {"*": 2, "a": 2, "b": "z", "c": None},
            ],
            list(data_store._scan_parquet_file(path, keep_none=True)),
            "keep none",
        )

    def test_only_one_end_inclusive(self) -> None:
        path = os.path.join(self.datastore_root, "base-10.parquet")
        data_store._write_parquet_file(
            path,
            pa.Table.from_pydict(
                {
                    "a": [0],
                    "b": ["x"],
                    "c": [10],
                    "-": [None],
                    "~c": [False],
                },
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "a",
                            [
                                data_store.ColumnSchema("a", data_store.INT64),
                                data_store.ColumnSchema("b", data_store.STRING),
                                data_store.ColumnSchema("c", data_store.INT64),
                                data_store.ColumnSchema("-", data_store.BOOL),
                                data_store.ColumnSchema("~c", data_store.BOOL),
                            ],
                        )
                    )
                },
            ),
        )
        self.assertEqual(
            1,
            len(
                list(
                    data_store._scan_parquet_file(
                        path,
                        start=0,
                        end=0,
                        end_inclusive=True,
                    )
                )
            ),
            "end inclusive and end is max",
        )

    def test_merge_scan(self) -> None:
        self.assertEqual([], list(data_store._merge_scan([], False)), "no iter")
        self.assertEqual(
            [{"*": 0, "a": 0}, {"*": 1, "a": 1}],
            list(
                data_store._merge_scan(
                    [iter([{"*": 0, "a": 0}, {"*": 1, "a": 1}])], False
                )
            ),
            "one iter - ignore none",
        )
        self.assertEqual(
            [{"*": 0, "a": 0}, {"*": 1, "a": 1}, {"*": 2, "a": 2}, {"*": 3, "a": 3}],
            list(
                data_store._merge_scan(
                    [
                        iter([{"*": 0, "a": 0}, {"*": 2, "a": 2}]),
                        iter([{"*": 1, "a": 1}, {"*": 3, "a": 3}]),
                    ],
                    False,
                )
            ),
            "two iters",
        )
        self.assertEqual(
            [{"*": 0, "a": 0}, {"*": 1, "a": 1}, {"*": 2, "a": 2}, {"*": 3, "a": 3}],
            list(
                data_store._merge_scan(
                    [
                        iter([{"*": 0, "a": 0}, {"*": 1, "a": 1}]),
                        iter([{"*": 2, "a": 2}, {"*": 3, "a": 3}]),
                    ],
                    False,
                )
            ),
            "two iters without range overlap",
        )
        self.assertEqual(
            [{"*": 0, "a": 0}, {"*": 1, "a": 1}, {"*": 2, "a": 2}, {"*": 3, "a": 3}],
            list(
                data_store._merge_scan(
                    [
                        iter([]),
                        iter(
                            [
                                {"*": 0, "a": 0},
                                {"*": 1, "a": 1},
                                {"*": 2, "a": 2},
                                {"*": 3, "a": 3},
                            ]
                        ),
                    ],
                    False,
                )
            ),
            "0 and 4",
        )
        self.assertEqual(
            [{"*": 0, "a": 0}, {"*": 1, "a": 1}, {"*": 2, "a": 2}, {"*": 3, "a": 3}],
            list(
                data_store._merge_scan(
                    [
                        iter([{"*": 1, "a": 1}]),
                        iter([{"*": 0, "a": 0}, {"*": 3, "a": 3}]),
                        iter([{"*": 2, "a": 2}]),
                    ],
                    False,
                )
            ),
            "1 and 3",
        )
        self.assertEqual(
            [{"*": 1, "b": 2, "c": 3}],
            list(
                data_store._merge_scan(
                    [
                        iter([{"*": 1, "a": 1}]),
                        iter([{"*": 1, "-": True}, {"*": 1, "b": 2}]),
                        iter([{"*": 1, "c": 3, "-": False}]),
                    ],
                    False,
                )
            ),
            "removal",
        )
        self.assertEqual(
            [
                {"*": 0, "a": "0"},
                {"*": 1, "a": "1"},
                {"*": 3},
            ],
            list(
                data_store._merge_scan(
                    [
                        iter(
                            [
                                {"*": 1, "a": "1"},
                                {"*": 3, "a": "3"},
                            ]
                        ),
                        iter(
                            [
                                {"*": 0, "a": "0"},
                                {"*": 3, "a": None},
                            ]
                        ),
                    ],
                    False,
                )
            ),
            "keep none 1",
        )
        self.assertEqual(
            [
                {"*": 0, "a": "0"},
                {"*": 1, "a": "1"},
                {"*": 3, "a": None},
            ],
            list(
                data_store._merge_scan(
                    [
                        iter(
                            [
                                {"*": 1, "a": "1"},
                                {"*": 3, "a": "3"},
                            ]
                        ),
                        iter(
                            [
                                {"*": 0, "a": "0"},
                                {"*": 3, "a": None},
                            ]
                        ),
                    ],
                    True,
                )
            ),
            "keep none 2",
        )
        self.assertEqual(
            [
                {"*": 0, "a": "0", "b": "0"},
                {"*": 1, "a": "1", "b": "1"},
                {"*": 2, "a": "1", "b": "2"},
                {"*": 3, "a": "3", "b": "3"},
                {"*": 5, "a": "5.5"},
            ],
            list(
                data_store._merge_scan(
                    [
                        iter(
                            [
                                {"*": 1, "a": "1"},
                                {"*": 3, "a": "3"},
                                {"*": 4, "a": "4"},
                                {"*": 5, "a": "5"},
                            ]
                        ),
                        iter(
                            [
                                {"*": 0, "a": "0"},
                                {"*": 2, "a": "2"},
                                {"*": 3, "a": "3.3"},
                                {"*": 5, "a": "5.5"},
                            ]
                        ),
                        iter(
                            [
                                {"*": 2, "-": True},
                                {"*": 2, "a": "1"},
                                {"*": 4, "-": True},
                            ]
                        ),
                        iter(
                            [
                                {"*": 0, "b": "0"},
                                {"*": 1, "b": "1", "-": False},
                                {"*": 2, "b": "2"},
                                {"*": 3, "a": "3", "b": "3"},
                            ]
                        ),
                    ],
                    True,
                )
            ),
            "mixed",
        )

    def test_get_table_files(self) -> None:
        data: Dict[str, List[str]] = {
            "0": [],
            "1": ["base-1.parquet"],
            "2": ["base-0.parquet", "patch-1.parquet", "patch-3.parquet"],
            "3": [
                "base-0.parquet",
                "patch-1.parquet",
                "base-1.parquet",
                "patch-2.parquet",
            ],
        }
        for dir, files in data.items():
            dir = os.path.join(self.datastore_root, dir)
            os.makedirs(dir)
            for file in files:
                file = os.path.join(dir, file)
                with open(file, "w"):
                    pass
        self.assertEqual(
            [],
            data_store._get_table_files(os.path.join(self.datastore_root, "0")),
            "empty",
        )
        self.assertEqual(
            [os.path.join(self.datastore_root, "1", "base-1.parquet")],
            data_store._get_table_files(os.path.join(self.datastore_root, "1")),
            "base only",
        )
        self.assertEqual(
            [
                os.path.join(self.datastore_root, "2", f)
                for f in ("base-0.parquet", "patch-1.parquet", "patch-3.parquet")
            ],
            data_store._get_table_files(os.path.join(self.datastore_root, "2")),
            "base and patches",
        )
        self.assertEqual(
            [
                os.path.join(self.datastore_root, "3", f)
                for f in ("base-1.parquet", "patch-2.parquet")
            ],
            data_store._get_table_files(os.path.join(self.datastore_root, "3")),
            "multiple bases",
        )

    def test_scan_table(self) -> None:
        data_store._write_parquet_file(
            os.path.join(self.datastore_root, "base-0.parquet"),
            pa.Table.from_pydict(
                {"a": [0, 1, 2], "t": [7, 7, 7]},
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "a",
                            [
                                data_store.ColumnSchema("a", data_store.INT64),
                                data_store.ColumnSchema("t", data_store.INT64),
                            ],
                        )
                    )
                },
            ),
        )
        data_store._write_parquet_file(
            os.path.join(self.datastore_root, "patch-1.parquet"),
            pa.Table.from_pydict(
                {
                    "a": [0, 1, 2],
                    "b": ["x", "y", "z"],
                    "c": [10, 11, 12],
                    "d": [None, True, None],
                },
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "a",
                            [
                                data_store.ColumnSchema("a", data_store.INT64),
                                data_store.ColumnSchema("b", data_store.STRING),
                                data_store.ColumnSchema("c", data_store.INT64),
                                data_store.ColumnSchema("d", data_store.BOOL),
                            ],
                        )
                    )
                },
            ),
        )
        data_store._write_parquet_file(
            os.path.join(self.datastore_root, "base-1.parquet"),
            pa.Table.from_pydict(
                {"k": [1, 3, 4, 5], "a": ["1", "3", "4", "5"]},
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "k",
                            [
                                data_store.ColumnSchema("k", data_store.INT64),
                                data_store.ColumnSchema("a", data_store.STRING),
                            ],
                        )
                    )
                },
            ),
        )
        data_store._write_parquet_file(
            os.path.join(self.datastore_root, "patch-2.parquet"),
            pa.Table.from_pydict(
                {"k": [0, 2, 3, 5], "a": ["0", "2", "3.3", "5.5"]},
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "k",
                            [
                                data_store.ColumnSchema("k", data_store.INT64),
                                data_store.ColumnSchema("a", data_store.STRING),
                            ],
                        )
                    )
                },
            ),
        )
        data_store._write_parquet_file(
            os.path.join(self.datastore_root, "patch-3.parquet"),
            pa.Table.from_pydict(
                {"k": [2, 4], "-": [True, True]},
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "k",
                            [
                                data_store.ColumnSchema("k", data_store.INT64),
                                data_store.ColumnSchema("-", data_store.BOOL),
                            ],
                        )
                    )
                },
            ),
        )
        data_store._write_parquet_file(
            os.path.join(self.datastore_root, "patch-4.parquet"),
            pa.Table.from_pydict(
                {
                    "k": [0, 1, 2, 3],
                    "a": [None, None, None, "3"],
                    "b": ["0", "1", "2", "3"],
                    "~b": [False, False, False, True],
                },
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "k",
                            [
                                data_store.ColumnSchema("k", data_store.INT64),
                                data_store.ColumnSchema("a", data_store.STRING),
                                data_store.ColumnSchema("b", data_store.STRING),
                                data_store.ColumnSchema("~b", data_store.BOOL),
                            ],
                        )
                    )
                },
            ),
        )
        self.assertEqual(
            [],
            list(data_store._scan_table(os.path.join(self.datastore_root, "no"))),
            "empty",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0", "b": "0"},
                {"*": 1, "k": 1, "a": "1", "b": "1"},
                {"*": 2, "k": 2, "b": "2"},
                {"*": 3, "k": 3, "a": "3"},
                {"*": 5, "k": 5, "a": "5.5"},
            ],
            list(data_store._scan_table(self.datastore_root)),
            "scan all",
        )
        self.assertEqual(
            [
                {"*": 0, "i": "0", "j": "0"},
                {"*": 1, "i": "1", "j": "1"},
                {"*": 2, "j": "2"},
                {"*": 3, "i": "3"},
                {"*": 5, "i": "5.5"},
            ],
            list(data_store._scan_table(self.datastore_root, {"a": "i", "b": "j"})),
            "some columns",
        )
        self.assertEqual(
            [{"*": 2, "j": "2"}, {"*": 3, "i": "3"}],
            list(
                data_store._scan_table(
                    self.datastore_root, {"a": "i", "b": "j"}, start=2, end=5
                )
            ),
            "with start and end",
        )
        self.assertEqual(
            [{"*": 2, "j": "2"}, {"*": 3, "i": "3"}],
            list(
                data_store._scan_table(
                    self.datastore_root,
                    {"a": "i", "b": "j"},
                    start=2,
                    end=3,
                    end_inclusive=True,
                )
            ),
            "with start and end(inclusive)",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0", "b": "0"},
                {"*": 1, "k": 1, "a": "1", "b": "1"},
                {"*": 2, "k": 2, "b": "2"},
                {"*": 3, "k": 3, "a": "3", "b": None},
                {"*": 5, "k": 5, "a": "5.5"},
            ],
            list(data_store._scan_table(self.datastore_root, keep_none=True)),
            "keep none",
        )

    def test_get_type(self) -> None:
        self.assertEqual(data_store.UNKNOWN, data_store._get_type(None), "unknown")
        self.assertEqual(data_store.BOOL, data_store._get_type(False), "bool")
        self.assertEqual(data_store.INT8, data_store._get_type(np.int8(0)), "int8")
        self.assertEqual(data_store.INT16, data_store._get_type(np.int16(0)), "int16")
        self.assertEqual(data_store.INT32, data_store._get_type(np.int32(0)), "int32")
        self.assertEqual(data_store.INT64, data_store._get_type(0), "int64")
        self.assertEqual(
            data_store.FLOAT16, data_store._get_type(np.float16(0)), "float16"
        )
        self.assertEqual(
            data_store.FLOAT32, data_store._get_type(np.float32(0)), "float32"
        )
        self.assertEqual(data_store.FLOAT64, data_store._get_type(0.0), "float64")
        self.assertEqual(data_store.STRING, data_store._get_type(""), "string")
        self.assertEqual(data_store.BYTES, data_store._get_type(b""), "bytes")
        self.assertEqual(
            data_store.SwListType(data_store.UNKNOWN),
            data_store._get_type([]),
            "[unknown] 1",
        )
        self.assertEqual(
            data_store.SwListType(data_store.UNKNOWN),
            data_store._get_type([None]),
            "[unknown] 2",
        )
        self.assertEqual(
            data_store.SwListType(data_store.INT64),
            data_store._get_type([0]),
            "[int64] 1",
        )
        self.assertEqual(
            data_store.SwListType(data_store.INT64),
            data_store._get_type([0, None]),
            "[int64] 2",
        )
        self.assertEqual(
            data_store.SwListType(data_store.INT64),
            data_store._get_type([None, 0]),
            "[int64] 3",
        )
        self.assertEqual(
            data_store.SwTupleType(data_store.UNKNOWN),
            data_store._get_type(()),
            "(unknown) 1",
        )
        self.assertEqual(
            data_store.SwTupleType(data_store.UNKNOWN),
            data_store._get_type((None,)),
            "(unknown) 2",
        )
        self.assertEqual(
            data_store.SwTupleType(data_store.INT64),
            data_store._get_type((0,)),
            "(int64) 1",
        )
        self.assertEqual(
            data_store.SwTupleType(data_store.INT64),
            data_store._get_type((0, None)),
            "(int64) 2",
        )
        self.assertEqual(
            data_store.SwTupleType(data_store.INT64),
            data_store._get_type((None, 0)),
            "(int64) 3",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN),
            data_store._get_type({}),
            "{unknown:unknown}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.INT64, data_store.UNKNOWN),
            data_store._get_type({0: None}),
            "{int64:unknown}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.INT64, data_store.INT64),
            data_store._get_type({0: 1}),
            "{int64:int64} 1",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.INT64, data_store.INT64),
            data_store._get_type({0: None, 1: 2}),
            "{int64:int64} 2",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.INT64, data_store.INT64),
            data_store._get_type({0: 1, 1: None}),
            "{int64:int64} 3",
        )
        self.assertEqual(
            data_store.SwObjectType(
                data_store.Link,
                {
                    "uri": data_store.UNKNOWN,
                    "display_text": data_store.UNKNOWN,
                    "mime_type": data_store.UNKNOWN,
                },
            ),
            data_store._get_type(data_store.Link()),
            "{} 1",
        )
        self.assertEqual(
            data_store.SwObjectType(
                data_store.Link,
                {
                    "uri": data_store.STRING,
                    "display_text": data_store.STRING,
                    "mime_type": data_store.STRING,
                },
            ),
            data_store._get_type(data_store.Link("1", "2", "3")),
            "{} 2",
        )
        self.assertEqual(
            data_store.SwListType(
                data_store.SwObjectType(
                    data_store.Link,
                    {
                        "uri": data_store.STRING,
                        "display_text": data_store.STRING,
                        "mime_type": data_store.STRING,
                    },
                )
            ),
            data_store._get_type([data_store.Link("1", "2", "3")]),
            "[{}]",
        )
        self.assertEqual(
            data_store.SwTupleType(
                data_store.SwObjectType(
                    data_store.Link,
                    {
                        "uri": data_store.STRING,
                        "display_text": data_store.STRING,
                        "mime_type": data_store.STRING,
                    },
                )
            ),
            data_store._get_type((data_store.Link("1", "2", "3"),)),
            "({})",
        )
        self.assertEqual(
            data_store.SwMapType(
                data_store.STRING,
                data_store.SwObjectType(
                    data_store.Link,
                    {
                        "uri": data_store.STRING,
                        "display_text": data_store.STRING,
                        "mime_type": data_store.STRING,
                    },
                ),
            ),
            data_store._get_type({"t": data_store.Link("1", "2", "3")}),
            "{" ":{}}",
        )

    def test_type_merge(self) -> None:
        self.assertEqual(
            data_store.INT32,
            data_store.UNKNOWN.merge(data_store.INT32),
            "unknown and int",
        )
        self.assertEqual(
            data_store.INT32,
            data_store.INT32.merge(data_store.UNKNOWN),
            "int and unknown",
        )
        self.assertEqual(
            data_store.INT64,
            data_store.INT64.merge(data_store.INT64),
            "int and unknown",
        )
        with self.assertRaises(RuntimeError, msg="scalar conflict"):
            data_store.INT32.merge(data_store.INT64)
        self.assertEqual(
            data_store.SwListType(data_store.UNKNOWN),
            data_store.SwListType(data_store.UNKNOWN).merge(
                data_store.SwListType(data_store.UNKNOWN)
            ),
            "[unknown] and [unknown]",
        )
        self.assertEqual(
            data_store.SwListType(data_store.INT64),
            data_store.SwListType(data_store.UNKNOWN).merge(
                data_store.SwListType(data_store.INT64)
            ),
            "[unknown] and [int64]",
        )
        self.assertEqual(
            data_store.SwListType(data_store.INT64),
            data_store.SwListType(data_store.INT64).merge(
                data_store.SwListType(data_store.UNKNOWN)
            ),
            "[int64] and [unknown]",
        )
        with self.assertRaises(RuntimeError, msg="list conflict"):
            data_store.SwListType(data_store.INT32).merge(
                data_store.SwListType(data_store.INT64)
            )
        with self.assertRaises(RuntimeError, msg="list and scalar"):
            data_store.SwListType(data_store.INT64).merge(data_store.INT64)
        with self.assertRaises(RuntimeError, msg="scalar and list"):
            data_store.INT64.merge(data_store.SwListType(data_store.INT64))
        self.assertEqual(
            data_store.SwTupleType(data_store.UNKNOWN),
            data_store.SwTupleType(data_store.UNKNOWN).merge(
                data_store.SwTupleType(data_store.UNKNOWN)
            ),
            "(unknown) and (unknown)",
        )
        self.assertEqual(
            data_store.SwTupleType(data_store.INT64),
            data_store.SwTupleType(data_store.UNKNOWN).merge(
                data_store.SwTupleType(data_store.INT64)
            ),
            "(unknown) and (int64)",
        )
        self.assertEqual(
            data_store.SwTupleType(data_store.INT64),
            data_store.SwTupleType(data_store.INT64).merge(
                data_store.SwTupleType(data_store.UNKNOWN)
            ),
            "(int64) and (unknown)",
        )
        with self.assertRaises(RuntimeError, msg="list conflict"):
            data_store.SwTupleType(data_store.INT32).merge(
                data_store.SwTupleType(data_store.INT64)
            )
        with self.assertRaises(RuntimeError, msg="tuple and scalar"):
            data_store.SwTupleType(data_store.INT64).merge(data_store.INT64)
        with self.assertRaises(RuntimeError, msg="scalar and tuple"):
            data_store.INT64.merge(data_store.SwTupleType(data_store.INT64))
        self.assertEqual(
            data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN),
            data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN).merge(
                data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN)
            ),
            "{unknown:unknown} and {unknown:unknown}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.UNKNOWN, data_store.INT64),
            data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN).merge(
                data_store.SwMapType(data_store.UNKNOWN, data_store.INT64)
            ),
            "{unknown:unknown} and {unknown:int64}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.UNKNOWN, data_store.INT64),
            data_store.SwMapType(data_store.UNKNOWN, data_store.INT64).merge(
                data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN)
            ),
            "{unknown:int64} and {unknown:unknown}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.UNKNOWN, data_store.INT64),
            data_store.SwMapType(data_store.UNKNOWN, data_store.INT64).merge(
                data_store.SwMapType(data_store.UNKNOWN, data_store.INT64)
            ),
            "{unknown:int64} and {unknown:int64}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.INT64, data_store.UNKNOWN),
            data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN).merge(
                data_store.SwMapType(data_store.INT64, data_store.UNKNOWN)
            ),
            "{unknown:unknown} and {int64:unknown}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.INT64, data_store.UNKNOWN),
            data_store.SwMapType(data_store.INT64, data_store.UNKNOWN).merge(
                data_store.SwMapType(data_store.UNKNOWN, data_store.UNKNOWN)
            ),
            "{int64:unknown} and {unknown:unknown}",
        )
        self.assertEqual(
            data_store.SwMapType(data_store.INT64, data_store.UNKNOWN),
            data_store.SwMapType(data_store.INT64, data_store.UNKNOWN).merge(
                data_store.SwMapType(data_store.INT64, data_store.UNKNOWN)
            ),
            "{int64:unknown} and {int64:unknown}",
        )
        with self.assertRaises(RuntimeError, msg="map and scalar"):
            data_store.SwMapType(data_store.INT64, data_store.INT64).merge(
                data_store.INT64
            )
        with self.assertRaises(RuntimeError, msg="scalar and map"):
            data_store.INT64.merge(
                data_store.SwMapType(data_store.INT64, data_store.INT64)
            )
        self.assertEqual(
            data_store.SwObjectType(
                data_store.Link, {"a": data_store.STRING, "b": data_store.INT64}
            ),
            data_store.SwObjectType(data_store.Link, {"a": data_store.STRING}).merge(
                data_store.SwObjectType(data_store.Link, {"b": data_store.INT64})
            ),
            "{}",
        )
        self.assertEqual(
            data_store.SwObjectType(
                data_store.Link, {"a": data_store.STRING, "b": data_store.INT64}
            ),
            data_store.SwObjectType(
                data_store.Link, {"a": data_store.STRING, "b": data_store.INT64}
            ).merge(data_store.UNKNOWN),
            "{}",
        )

    def test_update_schema(self) -> None:
        self.assertEqual(
            data_store.TableSchema(
                "a", [data_store.ColumnSchema("a", data_store.INT64)]
            ),
            data_store._update_schema(data_store.TableSchema("a", []), {"a": 1}),
            "new field 1",
        )
        self.assertEqual(
            data_store.TableSchema(
                "a",
                [
                    data_store.ColumnSchema("a", data_store.INT64),
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            data_store._update_schema(
                data_store.TableSchema(
                    "a", [data_store.ColumnSchema("a", data_store.INT64)]
                ),
                {"b": ""},
            ),
            "new field 2",
        )
        with self.assertRaises(RuntimeError, msg="conflict"):
            data_store._update_schema(
                data_store.TableSchema(
                    "a",
                    [
                        data_store.ColumnSchema("a", data_store.INT64),
                        data_store.ColumnSchema("b", data_store.STRING),
                    ],
                ),
                {"b": 0},
            )
        self.assertEqual(
            data_store.TableSchema(
                "a", [data_store.ColumnSchema("a", data_store.UNKNOWN)]
            ),
            data_store._update_schema(data_store.TableSchema("a", []), {"a": None}),
            "none 1",
        )
        self.assertEqual(
            data_store.TableSchema(
                "a", [data_store.ColumnSchema("a", data_store.UNKNOWN)]
            ),
            data_store._update_schema(
                data_store.TableSchema(
                    "a", [data_store.ColumnSchema("a", data_store.UNKNOWN)]
                ),
                {"a": None},
            ),
            "none 2",
        )
        self.assertEqual(
            data_store.TableSchema(
                "a", [data_store.ColumnSchema("a", data_store.INT64)]
            ),
            data_store._update_schema(
                data_store.TableSchema(
                    "a", [data_store.ColumnSchema("a", data_store.UNKNOWN)]
                ),
                {"a": 0},
            ),
            "none 3",
        )
        self.assertEqual(
            data_store.TableSchema(
                "a", [data_store.ColumnSchema("a", data_store.INT64)]
            ),
            data_store._update_schema(
                data_store.TableSchema(
                    "a", [data_store.ColumnSchema("a", data_store.INT64)]
                ),
                {"a": None},
            ),
            "none 4",
        )


class TestMemoryTable(BaseTestCase):
    def test_mixed(self) -> None:
        table = data_store.MemoryTable(
            "test",
            data_store.TableSchema(
                "k", [data_store.ColumnSchema("k", data_store.INT64)]
            ),
        )
        table.insert({"k": 0, "a": "0"})
        table.insert({"k": 1, "a": "1"})
        table.insert({"k": 2, "a": "2"})
        table.insert({"k": 3, "a": "3"})
        table.insert({"k": 1, "b": "1"})
        table.delete([2])
        table.insert({"k": 1, "a": None})
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0"},
                {"*": 1, "k": 1, "b": "1"},
                {"*": 2, "k": 2, "-": True},
                {"*": 3, "k": 3, "a": "3"},
            ],
            list(table.scan()),
            "scan all",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0"},
                {"*": 1, "k": 1, "a": None, "b": "1"},
                {"*": 2, "k": 2, "-": True},
                {"*": 3, "k": 3, "a": "3"},
            ],
            list(table.scan(keep_none=True)),
            "keep none",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0"},
            ],
            list(table.scan(start=0, end=0, end_inclusive=True)),
            "one row",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "x": "0"},
                {"*": 1, "k": 1},
                {"*": 2, "k": 2, "-": True},
                {"*": 3, "k": 3, "x": "3"},
            ],
            list(table.scan({"k": "k", "a": "x"})),
            "some columns",
        )

    def test_write_with_object(self) -> None:
        table = data_store.MemoryTable(
            "test",
            data_store.TableSchema(
                "k", [data_store.ColumnSchema("k", data_store.INT64)]
            ),
        )
        table.insert({"k": 0, "data/text": Text("my_text")})
        self.assertEqual(
            [{"k": 0, "data/text": Text("my_text"), "*": 0}],
            list(table.scan()),
            "get",
        )

        column_schemas = []
        for col in table.get_schema().columns.values():
            d = SwType.encode_schema(col.type)
            d["name"] = col.name
            column_schemas.append(d)

        # TODO wait to resolve UNKNOWN type
        self.assertEqual(
            column_schemas,
            [
                {"type": "INT64", "name": "k"},
                {
                    "type": "OBJECT",
                    "attributes": [
                        {"type": "STRING", "name": "_content"},
                        {"type": "BYTES", "name": "fp"},
                        {"type": "BYTES", "name": "_BaseArtifact__cache_bytes"},
                        {"type": "STRING", "name": "_type"},
                        {"type": "STRING", "name": "display_name"},
                        {"type": "STRING", "name": "_mime_type"},
                        {
                            "type": "TUPLE",
                            "elementType": {"type": "UNKNOWN"},
                            "name": "shape",
                        },
                        {"type": "STRING", "name": "_dtype_name"},
                        {"type": "STRING", "name": "encoding"},
                        {"type": "UNKNOWN", "name": "link"},
                        {"type": "UNKNOWN", "name": "owner"},
                    ],
                    "pythonType": "starwhale.core.dataset.type.Text",
                    "name": "data/text",
                },
            ],
        )


class TestLocalDataStore(BaseTestCase):
    def test_data_store_update_table(self) -> None:
        ds = data_store.LocalDataStore(self.datastore_root)
        with self.assertRaises(RuntimeError, msg="invalid column name"):
            ds.update_table(
                "test",
                data_store.TableSchema(
                    "+", [data_store.ColumnSchema("+", data_store.INT64)]
                ),
                [{"+": 0}],
            )
        with self.assertRaises(RuntimeError, msg="no key field"):
            ds.update_table(
                "test",
                data_store.TableSchema(
                    "k", [data_store.ColumnSchema("k", data_store.INT64)]
                ),
                [{"a": 0}],
            )
        ds.update_table(
            "project/a_b/eval/test-0",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("a", data_store.STRING),
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            [{"k": 0, "a": "0", "b": "0"}, {"k": 1, "a": "1"}],
        )
        self.assertEqual(
            [{"k": 0, "a": "0", "b": "0"}, {"k": 1, "a": "1"}],
            list(
                ds.scan_tables(
                    [data_store.TableDesc("project/a_b/eval/test-0", None, False)]
                )
            ),
            "name check",
        )
        ds.update_table(
            "test",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("a", data_store.STRING),
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            [{"k": 0, "a": "0", "b": "0"}, {"k": 1, "a": "1"}],
        )
        self.assertEqual(
            [{"k": 0, "a": "0", "b": "0"}, {"k": 1, "a": "1"}],
            list(ds.scan_tables([data_store.TableDesc("test", None, False)])),
            "base",
        )
        ds.update_table(
            "test",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("a", data_store.STRING),
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            [
                {"k": 1, "-": True},
                {"k": 2, "a": None, "b": "2"},
                {"k": 3, "-": True},
                {"k": 3, "a": "3", "b": "3"},
            ],
        )
        self.assertEqual(
            [
                {"k": 0, "a": "0", "b": "0"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
            list(ds.scan_tables([data_store.TableDesc("test", None, False)])),
            "batch+patch",
        )
        self.assertEqual(
            [
                {"k": 0, "a": "0", "b": "0"},
                {"k": 2, "a": None, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
            list(
                ds.scan_tables(
                    [data_store.TableDesc("test", None, True)], keep_none=True
                )
            ),
            "batch+patch keep none",
        )
        ds.update_table(
            "test",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("a", data_store.STRING),
                    data_store.ColumnSchema("b", data_store.STRING),
                    data_store.ColumnSchema("c", data_store.INT64),
                ],
            ),
            [
                {"k": 0, "-": True},
                {"k": 1, "a": "1", "b": "1"},
                {"k": 3, "a": "33", "c": 3},
            ],
        )
        self.assertEqual(
            [
                {"k": 1, "a": "1", "b": "1"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "33", "b": "3", "c": 3},
            ],
            list(ds.scan_tables([data_store.TableDesc("test", None, False)])),
            "overwrite",
        )
        ds.update_table(
            "test",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema(
                        "x", data_store.SwListType(data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "xx", data_store.SwTupleType(data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "xxx", data_store.SwMapType(data_store.STRING, data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "y", data_store._get_type(data_store.Link("a", "b", "c"))
                    ),
                    data_store.ColumnSchema(
                        "z",
                        data_store.SwListType(
                            data_store._get_type(data_store.Link("a", "b", "c"))
                        ),
                    ),
                    data_store.ColumnSchema(
                        "zz",
                        data_store.SwTupleType(
                            data_store._get_type(data_store.Link("a", "b", "c"))
                        ),
                    ),
                    data_store.ColumnSchema(
                        "zzz",
                        data_store.SwMapType(
                            data_store.STRING,
                            data_store._get_type(data_store.Link("a", "b", "c")),
                        ),
                    ),
                ],
            ),
            [
                {
                    "k": 0,
                    "x": [1, 2, 3],
                    "xx": (1, 2, 3),
                    "xxx": {"a": 1, "b": 2},
                    "y": data_store.Link("a", "b", "c"),
                    "z": [
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ],
                    "zz": (
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ),
                    "zzz": {"t": data_store.Link("1", "1", "1")},
                },
            ],
        )
        self.assertEqual(
            [
                {
                    "k": 0,
                    "x": [1, 2, 3],
                    "xx": (1, 2, 3),
                    "xxx": {"a": 1, "b": 2},
                    "y": data_store.Link("a", "b", "c"),
                    "z": [
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ],
                    "zz": (
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ),
                    "zzz": {"t": data_store.Link("1", "1", "1")},
                },
                {"k": 1, "a": "1", "b": "1"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "33", "b": "3", "c": 3},
            ],
            list(ds.scan_tables([data_store.TableDesc("test", None, False)])),
            "composite",
        )

    def test_data_store_update_table_with_multithread(self) -> None:
        ds = data_store.LocalDataStore(self.datastore_root)

        def ds_update(index: int) -> bool:
            for i in range(100 * (index - 1), 100 * index):
                ds.update_table(
                    "project/a_b/eval/test-m",
                    data_store.TableSchema(
                        "k",
                        [
                            data_store.ColumnSchema("k", data_store.INT64),
                            data_store.ColumnSchema("a", data_store.STRING),
                            data_store.ColumnSchema("b", data_store.STRING),
                        ],
                    ),
                    [{"k": i, "a": "0", "b": "0"}],
                )
            return True

        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
            futures = [pool.submit(ds_update(index=index)) for index in range(0, 5)]
            results = [
                future.result for future in concurrent.futures.as_completed(futures)
            ]

        assert all(results)

        self.assertEqual(
            500,
            len(
                list(
                    ds.scan_tables(
                        [data_store.TableDesc("project/a_b/eval/test-m", None, False)]
                    )
                )
            ),
            "length check",
        )

    def test_data_store_scan(self) -> None:
        ds = data_store.LocalDataStore(self.datastore_root)
        ds.update_table(
            "1",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("a", data_store.STRING),
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            [
                {"k": 0, "a": "0", "b": "0"},
                {"k": 1, "a": "1", "b": "1"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
        )
        ds.update_table(
            "2",
            data_store.TableSchema(
                "a",
                [
                    data_store.ColumnSchema("a", data_store.INT64),
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            [
                {"a": 0, "b": "0"},
                {"a": 1, "b": "1"},
                {"a": 2, "b": "2"},
                {"a": 3, "b": "3"},
            ],
        )
        ds.update_table(
            "3",
            data_store.TableSchema(
                "a",
                [
                    data_store.ColumnSchema("a", data_store.INT64),
                    data_store.ColumnSchema("x", data_store.STRING),
                ],
            ),
            [
                {"a": 0, "x": "0"},
                {"a": 1, "x": "1"},
                {"a": 2, "x": "2"},
                {"a": 3, "x": "3"},
            ],
        )
        ds.update_table(
            "4",
            data_store.TableSchema(
                "x",
                [
                    data_store.ColumnSchema("x", data_store.STRING),
                ],
            ),
            [{"x": "0"}, {"x": "1"}, {"x": "2"}, {"x": "3"}],
        )
        ds.update_table(
            "5",
            data_store.TableSchema(
                "a",
                [
                    data_store.ColumnSchema("a", data_store.INT64),
                    data_store.ColumnSchema("x", data_store.STRING),
                ],
            ),
            [{"a": 0, "x": "10"}, {"a": 1}, {"a": 2, "x": "12"}, {"a": 3, "x": "13"}],
        )
        with open(os.path.join(self.datastore_root, "6"), "w"):
            pass
        ds.update_table(
            "7",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema(
                        "x", data_store.SwListType(data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "xx", data_store.SwTupleType(data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "xxx", data_store.SwMapType(data_store.STRING, data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "y", data_store._get_type(data_store.Link("a", "b", "c"))
                    ),
                    data_store.ColumnSchema(
                        "z",
                        data_store.SwListType(
                            data_store._get_type(data_store.Link("a", "b", "c"))
                        ),
                    ),
                    data_store.ColumnSchema(
                        "zz",
                        data_store.SwTupleType(
                            data_store._get_type(data_store.Link("a", "b", "c"))
                        ),
                    ),
                    data_store.ColumnSchema(
                        "zzz",
                        data_store.SwMapType(
                            data_store.STRING,
                            data_store._get_type(data_store.Link("a", "b", "c")),
                        ),
                    ),
                ],
            ),
            [
                {
                    "k": 0,
                    "x": [1, 2, 3],
                    "xx": (1, 2, 3),
                    "xxx": {"a": 1, "b": 2},
                    "y": data_store.Link("a", "b", "c"),
                    "z": [
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ],
                    "zz": (
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ),
                    "zzz": {"t": data_store.Link("1", "1", "1")},
                }
            ],
        )
        with self.assertRaises(RuntimeError, msg="duplicate alias"):
            list(
                ds.scan_tables([data_store.TableDesc("1", {"k": "v", "a": "v"}, False)])
            )
        with self.assertRaises(RuntimeError, msg="conflicting key type"):
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", None, False),
                        data_store.TableDesc("4", None, False),
                    ]
                )
            )
        self.assertEqual(
            [
                {"k": 0, "a": "0", "b": "0"},
                {"k": 1, "a": "1", "b": "1"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
            list(ds.scan_tables([data_store.TableDesc("1", None, False)])),
            "scan all",
        )
        self.assertEqual(
            [
                {"k": 0, "a": 0, "b": "0", "x": "10"},
                {"k": 1, "a": 1, "b": "1", "x": "1"},
                {"k": 2, "a": 2, "b": "2", "x": "12"},
                {"k": 3, "a": 3, "b": "3", "x": "13"},
            ],
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", None, False),
                        data_store.TableDesc("2", None, False),
                        data_store.TableDesc("3", None, False),
                        data_store.TableDesc("5", None, False),
                    ]
                )
            ),
            "merge all",
        )
        self.assertEqual(
            [
                {"a": 0, "c": "0", "x": "0", "y": "10"},
                {"a": 1, "c": "1", "x": "1"},
                {"a": 2, "c": "2", "x": "2", "y": "12"},
                {"a": 3, "c": "3", "x": "3", "y": "13"},
            ],
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", {"a": "a", "b": "c"}, False),
                        data_store.TableDesc("2", {"a": "a", "b": "c"}, False),
                        data_store.TableDesc(
                            "3", {"a": "a", "b": "c", "x": "x"}, False
                        ),
                        data_store.TableDesc(
                            "5", {"a": "a", "b": "c", "x": "y"}, False
                        ),
                    ],
                )
            ),
            "some columns",
        )
        self.assertEqual(
            [{"a": 1, "c": "1", "x": "1"}],
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", {"a": "a", "b": "c"}, False),
                        data_store.TableDesc("2", {"a": "a", "b": "c"}, False),
                        data_store.TableDesc(
                            "3", {"a": "a", "b": "c", "x": "x"}, False
                        ),
                        data_store.TableDesc(
                            "5", {"a": "a", "b": "c", "x": "y"}, False
                        ),
                    ],
                    1,
                    2,
                )
            ),
            "with start and end",
        )
        self.assertEqual(
            [
                {
                    "k": 0,
                    "x": [1, 2, 3],
                    "xx": (1, 2, 3),
                    "xxx": {"a": 1, "b": 2},
                    "y": data_store.Link("a", "b", "c"),
                    "z": [
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ],
                    "zz": (
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ),
                    "zzz": {"t": data_store.Link("1", "1", "1")},
                }
            ],
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", {"k": "k"}, False),
                        data_store.TableDesc(
                            "7",
                            {
                                "x": "x",
                                "xx": "xx",
                                "xxx": "xxx",
                                "y": "y",
                                "z": "z",
                                "zz": "zz",
                                "zzz": "zzz",
                            },
                            False,
                        ),
                    ],
                    0,
                    1,
                )
            ),
            "composite type",
        )

        ds.update_table(
            "1",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("a", data_store.STRING),
                ],
            ),
            [
                {"k": 0, "a": None},
            ],
        )
        ds.dump()
        ds = data_store.LocalDataStore(self.datastore_root)
        self.assertEqual(
            [
                {
                    "k": 0,
                    "a": None,
                    "b": "0",
                    "x": [1, 2, 3],
                    "xx": (1, 2, 3),
                    "xxx": {"a": 1, "b": 2},
                    "y": data_store.Link("a", "b", "c"),
                    "z": [
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ],
                    "zz": (
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ),
                    "zzz": {"t": data_store.Link("1", "1", "1")},
                },
                {"k": 1, "a": "1", "b": "1"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", None, True),
                        data_store.TableDesc("7", None, False),
                    ],
                    keep_none=True,
                )
            ),
            "scan disk",
        )

        ds.update_table(
            "1",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("c", data_store.INT64),
                ],
            ),
            [
                {"k": 0, "c": 1},
                {"k": 1, "-": True},
            ],
        )
        self.assertEqual(
            [
                {"k": 0, "a": None, "b": "0", "c": 1},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", None, True),
                    ],
                    keep_none=True,
                )
            ),
            "scan mem and disk",
        )

        ds.dump()
        ds = data_store.LocalDataStore(self.datastore_root)
        self.assertEqual(
            [
                {"k": 0, "a": None, "b": "0", "c": 1},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
            list(
                ds.scan_tables(
                    [
                        data_store.TableDesc("1", None, True),
                    ],
                    keep_none=True,
                )
            ),
            "merge dump",
        )


class TestRemoteDataStore(unittest.TestCase):
    def setUp(self) -> None:
        self.ds = data_store.RemoteDataStore("http://test", "tt")

    @patch("starwhale.api._impl.data_store.requests.post")
    def test_update_table(self, mock_post: Mock) -> None:
        mock_post.return_value.status_code = 200
        mock_post.return_value.json.return_value = ""
        self.ds.update_table(
            "t1",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("a", data_store.STRING),
                    data_store.ColumnSchema("z", data_store.FLOAT64),
                ],
            ),
            [
                {"k": 1, "a": "1"},
                {"k": 2, "a": "2"},
                {"k": 3, "-": True},
                {"k": 4, "a": None},
                {"k": 5, "z": 0.0},
            ],
        )
        mock_post.assert_called_with(
            "http://test/api/v1/datastore/updateTable",
            data=json.dumps(
                {
                    "tableName": "t1",
                    "tableSchemaDesc": {
                        "keyColumn": "k",
                        "columnSchemaList": [
                            {"type": "INT64", "name": "k"},
                            {"type": "STRING", "name": "a"},
                            {"type": "FLOAT64", "name": "z"},
                        ],
                    },
                    "records": [
                        {
                            "values": [
                                {"key": "k", "value": "0000000000000001"},
                                {"key": "a", "value": "1"},
                            ]
                        },
                        {
                            "values": [
                                {"key": "k", "value": "0000000000000002"},
                                {"key": "a", "value": "2"},
                            ]
                        },
                        {
                            "values": [
                                {"key": "k", "value": "0000000000000003"},
                                {"key": "-", "value": "1"},
                            ]
                        },
                        {
                            "values": [
                                {"key": "k", "value": "0000000000000004"},
                                {"key": "a", "value": None},
                            ]
                        },
                        {
                            "values": [
                                {"key": "k", "value": "0000000000000005"},
                                {"key": "z", "value": "0000000000000000"},
                            ]
                        },
                    ],
                },
                separators=(",", ":"),
            ),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": "tt",
            },
            timeout=60,
        )
        self.ds.update_table(
            "t1",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                ],
            ),
            [],
        )
        mock_post.assert_called_with(
            "http://test/api/v1/datastore/updateTable",
            data=json.dumps(
                {
                    "tableName": "t1",
                    "tableSchemaDesc": {
                        "keyColumn": "k",
                        "columnSchemaList": [
                            {"type": "INT64", "name": "k"},
                        ],
                    },
                    "records": [],
                },
                separators=(",", ":"),
            ),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": "tt",
            },
            timeout=60,
        )
        self.ds.update_table(
            "t1",
            data_store.TableSchema(
                "key",
                [
                    data_store.ColumnSchema("key", data_store.INT64),
                    data_store.ColumnSchema("b", data_store.BOOL),
                    data_store.ColumnSchema("c", data_store.INT8),
                    data_store.ColumnSchema("d", data_store.INT16),
                    data_store.ColumnSchema("e", data_store.INT32),
                    data_store.ColumnSchema("f", data_store.FLOAT16),
                    data_store.ColumnSchema("g", data_store.FLOAT32),
                    data_store.ColumnSchema("h", data_store.FLOAT64),
                    data_store.ColumnSchema("i", data_store.BYTES),
                    data_store.ColumnSchema(
                        "j", data_store.SwListType(data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "jj", data_store.SwTupleType(data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "jjj", data_store.SwMapType(data_store.STRING, data_store.INT64)
                    ),
                    data_store.ColumnSchema(
                        "k",
                        data_store.SwObjectType(
                            data_store.Link,
                            {
                                "uri": data_store.STRING,
                                "display_text": data_store.STRING,
                                "mime_type": data_store.STRING,
                            },
                        ),
                    ),
                    data_store.ColumnSchema(
                        "l",
                        data_store.SwListType(
                            data_store.SwObjectType(
                                data_store.Link,
                                {
                                    "uri": data_store.STRING,
                                    "display_text": data_store.STRING,
                                    "mime_type": data_store.STRING,
                                },
                            )
                        ),
                    ),
                    data_store.ColumnSchema(
                        "ll",
                        data_store.SwTupleType(
                            data_store.SwObjectType(
                                data_store.Link,
                                {
                                    "uri": data_store.STRING,
                                    "display_text": data_store.STRING,
                                    "mime_type": data_store.STRING,
                                },
                            )
                        ),
                    ),
                    data_store.ColumnSchema(
                        "lll",
                        data_store.SwMapType(
                            data_store.STRING,
                            data_store.SwObjectType(
                                data_store.Link,
                                {
                                    "uri": data_store.STRING,
                                    "display_text": data_store.STRING,
                                    "mime_type": data_store.STRING,
                                },
                            ),
                        ),
                    ),
                ],
            ),
            [
                {
                    "key": 1,
                    "b": True,
                    "c": 1,
                    "d": 1,
                    "e": 1,
                    "f": 1.0,
                    "g": 1.0,
                    "h": 1.0,
                    "i": b"1",
                    "j": [1, 2, 3],
                    "jj": (1, 2, 3),
                    "jjj": {"a": 1, "b": 2},
                    "k": data_store.Link("a", "b", "c"),
                    "l": [
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ],
                    "ll": (
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ),
                    "lll": {"t": data_store.Link("1", "1", "1")},
                }
            ],
        )
        mock_post.assert_called_with(
            "http://test/api/v1/datastore/updateTable",
            data=json.dumps(
                {
                    "tableName": "t1",
                    "tableSchemaDesc": {
                        "keyColumn": "key",
                        "columnSchemaList": [
                            {"type": "INT64", "name": "key"},
                            {"type": "BOOL", "name": "b"},
                            {"type": "INT8", "name": "c"},
                            {"type": "INT16", "name": "d"},
                            {"type": "INT32", "name": "e"},
                            {"type": "FLOAT16", "name": "f"},
                            {"type": "FLOAT32", "name": "g"},
                            {"type": "FLOAT64", "name": "h"},
                            {"type": "BYTES", "name": "i"},
                            {
                                "type": "LIST",
                                "elementType": {"type": "INT64"},
                                "name": "j",
                            },
                            {
                                "type": "TUPLE",
                                "elementType": {"type": "INT64"},
                                "name": "jj",
                            },
                            {
                                "type": "MAP",
                                "keyType": {"type": "STRING"},
                                "valueType": {"type": "INT64"},
                                "name": "jjj",
                            },
                            {
                                "type": "OBJECT",
                                "attributes": [
                                    {"type": "STRING", "name": "uri"},
                                    {"type": "STRING", "name": "display_text"},
                                    {"type": "STRING", "name": "mime_type"},
                                ],
                                "pythonType": "LINK",
                                "name": "k",
                            },
                            {
                                "type": "LIST",
                                "elementType": {
                                    "type": "OBJECT",
                                    "attributes": [
                                        {"type": "STRING", "name": "uri"},
                                        {"type": "STRING", "name": "display_text"},
                                        {"type": "STRING", "name": "mime_type"},
                                    ],
                                    "pythonType": "LINK",
                                },
                                "name": "l",
                            },
                            {
                                "type": "TUPLE",
                                "elementType": {
                                    "type": "OBJECT",
                                    "attributes": [
                                        {"type": "STRING", "name": "uri"},
                                        {"type": "STRING", "name": "display_text"},
                                        {"type": "STRING", "name": "mime_type"},
                                    ],
                                    "pythonType": "LINK",
                                },
                                "name": "ll",
                            },
                            {
                                "type": "MAP",
                                "keyType": {"type": "STRING"},
                                "valueType": {
                                    "type": "OBJECT",
                                    "attributes": [
                                        {"type": "STRING", "name": "uri"},
                                        {"type": "STRING", "name": "display_text"},
                                        {"type": "STRING", "name": "mime_type"},
                                    ],
                                    "pythonType": "LINK",
                                },
                                "name": "lll",
                            },
                        ],
                    },
                    "records": [
                        {
                            "values": [
                                {"key": "key", "value": "0000000000000001"},
                                {"key": "b", "value": "1"},
                                {"key": "c", "value": "01"},
                                {"key": "d", "value": "0001"},
                                {"key": "e", "value": "00000001"},
                                {"key": "f", "value": "3c00"},
                                {"key": "g", "value": "3f800000"},
                                {"key": "h", "value": "3ff0000000000000"},
                                {"key": "i", "value": "MQ=="},
                                {
                                    "key": "j",
                                    "value": [
                                        "0000000000000001",
                                        "0000000000000002",
                                        "0000000000000003",
                                    ],
                                },
                                {
                                    "key": "jj",
                                    "value": [
                                        "0000000000000001",
                                        "0000000000000002",
                                        "0000000000000003",
                                    ],
                                },
                                {
                                    "key": "jjj",
                                    "value": {
                                        "a": "0000000000000001",
                                        "b": "0000000000000002",
                                    },
                                },
                                {
                                    "key": "k",
                                    "value": {
                                        "uri": "a",
                                        "display_text": "b",
                                        "mime_type": "c",
                                    },
                                },
                                {
                                    "key": "l",
                                    "value": [
                                        {
                                            "uri": "1",
                                            "display_text": "1",
                                            "mime_type": "1",
                                        },
                                        {
                                            "uri": "2",
                                            "display_text": "2",
                                            "mime_type": "2",
                                        },
                                        {
                                            "uri": "3",
                                            "display_text": "3",
                                            "mime_type": "3",
                                        },
                                    ],
                                },
                                {
                                    "key": "ll",
                                    "value": [
                                        {
                                            "uri": "1",
                                            "display_text": "1",
                                            "mime_type": "1",
                                        },
                                        {
                                            "uri": "2",
                                            "display_text": "2",
                                            "mime_type": "2",
                                        },
                                        {
                                            "uri": "3",
                                            "display_text": "3",
                                            "mime_type": "3",
                                        },
                                    ],
                                },
                                {
                                    "key": "lll",
                                    "value": {
                                        "t": {
                                            "uri": "1",
                                            "display_text": "1",
                                            "mime_type": "1",
                                        }
                                    },
                                },
                            ]
                        }
                    ],
                },
                separators=(",", ":"),
            ),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": "tt",
            },
            timeout=60,
        )

    @patch("starwhale.api._impl.data_store.requests.post")
    def test_scan_table(self, mock_post: Mock) -> None:
        mock_post.return_value.status_code = 200
        mock_post.return_value.json.return_value = {
            "data": {
                "columnTypes": [
                    {"name": "a", "type": "BOOL"},
                    {"name": "b", "type": "INT8"},
                    {"name": "c", "type": "INT16"},
                    {"name": "d", "type": "INT32"},
                    {"name": "e", "type": "INT64"},
                    {"name": "f", "type": "FLOAT16"},
                    {"name": "g", "type": "FLOAT32"},
                    {"name": "h", "type": "FLOAT64"},
                    {"name": "i", "type": "STRING"},
                    {"name": "j", "type": "BYTES"},
                    {"name": "k", "type": "LIST", "elementType": {"type": "INT64"}},
                    {"name": "kk", "type": "TUPLE", "elementType": {"type": "INT64"}},
                    {
                        "name": "kkk",
                        "type": "MAP",
                        "keyType": {"type": "STRING"},
                        "valueType": {"type": "INT64"},
                    },
                    {
                        "name": "l",
                        "type": "OBJECT",
                        "pythonType": "LINK",
                        "attributes": [
                            {"type": "STRING", "name": "uri"},
                            {"type": "STRING", "name": "display_text"},
                            {"type": "STRING", "name": "mime_type"},
                        ],
                    },
                    {
                        "name": "m",
                        "type": "LIST",
                        "elementType": {
                            "type": "OBJECT",
                            "pythonType": "LINK",
                            "attributes": [
                                {"type": "STRING", "name": "uri"},
                                {"type": "STRING", "name": "display_text"},
                                {"type": "STRING", "name": "mime_type"},
                            ],
                        },
                    },
                    {
                        "name": "mm",
                        "type": "TUPLE",
                        "elementType": {
                            "type": "OBJECT",
                            "pythonType": "LINK",
                            "attributes": [
                                {"type": "STRING", "name": "uri"},
                                {"type": "STRING", "name": "display_text"},
                                {"type": "STRING", "name": "mime_type"},
                            ],
                        },
                    },
                    {
                        "name": "mmm",
                        "type": "MAP",
                        "keyType": {"type": "STRING"},
                        "valueType": {
                            "type": "OBJECT",
                            "pythonType": "LINK",
                            "attributes": [
                                {"type": "STRING", "name": "uri"},
                                {"type": "STRING", "name": "display_text"},
                                {"type": "STRING", "name": "mime_type"},
                            ],
                        },
                    },
                    {"name": "n", "type": "FLOAT16"},
                    {"name": "o", "type": "FLOAT32"},
                    {"name": "p", "type": "FLOAT64"},
                    {"name": "q", "type": "FLOAT64"},
                ],
                "records": [
                    {
                        "a": "1",
                        "b": "1",
                        "c": "1",
                        "d": "1",
                        "e": "1",
                        "f": "3c00",
                        "g": "3f800000",
                        "h": "3ff0000000000000",
                        "i": "1",
                        "j": "MQ==",
                        "k": ["1", "2", "3"],
                        "kk": ["1", "2", "3"],
                        "kkk": {"a": "1", "b": "2"},
                        "l": {"uri": "a", "display_text": "b", "mime_type": "c"},
                        "m": [
                            {"uri": "1", "display_text": "1", "mime_type": "1"},
                            {"uri": "2", "display_text": "2", "mime_type": "2"},
                            {"uri": "3", "display_text": "3", "mime_type": "3"},
                        ],
                        "mm": [
                            {"uri": "1", "display_text": "1", "mime_type": "1"},
                            {"uri": "2", "display_text": "2", "mime_type": "2"},
                            {"uri": "3", "display_text": "3", "mime_type": "3"},
                        ],
                        "mmm": {
                            "t": {"uri": "1", "display_text": "1", "mime_type": "1"}
                        },
                        "n": "0",  # client(python):0000, server(java):0
                        "o": "0",  # client(python):00000000, server(java):0
                        "p": "0",  # client(python):0000000000000000, server(java):0
                        "q": "111111",  # client(python):000000000111111, server(java):111111
                    }
                ],
            }
        }
        self.assertEqual(
            [
                {
                    "a": True,
                    "b": 1,
                    "c": 1,
                    "d": 1,
                    "e": 1,
                    "f": 1.0,
                    "g": 1.0,
                    "h": 1.0,
                    "i": "1",
                    "j": b"1",
                    "k": [1, 2, 3],
                    "kk": (1, 2, 3),
                    "kkk": {"a": 1, "b": 2},
                    "l": data_store.Link("a", "b", "c"),
                    "m": [
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ],
                    "mm": (
                        data_store.Link("1", "1", "1"),
                        data_store.Link("2", "2", "2"),
                        data_store.Link("3", "3", "3"),
                    ),
                    "mmm": {"t": data_store.Link("1", "1", "1")},
                    "n": 0.0,
                    "o": 0.0,
                    "p": 0.0,
                    "q": 5.52603e-318,
                }
            ],
            list(
                self.ds.scan_tables(
                    [
                        data_store.TableDesc("t1", {"a": "b"}, True),
                        data_store.TableDesc("t2", ["a"]),
                        data_store.TableDesc("t3"),
                    ],
                    1,
                    1,
                    True,
                )
            ),
            "all types",
        )
        mock_post.assert_any_call(
            "http://test/api/v1/datastore/scanTable",
            data=json.dumps(
                {
                    "tables": [
                        {
                            "tableName": "t1",
                            "columns": [{"columnName": "a", "alias": "b"}],
                            "keepNone": True,
                        },
                        {
                            "tableName": "t2",
                            "columns": [{"columnName": "a", "alias": "a"}],
                        },
                        {
                            "tableName": "t3",
                        },
                    ],
                    "end": "0000000000000001",
                    "start": "0000000000000001",
                    "limit": 1000,
                    "keepNone": True,
                },
                separators=(",", ":"),
            ),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": "tt",
            },
            timeout=60,
        )
        mock_post.return_value.json.side_effect = [
            {
                "data": {
                    "columnTypes": [{"name": "a", "type": "INT32"}],
                    "records": [{"a": f"{i:x}"} for i in range(1000)],
                    "lastKey": f"{999:x}",
                }
            },
            {
                "data": {
                    "columnTypes": [{"name": "a", "type": "INT32"}],
                    "records": [{"a": f"{i+1000:x}"} for i in range(1000)],
                    "lastKey": f"{1999:x}",
                }
            },
            {
                "data": {
                    "columnTypes": [{"name": "a", "type": "INT32"}],
                    "records": [{"a": f"{2000:x}"}],
                }
            },
        ]

        self.assertEqual(
            [{"a": i} for i in range(2001)],
            list(self.ds.scan_tables([data_store.TableDesc("t1")])),
            "scan page",
        )
        mock_post.assert_any_call(
            "http://test/api/v1/datastore/scanTable",
            data=json.dumps(
                {
                    "tables": [
                        {
                            "tableName": "t1",
                        },
                    ],
                    "limit": 1000,
                },
                separators=(",", ":"),
            ),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": "tt",
            },
            timeout=60,
        )
        mock_post.assert_any_call(
            "http://test/api/v1/datastore/scanTable",
            data=json.dumps(
                {
                    "tables": [
                        {
                            "tableName": "t1",
                        },
                    ],
                    "limit": 1000,
                    "start": f"{999:x}",
                    "startInclusive": False,
                },
                separators=(",", ":"),
            ),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": "tt",
            },
            timeout=60,
        )
        mock_post.assert_any_call(
            "http://test/api/v1/datastore/scanTable",
            data=json.dumps(
                {
                    "tables": [
                        {
                            "tableName": "t1",
                        },
                    ],
                    "limit": 1000,
                    "start": f"{1999:x}",
                    "startInclusive": False,
                },
                separators=(",", ":"),
            ),
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": "tt",
            },
            timeout=60,
        )


class TestTableWriter(BaseTestCase):
    def setUp(self) -> None:
        super().setUp()
        self.writer = data_store.TableWriter("p/test", "k")

    def tearDown(self) -> None:
        self.writer.close()
        super().tearDown()

    def test_writer(self):
        _writer = data_store.TableWriter("p/test_flush", "id")
        _writer.insert({"id": 0, "result": "data"})
        with self.assertRaises(TableEmptyException):
            list(_writer.data_store.scan_tables([data_store.TableDesc("p/test_flush")]))
        _writer.close()

        _writer2 = data_store.TableWriter("p/test_flush2", "id")
        _writer2.insert({"id": 0, "result": "data"})
        _writer2.flush()
        self.assertEqual(
            len(
                list(
                    _writer.data_store.scan_tables(
                        [data_store.TableDesc("p/test_flush2")]
                    )
                )
            ),
            1,
        )
        _writer2.close()

        _writer3 = data_store.TableWriter("p/test_flush3", "id")
        _writer3.insert({"id": 0, "result": "data-0"})
        _writer3.flush()
        with patch(
            "starwhale.api._impl.data_store.LocalDataStore.update_table"
        ) as update_table:
            update_table.side_effect = RuntimeError()
            for i in range(1, 11):
                _writer3.insert({"id": i, "result": f"data-{i}"})
            _writer3.flush()
            self.assertEqual(
                len(
                    list(
                        _writer.data_store.scan_tables(
                            [data_store.TableDesc("p/test_flush3")]
                        )
                    )
                ),
                1,
            )
        with self.assertRaises(TableWriterException):
            _writer3.close()

    def test_insert_and_delete(self) -> None:
        with self.assertRaises(RuntimeError, msg="no key"):
            self.writer.insert({"a": "0"})
        self.writer.insert({"k": 0, "a": "0"})
        self.writer.insert({"k": 1, "a": "1"})
        self.writer.insert({"k": 2, "a": "2"})
        self.writer.insert({"k": 2, "a": "22"})
        self.writer.delete(1)
        self.writer.insert({"k": 3, "b": "3"})
        self.writer.insert({"k": 0, "a": None})
        self.writer.insert({"k": 4, "a": {"b": 0, "c": 1}})
        self.writer.insert(
            {
                "k": 5,
                "x": data_store.Link("http://test.com/1.jpg"),
                "y": data_store.Link("http://test.com/1.jpg", "1", "image/jpeg"),
            }
        )
        with self.assertRaises(RuntimeError, msg="conflicting type"):
            self.writer.insert({"k": 4, "a": 0})
        self.writer.close()
        self.assertEqual(
            [
                {"k": 0, "a": None},
                {"k": 2, "a": "22"},
                {"k": 3, "b": "3"},
                {"k": 4, "a/b": 0, "a/c": 1},
                {
                    "k": 5,
                    "x": data_store.Link("http://test.com/1.jpg"),
                    "y": data_store.Link("http://test.com/1.jpg", "1", "image/jpeg"),
                },
            ],
            list(
                data_store.LocalDataStore.get_instance().scan_tables(
                    [data_store.TableDesc("p/test", None, True)], keep_none=True
                )
            ),
            "scan all",
        )
        assert not self.writer.is_alive()

    @Mocker()
    def test_run_thread_exception_limit(self, request_mock: Mocker) -> None:
        request_mock.request(
            HTTPMethod.POST,
            url="http://1.1.1.1/api/v1/datastore/updateTable",
            status_code=400,
        )
        remote_store = data_store.RemoteDataStore("http://1.1.1.1", "tt")
        remote_writer = data_store.TableWriter(
            "p/test", "k", remote_store, run_exceptions_limits=0
        )

        assert remote_writer.is_alive()
        remote_writer.insert({"k": 0, "a": "0"})
        remote_writer.insert({"k": 1, "a": "1"})

        start = time.time()
        is_timeout = False
        while True:
            if not remote_writer.is_alive():
                break
            if time.time() - start > 10:
                is_timeout = True

        assert not is_timeout
        assert not remote_writer.is_alive()
        assert not remote_writer._stopped

        assert len(remote_writer._queue_run_exceptions) > 0
        for e in remote_writer._queue_run_exceptions:
            assert isinstance(e, requests.exceptions.HTTPError)

        with self.assertRaises(data_store.TableWriterException):
            remote_writer.insert({"k": 2, "a": "2"})

        assert len(remote_writer._queue_run_exceptions) == 0
        remote_writer.close()

    @Mocker()
    def test_run_thread_exception(self, request_mock: Mocker) -> None:
        request_mock.request(
            HTTPMethod.POST,
            url="http://1.1.1.1/api/v1/datastore/updateTable",
            status_code=400,
        )
        remote_store = data_store.RemoteDataStore("http://1.1.1.1", "tt")
        remote_writer = data_store.TableWriter("p/test", "k", remote_store)

        assert remote_writer.is_alive()
        remote_writer.insert({"k": 0, "a": "0"})
        remote_writer.insert({"k": 1, "a": "1"})

        with self.assertRaises(data_store.TableWriterException):
            remote_writer.close()


class TestScalarEncodeDecode(BaseTestCase):
    def test_int8(self) -> None:
        test_cases = {"ff": -1, "01": 1, "00": 0, "80": -128, "7f": 127}
        for tc in test_cases.items():
            self.assertEqual(
                tc[0], data_store.INT8.encode(tc[1]), f"INT8 decode {tc[1]} error"
            )
            self.assertEqual(
                tc[1], data_store.INT8.decode(tc[0]), f"INT8 encode {tc[0]} error"
            )

    def test_int16(self) -> None:
        test_cases = {"ffff": -1, "0001": 1, "0000": 0, "8000": -32768, "7fff": 32767}
        for tc in test_cases.items():
            self.assertEqual(
                tc[0], data_store.INT16.encode(tc[1]), f"INT16 decode {tc[1]} error"
            )
            self.assertEqual(
                tc[1], data_store.INT16.decode(tc[0]), f"INT16 encode {tc[0]} error"
            )

    def test_int32(self) -> None:
        test_cases = {
            "ffffffff": -1,
            "00000001": 1,
            "00000000": 0,
            "80000000": -2147483648,
            "7fffffff": 2147483647,
        }
        for tc in test_cases.items():
            self.assertEqual(
                tc[0], data_store.INT32.encode(tc[1]), f"INT32 decode {tc[1]} error"
            )
            self.assertEqual(
                tc[1], data_store.INT32.decode(tc[0]), f"INT32 encode {tc[0]} error"
            )

    def test_int64(self) -> None:
        test_cases = {
            "ffffffffffffffff": -1,
            "0000000000000001": 1,
            "0000000000000000": 0,
            "8000000000000000": -9223372036854775808,
            "7fffffffffffffff": 9223372036854775807,
        }
        for tc in test_cases.items():
            self.assertEqual(
                tc[0], data_store.INT64.encode(tc[1]), f"INT64 decode {tc[1]} error"
            )
            self.assertEqual(
                tc[1], data_store.INT64.decode(tc[0]), f"INT64 encode {tc[0]} error"
            )


if __name__ == "__main__":
    unittest.main()
