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

package ai.starwhale.mlops.domain.project.sort;

import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.bo.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("latest")
public class LatestSort implements Sort {

    private final ProjectMapper projectMapper;

    public LatestSort(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public List<ProjectEntity> list(String projectName, User user, boolean bShowAll) {
        String orderBy = "id desc";
        if (bShowAll) {
            return projectMapper.listAll(projectName, orderBy);
        } else {
            return projectMapper.listOfUser(projectName, user.getId(), orderBy);
        }
    }
}
