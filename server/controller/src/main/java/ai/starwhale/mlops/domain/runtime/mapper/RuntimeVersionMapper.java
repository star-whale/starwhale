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

package ai.starwhale.mlops.domain.runtime.mapper;

import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionViewEntity;
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
public interface RuntimeVersionMapper {

    String COLUMNS = "runtime_version.id, version_order, runtime_id, runtime_version.owner_id,"
            + " version_name, version_tag, version_meta, storage_path, image, built_image, shared,"
            + " runtime_version.created_time, runtime_version.modified_time";

    String VERSION_VIEW_COLUMNS = "u.user_name, p.project_name, b.runtime_name, b.id as runtime_id,"
            + " v.id, v.version_order, v.version_name, v.shared, v.created_time, v.modified_time";

    @SelectProvider(value = RuntimeVersionProvider.class, method = "listSql")
    List<RuntimeVersionEntity> list(@Param("runtimeId") Long runtimeId,
            @Param("namePrefix") String namePrefix);

    @Select("select " + COLUMNS + " from runtime_version where shared = 1")
    List<RuntimeVersionEntity> listShared();

    @Select("select " + COLUMNS + " from runtime_version where id = #{id}")
    RuntimeVersionEntity find(@Param("id") Long id);

    @Select("select " + COLUMNS + " from runtime_version where id in (${ids})")
    List<RuntimeVersionEntity> findByIds(@Param("ids") String ids);

    @Select("select " + COLUMNS + " from runtime_version"
            + " where runtime_id = #{runtimeId}"
            + " order by version_order desc"
            + " limit 1")
    RuntimeVersionEntity findByLatest(@Param("runtimeId") Long runtimeId);

    @Select("select version_order from runtime_version where id = #{id} for update")
    Long selectVersionOrderForUpdate(@Param("id") Long id);

    @Select("select max(version_order) as max from runtime_version where runtime_id = #{runtimeId} for update")
    Long selectMaxVersionOrderOfRuntimeForUpdate(@Param("runtimeId") Long runtimeId);

    @Update("update runtime_version set version_order = #{versionOrder} where id = #{id}")
    int updateVersionOrder(@Param("id") Long id, @Param("versionOrder") Long versionOrder);

    @Insert("insert into runtime_version (runtime_id, owner_id, version_name, version_tag, version_meta,"
            + " storage_path, image, shared)"
            + " values (#{runtimeId}, #{ownerId}, #{versionName}, #{versionTag}, #{versionMeta},"
            + " #{storagePath}, #{image}, #{shared})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(RuntimeVersionEntity version);

    @UpdateProvider(value = RuntimeVersionProvider.class, method = "updateSql")
    int update(RuntimeVersionEntity version);

    @Update("update runtime_version set version_tag = #{tag} where id = #{id}")
    int updateTag(@Param("id") Long id, @Param("tag") String tag);

    @Update("update runtime_version set shared = #{shared} where id = #{id}")
    int updateShared(@Param("id") Long id, @Param("shared") Boolean shared);

    @SelectProvider(value = RuntimeVersionProvider.class, method = "findByNameAndRuntimeIdSql")
    RuntimeVersionEntity findByNameAndRuntimeId(@Param("versionName") String versionName,
            @Param("runtimeId") Long runtimeId);

    @Update("update runtime_version set built_image = #{builtImage} where version_name = #{versionName}")
    int updateBuiltImage(@Param("versionName") String versionName, @Param("builtImage") String builtImage);

    @Select("select " + COLUMNS + " from runtime_version"
            + " where version_order = #{versionOrder}"
            + " and runtime_id = #{runtimeId}")
    RuntimeVersionEntity findByVersionOrder(@Param("versionOrder") Long versionOrder,
            @Param("runtimeId") Long runtimeId);

    @SelectProvider(value = RuntimeVersionProvider.class, method = "findLatestByProjectIdSql")
    List<RuntimeVersionEntity> findLatestByProjectId(@Param("projectId") Long projectId, @Param("limit") Integer limit);


    @Select("select " + VERSION_VIEW_COLUMNS
            + " from runtime_version as v, runtime_info as b, project_info as p, user_info as u"
            + " where v.runtime_id = b.id"
            + " and b.project_id = p.id"
            + " and b.runtime_name != '" + Constants.SW_BUILT_IN_RUNTIME + "'"
            + " and p.owner_id = u.id"
            + " and b.deleted_time = 0"
            + " and p.is_deleted = 0"
            + " and p.id = #{projectId}"
            + " order by b.id desc, v.version_order desc")
    List<RuntimeVersionViewEntity> listRuntimeVersionViewByProject(@Param("projectId") Long projectId);

    @Select("select " + VERSION_VIEW_COLUMNS
            + " from runtime_version as v, runtime_info as b, project_info as p, user_info as u"
            + " where v.runtime_id = b.id"
            + " and b.project_id = p.id"
            + " and b.runtime_name != '" + Constants.SW_BUILT_IN_RUNTIME + "'"
            + " and p.owner_id = u.id"
            + " and b.deleted_time = 0"
            + " and p.is_deleted = 0"
            + " and p.privacy = 1"
            + " and v.shared = 1"
            + " and p.id != #{excludeProjectId}"
            + " order by b.id desc, v.version_order desc")
    List<RuntimeVersionViewEntity> listRuntimeVersionViewByShared(@Param("excludeProjectId") Long excludeProjectId);

    @Update("update runtime_version set shared = 0 where runtime_id in (select id from runtime_info where project_id = "
            + "#{projectId})")
    int unShareRuntimeVersionWithinProject(@Param("projectId") Long projectId);

    class RuntimeVersionProvider {

        public String listSql(@Param("runtimeId") Long runtimeId,
                @Param("namePrefix") String namePrefix) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("runtime_version");
                    WHERE("runtime_id = #{runtimeId}");
                    if (StrUtil.isNotEmpty(namePrefix)) {
                        WHERE("version_name like concat(#{namePrefix}, '%')");
                    }
                    ORDER_BY("version_order desc");
                }
            }.toString();
        }

        public String findByNameAndRuntimeIdSql(@Param("versionName") String versionName,
                @Param("runtimeId") Long runtimeId) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("runtime_version");
                    WHERE("version_name = #{versionName}");
                    if (Objects.nonNull(runtimeId)) {
                        WHERE("runtime_id = #{runtimeId}");
                    }
                }
            }.toString();
        }

        public String updateSql(RuntimeVersionEntity version) {
            return new SQL() {
                {
                    UPDATE("runtime_version");
                    if (StrUtil.isNotEmpty(version.getVersionTag())) {
                        SET("version_tag = #{versionTag}");
                    }
                    WHERE("where id = #{id}");
                }
            }.toString();
        }

        public String findLatestByProjectIdSql(Long projectId, Integer limit) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("runtime_version");
                    INNER_JOIN("runtime_info on runtime_info.id = runtime_version.runtime_id");
                    WHERE("runtime_info.project_id = #{projectId}");
                    ORDER_BY("runtime_version.modified_time desc");
                    if (limit != null) {
                        LIMIT(limit);
                    }
                }
            }.toString();
        }
    }
}
