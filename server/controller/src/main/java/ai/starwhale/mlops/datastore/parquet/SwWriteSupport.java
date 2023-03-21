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

package ai.starwhale.mlops.datastore.parquet;

import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.Wal;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.BoolValue;
import ai.starwhale.mlops.datastore.type.BytesValue;
import ai.starwhale.mlops.datastore.type.Float32Value;
import ai.starwhale.mlops.datastore.type.Float64Value;
import ai.starwhale.mlops.datastore.type.Int16Value;
import ai.starwhale.mlops.datastore.type.Int32Value;
import ai.starwhale.mlops.datastore.type.Int64Value;
import ai.starwhale.mlops.datastore.type.Int8Value;
import ai.starwhale.mlops.datastore.type.ListValue;
import ai.starwhale.mlops.datastore.type.MapValue;
import ai.starwhale.mlops.datastore.type.ObjectValue;
import ai.starwhale.mlops.datastore.type.StringValue;
import ai.starwhale.mlops.exception.SwProcessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Type.Repetition;
import org.apache.parquet.schema.Types;


@Slf4j
public class SwWriteSupport extends WriteSupport<Map<String, BaseValue>> {

    private final Map<String, ColumnSchema> schema;
    private final String tableSchema;
    private final String metadata;
    private final Map<String, String> extraMeta;
    private RecordConsumer recordConsumer;

    public SwWriteSupport(Map<String, ColumnSchema> schema,
            Map<String, String> extraMeta,
            String tableSchema,
            String metadata) {
        this.schema = schema;
        this.tableSchema = tableSchema;
        this.metadata = metadata;
        this.extraMeta = extraMeta;
    }

    @Override
    public String getName() {
        return "starwhale";
    }

    @Override
    public WriteContext init(Configuration configuration) {
        try {
            var parquetSchemaStr = new ObjectMapper().writeValueAsString(this.schema.values().stream()
                    .map(ColumnSchema::toColumnSchemaDesc)
                    .collect(Collectors.toList()));
            extraMeta.put(SwReadSupport.PARQUET_SCHEMA_KEY, parquetSchemaStr);
            extraMeta.put(SwReadSupport.SCHEMA_KEY, this.tableSchema);
            extraMeta.put(SwReadSupport.META_DATA_KEY, this.metadata);
            extraMeta.put(SwReadSupport.ERROR_FLAG_KEY, String.valueOf(true));
            return new WriteContext(new MessageType("table",
                    Stream.concat(this.schema.entrySet().stream()
                                            .sorted(Map.Entry.comparingByKey())
                                            .map(entry -> SwWriteSupport.createParquetType(entry.getValue())),
                                    Stream.of(Types.primitive(
                                            PrimitiveTypeName.BINARY,
                                            Repetition.OPTIONAL).named("wal")))
                            .collect(Collectors.toList())),
                    extraMeta);
        } catch (JsonProcessingException e) {
            throw new SwProcessException(SwProcessException.ErrorType.DATASTORE, "can not convert schema to json", e);
        }

    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        this.recordConsumer = recordConsumer;
    }

    @Override
    public void write(Map<String, BaseValue> record) {
        this.recordConsumer.startMessage();
        var result = SwWriteSupport.createWal(this.schema, record);
        if (result.getLeft() != null) {
            SwWriteSupport.writeColumns(recordConsumer, this.schema, result.getLeft());
        }
        if (result.getRight() != null) {
            recordConsumer.startField("wal", this.schema.size());
            recordConsumer.addBinary(Binary.fromConstantByteArray(
                    Wal.Column.newBuilder().putAllObjectValue(result.getRight()).build().toByteArray()));
            recordConsumer.endField("wal", this.schema.size());
        }
        this.recordConsumer.endMessage();
    }

