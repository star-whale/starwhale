/**
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

import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ProjectManager {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private UserService userService;

    private static final Map<String, String> SORT_MAP = Map.of(
        "id", "project_id",
        "name", "project_name",
        "time", "created_time",
        "createdTime", "created_time");

    public List<ProjectEntity> listProjects(Project project, User owner, OrderParams orderParams) {
        if(owner != null && owner.getId() != null) {
            return projectMapper.listProjectsByOwner(owner.getId(), orderParams.getOrderSQL(SORT_MAP));
        } else if (owner != null && StringUtils.hasText(owner.getName())) {
            return projectMapper.listProjectsByOwnerName(owner.getName(), orderParams.getOrderSQL(SORT_MAP));
        }

        return projectMapper.listProjects(project.getName(), orderParams.getOrderSQL(SORT_MAP));

    }

    public ProjectEntity findDefaultProject() {
        Long userId = userService.currentUserDetail().getIdTableKey();
        ProjectEntity defaultProject = projectMapper.findDefaultProject(userId);
        if(defaultProject == null) {
            List<ProjectEntity> entities = projectMapper.listProjectsByOwner(userId, null);
            if(entities.isEmpty()) {
                log.error("Can not find default project by user, id = {}", userId);
                return null;
            }
            defaultProject = entities.get(0);
        }
        return defaultProject;
    }

    public ProjectEntity findByName(String projectName){
        List<ProjectEntity> projectEntities = projectMapper.listProjects(projectName, null);
        if(null != projectEntities && !projectEntities.isEmpty()){
            return projectEntities.get(0);
        }
        return findDefaultProject();
    }


}
