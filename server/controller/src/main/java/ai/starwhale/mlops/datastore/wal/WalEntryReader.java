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

import ai.starwhale.mlops.datastore.Wal;
import ai.starwhale.mlops.datastore.Wal.WalEntry;
import ai.starwhale.mlops.exception.SwProcessException;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class WalEntryReader implements Iterator<WalEntry> {

    private final List<Long> indexes;

    private final byte[] uncompressBuffer;
    private final boolean ignoreLastCorruptedProto;

    private InputStream inputStream;

    private WalEntry nextEntry;

    @Getter
    private long currentLogIndex;

    private long nextLogIndex;

    public WalEntryReader(List<Long> indexes, int uncompressBufferSize, boolean ignoreLastCorruptedProto) {
        this.indexes = new ArrayList<>(indexes);
        this.uncompressBuffer = new byte[uncompressBufferSize];
        this.ignoreLastCorruptedProto = ignoreLastCorruptedProto;
        this.getNextEntry();
    }

    @Override
    public boolean hasNext() {
        return this.nextEntry != null;
    }

    @Override
    public WalEntry next() {
        var ret = this.nextEntry;
        this.currentLogIndex = this.nextLogIndex;
        if (ret != null) {
            this.getNextEntry();
        }
        return ret;
    }

    private void getNextEntry() {
        if (this.inputStream == null) {
            if (this.indexes.isEmpty()) {
                this.nextEntry = null;
                return;
            }
            this.getNextStream();
        }
        try {
            var entry = Wal.WalEntry.parseDelimitedFrom(this.inputStream);
            if (this.inputStream.available() == 0) {
                this.inputStream = null;
            }
            this.nextEntry = entry;
        } catch (IOException e) {
            this.nextEntry = null;
            if (!this.ignoreLastCorruptedProto
                    || (!(e instanceof InvalidProtocolBufferException && this.indexes.isEmpty()))) {
                throw new SwProcessException(SwProcessException.ErrorType.DATASTORE,
                        "failed to parse proto from wal.log." + this.nextLogIndex,
                        e);
            }
        }
    }

    @SneakyThrows
    private void getNextStream() {
        this.nextLogIndex = this.indexes.remove(0);
        var data = this.readLogFile(this.nextLogIndex);
        this.inputStream = WalManager.newWalEntryInputStream(data, this.uncompressBuffer);
    }

    protected abstract byte[] readLogFile(long index);
}
