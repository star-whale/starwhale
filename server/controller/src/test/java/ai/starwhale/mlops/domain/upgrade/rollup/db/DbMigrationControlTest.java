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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.backup.db.MysqlBackupService;
import ai.starwhale.mlops.configuration.DataSourceProperties;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * a test for DbMigrationControl
 */
public class DbMigrationControlTest {

    private Flyway flyway;

    private StorageAccessService accessService;

    private StoragePathCoordinator storagePathCoordinator;

    private DataSourceProperties dataSourceProperties;

    private MysqlBackupService mysqlBackupService;

    private DbMigrationControl dbMigrationControl;

    @BeforeEach
    public void setup() throws NoSuchFieldException, IllegalAccessException, SQLException, ClassNotFoundException {
        flyway = mock(Flyway.class);
        accessService = mock(StorageAccessService.class);
        storagePathCoordinator = mock(StoragePathCoordinator.class);
        when(storagePathCoordinator.allocatePluginPath(any(), any())).thenReturn("path");
        dataSourceProperties = mock(DataSourceProperties.class);
        mysqlBackupService = mock(MysqlBackupService.class);
        when(mysqlBackupService.backup(any())).thenReturn("sql_sql");
        dbMigrationControl = new DbMigrationControl(
                flyway,
                accessService,
                dataSourceProperties,
                storagePathCoordinator,
                true
        );
        Field field = dbMigrationControl.getClass().getDeclaredField("mysqlBackupService");
        field.setAccessible(true);
        field.set(dbMigrationControl, mysqlBackupService);
    }

    @Test
    public void testNewInstanceBorn() throws Throwable {
        dbMigrationControl.onNewInstanceStatus(ServerInstanceStatus.BORN);
        ArgumentCaptor<InputStream> argument = ArgumentCaptor.forClass(InputStream.class);
        verify(accessService).put(eq("path"), argument.capture());
        Assertions.assertEquals("sql_sql", IOUtils.toString(argument.getValue(), StandardCharsets.UTF_8));
    }

    @Test
    public void testOldInstanceReadyDown() throws Throwable {
        dbMigrationControl.onOldInstanceStatus(ServerInstanceStatus.READY_DOWN);
        verify(flyway).migrate();
    }

    @Test
    public void testRollUp() throws Throwable {
        Field field = dbMigrationControl.getClass().getDeclaredField("rollUpStart");
        field.setAccessible(true);
        field.set(dbMigrationControl, Boolean.FALSE);
        dbMigrationControl.onOldInstanceStatus(ServerInstanceStatus.READY_DOWN);
        verify(flyway, times(0)).migrate();
    }

}
