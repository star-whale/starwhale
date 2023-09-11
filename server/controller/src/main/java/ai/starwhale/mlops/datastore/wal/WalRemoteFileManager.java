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
import ai.starwhale.mlops.datastore.wal.WalLocalFileManager.LogFileInfo;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;

@Slf4j
public class WalRemoteFileManager extends Thread {

    private transient boolean terminated;
    private final StorageAccessService storageAccessService;

    private final int ossMaxAttempts;
    private final String logFilePrefix;

    private final TreeMap<Long, LogFileInfo> logFileToPush = new TreeMap<>();

    private LogFileInfo lastPushed;

    private final TreeMap<Long, Long> indexEntryIdMap = new TreeMap<>();

    public WalRemoteFileManager(StorageAccessService storageAccessService, int ossMaxAttempts, String logFilePrefix) {
        this.storageAccessService = storageAccessService;
        this.ossMaxAttempts = ossMaxAttempts;
        this.logFilePrefix = logFilePrefix;
        try {
            var indexes = WalManager.parseIndexes(Retry.decorateCheckedSupplier(
                            this.createRetry("list"),
                            () -> this.storageAccessService.list(this.logFilePrefix))
                    .apply());
            for (var index : indexes) {
                this.indexEntryIdMap.put(index, -1L);
            }
        } catch (Throwable e) {
            throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "fail to list WAL", e);
        }
        this.start();
    }

    public long getMaxIndex() {
        synchronized (this.indexEntryIdMap) {
            if (this.indexEntryIdMap.isEmpty()) {
                return -1;
            }
            return this.indexEntryIdMap.lastKey();
        }
    }


    public void terminate() {
        this.terminated = true;
        synchronized (this.logFileToPush) {
            this.logFileToPush.notifyAll();
        }
        try {
            this.join();
        } catch (InterruptedException e) {
            log.warn("interrupted", e);
        }
    }

    public Iterator<WalEntry> readAll(long maxIndex, int maxUncompressedSize) {
        this.indexEntryIdMap.keySet().removeIf(index -> index > maxIndex);
        return new WalEntryReader(
                new ArrayList<>(this.indexEntryIdMap.keySet()),
                maxUncompressedSize,
                false) {

            @Override
            public WalEntry next() {
                var entry = super.next();
                WalRemoteFileManager.this.indexEntryIdMap.put(this.getCurrentLogIndex(), entry.getId());
                return entry;
            }

            protected byte[] readLogFile(long logIndex) {
                try {
                    return Retry.decorateCheckedSupplier(
                                    WalRemoteFileManager.this.createRetry("get"),
                                    () -> {
                                        try (var input = WalRemoteFileManager.this.storageAccessService.get(
                                                WalRemoteFileManager.this.logFilePrefix + logIndex)) {
                                            int len = Math.toIntExact(input.getSize());
                                            var ret = new byte[len];
                                            var n = input.readNBytes(ret, 0, len);
                                            if (n != len) {
                                                throw new RuntimeException(
                                                        MessageFormat.format("expected size {0}, actual {1}", len, n));
                                            }
                                            return ret;
                                        }
                                    })
                            .apply();
                } catch (Throwable e) {
                    throw new SwProcessException(ErrorType.DATASTORE, "fail to read from storage", e);
                }
            }
        };
    }


    public void push(LogFileInfo info) {
        synchronized (this.logFileToPush) {
            this.logFileToPush.put(info.getIndex(), info);
            this.logFileToPush.notifyAll();
        }
    }

    @Override
    public void run() {
        while (!this.terminated || !this.logFileToPush.isEmpty()) {
            LogFileInfo info;
            synchronized (this.logFileToPush) {
                if (this.logFileToPush.isEmpty()) {
                    try {
                        this.logFileToPush.wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    continue;
                } else {
                    var entry = this.logFileToPush.firstEntry();
                    info = entry.getValue();
                }
            }
            try {
                try (var in = Files.newInputStream(info.getPath(), StandardOpenOption.READ)) {
                    var b = in.readNBytes(info.getSize());
                    if (b.length != info.getSize()) {
                        throw new RuntimeException(
                                MessageFormat.format("invalid file {}, expected size {0}, actual {1}",
                                        info.getPath(), info.getSize(), b.length));
                    }
                    int compressedSize;
                    var compressedBuffer = new byte[b.length + 100];
                    try {
                        compressedBuffer[0] = WalManager.HEADER[0];
                        compressedBuffer[1] = WalManager.HEADER[1];
                        compressedBuffer[2] = WalManager.HEADER[2];
                        compressedBuffer[3] = WalManager.SNAPPY;
                        compressedSize =
                                Snappy.compress(
                                        b,
                                        WalManager.HEADER.length,
                                        b.length - WalManager.HEADER.length,
                                        compressedBuffer,
                                        WalManager.HEADER.length);
                        compressedSize += WalManager.HEADER.length;
                        if (compressedSize > info.getSize()) {
                            compressedBuffer = b;
                            compressedSize = b.length;
                        }
                    } catch (IOException e) {
                        log.warn("failed to compress", e);
                        compressedBuffer = b;
                        compressedSize = b.length;
                    }
                    this.writeToStorageAccessService(this.logFilePrefix + info.getIndex(),
                            new ByteArrayInputStream(compressedBuffer, 0, compressedSize), compressedSize);
                }
                if (this.lastPushed != null && info.getIndex() > this.lastPushed.getIndex()) {
                    log.debug("remove {}", this.lastPushed.getPath());
                    Files.delete(this.lastPushed.getPath());
                }
                synchronized (this.indexEntryIdMap) {
                    this.indexEntryIdMap.put(info.getIndex(), info.getMaxEntryId());
                }
                this.lastPushed = info;
                synchronized (this.logFileToPush) {
                    this.logFileToPush.remove(info.getIndex(), info);
                }
            } catch (Throwable e) {
                log.error("failed to sync file", e);
                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }
    }

    public void removeWalLogFiles(long minWalLogIdToRetain) throws IOException {
        List<Long> indexesToRemove;
        synchronized (this.indexEntryIdMap) {
            var minValue = Math.min(this.indexEntryIdMap.values().stream().mapToLong(v -> v).max().orElse(0),
                    minWalLogIdToRetain);
            indexesToRemove = this.indexEntryIdMap.entrySet().stream()
                    .filter(entry -> entry.getValue() < minValue
                            && entry.getKey() < this.indexEntryIdMap.lastKey())
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
        }
        for (var logIndex : indexesToRemove) {
            this.storageAccessService.delete(this.logFilePrefix + logIndex);
        }
        synchronized (this.indexEntryIdMap) {
            for (var logIndex : indexesToRemove) {
                this.indexEntryIdMap.remove(logIndex);
            }
        }
    }

    private void writeToStorageAccessService(String key, InputStream inputStream, long size) throws Throwable {
        Retry.decorateCheckedRunnable(
                        this.createRetry("put"),
                        () -> this.storageAccessService.put(key,
                                inputStream,
                                size))
                .run();
    }

    private Retry createRetry(String name) {
        return Retry.of(name, RetryConfig.custom()
                .maxAttempts(this.ossMaxAttempts)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(100, 2.0, 0.5, 10000))
                .build());
    }
}
