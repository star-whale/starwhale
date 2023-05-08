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

import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.model.po.ModelVersionViewEntity;
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
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface ModelVersionMapper {

    String COLUMNS = "id, version_order, model_id, owner_id, version_name, version_tag, version_meta,"
            + " storage_path, created_time, modified_time, jobs, status, shared";

    String VERSION_VIEW_COLUMNS = "u.user_name, p.project_name, m.model_name, m.id as model_id,"
            + " v.id, v.version_order, v.version_name, v.jobs, v.shared, v.storage_path,"
            + " v.created_time, v.modified_time";

    @SelectProvider(value = ModelVersionProvider.class, method = "listSql")
    List<ModelVersionEntity> list(@Param("modelId") Long modelId,
            @Param("namePrefix") String namePrefix,
            @Param("tag") String tag);

    @Select("select " + COLUMNS + " from model_version where id = #{id}")
    ModelVersionEntity find(@Param("id") Long id);

    @Select("select " + COLUMNS + " from model_version where id in (${ids})")
    List<ModelVersionEntity> findByIds(@Param("ids") String ids);

    @Select("select " + COLUMNS + " from model_version"
            + " where model_id = #{modelId}"
            + " order by version_order desc"
            + " limit 1")
    ModelVersionEntity findByLatest(@Param("modelId") Long modelId);

    @Update("update model_version set shared = #{shared} where id = #{id}")
    int updateShared(@Param("id") Long id, @Param("shared") Boolean shared);

    @Select("select " + VERSION_VIEW_COLUMNS
            + " from model_info as m, model_version as v, project_info as p, user_info as u"
            + " where v.model_id = m.id"
            + " and m.project_id = p.id"
            + " and p.owner_id = u.id"
            + " and m.is_deleted = 0"
            + " and p.is_deleted = 0"
            + " and p.id = #{projectId}"
            + " order by m.id desc, v.version_order desc")
    List<ModelVersionViewEntity> listModelVersionViewByProject(@Param("projectId") Long projectId);

    @Select("select " + VERSION_VIEW_COLUMNS
            + " from model_info as m, model_version as v, project_info as p, user_info as u"
            + " where v.model_id = m.id"
            + " and m.project_id = p.id"
            + " and p.owner_id = u.id"
            + " and p.is_deleted = 0"
            + " and m.is_deleted = 0"
            + " and p.privacy = 1"
            + " and v.shared = 1"
            + " and p.id != #{excludeProjectId}"
            + " order by m.id desc, v.version_order desc")
    List<ModelVersionViewEntity> listModelVersionViewByShared(@Param("excludeProjectId") Long excludeProjectId);


    @Select("select version_order from model_version where id = #{id} for update")
    Long selectVersionOrderForUpdate(@Param("id") Long id);

    @Select("select max(version_order) as max from model_version where model_id = #{modelId} for update")
    Long selectMaxVersionOrderOfModelForUpdate(@Param("modelId") Long modelId);

    @Update("update model_version set version_order = #{versionOrder} where id = #{id}")
    int updateVersionOrder(@Param("id") Long id, @Param("versionOrder") Long versionOrder);

    @Insert("insert into model_version (model_id, owner_id, version_name, version_tag, version_meta,"
            + " storage_path, jobs)"
            + " values (#{modelId}, #{ownerId}, #{versionName}, #{versionTag}, #{versionMeta},"
            + " #{storagePath}, #{jobs})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(ModelVersionEntity version);

    @Update("update model_version set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @UpdateProvider(value = ModelVersionProvider.class, method = "updateSql")
    int update(ModelVersionEntity version);

    @Update("update model_version set version_tag = #{tag} where id = #{id}")
    int updateTag(@Param("id") Long id, @Param("tag") String tag);

    @SelectProvider(value = ModelVersionProvider.class, method = "findByNameAndModelIdSql")
    ModelVersionEntity findByNameAndModelId(@Param("versionName") String versionName, @Param("modelId") Long id);

    @Select("select " + COLUMNS + " from model_version"
            + " where version_order = #{versionOrder}"
            + " and model_id = #{modelId}")
    ModelVersionEntity findByVersionOrder(@Param("versionOrder") Long versionOrder,
            @Param("modelId") Long modelId);

    class ModelVersionProvider {

        public String listSql(@Param("modelId") Long modelId,
                @Param("namePrefix") String namePrefix,
                @Param("tag") String tag) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("model_version");
                    WHERE("model_id = #{modelId}");
                    if (StrUtil.isNotEmpty(namePrefix)) {
                        WHERE("version_name like concat(#{namePrefix}, '%')");
                    }
                    if (StrUtil.isNotEmpty(tag)) {
                        WHERE("FIND_IN_SET(#{tag}, version_tag)");
                    }
                    ORDER_BY("version_order desc");
                }
            }.toString();
        }

        public String findByNameAndModelIdSql(@Param("versionName") String versionName,
                @Param("modelId") Long modelId) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("model_version");
                    WHERE("version_name = #{versionName}");
                    if (Objects.nonNull(modelId)) {
                        WHERE("model_id = #{modelId}");
                    }
                }
            }.toString();
        }

        public String updateSql(ModelVersionEntity version) {
            return new SQL() {
                {
                    UPDATE("model_version");
                    if (StrUtil.isNotEmpty(version.getVersionTag())) {
                        SET("version_tag = #{versionTag}");
                    }
                    if (StrUtil.isNotEmpty(version.getJobs())) {
                        SET("jobs=#{jobs}");
                    }
                    if (Objects.nonNull(version.getStatus())) {
                        SET("status=#{status}");
                    }
                    WHERE("id = #{id}");
                }
            }.toString();
        }
    }
}
