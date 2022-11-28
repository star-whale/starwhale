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

package ai.starwhale.mlops.domain.model.converter;

import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class ModelVoConverter {

    private final IdConverter idConverter;

    public ModelVoConverter(IdConverter idConverter) {
        this.idConverter = idConverter;
    }


    public ModelVo convert(ModelEntity entity)
            throws ConvertException {
        if (entity == null) {
            return ModelVo.empty();
        }
        return ModelVo.builder()
                .id(idConverter.convert(entity.getId()))
                .name(entity.getModelName())
                .createdTime(entity.getCreatedTime().getTime())
                .build();
    }
}
