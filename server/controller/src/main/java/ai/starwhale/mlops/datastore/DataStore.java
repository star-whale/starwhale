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
import ai.starwhale.mlops.datastore.impl.RecordEncoder;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.wal.WalManager;
import ai.starwhale.mlops.domain.upgrade.rollup.OrderedRollingUpdateStatusListener;
import ai.starwhale.mlops.domain.upgrade.rollup.aspectcut.WriteOperation;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

@Slf4j
@Component
public class DataStore implements OrderedRollingUpdateStatusListener {

    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[\\p{Alnum}-_/: ]*$");

    private static final String PATH_SEPARATOR = "_._";

    public static final Integer QUERY_LIMIT = 1000;

    private WalManager walManager;
    private final StorageAccessService storageAccessService;

    private final Map<String, SoftReference<MemoryTable>> tables = new ConcurrentHashMap<>();
    private final Set<MemoryTable> dirtyTables = new HashSet<>();
    private final String snapshotRootPath;
    private final ParquetConfig parquetConfig;

    private final Set<String> loadingTables = new HashSet<>();

    private final BlockingQueue<Object> updateHandle = new LinkedBlockingQueue<>();
    private final DumpThread dumpThread;

    private final String dataRootPath;
    private final int walMaxFileSize;

    private final Path walLocalCacheDir;

    private final int ossMaxAttempts;

    public DataStore(StorageAccessService storageAccessService,
            @Value("${sw.datastore.wal-max-file-size}") int walMaxFileSize,
            @Value("#{T(java.nio.file.Paths).get('${sw.datastore.wal-local-cache-dir:wal_cache}')}")
            Path walLocalCacheDir,
            @Value("${sw.datastore.oss-max-attempts:5}") int ossMaxAttempts,
            @Value("${sw.datastore.data-root-path:}") String dataRootPath,
            @Value("${sw.datastore.dump-interval:1h}") String dumpInterval,
            @Value("${sw.datastore.min-no-update-period:4h}") String minNoUpdatePeriod,
            @Value("${sw.datastore.min-wal-id-gap:1000}") int minWalIdGap,
            @Value("${sw.datastore.parquet.compression-codec:SNAPPY}") String compressionCodec,
            @Value("${sw.datastore.parquet.row-group-size:128MB}") String rowGroupSize,
            @Value("${sw.datastore.parquet.page-size:1MB}") String pageSize,
            @Value("${sw.datastore.parquet.page-row-count-limit:20000}") int pageRowCountLimit) {
        this.storageAccessService = storageAccessService;
        if (!dataRootPath.isEmpty() && !dataRootPath.endsWith("/")) {
            dataRootPath += "/";
        }
        this.dataRootPath = dataRootPath;
        this.snapshotRootPath = dataRootPath + "snapshot/";
        this.walMaxFileSize = walMaxFileSize;
        this.walLocalCacheDir = walLocalCacheDir;
        this.ossMaxAttempts = ossMaxAttempts;
        this.parquetConfig = new ParquetConfig();
        this.parquetConfig.setCompressionCodec(CompressionCodec.valueOf(compressionCodec));
        this.parquetConfig.setRowGroupSize(DataSize.parse(rowGroupSize).toBytes());
        this.parquetConfig.setPageSize((int) DataSize.parse(pageSize).toBytes());
        this.parquetConfig.setPageRowCountLimit(pageRowCountLimit);
        this.dumpThread = new DumpThread(DurationStyle.detectAndParse(dumpInterval).toMillis(),
                DurationStyle.detectAndParse(minNoUpdatePeriod).toMillis(),
                minWalIdGap);
    }

