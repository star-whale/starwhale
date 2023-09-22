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
import com.google.protobuf.CodedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalLocalFileManager {


    private final Path cacheDir;
    private final int walMaxFileSize;

    private final List<Long> indexes;

    private long logFileIndex;

    private LogFileInfo logFileInfo;

    private OutputStream outputStream;

    private int currentFileSize;

    @AllArgsConstructor
    public class LogFileInfo {

        public LogFileInfo(long index) {
            this.index = index;
            this.path = WalLocalFileManager.this.getLocalFilePath(index);
            this.maxEntryId = -1;
            this.size = WalManager.HEADER.length;
        }

        public LogFileInfo(LogFileInfo info) {
            this.index = info.index;
            this.path = info.path;
            this.maxEntryId = info.maxEntryId;
            this.size = info.size;
        }

        @Getter
        private long index;

        @Getter
        private Path path;

        @Getter
        private long maxEntryId;

        @Getter
        private int size;
    }

    public WalLocalFileManager(Path cacheDir, int walMaxFileSize, long suggestedFirstIndex) throws IOException {
        Files.createDirectories(cacheDir);
        this.cacheDir = cacheDir;
        this.walMaxFileSize = walMaxFileSize;
        try (var stream = Files.list(this.cacheDir)) {
            this.indexes = WalManager.parseIndexes(
                    stream.map(path -> path.getFileName().toString()).filter(path -> path.startsWith("wal.log.")));
        }
        if (this.indexes.isEmpty() || suggestedFirstIndex - 1 > this.indexes.get(this.indexes.size() - 1)) {
            this.logFileIndex = suggestedFirstIndex;
        } else {
            this.logFileIndex = this.indexes.get(this.indexes.size() - 1);
        }
    }

    public long getFirstIndex() {
        return this.indexes.isEmpty() ? -1 : this.indexes.get(0);
    }

    public LogFileInfo getCurrentLogFileInfo() {
        return this.logFileInfo == null ? null : new LogFileInfo(this.logFileInfo);
    }

    public int write(List<WalEntry> entries) throws IOException {
        if (this.outputStream == null) {
            var path = this.getLocalFilePath(this.logFileIndex);
            if (!Files.exists(path)) {
                this.initLogFile();
            } else {
                var ch = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                ch.truncate(this.currentFileSize);
                this.outputStream = Channels.newOutputStream(ch);
            }
        }
        int ret = entries.size();
        int sizeTotal = 0;
        try {
            for (int i = 0; i < entries.size(); ++i) {
                var entry = entries.get(i);
                var entrySize = CodedOutputStream.computeMessageSizeNoTag(entry);
                if (WalManager.HEADER.length + entrySize > this.walMaxFileSize) {
                    log.error("data loss: discard unexpected huge entry. "
                                    + "size={} table={} schema={} records count={}",
                            entry.getSerializedSize(),
                            entry.getTableName(),
                            entry.getTableSchema(),
                            entry.getRecordsCount());
                    continue;
                }
                if (this.currentFileSize + sizeTotal + entrySize > this.walMaxFileSize) {
                    if (i == 0) {
                        ++this.logFileIndex;
                        this.initLogFile();
                        return this.write(entries);
                    }
                    ret = i;
                    break;
                }
                entry.writeDelimitedTo(this.outputStream);
                sizeTotal += entrySize;
            }
            this.outputStream.flush();
        } catch (IOException e) {
            if (this.outputStream != null) {
                try {
                    this.outputStream.close();
                } catch (IOException e1) {
                    log.error("failed to close", e1);
                }
                this.outputStream = null;
            }
            throw e;
        }
        this.currentFileSize += sizeTotal;
        this.logFileInfo.maxEntryId = entries.get(ret - 1).getId();
        this.logFileInfo.size += sizeTotal;
        return ret;
    }

    public Iterator<WalEntry> readAll(int maxUncompressedSize, List<LogFileInfo> infoList) {
        return new WalEntryReader(this.indexes, maxUncompressedSize, true) {

            @Override
            public WalEntry next() {
                var entry = super.next();
                if (infoList.isEmpty()) {
                    infoList.add(new LogFileInfo(this.getCurrentLogIndex()));
                }
                var info = infoList.get(infoList.size() - 1);
                if (info.getIndex() != this.getCurrentLogIndex()) {
                    info = new LogFileInfo(this.getCurrentLogIndex());
                    infoList.add(info);
                }
                info.maxEntryId = entry.getId();
                info.size += CodedOutputStream.computeMessageSizeNoTag(entry);
                if (this.getCurrentLogIndex() == WalLocalFileManager.this.logFileIndex) {
                    WalLocalFileManager.this.logFileInfo = info;
                    WalLocalFileManager.this.currentFileSize = info.size;
                }
                return entry;
            }

            protected byte[] readLogFile(long logIndex) {
                var path = WalLocalFileManager.this.getLocalFilePath(logIndex);
                try {
                    return Files.readAllBytes(path);
                } catch (IOException e) {
                    throw new SwProcessException(ErrorType.DATASTORE, "fail to read " + path, e);
                }
            }
        };
    }

    private Path getLocalFilePath(long index) {
        return this.cacheDir.resolve("wal.log." + index);
    }

    private void initLogFile() {
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (IOException e) {
                log.error("failed to close", e);
            }
        }
        this.currentFileSize = WalManager.HEADER.length;
        this.logFileInfo = new LogFileInfo(this.logFileIndex);
        for (int i = 0; ; i++) {
            try {
                this.outputStream = Files.newOutputStream(this.logFileInfo.getPath(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                this.outputStream.write(WalManager.HEADER, 0, WalManager.HEADER.length);
                return;
            } catch (IOException e) {
                if (i % 100 == 0) {
                    log.error("failed to create new log file" + this.logFileInfo.getPath(), e);
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }
    }
}
