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

package ai.starwhale.mlops.domain.dataset.build.mapper;

import ai.starwhale.mlops.api.protobuf.Dataset.BuildStatus;
import ai.starwhale.mlops.domain.dataset.build.po.BuildRecordEntity;
import java.util.List;
import java.util.Objects;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface BuildRecordMapper {
    String COLUMNS_FOR_INSERT = "dataset_id, dataset_name, project_id, "
            + "type, status, storage_path, log_path, format, shared, created_time";
    String COLUMNS_FOR_SELECT = "id, " + COLUMNS_FOR_INSERT;

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM dataset_build_record WHERE id = #{id}")
    BuildRecordEntity selectById(Long id);

    @SelectProvider(value = SqlProvider.class, method = "listByStatus")
    List<BuildRecordEntity> selectByStatus(
            @Param("projectId") Long projectId, @Param("status") BuildStatus status);

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM dataset_build_record "
            + "WHERE project_id = #{projectId} AND dataset_name = #{datasetName} AND status = 'BUILDING' "
            + "FOR UPDATE")
    List<BuildRecordEntity> selectBuildingInOneProjectForUpdate(
            @Param("projectId") Long projectId, @Param("datasetName") String datasetName);

    @Select("SELECT " + COLUMNS_FOR_SELECT + " FROM dataset_build_record "
            + "WHERE (status = 'SUCCESS' or status = 'FAILED') AND cleaned = 0")
    List<BuildRecordEntity> selectFinishedAndUncleaned();

    @Update("UPDATE dataset_build_record set status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") BuildStatus status);

    @Update("UPDATE dataset_build_record set log_path = #{path} WHERE id = #{id}")
    int updateLogPath(@Param("id") Long id, @Param("path") String logPath);

    @Update("UPDATE dataset_build_record set cleaned = 1 WHERE id = #{id} AND cleaned = 0")
    int updateCleaned(@Param("id") Long id);

    @Insert("INSERT INTO dataset_build_record (" + COLUMNS_FOR_INSERT + ") "
            + "VALUES ("
            + "#{datasetId}, #{datasetName}, #{projectId}, "
            + "#{type}, #{status}, #{storagePath}, #{logPath}, #{format}, #{shared}, #{createdTime}"
            + ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BuildRecordEntity buildRecord);

    class SqlProvider {
        public String listByStatus(@Param("projectId") Long projectId, @Param("status") BuildStatus status) {
            return new SQL() {
                {
                    SELECT(COLUMNS_FOR_SELECT);
                    FROM("dataset_build_record");
                    if (Objects.nonNull(projectId)) {
                        WHERE("project_id = #{projectId}");
                    }
                    if (null != status) {
                        WHERE("status = #{status}");
                    }
                    ORDER_BY("id desc");
                }
            }.toString();
        }
    }
}
