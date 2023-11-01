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

import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.DataStoreScanRangeRequest;
import ai.starwhale.mlops.datastore.DataStoreScanRequest;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataIndex;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "sw.dataset.range.provider", havingValue = "datastore", matchIfMissing = true)
public class DataStoreIndexProvider implements DataIndexProvider {

    /**
     * the key column of dataset
     */
    private static final String KeyColumn = "id";

    private final DataStore dataStore;

    public DataStoreIndexProvider(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public List<DataIndex> returnDataIndex(QueryDataIndexRequest request) {
        // TODO: cache for dataset version

        var ranges = dataStore.scanKeyRange(DataStoreScanRangeRequest.builder()
                .start(request.getStart())
                .startType(request.getStartType())
                .startInclusive(request.isStartInclusive())
                .end(request.getEnd())
                .endType(request.getEndType())
                .endInclusive(request.isEndInclusive())
                .keepNone(true)
                .rawResult(false)
                .tables(List.of(
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName(request.getTableName())
                                .columns(Map.of(KeyColumn, KeyColumn))
                                .build()
                ))
                .rangeInfo(DataStoreScanRangeRequest.RangeInfo.builder().batchSize(request.getBatchSize()).build())
                .build()
        );
        return ranges.getRanges().stream()
                .map(range -> DataIndex.builder()
                        .start(range.getStart())
                        .startType(range.getStartType())
                        .startInclusive(range.isStartInclusive())
                        .end(range.getEnd())
                        .endType(range.getEndType())
                        .endInclusive(range.isEndInclusive())
                        .size(range.getSize())
                        .build())
                .collect(Collectors.toList());
    }
}
