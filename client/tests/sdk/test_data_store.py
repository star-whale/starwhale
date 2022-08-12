import os
import unittest
from typing import Dict, List
from unittest.mock import patch, MagicMock

import numpy as np
import pyarrow as pa  # type: ignore

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
        path = os.path.join(self.root, "base-0.parquet")
        data_store._write_parquet_file(
            path,
            pa.Table.from_pydict(
                {
                    "a": [0, 1, 2],
                    "b": ["x", "y", "z"],
                    "c": [10, 11, 12],
                    "d": [None, None, str(data_store.Link(""))],
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
                                data_store.ColumnSchema("d", data_store.LINK),
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
                {"*": 2, "a": 2, "b": "z", "d": data_store.Link("")},
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

    def test_merge_scan(self) -> None:
        self.assertEqual([], list(data_store._merge_scan([])), "no iter")
        self.assertEqual(
            [{"*": 0, "a": 0}, {"*": 1, "a": 1}],
            list(data_store._merge_scan([iter([{"*": 0, "a": 0}, {"*": 1, "a": 1}])])),
            "one iter - ignore none",
        )
        self.assertEqual(
            [{"*": 0, "a": 0}, {"*": 1, "a": 1}, {"*": 2, "a": 2}, {"*": 3, "a": 3}],
            list(
                data_store._merge_scan(
                    [
                        iter([{"*": 0, "a": 0}, {"*": 2, "a": 2}]),
                        iter([{"*": 1, "a": 1}, {"*": 3, "a": 3}]),
                    ]
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
                    ]
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
                    ]
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
                )
            ),
            "removal",
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
            dir = os.path.join(self.root, dir)
            os.makedirs(dir)
            for file in files:
                file = os.path.join(dir, file)
                with open(file, "w"):
                    pass
        self.assertEqual(
            [],
            data_store._get_table_files(os.path.join(self.root, "0")),
            "empty",
        )
        self.assertEqual(
            [os.path.join(self.root, "1", "base-1.parquet")],
            data_store._get_table_files(os.path.join(self.root, "1")),
            "base only",
        )
        self.assertEqual(
            [
                os.path.join(self.root, "2", f)
                for f in ("base-0.parquet", "patch-1.parquet", "patch-3.parquet")
            ],
            data_store._get_table_files(os.path.join(self.root, "2")),
            "base and patches",
        )
        self.assertEqual(
            [
                os.path.join(self.root, "3", f)
                for f in ("base-1.parquet", "patch-2.parquet")
            ],
            data_store._get_table_files(os.path.join(self.root, "3")),
            "multiple bases",
        )

    def test_scan_table(self) -> None:
        data_store._write_parquet_file(
            os.path.join(self.root, "base-0.parquet"),
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
            os.path.join(self.root, "patch-1.parquet"),
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
            os.path.join(self.root, "base-1.parquet"),
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
            os.path.join(self.root, "patch-2.parquet"),
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
            os.path.join(self.root, "patch-3.parquet"),
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
            os.path.join(self.root, "patch-4.parquet"),
            pa.Table.from_pydict(
                {
                    "k": [0, 1, 2, 3],
                    "a": [None, None, None, "3"],
                    "b": ["0", "1", "2", "3"],
                },
                metadata={
                    "schema": str(
                        data_store.TableSchema(
                            "k",
                            [
                                data_store.ColumnSchema("k", data_store.INT64),
                                data_store.ColumnSchema("a", data_store.STRING),
                                data_store.ColumnSchema("b", data_store.STRING),
                            ],
                        )
                    )
                },
            ),
        )
        self.assertEqual(
            [],
            list(data_store._scan_table(os.path.join(self.root, "no"))),
            "empty",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0", "b": "0"},
                {"*": 1, "k": 1, "a": "1", "b": "1"},
                {"*": 2, "k": 2, "b": "2"},
                {"*": 3, "k": 3, "a": "3", "b": "3"},
                {"*": 5, "k": 5, "a": "5.5"},
            ],
            list(data_store._scan_table(self.root)),
            "scan all",
        )
        self.assertEqual(
            [
                {"*": 0, "i": "0", "j": "0"},
                {"*": 1, "i": "1", "j": "1"},
                {"*": 2, "j": "2"},
                {"*": 3, "i": "3", "j": "3"},
                {"*": 5, "i": "5.5"},
            ],
            list(data_store._scan_table(self.root, {"a": "i", "b": "j"})),
            "some columns",
        )
        self.assertEqual(
            [{"*": 2, "j": "2"}, {"*": 3, "i": "3", "j": "3"}],
            list(
                data_store._scan_table(self.root, {"a": "i", "b": "j"}, start=2, end=5)
            ),
            "with start and end",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0", "b": "0"},
                {"*": 1, "k": 1, "a": "1", "b": "1"},
                {"*": 2, "k": 2, "a": None, "b": "2"},
                {"*": 3, "k": 3, "a": "3", "b": "3"},
                {"*": 5, "k": 5, "a": "5.5", "b": None},
            ],
            list(data_store._scan_table(self.root, explicit_none=True)),
            "explicit none",
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
                "a", [data_store.ColumnSchema("a", data_store.NONE)]
            ),
            data_store._update_schema(data_store.TableSchema("a", []), {"a": None}),
            "none 1",
        )
        self.assertEqual(
            data_store.TableSchema(
                "a", [data_store.ColumnSchema("a", data_store.NONE)]
            ),
            data_store._update_schema(
                data_store.TableSchema(
                    "a", [data_store.ColumnSchema("a", data_store.NONE)]
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
                    "a", [data_store.ColumnSchema("a", data_store.NONE)]
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
        table.insert({"k": 4, "x": data_store.Link("t")})
        table.delete([2])
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0"},
                {"*": 1, "k": 1, "a": "1", "b": "1"},
                {"*": 2, "k": 2, "-": True},
                {"*": 3, "k": 3, "a": "3"},
                {"*": 4, "k": 4, "x": data_store.Link("t")},
            ],
            list(table.scan()),
            "scan all",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0", "b": None, "x": None},
                {"*": 1, "k": 1, "a": "1", "b": "1", "x": None},
                {"*": 2, "k": 2, "a": None, "b": None, "x": None, "-": True},
                {"*": 3, "k": 3, "a": "3", "b": None, "x": None},
                {"*": 4, "k": 4, "a": None, "b": None, "x": data_store.Link("t")},
            ],
            list(table.scan(explicit_none=True)),
            "explicit none",
        )
        self.assertEqual(
            [
                {"*": 0, "k": 0, "x": "0"},
                {"*": 1, "k": 1, "x": "1"},
                {"*": 2, "k": 2, "-": True},
                {"*": 3, "k": 3, "x": "3"},
                {"*": 4, "k": 4},
            ],
            list(table.scan({"k": "k", "a": "x"})),
            "some columns",
        )
        table.dump(self.root)
        self.assertEqual(
            [os.path.join(self.root, "test", "base-0.parquet")],
            data_store._get_table_files(os.path.join(self.root, "test")),
            "dump 1",
        )
        table.dump(self.root)
        self.assertEqual(
            [os.path.join(self.root, "test", "base-1.parquet")],
            data_store._get_table_files(os.path.join(self.root, "test")),
            "dump 2",
        )
        table = data_store.MemoryTable(
            "test",
            data_store.TableSchema(
                "k", [data_store.ColumnSchema("k", data_store.INT64)]
            ),
        )
        table.load(self.root)
        self.assertEqual(
            [
                {"*": 0, "k": 0, "a": "0"},
                {"*": 1, "k": 1, "a": "1", "b": "1"},
                {"*": 3, "k": 3, "a": "3"},
                {"*": 4, "k": 4, "x": data_store.Link("t")},
            ],
            list(table.scan()),
            "load",
        )


