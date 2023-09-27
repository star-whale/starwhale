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

package ai.starwhale.mlops.domain.run.mapper;

import ai.starwhale.mlops.domain.run.bo.RunSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class RunSpecConvertor extends BaseTypeHandler<RunSpec> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, RunSpec runSpec, JdbcType jdbcType)
            throws SQLException {
        try {
            preparedStatement.setString(i, objectMapper.writeValueAsString(runSpec));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RunSpec getNullableResult(ResultSet resultSet, String s) throws SQLException {
        if (resultSet.getString(s) != null) {
            try {
                return objectMapper.readValue(resultSet.getString(s), RunSpec.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public RunSpec getNullableResult(ResultSet resultSet, int i) throws SQLException {
        if (resultSet.getString(i) != null) {
            try {
                return objectMapper.readValue(resultSet.getString(i), RunSpec.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public RunSpec getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        if (callableStatement.getString(i) != null) {
            try {
                return objectMapper.readValue(callableStatement.getString(i), RunSpec.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
