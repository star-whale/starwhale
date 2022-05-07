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

package ai.starwhale.mlops.domain.user;

import ai.starwhale.mlops.api.protocol.user.RoleVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RoleConvertor implements Convertor<RoleEntity, RoleVO> {

    @Resource
    private IDConvertor idConvertor;

    @Override
    public RoleVO convert(RoleEntity entity) throws ConvertException {
        if(entity == null) {
            return RoleVO.empty();
        }
        return RoleVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .roleName(entity.getRoleName())
            .roleNameEn(entity.getRoleNameEn())
            .build();
    }

    @Override
    public RoleEntity revert(RoleVO vo) throws ConvertException {
        Objects.requireNonNull(vo, "RoleVO");
        return RoleEntity.builder()
            .id(idConvertor.revert(vo.getId()))
            .roleName(vo.getRoleName())
            .roleNameEn(vo.getRoleNameEn())
            .build();
    }
}