    private static Pair<Map<String, BaseValue>, Map<String, Wal.Column>> createWal(
            Map<String, ColumnSchema> schema,
            @NonNull Map<String, BaseValue> value) {
        Map<String, BaseValue> values = null;
        Map<String, Wal.Column> wals = null;
        for (var entry : new TreeMap<>(schema).entrySet()) {
            var key = entry.getKey();
            if (!value.containsKey(key)) {
                continue;
            }
            var attrSchema = entry.getValue();
            var attrValue = value.get(key);
            if (attrValue == null) {
                if (values == null) {
                    values = new HashMap<>();
                }
                values.put(key, null);
            } else {
                var result = SwWriteSupport.createWal(attrSchema, attrValue);
                if (result.getLeft() != null) {
                    if (values == null) {
                        values = new HashMap<>();
                    }
                    values.put(key, result.getLeft());
                }
                if (result.getRight() != null) {
                    if (wals == null) {
                        wals = new HashMap<>();
                    }
                    wals.put(key, result.getRight().build());
                }
            }
        }
        for (var entry : value.entrySet()) {
            if (!schema.containsKey(entry.getKey())) {
                if (wals == null) {
                    wals = new HashMap<>();
                }
                wals.put(entry.getKey(), BaseValue.encodeWal(entry.getValue()).build());
            }
        }
        return Pair.of(values, wals);
    }

    private static Pair<BaseValue, Wal.Column.Builder> createWal(ColumnSchema schema, @NonNull BaseValue value) {
        if (BaseValue.getColumnType(value) != schema.getType()) {
            return Pair.of(null, BaseValue.encodeWal(value));
        }
        Wal.Column.Builder wal = null;
        switch (schema.getType()) {
            case LIST:
            case TUPLE:
                var listValue = (ListValue) value;
                ListValue newListValue = new ListValue();
                for (var i = 0; i < listValue.size(); ++i) {
                    var element = listValue.get(i);
                    if (element == null) {
                        newListValue.add(null);
                    } else {
                        var result = SwWriteSupport.createWal(schema.getElementSchema(), element);
                        if (result.getLeft() != null) {
                            newListValue.add(result.getLeft());
                        }
                        if (result.getRight() != null) {
                            if (wal == null) {
                                wal = Wal.Column.newBuilder().setType(schema.getType().getIndex());
                            }
                            wal.addListValue(result.getRight().setIndex(i));
                        }
                    }
                }
                if (wal != null && newListValue.isEmpty()) {
                    newListValue = null;
                }
                return Pair.of(newListValue, wal);
            case MAP:
                var mapValue = (MapValue) value;
                MapValue newMapValue = new MapValue();
                for (var entry : mapValue.entrySet()) {
                    Pair<BaseValue, Wal.Column.Builder> resultKey = null;
                    Pair<BaseValue, Wal.Column.Builder> resultValue = null;
                    if (entry.getKey() != null) {
                        resultKey = SwWriteSupport.createWal(schema.getKeySchema(), entry.getKey());
                    }
                    if (entry.getValue() != null) {
                        resultValue = SwWriteSupport.createWal(schema.getValueSchema(), entry.getValue());
                    }
                    if ((resultKey == null || resultKey.getLeft() != null)
                            && (resultValue == null || resultValue.getLeft() != null)) {
                        newMapValue.put(resultKey == null ? null : resultKey.getLeft(),
                                resultValue == null ? null : resultValue.getLeft());
                    } else {
                        var builder = Wal.Column.MapEntry.newBuilder();
                        builder.setKey(BaseValue.encodeWal(entry.getKey()));
                        builder.setValue(BaseValue.encodeWal(entry.getValue()));
                        if (wal == null) {
                            wal = Wal.Column.newBuilder().setType(schema.getType().getIndex());
                        }
                        wal.addMapValue(builder);
                    }
                }
                if (wal != null && newMapValue.isEmpty()) {
                    newMapValue = null;
                }
                return Pair.of(newMapValue, wal);
            case OBJECT:
                var result = SwWriteSupport.createWal(schema.getAttributesSchema(), (ObjectValue) value);
                ObjectValue newObjectValue = new ObjectValue(((ObjectValue) value).getPythonType());
                if (result.getLeft() != null) {
                    newObjectValue.putAll(result.getLeft());
                }
                if (result.getRight() != null
                        || !((ObjectValue) value).getPythonType().equals(schema.getPythonType())) {
                    wal = Wal.Column.newBuilder()
                            .setStringValue(((ObjectValue) value).getPythonType())
                            .setType(schema.getType().getIndex());
                    if (result.getRight() != null) {
                        wal.putAllObjectValue(result.getRight());
                    }
                    if (newObjectValue.isEmpty()) {
                        newObjectValue = null;
                    }
                }
                return Pair.of(newObjectValue, wal);
            default:
                return Pair.of(value, null);
        }
    }