    public DataStore start() {
        synchronized (dumpThread) {
            if (null != this.walManager) {
                return this;
            }
            try {
                this.walManager = new WalManager(
                        this.storageAccessService,
                        this.walMaxFileSize,
                        this.walLocalCacheDir,
                        this.dataRootPath + "wal/",
                        this.ossMaxAttempts
                );
            } catch (IOException e) {
                throw new SwProcessException(ErrorType.DATASTORE, "failed to create wal manager", e);
            }
            var it = this.walManager.readAll();
            log.info("Start to load wal log...");
            while (it.hasNext()) {
                var entry = it.next();
                var table = this.getTable(entry.getTableName(), true, true);
                //noinspection ConstantConditions
                table.updateFromWal(entry);
                if (table.getFirstWalLogId() >= 0) {
                    synchronized (this.dirtyTables) {
                        this.dirtyTables.add(table);
                    }
                }
            }
            log.info("Finished load wal log...");
            this.dumpThread.start();
        }
        return this;
    }

    public void terminate() {
        this.dumpThread.terminate();
        this.walManager.terminate();
    }

    public List<String> list(Set<String> prefixes) {
        return prefixes.stream()
                .flatMap(prefix -> {
                    try {
                        return Stream.concat(
                                this.storageAccessService.list(this.snapshotRootPath + prefix)
                                        .map(path -> {
                                            path = path.substring(this.snapshotRootPath.length());
                                            var index = path.indexOf(PATH_SEPARATOR);
                                            if (index < 0) {
                                                return path;
                                            } else {
                                                return path.substring(0, index);
                                            }
                                        }),
                                tables.keySet().stream().filter(name -> name.startsWith(prefix))
                        );
                    } catch (IOException e) {
                        throw new SwProcessException(ErrorType.DATASTORE, "failed to list", e);
                    }
                })
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    @WriteOperation
    public String update(String tableName,
            TableSchemaDesc schema,
            List<Map<String, Object>> records) {
        if (schema != null && schema.getColumnSchemaList() != null) {
            for (var col : schema.getColumnSchemaList()) {
                if (col.getName() != null && !COLUMN_NAME_PATTERN.matcher(col.getName()).matches()) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "invalid column name " + col.getName()
                                    + ". only alphabets, digits, hyphen(-), underscore(_), "
                                    + "slash(/), colon(:), and space are allowed.");
                }
            }
        }
        // this line would fail and cause bugs when updateHandler has Integer.MAX_VALUE elements,
        // but let's assume that would never happen
        this.updateHandle.offer(new Object());
        var table = this.getTable(tableName, true, true);
        //noinspection ConstantConditions
        table.lock(false);
        try {
            var ts = table.update(schema, records);
            synchronized (this.dirtyTables) {
                this.dirtyTables.add(table);
            }
            return Long.toString(ts);
        } finally {
            this.updateHandle.poll();
            synchronized (updateHandle) {
                updateHandle.notifyAll();
            }
            table.unlock(false);
        }

    }

    public void flush() {
        this.walManager.flush();
    }

    public int migration(DataStoreMigrationRequest req) {
        if (req.getSrcTableName().equals(req.getTargetTableName())) {
            throw new SwValidationException(
                    ValidSubject.DATASTORE, "Source and target table names cannot be the same");
        }
        var srcTable = this.getTable(req.getSrcTableName(), false, false);
        var targetTable = this.getTable(req.getTargetTableName(), true, req.isCreateNonExistingTargetTable());
        if (srcTable == null || targetTable == null) {
            throw new SwValidationException(ValidSubject.DATASTORE, "Source or target table doesn't exist");
        }
        this.updateHandle.offer(new Object());
        srcTable.lock(true);
        targetTable.lock(false);
        try {
            int skipCount = req.getStart();
            if (skipCount < 0) {
                skipCount = 0;
            }

            var schema = srcTable.getSchema();
            var columns = this.getColumnAliases(schema, req.getColumns());
            var records = new ArrayList<Map<String, BaseValue>>();
            var revision = req.getSrcRevision();
            if (revision == 0) {
                revision = srcTable.getLastRevision();
            }
            var iterator = srcTable.query(
                    revision,
                    columns,
                    null,
                    req.getFilter(),
                    true);
            while (iterator.hasNext() && skipCount > 0) {
                iterator.next();
                --skipCount;
            }
            while (iterator.hasNext()) {
                var r = iterator.next();
                if (r.isDeleted()) {
                    continue;
                }
                records.add(r.getValues());
            }
            if (!records.isEmpty()) {
                targetTable.updateWithObject(srcTable.getSchema().toTableSchemaDesc(), records);
            }
            return records.size();
        } finally {
            this.updateHandle.poll();
            synchronized (updateHandle) {
                updateHandle.notifyAll();
            }
            targetTable.unlock(false);
            srcTable.unlock(true);
        }
    }

    public RecordList query(DataStoreQueryRequest req) {
        var table = this.getTable(req.getTableName(), req.isIgnoreNonExistingTable(), false);
        if (table == null) {
            return new RecordList(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), null, null);
        }
        table.lock(true);
        try {
            int skipCount = req.getStart();
            if (skipCount < 0) {
                skipCount = 0;
            }
            int limitCount = req.getLimit();
            if (limitCount > QUERY_LIMIT) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "limit must be less or equal to " + QUERY_LIMIT + ". request=" + req);
            }
            if (limitCount < 0) {
                limitCount = QUERY_LIMIT;
            }
            var schema = table.getSchema();
            var columns = this.getColumnAliases(schema, req.getColumns());
            var results = new ArrayList<RecordResult>();
            var revision = req.getRevision();
            if (revision == 0) {
                revision = table.getLastRevision();
            }
            var iterator = table.query(
                    revision,
                    columns,
                    req.getOrderBy(),
                    req.getFilter(),
                    req.isKeepNone());
            while (iterator.hasNext() && skipCount > 0) {
                iterator.next();
                --skipCount;
            }
            while (iterator.hasNext() && limitCount > 0) {
                var r = iterator.next();
                if (r.isDeleted()) {
                    continue;
                }
                results.add(r);
                --limitCount;
            }
            String lastKey;
            String lastKeyType;
            if (results.isEmpty()) {
                lastKey = null;
                lastKeyType = null;
            } else {
                var last = results.get(results.size() - 1).getKey();
                lastKey = (String) BaseValue.encode(
                        last,
                        req.isRawResult(),
                        false);
                lastKeyType = BaseValue.getColumnType(last).name();
            }
            Map<String, ColumnSchema> columnSchemaMap;
            if (!req.isEncodeWithType()) {
                columnSchemaMap = table.getSchema().getColumnSchemaList().stream()
                        .filter(col -> columns.containsKey(col.getName()))
                        .map(col -> {
                            var ret = new ColumnSchema(col);
                            ret.setName(columns.get(col.getName()));
                            return ret;
                        })
                        .collect(Collectors.toMap(ColumnSchema::getName, Function.identity()));
            } else {
                columnSchemaMap = null;
            }
            var records = results.stream()
                    .map(r -> RecordEncoder.encodeRecord(r.getValues(), req.isRawResult(), req.isEncodeWithType()))
                    .collect(Collectors.toList());
            return new RecordList(columnSchemaMap,
                    table.getColumnStatistics(columns).entrySet().stream()
                            .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().toColumnHintsDesc())),
                    records,
                    lastKey,
                    lastKeyType);
        } finally {
            table.unlock(true);
        }
    }

    public RecordList scan(DataStoreScanRequest req) {
        var limit = req.getLimit();
        if (limit > QUERY_LIMIT) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "limit must be less or equal to " + QUERY_LIMIT + ". request=" + req);
        }
        if (limit < 0) {
            limit = QUERY_LIMIT;
        }
        int finalLimit = limit;
        return scanRecords(req, new ResultResolver<>() {
                    @Override
                    public RecordList apply(
                            List<TableMeta> tables,
                            Map<String, ColumnSchema> columnSchemaMap,
                            List<TableRecords> records
                    ) {
                        BaseValue lastKey = null;
                        List<Map<String, Object>> ret = new ArrayList<>();
                        while (!records.isEmpty() && ret.size() < finalLimit) {
                            lastKey = Collections.min(records, (a, b) -> {
                                var x = a.record.getKey();
                                var y = b.record.getKey();
                                return x.compareTo(y);
                            }).record.getKey();
                            Map<String, Object> record = null;
                            for (var r : records) {
                                if (r.record.getKey().equals(lastKey)) {
                                    if (r.record.isDeleted()) {
                                        record = null;
                                    } else {
                                        if (record == null) {
                                            record = new HashMap<>();
                                        }
                                        record.putAll(
                                                RecordEncoder.encodeRecord(r.record.getValues(),
                                                        req.isRawResult(),
                                                        req.isEncodeWithType()));
                                    }
                                    if (r.iterator.hasNext()) {
                                        r.record = r.iterator.next();
                                    } else {
                                        r.record = null;
                                    }
                                }
                            }
                            if (record != null) {
                                if (!req.isKeepNone()) {
                                    record.entrySet().removeIf(x -> x.getValue() == null);
                                }
                                ret.add(record);
                            }
                            records.removeIf(r -> r.record == null);
                        }
                        var columnStatistics = new HashMap<String, ColumnStatistics>();
                        for (var table : tables) {
                            table.table.getColumnStatistics(table.columns).forEach((k, v) ->
                                    columnStatistics.computeIfAbsent(k, x -> new ColumnStatistics()).merge(v));
                        }
                        return new RecordList(
                                columnSchemaMap,
                                columnStatistics.entrySet().stream().collect(
                                        Collectors.toMap(Entry::getKey, entry -> entry.getValue().toColumnHintsDesc())),
                                ret,
                                (String) BaseValue.encode(lastKey, false, false),
                                BaseValue.getColumnType(lastKey).name()
                        );
                    }

                    @Override
                    public boolean stop(int recordSize) {
                        return recordSize >= finalLimit;
                    }

                    @Override
                    public RecordList empty() {
                        return new RecordList(
                                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), null, null);
                    }
                }
        );
    }

    public KeyRangeList scanKeyRange(DataStoreScanRangeRequest req) {
        return scanRecords(req, new ResultResolver<>() {
                    @Override
                    public KeyRangeList apply(
                            List<TableMeta> tables,
                            Map<String, ColumnSchema> columnSchemaMap,
                            List<TableRecords> records
                    ) {

                        BaseValue lastKey;
                        List<BaseValue> keys = new ArrayList<>();
                        while (!records.isEmpty()) {
                            lastKey = Collections.min(records, (a, b) -> {
                                var x = a.record.getKey();
                                var y = b.record.getKey();
                                return x.compareTo(y);
                            }).record.getKey();
                            for (var r : records) {
                                if (r.record.getKey().equals(lastKey)) {
                                    if (!r.record.isDeleted() && !keys.contains(r.record.getKey())) {
                                        keys.add(r.record.getKey());
                                    }
                                    if (r.iterator.hasNext()) {
                                        r.record = r.iterator.next();
                                    } else {
                                        r.record = null;
                                    }
                                }
                            }

                            records.removeIf(r -> r.record == null);
                        }

                        var ranges = new ArrayList<KeyRangeList.Range>();
                        var batchSize = req.getRangeInfo().getBatchSize();
                        var index = 0;
                        while (index < keys.size()) {
                            if (index + batchSize < keys.size()) {
                                var startKey = keys.get(index);
                                var start = (String) startKey.encode(false, false);
                                var startType = startKey.getColumnType().name();

                                var endKey = keys.get(index + batchSize);
                                var end = (String) endKey.encode(false, false);
                                var endType = endKey.getColumnType().name();
                                ranges.add(KeyRangeList.Range.builder()
                                        .start(start)
                                        .startType(startType)
                                        .startInclusive(true)
                                        .end(end)
                                        .endType(endType)
                                        .endInclusive(false)
                                        .size(batchSize)
                                        .build());
                                index += batchSize;
                            } else {
                                var startKey = keys.get(index);
                                var start = (String) startKey.encode(false, false);
                                var startType = startKey.getColumnType().name();

                                var endKey = keys.get(keys.size() - 1);
                                var end = (String) endKey.encode(false, false);
                                var endType = endKey.getColumnType().name();
                                ranges.add(KeyRangeList.Range.builder()
                                        .start(start)
                                        .startType(startType)
                                        .startInclusive(true)
                                        .end(StringUtils.hasText(req.getEnd()) ? end : null)
                                        .endType(endType)
                                        .endInclusive(StringUtils.hasText(req.getEnd()))
                                        .size(keys.size() - index)
                                        .build());
                                index = keys.size();
                            }
                        }
                        return new KeyRangeList(ranges);
                    }

                    @Override
                    public KeyRangeList empty() {
                        return new KeyRangeList(Collections.emptyList());
                    }
                }
        );
    }

    static class TableMeta {

        String tableName;
        long revision;
        MemoryTable table;
        TableSchema schema;
        Map<String, String> columns;
        Map<String, ColumnSchema> columnSchemaMap;
        boolean keepNone;
    }

    static class TableRecords {

        TableMeta meta;
        Iterator<RecordResult> iterator;
        RecordResult record;
    }

    @FunctionalInterface
    public interface ResultResolver<R> {

        /**
         * Applies this function to the given arguments.
         *
         * @param tables table meta data
         * @param columnSchemaMap column schema map
         * @param recordIter records iterator
         * @return r the result
         */
        R apply(
                List<TableMeta> tables,
                Map<String, ColumnSchema> columnSchemaMap,
                List<TableRecords> recordIter
        );

        default R empty() {
            return null;
        }

        default boolean stop(int resultSize) {
            return false;
        }
    }

    private <R> R scanRecords(DataStoreScanRequest req, ResultResolver<R> resultResolver) {
        var tablesToLock =
                req.getTables()
                        .stream()
                        .map(DataStoreScanRequest.TableInfo::getTableName)
                        .sorted() // prevent deadlock
                        .map(i -> getTable(i, req.isIgnoreNonExistingTable(), false))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        for (var table : tablesToLock) {
            table.lock(true);
        }
        try {
            var tables = req.getTables().stream().map(info -> {
                var ret = new TableMeta();
                ret.tableName = info.getTableName();
                ret.table = this.getTable(info.getTableName(), req.isIgnoreNonExistingTable(), false);
                if (ret.table == null) {
                    return null;
                }
                if (info.getRevision() > 0) {
                    ret.revision = info.getRevision();
                } else if (req.getRevision() > 0) {
                    ret.revision = req.getRevision();
                } else {
                    ret.revision = ret.table.getLastRevision();
                }
                ret.schema = ret.table.getSchema();
                ret.columns = this.getColumnAliases(ret.schema, info.getColumns());
                if (info.getColumnPrefix() != null) {
                    ret.columns = ret.columns.entrySet().stream()
                            .collect(Collectors.toMap(Entry::getKey,
                                    entry -> info.getColumnPrefix() + entry.getValue()));
                }
                ret.columnSchemaMap = ret.schema.getColumnSchemaList().stream()
                        .filter(c -> ret.columns.containsKey(c.getName()))
                        .map(c -> {
                            var schema = new ColumnSchema(c);
                            schema.setName(ret.columns.get(c.getName()));
                            return schema;
                        })
                        .collect(Collectors.toMap(ColumnSchema::getName, Function.identity()));
                ret.keepNone = info.isKeepNone();
                return ret;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            if (tables.isEmpty()) {
                return resultResolver.empty();
            }
            Map<String, ColumnSchema> columnSchemaMap;
            if (req.isEncodeWithType()) {
                columnSchemaMap = null;
            } else {
                columnSchemaMap = new HashMap<>();
                for (var table : tables) {
                    if (!table.schema.getKeyColumnSchema().isSameType(tables.get(0).schema.getKeyColumnSchema())) {
                        throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                MessageFormat.format(
                                        "conflicting key column type. {0}: key={1}, type={2}, {3}: key={4}, type={5}",
                                        tables.get(0).tableName,
                                        tables.get(0).schema.getKeyColumn(),
                                        tables.get(0).schema.getKeyColumnSchema(),
                                        table.tableName,
                                        table.schema.getKeyColumn(),
                                        table.schema.getKeyColumnSchema()));
                    }
                    for (var entry : table.columnSchemaMap.entrySet()) {
                        var columnName = entry.getKey();
                        var columnSchema = entry.getValue();
                        var old = columnSchemaMap.putIfAbsent(columnName, columnSchema);
                        if (old != null && !old.isSameType(columnSchema)) {
                            for (var t : tables) {
                                if (t.columnSchemaMap.get(columnName) != null) {
                                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                            MessageFormat.format(
                                                    "conflicting column type. {0}: column={1}, alias={2}, type={3}, "
                                                            + "{4}: column={5}, alias={6}, type={7}",
                                                    t.tableName,
                                                    columnName,
                                                    t.columns.get(columnName),
                                                    t.columnSchemaMap.get(columnName),
                                                    table.tableName,
                                                    columnName,
                                                    table.columns.get(columnName),
                                                    table.columnSchemaMap.get(columnName)));
                                }
                            }
                        }
                    }
                }
            }

            var records = new ArrayList<TableRecords>();
            for (var table : tables) {
                var r = new TableRecords();
                r.meta = table;
                r.iterator = table.table.scan(table.revision,
                        table.columns,
                        req.getStart(),
                        req.getStartType(),
                        req.isStartInclusive(),
                        req.getEnd(),
                        req.getEndType(),
                        req.isEndInclusive(),
                        table.keepNone);
                if (r.iterator.hasNext()) {
                    r.record = r.iterator.next();
                    records.add(r);
                }
            }

            return resultResolver.apply(tables, columnSchemaMap, records);
        } finally {
            for (var table : tablesToLock) {
                table.unlock(true);
            }
        }
    }

    /**
     * For unit test only
     *
     * @return true if there is any dirty table.
     */
    public boolean hasDirtyTables() {
        synchronized (this.dirtyTables) {
            return !this.dirtyTables.isEmpty();
        }
    }

    /**
     * For unit test only
     */
    public void stopDump() {
        this.dumpThread.terminate();
    }

    private MemoryTable getTable(String tableName, boolean allowNull, boolean createIfNull) {
        for (; ; ) {
            var tableRef = this.tables.get(tableName);
            var table = tableRef == null ? null : tableRef.get();
            if (table == null) {
                synchronized (this.loadingTables) {
                    if (this.loadingTables.contains(tableName)) {
                        try {
                            this.loadingTables.wait();
                        } catch (InterruptedException e) {
                            throw new SwProcessException(ErrorType.DATASTORE, "interrupted", e);
                        }
                        continue;
                    }
                    this.loadingTables.add(tableName);
                }
                try {
                    table = new MemoryTableImpl(tableName,
                            this.walManager,
                            this.storageAccessService,
                            this.snapshotRootPath + tableName + PATH_SEPARATOR,
                            this.parquetConfig);
                    if (table.getSchema().getKeyColumn() != null || createIfNull) {
                        this.tables.put(tableName, new SoftReference<>(table));
                    } else if (table.getSchema().getKeyColumn() == null) {
                        if (allowNull) {
                            return null;
                        } else {
                            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                    "invalid table name " + tableName);
                        }
                    } else {
                        this.tables.put(tableName, new SoftReference<>(table));
                    }
                } finally {
                    synchronized (this.loadingTables) {
                        this.loadingTables.remove(tableName);
                        this.loadingTables.notifyAll();
                    }
                }
            }
            return table;
        }
    }

    private Map<String, String> getColumnAliases(TableSchema schema, Map<String, String> columns) {
        if (columns == null || columns.isEmpty()) {
            return schema.getColumnSchemaList().stream()
                    .map(ColumnSchema::getName)
                    .collect(Collectors.toMap(Function.identity(), Function.identity()));
        } else {
            var ret = new HashMap<String, String>();
            var invalidColumns = new HashSet<>(columns.keySet());
            for (var columnSchema : schema.getColumnSchemaList()) {
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

    private void saveTables(long minNoUpdatePeriodMillis, int minWalIdGap) throws IOException {
        var now = System.currentTimeMillis();
        var maxWalLogId = this.walManager.getMaxEntryId();
        List<MemoryTable> tables;
        synchronized (this.dirtyTables) {
            tables = new ArrayList<>(this.dirtyTables);
        }
        for (var table : tables) {
            if ((now - table.getLastUpdateTime()) >= minNoUpdatePeriodMillis
                    || maxWalLogId - table.getFirstWalLogId() >= minWalIdGap) {
                log.info("dumping {}", table.getTableName());
                table.save();
                synchronized (this.dirtyTables) {
                    if (table.getFirstWalLogId() < 0) {
                        this.dirtyTables.remove(table);
                    }
                }
            }
        }
    }

    private void clearWalLogFiles() {
        long minWalLogIdToRetain = this.walManager.getMaxEntryId() + 1;
        for (var tableRef : this.tables.values()) {
            var table = tableRef.get();
            if (table == null) {
                continue;
            }
            table.lock(false);
            try {
                if (table.getFirstWalLogId() >= 0 && table.getFirstWalLogId() < minWalLogIdToRetain) {
                    minWalLogIdToRetain = table.getFirstWalLogId();
                }
            } finally {
                table.unlock(false);
            }
        }
        try {
            this.walManager.removeWalLogFiles(minWalLogIdToRetain);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.DATASTORE, "failed to clear wal log files", e);
        }
    }

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.BORN) {
            while (updateHandle.size() > 0) {
                log.debug("currently {} updating process", updateHandle.size());
                synchronized (updateHandle) {
                    try {
                        updateHandle.wait(); // wait for all in process update operations done
                    } catch (InterruptedException e) {
                        log.error("wait for wal write done is interrupted", e);
                    }
                }
            }
            this.flush();
        }
    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.READY_DOWN) {
            this.start();
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }

    private class DumpThread extends Thread {

        private final long dumpIntervalMillis;
        private final long minNoUpdatePeriodMillis;
        private final int minWalIdGap;
        private volatile boolean terminated;

        public DumpThread(long dumpIntervalMillis, long minNoUpdatePeriodMillis, int minWalIdGap) {
            this.dumpIntervalMillis = dumpIntervalMillis;
            this.minNoUpdatePeriodMillis = minNoUpdatePeriodMillis;
            this.minWalIdGap = minWalIdGap;
        }

        public void run() {
            while (!this.terminated) {
                try {
                    saveTables(this.minNoUpdatePeriodMillis, this.minWalIdGap);
                    clearWalLogFiles();
                } catch (Throwable t) {
                    log.error("failed to save table", t);
                }
                try {
                    synchronized (this) {
                        if (this.terminated) {
                            return;
                        }
                        this.wait(dumpIntervalMillis);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        public void terminate() {
            synchronized (this) {
                this.terminated = true;
                this.notifyAll();
            }
            try {
                this.join();
            } catch (InterruptedException e) {
                log.error("interrupted", e);
            }
        }
    }
}
