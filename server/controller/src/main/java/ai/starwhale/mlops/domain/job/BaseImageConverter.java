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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.runtime.BaseImageVo;
import ai.starwhale.mlops.common.Converter;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.job.po.BaseImageEntity;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BaseImageConverter implements Converter<BaseImageEntity, BaseImageVo> {

    private final IdConverter idConvertor;

    public BaseImageConverter(IdConverter idConvertor) {
        this.idConvertor = idConvertor;
    }

    @Override
    public BaseImageVo convert(BaseImageEntity baseImageEntity) throws ConvertException {
        if (baseImageEntity == null) {
            return BaseImageVo.empty();
        }
        return BaseImageVo.builder()
                .id(idConvertor.convert(baseImageEntity.getId()))
                .name(baseImageEntity.getImageName())
                .build();
    }

    @Override
    public BaseImageEntity revert(BaseImageVo baseImageVo) throws ConvertException {
        Objects.requireNonNull(baseImageVo, "baseImageVo");
        return BaseImageEntity.builder()
                .id(idConvertor.revert(baseImageVo.getId()))
                .imageName(baseImageVo.getName())
                .build();
    }
}
