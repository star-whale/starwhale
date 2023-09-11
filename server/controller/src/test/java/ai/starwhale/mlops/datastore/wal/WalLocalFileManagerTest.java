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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.Wal.WalEntry;
import ai.starwhale.mlops.datastore.Wal.WalEntry.Type;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WalLocalFileManagerTest {

    private FileSystem fs;

    private WalLocalFileManager walLocalFileManager;

    @BeforeEach
    @SneakyThrows
    public void setUp() {
        this.fs = Jimfs.newFileSystem(Configuration.unix().toBuilder().setBlockSize(64).setMaxSize(4096).build());
        Path cacheDir = this.fs.getPath("/wal");
        Files.createDirectories(cacheDir);
        for (int i = 0; i < 10; ++i) {
            try (var out = Files.newOutputStream(cacheDir.resolve("wal.log." + i),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                out.write(WalManager.HEADER);
                for (int j = 0; j < 10; ++j) {
                    WalEntry.newBuilder()
                            .setEntryType(Type.UPDATE)
                            .setId(i * 10 + j)
                            .setTableName("t")
                            .build()
                            .writeDelimitedTo(out);
                }
            }
        }
        this.walLocalFileManager = new WalLocalFileManager(cacheDir, 1024, 0);
    }

    @AfterEach
    @SneakyThrows
    public void tearDown() {
        this.fs.close();
    }

    @Test
    @SneakyThrows
    public void testNormal() {
        long i = 0;
        var it = this.walLocalFileManager.readAll(4096);
        while (it.hasNext()) {
            var entry = it.next();
            assertThat(entry.getId(), is(i));
            ++i;
        }
        assertThat(i, is(100L));
        assertThat(this.walLocalFileManager.getFirstIndex(), is(0L));
        var info = this.walLocalFileManager.getCurrentLogFileInfo();
        assertThat(info.getIndex(), is(9L));
        assertThat(info.getPath().toString(), is("/wal/wal.log.9"));
        assertThat(info.getMaxEntryId(), is(99L));
        assertThat(info.getSize(), is(64));

        assertThat(this.walLocalFileManager.write(List.of(WalEntry.newBuilder()
                .setEntryType(Type.UPDATE)
                .setId(110)
                .setTableName("t")
                .build())), is(1));
        info = this.walLocalFileManager.getCurrentLogFileInfo();
        assertThat(info.getIndex(), is(9L));
        assertThat(info.getPath().toString(), is("/wal/wal.log.9"));
        assertThat(info.getMaxEntryId(), is(110L));
        assertThat(info.getSize(), is(70));
        assertThat(Files.readAllBytes(info.getPath()).length, is(70));
        assertThat(this.walLocalFileManager.write(
                        IntStream.range(111, 200).mapToObj(j -> WalEntry.newBuilder()
                                .setEntryType(Type.UPDATE)
                                .setId(j)
                                .setTableName("t")
                                .build()).collect(Collectors.toList())),
                is(89));
        info = this.walLocalFileManager.getCurrentLogFileInfo();
        assertThat(info.getIndex(), is(9L));
        assertThat(info.getPath().toString(), is("/wal/wal.log.9"));
        assertThat(info.getMaxEntryId(), is(199L));
        assertThat(info.getSize(), is(676));
        assertThat(Files.readAllBytes(info.getPath()).length, is(676));

        assertThat(this.walLocalFileManager.write(
                        IntStream.range(200, 300).mapToObj(j -> WalEntry.newBuilder()
                                .setEntryType(Type.UPDATE)
                                .setId(j)
                                .setTableName("t")
                                .build()).collect(Collectors.toList())),
                is(49));
        info = this.walLocalFileManager.getCurrentLogFileInfo();
        assertThat(info.getIndex(), is(9L));
        assertThat(info.getPath().toString(), is("/wal/wal.log.9"));
        assertThat(info.getMaxEntryId(), is(248L));
        assertThat(info.getSize(), is(1019));
        assertThat(Files.readAllBytes(info.getPath()).length, is(1019));

        assertThat(this.walLocalFileManager.write(
                        IntStream.range(249, 400).mapToObj(j -> WalEntry.newBuilder()
                                .setEntryType(Type.UPDATE)
                                .setId(j)
                                .setTableName("t")
                                .build()).collect(Collectors.toList())),
                is(145));
        info = this.walLocalFileManager.getCurrentLogFileInfo();
        assertThat(info.getIndex(), is(10L));
        assertThat(info.getPath().toString(), is("/wal/wal.log.10"));
        assertThat(info.getMaxEntryId(), is(393L));
        assertThat(info.getSize(), is(1019));
        assertThat(Files.readAllBytes(info.getPath()).length, is(1019));
    }

    @Test
    @SneakyThrows
    public void testStartWithoutLogs() {
        this.walLocalFileManager = new WalLocalFileManager(this.fs.getPath("/tmp"), 1024, 1);
        assertThat(this.walLocalFileManager.readAll(4096).hasNext(), is(false));
        assertThat(this.walLocalFileManager.getFirstIndex(), is(-1L));
        assertThat(this.walLocalFileManager.getCurrentLogFileInfo(), is(nullValue()));
        this.walLocalFileManager.write(List.of(WalEntry.newBuilder()
                .setEntryType(Type.UPDATE)
                .setId(1)
                .setTableName("t")
                .build()));
        var info = this.walLocalFileManager.getCurrentLogFileInfo();
        assertThat(info.getIndex(), is(1L));
        assertThat(info.getPath().toString(), is("/tmp/wal.log.1"));
        assertThat(info.getMaxEntryId(), is(1L));
        assertThat(info.getSize(), is(10));
        assertThat(Files.readAllBytes(info.getPath()).length, is(10));
    }

    @Test
    @SneakyThrows
    public void testWriteFailure() {
        this.walLocalFileManager = new WalLocalFileManager(this.fs.getPath("/wal"), 4096, 0);
        var it = this.walLocalFileManager.readAll(4096);
        while (it.hasNext()) {
            it.next();
        }
        assertThrows(IOException.class,
                () -> this.walLocalFileManager.write(IntStream.range(100, 1000).mapToObj(i -> WalEntry.newBuilder()
                        .setEntryType(Type.UPDATE)
                        .setId(i)
                        .setTableName("t")
                        .build()).collect(Collectors.toList())));
        this.walLocalFileManager.write(List.of(WalEntry.newBuilder()
                .setEntryType(Type.UPDATE)
                .setId(110)
                .setTableName("t")
                .build()));
        var info = this.walLocalFileManager.getCurrentLogFileInfo();
        assertThat(info.getIndex(), is(9L));
        assertThat(info.getPath().toString(), is("/wal/wal.log.9"));
        assertThat(info.getMaxEntryId(), is(110L));
        assertThat(info.getSize(), is(70));
        assertThat(Files.readAllBytes(info.getPath()).length, is(70));
    }

    @Test
    @SneakyThrows
    public void testSuggestedIndex() {
        this.walLocalFileManager = new WalLocalFileManager(this.fs.getPath("/wal"), 1024, 10);
        var it = this.walLocalFileManager.readAll(4096);
        while (it.hasNext()) {
            it.next();
        }
        assertThat(this.walLocalFileManager.getCurrentLogFileInfo().getIndex(), is(9L));
        this.walLocalFileManager = new WalLocalFileManager(this.fs.getPath("/wal"), 1024, 11);
        it = this.walLocalFileManager.readAll(4096);
        while (it.hasNext()) {
            it.next();
        }
        assertThat(this.walLocalFileManager.getCurrentLogFileInfo(), is(nullValue()));
    }
}
