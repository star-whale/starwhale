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
import ai.starwhale.mlops.datastore.type.ScalarValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
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

    private static final int MAX_COLUMN_VALUE_COUNT = 50;

    private final Map<ColumnType, Long> columnTypeCounter = new HashMap<>();
    private final Map<BaseValue, Long> columnValueCounter = new HashMap<>();
    private boolean columnValueCounterFull = false;
    private ColumnStatistics elementStatistics;
    private ColumnStatistics keyStatistics;
    private ColumnStatistics valueStatistics;
    private Map<String, ColumnStatistics> attributesStatistics;

    public void update(BaseValue value) {
        var type = BaseValue.getColumnType(value);
        this.columnTypeCounter.merge(type, 1L, Long::sum);
        if (!this.columnValueCounterFull && value instanceof ScalarValue) {
            this.columnValueCounter.merge(value, 1L, Long::sum);
            if (this.columnValueCounter.size() >= MAX_COLUMN_VALUE_COUNT) {
                this.columnValueCounterFull = true;
                this.columnValueCounter.clear();
            }
        }

        if (value instanceof ListValue) {
            if (this.elementStatistics == null) {
                this.elementStatistics = new ColumnStatistics();
            }
            for (var element : (ListValue) value) {
                this.elementStatistics.update(element);
            }
        } else if (value instanceof MapValue) {
            if (this.keyStatistics == null) {
                this.keyStatistics = new ColumnStatistics();
            }
            if (this.valueStatistics == null) {
                this.valueStatistics = new ColumnStatistics();
            }
            for (var entry : ((MapValue) value).entrySet()) {
                this.keyStatistics.update(entry.getKey());
                this.valueStatistics.update(entry.getValue());
            }
        } else if (value instanceof ObjectValue) {
            if (this.attributesStatistics == null) {
                this.attributesStatistics = new HashMap<>();
            }
            for (var entry : ((ObjectValue) value).entrySet()) {
                this.attributesStatistics.computeIfAbsent(entry.getKey(), k -> new ColumnStatistics())
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
                ret.setElementSchema(this.elementStatistics.createSchema("element", 0));
                break;
            case MAP:
                ret.setKeySchema(this.keyStatistics.createSchema("key", 0));
                ret.setValueSchema(this.valueStatistics.createSchema("value", 1));
                break;
            case OBJECT:
                // pythonType is useless for parquet schema
                ret.setPythonType("placeholder");
                var attrSchema = new HashMap<String, ColumnSchema>();
                int i = 0;
                for (var entry : new TreeMap<>(this.attributesStatistics).entrySet()) {
                    attrSchema.put(entry.getKey(), entry.getValue().createSchema(entry.getKey(), i++));
                }
                ret.setAttributesSchema(attrSchema);
                break;
            default:
                break;
        }
        return ret;
    }

    public void merge(ColumnStatistics other) {
        other.columnTypeCounter.forEach((k, v) -> this.columnTypeCounter.merge(k, v, Long::sum));
        other.columnValueCounter.forEach((k, v) -> this.columnValueCounter.merge(k, v, Long::sum));

        if (other.elementStatistics != null) {
            if (this.elementStatistics == null) {
                this.elementStatistics = new ColumnStatistics();
            }
            this.elementStatistics.merge(other.elementStatistics);
        }

        if (other.keyStatistics != null) {
            if (this.keyStatistics == null) {
                this.keyStatistics = new ColumnStatistics();
            }
            this.keyStatistics.merge(other.keyStatistics);
        }

        if (other.valueStatistics != null) {
            if (this.valueStatistics == null) {
                this.valueStatistics = new ColumnStatistics();
            }
            this.valueStatistics.merge(other.valueStatistics);
        }

        if (other.attributesStatistics != null) {
            if (this.attributesStatistics == null) {
                this.attributesStatistics = new HashMap<>();
            }
            other.attributesStatistics.forEach((k, v) -> {
                this.attributesStatistics.computeIfAbsent(k, x -> new ColumnStatistics()).merge(v);
            });
        }
    }

    public ColumnHintsDesc toColumnHintsDesc() {
        var builder = ColumnHintsDesc.builder();
        builder.typeHints(this.columnTypeCounter.keySet().stream()
                .filter(x -> x != ColumnType.UNKNOWN)
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList()));
        if (!this.columnValueCounterFull) {
            builder.columnValueHints(this.columnValueCounter.keySet().stream()
                    .filter(Objects::nonNull)
                    .sorted()
                    .map(value -> (String) value.encode(true, false))
                    .distinct()
                    .collect(Collectors.toList()));
        }
        if (this.elementStatistics != null) {
            builder.elementHints(this.elementStatistics.toColumnHintsDesc());
        }
        if (this.keyStatistics != null) {
            builder.keyHints(this.keyStatistics.toColumnHintsDesc());
        }
        if (this.valueStatistics != null) {
            builder.valueHints(this.valueStatistics.toColumnHintsDesc());
        }
        if (this.attributesStatistics != null) {
            builder.attributesHints(
                    this.attributesStatistics.entrySet().stream()
                            .collect(Collectors.toMap(Entry::getKey,
                                    entry -> entry.getValue().toColumnHintsDesc())));
        }
        return builder.build();
    }
}
