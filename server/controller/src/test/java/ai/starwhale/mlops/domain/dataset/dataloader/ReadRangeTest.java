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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.DataStoreScanRangeRequest;
import ai.starwhale.mlops.datastore.DataStoreScanRequest;
import ai.starwhale.mlops.datastore.KeyRangeList;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataIndex;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataReadLog;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.Session;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.DataReadLogDao;
import ai.starwhale.mlops.domain.dataset.dataloader.dao.SessionDao;
import ai.starwhale.mlops.exception.SwValidationException;
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
    private final Integer cacheSize = 1;

    @BeforeEach
    public void setup() {
        dataStore = mock(DataStore.class);
        sessionDao = mock(SessionDao.class);
        dataReadLogDao = mock(DataReadLogDao.class);
        dataRangeProvider = new DataStoreIndexProvider(dataStore);
        DataReadManager dataReadManager = new DataReadManager(
                sessionDao, dataReadLogDao, dataRangeProvider, 1, cacheSize, "10s", 10);
        dataLoader = new DataLoader(dataReadManager, 1);
    }

    @Test
    public void testGenerateRange() {
        var request = QueryDataIndexRequest.builder()
                .tableName("test-table-name")
                .start("0000-000")
                .startInclusive(true)
                .batchSize(10)
                .build();
        given(dataStore.scanKeyRange(
                DataStoreScanRangeRequest.builder()
                        .start("0000-000")
                        .startInclusive(true)
                        .end(null)
                        .endInclusive(false)
                        .keepNone(true)
                        .rawResult(false)
                        .tables(List.of(
                                DataStoreScanRequest.TableInfo.builder()
                                        .tableName("test-table-name")
                                        .columns(Map.of("id", "id"))
                                        .build()
                        ))
                        .rangeInfo(DataStoreScanRangeRequest.RangeInfo.builder().batchSize(10).build())
                        .build()
        )).willReturn(new KeyRangeList(
                List.of(
                        KeyRangeList.Range.builder()
                                .start("0000-000")
                                .startType("STRING")
                                .startInclusive(true)
                                .end("0000-011")
                                .endType("STRING")
                                .endInclusive(false)
                                .size(10)
                                .build(),
                        KeyRangeList.Range.builder()
                                .start("0000-011")
                                .startType("STRING")
                                .startInclusive(true)
                                .end("0000-011")
                                .endType("STRING")
                                .endInclusive(true)
                                .size(1)
                                .build()
                )
        ));
        var dataRanges = dataRangeProvider.returnDataIndex(request);
        assertThat("ranges",
                dataRanges,
                is(List.of(
                        DataIndex.builder()
                                .start("0000-000").startType("STRING").startInclusive(true)
                                .end("0000-011").endType("STRING").endInclusive(false)
                                .size(10)
                                .build(),
                        DataIndex.builder()
                                .start("0000-011").startType("STRING").startInclusive(true)
                                .end("0000-011").endType("STRING").endInclusive(true)
                                .size(1)
                                .build()
                ))
        );
        assertThat("number", dataRanges.size() == 2);
        verify(dataStore, times(1)).scanKeyRange(any());
    }

    @Test
    public void testNextDataRange() {
        var sid = 2L;
        var sessionId = "1-session";
        var datasetName = "test-name";
        var datasetVersion = 1L;
        var tableName = "test-table-name";
        var batchSize = 10;
        var consumerIdFor1 = "1";
        var request = DataReadRequest.builder()
                .sessionId(sessionId)
                .consumerId(consumerIdFor1)
                .tableName(tableName)
                .datasetName(datasetName)
                .datasetVersionId(datasetVersion)
                .processedData(List.of())
                .batchSize(batchSize)
                .start("0000-000")
                .startType("STRING")
                .startInclusive(true)
                .end("0000-011")
                .endType("STRING")
                .endInclusive(true)
                .build();

        // case 1: generate
        given(sessionDao.selectOne(sessionId, String.valueOf(datasetVersion))).willReturn(null);
        given(sessionDao.insert(any())).willAnswer((Answer<Boolean>) invocation -> {
            var s = invocation.getArgument(0, Session.class);
            s.setId(sid);
            return true;
        });
        given(sessionDao.selectOne(sid)).willReturn(Session.builder()
                        .id(sid)
                        .datasetName(datasetName)
                        .datasetVersion(String.valueOf(datasetVersion))
                        .tableName(datasetName)
                        .start("0000-000").startType("STRING").startInclusive(true)
                        .end("0000-011").endType("STRING").endInclusive(true)
                        .batchSize(batchSize)
                        .status(Status.SessionStatus.UNFINISHED)
                        .build());

        given(dataReadLogDao.selectTopsUnAssignedData(sid, cacheSize))
                .willReturn(List.of(DataReadLog.builder()
                        .id(1L)
                        .sessionId(sid)
                        .start("0000-000").startType("STRING").startInclusive(true)
                        .end("0000-011").endType("STRING").endInclusive(false)
                        .size(10)
                        .build()));

        given(dataStore.scanKeyRange(
                DataStoreScanRangeRequest.builder()
                        .start("0000-000")
                        .startType("STRING")
                        .startInclusive(true)
                        .end("0000-011")
                        .endType("STRING")
                        .endInclusive(true)
                        .keepNone(true)
                        .rawResult(false)
                        .tables(List.of(
                                DataStoreScanRequest.TableInfo.builder()
                                        .tableName(datasetName)
                                        .columns(Map.of("id", "id"))
                                        .build()
                        ))
                        .rangeInfo(DataStoreScanRangeRequest.RangeInfo.builder().batchSize(10).build())
                        .build()
        )).willReturn(new KeyRangeList(
                List.of(
                        KeyRangeList.Range.builder()
                                .start("0000-000")
                                .startType("STRING")
                                .startInclusive(true)
                                .end("0000-011")
                                .endType("STRING")
                                .endInclusive(false)
                                .size(10)
                                .build(),
                        KeyRangeList.Range.builder()
                                .start("0000-011")
                                .startType("STRING")
                                .startInclusive(true)
                                .end("0000-011")
                                .endType("STRING")
                                .endInclusive(true)
                                .size(1)
                                .build()
                )
        ));

        var dataRange = dataLoader.next(request);

        assertThat("get data range", dataRange,
                is(DataReadLog.builder()
                        .id(1L)
                        .sessionId(sid)
                        .consumerId(consumerIdFor1) // update consumer id
                        .start("0000-000").startType("STRING").startInclusive(true)
                        .end("0000-011").endType("STRING").endInclusive(false)
                        .assignedNum(1)
                        .size(10)
                        .status(Status.DataStatus.UNPROCESSED)
                        .build()
                ));
        verify(dataStore, times(1)).scanKeyRange(any());
        verify(dataReadLogDao, times(1)).updateToAssigned(any());
        verify(dataReadLogDao, times(0)).updateToProcessed(any(), any(), any(), any());
        verify(sessionDao, times(1)).insert(any());

        // case 2: get next data with exist session and consumer 1
        request.setProcessedData(List.of(
                DataIndexDesc.builder().start("0000-011").end("0000-011").build()
        ));
        var session = Session.builder()
                .id(sid)
                .datasetName(datasetName)
                .datasetVersion(String.valueOf(datasetVersion))
                .tableName(tableName)
                .start("0000-000").startType("STRING").startInclusive(true)
                .end("0000-011").endType("STRING").endInclusive(true)
                .batchSize(batchSize)
                .status(Status.SessionStatus.FINISHED)
                .build();

        given(sessionDao.selectOne(sessionId, String.valueOf(datasetVersion))).willReturn(session);
        given(dataReadLogDao.selectTopsUnAssignedData(sid, cacheSize))
                .willReturn(List.of(DataReadLog.builder()
                        .id(2L)
                        .sessionId(sid)
                        .start("0000-011").startType("STRING").startInclusive(true)
                        .end("0000-011").endType("STRING").endInclusive(true)
                        .size(1)
                        .assignedNum(0)
                        .build()));

        dataRange = dataLoader.next(request);

        assertThat("get data range", dataRange,
                is(DataReadLog.builder()
                        .id(2L)
                        .sessionId(sid)
                        .consumerId(consumerIdFor1)
                        .start("0000-011").startType("STRING").startInclusive(true)
                        .end("0000-011").endType("STRING").endInclusive(true)
                        .size(1)
                        .assignedNum(1)
                        .status(Status.DataStatus.UNPROCESSED)
                        .build()));

        verify(dataStore, times(1)).scanKeyRange(any());
        verify(dataReadLogDao, times(2)).updateToAssigned(any());
        verify(dataReadLogDao, times(1))
                .updateToProcessed(sid, consumerIdFor1, "0000-011", "0000-011");
        verify(sessionDao, times(1)).insert(any());


        // case 3: invalid params(without startType)
        given(sessionDao.selectOne(sessionId, String.valueOf(datasetVersion))).willReturn(null);
        assertThrows(SwValidationException.class, () -> dataLoader.next(DataReadRequest.builder()
                .sessionId(sessionId)
                .consumerId(consumerIdFor1)
                .tableName(tableName)
                .datasetName(datasetName)
                .datasetVersionId(datasetVersion)
                .processedData(List.of())
                .batchSize(batchSize)
                .start("0000-000") // without startType
                .startInclusive(true)
                .end("0000-011")
                .endType("STRING")
                .endInclusive(true)
                .build())
        );

    }
}
