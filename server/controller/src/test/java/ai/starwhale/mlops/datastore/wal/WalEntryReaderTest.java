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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.Wal.WalEntry;
import ai.starwhale.mlops.datastore.Wal.WalEntry.Type;
import ai.starwhale.mlops.exception.SwProcessException;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xerial.snappy.Snappy;

public class WalEntryReaderTest {

    private Map<Long, byte[]> logFileMap;

    private class TestReader extends WalEntryReader {

        public TestReader(List<Long> indexes, int uncompressBufferSize, boolean ignoreLastCorruptedProto) {
            super(indexes, uncompressBufferSize, ignoreLastCorruptedProto);
        }

        @Override
        protected byte[] readLogFile(long index) {
            return WalEntryReaderTest.this.logFileMap.get(index);
        }
    }

    @BeforeEach
    @SneakyThrows
    public void setUp() {
        this.logFileMap = new HashMap<>();

        var out = new ByteArrayOutputStream();
        out.write(WalManager.HEADER);
        for (int i = 0; i < 10; ++i) {
            WalEntry.newBuilder()
                    .setEntryType(Type.UPDATE)
                    .setId(i)
                    .setTableName("t")
                    .build()
                    .writeDelimitedTo(out);
        }
        this.logFileMap.put(0L, out.toByteArray());

        out = new ByteArrayOutputStream();
        for (int i = 10; i < 20; ++i) {
            WalEntry.newBuilder()
                    .setEntryType(Type.UPDATE)
                    .setId(i)
                    .setTableName("t")
                    .build()
                    .writeDelimitedTo(out);
        }
        var src = out.toByteArray();
        var dest = new byte[4096];
        System.arraycopy(WalManager.HEADER, 0, dest, 0, WalManager.HEADER.length - 1);
        dest[WalManager.HEADER.length - 1] = WalManager.SNAPPY;
        var compressedSize = Snappy.compress(src, 0, src.length, dest, WalManager.HEADER.length);
        this.logFileMap.put(1L, Arrays.copyOf(dest, WalManager.HEADER.length + compressedSize));

        out = new ByteArrayOutputStream();
        out.write(WalManager.HEADER);
        for (int i = 20; i < 30; ++i) {
            WalEntry.newBuilder()
                    .setEntryType(Type.UPDATE)
                    .setId(i)
                    .setTableName("t")
                    .build()
                    .writeDelimitedTo(out);
        }
        src = out.toByteArray();
        this.logFileMap.put(2L, Arrays.copyOf(src, src.length - 1));

        this.logFileMap.put(3L, new byte[0]);
        this.logFileMap.put(4L, new byte[]{0, 0, 0, 0});
        this.logFileMap.put(5L, new byte[]{'s', 'w', 'l', 2});
    }

    @Test
    public void testReadNormal() {
        var reader = new TestReader(List.of(0L, 1L), 4096, false);
        long i = 0;
        while (reader.hasNext()) {
            var entry = reader.next();
            assertThat(entry.getId(), is(i));
            assertThat(reader.getCurrentLogIndex(), is((long) i / 10));
            ++i;
        }
        assertThat(i, is(20L));
    }

    @Test
    public void testReadCorruptedProto() {
        var reader = new TestReader(List.of(0L, 1L, 2L), 4096, false);
        assertThrows(SwProcessException.class, () -> {
            while (reader.hasNext()) {
                reader.next();
            }
        });
    }

    @Test
    public void testReadNormalIgnoreCorruptedProto() {
        var reader = new TestReader(List.of(0L, 1L, 2L), 4096, true);
        long i = 0;
        while (reader.hasNext()) {
            var entry = reader.next();
            assertThat(entry.getId(), is(i));
            assertThat(reader.getCurrentLogIndex(), is((long) i / 10));
            ++i;
        }
        assertThat(i, is(29L));
    }

    @Test
    public void testReadInvalidData() {
        assertThrows(SwProcessException.class, () -> new TestReader(List.of(3L), 4096, false));
        assertThrows(SwProcessException.class, () -> new TestReader(List.of(4L), 4096, false));
        assertThrows(SwProcessException.class, () -> new TestReader(List.of(5L), 4096, false));
    }

    @Test
    public void testEmptyIndexes() {
        assertThat(new TestReader(List.of(), 4096, false).hasNext(), is(false));
    }
}
