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

import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCountEntity;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectManager implements ProjectAccessor {

    public static final String PROJECT_SEPERATOR = ":";

    private final ProjectMapper projectMapper;

    private final IdConvertor idConvertor;

    private static final Map<String, String> SORT_MAP = Map.of(
            "id", "project_id",
            "name", "project_name",
            "time", "project_created_time",
            "createdTime", "project_created_time");

    public ProjectManager(ProjectMapper projectMapper, IdConvertor idConvertor) {
        this.projectMapper = projectMapper;
        this.idConvertor = idConvertor;
    }

    public List<ProjectEntity> listProjects(String projectName, Long userId, OrderParams orderParams) {
        return projectMapper.listProjects(projectName, orderParams.getOrderSql(SORT_MAP), 0, userId);
    }


    public ProjectEntity findDefaultProject(Long userId) {
        ProjectEntity defaultProject = projectMapper.findDefaultProject(userId);
        if (defaultProject == null) {
            List<ProjectEntity> entities = projectMapper.listProjectsByOwner(userId, null, 0);
            if (entities.isEmpty()) {
                log.error("Can not find default project by user, id = {}", userId);
                return null;
            }
            defaultProject = entities.get(0);
        }
        return defaultProject;
    }

    public ProjectEntity findByNameOrDefault(String projectName, Long userId) {
        if (!StrUtil.isEmpty(projectName)) {
            ProjectEntity entity = projectMapper.findProjectByName(projectName);
            if (entity != null) {
                return entity;
            }
        }
        return findDefaultProject(userId);
    }

    public ProjectEntity findById(Long projectId) {
        return projectMapper.findProject(projectId);
    }

    public Boolean existProject(String projectName) {
        ProjectEntity existProject = projectMapper.findProjectByNameForUpdate(projectName);
        return existProject != null;
    }

    public Map<Long, ProjectObjectCountEntity> getObjectCountsOfProjects(List<Long> projectIds) {
        List<ProjectObjectCountEntity> counts = projectMapper.listObjectCounts(
                projectIds);
        return counts.stream()
                .collect(Collectors.toMap(ProjectObjectCountEntity::getProjectId, entity -> entity));
    }

    @Override
    public ProjectEntity getProject(@NotNull String projectUrl) {
        return findById(getProjectId(projectUrl));
    }

    public Long getProjectId(@NotNull String projectUrl) {
        ProjectEntity projectEntity;
        if (idConvertor.isId(projectUrl)) {
            Long id = idConvertor.revert(projectUrl);
            if (id == 0) {
                return id;
            }
            projectEntity = projectMapper.findProject(id);
        } else {
            if (projectUrl.contains(PROJECT_SEPERATOR)) {
                // OWNER:PROJECT
                String[] arr = projectUrl.split(PROJECT_SEPERATOR);
                if (idConvertor.isId(arr[0])) {
                    projectEntity = projectMapper.findProjectByNameAndOwnerId(arr[1], idConvertor.revert(arr[0]));
                } else {
                    projectEntity = projectMapper.findProjectByNameAndOwnerName(arr[1], arr[0]);
                }
            } else {
                projectEntity = projectMapper.findProjectByName(projectUrl);
            }
        }
        if (projectEntity == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT,
                            String.format("Unable to find project %s", projectUrl)),
                    HttpStatus.BAD_REQUEST);
        }
        return projectEntity.getId();
    }

}
