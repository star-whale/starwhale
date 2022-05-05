/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import ai.starwhale.mlops.api.protocol.swds.DatasetVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SWDSConvertor implements Convertor<SWDatasetEntity, DatasetVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public DatasetVO convert(SWDatasetEntity entity) throws ConvertException {
        return DatasetVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .name(entity.getDatasetName())
            .owner(userConvertor.convert(entity.getOwner()))
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .build();
    }

    @Override
    public SWDatasetEntity revert(DatasetVO vo) throws ConvertException {
        Objects.requireNonNull(vo, "datasetVO");
        return SWDatasetEntity.builder()
            .id(idConvertor.revert(vo.getId()))
            .datasetName(vo.getName())
            .projectId(idConvertor.revert(vo.getId()))
            .build();
    }
}
