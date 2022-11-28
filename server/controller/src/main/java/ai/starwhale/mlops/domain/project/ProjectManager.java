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
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCounts;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectManager implements ProjectAccessor {

    public static final String PROJECT_SEPARATOR = ":";

    private final ProjectMapper projectMapper;

    private final IdConverter idConvertor;

    private static final Map<String, String> SORT_MAP = Map.of(
            "id", "project_id",
            "name", "project_name",
            "time", "project_created_time",
            "createdTime", "project_created_time");

    public ProjectManager(ProjectMapper projectMapper, IdConverter idConvertor) {
        this.projectMapper = projectMapper;
        this.idConvertor = idConvertor;
    }

    public List<ProjectEntity> listProjects(String projectName, Long userId, OrderParams orderParams) {
        return projectMapper.list(projectName, userId, orderParams.getOrderSql(SORT_MAP));
    }

    public ProjectEntity findById(Long projectId) {
        if (projectId == 0) {
            return ProjectEntity.builder()
                    .id(0L)
                    .projectName("SYSTEM")
                    .projectDescription("System")
                    .privacy(1)
                    .ownerId(0L)
                    .build();
        }
        return projectMapper.find(projectId);
    }

    public Boolean existProject(String projectName, Long userId) {
        ProjectEntity existProject = projectMapper.findByNameForUpdateAndOwner(projectName, userId);
        return existProject != null;
    }

    public Map<Long, ProjectObjectCounts> getObjectCountsOfProjects(List<Long> projectIds) {
        String ids = Joiner.on(",").join(projectIds);
        Map<Long, ProjectObjectCounts> map = Maps.newHashMap();
        for (Long projectId : projectIds) {
            map.put(projectId, new ProjectObjectCounts());
        }

        List<ObjectCountEntity> modelCounts = projectMapper.countModel(ids);
        setCounts(modelCounts, map, ProjectObjectCounts::setCountModel);

        List<ObjectCountEntity> datasetCounts = projectMapper.countDataset(ids);
        setCounts(datasetCounts, map, ProjectObjectCounts::setCountDataset);

        List<ObjectCountEntity> runtimeCounts = projectMapper.countRuntime(ids);
        setCounts(runtimeCounts, map, ProjectObjectCounts::setCountRuntime);

        List<ObjectCountEntity> jobCounts = projectMapper.countJob(ids);
        setCounts(jobCounts, map, ProjectObjectCounts::setCountJob);

        List<ObjectCountEntity> memberCounts = projectMapper.countMember(ids);
        setCounts(memberCounts, map, ProjectObjectCounts::setCountMember);

        return map;
    }

    private void setCounts(List<ObjectCountEntity> list, Map<Long, ProjectObjectCounts> map, CountSetter setter) {
        for (ObjectCountEntity entity : list) {
            if (map.containsKey(entity.getProjectId())) {
                setter.set(map.get(entity.getProjectId()), entity.getCount());
            }
        }
    }

    interface CountSetter {

        void set(ProjectObjectCounts obj, Integer count);
    }

    @Override
    public ProjectEntity getProject(@NotNull String projectUrl) {
        return findById(getProjectId(projectUrl));
    }

    public String[] splitProjectUrl(String projectUrl) {
        return projectUrl.split(PROJECT_SEPARATOR);
    }

    public Long getProjectId(@NotNull String projectUrl) {
        ProjectEntity projectEntity = null;
        if (idConvertor.isId(projectUrl)) {
            Long id = idConvertor.revert(projectUrl);
            if (id == 0) {
                return id;
            }
            projectEntity = projectMapper.find(id);
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
                if (byName.size() > 0) {
                    projectEntity = byName.get(0);
                }
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