    private static void writeColumns(RecordConsumer recordConsumer,
            Map<String, ColumnSchema> schema,
            Map<String, BaseValue> value) {
        int index = 0;
        for (var entry : new TreeMap<>(schema).entrySet()) {
            var attrSchema = entry.getValue();
            var key = entry.getKey();
            if (value.containsKey(key)) {
                var attrValue = value.get(key);
                recordConsumer.startField(key, index);
                SwWriteSupport.writeParquetValue(recordConsumer, attrSchema, attrValue);
                recordConsumer.endField(key, index);
            }
            ++index;
        }
        if (value instanceof ObjectValue) {
            recordConsumer.startField("#pythonType", index);
            recordConsumer.addBinary(Binary.fromString(((ObjectValue) value).getPythonType()));
            recordConsumer.endField("#pythonType", index);
        }
    }

    private static Type createParquetType(ColumnSchema schema) {
        Types.Builder<?, ? extends Type> valueType;
        switch (schema.getType()) {
            case UNKNOWN:
            case BOOL:
                valueType = Types.primitive(PrimitiveTypeName.BOOLEAN, Repetition.OPTIONAL);
                break;
            case INT8:
                valueType = Types.primitive(PrimitiveTypeName.INT32, Repetition.OPTIONAL)
                        .as(LogicalTypeAnnotation.intType(8, true));
                break;
            case INT16:
                valueType = Types.primitive(PrimitiveTypeName.INT32, Repetition.OPTIONAL)
                        .as(LogicalTypeAnnotation.intType(16, true));
                break;
            case INT32:
                valueType = Types.primitive(PrimitiveTypeName.INT32, Repetition.OPTIONAL);
                break;
            case INT64:
                valueType = Types.primitive(PrimitiveTypeName.INT64, Repetition.OPTIONAL);
                break;
            case FLOAT32:
                valueType = Types.primitive(PrimitiveTypeName.FLOAT, Repetition.OPTIONAL);
                break;
            case FLOAT64:
                valueType = Types.primitive(PrimitiveTypeName.DOUBLE, Repetition.OPTIONAL);
                break;
            case STRING:
                valueType = Types.primitive(PrimitiveTypeName.BINARY, Repetition.OPTIONAL)
                        .as(LogicalTypeAnnotation.stringType());
                break;
            case BYTES:
                valueType = Types.primitive(PrimitiveTypeName.BINARY, Repetition.OPTIONAL);
                break;
            case LIST:
            case TUPLE:
                valueType = Types.optionalGroup().addField(
                        Types.repeatedGroup()
                                .addField(SwWriteSupport.createParquetType(schema.getElementSchema()))
                                .named("list"));
                break;
            case MAP:
                valueType = Types.optionalGroup().addField(
                        Types.repeatedGroup()
                                .addField(SwWriteSupport.createParquetType(schema.getKeySchema()))
                                .addField(SwWriteSupport.createParquetType(schema.getValueSchema()))
                                .named("map"));
                break;
            case OBJECT:
                valueType = Types.optionalGroup()
                        .addFields(schema.getAttributesSchema().values().stream()
                                .map(SwWriteSupport::createParquetType)
                                .toArray(Type[]::new))
                        .addField(Types.primitive(PrimitiveTypeName.BINARY, Repetition.OPTIONAL)
                                .as(LogicalTypeAnnotation.stringType())
                                .named("#"));
                break;
            default:
                throw new IllegalArgumentException("invalid type " + schema.getType());
        }
        var builder = Types.optionalGroup();
        builder.addField(Types.primitive(PrimitiveTypeName.BOOLEAN, Repetition.REQUIRED).named("null"));
        builder.addField(valueType.named("value"));
        return builder.named(schema.getName());
    }

