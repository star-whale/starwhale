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

package ai.starwhale.mlops.domain.swds.converter;

import ai.starwhale.mlops.api.protocol.swds.DatasetVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.swds.po.SwDatasetEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SwdsVoConvertor implements Convertor<SwDatasetEntity, DatasetVo> {

    @Resource
    private IdConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public DatasetVo convert(SwDatasetEntity entity) throws ConvertException {
        return DatasetVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getDatasetName())
                .owner(userConvertor.convert(entity.getOwner()))
                .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
                .build();
    }

    @Override
    public SwDatasetEntity revert(DatasetVo vo) throws ConvertException {
        Objects.requireNonNull(vo, "datasetVo");
        return SwDatasetEntity.builder()
                .id(idConvertor.revert(vo.getId()))
                .datasetName(vo.getName())
                .projectId(idConvertor.revert(vo.getId()))
                .build();
    }
}
