/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.runtime.BaseImageVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class BaseImageConvertor implements Convertor<BaseImageEntity, BaseImageVO> {

    @Resource
    private IDConvertor idConvertor;

    @Override
    public BaseImageVO convert(BaseImageEntity baseImageEntity) throws ConvertException {
        if(baseImageEntity == null) {
            return BaseImageVO.empty();
        }
        return BaseImageVO.builder()
            .id(idConvertor.convert(baseImageEntity.getId()))
            .name(baseImageEntity.getImageName())
            .build();
    }

    @Override
    public BaseImageEntity revert(BaseImageVO baseImageVO) throws ConvertException {
        Objects.requireNonNull(baseImageVO, "baseImageVO");
        return BaseImageEntity.builder()
            .id(idConvertor.revert(baseImageVO.getId()))
            .imageName(baseImageVO.getName())
            .build();
    }
}
