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

package ai.starwhale.mlops.domain.sft.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class ListStringTypeHandler extends BaseTypeHandler<List<Long>> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, List<Long> longs, JdbcType jdbcType)
            throws SQLException {
        try {
            preparedStatement.setString(i, objectMapper.writeValueAsString(longs));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Long> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        if (resultSet.getString(s) != null) {
            try {
                return objectMapper.readValue(resultSet.getString(s), new TypeReference<List<Long>>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public List<Long> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        if (resultSet.getString(i) != null) {
            try {
                return objectMapper.readValue(resultSet.getString(i), new TypeReference<List<Long>>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public List<Long> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        if (callableStatement.getString(i) != null) {
            try {
                return objectMapper.readValue(callableStatement.getString(i), new TypeReference<List<Long>>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
