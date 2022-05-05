/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SWMPConvertor implements Convertor<SWModelPackageEntity, SWModelPackageVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public SWModelPackageVO convert(SWModelPackageEntity entity)
        throws ConvertException {
        return SWModelPackageVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .name(entity.getSwmpName())
            .owner(userConvertor.convert(entity.getOwner()))
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .build();
    }

    @Override
    public SWModelPackageEntity revert(SWModelPackageVO vo) throws ConvertException {
        Objects.requireNonNull(vo, "SWModelPackageVO");
        return SWModelPackageEntity.builder()
            .id(idConvertor.revert(vo.getId()))
            .swmpName(vo.getName())
            .projectId(idConvertor.revert(vo.getId()))
            .build();
    }
}
