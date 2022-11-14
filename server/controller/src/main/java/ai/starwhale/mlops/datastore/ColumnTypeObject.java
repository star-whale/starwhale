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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
public class ColumnTypeObject extends ColumnType {

    private static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("^[\\p{Alnum}_]+$");

    public static final String TYPE_NAME = "OBJECT";

    private final String pythonType;

    private final Map<String, ColumnType> attributes;

    ColumnTypeObject(@NonNull String pythonType, @NonNull Map<String, ColumnType> attributes) {
        this.pythonType = pythonType;
        this.attributes = attributes;
        attributes.keySet().forEach(key -> {
            if (!ATTRIBUTE_NAME_PATTERN.matcher(key).matches()) {
                throw new IllegalArgumentException(
                        "invalid attribute name " + key + ". only alphabets, digits, and underscore are allowed");
            }
        });
    }

    @Override
    public String toString() {
        var ret = new StringBuilder(this.getPythonType());
        ret.append('{');
        var len = ret.length();
        attributes.forEach((key, value) -> {
            if (ret.length() > len) {
                ret.append(',');
            }
            ret.append(key);
            ret.append(':');
            ret.append(value);
        });
        ret.append('}');
        return ret.toString();
    }

    @Override
    public String getTypeName() {
        return ColumnTypeObject.TYPE_NAME;
    }

    @Override
    public ColumnSchemaDesc toColumnSchemaDesc(String name) {
        return ColumnSchemaDesc.builder()
                .name(name)
                .type(this.getTypeName())
                .pythonType(this.getPythonType())
                .attributes((this.attributes.entrySet().stream()
                        .map(entry -> entry.getValue().toColumnSchemaDesc(entry.getKey()))
                        .collect(Collectors.toList())))
                .build();
    }

    @Override
    public boolean isComparableWith(ColumnType other) {
        if (other == ColumnTypeScalar.UNKNOWN) {
            return true;
        }
        return other instanceof ColumnTypeObject
                && this.pythonType.equals(((ColumnTypeObject) other).pythonType)
                && this.attributes.equals(((ColumnTypeObject) other).attributes);
    }

    @Override
    public Object encode(Object value, boolean rawResult) {
        if (value == null) {
            return null;
        }
        var ret = new HashMap<String, Object>();
        //noinspection unchecked
        ((Map<String, ?>) value).forEach((k, v) -> {
            var type = this.attributes.get(k);
            if (type == null) {
                throw new IllegalArgumentException("invalid attribute " + k);
            }
            ret.put(k, type.encode(v, rawResult));
        });
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
        var ret = new HashMap<String, Object>();
        //noinspection unchecked
        ((Map<String, ?>) value).forEach((k, v) -> {
            var type = this.attributes.get(k);
            if (type == null) {
                throw new SwValidationException(ValidSubject.DATASTORE, "invalid attribute " + k);
            }
            ret.put(k, type.decode(v));
        });
        return ret;
    }

    @Override
    public void fillWalColumnSchema(Wal.ColumnSchema.Builder builder) {
        builder.setPythonType(this.pythonType)
                .addAllAttributes(this.attributes.entrySet().stream()
                        .map(entry -> entry.getValue().newWalColumnSchema(0, entry.getKey()).build())
                        .collect(Collectors.toList()));
    }

    @Override
    public Object fromWal(Wal.Column col) {
        if (col.getNullValue()) {
            return null;
        }
        var ret = new HashMap<>();
        col.getObjectValueMap().forEach((k, v) -> {
            var type = this.attributes.get(k);
            if (type == null) {
                throw new IllegalArgumentException("invalid attribute " + k);
            }
            ret.put(k, type.fromWal(v));
        });
        return ret;
    }

    @Override
    public Wal.Column.Builder toWal(int columnIndex, Object value) {
        var ret = Wal.Column.newBuilder().setIndex(columnIndex);
        if (value == null) {
            return ret.setNullValue(true);
        }
        //noinspection unchecked
        ((Map<String, ?>) value).forEach((k, v) -> {
            var type = this.attributes.get(k);
            if (type == null) {
                throw new IllegalArgumentException("invalid attribute " + k);
            }
            ret.putObjectValue(k, type.toWal(0, v).build());
        });
        return ret;
    }

    @Override
    public Types.Builder<?, ? extends Type> buildParquetType() {
        var builder = Types.optionalGroup();
        for (var entry : this.attributes.entrySet()) {
            builder.addField(entry.getValue().toParquetType(entry.getKey()));
        }
        return builder;
    }

    @Override
    public void writeNonNullParquetValue(RecordConsumer recordConsumer, @NonNull Object value) {
        recordConsumer.startGroup();
        ColumnTypeObject.writeMapValue(recordConsumer, this.attributes, (Map<?, ?>) value);
        recordConsumer.endGroup();
    }

    public static void writeMapValue(RecordConsumer recordConsumer,
            Map<String, ColumnType> schema,
            Map<?, ?> value) {
        int index = 0;
        for (var entry : new TreeMap<>(schema).entrySet()) {
            var attrType = entry.getValue();
            var attrValue = value.get(entry.getKey());
            if (value.containsKey(entry.getKey())) {
                recordConsumer.startField(entry.getKey(), index);
                attrType.writeParquetValue(recordConsumer, attrValue);
                recordConsumer.endField(entry.getKey(), index);
            }
            ++index;
        }
    }

    @Override
    protected Converter getParquetValueConverter(ValueSetter valueSetter) {
        return ColumnTypeObject.getObjectConverter(valueSetter, this.attributes);
    }

    public static GroupConverter getObjectConverter(ValueSetter valueSetter, Map<String, ColumnType> schema) {
        var converters = new ArrayList<Converter>();
        var map = new AtomicReference<Map<String, Object>>();
        for (var entry : new TreeMap<>(schema).entrySet()) {
            var name = entry.getKey();
            var type = entry.getValue();
            converters.add(type.getParquetConverter(value -> map.get().put(name, value)));
        }
        return new GroupConverter() {
            @Override
            public Converter getConverter(int i) {
                return converters.get(i);
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
