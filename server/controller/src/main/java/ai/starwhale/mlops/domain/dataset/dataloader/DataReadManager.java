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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DataReadManager {
    private final SessionDao sessionDao;
    private final DataReadLogDao dataReadLogDao;
    private final DataIndexProvider dataIndexProvider;

    public DataReadManager(SessionDao sessionDao,
                           DataReadLogDao dataReadLogDao,
                           DataIndexProvider dataIndexProvider) {
        this.sessionDao = sessionDao;
        this.dataReadLogDao = dataReadLogDao;
        this.dataIndexProvider = dataIndexProvider;
    }

    public Session getSession(DataReadRequest request) {
        var sessionId = request.getSessionId();
        var datasetVersionId = request.getDatasetVersionId();

        return sessionDao.selectOne(sessionId, String.valueOf(datasetVersionId));
    }

    @Transactional
    public Session generateSession(DataReadRequest request) {
        var session = Session.builder()
                    .sessionId(request.getSessionId())
                    .datasetName(request.getDatasetName())
                    .datasetVersion(String.valueOf(request.getDatasetVersionId()))
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
                                .startType(dataIndex.getStartType())
                                .startInclusive(dataIndex.isStartInclusive())
                                .end(dataIndex.getEnd())
                                .endType(dataIndex.getEndType())
                                .endInclusive(dataIndex.isEndInclusive())
                                .size(dataIndex.getSize())
                                .status(Status.DataStatus.UNPROCESSED)
                                .build())
                        .collect(Collectors.toList()),
                1000).forEach(dataReadLogDao::batchInsert);

        return session;

    }

    /**
     * Assign data for consumer
     *
     * @param consumerId consumer
     * @param session session
     * @return data
     */
    @Transactional
    public DataReadLog assignmentData(String consumerId, Session session) {
        var sid = session.getId();

        // get first
        var dataRange = dataReadLogDao.selectTop1UnAssignedData(sid);

        if (Objects.nonNull(dataRange)) {
            dataRange.setConsumerId(consumerId);
            dataRange.setAssignedNum(dataRange.getAssignedNum() + 1);
            dataReadLogDao.updateToAssigned(dataRange);
            log.info("Assignment data id: {} to consumer:{}", dataRange.getId(), dataRange.getConsumerId());
        }
        return dataRange;
    }

    @Transactional
    public void handleConsumerData(
            String consumerId, List<DataIndexDesc> processedData, Session session) {
        var sid = session.getId();
        // update processed data
        if (CollectionUtils.isNotEmpty(processedData)) {
            for (DataIndexDesc indexDesc : processedData) {
                dataReadLogDao.updateToProcessed(sid, consumerId, indexDesc.getStart(), indexDesc.getEnd());
            }
        }
    }

    @Transactional
    public void resetUnProcessedData(String consumerId) {
        var res = dataReadLogDao.updateUnProcessedToUnAssigned(consumerId);
        log.info("Reset unprocessed data for consumer:{}, result:{}", consumerId, res);
    }
}
