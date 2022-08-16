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
package ai.starwhale.mlops.datastore;

import ai.starwhale.mlops.datastore.impl.MemoryTableImpl;
import ai.starwhale.mlops.exception.SWValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataStore {
    private final Map<String, MemoryTable> tables = new ConcurrentHashMap<>();

    public void update(String tableName,
                       TableSchemaDesc schema,
                       List<Map<String, String>> records) {
        var table = this.tables.computeIfAbsent(tableName, k -> new MemoryTableImpl());
        table.update(schema, records);
    }

    public RecordList query(DataStoreQueryRequest req) {
        var table = this.getTable(req.getTableName());
        var columns = req.getColumns();

        return table.query(columns,
                req.getOrderBy(),
                req.getFilter(),
                req.getStart(),
                req.getLimit());
    }

    public RecordList scan(DataStoreScanRequest req) {
        List<TableScanIterator> iters = new ArrayList<>();
        for (var info : req.getTables()) {
            var table = this.getTable(info.getTableName());
            var iter = table.scan(info.getColumns(),
                    req.getStart(),
                    req.isStartInclusive(),
                    req.getEnd(),
                    req.isEndInclusive(),
                    info.isKeepNone());
            iter.next();
            if (iter.getRecord() != null) {
                iters.add(iter);
            }
        }
        if (iters.isEmpty()) {
            return new RecordList(null, null, null);
        }
        var columnTypeMap = new HashMap<String, ColumnType>();
        for (var it : iters) {
            columnTypeMap.putAll(it.getColumnTypeMapping());
        }
        var keyColumnType = iters.get(0).getKeyColumnType();
        Object lastKey = null;
        List<Map<String, String>> ret = new ArrayList<>();
        while (!iters.isEmpty() && (req.getLimit() < 0 || ret.size() < req.getLimit())) {
            lastKey = Collections.min(iters, (a, b) -> {
                var x = (Comparable) a.getKey();
                var y = (Comparable) b.getKey();
                return x.compareTo(y);
            }).getKey();
            var record = new HashMap<String, String>();
            for (var iter : iters) {
                if (iter.getKey().equals(lastKey)) {
                    record.putAll(iter.getRecord());
                    iter.next();
                }
            }
            if (!req.isKeepNone()) {
                record.entrySet().removeIf(x -> x.getValue() == null);
            }
            ret.add(record);
            iters.removeIf(x -> x.getRecord() == null);
        }
        return new RecordList(columnTypeMap, ret, keyColumnType.encode(lastKey));
    }

    private MemoryTable getTable(String tableName) {
        var table = tables.get(tableName);
        if (table == null) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    "invalid table name " + tableName);
        }
        return table;
    }
}
