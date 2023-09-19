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

package ai.starwhale.mlops.domain.dataset.dataloader.converter;

import ai.starwhale.mlops.common.Converter;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.po.DataReadLogEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class DataReadLogConverter implements Converter<DataReadLog, DataReadLogEntity> {

    @Override
    public DataReadLogEntity convert(DataReadLog dataReadLog) throws ConvertException {
        return DataReadLogEntity.builder()
                .id(dataReadLog.getId())
                .sessionId(dataReadLog.getSessionId())
                .consumerId(dataReadLog.getConsumerId())
                .start(dataReadLog.getStart())
                .startType(dataReadLog.getStartType())
                .startInclusive(dataReadLog.isStartInclusive())
                .end(dataReadLog.getEnd())
                .endType(dataReadLog.getEndType())
                .endInclusive(dataReadLog.isEndInclusive())
                .size(dataReadLog.getSize())
                .status(dataReadLog.getStatus())
                .assignedTime(dataReadLog.getAssignedTime())
                .assignedNum(dataReadLog.getAssignedNum())
                .finishedTime(dataReadLog.getFinishedTime())
                .createdTime(dataReadLog.getCreatedTime())
                .build();
    }

    @Override
    public DataReadLog revert(DataReadLogEntity dataRange) throws ConvertException {
        return DataReadLog.builder()
                .id(dataRange.getId())
                .sessionId(dataRange.getSessionId())
                .consumerId(dataRange.getConsumerId())
                .start(dataRange.getStart())
                .startType(dataRange.getStartType())
                .startInclusive(dataRange.isStartInclusive())
                .end(dataRange.getEnd())
                .endType(dataRange.getEndType())
                .endInclusive(dataRange.isEndInclusive())
                .size(dataRange.getSize())
                .status(dataRange.getStatus())
                .assignedTime(dataRange.getAssignedTime())
                .assignedNum(dataRange.getAssignedNum())
                .finishedTime(dataRange.getFinishedTime())
                .createdTime(dataRange.getCreatedTime())
                .build();
    }
}
