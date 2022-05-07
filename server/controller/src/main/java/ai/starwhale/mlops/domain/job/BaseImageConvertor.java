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
