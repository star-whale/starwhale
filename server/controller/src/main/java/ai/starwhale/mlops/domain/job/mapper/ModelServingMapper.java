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
import ai.starwhale.mlops.domain.job.status.JobStatus;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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
            "job_id",
            "owner_id",
            "created_time",
            "finished_time",
            "runtime_version_id",
            "resource_pool",
            "last_visit_time",
            "spec",
    };
    String TABLE_WRITE = "model_serving_info";
    String TABLE_SELECT = "model_serving_info t left outer join job_info as j on t.job_id=j.id";
    String COLUMNS_SELECT = "t.*, j.job_status";

    @InsertProvider(value = SqlProviderAdapter.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ModelServingEntity entity);

    @Select("select " + COLUMNS_SELECT + " from " + TABLE_SELECT + " where t.id=#{id}")
    ModelServingEntity find(long id);

    @Select("<script>"
            + " select " + COLUMNS_SELECT + " from " + TABLE_SELECT + " where j.job_status in"
            + " <foreach item='item' index='index' collection='status' open='(' separator=',' close=')'>"
            + "   #{item}"
            + " </foreach>"
            + "</script>")
    List<ModelServingEntity> findByStatusIn(JobStatus... status);

    @Update("update " + TABLE_WRITE + " set last_visit_time=#{date} where id=#{id}")
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
                    INSERT_INTO(TABLE_WRITE);
                    INTO_COLUMNS(COLUMNS);
                    INTO_VALUES(values);
                }
            }.toString();
        }

        public String listByConditions(
                @Param("projectId") Long projectId,
                @Param("modelVersionId") Long modelVersionId,
                @Param("runtimeVersionId") Long runtimeVersionId,
                @Param("resourcePool") String resourcePool
        ) {
            return new SQL() {
                {
                    SELECT("model_serving_info.id");
                    SELECT(Arrays.stream(COLUMNS)
                                   .map(s -> "model_serving_info." + s)
                                   .collect(Collectors.toList())
                                   .toArray(new String[]{}));
                    SELECT("job_info.job_status");
                    FROM(TABLE_WRITE);
                    LEFT_OUTER_JOIN("job_info on job_id=job_info.id");
                    if (projectId != null) {
                        WHERE("model_serving_info.project_id=#{projectId}");
                    }
                    if (modelVersionId != null) {
                        WHERE("model_serving_info.model_version_id=#{modelVersionId}");
                    }
                    if (runtimeVersionId != null) {
                        WHERE("model_serving_info.runtime_version_id=#{runtimeVersionId}");
                    }
                    if (resourcePool != null) {
                        WHERE("model_serving_info.resource_pool=#{resourcePool}");
                    }
                }
            }.toString();
        }
    }
}
