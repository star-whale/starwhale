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

import ai.starwhale.mlops.exception.SwValidationException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class TableSchema {

    @Getter
    private final String keyColumn;
    @Getter
    private final ColumnType keyColumnType;
    private final Map<String, ColumnSchema> columnSchemaMap;
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
        this.maxColumnIndex = schema.maxColumnIndex;
    }

    public ColumnSchema getColumnSchemaByName(@NonNull String name) {
        return this.columnSchemaMap.get(name);
    }

    public List<ColumnSchema> getColumnSchemas() {
        return List.copyOf(this.columnSchemaMap.values());
    }

    public List<ColumnSchema> merge(@NonNull TableSchemaDesc schema) {
        if (schema.getKeyColumn() != null && !this.keyColumn.equals(schema.getKeyColumn())) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE).tip(
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
        this.maxColumnIndex = columnIndex;
        return List.copyOf(columnSchemaMap.values());
    }

    public Map<String, ColumnType> getColumnTypeMapping() {
        return this.columnSchemaMap.values().stream().collect(Collectors.toMap(
                ColumnSchema::getName, ColumnSchema::getType));
    }

    public Map<String, ColumnType> getColumnTypeMapping(@NonNull Map<String, String> columnAliases) {
        var ret = new HashMap<String, ColumnType>();
        for (var entry : columnAliases.entrySet()) {
            var columnSchema = this.columnSchemaMap.get(entry.getKey());
            if (columnSchema == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "column name " + entry.getKey() + " not found");
            }
            ret.put(entry.getValue(), columnSchema.getType());
        }
        return ret;
    }
}
