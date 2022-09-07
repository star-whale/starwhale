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

import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class RoleConvertor implements Convertor<RoleEntity, RoleVo> {

    private final IdConvertor idConvertor;

    public RoleConvertor(IdConvertor idConvertor) {
        this.idConvertor = idConvertor;
    }


    @Override
    public RoleVo convert(RoleEntity roleEntity) throws ConvertException {
        if (roleEntity == null) {
            return RoleVo.empty();
        }
        return RoleVo.builder()
                .id(idConvertor.convert(roleEntity.getId()))
                .name(roleEntity.getRoleName())
                .code(roleEntity.getRoleCode())
                .description(roleEntity.getRoleDescription())
                .build();
    }

    @Override
    public RoleEntity revert(RoleVo projectRoleVo) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
