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

package ai.starwhale.mlops.backup.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Set;
import java.util.function.BiPredicate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class MysqlBackupServiceTest extends MySqlContainerHolder {

    @Test
    public void testBackupAndRestore() throws SQLException, ClassNotFoundException {
        assertThat("extract db",
                MysqlBackupService.extractDatabaseFromUrl(mySqlDB.getJdbcUrl()), is("arbitrary_dbname"));

        BiPredicate<Statement, Integer> count = (statement, integer) -> {
            ResultSet rs;
            try {
                rs = statement.executeQuery("select count(1) from test_r");
                while (rs.next()) {
                    int rowCount = rs.getInt("COUNT(1)");
                    assertThat("records count", rowCount, is(integer));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        // create table and insert some records
        try (var connection = mySqlDB.createConnection("");
                var statement = connection.createStatement()) {
            String createTestTable = "create table test_r(id int null)";
            String insertTestTable = "insert into test_r (id) values(1),(2),(3),(4),(5)";
            statement.addBatch(createTestTable);
            statement.addBatch(insertTestTable);
            var res = statement.executeBatch();
            log.debug("{} queries were executed:{}", res.length, Arrays.toString(res));

            count.test(statement, 5);
        }

        // backup
        var backupService = MysqlBackupService.builder()
                .url(mySqlDB.getJdbcUrl())
                .username(mySqlDB.getUsername())
                .password(mySqlDB.getPassword())
                .driver(mySqlDB.getDriverClassName())
                .build();
        String sql = backupService.backup(Set.of());
        assertNotNull(sql);
        log.debug("dump sql:{}", sql);

        // insert some records again
        try (var connection = mySqlDB.createConnection("");
                var statement = connection.createStatement()) {
            String insertTestTable = "insert into test_r (id) values(6),(7),(8),(9),(10)";
            statement.addBatch(insertTestTable);
            var res = statement.executeBatch();
            log.debug("{} inserts were executed:{}", res.length, Arrays.toString(res));
            assertThat("insert records", res[0], is(5));

            count.test(statement, 10);
        }

        // restore
        assertThat("restore", backupService.restore(sql), is(true));

        try (var connection = mySqlDB.createConnection("");
                var statement = connection.createStatement()) {
            count.test(statement, 5);
        }
    }
}
