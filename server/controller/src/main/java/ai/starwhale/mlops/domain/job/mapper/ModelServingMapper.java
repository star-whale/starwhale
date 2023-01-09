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

package ai.starwhale.mlops.domain.job.mapper;

import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.text.CaseUtils;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface ModelServingMapper {
    String[] COLUMNS = {
            "project_id",
            "model_version_id",
            "owner_id",
            "created_time",
            "finished_time",
            "job_status",
            "runtime_version_id",
            "resource_pool",
            "last_visit_time",
    };
    String TABLE = "model_serving_info";

    @InsertProvider(value = SqlProviderAdapter.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ModelServingEntity entity);

    @InsertProvider(value = SqlProviderAdapter.class, method = "insertIgnore")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertIgnore(ModelServingEntity entity);

    @Select("select * from " + TABLE + " where id=#{id}")
    ModelServingEntity find(long id);

    @Update("update " + TABLE + " set last_visit_time=#{date} where id=#{id}")
    void updateLastVisitTime(long id, Date date);

    @SelectProvider(value = SqlProviderAdapter.class, method = "listByConditions")
    List<ModelServingEntity> list(
            @Param("projectId") Long projectId,
            @Param("modelVersionId") Long modelVersionId,
            @Param("runtimeVersionId") Long runtimeVersionId,
            @Param("resourcePool") String resourcePool
    );

    class SqlProviderAdapter {
        public String insert() {
            var values = Arrays.stream(COLUMNS)
                    .map(i -> "#{" + CaseUtils.toCamelCase(i, false, '_') + "}")
                    .toArray(String[]::new);
            return new SQL() {
                {
                    INSERT_INTO(TABLE);
                    INTO_COLUMNS(COLUMNS);
                    INTO_VALUES(values);
                }
            }.toString();
        }

        public String insertIgnore() {
            var values = Arrays.stream(COLUMNS)
                    .map(i -> "#{" + CaseUtils.toCamelCase(i, false, '_') + "}")
                    .toArray(String[]::new);
            return String.format("insert ignore into %s(%s) values (%s)",
                    TABLE, String.join(", ", COLUMNS), String.join(", ", values));
        }

        public String listByConditions(
                @Param("projectId") Long projectId,
                @Param("modelVersionId") Long modelVersionId,
                @Param("runtimeVersionId") Long runtimeVersionId,
                @Param("resourcePool") String resourcePool
        ) {
            return new SQL() {
                {
                    SELECT("id");
                    SELECT(COLUMNS);
                    FROM(TABLE);
                    if (projectId != null) {
                        WHERE("project_id=#{projectId}");
                    }
                    if (modelVersionId != null) {
                        WHERE("model_version_id=#{modelVersionId}");
                    }
                    if (runtimeVersionId != null) {
                        WHERE("runtime_version_id=#{runtimeVersionId}");
                    }
                    if (resourcePool != null) {
                        WHERE("resource_pool=#{resourcePool}");
                    }
                }
            }.toString();
        }
    }
}
