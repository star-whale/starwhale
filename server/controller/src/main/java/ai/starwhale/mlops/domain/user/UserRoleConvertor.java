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

package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.UserRoleVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.project.ProjectConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class UserRoleConvertor implements Convertor<ProjectRoleEntity, UserRoleVo> {

    private final IdConvertor idConvertor;
    private final ProjectConvertor projectConvertor;
    private final RoleConvertor roleConvertor;

    public UserRoleConvertor(IdConvertor idConvertor, ProjectConvertor projectConvertor,
            RoleConvertor roleConvertor) {
        this.idConvertor = idConvertor;
        this.projectConvertor = projectConvertor;
        this.roleConvertor = roleConvertor;
    }


    @Override
    public UserRoleVo convert(ProjectRoleEntity projectRoleEntity) throws ConvertException {
        if(projectRoleEntity == null) {
            return UserRoleVo.empty();
        }
        return UserRoleVo.builder()
                .id(idConvertor.convert(projectRoleEntity.getId()))
                .project(projectConvertor.convert(projectRoleEntity.getProject()))
                .role(roleConvertor.convert(projectRoleEntity.getRole()))
                .build();
    }

    @Override
    public ProjectRoleEntity revert(UserRoleVo userRoleVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
