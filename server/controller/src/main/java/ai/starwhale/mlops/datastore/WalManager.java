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

import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.memory.SwBuffer;
import ai.starwhale.mlops.memory.SwBufferInputStream;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.memory.SwBufferOutputStream;
import ai.starwhale.mlops.objectstore.ObjectStore;
import com.google.protobuf.CodedOutputStream;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

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

    private final LinkedList<Wal.WalEntry> entries = new LinkedList<>();

    private SwBufferOutputStream outputStream;

    private final SwBuffer outputBuffer;

    private final SwBuffer compressedBuffer;

    private static final byte NO_COMPRESSION = 0;

    private static final byte SNAPPY = 1;

    private final byte[] header = new byte[]{'s', 'w', 'l', 0};

    private boolean terminated;

    private int logFileIndex;

    private final List<String> existedLogFiles = new ArrayList<>();

    public WalManager(ObjectStore objectStore,
                      SwBufferManager bufferManager,
                      @Value("${sw.datastore.walFileSize}") int walFileSize,
                      @Value("${sw.datastore.walMaxFileSize}") int walMaxFileSize,
                      @Value("${sw.datastore.walPrefix}") String walPrefix,
                      @Value("${sw.datastore.walWaitIntervalMillis}") int walWaitIntervalMillis) throws IOException {
        this.objectStore = objectStore;
        this.bufferManager = bufferManager;
        this.walFileSize = walFileSize;
        this.walMaxFileSize = walMaxFileSize;
        this.walMaxFileSizeNoHeader = this.walMaxFileSize - this.header.length;
        this.logFilePrefix = walPrefix + "wal.log.";
        this.walWaitIntervalMillis = walWaitIntervalMillis;
        this.outputBuffer = this.bufferManager.allocate(this.walMaxFileSizeNoHeader);
        this.compressedBuffer = this.bufferManager.allocate(this.walMaxFileSize);
        this.outputStream = new SwBufferOutputStream(this.outputBuffer);
        var walMap = new TreeMap<Integer, String>();
        var it = this.objectStore.list(this.logFilePrefix);
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
            this.existedLogFiles.addAll(walMap.values());
        }
        this.start();
    }

    public Iterator<Wal.WalEntry> readAll() {
        return new Iterator<>() {
            private final List<String> files = new ArrayList<>(WalManager.this.existedLogFiles);
            private final SwBuffer buf = WalManager.this.bufferManager.allocate(WalManager.this.walMaxFileSize);
            private SwBufferInputStream inputStream;

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
                    return ret;
                } catch (IOException e) {
                    log.error("failed to parse proto", e);
                    this.inputStream = null;
                    throw new SWProcessException(SWProcessException.ErrorType.DATASTORE);
                }
            }

            private void getNext() {
                var fn = this.files.get(0);
                this.files.remove(0);
                SwBuffer data;
                try {
                    data = objectStore.get(fn);
                } catch (IOException e) {
                    log.error("fail to read from object store", e);
                    throw new SWProcessException(SWProcessException.ErrorType.DATASTORE);
                }
                if (data.capacity() < WalManager.this.header.length) {
                    log.error("corrupted file, size={}", data.capacity());
                    throw new SWProcessException(SWProcessException.ErrorType.DATASTORE);
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
                        log.error("fail to uncompress", e);
                        throw new SWProcessException(SWProcessException.ErrorType.DATASTORE);
                    }
                }
                WalManager.this.bufferManager.release(data);
                this.inputStream = new SwBufferInputStream(this.buf.slice(0, uncompressedSize));
            }
        };
    }

    public void append(Wal.WalEntry entry) {
        if (entry.getSerializedSize() > this.walMaxFileSizeNoHeader) {
            for (var e : this.splitEntry(entry)) {
                this.append(e);
            }
        } else {
            synchronized (this.entries) {
                if (this.terminated) {
                    throw new SWProcessException(SWProcessException.ErrorType.DATASTORE, "terminated");
                }
                this.entries.add(entry);
            }
        }
    }

    public void terminate() {
        synchronized (this.entries) {
            if (this.terminated) {
                return;
            }
            this.terminated = true;
            this.entries.notifyAll();
        }
        try {
            this.join();
        } catch (InterruptedException e) {
            log.warn("interrupted", e);
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

    private enum PopulationStatus {
        TERMINATED,
        BUFFER_FULL,
        NO_MORE_ENTRIES
    }

    private PopulationStatus populateOutput() {
        synchronized (this.entries) {
            while (!this.terminated && this.entries.isEmpty()) {
                try {
                    this.entries.wait(this.walWaitIntervalMillis);
                } catch (InterruptedException e) {
                    log.warn("interrupted", e);
                }
            }
            if (this.terminated && this.entries.isEmpty()) {
                return PopulationStatus.TERMINATED;
            }
        }
        for (; ; ) {
            Wal.WalEntry entry;
            synchronized (this.entries) {
                if (this.entries.isEmpty()) {
                    break;
                }
                entry = this.entries.getFirst();
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
            synchronized (this.entries) {
                this.entries.removeFirst();
            }
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
        try {
            int compressedBufferSize = compressedSize + this.header.length;
            Retry.decorateCheckedRunnable(
                            Retry.of("put", RetryConfig.custom()
                                    .maxAttempts(10000)
                                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5, 10000))
                                    .build()),
                            () -> this.objectStore.put(this.logFilePrefix + this.logFileIndex,
                                    this.compressedBuffer.slice(0, compressedBufferSize)))
                    .run();
        } catch (Throwable e) {
            log.error("data loss: failed to write wal log", e);
        }
        if (clearOutput) {
            ++this.logFileIndex;
            this.outputStream = new SwBufferOutputStream(this.outputBuffer);
        }
    }

    private List<Wal.WalEntry> splitEntry(Wal.WalEntry entry) {
        List<Wal.WalEntry> ret = new ArrayList<>();
        var builder = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName(entry.getTableName());
        int headerSize = builder.build().getSerializedSize();
        if (entry.hasTableSchema()) {
            builder.setTableSchema(entry.getTableSchema());
        }
        int currentEntrySize = builder.build().getSerializedSize();
        if (currentEntrySize > this.walMaxFileSizeNoHeader) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                    "schema is too large or walMaxFileSize is too small. size=" + currentEntrySize
                            + " walMaxFileSizeNoHeader=" + this.walMaxFileSizeNoHeader);
        }
        for (var record : entry.getRecordsList()) {
            // field number is less than 128, so simply use 1 instead
            var recordSize = CodedOutputStream.computeMessageSize(1, record);
            currentEntrySize += recordSize;
            if (currentEntrySize + CodedOutputStream.computeUInt32SizeNoTag(currentEntrySize)
                    > this.walMaxFileSizeNoHeader) {
                ret.add(builder.build());
                builder.clearTableSchema();
                builder.clearRecords();
                currentEntrySize = headerSize + recordSize;
                if (currentEntrySize + CodedOutputStream.computeUInt32SizeNoTag(currentEntrySize)
                        > this.walMaxFileSizeNoHeader) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                            "huge single record. size=" + currentEntrySize);
                }
            }
            builder.addRecords(record);
        }
        if (builder.getRecordsCount() > 0) {
            ret.add(builder.build());
        }
        for (var e : ret) {
            if (e.getSerializedSize() > this.walMaxFileSizeNoHeader) {
                throw new SWProcessException(SWProcessException.ErrorType.DATASTORE,
                        "invalid entry size " + e.getSerializedSize());
            }
        }
        return ret;
    }
}
