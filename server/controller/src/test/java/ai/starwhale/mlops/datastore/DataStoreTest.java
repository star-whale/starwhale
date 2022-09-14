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
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.memory.impl.SwByteBufferManager;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DataStoreTest {

    @TempDir
    private File rootDir;

    private DataStore dataStore;

    private SwByteBufferManager bufferManager;

    private ObjectStore objectStore;

    private WalManager walManager;

    @BeforeEach
    public void setUp() throws IOException {
        this.bufferManager = new SwByteBufferManager();
        this.objectStore = new ObjectStore(bufferManager, new StorageAccessServiceFile(this.rootDir.getAbsolutePath()));
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 256, 4096, "test/", 10, 3);
        this.dataStore = new DataStore(this.walManager);
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
    public void testUpdate() throws IOException {
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
                is(List.of(Map.of("k", "0", "a", "5"), Map.of("k", "1", "a", "2"))));
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
                is(List.of(Map.of("k", "1", "a", "2"))));
        assertThat("t2",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t2")
                                        .columns(Map.of("k", "k", "x", "x"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "3", "x", "2"))));

        this.dataStore.terminate();
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 256, 4096, "test/", 10, 3);
        this.dataStore = new DataStore(this.walManager);
        assertThat("t1",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t1")
                                        .columns(Map.of("k", "k", "a", "a"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "1", "a", "2"))));
        assertThat("t2",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t2")
                                        .columns(Map.of("k", "k", "x", "x"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "3", "x", "2"))));
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
                is(List.of(Map.of("a", "3"),
                        Map.of("a", "4"))));

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
                is(List.of(Map.of("k", "2", "a", "3"),
                        Map.of("k", "1", "a", "4"))));

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
                is(List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        Map.of("k", "2", "a", "3"),
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1"),
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
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                        }},
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1")));
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
                is(List.of(Map.of("a", "4"),
                        new HashMap<>() {{
                            put("a", null);
                        }},
                        Map.of("a", "2"))));
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
                is(List.of(Map.of("a", "4"), Map.of())));
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
                .limit(2)
                .build());
        assertThat("all columns",
                recordList.getColumnTypeMap(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32)));
        assertThat("all columns",
                recordList.getRecords(),
                is(List.of(Map.of("k", "1", "a", "4"), Map.of("k", "2"))));
        assertThat("all columns", recordList.getLastKey(), is("2"));

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .keepNone(true)
                        .build()))
                .limit(0)
                .build());
        assertThat("schema only",
                recordList.getColumnTypeMap(),
                is(Map.of("k", ColumnTypeScalar.STRING, "a", ColumnTypeScalar.INT32)));
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
                        "x:link/url",
                        ColumnTypeScalar.STRING,
                        "x:link/mime_type",
                        ColumnTypeScalar.STRING)));
        assertThat("object type",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        Map.of("k", "2"),
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1"),
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
                List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        Map.of("k", "2", "a", "3"),
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1")));
        this.dataStore.update("t2",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("b").type("INT32").build())),
                List.of(Map.of("k", "0", "b", "15"),
                        Map.of("k", "2", "b", "13"),
                        Map.of("k", "4", "b", "11")));
        this.dataStore.update("t3",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                List.of(Map.of("k", "2")));
        this.dataStore.update("t4",
                new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())),
                List.of(Map.of("k", "2")));
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
                is(List.of(Map.of("k", "0", "a", "5", "b", "15"),
                        Map.of("k", "1", "a", "4"),
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                            put("b", "13");
                        }},
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1", "b", "11"))));
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
                is(List.of(Map.of("k", "0", "a", "5", "b", "15"),
                        Map.of("k", "1", "a", "4"),
                        Map.of("k", "2", "b", "13"),
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1", "b", "11"))));
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
                is(List.of(Map.of("k", "0", "a", "15"),
                        Map.of("k", "1", "a", "4"),
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                        }},
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "11"))));
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
                is(List.of(Map.of("k", "0", "a", "5"))));
    }

    @Test
    public void testMultiThreads() throws Throwable {
        this.dataStore.terminate();
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 65536, 65536 * 1024, "test/", 1000, 3);
        this.dataStore = new DataStore(this.walManager);

        abstract class TestThread extends Thread {

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

            abstract void execute();

            public void checkException() throws Throwable {
                if (this.throwable != null) {
                    throw this.throwable;
                }
            }
        }

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
                        var records = dataStore.query(DataStoreQueryRequest.builder()
                                .tableName(tableName)
                                .limit(10)
                                .rawResult(true)
                                .build()).getRecords();
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
}
