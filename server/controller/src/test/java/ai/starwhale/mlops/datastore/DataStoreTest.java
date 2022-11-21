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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.TableQueryFilter.Constant;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Builder.Default;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class DataStoreTest {

    private DataStore dataStore;

    private StorageAccessService storageAccessService;

    @Builder
    private static class DataStoreParams {

        @Default
        int walFileSize = 256;
        @Default
        int walMaxFileSize = 4096;
        @Default
        int walWaitIntervalMillis = 10;
        @Default
        int ossMaxAttempts = 3;
        @Default
        String dataRootPath = "";
        @Default
        String dumpInterval = "1h";
        @Default
        String minNoUpdatePeriod = "1d";
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
        ((Logger) LoggerFactory.getLogger("org.apache.parquet")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org.apache.hadoop")).setLevel(Level.ERROR);
        this.storageAccessService = new StorageAccessServiceMemory();
        this.createDateStore(DataStoreParams.builder().build());
    }

    private void createDateStore(DataStoreParams params) {
        this.dataStore = new DataStore(this.storageAccessService,
                params.walFileSize,
                params.walMaxFileSize,
                params.walWaitIntervalMillis,
                params.ossMaxAttempts,
                params.dataRootPath,
                params.dumpInterval,
                params.minNoUpdatePeriod,
                params.compressionCodec,
                params.rowGroupSize,
                params.pageSize,
                params.pageRowCountLimit);
    }

    @AfterEach
    public void tearDown() {
        this.dataStore.terminate();
    }

    @Test
    public void testList() {
        assertThat("empty", this.dataStore.list(""), empty());
        this.dataStore.update("t1",
                new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())), null);
        this.dataStore.update("t2",
                new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())), null);
        this.dataStore.update("test",
                new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())), null);
        assertThat("all", this.dataStore.list("t"), containsInAnyOrder("t1", "t2", "test"));
        assertThat("partial", this.dataStore.list("te"), containsInAnyOrder("test"));
        assertThat("none", this.dataStore.list("t3"), empty());
    }

    @Test
    public void testUpdate() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                List.of(Map.of("k", "0", "a", "1")));
        this.dataStore.update("t1",
                null,
                List.of(Map.of("k", "1", "a", "2")));
        this.dataStore.update("t2",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("x").type("INT32").build())),
                List.of(Map.of("k", "3", "x", "2")));
        this.dataStore.update("t1",
                null,
                List.of(Map.of("k", "0", "a", "5"), Map.of("k", "4", "-", "1")));
        assertThat("t1",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t1")
                                        .columns(Map.of("k", "k", "a", "a"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000005"), Map.of("k", "1", "a", "00000002"))));
        this.dataStore.update("t1",
                null,
                List.of(Map.of("k", "0", "-", "anyString"), Map.of("k", "4", "-", "1")));
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
    }

    @Test
    public void testQuery() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
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
                        .operands(List.of(new TableQueryFilter.Column("a"), new Constant(ColumnTypeScalar.INT32, 1)))
                        .build())
                .orderBy(List.of(new OrderByDesc("a")))
                .start(1)
                .limit(2)
                .build());
        assertThat("test", recordList.getColumnTypeMap(), is(Map.of("a", ColumnTypeScalar.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("a", "00000003"),
                        Map.of("a", "00000004"))));

        recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName("t1")
                .filter(TableQueryFilter.builder()
                        .operator(TableQueryFilter.Operator.GREATER)
                        .operands(List.of(new TableQueryFilter.Column("a"), new Constant(ColumnTypeScalar.INT32, 1)))
                        .build())
                .orderBy(List.of(new OrderByDesc("a")))
                .start(1)
                .limit(2)
                .build());
        assertThat("all columns",
                recordList.getColumnTypeMap(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32)));
        assertThat("all columns",
                recordList.getRecords(),
                is(List.of(Map.of("k", "2", "a", "00000003"),
                        Map.of("k", "1", "a", "00000004"))));

        this.dataStore.update("t1",
                new TableSchemaDesc(null,
                        List.of(ColumnSchemaDesc.builder().name("x:link/url").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("x:link/mime_type").type("STRING").build())),
                List.of(Map.of("k", "5", "x:link/url", "http://test.com/1.jpg", "x:link/mime_type", "image/jpeg"),
                        Map.of("k", "6", "x:link/url", "http://test.com/2.png", "x:link/mime_type", "image/png")));
        recordList = this.dataStore.query(DataStoreQueryRequest.builder().tableName("t1").build());
        assertThat("object type",
                recordList.getColumnTypeMap(),
                is(Map.of("k",
                        ColumnTypeScalar.STRING,
                        "a",
                        ColumnTypeScalar.INT32,
                        "x:link/url",
                        ColumnTypeScalar.STRING,
                        "x:link/mime_type",
                        ColumnTypeScalar.STRING)));
        assertThat("object type",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "00000005"),
                        Map.of("k", "1", "a", "00000004"),
                        Map.of("k", "2", "a", "00000003"),
                        Map.of("k", "3", "a", "00000002"),
                        Map.of("k", "4", "a", "00000001"),
                        Map.of("k", "5", "x:link/url", "http://test.com/1.jpg", "x:link/mime_type", "image/jpeg"),
                        Map.of("k", "6", "x:link/url", "http://test.com/2.png", "x:link/mime_type", "image/png"))));

        recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName("t1")
                .columns(Map.of("x", "y", "x:link/url", "url"))
                .build());
        assertThat("object type alias",
                recordList.getColumnTypeMap(),
                is(Map.of("url", ColumnTypeScalar.STRING, "y:link/mime_type", ColumnTypeScalar.STRING)));
        assertThat("object type alias",
                recordList.getRecords(),
                is(List.of(Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of("url", "http://test.com/1.jpg", "y:link/mime_type", "image/jpeg"),
                        Map.of("url", "http://test.com/2.png", "y:link/mime_type", "image/png"))));
        // query non exist table
        final String tableNonExist = "tableNonExist";
        assertThrows(SwValidationException.class, () -> this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName(tableNonExist).build()));
        recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName(tableNonExist).ignoreNonExistingTable(true).build());
        assertThat("result of non exist table", recordList.getColumnTypeMap().isEmpty());
        assertThat("result of non exist table", recordList.getRecords().isEmpty());
    }

    @Test
    public void testScanOneTable() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("l").type("FLOAT64").build())),
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
        assertThat("test", recordList.getColumnTypeMap(), is(Map.of("a", ColumnTypeScalar.INT32)));
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
        assertThat("test", recordList.getColumnTypeMap(), is(Map.of("a", ColumnTypeScalar.INT32)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32, "l", ColumnTypeScalar.FLOAT64)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32, "l", ColumnTypeScalar.FLOAT64)));
        assertThat("schema only", recordList.getRecords(), empty());

        this.dataStore.update("t1",
                new TableSchemaDesc(null,
                        List.of(ColumnSchemaDesc.builder().name("x:link/url").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("x:link/mime_type").type("STRING").build())),
                List.of(Map.of("k", "5", "x:link/url", "http://test.com/1.jpg", "x:link/mime_type", "image/jpeg"),
                        Map.of("k", "6", "x:link/url", "http://test.com/2.png", "x:link/mime_type", "image/png")));
        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .build()))
                .build());
        assertThat("object type",
                recordList.getColumnTypeMap(),
                is(Map.of("k",
                        ColumnTypeScalar.STRING,
                        "a",
                        ColumnTypeScalar.INT32,
                        "l",
                        ColumnTypeScalar.FLOAT64,
                        "x:link/url",
                        ColumnTypeScalar.STRING,
                        "x:link/mime_type",
                        ColumnTypeScalar.STRING)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("url", ColumnTypeScalar.STRING, "y:link/mime_type", ColumnTypeScalar.STRING)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("k",
                        ColumnTypeScalar.STRING,
                        "a",
                        ColumnTypeScalar.INT32,
                        "b",
                        ColumnTypeScalar.INT32)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("k",
                        ColumnTypeScalar.STRING,
                        "a",
                        ColumnTypeScalar.INT32,
                        "b",
                        ColumnTypeScalar.INT32)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("a", ColumnTypeScalar.INT32, "b", ColumnTypeScalar.INT32, "k", ColumnTypeScalar.STRING)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32)));
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
                recordList.getColumnTypeMap(),
                is(Map.of("ak",
                        ColumnTypeScalar.STRING,
                        "bk",
                        ColumnTypeScalar.STRING,
                        "aa",
                        ColumnTypeScalar.INT32,
                        "bb",
                        ColumnTypeScalar.INT32)));
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
                .walFileSize(65536)
                .walMaxFileSize(65536)
                .dumpInterval("1s")
                .minNoUpdatePeriod("1ms")
                .build());
        try {
            for (int i = 0; i < 10; ++i) {
                this.dataStore.update("t" + i,
                        new TableSchemaDesc("k",
                                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build())),
                        IntStream.range(0, 1024)
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
                .walFileSize(65536)
                .walMaxFileSize(65536)
                .build());
        assertThat(this.dataStore.hasDirtyTables(), is(true));
        assertThat(this.dataStore.list("t"),
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

    private abstract static class TestThread extends Thread {

        protected final Random random = new Random();
        protected final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
        private Throwable throwable;

        public void run() {
            try {
                this.execute();
            } catch (Throwable t) {
                t.printStackTrace();
                this.throwable = t;
            }
        }

        abstract void execute() throws Exception;

        public void checkException() throws Throwable {
            if (this.throwable != null) {
                throw this.throwable;
            }
        }
    }

    @Test
    public void testMultiThreads() throws Throwable {
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder()
                .walFileSize(65536)
                .walMaxFileSize(65536 * 1024)
                .walWaitIntervalMillis(1000)
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
                    null);
            threads.add(new TestThread() {
                public void execute() {
                    var columnName = Integer.toString(index);
                    for (int j = 0; j < 100000; ++j) {
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
                            .walWaitIntervalMillis(1000)
                            .dumpInterval("1s")
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
                        for (int k = 0; k < 1000; ++k) {
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
                    }
                });
        var columnTypeMap = new HashMap<String, ColumnType>() {
            {
                put("key", ColumnTypeScalar.STRING);
                put("a", ColumnTypeScalar.BOOL);
                put("b", ColumnTypeScalar.INT8);
                put("c", ColumnTypeScalar.INT16);
                put("d", ColumnTypeScalar.INT32);
                put("e", ColumnTypeScalar.INT64);
                put("f", ColumnTypeScalar.FLOAT32);
                put("g", ColumnTypeScalar.FLOAT64);
                put("h", ColumnTypeScalar.BYTES);
                put("i", ColumnTypeScalar.UNKNOWN);
                put("j", new ColumnTypeList(ColumnTypeScalar.INT32));
                put("k", new ColumnTypeObject("t", Map.of("a", ColumnTypeScalar.INT32, "b", ColumnTypeScalar.INT32)));
                put("l", new ColumnTypeTuple(ColumnTypeScalar.INT32));
                put("m", new ColumnTypeMap(ColumnTypeScalar.INT8, ColumnTypeScalar.INT16));
                put("complex", new ColumnTypeObject("tt", Map.of(
                        "a", new ColumnTypeList(new ColumnTypeTuple(ColumnTypeScalar.INT32)),
                        "b", new ColumnTypeTuple(new ColumnTypeList(ColumnTypeScalar.INT32)),
                        "c", new ColumnTypeMap(ColumnTypeScalar.STRING, new ColumnTypeList(ColumnTypeScalar.INT32))
                )));
            }
        };
        var expected = new RecordList(columnTypeMap, records, "y");
        this.dataStore.update("t",
                new TableSchemaDesc("key",
                        columnTypeMap.entrySet().stream()
                                .map(entry -> entry.getValue().toColumnSchemaDesc(entry.getKey()))
                                .collect(Collectors.toList())),
                records);
        assertThat(this.dataStore.scan(DataStoreScanRequest.builder()
                        .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t")
                                .keepNone(true)
                                .build()))
                        .keepNone(true)
                        .build()),
                is(expected));

        // check WAL
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder()
                .dumpInterval("1s")
                .minNoUpdatePeriod("1ms")
                .build());
        assertThat(this.dataStore.scan(DataStoreScanRequest.builder()
                        .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t")
                                .keepNone(true)
                                .build()))
                        .keepNone(true)
                        .build()),
                is(expected));
        // check parquet
        while (this.dataStore.hasDirtyTables()) {
            Thread.sleep(100);
        }
        this.dataStore.terminate();
        this.createDateStore(DataStoreParams.builder().build());
        assertThat(this.dataStore.scan(DataStoreScanRequest.builder()
                        .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t")
                                .keepNone(true)
                                .build()))
                        .keepNone(true)
                        .build()),
                is(expected));
    }
}
