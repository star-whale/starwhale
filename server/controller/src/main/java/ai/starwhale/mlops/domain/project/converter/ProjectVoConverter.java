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

package ai.starwhale.mlops.domain.project.converter;

import ai.starwhale.mlops.api.protobuf.Project.ProjectVo;
import ai.starwhale.mlops.api.protobuf.Project.StatisticsVo;
import ai.starwhale.mlops.api.protobuf.User;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.converter.UserVoConverter;
import org.springframework.stereotype.Component;

@Component
public class ProjectVoConverter {

    private final UserVoConverter userVoConverter;

    public ProjectVoConverter(UserVoConverter userVoConverter) {
        this.userVoConverter = userVoConverter;
    }

    public static ProjectVo system() {
        return ProjectVo.newBuilder()
                .setId("0")
                .setName("SYSTEM")
                .setOwner(User.UserVo.newBuilder().build())
                .setCreatedTime(-1L)
                .setPrivacy(Project.Privacy.PUBLIC.toString())
                .setDescription("System")
                .setStatistics(StatisticsVo.newBuilder().build())
                .build();
    }

    public ProjectVo fromBo(Project project, IdConverter idConvertor) {
        if (project == null) {
            return ProjectVo.newBuilder().build();
        }
        if (project.getId() == 0) {
            return system();
        }

        return ProjectVo.newBuilder()
                .setId(idConvertor.convert(project.getId()))
                .setName(project.getName())
                .setOwner(userVoConverter.fromBo(project.getOwner()))
                .setCreatedTime(project.getCreatedTime().getTime())
                .setPrivacy(project.getPrivacy().name())
                .setDescription(project.getDescription() != null ? project.getDescription() : "")
                .setStatistics(StatisticsVo.newBuilder().build())
                .build();
    }

    public ProjectVo fromEntity(ProjectEntity entity, IdConverter idConvertor, User.UserVo owner) {
        if (entity == null) {
            return ProjectVo.newBuilder().build();
        }
        if (entity.getId() == 0) {
            return system();
        }
        return ProjectVo.newBuilder()
                .setId(idConvertor.convert(entity.getId()))
                .setName(entity.getProjectName())
                .setOwner(owner)
                .setCreatedTime(entity.getCreatedTime().getTime())
                .setPrivacy(Project.Privacy.fromValue(entity.getPrivacy()).name())
                .setDescription(entity.getProjectDescription() != null ? entity.getProjectDescription() : "")
                .setStatistics(StatisticsVo.newBuilder().build())
                .build();
    }
}
