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

import ai.starwhale.mlops.api.protocol.user.ProjectRoleVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.domain.user.RoleConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class ProjectRoleConvertor implements Convertor<ProjectRoleEntity, ProjectRoleVo> {

    private final IdConvertor idConvertor;
    private final ProjectConvertor projectConvertor;
    private final RoleConvertor roleConvertor;
    private final UserConvertor userConvertor;

    public ProjectRoleConvertor(IdConvertor idConvertor,
            ProjectConvertor projectConvertor, RoleConvertor roleConvertor,
            UserConvertor userConvertor) {
        this.idConvertor = idConvertor;
        this.projectConvertor = projectConvertor;
        this.roleConvertor = roleConvertor;
        this.userConvertor = userConvertor;
    }

    @Override
    public ProjectRoleVo convert(ProjectRoleEntity entity) throws ConvertException {
        if (entity == null) {
            return ProjectRoleVo.empty();
        }
        return ProjectRoleVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .project(projectConvertor.convert(entity.getProject()))
                .role(roleConvertor.convert(entity.getRole()))
                .user(userConvertor.convert(entity.getUser()))
                .build();
    }

    @Override
    public ProjectRoleEntity revert(ProjectRoleVo projectRoleVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
