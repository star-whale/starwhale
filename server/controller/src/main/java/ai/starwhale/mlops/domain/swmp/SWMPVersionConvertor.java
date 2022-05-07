/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SWMPVersionConvertor implements Convertor<SWModelPackageVersionEntity, SWModelPackageVersionVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public SWModelPackageVersionVO convert(SWModelPackageVersionEntity entity)
        throws ConvertException {
        return SWModelPackageVersionVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .name(entity.getVersionName())
            .owner(userConvertor.convert(entity.getOwner()))
            .tag(entity.getVersionTag())
            .meta(entity.getVersionMeta())
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .build();
    }

    @Override
    public SWModelPackageVersionEntity revert(SWModelPackageVersionVO vo)
        throws ConvertException {
        Objects.requireNonNull(vo, "SWModelPackageVersionVO");
        return SWModelPackageVersionEntity.builder()
            .id(idConvertor.revert(vo.getId()))
            .versionName(vo.getName())
            .ownerId(idConvertor.revert(vo.getOwner().getId()))
            .versionTag(vo.getTag())
            .build();
    }
}
