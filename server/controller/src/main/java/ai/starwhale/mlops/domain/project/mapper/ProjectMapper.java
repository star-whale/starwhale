/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.project.mapper;

import ai.starwhale.mlops.domain.project.ProjectEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectMapper {

    int createProject(ProjectEntity project);

    int deleteProject(@Param("id") Long id);

    List<ProjectEntity> listProjects(@Param("projectName") String projectName);

    List<ProjectEntity> listProjectsByOwner(@Param("userId") Long userId);

    ProjectEntity findProject(@Param("id") Long id);

    ProjectEntity findDefaultProject(@Param("userId") Long userId);

    int modifyProject(ProjectEntity project);
}
