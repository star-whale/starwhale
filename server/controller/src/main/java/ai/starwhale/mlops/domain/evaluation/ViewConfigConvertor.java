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

package ai.starwhale.mlops.domain.evaluation;

import ai.starwhale.mlops.api.protocol.evaluation.ConfigVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.evaluation.po.ViewConfigEntity;
import ai.starwhale.mlops.exception.ConvertException;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class ViewConfigConvertor implements Convertor<ViewConfigEntity, ConfigVO> {

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public ConfigVO convert(ViewConfigEntity entity) throws ConvertException {
        if(entity == null) {
            return ConfigVO.empty();
        }
        return ConfigVO.builder()
            .name(entity.getConfigName())
            .content(entity.getContent())
            .createTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .build();
    }

    @Override
    public ViewConfigEntity revert(ConfigVO configVO) throws ConvertException {
        throw new UnsupportedOperationException();
    }
}
