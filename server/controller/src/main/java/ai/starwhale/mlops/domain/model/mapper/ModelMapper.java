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

package ai.starwhale.mlops.domain.model.mapper;

import ai.starwhale.mlops.domain.model.po.ModelEntity;
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
public interface ModelMapper {

    String COLUMNS = "id, model_name, project_id, owner_id, is_deleted, created_time, modified_time";

    @SelectProvider(value = ModelProvider.class, method = "listSql")
    List<ModelEntity> list(@Param("projectId") Long projectId,
            @Param("namePrefix") String namePrefix,
            @Param("ownerId") Long ownerId,
            @Param("order") String order);

    @Insert("insert into model_info(model_name, project_id, owner_id)"
            + " values(#{modelName}, #{projectId}, #{ownerId})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    long insert(ModelEntity entity);

    @Update("update model_info set is_deleted = 1 where id = #{id}")
    int remove(@Param("id") Long id);

    @Update("update model_info set is_deleted = 0 where id = #{id}")
    int recover(@Param("id") Long id);

    @Select("select " + COLUMNS + " from model_info where id = #{id}")
    ModelEntity find(@Param("id") Long id);

    @Select("select " + COLUMNS + " from model_info where model_name = #{name}")
    ModelEntity findByNameOnly(@Param("name") String name);

    @Select("select " + COLUMNS + " from model_info where id in (${ids})")
    List<ModelEntity> findByIds(@Param("ids") String ids);

    @SelectProvider(value = ModelProvider.class, method = "findByNameSql")
    ModelEntity findByName(@Param("name") String name,
            @Param("projectId") Long projectId,
            @Param("forUpdate") Boolean forUpdate);

    @Select("select " + COLUMNS + " from model_info where id = #{id} and is_deleted = 1")
    ModelEntity findDeleted(@Param("id") Long id);


    class ModelProvider {

        public String listSql(@Param("projectId") Long projectId,
                @Param("namePrefix") String namePrefix,
                @Param("ownerId") Long ownerId,
                @Param("order") String order) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("model_info");
                    WHERE("is_deleted = 0");
                    if (Objects.nonNull(projectId)) {
                        WHERE("project_id = #{projectId}");
                    }
                    if (StrUtil.isNotEmpty(namePrefix)) {
                        WHERE("model_name like concat(#{namePrefix}, '%')");
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
                    FROM("model_info");
                    WHERE("model_name = #{name}");
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
