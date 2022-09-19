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
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import com.google.protobuf.ByteString;
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
import lombok.NonNull;

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
        if (columnSchema.getType() == ColumnType.UNKNOWN) {
            return null;
        } else if (columnSchema.getType() == ColumnType.BOOL) {
            return col.getBoolValue();
        } else if (columnSchema.getType() == ColumnType.INT8) {
            return (byte) col.getIntValue();
        } else if (columnSchema.getType() == ColumnType.INT16) {
            return (short) col.getIntValue();
        } else if (columnSchema.getType() == ColumnType.INT32) {
            return (int) col.getIntValue();
        } else if (columnSchema.getType() == ColumnType.INT64) {
            return col.getIntValue();
        } else if (columnSchema.getType() == ColumnType.FLOAT32) {
            return col.getFloatValue();
        } else if (columnSchema.getType() == ColumnType.FLOAT64) {
            return col.getDoubleValue();
        } else if (columnSchema.getType() == ColumnType.STRING) {
            return col.getStringValue();
        } else if (columnSchema.getType() == ColumnType.BYTES) {
            return ByteBuffer.wrap(col.getBytesValue().toByteArray());
        }
        throw new IllegalArgumentException("invalid type " + this);
    }


    @Override
    public void update(TableSchemaDesc schema, List<Map<String, String>> records) {
        var logEntryBuilder = Wal.WalEntry.newBuilder()
                .setEntryType(Wal.WalEntry.Type.UPDATE)
                .setTableName(this.tableName);
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
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
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
                if (colSchema.getType() == ColumnType.UNKNOWN) {
                    ret.setNullValue(true);
                } else if (colSchema.getType() == ColumnType.BOOL) {
                    ret.setBoolValue((Boolean) value);
                } else if (colSchema.getType() == ColumnType.INT8 || colSchema.getType() == ColumnType.INT16
                        || colSchema.getType() == ColumnType.INT32 || colSchema.getType() == ColumnType.INT64) {
                    ret.setIntValue(((Number) value).longValue());
                } else if (colSchema.getType() == ColumnType.FLOAT32) {
                    ret.setFloatValue((Float) value);
                } else if (colSchema.getType() == ColumnType.FLOAT64) {
                    ret.setDoubleValue((Double) value);
                } else if (colSchema.getType() == ColumnType.STRING) {
                    ret.setStringValue((String) value);
                } else if (colSchema.getType() == ColumnType.BYTES) {
                    ret.setBytesValue(ByteString.copyFrom(((ByteBuffer) value).array()));
                } else {
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
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "order by column should not be null");
                }
                if (this.schema.getColumnSchemaByName(col.getColumnName()) == null) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
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
                    var columnType = this.schema.getColumnSchemaByName(col.getColumnName()).getType();
                    var result = MemoryTableImpl.sortCompare(
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

    private static int sortCompare(ColumnType type1, Object value1, ColumnType type2, Object value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        if (!type1.equals(type2)) {
            throw new IllegalArgumentException(
                    MessageFormat.format("not same type: {0} and {1}", type1, type2));
        }
        if (type1 == ColumnType.STRING) {
            return ((String) value1).compareTo((String) value2);
        } else if (type1 == ColumnType.BYTES) {
            return ((ByteBuffer) value1).compareTo((ByteBuffer) value2);
        } else if (type1 == ColumnType.BOOL) {
            return ((Boolean) value1).compareTo((Boolean) value2);
        } else if (type1.getCategory().equals(ColumnType.INT32.getCategory())) {
            return Long.compare(((Number) value1).longValue(), ((Number) value2).longValue());
        } else if (type1.getCategory().equals(ColumnType.FLOAT32.getCategory())) {
            return Double.compare(((Number) value1).doubleValue(), ((Number) value2).doubleValue());
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
                        this.getType(filter.getOperands().get(0)),
                        this.getValue(filter.getOperands().get(0), record),
                        this.getType(filter.getOperands().get(1)),
                        this.getValue(filter.getOperands().get(1), record),
                        filter.getOperator());
            default:
                throw new IllegalArgumentException("Unexpected value: " + filter.getOperator());
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
            return ColumnType.BOOL;
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
                this.checkSameType(filter.getOperands());
                break;
            default:
                throw new IllegalArgumentException("Unexpected operator: " + filter.getOperator());
        }
    }

    private void checkSameType(List<Object> operands) {
        var types = operands.stream()
                .map(this::getType)
                .filter(t -> t != ColumnType.UNKNOWN)
                .collect(Collectors.toList());
        if (types.isEmpty()) {
            return;
        }
        ColumnType type1 = types.get(0);
        for (var type : types) {
            if (!type.getCategory().equals(type1.getCategory())) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        MessageFormat.format("invalid filter, can not compare {0} with {1}", type1, type));
            }
        }
    }

    private static boolean filterCompare(
            ColumnType type1,
            Object value1,
            ColumnType type2,
            Object value2,
            TableQueryFilter.Operator op) {
        if (value1 == null || value2 == null) {
            return value1 == null && value2 == null && op == TableQueryFilter.Operator.EQUAL;
        }
        int result;
        if (type1.getCategory().equals(type2.getCategory())) {
            if (type1 == ColumnType.BOOL) {
                result = ((Boolean) value1).compareTo((Boolean) value2);
            } else if (type1 == ColumnType.STRING) {
                result = ((String) value1).compareTo((String) value2);
            } else if (type1 == ColumnType.BYTES) {
                result = ((ByteBuffer) value1).compareTo((ByteBuffer) value2);
            } else if (type1.getCategory().equals(ColumnType.INT32.getCategory())) {
                result = Long.compare(((Number) value1).longValue(), ((Number) value2).longValue());
            } else if (type1.getCategory().equals(ColumnType.FLOAT32.getCategory())) {
                result = Double.compare(((Number) value1).doubleValue(), ((Number) value2).doubleValue());
            } else {
                throw new IllegalArgumentException("invalid type " + type1);
            }
        } else if (value1 instanceof Number && value2 instanceof Number) {
            result = Double.compare(((Number) value1).doubleValue(), ((Number) value2).doubleValue());
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
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "no schema found for column " + name);
            }
            try {
                ret.put(columnSchema.getName(), columnSchema.getType().decode(value));
            } catch (Exception e) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        MessageFormat.format("fail to decode value {0} for column {1}: {2}",
                                value,
                                name,
                                e.getMessage()));
            }
        }
        return ret;
    }

}
