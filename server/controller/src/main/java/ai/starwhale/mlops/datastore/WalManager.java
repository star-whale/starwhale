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
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.google.protobuf.CodedOutputStream;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.util.FastByteArrayOutputStream;
import org.xerial.snappy.Snappy;

@Slf4j
public class WalManager extends Thread {

    private static final int WAIT_INTERVAL_MILLIS = 1000;

    private static final long FLUSH_ID = -12345;

    private final StorageAccessService storageAccessService;

    private final int walFileSize;

    private final int walMaxFileSize;

    private final int walMaxFileSizeNoHeader;

    private final String logFilePrefix;

    private final LinkedList<Wal.WalEntry> entriesToWrite = new LinkedList<>();

    private final FastByteArrayOutputStream outputStream;

    private final byte[] compressedBuffer;

    private static final byte NO_COMPRESSION = 0;

    private static final byte SNAPPY = 1;

    private final byte[] header = new byte[]{'s', 'w', 'l', 0};

    private boolean terminated;

    private int logFileIndex;

    private long maxEntryId;

    private long maxEntryIdInOutputBuffer;

    private final int ossMaxAttempts;

    private final Map<String, Long> walLogFileMap = new ConcurrentHashMap<>();

    public WalManager(StorageAccessService storageAccessService,
            int walFileSize,
            int walMaxFileSize,
            String walPrefix,
            int ossMaxAttempts) {
        this.storageAccessService = storageAccessService;
        this.walFileSize = walFileSize;
        this.walMaxFileSize = walMaxFileSize;
        this.walMaxFileSizeNoHeader =
                this.walMaxFileSize - this.header.length - 10; // 10 is for possible overhead of SNAPPY
        this.logFilePrefix = walPrefix + "wal.log.";
        this.ossMaxAttempts = ossMaxAttempts;
        this.compressedBuffer = new byte[this.walMaxFileSize];
        System.arraycopy(this.header, 0, this.compressedBuffer, 0, this.header.length);
        this.outputStream = new FastByteArrayOutputStream(this.walMaxFileSize);
        var walMap = new TreeMap<Integer, String>();
        Iterator<String> it;
        try {
            it = Retry.decorateCheckedSupplier(
                            Retry.of("put", RetryConfig.custom()
                                    .maxAttempts(ossMaxAttempts)
                                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5, 10000))
                                    .retryOnException(e -> !terminated)
                                    .build()),
                            () -> this.storageAccessService.list(this.logFilePrefix).iterator())
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
            private final byte[] buf = new byte[WalManager.this.walMaxFileSize];
            private InputStream inputStream;
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
                    if (this.inputStream.available() == 0) {
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
                byte[] data;
                try {
                    data = Retry.decorateCheckedSupplier(
                                    Retry.of("get", RetryConfig.custom()
                                            .maxAttempts(WalManager.this.ossMaxAttempts)
                                            .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                                                    100, 2.0, 0.5, 10000))
                                            .build()),
                                    () -> {
                                        var input = WalManager.this.storageAccessService.get(this.currentFile);
                                        int len = Math.toIntExact(input.getSize());
                                        var ret = new byte[len];
                                        var n = input.readNBytes(ret, 0, len);
                                        if (n != len) {
                                            throw new RuntimeException(
                                                    MessageFormat.format("expected size {0}, actual {1}", len, n));
                                        }
                                        return ret;
                                    })
                            .apply();
                } catch (Throwable e) {
                    throw new SwProcessException(ErrorType.DATASTORE, "fail to read from storage", e);
                }
                if (data.length < WalManager.this.header.length) {
                    throw new SwProcessException(ErrorType.DATASTORE,
                            MessageFormat.format("corrupted file, size={0}", data.length));
                }
                if (data[0] != WalManager.this.header[0]
                        || data[1] != WalManager.this.header[1]
                        || data[2] != WalManager.this.header[2]) {
                    throw new SwProcessException(ErrorType.DATASTORE, "invalid wal log file header");
                }
                int uncompressedSize;
                if (data[3] == NO_COMPRESSION) {
                    this.inputStream = new ByteArrayInputStream(data,
                            WalManager.this.header.length,
                            data.length - WalManager.this.header.length);
                } else if (data[3] == SNAPPY) {
                    try {
                        uncompressedSize = Snappy.uncompress(data,
                                WalManager.this.header.length,
                                data.length - WalManager.this.header.length,
                                this.buf,
                                0);
                        this.inputStream = new ByteArrayInputStream(this.buf, 0, uncompressedSize);
                    } catch (IOException e) {
                        throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "fail to uncompress", e);
                    }
                } else {
                    throw new SwProcessException(SwProcessException.ErrorType.DATASTORE,
                            "unknown compression code" + data[3]);
                }
            }
        };
    }

    public long getMaxEntryId() {
        synchronized (this.entriesToWrite) {
            return this.maxEntryId;
        }
    }

    public long append(Wal.WalEntry.Builder builder) {
        synchronized (this.entriesToWrite) {
            if (this.terminated) {
                throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "terminated");
            }
            var entry = builder.setId(this.maxEntryId + 1).build();
            if (entry.getSerializedSize() <= this.walMaxFileSizeNoHeader) {
                this.entriesToWrite.add(entry);
                ++this.maxEntryId;
            } else {
                this.splitEntryAndAppend(entry);
            }
            this.entriesToWrite.notifyAll();
            return this.maxEntryId;
        }
    }

    public void flush() {
        var entry = Wal.WalEntry.newBuilder().setId(FLUSH_ID).build();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (entry) {
            synchronized (this.entriesToWrite) {
                if (this.terminated) {
                    throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "terminated");
                }
                this.entriesToWrite.add(entry);
                this.entriesToWrite.notifyAll();
            }
            try {
                entry.wait();
            } catch (InterruptedException e) {
                log.error("interrupted", e);
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

    /**
     * Remove any WAL log files that contain no entry IDs greater than or equal to minWalLogIdToRetain, except the
     * latest one.
     * <p>
     * The latest WAL log file should always be kept so that maxEntryId can be recovered from WAl log files when the
     * data store starts up.
     *
     * @param minWalLogIdToRetain the minimum WAL log id to retain.
     * @throws IOException if the underlying storage access failed
     */
    public void removeWalLogFiles(long minWalLogIdToRetain) throws IOException {
        var minValue = Math.min(this.walLogFileMap.values().stream().mapToLong(v -> v).max().orElse(0),
                minWalLogIdToRetain);
        for (var logFile : this.walLogFileMap.entrySet().stream()
                .filter(entry -> entry.getValue() < minValue)
                .map(Entry::getKey)
                .collect(Collectors.toList())) {
            this.storageAccessService.delete(logFile);
            this.walLogFileMap.remove(logFile);
        }
    }

    @Override
    public void run() {
        for (; ; ) {
            var flushEntries = new ArrayList<Wal.WalEntry>();
            try {
                var status = this.populateOutput(flushEntries);
                if (status == PopulationStatus.TERMINATED) {
                    return;
                }
                if (this.outputStream.size() > 0) {
                    this.writeToStorage(status == PopulationStatus.BUFFER_FULL);
                }
                for (var entry : flushEntries) {
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (entry) {
                        entry.notify();
                    }
                }
            } catch (Throwable e) {
                log.error("unexpected exception", e);
                // put all flush entries back
                synchronized (this.entriesToWrite) {
                    this.entriesToWrite.addAll(flushEntries);
                }
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

    private PopulationStatus populateOutput(List<Wal.WalEntry> flushEntries) {
        synchronized (this.entriesToWrite) {
            while (!this.terminated && this.entriesToWrite.isEmpty()) {
                try {
                    this.entriesToWrite.wait(WAIT_INTERVAL_MILLIS);
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
            if (entry.getId() == FLUSH_ID) {
                synchronized (this.entriesToWrite) {
                    this.entriesToWrite.removeFirst();
                }
                flushEntries.add(entry);
                continue;
            }
            if (CodedOutputStream.computeMessageSizeNoTag(entry) + this.outputStream.size()
                    > this.walMaxFileSizeNoHeader) {
                if (this.outputStream.size() == 0) {
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
        if (this.outputStream.size() >= this.walFileSize) {
            return PopulationStatus.BUFFER_FULL;
        }
        return PopulationStatus.NO_MORE_ENTRIES;
    }

    private void writeToStorage(boolean clearOutput) {
        int compressedSize;
        try {
            this.compressedBuffer[3] = WalManager.SNAPPY;
            compressedSize = Snappy.compress(this.outputStream.toByteArrayUnsafe(),
                    0,
                    this.outputStream.size(),
                    this.compressedBuffer,
                    this.header.length);
        } catch (IOException e) {
            log.warn("failed to compress", e);
            this.compressedBuffer[3] = WalManager.NO_COMPRESSION;
            compressedSize = this.outputStream.size();
            System.arraycopy(this.outputStream.toByteArrayUnsafe(),
                    0,
                    this.compressedBuffer,
                    this.header.length,
                    compressedSize);
        }
        var key = this.logFilePrefix + this.logFileIndex;
        try {
            int compressedBufferSize = compressedSize + this.header.length;
            Retry.decorateCheckedRunnable(
                            Retry.of("put", RetryConfig.custom()
                                    .maxAttempts(this.ossMaxAttempts)
                                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5, 10000))
                                    .build()),
                            () -> this.storageAccessService.put(key,
                                    new ByteArrayInputStream(this.compressedBuffer),
                                    compressedBufferSize))
                    .run();
        } catch (Throwable e) {
            log.error("data loss: failed to write wal log", e);
        }
        this.walLogFileMap.put(key, this.maxEntryIdInOutputBuffer);
        if (clearOutput) {
            ++this.logFileIndex;
            this.outputStream.reset();
        }
    }

    private void splitEntryAndAppend(Wal.WalEntry entry) {
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
    }
}
