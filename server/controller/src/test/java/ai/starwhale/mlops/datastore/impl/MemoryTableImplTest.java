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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.ColumnTypeScalar;
import ai.starwhale.mlops.datastore.MemoryTable;
import ai.starwhale.mlops.datastore.ObjectStore;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableQueryFilter.Operator;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.WalManager;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.memory.impl.SwByteBufferManager;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MemoryTableImplTest {

    private static List<MemoryTable.RecordResult> scanAll(MemoryTable memoryTable,
            List<String> columns,
            boolean keepNone) {
        return memoryTable.scan(columns.stream().collect(Collectors.toMap(Function.identity(), Function.identity())),
                null,
                true,
                null,
                false,
                1000,
                keepNone);
    }

    @TempDir
    private File rootDir;

    private WalManager walManager;

    @BeforeEach
    public void setUp() throws IOException {
        var bufferManager = new SwByteBufferManager();
        var objectStore = new ObjectStore(bufferManager, new StorageAccessServiceFile(this.rootDir.getAbsolutePath()));
        this.walManager = new WalManager(objectStore, bufferManager, 256, 4096, "test/", 10, 3);
    }

    @AfterEach
    public void tearDown() throws IOException {
        walManager.terminate();
        for (var f : Objects.requireNonNull(new File(System.getProperty("java.io.tmpdir"))
                .listFiles((FileFilter) new PrefixFileFilter("junit")))) {
            if (!rootDir.equals(f)) {
                System.out.println("deleting" + f);
                FileUtils.deleteDirectory(f);
            }
        }
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
                            ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                            ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                    List.of(Map.of("k", "0", "a", "a")));
            assertThat("init",
                    scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a", 10))));

            this.memoryTable.update(null, List.of(Map.of("k", "1", "a", "b")));
            assertThat("insert", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a", 10)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "a", 11))));

            this.memoryTable.update(
                    null,
                    List.of(Map.of("k", "2", "a", "c"),
                            Map.of("k", "3", "a", "d")));
            assertThat("insert multiple", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a", 10)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "a", 11)),
                            new MemoryTable.RecordResult("2", Map.of("k", "2", "a", 12)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 13))));

            this.memoryTable.update(null, List.of(Map.of("k", "1", "a", "c")));
            assertThat("overwrite", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a", 10)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "a", 12)),
                            new MemoryTable.RecordResult("2", Map.of("k", "2", "a", 12)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 13))));

            this.memoryTable.update(null, List.of(Map.of("k", "2", "-", "1")));
            assertThat("delete", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a", 10)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "a", 12)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 13))));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("b").type("INT32").build())),
                    List.of(Map.of("k", "1", "b", "0")));
            assertThat("new column", scanAll(this.memoryTable, List.of("k", "a", "b"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a", 10)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "a", 12, "b", 0)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 13))));

            this.memoryTable.update(
                    null,
                    List.of(new HashMap<>() {
                                {
                                    put("k", "1");
                                    put("a", null);
                                }
                            },
                            new HashMap<>() {
                                {
                                    put("k", "2");
                                    put("a", null);
                                }
                            },
                            new HashMap<>() {
                                {
                                    put("k", "3");
                                    put("b", null);
                                }
                            }));
            assertThat("null value", scanAll(this.memoryTable, List.of("k", "a", "b"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a", 10)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "b", 0)),
                            new MemoryTable.RecordResult("2", Map.of("k", "2")),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 13))));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("c").type("INT32").build())),
                    List.of(Map.of("k", "3", "-", "1"),
                            Map.of("k", "2", "a", "0"),
                            Map.of("k", "3", "a", "0"),
                            Map.of("k", "4", "c", "0"),
                            new HashMap<>() {
                                {
                                    put("k", "1");
                                    put("b", null);
                                    put("c", "1");
                                }
                            },
                            Map.of("k", "0", "-", "1"),
                            Map.of("k", "2", "-", "1")));
            assertThat("mixed", scanAll(this.memoryTable, List.of("k", "a", "b", "c"), false),
                    contains(new MemoryTable.RecordResult("1", Map.of("k", "1", "c", 1)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 0)),
                            new MemoryTable.RecordResult("4", Map.of("k", "4", "c", 0))));

            this.memoryTable.update(
                    new TableSchemaDesc(null,
                            List.of(ColumnSchemaDesc.builder().name("a-b/c/d:e_f").type("INT32").build())),
                    List.of(Map.of("k", "0", "a-b/c/d:e_f", "0")));
            assertThat("complex name", scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a-b/c/d:e_f", 0)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "c", 1)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 0)),
                            new MemoryTable.RecordResult("4", Map.of("k", "4", "c", 0))));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build())),
                    List.of(new HashMap<>() {
                        {
                            put("k", "0");
                            put("x", null);
                        }
                    }));
            assertThat("unknown",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnTypeScalar.UNKNOWN));
            assertThat("unknown", scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a-b/c/d:e_f", 0)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "c", 1)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 0)),
                            new MemoryTable.RecordResult("4", Map.of("k", "4", "c", 0))));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("x").type("INT32").build())),
                    List.of(Map.of("k", "1", "x", "1")));
            assertThat("update unknown",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnTypeScalar.INT32));
            assertThat("update unknown",
                    scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f", "x"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a-b/c/d:e_f", 0)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "c", 1, "x", 1)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 0)),
                            new MemoryTable.RecordResult("4", Map.of("k", "4", "c", 0))));

            this.memoryTable.update(
                    new TableSchemaDesc(null, List.of(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build())),
                    List.of(new HashMap<>() {
                        {
                            put("k", "1");
                            put("x", null);
                        }
                    }));
            assertThat("unknown again",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnTypeScalar.INT32));
            assertThat("unknown again",
                    scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f", "x"), false),
                    contains(new MemoryTable.RecordResult("0", Map.of("k", "0", "a-b/c/d:e_f", 0)),
                            new MemoryTable.RecordResult("1", Map.of("k", "1", "c", 1)),
                            new MemoryTable.RecordResult("3", Map.of("k", "3", "a", 0)),
                            new MemoryTable.RecordResult("4", Map.of("k", "4", "c", 0))));
        }

        @Test
        public void testUpdateAllColumnTypes() {
            this.memoryTable.update(
                    new TableSchemaDesc("key", List.of(
                            ColumnSchemaDesc.builder().name("key").type("STRING").build(),
                            ColumnSchemaDesc.builder().name("a").type("BOOL").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT8").build(),
                            ColumnSchemaDesc.builder().name("c").type("INT16").build(),
                            ColumnSchemaDesc.builder().name("d").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("e").type("INT64").build(),
                            ColumnSchemaDesc.builder().name("f").type("FLOAT32").build(),
                            ColumnSchemaDesc.builder().name("g").type("FLOAT64").build(),
                            ColumnSchemaDesc.builder().name("h").type("BYTES").build(),
                            ColumnSchemaDesc.builder().name("i").type("UNKNOWN").build(),
                            ColumnSchemaDesc.builder().name("j")
                                    .type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("k")
                                    .type("OBJECT")
                                    .pythonType("t")
                                    .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                            ColumnSchemaDesc.builder().name("b").type("INT32").build()))
                                    .build(),
                            ColumnSchemaDesc.builder().name("l").type("FLOAT64").build())),

                    List.of(new HashMap<>() {
                        {
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
                            put("j", List.of("a"));
                            put("k", Map.of("a", "b", "b", "c"));
                            put("l", Long.toHexString(Double.doubleToLongBits(0.0)));
                        }
                    }));
            assertThat("all types",
                    scanAll(this.memoryTable,
                        List.of("key", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"),
                        false),
                    contains(new MemoryTable.RecordResult("x",
                            new HashMap<>() {
                                {
                                    put("key", "x");
                                    put("a", true);
                                    put("b", (byte) 16);
                                    put("c", Short.parseShort("1000", 16));
                                    put("d", Integer.parseInt("100000", 16));
                                    put("e", Long.parseLong("10000000", 16));
                                    put("f", 1.1f);
                                    put("g", 1.1);
                                    put("h", ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
                                    put("j", List.of(10));
                                    put("k", Map.of("a", 11, "b", 12));
                                    put("l", 0.0);
                                }
                            })));
        }

        @Test
        public void testUpdateAllKeyColumnTypes() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("BOOL").build())),
                    List.of(Map.of("k", "1")));
            assertThat("bool",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult(true, Map.of("k", true))));

            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT8").build())),
                    List.of(Map.of("k", "10")));
            assertThat("int8",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult((byte) 16, Map.of("k", (byte) 16))));

            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT16").build())),
                    List.of(Map.of("k", "1000")));
            assertThat("int16",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult(Short.parseShort("1000", 16),
                            Map.of("k", Short.parseShort("1000", 16)))));

            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build())),
                    List.of(Map.of("k", "100000")));
            assertThat("int32",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult(Integer.parseInt("100000", 16),
                            Map.of("k", Integer.parseInt("100000", 16)))));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT64").build())),
                    List.of(Map.of("k", "10000000")));
            assertThat("int64",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult(Long.parseLong("10000000", 16),
                            Map.of("k", Long.parseLong("10000000", 16)))));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("FLOAT32").build())),
                    List.of(Map.of("k", Integer.toHexString(Float.floatToIntBits(1.1f)))));
            assertThat("float32", scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult(1.1f,
                            Map.of("k", 1.1f))));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("FLOAT64").build())),
                    List.of(Map.of("k", Long.toHexString(Double.doubleToLongBits(1.1)))));
            assertThat("float64", scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult(1.1,
                            Map.of("k", 1.1))));

            this.memoryTable = new MemoryTableImpl("", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("BYTES").build())),
                    List.of(Map.of("k", Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)))));
            assertThat("bytes", scanAll(this.memoryTable, List.of("k"), false),
                    contains(new MemoryTable.RecordResult(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)),
                            Map.of("k", ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))))));
        }

        @Test
        public void testUpdateExceptions() {
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(null, null),
                    "null schema");

            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(
                            new TableSchemaDesc("k",
                                    List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                            List.of(Map.of("k", "0"), Map.of("k", "1"))),
                    "no key column schema");
            assertThat("no key column schema", scanAll(this.memoryTable, List.of("k", "a"), false), empty());

            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(
                            new TableSchemaDesc("k",
                                    List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                            List.of(Map.of("k", "0"), Map.of("k", "1", "a", "1"))),
                    "extra column data");
            assertThat("extra column data", scanAll(this.memoryTable, List.of("k", "a"), false), empty());

            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(
                            new TableSchemaDesc("k", List.of(
                                    ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                    ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                            List.of(Map.of("k", "0"), Map.of("k", "1", "a", "h"))),
                    "fail to decode");
            assertThat("fail to decode", scanAll(this.memoryTable, List.of("k", "a"), false), empty());
        }

        @Test
        public void testUpdateWalError() {
            var schema = new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build()));
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(
                            schema,
                            List.of(Map.of("k", "a".repeat(5000)))),
                    "huge entry");
            assertThat("null", this.memoryTable.getSchema(), nullValue());
            this.memoryTable.update(schema, null);
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(
                            null,
                            List.of(Map.of("k", "a".repeat(5000)))),
                    "huge entry");
            assertThat("schema", this.memoryTable.getSchema(), is(new TableSchema(schema)));
            assertThat("records", scanAll(this.memoryTable, List.of("k"), false), empty());
        }

        @Test
        public void testUpdateFromWal() throws IOException {
            this.memoryTable.update(
                    new TableSchemaDesc("key", List.of(
                            ColumnSchemaDesc.builder().name("key").type("STRING").build(),
                            ColumnSchemaDesc.builder().name("a").type("BOOL").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT8").build(),
                            ColumnSchemaDesc.builder().name("c").type("INT16").build(),
                            ColumnSchemaDesc.builder().name("d").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("e").type("INT64").build(),
                            ColumnSchemaDesc.builder().name("f").type("FLOAT32").build(),
                            ColumnSchemaDesc.builder().name("g").type("FLOAT64").build(),
                            ColumnSchemaDesc.builder().name("h").type("BYTES").build(),
                            ColumnSchemaDesc.builder().name("i").type("UNKNOWN").build())),
                    null);
            List<Map<String, Object>> records = new ArrayList<>();
            for (int i = 0; i < 100; ++i) {
                final int index = i;
                records.add(new HashMap<>() {
                    {
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
                    }
                });
            }
            this.memoryTable.update(null, records);
            MemoryTableImplTest.this.walManager.terminate();
            SwBufferManager bufferManager = new SwByteBufferManager();
            var objectStore = new ObjectStore(bufferManager,
                    new StorageAccessServiceFile(MemoryTableImplTest.this.rootDir.getAbsolutePath()));
            MemoryTableImplTest.this.walManager = new WalManager(objectStore, bufferManager, 256, 4096, "test/", 10, 3);
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            var it = MemoryTableImplTest.this.walManager.readAll();
            while (it.hasNext()) {
                this.memoryTable.updateFromWal(it.next());
            }
            assertThat(scanAll(this.memoryTable, List.of("key", "a", "b", "c", "d", "e", "f", "g", "h", "i"), true),
                    is(IntStream.range(0, 100)
                            .mapToObj(index -> new MemoryTable.RecordResult(
                                    String.format("%03d", index),
                                    new HashMap<>() {
                                        {
                                            put("key", String.format("%03d", index));
                                            put("a", index % 2 == 1);
                                            put("b", (byte) (index + 10));
                                            put("c", (short) (index + 1000));
                                            put("d", index + 100000);
                                            put("e", index + 10000000L);
                                            put("f", index + 0.1f);
                                            put("g", index + 0.1);
                                            put("h",
                                                    ByteBuffer.wrap(("test" + index).getBytes(StandardCharsets.UTF_8)));
                                            put("i", null);
                                        }
                                    }))
                            .collect(Collectors.toList())));
        }
    }

    @Nested
    public class QueryScanTest {

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
            var records = IntStream.rangeClosed(0, 9).mapToObj(
                            i -> {
                                Map<String, Object> values = new HashMap<>();
                                values.put("key", i);
                                if (data[0][i] != null) {
                                    values.put("a", data[0][i]);
                                }
                                if (data[1][i] != null) {
                                    values.put("b", data[1][i]);
                                }
                                if (data[2][i] != null) {
                                    values.put("c", data[2][i]);
                                }
                                if (data[3][i] != null) {
                                    values.put("d", data[3][i]);
                                }
                                if (data[4][i] != null) {
                                    values.put("e", data[4][i]);
                                }
                                if (data[5][i] != null) {
                                    values.put("f", data[5][i]);
                                }
                                if (data[6][i] != null) {
                                    values.put("g", data[6][i]);
                                }
                                if (data[7][i] != null) {
                                    values.put("h", data[7][i]);
                                }
                                if (data[8][i] != null) {
                                    values.put("i", ByteBuffer.wrap(
                                            ((String) data[8][i]).getBytes(StandardCharsets.UTF_8)));
                                }
                                return new MemoryTable.RecordResult(Integer.toHexString(i), values);
                            })
                    .collect(Collectors.toList());
            var schema = new TableSchemaDesc("key", List.of(
                    ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                    ColumnSchemaDesc.builder().name("a").type("BOOL").build(),
                    ColumnSchemaDesc.builder().name("b").type("INT8").build(),
                    ColumnSchemaDesc.builder().name("c").type("INT16").build(),
                    ColumnSchemaDesc.builder().name("d").type("INT32").build(),
                    ColumnSchemaDesc.builder().name("e").type("INT64").build(),
                    ColumnSchemaDesc.builder().name("f").type("FLOAT32").build(),
                    ColumnSchemaDesc.builder().name("g").type("FLOAT64").build(),
                    ColumnSchemaDesc.builder().name("h").type("STRING").build(),
                    ColumnSchemaDesc.builder().name("i").type("BYTES").build(),
                    ColumnSchemaDesc.builder().name("z").type("UNKNOWN").build()));
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(schema,
                    records.stream()
                            .map(r -> r.getValues().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> ColumnTypeScalar.getColumnTypeByName(
                                                    schema.getColumnSchemaList().stream()
                                                            .filter(col -> col.getName().equals(entry.getKey()))
                                                            .findFirst()
                                                            .orElseThrow()
                                                            .getType())
                                            .encode(entry.getValue(), false))))
                            .collect(Collectors.toList()));
        }

        private final Map<Class<?>, ColumnType> typeMap = Map.of(
                Boolean.class, ColumnTypeScalar.BOOL,
                Byte.class, ColumnTypeScalar.INT8,
                Short.class, ColumnTypeScalar.INT16,
                Integer.class, ColumnTypeScalar.INT32,
                Long.class, ColumnTypeScalar.INT64,
                Float.class, ColumnTypeScalar.FLOAT32,
                Double.class, ColumnTypeScalar.FLOAT64,
                String.class, ColumnTypeScalar.STRING);

        private ColumnType getColumnType(Object value) {
            if (value == null) {
                return ColumnTypeScalar.UNKNOWN;
            }
            var ret = typeMap.get(value.getClass());
            if (ret == null) {
                if (value instanceof ByteBuffer) {
                    return ColumnTypeScalar.BYTES;
                }
                throw new IllegalArgumentException("unsupported column type " + value.getClass());
            }
            return ret;
        }

        private TableQueryFilter.Constant createConstant(Object value) {
            return new TableQueryFilter.Constant(this.getColumnType(value), value);
        }

        @Test
        public void testQueryInitialEmptyTable() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            var results = this.memoryTable.query(Map.of("a", "a"), null, null, -1, -1, false, false);
            assertThat(results, empty());
        }

        @Test
        public void testQueryEmptyTableWithSchema() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                    List.of(Map.of("k", "0", "-", "1")));
            var results = this.memoryTable.query(Map.of("k", "k"), null, null, -1, -1, false, false);
            assertThat(results, empty());
        }

        @Test
        public void testQueryColumnAliases() {
            var results = this.memoryTable.query(Map.of("a", "x", "d", "y"), null, null, -1, -1, false, false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("y", 2)),
                            new MemoryTable.RecordResult(1, Map.of("x", false, "y", 3)),
                            new MemoryTable.RecordResult(2, Map.of("x", true, "y", 4)),
                            new MemoryTable.RecordResult(3, Map.of("x", false, "y", 5)),
                            new MemoryTable.RecordResult(4, Map.of("x", true, "y", 6)),
                            new MemoryTable.RecordResult(5, Map.of("x", false, "y", 7)),
                            new MemoryTable.RecordResult(6, Map.of("x", true, "y", 8)),
                            new MemoryTable.RecordResult(7, Map.of("x", false)),
                            new MemoryTable.RecordResult(8, Map.of("x", true, "y", 0)),
                            new MemoryTable.RecordResult(9, Map.of("x", false, "y", 1)))));
        }

        @Test
        public void testQueryColumnAliasesInvalid() {
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.query(Map.of("x", "x"), null, null, -1, -1, false, false),
                    "invalid column");
        }

        @Test
        public void testQueryNonScalar() {
            this.memoryTable.update(
                    new TableSchemaDesc(null,
                            List.of(ColumnSchemaDesc.builder()
                                    .name("x")
                                    .type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                    .build())),
                    List.of(Map.of("key", "1", "x", List.of("a"))));
            var results = this.memoryTable.query(
                    Map.of("key", "key", "x", "x"),
                    null,
                    TableQueryFilter.builder()
                            .operator(Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("key"), this.createConstant(1)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(1, Map.of("key", 1, "x", List.of(10))))));
        }

        @Test
        public void testQueryOrderBySingle() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "b", "b"),
                    List.of(new OrderByDesc("a")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("b", (byte) 0)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "b", (byte) 1)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "b", (byte) 3)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "b", (byte) 5)),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "b", (byte) 7)),
                            new MemoryTable.RecordResult(9, Map.of("a", false)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "b", (byte) 2)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "b", (byte) 4)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "b", (byte) 6)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "b", (byte) 8)))));
        }

        @Test
        public void testQueryOrderByDescending() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "b", "b"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("b", true)),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("b", (byte) 0)),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "b", (byte) 7)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "b", (byte) 5)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "b", (byte) 3)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "b", (byte) 1)),
                            new MemoryTable.RecordResult(9, Map.of("a", false)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "b", (byte) 8)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "b", (byte) 6)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "b", (byte) 4)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "b", (byte) 2)))));
        }

        @Test
        public void testQueryOrderByInt8() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "b", "b"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("b")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("b", (byte) 0)),
                            new MemoryTable.RecordResult(9, Map.of("a", false)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "b", (byte) 1)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "b", (byte) 3)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "b", (byte) 5)),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "b", (byte) 7)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "b", (byte) 2)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "b", (byte) 4)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "b", (byte) 6)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "b", (byte) 8)))));
        }

        @Test
        public void testQueryOrderByInt16() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "c", "c"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("c")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("c", (short) 1)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "c", (short) 0)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "c", (short) 2)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "c", (short) 4)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "c", (short) 6)),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "c", (short) 8)),
                            new MemoryTable.RecordResult(8, Map.of("a", true)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "c", (short) 3)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "c", (short) 5)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "c", (short) 7)))));
        }

        @Test
        public void testQueryOrderByInt32() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "d", "d"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(7, Map.of("a", false)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "d", 1)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "d", 3)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "d", 5)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "d", 7)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "d", 0)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "d", 4)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "d", 6)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "d", 8)))));
        }

        @Test
        public void testQueryOrderByInt64() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "e", "e"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("e")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("e", 3L)),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "e", 0L)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "e", 2L)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "e", 4L)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "e", 6L)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "e", 8L)),
                            new MemoryTable.RecordResult(6, Map.of("a", true)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "e", 1L)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "e", 5L)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "e", 7L)))));
        }

        @Test
        public void testQueryOrderByFloat32() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "f", "f"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("f")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("f", 4.f)),
                            new MemoryTable.RecordResult(5, Map.of("a", false)),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "f", 1.f)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "f", 3.f)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "f", 5.f)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "f", 7.f)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "f", 0.f)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "f", 2.f)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "f", 6.f)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "f", 8.f)))));
        }

        @Test
        public void testQueryOrderByFloat64() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "g", "g"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("g")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("g", 5.)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "g", 0.)),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "g", 2.)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "g", 4.)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "g", 6.)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "g", 8.)),
                            new MemoryTable.RecordResult(4, Map.of("a", true)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "g", 1.)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "g", 3.)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "g", 7.)))));
        }

        @Test
        public void testQueryOrderByString() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "h", "h"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("h")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("h", "6")),
                            new MemoryTable.RecordResult(3, Map.of("a", false)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "h", "1")),
                            new MemoryTable.RecordResult(7, Map.of("a", false, "h", "3")),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "h", "5")),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "h", "7")),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "h", "0")),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "h", "2")),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "h", "4")),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "h", "8")))));
        }

        @Test
        public void testQueryOrderByBytes() {
            var results = this.memoryTable.query(
                    Map.of("a", "a", "i", "i"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("i")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0,
                                    Map.of("i", ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(3,
                                    Map.of("a", false, "i", ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(5,
                                    Map.of("a", false, "i", ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(7,
                                    Map.of("a", false, "i", ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(9,
                                    Map.of("a", false, "i", ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(1,
                                    Map.of("a", false, "i", ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(2, Map.of("a", true)),
                            new MemoryTable.RecordResult(4,
                                    Map.of("a", true, "i", ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(6,
                                    Map.of("a", true, "i", ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(8,
                                    Map.of("a", true, "i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))));
        }

        @Test
        public void testQueryOrderByMixed() {
            var results = this.memoryTable.query(
                    Map.of("d", "x"),
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    null,
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("x", 2)),
                            new MemoryTable.RecordResult(7, Map.of()),
                            new MemoryTable.RecordResult(9, Map.of("x", 1)),
                            new MemoryTable.RecordResult(1, Map.of("x", 3)),
                            new MemoryTable.RecordResult(3, Map.of("x", 5)),
                            new MemoryTable.RecordResult(5, Map.of("x", 7)),
                            new MemoryTable.RecordResult(8, Map.of("x", 0)),
                            new MemoryTable.RecordResult(2, Map.of("x", 4)),
                            new MemoryTable.RecordResult(4, Map.of("x", 6)),
                            new MemoryTable.RecordResult(6, Map.of("x", 8)))));
        }

        @Test
        public void testQueryOrderByInvalid() {
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.query(Map.of("a", "a"),
                            List.of(new OrderByDesc("x")),
                            null,
                            -1,
                            -1,
                            false,
                            false),
                    "invalid order by column");

            this.memoryTable.update(
                    new TableSchemaDesc(null,
                            List.of(ColumnSchemaDesc.builder()
                                    .name("x")
                                    .type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                    .build())),
                    List.of(Map.of("key", "1", "x", List.of("a"))));
        }

        @Test
        public void testQueryFilterNullEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(new ArrayList<>() {
                                {
                                    add(new TableQueryFilter.Column("a"));
                                    add(createConstant(null));
                                }
                            })
                            .build(),
                    -1,
                    -1,
                    false, false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("d", 2)))));
        }


        @Test
        public void testQueryFilterBoolEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("a"), createConstant(true)))
                            .build(),
                    -1,
                    -1,
                    false, false);
            assertThat(results,
                    is(List.of(
                            new MemoryTable.RecordResult(2, Map.of("a", true, "d", 4)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "d", 6)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "d", 8)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "d", 0)))));
        }

        @Test
        public void testQueryFilterInt8Equal() {
            var columns = Map.of("b", "b");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(5, Map.of("b", (byte) 5)))));
        }

        @Test
        public void testQueryFilterInt16Equal() {
            var columns = Map.of("c", "c");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(4, Map.of("c", (short) 5)))));
        }

        @Test
        public void testQueryFilterInt32Equal() {
            var columns = Map.of("d", "d");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(3, Map.of("d", 5)))));
        }

        @Test
        public void testQueryFilterInt64Equal() {
            var columns = Map.of("e", "e");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(2, Map.of("e", 5L)))));
        }

        @Test
        public void testQueryFilterFloat32Equal() {
            var columns = Map.of("f", "f");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(1, Map.of("f", 5.f)))));
        }

        @Test
        public void testQueryFilterFloat64Equal() {
            var columns = Map.of("g", "g");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(0, Map.of("g", 5.)))));
        }

        @Test
        public void testQueryFilterStringEqual() {
            var columns = Map.of("h", "h");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(9, Map.of("h", "5")))));
        }

        @Test
        public void testQueryFilterBytesEqual() {
            var columns = Map.of("i", "i");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(8,
                            Map.of("i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))));
        }

        @Test
        public void testQueryFilterBoolLess() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("a"), createConstant(true)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(
                            new MemoryTable.RecordResult(7, Map.of("a", false)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "d", 1)),
                            new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "d", 3)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "d", 5)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "d", 7)))));
        }

        @Test
        public void testQueryFilterInt8Less() {
            var columns = Map.of("b", "b");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(9, Map.of()),
                            new MemoryTable.RecordResult(0, Map.of("b", (byte) 0)),
                            new MemoryTable.RecordResult(1, Map.of("b", (byte) 1)),
                            new MemoryTable.RecordResult(2, Map.of("b", (byte) 2)),
                            new MemoryTable.RecordResult(3, Map.of("b", (byte) 3)),
                            new MemoryTable.RecordResult(4, Map.of("b", (byte) 4)))));
        }

        @Test
        public void testQueryFilterInt16Less() {
            var columns = Map.of("c", "c");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(8, Map.of()),
                            new MemoryTable.RecordResult(9, Map.of("c", (short) 0)),
                            new MemoryTable.RecordResult(0, Map.of("c", (short) 1)),
                            new MemoryTable.RecordResult(1, Map.of("c", (short) 2)),
                            new MemoryTable.RecordResult(2, Map.of("c", (short) 3)),
                            new MemoryTable.RecordResult(3, Map.of("c", (short) 4)))));
        }

        @Test
        public void testQueryFilterInt32Less() {
            var columns = Map.of("d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(7, Map.of()),
                            new MemoryTable.RecordResult(8, Map.of("d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("d", 1)),
                            new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("d", 4)))));
        }

        @Test
        public void testQueryFilterInt64Less() {
            var columns = Map.of("e", "e");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(6, Map.of()),
                            new MemoryTable.RecordResult(7, Map.of("e", 0L)),
                            new MemoryTable.RecordResult(8, Map.of("e", 1L)),
                            new MemoryTable.RecordResult(9, Map.of("e", 2L)),
                            new MemoryTable.RecordResult(0, Map.of("e", 3L)),
                            new MemoryTable.RecordResult(1, Map.of("e", 4L)))));
        }

        @Test
        public void testQueryFilterFloat32Less() {
            var columns = Map.of("f", "f");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(5, Map.of()),
                            new MemoryTable.RecordResult(6, Map.of("f", 0.f)),
                            new MemoryTable.RecordResult(7, Map.of("f", 1.f)),
                            new MemoryTable.RecordResult(8, Map.of("f", 2.f)),
                            new MemoryTable.RecordResult(9, Map.of("f", 3.f)),
                            new MemoryTable.RecordResult(0, Map.of("f", 4.f)))));
        }

        @Test
        public void testQueryFilterFloat64Less() {
            var columns = Map.of("g", "g");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(4, Map.of()),
                            new MemoryTable.RecordResult(5, Map.of("g", 0.)),
                            new MemoryTable.RecordResult(6, Map.of("g", 1.)),
                            new MemoryTable.RecordResult(7, Map.of("g", 2.)),
                            new MemoryTable.RecordResult(8, Map.of("g", 3.)),
                            new MemoryTable.RecordResult(9, Map.of("g", 4.)))));
        }

        @Test
        public void testQueryFilterStringLess() {
            var columns = Map.of("h", "h");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("h")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(3, Map.of()),
                            new MemoryTable.RecordResult(4, Map.of("h", "0")),
                            new MemoryTable.RecordResult(5, Map.of("h", "1")),
                            new MemoryTable.RecordResult(6, Map.of("h", "2")),
                            new MemoryTable.RecordResult(7, Map.of("h", "3")),
                            new MemoryTable.RecordResult(8, Map.of("h", "4")))));
        }

        @Test
        public void testQueryFilterBytesLess() {
            var columns = Map.of("i", "i");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(2, Map.of()),
                            new MemoryTable.RecordResult(3,
                                    Map.of("i", ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(4,
                                    Map.of("i", ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(5,
                                    Map.of("i", ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(6,
                                    Map.of("i", ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(7,
                                    Map.of("i", ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8)))))));
        }

        @Test
        public void testQueryFilterBoolLessEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("a"), createConstant(true)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(
                            new MemoryTable.RecordResult(7, Map.of("a", false)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "d", 1)),
                            new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "d", 5)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "d", 6)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "d", 7)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "d", 8)))));
        }

        @Test
        public void testQueryFilterInt8LessEqual() {
            var columns = Map.of("b", "b");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(9, Map.of()),
                            new MemoryTable.RecordResult(0, Map.of("b", (byte) 0)),
                            new MemoryTable.RecordResult(1, Map.of("b", (byte) 1)),
                            new MemoryTable.RecordResult(2, Map.of("b", (byte) 2)),
                            new MemoryTable.RecordResult(3, Map.of("b", (byte) 3)),
                            new MemoryTable.RecordResult(4, Map.of("b", (byte) 4)),
                            new MemoryTable.RecordResult(5, Map.of("b", (byte) 5)))));
        }

        @Test
        public void testQueryFilterInt16LessEqual() {
            var columns = Map.of("c", "c");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(8, Map.of()),
                            new MemoryTable.RecordResult(9, Map.of("c", (short) 0)),
                            new MemoryTable.RecordResult(0, Map.of("c", (short) 1)),
                            new MemoryTable.RecordResult(1, Map.of("c", (short) 2)),
                            new MemoryTable.RecordResult(2, Map.of("c", (short) 3)),
                            new MemoryTable.RecordResult(3, Map.of("c", (short) 4)),
                            new MemoryTable.RecordResult(4, Map.of("c", (short) 5)))));
        }

        @Test
        public void testQueryFilterInt32LessEqual() {
            var columns = Map.of("d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(7, Map.of()),
                            new MemoryTable.RecordResult(8, Map.of("d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("d", 1)),
                            new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("d", 5)))));
        }

        @Test
        public void testQueryFilterInt64LessEqual() {
            var columns = Map.of("e", "e");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(6, Map.of()),
                            new MemoryTable.RecordResult(7, Map.of("e", 0L)),
                            new MemoryTable.RecordResult(8, Map.of("e", 1L)),
                            new MemoryTable.RecordResult(9, Map.of("e", 2L)),
                            new MemoryTable.RecordResult(0, Map.of("e", 3L)),
                            new MemoryTable.RecordResult(1, Map.of("e", 4L)),
                            new MemoryTable.RecordResult(2, Map.of("e", 5L)))));
        }

        @Test
        public void testQueryFilterFloat32LessEqual() {
            var columns = Map.of("f", "f");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(5, Map.of()),
                            new MemoryTable.RecordResult(6, Map.of("f", 0.f)),
                            new MemoryTable.RecordResult(7, Map.of("f", 1.f)),
                            new MemoryTable.RecordResult(8, Map.of("f", 2.f)),
                            new MemoryTable.RecordResult(9, Map.of("f", 3.f)),
                            new MemoryTable.RecordResult(0, Map.of("f", 4.f)),
                            new MemoryTable.RecordResult(1, Map.of("f", 5.f)))));
        }

        @Test
        public void testQueryFilterFloat64LessEqual() {
            var columns = Map.of("g", "g");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(4, Map.of()),
                            new MemoryTable.RecordResult(5, Map.of("g", 0.)),
                            new MemoryTable.RecordResult(6, Map.of("g", 1.)),
                            new MemoryTable.RecordResult(7, Map.of("g", 2.)),
                            new MemoryTable.RecordResult(8, Map.of("g", 3.)),
                            new MemoryTable.RecordResult(9, Map.of("g", 4.)),
                            new MemoryTable.RecordResult(0, Map.of("g", 5.)))));
        }

        @Test
        public void testQueryFilterStringLessEqual() {
            var columns = Map.of("h", "h");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("h")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(3, Map.of()),
                            new MemoryTable.RecordResult(4, Map.of("h", "0")),
                            new MemoryTable.RecordResult(5, Map.of("h", "1")),
                            new MemoryTable.RecordResult(6, Map.of("h", "2")),
                            new MemoryTable.RecordResult(7, Map.of("h", "3")),
                            new MemoryTable.RecordResult(8, Map.of("h", "4")),
                            new MemoryTable.RecordResult(9, Map.of("h", "5")))));
        }

        @Test
        public void testQueryFilterBytesLessEqual() {
            var columns = Map.of("i", "i");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.LESS_EQUAL)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(2, Map.of()),
                            new MemoryTable.RecordResult(3,
                                    Map.of("i", ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(4,
                                    Map.of("i", ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(5,
                                    Map.of("i", ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(6,
                                    Map.of("i", ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(7,
                                    Map.of("i", ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(8,
                                    Map.of("i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))));
        }

        @Test
        public void testQueryFilterBoolGreater() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("a"), createConstant(false)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(
                            new MemoryTable.RecordResult(8, Map.of("a", true, "d", 0)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "d", 4)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "d", 6)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "d", 8)))));
        }

        @Test
        public void testQueryFilterInt8Greater() {
            var columns = Map.of("b", "b");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(6, Map.of("b", (byte) 6)),
                            new MemoryTable.RecordResult(7, Map.of("b", (byte) 7)),
                            new MemoryTable.RecordResult(8, Map.of("b", (byte) 8)))));
        }

        @Test
        public void testQueryFilterInt16Greater() {
            var columns = Map.of("c", "c");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(5, Map.of("c", (short) 6)),
                            new MemoryTable.RecordResult(6, Map.of("c", (short) 7)),
                            new MemoryTable.RecordResult(7, Map.of("c", (short) 8)))));
        }

        @Test
        public void testQueryFilterInt32Greater() {
            var columns = Map.of("d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(4, Map.of("d", 6)),
                            new MemoryTable.RecordResult(5, Map.of("d", 7)),
                            new MemoryTable.RecordResult(6, Map.of("d", 8)))));
        }

        @Test
        public void testQueryFilterInt64Greater() {
            var columns = Map.of("e", "e");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(3, Map.of("e", 6L)),
                            new MemoryTable.RecordResult(4, Map.of("e", 7L)),
                            new MemoryTable.RecordResult(5, Map.of("e", 8L)))));
        }

        @Test
        public void testQueryFilterFloat32Greater() {
            var columns = Map.of("f", "f");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(2, Map.of("f", 6.f)),
                            new MemoryTable.RecordResult(3, Map.of("f", 7.f)),
                            new MemoryTable.RecordResult(4, Map.of("f", 8.f)))));
        }

        @Test
        public void testQueryFilterFloat64Greater() {
            var columns = Map.of("g", "g");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(1, Map.of("g", 6.)),
                            new MemoryTable.RecordResult(2, Map.of("g", 7.)),
                            new MemoryTable.RecordResult(3, Map.of("g", 8.)))));
        }

        @Test
        public void testQueryFilterStringGreater() {
            var columns = Map.of("h", "h");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("h", "6")),
                            new MemoryTable.RecordResult(1, Map.of("h", "7")),
                            new MemoryTable.RecordResult(2, Map.of("h", "8")))));
        }

        @Test
        public void testQueryFilterBytesGreater() {
            var columns = Map.of("i", "i");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(9,
                                    Map.of("i", ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(0,
                                    Map.of("i", ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(1,
                                    Map.of("i", ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8)))))));
        }

        @Test
        public void testQueryFilterBoolGreaterEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("a"), createConstant(false)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(
                            new MemoryTable.RecordResult(7, Map.of("a", false)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "d", 1)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("a", false, "d", 5)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "d", 6)),
                            new MemoryTable.RecordResult(5, Map.of("a", false, "d", 7)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "d", 8)))));
        }

        @Test
        public void testQueryFilterInt8GreaterEqual() {
            var columns = Map.of("b", "b");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("b")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(5, Map.of("b", (byte) 5)),
                            new MemoryTable.RecordResult(6, Map.of("b", (byte) 6)),
                            new MemoryTable.RecordResult(7, Map.of("b", (byte) 7)),
                            new MemoryTable.RecordResult(8, Map.of("b", (byte) 8)))));
        }

        @Test
        public void testQueryFilterInt16GreaterEqual() {
            var columns = Map.of("c", "c");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("c")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(4, Map.of("c", (short) 5)),
                            new MemoryTable.RecordResult(5, Map.of("c", (short) 6)),
                            new MemoryTable.RecordResult(6, Map.of("c", (short) 7)),
                            new MemoryTable.RecordResult(7, Map.of("c", (short) 8)))));
        }

        @Test
        public void testQueryFilterInt32GreaterEqual() {
            var columns = Map.of("d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(3, Map.of("d", 5)),
                            new MemoryTable.RecordResult(4, Map.of("d", 6)),
                            new MemoryTable.RecordResult(5, Map.of("d", 7)),
                            new MemoryTable.RecordResult(6, Map.of("d", 8)))));
        }

        @Test
        public void testQueryFilterInt64GreaterEqual() {
            var columns = Map.of("e", "e");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("e")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(2, Map.of("e", 5L)),
                            new MemoryTable.RecordResult(3, Map.of("e", 6L)),
                            new MemoryTable.RecordResult(4, Map.of("e", 7L)),
                            new MemoryTable.RecordResult(5, Map.of("e", 8L)))));
        }

        @Test
        public void testQueryFilterFloat32GreaterEqual() {
            var columns = Map.of("f", "f");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("f")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(1, Map.of("f", 5.f)),
                            new MemoryTable.RecordResult(2, Map.of("f", 6.f)),
                            new MemoryTable.RecordResult(3, Map.of("f", 7.f)),
                            new MemoryTable.RecordResult(4, Map.of("f", 8.f)))));
        }

        @Test
        public void testQueryFilterFloat64GreaterEqual() {
            var columns = Map.of("g", "g");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("g"), this.createConstant(5.)))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("g", 5.)),
                            new MemoryTable.RecordResult(1, Map.of("g", 6.)),
                            new MemoryTable.RecordResult(2, Map.of("g", 7.)),
                            new MemoryTable.RecordResult(3, Map.of("g", 8.)))));
        }

        @Test
        public void testQueryFilterStringGreaterEqual() {
            var columns = Map.of("h", "h");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("g")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(9, Map.of("h", "5")),
                            new MemoryTable.RecordResult(0, Map.of("h", "6")),
                            new MemoryTable.RecordResult(1, Map.of("h", "7")),
                            new MemoryTable.RecordResult(2, Map.of("h", "8")))));
        }

        @Test
        public void testQueryFilterBytesGreaterEqual() {
            var columns = Map.of("i", "i");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("i")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                            .operands(List.of(
                                    new TableQueryFilter.Column("i"),
                                    createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(8,
                                    Map.of("i", ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(9,
                                    Map.of("i", ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(0,
                                    Map.of("i", ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8)))),
                            new MemoryTable.RecordResult(1,
                                    Map.of("i", ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8)))))));
        }

        @Test
        public void testQueryFilterNot() {
            var columns = Map.of("d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.NOT)
                            .operands(List.of(
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.EQUAL)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("d"),
                                                    createConstant(5)))
                                            .build()))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(7, Map.of()),
                            new MemoryTable.RecordResult(8, Map.of("d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("d", 1)),
                            new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("d", 4)),
                            new MemoryTable.RecordResult(4, Map.of("d", 6)),
                            new MemoryTable.RecordResult(5, Map.of("d", 7)),
                            new MemoryTable.RecordResult(6, Map.of("d", 8)))));
        }

        @Test
        public void testQueryFilterAnd() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.AND)
                            .operands(List.of(
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.LESS)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("d"),
                                                    createConstant(5)))
                                            .build(),
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.EQUAL)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("a"),
                                                    createConstant(true)))
                                            .build()))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(8, Map.of("a", true, "d", 0)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "d", 4)))));
        }

        @Test
        public void testQueryFilterOr() {
            var columns = Map.of("a", "a", "d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.OR)
                            .operands(List.of(
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.LESS)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("d"),
                                                    createConstant(5)))
                                            .build(),
                                    TableQueryFilter.builder()
                                            .operator(TableQueryFilter.Operator.EQUAL)
                                            .operands(List.of(
                                                    new TableQueryFilter.Column("a"),
                                                    createConstant(true)))
                                            .build()))
                            .build(),
                    -1,
                    -1,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(7, Map.of("a", false)),
                            new MemoryTable.RecordResult(9, Map.of("a", false, "d", 1)),
                            new MemoryTable.RecordResult(1, Map.of("a", false, "d", 3)),
                            new MemoryTable.RecordResult(8, Map.of("a", true, "d", 0)),
                            new MemoryTable.RecordResult(2, Map.of("a", true, "d", 4)),
                            new MemoryTable.RecordResult(4, Map.of("a", true, "d", 6)),
                            new MemoryTable.RecordResult(6, Map.of("a", true, "d", 8)))));
        }

        @Test
        public void testQueryStartLimit() {
            var columns = Map.of("d", "d");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("d")),
                    null,
                    5,
                    2,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(2, Map.of("d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("d", 5)))));
        }

        @Test
        public void testQueryUnknown() {
            var columns = Map.of("z", "z");
            var results = this.memoryTable.query(
                    columns,
                    List.of(new OrderByDesc("z")),
                    null,
                    5,
                    2,
                    false,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(5, Map.of()),
                            new MemoryTable.RecordResult(6, Map.of()))));
        }

        @Test
        public void testQueryKeepNone() {
            var columns = Map.of("a", "a");
            var results = this.memoryTable.query(
                    columns,
                    null,
                    TableQueryFilter.builder()
                            .operator(TableQueryFilter.Operator.EQUAL)
                            .operands(List.of(new TableQueryFilter.Column("b"), createConstant(0)))
                            .build(),
                    -1,
                    -1,
                    true,
                    false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, new HashMap<>() {
                        {
                            put("a", null);
                        }
                    }))));
        }

        @Test
        public void testScanInitialEmptyTable() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            var results = this.memoryTable.scan(Map.of("a", "a"), null, false, null, false, 1000, false);
            assertThat("empty", results, empty());
        }

        @Test
        public void testScanEmptyTableWithSchema() {
            this.memoryTable = new MemoryTableImpl("test", MemoryTableImplTest.this.walManager);
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                    List.of(Map.of("k", "0", "-", "1")));
            var results = this.memoryTable.scan(Map.of("a", "a"), null, false, null, false, 1000, false);
            assertThat("empty", results, empty());
        }

        @Test
        public void testScanColumnAliases() {
            var results = this.memoryTable.scan(Map.of("a", "x", "d", "y"), null, false, null, false, 1000, false);
            assertThat(results,
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("y", 2)),
                            new MemoryTable.RecordResult(1, Map.of("x", false, "y", 3)),
                            new MemoryTable.RecordResult(2, Map.of("x", true, "y", 4)),
                            new MemoryTable.RecordResult(3, Map.of("x", false, "y", 5)),
                            new MemoryTable.RecordResult(4, Map.of("x", true, "y", 6)),
                            new MemoryTable.RecordResult(5, Map.of("x", false, "y", 7)),
                            new MemoryTable.RecordResult(6, Map.of("x", true, "y", 8)),
                            new MemoryTable.RecordResult(7, Map.of("x", false)),
                            new MemoryTable.RecordResult(8, Map.of("x", true, "y", 0)),
                            new MemoryTable.RecordResult(9, Map.of("x", false, "y", 1)))));
        }

        @Test
        public void testScanNonScalar() {
            this.memoryTable.update(
                    new TableSchemaDesc(null,
                            List.of(ColumnSchemaDesc.builder()
                                    .name("x")
                                    .type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                    .build())),
                    List.of(Map.of("key", "1", "x", List.of("a"))));
            var results = this.memoryTable.scan(
                    Map.of("key", "key", "x", "x"),
                    "1",
                    true,
                    "1",
                    true,
                    -1,
                    false);
            assertThat(results, is(List.of(new MemoryTable.RecordResult(1, Map.of("key", 1, "x", List.of(10))))));
        }


        @Test
        public void testScanStartEnd() {
            assertThat("start,non-inclusive",
                    this.memoryTable.scan(Map.of("d", "d"), "5", false, null, false, 1000, false),
                    is(List.of(new MemoryTable.RecordResult(6, Map.of("d", 8)),
                            new MemoryTable.RecordResult(7, Map.of()),
                            new MemoryTable.RecordResult(8, Map.of("d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("d", 1)))));

            assertThat("start,inclusive",
                    this.memoryTable.scan(Map.of("d", "d"), "5", true, null, false, 1000, false),
                    is(List.of(new MemoryTable.RecordResult(5, Map.of("d", 7)),
                            new MemoryTable.RecordResult(6, Map.of("d", 8)),
                            new MemoryTable.RecordResult(7, Map.of()),
                            new MemoryTable.RecordResult(8, Map.of("d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("d", 1)))));

            assertThat("end,non-inclusive",
                    this.memoryTable.scan(Map.of("d", "d"), null, false, "5", false, 1000, false),
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("d", 5)),
                            new MemoryTable.RecordResult(4, Map.of("d", 6)))));

            assertThat("end,inclusive",
                    this.memoryTable.scan(Map.of("d", "d"), null, false, "5", true, 1000, false),
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("d", 5)),
                            new MemoryTable.RecordResult(4, Map.of("d", 6)),
                            new MemoryTable.RecordResult(5, Map.of("d", 7)))));

            assertThat("start+end",
                    this.memoryTable.scan(Map.of("d", "d"), "2", true, "5", false, 1000, false),
                    is(List.of(new MemoryTable.RecordResult(2, Map.of("d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("d", 5)),
                            new MemoryTable.RecordResult(4, Map.of("d", 6)))));
        }

        @Test
        public void testScanLimit() {
            assertThat("test",
                    this.memoryTable.scan(Map.of("d", "d"), null, false, null, false, 2, true),
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("d", 3)))));
        }

        @Test
        public void testScanKeepNone() {
            assertThat("test",
                    this.memoryTable.scan(Map.of("d", "d"), null, false, null, false, 1000, true),
                    is(List.of(new MemoryTable.RecordResult(0, Map.of("d", 2)),
                            new MemoryTable.RecordResult(1, Map.of("d", 3)),
                            new MemoryTable.RecordResult(2, Map.of("d", 4)),
                            new MemoryTable.RecordResult(3, Map.of("d", 5)),
                            new MemoryTable.RecordResult(4, Map.of("d", 6)),
                            new MemoryTable.RecordResult(5, Map.of("d", 7)),
                            new MemoryTable.RecordResult(6, Map.of("d", 8)),
                            new MemoryTable.RecordResult(7, new HashMap<>() {
                                {
                                    put("d", null);
                                }
                            }),
                            new MemoryTable.RecordResult(8, Map.of("d", 0)),
                            new MemoryTable.RecordResult(9, Map.of("d", 1)))));
        }

        @Test
        public void testScanUnknown() {
            assertThat(this.memoryTable.scan(Map.of("z", "z"), null, false, null, false, 1000, false),
                    is(List.of(new MemoryTable.RecordResult(0, Map.of()),
                            new MemoryTable.RecordResult(1, Map.of()),
                            new MemoryTable.RecordResult(2, Map.of()),
                            new MemoryTable.RecordResult(3, Map.of()),
                            new MemoryTable.RecordResult(4, Map.of()),
                            new MemoryTable.RecordResult(5, Map.of()),
                            new MemoryTable.RecordResult(6, Map.of()),
                            new MemoryTable.RecordResult(7, Map.of()),
                            new MemoryTable.RecordResult(8, Map.of()),
                            new MemoryTable.RecordResult(9, Map.of()))));
        }
    }
}
