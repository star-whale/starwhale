package ai.starwhale.mlops.datastore;

import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.memory.impl.SwByteBufferManager;
import ai.starwhale.mlops.objectstore.ObjectStore;
import ai.starwhale.mlops.objectstore.impl.FileSystemObjectStore;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WalManagerTest {
    @TempDir
    private File rootDir;

    private SwBufferManager bufferManager;

    private FileSystemObjectStore objectStore;

    private WalManager walManager;

    @BeforeEach
    public void setUp() throws IOException {
        this.bufferManager = new SwByteBufferManager();
        this.objectStore = new FileSystemObjectStore(this.bufferManager, this.rootDir.getAbsolutePath());
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 256, 4096, "test/", 10);
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
        var entries = List.of(
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
                        )))
                        .build(),
                Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t2")
                        .setTableSchema(this.createTableSchema(null,
                                List.of(Triple.of(3, "x", "INT32"))))
                        .addAllRecords(this.createRecords(List.of(
                                Map.of(1, "a", 3, 1),
                                Map.of(1, "b", 3, 2),
                                Map.of(1, "c", 3, 3)
                        )))
                        .build(),
                Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t2")
                        .setTableSchema(this.createTableSchema(null,
                                List.of(Triple.of(4, "y", "STRING"))))
                        .addAllRecords(this.createRecords(List.of(
                                Map.of(1, "a", 4, "a".repeat(100)),
                                Map.of(1, "b", 4, "b".repeat(100)),
                                Map.of(1, "c", 4, "c".repeat(100))
                        )))
                        .build(),
                Wal.WalEntry.newBuilder()
                        .setEntryType(Wal.WalEntry.Type.UPDATE)
                        .setTableName("t2")
                        .setTableSchema(this.createTableSchema(null,
                                List.of(Triple.of(4, "y", "STRING"))))
                        .addAllRecords(this.createRecords(List.of(
                                Map.of(1, "a", 4, "a".repeat(10)),
                                Map.of(1, "b", 4, "b".repeat(10)),
                                Map.of(1, "c", 4, "c".repeat(10))
                        )))
                        .build());
        this.walManager.append(entries.get(0));
        this.walManager.append(entries.get(1));
        Thread.sleep(50);
        this.walManager.append(entries.get(2));
        Thread.sleep(50);
        this.walManager.append(entries.get(3));
        this.walManager.terminate();
        assertThat(ImmutableList.copyOf(this.objectStore.list("")), is(List.of("test/wal.log.0", "test/wal.log.1")));
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 256, 4096, "test/", 10);
        assertThat(ImmutableList.copyOf(this.walManager.readAll()), is(entries));
    }

    @Test
    public void testMany() throws IOException, InterruptedException {
        List<Wal.WalEntry> entries = new ArrayList<>();
        entries.add(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(this.createTableSchema("k", List.of(Triple.of(1, "k", "STRING"))))
                .build());
        for (int i = 0; i < 50000; ++i) {
            entries.add(Wal.WalEntry.newBuilder()
                    .setEntryType(Wal.WalEntry.Type.UPDATE)
                    .setTableName("t")
                    .addAllRecords(this.createRecords(List.of(Map.of(1, "" + i))))
                    .build());
        }
        for (var entry : entries) {
            this.walManager.append(entry);
        }
        this.walManager.terminate();
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 256, 4096, "test/", 10);
        assertThat(ImmutableList.copyOf(this.walManager.readAll()), is(entries));
    }

    @Test
    public void testHugeEntry() throws IOException {
        var entry = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(this.createTableSchema("k", List.of(Triple.of(1, "k", "INT32"))))
                .addAllRecords(this.createRecords(IntStream.range(1, 5000)
                        .mapToObj(i -> Map.of(1, (Object) i))
                        .collect(Collectors.toList())))
                .build();
        this.walManager.append(entry);
        this.walManager.terminate();
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 256, 4096, "test/", 10);
        var entries = ImmutableList.copyOf(this.walManager.readAll());
        assertThat(entries.size(), greaterThan(1));
        assertThat(entries.get(0).getTableSchema(), is(entry.getTableSchema()));
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
        assertThrows(SWValidationException.class, () -> this.walManager.append(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(this.createTableSchema("k",
                        IntStream.range(1, 5000)
                                .mapToObj(i -> Triple.of(1, "" + i, "INT32"))
                                .collect(Collectors.toList())))
                .build()));
    }

    @Test
    public void testAppendHugeSingleRecord() {
        assertThrows(SWValidationException.class, () -> this.walManager.append(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .addAllRecords(this.createRecords(List.of(IntStream.range(1, 5000)
                        .boxed()
                        .collect(Collectors.toMap(i -> i, i -> i)))))
                .build()));
    }

    @Test
    public void testAppendSplitSizeCalculation() throws IOException {
        var builder = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .setTableSchema(this.createTableSchema("k", List.of(Triple.of(1, "k", "INT32"))))
                .addAllRecords(this.createRecords(IntStream.range(1, 200)
                        .mapToObj(i -> Map.of(1, (Object) i))
                        .collect(Collectors.toList())));
        var entry1 = builder.build();
        var entry2 = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .addAllRecords(this.createRecords(List.of(Map.of(1, 1))))
                .build();
        // make max file size equal the message
        this.walManager = new WalManager(this.objectStore,
                this.bufferManager,
                256,
                entry1.getSerializedSize() + CodedOutputStream.computeUInt32SizeNoTag(entry1.getSerializedSize()),
                "test/",
                10);
        builder.addAllRecords(entry2.getRecordsList());
        this.walManager.append(builder.build());
        this.walManager.terminate();
        this.walManager = new WalManager(this.objectStore, this.bufferManager, 256, 4096, "test/", 10);
        assertThat(ImmutableList.copyOf(this.walManager.readAll()), is(List.of(entry1, entry2)));
    }

    @Test
    public void testWriteFailureAndRetry() throws Exception {
        var objectStore = Mockito.mock(ObjectStore.class);
        given(objectStore.list(anyString())).willReturn(Collections.emptyIterator());
        doThrow(new IOException())
                .doThrow(new IOException())
                .doNothing()
                .when(objectStore).put(anyString(), any());
        var walManager = new WalManager(objectStore, this.bufferManager, 256, 4096, "test/", 10);
        walManager.append(Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName("t")
                .build());
        Thread.sleep(1000);
        walManager.terminate();
        verify(objectStore, times(3)).put(eq("test/wal.log.0"), any());
    }
}
