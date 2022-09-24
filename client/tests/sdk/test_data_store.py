import os
import json
import time
import unittest
from typing import Dict, List
from unittest.mock import Mock, patch

import numpy as np
import pyarrow as pa  # type: ignore
import requests
from requests_mock import Mocker

from starwhale.consts import HTTPMethod
from starwhale.api._impl import data_store

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
            [
                {"*": 0, "a": 0, "b": "x", "c": 10},
                {"*": 1, "a": 1, "b": "y", "c": 11, "-": True},
                {"*": 2, "a": 2, "b": "z", "c": None},
            ],
            list(data_store._scan_parquet_file(path, keep_none=True)),
            "keep none",
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
                "a",
                [
                    data_store.ColumnSchema("a", data_store.INT64),
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            data_store._update_schema(
                data_store.TableSchema(
                    "a",
                    [
                        data_store.ColumnSchema("a", data_store.INT64),
                        data_store.ColumnSchema("b", data_store.STRING),
                    ],
                ),
                {"a": np.int32(0)},
            ),
            "less bits",
        )
        self.assertEqual(
            data_store.TableSchema(
                "c", [data_store.ColumnSchema("c", data_store.INT64)]
            ),
            data_store._update_schema(
                data_store.TableSchema(
                    "c", [data_store.ColumnSchema("c", data_store.INT32)]
                ),
                {"c": 0},
            ),
            "more bits",
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
                {"*": 0, "k": 0, "x": "0"},
                {"*": 1, "k": 1},
                {"*": 2, "k": 2, "-": True},
                {"*": 3, "k": 3, "x": "3"},
            ],
            list(table.scan({"k": "k", "a": "x"})),
            "some columns",
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
                {"k": 0, "a": None, "b": "0"},
                {"k": 1, "a": "1", "b": "1"},
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
            "scan disk",
        )

        ds.update_table(
            "1",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("c", data_store.INT32),
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
                ],
            ),
            [
                {"k": 1, "a": "1"},
                {"k": 2, "a": "2"},
                {"k": 3, "-": True},
                {"k": 4, "a": None},
            ],
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
        self.ds.update_table(
            "t1",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("b", data_store.BOOL),
                    data_store.ColumnSchema("c", data_store.INT8),
                    data_store.ColumnSchema("d", data_store.INT16),
                    data_store.ColumnSchema("e", data_store.INT32),
                    data_store.ColumnSchema("f", data_store.FLOAT16),
                    data_store.ColumnSchema("g", data_store.FLOAT32),
                    data_store.ColumnSchema("h", data_store.FLOAT64),
                    data_store.ColumnSchema("i", data_store.BYTES),
                ],
            ),
            [
                {
                    "k": 1,
                    "b": True,
                    "c": 1,
                    "d": 1,
                    "e": 1,
                    "f": 1.0,
                    "g": 1.0,
                    "h": 1.0,
                    "i": b"1",
                }
            ],
        )
        mock_post.assert_any_call(
            "http://test/api/v1/datastore/updateTable",
            data=json.dumps(
                {
                    "tableName": "t1",
                    "tableSchemaDesc": {
                        "keyColumn": "k",
                        "columnSchemaList": [
                            {"name": "k", "type": "INT64"},
                            {"name": "a", "type": "STRING"},
                        ],
                    },
                    "records": [
                        {
                            "values": [
                                {"key": "k", "value": "1"},
                                {"key": "a", "value": "1"},
                            ]
                        },
                        {
                            "values": [
                                {"key": "k", "value": "2"},
                                {"key": "a", "value": "2"},
                            ]
                        },
                        {
                            "values": [
                                {"key": "k", "value": "3"},
                                {"key": "-", "value": "1"},
                            ]
                        },
                        {
                            "values": [
                                {"key": "k", "value": "4"},
                                {"key": "a", "value": None},
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
        mock_post.assert_any_call(
            "http://test/api/v1/datastore/updateTable",
            data=json.dumps(
                {
                    "tableName": "t1",
                    "tableSchemaDesc": {
                        "keyColumn": "k",
                        "columnSchemaList": [
                            {"name": "k", "type": "INT64"},
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
        mock_post.assert_any_call(
            "http://test/api/v1/datastore/updateTable",
            data=json.dumps(
                {
                    "tableName": "t1",
                    "tableSchemaDesc": {
                        "keyColumn": "k",
                        "columnSchemaList": [
                            {"name": "k", "type": "INT64"},
                            {"name": "b", "type": "BOOL"},
                            {"name": "c", "type": "INT8"},
                            {"name": "d", "type": "INT16"},
                            {"name": "e", "type": "INT32"},
                            {"name": "f", "type": "FLOAT16"},
                            {"name": "g", "type": "FLOAT32"},
                            {"name": "h", "type": "FLOAT64"},
                            {"name": "i", "type": "BYTES"},
                        ],
                    },
                    "records": [
                        {
                            "values": [
                                {"key": "k", "value": "1"},
                                {"key": "b", "value": "1"},
                                {"key": "c", "value": "1"},
                                {"key": "d", "value": "1"},
                                {"key": "e", "value": "1"},
                                {"key": "f", "value": "3c00"},
                                {"key": "g", "value": "3f800000"},
                                {"key": "h", "value": "3ff0000000000000"},
                                {"key": "i", "value": "MQ=="},
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
                "columnTypes": {
                    "a": "BOOL",
                    "b": "INT8",
                    "c": "INT16",
                    "d": "INT32",
                    "e": "INT64",
                    "f": "FLOAT16",
                    "g": "FLOAT32",
                    "h": "FLOAT64",
                    "i": "STRING",
                    "j": "BYTES",
                },
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
        mock_post.return_value.json.side_effect = [
            {
                "data": {
                    "columnTypes": {"a": "INT32"},
                    "records": [{"a": f"{i:x}"} for i in range(1000)],
                    "lastKey": f"{999:x}",
                }
            },
            {
                "data": {
                    "columnTypes": {"a": "INT32"},
                    "records": [{"a": f"{i+1000:x}"} for i in range(1000)],
                    "lastKey": f"{1999:x}",
                }
            },
            {
                "data": {
                    "columnTypes": {"a": "INT32"},
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
                    "end": "1",
                    "start": "1",
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

        assert is_timeout
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


if __name__ == "__main__":
    unittest.main()
