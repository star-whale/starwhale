/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SWDSVersionConvertor implements Convertor<SWDatasetVersionEntity, DatasetVersionVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public DatasetVersionVO convert(SWDatasetVersionEntity entity)
        throws ConvertException {
        return DatasetVersionVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .name(entity.getVersionName())
            .owner(userConvertor.convert(entity.getOwner()))
            .tag(entity.getVersionTag())
            .meta(entity.getVersionMeta())
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .build();
    }

    @Override
    public SWDatasetVersionEntity revert(DatasetVersionVO vo)
        throws ConvertException {
        Objects.requireNonNull(vo, "datasetVersionVO");
        return SWDatasetVersionEntity.builder()
            .id(idConvertor.revert(vo.getId()))
            .versionName(vo.getName())
            .ownerId(idConvertor.revert(vo.getOwner().getId()))
            .versionTag(vo.getTag())
            .build();
    }
}
