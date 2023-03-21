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

package ai.starwhale.mlops.datastore.impl;

import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.ColumnTypeScalar;
import ai.starwhale.mlops.datastore.MemoryTable;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.ParquetConfig;
import ai.starwhale.mlops.datastore.RecordResult;
import ai.starwhale.mlops.datastore.TableMeta;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.Wal;
import ai.starwhale.mlops.datastore.WalManager;
import ai.starwhale.mlops.datastore.parquet.SwParquetReaderBuilder;
import ai.starwhale.mlops.datastore.parquet.SwParquetWriterBuilder;
import ai.starwhale.mlops.datastore.parquet.SwReadSupport;
import ai.starwhale.mlops.datastore.parquet.SwWriter;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

@Slf4j
public class MemoryTableImpl implements MemoryTable {

    private static final String TIMESTAMP_COLUMN_NAME = "^";

    private static final String DELETED_FLAG_COLUMN_NAME = "-";

    private final String tableName;

    private final WalManager walManager;
    private final StorageAccessService storageAccessService;
    private final String dataPathPrefix;
    private final SimpleDateFormat dataPathSuffixFormat = new SimpleDateFormat("yyMMddHHmmss.SSS");
    private final ParquetConfig parquetConfig;

    private TableSchema schema = null;

    @Getter
    private long firstWalLogId = -1;

    private long lastWalLogId = -1;

    @Getter
    private long lastUpdateTime = 0;

    private final TreeMap<Object, List<MemoryRecord>> recordMap = new TreeMap<>();

    private final Lock lock = new ReentrantLock();

    public MemoryTableImpl(String tableName,
            WalManager walManager,
            StorageAccessService storageAccessService,
            String dataPathPrefix,
            ParquetConfig parquetConfig) {
        this.tableName = tableName;
        this.walManager = walManager;
        this.storageAccessService = storageAccessService;
        this.parquetConfig = parquetConfig;
        this.dataPathPrefix = dataPathPrefix;
        this.dataPathSuffixFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        this.load();
    }

    private void load() {
        Set<String> paths;
        try {
            paths = this.storageAccessService.list(dataPathPrefix).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.DATASTORE, "failed to load " + this.tableName, e);
        }

