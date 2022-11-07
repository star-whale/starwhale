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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataIndex;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MultiConsumerTest extends MySqlContainerHolder {

    private DataReadManager dataReadManager;
    @MockBean
    private DataStoreIndexProvider dataRangeProvider;
    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private DataReadLogMapper dataReadLogMapper;

    public static Stream<Arguments> provideMultiParams() {
        return Stream.of(
            Arguments.of(0, true),
            Arguments.of(2, true),
            Arguments.of(6, true),
            Arguments.of(10, true)
        );
    }

    @BeforeEach
    public void setup() {
        SessionDao sessionDao = new SessionDao(sessionMapper, new SessionConverter());
        DataReadLogDao dataReadLogDao = new DataReadLogDao(dataReadLogMapper, new DataReadLogConverter());
        dataReadManager = new DataReadManager(sessionDao, dataReadLogDao, dataRangeProvider, 10);
    }

    @ParameterizedTest
    @MethodSource("provideMultiParams")
    public void testMultiConsumerRead(int errorNumPerConsumer, boolean isSerial)
            throws InterruptedException, ExecutionException {
        AtomicInteger count = new AtomicInteger(0);

        Random random = new Random();

        class ConsumerMock implements Runnable {
            private final String consumerId;
            private final String sessionId;
            private final int errorNum;
            private int retryNum = 0;

            ConsumerMock(String consumerId, String sessionId, int errorNum) {
                this.consumerId = consumerId;
                this.sessionId = sessionId;
                this.errorNum = errorNum;
            }

            @Override
            public void run() {
                var request = DataReadRequest.builder()
                            .sessionId(sessionId)
                            .consumerId(consumerId)
                            .isSerial(isSerial)
                            .datasetName("test-name")
                            .datasetVersion("test-version")
                            .tableName("test-table-name")
                            .processedData(List.of())
                            .batchSize(10)
                            .start(null)
                            .startInclusive(true)
                            .end(null)
                            .endInclusive(true)
                            .build();
                for (; ; ) {
                    var dataRange = dataReadManager.next(request);
                    if (dataRange == null) {
                        break;
                    }
                    // System.out.printf("%s get data:%s%n", Thread.currentThread(), JSONUtil.toJsonStr(dataRange));
                    // mock error
                    if (retryNum < errorNum) {
                        // mock restart
                        retryNum++;
                        try {
                            Thread.sleep(random.nextInt(10));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        request.setProcessedData(null);
                    } else {
                        count.addAndGet(dataRange.getSize());
                        // data processed
                        request.setProcessedData(List.of(
                                DataIndexDesc.builder()
                                    .start(dataRange.getStart())
                                    .end(dataRange.getEnd())
                                    .build()
                        ));
                    }

                }

            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        var sessionId = "session0" + errorNumPerConsumer + isSerial;
        var batchSize = 10;
        var indices = new ArrayList<DataIndex>();
        var totalRangesNum = 1001;
        for (int i = 1; i < totalRangesNum; i++) {
            indices.add(DataIndex.builder()
                    .start(String.valueOf((i - 1) * batchSize))
                    .end(String.valueOf(i * batchSize))
                    .size(batchSize)
                    .build()
            );
        }
        indices.add(
                DataIndex.builder()
                    .start(String.valueOf((totalRangesNum - 1) * batchSize))
                    .end(null)
                    .size(8)
                    .build()
        );

        given(dataRangeProvider.returnDataIndex(any()))
                .willReturn(indices);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(
                new ConsumerMock(String.valueOf(i), sessionId, errorNumPerConsumer)));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        var totalDataSize = (totalRangesNum - 1) * batchSize + 8;
        assertEquals(totalDataSize, count.get());


        var processedData = dataReadLogMapper.selectByStatus(sessionId, Status.DataStatus.PROCESSED.name());
        var unprocessedData = dataReadLogMapper.selectByStatus(sessionId, Status.DataStatus.UNPROCESSED.name());
        var totalProcessedNum = dataReadLogMapper.totalAssignedNum(sessionId);

        assertEquals(totalRangesNum, processedData.size());
        assertEquals(0, unprocessedData.size());
        assertEquals(totalRangesNum + errorNumPerConsumer * 10, totalProcessedNum);

        executor.shutdownNow();
    }

}
