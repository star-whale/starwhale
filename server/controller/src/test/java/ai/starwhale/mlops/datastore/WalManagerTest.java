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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.datastore.Wal.WalEntry.Type;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WalManagerTest {

    private StorageAccessServiceMemory storageAccessService;
    private WalManager walManager;

    @BeforeEach
    public void setUp() throws IOException {
        this.storageAccessService = new StorageAccessServiceMemory();
        this.createInstance();
    }

    private void createInstance() {
        this.walManager = new WalManager(this.storageAccessService, 4096, 4096, "test/", 3);
    }


    @AfterEach
    public void tearDown() {
        this.walManager.terminate();
    }

    private Wal.TableSchema createTableSchema(String keyColumn, List<Triple<Integer, String, String>> columns) {
        var builder = Wal.TableSchema.newBuilder();
        if (keyColumn != null) {
            builder.setKeyColumn(keyColumn);
        }
        for (var triple : columns) {
            builder.addColumns(Wal.ColumnSchema.newBuilder()
                    .setColumnIndex(triple.getLeft())
                    .setColumnName(triple.getMiddle())
                    .setColumnType(triple.getRight()));
        }
        return builder.build();
    }

    private List<Wal.Record> createRecords(List<Map<Integer, Object>> records) {
        var ret = new ArrayList<Wal.Record>();
        for (var record : records) {
            var recordBuilder = Wal.Record.newBuilder();
            for (var entry : record.entrySet()) {
                var columnBuilder = Wal.Column.newBuilder();
                columnBuilder.setIndex(entry.getKey());
                if (entry.getValue() == null) {
                    columnBuilder.setNullValue(true);
                } else {
                    if (entry.getValue() instanceof Boolean) {
                        columnBuilder.setBoolValue((Boolean) entry.getValue());
                    } else if (entry.getValue() instanceof Byte) {
                        columnBuilder.setIntValue((Byte) entry.getValue());
                    } else if (entry.getValue() instanceof Short) {
                        columnBuilder.setIntValue((Short) entry.getValue());
                    } else if (entry.getValue() instanceof Integer) {
                        columnBuilder.setIntValue((Integer) entry.getValue());
                    } else if (entry.getValue() instanceof Long) {
                        columnBuilder.setIntValue((Long) entry.getValue());
                    } else if (entry.getValue() instanceof Float) {
                        columnBuilder.setFloatValue((Float) entry.getValue());
                    } else if (entry.getValue() instanceof Double) {
                        columnBuilder.setDoubleValue((Double) entry.getValue());
                    } else if (entry.getValue() instanceof String) {
                        columnBuilder.setStringValue((String) entry.getValue());
                    } else if (entry.getValue() instanceof ByteBuffer) {
                        columnBuilder.setBytesValue(ByteString.copyFrom((ByteBuffer) entry.getValue()));
                    }
                }
                recordBuilder.addColumns(columnBuilder);
            }
            ret.add(recordBuilder.build());
        }
        return ret;
    }

    @Test
    public void testSimple() throws IOException, InterruptedException {
        var builders = List.of(
                Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t1")
                        .setTableSchema(this.createTableSchema("k",
                                List.of(Triple.of(1, "k", "STRING"),
                                        Triple.of(2, "a", "INT32"))))
                        .addAllRecords(this.createRecords(List.of(
                                Map.of(1, "a", 2, 1),
                                Map.of(1, "b", 2, 2),
                                Map.of(1, "c", 2, 3)
                        ))),
                Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t2")
                        .setTableSchema(this.createTableSchema(null,
                                List.of(Triple.of(3, "x", "INT32"))))
                        .addAllRecords(this.createRecords(List.of(
                                Map.of(1, "a", 3, 1),
                                Map.of(1, "b", 3, 2),
                                Map.of(1, "c", 3, 3)
                        ))),
                Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t2")
                        .setTableSchema(this.createTableSchema(null,
                                List.of(Triple.of(4, "y", "STRING"))))
                        .addAllRecords(this.createRecords(List.of(
                                Map.of(1, "a", 4, "a".repeat(1200)),
                                Map.of(1, "b", 4, "b".repeat(1200)),
                                Map.of(1, "c", 4, "c".repeat(1200))
                        ))),
                Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t2")
                        .setTableSchema(this.createTableSchema(null,
                                List.of(Triple.of(4, "y", "STRING"))))
                        .addAllRecords(this.createRecords(List.of(
                                Map.of(1, "a", 4, "a".repeat(100)),
                                Map.of(1, "b", 4, "b".repeat(100)),
                                Map.of(1, "c", 4, "c".repeat(100))
                        ))));
        assertThat(this.walManager.append(builders.get(0)), is(1L));
        assertThat(this.walManager.append(builders.get(1)), is(2L));
        Thread.sleep(50);
        assertThat(this.walManager.append(builders.get(2)), is(3L));
        Thread.sleep(1000);
        assertThat(this.walManager.append(builders.get(3)), is(4L));
        this.walManager.terminate();
        assertThat(this.storageAccessService.list("").collect(Collectors.toList()),
                is(List.of("test/wal.log.0", "test/wal.log.1")));
        this.createInstance();
        assertThat(ImmutableList.copyOf(this.walManager.readAll()),
                is(builders.stream().map(Wal.WalEntry.Builder::build).collect(Collectors.toList())));
        assertThat(this.walManager.append(builders.get(0)), is(5L));
    }

    @Test
    public void testMany() {
        List<Wal.WalEntry.Builder> builders = new ArrayList<>();
        builders.add(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(this.createTableSchema("k", List.of(Triple.of(1, "k", "STRING")))));
        for (int i = 0; i < 50000; ++i) {
            builders.add(Wal.WalEntry.newBuilder()
                    .setEntryType(Wal.WalEntry.Type.UPDATE)
                    .setTableName("t")
                    .addAllRecords(this.createRecords(List.of(Map.of(1, "" + i)))));
        }
        for (var builder : builders) {
            this.walManager.append(builder);
        }
        this.walManager.terminate();
        this.createInstance();
        assertThat(ImmutableList.copyOf(this.walManager.readAll()),
                is(builders.stream().map(Wal.WalEntry.Builder::build).collect(Collectors.toList())));
    }

    @Test
    public void testHugeEntry() {
        var schema = this.createTableSchema("k", List.of(Triple.of(1, "k", "INT32")));
        var builder = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(schema)
                .addAllRecords(this.createRecords(IntStream.range(1, 5000)
                        .mapToObj(i -> Map.of(1, (Object) i))
                        .collect(Collectors.toList())));

        var id = this.walManager.append(builder);
        this.walManager.terminate();
        this.createInstance();
        var entries = ImmutableList.copyOf(this.walManager.readAll());
        assertThat(entries.size(), is((int) id));
        assertThat(entries.get(0).getTableSchema(), is(schema));
        int index = 1;
        for (var e : entries) {
            for (var r : e.getRecordsList()) {
                for (var c : r.getColumnsList()) {
                    assertThat("index", c.getIndex(), is(1));
                    assertThat("value", c.getIntValue(), is((long) index));
                    ++index;
                }
            }
        }
        assertThat("count", index, is(5000));
    }

    @Test
    public void testAppendHugeSchema() {
        assertThrows(SwValidationException.class, () -> this.walManager.append(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(this.createTableSchema("k",
                        IntStream.range(1, 5000)
                                .mapToObj(i -> Triple.of(1, "" + i, "INT32"))
                                .collect(Collectors.toList())))));
    }

    @Test
    public void testAppendHugeSingleRecord() {
        assertThrows(SwValidationException.class, () -> this.walManager.append(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .addAllRecords(this.createRecords(List.of(IntStream.range(1, 5000)
                        .boxed()
                        .collect(Collectors.toMap(i -> i, i -> i)))))));
    }

    @Test
    public void testAppendSplitSizeCalculation() {
        var entry1 = Wal.WalEntry.newBuilder()
                .setId(1)
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(this.createTableSchema("k", List.of(Triple.of(1, "k", "INT32"))))
                .addAllRecords(this.createRecords(IntStream.range(1, 200)
                        .mapToObj(i -> Map.of(1, (Object) i))
                        .collect(Collectors.toList())))
                .build();
        var entry2 = Wal.WalEntry.newBuilder()
                .setId(2)
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .addAllRecords(this.createRecords(List.of(Map.of(1, 1))))
                .build();
        // make max file size equal the message
        this.walManager = new WalManager(this.storageAccessService,
                256,
                entry1.getSerializedSize() + CodedOutputStream.computeUInt32SizeNoTag(entry1.getSerializedSize()) + 14,
                "test/",
                3);
        this.walManager.append(entry1.toBuilder().addAllRecords(entry2.getRecordsList()));
        this.walManager.terminate();
        this.createInstance();
        assertThat(ImmutableList.copyOf(this.walManager.readAll()), is(List.of(entry1, entry2)));
    }

    @Test
    public void testRemoveLogFiles() throws Exception {
        assertThat(this.walManager.append((Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t")
                        .setTableSchema(this.createTableSchema("k", List.of(Triple.of(1, "k", "STRING")))))),
                is(1L));
        for (long i = 2; i <= 41; ++i) {
            assertThat(this.walManager.append(Wal.WalEntry.newBuilder()
                            .setEntryType(Wal.WalEntry.Type.UPDATE)
                            .setTableName("t")
                            .addAllRecords(this.createRecords(List.of(Map.of(1, "0".repeat(900) + i))))),
                    is(i));
        }
        Thread.sleep(1000);
        assertThat(this.storageAccessService.list("").collect(Collectors.toList()),
                is(IntStream.range(0, 10).mapToObj(k -> "test/wal.log." + k).collect(Collectors.toList())));
        for (int i = -1; i <= 5; ++i) {
            this.walManager.removeWalLogFiles(i);
            assertThat(this.storageAccessService.list("").collect(Collectors.toList()),
                    is(IntStream.range(0, 10).mapToObj(k -> "test/wal.log." + k).collect(Collectors.toList())));
        }
        this.walManager.removeWalLogFiles(6);
        assertThat(this.storageAccessService.list("").collect(Collectors.toList()),
                is(IntStream.range(1, 10).mapToObj(k -> "test/wal.log." + k).collect(Collectors.toList())));
        this.walManager.terminate();
        this.createInstance();
        //noinspection ResultOfMethodCallIgnored
        ImmutableList.copyOf(walManager.readAll());
        this.walManager.removeWalLogFiles(14);
        assertThat(this.storageAccessService.list("").collect(Collectors.toList()),
                is(IntStream.range(3, 10).mapToObj(k -> "test/wal.log." + k).collect(Collectors.toList())));
        this.walManager.removeWalLogFiles(100);
        assertThat(this.storageAccessService.list("").collect(Collectors.toList()), is(List.of("test/wal.log.9")));
    }

    @Test
    public void testWriteFailureAndRetry() throws Exception {
        var storageAccessService = Mockito.mock(StorageAccessService.class);
        given(storageAccessService.list(anyString())).willReturn(Stream.empty());
        doThrow(new IOException())
                .doThrow(new IOException())
                .doNothing()
                .when(storageAccessService).put(anyString(), any(), anyLong());
        var walManager = new WalManager(storageAccessService, 256, 4096, "test/", 3);
        walManager.append(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t"));
        Thread.sleep(1000);
        walManager.terminate();
        verify(storageAccessService, times(3)).put(eq("test/wal.log.0"), any(), anyLong());
    }

    @Test
    public void testReadFailureAndRetry() throws Exception {
        var storageAccessService = Mockito.mock(StorageAccessService.class);
        given(storageAccessService.list(anyString()))
                .willThrow(new IOException())
                .willReturn(Stream.of("test/wal.log.0"));
        given(storageAccessService.get(anyString())).willThrow(new IOException())
                .willThrow(new IOException())
                .willReturn(new LengthAbleInputStream(
                        new ByteArrayInputStream(new byte[]{'s', 'w', 'l', 0, 0, 0, 0, 0, 0, 0}), 10));
        var walManager = new WalManager(storageAccessService, 256, 4096, "test/", 3);
        //noinspection ResultOfMethodCallIgnored
        ImmutableList.copyOf(walManager.readAll());
        walManager.terminate();
        verify(storageAccessService, times(3)).get(eq("test/wal.log.0"));
    }

    @Test
    public void testFlush() throws Throwable {
        // really hard to verify whether previous logs are flushed, only test if flush can work in multiple threads
        var flag = new AtomicBoolean(true);
        var threads = new ArrayList<TestThread>();
        try {
            for (int i = 0; i < 100; ++i) {
                var t = new TestThread() {
                    @Override
                    void execute() {
                        while (flag.get()) {
                            walManager.append(Wal.WalEntry.newBuilder().setEntryType(Type.UPDATE));
                            walManager.flush();
                        }
                    }
                };
                t.start();
                threads.add(t);
            }
            Thread.sleep(5000);
            flag.set(false);
            for (var t : threads) {
                t.join();
            }
            for (var t : threads) {
                t.checkException();
            }
        } finally {
            flag.set(false);
            for (var t : threads) {
                t.interrupt();
            }
        }
    }
}
