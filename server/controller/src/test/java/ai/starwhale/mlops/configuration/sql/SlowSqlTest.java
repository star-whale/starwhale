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

package ai.starwhale.mlops.configuration.sql;

import ai.starwhale.mlops.configuration.SlowSqlLogInterceptor;
import java.util.Map;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SlowSqlTest {

    @Test
    public void sqlFormatTest() {
        var interceptor = new SlowSqlLogInterceptor(100, 200);

        var configuration = new Configuration();
        configuration.addMapper(SqlMapper.class);

        var statement = configuration.getMappedStatement("ai.starwhale.mlops.configuration.sql.SqlMapper.insert");
        var boundSql = statement.getBoundSql("sw");
        Assertions.assertEquals("INSERT INTO sw_user (name) VALUES ('sw')",
                interceptor.formatSql(boundSql, configuration));

        statement = configuration.getMappedStatement("ai.starwhale.mlops.configuration.sql.SqlMapper.selectById");
        boundSql = statement.getBoundSql(Map.of("id", 1, "name", "sw"));
        Assertions.assertEquals("SELECT name FROM sw_user where id = 1 and name = 'sw'",
                interceptor.formatSql(boundSql, configuration));

        statement = configuration.getMappedStatement("ai.starwhale.mlops.configuration.sql.SqlMapper.selectAll");
        boundSql = statement.getBoundSql(null);
        Assertions.assertEquals("SELECT name FROM sw_user", interceptor.formatSql(boundSql, configuration));
    }

}
