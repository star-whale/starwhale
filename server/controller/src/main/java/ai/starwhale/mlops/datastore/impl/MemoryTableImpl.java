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
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.MemoryTable;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.Wal;
import ai.starwhale.mlops.datastore.WalManager;
import ai.starwhale.mlops.exception.SWValidationException;
import com.google.protobuf.ByteString;
import lombok.NonNull;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MemoryTableImpl implements MemoryTable {
    private final String tableName;

    private final WalManager walManager;

    private TableSchema schema = null;

    private final TreeMap<Object, Map<String, Object>> recordMap = new TreeMap<>();

    // used only for initialization from WAL
    private final Map<Integer, ColumnSchema> indexMap = new HashMap<>();

    private final Lock lock = new ReentrantLock();

    public MemoryTableImpl(String tableName, WalManager walManager) {
        this.tableName = tableName;
        this.walManager = walManager;
    }

    public void lock() {
        this.lock.lock();
    }

    public void unlock() {
        this.lock.unlock();
    }

    public TableSchema getSchema() {
        return this.schema == null ? null : new TableSchema(this.schema);
    }

    @Override
    public void updateFromWal(Wal.WalEntry entry) {
        if (entry.hasTableSchema()) {
            var schemaDesc = this.parseSchema(entry.getTableSchema());
            if (this.schema == null) {
                this.schema = new TableSchema(schemaDesc);
            } else {
                this.schema.merge(schemaDesc);
            }
        }
        var recordList = entry.getRecordsList();
        if (!recordList.isEmpty()) {
            this.insertRecords(recordList.stream().map(this::parseRecord).collect(Collectors.toList()));
        }
    }

    private TableSchemaDesc parseSchema(Wal.TableSchema tableSchema) {
        var ret = new TableSchemaDesc();
        var keyColumn = tableSchema.getKeyColumn();
        if (!keyColumn.isEmpty()) {
            ret.setKeyColumn(keyColumn);
        }
        var columnList = new ArrayList<>(tableSchema.getColumnsList());
        columnList.sort(Comparator.comparingInt(Wal.ColumnSchema::getColumnIndex));
        var columnSchemaList = new ArrayList<ColumnSchemaDesc>();
        for (var col : columnList) {
            var colDesc = new ColumnSchemaDesc(col.getColumnName(), col.getColumnType());
            columnSchemaList.add(colDesc);
            this.indexMap.put(col.getColumnIndex(), new ColumnSchema(colDesc, col.getColumnIndex()));
        }
        ret.setColumnSchemaList(columnSchemaList);
        return ret;
    }

    private Map<String, Object> parseRecord(Wal.Record record) {
        Map<String, Object> ret = new HashMap<>();
        for (var col : record.getColumnsList()) {
            if (col.getIndex() == -1) {
                ret.put("-", true);
            } else {
                var colSchema = this.indexMap.get(col.getIndex());
                ret.put(colSchema.getName(), this.parseValue(colSchema, col));
            }
        }
        return ret;
    }

    private Object parseValue(ColumnSchema columnSchema, Wal.Column col) {
        if (col.getNullValue()) {
            return null;
        }
        switch (columnSchema.getType()) {
            case UNKNOWN:
                return null;
            case BOOL:
                return col.getBoolValue();
            case INT8:
                return (byte) col.getIntValue();
            case INT16:
                return (short) col.getIntValue();
            case INT32:
                return (int) col.getIntValue();
            case INT64:
                return col.getIntValue();
            case FLOAT32:
                return col.getFloatValue();
            case FLOAT64:
                return col.getDoubleValue();
            case STRING:
                return col.getStringValue();
            case BYTES:
                return ByteBuffer.wrap(col.getBytesValue().toByteArray());
            default:
                throw new IllegalArgumentException("invalid type " + this);
        }
    }


    @Override
    public void update(TableSchemaDesc schema, List<Map<String, String>> records) {
        var logEntryBuilder = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName(this.tableName);
        TableSchema newSchema = this.schema;
        if (schema == null) {
            if (this.schema == null) {
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
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
                logSchemaBuilder.addColumns(Wal.ColumnSchema.newBuilder()
                        .setColumnIndex(col.getIndex())
                        .setColumnName(col.getName())
                        .setColumnType(col.getType().toString()));
            }
            logEntryBuilder.setTableSchema(logSchemaBuilder);
        }
        List<Map<String, Object>> decodedRecords = null;
        if (records != null) {
            decodedRecords = new ArrayList<>();
            for (var record : records) {
                var key = record.get(newSchema.getKeyColumn());
                if (key == null) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                            MessageFormat.format("key column {0} is null", newSchema.getKeyColumn()));
                }
                if (record.get("-") != null) {
                    decodedRecords.add(Map.of(newSchema.getKeyColumn(),
                            newSchema.getKeyColumnType().decode(key),
                            "-",
                            true));
                } else {
                    decodedRecords.add(MemoryTableImpl.decodeRecord(newSchema, record));
                }
            }
            for (var record : decodedRecords) {
                logEntryBuilder.addRecords(MemoryTableImpl.writeRecord(newSchema, record));
            }
        }
        this.walManager.append(logEntryBuilder.build());
        this.schema = newSchema;
        if (decodedRecords != null) {
            this.insertRecords(decodedRecords);
        }
    }

    private void insertRecords(List<Map<String, Object>> records) {
        for (var record : records) {
            var key = record.get(this.schema.getKeyColumn());
            if (record.get("-") != null) {
                this.recordMap.remove(key);
            } else {
                var old = this.recordMap.putIfAbsent(key, record);
                if (old != null) {
                    old.putAll(record);
                }
            }
        }
    }

    private static Wal.Record.Builder writeRecord(TableSchema schema, Map<String, Object> record) {
        var ret = Wal.Record.newBuilder();
        for (var entry : record.entrySet()) {
            ret.addColumns(MemoryTableImpl.writeColumn(schema, entry.getKey(), entry.getValue()));
        }
        return ret;
    }

    private static Wal.Column.Builder writeColumn(TableSchema schema, String name, Object value) {
        var ret = Wal.Column.newBuilder();
        if (name.equals("-")) {
            ret.setIndex(-1);
        } else {
            var colSchema = schema.getColumnSchemaByName(name);
            ret.setIndex(colSchema.getIndex());
            if (value == null) {
                ret.setNullValue(true);
            } else {
                switch (colSchema.getType()) {
                    case UNKNOWN:
                        ret.setNullValue(true);
                        break;
                    case BOOL:
                        ret.setBoolValue((Boolean) value);
                        break;
                    case INT8:
                    case INT16:
                    case INT32:
                    case INT64:
                        ret.setIntValue(((Number) value).longValue());
                        break;
                    case FLOAT32:
                        ret.setFloatValue((Float) value);
                        break;
                    case FLOAT64:
                        ret.setDoubleValue((Double) value);
                        break;
                    case STRING:
                        ret.setStringValue((String) value);
                        break;
                    case BYTES:
                        ret.setBytesValue(ByteString.copyFrom(((ByteBuffer) value).array()));
                        break;
                    default:
                        throw new IllegalArgumentException("invalid type " + colSchema.getType());
                }
            }
        }
        return ret;
    }

    @Override
    public List<RecordResult> query(
            @NonNull Map<String, String> columns,
            List<OrderByDesc> orderBy,
            TableQueryFilter filter,
            int start,
            int limit,
            boolean keepNone,
            boolean rawResult) {
        if (this.schema == null) {
            return Collections.emptyList();
        }
        this.schema.getColumnTypeMapping(columns); // check if all column names are valid
        if (orderBy != null) {
            for (var col : orderBy) {
                if (col == null) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                            "order by column should not be null");
                }
                if (this.schema.getColumnSchemaByName(col.getColumnName()) == null) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                            "unknown orderBy column " + col);
                }
            }
        }
        if (filter != null) {
            this.checkFilter(filter);
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (var record : this.recordMap.values()) {
            if (filter == null || this.match(filter, record)) {
                results.add(record);
            }
        }
        if (start < 0) {
            start = 0;
        }
        if (limit < 0) {
            limit = results.size();
        }
        int end = start + limit;
        if (end > results.size()) {
            end = results.size();
        }
        if (orderBy != null) {
            results.sort((a, b) -> {
                for (var col : orderBy) {
                    var result = MemoryTableImpl.sortCompare(a.get(col.getColumnName()),
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

        return results.subList(start, end).stream().map(record -> {
            var r = new HashMap<String, Object>();
            for (var entry : columns.entrySet()) {
                var value = record.get(entry.getKey());
                if (keepNone || value != null) {
                    r.put(entry.getValue(), value);
                }
            }
            return new RecordResult(record.get(this.schema.getKeyColumn()), r);
        }).collect(Collectors.toList());
    }


    @Override
    public List<RecordResult> scan(
            @NonNull Map<String, String> columns,
            String start,
            boolean startInclusive,
            String end,
            boolean endInclusive,
            int limit,
            boolean keepNone) {
        if (this.schema == null) {
            return Collections.emptyList();
        }
        if (this.recordMap.isEmpty() || limit == 0) {
            return Collections.emptyList();
        }

        var startKey = MemoryTableImpl.this.schema.getKeyColumnType().decode(start);
        var endKey = MemoryTableImpl.this.schema.getKeyColumnType().decode(end);
        if (startKey == null) {
            startKey = MemoryTableImpl.this.recordMap.firstKey();
            startInclusive = true;
        }
        if (endKey == null) {
            endKey = MemoryTableImpl.this.recordMap.lastKey();
            endInclusive = true;
        }
        //noinspection rawtypes,unchecked
        if (((Comparable) startKey).compareTo(endKey) > 0) {
            return Collections.emptyList();
        }
        var keyColumn = this.schema.getKeyColumn();
        var records = new ArrayList<RecordResult>();
        for (var record : MemoryTableImpl.this.recordMap.subMap(startKey, startInclusive, endKey, endInclusive)
                .values()) {
            var values = new HashMap<String, Object>();
            for (var entry : columns.entrySet()) {
                var columnName = entry.getKey();
                var alias = entry.getValue();
                var value = record.get(columnName);
                if (keepNone || value != null) {
                    values.put(alias, value);
                }
            }
            records.add(new RecordResult(record.get(keyColumn), values));
            if (records.size() == limit) {
                break;
            }
        }
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records;
        }
    }

    private static int sortCompare(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        var type1 = ColumnType.getColumnType(a);
        var type2 = ColumnType.getColumnType(b);
        if (type1 != type2) {
            throw new IllegalArgumentException(
                    MessageFormat.format("not same type: {0} and {1}", type1, type2));
        }
        if (type1 == ColumnType.STRING) {
            return ((String) a).compareTo((String) b);
        } else if (type1 == ColumnType.BYTES) {
            return ((ByteBuffer) a).compareTo((ByteBuffer) b);
        } else if (type1 == ColumnType.BOOL) {
            return ((Boolean) a).compareTo((Boolean) b);
        } else if (type1.getName().equals(ColumnType.INT32.getName())) {
            return Long.compare(((Number) a).longValue(), ((Number) b).longValue());
        } else if (type1.getName().equals(ColumnType.FLOAT32.getName())) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        } else {
            throw new IllegalArgumentException("invalid type " + type1);
        }
    }

    private boolean match(TableQueryFilter filter, Map<String, Object> record) {
        switch (filter.getOperator()) {
            case NOT:
                return !this.match((TableQueryFilter) filter.getOperands().get(0), record);
            case AND:
                return this.match((TableQueryFilter) filter.getOperands().get(0), record)
                        && this.match((TableQueryFilter) filter.getOperands().get(1), record);
            case OR:
                return this.match((TableQueryFilter) filter.getOperands().get(0), record)
                        || this.match((TableQueryFilter) filter.getOperands().get(1), record);
            case EQUAL:
            case GREATER:
            case GREATER_EQUAL:
            case LESS:
            case LESS_EQUAL:
                return MemoryTableImpl.filterCompare(
                        this.getValue(filter.getOperands().get(0), record),
                        this.getValue(filter.getOperands().get(1), record),
                        filter.getOperator());
            default:
                throw new IllegalArgumentException("Unexpected value: " + filter.getOperator());
        }
    }

    private Object getValue(Object operand, Map<String, Object> record) {
        if (operand instanceof TableQueryFilter.Column) {
            return record.get(((TableQueryFilter.Column) operand).getName());
        }
        return operand;
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
                this.checkSameType(filter.getOperands());
                break;
            default:
                throw new IllegalArgumentException("Unexpected operator: " + filter.getOperator());
        }
    }

    private void checkSameType(List<Object> operands) {
        if (operands.isEmpty()) {
            return;
        }
        var firstCol = operands.stream()
                .filter(x -> x instanceof TableQueryFilter.Column)
                .map(x -> (TableQueryFilter.Column) x)
                .findFirst();
        var type = this.schema.getColumnSchemaByName(firstCol.orElseThrow().getName()).getType();
        for (var op : operands) {
            if (op instanceof TableQueryFilter.Column) {
                var col = (TableQueryFilter.Column) op;
                var colSchema = this.schema.getColumnSchemaByName(col.getName());
                if (colSchema == null) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                            "invalid filter, unknown column " + col.getName());
                }
                if (!type.getName().equals(colSchema.getType().getName())) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                            MessageFormat.format(
                                    "invalid filter, can not compare column {0} of type {1} with column {2} of type {3}",
                                    col.getName(),
                                    colSchema.getType(),
                                    firstCol.orElseThrow().getName(),
                                    type));
                }
            } else if (op != null) {
                boolean checkFailed;
                if (op instanceof String) {
                    checkFailed = type != ColumnType.STRING;
                } else if (op instanceof ByteBuffer) {
                    checkFailed = type != ColumnType.BYTES;
                } else if (op instanceof Boolean) {
                    checkFailed = type != ColumnType.BOOL;
                } else if (op instanceof Number) {
                    checkFailed = !type.getName().equals(ColumnType.INT32.getName())
                            && !type.getName().equals(ColumnType.FLOAT32.getName());
                } else {
                    throw new IllegalArgumentException("unexpected operand class " + op.getClass());
                }
                if (checkFailed) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                            MessageFormat.format(
                                    "invalid filter, can not compare column {0} of type {1} with value {2} of type {3}",
                                    firstCol.orElseThrow().getName(),
                                    type,
                                    op,
                                    op.getClass()));
                }
            }
        }

    }


    private static boolean filterCompare(Object a, Object b, TableQueryFilter.Operator op) {
        if (a == null || b == null) {
            return a == null && b == null && op == TableQueryFilter.Operator.EQUAL;
        }
        int result;
        var type1 = ColumnType.getColumnType(a);
        var type2 = ColumnType.getColumnType(b);
        if (type1.getName().equals(type2.getName())) {
            if (type1 == ColumnType.BOOL) {
                result = ((Boolean) a).compareTo((Boolean) b);
            } else if (type1 == ColumnType.STRING) {
                result = ((String) a).compareTo((String) b);
            } else if (type1 == ColumnType.BYTES) {
                result = ((ByteBuffer) a).compareTo((ByteBuffer) b);
            } else if (type1.getName().equals(ColumnType.INT32.getName())) {
                result = Long.compare(((Number) a).longValue(), ((Number) b).longValue());
            } else if (type1.getName().equals(ColumnType.FLOAT32.getName())) {
                result = Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            } else {
                throw new IllegalArgumentException("invalid type " + type1);
            }
        } else if (a instanceof Number && b instanceof Number) {
            result = Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        } else {
            throw new IllegalArgumentException("can not compare " + type1 + " with " + type2);
        }
        switch (op) {
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
                throw new IllegalArgumentException("Unexpected operator: " + op);
        }
    }

    private static Map<String, Object> decodeRecord(TableSchema schema, Map<String, String> record) {
        if (record == null) {
            return null;
        }
        var ret = new HashMap<String, Object>();
        for (var entry : record.entrySet()) {
            var name = entry.getKey();
            var value = entry.getValue();
            var columnSchema = schema.getColumnSchemaByName(name);
            if (columnSchema == null) {
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                        "no schema found for column " + name);
            }
            try {
                ret.put(columnSchema.getName(), columnSchema.getType().decode(value));
            } catch (Exception e) {
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE,
                        MessageFormat.format("fail to decode value {0} for column {1}: {2}",
                                value,
                                name,
                                e.getMessage()));
            }
        }
        return ret;
    }

}
