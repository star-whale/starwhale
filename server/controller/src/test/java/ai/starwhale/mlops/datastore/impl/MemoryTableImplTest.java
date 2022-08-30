/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.starwhale.mlops.datastore.impl;

import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.MemoryTable;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableScanIterator;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.WalManager;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.memory.impl.SwByteBufferManager;
import ai.starwhale.mlops.objectstore.impl.FileSystemObjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MemoryTableImplTest {

    private static List<Map<String, String>> getRecords(TableScanIterator it) {
        var ret = new ArrayList<Map<String, String>>();
        for (; ; ) {
            it.next();
            var record = it.getRecord();
            if (record == null) {
                break;
            }
            ret.add(it.getRecord());
        }
        return ret;
    }

    private static List<Map<String, String>> scanAll(MemoryTable memoryTable, boolean keepNone) {
        return MemoryTableImplTest.getRecords(memoryTable.scan(null, null, true, null, false, keepNone, false));
    }

    private static List<Map<String, Object>> decodeRecords(Map<String, ColumnType> columnTypeMap,
                                                           List<Map<String, String>> records) {
        return records.stream()
                .map(r -> {
                    var record = new HashMap<String, Object>();
                    for (var entry : r.entrySet()) {
                        record.put(entry.getKey(), columnTypeMap.get(entry.getKey()).decode(entry.getValue()));
                    }
                    return record;
                })
                .collect(Collectors.toList());
    }

    @TempDir
    private File rootDir;

    private WalManager walManager;

    @BeforeEach
    public void setUp() throws IOException {
        SwBufferManager bufferManager = new SwByteBufferManager();
        FileSystemObjectStore objectStore = new FileSystemObjectStore(bufferManager, this.rootDir.getAbsolutePath());
        this.walManager = new WalManager(objectStore, bufferManager, 256, 4096, "test/", 10);
    }

    @Nested
    public class UpdateTest {
        private MemoryTableImpl memoryTable;

        @BeforeEach
        public void setUp() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
        }

        @Test
        public void testUpdateCommon() {
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(
                            new ColumnSchemaDesc("k", "STRING"),
                            new ColumnSchemaDesc("a", "INT32"))),
                    List.of(Map.of("k", "0", "a", "a")));
            assertThat("init", scanAll(this.memoryTable, false), contains(Map.of("k", "0", "a", "a")));

            this.memoryTable.update(null, List.of(Map.of("k", "1", "a", "b")));
            assertThat("insert", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a", "a"),
                    Map.of("k", "1", "a", "b")));

            this.memoryTable.update(
                    null,
                    List.of(Map.of("k", "2", "a", "c"),
                            Map.of("k", "3", "a", "d")));
            assertThat("insert multiple", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a", "a"),
                    Map.of("k", "1", "a", "b"),
                    Map.of("k", "2", "a", "c"),
                    Map.of("k", "3", "a", "d")));

            this.memoryTable.update(null, List.of(Map.of("k", "1", "a", "c")));
            assertThat("overwrite", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a", "a"),
                    Map.of("k", "1", "a", "c"),
                    Map.of("k", "2", "a", "c"),
                    Map.of("k", "3", "a", "d")));

            this.memoryTable.update(null, List.of(Map.of("k", "2", "-", "1")));
            assertThat("delete", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a", "a"),
                    Map.of("k", "1", "a", "c"),
                    Map.of("k", "3", "a", "d")));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("b", "INT32"))),
                    List.of(Map.of("k", "1", "b", "0")));
            assertThat("new column", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a", "a"),
                    Map.of("k", "1", "a", "c", "b", "0"),
                    Map.of("k", "3", "a", "d")));

            this.memoryTable.update(
                    null,
                    List.of(new HashMap<>() {{
                                put("k", "1");
                                put("a", null);
                            }},
                            new HashMap<>() {{
                                put("k", "2");
                                put("a", null);
                            }},
                            new HashMap<>() {{
                                put("k", "3");
                                put("b", null);
                            }}));
            assertThat("null value", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a", "a"),
                    Map.of("k", "1", "b", "0"),
                    Map.of("k", "2"),
                    Map.of("k", "3", "a", "d")));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("c", "INT32"))),
                    List.of(Map.of("k", "3", "-", "1"),
                            Map.of("k", "2", "a", "0"),
                            Map.of("k", "3", "a", "0"),
                            Map.of("k", "4", "c", "0"),
                            new HashMap<>() {{
                                put("k", "1");
                                put("b", null);
                                put("c", "1");
                            }},
                            Map.of("k", "0", "-", "1"),
                            Map.of("k", "2", "-", "1")));
            assertThat("mixed", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "1", "c", "1"),
                    Map.of("k", "3", "a", "0"),
                    Map.of("k", "4", "c", "0")));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("a-b/c/d:e_f", "INT32"))),
                    List.of(Map.of("k", "0", "a-b/c/d:e_f", "0")));
            assertThat("complex name", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a-b/c/d:e_f", "0"),
                    Map.of("k", "1", "c", "1"),
                    Map.of("k", "3", "a", "0"),
                    Map.of("k", "4", "c", "0")));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("x", "UNKNOWN"))),
                    List.of(new HashMap<>() {{
                        put("k", "0");
                        put("x", null);
                    }}));
            assertThat("unknown",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnType.UNKNOWN));
            assertThat("unknown", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a-b/c/d:e_f", "0"),
                    Map.of("k", "1", "c", "1"),
                    Map.of("k", "3", "a", "0"),
                    Map.of("k", "4", "c", "0")));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("x", "INT32"))),
                    List.of(Map.of("k", "1", "x", "1")));
            assertThat("update unknown",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnType.INT32));
            assertThat("update unknown", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a-b/c/d:e_f", "0"),
                    Map.of("k", "1", "c", "1", "x", "1"),
                    Map.of("k", "3", "a", "0"),
                    Map.of("k", "4", "c", "0")));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(new ColumnSchemaDesc("x", "UNKNOWN"))),
                    List.of(new HashMap<>() {{
                        put("k", "1");
                        put("x", null);
                    }}));
            assertThat("unknown again",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnType.INT32));
            assertThat("unknown again", scanAll(this.memoryTable, false), contains(
                    Map.of("k", "0", "a-b/c/d:e_f", "0"),
                    Map.of("k", "1", "c", "1"),
                    Map.of("k", "3", "a", "0"),
                    Map.of("k", "4", "c", "0")));
        }

        @Test
        public void testUpdateAllColumnTypes() {
            this.memoryTable.update(
                    new TableSchemaDesc("key", List.of(
                            new ColumnSchemaDesc("key", "STRING"),
                            new ColumnSchemaDesc("a", "BOOL"),
                            new ColumnSchemaDesc("b", "INT8"),
                            new ColumnSchemaDesc("c", "INT16"),
                            new ColumnSchemaDesc("d", "INT32"),
                            new ColumnSchemaDesc("e", "INT64"),
                            new ColumnSchemaDesc("f", "FLOAT32"),
                            new ColumnSchemaDesc("g", "FLOAT64"),
                            new ColumnSchemaDesc("h", "BYTES"),
                            new ColumnSchemaDesc("i", "UNKNOWN"))),
                    List.of(new HashMap<>() {{
                        put("key", "x");
                        put("a", "1");
                        put("b", "10");
                        put("c", "1000");
                        put("d", "100000");
                        put("e", "10000000");
                        put("f", Integer.toHexString(Float.floatToIntBits(1.1f)));
                        put("g", Long.toHexString(Double.doubleToLongBits(1.1)));
                        put("h", Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));
                        put("i", null);
                    }}));
            assertThat("all types", scanAll(this.memoryTable, false), contains(
                    new HashMap<>() {{
                        put("key", "x");
                        put("a", "1");
                        put("b", "10");
                        put("c", "1000");
                        put("d", "100000");
                        put("e", "10000000");
                        put("f", Integer.toHexString(Float.floatToIntBits(1.1f)));
                        put("g", Long.toHexString(Double.doubleToLongBits(1.1)));
                        put("h", Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));
                    }}));
        }

        @Test
        public void testUpdateAllKeyColumnTypes() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "BOOL"))),
                    List.of(Map.of("k", "1")));
            assertThat("bool", scanAll(this.memoryTable, false), contains(Map.of("k", "1")));

            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "INT8"))),
                    List.of(Map.of("k", "10")));
            assertThat("int8", scanAll(this.memoryTable, false), contains(Map.of("k", "10")));

            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "INT16"))),
                    List.of(Map.of("k", "1000")));
            assertThat("int16", scanAll(this.memoryTable, false), contains(Map.of("k", "1000")));

            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "INT32"))),
                    List.of(Map.of("k", "100000")));
            assertThat("int32", scanAll(this.memoryTable, false), contains(Map.of("k", "100000")));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "INT64"))),
                    List.of(Map.of("k", "10000000")));
            assertThat("int64", scanAll(this.memoryTable, false), contains(Map.of("k", "10000000")));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "FLOAT32"))),
                    List.of(Map.of("k", Integer.toHexString(Float.floatToIntBits(1.1f)))));
            assertThat("float32", scanAll(this.memoryTable, false), contains(
                    Map.of("k", Integer.toHexString(Float.floatToIntBits(1.1f)))));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "FLOAT64"))),
                    List.of(Map.of("k", Long.toHexString(Double.doubleToLongBits(1.1)))));
            assertThat("float64", scanAll(this.memoryTable, false), contains(
                    Map.of("k", Long.toHexString(Double.doubleToLongBits(1.1)))));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "BYTES"))),
                    List.of(Map.of("k", Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)))));
            assertThat("bytes", scanAll(this.memoryTable, false), contains(
                    Map.of("k", Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)))));
        }

        @Test
        public void testUpdateExceptions() {
            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.update(null, null),
                    "null schema");

            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.update(
                            new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("a", "INT32"))),
                            List.of(Map.of("k", "0"), Map.of("k", "1"))),
                    "no key column schema");
            assertThat("no key column schema", scanAll(this.memoryTable, false), empty());

            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.update(
                            new TableSchemaDesc("k",
                                    List.of(new ColumnSchemaDesc("k", "STRING"),
                                            new ColumnSchemaDesc("-", "INT32"))),
                            List.of(Map.of("k", "0"))),
                    "invalid column name");
            assertThat("invalid column name", scanAll(this.memoryTable, false), empty());

            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.update(
                            new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "STRING"))),
                            List.of(Map.of("k", "0"), Map.of("k", "1", "a", "1"))),
                    "extra column data");
            assertThat("extra column data", scanAll(this.memoryTable, false), empty());

            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.update(
                            new TableSchemaDesc("k", List.of(
                                    new ColumnSchemaDesc("k", "STRING"),
                                    new ColumnSchemaDesc("a", "INT32"))),
                            List.of(Map.of("k", "0"), Map.of("k", "1", "a", "h"))),
                    "fail to decode");
            assertThat("fail to decode", scanAll(this.memoryTable, false), empty());
        }

        @Test
        public void testUpdateWalError() {
            var schema = new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "STRING")));
            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.update(
                            schema,
                            List.of(Map.of("k", "a".repeat(5000)))),
                    "huge entry");
            assertThat("null", this.memoryTable.getSchema(), nullValue());
            this.memoryTable.update(schema, null);
            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.update(
                            null,
                            List.of(Map.of("k", "a".repeat(5000)))),
                    "huge entry");
            assertThat("schema", this.memoryTable.getSchema(), is(new TableSchema(schema)));
            assertThat("records", scanAll(this.memoryTable, false), empty());
        }

        @Test
        public void testUpdateFromWal() throws IOException {
            this.memoryTable.update(
                    new TableSchemaDesc("key", List.of(
                            new ColumnSchemaDesc("key", "STRING"),
                            new ColumnSchemaDesc("a", "BOOL"),
                            new ColumnSchemaDesc("b", "INT8"),
                            new ColumnSchemaDesc("c", "INT16"),
                            new ColumnSchemaDesc("d", "INT32"),
                            new ColumnSchemaDesc("e", "INT64"),
                            new ColumnSchemaDesc("f", "FLOAT32"),
                            new ColumnSchemaDesc("g", "FLOAT64"),
                            new ColumnSchemaDesc("h", "BYTES"),
                            new ColumnSchemaDesc("i", "UNKNOWN"))),
                    null);
            List<Map<String, String>> records = new ArrayList<>();
            for (int i = 0; i < 100; ++i) {
                final int index = i;
                records.add(new HashMap<>() {{
                    put("key", String.format("%03d", index));
                    put("a", "" + index % 2);
                    put("b", Integer.toHexString(index + 10));
                    put("c", Integer.toHexString(index + 1000));
                    put("d", Integer.toHexString(index + 100000));
                    put("e", Integer.toHexString(index + 10000000));
                    put("f", Integer.toHexString(Float.floatToIntBits(index + 0.1f)));
                    put("g", Long.toHexString(Double.doubleToLongBits(index + 0.1)));
                    put("h", Base64.getEncoder().encodeToString(
                            ("test" + index).getBytes(StandardCharsets.UTF_8)));
                    put("i", null);
                }});
            }
            this.memoryTable.update(null, records);
            MemoryTableImplTest.this.walManager.terminate();
            SwBufferManager bufferManager = new SwByteBufferManager();
            FileSystemObjectStore objectStore = new FileSystemObjectStore(bufferManager,
                    MemoryTableImplTest.this.rootDir.getAbsolutePath());
            MemoryTableImplTest.this.walManager = new WalManager(objectStore, bufferManager, 256, 4096, "test/", 10);
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            var it = MemoryTableImplTest.this.walManager.readAll();
            while (it.hasNext()) {
                this.memoryTable.updateFromWal(it.next());
            }
            assertThat(scanAll(this.memoryTable, true), is(records));
        }
    }

    @Nested
    public class QueryScanTest {
        private List<Map<String, String>> records;
        private MemoryTableImpl memoryTable;

        @BeforeEach
        public void setUp() {
            var data = new Object[][]{
                    {null, false, true, false, true, false, true, false, true, false},
                    {0, 1, 2, 3, 4, 5, 6, 7, 8, null},
                    {1, 2, 3, 4, 5, 6, 7, 8, null, 0},
                    {2, 3, 4, 5, 6, 7, 8, null, 0, 1},
                    {3, 4, 5, 6, 7, 8, null, 0, 1, 2},
                    {4f, 5f, 6f, 7f, 8f, null, 0f, 1f, 2f, 3f},
                    {5.0, 6.0, 7.0, 8.0, null, 0.0, 1.0, 2.0, 3.0, 4.0},
                    {"6", "7", "8", null, "0", "1", "2", "3", "4", "5"},
                    {"7", "8", null, "0", "1", "2", "3", "4", "5", "6"}};
            this.records = IntStream.rangeClosed(0, 9).mapToObj(
                            i -> {
                                Map<String, String> ret = new HashMap<>();
                                ret.put("key", Integer.toHexString(i));
                                if (data[0][i] != null) {
                                    ret.put("a", ColumnType.BOOL.encode(data[0][i], false));
                                }
                                if (data[1][i] != null) {
                                    ret.put("b", ColumnType.INT8.encode(data[1][i], false));
                                }
                                if (data[2][i] != null) {
                                    ret.put("c", ColumnType.INT16.encode(data[2][i], false));
                                }
                                if (data[3][i] != null) {
                                    ret.put("d", ColumnType.INT32.encode(data[3][i], false));
                                }
                                if (data[4][i] != null) {
                                    ret.put("e", ColumnType.INT64.encode(data[4][i], false));
                                }
                                if (data[5][i] != null) {
                                    ret.put("f", ColumnType.FLOAT32.encode(data[5][i], false));
                                }
                                if (data[6][i] != null) {
                                    ret.put("g", ColumnType.FLOAT64.encode(data[6][i], false));
                                }
                                if (data[7][i] != null) {
                                    ret.put("h", (String) data[7][i]);
                                }
                                if (data[8][i] != null) {
                                    ret.put("i", ColumnType.BYTES.encode(
                                            ByteBuffer.wrap(((String) data[8][i]).getBytes(StandardCharsets.UTF_8)),
                                            false));
                                }
                                return ret;
                            })
                    .collect(Collectors.toList());
            var schema = new TableSchemaDesc("key", List.of(
                    new ColumnSchemaDesc("key", "INT32"),
                    new ColumnSchemaDesc("a", "BOOL"),
                    new ColumnSchemaDesc("b", "INT8"),
                    new ColumnSchemaDesc("c", "INT16"),
                    new ColumnSchemaDesc("d", "INT32"),
                    new ColumnSchemaDesc("e", "INT64"),
                    new ColumnSchemaDesc("f", "FLOAT32"),
                    new ColumnSchemaDesc("g", "FLOAT64"),
                    new ColumnSchemaDesc("h", "STRING"),
                    new ColumnSchemaDesc("i", "BYTES"),
                    new ColumnSchemaDesc("z", "UNKNOWN")));
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(schema, records);
        }

        @Test
        public void testQueryInitialEmptyTable() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            var recordList = this.memoryTable.query(null, null, null, -1, -1, false, false);
            assertThat("empty", recordList.getColumnTypeMap(), nullValue());
            assertThat("empty", recordList.getRecords(), empty());
        }

        @Test
        public void testQueryEmptyTableWithSchema() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "STRING"))),
                    List.of(Map.of("k", "0", "-", "1")));
            var recordList = this.memoryTable.query(null, null, null, -1, -1, false, false);
            assertThat("empty", recordList.getColumnTypeMap(), is(Map.of("k", ColumnType.STRING)));
            assertThat("empty", recordList.getRecords(), empty());
        }

        @Test
        public void testQueryAll() {
            var recordList = this.memoryTable.query(null, null, null, -1, -1, false, false);
            assertThat("all",
                    recordList.getColumnTypeMap(),
                    is(this.memoryTable.getSchema().getColumnSchemas().stream()
                            .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::getType))));
            assertThat("all", recordList.getRecords(), is(this.records));
        }

        @Test
        public void testQueryColumnAliases() {
            var recordList = this.memoryTable.query(Map.of("a", "x", "d", "y"), null, null, -1, -1, false, false);
            assertThat("columns",
                    recordList.getColumnTypeMap(),
                    is(Map.of("x", ColumnType.BOOL, "y", ColumnType.INT32)));
            assertThat("columns",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("a", "x", "d", "y")),
                            recordList.getRecords()),
                    is(List.of(Map.of("y", 2),
                            Map.of("x", false, "y", 3),
                            Map.of("x", true, "y", 4),
                            Map.of("x", false, "y", 5),
                            Map.of("x", true, "y", 6),
                            Map.of("x", false, "y", 7),
                            Map.of("x", true, "y", 8),
                            Map.of("x", false),
                            Map.of("x", true, "y", 0),
                            Map.of("x", false, "y", 1))));
        }

        @Test
        public void testQueryColumnAliasesInvalid() {
            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.query(Map.of("x", "x"), null, null, -1, -1, false, false),
                    "invalid column");
        }

        @Test
        public void testQueryOrderBySingle() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "b", "b"),
                    List.of(new OrderByDesc("a")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "b", ColumnType.INT8)));
            assertThat("order by a",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("b", (byte) 0),
                            Map.of("a", false, "b", (byte) 1),
                            Map.of("a", false, "b", (byte) 3),
                            Map.of("a", false, "b", (byte) 5),
                            Map.of("a", false, "b", (byte) 7),
                            Map.of("a", false),
                            Map.of("a", true, "b", (byte) 2),
                            Map.of("a", true, "b", (byte) 4),
                            Map.of("a", true, "b", (byte) 6),
                            Map.of("a", true, "b", (byte) 8))));
        }

        @Test
        public void testQueryOrderByDescending() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "b", "b"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("b", true)),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "b", ColumnType.INT8)));
            assertThat("",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("b", (byte) 0),
                            Map.of("a", false, "b", (byte) 7),
                            Map.of("a", false, "b", (byte) 5),
                            Map.of("a", false, "b", (byte) 3),
                            Map.of("a", false, "b", (byte) 1),
                            Map.of("a", false),
                            Map.of("a", true, "b", (byte) 8),
                            Map.of("a", true, "b", (byte) 6),
                            Map.of("a", true, "b", (byte) 4),
                            Map.of("a", true, "b", (byte) 2))));
        }

        @Test
        public void testQueryOrderByInt8() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "b", "b"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("b")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,b",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "b", ColumnType.INT8)));
            assertThat("order by a,b",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("b", (byte) 0),
                            Map.of("a", false),
                            Map.of("a", false, "b", (byte) 1),
                            Map.of("a", false, "b", (byte) 3),
                            Map.of("a", false, "b", (byte) 5),
                            Map.of("a", false, "b", (byte) 7),
                            Map.of("a", true, "b", (byte) 2),
                            Map.of("a", true, "b", (byte) 4),
                            Map.of("a", true, "b", (byte) 6),
                            Map.of("a", true, "b", (byte) 8))));
        }

        @Test
        public void testQueryOrderByInt16() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "c", "c"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("c")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,c",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "c", ColumnType.INT16)));
            assertThat("order by a,c",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("c", (short) 1),
                            Map.of("a", false, "c", (short) 0),
                            Map.of("a", false, "c", (short) 2),
                            Map.of("a", false, "c", (short) 4),
                            Map.of("a", false, "c", (short) 6),
                            Map.of("a", false, "c", (short) 8),
                            Map.of("a", true),
                            Map.of("a", true, "c", (short) 3),
                            Map.of("a", true, "c", (short) 5),
                            Map.of("a", true, "c", (short) 7))));
        }

        @Test
        public void testQueryOrderByInt32() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "d", "d"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,d",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "d", ColumnType.INT32)));
            assertThat("order by a,d",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("d", 2),
                            Map.of("a", false),
                            Map.of("a", false, "d", 1),
                            Map.of("a", false, "d", 3),
                            Map.of("a", false, "d", 5),
                            Map.of("a", false, "d", 7),
                            Map.of("a", true, "d", 0),
                            Map.of("a", true, "d", 4),
                            Map.of("a", true, "d", 6),
                            Map.of("a", true, "d", 8))));
        }

        @Test
        public void testQueryOrderByInt64() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "e", "e"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("e")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,e",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "e", ColumnType.INT64)));
            assertThat("order by a,e",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("e", 3L),
                            Map.of("a", false, "e", 0L),
                            Map.of("a", false, "e", 2L),
                            Map.of("a", false, "e", 4L),
                            Map.of("a", false, "e", 6L),
                            Map.of("a", false, "e", 8L),
                            Map.of("a", true),
                            Map.of("a", true, "e", 1L),
                            Map.of("a", true, "e", 5L),
                            Map.of("a", true, "e", 7L))));
        }

        @Test
        public void testQueryOrderByFloat32() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "f", "f"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("f")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,f",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "f", ColumnType.FLOAT32)));
            assertThat("order by a,f",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("f", 4.f),
                            Map.of("a", false),
                            Map.of("a", false, "f", 1.f),
                            Map.of("a", false, "f", 3.f),
                            Map.of("a", false, "f", 5.f),
                            Map.of("a", false, "f", 7.f),
                            Map.of("a", true, "f", 0.f),
                            Map.of("a", true, "f", 2.f),
                            Map.of("a", true, "f", 6.f),
                            Map.of("a", true, "f", 8.f))));
        }

        @Test
        public void testQueryOrderByFloat64() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "g", "g"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("g")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,g",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "g", ColumnType.FLOAT64)));
            assertThat("order by a,g",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("g", 5.),
                            Map.of("a", false, "g", 0.),
                            Map.of("a", false, "g", 2.),
                            Map.of("a", false, "g", 4.),
                            Map.of("a", false, "g", 6.),
                            Map.of("a", false, "g", 8.),
                            Map.of("a", true),
                            Map.of("a", true, "g", 1.),
                            Map.of("a", true, "g", 3.),
                            Map.of("a", true, "g", 7.))));
        }

        @Test
        public void testQueryOrderByString() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "h", "h"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("h")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,h",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "h", ColumnType.STRING)));
            assertThat("order by a,h",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("h", "6"),
                            Map.of("a", false),
                            Map.of("a", false, "h", "1"),
                            Map.of("a", false, "h", "3"),
                            Map.of("a", false, "h", "5"),
                            Map.of("a", false, "h", "7"),
                            Map.of("a", true, "h", "0"),
                            Map.of("a", true, "h", "2"),
                            Map.of("a", true, "h", "4"),
                            Map.of("a", true, "h", "8"))));
        }

        @Test
        public void testQueryOrderByBytes() {
            var recordList = this.memoryTable.query(
                    Map.of("a", "a", "i", "i"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("i")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("order by a,i",
                    recordList.getColumnTypeMap(),
                    is(Map.of("a", ColumnType.BOOL, "i", ColumnType.BYTES)));
            assertThat("order by a,i",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(), recordList.getRecords()),
                    is(List.of(Map.of("i", ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", false, "i", ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", false, "i", ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", false, "i", ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", false, "i", ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", false, "i", ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", true),
                            Map.of("a", true, "i", ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", true, "i", ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8))),
                            Map.of("a", true, "i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))));
        }

        @Test
        public void testQueryOrderByMixed() {
            var recordList = this.memoryTable.query(
                    Map.of("d", "x"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat("aliases and order by",
                    recordList.getColumnTypeMap(),
                    is(Map.of("x", ColumnType.INT32)));
            assertThat("aliases and order by",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("d", "x")),
                            recordList.getRecords()),
                    is(List.of(Map.of("x", 2),
                            Map.of(),
                            Map.of("x", 1),
                            Map.of("x", 3),
                            Map.of("x", 5),
                            Map.of("x", 7),
                            Map.of("x", 0),
                            Map.of("x", 4),
                            Map.of("x", 6),
                            Map.of("x", 8))));
        }

        @Test
        public void testQueryOrderByInvalid() {
            assertThrows(SWValidationException.class,
                    () -> this.memoryTable.query(null, List.of(new OrderByDesc("x")), null, -1, -1, false, false),
                    "invalid order by column");
        }

        @Test
        public void testQueryFilterNullEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(new ArrayList<>() {{
                                add(new TableQueryFilter.Column("a"));
                                add(null);
                            }})
                            .build(),
                    -1,
                    -1,
                    false, false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 2))));
        }


        @Test
        public void testQueryFilterBoolEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("a"), true))
                            .build(),
                    -1,
                    -1,
                    false, false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(
                            Map.of("a", true, "d", 4),
                            Map.of("a", true, "d", 6),
                            Map.of("a", true, "d", 8),
                            Map.of("a", true, "d", 0))));
        }

        @Test
        public void testQueryFilterInt8Equal() {
            var columns = Map.of("b", "b");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("b", (byte) 5))));
        }

        @Test
        public void testQueryFilterInt16Equal() {
            var columns = Map.of("c", "c");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("c"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("c", (short) 5))));
        }

        @Test
        public void testQueryFilterInt32Equal() {
            var columns = Map.of("d", "d");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("d"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 5))));
        }

        @Test
        public void testQueryFilterInt64Equal() {
            var columns = Map.of("e", "e");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("e"), 5L))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("e", 5L))));
        }

        @Test
        public void testQueryFilterFloat32Equal() {
            var columns = Map.of("f", "f");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("f"), 5.f))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("f", 5.f))));
        }

        @Test
        public void testQueryFilterFloat64Equal() {
            var columns = Map.of("g", "g");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("g"), 5.))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("g", 5.))));
        }

        @Test
        public void testQueryFilterStringEqual() {
            var columns = Map.of("h", "h");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("h"), "5"))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("h", "5"))));
        }

        @Test
        public void testQueryFilterBytesEqual() {
            var columns = Map.of("i", "i");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))));
        }

        @Test
        public void testQueryFilterBoolLess() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("a"), true))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(
                            Map.of("a", false),
                            Map.of("a", false, "d", 1),
                            Map.of("a", false, "d", 3),
                            Map.of("a", false, "d", 5),
                            Map.of("a", false, "d", 7))));
        }

        @Test
        public void testQueryFilterInt8Less() {
            var columns = Map.of("b", "b");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("b"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("b", (byte) 0),
                            Map.of("b", (byte) 1),
                            Map.of("b", (byte) 2),
                            Map.of("b", (byte) 3),
                            Map.of("b", (byte) 4))));
        }

        @Test
        public void testQueryFilterInt16Less() {
            var columns = Map.of("c", "c");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("c"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("c", (short) 0),
                            Map.of("c", (short) 1),
                            Map.of("c", (short) 2),
                            Map.of("c", (short) 3),
                            Map.of("c", (short) 4))));
        }

        @Test
        public void testQueryFilterInt32Less() {
            var columns = Map.of("d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("d"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 0),
                            Map.of("d", 1),
                            Map.of("d", 2),
                            Map.of("d", 3),
                            Map.of("d", 4))));
        }

        @Test
        public void testQueryFilterInt64Less() {
            var columns = Map.of("e", "e");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("e"), 5L))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("e", 0L),
                            Map.of("e", 1L),
                            Map.of("e", 2L),
                            Map.of("e", 3L),
                            Map.of("e", 4L))));
        }

        @Test
        public void testQueryFilterFloat32Less() {
            var columns = Map.of("f", "f");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("f"), 5.f))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("f", 0.f),
                            Map.of("f", 1.f),
                            Map.of("f", 2.f),
                            Map.of("f", 3.f),
                            Map.of("f", 4.f))));
        }

        @Test
        public void testQueryFilterFloat64Less() {
            var columns = Map.of("g", "g");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("g"), 5.))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("g", 0.),
                            Map.of("g", 1.),
                            Map.of("g", 2.),
                            Map.of("g", 3.),
                            Map.of("g", 4.))));
        }

        @Test
        public void testQueryFilterStringLess() {
            var columns = Map.of("h", "h");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("h"), "5"))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("h", "0"),
                            Map.of("h", "1"),
                            Map.of("h", "2"),
                            Map.of("h", "3"),
                            Map.of("h", "4"))));
        }

        @Test
        public void testQueryFilterBytesLess() {
            var columns = Map.of("i", "i");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("i", ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8))))));
        }

        @Test
        public void testQueryFilterBoolLessEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("a"), true))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(
                            Map.of("a", false),
                            Map.of("a", true, "d", 0),
                            Map.of("a", false, "d", 1),
                            Map.of("a", false, "d", 3),
                            Map.of("a", true, "d", 4),
                            Map.of("a", false, "d", 5),
                            Map.of("a", true, "d", 6),
                            Map.of("a", false, "d", 7),
                            Map.of("a", true, "d", 8))));
        }

        @Test
        public void testQueryFilterInt8LessEqual() {
            var columns = Map.of("b", "b");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("b", (byte) 0),
                            Map.of("b", (byte) 1),
                            Map.of("b", (byte) 2),
                            Map.of("b", (byte) 3),
                            Map.of("b", (byte) 4),
                            Map.of("b", (byte) 5))));
        }

        @Test
        public void testQueryFilterInt16LessEqual() {
            var columns = Map.of("c", "c");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("c"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("c", (short) 0),
                            Map.of("c", (short) 1),
                            Map.of("c", (short) 2),
                            Map.of("c", (short) 3),
                            Map.of("c", (short) 4),
                            Map.of("c", (short) 5))));
        }

        @Test
        public void testQueryFilterInt32LessEqual() {
            var columns = Map.of("d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("d"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 0),
                            Map.of("d", 1),
                            Map.of("d", 2),
                            Map.of("d", 3),
                            Map.of("d", 4),
                            Map.of("d", 5))));
        }

        @Test
        public void testQueryFilterInt64LessEqual() {
            var columns = Map.of("e", "e");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("e"), 5L))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("e", 0L),
                            Map.of("e", 1L),
                            Map.of("e", 2L),
                            Map.of("e", 3L),
                            Map.of("e", 4L),
                            Map.of("e", 5L))));
        }

        @Test
        public void testQueryFilterFloat32LessEqual() {
            var columns = Map.of("f", "f");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("f"), 5.f))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("f", 0.f),
                            Map.of("f", 1.f),
                            Map.of("f", 2.f),
                            Map.of("f", 3.f),
                            Map.of("f", 4.f),
                            Map.of("f", 5.f))));
        }

        @Test
        public void testQueryFilterFloat64LessEqual() {
            var columns = Map.of("g", "g");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("g"), 5.))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("g", 0.),
                            Map.of("g", 1.),
                            Map.of("g", 2.),
                            Map.of("g", 3.),
                            Map.of("g", 4.),
                            Map.of("g", 5.))));
        }

        @Test
        public void testQueryFilterStringLessEqual() {
            var columns = Map.of("h", "h");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("h"), "5"))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("h", "0"),
                            Map.of("h", "1"),
                            Map.of("h", "2"),
                            Map.of("h", "3"),
                            Map.of("h", "4"),
                            Map.of("h", "5"))));
        }

        @Test
        public void testQueryFilterBytesLessEqual() {
            var columns = Map.of("i", "i");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("i", ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))));
        }

        @Test
        public void testQueryFilterBoolGreater() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("a"), false))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(
                            Map.of("a", true, "d", 0),
                            Map.of("a", true, "d", 4),
                            Map.of("a", true, "d", 6),
                            Map.of("a", true, "d", 8))));
        }

        @Test
        public void testQueryFilterInt8Greater() {
            var columns = Map.of("b", "b");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("b"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("b", (byte) 6),
                            Map.of("b", (byte) 7),
                            Map.of("b", (byte) 8))));
        }

        @Test
        public void testQueryFilterInt16Greater() {
            var columns = Map.of("c", "c");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("c"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("c", (short) 6),
                            Map.of("c", (short) 7),
                            Map.of("c", (short) 8))));
        }

        @Test
        public void testQueryFilterInt32Greater() {
            var columns = Map.of("d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("d"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 6),
                            Map.of("d", 7),
                            Map.of("d", 8))));
        }

        @Test
        public void testQueryFilterInt64Greater() {
            var columns = Map.of("e", "e");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("e"), 5L))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("e", 6L),
                            Map.of("e", 7L),
                            Map.of("e", 8L))));
        }

        @Test
        public void testQueryFilterFloat32Greater() {
            var columns = Map.of("f", "f");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("f"), 5.f))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("f", 6.f),
                            Map.of("f", 7.f),
                            Map.of("f", 8.f))));
        }

        @Test
        public void testQueryFilterFloat64Greater() {
            var columns = Map.of("g", "g");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("g"), 5.))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("g", 6.),
                            Map.of("g", 7.),
                            Map.of("g", 8.))));
        }

        @Test
        public void testQueryFilterStringGreater() {
            var columns = Map.of("h", "h");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("h"), "5"))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("h", "6"),
                            Map.of("h", "7"),
                            Map.of("h", "8"))));
        }

        @Test
        public void testQueryFilterBytesGreater() {
            var columns = Map.of("i", "i");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("i", ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8))))));
        }

        @Test
        public void testQueryFilterBoolGreaterEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("a"), false))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(
                            Map.of("a", false),
                            Map.of("a", true, "d", 0),
                            Map.of("a", false, "d", 1),
                            Map.of("a", false, "d", 3),
                            Map.of("a", true, "d", 4),
                            Map.of("a", false, "d", 5),
                            Map.of("a", true, "d", 6),
                            Map.of("a", false, "d", 7),
                            Map.of("a", true, "d", 8))));
        }

        @Test
        public void testQueryFilterInt8GreaterEqual() {
            var columns = Map.of("b", "b");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("b", (byte) 5),
                            Map.of("b", (byte) 6),
                            Map.of("b", (byte) 7),
                            Map.of("b", (byte) 8))));
        }

        @Test
        public void testQueryFilterInt16GreaterEqual() {
            var columns = Map.of("c", "c");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("c"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("c", (short) 5),
                            Map.of("c", (short) 6),
                            Map.of("c", (short) 7),
                            Map.of("c", (short) 8))));
        }

        @Test
        public void testQueryFilterInt32GreaterEqual() {
            var columns = Map.of("d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("d"), 5))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 5),
                            Map.of("d", 6),
                            Map.of("d", 7),
                            Map.of("d", 8))));
        }

        @Test
        public void testQueryFilterInt64GreaterEqual() {
            var columns = Map.of("e", "e");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("e"), 5L))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("e", 5L),
                            Map.of("e", 6L),
                            Map.of("e", 7L),
                            Map.of("e", 8L))));
        }

        @Test
        public void testQueryFilterFloat32GreaterEqual() {
            var columns = Map.of("f", "f");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("f"), 5.f))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("f", 5.f),
                            Map.of("f", 6.f),
                            Map.of("f", 7.f),
                            Map.of("f", 8.f))));
        }

        @Test
        public void testQueryFilterFloat64GreaterEqual() {
            var columns = Map.of("g", "g");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("g"), 5.))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("g", 5.),
                            Map.of("g", 6.),
                            Map.of("g", 7.),
                            Map.of("g", 8.))));
        }

        @Test
        public void testQueryFilterStringGreaterEqual() {
            var columns = Map.of("h", "h");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("h"), "5"))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("h", "5"),
                            Map.of("h", "6"),
                            Map.of("h", "7"),
                            Map.of("h", "8"))));
        }

        @Test
        public void testQueryFilterBytesGreaterEqual() {
            var columns = Map.of("i", "i");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8))),
                            Map.of("i", ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8))))));
        }

        @Test
        public void testQueryFilterNot() {
            var columns = Map.of("d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.NOT)
                            .operands(List.of(
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.EQUAL)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("d"),
                                                    5))
                                            .build()))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of(),
                            Map.of("d", 0),
                            Map.of("d", 1),
                            Map.of("d", 2),
                            Map.of("d", 3),
                            Map.of("d", 4),
                            Map.of("d", 6),
                            Map.of("d", 7),
                            Map.of("d", 8))));
        }

        @Test
        public void testQueryFilterAnd() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.AND)
                            .operands(List.of(
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.LESS)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("d"),
                                                    5))
                                            .build(),
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.EQUAL)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("a"),
                                                    true))
                                            .build()))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("a", true, "d", 0),
                            Map.of("a", true, "d", 4))));
        }

        @Test
        public void testQueryFilterOr() {
            var columns = Map.of("a", "a", "d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.OR)
                            .operands(List.of(
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.LESS)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("d"),
                                                    5))
                                            .build(),
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.EQUAL)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("a"),
                                                    true))
                                            .build()))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 2),
                            Map.of("a", false, "d", 1),
                            Map.of("a", false, "d", 3),
                            Map.of("a", true, "d", 0),
                            Map.of("a", true, "d", 4),
                            Map.of("a", true, "d", 6),
                            Map.of("a", true, "d", 8))));
        }

        @Test
        public void testQueryStartLimit() {
            var columns = Map.of("d", "d");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    null,
                    5,
                    2,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of("d", 4),
                            Map.of("d", 5))));
        }

        @Test
        public void testQueryUnknown() {
            var columns = Map.of("z", "z");
            var records = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("z")),
                    null,
                    5,
                    2,
                    false,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(Map.of(), Map.of())));
        }

        @Test
        public void testQueryKeepNone() {
            var columns = Map.of("a", "a");
            var records = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), 0))
                            .build(),
                    -1,
                    -1,
                    true,
                    false);
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(columns), records.getRecords()),
                    is(List.of(new HashMap<String, Object>() {{
                        put("a", null);
                    }})));
        }

        @Test
        public void testScanInitialEmptyTable() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            var it = this.memoryTable.scan(null, null, false, null, false, false, false);
            var recordList = MemoryTableImplTest.getRecords(it);
            assertThat("empty", it.getColumnTypeMapping(), nullValue());
            assertThat("empty", recordList, empty());
        }

        @Test
        public void testScanEmptyTableWithSchema() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(new TableSchemaDesc("k", List.of(new ColumnSchemaDesc("k", "STRING"))),
                    List.of(Map.of("k", "0", "-", "1")));
            var it = this.memoryTable.scan(null, null, false, null, false, false, false);
            var recordList = MemoryTableImplTest.getRecords(it);
            assertThat("empty", it.getColumnTypeMapping(), is(Map.of("k", ColumnType.STRING)));
            assertThat("empty", recordList, empty());
        }

        @Test
        public void testScanAll() {
            var it = this.memoryTable.scan(null, null, false, null, false, false, false);
            var recordList = MemoryTableImplTest.getRecords(it);
            assertThat("all",
                    it.getColumnTypeMapping(),
                    is(this.memoryTable.getSchema().getColumnSchemas().stream()
                            .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::getType))));
            assertThat("all", recordList, is(this.records));
        }

        @Test
        public void testScanColumnAliases() {
            var it = this.memoryTable.scan(Map.of("a", "x", "d", "y"), null, false, null, false, false, false);
            var recordList = MemoryTableImplTest.getRecords(it);
            assertThat("columns",
                    it.getColumnTypeMapping(),
                    is(Map.of("x", ColumnType.BOOL, "y", ColumnType.INT32)));
            assertThat("columns",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("a", "x", "d", "y")),
                            recordList),
                    is(List.of(Map.of("y", 2),
                            Map.of("x", false, "y", 3),
                            Map.of("x", true, "y", 4),
                            Map.of("x", false, "y", 5),
                            Map.of("x", true, "y", 6),
                            Map.of("x", false, "y", 7),
                            Map.of("x", true, "y", 8),
                            Map.of("x", false),
                            Map.of("x", true, "y", 0),
                            Map.of("x", false, "y", 1))));
        }

        @Test
        public void testScanStartEnd() {
            assertThat("start,non-inclusive",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("d", "d")),
                            MemoryTableImplTest.getRecords(
                                    this.memoryTable.scan(Map.of("d", "d"), "5", false, null, false, false, false))),
                    is(List.of(Map.of("d", 8),
                            Map.of(),
                            Map.of("d", 0),
                            Map.of("d", 1))));

            assertThat("start,inclusive",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("d", "d")),
                            MemoryTableImplTest.getRecords(
                                    this.memoryTable.scan(Map.of("d", "d"), "5", true, null, false, false, false))),
                    is(List.of(Map.of("d", 7),
                            Map.of("d", 8),
                            Map.of(),
                            Map.of("d", 0),
                            Map.of("d", 1))));

            assertThat("end,non-inclusive",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("d", "d")),
                            MemoryTableImplTest.getRecords(
                                    this.memoryTable.scan(Map.of("d", "d"), null, false, "5", false, false, false))),
                    is(List.of(Map.of("d", 2),
                            Map.of("d", 3),
                            Map.of("d", 4),
                            Map.of("d", 5),
                            Map.of("d", 6))));

            assertThat("end,inclusive",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("d", "d")),
                            MemoryTableImplTest.getRecords(
                                    this.memoryTable.scan(Map.of("d", "d"), null, false, "5", true, false, false))),
                    is(List.of(Map.of("d", 2),
                            Map.of("d", 3),
                            Map.of("d", 4),
                            Map.of("d", 5),
                            Map.of("d", 6),
                            Map.of("d", 7))));

            assertThat("start+end",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("d", "d")),
                            MemoryTableImplTest.getRecords(
                                    this.memoryTable.scan(Map.of("d", "d"), "2", true, "5", false, false, false))),
                    is(List.of(Map.of("d", 4),
                            Map.of("d", 5),
                            Map.of("d", 6))));
        }

        @Test
        public void testScanKeepNone() {
            assertThat("test",
                    decodeRecords(this.memoryTable.getSchema().getColumnTypeMapping(Map.of("d", "d")),
                            MemoryTableImplTest.getRecords(
                                    this.memoryTable.scan(Map.of("d", "d"), null, false, null, false, true, false))),
                    is(List.of(Map.of("d", 2),
                            Map.of("d", 3),
                            Map.of("d", 4),
                            Map.of("d", 5),
                            Map.of("d", 6),
                            Map.of("d", 7),
                            Map.of("d", 8),
                            new HashMap<String, Integer>() {{
                                put("d", null);
                            }},
                            Map.of("d", 0),
                            Map.of("d", 1))));
        }

        @Test
        public void testScanUnknown() {
            assertThat("test",
                    MemoryTableImplTest.getRecords(
                            this.memoryTable.scan(Map.of("z", "z"), null, false, null, false, false, false)),
                    is(List.of(Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of())));
        }
    }
}