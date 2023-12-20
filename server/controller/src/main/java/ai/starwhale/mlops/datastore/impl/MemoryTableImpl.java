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

import ai.starwhale.mlops.datastore.Checkpoint;
import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnStatistics;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.MemoryTable;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.ParquetConfig;
import ai.starwhale.mlops.datastore.RecordResult;
import ai.starwhale.mlops.datastore.TableMeta;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.Wal;
import ai.starwhale.mlops.datastore.Wal.Checkpoint.OP;
import ai.starwhale.mlops.datastore.parquet.SwParquetReaderBuilder;
import ai.starwhale.mlops.datastore.parquet.SwParquetWriterBuilder;
import ai.starwhale.mlops.datastore.parquet.SwReadSupport;
import ai.starwhale.mlops.datastore.parquet.SwWriter;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.BoolValue;
import ai.starwhale.mlops.datastore.type.Float64Value;
import ai.starwhale.mlops.datastore.type.FloatValue;
import ai.starwhale.mlops.datastore.type.Int64Value;
import ai.starwhale.mlops.datastore.type.IntValue;
import ai.starwhale.mlops.datastore.type.StringValue;
import ai.starwhale.mlops.datastore.wal.WalManager;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

@Slf4j
public class MemoryTableImpl implements MemoryTable {

    private static final String REVISION_COLUMN_NAME = "^";

    static final String DELETED_FLAG_COLUMN_NAME = "-";

    static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("^[\\p{Alnum}_]+$");

    @Getter
    private final String tableName;

    private final WalManager walManager;
    private final StorageAccessService storageAccessService;
    private final String dataPathPrefix;
    private final SimpleDateFormat dataPathSuffixFormat = new SimpleDateFormat("yyMMddHHmmss.SSS");
    private final ParquetConfig parquetConfig;

    @Getter
    private TableSchema schema = new TableSchema();

    private final Map<String, ColumnStatistics> statisticsMap = new HashMap<>();

    @Getter
    private volatile long firstWalLogId = -1;

    private long lastWalLogId = -1;

    @Getter
    private long lastUpdateTime = 0;

    @Getter
    private volatile long lastRevision = 0;

    @Setter
    private boolean useTimestampAsRevision = false; // unittest only

    private static final long MIN_TIMESTAMP = 86400L * 1000 * 365 * 50;
    private static final long MAX_TIMESTAMP = 86400L * 1000 * 365 * 100;

    private final TreeMap<BaseValue, List<MemoryRecord>> recordMap = new TreeMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final Map<Long, Checkpoint> checkpoints = new ConcurrentHashMap<>();

