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

package ai.starwhale.mlops.domain.upgrade.step;

import ai.starwhale.mlops.backup.db.MysqlBackupService;
import ai.starwhale.mlops.configuration.DataSourceProperties;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.upgrade.UpgradeAccess;
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Order(1)
public class BackupDatabase extends UpgradeStepBase {

    private final StorageAccessService accessService;

    private final StoragePathCoordinator storagePathCoordinator;

    @Setter
    private MysqlBackupService mysqlBackupService;

    public BackupDatabase(UpgradeAccess upgradeAccess,
            DataSourceProperties dataSourceProperties,
            StorageAccessService accessService,
            StoragePathCoordinator storagePathCoordinator) {
        super(upgradeAccess);
        this.accessService = accessService;
        this.storagePathCoordinator = storagePathCoordinator;
        this.mysqlBackupService = MysqlBackupService.builder()
                .driver(dataSourceProperties.getDriverClassName())
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(dataSourceProperties.getPassword())
                .build();
    }

    @Override
    protected void doStep(Upgrade upgrade) {
        log.info("Back up database" + upgrade.toString());
        try {
            String sql = mysqlBackupService.backup(getIgnores());
            //store sql
            accessService.put(getStorePath(), new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)));

        } catch (SQLException | ClassNotFoundException e) {
            throw new SwProcessException(ErrorType.DB, "Backup mysql error.", e);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.DATASTORE, "Backup mysql error.", e);
        }

    }

    private Set<String> getIgnores() {
        return Set.of("server_status", "upgrade_log");
    }

    private String getStorePath() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String now = LocalDateTime.now().format(formatter);
        return storagePathCoordinator.allocatePluginPath("backupdb",
                String.format("%s-%s", upgrade.getProgressId(), now));
    }

    @Override
    public boolean isComplete() {
        //Sync process
        return true;
    }

    @Override
    protected String getTitle() {
        return "Back up database";
    }

    @Override
    protected String getContent() {
        return "";
    }

    @Override
    public void cancel(Upgrade upgrade) {

    }
}
