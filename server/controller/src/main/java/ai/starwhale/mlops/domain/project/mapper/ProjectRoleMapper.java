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

import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectRoleMapper {

    String COLUMNS = "id,user_id,role_id,project_id,created_time,modified_time";

    @Insert("replace into user_role_rel (user_id, role_id, project_id)"
            + " values (#{userId}, #{roleId}, #{projectId})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(@NotNull ProjectRoleEntity projectRole);

    @Insert("replace into user_role_rel (user_id, role_id, project_id)"
            + " values (#{userId}, (select id from user_role_info where role_name=#{roleName}), #{projectId})")
    int insertByRoleName(@NotNull @Param("userId") Long userId,
            @NotNull @Param("projectId") Long projectId,
            @NotNull @Param("roleName") String roleName);

    @Delete("delete from user_role_rel where id = #{id}")
    int delete(@NotNull @Param("id") Long id);

    @Update("update user_role_rel"
            + " set role_id = #{roleId}"
            + " where id = #{id}")
    int updateRole(@NotNull @Param("id") Long id, @NotNull @Param("roleId") Long roleId);

    @Select("select " + COLUMNS + " from user_role_rel"
            + " where user_id = #{userId}"
            + " and project_id = #{projectId}")
    ProjectRoleEntity findByUserAndProject(@NotNull @Param("userId") Long userId,
            @NotNull @Param("projectId") Long projectId);

    @Select("select " + COLUMNS + " from user_role_rel"
            + " where project_id = #{projectId}")
    List<ProjectRoleEntity> listByProject(@NotNull @Param("projectId") Long projectId);

    @Select("select " + COLUMNS + " from user_role_rel"
            + " where user_id = #{userId}")
    List<ProjectRoleEntity> listByUser(@NotNull @Param("userId") Long userId);

}
