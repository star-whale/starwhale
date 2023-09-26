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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.MemoryTable;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.ParquetConfig;
import ai.starwhale.mlops.datastore.ParquetConfig.CompressionCodec;
import ai.starwhale.mlops.datastore.RecordResult;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableQueryFilter.Operator;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.ObjectValue;
import ai.starwhale.mlops.datastore.type.TupleValue;
import ai.starwhale.mlops.datastore.wal.WalManager;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MemoryTableImplTest {

    private static List<RecordResult> scanAll(MemoryTable memoryTable,
            List<String> columns,
            boolean keepNone) {
        return ImmutableList.copyOf(
                memoryTable.scan(Long.MAX_VALUE,
                        columns.stream().collect(Collectors.toMap(Function.identity(), Function.identity())),
                        null,
                        null,
                        true,
                        null,
                        null,
                        false,
                        keepNone));
    }

    private FileSystem fs;
    private WalManager walManager;
    private StorageAccessService storageAccessService;

    @BeforeEach
    public void setUp() throws IOException {
        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.storageAccessService = new StorageAccessServiceMemory();
        this.walManager = new WalManager(this.storageAccessService, 4096, this.fs.getPath("/wal_cache"), "wal/", 3);
    }

    @AfterEach
    @SneakyThrows
    public void tearDown() {
        this.walManager.terminate();
        this.fs.close();
    }

    public MemoryTableImpl createInstance(String name) {
        var parquetConfig = new ParquetConfig();
        parquetConfig.setCompressionCodec(CompressionCodec.SNAPPY);
        parquetConfig.setRowGroupSize(1024 * 1024);
        parquetConfig.setPageSize(4096);
        parquetConfig.setPageRowCountLimit(1000);
        return new MemoryTableImpl(name,
                this.walManager,
                this.storageAccessService,
                name,
                parquetConfig);
    }

    @Nested
    public class UpdateTest {

        private MemoryTableImpl memoryTable;

        @BeforeEach
        public void setUp() {
            this.memoryTable = createInstance("test");
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
                    contains(new RecordResult(BaseValue.valueOf("0"),
                            false,
                            Map.of("k", BaseValue.valueOf("0"), "a", BaseValue.valueOf(10)))));

            this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("k", "1", "a", "b")));
            assertThat("insert", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a", BaseValue.valueOf(10))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "a", BaseValue.valueOf(11)))));

            this.memoryTable.update(
                    this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("k", "2", "a", "c"),
                            Map.of("k", "3", "a", "d")));
            assertThat("insert multiple", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a", BaseValue.valueOf(10))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "a", BaseValue.valueOf(11))),
                            new RecordResult(BaseValue.valueOf("2"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("2"), "a", BaseValue.valueOf(12))),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(13)))));

            this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("k", "1", "a", "c")));
            assertThat("overwrite", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a", BaseValue.valueOf(10))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "a", BaseValue.valueOf(12))),
                            new RecordResult(BaseValue.valueOf("2"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("2"), "a", BaseValue.valueOf(12))),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(13)))));

            this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("k", "2", "-", "1")));
            assertThat("delete", scanAll(this.memoryTable, List.of("k", "a"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a", BaseValue.valueOf(10))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "a", BaseValue.valueOf(12))),
                            new RecordResult(BaseValue.valueOf("2"), true, null),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(13)))));

            this.memoryTable.update(
                    new TableSchemaDesc(null,
                            List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                    ColumnSchemaDesc.builder().name("b").type("INT32").build())),
                    List.of(Map.of("k", "1", "b", "0")));
            assertThat("new column", scanAll(this.memoryTable, List.of("k", "a", "b"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"),
                                            "a", BaseValue.valueOf(10))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"),
                                            "a", BaseValue.valueOf(12),
                                            "b", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("2"), true, null),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(13)))));

            this.memoryTable.update(
                    this.memoryTable.getSchema().toTableSchemaDesc(),
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
                    contains(new RecordResult(BaseValue.valueOf("0"), false,
                                    Map.of("k", BaseValue.valueOf("0"), "a", BaseValue.valueOf(10))),
                            new RecordResult(BaseValue.valueOf("1"), false,
                                    Map.of("k", BaseValue.valueOf("1"), "b", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("2"), false, Map.of("k", BaseValue.valueOf("2"))),
                            new RecordResult(BaseValue.valueOf("3"), false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(13)))));
            var desc = this.memoryTable.getSchema().toTableSchemaDesc();
            desc.getColumnSchemaList().add(ColumnSchemaDesc.builder().name("c").type("INT32").build());
            this.memoryTable.update(desc,
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
                    contains(new RecordResult(BaseValue.valueOf("0"), true, null),
                            new RecordResult(BaseValue.valueOf(BaseValue.valueOf("1")),
                                    false,
                                    Map.of("k", BaseValue.valueOf(BaseValue.valueOf("1")),
                                            "c", BaseValue.valueOf(BaseValue.valueOf(1)))),
                            new RecordResult(BaseValue.valueOf("2"), true, null),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("4"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("4"), "c", BaseValue.valueOf(0)))));
            desc.getColumnSchemaList().add(ColumnSchemaDesc.builder().name("a-b/c/d:e_f").type("INT32").build());
            this.memoryTable.update(desc, List.of(Map.of("k", "0", "a-b/c/d:e_f", "0")));
            assertThat("complex name", scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a-b/c/d:e_f", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "c", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf("2"), true, null),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("4"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("4"), "c", BaseValue.valueOf(0)))));
            desc.getColumnSchemaList().add(ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build());
            this.memoryTable.update(desc,
                    List.of(new HashMap<>() {
                        {
                            put("k", "0");
                            put("x", null);
                        }
                    }));
            assertThat("unknown",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnType.UNKNOWN));
            assertThat("unknown", scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a-b/c/d:e_f", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "c", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf("2"), true, null),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("4"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("4"), "c", BaseValue.valueOf(0)))));
            desc.getColumnSchemaList().set(desc.getColumnSchemaList().size() - 1,
                    ColumnSchemaDesc.builder().name("x").type("INT32").build());
            this.memoryTable.update(desc, List.of(Map.of("k", "1", "x", "1")));
            assertThat("update unknown",
                    this.memoryTable.getSchema().getColumnSchemaByName("x").getType(),
                    is(ColumnType.INT32));
            assertThat("update unknown",
                    scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f", "x"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a-b/c/d:e_f", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "c", BaseValue.valueOf(1), "x",
                                            BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf("2"), true, null),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("4"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("4"), "c", BaseValue.valueOf(0)))));

            desc.getColumnSchemaList().set(desc.getColumnSchemaList().size() - 1,
                    ColumnSchemaDesc.builder().name("x").type("UNKNOWN").build());
            this.memoryTable.update(desc,
                    List.of(new HashMap<>() {
                        {
                            put("k", "1");
                            put("x", null);
                        }
                    }));
            assertThat("unknown again",
                    scanAll(this.memoryTable, List.of("k", "a", "b", "c", "a-b/c/d:e_f", "x"), false),
                    contains(new RecordResult(BaseValue.valueOf("0"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("0"), "a-b/c/d:e_f", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("1"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("1"), "c", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf("2"), true, null),
                            new RecordResult(BaseValue.valueOf("3"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("3"), "a", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf("4"),
                                    false,
                                    Map.of("k", BaseValue.valueOf("4"), "c", BaseValue.valueOf(0)))));
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
                                    .elementType(ColumnSchemaDesc.builder().name("element").type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("k")
                                    .type("OBJECT")
                                    .pythonType("t")
                                    .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                            ColumnSchemaDesc.builder().name("b").type("INT32").build()))
                                    .build(),
                            ColumnSchemaDesc.builder().name("l").type("FLOAT64").build(),
                            ColumnSchemaDesc.builder().name("m")
                                    .type("TUPLE")
                                    .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("n")
                                    .type("MAP")
                                    .keyType(ColumnSchemaDesc.builder().type("INT8").build())
                                    .valueType(ColumnSchemaDesc.builder().type("INT16").build())
                                    .build())),

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
                            put("m", List.of("b"));
                            put("n", Map.of("1", "2"));
                        }
                    }));
            this.memoryTable.update(
                    new TableSchemaDesc("key", List.of(
                            ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("STRING").build(),
                            ColumnSchemaDesc.builder().name("f").type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().name("element").type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("i").type("TUPLE")
                                    .elementType(ColumnSchemaDesc.builder().name("element").type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("j").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("k")
                                    .type("OBJECT")
                                    .pythonType("tt")
                                    .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                            ColumnSchemaDesc.builder().name("b").type("INT32").build()))
                                    .build(),
                            ColumnSchemaDesc.builder().name("n")
                                    .type("MAP")
                                    .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                                    .valueType(ColumnSchemaDesc.builder().type("INT16").build())
                                    .build())),

                    List.of(new HashMap<>() {
                        {
                            put("key", "1");
                            put("b", "10");
                            put("f", List.of("a"));
                            put("i", List.of("b"));
                            put("j", "7");
                            put("k", Map.of("a", "b", "b", "c"));
                            put("n", Map.of("1", "2"));
                        }
                    }));
            assertThat("all types",
                    scanAll(this.memoryTable,
                            List.of("key", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n"),
                            false),
                    contains(new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("key", BaseValue.valueOf(1),
                                            "b", BaseValue.valueOf("10"),
                                            "f", BaseValue.valueOf(List.of(10)),
                                            "i", TupleValue.valueOf(List.of(11)),
                                            "j", BaseValue.valueOf(7),
                                            "k", ObjectValue.valueOf("tt", Map.of("a", 11, "b", 12)),
                                            "n", BaseValue.valueOf(Map.of("1", (short) 2)))),
                            new RecordResult(BaseValue.valueOf("x"),
                                    false,
                                    new HashMap<>() {
                                        {
                                            put("key", BaseValue.valueOf("x"));
                                            put("a", BaseValue.valueOf(true));
                                            put("b", BaseValue.valueOf((byte) 16));
                                            put("c", BaseValue.valueOf(Short.parseShort("1000", 16)));
                                            put("d", BaseValue.valueOf(Integer.parseInt("100000", 16)));
                                            put("e", BaseValue.valueOf(Long.parseLong("10000000", 16)));
                                            put("f", BaseValue.valueOf(1.1f));
                                            put("g", BaseValue.valueOf(1.1));
                                            put("h", BaseValue.valueOf(
                                                    ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))));
                                            put("j", BaseValue.valueOf(List.of(10)));
                                            put("k", ObjectValue.valueOf("t", Map.of("a", 11, "b", 12)));
                                            put("l", BaseValue.valueOf(0.0));
                                            put("m", TupleValue.valueOf(List.of(11)));
                                            put("n", BaseValue.valueOf(Map.of((byte) 1, (short) 2)));
                                        }
                                    })));
            // the type of j column changes from list to int and then list again, and with the same element type
            this.memoryTable.update(
                    new TableSchemaDesc("key", List.of(
                            ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("j").type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                    .build())),
                    List.of(new HashMap<>() {
                        {
                            put("key", "2");
                            put("j", List.of("b"));
                        }
                    }));
            var all = scanAll(this.memoryTable, List.of("j"), false);
            assertThat(all, contains(
                    new RecordResult(BaseValue.valueOf(1), false, Map.of("j", BaseValue.valueOf(7))),
                    new RecordResult(BaseValue.valueOf(2), false, Map.of("j", BaseValue.valueOf(List.of(11)))),
                    new RecordResult(BaseValue.valueOf("x"), false, Map.of("j", BaseValue.valueOf(List.of(10))))
            ));
        }

        @Test
        public void testUpdateAllKeyColumnTypes() {
            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("BOOL").build())),
                    List.of(Map.of("k", "1")));
            assertThat("bool",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(BaseValue.valueOf(true), false, Map.of("k", BaseValue.valueOf(true)))));

            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT8").build())),
                    List.of(Map.of("k", "10")));
            assertThat("int8",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(BaseValue.valueOf((byte) 16), false,
                            Map.of("k", BaseValue.valueOf((byte) 16)))));

            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT16").build())),
                    List.of(Map.of("k", "1000")));
            assertThat("int16",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(BaseValue.valueOf(Short.parseShort("1000", 16)),
                            false,
                            Map.of("k", BaseValue.valueOf(Short.parseShort("1000", 16))))));

            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build())),
                    List.of(Map.of("k", "100000")));
            assertThat("int32",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(BaseValue.valueOf(Integer.parseInt("100000", 16)),
                            false,
                            Map.of("k", BaseValue.valueOf(Integer.parseInt("100000", 16))))));

            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT64").build())),
                    List.of(Map.of("k", "10000000")));
            assertThat("int64",
                    scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(BaseValue.valueOf(Long.parseLong("10000000", 16)),
                            false,
                            Map.of("k", BaseValue.valueOf(Long.parseLong("10000000", 16))))));

            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("FLOAT32").build())),
                    List.of(Map.of("k", Integer.toHexString(Float.floatToIntBits(1.1f)))));
            assertThat("float32", scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(BaseValue.valueOf(1.1f),
                            false,
                            Map.of("k", BaseValue.valueOf(1.1f)))));

            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("FLOAT64").build())),
                    List.of(Map.of("k", Long.toHexString(Double.doubleToLongBits(1.1)))));
            assertThat("float64", scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(BaseValue.valueOf(1.1),
                            false,
                            Map.of("k", BaseValue.valueOf(1.1)))));

            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("BYTES").build())),
                    List.of(Map.of("k", Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)))));
            assertThat("bytes", scanAll(this.memoryTable, List.of("k"), false),
                    contains(new RecordResult(
                            BaseValue.valueOf(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))),
                            false,
                            Map.of("k", BaseValue.valueOf(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)))))));
        }

        @Test
        public void testUpdateMixedTypes() {
            this.memoryTable = createInstance("test");

        }

        @Test
        public void testUpdateExceptions() {
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(), List.of()),
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
            this.memoryTable.update(schema, List.of());
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.update(
                            this.memoryTable.getSchema().toTableSchemaDesc(),
                            List.of(Map.of("k", "a".repeat(5000)))),
                    "huge entry");
            assertThat("schema", this.memoryTable.getSchema(), is(new TableSchema(schema)));
            assertThat("records", scanAll(this.memoryTable, List.of("k"), false), empty());
        }

        @Test
        public void testUpdateFromWal() throws IOException {
            this.memoryTable.setUseTimestampAsRevision(true);
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
                                    .elementType(ColumnSchemaDesc.builder().name("element").type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("k")
                                    .type("TUPLE")
                                    .elementType(ColumnSchemaDesc.builder().name("element").type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("l")
                                    .type("MAP")
                                    .keyType(ColumnSchemaDesc.builder().name("key").type("INT32").build())
                                    .valueType(ColumnSchemaDesc.builder().name("value").type("INT32").build())
                                    .build(),
                            ColumnSchemaDesc.builder().name("m")
                                    .type("OBJECT")
                                    .pythonType("t")
                                    .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                            ColumnSchemaDesc.builder().name("b").type("INT32").build()))
                                    .build(),
                            ColumnSchemaDesc.builder().name("n")
                                    .type("LIST")
                                    .elementType(ColumnSchemaDesc.builder()
                                            .name("element")
                                            .type("TUPLE")
                                            .elementType(ColumnSchemaDesc.builder()
                                                    .type("MAP")
                                                    .name("element")
                                                    .keyType(ColumnSchemaDesc.builder()
                                                            .type("OBJECT")
                                                            .pythonType("tt")
                                                            .attributes(List.of(ColumnSchemaDesc.builder().name("a")
                                                                            .type("LIST")
                                                                            .elementType(
                                                                                    ColumnSchemaDesc.builder()
                                                                                            .type("INT32")
                                                                                            .build())
                                                                            .build(),
                                                                    ColumnSchemaDesc.builder().name("b")
                                                                            .type("INT32")
                                                                            .build()))
                                                            .build())
                                                    .valueType(ColumnSchemaDesc.builder()
                                                            .type("LIST")
                                                            .elementType(
                                                                    ColumnSchemaDesc.builder()
                                                                            .name("element").type("INT32").build())
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())),
                    List.of());
            List<Map<String, Object>> records = new ArrayList<>();
            for (int i = 0; i < 50; ++i) {
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
                        put("j", List.of(Integer.toHexString(index + 1)));
                        put("k", List.of(Integer.toHexString(index + 2)));
                        put("l", Map.of(Integer.toHexString(index + 3), Integer.toHexString(index + 4)));
                        put("m", Map.of("a", Integer.toHexString(index + 5),
                                "b", Integer.toHexString(index + 6)));
                        put("n", List.of(List.of(
                                Map.of(Map.of("a", List.of(Integer.toHexString(index + 7)),
                                                "b", Integer.toHexString(index + 8)),
                                        List.of(Integer.toHexString(index + 9))))));
                    }
                });
            }
            var desc = this.memoryTable.getSchema().toTableSchemaDesc();
            this.memoryTable.update(desc, records);
            this.memoryTable.update(new TableSchemaDesc("key", List.of(
                            ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("d").type("STRING").build(),
                            ColumnSchemaDesc.builder().name("e")
                                    .type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                    .build())),
                    List.of(Map.of("key", "1", "d", "0", "e", List.of("a", "b")),
                            Map.of("key", "2", "d", "0", "e", List.of("a", "b"))));
            this.memoryTable.save();
            this.memoryTable.update(new TableSchemaDesc("key", List.of(
                            ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("d").type("BOOL").build(),
                            ColumnSchemaDesc.builder().name("e")
                                    .type("LIST")
                                    .elementType(ColumnSchemaDesc.builder().name("element").type("BOOL").build())
                                    .build())),
                    List.of(Map.of("key", "2", "d", "0", "e", List.of("0", "1"))));
            records.clear();
            for (int i = 50; i < 100; ++i) {
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
                        put("j", List.of(Integer.toHexString(index + 1)));
                        put("k", List.of(Integer.toHexString(index + 2)));
                        put("l", Map.of(Integer.toHexString(index + 3), Integer.toHexString(index + 4)));
                        put("m", Map.of("a", Integer.toHexString(index + 5),
                                "b", Integer.toHexString(index + 6)));
                        put("n", List.of(List.of(
                                Map.of(Map.of("a", List.of(Integer.toHexString(index + 7)),
                                                "b", Integer.toHexString(index + 8)),
                                        List.of(Integer.toHexString(index + 9))))));
                    }
                });
            }
            this.memoryTable.setUseTimestampAsRevision(false);
            this.memoryTable.update(desc, records);
            MemoryTableImplTest.this.walManager.terminate();
            MemoryTableImplTest.this.walManager = new WalManager(MemoryTableImplTest.this.storageAccessService,
                    4096,
                    MemoryTableImplTest.this.fs.getPath("/wal_cache"),
                    "wal/",
                    3);
            this.memoryTable = createInstance("test");
            var it = MemoryTableImplTest.this.walManager.readAll();
            while (it.hasNext()) {
                this.memoryTable.updateFromWal(it.next());
            }
            assertThat(scanAll(this.memoryTable,
                            List.of("key", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n"),
                            true),
                    is(Stream.concat(Stream.of(new RecordResult(BaseValue.valueOf(1), false,
                                                    Map.of("key", BaseValue.valueOf(1),
                                                            "d", BaseValue.valueOf("0"),
                                                            "e", BaseValue.valueOf(List.of(10, 11)))),
                                            new RecordResult(BaseValue.valueOf(2), false,
                                                    Map.of("key", BaseValue.valueOf(2),
                                                            "d", BaseValue.valueOf(false),
                                                            "e", BaseValue.valueOf(List.of(false, true))))),
                                    IntStream.range(0, 100)
                                            .mapToObj(index -> new RecordResult(
                                                    BaseValue.valueOf(String.format("%03d", index)),
                                                    false,
                                                    new HashMap<>() {
                                                        {
                                                            put("key", BaseValue.valueOf(String.format("%03d", index)));
                                                            put("a", BaseValue.valueOf(index % 2 == 1));
                                                            put("b", BaseValue.valueOf((byte) (index + 10)));
                                                            put("c", BaseValue.valueOf((short) (index + 1000)));
                                                            put("d", BaseValue.valueOf(index + 100000));
                                                            put("e", BaseValue.valueOf(index + 10000000L));
                                                            put("f", BaseValue.valueOf(index + 0.1f));
                                                            put("g", BaseValue.valueOf(index + 0.1));
                                                            put("h", BaseValue.valueOf(
                                                                    ByteBuffer.wrap(
                                                                            ("test" + index).getBytes(
                                                                                    StandardCharsets.UTF_8))));
                                                            put("i", null);
                                                            put("j", BaseValue.valueOf(List.of(index + 1)));
                                                            put("k", TupleValue.valueOf(List.of(index + 2)));
                                                            put("l", BaseValue.valueOf(Map.of(index + 3, index + 4)));
                                                            put("m", ObjectValue.valueOf("t",
                                                                    Map.of("a", index + 5, "b", index + 6)));
                                                            put("n", BaseValue.valueOf(List.of(TupleValue.valueOf(
                                                                    List.of(Map.of(ObjectValue.valueOf("tt",
                                                                                    Map.of("a", List.of(index + 7),
                                                                                            "b", index + 8)),
                                                                            List.of(index + 9)))))));
                                                        }
                                                    })))
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
                                Map<String, BaseValue> values = new HashMap<>();
                                values.put("key", BaseValue.valueOf(i));
                                if (data[0][i] != null) {
                                    values.put("a", BaseValue.valueOf(data[0][i]));
                                }
                                if (data[1][i] != null) {
                                    values.put("b", BaseValue.valueOf(data[1][i]));
                                }
                                if (data[2][i] != null) {
                                    values.put("c", BaseValue.valueOf(data[2][i]));
                                }
                                values.put("d", BaseValue.valueOf(data[3][i]));
                                if (data[4][i] != null) {
                                    values.put("e", BaseValue.valueOf(data[4][i]));
                                }
                                if (data[5][i] != null) {
                                    values.put("f", BaseValue.valueOf(data[5][i]));
                                }
                                if (data[6][i] != null) {
                                    values.put("g", BaseValue.valueOf(data[6][i]));
                                }
                                if (data[7][i] != null) {
                                    values.put("h", BaseValue.valueOf(data[7][i]));
                                }
                                if (data[8][i] != null) {
                                    values.put("i", BaseValue.valueOf(ByteBuffer.wrap(
                                            ((String) data[8][i]).getBytes(StandardCharsets.UTF_8))));
                                }
                                return new RecordResult(BaseValue.valueOf(Integer.toHexString(i)), false, values);
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
            this.memoryTable = createInstance("test");
            this.memoryTable.update(schema,
                    records.stream()
                            .map(r -> {
                                var record = new HashMap<String, Object>();
                                r.getValues().forEach((k, v) -> record.put(k, BaseValue.encode(v, false, false)));
                                return record;
                            })
                            .collect(Collectors.toList()));
        }

        private TableQueryFilter.Constant createConstant(Object value) {
            return new TableQueryFilter.Constant(
                    value == null ? ColumnType.UNKNOWN : BaseValue.valueOf(value).getColumnType(),
                    value);
        }

        @Test
        public void testQueryInitialEmptyTable() {
            this.memoryTable = createInstance("test");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(Long.MAX_VALUE, Map.of("a", "a"), null, null, false, false));
            assertThat(results, empty());
        }

        @Test
        public void testQueryEmptyTableWithSchema() {
            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                    List.of());
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(Long.MAX_VALUE, Map.of("k", "k"), null, null, false, false));
            assertThat(results, empty());
        }

        @Test
        public void testQueryEmptyTableWithDeletedRecords() {
            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                    List.of(Map.of("k", "0", "-", "1")));
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(Long.MAX_VALUE, Map.of("k", "k"), null, null, false, false));
            assertThat(results, is(List.of(new RecordResult(BaseValue.valueOf("0"), true, null))));
        }

        @Test
        public void testQueryColumnAliases() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(Long.MAX_VALUE, Map.of("a", "x", "d", "y"), null, null, false, false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("y", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(8))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("x", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(1))))));
        }

        @Test
        public void testQueryNonScalar() {
            this.memoryTable.update(
                    new TableSchemaDesc(null,
                            List.of(ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                                    ColumnSchemaDesc.builder()
                                            .name("x")
                                            .type("LIST")
                                            .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                            .build())),
                    List.of(Map.of("key", "1", "x", List.of("a"))));
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("key", "key", "x", "x"),
                            null,
                            TableQueryFilter.builder()
                                    .operator(Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("key"), this.createConstant(1)))
                                    .build(),
                            false,
                            false));
            assertThat(results, is(List.of(new RecordResult(BaseValue.valueOf(1), false,
                    Map.of("key", BaseValue.valueOf(1), "x", BaseValue.valueOf(List.of(10)))))));
        }

        @Test
        public void testQueryOrderBySingle() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "b", "b"),
                            List.of(new OrderByDesc("a")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("b", BaseValue.valueOf((byte) 0))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 1))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 3))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 5))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 7))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 2))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 4))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 6))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 8))))));
        }

        @Test
        public void testQueryOrderByDescending() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "b", "b"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("b", true)),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("b", BaseValue.valueOf((byte) 0))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 7))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 5))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 3))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 1))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 8))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 6))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 4))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 2))))));
        }

        @Test
        public void testQueryOrderByInt8() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "b", "b"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("b")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("b", BaseValue.valueOf((byte) 0))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 1))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 3))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 5))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf((byte) 7))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 2))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 4))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 6))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "b", BaseValue.valueOf((byte) 8))))));
        }

        @Test
        public void testQueryOrderByInt16() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "c", "c"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("c")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("c", BaseValue.valueOf((short) 1))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "c", BaseValue.valueOf((short) 0))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "c", BaseValue.valueOf((short) 2))),
                            new RecordResult(BaseValue.valueOf(3),

                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "c", BaseValue.valueOf((short) 4))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "c", BaseValue.valueOf((short) 6))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "c", BaseValue.valueOf((short) 8))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "c", BaseValue.valueOf((short) 3))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "c", BaseValue.valueOf((short) 5))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "c", BaseValue.valueOf((short) 7))))));
        }

        @Test
        public void testQueryOrderByInt32() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "d", "d"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryOrderByInt64() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "e", "e"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("e")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("e", BaseValue.valueOf(3L))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "e", BaseValue.valueOf(0L))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "e", BaseValue.valueOf(2L))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "e", BaseValue.valueOf(4L))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "e", BaseValue.valueOf(6L))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "e", BaseValue.valueOf(8L))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "e", BaseValue.valueOf(1L))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "e", BaseValue.valueOf(5L))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "e", BaseValue.valueOf(7L))))));
        }

        @Test
        public void testQueryOrderByFloat32() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "f", "f"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("f")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("f", BaseValue.valueOf(4.f))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "f", BaseValue.valueOf(1.f))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "f", BaseValue.valueOf(3.f))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "f", BaseValue.valueOf(5.f))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "f", BaseValue.valueOf(7.f))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "f", BaseValue.valueOf(0.f))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "f", BaseValue.valueOf(2.f))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "f", BaseValue.valueOf(6.f))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "f", BaseValue.valueOf(8.f))))));
        }

        @Test
        public void testQueryOrderByFloat64() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "g", "g"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("g")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("g", BaseValue.valueOf(5.))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "g", BaseValue.valueOf(0.))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "g", BaseValue.valueOf(2.))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "g", BaseValue.valueOf(4.))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "g", BaseValue.valueOf(6.))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "g", BaseValue.valueOf(8.))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "g", BaseValue.valueOf(1.))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "g", BaseValue.valueOf(3.))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "g", BaseValue.valueOf(7.))))));
        }

        @Test
        public void testQueryOrderByString() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "h", "h"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("h")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("h", BaseValue.valueOf("6"))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "h", BaseValue.valueOf("1"))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "h", BaseValue.valueOf("3"))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "h", BaseValue.valueOf("5"))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "h", BaseValue.valueOf("7"))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "h", BaseValue.valueOf("0"))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "h", BaseValue.valueOf("2"))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "h", BaseValue.valueOf("4"))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "h", BaseValue.valueOf("8"))))));
        }

        @Test
        public void testQueryOrderByBytes() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a", "i", "i"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("i")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("i", BaseValue.valueOf(
                                            ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false),
                                            "i",
                                            BaseValue.valueOf(ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false),
                                            "i",
                                            BaseValue.valueOf(ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false),
                                            "i",
                                            BaseValue.valueOf(ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false),
                                            "i",
                                            BaseValue.valueOf(ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false),
                                            "i",
                                            BaseValue.valueOf(ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "i",
                                            BaseValue.valueOf(ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "i",
                                            BaseValue.valueOf(ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true), "i",
                                            BaseValue.valueOf(
                                                    ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))))));
        }

        @Test
        public void testQueryOrderByMixed() {
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("d", "x"),
                            List.of(new OrderByDesc("a"), new OrderByDesc("d")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("x", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("x", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("x", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("x", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("x", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("x", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("x", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("x", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("x", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryOrderByInvalid() {
            assertThrows(SwValidationException.class,
                    () -> this.memoryTable.query(
                            Long.MAX_VALUE,
                            Map.of("a", "a"),
                            List.of(new OrderByDesc("x")),
                            null,
                            false,
                            false),
                    "invalid order by column");
        }

        @Test
        public void testQueryFilterNullEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
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
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))))));
        }


        @Test
        public void testQueryFilterBoolEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("a"), createConstant(true)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(
                            new RecordResult(BaseValue.valueOf(2), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(4), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(6), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(8))),
                            new RecordResult(BaseValue.valueOf(8), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(0))))));
        }

        @Test
        public void testQueryFilterInt8Equal() {
            var columns = Map.of("b", "b");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results, is(List.of(
                    new RecordResult(BaseValue.valueOf(5), false, Map.of("b", BaseValue.valueOf((byte) 5))))));
        }

        @Test
        public void testQueryFilterInt16Equal() {
            var columns = Map.of("c", "c");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results, is(List.of(
                    new RecordResult(BaseValue.valueOf(4), false, Map.of("c", BaseValue.valueOf((short) 5))))));
        }

        @Test
        public void testQueryFilterInt32Equal() {
            var columns = Map.of("d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(3), false, Map.of("d", BaseValue.valueOf(5))))));
        }

        @Test
        public void testQueryFilterInt64Equal() {
            var columns = Map.of("e", "e");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(2), false, Map.of("e", BaseValue.valueOf(5L))))));
        }

        @Test
        public void testQueryFilterFloat32Equal() {
            var columns = Map.of("f", "f");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(1), false, Map.of("f", BaseValue.valueOf(5.f))))));
        }

        @Test
        public void testQueryFilterFloat64Equal() {
            var columns = Map.of("g", "g");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("g", BaseValue.valueOf(5.))))));
        }

        @Test
        public void testQueryFilterStringEqual() {
            var columns = Map.of("h", "h");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(9), false, Map.of("h", BaseValue.valueOf("5"))))));
        }

        @Test
        public void testQueryFilterBytesEqual() {
            var columns = Map.of("i", "i");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(
                                            new TableQueryFilter.Column("i"),
                                            createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(8),
                            false,
                            Map.of("i", BaseValue.valueOf(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))))));
        }

        @Test
        public void testQueryFilterBoolLess() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("a"), createConstant(true)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(7))))));
        }

        @Test
        public void testQueryFilterInt8Less() {
            var columns = Map.of("b", "b");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("b")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(9), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("b", BaseValue.valueOf((byte) 0))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("b", BaseValue.valueOf((byte) 1))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("b", BaseValue.valueOf((byte) 2))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("b", BaseValue.valueOf((byte) 3))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("b", BaseValue.valueOf((byte) 4))))));
        }

        @Test
        public void testQueryFilterInt16Less() {
            var columns = Map.of("c", "c");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("c")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(8), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("c", BaseValue.valueOf((short) 0))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("c", BaseValue.valueOf((short) 1))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("c", BaseValue.valueOf((short) 2))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("c", BaseValue.valueOf((short) 3))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("c", BaseValue.valueOf((short) 4))))));
        }

        @Test
        public void testQueryFilterInt32Less() {
            var columns = Map.of("d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(7), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("d", BaseValue.valueOf(4))))));
        }

        @Test
        public void testQueryFilterInt64Less() {
            var columns = Map.of("e", "e");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("e")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(6), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("e", BaseValue.valueOf(0L))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("e", BaseValue.valueOf(1L))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("e", BaseValue.valueOf(2L))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("e", BaseValue.valueOf(3L))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("e", BaseValue.valueOf(4L))))));
        }

        @Test
        public void testQueryFilterFloat32Less() {
            var columns = Map.of("f", "f");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("f")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(5), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("f", BaseValue.valueOf(0.f))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("f", BaseValue.valueOf(1.f))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("f", BaseValue.valueOf(2.f))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("f", BaseValue.valueOf(3.f))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("f", BaseValue.valueOf(4.f))))));
        }

        @Test
        public void testQueryFilterFloat64Less() {
            var columns = Map.of("g", "g");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("g")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(4), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("g", BaseValue.valueOf(0.))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("g", BaseValue.valueOf(1.))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("g", BaseValue.valueOf(2.))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("g", BaseValue.valueOf(3.))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("g", BaseValue.valueOf(4.))))));
        }

        @Test
        public void testQueryFilterStringLess() {
            var columns = Map.of("h", "h");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("h")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(3), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("h", BaseValue.valueOf("0"))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("h", BaseValue.valueOf("1"))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("h", BaseValue.valueOf("2"))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("h", BaseValue.valueOf("3"))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("h", BaseValue.valueOf("4"))))));
        }

        @Test
        public void testQueryFilterBytesLess() {
            var columns = Map.of("i", "i");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("i")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS)
                                    .operands(List.of(
                                            new TableQueryFilter.Column("i"),
                                            createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of()),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("i", BaseValue.valueOf(
                                            ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8))))))));
        }

        @Test
        public void testQueryFilterBoolLessEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("a"), createConstant(true)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(8), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryFilterInt8LessEqual() {
            var columns = Map.of("b", "b");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("b")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(9), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("b", BaseValue.valueOf((byte) 0))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("b", BaseValue.valueOf((byte) 1))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("b", BaseValue.valueOf((byte) 2))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("b", BaseValue.valueOf((byte) 3))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("b", BaseValue.valueOf((byte) 4))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("b", BaseValue.valueOf((byte) 5))))));
        }

        @Test
        public void testQueryFilterInt16LessEqual() {
            var columns = Map.of("c", "c");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("c")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(8), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("c", BaseValue.valueOf((short) 0))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("c", BaseValue.valueOf((short) 1))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("c", BaseValue.valueOf((short) 2))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("c", BaseValue.valueOf((short) 3))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("c", BaseValue.valueOf((short) 4))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("c", BaseValue.valueOf((short) 5))))));
        }

        @Test
        public void testQueryFilterInt32LessEqual() {
            var columns = Map.of("d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(7), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("d", BaseValue.valueOf(5))))));
        }

        @Test
        public void testQueryFilterInt64LessEqual() {
            var columns = Map.of("e", "e");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("e")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(6), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("e", BaseValue.valueOf(0L))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("e", BaseValue.valueOf(1L))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("e", BaseValue.valueOf(2L))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("e", BaseValue.valueOf(3L))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("e", BaseValue.valueOf(4L))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("e", BaseValue.valueOf(5L))))));
        }

        @Test
        public void testQueryFilterFloat32LessEqual() {
            var columns = Map.of("f", "f");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("f")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(5), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("f", BaseValue.valueOf(0.f))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("f", BaseValue.valueOf(1.f))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("f", BaseValue.valueOf(2.f))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("f", BaseValue.valueOf(3.f))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("f", BaseValue.valueOf(4.f))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("f", BaseValue.valueOf(5.f))))));
        }

        @Test
        public void testQueryFilterFloat64LessEqual() {
            var columns = Map.of("g", "g");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("g")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(4), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("g", BaseValue.valueOf(0.))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("g", BaseValue.valueOf(1.))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("g", BaseValue.valueOf(2.))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("g", BaseValue.valueOf(3.))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("g", BaseValue.valueOf(4.))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("g", BaseValue.valueOf(5.))))));
        }

        @Test
        public void testQueryFilterStringLessEqual() {
            var columns = Map.of("h", "h");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("h")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(3), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("h", BaseValue.valueOf("0"))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("h", BaseValue.valueOf("1"))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("h", BaseValue.valueOf("2"))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("h", BaseValue.valueOf("3"))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("h", BaseValue.valueOf("4"))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("h", BaseValue.valueOf("5"))))));
        }

        @Test
        public void testQueryFilterBytesLessEqual() {
            var columns = Map.of("i", "i");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("i")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.LESS_EQUAL)
                                    .operands(List.of(
                                            new TableQueryFilter.Column("i"),
                                            createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of()),
                            new RecordResult(BaseValue.valueOf(3),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("0".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(5),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("i", BaseValue.valueOf(
                                            ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))))));
        }

        @Test
        public void testQueryFilterBoolGreater() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("a"), createConstant(false)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(
                            new RecordResult(BaseValue.valueOf(8), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(2), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(4), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(6), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryFilterInt8Greater() {
            var columns = Map.of("b", "b");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("b")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(6), false, Map.of("b", BaseValue.valueOf((byte) 6))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("b", BaseValue.valueOf((byte) 7))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("b", BaseValue.valueOf((byte) 8))))));
        }

        @Test
        public void testQueryFilterInt16Greater() {
            var columns = Map.of("c", "c");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("c")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(5), false, Map.of("c", BaseValue.valueOf((short) 6))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("c", BaseValue.valueOf((short) 7))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("c", BaseValue.valueOf((short) 8))))));
        }

        @Test
        public void testQueryFilterInt32Greater() {
            var columns = Map.of("d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(4), false, Map.of("d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryFilterInt64Greater() {
            var columns = Map.of("e", "e");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("e")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(3), false, Map.of("e", BaseValue.valueOf(6L))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("e", BaseValue.valueOf(7L))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("e", BaseValue.valueOf(8L))))));
        }

        @Test
        public void testQueryFilterFloat32Greater() {
            var columns = Map.of("f", "f");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("f")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(2), false, Map.of("f", BaseValue.valueOf(6.f))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("f", BaseValue.valueOf(7.f))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("f", BaseValue.valueOf(8.f))))));
        }

        @Test
        public void testQueryFilterFloat64Greater() {
            var columns = Map.of("g", "g");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("g")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("g"), createConstant(5.)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(1), false, Map.of("g", BaseValue.valueOf(6.))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("g", BaseValue.valueOf(7.))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("g", BaseValue.valueOf(8.))))));
        }

        @Test
        public void testQueryFilterStringGreater() {
            var columns = Map.of("h", "h");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("g")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("h", BaseValue.valueOf("6"))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("h", BaseValue.valueOf("7"))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("h", BaseValue.valueOf("8"))))));
        }

        @Test
        public void testQueryFilterBytesGreater() {
            var columns = Map.of("i", "i");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("i")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER)
                                    .operands(List.of(
                                            new TableQueryFilter.Column("i"),
                                            createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("i", BaseValue.valueOf(
                                            ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("i", BaseValue.valueOf(
                                            ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8))))))));
        }

        @Test
        public void testQueryFilterBoolGreaterEqual() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("a"), createConstant(false)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(8), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(1), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryFilterInt8GreaterEqual() {
            var columns = Map.of("b", "b");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("b")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("b"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(5), false, Map.of("b", BaseValue.valueOf((byte) 5))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("b", BaseValue.valueOf((byte) 6))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("b", BaseValue.valueOf((byte) 7))),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("b", BaseValue.valueOf((byte) 8))))));
        }

        @Test
        public void testQueryFilterInt16GreaterEqual() {
            var columns = Map.of("c", "c");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("c")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("c"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(4), false, Map.of("c", BaseValue.valueOf((short) 5))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("c", BaseValue.valueOf((short) 6))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("c", BaseValue.valueOf((short) 7))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("c", BaseValue.valueOf((short) 8))))));
        }

        @Test
        public void testQueryFilterInt32GreaterEqual() {
            var columns = Map.of("d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("d")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("d"), createConstant(5)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(3), false, Map.of("d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryFilterInt64GreaterEqual() {
            var columns = Map.of("e", "e");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("e")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("e"), createConstant(5L)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(2), false, Map.of("e", BaseValue.valueOf(5L))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("e", BaseValue.valueOf(6L))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("e", BaseValue.valueOf(7L))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("e", BaseValue.valueOf(8L))))));
        }

        @Test
        public void testQueryFilterFloat32GreaterEqual() {
            var columns = Map.of("f", "f");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("f")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("f"), createConstant(5.f)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(1), false, Map.of("f", BaseValue.valueOf(5.f))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("f", BaseValue.valueOf(6.f))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("f", BaseValue.valueOf(7.f))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("f", BaseValue.valueOf(8.f))))));
        }

        @Test
        public void testQueryFilterFloat64GreaterEqual() {
            var columns = Map.of("g", "g");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("g")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("g"), this.createConstant(5.)))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("g", BaseValue.valueOf(5.))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("g", BaseValue.valueOf(6.))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("g", BaseValue.valueOf(7.))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("g", BaseValue.valueOf(8.))))));
        }

        @Test
        public void testQueryFilterStringGreaterEqual() {
            var columns = Map.of("h", "h");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("g")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("h"), createConstant("5")))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(9), false, Map.of("h", BaseValue.valueOf("5"))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("h", BaseValue.valueOf("6"))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("h", BaseValue.valueOf("7"))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("h", BaseValue.valueOf("8"))))));
        }

        @Test
        public void testQueryFilterBytesGreaterEqual() {
            var columns = Map.of("i", "i");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("i")),
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.GREATER_EQUAL)
                                    .operands(List.of(
                                            new TableQueryFilter.Column("i"),
                                            createConstant(ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8)))))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("i", BaseValue.valueOf(
                                            ByteBuffer.wrap("5".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("6".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("i",
                                            BaseValue.valueOf(ByteBuffer.wrap("7".getBytes(StandardCharsets.UTF_8))))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("i", BaseValue.valueOf(
                                            ByteBuffer.wrap("8".getBytes(StandardCharsets.UTF_8))))))));
        }

        @Test
        public void testQueryFilterFuzzyEqual() {
            var columns = Map.of("h", "h");
            this.memoryTable.update(
                    new TableSchemaDesc("key",
                            List.of(ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                                    ColumnSchemaDesc.builder().name("h").type("INT32").build())),
                    List.of(Map.of("key", "a", "h", "5")));
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(Operator.EQUAL)
                                    .operands(List.of(
                                            new TableQueryFilter.Column("h"),
                                            createConstant("5")))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(9), false, Map.of("h", BaseValue.valueOf("5"))),
                            new RecordResult(BaseValue.valueOf(10), false, Map.of("h", BaseValue.valueOf(5))))));
        }

        @Test
        public void testQueryFilterNot() {
            var columns = Map.of("d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
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
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(7), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryFilterAnd() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
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
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(8), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(2), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(4))))));
        }

        @Test
        public void testQueryFilterMultiAnd() {
            var columns = Map.of("a", "a", "b", "b", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("a"), new OrderByDesc("b"), new OrderByDesc("d")),
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
                                                    .operator(TableQueryFilter.Operator.LESS)
                                                    .operands(List.of(
                                                            new TableQueryFilter.Column("b"),
                                                            createConstant(8)))
                                                    .build(),
                                            TableQueryFilter.builder()
                                                    .operator(TableQueryFilter.Operator.EQUAL)
                                                    .operands(List.of(
                                                            new TableQueryFilter.Column("a"),
                                                            createConstant(true)))
                                                    .build()))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(2),
                            false,
                            Map.of("a", BaseValue.valueOf(true),
                                    "b", BaseValue.valueOf(Byte.parseByte("2")),
                                    "d", BaseValue.valueOf(4))))));
        }

        @Test
        public void testQueryFilterOr() {
            var columns = Map.of("a", "a", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
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
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("a", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(9), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(1), false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(8), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(2), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(4), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(6), false,
                                    Map.of("a", BaseValue.valueOf(true), "d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryFilterMultiOr() {
            var columns = Map.of("a", "a", "b", "b", "d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
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
                                                    .operator(TableQueryFilter.Operator.LESS)
                                                    .operands(List.of(
                                                            new TableQueryFilter.Column("b"),
                                                            createConstant(2)))
                                                    .build(),
                                            TableQueryFilter.builder()
                                                    .operator(TableQueryFilter.Operator.EQUAL)
                                                    .operands(List.of(
                                                            new TableQueryFilter.Column("a"),
                                                            createConstant(true)))
                                                    .build()))
                                    .build(),
                            false,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(0),
                                    false,
                                    Map.of("b", BaseValue.valueOf(Byte.parseByte("0")), "d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(7),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "b", BaseValue.valueOf(Byte.parseByte("7")))),
                            new RecordResult(BaseValue.valueOf(9),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false), "d", BaseValue.valueOf(1))),
                            new RecordResult(BaseValue.valueOf(1),
                                    false,
                                    Map.of("a", BaseValue.valueOf(false),
                                            "b", BaseValue.valueOf(Byte.parseByte("1")),
                                            "d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(8),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true),
                                            "b", BaseValue.valueOf(Byte.parseByte("8")),
                                            "d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(2),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true),
                                            "b", BaseValue.valueOf(Byte.parseByte("2")),
                                            "d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(4),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true),
                                            "b", BaseValue.valueOf(Byte.parseByte("4")),
                                            "d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(6),
                                    false,
                                    Map.of("a", BaseValue.valueOf(true),
                                            "b", BaseValue.valueOf(Byte.parseByte("6")),
                                            "d", BaseValue.valueOf(8))))));
        }

        @Test
        public void testQueryUnknown() {
            var columns = Map.of("z", "z");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            List.of(new OrderByDesc("z")),
                            null,
                            false,
                            false));
            assertThat(results,
                    is(IntStream.range(0, 10).mapToObj(i -> new RecordResult(BaseValue.valueOf(i), false, Map.of()))
                            .collect(Collectors.toList())));
        }

        @Test
        public void testQueryKeepNone() {
            var columns = Map.of("d", "d");
            var results = ImmutableList.copyOf(
                    this.memoryTable.query(
                            Long.MAX_VALUE,
                            columns,
                            null,
                            TableQueryFilter.builder()
                                    .operator(TableQueryFilter.Operator.EQUAL)
                                    .operands(List.of(new TableQueryFilter.Column("b"), createConstant(7)))
                                    .build(),
                            true,
                            false));
            assertThat(results,
                    is(List.of(new RecordResult(BaseValue.valueOf(7), false, new HashMap<>() {
                        {
                            put("d", null);
                        }
                    }))));
        }

        @Test
        public void testScanInitialEmptyTable() {
            this.memoryTable = createInstance("test");
            var results = this.memoryTable.scan(Long.MAX_VALUE, Map.of("a", "a"),
                    null, null, false, null, null, false, false);
            assertThat("empty", ImmutableList.copyOf(results), empty());
        }

        @Test
        public void testScanEmptyTableWithSchema() {
            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                    List.of());
            var results = this.memoryTable.scan(Long.MAX_VALUE, Map.of("a", "a"),
                    null, null, false, null, null, false, false);
            assertThat("empty", ImmutableList.copyOf(results), empty());
        }

        @Test
        public void testScanEmptyTableWithDeletedRecords() {
            this.memoryTable = createInstance("test");
            this.memoryTable.update(
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                    List.of(Map.of("k", "0", "-", "1")));
            var results = this.memoryTable.scan(Long.MAX_VALUE, Map.of("a", "a"),
                    null, null, false, null, null, false, false);
            assertThat("deleted", ImmutableList.copyOf(results),
                    is(List.of(new RecordResult(BaseValue.valueOf("0"), true, null))));
        }

        @Test
        public void testScanColumnAliases() {
            assertThat(ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE,
                                    Map.of("a", "x", "d", "y"),
                                    null,
                                    null,
                                    false,
                                    null,
                                    null,
                                    false,
                                    false)),
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("y", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(8))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of("x", BaseValue.valueOf(false))),
                            new RecordResult(BaseValue.valueOf(8), false,
                                    Map.of("x", BaseValue.valueOf(true), "y", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false,
                                    Map.of("x", BaseValue.valueOf(false), "y", BaseValue.valueOf(1))))));
        }

        @Test
        public void testScanNonScalar() {
            this.memoryTable.update(
                    new TableSchemaDesc(null,
                            List.of(ColumnSchemaDesc.builder().name("key").type("INT32").build(),
                                    ColumnSchemaDesc.builder()
                                            .name("x")
                                            .type("LIST")
                                            .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                            .build())),
                    List.of(Map.of("key", "1", "x", List.of("a"))));
            var results = this.memoryTable.scan(
                    Long.MAX_VALUE,
                    Map.of("key", "key", "x", "x"),
                    "1",
                    "INT32",
                    true,
                    "1",
                    "INT32",
                    true,
                    false);
            assertThat(ImmutableList.copyOf(results),
                    is(List.of(new RecordResult(BaseValue.valueOf(1), false,
                            Map.of("key", BaseValue.valueOf(1), "x", BaseValue.valueOf(List.of(10)))))));
        }


        @Test
        public void testScanStartEnd() {
            assertThat("start,non-inclusive",
                    ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE,
                                    Map.of("d", "d"),
                                    "5",
                                    "INT32",
                                    false,
                                    null,
                                    null,
                                    false,
                                    false)),
                    is(List.of(new RecordResult(BaseValue.valueOf(6), false, Map.of("d", BaseValue.valueOf(8))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("d", BaseValue.valueOf(1))))));

            assertThat("start,inclusive",
                    ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE, Map.of("d", "d"),
                                    "5",
                                    "INT32",
                                    true,
                                    null,
                                    null,
                                    false,
                                    false)),
                    is(List.of(new RecordResult(BaseValue.valueOf(5), false, Map.of("d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("d", BaseValue.valueOf(8))),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("d", BaseValue.valueOf(1))))));

            assertThat("end,non-inclusive",
                    ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE,
                                    Map.of("d", "d"),
                                    null,
                                    null,
                                    false,
                                    "5",
                                    "INT32",
                                    false,
                                    false)),
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("d", BaseValue.valueOf(6))))));

            assertThat("end,inclusive",
                    ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE,
                                    Map.of("d", "d"),
                                    null,
                                    null,
                                    false,
                                    "5",
                                    "INT32",
                                    true,
                                    false)),
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("d", BaseValue.valueOf(7))))));

            assertThat("start+end",
                    ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE,
                                    Map.of("d", "d"),
                                    "2",
                                    "INT32",
                                    true,
                                    "5",
                                    "INT32",
                                    false,
                                    false)),
                    is(List.of(new RecordResult(BaseValue.valueOf(2), false, Map.of("d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("d", BaseValue.valueOf(6))))));
        }

        @Test
        public void testScanKeepNone() {
            assertThat("test",
                    ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE, Map.of("d", "d"),
                                    null, null, false, null, null, false, true)),
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of("d", BaseValue.valueOf(2))),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of("d", BaseValue.valueOf(3))),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of("d", BaseValue.valueOf(4))),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of("d", BaseValue.valueOf(5))),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of("d", BaseValue.valueOf(6))),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of("d", BaseValue.valueOf(7))),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of("d", BaseValue.valueOf(8))),
                            new RecordResult(BaseValue.valueOf(7), false, new HashMap<>() {
                                {
                                    put("d", null);
                                }
                            }),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of("d", BaseValue.valueOf(0))),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of("d", BaseValue.valueOf(1))))));
        }

        @Test
        public void testScanUnknown() {
            assertThat(ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE, Map.of("z", "z"),
                                    null, null, false, null, null, false, false)),
                    is(List.of(new RecordResult(BaseValue.valueOf(0), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(1), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(2), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(3), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(4), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(5), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(6), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(7), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(8), false, Map.of()),
                            new RecordResult(BaseValue.valueOf(9), false, Map.of()))));
        }

        @Test
        public void testQueryScanVersion() {
            var t1 = this.memoryTable.getLastRevision();
            var t2 = this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("key", "0", "d", "7", "e", "8"),
                            Map.of("key", "9", "d", "6")));
            var t3 = this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("key", "0", "d", "8", "h", "t"),
                            Map.of("key", "9", "-", "1")));
            var t4 = this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("key", "0", "d", "9"),
                            Map.of("key", "9", "d", "8")));
            this.memoryTable.update(this.memoryTable.getSchema().toTableSchemaDesc(),
                    List.of(Map.of("key", "a", "d", "7")));
            var columns = Map.of("d", "d", "e", "e", "h", "h");
            var expected = List.of(
                    new RecordResult(BaseValue.valueOf(0),
                            false,
                            Map.of("d", BaseValue.valueOf(2), "e", BaseValue.valueOf(3L), "h", BaseValue.valueOf("6"))),
                    new RecordResult(BaseValue.valueOf(1),
                            false,
                            Map.of("d", BaseValue.valueOf(3), "e", BaseValue.valueOf(4L), "h", BaseValue.valueOf("7"))),
                    new RecordResult(BaseValue.valueOf(2),
                            false,
                            Map.of("d", BaseValue.valueOf(4), "e", BaseValue.valueOf(5L), "h", BaseValue.valueOf("8"))),
                    new RecordResult(BaseValue.valueOf(3),
                            false,
                            Map.of("d", BaseValue.valueOf(5), "e", BaseValue.valueOf(6L))),
                    new RecordResult(BaseValue.valueOf(4),
                            false,
                            Map.of("d", BaseValue.valueOf(6), "e", BaseValue.valueOf(7L), "h", BaseValue.valueOf("0"))),
                    new RecordResult(BaseValue.valueOf(5),
                            false,
                            Map.of("d", BaseValue.valueOf(7), "e", BaseValue.valueOf(8L), "h", BaseValue.valueOf("1"))),
                    new RecordResult(BaseValue.valueOf(6),
                            false,
                            Map.of("d", BaseValue.valueOf(8), "h", BaseValue.valueOf("2"))),
                    new RecordResult(BaseValue.valueOf(7),
                            false,
                            Map.of("e", BaseValue.valueOf(0L), "h", BaseValue.valueOf("3"))),
                    new RecordResult(BaseValue.valueOf(8),
                            false,
                            Map.of("d", BaseValue.valueOf(0), "e", BaseValue.valueOf(1L), "h", BaseValue.valueOf("4"))),
                    new RecordResult(BaseValue.valueOf(9),
                            false,
                            Map.of("d", BaseValue.valueOf(1),
                                    "e", BaseValue.valueOf(2L),
                                    "h", BaseValue.valueOf("5"))));
            assertThat(ImmutableList.copyOf(this.memoryTable.query(t1, columns, null, null, false, false)),
                    is(expected));
            assertThat(ImmutableList.copyOf(
                            this.memoryTable.scan(t1, columns, null, null, false, null, null, false, false)),
                    is(expected));
            for (var result : expected) {
                result.setValues(new HashMap<>(result.getValues()));
            }
            expected.get(0).getValues().putAll(Map.of("d", BaseValue.valueOf(7), "e", BaseValue.valueOf(8L)));
            expected.get(9).getValues().put("d", BaseValue.valueOf(6));
            assertThat(ImmutableList.copyOf(this.memoryTable.query(t2, columns, null, null, false, false)),
                    is(expected));
            assertThat(ImmutableList.copyOf(
                            this.memoryTable.scan(t2, columns, null, null, false, null, null, false, false)),
                    is(expected));
            expected.get(0).getValues().putAll(Map.of("d", BaseValue.valueOf(8), "h", BaseValue.valueOf("t")));
            expected.get(9).setDeleted(true);
            expected.get(9).setValues(null);
            assertThat(ImmutableList.copyOf(this.memoryTable.query(t3, columns, null, null, false, false)),
                    is(expected));
            assertThat(ImmutableList.copyOf(
                            this.memoryTable.scan(t3, columns, null, null, false, null, null, false, false)),
                    is(expected));
            expected.get(0).getValues().put("d", BaseValue.valueOf(9));
            expected.get(9).setDeleted(false);
            expected.get(9).setValues(Map.of("d", BaseValue.valueOf(8)));
            assertThat(ImmutableList.copyOf(this.memoryTable.query(t4, columns, null, null, false, false)),
                    is(expected));
            assertThat(ImmutableList.copyOf(
                            this.memoryTable.scan(t4, columns,
                                    null, null, false, null, null, false, false)),
                    is(expected));
            expected = new ArrayList<>(expected);
            expected.add(new RecordResult(BaseValue.valueOf(10),
                    false,
                    Map.of("d", BaseValue.valueOf(7))));
            assertThat(ImmutableList.copyOf(this.memoryTable.query(Long.MAX_VALUE, columns, null, null, false, false)),
                    is(expected));
            assertThat(ImmutableList.copyOf(
                            this.memoryTable.scan(Long.MAX_VALUE, columns,
                                    null, null, false, null, null, false, false)),
                    is(expected));
        }
    }
}
