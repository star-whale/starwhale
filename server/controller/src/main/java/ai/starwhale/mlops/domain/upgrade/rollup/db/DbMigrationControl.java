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

package ai.starwhale.mlops.domain.upgrade.rollup.db;

import ai.starwhale.mlops.backup.db.MysqlBackupService;
import ai.starwhale.mlops.configuration.DataSourceProperties;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DbMigrationControl implements RollingUpdateStatusListener {

    private final Flyway flyway;

    private final StorageAccessService accessService;

    private final StoragePathCoordinator storagePathCoordinator;

    private final MysqlBackupService mysqlBackupService;

    public DbMigrationControl(Flyway flyway,
                              StorageAccessService accessService,
                              DataSourceProperties dataSourceProperties,
                              StoragePathCoordinator storagePathCoordinator
    ) {
        this.flyway = flyway;
        this.accessService = accessService;
        this.storagePathCoordinator = storagePathCoordinator;
        this.mysqlBackupService = MysqlBackupService.builder()
                .driver(dataSourceProperties.getDriverClassName())
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(dataSourceProperties.getPassword())
                .build();;
    }

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) throws InterruptedException {
        if (status == ServerInstanceStatus.READY_UP) {
            try {
                String sql = mysqlBackupService.backup(Set.of("server_status", "upgrade_log"));
                //store sql
                accessService.put(getStorePath(), new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)));

            } catch (SQLException | ClassNotFoundException e) {
                throw new SwProcessException(ErrorType.DB, "Backup mysql error.", e);
            } catch (IOException e) {
                throw new SwProcessException(ErrorType.DATASTORE, "Backup mysql error.", e);
            }
        }
    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.READY_DOWN) {
            try {
                flyway.migrate();
            } catch (Exception e) {
                log.error("flyway db migration failed", e);
                // https://documentation.red-gate.com/fd/rolling-back-138347144.html#Rollingback-Rollingback
                flyway.undo();
                throw e;
            }
        }
    }

    private String getStorePath() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String now = LocalDateTime.now().format(formatter);
        return storagePathCoordinator.allocatePluginPath("backupdb", now);
    }
}
