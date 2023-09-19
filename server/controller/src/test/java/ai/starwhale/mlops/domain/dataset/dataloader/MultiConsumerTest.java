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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataIndex;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.converter.DataReadLogConverter;
import ai.starwhale.mlops.domain.dataset.dataloader.converter.SessionConverter;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.DataReadLogDao;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.SessionDao;
import ai.starwhale.mlops.domain.dataset.dataloader.mapper.DataReadLogMapper;
import ai.starwhale.mlops.domain.dataset.dataloader.mapper.SessionMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MybatisTest(properties = {
    "logging.level.root=error",
    "logging.level.ai.starwhale.mlops=error",
    "logging.level.ai.starwhale.mlops.domain.dataset.dataloader.DataReadManager=debug",
    "mybatis.configuration.map-underscore-to-camel-case=true",
    "sw.dataset.processed.timeout.tolerance=100"
})
@Import({DataLoader.class, DataReadManager.class,
        SessionDao.class, SessionConverter.class,
        DataReadLogDao.class, DataReadLogConverter.class})
@EnableTransactionManagement
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MultiConsumerTest extends MySqlContainerHolder {

    @Autowired
    private DataLoader dataLoader;
    @MockBean
    private DataStoreIndexProvider dataRangeProvider;
    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private SessionConverter sessionConverter;
    @Autowired
    private DataReadLogMapper dataReadLogMapper;
    @Autowired
    private DataReadLogConverter dataReadLogConverter;

    @ParameterizedTest
    @CsvSource(value = {
            "1,0,1",
            "2,2,1",
            "3,6,1",
            "4,10,1",
            "5,0,2",
            "6,2,2",
            "7,6,2",
            "8,10,2",
            "9,0,3",
            "10,2,3",
            "11,6,3",
            "12,10,3"
    })
    public void testMultiConsumerRead(String consumptionRound, int errorNumPerConsumer, int datasetNum)
            throws InterruptedException, ExecutionException {

        var sessionId = "session" + errorNumPerConsumer + datasetNum;
        var datasetName = "test-name";
        var batchSize = 10;

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger indexCount = new AtomicInteger(0);

        Random random = new Random();

        class ConsumerMock implements Runnable {
            private final String consumerId;
            private final String datasetName;
            private final int errorNum;
            private final long datasetNum;
            private int retryNum = 0;

            ConsumerMock(String consumerId, String datasetName, int errorNum, long datasetNum) {
                this.consumerId = consumerId;
                this.datasetName = datasetName;
                this.errorNum = errorNum;
                this.datasetNum = datasetNum;
            }

            @Override
            public void run() {
                var request = DataReadRequest.builder()
                            .sessionId(sessionId)
                            .consumerId(consumerId)
                            .datasetName(datasetName)
                            .tableName("test-table-name")
                            .processedData(List.of())
                            .batchSize(batchSize)
                            .start(null)
                            .startInclusive(true)
                            .end(null)
                            .endInclusive(true)
                            .build();
                for (long i = 0; i < datasetNum; i++) {
                    // mock multi datasets
                    request.setDatasetVersionId(i);
                    request.setProcessedData(null);
                    for (; ; ) {
                        var dataRange = dataLoader.next(request);
                        if (dataRange == null) {
                            break;
                        }

                        assertEquals(dataRange.getStartType(), "STRING");
                        assertEquals(dataRange.getEndType(), "STRING");

                        try {
                            Thread.sleep(random.nextInt(10));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // mock error
                        if (retryNum < errorNum) {
                            // mock error
                            retryNum++;
                            request.setProcessedData(null);
                        } else {
                            indexCount.addAndGet(1);
                            count.addAndGet(dataRange.getSize());
                            // data processed
                            request.setProcessedData(List.of(
                                    DataIndexDesc.builder()
                                            .start(dataRange.getStart())
                                            .startType(dataRange.getStartType())
                                            .end(dataRange.getEnd())
                                            .endType(dataRange.getEndType())
                                            .build()
                            ));
                        }

                    }
                }
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);

        var totalRangesNum = 1001;
        var consumerNum = 10;

        ArrayList<DataIndex> indices = getDataIndices(batchSize, totalRangesNum);

        given(dataRangeProvider.returnDataIndex(any())).willReturn(indices);

        var errorConsumers = new ArrayList<String>();
        // first consumption with error
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < consumerNum; i++) {
            var consumerId = consumptionRound + i;
            if (errorNumPerConsumer > 0) {
                errorConsumers.add(consumerId);
            }
            futures.add(executor.submit(
                new ConsumerMock(consumerId, datasetName, errorNumPerConsumer, datasetNum)));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        // mock data reset logic
        futures.clear();
        for (String errorConsumer : errorConsumers) {
            futures.add(executor.submit(() -> dataLoader.resetUnProcessed(errorConsumer)));
        }
        for (Future<?> future : futures) {
            future.get();
        }

        // try again
        futures.clear();
        for (String errorConsumer : errorConsumers) {
            futures.add(executor.submit(
                    new ConsumerMock(errorConsumer, datasetName, 0, datasetNum)));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        verify(dataRangeProvider, times(datasetNum)).returnDataIndex(any());

        var datasetSize = (totalRangesNum - 1) * batchSize + 8;

        assertEquals(datasetSize * datasetNum, count.get());

        var processedData = dataReadLogMapper.selectByStatus(sessionId, Status.DataStatus.PROCESSED.name());
        var unprocessedData = dataReadLogMapper.selectByStatus(sessionId, Status.DataStatus.UNPROCESSED.name());
        var totalProcessedNum = dataReadLogMapper.totalAssignedNum(sessionId);

        assertEquals(totalRangesNum * datasetNum, processedData.size());
        assertEquals(0, unprocessedData.size());

        assertEquals((totalRangesNum) * datasetNum + errorNumPerConsumer * consumerNum, totalProcessedNum);

        executor.shutdownNow();
    }

    @NotNull
    private static ArrayList<DataIndex> getDataIndices(int batchSize, int totalRangesNum) {
        var indices = new ArrayList<DataIndex>();

        for (int i = 1; i < totalRangesNum; i++) {
            indices.add(DataIndex.builder()
                    .start(String.valueOf((i - 1) * batchSize))
                    .startType("STRING")
                    .end(String.valueOf(i * batchSize))
                    .endType("STRING")
                    .size(batchSize)
                    .build()
            );
        }
        indices.add(
                DataIndex.builder()
                        .start(String.valueOf((totalRangesNum - 1) * batchSize))
                        .startType("STRING")
                        .end(null)
                        .endType("STRING")
                        .size(8)
                        .build()
        );
        return indices;
    }

    @Test
    public void testMapper() {
        // insert a session
        var sessionId = "000000000000000001";
        var datasetName = "test-name";
        var datasetVersion = "1";
        var session = Session.builder()
                .sessionId(sessionId)
                .datasetName(datasetName)
                .datasetVersion(datasetVersion)
                .tableName("test-table")
                .batchSize(10)
                .start("s-start")
                .startInclusive(false)
                .end("s-end")
                .endInclusive(false)
                .build();

        assertEquals(1, sessionMapper.insert(sessionConverter.convert(session)));

        var result = sessionMapper.selectOne(sessionId, datasetVersion);

        assertEquals(session.getDatasetName(), result.getDatasetName());
        assertEquals(session.getDatasetVersion(), result.getDatasetVersion());
        assertEquals(session.getTableName(), result.getTableName());
        assertNull(result.getCurrent());
        assertTrue(result.isCurrentInclusive());
        assertEquals(session.getStart(), result.getStart());
        assertEquals(session.isStartInclusive(), result.isStartInclusive());
        assertEquals(session.getEnd(), result.getEnd());
        assertEquals(session.getBatchSize(), result.getBatchSize());
        assertNotNull(session.getCreatedTime());

        // insert a read log
        var dataReadLog = DataReadLog.builder()
                .sessionId(result.getId())
                .start("s-start")
                .end("e-end")
                .size(10)
                .build();
        dataReadLogMapper.batchInsert(List.of(dataReadLogConverter.convert(dataReadLog)));

        // select top 1 unassigned
        var top1UnAssigned = dataReadLogMapper.selectTop1UnAssigned(
                result.getId(), Status.DataStatus.UNPROCESSED.name());

        assertNotNull(top1UnAssigned.getId());
        assertEquals(dataReadLog.getSessionId(), top1UnAssigned.getSessionId());
        assertEquals(dataReadLog.getStart(), top1UnAssigned.getStart());
        assertEquals(dataReadLog.isStartInclusive(), top1UnAssigned.isStartInclusive());
        assertEquals(dataReadLog.getEnd(), top1UnAssigned.getEnd());
        assertEquals(dataReadLog.isEndInclusive(), top1UnAssigned.isEndInclusive());
        assertEquals(dataReadLog.getStatus(), top1UnAssigned.getStatus());
        assertEquals(Status.DataStatus.UNPROCESSED, top1UnAssigned.getStatus());
        assertEquals(dataReadLog.getAssignedNum(), top1UnAssigned.getAssignedNum());
        assertEquals(dataReadLog.getSize(), top1UnAssigned.getSize());
        assertNull(top1UnAssigned.getConsumerId());
        assertNull(top1UnAssigned.getFinishedTime());
        assertNull(top1UnAssigned.getAssignedTime());
        assertNotNull(top1UnAssigned.getCreatedTime());

        var consumerId = "00001";
        // assign it to consumer
        var top1UnsignedBo = dataReadLogConverter.revert(top1UnAssigned);
        top1UnsignedBo.setConsumerId(consumerId);

        assertEquals(1, dataReadLogMapper.updateToAssigned(dataReadLogConverter.convert(top1UnsignedBo)));

        // query data
        var updated = dataReadLogMapper.selectOne(top1UnAssigned.getId());

        assertEquals(1, updated.getAssignedNum());
        assertNotNull(updated.getAssignedTime());

        // assign it again
        var updatedBo = dataReadLogConverter.revert(updated);
        updatedBo.setConsumerId(consumerId);

        assertEquals(1, dataReadLogMapper.updateToAssigned(dataReadLogConverter.convert(updatedBo)));
        updated = dataReadLogMapper.selectOne(top1UnAssigned.getId());

        assertEquals(2, updated.getAssignedNum());
        assertNotNull(updated.getAssignedTime());
    }

}
