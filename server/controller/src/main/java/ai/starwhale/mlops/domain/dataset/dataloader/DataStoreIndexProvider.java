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

import static ai.starwhale.mlops.datastore.DataStore.QUERY_LIMIT;

import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.DataStoreScanRequest;
import ai.starwhale.mlops.domain.dataset.dataloader.bo.DataIndex;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "sw.dataset.range.provider", havingValue = "datastore", matchIfMissing = true)
public class DataStoreIndexProvider implements DataIndexProvider {

    private Integer maxScanSize = QUERY_LIMIT;

    private static final String KeyColumn = "id";

    private final DataStore dataStore;

    public DataStoreIndexProvider(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setMaxScanSize(Integer maxScanSize) {
        this.maxScanSize = maxScanSize;
    }

    class DataIndexIterator implements Iterator<List<DataIndex>> {
        private Integer maxScanSize = QUERY_LIMIT - 1;
        private String start;
        private boolean startInclusive;
        private final int limit;
        private final int batchSize;
        private final QueryDataIndexRequest request;
        private boolean done;

        @Getter
        @AllArgsConstructor
        class Key {
            String value;
            String type;
        }

        DataIndexIterator(QueryDataIndexRequest request) {
            this.request = request;
            this.start = request.getStart();
            this.startInclusive = request.isStartInclusive();
            // +1 is to ensure we get the last item for next round
            this.limit = request.getBatchSize() > maxScanSize - 1
                    ? maxScanSize : (maxScanSize / request.getBatchSize()) * request.getBatchSize() + 1;
            this.batchSize = request.getBatchSize() > maxScanSize ? maxScanSize : request.getBatchSize();
            this.done = false;
        }

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public List<DataIndex> next() {
            if (done) {
                throw new NoSuchElementException();
            }
            var result = dataStore.scan(DataStoreScanRequest.builder()
                    // start params must use the current cursor
                    .start(this.start)
                    .startInclusive(this.startInclusive)
                    .end(request.getEnd())
                    .endInclusive(request.isEndInclusive())
                    .keepNone(true)
                    .rawResult(false)
                    .tables(List.of(
                            DataStoreScanRequest.TableInfo.builder()
                                    .tableName(request.getTableName())
                                    .columns(Map.of(KeyColumn, KeyColumn))
                                    .build()
                    ))
                    .limit(this.limit)
                    // return the key and value
                    .encodeWithType(true)
                    .build()
            );
            if (result.getRecords().size() == 0) {
                this.done = true;
                return List.of();
            } else {
                var keys = result.getRecords()
                        .stream()
                        .map(r -> new Key(
                                (String) ((Map<?, ?>) r.get(KeyColumn)).get("value"),
                                (String) ((Map<?, ?>) r.get(KeyColumn)).get("type"))
                        ).collect(Collectors.toCollection(LinkedList::new));
                // update query param for next request
                this.start = result.getLastKey();
                this.startInclusive = false;
                var index = 0;
                var indices = new ArrayList<DataIndex>();
                while (index < keys.size()) {
                    if (index + batchSize < keys.size()) {
                        indices.add(DataIndex.builder()
                                .start(keys.get(index).value)
                                .startType(keys.get(index).type)
                                .end(keys.get(index + batchSize).value)
                                .endType(keys.get(index + batchSize).type)
                                .size(batchSize)
                                .build());
                        index += batchSize;
                    } else {
                        // this may be the last request(key size less than limit) or not
                        if (keys.size() < limit) {
                            indices.add(DataIndex.builder()
                                    .start(keys.get(index).value)
                                    .startType(keys.get(index).type)
                                    .end(null)
                                    .endType(keys.get(keys.size() - 1).type)
                                    .size(keys.size() - index)
                                    .build());
                            index = keys.size();
                        }
                    }
                }

                if (indices.size() < limit) {
                    this.done = true;
                }
                return indices;
            }
        }
    }

    @Override
    public Iterator<List<DataIndex>> returnDataIndexIter(QueryDataIndexRequest request) {
        return new DataIndexIterator(request);
    }

    @Override
    public List<DataIndex> returnDataIndex(QueryDataIndexRequest request) {
        String start = request.getStart();
        boolean startInclusive = request.isStartInclusive();
        // TODO: cache for dataset version

        @Getter
        @AllArgsConstructor
        class Key {
            String value;
            String type;
        }

        var keys = new LinkedList<Key>();
        for (; ; ) {
            var result = dataStore.scan(DataStoreScanRequest.builder()
                    // start params must use the current cursor
                    .start(start)
                    .startInclusive(startInclusive)
                    .end(request.getEnd())
                    .endInclusive(request.isEndInclusive())
                    .keepNone(true)
                    .rawResult(false)
                    .tables(List.of(
                            DataStoreScanRequest.TableInfo.builder()
                                    .tableName(request.getTableName())
                                    .columns(Map.of(KeyColumn, KeyColumn))
                                    .build()
                    ))
                    .limit(maxScanSize)
                    // return the key and value
                    .encodeWithType(true)
                    .build()
            );
            if (result.getRecords().size() == 0) {
                break;
            } else {
                keys.addAll(
                        result.getRecords()
                                .stream()
                                .map(r -> new Key(
                                        (String) ((Map<?, ?>) r.get(KeyColumn)).get("value"),
                                        (String) ((Map<?, ?>) r.get(KeyColumn)).get("type"))
                                )
                                .collect(Collectors.toList())
                );
                if (result.getRecords().size() < maxScanSize) {
                    break;
                }
                start = result.getLastKey();
                startInclusive = false;
            }
        }
        var index = 0;
        var batchSize = request.getBatchSize();
        var indices = new ArrayList<DataIndex>();
        while (index < keys.size()) {
            if (index + batchSize < keys.size()) {
                indices.add(DataIndex.builder()
                        .start(keys.get(index).value)
                        .startType(keys.get(index).type)
                        .end(keys.get(index + batchSize).value)
                        .endType(keys.get(index + batchSize).type)
                        .size(batchSize)
                        .build());
                index += batchSize;
            } else {
                indices.add(DataIndex.builder()
                        .start(keys.get(index).value)
                        .startType(keys.get(index).type)
                        .end(null)
                        .endType(keys.get(keys.size() - 1).type)
                        .size(keys.size() - index)
                        .build());
                index = keys.size();
            }
        }
        return indices;
    }
}
