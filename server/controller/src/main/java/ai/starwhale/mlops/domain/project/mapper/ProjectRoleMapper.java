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
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProjectRoleMapper {

    List<ProjectRoleEntity> listSystemRoles();

    List<ProjectRoleEntity> listUserRoles(@NotNull @Param("userId") Long userId, @Param("projectId") Long projectId);

    List<ProjectRoleEntity> listProjectRoles(@NotNull @Param("projectId") Long projectId);

    int addProjectRole(@NotNull @Param("projectRole") ProjectRoleEntity projectRole);

    int addProjectRoleByName(@NotNull @Param("userId") Long userId,
            @NotNull @Param("projectId") Long projectId,
            @NotNull @Param("roleName") String roleName);

    int deleteProjectRole(@NotNull @Param("projectRoleId") Long projectRoleId);

    int updateProjectRole(@NotNull @Param("projectRole") ProjectRoleEntity projectRole);

}
