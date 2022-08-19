package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.domain.project.po.ProjectEntity;

public interface ProjectAccessor {

    ProjectEntity getProject(String projectUrl);
}
