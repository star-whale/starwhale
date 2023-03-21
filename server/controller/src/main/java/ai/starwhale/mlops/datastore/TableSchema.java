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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private String keyColumn;

    private final Map<String, Integer> columnSchemaIndex = new HashMap<>();

    @Getter
    private final List<ColumnSchema> columnSchemaList = new ArrayList<>();

    public TableSchema() {
    }

    public TableSchema(@NonNull TableSchema schema) {
        this.keyColumn = schema.keyColumn;
        for (var col : schema.columnSchemaList) {
            this.columnSchemaIndex.put(col.getName(), col.getIndex());
            this.columnSchemaList.add(new ColumnSchema(col));
        }
    }

    public TableSchema(@NonNull TableSchemaDesc schema) {
        this.keyColumn = schema.getKeyColumn();
        if (schema.getColumnSchemaList() != null) {
            for (var col : schema.getColumnSchemaList()) {
                var index = this.columnSchemaList.size();
                this.columnSchemaIndex.put(col.getName(), index);
                this.columnSchemaList.add(new ColumnSchema(col, index));
            }
        }
    }

    public TableSchema(@NonNull Wal.TableSchema schema) {
        this.keyColumn = schema.getKeyColumn();
        for (var col : schema.getColumnsList()) {
            this.columnSchemaIndex.put(col.getColumnName(), col.getColumnIndex());
            while (col.getColumnIndex() >= this.columnSchemaList.size()) {
                this.columnSchemaList.add(null);
            }
            this.columnSchemaList.set(col.getColumnIndex(), new ColumnSchema(col));
        }
    }

    public ColumnSchema getKeyColumnSchema() {
        return this.columnSchemaList.get(this.columnSchemaIndex.get(this.keyColumn));
    }

    public TableSchemaDesc toTableSchemaDesc() {
        return new TableSchemaDesc(this.keyColumn,
                this.columnSchemaList.stream().map(ColumnSchema::toColumnSchemaDesc).collect(Collectors.toList()));
    }

    public Wal.TableSchema.Builder toWal() {
        var builder = Wal.TableSchema.newBuilder();
        builder.setKeyColumn(this.keyColumn);
        for (var col : this.columnSchemaList) {
            builder.addColumns(col.toWal());
        }
        return builder;
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
            return JsonFormat.printer().print(this.toWal());
        } catch (InvalidProtocolBufferException e) {
            throw new SwProcessException(ErrorType.DATASTORE, "failed to print proto", e);
        }
    }

    public Wal.TableSchema.Builder getDiff(@NonNull TableSchemaDesc schema) {
        Wal.TableSchema.Builder ret = null;
        if (this.keyColumn == null) {
            if (schema.getKeyColumn() == null) {
                throw new IllegalArgumentException("no key column");
            }
            ret = Wal.TableSchema.newBuilder().setKeyColumn(schema.getKeyColumn());
        }
        var nextIndex = this.columnSchemaList.size();
        for (var col : schema.getColumnSchemaList()) {
            var index = this.columnSchemaIndex.get(col.getName());
            ColumnSchema columnSchema;
            if (index == null) {
                index = nextIndex++;
                columnSchema = new ColumnSchema(col.getName(), index);
            } else {
                columnSchema = this.columnSchemaList.get(index);
            }
            var wal = columnSchema.getDiff(col);
            if (wal != null) {
                if (ret == null) {
                    ret = Wal.TableSchema.newBuilder();
                }
                ret.addColumns(wal.setColumnName(col.getName()).setColumnIndex(index));
            }
        }
        return ret;
    }

    public void update(@NonNull Wal.TableSchema schema) {
        if (this.keyColumn == null) {
            this.keyColumn = schema.getKeyColumn();
        }
        for (var col : schema.getColumnsList()) {
            var index = col.getColumnIndex();
            while (index >= this.columnSchemaList.size()) {
                this.columnSchemaList.add(null);
            }
            ColumnSchema columnSchema = this.columnSchemaList.get(index);
            if (columnSchema == null) {
                columnSchema = new ColumnSchema(col.getColumnName(), index);
                this.columnSchemaIndex.put(col.getColumnName(), index);
                this.columnSchemaList.set(index, columnSchema);
            }
            columnSchema.update(col);
        }
    }

    public ColumnSchema getColumnSchemaByName(@NonNull String name) {
        var index = this.columnSchemaIndex.get(name);
        if (index == null) {
            return null;
        }
        return this.getColumnSchemaByIndex(index);
    }

    public ColumnSchema getColumnSchemaByIndex(int index) {
        return this.columnSchemaList.get(index);
    }
}