        while (!paths.isEmpty()) {
            var path = paths.stream().max(Comparator.naturalOrder()).orElse(null);
            try {
                var conf = new Configuration();
                try (var reader = new SwParquetReaderBuilder(this.storageAccessService, path).withConf(conf).build()) {
                    for (; ; ) {
                        var record = reader.read();
                        if (this.schema == null) {
                            this.schema = TableSchema.fromJsonString(conf.get(SwReadSupport.SCHEMA_KEY));
                            var metaBuilder = TableMeta.MetaData.newBuilder();
                            try {
                                JsonFormat.parser().merge(conf.get(SwReadSupport.META_DATA_KEY), metaBuilder);
                            } catch (InvalidProtocolBufferException e) {
                                throw new SwProcessException(ErrorType.DATASTORE, "failed to parse metadata", e);
                            }
                            var metadata = metaBuilder.build();
                            this.lastWalLogId = metadata.getLastWalLogId();
                            this.lastUpdateTime = metadata.getLastUpdateTime();
                        }
                        if (record == null) {
                            break;
                        }
                        var key = record.remove(this.schema.getKeyColumn());
                        var timestamp = (Long) record.remove(TIMESTAMP_COLUMN_NAME);
                        var deletedFlag = (Boolean) record.remove(DELETED_FLAG_COLUMN_NAME);
                        this.recordMap.computeIfAbsent(key, k -> new ArrayList<>())
                                .add(MemoryRecord.builder()
                                    .timestamp(timestamp)
                                    .deleted(deletedFlag)
                                    .values(record)
                                    .build());
                    }
                }
                break;
            } catch (SwValidationException e) {
                log.warn("fail to load table:{} with path:{}, because it is invalid, try to load previous file again.",
                            this.tableName, path);
                paths.remove(path);
            } catch (RuntimeException | IOException e) {
                throw new SwProcessException(ErrorType.DATASTORE, "failed to load " + this.tableName, e);
            }
        }

    }

    @Override
    public void save() throws IOException {
        String metadata;
        try {
            metadata = JsonFormat.printer().print(TableMeta.MetaData.newBuilder()
                    .setLastWalLogId(this.lastWalLogId)
                    .setLastUpdateTime(this.lastUpdateTime)
                    .build());
        } catch (InvalidProtocolBufferException e) {
            throw new SwProcessException(ErrorType.DATASTORE, "failed to print table meta", e);
        }
        var columnSchema = this.schema.getColumnTypeMapping();
        columnSchema.put(TIMESTAMP_COLUMN_NAME, ColumnTypeScalar.INT64);
        columnSchema.put(DELETED_FLAG_COLUMN_NAME, ColumnTypeScalar.BOOL);

        try {
            SwWriter.writeWithBuilder(
                    new SwParquetWriterBuilder(this.storageAccessService, columnSchema,
                        this.schema.toJsonString(), metadata,
                        this.dataPathPrefix + this.dataPathSuffixFormat.format(new Date()), this.parquetConfig),
                    this.recordMap.entrySet().stream()
                        .map(entry -> {
                            var list = new ArrayList<Map<String, Object>>();
                            for (var record : entry.getValue()) {
                                var recordMap = new HashMap<String, Object>();
                                if (record.getValues() != null) {
                                    recordMap.putAll(record.getValues());
                                }
                                recordMap.put(this.schema.getKeyColumn(), entry.getKey());
                                recordMap.put(TIMESTAMP_COLUMN_NAME, record.getTimestamp());
                                recordMap.put(DELETED_FLAG_COLUMN_NAME, record.isDeleted());
                                list.add(recordMap);
                            }
                            return list;
                        }).flatMap(Collection::stream).iterator());
        } catch (Throwable e) {
            log.error("fail to save table:{}, error:{}", this.tableName, e.getMessage(), e);
            throw e;
        }
        this.firstWalLogId = -1;
    }

    @Override
    public void lock() {
        this.lock.lock();
    }

    @Override
    public void unlock() {
        this.lock.unlock();
    }

    public TableSchema getSchema() {
        return this.schema == null ? null : new TableSchema(this.schema);
    }

    @Override
    public void updateFromWal(Wal.WalEntry entry) {
        if (entry.getId() <= this.lastWalLogId) {
            return;
        }
        if (entry.hasTableSchema()) {
            var schemaDesc = WalManager.parseTableSchema(entry.getTableSchema());
            if (this.schema == null) {
                this.schema = new TableSchema(schemaDesc);
            } else {
                this.schema.merge(schemaDesc);
            }
        }
        var recordList = entry.getRecordsList();
        if (!recordList.isEmpty()) {
            this.insertRecords(entry.getTimestamp(),
                    recordList.stream().map(this::parseRecord).collect(Collectors.toList()));
        }
        if (this.firstWalLogId < 0) {
            this.firstWalLogId = entry.getId();
        }
        this.lastWalLogId = entry.getId();
    }

    private Map<String, Object> parseRecord(Wal.Record record) {
        Map<String, Object> ret = new HashMap<>();
        for (var col : record.getColumnsList()) {
            if (col.getIndex() == -1) {
                ret.put(DELETED_FLAG_COLUMN_NAME, true);
            } else {
                var colSchema = this.schema.getColumnSchemaByIndex(col.getIndex());
                ret.put(colSchema.getName(), colSchema.getType().fromWal(col));
            }
        }
        return ret;
    }

    @Override
    public String update(TableSchemaDesc schema, List<Map<String, Object>> records) {
        var timestamp = System.currentTimeMillis();
        var logEntryBuilder = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName(this.tableName)
                .setTimestamp(timestamp);
        TableSchema newSchema = this.schema;
        if (schema == null) {
            if (this.schema == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "schema should not be null for the first update");
            }
        } else {
            var logSchemaBuilder = Wal.TableSchema.newBuilder();
            List<ColumnSchema> diff;
            if (this.schema == null) {
                newSchema = new TableSchema(schema);
                diff = newSchema.getColumnSchemas();
                logSchemaBuilder.setKeyColumn(newSchema.getKeyColumn());
            } else {
                newSchema = new TableSchema(this.schema);
                diff = newSchema.merge(schema);
            }
            for (var col : diff) {
                logSchemaBuilder.addColumns(WalManager.convertColumnSchema(col));
            }
            logEntryBuilder.setTableSchema(logSchemaBuilder);
        }
        List<Map<String, Object>> decodedRecords = null;
        if (records != null) {
            decodedRecords = new ArrayList<>();
            for (var record : records) {
                var key = record.get(newSchema.getKeyColumn());
                if (key == null) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            MessageFormat.format("key column {0} is null", newSchema.getKeyColumn()));
                }
                if (record.get(DELETED_FLAG_COLUMN_NAME) != null) {
                    decodedRecords.add(Map.of(newSchema.getKeyColumn(),
                            newSchema.getKeyColumnType().decode(key),
                            DELETED_FLAG_COLUMN_NAME,
                            true));
                } else {
                    decodedRecords.add(MemoryTableImpl.decodeRecord(newSchema, record));
                }
            }
            for (var record : decodedRecords) {
                logEntryBuilder.addRecords(MemoryTableImpl.writeRecord(newSchema, record));
            }
        }
        this.lastWalLogId = this.walManager.append(logEntryBuilder);
        if (this.firstWalLogId < 0) {
            this.firstWalLogId = this.lastWalLogId;
        }
        this.lastUpdateTime = System.currentTimeMillis();
        this.schema = newSchema;
        if (decodedRecords != null) {
            this.insertRecords(timestamp, decodedRecords);
        }

        return Long.toString(timestamp);
    }

    private void insertRecords(long timestamp, List<Map<String, Object>> records) {
        var typeMap = this.schema.getColumnTypeMapping();
        for (var record : records) {
            var newRecord = new HashMap<>(record);
            var key = newRecord.remove(this.schema.getKeyColumn());
            var deletedFlag = (Boolean) newRecord.remove(DELETED_FLAG_COLUMN_NAME);
            if (deletedFlag == null) {
                deletedFlag = false;
            }
            var versions = this.recordMap.computeIfAbsent(key, k -> new ArrayList<>());
            if (deletedFlag) {
                if (versions.isEmpty() || !versions.get(versions.size() - 1).isDeleted()) {
                    versions.add(MemoryRecord.builder()
                            .timestamp(timestamp)
                            .deleted(true)
                            .build());
                }
            } else {
                var old = this.getRecordMap(key, versions, timestamp);
                for (var it = newRecord.entrySet().iterator(); it.hasNext(); ) {
                    var entry = it.next();
                    if (old.containsKey(entry.getKey())) {
                        var oldValue = old.get(entry.getKey());
                        var type = typeMap.get(entry.getKey());
                        if (ColumnType.compare(type, oldValue, type, entry.getValue()) == 0) {
                            it.remove();
                        }
                    }
                }
                if (versions.isEmpty() || !newRecord.isEmpty()) {
                    versions.add(MemoryRecord.builder()
                            .timestamp(timestamp)
                            .deleted(deletedFlag)
                            .values(newRecord)
                            .build());
                }
            }
        }
    }

    private static Wal.Record.Builder writeRecord(TableSchema schema, Map<String, Object> record) {
        var ret = Wal.Record.newBuilder();
        for (var entry : record.entrySet()) {
            if (Objects.equals(entry.getKey(), DELETED_FLAG_COLUMN_NAME)) {
                ret.addColumns(ColumnTypeScalar.BOOL.toWal(-1, true));
            } else {
                var columnSchema = schema.getColumnSchemaByName(entry.getKey());
                if (columnSchema == null) {
                    throw new IllegalArgumentException("invalid column " + entry.getKey());
                }
                ret.addColumns(columnSchema.getType().toWal(columnSchema.getIndex(), entry.getValue()));
            }
        }
        return ret;
    }

    private Map<String, Object> getRecordMap(Object key, List<MemoryRecord> versions, long timestamp) {
        var ret = new HashMap<String, Object>();
        boolean deleted = false;
        for (var record : versions) {
            if (record.getTimestamp() <= timestamp) {
                if (record.isDeleted()) {
                    deleted = true;
                    ret.clear();
                } else {
                    deleted = false;
                    ret.putAll(record.getValues());
                }
            }
        }
        if (deleted) {
            ret.put(DELETED_FLAG_COLUMN_NAME, true);
        }
        ret.put(this.schema.getKeyColumn(), key);
        return ret;
    }

    @Override
    public Iterator<RecordResult> query(
            long timestamp,
            @NonNull Map<String, String> columns,
            List<OrderByDesc> orderBy,
            TableQueryFilter filter,
            boolean keepNone,
            boolean rawResult) {
        if (this.schema == null) {
            return Collections.emptyIterator();
        }
        this.schema.getColumnTypeMapping(columns); // check if all column names are valid
        if (orderBy != null) {
            for (var col : orderBy) {
                if (col == null) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "order by column should not be null");
                }
                var colSchema = this.schema.getColumnSchemaByName(col.getColumnName());
                if (colSchema == null) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "unknown orderBy column " + col);
                }
            }
        }
        if (filter != null) {
            this.checkFilter(filter);
        }
        var stream = this.recordMap.entrySet().stream()
                .map(entry -> this.getRecordMap(entry.getKey(), entry.getValue(), timestamp))
                .filter(record -> filter == null || this.match(filter, record));
        if (orderBy != null) {
            stream = stream.sorted((a, b) -> {
                for (var col : orderBy) {
                    var columnType = this.schema.getColumnSchemaByName(col.getColumnName()).getType();
                    var result = ColumnType.compare(
                            columnType,
                            a.get(col.getColumnName()),
                            columnType,
                            b.get(col.getColumnName()));
                    if (result != 0) {
                        if (col.isDescending()) {
                            return -result;
                        }
                        return result;
                    }
                }
                return 0;
            });
        }
        return stream.map(record -> this.toRecordResult(record, columns, keepNone)).iterator();
    }


    @Override
    public Iterator<RecordResult> scan(
            long timestamp,
            @NonNull Map<String, String> columns,
            String start,
            boolean startInclusive,
            String end,
            boolean endInclusive,
            boolean keepNone) {
        if (this.schema == null) {
            return Collections.emptyIterator();
        }
        if (this.recordMap.isEmpty()) {
            return Collections.emptyIterator();
        }

        var startKey = this.schema.getKeyColumnType().decode(start);
        var endKey = this.schema.getKeyColumnType().decode(end);
        if (startKey == null) {
            startKey = this.recordMap.firstKey();
            startInclusive = true;
        }
        if (endKey == null) {
            endKey = this.recordMap.lastKey();
            endInclusive = true;
        }
        //noinspection rawtypes,unchecked
        if (((Comparable) startKey).compareTo(endKey) > 0) {
            return Collections.emptyIterator();
        }
        var iterator = this.recordMap.subMap(startKey, startInclusive, endKey, endInclusive).entrySet().stream()
                .map(entry -> this.getRecordMap(entry.getKey(), entry.getValue(), timestamp))
                .iterator();
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public RecordResult next() {
                return toRecordResult(iterator.next(), columns, keepNone);
            }
        };
    }

    private boolean match(TableQueryFilter filter, Map<String, Object> record) {
        switch (filter.getOperator()) {
            case NOT:
                return !this.match((TableQueryFilter) filter.getOperands().get(0), record);
            case AND:
                for (Object operand : filter.getOperands()) {
                    if (!this.match((TableQueryFilter) operand, record)) {
                        return false;
                    }
                }
                return true;
            case OR:
                for (Object operand : filter.getOperands()) {
                    if (this.match((TableQueryFilter) operand, record)) {
                        return true;
                    }
                }
                return false;
            default:
                var result = ColumnType.compare(
                        this.getType(filter.getOperands().get(0)),
                        this.getValue(filter.getOperands().get(0), record),
                        this.getType(filter.getOperands().get(1)),
                        this.getValue(filter.getOperands().get(1), record));
                switch (filter.getOperator()) {
                    case EQUAL:
                        return result == 0;
                    case LESS:
                        return result < 0;
                    case LESS_EQUAL:
                        return result <= 0;
                    case GREATER:
                        return result > 0;
                    case GREATER_EQUAL:
                        return result >= 0;
                    default:
                        throw new IllegalArgumentException("Unexpected value: " + filter.getOperator());
                }
        }
    }

    private ColumnType getType(Object operand) {
        if (operand instanceof TableQueryFilter.Column) {
            var colName = ((TableQueryFilter.Column) operand).getName();
            var colSchema = this.schema.getColumnSchemaByName(colName);
            if (colSchema == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "invalid filter, unknown column " + colName);
            }
            return colSchema.getType();
        } else if (operand instanceof TableQueryFilter.Constant) {
            var type = ((TableQueryFilter.Constant) operand).getType();
            if (type == null) {
                throw new SwProcessException(ErrorType.DATASTORE, "invalid filter, constant type is null");
            }
            return type;
        } else if (operand instanceof TableQueryFilter) {
            return ColumnTypeScalar.BOOL;
        } else {
            throw new IllegalArgumentException("invalid operand type " + operand.getClass());
        }
    }

    private Object getValue(Object operand, Map<String, Object> record) {
        if (operand instanceof TableQueryFilter.Column) {
            return record.get(((TableQueryFilter.Column) operand).getName());
        } else if (operand instanceof TableQueryFilter.Constant) {
            return ((TableQueryFilter.Constant) operand).getValue();
        } else {
            throw new IllegalArgumentException("invalid operand type " + operand.getClass());
        }
    }

    private void checkFilter(TableQueryFilter filter) {
        switch (filter.getOperator()) {
            case NOT:
            case AND:
            case OR:
                for (var op : filter.getOperands()) {
                    this.checkFilter((TableQueryFilter) op);
                }
                break;
            case EQUAL:
            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
                this.checkComparability(filter.getOperands().stream()
                        .map(this::getType)
                        .collect(Collectors.toList()));
                break;
            default:
                throw new IllegalArgumentException("Unexpected operator: " + filter.getOperator());
        }
    }

    private void checkComparability(List<ColumnType> types) {
        types = types.stream().filter(t -> t != ColumnTypeScalar.UNKNOWN).collect(Collectors.toList());
        if (types.isEmpty()) {
            return;
        }
        ColumnType type1 = types.get(0);
        for (var type : types) {
            if (!type.isComparableWith(type1)) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        MessageFormat.format("invalid filter, can not compare {0} with {1}", type));
            }
        }
    }

    private RecordResult toRecordResult(Map<String, Object> record, Map<String, String> columnMapping,
            boolean keepNone) {
        var key = record.get(this.schema.getKeyColumn());
        if (record.get(DELETED_FLAG_COLUMN_NAME) != null) {
            return new RecordResult(key, true, null);
        }
        var r = new HashMap<String, Object>();
        for (var entry : columnMapping.entrySet()) {
            var column = entry.getKey();
            if (record.containsKey(column)) {
                var value = record.get(column);
                if (keepNone || value != null) {
                    r.put(entry.getValue(), value);
                }
            }
        }
        return new RecordResult(key, false, r);
    }

    private static Map<String, Object> decodeRecord(TableSchema schema, Map<String, Object> record) {
        if (record == null) {
            return null;
        }
        var ret = new HashMap<String, Object>();
        for (var entry : record.entrySet()) {
            var name = entry.getKey();
            var value = entry.getValue();
            var columnSchema = schema.getColumnSchemaByName(name);
            if (columnSchema == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "no schema found for column " + name);
            }
            var columnType = columnSchema.getType();
            try {
                ret.put(name, columnType.decode(value));
            } catch (Exception e) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        MessageFormat.format("failed to decode value {0} for column {1}",
                                value,
                                name),
                        e);
            }
        }
        return ret;
    }
}
