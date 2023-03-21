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

import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.ListValue;
import ai.starwhale.mlops.datastore.type.MapValue;
import ai.starwhale.mlops.datastore.type.ObjectValue;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ColumnStatistics {

    private final Map<ColumnType, Long> columnTypeCounter = new HashMap<>();
    private ColumnStatistics elementSchema;
    private ColumnStatistics keySchema;
    private ColumnStatistics valueSchema;
    private Map<String, ColumnStatistics> attributesSchema;

    public void update(BaseValue value) {
        var type = BaseValue.getColumnType(value);
        var count = this.columnTypeCounter.get(type);
        if (count == null) {
            this.columnTypeCounter.put(type, 1L);
        } else {
            this.columnTypeCounter.put(type, count + 1);
        }
        if (value instanceof ListValue) {
            if (this.elementSchema == null) {
                this.elementSchema = new ColumnStatistics();
            }
            for (var element : (ListValue) value) {
                this.elementSchema.update(element);
            }
        } else if (value instanceof MapValue) {
            if (this.keySchema == null) {
                this.keySchema = new ColumnStatistics();
            }
            if (this.valueSchema == null) {
                this.valueSchema = new ColumnStatistics();
            }
            for (var entry : ((MapValue) value).entrySet()) {
                this.keySchema.update(entry.getKey());
                this.valueSchema.update(entry.getValue());
            }
        } else if (value instanceof ObjectValue) {
            if (this.attributesSchema == null) {
                this.attributesSchema = new HashMap<>();
            }
            for (var entry : ((ObjectValue) value).entrySet()) {
                this.attributesSchema.computeIfAbsent(entry.getKey(), k -> new ColumnStatistics())
                        .update(entry.getValue());
            }
        }
    }

    public ColumnSchema createSchema(@NonNull String name, int index) {
        var ret = new ColumnSchema(name, index);
        ret.setType(ColumnType.UNKNOWN);
        long maxCount = 0;
        for (var entry : this.columnTypeCounter.entrySet()) {
            if (entry.getKey() == ColumnType.UNKNOWN) {
                continue;
            }
            long count = entry.getValue();
            if (count > maxCount) {
                maxCount = count;
                ret.setType(entry.getKey());
            }
        }
        switch (ret.getType()) {
            case LIST:
            case TUPLE:
                ret.setElementSchema(this.elementSchema.createSchema("element", 0));
                break;
            case MAP:
                ret.setKeySchema(this.keySchema.createSchema("key", 0));
                ret.setValueSchema(this.valueSchema.createSchema("value", 1));
                break;
            case OBJECT:
                // pythonType is useless for parquet schema
                ret.setPythonType("placeholder");
                var attrSchema = new HashMap<String, ColumnSchema>();
                int i = 0;
                for (var entry : new TreeMap<>(this.attributesSchema).entrySet()) {
                    attrSchema.put(entry.getKey(), entry.getValue().createSchema(entry.getKey(), i++));
                }
                ret.setAttributesSchema(attrSchema);
                break;
            default:
                break;
        }
        return ret;
    }
}
