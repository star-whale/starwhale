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

package ai.starwhale.mlops.datastore.wal;

import ai.starwhale.mlops.datastore.Wal.WalEntry;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.google.protobuf.CodedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;

@Slf4j
public class WalManager extends Thread {

    public static final byte NO_COMPRESSION = 0;
    public static final byte SNAPPY = 1;

    public static final byte[] HEADER = new byte[]{'s', 'w', 'l', NO_COMPRESSION};

    private static final long FLUSH_ID = -12345;

    private final LinkedList<WalEntry> entriesToWrite = new LinkedList<>();
    private final int walMaxFileSize;
    private final int walMaxFileSizeNoHeader;
    private transient boolean terminated;

    private long maxEntryId;

    private final WalLocalFileManager walLocalFileManager;

    private final WalRemoteFileManager walRemoteFileManager;


    public WalManager(StorageAccessService storageAccessService,
            int walMaxFileSize,
            Path walLocalCacheDir,
            String walPrefix,
            int ossMaxAttempts) throws IOException {
        this.walMaxFileSize = walMaxFileSize;
        this.walMaxFileSizeNoHeader = walMaxFileSize - WalManager.HEADER.length;
        this.walRemoteFileManager = new WalRemoteFileManager(storageAccessService,
                ossMaxAttempts,
                walPrefix + "wal.log."
        );
        this.walLocalFileManager = new WalLocalFileManager(walLocalCacheDir,
                walMaxFileSize,
                this.walRemoteFileManager.getMaxIndex() + 1);
        this.start();
    }

    public Iterator<WalEntry> readAll() {
        var firstLocalIndex = this.walLocalFileManager.getFirstIndex();
        var remoteReader = this.walRemoteFileManager.readAll(
                firstLocalIndex < 0 ? Long.MAX_VALUE : firstLocalIndex - 1,
                this.walMaxFileSizeNoHeader);
        var infoList = new ArrayList<WalLocalFileManager.LogFileInfo>();
        var localReader = this.walLocalFileManager.readAll(this.walMaxFileSizeNoHeader, infoList);
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                var ret = remoteReader.hasNext() || localReader.hasNext();
                if (!ret) {
                    for (var info : infoList) {
                        WalManager.this.walRemoteFileManager.push(info);
                    }
                }
                return ret;
            }

            @Override
            public WalEntry next() {
                WalEntry entry;
                if (remoteReader.hasNext()) {
                    entry = remoteReader.next();
                } else {
                    entry = localReader.next();
                }
                WalManager.this.maxEntryId = entry.getId();
                return entry;
            }
        };
    }

    public long getMaxEntryId() {
        synchronized (this.entriesToWrite) {
            return this.maxEntryId;
        }
    }

    public long append(WalEntry.Builder builder) {
        synchronized (this.entriesToWrite) {
            if (this.terminated) {
                throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "terminated");
            }
            var entry = builder.setId(this.maxEntryId + 1).build();
            if (CodedOutputStream.computeMessageSizeNoTag(entry) <= this.walMaxFileSizeNoHeader) {
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
        var entry = WalEntry.newBuilder().setId(FLUSH_ID).build();
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
        this.walRemoteFileManager.terminate();
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
        this.walRemoteFileManager.removeWalLogFiles(minWalLogIdToRetain);
    }

    @Override
    public void run() {
        for (; ; ) {
            List<WalEntry> entriesBatch = new ArrayList<>();
            List<WalEntry> entriesToNotify = new ArrayList<>();
            synchronized (this.entriesToWrite) {
                while (!this.terminated && this.entriesToWrite.isEmpty()) {
                    try {
                        this.entriesToWrite.wait();
                    } catch (InterruptedException e) {
                        log.warn("interrupted", e);
                    }
                }
                if (this.entriesToWrite.isEmpty()) {
                    // terminated
                    break;
                }
                for (var entry : this.entriesToWrite) {
                    if (entry.getId() == FLUSH_ID) {
                        entriesToNotify.add(entry);
                    } else {
                        entriesBatch.add(entry);
                    }
                }
                this.entriesToWrite.clear();
            }
            if (!entriesBatch.isEmpty()) {
                for (int i = 0; ; ++i) {
                    try {
                        int n = this.walLocalFileManager.write(entriesBatch);
                        this.walRemoteFileManager.push(this.walLocalFileManager.getCurrentLogFileInfo());
                        if (n == entriesBatch.size()) {
                            break;
                        }
                        entriesBatch = entriesBatch.subList(n, entriesBatch.size());
                        i = 0;
                    } catch (IOException e) {
                        if (i % 30 == 0) {
                            log.error("fail to write wal log", e);
                        }
                        try {
                            //noinspection BusyWait
                            Thread.sleep(Math.min((1 << i) * 10, 10000));
                        } catch (InterruptedException e1) {
                            // ignore
                        }
                    }
                }
            }
            for (var entry : entriesToNotify) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (entry) {
                    entry.notify();
                }
            }
        }
    }


    private void splitEntryAndAppend(WalEntry entry) {
        List<WalEntry> entries = new ArrayList<>();
        var builder = WalEntry.newBuilder()
                .setId(this.maxEntryId + 1)
                .setEntryType(WalEntry.Type.UPDATE)
                .setTableName(entry.getTableName());
        if (entry.hasTableSchema()) {
            builder.setTableSchema(entry.getTableSchema());
        }
        int currentEntrySize = builder.build().getSerializedSize();
        if (currentEntrySize > this.walMaxFileSizeNoHeader) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "schema is too large or walMaxFileSize is too small. size=" + currentEntrySize
                            + " walMaxFileSize=" + this.walMaxFileSize);
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

    static List<Long> parseIndexes(Stream<String> stream) {
        return stream
                .map(fn -> {
                    var pos = fn.lastIndexOf('.');
                    if (pos >= 0) {
                        try {
                            return Long.parseLong(fn.substring(pos + 1));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                    return -1L;
                })
                .filter(index -> index >= 0)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    static InputStream newWalEntryInputStream(byte[] data, byte[] uncompressBuffer) {
        if (data.length < WalManager.HEADER.length) {
            throw new SwProcessException(ErrorType.DATASTORE,
                    MessageFormat.format("corrupted file, size={0}", data.length));
        }
        if (data[0] != WalManager.HEADER[0]
                || data[1] != WalManager.HEADER[1]
                || data[2] != WalManager.HEADER[2]) {
            throw new SwProcessException(ErrorType.DATASTORE, "invalid wal log file header");
        }
        int uncompressedSize;
        if (data[3] == WalManager.NO_COMPRESSION) {
            return new ByteArrayInputStream(data,
                    WalManager.HEADER.length,
                    data.length - WalManager.HEADER.length);
        } else if (data[3] == WalManager.SNAPPY) {
            try {
                uncompressedSize = Snappy.uncompress(data,
                        WalManager.HEADER.length,
                        data.length - WalManager.HEADER.length,
                        uncompressBuffer,
                        0);
                return new ByteArrayInputStream(uncompressBuffer, 0, uncompressedSize);
            } catch (IOException e) {
                throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "fail to uncompress",
                        e);
            }
        } else {
            throw new SwProcessException(SwProcessException.ErrorType.DATASTORE,
                    "unknown compression code" + data[3]);
        }
    }
}
