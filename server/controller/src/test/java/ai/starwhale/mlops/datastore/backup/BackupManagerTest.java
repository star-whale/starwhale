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

package ai.starwhale.mlops.datastore.backup;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import java.io.IOException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BackupManagerTest {

    @Test
    public void test() throws IOException {
        var storage = new StorageAccessServiceMemory();
        var backupRoot = "backup";
        var dataStoreSnapshot = "data-store-snapshot";
        var dataStoreWal = "data-store-wal";

        storage.put(dataStoreSnapshot + "/1", new byte[]{1, 2, 3});
        storage.put(dataStoreSnapshot + "/2", new byte[]{4, 5, 6});
        storage.put(dataStoreWal + "/wal.1", new byte[]{7, 8, 9});

        var manager = new BackupManager(backupRoot, dataStoreSnapshot, dataStoreWal, storage);

        var meta = manager.createBackup();
        assertTrue(meta.getDoneAt() > meta.getCreatedAt());
        assertEquals(2, meta.getSnapshots().size());
        assertEquals("1", meta.getSnapshots().get(0).getOriginName());
        assertEquals("2", meta.getSnapshots().get(1).getOriginName());

        var allMetas = manager.listBackups();
        assertEquals(1, allMetas.size());
        assertEquals(meta, allMetas.get(0));

        // delete all the file in datastore
        storage.clear(dataStoreSnapshot);
        storage.clear(dataStoreWal);

        // restore
        // add some garbage data
        storage.put(dataStoreSnapshot + "/garbage1", new byte[]{10, 11, 12});
        storage.put(dataStoreWal + "/garbage2", new byte[]{13, 14, 15});
        manager.restoreWithBackup(meta.getId());
        // check if the garbage data is deleted
        assertFalse(storage.head(dataStoreSnapshot + "/garbage1").isExists());
        assertFalse(storage.head(dataStoreWal + "/garbage2").isExists());

        // verify
        var content = storage.get(dataStoreSnapshot + "/1").readAllBytes();
        assertArrayEquals(new byte[]{1, 2, 3}, content);
        content = storage.get(dataStoreSnapshot + "/2").readAllBytes();
        assertArrayEquals(new byte[]{4, 5, 6}, content);
        content = storage.get(dataStoreWal + "/wal.1").readAllBytes();
        assertArrayEquals(new byte[]{7, 8, 9}, content);

        // add a snapshot and create another backup
        storage.put(dataStoreSnapshot + "/3", new byte[]{10, 11, 12});
        var meta2 = manager.createBackup();
        assertEquals(3, meta2.getSnapshots().size());
        assertEquals("1", meta2.getSnapshots().get(0).getOriginName());
        assertEquals("2", meta2.getSnapshots().get(1).getOriginName());
        assertEquals("3", meta2.getSnapshots().get(2).getOriginName());

        // test list
        allMetas = manager.listBackups();
        assertEquals(2, allMetas.size());
        assertEquals(meta, allMetas.get(1));
        assertEquals(meta2, allMetas.get(0));

        // reload
        // make garbage backup (without meta.json)
        storage.put(BackupManager.join(backupRoot, "backups", "garbage", "abc"), new byte[]{10, 11, 12});
        manager = new BackupManager(backupRoot, dataStoreSnapshot, dataStoreWal, storage);
        allMetas = manager.listBackups();
        assertEquals(2, allMetas.size());
        assertEquals(meta, allMetas.get(1));
        assertEquals(meta2, allMetas.get(0));
        // check if the garbage backup is deleted
        assertFalse(storage.head(BackupManager.join(backupRoot, "backups", "garbage", "abc")).isExists());

        // check if shared snapshot reused
        var snapshots = storage.list(BackupManager.join(backupRoot, "shared")).collect(Collectors.toList());
        assertEquals(3, snapshots.size());

        // delete backup
        manager.deleteBackup(meta2.getId());
        // the shared snapshot3 should be deleted and snapshot1, snapshot2 should be kept
        snapshots = storage.list(BackupManager.join(backupRoot, "shared")).collect(Collectors.toList());
        assertEquals(2, snapshots.size());
        assertEquals(1, snapshots.stream().filter(s -> s.endsWith(meta.getSnapshots().get(0).getMd5sum())).count());
        assertEquals(1, snapshots.stream().filter(s -> s.endsWith(meta.getSnapshots().get(1).getMd5sum())).count());

        allMetas = manager.listBackups();
        assertEquals(1, allMetas.size());
        assertEquals(meta, allMetas.get(0));
    }
}
