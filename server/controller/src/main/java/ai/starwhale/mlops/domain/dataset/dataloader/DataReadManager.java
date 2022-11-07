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

package ai.starwhale.mlops.domain.dataset.dataloader;

import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.common.KeyLock;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataIndex;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.DataReadLogDao;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.SessionDao;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DataReadManager {
    private final Integer timeoutTolerance;
    private final SessionDao sessionDao;
    private final DataReadLogDao dataReadLogDao;
    private final DataIndexProvider dataIndexProvider;

    public DataReadManager(SessionDao sessionDao,
                           DataReadLogDao dataReadLogDao,
                           DataIndexProvider dataIndexProvider,
                           @Value("${sw.dataset.processed.timeout.tolerance: 5}") Integer timeoutTolerance) {
        this.sessionDao = sessionDao;
        this.dataReadLogDao = dataReadLogDao;
        this.dataIndexProvider = dataIndexProvider;
        this.timeoutTolerance = timeoutTolerance;
    }


    public DataReadLog next(DataReadRequest request) {

        this.handleConsumerData(request);

        return getDataReadIndex(request);
    }

    @Transactional
    DataReadLog getDataReadIndex(DataReadRequest request) {
        var sessionId = request.getSessionId();
        var consumerId = request.getConsumerId();
        // use key lock
        var sessionLock = new KeyLock<>(sessionId);
        try {
            sessionLock.lock();
            var session = sessionDao.selectById(sessionId);
            if (session == null) {
                session = Session.builder()
                        .id(sessionId)
                        .datasetName(request.getDatasetName())
                        .datasetVersion(request.getDatasetVersion())
                        .tableName(request.getTableName())
                        .start(request.getStart())
                        .startInclusive(request.isStartInclusive())
                        .end(request.getEnd())
                        .endInclusive(request.isEndInclusive())
                        .batchSize(request.getBatchSize())
                        .build();
                // get data index
                List<DataIndex> dataIndices = dataIndexProvider.returnDataIndex(
                        QueryDataIndexRequest.builder()
                            .tableName(session.getTableName())
                            .batchSize(session.getBatchSize())
                            .start(session.getStart())
                            .startInclusive(session.isStartInclusive())
                            .end(session.getEnd())
                            .endInclusive(session.isEndInclusive())
                            .build()
                );

                Iterables.partition(
                    dataIndices.stream()
                        .map(dataIndex -> DataReadLog.builder()
                                .sessionId(sessionId)
                                .start(dataIndex.getStart())
                                .startInclusive(dataIndex.isStartInclusive())
                                .end(dataIndex.getEnd())
                                .endInclusive(dataIndex.isEndInclusive())
                                .size(dataIndex.getSize())
                                .status(Status.DataStatus.UNPROCESSED)
                                .build())
                        .collect(Collectors.toList()),
                    1000).forEach(dataReadLogDao::batchInsert);

                // insert session
                sessionDao.insert(session);
            }
            // get first
            var dataRange = dataReadLogDao.selectTop1UnAssignedData(sessionId);

            if (Objects.isNull(dataRange)) {
                // find timeout data to consume
                var maxProcessedTime = dataReadLogDao.getMaxProcessedMicrosecondTime(sessionId);
                dataRange = dataReadLogDao.selectTop1TimeoutData(
                        sessionId, maxProcessedTime * timeoutTolerance);
            }

            if (Objects.nonNull(dataRange)) {
                dataRange.setConsumerId(consumerId);
                dataRange.setAssignedNum(dataRange.getAssignedNum() + 1);
                dataReadLogDao.updateToAssigned(dataRange);
            }
            return dataRange;
        } finally {
            sessionLock.unlock();
        }
    }

    void handleConsumerData(DataReadRequest request) {
        var sessionId = request.getSessionId();
        var consumerId = request.getConsumerId();
        var processedData = request.getProcessedData();
        var lock = new KeyLock<>(consumerId);
        try {
            lock.lock();
            // update processed data
            if (CollectionUtils.isNotEmpty(processedData)) {
                for (DataIndexDesc indexDesc : processedData) {
                    dataReadLogDao.updateToProcessed(
                            sessionId, consumerId, indexDesc.getStart(), indexDesc.getEnd());
                }
            }
            // Whether to process serially under the same consumer,
            // if serial is true, unassigned the previous unprocessed data
            if (request.isSerial()) {
                dataReadLogDao.updateUnProcessedToUnAssigned(consumerId);
            }
        } finally {
            lock.unlock();
        }

    }
}
