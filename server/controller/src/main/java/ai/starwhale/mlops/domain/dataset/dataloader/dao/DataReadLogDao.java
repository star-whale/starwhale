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

package ai.starwhale.mlops.domain.dataset.dataloader.dao;

import ai.starwhale.mlops.domain.dataset.dataloader.Status;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.converter.DataReadLogConverter;
import ai.starwhale.mlops.domain.dataset.dataloader.mapper.DataReadLogMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DataReadLogDao {
    private final DataReadLogMapper mapper;
    private final DataReadLogConverter converter;

    public DataReadLogDao(DataReadLogMapper mapper, DataReadLogConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    public boolean insertOne(DataReadLog dataReadLog) {
        var entity = converter.convert(dataReadLog);
        var count = mapper.insert(entity);
        dataReadLog.setId(entity.getId());
        return count > 0;
    }

    public boolean batchInsert(List<DataReadLog> dataReadLogs) {
        var entities = dataReadLogs.stream()
                .map(converter::convert)
                .collect(Collectors.toList());
        return mapper.batchInsert(entities) > 0;
    }


    public boolean updateToAssigned(DataReadLog dataReadLog) {
        return mapper.updateToAssigned(converter.convert(dataReadLog)) > 0;
    }

    public boolean updateToProcessed(Long sid, String consumerId, String start, String end) {
        return mapper.updateToProcessed(sid, consumerId, start, end, Status.DataStatus.PROCESSED.name()) > 0;
    }

    public boolean updateUnProcessedToUnAssigned(Long sid, String consumerId) {
        return mapper.updateToUnAssigned(sid, consumerId, Status.DataStatus.UNPROCESSED.name()) > 0;
    }

    public boolean updateUnProcessedToUnAssigned(Long sid) {
        return mapper.updateToUnAssignedForSession(sid, Status.DataStatus.UNPROCESSED.name()) > 0;
    }

    public DataReadLog selectTop1UnAssignedData(Long sid) {
        var entity = mapper.selectTop1UnAssigned(sid, Status.DataStatus.UNPROCESSED.name());
        return entity == null ? null : converter.revert(entity);
    }

    public DataReadLog selectTop1TimeoutData(Long sid, long secondTimeout) {
        var entity = mapper.selectTop1TimeoutData(sid, Status.DataStatus.UNPROCESSED.name(), secondTimeout);
        return entity == null ? null : converter.revert(entity);
    }

    public DataReadLog selectTop1UnProcessedDataBelongToOtherConsumers(Long sid, String consumerId) {
        var entity = mapper.selectTop1UnProcessedDataBelongToOtherConsumers(sid, consumerId);
        return entity == null ? null : converter.revert(entity);
    }

    public Long getMaxProcessedMicrosecondTime(Long sid) {
        return mapper.selectMaxProcessedMicrosecondTime(sid, Status.DataStatus.PROCESSED.name());
    }
}
