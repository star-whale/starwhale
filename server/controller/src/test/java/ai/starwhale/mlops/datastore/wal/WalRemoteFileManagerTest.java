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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.datastore.Wal.WalEntry;
import ai.starwhale.mlops.datastore.Wal.WalEntry.Type;
import ai.starwhale.mlops.datastore.wal.WalLocalFileManager.LogFileInfo;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WalRemoteFileManagerTest {

    private StorageAccessService storageAccessService;
    private FileSystem fs;

    private WalRemoteFileManager walRemoteFileManager;

    @BeforeEach
    @SneakyThrows
    public void setUp() {
        this.storageAccessService = new StorageAccessServiceMemory();
        for (int i = 0; i < 5; ++i) {
            var out = new ByteArrayOutputStream();
            out.write(WalManager.HEADER);
            for (int j = 0; j < 10; ++j) {
                WalEntry.newBuilder()
                        .setEntryType(Type.UPDATE)
                        .setId(i * 10 + j)
                        .setTableName("t")
                        .build()
                        .writeDelimitedTo(out);
            }
            this.storageAccessService.put("/wal/wal.log." + i, out.toByteArray());
        }
        this.fs = Jimfs.newFileSystem(Configuration.unix().toBuilder().setBlockSize(64).setMaxSize(1024).build());
        Path cacheDir = this.fs.getPath("/wal");
        Files.createDirectories(cacheDir);
        for (int i = 5; i < 10; ++i) {
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
        this.walRemoteFileManager = new WalRemoteFileManager(this.storageAccessService, 3, "/wal/wal.log.");
    }

    @AfterEach
    @SneakyThrows
    public void tearDown() {
        this.walRemoteFileManager.terminate();
        this.fs.close();
    }

    @Test
    @SneakyThrows
    public void testNormal() {
        long i = 0;
        var it = this.walRemoteFileManager.readAll(3, 4096);
        while (it.hasNext()) {
            var entry = it.next();
            assertThat(entry.getId(), is(i));
            ++i;
        }
        assertThat(i, is(40L));
        assertThat(this.walRemoteFileManager.getMaxIndex(), is(3L));
        var walLocalFileManager = new WalLocalFileManager(this.fs.getPath("/wal"), 4096, 0);
        var infoList = new ArrayList<LogFileInfo>();
        it = walLocalFileManager.readAll(4096, infoList);
        while (it.hasNext()) {
            it.next();
        }
        this.walRemoteFileManager.push(infoList.get(0));
        this.walRemoteFileManager.push(infoList.get(1));
        Thread.sleep(1000);
        assertThat(Files.exists(this.fs.getPath("/wal/wal.log.5")), is(false));
        assertThat(Files.exists(this.fs.getPath("/wal/wal.log.6")), is(true));
        this.walRemoteFileManager.terminate();
        this.walRemoteFileManager = new WalRemoteFileManager(this.storageAccessService, 3, "/wal/wal.log.");
        it = this.walRemoteFileManager.readAll(6, 4096);
        i = 0;
        while (it.hasNext()) {
            var entry = it.next();
            assertThat(entry.getId(), is(i));
            ++i;
        }
        assertThat(i, is(70L));
        assertThat(this.walRemoteFileManager.getMaxIndex(), is(6L));

        this.walRemoteFileManager.removeWalLogFiles(0);
        assertThat(this.storageAccessService.head("/wal/wal.log.0").isExists(), is(true));
        this.walRemoteFileManager.removeWalLogFiles(9);
        assertThat(this.storageAccessService.head("/wal/wal.log.0").isExists(), is(true));
        this.walRemoteFileManager.removeWalLogFiles(10);
        assertThat(this.storageAccessService.head("/wal/wal.log.0").isExists(), is(false));
        assertThat(this.storageAccessService.head("/wal/wal.log.1").isExists(), is(true));
        this.walRemoteFileManager.terminate();
        this.walRemoteFileManager = new WalRemoteFileManager(this.storageAccessService, 3, "/wal/wal.log.");
        it = this.walRemoteFileManager.readAll(1000, 4096);
        while (it.hasNext()) {
            it.next();
        }
        this.walRemoteFileManager.removeWalLogFiles(35);
        assertThat(this.storageAccessService.head("/wal/wal.log.1").isExists(), is(false));
        assertThat(this.storageAccessService.head("/wal/wal.log.2").isExists(), is(false));
        assertThat(this.storageAccessService.head("/wal/wal.log.3").isExists(), is(true));
        this.walRemoteFileManager.removeWalLogFiles(1000);
        assertThat(this.storageAccessService.head("/wal/wal.log.3").isExists(), is(false));
        assertThat(this.storageAccessService.head("/wal/wal.log.4").isExists(), is(false));
        assertThat(this.storageAccessService.head("/wal/wal.log.5").isExists(), is(false));
        assertThat(this.storageAccessService.head("/wal/wal.log.6").isExists(), is(true));
    }

    @Test
    @SneakyThrows
    public void testStartWithoutLogs() {
        this.walRemoteFileManager.terminate();
        this.walRemoteFileManager = new WalRemoteFileManager(this.storageAccessService, 3, "/tmp/wal.log.");
        assertThat(this.walRemoteFileManager.readAll(5, 4096).hasNext(), is(false));
        assertThat(this.walRemoteFileManager.getMaxIndex(), is(-1L));
    }

    @Test
    @SneakyThrows
    public void testWriteFailure() {
        var storageAccessService = Mockito.mock(StorageAccessService.class);
        given(storageAccessService.list(anyString())).willReturn(Stream.empty());
        doThrow(new IOException())
                .doThrow(new IOException())
                .doThrow(new IOException())
                .doThrow(new IOException())
                .doThrow(new IOException())
                .doNothing()
                .when(storageAccessService).put(anyString(), any(), anyLong());
        this.walRemoteFileManager.terminate();
        this.walRemoteFileManager = new WalRemoteFileManager(storageAccessService, 3, "/wal/wal.log.");
        var walLocalFileManager = new WalLocalFileManager(this.fs.getPath("/wal"), 4096, 0);
        this.walRemoteFileManager.push(walLocalFileManager.new LogFileInfo(5));
        Thread.sleep(1000);
        verify(storageAccessService, times(6)).put(anyString(), any(), anyLong());
    }

}
