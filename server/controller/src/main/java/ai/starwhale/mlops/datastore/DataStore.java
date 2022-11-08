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

import ai.starwhale.mlops.datastore.ParquetConfig.CompressionCodec;
import ai.starwhale.mlops.datastore.impl.MemoryTableImpl;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.core.collection.CollectionUtil;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Slf4j
@Component
public class DataStore {

    private final WalManager walManager;
    private final StorageAccessService storageAccessService;

    private final Map<String, MemoryTable> tables = new ConcurrentHashMap<>();
    private final String dataRootPath;
    private final ParquetConfig parquetConfig;

    public DataStore(WalManager walManager,
            StorageAccessService storageAccessService,
            @Value("${sw.datastore.dataRootPath:}") String dataRootPath,
            @Value("${sw.datastore.parquet.compressionCodec:SNAPPY}") String compressionCodec,
            @Value("${sw.datastore.parquet.rowGroupSize:128MB}") String rowGroupSize,
            @Value("${sw.datastore.parquet.pageSize:1MB}") String pageSize,
            @Value("${sw.datastore.parquet.pageRowCountLimit:20000}") int pageRowCountLimit) {
        this.storageAccessService = storageAccessService;
        this.dataRootPath = dataRootPath;
        this.parquetConfig = new ParquetConfig();
        this.parquetConfig.setCompressionCodec(CompressionCodec.valueOf(compressionCodec));
        this.parquetConfig.setRowGroupSize(DataSize.parse(rowGroupSize).toBytes());
        this.parquetConfig.setPageSize((int) DataSize.parse(pageSize).toBytes());
        this.parquetConfig.setPageRowCountLimit(pageRowCountLimit);
        this.walManager = walManager;
        var it = this.walManager.readAll();
        while (it.hasNext()) {
            var entry = it.next();
            var tableName = entry.getTableName();
            var table = this.tables.computeIfAbsent(tableName,
                    k -> new MemoryTableImpl(tableName,
                            this.walManager,
                            this.storageAccessService,
                            this.dataRootPath,
                            this.parquetConfig));
            table.updateFromWal(entry);
        }
    }

    public void terminate() {
        this.walManager.terminate();
    }

    public List<String> list(String prefix) {
        return tables.keySet().stream().filter(name -> name.startsWith(prefix)).collect(Collectors.toList());
    }

    public void update(String tableName,
            TableSchemaDesc schema,
            List<Map<String, Object>> records) {
        var table = this.tables.computeIfAbsent(tableName,
                k -> new MemoryTableImpl(tableName,
                        this.walManager,
                        this.storageAccessService,
                        this.dataRootPath,
                        this.parquetConfig));
        table.lock();
        try {
            table.update(schema, records);
        } finally {
            table.unlock();
        }
    }

    public RecordList query(DataStoreQueryRequest req) {
        var table = this.getTable(req.getTableName(), req.isIgnoreNonExistingTable());
        if (table == null) {
            return new RecordList(Collections.emptyMap(), Collections.emptyList(), null);
        }
        table.lock();
        try {
            var schema = table.getSchema();
            var columns = this.getColumnAliases(schema, req.getColumns());
            var columnTypeMap = schema.getColumnTypeMapping(columns);
            var results = table.query(
                    columns,
                    req.getOrderBy(),
                    req.getFilter(),
                    req.getStart(),
                    req.getLimit(),
                    req.isKeepNone(),
                    req.isRawResult());
            String lastKey;
            if (results.isEmpty()) {
                lastKey = null;
            } else {
                lastKey = (String) schema.getKeyColumnType().encode(
                        results.get(results.size() - 1).getKey(),
                        req.isRawResult());
            }
            var records = results.stream()
                    .map(r -> DataStore.encodeRecord(columnTypeMap, r.getValues(), req.isRawResult()))
                    .collect(Collectors.toList());
            return new RecordList(columnTypeMap, records, lastKey);
        } finally {
            table.unlock();
        }
    }

