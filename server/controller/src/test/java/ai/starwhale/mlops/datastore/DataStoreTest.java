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

package ai.starwhale.mlops.datastore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.ColumnSchemaDesc.KeyValuePairSchema;
import ai.starwhale.mlops.datastore.TableQueryFilter.Constant;
import ai.starwhale.mlops.datastore.TableQueryFilter.Operator;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.BytesValue;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class DataStoreTest {

    private FileSystem fs;
    private volatile DataStore dataStore;

    private StorageAccessService storageAccessService;

    @Builder
    private static class DataStoreParams {

        @Default
        int walMaxFileSize = 4096;
        @Default
        int ossMaxAttempts = 3;
        @Default
        String dataRootPath = "";
        @Default
        String dumpInterval = "1s";
        @Default
        String minNoUpdatePeriod = "1s";
        @Default
        int minWalIdGap = 10;
        @Default
        String compressionCodec = "SNAPPY";
        @Default
        String rowGroupSize = "1MB";
        @Default
        String pageSize = "1KB";
        @Default
        int pageRowCountLimit = 1000;
    }

    @BeforeEach
    public void setUp() throws IOException {
        this.fs = Jimfs.newFileSystem(Configuration.unix());
        ((Logger) LoggerFactory.getLogger("org.apache.parquet")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org.apache.hadoop")).setLevel(Level.ERROR);
        this.storageAccessService = new StorageAccessServiceMemory();
        this.createDateStore(DataStoreParams.builder().build());
    }

    @SneakyThrows
    private void createDateStore(DataStoreParams params) {
        this.dataStore = new DataStore(this.storageAccessService,
                params.walMaxFileSize,
                this.fs.getPath("/wal_cache"),
                params.ossMaxAttempts,
                params.dataRootPath,
                params.dumpInterval,
                params.minNoUpdatePeriod,
                params.minWalIdGap,
                params.compressionCodec,
                params.rowGroupSize,
                params.pageSize,
                params.pageRowCountLimit)
                .start();
    }

    @AfterEach
    @SneakyThrows
    public void tearDown() {
        this.dataStore.terminate();
        this.fs.close();
    }

    @Test
    public void testList() {
        assertThat("empty", this.dataStore.list(Set.of("")), empty());
        this.dataStore.update("t1",
                new TableSchemaDesc("k", List.of(ColumnSchemaDesc.string().name("k").build())),
                List.of());
        this.dataStore.update("t2",
                new TableSchemaDesc("k", List.of(ColumnSchemaDesc.string().name("k").build())),
                List.of());
        this.dataStore.update("test",
                new TableSchemaDesc("k", List.of(ColumnSchemaDesc.string().name("k").build())),
                List.of());
        assertThat("all", this.dataStore.list(Set.of("")), containsInAnyOrder("t1", "t2", "test"));
        assertThat("all", this.dataStore.list(Set.of("t")), containsInAnyOrder("t1", "t2", "test"));
        assertThat("partial", this.dataStore.list(Set.of("te")), containsInAnyOrder("test"));
        assertThat("partial", this.dataStore.list(Set.of("te", "t2")), containsInAnyOrder("t2", "test"));
        assertThat("none", this.dataStore.list(Set.of("t3")), empty());
    }

    @Test
    public void testUpdate() {
        var objDesc = ColumnSchemaDesc.objectOf("test-py", ColumnSchemaDesc.string().name("property")).name("v");
        var desc = new TableSchemaDesc("k",
                List.of(ColumnSchemaDesc.string().name("k").build(),
                        ColumnSchemaDesc.int32().name("a").build(),
                        ColumnSchemaDesc.int32().name("x").build(),
                        ColumnSchemaDesc.listOf(objDesc).name("u").build(),
                        ColumnSchemaDesc.listOf(ColumnSchemaDesc.int32()).name("l")
                                .attributes(List.of(
                                        ColumnSchemaDesc.string().index(1).build(),
                                        objDesc.index(3).build()
                                ))
                                .build()

                ));
        this.dataStore.update("t1", desc, List.of(Map.of("k", "0", "a", "1")));
        this.dataStore.update("t1", desc, List.of(Map.of("k", "0", "u", List.of(Map.of("property", "0")))));
        this.dataStore.update("t1", desc, List.of(Map.of("k", "1", "a", "2")));
        this.dataStore.update("t1", desc, List.of(Map.of("k", "1", "u", List.of(Map.of("property", "1")))));
        this.dataStore.update("t2", desc, List.of(Map.of("k", "3", "x", "2")));
        this.dataStore.update("t1", desc, List.of(Map.of("k", "0", "a", "5"), Map.of("k", "4", "-", "1")));
        this.dataStore.update("t1", desc, List.of(
                Map.of("k", "1", "l", List.of("1", "abcde", "3", Map.of("property", "4")))));
        assertThat("t1",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t1")
                                        .columns(Map.of("k", "k", "a", "a", "u", "u"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(
                        Map.of("k", "0", "a", "00000005", "u", List.of(Map.of("property", "0"))),
                        Map.of("k", "1", "a", "00000002", "u", List.of(Map.of("property", "1"))))));
        this.dataStore.update("t1", desc, List.of(Map.of("k", "0", "-", "anyString"), Map.of("k", "4", "-", "1")));
        assertThat("t1",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t1")
                                        .columns(Map.of("k", "k", "a", "a"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "1", "a", "00000002"))));
        assertThat("t2",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t2")
                                        .columns(Map.of("k", "k", "x", "x"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "3", "x", "00000002"))));

        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder().build());
        assertThat("t1",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t1")
                                        .columns(Map.of("k", "k", "a", "a", "u", "u", "l", "l"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(
                        Map.of(
                                "k", "1",
                                "a", "00000002",
                                "u", List.of(Map.of("property", "1")),
                                "l", List.of("00000001", "abcde", "00000003", Map.of("property", "4"))))));
        assertThat("t2",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t2")
                                        .columns(Map.of("k", "k", "x", "x"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "3", "x", "00000002"))));
    }

    @Test
    public void testQuery() {
        var desc = new TableSchemaDesc("k",
                List.of(ColumnSchemaDesc.string().name("k").build(),
                        ColumnSchemaDesc.int32().name("a").build()));
        this.dataStore.update("t1",
                desc,
                List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        Map.of("k", "2", "a", "3"),
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1")));
        var recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName("t1")
                .columns(Map.of("a", "a"))
                .filter(TableQueryFilter.builder()
                        .operator(TableQueryFilter.Operator.GREATER)
                        .operands(List.of(new TableQueryFilter.Column("a"), new Constant(ColumnType.INT32, 1)))
                        .build())
                .orderBy(List.of(new OrderByDesc("a")))
                .start(1)
                .limit(2)
                .build());
        assertThat("test",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("a", ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("a", "00000003"),
                        Map.of("a", "00000004"))));
        assertThat("test", recordList.getColumnHints(),
                is(Map.of("a", ColumnHintsDesc.builder()
                        .typeHints(List.of("INT32"))
                        .columnValueHints(List.of("1", "2", "3", "4", "5"))
                        .build())));

        // query with limit without deleted rows
        this.dataStore.update("t1", desc, List.of(Map.of("k", "0", "-", "1")));
        recordList = this.dataStore.query(DataStoreQueryRequest.builder().tableName("t1").limit(2).build());
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("k", "1", "a", "00000004"),
                        Map.of("k", "2", "a", "00000003"))));

        // revert deleted rows
        this.dataStore.update("t1", desc, List.of(Map.of("k", "0", "a", "5")));

        recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName("t1")
                .filter(TableQueryFilter.builder()
                        .operator(TableQueryFilter.Operator.GREATER)
                        .operands(List.of(new TableQueryFilter.Column("a"), new Constant(ColumnType.INT32, 1)))
                        .build())
                .orderBy(List.of(new OrderByDesc("a")))
                .start(1)
                .limit(2)
                .build());
        assertThat("all columns",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k", ColumnType.STRING, "a", ColumnType.INT32)));
        assertThat("all columns",
                recordList.getRecords(),
                is(List.of(Map.of("k", "2", "a", "00000003"),
                        Map.of("k", "1", "a", "00000004"))));
        assertThat("all columns", recordList.getColumnHints(),
                is(Map.of("k", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("0", "1", "2", "3", "4"))
                                .build(),
                        "a", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT32"))
                                .columnValueHints(List.of("1", "2", "3", "4", "5"))
                                .build())));

        desc.setColumnSchemaList(new ArrayList<>(desc.getColumnSchemaList()));
        desc.getColumnSchemaList().addAll(
                List.of(ColumnSchemaDesc.builder().name("x:link/url").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("x:link/mime_type").type("STRING").build()));
        this.dataStore.update("t1",
                desc,
                List.of(Map.of("k", "5", "x:link/url", "http://test.com/1.jpg", "x:link/mime_type", "image/jpeg"),
                        Map.of("k", "6", "x:link/url", "http://test.com/2.png", "x:link/mime_type", "image/png")));
        recordList = this.dataStore.query(DataStoreQueryRequest.builder().tableName("t1").build());
        assertThat("object type",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k",
                        ColumnType.STRING,
                        "a",
                        ColumnType.INT32,
                        "x:link/url",
                        ColumnType.STRING,
                        "x:link/mime_type",
                        ColumnType.STRING)));
        assertThat("object type",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000005"),
                        Map.of("k", "1", "a", "00000004"),
                        Map.of("k", "2", "a", "00000003"),
                        Map.of("k", "3", "a", "00000002"),
                        Map.of("k", "4", "a", "00000001"),
                        Map.of("k", "5", "x:link/url", "http://test.com/1.jpg", "x:link/mime_type", "image/jpeg"),
                        Map.of("k", "6", "x:link/url", "http://test.com/2.png", "x:link/mime_type", "image/png"))));
        assertThat("object type", recordList.getColumnHints(),
                is(Map.of("k", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("0", "1", "2", "3", "4", "5", "6"))
                                .build(),
                        "a", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT32"))
                                .columnValueHints(List.of("1", "2", "3", "4", "5"))
                                .build(),
                        "x:link/url", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("http://test.com/1.jpg", "http://test.com/2.png"))
                                .build(),
                        "x:link/mime_type", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("image/jpeg", "image/png"))
                                .build())));

        recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName("t1")
                .columns(Map.of("x", "y", "x:link/url", "url"))
                .build());
        assertThat("object type alias",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("url", ColumnType.STRING, "y:link/mime_type", ColumnType.STRING)));
        assertThat("object type alias",
                recordList.getRecords(),
                is(List.of(Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of("url", "http://test.com/1.jpg", "y:link/mime_type", "image/jpeg"),
                        Map.of("url", "http://test.com/2.png", "y:link/mime_type", "image/png"))));
        assertThat("object type alias", recordList.getColumnHints(),
                is(Map.of("url", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("http://test.com/1.jpg", "http://test.com/2.png"))
                                .build(),
                        "y:link/mime_type", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("image/jpeg", "image/png"))
                                .build())));

        // query non exist table
        final String tableNonExist = "tableNonExist";
        assertThrows(SwValidationException.class, () -> this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName(tableNonExist).build()));
        recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName(tableNonExist).ignoreNonExistingTable(true).build());
        assertThat("result of non exist table", recordList.getColumnSchemaMap().isEmpty());
        assertThat("result of non exist table", recordList.getRecords().isEmpty());
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build())),
                List.of(Map.of("k", "0")));
        var req = DataStoreQueryRequest.builder()
                .tableName("t1")
                .columns(Map.of("k", "k"))
                .filter(TableQueryFilter.builder()
                        .operator(Operator.EQUAL)
                        .operands(List.of(new TableQueryFilter.Column("k"), new Constant(ColumnType.STRING, "0")))
                        .build());

        recordList = this.dataStore.query(req.build());
        assertThat("typed", recordList.getColumnSchemaMap(), notNullValue());
        assertThat("typed", recordList.getRecords(), is(List.of(Map.of("k", "00000000"), Map.of("k", "0"))));

        recordList = this.dataStore.query(req.encodeWithType(true).build());
        assertThat("mixed", recordList.getColumnSchemaMap(), nullValue());
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("k", Map.of("type", "INT32", "value", "00000000")),
                        Map.of("k", Map.of("type", "STRING", "value", "0")))));
    }

    @Test
    public void testQueryMultipleTimesForBytes() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(
                                ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("BYTES").build()
                        )),
                List.of(
                        Map.of("k", "0", "a", "Nw=="),
                        Map.of("k", "1", "a", "OA==")
                )
        );
        class EncodeString implements BiFunction<String, Boolean, Object> {

            @Override
            public Object apply(String str, Boolean rawResult) {
                return BaseValue.encode(new BytesValue(ByteBuffer.wrap((str).getBytes(StandardCharsets.UTF_8))),
                        rawResult,
                        false);
            }
        }

        var encodeString = new EncodeString();
        var testParams = new boolean[] {true, false, true, false, true, false};
        for (boolean rawResult : testParams) {
            var recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                    .tableName("t1")
                    .columns(Map.of("a", "a"))
                    .rawResult(rawResult)
                    .build()).getRecords();
            assertThat(recordList,
                    is(List.of(
                            Map.of("a", encodeString.apply("7", rawResult)),
                            Map.of("a", encodeString.apply("8", rawResult))
                    ))
            );
        }

    }

    @Test
    public void testScanOneTable() {
        var desc = new TableSchemaDesc("k",
                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                        ColumnSchemaDesc.builder().name("l").type("FLOAT64").build()));
        this.dataStore.update("t1",
                desc,
                List.of(Map.of("k", "0", "a", "5", "l", "3ff8000000000000"), // l=1.5
                        Map.of("k", "1", "a", "4", "l", "3ff6666666666666"), // l=1.4
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                            put("l", "0000000000000000"); // 0.0
                        }},
                        Map.of("k", "3", "a", "2", "l", "3ff3333333333333"), // l=1.2
                        Map.of("k", "4", "a", "1", "l", "3ff199999999999a"))); // l=1.1
        var recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .columns(Map.of("a", "a"))
                        .keepNone(true)
                        .build()))
                .start("1")
                .startInclusive(true)
                .end("3")
                .endInclusive(true)
                .keepNone(true)
                .build());
        assertThat("test",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("a", ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("a", "00000004"),
                        new HashMap<>() {{
                            put("a", null);
                        }},
                        Map.of("a", "00000002"))));
        assertThat("test", recordList.getLastKey(), is("3"));

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .columns(Map.of("a", "a"))
                        .keepNone(true)
                        .build()))
                .start("1")
                .startInclusive(true)
                .end("3")
                .endInclusive(true)
                .limit(2)
                .build());
        assertThat("test",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("a", ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("a", "00000004"), Map.of())));
        assertThat("test", recordList.getLastKey(), is("2"));

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .keepNone(true)
                        .build()))
                .start("1")
                .startInclusive(true)
                .end("3")
                .endInclusive(true)
                .limit(3)
                .build());
        assertThat("all columns",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k", ColumnType.STRING, "a", ColumnType.INT32, "l", ColumnType.FLOAT64)));
        assertThat("all columns",
                recordList.getRecords(),
                is(List.of(
                        Map.of("k", "1", "a", "00000004", "l", "3ff6666666666666"),
                        Map.of("k", "2", "l", "0000000000000000"),
                        Map.of("k", "3", "a", "00000002", "l", "3ff3333333333333")
                )));
        assertThat("all columns", recordList.getLastKey(), is("3"));

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .keepNone(true)
                        .build()))
                .limit(0)
                .build());
        assertThat("schema only",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k", ColumnType.STRING, "a", ColumnType.INT32, "l", ColumnType.FLOAT64)));
        assertThat("schema only", recordList.getRecords(), empty());
        desc.setColumnSchemaList(new ArrayList<>(desc.getColumnSchemaList()));
        desc.getColumnSchemaList().addAll(
                List.of(ColumnSchemaDesc.builder().name("x:link/url").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("x:link/mime_type").type("STRING").build()));
        this.dataStore.update("t1",
                desc,
                List.of(Map.of("k", "5", "x:link/url", "http://test.com/1.jpg", "x:link/mime_type", "image/jpeg"),
                        Map.of("k", "6", "x:link/url", "http://test.com/2.png", "x:link/mime_type", "image/png")));
        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .build()))
                .build());
        assertThat("object type",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k",
                        ColumnType.STRING,
                        "a",
                        ColumnType.INT32,
                        "l",
                        ColumnType.FLOAT64,
                        "x:link/url",
                        ColumnType.STRING,
                        "x:link/mime_type",
                        ColumnType.STRING)));
        assertThat("object type",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000005", "l", "3ff8000000000000"),
                        Map.of("k", "1", "a", "00000004", "l", "3ff6666666666666"),
                        Map.of("k", "2", "l", "0000000000000000"),
                        Map.of("k", "3", "a", "00000002", "l", "3ff3333333333333"),
                        Map.of("k", "4", "a", "00000001", "l", "3ff199999999999a"),
                        Map.of("k", "5", "x:link/url", "http://test.com/1.jpg", "x:link/mime_type", "image/jpeg"),
                        Map.of("k", "6", "x:link/url", "http://test.com/2.png", "x:link/mime_type", "image/png"))));

        recordList = this.dataStore
                .scan(DataStoreScanRequest.builder()
                        .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .columns(Map.of("x", "y", "x:link/url", "url"))
                                .build()))
                        .build());
        assertThat("object type alias",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("url", ColumnType.STRING, "y:link/mime_type", ColumnType.STRING)));
        assertThat("object type alias",
                recordList.getRecords(),
                is(List.of(Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of("url", "http://test.com/1.jpg", "y:link/mime_type", "image/jpeg"),
                        Map.of("url", "http://test.com/2.png", "y:link/mime_type", "image/png"))));

        assertThrows(SwValidationException.class, () -> this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder().tableName("t1").build()))
                .limit(1001)
                .build()));
    }

    @Test
    public void testScanMultipleTables() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                List.of(Map.of("k", "0", "a", "00000005"),
                        Map.of("k", "1", "a", "00000004"),
                        Map.of("k", "2", "a", "00000003"),
                        Map.of("k", "3", "a", "00000002"),
                        Map.of("k", "4", "a", "00000001")));
        this.dataStore.update("t2",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("b").type("INT32").build())),
                List.of(Map.of("k", "0", "b", "00000015"),
                        Map.of("k", "2", "b", "00000013"),
                        Map.of("k", "4", "b", "00000011")));
        this.dataStore.update("t3",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                List.of(new HashMap<>() {
                    {
                        put("k", "2");
                        put("a", null);
                    }
                }));
        this.dataStore.update("t4",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                List.of(Map.of("k", "00000002")));
        var recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t3")
                                .keepNone(true)
                                .build()))
                .keepNone(true)
                .build());
        assertThat("test",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k",
                        ColumnType.STRING,
                        "a",
                        ColumnType.INT32,
                        "b",
                        ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000005", "b", "00000015"),
                        Map.of("k", "1", "a", "00000004"),
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                            put("b", "00000013");
                        }},
                        Map.of("k", "3", "a", "00000002"),
                        Map.of("k", "4", "a", "00000001", "b", "00000011"))));
        assertThat("test", recordList.getLastKey(), is("4"));
        assertThat("test", recordList.getColumnHints(), is(
                Map.of("k", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("0", "1", "2", "3", "4"))
                                .build(),
                        "a", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT32"))
                                .columnValueHints(List.of("1", "2", "3", "4", "5"))
                                .build(),
                        "b", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT32"))
                                .columnValueHints(List.of("17", "19", "21"))
                                .build())));

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t3")
                                .keepNone(true)
                                .build()))
                .build());
        assertThat("test",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k",
                        ColumnType.STRING,
                        "a",
                        ColumnType.INT32,
                        "b",
                        ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000005", "b", "00000015"),
                        Map.of("k", "1", "a", "00000004"),
                        Map.of("k", "2", "b", "00000013"),
                        Map.of("k", "3", "a", "00000002"),
                        Map.of("k", "4", "a", "00000001", "b", "00000011"))));
        assertThat("test", recordList.getLastKey(), is("4"));

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t3")
                                .keepNone(true)
                                .build()))
                .start("7")
                .build());
        assertThat("empty",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("a", ColumnType.INT32, "b", ColumnType.INT32, "k", ColumnType.STRING)));
        assertThat("empty", recordList.getRecords(), empty());
        assertThat("empty", recordList.getLastKey(), nullValue());

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .columns(Map.of("b", "a"))
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t3")
                                .keepNone(true)
                                .build()))
                .keepNone(true)
                .build());
        assertThat("alias",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k", ColumnType.STRING, "a", ColumnType.INT32)));
        assertThat("alias",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000015"),
                        Map.of("k", "1", "a", "00000004"),
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                        }},
                        Map.of("k", "3", "a", "00000002"),
                        Map.of("k", "4", "a", "00000011"))));
        assertThat("alias", recordList.getLastKey(), is("4"));

        assertThrows(SwValidationException.class, () -> this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder().tableName("t1").build(),
                        DataStoreScanRequest.TableInfo.builder().tableName("t4").build()))
                .build()));
        assertThrows(SwValidationException.class, () -> this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder().tableName("t1").build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .columns(Map.of("k", "a"))
                                .build()))
                .build()));

        // scan non exist table
        final String tableNonExist = "tableNonExist";
        var builder = DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder().tableName("t1").build(),
                        DataStoreScanRequest.TableInfo.builder().tableName(tableNonExist).build()))
                .limit(1);
        assertThrows(SwValidationException.class, () -> this.dataStore.scan(builder.build()));

        recordList = this.dataStore.scan(builder.ignoreNonExistingTable(true).build());
        assertThat("result of non exist table",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("k", ColumnType.STRING, "a", ColumnType.INT32)));
        assertThat("result of non exist table",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000005"))));

        // scan with column prefix
        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .columnPrefix("a")
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .columnPrefix("b")
                                .build()))
                .build());
        assertThat("column prefix",
                recordList.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(Map.of("ak",
                        ColumnType.STRING,
                        "bk",
                        ColumnType.STRING,
                        "aa",
                        ColumnType.INT32,
                        "bb",
                        ColumnType.INT32)));
        assertThat("column prefix",
                recordList.getRecords(),
                is(List.of(Map.of("ak", "0", "bk", "0", "aa", "00000005", "bb", "00000015"),
                        Map.of("ak", "1", "aa", "00000004"),
                        Map.of("ak", "2", "bk", "2", "aa", "00000003", "bb", "00000013"),
                        Map.of("ak", "3", "aa", "00000002"),
                        Map.of("ak", "4", "bk", "4", "aa", "00000001", "bb", "00000011"))));
    }

    @Test
    public void testSoftReferences() throws Exception {
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder()
                .walMaxFileSize(65536)
                .dumpInterval("1s")
                .minNoUpdatePeriod("1ms")
                .build());
        try {
            for (int i = 0; i < 10; ++i) {
                this.dataStore.update("t" + i,
                        new TableSchemaDesc("k",
                                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                        IntStream.range(0, 1000)
                                .mapToObj(k -> Map.of("k", (Object) String.format("%04d", k)))
                                .collect(Collectors.toList()));
            }
            // makes sure tables are dumped
            while (this.dataStore.hasDirtyTables()) {
                Thread.sleep(1000);
            }
            var buf = new ArrayList<long[]>();
            for (; ; ) {
                buf.add(new long[1024 * 1024 * 16]);
            }
        } catch (OutOfMemoryError ignored) {
            // now all soft references are garbage collected.
        }
        assertThat(this.storageAccessService.list("wal/").collect(Collectors.toList()), is(List.of("wal/wal.log.1")));
        for (int i = 0; i < 10; ++i) {
            assertThat(
                    this.dataStore.scan(DataStoreScanRequest.builder()
                            .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                    .tableName("t" + i)
                                    .build()))
                            .build()).getRecords(),
                    is(IntStream.range(0, 1000)
                            .mapToObj(k -> Map.of("k", String.format("%04d", k)))
                            .collect(Collectors.toList())));
        }

        this.dataStore.stopDump();

        // make some tables dirty
        for (int i = 0; i < 5; ++i) {
            this.dataStore.update("t" + i,
                    new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                    List.of(Map.of("k", "0")));
        }

        // restart the datastore
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder()
                .walMaxFileSize(65536)
                .build());
        assertThat(this.dataStore.hasDirtyTables(), is(true));
        assertThat(this.dataStore.list(Set.of("t")),
                is(IntStream.range(0, 10).mapToObj(k -> "t" + k).collect(Collectors.toList())));
        for (int i = 0; i < 5; ++i) {
            assertThat(
                    this.dataStore.scan(DataStoreScanRequest.builder()
                            .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                    .tableName("t" + i)
                                    .build()))
                            .build()).getRecords(),
                    is(Stream.concat(Stream.of(Map.of("k", "0")),
                                    IntStream.range(0, 999)
                                            .mapToObj(k -> Map.of("k", String.format("%04d", k))))
                            .collect(Collectors.toList())));
        }
        for (int i = 6; i < 10; ++i) {
            assertThat(
                    this.dataStore.scan(DataStoreScanRequest.builder()
                            .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                    .tableName("t" + i)
                                    .build()))
                            .build()).getRecords(),
                    is(IntStream.range(0, 1000)
                            .mapToObj(k -> Map.of("k", String.format("%04d", k)))
                            .collect(Collectors.toList())));
        }
    }

    @Test
    public void testMultiThreads() throws Throwable {
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder()
                .walMaxFileSize(65536 * 1024)
                .build());

        var threads = new ArrayList<TestThread>();
        for (int i = 0; i < 20; ++i) {
            // update
            var index = i;
            var tableName = "t" + i % 4;
            dataStore.update(tableName,
                    new TableSchemaDesc("k",
                            List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                    ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                    List.of());
            threads.add(new TestThread() {
                public void execute() {
                    var columnName = Integer.toString(index);
                    for (int j = 0; j < 10000; ++j) {
                        dataStore.update(tableName,
                                new TableSchemaDesc("k",
                                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                                ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                                ColumnSchemaDesc.builder().name(columnName).type("INT32").build())),
                                List.of(Map.of("k",
                                        String.format("%06d", j),
                                        "a",
                                        Integer.toHexString(index * 10000 + j),
                                        columnName,
                                        Integer.toHexString(index))));
                        Thread.yield();
                    }
                    System.out.printf("%s update %d done\n", this.dateFormat.format(new Date()), index);
                }
            });
        }
        for (int i = 0; i < 200; ++i) {
            // scan
            var index = i;
            var tableName1 = "t" + i % 4;
            var tableName2 = "t" + (i % 4 + 1) % 4;
            threads.add(new TestThread() {
                public void execute() {
                    for (int j = 0; j < 50; ++j) {
                        var records = dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                                .tableName(tableName1)
                                                .build(),
                                        DataStoreScanRequest.TableInfo.builder()
                                                .tableName(tableName2)
                                                .build()))
                                .limit(100)
                                .rawResult(true)
                                .keepNone(true)
                                .build()).getRecords();
                        if (records != null) {
                            for (var record : records) {
                                var k = Integer.parseInt((String) record.get("k"));
                                var a = Integer.parseInt((String) record.get("a"));
                                assertThat(a % 10000, is(k));
                                assertThat(record.toString(), record.get(Integer.toString(a / 10000)), notNullValue());
                            }
                        }
                        try {
                            Thread.sleep(this.random.nextInt(5));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.printf("%s scan %d done\n", dateFormat.format(new Date()), index);
                }
            });
        }
        for (int i = 0; i < 200; ++i) {
            // query
            var index = i;
            var tableName = "t" + i % 4;
            threads.add(new TestThread() {
                public void execute() {
                    for (int j = 0; j < 100; ++j) {
                        dataStore.query(DataStoreQueryRequest.builder()
                                .tableName(tableName)
                                .limit(10)
                                .rawResult(true)
                                .build());
                        try {
                            Thread.sleep(this.random.nextInt(5));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.printf("%s query %d done\n", dateFormat.format(new Date()), index);
                }
            });
        }
        for (var thread : threads) {
            thread.start();
        }
        for (var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread.checkException();
        }
    }

    @Test
    public void testRestartWhenUpdating() throws Throwable {
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder()
                .dumpInterval("1s")
                .minNoUpdatePeriod("1ms")
                .build());

        var threads = new ArrayList<TestThread>();
        var stopRestart = new AtomicBoolean();
        var restartThread = new TestThread() {
            public void execute() throws Exception {
                while (!stopRestart.get()) {
                    Thread.sleep(100);
                    System.out.printf("%s terminating\n", this.dateFormat.format(new Date()));
                    dataStore.terminate();
                    System.out.printf("%s terminated\n", this.dateFormat.format(new Date()));
                    createDateStore(DataStoreParams.builder()
                            .dumpInterval("10ms")
                            .minNoUpdatePeriod("1ms")
                            .build());
                    System.out.printf("%s restarted\n", this.dateFormat.format(new Date()));
                }
            }
        };
        var inserted = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<String>>();
        for (int i = 0; i < 20; ++i) {
            // update
            var index = i;
            var tableName = "t" + i;
            var queue = new ConcurrentLinkedQueue<String>();
            inserted.put(i, queue);
            threads.add(new TestThread() {
                public void execute() throws Exception {
                    for (int j = 0; j < 100; ++j) {
                        for (int k = 0; k < 100; ++k) {
                            try {
                                String key = String.format("%06d", j * 1000 + k);
                                dataStore.update(tableName,
                                        new TableSchemaDesc("k",
                                                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                                        List.of(Map.of("k", key)));
                                queue.add(key);
                            } catch (SwProcessException ignore) {
                                Thread.yield();
                            }
                        }
                        Thread.sleep(100);
                    }
                    System.out.printf("%s update %d done. %d keys inserted\n", this.dateFormat.format(new Date()),
                            index,
                            queue.size());
                }
            });
        }
        restartThread.start();
        for (var thread : threads) {
            thread.start();
        }
        for (var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread.checkException();
        }
        stopRestart.set(true);
        restartThread.join();
        restartThread.checkException();
        for (int i = 0; i < 20; ++i) {
            var result = new ArrayList<String>();
            String key = "";
            for (; ; ) {
                var t = this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t" + i)
                                        .build()))
                                .start(key)
                                .startInclusive(false)
                                .build()).getRecords().stream()
                        .map(m -> (String) m.get("k"))
                        .collect(Collectors.toList());
                result.addAll(t);
                if (t.size() < 1000) {
                    break;
                }
                key = t.get(t.size() - 1);
            }
            assertThat(result, is(inserted.get(i).stream().sorted().collect(Collectors.toList())));
        }
    }

    @Test
    public void testAllTypes() throws Exception {
        List<Map<String, Object>> records = List.of(
                new HashMap<>() {
                    {
                        put("key", "x");
                        put("a", "1");
                        put("b", "10");
                        put("c", "1000");
                        put("d", "00100000");
                        put("e", "0000000010000000");
                        put("f", Integer.toHexString(Float.floatToIntBits(1.1f)));
                        put("g", Long.toHexString(Double.doubleToLongBits(1.1)));
                        put("h", Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8)));
                        put("i", null);
                        put("j", List.of("0000000a"));
                        put("k", Map.of("a", "0000000b", "b", "0000000c"));
                        put("l", List.of("0000000b"));
                        put("m", Map.of("01", "0002"));
                        // list with multiple types
                        put("n", List.of("00000001", "02", "3", "04", "05"));
                        // map with multiple types
                        put("o", List.of(
                                List.of("01", "00000001"),
                                List.of("foo", Long.toHexString(Double.doubleToLongBits(4.2))),
                                List.of("03", "00000003")));
                        put("complex", Map.of("a", List.of(List.of("00000001")),
                                "b", List.of(List.of("00000002")),
                                "c", Map.of("t", List.of("00000004"))));
                    }
                },
                new HashMap<>() {
                    {
                        put("key", "y");
                        put("a", null);
                        put("j", new ArrayList<String>() {
                            {
                                add(null);
                            }
                        });
                        put("k", new HashMap<String, String>() {
                            {
                                put("a", null);
                                put("b", null);
                            }
                        });
                        put("l", new ArrayList<String>() {
                            {
                                add(null);
                            }
                        });
                        put("m", new HashMap<String, String>() {
                            {
                                put("01", null);
                                put("02", null);
                            }
                        });
                        put("n", new ArrayList<String>() {
                            {
                                add(null);
                                add("02");
                                add(null);
                                add("04");
                                add(null);
                            }
                        });
                    }
                },
                new HashMap<>() {
                    {
                        put("key", "z");
                        put("j", List.of());
                        put("k", Map.of());
                        put("l", List.of());
                        put("m", Map.of());
                        put("n", List.of());
                        put("o", Map.of());
                    }
                });

        var int8Type = ColumnSchemaDesc.int8();
        var int16Type = ColumnSchemaDesc.int16();
        var int32Type = ColumnSchemaDesc.int32();
        var float64Type = ColumnSchemaDesc.float64();
        var stringType = ColumnSchemaDesc.string();

        var columnSchemaList = List.of(
                stringType.name("key").build(),
                ColumnSchemaDesc.bool().name("a").build(),
                int8Type.name("b").build(),
                int16Type.name("c").build(),
                int32Type.name("d").build(),
                ColumnSchemaDesc.int64().name("e").build(),
                ColumnSchemaDesc.float32().name("f").build(),
                float64Type.name("g").build(),
                ColumnSchemaDesc.bytes().name("h").build(),
                ColumnSchemaDesc.unknown().name("i").build(),
                ColumnSchemaDesc.listOf(int32Type).name("j").build(),
                ColumnSchemaDesc.objectOf("t", ColumnSchemaDesc.int32().name("a"), ColumnSchemaDesc.int32().name("b"))
                        .name("k")
                        .build(),
                ColumnSchemaDesc.tupleOf(int32Type).name("l").build(),
                ColumnSchemaDesc.mapOf(int8Type, int16Type).name("m").build(),
                ColumnSchemaDesc.listOf(int8Type).name("n")
                        .attributes(List.of(ColumnSchemaDesc.int32().index(0).build(),
                                ColumnSchemaDesc.string().index(2).build()))
                        .build(),
                ColumnSchemaDesc.mapOf(int8Type, int32Type,
                                Map.of(1, new KeyValuePairSchema(stringType.build(), float64Type.build())))
                        .name("o")
                        .build(),
                ColumnSchemaDesc.objectOf("tt",
                                ColumnSchemaDesc.listOf(ColumnSchemaDesc.tupleOf(int32Type)).name("a"),
                                ColumnSchemaDesc.tupleOf(ColumnSchemaDesc.listOf(int32Type)).name("b"),
                                ColumnSchemaDesc.mapOf(stringType, ColumnSchemaDesc.listOf(int32Type)).name("c")
                        ).name("complex")
                        .build());
        var schema = new TableSchemaDesc("key", columnSchemaList);
        var expected = new RecordList(
                columnSchemaList.stream()
                        .collect(Collectors.toMap(ColumnSchemaDesc::getName, col -> new ColumnSchema(col, 0))),
                new HashMap<>() {
                    {
                        put("key", ColumnHintsDesc.builder()
                                .typeHints(List.of("STRING"))
                                .columnValueHints(List.of("x", "y", "z"))
                                .build());
                        put("a", ColumnHintsDesc.builder()
                                .typeHints(List.of("BOOL"))
                                .columnValueHints(List.of("true"))
                                .build());
                        put("b", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT8"))
                                .columnValueHints(List.of("16"))
                                .build());
                        put("c", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT16"))
                                .columnValueHints(List.of("4096"))
                                .build());
                        put("d", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT32"))
                                .columnValueHints(List.of("1048576"))
                                .build());
                        put("e", ColumnHintsDesc.builder()
                                .typeHints(List.of("INT64"))
                                .columnValueHints(List.of("268435456"))
                                .build());
                        put("f", ColumnHintsDesc.builder()
                                .typeHints(List.of("FLOAT32"))
                                .columnValueHints(List.of("1.1"))
                                .build());
                        put("g", ColumnHintsDesc.builder()
                                .typeHints(List.of("FLOAT64"))
                                .columnValueHints(List.of("1.1"))
                                .build());
                        put("h", ColumnHintsDesc.builder()
                                .typeHints(List.of("BYTES"))
                                .columnValueHints(List.of("test"))
                                .build());
                        put("i", ColumnHintsDesc.builder()
                                .typeHints(List.of())
                                .columnValueHints(List.of())
                                .build());
                        put("j", ColumnHintsDesc.builder()
                                .typeHints(List.of("LIST"))
                                .columnValueHints(List.of())
                                .elementHints(ColumnHintsDesc.builder()
                                        .typeHints(List.of("INT32"))
                                        .columnValueHints(List.of("10"))
                                        .build())
                                .build());
                        put("k", ColumnHintsDesc.builder()
                                .typeHints(List.of("OBJECT"))
                                .columnValueHints(List.of())
                                .attributesHints(
                                        Map.of("a", ColumnHintsDesc.builder()
                                                        .typeHints(List.of("INT32"))
                                                        .columnValueHints(List.of("11"))
                                                        .build(),
                                                "b", ColumnHintsDesc.builder()
                                                        .typeHints(List.of("INT32"))
                                                        .columnValueHints(List.of("12"))
                                                        .build()))
                                .build());
                        put("l", ColumnHintsDesc.builder()
                                .typeHints(List.of("TUPLE"))
                                .columnValueHints(List.of())
                                .elementHints(ColumnHintsDesc.builder()
                                        .typeHints(List.of("INT32"))
                                        .columnValueHints(List.of("11"))
                                        .build())
                                .build());
                        put("m", ColumnHintsDesc.builder()
                                .typeHints(List.of("MAP"))
                                .columnValueHints(List.of())
                                .keyHints(ColumnHintsDesc.builder()
                                        .typeHints(List.of("INT8"))
                                        .columnValueHints(List.of("1", "2"))
                                        .build())
                                .valueHints(ColumnHintsDesc.builder()
                                        .typeHints(List.of("INT16"))
                                        .columnValueHints(List.of("2"))
                                        .build())
                                .build());
                        put("n", ColumnHintsDesc.builder()
                                .typeHints(List.of("LIST"))
                                .columnValueHints(List.of())
                                .elementHints(ColumnHintsDesc.builder()
                                        .typeHints(List.of("INT32", "INT8", "STRING"))
                                        .columnValueHints(List.of("1", "2", "4", "5", "3"))
                                        .build())
                                .build());
                        put("o", ColumnHintsDesc.builder()
                                .typeHints(List.of("MAP"))
                                .columnValueHints(List.of())
                                .keyHints(ColumnHintsDesc.builder()
                                        .typeHints(List.of("INT8", "STRING"))
                                        .columnValueHints(List.of("1", "3", "foo"))
                                        .build())
                                .valueHints(ColumnHintsDesc.builder()
                                        .typeHints(List.of("FLOAT64", "INT32"))
                                        .columnValueHints(List.of("1", "3", "4.2"))
                                        .build())
                                .build());
                        put("complex", ColumnHintsDesc.builder()
                                .typeHints(List.of("OBJECT"))
                                .columnValueHints(List.of())
                                .attributesHints(Map.of(
                                        "a", ColumnHintsDesc.builder()
                                                .typeHints(List.of("LIST"))
                                                .columnValueHints(List.of())
                                                .elementHints(ColumnHintsDesc.builder()
                                                        .typeHints(List.of("TUPLE"))
                                                        .columnValueHints(List.of())
                                                        .elementHints(ColumnHintsDesc.builder()
                                                                .typeHints(List.of("INT32"))
                                                                .columnValueHints(List.of("1"))
                                                                .build())
                                                        .build())
                                                .build(),
                                        "b", ColumnHintsDesc.builder()
                                                .typeHints(List.of("TUPLE"))
                                                .columnValueHints(List.of())
                                                .elementHints(ColumnHintsDesc.builder()
                                                        .typeHints(List.of("LIST"))
                                                        .columnValueHints(List.of())
                                                        .elementHints(ColumnHintsDesc.builder()
                                                                .typeHints(List.of("INT32"))
                                                                .columnValueHints(List.of("2"))
                                                                .build())
                                                        .build())
                                                .build(),
                                        "c", ColumnHintsDesc.builder()
                                                .typeHints(List.of("MAP"))
                                                .columnValueHints(List.of())
                                                .keyHints(ColumnHintsDesc.builder()
                                                        .typeHints(List.of("STRING"))
                                                        .columnValueHints(List.of("t"))
                                                        .build())
                                                .valueHints(ColumnHintsDesc.builder()
                                                        .typeHints(List.of("LIST"))
                                                        .columnValueHints(List.of())
                                                        .elementHints(ColumnHintsDesc.builder()
                                                                .typeHints(List.of("INT32"))
                                                                .columnValueHints(List.of("4"))
                                                                .build())
                                                        .build())
                                                .build()))
                                .build());
                    }
                },
                records,
                "z",
                "STRING");
        this.dataStore.update("t", schema, records);
        var result = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t")
                        .keepNone(true)
                        .build()))
                .keepNone(true)
                .build());
        result.getColumnSchemaMap().entrySet()
                .forEach(entry -> entry.setValue(new ColumnSchema(entry.getValue().toColumnSchemaDesc(), 0)));

        var originFlatMap = records.get(0).get("o");
        // hack flat map in record list
        // because the scan method returns map when the object type is map, not flat map
        records.get(0).put("o", Map.of("01", "00000001", "foo", Long.toHexString(Double.doubleToLongBits(4.2)),
                "03", "00000003"));

        assertEquals(expected, result);
        result = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t")
                        .keepNone(true)
                        .build()))
                .keepNone(true)
                .encodeWithType(true)
                .build());
        // restore flat map in record list
        records.get(0).put("o", originFlatMap);
        var encoded = encodeResultWithType(expected);

        // encode of map value will return list of map,
        // the order of the list items is not guaranteed,
        // so we sort the list items to make sure the test is stable
        Consumer<RecordList> sortMapValue = r -> {
            for (var record : r.getRecords()) {
                if (!record.containsKey("o")) {
                    continue;
                }
                // noinspection unchecked
                var col = (Map<String, Object>) record.get("o");
                // noinspection unchecked
                var o = (List<Map<String, Object>>) col.get("value");
                o.sort(Comparator.comparing(m -> m.get("key").toString()));
            }
        };
        List.of(encoded, result).forEach(sortMapValue);

        assertEquals(encoded, result);

        this.dataStore.update("t",
                new TableSchemaDesc("key", List.of(ColumnSchemaDesc.builder().name("key").type("INT32").build())),
                List.of(Map.of("key", "1")));
        result = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder().tableName("t").build())).build());
        assertThat(result.getColumnSchemaMap().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getType())),
                is(new HashMap<>() {
                    {
                        put("key", ColumnType.INT32);
                        put("a", ColumnType.BOOL);
                        put("b", ColumnType.INT8);
                        put("c", ColumnType.INT16);
                        put("d", ColumnType.INT32);
                        put("e", ColumnType.INT64);
                        put("f", ColumnType.FLOAT32);
                        put("g", ColumnType.FLOAT64);
                        put("h", ColumnType.BYTES);
                        put("i", ColumnType.UNKNOWN);
                        put("j", ColumnType.LIST);
                        put("k", ColumnType.OBJECT);
                        put("l", ColumnType.TUPLE);
                        put("m", ColumnType.MAP);
                        put("n", ColumnType.LIST);
                        put("o", ColumnType.MAP);
                        put("complex", ColumnType.OBJECT);
                    }
                }));
        result = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t")
                        .keepNone(true)
                        .build()))
                .keepNone(true)
                .encodeWithType(true)
                .build());
        encoded.getRecords().add(0, Map.of("key", Map.of("type", "INT32", "value", "00000001")));
        encoded.getColumnHints().get("key").setTypeHints(List.of("INT32", "STRING"));
        encoded.getColumnHints().get("key").setColumnValueHints(List.of("1", "x", "y", "z"));
        List.of(encoded, result).forEach(sortMapValue);
        assertEquals(encoded, result);

        // check WAL
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder()
                .dumpInterval("1s")
                .minNoUpdatePeriod("1ms")
                .build());
        result = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t")
                        .keepNone(true)
                        .build()))
                .keepNone(true)
                .encodeWithType(true)
                .build());

        List.of(encoded, result).forEach(sortMapValue);
        assertEquals(encoded, result);
        // check parquet
        while (this.dataStore.hasDirtyTables()) {
            Thread.sleep(100);
        }
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder().build());
        result = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t")
                        .keepNone(true)
                        .build()))
                .keepNone(true)
                .encodeWithType(true)
                .build());
        List.of(encoded, result).forEach(sortMapValue);
        assertEquals(encoded, result);
    }

    private static RecordList encodeResultWithType(RecordList records) {
        return new RecordList(null,
                records.getColumnHints(),
                records.getRecords().stream()
                        .map(r -> r.entrySet().stream()
                                .collect(Collectors.toMap(Entry::getKey,
                                        entry -> encodeValueWithType(records.getColumnSchemaMap().get(entry.getKey()),
                                                entry.getValue()))))
                        .collect(Collectors.toList()),
                records.getLastKey(),
                records.getLastKeyType());
    }

    private static Object encodeValueWithType(ColumnSchema schema, Object value) {
        var ret = new HashMap<String, Object>();
        if (value == null) {
            ret.put("type", "UNKNOWN");
            ret.put("value", null);
            return ret;
        }
        switch (schema.getType()) {
            case LIST:
            case TUPLE: {
                var items = (List<?>) value;
                var encoded = new ArrayList<>();
                for (int i = 0; i < items.size(); ++i) {
                    ColumnSchema elementSchema = null;
                    if (schema.getSparseElementSchema() != null) {
                        elementSchema = schema.getSparseElementSchema().get(i);
                    }
                    if (elementSchema == null) {
                        elementSchema = schema.getElementSchema();
                    }
                    encoded.add(encodeValueWithType(elementSchema, items.get(i)));
                }
                value = encoded;
                break;
            }
            case MAP: {
                if (value instanceof List<?>) {
                    // map transformed to list of map.entry
                    var items = (List<?>) value;
                    var encoded = new ArrayList<Map<String, Object>>();
                    for (int i = 0; i < items.size(); i++) {
                        var item = items.get(i);
                        Pair<ColumnSchema, ColumnSchema> entrySchema = null;
                        if (schema.getSparseKeyValueSchema() != null) {
                            entrySchema = schema.getSparseKeyValueSchema().get(i);
                        }
                        if (entrySchema == null) {
                            // use the default schema
                            entrySchema = Pair.of(schema.getKeySchema(), schema.getValueSchema());
                        }
                        var entry = (List<?>) item;
                        encoded.add(Map.of(
                                "key", encodeValueWithType(entrySchema.getLeft(), entry.get(0)),
                                "value", encodeValueWithType(entrySchema.getRight(), entry.get(1))));
                    }
                    value = encoded;
                } else {
                    // old version client posted record with map
                    value = ((Map<?, ?>) value).entrySet().stream()
                            .map(entry -> Map.of(
                                    "key", encodeValueWithType(schema.getKeySchema(), entry.getKey()),
                                    "value", encodeValueWithType(schema.getValueSchema(), entry.getValue())))
                            .collect(Collectors.toList());
                }
                break;
            }
            case OBJECT:
                ret.put("pythonType", schema.getPythonType());
                //noinspection unchecked
                value = ((Map<String, ?>) value).entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey,
                                entry -> encodeValueWithType(schema.getAttributesSchema().get(entry.getKey()),
                                        entry.getValue())));
                break;
            default:
                break;
        }
        ret.put("type", schema.getType().name());
        ret.put("value", value);
        return ret;
    }
}
