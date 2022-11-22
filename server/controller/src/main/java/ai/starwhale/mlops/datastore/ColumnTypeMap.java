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

import ai.starwhale.mlops.datastore.parquet.ValueSetter;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;

@Getter
@EqualsAndHashCode(callSuper = false)
public class ColumnTypeMap extends ColumnType {

    public static final String TYPE_NAME = "MAP";

    private final ColumnType keyType;

    private final ColumnType valueType;

    ColumnTypeMap(ColumnType keyType, ColumnType valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "{" + this.keyType + ":" + this.valueType + "}";
    }

    @Override
    public String getTypeName() {
        return ColumnTypeMap.TYPE_NAME;
    }

    @Override
    public ColumnSchemaDesc toColumnSchemaDesc(String name) {
        return ColumnSchemaDesc.builder()
                .name(name)
                .type(this.getTypeName())
                .keyType(this.keyType.toColumnSchemaDesc(null))
                .valueType(this.valueType.toColumnSchemaDesc(null))
                .build();
    }

    @Override
    public boolean isComparableWith(ColumnType other) {
        if (other == ColumnTypeScalar.UNKNOWN) {
            return true;
        }
        return other instanceof ColumnTypeMap
                && this.keyType.equals(((ColumnTypeMap) other).keyType)
                && this.valueType.isComparableWith(((ColumnTypeMap) other).keyType);
    }

    @Override
    public Object encode(Object value, boolean rawResult) {
        if (value == null) {
            return null;
        }
        var ret = new HashMap<>();
        ((Map<?, ?>) value).forEach(
                (k, v) -> ret.put(this.keyType.encode(k, rawResult), this.valueType.encode(v, rawResult)));
        return ret;
    }

    @Override
    public Object decode(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new SwValidationException(ValidSubject.DATASTORE,
                    "value should be of type Map, but is " + value.getClass());
        }
        var ret = new HashMap<>();
        ((Map<?, ?>) value).forEach(
                (k, v) -> ret.put(this.keyType.decode(k), this.valueType.decode(v)));
        return ret;
    }

    @Override
    public void fillWalColumnSchema(Wal.ColumnSchema.Builder builder) {
        builder.setKeyType(this.keyType.newWalColumnSchema(0, ""))
                .setValueType(this.valueType.newWalColumnSchema(0, ""));
    }

    @Override
    public Object fromWal(Wal.Column col) {
        if (col.getNullValue()) {
            return null;
        }
        var ret = new HashMap<>();
        col.getMapValueList().forEach(mapEntry -> ret.put(this.keyType.fromWal(mapEntry.getKey()),
                this.valueType.fromWal(mapEntry.getValue())));
        return ret;
    }

    @Override
    public Wal.Column.Builder toWal(int columnIndex, Object value) {
        var ret = Wal.Column.newBuilder().setIndex(columnIndex);
        if (value == null) {
            return ret.setNullValue(true);
        }
        ((Map<?, ?>) value).forEach((k, v) -> ret.addMapValue(
                Wal.Column.MapEntry.newBuilder().setKey(this.keyType.toWal(0, k))
                        .setValue(this.valueType.toWal(0, v))));
        return ret;
    }

    @Override
    public Types.Builder<?, ? extends Type> buildParquetType() {
        return Types.optionalGroup().addField(
                Types.repeatedGroup()
                        .addField(this.keyType.toParquetType("key"))
                        .addField(this.valueType.toParquetType("value"))
                        .named("map"));
    }

    @Override
    public void writeNonNullParquetValue(RecordConsumer recordConsumer, @NonNull Object value) {
        recordConsumer.startGroup();
        var mapValue = (Map<?, ?>) value;
        if (!mapValue.isEmpty()) {
            recordConsumer.startField("map", 0);
            mapValue.forEach((k, v) -> {
                recordConsumer.startGroup();
                recordConsumer.startField("key", 0);
                this.keyType.writeParquetValue(recordConsumer, k);
                recordConsumer.endField("key", 0);
                recordConsumer.startField("value", 1);
                this.valueType.writeParquetValue(recordConsumer, v);
                recordConsumer.endField("value", 1);
                recordConsumer.endGroup();
            });
            recordConsumer.endField("map", 0);
        }
        recordConsumer.endGroup();
    }

    @Override
    protected Converter getParquetValueConverter(ValueSetter valueSetter) {
        var map = new AtomicReference<Map<Object, Object>>();
        var mapConverter = new GroupConverter() {
            private Object key;
            private Object value;
            private final Converter keyConverter = keyType.getParquetConverter(v -> key = v);
            private final Converter valueConverter = valueType.getParquetConverter(v -> value = v);

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
                map.set(new HashMap<>());
            }

            @Override
            public void end() {
                valueSetter.setValue(map.get());
            }
        };
    }
}
