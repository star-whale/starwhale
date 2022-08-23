/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.domain.project.mapper;

import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCountEntity;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Param;

public interface ProjectMapper {

    int createProject(@Param("project")ProjectEntity project);

    int deleteProject(@Param("id") Long id);

    int recoverProject(@Param("id") Long id);

    List<ProjectEntity> listProjects(@Param("projectName") String projectName, @Param("order") String order, @Param("isDeleted") Integer isDeleted, @Param("userId") Long userId);

    List<ProjectEntity> listProjectsByOwner(@Param("userId") Long userId, @Param("order") String order, @Param("isDeleted") Integer isDeleted);

    List<ProjectEntity> listProjectsByOwnerName(@Param("userName") String userName, @Param("order") String order, @Param("isDeleted") Integer isDeleted);

    ProjectEntity findProject(@Param("id") Long id);

    ProjectEntity findProjectByName(@NotNull @Param("projectName")String projectName);

    ProjectEntity findDefaultProject(@Param("userId") Long userId);

    int modifyProject(@Param("project")ProjectEntity project);

    List<ProjectObjectCountEntity> listObjectCounts(@Param("projectIds")List<Long> projectIds);
}
