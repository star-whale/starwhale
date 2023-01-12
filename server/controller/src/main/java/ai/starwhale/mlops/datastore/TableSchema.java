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

import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@EqualsAndHashCode
@Slf4j
public class TableSchema {

    @Getter
    private final String keyColumn;
    @Getter
    private final ColumnType keyColumnType;
    private final Map<String, ColumnSchema> columnSchemaMap;
    private final Map<Integer, ColumnSchema> columnSchemaIndexMap;
    private int maxColumnIndex;

    public TableSchema(@NonNull TableSchemaDesc schema) {
        this.keyColumn = schema.getKeyColumn();
        if (this.keyColumn == null) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "key column should not be null");
        }
        this.columnSchemaMap = new HashMap<>();
        if (schema.getColumnSchemaList() == null || schema.getColumnSchemaList().isEmpty()) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "columnSchemaList should not be empty");
        }
        for (var col : schema.getColumnSchemaList()) {
            var colSchema = new ColumnSchema(col, this.maxColumnIndex++);
            if (this.columnSchemaMap.putIfAbsent(col.getName(), colSchema) != null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "duplicate column name " + col.getName());
            }
        }
        this.columnSchemaIndexMap = this.columnSchemaMap.values().stream()
                .collect(Collectors.toMap(ColumnSchema::getIndex, Function.identity()));
        var keyColumnSchema = this.columnSchemaMap.get(keyColumn);
        if (keyColumnSchema == null) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "key column " + keyColumn + " not found in column schemas");
        }
        this.keyColumnType = keyColumnSchema.getType();
        if (!(this.keyColumnType instanceof ColumnTypeScalar)) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "key column " + keyColumn + " should be of scalar type");
        }
        if (this.keyColumnType == ColumnTypeScalar.UNKNOWN) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "key column " + keyColumn + " should not be of type UNKNOWN");
        }
    }


    public TableSchema(@NonNull TableSchema schema) {
        this.keyColumn = schema.keyColumn;
        this.keyColumnType = schema.keyColumnType;
        this.columnSchemaMap = new HashMap<>(schema.columnSchemaMap);
        this.columnSchemaIndexMap = new HashMap<>(schema.columnSchemaIndexMap);
        this.maxColumnIndex = schema.maxColumnIndex;
    }


    private TableSchema(@NonNull Wal.TableSchema schema) {
        this.keyColumn = schema.getKeyColumn();
        this.columnSchemaMap = schema.getColumnsList().stream()
                .map(ColumnSchema::new)
                .collect(Collectors.toMap(ColumnSchema::getName, Function.identity()));
        this.columnSchemaIndexMap = this.columnSchemaMap.values().stream()
                .collect(Collectors.toMap(ColumnSchema::getIndex, Function.identity()));
        this.keyColumnType = this.columnSchemaMap.get(keyColumn).getType();
        this.maxColumnIndex = this.columnSchemaIndexMap.keySet().stream().mapToInt(v -> v).max().orElse(-1) + 1;
    }

    public static TableSchema fromJsonString(String schemaStr) {
        var schemaBuilder = Wal.TableSchema.newBuilder();
        try {
            JsonFormat.parser().merge(schemaStr, schemaBuilder);
        } catch (InvalidProtocolBufferException e) {
            throw new SwProcessException(ErrorType.DATASTORE, "failed to parse schema", e);
        }
        return new TableSchema(schemaBuilder.build());
    }

    public String toJsonString() {
        try {
            return JsonFormat.printer().print(WalManager.convertTableSchema(this));
        } catch (InvalidProtocolBufferException e) {
            throw new SwProcessException(ErrorType.DATASTORE, "failed to print proto", e);
        }
    }

    public ColumnSchema getColumnSchemaByName(@NonNull String name) {
        return this.columnSchemaMap.get(name);
    }

    public ColumnSchema getColumnSchemaByIndex(int index) {
        return this.columnSchemaIndexMap.get(index);
    }

    public List<ColumnSchema> getColumnSchemas() {
        return List.copyOf(this.columnSchemaMap.values());
    }

    public List<ColumnSchema> merge(@NonNull TableSchemaDesc schema) {
        if (schema.getKeyColumn() != null && !this.keyColumn.equals(schema.getKeyColumn())) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    MessageFormat.format(
                            "can not merge two schemas with different key columns, expected {0}, actual {1}",
                            this.keyColumn,
                            schema.getKeyColumn()));
        }
        var columnSchemaMap = new HashMap<String, ColumnSchema>();
        var columnIndex = this.maxColumnIndex;
        for (var col : schema.getColumnSchemaList()) {
            var current = this.columnSchemaMap.get(col.getName());
            var colSchema = new ColumnSchema(col, current == null ? columnIndex++ : current.getIndex());
            if (current == null) {
                columnSchemaMap.put(col.getName(), colSchema);
            } else if (!current.getType().equals(colSchema.getType())) {
                if (current.getType() == ColumnTypeScalar.UNKNOWN) {
                    columnSchemaMap.put(col.getName(), colSchema);
                } else if (colSchema.getType() != ColumnTypeScalar.UNKNOWN) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            MessageFormat.format("conflicting type for column {0}, expected {1}, actual {2}",
                                    col.getName(), current.getType(), col.getType()));
                }
            }
        }
        this.columnSchemaMap.putAll(columnSchemaMap);
        for (var col : columnSchemaMap.values()) {
            this.columnSchemaIndexMap.put(col.getIndex(), col);
        }
        this.maxColumnIndex = columnIndex;
        return List.copyOf(columnSchemaMap.values());
    }

    public Map<String, ColumnType> getColumnTypeMapping() {
        return this.columnSchemaMap.values().stream().collect(Collectors.toMap(
                ColumnSchema::getName, ColumnSchema::getType));
    }

    /**
     * get column pairs
     *
     * @param columnAliases originName, alias
     * @return alias, ColumnType
     */
    public Map<String, ColumnType> getColumnTypeMapping(@NonNull Map<String, String> columnAliases) {
        var ret = new HashMap<String, ColumnType>();
        for (var entry : columnAliases.entrySet()) {
            var name = entry.getKey();
            var columnSchema = this.columnSchemaMap.get(name);
            if (columnSchema == null) {
                if (ColumnTypeVirtual.isVirtual(name)) {
                    var virtualSchema = ColumnTypeVirtual.build(
                            entry.getValue(), name, this.columnSchemaMap::get);
                    if (virtualSchema != null) {
                        ret.put(entry.getValue(), virtualSchema);
                    }
                    continue;
                }
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "column name " + entry.getKey() + " not found");
            }
            ret.put(entry.getValue(), columnSchema.getType());
        }
        return ret;
    }
}
