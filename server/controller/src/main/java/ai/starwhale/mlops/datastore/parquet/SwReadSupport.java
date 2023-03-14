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
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.Wal;
import ai.starwhale.mlops.datastore.impl.WalRecordDecoder;
import ai.starwhale.mlops.datastore.type.BaseValue;
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
import ai.starwhale.mlops.datastore.type.TupleValue;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.InitContext;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;

@Slf4j
public class SwReadSupport extends ReadSupport<Map<String, BaseValue>> {

    public static final String PARQUET_SCHEMA_KEY = "parquet_schema";

    public static final String SCHEMA_KEY = "sw_schema";

    public static final String META_DATA_KEY = "sw_meta";

    public static final String ERROR_FLAG_KEY = "error";

    @Override
    public ReadContext init(InitContext context) {
        return new ReadContext(context.getFileSchema());
    }

    @Override
    public RecordMaterializer<Map<String, BaseValue>> prepareForRead(Configuration configuration,
            Map<String, String> metadata,
            MessageType messageType,
            ReadContext readContext) {
        var errorFlag = metadata.get(ERROR_FLAG_KEY);
        if (Boolean.parseBoolean(errorFlag)) {
            throw new SwValidationException(
                    SwValidationException.ValidSubject.DATASTORE, "the file is invalid, ignore it!");
        }
        var schemaStr = metadata.get(SCHEMA_KEY);
        if (schemaStr == null) {
            throw new SwProcessException(ErrorType.DATASTORE, "no table schema found");
        }
        var tableMetaStr = metadata.get(META_DATA_KEY);
        if (tableMetaStr == null) {
            throw new SwProcessException(ErrorType.DATASTORE, "no table meta data found");
        }
        var parquetSchemaStr = metadata.get(PARQUET_SCHEMA_KEY);
        configuration.set(SCHEMA_KEY, schemaStr);
        configuration.set(META_DATA_KEY, tableMetaStr);
        Map<String, ColumnSchema> schema;
        if (parquetSchemaStr != null) {
            try {
                schema = new ObjectMapper()
                        .readValue(parquetSchemaStr, new TypeReference<List<ColumnSchemaDesc>>() {
                        })
                        .stream()
                        .collect(Collectors.toMap(ColumnSchemaDesc::getName, desc -> new ColumnSchema(desc, 0)));
            } catch (JsonProcessingException e) {
                throw new SwProcessException(ErrorType.DATASTORE, "failed to parse parquet schema", e);
            }
        } else {
            schema = TableSchema.fromJsonString(schemaStr).getColumnSchemaList().stream()
                    .collect(Collectors.toMap(ColumnSchema::getName, Function.identity()));
        }
        var record = new AtomicReference<ObjectValue>();
        var wal = new AtomicReference<Wal.Column>();
        var converter = SwReadSupport.createObjectConverter(schema, v -> record.set((ObjectValue) v));
        return new RecordMaterializer<>() {
            @Override
            public Map<String, BaseValue> getCurrentRecord() {
                return record.get();
            }

            @Override
            public GroupConverter getRootConverter() {
                return new GroupConverter() {
                    @Override
                    public Converter getConverter(int i) {
                        if (i == schema.size()) {
                            return new PrimitiveConverter() {
                                @Override
                                @SneakyThrows
                                public void addBinary(Binary value) {
                                    wal.set(Wal.Column.parseFrom(value.getBytes()));
                                }
                            };
                        }
                        return converter.getConverter(i);
                    }

                    @Override
                    public void start() {
                        wal.set(null);
                        converter.start();
                    }

                    @Override
                    public void end() {
                        converter.end();
                        if (wal.get() != null) {
                            this.mergeObject(record.get(), wal.get());
                        }
                    }

                    private void mergeList(ListValue value, Wal.Column wal) {
                        wal.getListValueList()
                                .forEach(col -> value.add(col.getIndex(), WalRecordDecoder.decodeValue(null, col)));
                    }

                    private void mergeMap(MapValue value, Wal.Column wal) {
                        for (var entry : wal.getMapValueList()) {
                            this.mergeMapEntry(value,
                                    WalRecordDecoder.decodeValue(null, entry.getKey()),
                                    entry.getValue());
                        }
                    }

                    private void mergeObject(ObjectValue value, Wal.Column wal) {
                        wal.getObjectValueMap().forEach((k, v) -> this.mergeMapEntry(value, k, v));
                    }

                    private <T> void mergeMapEntry(Map<T, BaseValue> value, T k, Wal.Column v) {
                        var old = value.get(k);
                        if (old == null) {
                            value.put(k, WalRecordDecoder.decodeValue(null, v));
                        } else {
                            switch (old.getColumnType()) {
                                case LIST:
                                case TUPLE:
                                    this.mergeList((ListValue) old, v);
                                    break;
                                case MAP:
                                    this.mergeMap((MapValue) old, v);
                                    break;
                                case OBJECT:
                                    if (!v.getStringValue().isEmpty()
                                            && !((ObjectValue) old).getPythonType().equals(v.getStringValue())) {
                                        var t = new ObjectValue(v.getStringValue());
                                        t.putAll((ObjectValue) old);
                                        old = t;
                                        value.put(k, t);
                                    }
                                    this.mergeObject((ObjectValue) old, v);
                                    break;
                                default:
                                    throw new RuntimeException(
                                            "can not merge :" + k + " " + old + " " + v.toString());
                            }
                        }
                    }
                };
            }
        };
    }

