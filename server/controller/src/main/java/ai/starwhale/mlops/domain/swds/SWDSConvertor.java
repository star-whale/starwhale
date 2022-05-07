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
