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

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

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

    @Transactional(propagation = REQUIRES_NEW)
    Session getOrGenerateSession(DataReadRequest request) {
        var sessionId = request.getSessionId();
        var datasetName = request.getDatasetName();
        var datasetVersion = request.getDatasetVersion();
        // ensure serially in the same session
        var sessionLock = new KeyLock<>(String.format("%s-%s-%s", sessionId, datasetName, datasetVersion));
        try {
            sessionLock.lock();
            var session = sessionDao.selectOne(sessionId, datasetName, datasetVersion);
            if (session == null) {
                session = Session.builder()
                        .sessionId(sessionId)
                        .datasetName(request.getDatasetName())
                        .datasetVersion(request.getDatasetVersion())
                        .tableName(request.getTableName())
                        .start(request.getStart())
                        .startInclusive(request.isStartInclusive())
                        .end(request.getEnd())
                        .endInclusive(request.isEndInclusive())
                        .batchSize(request.getBatchSize())
                        .build();
                // insert session
                sessionDao.insert(session);
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
                Long sid = session.getId();
                Iterables.partition(
                    dataIndices.stream()
                        .map(dataIndex -> DataReadLog.builder()
                            .sessionId(sid)
                            .start(dataIndex.getStart())
                            .startInclusive(dataIndex.isStartInclusive())
                            .end(dataIndex.getEnd())
                            .endInclusive(dataIndex.isEndInclusive())
                            .size(dataIndex.getSize())
                            .status(Status.DataStatus.UNPROCESSED)
                            .build())
                        .collect(Collectors.toList()),
                    1000).forEach(dataReadLogDao::batchInsert);

            }
            return session;
        } finally {
            sessionLock.unlock();
        }

    }

    /**
     * Assign data for consumer
     * Message Delivery Semantics: At least once
     *
     * @param consumerId consumer
     * @param session session
     * @return data
     */
    @Transactional(propagation = REQUIRES_NEW)
    DataReadLog assignmentData(String consumerId, Session session) {
        var sid = session.getId();

        var sessionId = session.getSessionId();
        var datasetName = session.getDatasetName();
        var datasetVersion = session.getDatasetVersion();
        // ensure serially in the same session
        var sessionLock = new KeyLock<>(String.format("%s-%s-%s", sessionId, datasetName, datasetVersion));
        try {
            sessionLock.lock();
            // get first
            var dataRange = dataReadLogDao.selectTop1UnAssignedData(sid);

            if (Objects.isNull(dataRange)) {
                // find timeout data to consume
                var maxProcessedTime = dataReadLogDao.getMaxProcessedMicrosecondTime(sid);
                dataRange = maxProcessedTime == null ?  null : dataReadLogDao.selectTop1TimeoutData(
                    sid, maxProcessedTime * timeoutTolerance);
            }

            if (Objects.isNull(dataRange)) {
                // find unprocessed data to consume(only lead to repeat consume, but it doesn't matter)
                dataRange = dataReadLogDao.selectTop1UnProcessedDataBelongToOtherConsumers(sid, consumerId);
            }
            if (Objects.nonNull(dataRange)) {
                dataRange.setConsumerId(consumerId);
                dataReadLogDao.updateToAssigned(dataRange);
            }
            return dataRange;
        } finally {
            sessionLock.unlock();
        }

    }

    @Transactional(propagation = REQUIRES_NEW)
    void handleConsumerData(String consumerId, boolean isSerial, List<DataIndexDesc> processedData, Session session) {
        var sid = session.getId();
        var lock = new KeyLock<>(consumerId);
        try {
            lock.lock();
            // update processed data
            if (CollectionUtils.isNotEmpty(processedData)) {
                for (DataIndexDesc indexDesc : processedData) {
                    dataReadLogDao.updateToProcessed(sid, consumerId, indexDesc.getStart(), indexDesc.getEnd());
                }
            }
            // Whether to process serially under the same consumer,
            // if serial is true, unassigned the previous unprocessed data
            if (isSerial) {
                dataReadLogDao.updateUnProcessedToUnAssigned(sid, consumerId);
            }
        } finally {
            lock.unlock();
        }

    }
}
