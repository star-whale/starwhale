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
import org.mockito.stubbing.Answer;

public class ReadRangeTest {

    private static DataLoader dataLoader;
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
        DataReadManager dataReadManager = new DataReadManager(
                sessionDao, dataReadLogDao, dataRangeProvider);
        dataLoader = new DataLoader(dataReadManager);
    }

    @Test
    public void testGenerateRange() {
        dataRangeProvider.setMaxBatchSize(9);

        var request = QueryDataIndexRequest.builder()
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
                        .encodeWithType(true)
                        .build()
        )).willReturn(new RecordList(
                Map.of(),
                Map.of(),
                List.of(Map.of("id", Map.of("value", "0000-000", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-001", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-002", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-003", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-004", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-005", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-006", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-007", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-008", "type", "STRING"))
                ),
                "0000-008",
                "STRING"
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
                        .encodeWithType(true)
                        .build()
        )).willReturn(new RecordList(
                Map.of(), Map.of(), List.of(
                Map.of("id", Map.of("value", "0000-009", "type", "STRING")),
                Map.of("id", Map.of("value", "0000-010", "type", "STRING")),
                Map.of("id", Map.of("value", "0000-011", "type", "STRING"))
        ), "0000-010",
                "STRING"
        ));

        var dataRanges = dataRangeProvider.returnDataIndex(request);
        assertThat("number", dataRanges.size() == 4);
        verify(dataStore, times(2)).scan(any());

        request.setBatchSize(6);
        dataRanges = dataRangeProvider.returnDataIndex(request);

        assertThat("number", dataRanges.size() == 2);
        verify(dataStore, times(4)).scan(any());
    }

    @Test
    public void testNextDataRange() {
        var sid = 2L;
        var sessionId = "1-session";
        var datasetName = "test-name";
        var datasetVersion = 1L;
        var tableName = "test-table-name";
        var consumerIdFor1 = "1";
        var consumerIdFor2 = "2";
        var request = DataReadRequest.builder()
                .sessionId(sessionId)
                .consumerId(consumerIdFor1)
                .tableName(tableName)
                .datasetName(datasetName)
                .datasetVersionId(datasetVersion)
                .processedData(List.of())
                .batchSize(2)
                .start("0000-000")
                .startInclusive(true)
                .end("0000-008")
                .endInclusive(true)
                .build();

        // case 1: generate
        given(sessionDao.selectOne(sessionId, String.valueOf(datasetVersion))).willReturn(null);
        given(sessionDao.insert(any())).willAnswer((Answer<Boolean>) invocation -> {
            var session = invocation.getArgument(0, Session.class);
            session.setId(sid);
            return true;
        });
        given(dataReadLogDao.selectTop1UnAssignedData(sid))
                .willReturn(DataReadLog.builder()
                        .id(1L)
                        .sessionId(sid)
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
                        .encodeWithType(true)
                        .build()
        )).willReturn(new RecordList(
                Map.of(),
                Map.of(),
                List.of(Map.of("id", Map.of("value", "0000-000", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-001", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-002", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-003", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-004", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-005", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-006", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-007", "type", "STRING")),
                        Map.of("id", Map.of("value", "0000-008", "type", "STRING"))
                ),
                "0000-008",
                "STRING"
        ));

        var dataRange = dataLoader.next(request);

        assertThat("get data range", dataRange,
                is(DataReadLog.builder()
                        .id(1L)
                        .sessionId(sid)
                        .consumerId(consumerIdFor1) // update consumer id
                        .start("0000-000").startInclusive(true)
                        .end("0000-001").endInclusive(true)
                        .assignedNum(1)
                        .size(2)
                        .status(Status.DataStatus.UNPROCESSED)
                        .build()
                ));
        verify(dataStore, times(1)).scan(any());
        verify(dataReadLogDao, times(1)).updateToAssigned(any());
        verify(dataReadLogDao, times(0)).updateToProcessed(any(), any(), any(), any());
        verify(sessionDao, times(1)).insert(any());

        // case 2: get next data with exist session and consumer 1
        request.setProcessedData(List.of(
                DataIndexDesc.builder().start("0000-000").end("0000-001").build()
        ));
        var session = Session.builder()
                .id(sid)
                .datasetName("test-name")
                .datasetVersion("test-version")
                .tableName("test-table-name")
                .start("0000-000").startInclusive(true)
                .end("0000-008").endInclusive(true)
                .batchSize(2)
                .build();

        given(sessionDao.selectOne(sessionId, String.valueOf(datasetVersion))).willReturn(session);
        given(dataReadLogDao.selectTop1UnAssignedData(sid))
                .willReturn(DataReadLog.builder()
                        .id(2L)
                        .sessionId(sid)
                        .start("0000-002").startInclusive(true)
                        .end("0000-003").endInclusive(true)
                        .size(2)
                        .assignedNum(0)
                        .build());

        dataRange = dataLoader.next(request);

        assertThat("get data range", dataRange,
                is(DataReadLog.builder()
                        .id(2L)
                        .sessionId(sid)
                        .consumerId(consumerIdFor1)
                        .start("0000-002").startInclusive(true)
                        .end("0000-003").endInclusive(true)
                        .size(2)
                        .assignedNum(1)
                        .status(Status.DataStatus.UNPROCESSED)
                        .build()));

        verify(dataStore, times(1)).scan(any());
        verify(dataReadLogDao, times(2)).updateToAssigned(any());
        verify(dataReadLogDao, times(1))
                .updateToProcessed(sid, consumerIdFor1, "0000-000", "0000-001");
        verify(sessionDao, times(1)).insert(any());

    }
}