    public RecordList scan(DataStoreScanRequest req) {
        var limit = req.getLimit();
        if (limit > 1000) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "limit must be less or equal to 1000. request=" + req);
        }
        if (limit < 0) {
            limit = 1000;
        }

        var tablesToLock =
                req.getTables()
                        .stream()
                        .map(DataStoreScanRequest.TableInfo::getTableName)
                        .sorted() // prevent deadlock
                        .map(i -> getTable(i, req.isIgnoreNonExistingTable()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        for (var table : tablesToLock) {
            table.lock();
        }
        try {
            class TableMeta {

                String tableName;
                MemoryTable table;
                TableSchema schema;
                Map<String, String> columns;
                Map<String, ColumnType> columnTypeMap;
                boolean keepNone;
            }

            var tables = req.getTables().stream().map(info -> {
                var ret = new TableMeta();
                ret.tableName = info.getTableName();
                ret.table = this.getTable(info.getTableName(), req.isIgnoreNonExistingTable());
                if (ret.table == null) {
                    return null;
                }
                ret.schema = ret.table.getSchema();
                ret.columns = this.getColumnAliases(ret.schema, info.getColumns());
                ret.columnTypeMap = ret.schema.getColumnTypeMapping(ret.columns);
                ret.keepNone = info.isKeepNone();
                return ret;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            if (CollectionUtil.isEmpty(tables)) {
                return new RecordList(Map.of(), List.of(), null);
            }
            var columnTypeMap = new HashMap<String, ColumnType>();
            for (var table : tables) {
                if (table.schema.getKeyColumnType() != tables.get(0).schema.getKeyColumnType()) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            MessageFormat.format(
                                    "conflicting key column type. {0}: key={1}, type={2}, {3}: key={4}, type={5}",
                                    tables.get(0).tableName,
                                    tables.get(0).schema.getKeyColumn(),
                                    tables.get(0).schema.getKeyColumnType(),
                                    table.tableName,
                                    table.schema.getKeyColumn(),
                                    table.schema.getKeyColumnType()));
                }
                for (var entry : table.columnTypeMap.entrySet()) {
                    var columnName = entry.getKey();
                    var columnType = entry.getValue();
                    var old = columnTypeMap.putIfAbsent(columnName, columnType);
                    if (old != null && old != columnType) {
                        for (var t : tables) {
                            if (t.columnTypeMap.get(columnName) != null) {
                                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                        MessageFormat.format(
                                                "conflicting column type. {0}: column={1}, alias={2}, type={3}, "
                                                        + "{4}: column={5}, alias={6}, type={7}",
                                                t.tableName,
                                                columnName,
                                                t.columns.get(columnName),
                                                t.columnTypeMap.get(columnName),
                                                table.tableName,
                                                columnName,
                                                table.columns.get(columnName),
                                                table.columnTypeMap.get(columnName)));
                            }
                        }
                    }
                }
            }

            class TableRecords {

                TableMeta meta;
                List<MemoryTable.RecordResult> records;
                int index;

                public MemoryTable.RecordResult getRecord() {
                    return this.records.get(this.index);
                }

                public Object getKey() {
                    return this.getRecord().getKey();
                }
            }

            var records = new ArrayList<TableRecords>();
            for (var table : tables) {
                var r = new TableRecords();
                r.meta = table;
                r.records = table.table.scan(table.columns,
                        req.getStart(),
                        req.isStartInclusive(),
                        req.getEnd(),
                        req.isEndInclusive(),
                        limit,
                        table.keepNone);
                if (!r.records.isEmpty()) {
                    records.add(r);
                }
            }
            var keyColumnType = tables.get(0).schema.getKeyColumnType();
            Object lastKey = null;
            List<Map<String, Object>> ret = new ArrayList<>();
            while (!records.isEmpty() && ret.size() < limit) {
                lastKey = Collections.min(records, (a, b) -> {
                    @SuppressWarnings("rawtypes") var x = (Comparable) a.getKey();
                    @SuppressWarnings("rawtypes") var y = (Comparable) b.getKey();
                    //noinspection unchecked
                    return x.compareTo(y);
                }).getKey();
                var record = new HashMap<String, Object>();
                for (var r : records) {
                    if (r.getKey().equals(lastKey)) {
                        record.putAll(
                                DataStore.encodeRecord(r.meta.columnTypeMap, r.getRecord().values, req.isRawResult()));
                        ++r.index;
                    }
                }
                if (!req.isKeepNone()) {
                    record.entrySet().removeIf(x -> x.getValue() == null);
                }
                ret.add(record);
                records.removeIf(r -> r.index == r.records.size());
            }
            return new RecordList(columnTypeMap, ret, (String) keyColumnType.encode(lastKey, false));
        } finally {
            for (var table : tablesToLock) {
                table.unlock();
            }
        }
    }

    private MemoryTable getTable(String tableName, boolean allowNull) {
        var table = tables.get(tableName);
        if (table == null) {
            if (allowNull) {
                log.warn("not found table:{}!", tableName);
            } else {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE).tip(
                        "invalid table name " + tableName);
            }
        }
        return table;
    }

    private Map<String, String> getColumnAliases(TableSchema schema, Map<String, String> columns) {
        if (columns == null || columns.isEmpty()) {
            return schema.getColumnSchemas().stream()
                    .map(ColumnSchema::getName)
                    .collect(Collectors.toMap(Function.identity(), Function.identity()));
        } else {
            var ret = new HashMap<String, String>();
            var invalidColumns = new HashSet<>(columns.keySet());
            for (var columnSchema : schema.getColumnSchemas()) {
                var columnName = columnSchema.getName();
                var alias = columns.get(columnName);
                if (alias != null) {
                    ret.put(columnName, alias);
                    invalidColumns.remove(columnName);
                } else {
                    var colonIndex = columnName.indexOf(":");
                    if (colonIndex > 0) {
                        var prefix = columnName.substring(0, colonIndex);
                        alias = columns.get(prefix);
                        if (alias != null) {
                            ret.put(columnName, alias + columnName.substring(colonIndex));
                            invalidColumns.remove(prefix);
                        }
                    }
                }
            }
            if (!invalidColumns.isEmpty()) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "invalid columns: " + invalidColumns);
            }
            return ret;
        }
    }

    private static Map<String, Object> encodeRecord(Map<String, ColumnType> columnTypeMap,
            Map<String, Object> values,
            boolean rawResult) {
        var ret = new HashMap<String, Object>();
        for (var entry : values.entrySet()) {
            var columnName = entry.getKey();
            var columnValue = entry.getValue();
            ret.put(columnName, columnTypeMap.get(columnName).encode(columnValue, rawResult));
        }
        return ret;
    }
}