    private static Converter createParquetConverter(ColumnSchema schema, ValueSetter valueSetter) {
        var nullConverter = new PrimitiveConverter() {
            @Override
            public void addBoolean(boolean value) {
                if (value) {
                    valueSetter.setValue(null);
                }
            }
        };
        Converter nonNullConverter;
        switch (schema.getType()) {
            case UNKNOWN:
            case BOOL:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addBoolean(boolean value) {
                        valueSetter.setValue(BaseValue.valueOf(value));
                    }
                };
                break;
            case INT8:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addInt(int value) {
                        valueSetter.setValue(new Int8Value((byte) value));
                    }
                };
                break;
            case INT16:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addInt(int value) {
                        valueSetter.setValue(new Int16Value((short) value));
                    }
                };
                break;
            case INT32:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addInt(int value) {
                        valueSetter.setValue(new Int32Value(value));
                    }
                };
                break;
            case INT64:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addLong(long value) {
                        valueSetter.setValue(new Int64Value(value));
                    }
                };
                break;
            case FLOAT32:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addFloat(float value) {
                        valueSetter.setValue(new Float32Value(value));
                    }
                };
                break;
            case FLOAT64:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addDouble(double value) {
                        valueSetter.setValue(new Float64Value(value));
                    }
                };
                break;
            case STRING:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addBinary(Binary value) {
                        valueSetter.setValue(new StringValue(value.toStringUsingUTF8()));
                    }
                };
                break;
            case BYTES:
                nonNullConverter = new PrimitiveConverter() {
                    @Override
                    public void addBinary(Binary value) {
                        valueSetter.setValue(new BytesValue(value.toByteBuffer()));
                    }
                };
                break;
            case LIST:
            case TUPLE:
                nonNullConverter = createListConverter(schema, valueSetter);
                break;
            case MAP:
                nonNullConverter = createMapConverter(schema, valueSetter);
                break;
            case OBJECT:
                nonNullConverter = createObjectConverter(schema.getAttributesSchema(), valueSetter);
                break;
            default:
                throw new IllegalArgumentException("invalid type " + schema.getType());
        }
        return new GroupConverter() {
            @Override
            public Converter getConverter(int index) {
                if (index == 0) {
                    return nullConverter;
                }
                return nonNullConverter;
            }

            @Override
            public void start() {
            }

            @Override
            public void end() {
            }
        };
    }

    private static GroupConverter createListConverter(ColumnSchema schema, ValueSetter valueSetter) {
        var list = new AtomicReference<ListValue>();
        var elementConverter = SwReadSupport.createParquetConverter(schema.getElementSchema(),
                v -> list.get().add(v));
        var listConverter = new GroupConverter() {
            @Override
            public Converter getConverter(int i) {
                return elementConverter;
            }

            @Override
            public void start() {
            }

            @Override
            public void end() {
            }
        };
        return new GroupConverter() {
            @Override
            public Converter getConverter(int i) {
                return listConverter;
            }

            @Override
            public void start() {
                list.set(schema.getType() == ColumnType.LIST ? new ListValue() : new TupleValue());
            }

            @Override
            public void end() {
                valueSetter.setValue(list.get());
            }
        };
    }

    private static GroupConverter createMapConverter(ColumnSchema schema, ValueSetter valueSetter) {
        var map = new AtomicReference<MapValue>();
        var mapConverter = new GroupConverter() {
            private BaseValue key;
            private BaseValue value;
            private final Converter keyConverter = SwReadSupport.createParquetConverter(
                    schema.getKeySchema(),
                    v -> key = v);
            private final Converter valueConverter = SwReadSupport.createParquetConverter(
                    schema.getValueSchema(),
                    v -> value = v);

            @Override
            public Converter getConverter(int i) {
                if (i == 0) {
                    return keyConverter;
                } else {
                    return valueConverter;
                }
            }

            @Override
            public void start() {
                this.key = null;
                this.value = null;
            }

            @Override
            public void end() {
                map.get().put(this.key, this.value);
            }
        };
        return new GroupConverter() {
            @Override
            public Converter getConverter(int i) {
                return mapConverter;
            }

            @Override
            public void start() {
                map.set(new MapValue());
            }

            @Override
            public void end() {
                valueSetter.setValue(map.get());
            }
        };
    }

    private static GroupConverter createObjectConverter(Map<String, ColumnSchema> schema,
            ValueSetter valueSetter) {
        var converters = new ArrayList<Converter>();
        var map = new AtomicReference<Map<String, BaseValue>>();
        var pythonType = new AtomicReference<String>();
        for (var entry : new TreeMap<>(schema).entrySet()) {
            var name = entry.getKey();
            converters.add(SwReadSupport.createParquetConverter(entry.getValue(), value -> map.get().put(name, value)));
        }
        converters.add(new PrimitiveConverter() {
            @Override
            public void addBinary(Binary value) {
                pythonType.set(value.toStringUsingUTF8());
            }
        });
        return new GroupConverter() {
            @Override
            public Converter getConverter(int i) {
                return converters.get(i);
            }

            @Override
            public void start() {
                map.set(new HashMap<>());
                pythonType.set("placeholder");
            }

            @Override
            public void end() {
                var v = new ObjectValue(pythonType.get());
                v.putAll(map.get());
                valueSetter.setValue(v);
            }
        };
    }
}
