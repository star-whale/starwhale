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

import ai.starwhale.mlops.api.protocol.user.SystemRoleVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectRoleEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class SystemRoleConvertor implements Convertor<ProjectRoleEntity, SystemRoleVo> {


    private final IdConvertor idConvertor;

    private final RoleConvertor roleConvertor;

    private final UserConvertor userConvertor;

    public SystemRoleConvertor(IdConvertor idConvertor, RoleConvertor roleConvertor,
            UserConvertor userConvertor) {
        this.idConvertor = idConvertor;
        this.roleConvertor = roleConvertor;
        this.userConvertor = userConvertor;
    }


    @Override
    public SystemRoleVo convert(ProjectRoleEntity entity) throws ConvertException {
        if (entity == null) {
            return SystemRoleVo.empty();
        }
        return SystemRoleVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .role(roleConvertor.convert(entity.getRole()))
                .user(userConvertor.convert(entity.getUser()))
                .build();
    }

    @Override
    public ProjectRoleEntity revert(SystemRoleVo systemRoleVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
