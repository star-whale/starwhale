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

import ai.starwhale.mlops.exception.SWValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
public class TableSchema {
    private static final Pattern COLUMN_NAME_PATTERN =
            Pattern.compile("^[\\p{Alpha}\\p{Alnum}-_/: ]*$");
    @Getter
    private final String keyColumn;
    @Getter
    private final ColumnType keyColumnType;
    private final Map<String, ColumnSchema> columnSchemaMap;
    private int maxColumnIndex;

    public TableSchema(@NonNull TableSchemaDesc schema) {
        this.keyColumn = schema.getKeyColumn();
        if (this.keyColumn == null) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    "key column should not be null");
        }
        this.columnSchemaMap = new HashMap<>();
        if (schema.getColumnSchemaList() != null) {
            for (var col : schema.getColumnSchemaList()) {
                var colSchema = new ColumnSchema(col, this.maxColumnIndex++);
                if (!TableSchema.COLUMN_NAME_PATTERN.matcher(col.getName()).matches()) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                            "invalid column name " + col.getName());
                }
                if (this.columnSchemaMap.putIfAbsent(col.getName(), colSchema) != null) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                            "duplicate column name " + col.getName());
                }
            }
        }
        var keyColumnSchema = this.columnSchemaMap.get(keyColumn);
        if (keyColumnSchema == null) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    "key column " + keyColumn + " not found in column schemas");
        }
        this.keyColumnType = keyColumnSchema.getType();
        if (this.keyColumnType == ColumnType.UNKNOWN) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
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
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    MessageFormat.format(
                            "can not merge two schemas with different key columns, expected {1}, actual {2}",
                            this.keyColumn,
                            schema.getKeyColumn()));
        }
        var columnSchemaMap = new HashMap<String, ColumnSchema>();
        var columnIndex = this.maxColumnIndex;
        for (var col : schema.getColumnSchemaList()) {
            var current = this.columnSchemaMap.get(col.getName());
            var colSchema = new ColumnSchema(col, current == null ? columnIndex++ : current.getIndex());
            if (current != null
                    && current.getType() != ColumnType.UNKNOWN
                    && colSchema.getType() != ColumnType.UNKNOWN
                    && current.getType() != colSchema.getType()) {
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                        MessageFormat.format("conflicting type for column {0}, expected {1}, actual {2}",
                                col.getName(), current.getType(), col.getType()));
            }
            if (current == null
                    || current.getType() != colSchema.getType() && colSchema.getType() != ColumnType.UNKNOWN) {
                columnSchemaMap.put(col.getName(), colSchema);
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
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                        "column name " + entry.getKey() + " not found");
            }
            ret.put(entry.getValue(), columnSchema.getType());
        }
        return ret;
    }
}
