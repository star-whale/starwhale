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

package ai.starwhale.mlops.domain.user.converter;

import ai.starwhale.mlops.api.protobuf.User.RoleVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import org.springframework.stereotype.Component;

@Component
public class RoleVoConverter {

    private final IdConverter idConverter;

    public RoleVoConverter(IdConverter idConverter) {
        this.idConverter = idConverter;
    }

    public RoleVo fromBo(Role role) {
        if (role == null) {
            return RoleVo.newBuilder().build();
        }
        return RoleVo.newBuilder()
                .setId(idConverter.convert(role.getId()))
                .setName(role.getRoleName())
                .setCode(role.getRoleCode())
                .build();
    }

    public RoleVo convert(RoleEntity roleEntity) {
        if (roleEntity == null) {
            return RoleVo.newBuilder().build();
        }
        return RoleVo.newBuilder()
                .setId(idConverter.convert(roleEntity.getId()))
                .setName(roleEntity.getRoleName())
                .setCode(roleEntity.getRoleCode())
                .build();
    }
}
