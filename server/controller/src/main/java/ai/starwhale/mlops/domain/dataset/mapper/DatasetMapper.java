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

package ai.starwhale.mlops.domain.dataset.mapper;

import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import cn.hutool.core.util.StrUtil;
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
public interface DatasetMapper {

    String COLUMNS = "id, dataset_name, project_id, owner_id, is_deleted, created_time, modified_time";

    @SelectProvider(value = DatasetProvider.class, method = "listSql")
    List<DatasetEntity> list(@Param("projectId") Long projectId,
            @Param("namePrefix") String namePrefix,
            @Param("ownerId") Long ownerId,
            @Param("order") String order);

    @Insert("insert into dataset_info(dataset_name, project_id, owner_id)"
            + " values(#{datasetName}, #{projectId}, #{ownerId})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(DatasetEntity dataset);

    @Update("update dataset_info set is_deleted = 1 where id = #{id}")
    int remove(@Param("id") Long id);

    @Update("update dataset_info set is_deleted = 0 where id = #{id}")
    int recover(@Param("id") Long id);

    @Select("select " + COLUMNS + " from dataset_info where id = #{id}")
    DatasetEntity find(@Param("id") Long id);

    @SelectProvider(value = DatasetProvider.class, method = "findByNameSql")
    DatasetEntity findByName(@Param("name") String name, @Param("projectId") Long projectId,
            @Param("forUpdate") Boolean forUpdate);

    @Select("select " + COLUMNS + " from dataset_info where is_deleted = 1 and id = #{id}")
    DatasetEntity findDeleted(@Param("id") Long id);

    class DatasetProvider {

        public String listSql(@Param("projectId") Long projectId,
                @Param("namePrefix") String namePrefix,
                @Param("ownerId") Long ownerId,
                @Param("order") String order) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("dataset_info");
                    WHERE("is_deleted = 0");
                    if (Objects.nonNull(projectId)) {
                        WHERE("project_id = #{projectId}");
                    }
                    if (StrUtil.isNotEmpty(namePrefix)) {
                        WHERE("dataset_name like concat(#{namePrefix}, '%')");
                    }
                    if (Objects.nonNull(ownerId)) {
                        WHERE("owner_id = #{ownerId}");
                    }
                    if (StrUtil.isNotEmpty(order)) {
                        ORDER_BY(order);
                    } else {
                        ORDER_BY("id desc");
                    }
                }
            }.toString();
        }

        public String findByNameSql(@Param("name") String name,
                @Param("projectId") Long projectId,
                @Param("forUpdate") Boolean forUpdate) {
            String sql = new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("dataset_info");
                    WHERE("dataset_name = #{name}");
                    WHERE("is_deleted = 0");
                    if (Objects.nonNull(projectId)) {
                        WHERE("project_id = #{projectId}");
                    }
                }
            }.toString();
            return Objects.equals(forUpdate, true) ? (sql + " for update") : sql;
        }
    }

}