class TestLocalDataStore(BaseTestCase):
    def test_data_store_put(self) -> None:
        ds = data_store.LocalDataStore(self.root)
        with self.assertRaises(RuntimeError, msg="invalid column name"):
            ds.put(
                "test",
                data_store.TableSchema(
                    "+", [data_store.ColumnSchema("+", data_store.INT64)]
                ),
                [{"+": 0}],
            )
        with self.assertRaises(RuntimeError, msg="no key field"):
            ds.put(
                "test",
                data_store.TableSchema(
                    "k", [data_store.ColumnSchema("k", data_store.INT64)]
                ),
                [{"a": 0}],
            )
        ds.put(
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
            list(ds.scan_tables([("project/a_b/eval/test-0", "test", False)])),
            "name check",
        )
        ds.put(
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
            list(ds.scan_tables([("test", "test", False)])),
            "base",
        )
        ds.put(
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
            list(ds.scan_tables([("test", "test", False)])),
            "batch+patch",
        )
        ds.put(
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
            list(ds.scan_tables([("test", "test", False)])),
            "overwrite",
        )
        ds.put(
            "test",
            data_store.TableSchema(
                "k",
                [
                    data_store.ColumnSchema("k", data_store.INT64),
                    data_store.ColumnSchema("x", data_store.LINK),
                ],
            ),
            [{"k": 4, "x": data_store.Link("tt", "a", "b")}],
        )
        self.assertEqual(
            [
                {"k": 1, "a": "1", "b": "1"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "33", "b": "3", "c": 3},
                {"k": 4, "x": data_store.Link("tt", "a", "b")},
            ],
            list(ds.scan_tables([("test", "test", False)])),
            "link",
        )

    def test_data_store_scan(self) -> None:
        ds = data_store.LocalDataStore(self.root)
        ds.put(
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
        ds.put(
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
        ds.put(
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
        ds.put(
            "4",
            data_store.TableSchema(
                "x",
                [
                    data_store.ColumnSchema("b", data_store.STRING),
                ],
            ),
            [{"x": "0"}, {"x": "1"}, {"x": "2"}, {"x": "3"}],
        )
        ds.put(
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
        with open(os.path.join(self.root, "6"), "w"):
            pass
        with self.assertRaises(RuntimeError, msg="duplicate alias"):
            list(ds.scan_tables([("1", "1", False)], {"k": "v", "a": "v"}))
        with self.assertRaises(RuntimeError, msg="conflicting key type"):
            list(ds.scan_tables([("1", "1", False), ("4", "4", False)]))
        self.assertEqual(
            [
                {"k": 0, "a": "0", "b": "0"},
                {"k": 1, "a": "1", "b": "1"},
                {"k": 2, "b": "2"},
                {"k": 3, "a": "3", "b": "3"},
            ],
            list(ds.scan_tables([("1", "1", False)])),
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
                        ("1", "1", False),
                        ("2", "2", False),
                        ("3", "3", False),
                        ("5", "5", False),
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
                        ("1", "1", False),
                        ("2", "2", False),
                        ("3", "3", False),
                        ("5", "5", False),
                    ],
                    {"a": "a", "b": "c", "5.x": "y", "3.*": ""},
                )
            ),
            "some columns",
        )
        self.assertEqual(
            [{"a": 1, "c": "1", "x": "1"}],
            list(
                ds.scan_tables(
                    [
                        ("1", "1", False),
                        ("2", "2", False),
                        ("3", "3", False),
                        ("5", "5", False),
                    ],
                    {"a": "a", "b": "c", "5.x": "y", "3.*": ""},
                    1,
                    2,
                )
            ),
            "with start and end",
        )


class TestTableWriter(BaseTestCase):
    def setUp(self) -> None:
        self.mock_atexit = patch("starwhale.api._impl.data_store.atexit", MagicMock())
        self.mock_atexit.start()

        super().setUp()
        self.writer = data_store.TableWriter("p/test", "k")

    def tearDown(self) -> None:
        self.writer.close()
        super().tearDown()
        self.mock_atexit.stop()

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
        with self.assertRaises(RuntimeError, msg="conflicting type"):
            self.writer.insert({"k": 4, "a": 0})
        self.writer.close()
        self.assertEqual(
            [{"k": 0}, {"k": 2, "a": "22"}, {"k": 3, "b": "3"}],
            list(
                data_store.LocalDataStore.get_instance().scan_tables(
                    [("p/test", "test", False)]
                )
            ),
            "scan all",
        )


if __name__ == "__main__":
    unittest.main()
