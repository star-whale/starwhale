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

import cn.hutool.core.collection.ConcurrentHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;

@Data
public class ColumnSchema {

    private static Set<String> pythonTypeConstants = new ConcurrentHashSet<>();
    private String name;
    private int index;
    // offset is used to indicate the order of the elements in a list or tuple
    private Integer offset;
    private ColumnType type;
    private String pythonType;
    private ColumnSchema elementSchema;
    private Map<Integer, ColumnSchema> sparseElementSchema;

    private ColumnSchema keySchema;
    private ColumnSchema valueSchema;
    private Map<String, ColumnSchema> attributesSchema;

    // key is the offset of the map.entry list
    // value is the pair of key and value schema
    private Map<Integer, Pair<ColumnSchema, ColumnSchema>> sparseKeyValueSchema;

    public ColumnSchema(@NonNull String name, int index) {
        this.name = name;
        this.index = index;
    }

    public ColumnSchema(@NonNull ColumnSchema schema) {
        this.name = schema.name;
        this.index = schema.index;
        this.offset = schema.offset;
        this.type = schema.type;
        this.pythonType = schema.pythonType;
        if (schema.elementSchema != null) {
            this.elementSchema = new ColumnSchema(schema.elementSchema);
        }
        if (schema.sparseElementSchema != null) {
            this.sparseElementSchema = schema.sparseElementSchema.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> new ColumnSchema(entry.getValue())));
        }
        if (schema.keySchema != null) {
            this.keySchema = new ColumnSchema(schema.keySchema);
        }
        if (schema.valueSchema != null) {
            this.valueSchema = new ColumnSchema(schema.valueSchema);
        }
        if (schema.attributesSchema != null) {
            this.attributesSchema = schema.attributesSchema.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> new ColumnSchema(entry.getValue())));
        }
        if (schema.sparseKeyValueSchema != null) {
            this.sparseKeyValueSchema = schema.sparseKeyValueSchema.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry ->
                            Pair.of(new ColumnSchema(entry.getValue().getLeft()),
                                    new ColumnSchema(entry.getValue().getRight()))));
        }
    }

    public ColumnSchema(@NonNull ColumnSchemaDesc schema, int index) {
        // ensure the name is not null (wal do not allow null name)
        this.name = schema.getName() == null ? "" : schema.getName();
        this.index = index;
        this.type = ColumnType.valueOf(schema.getType());
        switch (this.type) {
            case LIST:
            case TUPLE:
                this.elementSchema = new ColumnSchema(schema.getElementType(), 0);
                this.elementSchema.setName("element");
                if (schema.getAttributes() != null && !schema.getAttributes().isEmpty()) {
                    this.sparseElementSchema = new HashMap<>();
                    for (var attr : schema.getAttributes()) {
                        var it = new ColumnSchema(attr, 0);
                        it.setOffset(attr.getIndex());
                        this.sparseElementSchema.put(attr.getIndex(), it);
                    }
                }
                break;
            case MAP:
                this.keySchema = new ColumnSchema(schema.getKeyType(), 0);
                this.keySchema.setName("key");
                this.valueSchema = new ColumnSchema(schema.getValueType(), 0);
                this.valueSchema.setName("value");
                if (schema.getSparseKeyValuePairSchema() != null) {
                    this.sparseKeyValueSchema = schema.getSparseKeyValuePairSchema().entrySet().stream()
                            .collect(Collectors.toMap(Entry::getKey,
                                    entry -> Pair.of(new ColumnSchema(entry.getValue().getKeyType(), 0),
                                            new ColumnSchema(entry.getValue().getValueType(), 0))));
                }
                break;
            case OBJECT:
                this.pythonType = schema.getPythonType();
                if (this.pythonType == null || this.pythonType.isEmpty()) {
                    throw new IllegalArgumentException("pythonType should not be null or empty");
                }
                this.attributesSchema = schema.getAttributes().stream()
                        .map(attr -> new ColumnSchema(attr, 0))
                        .collect(Collectors.toMap(ColumnSchema::getName, Function.identity()));
                break;
            default:
                break;
        }
    }

    public ColumnSchema(@NonNull Wal.ColumnSchema schema) {
        this.name = schema.getColumnName();
        this.index = schema.getColumnIndex();
        this.type = ColumnType.valueOf(schema.getColumnType());
        switch (this.type) {
            case LIST:
            case TUPLE:
                this.elementSchema = new ColumnSchema(schema.getElementType());
                this.elementSchema.setName("element");
                if (!schema.getAttributesList().isEmpty()) {
                    this.sparseElementSchema = new HashMap<>();
                    for (var attr : schema.getAttributesList()) {
                        var it = new ColumnSchema(attr);
                        it.setOffset(attr.getColumnIndex());
                        this.sparseElementSchema.put(attr.getColumnIndex(), it);
                    }
                }
                break;
            case MAP:
                this.keySchema = new ColumnSchema(schema.getKeyType());
                this.keySchema.setName("key");
                this.valueSchema = new ColumnSchema(schema.getValueType());
                this.valueSchema.setName("value");
                if (schema.getSparseKeyValueTypesCount() > 0) {
                    this.sparseKeyValueSchema = new HashMap<>();
                    var entrySet = schema.getSparseKeyValueTypesMap().entrySet();
                    for (var entry : entrySet) {
                        var i = entry.getKey();
                        var item = schema.getSparseKeyValueTypesMap().get(i);
                        var schemaPair = Pair.of(new ColumnSchema(item.getKey()), new ColumnSchema(item.getValue()));
                        this.sparseKeyValueSchema.put(i, schemaPair);
                    }
                }
                break;
            case OBJECT:
                this.pythonType = schema.getPythonType();
                if (this.pythonType.isEmpty()) {
                    throw new IllegalArgumentException("pythonType should not be null or empty");
                }
                this.attributesSchema = schema.getAttributesList().stream()
                        .map(ColumnSchema::new)
                        .collect(Collectors.toMap(ColumnSchema::getName, Function.identity()));
                break;
            default:
                break;
        }
    }

    public ColumnSchemaDesc toColumnSchemaDesc() {
        var builder = ColumnSchemaDesc.builder()
                .name(this.name)
                .index(this.offset)
                .type(this.type.name());
        switch (this.type) {
            case LIST:
            case TUPLE:
                builder.elementType(this.elementSchema.toColumnSchemaDesc());
                if (this.sparseElementSchema != null) {
                    builder.attributes(this.sparseElementSchema.values().stream()
                            .map(ColumnSchema::toColumnSchemaDesc)
                            .collect(Collectors.toList()));
                }
                break;
            case MAP:
                builder.keyType(this.keySchema.toColumnSchemaDesc());
                builder.valueType(this.valueSchema.toColumnSchemaDesc());
                if (this.sparseKeyValueSchema != null) {
                    builder.sparseKeyValuePairSchema(this.sparseKeyValueSchema.entrySet().stream()
                            .collect(Collectors.toMap(Entry::getKey,
                                    entry -> new ColumnSchemaDesc.KeyValuePairSchema(
                                            entry.getValue().getLeft().toColumnSchemaDesc(),
                                            entry.getValue().getRight().toColumnSchemaDesc()))));
                }
                break;
            case OBJECT:
                builder.pythonType(this.pythonType)
                        .attributes(this.attributesSchema.values().stream()
                                .map(ColumnSchema::toColumnSchemaDesc)
                                .collect(Collectors.toList()));
                break;
            default:
                break;
        }
        return builder.build();
    }

    public Wal.ColumnSchema.Builder toWal() {
        var builder = Wal.ColumnSchema.newBuilder()
                .setColumnName(this.name)
                .setColumnIndex(this.offset != null ? this.offset : this.index)
                .setColumnType(this.type.name());
        switch (this.type) {
            case LIST:
            case TUPLE:
                builder.setElementType(this.elementSchema.toWal());
                if (this.sparseElementSchema != null) {
                    this.sparseElementSchema.values().stream()
                            .map(ColumnSchema::toWal)
                            .forEach(builder::addAttributes);
                }
                break;
            case MAP:
                builder.setKeyType(this.keySchema.toWal());
                builder.setValueType(this.valueSchema.toWal());
                if (this.sparseKeyValueSchema != null) {
                    this.sparseKeyValueSchema.forEach((key, value) -> builder.putSparseKeyValueTypes(key,
                            Wal.ColumnSchema.KeyValuePair.newBuilder()
                                    .setKey(value.getLeft().toWal())
                                    .setValue(value.getRight().toWal())
                                    .build()));
                }
                break;
            case OBJECT:
                builder.setPythonType(this.pythonType);
                this.attributesSchema.values().stream()
                        .map(ColumnSchema::toWal)
                        .forEach(builder::addAttributes);
                break;
            default:
                break;
        }
        return builder;
    }

    public Wal.ColumnSchema.Builder getDiff(@NonNull ColumnSchemaDesc schema) {
        var type = ColumnType.valueOf(schema.getType());
        Wal.ColumnSchema.Builder ret = null;
        if (this.type != type) {
            var cs = new ColumnSchema(schema, this.index);
            // update column name for list, tuple, map
            cs.setName(this.name);
            return cs.toWal();
        }
        switch (type) {
            case LIST:
            case TUPLE:
                var elementSchema = this.elementSchema;
                if (elementSchema == null) {
                    elementSchema = new ColumnSchema("element", 0);
                }
                var elementWal = elementSchema.getDiff(schema.getElementType());
                if (elementWal != null) {
                    ret = Wal.ColumnSchema.newBuilder().setElementType(elementWal);
                }
                if (schema.getAttributes() != null) {
                    for (var attr : schema.getAttributes()) {
                        var attrSchema = this.sparseElementSchema.get(attr.getIndex());
                        if (attrSchema == null) {
                            attrSchema = new ColumnSchema(attr.getName(), 0);
                        }
                        var attrWal = attrSchema.getDiff(attr);
                        if (attrWal != null) {
                            if (ret == null) {
                                ret = Wal.ColumnSchema.newBuilder();
                            }
                            ret.addAttributes(attrWal.setColumnName(attr.getName()).setColumnIndex(attr.getIndex()));
                        }
                    }
                }
                break;
            case MAP:
                var keySchema = this.keySchema;
                if (keySchema == null) {
                    keySchema = new ColumnSchema("key", 0);
                }
                var valueSchema = this.valueSchema;
                if (valueSchema == null) {
                    valueSchema = new ColumnSchema("value", 0);
                }
                var keyWal = keySchema.getDiff(schema.getKeyType());
                var valueWal = valueSchema.getDiff(schema.getValueType());
                if (keyWal != null || valueWal != null) {
                    ret = Wal.ColumnSchema.newBuilder();
                    if (keyWal != null) {
                        ret.setKeyType(keyWal);
                    }
                    if (valueWal != null) {
                        ret.setValueType(valueWal);
                    }
                }
                if (schema.getSparseKeyValuePairSchema() != null) {
                    for (var entry : schema.getSparseKeyValuePairSchema().entrySet()) {
                        var index = entry.getKey();
                        var pairDesc = entry.getValue();
                        var pair = this.sparseKeyValueSchema.get(index);
                        if (pair == null) {
                            pair = Pair.of(new ColumnSchema(pairDesc.getKeyType(), 0),
                                    new ColumnSchema(pairDesc.getValueType(), 0));
                        }
                        var keyDiff = pair.getLeft().getDiff(pairDesc.getKeyType());
                        var valueDiff = pair.getRight().getDiff(pairDesc.getValueType());
                        if (keyDiff != null || valueDiff != null) {
                            if (ret == null) {
                                ret = Wal.ColumnSchema.newBuilder();
                            }
                            var kvPair = Wal.ColumnSchema.KeyValuePair.newBuilder();
                            if (keyDiff != null) {
                                kvPair.setKey(keyDiff);
                            }
                            if (valueDiff != null) {
                                kvPair.setValue(valueDiff);
                            }
                            ret.putSparseKeyValueTypes(index, kvPair.build());
                        }
                    }
                }
                break;
            case OBJECT:
                if (!schema.getPythonType().equals(this.pythonType)) {
                    ret = Wal.ColumnSchema.newBuilder();
                    ret.setPythonType(schema.getPythonType());
                }
                var attributesSchema = this.attributesSchema;
                if (attributesSchema == null) {
                    attributesSchema = new HashMap<>();
                }
                if (schema.getAttributes() != null) {
                    for (var attr : schema.getAttributes()) {
                        var attrSchema = attributesSchema.get(attr.getName());
                        if (attrSchema == null) {
                            attrSchema = new ColumnSchema(attr.getName(), 0);
                        }
                        var attrWal = attrSchema.getDiff(attr);
                        if (attrWal != null) {
                            if (ret == null) {
                                ret = Wal.ColumnSchema.newBuilder();
                            }
                            ret.addAttributes(attrWal.setColumnName(attr.getName()));
                        }
                    }
                }
                break;
            default:
                break;
        }
        if (ret != null) {
            ret.setColumnType(type.name());
        }
        return ret;
    }

    public void update(@NonNull Wal.ColumnSchema schema) {
        var type = ColumnType.valueOf(schema.getColumnType());
        if (this.type != type) {
            this.keySchema = null;
            this.valueSchema = null;
            this.elementSchema = null;
            this.attributesSchema = null;
            this.pythonType = null;
            this.type = type;
        }
        switch (type) {
            case LIST:
            case TUPLE:
                if (this.elementSchema == null) {
                    this.elementSchema = new ColumnSchema("element", 0);
                }
                this.elementSchema.update(schema.getElementType());
                if (!schema.getAttributesList().isEmpty()) {
                    if (this.sparseElementSchema == null) {
                        this.sparseElementSchema = new HashMap<>();
                    }
                    for (var attr : schema.getAttributesList()) {
                        var attrSchema = this.sparseElementSchema.get(attr.getColumnIndex());
                        if (attrSchema == null) {
                            attrSchema = new ColumnSchema(attr.getColumnName(), 0);
                            attrSchema.setOffset(attr.getColumnIndex());
                            this.sparseElementSchema.put(attr.getColumnIndex(), attrSchema);
                        }
                        attrSchema.update(attr);
                    }
                }
                break;
            case MAP:
                if (this.keySchema == null) {
                    if (schema.hasKeyType()) {
                        this.keySchema = new ColumnSchema("key", 0);
                    } else {
                        throw new IllegalArgumentException("no key type for MAP");
                    }
                }
                if (this.valueSchema == null) {
                    if (schema.hasValueType()) {
                        this.valueSchema = new ColumnSchema("value", 0);
                    } else {
                        throw new IllegalArgumentException("no value type for MAP");
                    }
                }
                if (schema.hasKeyType()) {
                    this.keySchema.update(schema.getKeyType());
                }
                if (schema.hasValueType()) {
                    this.valueSchema.update(schema.getValueType());
                }
                if (schema.getSparseKeyValueTypesCount() > 0) {
                    if (this.sparseKeyValueSchema == null) {
                        this.sparseKeyValueSchema = new HashMap<>();
                    }
                    for (var item : schema.getSparseKeyValueTypesMap().entrySet()) {
                        var index = item.getKey();
                        var existPair = this.sparseKeyValueSchema.get(index);
                        if (existPair == null) {
                            existPair = Pair.of(new ColumnSchema(item.getValue().getKey()),
                                    new ColumnSchema(item.getValue().getValue()));
                            this.sparseKeyValueSchema.put(index, existPair);
                        } else {
                            if (item.getValue().hasKey()) {
                                existPair.getLeft().update(item.getValue().getKey());
                            }
                            if (item.getValue().hasValue()) {
                                existPair.getRight().update(item.getValue().getValue());
                            }
                        }
                    }
                }
                break;
            case OBJECT:
                if (!schema.getPythonType().isEmpty()) {
                    this.pythonType = schema.getPythonType();
                }
                if (this.attributesSchema == null) {
                    this.attributesSchema = new HashMap<>();
                }
                for (var attr : schema.getAttributesList()) {
                    var attrSchema = this.attributesSchema.get(attr.getColumnName());
                    if (attrSchema == null) {
                        attrSchema = new ColumnSchema(attr.getColumnName(), 0);
                        this.attributesSchema.put(attr.getColumnName(), attrSchema);
                    }
                    attrSchema.update(attr);
                }
                break;
            default:
                break;
        }
    }

    public boolean isSameType(@NonNull ColumnSchema other) {
        if (this.type != other.type) {
            return false;
        }
        switch (this.type) {
            case LIST:
            case TUPLE:
                var same = this.elementSchema.isSameType(other.elementSchema);
                if (!same) {
                    return false;
                }
                if (this.sparseElementSchema == null && other.sparseElementSchema == null) {
                    return true;
                }
                if (this.sparseElementSchema == null || other.sparseElementSchema == null) {
                    return false;
                }
                if (this.sparseElementSchema.size() != other.sparseElementSchema.size()) {
                    return false;
                }
                for (var offset : this.sparseElementSchema.keySet()) {
                    if (!this.sparseElementSchema.get(offset).isSameType(other.sparseElementSchema.get(offset))) {
                        return false;
                    }
                }
                break;
            case MAP:
                if (!this.keySchema.isSameType(other.keySchema)) {
                    return false;
                }
                if (!this.valueSchema.isSameType(other.valueSchema)) {
                    return false;
                }
                if (this.sparseKeyValueSchema != null) {
                    if (other.sparseKeyValueSchema == null) {
                        return false;
                    }
                    if (!this.sparseKeyValueSchema.keySet().equals(other.sparseKeyValueSchema.keySet())) {
                        return false;
                    }
                    for (var key : this.sparseKeyValueSchema.keySet()) {
                        if (!this.sparseKeyValueSchema.get(key).getLeft()
                                .isSameType(other.sparseKeyValueSchema.get(key).getLeft())) {
                            return false;
                        }
                        if (!this.sparseKeyValueSchema.get(key).getRight()
                                .isSameType(other.sparseKeyValueSchema.get(key).getRight())) {
                            return false;
                        }
                    }
                } else {
                    return other.sparseKeyValueSchema == null;
                }
                break;
            case OBJECT:
                if (!this.pythonType.equals(other.pythonType)
                        || !this.attributesSchema.keySet().equals(other.attributesSchema.keySet())) {
                    return false;
                }
                for (var attrName : this.attributesSchema.keySet()) {
                    if (!this.attributesSchema.get(attrName).isSameType(other.attributesSchema.get(attrName))) {
                        return false;
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }
}
