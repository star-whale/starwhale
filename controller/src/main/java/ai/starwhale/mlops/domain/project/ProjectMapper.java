/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.project;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface ProjectMapper {

    Long createProject(@NotNull ProjectEntity project);

    int deleteProject(@NotNull Long id);

    List<ProjectEntity> listProjects(String projectName);

    ProjectEntity findProject(@NotNull Long id);

    int modifyProject(@NotNull ProjectEntity project);
}
