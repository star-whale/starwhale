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

import static ai.starwhale.mlops.exception.SwRequestFrequentException.RequestType.DATASET_LOAD;

import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.DataReadLogDao;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.SessionDao;
import ai.starwhale.mlops.exception.SwRequestFrequentException;
import cn.hutool.cache.impl.LRUCache;
import com.google.common.collect.Iterables;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DataReadManager {
    private final SessionDao sessionDao;
    private final DataReadLogDao dataReadLogDao;
    private final DataIndexProvider dataIndexProvider;
    private final LRUCache<String, LinkedList<DataReadLog>> sessionCache;
    private final Integer cacheSize;
    private final Integer insertBatchSize;

    private final DelayQueue<FailSession> failSessionQueue = new DelayQueue<>();

    public DataReadManager(SessionDao sessionDao,
                           DataReadLogDao dataReadLogDao,
                           DataIndexProvider dataIndexProvider,
                           @Value("${sw.dataset.load.read.log-cache-capacity:1000}") int capacity,
                           @Value("${sw.dataset.load.read.log-cache-size:1000}") int cacheSize,
                           @Value("${sw.dataset.load.read.log-cache-timeout:24h}") String cacheTimeout,
                           @Value("${sw.dataset.load.read.log-insert-batch:10}") int insertBatchSize
    ) {
        this.sessionDao = sessionDao;
        this.dataReadLogDao = dataReadLogDao;
        this.dataIndexProvider = dataIndexProvider;
        this.cacheSize = cacheSize;
        this.insertBatchSize = insertBatchSize;
        this.sessionCache = new LRUCache<>(capacity, DurationStyle.detectAndParse(cacheTimeout).toMillis());
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
        return session;
    }

    @Async
    public void generateDataReadLog(Long sessionId) {
        try {
            this.generate(sessionId);
        } catch (Exception e) {
            log.error("Error while generate data read logs for session: {}", sessionId, e);
            failSessionQueue.add(new FailSession(sessionId));
        }
    }

    private static class FailSession implements Delayed {
        int failCount = 0;

        Long sessionId;

        FailSession(Long sessionId) {
            this.sessionId = sessionId;
        }

        public long getDelayTime() {
            long delay = 100;
            if (failCount > 0) {
                delay *= (2L << (failCount - 1));
            }
            return delay;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return unit.convert(getDelayTime(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            long diffMillis = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
            diffMillis = Math.min(diffMillis, 1);
            diffMillis = Math.max(diffMillis, -1);
            return (int) diffMillis;
        }
    }

    @Scheduled(fixedRate = 1000)
    public void dealWithErrorSessions() {
        while (!failSessionQueue.isEmpty()) {
            FailSession delayed = failSessionQueue.poll();
            try {
                this.generate(delayed.sessionId);
            } catch (Exception e) {
                log.error("Error while deal with error session {}", delayed.sessionId, e);
                delayed.failCount++;
                failSessionQueue.add(delayed);
            }
        }
    }

    private void generate(Long sessionId) {
        var session = sessionDao.selectOne(sessionId);
        if (session == null || session.getStatus() == Status.SessionStatus.FINISHED) {
            return;
        }
        String start = session.getStart();
        boolean startInclusive = session.isStartInclusive();

        var lastLog = dataReadLogDao.selectLastData(session.getId());
        if (lastLog != null) {
            if (!StringUtils.hasText(lastLog.getEnd())) {
                sessionDao.updateToFinished(session.getId());
                return;
            } else {
                start = lastLog.getEnd();
                startInclusive = !lastLog.isEndInclusive();
            }
        }

        var request = QueryDataIndexRequest.builder()
                .tableName(session.getTableName())
                .batchSize(session.getBatchSize())
                .start(start)
                .startInclusive(startInclusive)
                .end(session.getEnd())
                .endInclusive(session.isEndInclusive())
                .build();
        // get data index TODO use iterator
        var data = dataIndexProvider.returnDataIndex(request);
        Iterables.partition(
                data.stream()
                        .map(dataIndex -> DataReadLog.builder()
                                .sessionId(session.getId())
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
                this.insertBatchSize
        ).forEach(dataReadLogDao::batchInsert);
        // save session
        sessionDao.updateToFinished(session.getId());
    }

    /**
     * Assign data for consumer
     *
     * @param consumerId consumer
     * @param session    session
     * @return data
     */
    @Transactional
    public DataReadLog assignmentData(String consumerId, Session session) {
        var queue = sessionCache.get(String.valueOf(session.getId()), LinkedList::new);

        if (queue.isEmpty()) {
            queue.addAll(dataReadLogDao.selectTopsUnAssignedData(session.getId(), cacheSize));
        }
        DataReadLog readLog = queue.poll();
        if (Objects.nonNull(readLog)) {
            readLog.setConsumerId(consumerId);
            readLog.setAssignedNum(readLog.getAssignedNum() + 1);
            dataReadLogDao.updateToAssigned(readLog);
            log.info("Assignment data id: {} to consumer:{}", readLog.getId(), readLog.getConsumerId());
        } else {
            if (session.getStatus() == Status.SessionStatus.UNFINISHED) {
                throw new SwRequestFrequentException(
                        DATASET_LOAD, "data load: index is building, please try again later");
            }
        }
        return readLog;
    }

    @Transactional
    public void handleConsumerData(
            String consumerId, List<DataIndexDesc> processedData, Long sessionId) {
        // update processed data
        if (CollectionUtils.isNotEmpty(processedData)) {
            for (DataIndexDesc indexDesc : processedData) {
                dataReadLogDao.updateToProcessed(sessionId, consumerId, indexDesc.getStart(), indexDesc.getEnd());
            }
        }
    }

    @Transactional
    public void resetUnProcessedData(String consumerId) {
        var res = dataReadLogDao.updateUnProcessedToUnAssigned(consumerId);
        log.info("Reset unprocessed data for consumer:{}, result:{}", consumerId, res);
    }
}
