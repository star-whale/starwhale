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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.DataStoreScanRequest;
import ai.starwhale.mlops.datastore.RecordList;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.DataReadLogDao;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.SessionDao;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReadRangeTest {
    private static DataReadManager dataReadManager;

    private static DataStoreIndexProvider dataRangeProvider;
    private static DataStore dataStore;
    private static SessionDao sessionDao;
    private static DataReadLogDao dataReadLogDao;

    @BeforeEach
    public void setup() {
        dataStore = mock(DataStore.class);
        sessionDao = mock(SessionDao.class);
        dataReadLogDao = mock(DataReadLogDao.class);
        dataRangeProvider = new DataStoreIndexProvider(dataStore);
        dataReadManager = new DataReadManager(sessionDao, dataReadLogDao, dataRangeProvider, 1);
    }

    @Test
    public void testGenerateRange() {
        dataRangeProvider.setMaxBatchSize(9);
        var sessionId = "1-mock";
        var session = Session.builder()
                .id(sessionId)
                .datasetName("test-name")
                .datasetVersion("test-version")
                .tableName("test-table-name")
                .start("0000-000")
                .startInclusive(true)
                .end("0000-008")
                .endInclusive(true)
                .batchSize(3)
                .build();
        given(dataStore.scan(
                DataStoreScanRequest.builder()
                    .start("0000-000")
                    .startInclusive(true)
                    .end("0000-008")
                    .endInclusive(true)
                    .keepNone(true)
                    .rawResult(false)
                    .tables(List.of(
                        DataStoreScanRequest.TableInfo.builder()
                            .tableName("test-table-name")
                            .columns(Map.of("id", "id"))
                            .build()
                    ))
                    .limit(9)
                    .build()
        )).willReturn(new RecordList(
                Map.of(),
                List.of(Map.of("id", "0000-000"),
                    Map.of("id", "0000-001"),
                    Map.of("id", "0000-002"),
                    Map.of("id", "0000-003"),
                    Map.of("id", "0000-004"),
                    Map.of("id", "0000-005"),
                    Map.of("id", "0000-006"),
                    Map.of("id", "0000-007"),
                    Map.of("id", "0000-008")
                ),
                "0000-008"
        ));
        given(dataStore.scan(
                DataStoreScanRequest.builder()
                    .start("0000-008")
                    .startInclusive(false)
                    .end("0000-008")
                    .endInclusive(true)
                    .keepNone(true)
                    .rawResult(false)
                    .tables(List.of(
                        DataStoreScanRequest.TableInfo.builder()
                            .tableName("test-table-name")
                            .columns(Map.of("id", "id"))
                            .build()
                    ))
                    .limit(9)
                    .build()
        )).willReturn(new RecordList(
                Map.of(), List.of(
                        Map.of("id", "0000-009"),
                        Map.of("id", "0000-010")
                    ), "0000-010"
        ));

        var dataRanges = dataRangeProvider.returnDataIndex(QueryDataIndexRequest.builder()
                    .tableName(session.getTableName())
                    .batchSize(session.getBatchSize())
                    .start(session.getStart())
                    .startInclusive(session.isStartInclusive())
                    .end(session.getEnd())
                    .endInclusive(session.isEndInclusive())
                    .build());
        assertThat("number", dataRanges.size() == 4);
        verify(dataStore, times(2)).scan(any());

    }

    @Test
    public void testNextDataRange() {
        var sessionId = "1-session";
        var consumerId = "1";
        var processId = "1-0";
        var request = DataReadRequest.builder()
                    .sessionId(sessionId)
                    .consumerId(consumerId)
                    .datasetName("test-name")
                    .datasetVersion("test-version")
                    .tableName("test-table-name")
                    .processedData(List.of())
                    .batchSize(2)
                    .start("0000-000")
                    .startInclusive(true)
                    .end("0000-008")
                    .endInclusive(true)
                    .build();

        // case 1: generate
        given(sessionDao.selectById(sessionId))
                .willReturn(null);
        given(dataReadLogDao.selectTop1UnAssignedData(sessionId))
                .willReturn(DataReadLog.builder()
                    .id(1L)
                    .sessionId(sessionId)
                    .start("0000-000").startInclusive(true)
                    .end("0000-001").endInclusive(true)
                    .size(2)
                    .build());

        given(dataStore.scan(
                DataStoreScanRequest.builder()
                    .start("0000-000")
                    .startInclusive(true)
                    .end("0000-008")
                    .endInclusive(true)
                    .keepNone(true)
                    .rawResult(false)
                    .tables(List.of(
                        DataStoreScanRequest.TableInfo.builder()
                            .tableName("test-table-name")
                            .columns(Map.of("id", "id"))
                            .build()
                    ))
                    .limit(1000)
                    .build()
        )).willReturn(new RecordList(
                Map.of(),
                List.of(Map.of("id", "0000-000"),
                        Map.of("id", "0000-001"),
                        Map.of("id", "0000-002"),
                        Map.of("id", "0000-003"),
                        Map.of("id", "0000-004"),
                        Map.of("id", "0000-005"),
                        Map.of("id", "0000-006"),
                        Map.of("id", "0000-007"),
                        Map.of("id", "0000-008")
                ),
                "0000-008"
        ));

        var dataRange = dataReadManager.next(request);

        assertThat("get data range", dataRange,
                    is(DataReadLog.builder()
                        .id(1L)
                        .sessionId(sessionId)
                        .consumerId(consumerId) // update consumer id
                        .start("0000-000").startInclusive(true)
                        .end("0000-001").endInclusive(true)
                        .size(2)
                        .assignedNum(1)
                        .status(Status.DataStatus.UNPROCESSED)
                        .build()
                    ));
        verify(dataStore, times(1)).scan(any());
        verify(dataReadLogDao, times(1)).updateToAssigned(any());
        verify(dataReadLogDao, times(0))
                .updateToProcessed(any(), any(), any(), any());
        verify(sessionDao, times(1)).insert(any());

        // case 2
        request.setProcessedData(List.of(
                    DataIndexDesc.builder().start("0000-000").end("0000-001").build()
        ));
        var session = Session.builder()
                .id(sessionId)
                .datasetName("test-name")
                .datasetVersion("test-version")
                .tableName("test-table-name")
                .start("0000-000")
                .startInclusive(true)
                .end("0000-008")
                .endInclusive(true)
                .batchSize(2)
                .build();

        given(sessionDao.selectById(sessionId))
                .willReturn(session);
        given(dataReadLogDao.selectTop1UnAssignedData(sessionId))
                .willReturn(DataReadLog.builder()
                    .id(2L)
                    .sessionId(sessionId)
                    .start("0000-002").startInclusive(true)
                    .end("0000-003").endInclusive(true)
                    .size(2)
                    .assignedNum(0)
                    .build());

        dataRange = dataReadManager.next(request);

        assertThat("get data range", dataRange,
                is(DataReadLog.builder()
                    .id(2L)
                    .sessionId(sessionId)
                    .consumerId(consumerId)
                    .start("0000-002").startInclusive(true)
                    .end("0000-003").endInclusive(true)
                    .size(2)
                    .assignedNum(1)
                    .status(Status.DataStatus.UNPROCESSED)
                    .build()));

        verify(dataStore, times(1)).scan(any());
        verify(dataReadLogDao, times(2)).updateToAssigned(any());
        verify(dataReadLogDao, times(1))
                .updateToProcessed(sessionId,  consumerId, "0000-000", "0000-001");
        verify(sessionDao, times(1)).insert(any());
    }
}
