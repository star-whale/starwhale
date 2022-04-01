/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