    private static void writeParquetValue(RecordConsumer recordConsumer, ColumnSchema schema,
            BaseValue value) {
        recordConsumer.startGroup();
        recordConsumer.startField("null", 0);
        recordConsumer.addBoolean(value == null);
        recordConsumer.endField("null", 0);
        if (value != null) {
            recordConsumer.startField("value", 1);
            switch (schema.getType()) {
                case BOOL:
                    recordConsumer.addBoolean(((BoolValue) value).isValue());
                    break;
                case INT8:
                    recordConsumer.addInteger(((Int8Value) value).getValue());
                    break;
                case INT16:
                    recordConsumer.addInteger(((Int16Value) value).getValue());
                    break;
                case INT32:
                    recordConsumer.addInteger(((Int32Value) value).getValue());
                    break;
                case INT64:
                    recordConsumer.addLong(((Int64Value) value).getValue());
                    break;
                case FLOAT32:
                    recordConsumer.addFloat(((Float32Value) value).getValue());
                    break;
                case FLOAT64:
                    recordConsumer.addDouble(((Float64Value) value).getValue());
                    break;
                case STRING:
                    recordConsumer.addBinary(Binary.fromString(((StringValue) value).getValue()));
                    break;
                case BYTES:
                    recordConsumer.addBinary(Binary.fromConstantByteBuffer(((BytesValue) value).getValue()));
                    break;
                case LIST:
                case TUPLE:
                    recordConsumer.startGroup();
                    var listValue = (ListValue) value;
                    if (!listValue.isEmpty()) {
                        recordConsumer.startField("list", 0);
                        for (BaseValue element : listValue) {
                            recordConsumer.startGroup();
                            recordConsumer.startField("element", 0);
                            SwWriteSupport.writeParquetValue(recordConsumer, schema.getElementSchema(), element);
                            recordConsumer.endField("element", 0);
                            recordConsumer.endGroup();
                        }
                        recordConsumer.endField("list", 0);
                    }
                    recordConsumer.endGroup();
                    break;
                case MAP:
                    recordConsumer.startGroup();
                    var mapValue = (MapValue) value;
                    if (!mapValue.isEmpty()) {
                        recordConsumer.startField("map", 0);
                        for (var entry : mapValue.entrySet()) {
                            recordConsumer.startGroup();
                            recordConsumer.startField("key", 0);
                            SwWriteSupport.writeParquetValue(recordConsumer, schema.getKeySchema(), entry.getKey());
                            recordConsumer.endField("key", 0);
                            recordConsumer.startField("value", 1);
                            SwWriteSupport.writeParquetValue(recordConsumer, schema.getValueSchema(), entry.getValue());
                            recordConsumer.endField("value", 1);
                            recordConsumer.endGroup();
                        }
                        recordConsumer.endField("map", 0);
                    }
                    recordConsumer.endGroup();
                    break;
                case OBJECT:
                    recordConsumer.startGroup();
                    SwWriteSupport.writeColumns(recordConsumer, schema.getAttributesSchema(), (ObjectValue) value);
                    recordConsumer.endGroup();
                    break;
                default:
                    throw new IllegalArgumentException("invalid type " + schema.getType());
            }
            recordConsumer.endField("value", 1);
        }
        recordConsumer.endGroup();
    }
}
