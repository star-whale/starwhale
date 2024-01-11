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

package ai.starwhale.mlops.domain.project.mapper;

import ai.starwhale.mlops.domain.project.po.ObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
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
public interface ProjectMapper {

    String COLUMNS = "id,project_name,owner_id,privacy,project_description,"
            + "is_default,is_deleted,created_time,modified_time";

    @Insert("insert into project_info(project_name, owner_id, privacy, project_description, readme, is_default)"
            + " values (#{projectName}, #{ownerId}, #{privacy}, #{projectDescription}, #{readme}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(@NotNull ProjectEntity project);

    @Update("update project_info set is_deleted = 1"
            + " where id = #{id}")
    int remove(@Param("id") Long id);

    @Update("update project_info set is_deleted = 0"
            + " where id = #{id}")
    int recover(@Param("id") Long id);


    @UpdateProvider(value = ProjectProvider.class, method = "updateSql")
    int update(@NotNull ProjectEntity project);

    @Select("select " + COLUMNS + " from project_info"
            + " where id = #{id}")
    ProjectEntity find(@Param("id") Long id);

    @Select("select readme from project_info where id = #{id}")
    String getReadme(@Param("id") Long id);

    @Select("select " + COLUMNS + " from project_info"
            + " where project_name = #{projectName}")
    List<ProjectEntity> findByName(@Param("projectName") String projectName);

    @Select("select " + COLUMNS + " from project_info"
            + " where project_name = #{projectName}"
            + " and owner_id = #{ownerId}")
    ProjectEntity findExistingByNameAndOwner(@NotNull @Param("projectName") String projectName,
            @NotNull @Param("ownerId") Long ownerId);

    @Select("select " + COLUMNS + " from project_info"
            + " where project_name = #{projectName}"
            + " and owner_id = (select id from user_info where user_name = #{ownerName})")
    ProjectEntity findExistingByNameAndOwnerName(@NotNull @Param("projectName") String projectName,
            @NotNull @Param("ownerName") String ownerName);

    @Select("select " + COLUMNS + " from project_info"
            + " where project_name = #{projectName}"
            + " and owner_id = #{ownerId}"
            + " for update")
    ProjectEntity findByNameForUpdateAndOwner(@NotNull @Param("projectName") String projectName,
            @NotNull @Param("ownerId") Long ownerId);

    @SelectProvider(value = ProjectProvider.class, method = "listSql")
    List<ProjectEntity> listOfUser(@Param("projectName") String projectName,
            @Param("userId") Long userId,
            @Param("order") String order);

    @SelectProvider(value = ProjectProvider.class, method = "listAllSql")
    List<ProjectEntity> listAll(@Param("projectName") String projectName,
            @Param("order") String order);

    @SelectProvider(value = ProjectProvider.class, method = "listRemovedSql")
    List<ProjectEntity> listRemovedProjects(@Param("projectName") String projectName,
            @Param("ownerId") Long ownerId);

    @Select("select count(*) as count, project_id from model_info"
            + " where project_id in (${projectIds}) and deleted_time = 0 group by project_id")
    List<ObjectCountEntity> countModel(@Param("projectIds") String projectIds);

    @Select("select count(*) as count, project_id from dataset_info"
            + " where project_id in (${projectIds}) and deleted_time = 0 group by project_id")
    List<ObjectCountEntity> countDataset(@Param("projectIds") String projectIds);

    @Select("select count(*) as count, project_id from runtime_info"
            + " where project_id in (${projectIds}) and deleted_time = 0 group by project_id")
    List<ObjectCountEntity> countRuntime(@Param("projectIds") String projectIds);

    @Select("select count(*) as count, project_id from job_info"
            + " where project_id in (${projectIds}) group by project_id")
    List<ObjectCountEntity> countJob(@Param("projectIds") String projectIds);

    @Select("select count(*) as count, project_id from user_role_rel"
            + " where project_id in (${projectIds}) group by project_id")
    List<ObjectCountEntity> countMember(@Param("projectIds") String projectIds);

    class ProjectProvider {

        public String updateSql(ProjectEntity project) {
            return new SQL() {
                {
                    UPDATE("project_info");
                    if (StrUtil.isNotEmpty(project.getProjectName())) {
                        SET("project_name = #{projectName}");
                    }
                    if (Objects.nonNull(project.getProjectDescription())) {
                        SET("project_description = #{projectDescription}");
                    }
                    if (Objects.nonNull(project.getReadme())) {
                        SET("readme = #{readme}");
                    }
                    if (Objects.nonNull(project.getOwnerId())) {
                        SET("owner_id = #{ownerId}");
                    }
                    if (Objects.nonNull(project.getPrivacy())) {
                        SET("privacy = #{privacy}");
                    }
                    if (Objects.nonNull(project.getIsDefault())) {
                        SET("is_default = #{isDefault}");
                    }
                    WHERE("id = #{id}");
                }
            }.toString();
        }

        public String listSql(@NotNull @Param("userId") Long userId,
                @Param("projectName") String projectName,
                @Param("order") String order) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("project_info");
                    WHERE("is_deleted = 0");
                    if (userId != null) {
                        WHERE("(privacy = 1"
                                + " OR"
                                + " (id in (select project_id from user_role_rel where user_id = #{userId})))");
                    }
                    if (StrUtil.isNotEmpty(projectName)) {
                        WHERE("project_name like concat(#{projectName}, '%')");
                    }
                    if (StrUtil.isNotEmpty(order)) {
                        ORDER_BY(order);
                    } else {
                        ORDER_BY("id desc");
                    }
                }
            }.toString();
        }

        public String listAllSql(
                @Param("projectName") String projectName,
                @Param("order") String order) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("project_info");
                    WHERE("is_deleted = 0");
                    if (StrUtil.isNotEmpty(projectName)) {
                        WHERE("project_name like concat(#{projectName}, '%')");
                    }
                    if (StrUtil.isNotEmpty(order)) {
                        ORDER_BY(order);
                    } else {
                        ORDER_BY("id desc");
                    }
                }
            }.toString();
        }

        public String listRemovedSql(@Param("projectName") String projectName, @Param("ownerId") Long ownerId) {
            return new SQL() {
                {
                    SELECT(COLUMNS);
                    FROM("project_info");
                    WHERE("is_deleted = 1");
                    if (StrUtil.isNotEmpty(projectName)) {
                        WHERE("project_name like concat(#{projectName}, '%')");
                    }
                }
            }.toString();
        }
    }
}