    public MemoryTableImpl(
            String tableName,
            WalManager walManager,
            StorageAccessService storageAccessService,
            String dataPathPrefix,
            ParquetConfig parquetConfig
    ) {
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
                        if (this.schema.getKeyColumn() == null) {
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
                            this.lastRevision = metadata.getLastRevision();
                            metadata.getCheckpointsList()
                                    .forEach(cp -> this.checkpoints.put(cp.getRevision(), Checkpoint.from(cp)));
                        }
                        if (record == null) {
                            break;
                        }
                        var key = record.remove(this.schema.getKeyColumn());
                        this.statisticsMap.computeIfAbsent(this.schema.getKeyColumn(), k -> new ColumnStatistics())
                                .update(key);
                        var revision = (Int64Value) record.remove(REVISION_COLUMN_NAME);
                        var deletedFlag = (BoolValue) record.remove(DELETED_FLAG_COLUMN_NAME);
                        this.recordMap.computeIfAbsent(key, k -> new ArrayList<>())
                                .add(MemoryRecord.builder()
                                        .revision(this.normalizeRevision(revision.getValue()))
                                        .deleted(deletedFlag.isValue())
                                        .values(record)
                                        .build());
                        for (var entry : record.entrySet()) {
                            this.statisticsMap.computeIfAbsent(entry.getKey(), k -> new ColumnStatistics())
                                    .update(entry.getValue());
                        }
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
        var columnSchema = new HashMap<String, ColumnSchema>();
        this.lock(false);
        var lastRevision = this.lastRevision;
        var firstWalLogId = this.firstWalLogId;
        this.firstWalLogId = -1;
        try {
            try {
                try {
                    metadata = JsonFormat.printer().print(TableMeta.MetaData.newBuilder()
                            .setLastWalLogId(this.lastWalLogId)
                            .setLastUpdateTime(this.lastUpdateTime)
                            .setLastRevision(this.lastRevision)
                            .addAllCheckpoints(this.checkpoints.values().stream()
                                    .map(Checkpoint::toWalCheckpoint)
                                    .collect(Collectors.toList()))
                            .build());
                } catch (InvalidProtocolBufferException e) {
                    throw new SwProcessException(ErrorType.DATASTORE, "failed to print table meta", e);
                }
                int index = 0;
                for (var entry : new TreeMap<>(this.statisticsMap).entrySet()) {
                    columnSchema.put(entry.getKey(), entry.getValue().createSchema(entry.getKey(), index++));
                }
                var timestampColumnSchema = new ColumnSchema(REVISION_COLUMN_NAME, index++);
                timestampColumnSchema.setType(ColumnType.INT64);
                var deletedFlagColumnSchema = new ColumnSchema(DELETED_FLAG_COLUMN_NAME, index);
                deletedFlagColumnSchema.setType(ColumnType.BOOL);
                columnSchema.put(REVISION_COLUMN_NAME, timestampColumnSchema);
                columnSchema.put(DELETED_FLAG_COLUMN_NAME, deletedFlagColumnSchema);
            } finally {
                this.unlock(false);
            }
            var currentSnapshots = this.storageAccessService.list(this.dataPathPrefix).collect(Collectors.toList());
            var path = this.dataPathPrefix + this.dataPathSuffixFormat.format(new Date());
            SwWriter.writeWithBuilder(
                    new SwParquetWriterBuilder(this.storageAccessService,
                            columnSchema,
                            this.schema.toJsonString(),
                            metadata,
                            path,
                            this.parquetConfig),
                    new Iterator<>() {
                        private final LinkedList<Map<String, BaseValue>> candidates = new LinkedList<>();

                        private BaseValue lastKey;

                        {
                            getNext();
                        }

                        @Override
                        public boolean hasNext() {
                            return !this.candidates.isEmpty();
                        }

                        @Override
                        public Map<String, BaseValue> next() {
                            var ret = candidates.poll();
                            if (candidates.isEmpty()) {
                                this.getNext();
                            }
                            return ret;
                        }

                        private void getNext() {
                            MemoryTableImpl.this.lock(false);
                            try {
                                NavigableMap<BaseValue, List<MemoryRecord>> target;
                                if (this.lastKey == null) {
                                    target = MemoryTableImpl.this.recordMap;
                                } else {
                                    target = MemoryTableImpl.this.recordMap.tailMap(this.lastKey, false);
                                }
                                int count = 0;
                                for (var entry : target.entrySet()) {
                                    this.lastKey = entry.getKey();
                                    for (var record : entry.getValue()) {
                                        if (record.getRevision() > lastRevision) {
                                            break;
                                        }
                                        var recordMap = new HashMap<String, BaseValue>();
                                        if (record.getValues() != null) {
                                            recordMap.putAll(record.getValues());
                                        }
                                        recordMap.put(MemoryTableImpl.this.schema.getKeyColumn(), entry.getKey());
                                        recordMap.put(REVISION_COLUMN_NAME, new Int64Value(record.getRevision()));
                                        recordMap.put(DELETED_FLAG_COLUMN_NAME, BaseValue.valueOf(record.isDeleted()));
                                        this.candidates.add(recordMap);
                                    }
                                    if (++count == 1000) {
                                        break;
                                    }
                                }
                            } finally {
                                MemoryTableImpl.this.unlock(false);
                            }
                        }
                    });
            for (var snapshot : currentSnapshots) {
                try {
                    if (!snapshot.equals(path)) {
                        this.storageAccessService.delete(snapshot);
                    }
                } catch (IOException e) {
                    log.warn("fail to delete {}", snapshot, e);
                }
            }
        } catch (Throwable e) {
            this.firstWalLogId = firstWalLogId;
            log.error("fail to save table:{}, error:{}", this.tableName, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void lock(boolean forRead) {
        if (forRead) {
            this.readLock.lock();
        } else {
            this.writeLock.lock();
        }
    }

    @Override
    public void unlock(boolean forRead) {
        if (forRead) {
            this.readLock.unlock();
        } else {
            this.writeLock.unlock();
        }
    }

    @Override
    public void updateFromWal(Wal.WalEntry entry) {
        if (entry.getId() <= this.lastWalLogId) {
            return;
        }

        if (tryUpdateCheckpointFromWal(entry)) {
            return;
        }

        if (entry.hasTableSchema()) {
            this.schema.update(entry.getTableSchema());
        }
        var recordList = entry.getRecordsList();
        if (!recordList.isEmpty()) {
            this.insertRecords(entry.getRevision(),
                    recordList.stream()
                            .map(r -> WalRecordDecoder.decodeRecord(this.schema, r))
                            .collect(Collectors.toList()));
        }
        if (this.firstWalLogId < 0) {
            this.firstWalLogId = entry.getId();
        }
        this.lastWalLogId = entry.getId();
    }

    private boolean tryUpdateCheckpointFromWal(Wal.WalEntry entry) {
        if (!Wal.WalEntry.Type.CHECKPOINT.equals(entry.getEntryType())) {
            return false;
        }
        var checkpoint = entry.getCheckpoint();
        if (OP.DELETE.equals(checkpoint.getOp())) {
            this.checkpoints.remove(checkpoint.getRevision());
            return true;
        }

        var cp = Checkpoint.builder()
                .revision(checkpoint.getRevision())
                .timestamp(checkpoint.getTimestamp())
                .userData(checkpoint.getUserData())
                .build();
        this.checkpoints.putIfAbsent(cp.getRevision(), cp);
        return true;
    }

    @Override
    public long update(TableSchemaDesc schema, @NonNull List<Map<String, Object>> records) {
        var decodedRecords = new ArrayList<Map<String, BaseValue>>();
        String keyColumn;
        if (schema.getKeyColumn() == null) {
            if (this.schema.getKeyColumn() == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "no key column in schema " + schema);
            }
            keyColumn = this.schema.getKeyColumn();
        } else {
            keyColumn = schema.getKeyColumn();
        }
        for (var record : records) {
            Map<String, BaseValue> decodedRecord;
            try {
                decodedRecord = RecordDecoder.decodeRecord(schema, record);
            } catch (Exception e) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "failed to decode record " + record, e);
            }
            var key = decodedRecord.get(keyColumn);
            if (key == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "no key column in record " + record);
            }
            if (key instanceof List || key instanceof Map) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "invalid key type " + key.getClass() + ". record: " + record);
            }
            decodedRecords.add(decodedRecord);
        }
        return this.updateWithObject(schema, decodedRecords);
    }

    @Override
    public long updateWithObject(TableSchemaDesc schema, @NonNull List<Map<String, BaseValue>> records) {
        var revision = this.useTimestampAsRevision ? System.currentTimeMillis() : this.lastRevision + 1;
        var logEntryBuilder = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName(this.tableName)
                .setRevision(revision);
        var logSchemaBuilder = this.schema.getDiff(schema);
        if (logSchemaBuilder != null) {
            logEntryBuilder.setTableSchema(logSchemaBuilder);
        }
        TableSchema recordSchema;
        if (logSchemaBuilder == null) {
            recordSchema = this.schema;
        } else {
            recordSchema = new TableSchema(this.schema);
            recordSchema.update(logSchemaBuilder.build());
        }
        for (var record : records) {
            logEntryBuilder.addRecords(WalRecordEncoder.encodeRecord(recordSchema, record));
        }
        this.lastWalLogId = this.walManager.append(logEntryBuilder);
        if (this.firstWalLogId < 0) {
            this.firstWalLogId = this.lastWalLogId;
        }
        this.lastUpdateTime = System.currentTimeMillis();
        this.schema = recordSchema;
        if (!records.isEmpty()) {
            this.insertRecords(revision, records);
        }
        return revision;
    }

    private long normalizeRevision(long revision) {
        return revision >= MIN_TIMESTAMP && !this.useTimestampAsRevision ? revision - MAX_TIMESTAMP : revision;
    }

    private void insertRecords(long revision, List<Map<String, BaseValue>> records) {
        revision = this.normalizeRevision(revision);
        if (revision > this.lastRevision) {
            this.lastRevision = revision;
        }
        for (var record : records) {
            var newRecord = new HashMap<>(record);
            var key = newRecord.remove(this.schema.getKeyColumn());
            this.statisticsMap.computeIfAbsent(this.schema.getKeyColumn(), k -> new ColumnStatistics()).update(key);
            var deletedFlag = newRecord.remove(DELETED_FLAG_COLUMN_NAME) != null;
            var versions = this.recordMap.computeIfAbsent(key, k -> new ArrayList<>());
            if (deletedFlag) {
                if (versions.isEmpty() || !versions.get(versions.size() - 1).isDeleted()) {
                    versions.add(MemoryRecord.builder()
                            .revision(revision)
                            .deleted(true)
                            .build());
                }
            } else {
                var old = this.getRecordMap(key, versions, revision);
                if (old != null) {
                    for (var it = newRecord.entrySet().iterator(); it.hasNext(); ) {
                        var entry = it.next();
                        if (old.containsKey(entry.getKey())) {
                            var oldValue = old.get(entry.getKey());
                            if (BaseValue.compare(oldValue, entry.getValue()) == 0) {
                                it.remove();
                            }
                        }
                    }
                }
                if (versions.isEmpty() || !newRecord.isEmpty()) {
                    versions.add(MemoryRecord.builder()
                            .revision(revision)
                            .values(newRecord)
                            .build());
                }
                for (var entry : newRecord.entrySet()) {
                    this.statisticsMap.computeIfAbsent(entry.getKey(), k -> new ColumnStatistics())
                            .update(entry.getValue());
                }
            }
        }
    }


    private Map<String, BaseValue> getRecordMap(BaseValue key, List<MemoryRecord> versions, long revision) {
        revision = this.normalizeRevision(revision);
        var ret = new HashMap<String, BaseValue>();
        boolean deleted = false;
        boolean hasVersion = false;
        for (var record : versions) {
            if (record.getRevision() <= revision) {
                // record may be empty, use hasVersion to mark if there is a record
                hasVersion = true;
                if (record.isDeleted()) {
                    deleted = true;
                    ret.clear();
                } else {
                    deleted = false;
                    ret.putAll(record.getValues());
                }
            }
        }
        if (!hasVersion) {
            return null;
        }
        ret.put(this.schema.getKeyColumn(), key);
        if (deleted) {
            ret.put(DELETED_FLAG_COLUMN_NAME, BoolValue.TRUE);
        }
        return ret;
    }

    @Override
    public Iterator<RecordResult> query(
            long revision,
            @NonNull Map<String, String> columns,
            List<OrderByDesc> orderBy,
            TableQueryFilter filter,
            boolean keepNone
    ) {
        if (this.schema.getKeyColumn() == null) {
            return Collections.emptyIterator();
        }
        if (orderBy != null) {
            for (var col : orderBy) {
                if (col == null) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "order by column should not be null");
                }
                var idx = this.schema.getColumnIndexByName(col.getColumnName());
                if (idx == null) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "unknown orderBy column " + col);
                }
            }
        }
        if (filter != null) {
            this.checkFilter(filter);
        }
        var stream = this.recordMap.entrySet().stream()
                .map(entry -> this.getRecordMap(entry.getKey(), entry.getValue(), revision))
                .filter(record -> record != null && (filter == null || this.match(filter, record)));
        if (orderBy != null) {
            stream = stream.sorted((a, b) -> {
                for (var col : orderBy) {
                    var result = BaseValue.compare(
                            a.get(col.getColumnName()),
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
            long revision,
            @NonNull Map<String, String> columns,
            String start,
            String startType,
            boolean startInclusive,
            String end,
            String endType,
            boolean endInclusive,
            boolean keepNone
    ) {
        if (this.schema.getKeyColumn() == null) {
            return Collections.emptyIterator();
        }
        if (this.recordMap.isEmpty()) {
            return Collections.emptyIterator();
        }
        BaseValue startKey;
        BaseValue endKey;
        if (start == null) {
            startKey = this.recordMap.firstKey();
            startInclusive = true;
        } else {
            ColumnType startKeyType;
            if (startType == null) {
                startKeyType = this.schema.getKeyColumnSchema().getType();
            } else {
                try {
                    startKeyType = ColumnType.valueOf(startType);
                } catch (Exception e) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "invalid startType " + startType, e);
                }
            }
            try {
                startKey = RecordDecoder.decodeScalar(startKeyType, start);
            } catch (Exception e) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "invalid start " + start, e);
            }
        }
        if (end == null) {
            endKey = this.recordMap.lastKey();
            endInclusive = true;
        } else {
            ColumnType endKeyType;
            if (endType == null) {
                endKeyType = this.schema.getKeyColumnSchema().getType();
            } else {
                try {
                    endKeyType = ColumnType.valueOf(endType);
                } catch (Exception e) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "invalid endType " + endType, e);
                }
            }
            try {
                endKey = RecordDecoder.decodeScalar(endKeyType, end);
            } catch (Exception e) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "invalid end " + end, e);
            }
        }
        if (startKey.compareTo(endKey) > 0) {
            return Collections.emptyIterator();
        }
        var iterator = this.recordMap.subMap(startKey, startInclusive, endKey, endInclusive).entrySet().stream()
                .map(entry -> this.getRecordMap(entry.getKey(), entry.getValue(), revision))
                .filter(Objects::nonNull)
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

    private boolean match(TableQueryFilter filter, Map<String, BaseValue> record) {
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
                var v1 = this.getValue(filter.getOperands().get(0), record);
                var v2 = this.getValue(filter.getOperands().get(1), record);
                if (v1 instanceof StringValue || v2 instanceof StringValue) {
                    if (v1 instanceof StringValue) {
                        v1 = this.convertStringToOtherType((StringValue) v1, v2);
                    } else {
                        v2 = this.convertStringToOtherType((StringValue) v2, v1);
                    }
                }
                var result = BaseValue.compare(v1, v2);
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

    private BaseValue convertStringToOtherType(StringValue value, BaseValue other) {
        var v = value.getValue();
        if (other instanceof BoolValue) {
            if (v.equalsIgnoreCase("true")) {
                return BoolValue.TRUE;
            }
            if (v.equalsIgnoreCase("false")) {
                return BoolValue.FALSE;
            }
        }
        try {
            if (other instanceof IntValue) {
                return new Int64Value(Long.parseLong(v));
            }
            if (other instanceof FloatValue) {
                return new Float64Value(Double.parseDouble(v));
            }
        } catch (NumberFormatException e) {
            // just ignore it
        }
        return value;

    }

    private BaseValue getValue(Object operand, Map<String, BaseValue> record) {
        if (operand instanceof TableQueryFilter.Column) {
            return record.get(((TableQueryFilter.Column) operand).getName());
        } else if (operand instanceof TableQueryFilter.Constant) {
            return BaseValue.valueOf(((TableQueryFilter.Constant) operand).getValue());
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
                break;
            default:
                throw new IllegalArgumentException("Unexpected operator: " + filter.getOperator());
        }
    }

    private RecordResult toRecordResult(
            Map<String, BaseValue> record,
            Map<String, String> columnMapping,
            boolean keepNone
    ) {
        var key = record.get(this.schema.getKeyColumn());
        if (record.get(DELETED_FLAG_COLUMN_NAME) != null) {
            return new RecordResult(key, true, null);
        }
        var r = new HashMap<String, BaseValue>();
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

    public Map<String, ColumnStatistics> getColumnStatistics(Map<String, String> columnMapping) {
        return this.statisticsMap.entrySet().stream()
                .filter(entry -> columnMapping.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> columnMapping.get(entry.getKey()),
                        Entry::getValue));
    }

    @Override
    public Checkpoint createCheckpoint(String userData) {
        // check if the checkpoint already exists
        // we do not support multiple checkpoints for the same revision (even with different user data)
        var revision = this.lastRevision;
        if (this.checkpoints.containsKey(revision)) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "checkpoint already exists for revision " + revision);
        }

        var timestamp = System.currentTimeMillis();
        var cp = Checkpoint.builder()
                .revision(revision)
                .timestamp(timestamp)
                .userData(userData)
                .build();

        var entry = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.CHECKPOINT)
                .setTableName(this.tableName)
                .setCheckpoint(Wal.Checkpoint.newBuilder()
                        .setOp(OP.CREATE)
                        .setTimestamp(timestamp)
                        .setRevision(revision)
                        .setUserData(userData));
        this.lastWalLogId = this.walManager.append(entry);
        if (this.firstWalLogId < 0) {
            this.firstWalLogId = this.lastWalLogId;
        }
        this.checkpoints.put(revision, cp);

        return cp;
    }

    @Override
    public List<Checkpoint> getCheckpoints() {
        return new ArrayList<>(this.checkpoints.values());
    }

    @Override
    public void deleteCheckpoint(long revision) {
        if (!this.checkpoints.containsKey(revision)) {
            throw new SwNotFoundException(ResourceType.DATASTORE, "checkpoint not found for revision " + revision);
        }

        var entry = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.CHECKPOINT)
                .setTableName(this.tableName)
                .setCheckpoint(Wal.Checkpoint.newBuilder()
                        .setOp(OP.DELETE)
                        .setRevision(revision));
        this.lastWalLogId = this.walManager.append(entry);
        if (this.firstWalLogId < 0) {
            this.firstWalLogId = this.lastWalLogId;
        }
        this.checkpoints.remove(revision);
    }
}
