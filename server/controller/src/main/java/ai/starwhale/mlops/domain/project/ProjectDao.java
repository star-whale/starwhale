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

package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectDao implements ProjectAccessor {

    public static final String PROJECT_SEPARATOR = ":";
    private final ProjectMapper projectMapper;
    private final IdConverter idConvertor;


    public ProjectDao(ProjectMapper projectMapper, IdConverter idConvertor) {
        this.projectMapper = projectMapper;
        this.idConvertor = idConvertor;
    }

    public ProjectEntity findById(Long projectId) {
        if (projectId == 0) {
            return ProjectEntity.builder()
                    .id(0L)
                    .projectName("SYSTEM")
                    .projectDescription("System")
                    .privacy(1)
                    .isDefault(0)
                    .isDeleted(0)
                    .build();
        }
        return projectMapper.find(projectId);
    }

    public ProjectEntity getProject(@NotNull String projectUrl) {
        ProjectEntity projectEntity = null;
        if (idConvertor.isId(projectUrl)) {
            Long id = idConvertor.revert(projectUrl);
            projectEntity = findById(id);
        } else {
            String[] arr = splitProjectUrl(projectUrl);
            if (arr.length > 1) {
                // OWNER:PROJECT
                if (idConvertor.isId(arr[0])) {
                    projectEntity = projectMapper.findExistingByNameAndOwner(arr[1], idConvertor.revert(arr[0]));
                } else {
                    projectEntity = projectMapper.findExistingByNameAndOwnerName(arr[1], arr[0]);
                }
            } else {
                List<ProjectEntity> byName = projectMapper.findByName(projectUrl);
                if (byName != null) {
                    if (byName.size() == 1) {
                        projectEntity = byName.get(0);
                    } else {
                        throw new SwNotFoundException(ResourceType.PROJECT,
                                String.format(
                                        "Unable to find project %s, "
                                                + "you may use OWNER:PROJECT or project id to access the project",
                                        projectUrl));
                    }
                }
            }
        }
        if (projectEntity == null) {
            throw new SwNotFoundException(ResourceType.PROJECT,
                    String.format("Unable to find project %s", projectUrl));
        }

        return projectEntity;
    }

    public Long getProjectId(@NotNull String projectUrl) {
        return getProject(projectUrl).getId();
    }

    static String[] splitProjectUrl(String projectUrl) {
        return projectUrl.split(PROJECT_SEPARATOR);
    }
}
