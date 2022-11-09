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

import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.memory.SwBuffer;
import ai.starwhale.mlops.memory.SwBufferInputStream;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.memory.SwBufferOutputStream;
import com.google.protobuf.CodedOutputStream;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xerial.snappy.Snappy;

@Slf4j
@Component
public class WalManager extends Thread {

    private final ObjectStore objectStore;

    private final SwBufferManager bufferManager;

    private final int walFileSize;

    private final int walMaxFileSize;

    private final int walMaxFileSizeNoHeader;

    private final String logFilePrefix;

    private final int walWaitIntervalMillis;

    private final LinkedList<Wal.WalEntry> entriesToWrite = new LinkedList<>();

    private SwBufferOutputStream outputStream;

    private final SwBuffer outputBuffer;

    private final SwBuffer compressedBuffer;

    private static final byte NO_COMPRESSION = 0;

    private static final byte SNAPPY = 1;

    private final byte[] header = new byte[]{'s', 'w', 'l', 0};

    private boolean terminated;

    private int logFileIndex;

    private long maxEntryId;

    private long maxEntryIdInOutputBuffer;

    private final int ossMaxAttempts;

    private final Map<String, Long> walLogFileMap = new ConcurrentHashMap<>();

    public WalManager(ObjectStore objectStore,
            SwBufferManager bufferManager,
            @Value("${sw.datastore.walFileSize}") int walFileSize,
            @Value("${sw.datastore.walMaxFileSize}") int walMaxFileSize,
            @Value("${sw.datastore.walPrefix}") String walPrefix,
            @Value("${sw.datastore.walWaitIntervalMillis}") int walWaitIntervalMillis,
            @Value("${sw.datastore.ossMaxAttempts}") int ossMaxAttempts) {
        this.objectStore = objectStore;
        this.bufferManager = bufferManager;
        this.walFileSize = walFileSize;
        this.walMaxFileSize = walMaxFileSize;
        this.walMaxFileSizeNoHeader = this.walMaxFileSize - this.header.length;
        this.logFilePrefix = walPrefix + "wal.log.";
        this.walWaitIntervalMillis = walWaitIntervalMillis;
        this.ossMaxAttempts = ossMaxAttempts;
        this.outputBuffer = this.bufferManager.allocate(this.walMaxFileSizeNoHeader);
        this.compressedBuffer = this.bufferManager.allocate(this.walMaxFileSize);
        this.outputStream = new SwBufferOutputStream(this.outputBuffer);
        var walMap = new TreeMap<Integer, String>();
        Iterator<String> it;
        try {
            it = Retry.decorateCheckedSupplier(
                            Retry.of("put", RetryConfig.custom()
                                    .maxAttempts(ossMaxAttempts)
                                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5, 10000))
                                    .retryOnException(e -> !terminated)
                                    .build()),
                            () -> this.objectStore.list(this.logFilePrefix))
                    .apply();
        } catch (Throwable e) {
            throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "fail to read WAL", e);
        }
        while (it.hasNext()) {
            var fn = it.next();
            try {
                var index = Integer.parseInt(fn.substring(this.logFilePrefix.length()));
                walMap.put(index, fn);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        if (!walMap.isEmpty()) {
            this.logFileIndex = walMap.lastKey() + 1;
            for (var key : walMap.values()) {
                this.walLogFileMap.put(key, -1L);
            }
        }
        this.start();
    }

    public Iterator<Wal.WalEntry> readAll() {
        return new Iterator<>() {
            private final List<String> files = WalManager.this.walLogFileMap.keySet().stream()
                    .sorted(Comparator.comparingInt(fn -> Integer.parseInt(fn.substring(logFilePrefix.length()))))
                    .collect(Collectors.toList());
            private final SwBuffer buf = WalManager.this.bufferManager.allocate(WalManager.this.walMaxFileSize);
            private SwBufferInputStream inputStream;
            private String currentFile;

            @Override
            public boolean hasNext() {
                return !this.files.isEmpty() || this.inputStream != null;
            }

            @Override
            public Wal.WalEntry next() {
                if (this.inputStream == null) {
                    if (this.files.isEmpty()) {
                        return null;
                    } else {
                        this.getNext();
                    }
                }
                try {
                    var ret = Wal.WalEntry.parseDelimitedFrom(this.inputStream);
                    if (this.inputStream.remaining() == 0) {
                        this.inputStream = null;
                    }
                    maxEntryId = ret.getId();
                    walLogFileMap.put(this.currentFile, ret.getId());
                    return ret;
                } catch (IOException e) {
                    this.inputStream = null;
                    throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "failed to parse proto", e);
                }
            }

            private void getNext() {
                this.currentFile = this.files.get(0);
                this.files.remove(0);
                SwBuffer data;
                try {
                    data = Retry.decorateCheckedSupplier(
                                    Retry.of("get", RetryConfig.custom()
                                            .maxAttempts(WalManager.this.ossMaxAttempts)
                                            .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                                                    100, 2.0, 0.5, 10000))
                                            .build()),
                                    () -> objectStore.get(this.currentFile))
                            .apply();
                } catch (Throwable e) {
                    throw new SwProcessException(SwProcessException.ErrorType.DATASTORE,
                            "fail to read from object store", e);
                }
                if (data.capacity() < WalManager.this.header.length) {
                    throw new SwProcessException(SwProcessException.ErrorType.DATASTORE,
                            MessageFormat.format("corrupted file, size={0}", data.capacity()));
                }
                int uncompressedSize;
                var h = new byte[4];
                data.getBytes(0, h, 0, h.length);
                var compressed = data.slice(h.length, data.capacity() - h.length);
                if (h[3] == NO_COMPRESSION) {
                    compressed.copyTo(this.buf);
                    uncompressedSize = compressed.capacity();
                } else {
                    try {
                        var inBuf = compressed.asByteBuffer();
                        var outBuf = this.buf.asByteBuffer();
                        if (inBuf.hasArray()) {
                            uncompressedSize = Snappy.uncompress(inBuf.array(),
                                    inBuf.arrayOffset(),
                                    inBuf.capacity(),
                                    outBuf.array(),
                                    outBuf.arrayOffset());
                        } else {
                            uncompressedSize = Snappy.uncompress(inBuf, outBuf);
                        }
                    } catch (IOException e) {
                        throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "fail to uncompress", e);
                    }
                }
                WalManager.this.bufferManager.release(data);
                this.inputStream = new SwBufferInputStream(this.buf.slice(0, uncompressedSize));
            }
        };
    }

    public long append(Wal.WalEntry.Builder builder) {
        synchronized (this.entriesToWrite) {
            if (this.terminated) {
                throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "terminated");
            }
            var entry = builder.setId(this.maxEntryId + 1).build();
            if (entry.getSerializedSize() <= this.walMaxFileSizeNoHeader) {
                this.entriesToWrite.add(entry);
                return ++this.maxEntryId;
            } else {
                return this.splitEntryAndAppend(entry);
            }
        }
    }

    public void terminate() {
        synchronized (this.entriesToWrite) {
            if (this.terminated) {
                return;
            }
            this.terminated = true;
            this.entriesToWrite.notifyAll();
        }
        try {
            this.join();
        } catch (InterruptedException e) {
            log.warn("interrupted", e);
        }
    }

    public void removeWalLogFiles(long minWalLogIdToRetain) throws IOException {
        for (var logFile : this.walLogFileMap.entrySet().stream()
                .filter(entry -> entry.getValue() < minWalLogIdToRetain)
                .map(Entry::getKey)
                .collect(Collectors.toList())) {
            this.objectStore.delete(logFile);
            this.walLogFileMap.remove(logFile);
        }
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                var status = this.populateOutput();
                if (status == PopulationStatus.TERMINATED) {
                    return;
                }
                this.writeToObjectStore(status == PopulationStatus.BUFFER_FULL);
            } catch (Throwable e) {
                log.error("unexpected exception", e);
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    log.warn("interrupted", e);
                }
            }
        }
    }

    public static TableSchemaDesc parseTableSchema(Wal.TableSchema tableSchema) {
        var ret = new TableSchemaDesc();
        var keyColumn = tableSchema.getKeyColumn();
        if (!keyColumn.isEmpty()) {
            ret.setKeyColumn(keyColumn);
        }
        var columnList = new ArrayList<>(tableSchema.getColumnsList());
        columnList.sort(Comparator.comparingInt(Wal.ColumnSchema::getColumnIndex));
        var columnSchemaList = new ArrayList<ColumnSchemaDesc>();
        for (var col : columnList) {
            var colDesc = WalManager.parseColumnSchema(col);
            columnSchemaList.add(colDesc);
        }
        ret.setColumnSchemaList(columnSchemaList);
        return ret;
    }

    public static ColumnSchemaDesc parseColumnSchema(Wal.ColumnSchema columnSchema) {
        var ret = ColumnSchemaDesc.builder()
                .name(columnSchema.getColumnName())
                .type(columnSchema.getColumnType());
        if (!columnSchema.getPythonType().isEmpty()) {
            ret.pythonType(columnSchema.getPythonType());
        }
        if (columnSchema.hasElementType()) {
            ret.elementType(WalManager.parseColumnSchema(columnSchema.getElementType()));
        }
        if (columnSchema.getAttributesCount() > 0) {
            ret.attributes(columnSchema.getAttributesList().stream()
                    .map(WalManager::parseColumnSchema)
                    .collect(Collectors.toList()));
        }
        return ret.build();
    }

    public static Wal.TableSchema.Builder convertTableSchema(TableSchema schema) {
        var builder = Wal.TableSchema.newBuilder();
        builder.setKeyColumn(schema.getKeyColumn());
        for (var col : schema.getColumnSchemas()) {
            builder.addColumns(WalManager.convertColumnSchema(col));
        }
        return builder;
    }

    public static Wal.ColumnSchema.Builder convertColumnSchema(ColumnSchema schema) {
        return WalManager.newColumnSchema(schema.getIndex(), schema.getName(), schema.getType());
    }

    private static Wal.ColumnSchema.Builder newColumnSchema(
            int columnIndex, String columnName, ColumnType columnType) {
        var ret = Wal.ColumnSchema.newBuilder()
                .setColumnIndex(columnIndex)
                .setColumnName(columnName)
                .setColumnType(columnType.getTypeName());
        if (columnType instanceof ColumnTypeList) {
            ret.setElementType(
                    WalManager.newColumnSchema(0, "", ((ColumnTypeList) columnType).getElementType()));
        } else if (columnType instanceof ColumnTypeObject) {
            ret.setPythonType(((ColumnTypeObject) columnType).getPythonType());
            ret.addAllAttributes(((ColumnTypeObject) columnType).getAttributes().entrySet().stream()
                    .map(entry -> WalManager.newColumnSchema(0, entry.getKey(), entry.getValue()).build())
                    .collect(Collectors.toList()));
        }
        return ret;
    }

    private enum PopulationStatus {
        TERMINATED,
        BUFFER_FULL,
        NO_MORE_ENTRIES
    }

    private PopulationStatus populateOutput() {
        synchronized (this.entriesToWrite) {
            while (!this.terminated && this.entriesToWrite.isEmpty()) {
                try {
                    this.entriesToWrite.wait(this.walWaitIntervalMillis);
                } catch (InterruptedException e) {
                    log.warn("interrupted", e);
                }
            }
            if (this.terminated && this.entriesToWrite.isEmpty()) {
                return PopulationStatus.TERMINATED;
            }
        }
        for (; ; ) {
            Wal.WalEntry entry;
            synchronized (this.entriesToWrite) {
                if (this.entriesToWrite.isEmpty()) {
                    break;
                }
                entry = this.entriesToWrite.getFirst();
            }
            if (CodedOutputStream.computeMessageSizeNoTag(entry) + this.outputStream.getOffset()
                    > this.walMaxFileSizeNoHeader) {
                if (this.outputStream.getOffset() == 0) {
                    // huge single entry
                    log.error(
                            "data loss: discard unexpected huge entry. size={} table={} schema={} records count={}",
                            entry.getSerializedSize(),
                            entry.getTableName(),
                            entry.getTableSchema(),
                            entry.getRecordsCount());
                } else {
                    return PopulationStatus.BUFFER_FULL;
                }
            } else {
                try {
                    entry.writeDelimitedTo(this.outputStream);
                } catch (IOException e) {
                    log.error("data loss: unexpected exception", e);
                }
            }
            synchronized (this.entriesToWrite) {
                this.entriesToWrite.removeFirst();
            }
            this.maxEntryIdInOutputBuffer = entry.getId();
        }
        if (this.outputStream.getOffset() >= this.walFileSize) {
            return PopulationStatus.BUFFER_FULL;
        }
        return PopulationStatus.NO_MORE_ENTRIES;
    }

    private void writeToObjectStore(boolean clearOutput) {
        int compressedSize;
        try {
            var inBuf = this.outputBuffer.asByteBuffer();
            var outBuf = this.compressedBuffer.asByteBuffer();
            this.header[3] = WalManager.SNAPPY;
            outBuf.put(header);
            if (inBuf.hasArray()) {
                compressedSize = Snappy.compress(inBuf.array(),
                        inBuf.arrayOffset(),
                        this.outputStream.getOffset(),
                        outBuf.array(),
                        outBuf.arrayOffset() + outBuf.position());
            } else {
                inBuf.limit(this.outputStream.getOffset());
                compressedSize = Snappy.compress(inBuf, outBuf);
            }
        } catch (IOException e) {
            log.warn("failed to compress", e);
            this.header[3] = WalManager.NO_COMPRESSION;
            this.compressedBuffer.setBytes(0, this.header, 0, this.header.length);
            this.outputBuffer.copyTo(this.compressedBuffer.slice(4, this.outputStream.getOffset()));
            compressedSize = this.outputStream.getOffset();
        }
        var key = this.logFilePrefix + this.logFileIndex;
        try {
            int compressedBufferSize = compressedSize + this.header.length;
            Retry.decorateCheckedRunnable(
                            Retry.of("put", RetryConfig.custom()
                                    .maxAttempts(this.ossMaxAttempts)
                                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5, 10000))
                                    .build()),
                            () -> this.objectStore.put(key, this.compressedBuffer.slice(0, compressedBufferSize)))
                    .run();
        } catch (Throwable e) {
            log.error("data loss: failed to write wal log", e);
        }
        if (clearOutput) {
            ++this.logFileIndex;
            this.outputStream = new SwBufferOutputStream(this.outputBuffer);
            this.walLogFileMap.put(key, this.maxEntryIdInOutputBuffer);
        }
    }

    private long splitEntryAndAppend(Wal.WalEntry entry) {
        List<Wal.WalEntry> entries = new ArrayList<>();
        var builder = Wal.WalEntry.newBuilder()
                .setId(this.maxEntryId + 1)
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName(entry.getTableName());
        if (entry.hasTableSchema()) {
            builder.setTableSchema(entry.getTableSchema());
        }
        int currentEntrySize = builder.build().getSerializedSize();
        if (currentEntrySize > this.walMaxFileSizeNoHeader) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "schema is too large or walMaxFileSize is too small. size=" + currentEntrySize
                            + " walMaxFileSizeNoHeader=" + this.walMaxFileSizeNoHeader);
        }
        for (var record : entry.getRecordsList()) {
            // field number is less than 128, so simply use 1 instead
            var recordSize = CodedOutputStream.computeMessageSize(1, record);
            currentEntrySize += recordSize;
            if (currentEntrySize + CodedOutputStream.computeUInt32SizeNoTag(currentEntrySize)
                    > this.walMaxFileSizeNoHeader) {
                entries.add(builder.build());
                builder.clearTableSchema();
                builder.clearRecords();
                builder.setId(this.maxEntryId + entries.size() + 1);
                currentEntrySize = builder.build().getSerializedSize() + recordSize;
                if (currentEntrySize + CodedOutputStream.computeUInt32SizeNoTag(currentEntrySize)
                        > this.walMaxFileSizeNoHeader) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "huge single record. size=" + currentEntrySize);
                }
            }
            builder.addRecords(record);
        }
        if (builder.getRecordsCount() > 0) {
            entries.add(builder.build());
        }
        for (var e : entries) {
            if (e.getSerializedSize() > this.walMaxFileSizeNoHeader) {
                throw new SwProcessException(SwProcessException.ErrorType.DATASTORE,
                        "invalid entry size " + e.getSerializedSize());
            }
        }
        this.entriesToWrite.addAll(entries);
        this.maxEntryId += entries.size();
        return this.maxEntryId;
    }
}
