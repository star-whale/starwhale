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

package ai.starwhale.mlops.domain;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class MySqlContainerHolder {

    public static MySQLContainer<?> mySqlDB;

    static {
        mySqlDB = new MySQLContainer<>(
                DockerImageName.parse("docker.io/bitnami/mysql:8.0.29-debian-10-r2").asCompatibleSubstituteFor("mysql"))
                .withDatabaseName("arbitrary_dbname")
                .withUrlParam("useUnicode", "true")
                .withUrlParam("characterEncoding", "UTF-8")
                .withUrlParam("createDatabaseIfNotExist", "true")
                .withUrlParam("allowMultiQueries", "true")
                .withUrlParam("useJDBCCompliantTimezoneShift", "true")
                .withUrlParam("serverTimezone", "UTC")
                .withUrlParam("useLegacyDatetimeCode", "false")
                .withUrlParam("sessionVariables", "time_zone='%2B00:00'")
                .withUsername("arbitrary_username")
                .withPassword("arbitrary_pwd");

        mySqlDB.start();
    }


    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySqlDB::getJdbcUrl);
        registry.add("spring.datasource.username", mySqlDB::getUsername);
        registry.add("spring.datasource.password", mySqlDB::getPassword);

    }

}
