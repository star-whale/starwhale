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

import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackupManager {
    private final String snapshotPathOfDataStore;
    private final String walPathOfDataStore;

    private final String backupRoot;
    private final String backupSharedRoot;
    private final String backupBackupsRoot;

    private final StorageAccessService storageAccessService;

    private final Map<String, Meta> backups = new HashMap<>();
    private Map<String, InterSnapshot> sharedSnapshots = new HashMap<>();

    private static final String META_FILE_NAME = "meta.json";
    private static final String SHARED_FOLDER_NAME = "shared";
    private static final String BACKUPS_FOLDER_NAME = "backups";
    private static final String WAL_FOLDER_NAME_IN_BACKUP = "wal";

    @Data
    @Builder
    static class InterSnapshot {
        private int refCount;
        private String originName;
    }

    /**
     * Backup manager, the backup path will be like:
     * <pre>
     * backupPath:
     * - backups
     *   - backup-1-uuid
     *     - meta.json
     *     - wal-1
     *   - backup-2-uuid
     *     - meta.json
     *     - wal-2
     * - shared
     *   - shared snapshot-1
     *   - shared snapshot-2
     * </pre>
     *
     * @param backupRoot              backup path, where to store backups
     * @param snapshotPathOfDataStore snapshot path of current datastore
     * @param walPathOfDataStore      wal path of current datastore
     * @param storageAccessService    storage access service
     */
    public BackupManager(
            String backupRoot,
            String snapshotPathOfDataStore,
            String walPathOfDataStore,
            StorageAccessService storageAccessService
    ) throws IOException {
        this.backupRoot = backupRoot;
        this.backupSharedRoot = join(this.backupRoot, SHARED_FOLDER_NAME);
        this.backupBackupsRoot = join(this.backupRoot, BACKUPS_FOLDER_NAME);
        this.snapshotPathOfDataStore = join(snapshotPathOfDataStore);
        this.walPathOfDataStore = join(walPathOfDataStore);
        this.storageAccessService = storageAccessService;
        log.debug("backupRoot: {}, snapshotPathOfDataStore: {}, walPathOfDataStore: {}",
                this.backupRoot, this.snapshotPathOfDataStore, this.walPathOfDataStore);
        this.load();
    }

    public Meta createBackup() throws IOException {
        var createdAt = System.currentTimeMillis();
        // load all the file to back up {snapshot, wal}
        var snapshots = this.storageAccessService.list(this.snapshotPathOfDataStore).collect(Collectors.toList());
        var wal = this.storageAccessService.list(this.walPathOfDataStore).collect(Collectors.toList());

        // calc the approximate size
        long size = 0L;
        var iterator = Stream.concat(snapshots.stream(), wal.stream()).iterator();
        while (iterator.hasNext()) {
            var file = iterator.next();
            var info = this.storageAccessService.head(file, true);
            if (info == null) {
                continue;
            }
            size += info.getContentLength();
        }

        List<Snapshot> snapshotsMeta = new ArrayList<>();
        // file -> md5sum
        Map<String, String> snapshotsToCopy = new HashMap<>();
        // check if the snapshot in the shared folder is the same as the current one
        for (var snapshot : snapshots) {
            var info = this.storageAccessService.head(snapshot, true);
            if (info == null) {
                continue;
            }
            var md5sum = info.getMd5sum();
            if (this.sharedSnapshots.containsKey(md5sum)) {
                var originName = snapshot.substring(this.snapshotPathOfDataStore.length() + 1);
                snapshotsMeta.add(Snapshot.builder()
                        .md5sum(md5sum)
                        .originName(originName)
                        .build());
            } else {
                snapshotsToCopy.put(snapshot, md5sum);
            }
        }
        // create a new backup folder
        var id = UUID.randomUUID().toString();
        var backupDir = join(this.backupBackupsRoot, id);

        // copy snapshots
        for (var snapshot : snapshotsToCopy.entrySet()) {
            var file = snapshot.getKey();
            var md5sum = snapshot.getValue();
            this.saveSnapshot(file, md5sum);
            // add this snapshot info to the global shared snapshots
            this.sharedSnapshots.put(md5sum, InterSnapshot.builder().originName(file).refCount(1).build());
            var originName = file.substring(this.snapshotPathOfDataStore.length() + 1);
            snapshotsMeta.add(Snapshot.builder()
                    .md5sum(md5sum)
                    .originName(originName)
                    .build());
        }

        if (snapshotsMeta.isEmpty()) {
            snapshotsMeta = null;
        }

        // copy the wal
        for (var file : wal) {
            var fileWithoutPrefix = file.substring(this.walPathOfDataStore.length() + 1);
            var target = join(backupDir, WAL_FOLDER_NAME_IN_BACKUP, fileWithoutPrefix);
            this.storageCopy(file, target);
        }

        var metaFile = join(backupDir, META_FILE_NAME);
        var meta = Meta.builder()
                .id(id)
                .createdAt(createdAt)
                .doneAt(System.currentTimeMillis())
                .approximateSizeBytes(size)
                .snapshots(snapshotsMeta)
                .build();
        this.storageAccessService.put(metaFile, meta.toBytes());
        this.backups.put(meta.getId(), meta);

        return meta;
    }

    public List<Meta> listBackups() {
        return this.backups.values().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public void restoreWithBackup(String metaId) throws IOException {
        var meta = this.backups.get(metaId);
        if (meta == null) {
            throw new RuntimeException("Backup not found");
        }

        // delete the old snapshot and wal
        log.info("delete the old snapshot and wal");
        var oldFiles = this.storageAccessService.list(this.snapshotPathOfDataStore).collect(Collectors.toList());
        for (var file : oldFiles) {
            this.storageAccessService.delete(file);
        }
        oldFiles = this.storageAccessService.list(this.walPathOfDataStore).collect(Collectors.toList());
        for (var file : oldFiles) {
            this.storageAccessService.delete(file);
        }

        // restore wal
        log.info("restore wal");
        var walDir = join(this.backupBackupsRoot, metaId, WAL_FOLDER_NAME_IN_BACKUP);
        var files = this.storageAccessService.list(walDir).collect(Collectors.toList());
        for (var file : files) {
            var fileWithoutPrefix = file.substring(walDir.length() + 1);
            var target = join(this.walPathOfDataStore, fileWithoutPrefix);
            this.storageCopy(file, target);
        }

        // restore snapshot
        log.info("restore snapshot");
        var snapshots = meta.getSnapshots();
        if (snapshots != null) {
            for (var snapshot : snapshots) {
                var md5sum = snapshot.getMd5sum();
                var s = this.sharedSnapshots.get(md5sum);
                if (s == null) {
                    log.error("shared snapshot {} not found when restore backup {}", md5sum, snapshot.getOriginName());
                    continue;
                }
                var source = join(this.backupSharedRoot, md5sum);
                var target = join(this.snapshotPathOfDataStore, snapshot.getOriginName());
                this.storageCopy(source, target);
            }
        }
    }

    public void deleteBackup(String metaId) throws IOException {
        var meta = this.backups.get(metaId);
        if (meta == null) {
            return;
        }
        // rm the meta, make this back invalid
        var metaFile = join(this.backupBackupsRoot, metaId, META_FILE_NAME);
        this.storageAccessService.delete(metaFile);

        // rm the wal
        var walDir = join(this.backupBackupsRoot, metaId, WAL_FOLDER_NAME_IN_BACKUP);
        this.storageAccessService.list(walDir).forEach(file -> {
            try {
                this.storageAccessService.delete(file);
            } catch (IOException e) {
                log.error("delete wal file {} error", file, e);
            }
        });

        // rm the snapshot
        var snapshots = meta.getSnapshots();
        if (snapshots != null) {
            for (var snapshot : snapshots) {
                var md5sum = snapshot.getMd5sum();
                var s = this.sharedSnapshots.get(md5sum);
                if (s == null) {
                    log.error("shared snapshot {} not found when delete backup {}", md5sum, snapshot.getOriginName());
                    continue;
                }
                s.setRefCount(s.getRefCount() - 1);
                if (s.getRefCount() == 0) {
                    this.storageAccessService.delete(s.getOriginName());
                    this.sharedSnapshots.remove(md5sum);
                }
            }
        }

        this.backups.remove(metaId);
    }

    private void load() throws IOException {
        this.sharedSnapshots = this.loadSharedSnapshots();
        var backupsRoot = join(this.backupRoot, BACKUPS_FOLDER_NAME);
        List<String> backupToClean = new ArrayList<>();
        this.storageAccessService.list(backupsRoot)
                .map(file -> {
                    // leave only the backup id
                    var index = file.indexOf('/', backupsRoot.length() + 1);
                    if (index == -1) {
                        return file;
                    }
                    return file.substring(0, index);
                })
                .collect(Collectors.toSet()).stream()
                .map(dir -> {
                    var meta = this.loadBackup(dir);
                    if (meta == null) {
                        backupToClean.add(dir);
                    }
                    return meta;
                })
                .filter(Objects::nonNull)
                .forEach(meta -> this.backups.put(meta.getId(), meta));
        // update ref count for shared snapshots
        for (var meta : this.backups.values()) {
            var snapshots = meta.getSnapshots();
            if (snapshots == null) {
                continue;
            }
            for (var snapshot : snapshots) {
                var md5sum = snapshot.getMd5sum();
                var sharedSnapshot = this.sharedSnapshots.get(md5sum);
                if (sharedSnapshot == null) {
                    // this can not happen
                    log.error("shared snapshot {} not found", md5sum);
                    continue;
                }
                sharedSnapshot.setRefCount(sharedSnapshot.getRefCount() + 1);
            }
        }
        this.cleanTheUnDoneBackups(backupToClean);
    }

    private Meta loadBackup(String backupDir) {
        var metaFile = join(backupDir, META_FILE_NAME);
        try {
            var meta = this.storageAccessService.get(metaFile);
            if (meta == null) {
                return null;
            }

            log.info("load backup meta file in {}", backupDir);
            return Meta.fromInputStream(meta);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            log.error("load backup meta file in {} error", backupDir, e);
            return null;
        }
    }

    /**
     * Load the shared snapshots from the shared folder
     *
     * @return the shared snapshots (md5sum -> filename)
     */
    private Map<String, InterSnapshot> loadSharedSnapshots() throws IOException {
        var ret = new HashMap<String, InterSnapshot>();
        var files = this.storageAccessService.list(this.backupSharedRoot).collect(Collectors.toList());
        for (var file : files) {
            var info = this.storageAccessService.head(file, true);
            if (info == null) {
                continue;
            }
            ret.put(info.getMd5sum(), InterSnapshot.builder().originName(file).build());
        }
        return ret;
    }

    private void cleanTheUnDoneBackups(List<String> dir) throws IOException {
        for (var d : dir) {
            this.storageAccessService.list(d).forEach(file -> {
                try {
                    this.storageAccessService.delete(file);
                } catch (IOException e) {
                    log.error("delete file {} error", file, e);
                }
            });
            this.storageAccessService.delete(d);
        }
    }

    private void saveSnapshot(String source, String md5sum) throws IOException {
        var target = join(this.backupSharedRoot, md5sum);
        this.storageCopy(source, target);
    }

    private void storageCopy(String src, String dst) throws IOException {
        try (var content = this.storageAccessService.get(src)) {
            this.storageAccessService.put(dst, content);
        }
    }

    public static String join(String... paths) {
        return Paths.get("", paths).toString();
    }
}
