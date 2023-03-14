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
import ai.starwhale.mlops.datastore.DataStoreScanRequest;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataIndex;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "sw.dataset.range.provider", havingValue = "datastore", matchIfMissing = true)
public class DataStoreIndexProvider implements DataIndexProvider {

    private Integer maxBatchSize = 1000;

    private static final String KeyColumn = "id";

    private final DataStore dataStore;

    public DataStoreIndexProvider(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setMaxBatchSize(@Value("sw.datastore.scan.maxSize: 1000") Integer maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    @Override
    public List<DataIndex> returnDataIndex(QueryDataIndexRequest request) {
        String start = request.getStart();
        boolean startInclusive = request.isStartInclusive();
        // TODO: cache for dataset version
        var keys = new LinkedList<String>();
        for (; ; ) {
            var records = dataStore.scan(DataStoreScanRequest.builder()
                    // start params must use the current cursor
                    .start(start)
                    .startType("STRING")
                    .startInclusive(startInclusive)
                    .end(request.getEnd())
                    .endType("STRING")
                    .endInclusive(request.isEndInclusive())
                    .keepNone(true)
                    .rawResult(false)
                    .tables(List.of(
                            DataStoreScanRequest.TableInfo.builder()
                                    .tableName(request.getTableName())
                                    .columns(Map.of(KeyColumn, KeyColumn))
                                    .build()
                    ))
                    .limit(maxBatchSize)
                    .build()
            );
            if (records.getRecords().size() == 0) {
                break;
            } else {
                keys.addAll(
                        records.getRecords()
                                .stream()
                                .map(r -> (String) r.get(KeyColumn))
                                .collect(Collectors.toList())
                );
                if (records.getRecords().size() < maxBatchSize) {
                    break;
                }
                start = records.getLastKey();
                startInclusive = false;
            }
        }
        var index = 0;
        var batchSize = request.getBatchSize();
        var indices = new ArrayList<DataIndex>();
        while (index < keys.size()) {
            if (index + batchSize < keys.size()) {
                indices.add(DataIndex.builder()
                        .start(keys.get(index))
                        .end(keys.get(index + batchSize))
                        .size(batchSize)
                        .build());
                index += batchSize;
            } else {
                indices.add(DataIndex.builder()
                        .start(keys.get(index))
                        .end(null)
                        .size(keys.size() - index)
                        .build());
                index = keys.size();
            }
        }
        return indices;
    }
}
