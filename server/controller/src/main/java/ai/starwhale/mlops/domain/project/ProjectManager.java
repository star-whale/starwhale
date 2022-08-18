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

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ProjectManager implements ProjectAccessor{



    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private IDConvertor idConvertor;

    private static final Map<String, String> SORT_MAP = Map.of(
        "id", "project_id",
        "name", "project_name",
        "time", "project_created_time",
        "createdTime", "project_created_time");

    public List<ProjectEntity> listProjects(String projectName, Long userId, OrderParams orderParams) {
        return projectMapper.listProjects(projectName, orderParams.getOrderSQL(SORT_MAP), 0, userId);
    }


    public ProjectEntity findDefaultProject(Long userId) {
        ProjectEntity defaultProject = projectMapper.findDefaultProject(userId);
        if(defaultProject == null) {
            List<ProjectEntity> entities = projectMapper.listProjectsByOwner(userId, null, 0);
            if(entities.isEmpty()) {
                log.error("Can not find default project by user, id = {}", userId);
                return null;
            }
            defaultProject = entities.get(0);
        }
        return defaultProject;
    }

    public ProjectEntity findByNameOrDefault(String projectName, Long userId){
        if(!StrUtil.isEmpty(projectName)) {
            ProjectEntity entity = projectMapper.findProjectByName(projectName);
            if(entity != null) {
                return entity;
            }
        }
        return findDefaultProject(userId);
    }

    public Boolean existProject(String projectName) {
        ProjectEntity existProject = projectMapper.findProjectByName(projectName);
        return existProject != null;
    }

    @Override
    public Long getProjectId(@NotNull String projectUrl) {
        if(idConvertor.isID(projectUrl)) {
            return idConvertor.revert(projectUrl);
        }
        ProjectEntity projectEntity = projectMapper.findProjectByName(projectUrl);
        if(projectEntity == null) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.PROJECT)
                .tip(String.format("Unable to find project %s", projectUrl)), HttpStatus.BAD_REQUEST);
        }
        return projectEntity.getId();
    }

}
