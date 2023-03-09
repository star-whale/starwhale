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
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Type.Repetition;
import org.apache.parquet.schema.Types;

@Getter
public abstract class ColumnType {

    public static ColumnType fromColumnSchemaDesc(ColumnSchemaDesc schema) {
        var typeName = schema.getType().toUpperCase();
        if (typeName.equals(ColumnTypeList.TYPE_NAME) || typeName.equals(ColumnTypeTuple.TYPE_NAME)) {
            var elementType = schema.getElementType();
            if (elementType == null) {
                throw new IllegalArgumentException("elementType should not be null for " + typeName);
            }
            if (typeName.equals(ColumnTypeList.TYPE_NAME)) {
                return new ColumnTypeList(ColumnType.fromColumnSchemaDesc(elementType));
            } else {
                return new ColumnTypeTuple(ColumnType.fromColumnSchemaDesc(elementType));
            }
        } else if (typeName.equals(ColumnTypeMap.TYPE_NAME)) {
            var keyType = schema.getKeyType();
            if (keyType == null) {
                throw new IllegalArgumentException("keyType should not be null for MAP");
            }
            var valueType = schema.getValueType();
            if (valueType == null) {
                throw new IllegalArgumentException("valueType should not be null for MAP");
            }
            return new ColumnTypeMap(ColumnType.fromColumnSchemaDesc(keyType),
                    ColumnType.fromColumnSchemaDesc(valueType));
        } else if (typeName.equals(ColumnTypeObject.TYPE_NAME)) {
            var attributes = schema.getAttributes();
            if (attributes == null || attributes.isEmpty()) {
                throw new IllegalArgumentException("attributes should not be null or empty for OBJECT");
            }
            var pythonType = schema.getPythonType();
            if (pythonType == null || pythonType.isEmpty()) {
                throw new IllegalArgumentException("pythonType should not be null or empty for OBJECT");
            }
            var attributeMap = new HashMap<String, ColumnType>();
            for (var attr : attributes) {
                if (attributeMap.putIfAbsent(attr.getName(), ColumnType.fromColumnSchemaDesc(attr)) != null) {
                    throw new IllegalArgumentException("duplicate attribute name " + attr.getName());
                }
            }
            return new ColumnTypeObject(schema.getPythonType(), attributeMap);
        }
        var columnType = ColumnTypeScalar.getColumnTypeByName(typeName);
        if (columnType == null) {
            throw new IllegalArgumentException("invalid type " + typeName);
        }
        return columnType;
    }

    public abstract String getTypeName();

    public abstract ColumnSchemaDesc toColumnSchemaDesc(String name);

    public abstract boolean isComparableWith(ColumnType other);

    public static int compare(ColumnType type1, Object value1, ColumnType type2, Object value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        if (type1 instanceof ColumnTypeScalar && type2 instanceof ColumnTypeScalar) {
            return ColumnType.compareScalar((ColumnTypeScalar) type1, value1, (ColumnTypeScalar) type2, value2);
        } else if (type1 instanceof ColumnTypeList && type2 instanceof ColumnTypeList) {
            return ColumnType.compareList((ColumnTypeList) type1, value1, (ColumnTypeList) type2, value2);
        } else if (type1 instanceof ColumnTypeMap && type2 instanceof ColumnTypeMap) {
            return ColumnType.compareMap((ColumnTypeMap) type1, value1, (ColumnTypeMap) type2, value2);
        } else if (type1 instanceof ColumnTypeObject && type2 instanceof ColumnTypeObject) {
            return ColumnType.compareObject((ColumnTypeObject) type1, value1, (ColumnTypeObject) type2, value2);
        }
        throw new IllegalArgumentException("can not compare " + type1 + " with " + type2);
    }

    private static int compareScalar(ColumnTypeScalar type1, Object value1, ColumnTypeScalar type2, Object value2) {
        if (type1 == type2) {
            if (type1 == ColumnTypeScalar.BOOL) {
                return ((Boolean) value1).compareTo((Boolean) value2);
            } else if (type1 == ColumnTypeScalar.INT8) {
                return ((Byte) value1).compareTo((Byte) value2);
            } else if (type1 == ColumnTypeScalar.INT16) {
                return ((Short) value1).compareTo((Short) value2);
            } else if (type1 == ColumnTypeScalar.INT32) {
                return ((Integer) value1).compareTo((Integer) value2);
            } else if (type1 == ColumnTypeScalar.INT64) {
                return ((Long) value1).compareTo((Long) value2);
            } else if (type1 == ColumnTypeScalar.FLOAT32) {
                return ((Float) value1).compareTo((Float) value2);
            } else if (type1 == ColumnTypeScalar.FLOAT64) {
                return ((Double) value1).compareTo((Double) value2);
            } else if (type1 == ColumnTypeScalar.STRING) {
                return ((String) value1).compareTo((String) value2);
            } else if (type1 == ColumnTypeScalar.BYTES) {
                return ((ByteBuffer) value1).compareTo((ByteBuffer) value2);
            }
        } else {
            var category1 = type1.getCategory();
            var category2 = type2.getCategory();
            if (category1.equals(category2)) {
                if (category1.equals(ColumnTypeScalar.INT32.getCategory())) {
                    return Long.compare(((Number) value1).longValue(), ((Number) value2).longValue());
                } else if (category1.equals(ColumnTypeScalar.FLOAT32.getCategory())) {
                    return Double.compare(((Number) value1).doubleValue(), ((Number) value2).doubleValue());
                }
            } else {
                if (category1.equals(ColumnTypeScalar.INT32.getCategory())
                        && category2.equals(ColumnTypeScalar.FLOAT32.getCategory())) {
                    if (type1 == ColumnTypeScalar.INT64) {
                        long v1 = (Long) value1;
                        if (v1 > (1L << 53)) {
                            return BigDecimal.valueOf(v1)
                                    .compareTo(BigDecimal.valueOf(((Number) value2).doubleValue()));
                        }
                    }
                    return Double.compare(((Number) value1).doubleValue(), ((Number) value2).doubleValue());
                } else if (category1.equals(ColumnTypeScalar.FLOAT32.getCategory())
                        && category2.equals(ColumnTypeScalar.INT32.getCategory())) {
                    return -ColumnType.compare(type2, value2, type1, value1);
                }
            }
        }
        throw new IllegalArgumentException("can not compare " + type1 + " with " + type2);
    }

    private static int compareList(ColumnTypeList type1, Object value1, ColumnTypeList type2, Object value2) {
        var iter1 = ((List<?>) value1).iterator();
        var iter2 = ((List<?>) value2).iterator();
        for (; ; ) {
            if (iter1.hasNext() && iter2.hasNext()) {
                var result = ColumnType.compare(type1.getElementType(),
                        iter1.next(),
                        type2.getElementType(),
                        iter2.next());
                if (result != 0) {
                    return result;
                }
            } else if (iter1.hasNext()) {
                return 1;
            } else if (iter2.hasNext()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private static int compareMap(ColumnTypeMap type1, Object value1, ColumnTypeMap type2, Object value2) {
        var map1 = (Map<?, ?>) value1;
        var map2 = (Map<?, ?>) value2;
        Object minDiffKey = null;
        int result = 0;
        for (var iter = Stream.concat(map1.keySet().stream(), map2.keySet().stream()).iterator();
                iter.hasNext(); ) {
            var key = iter.next();
            if (minDiffKey == null || ColumnType.compare(type1.getKeyType(), key, type1.getKeyType(), minDiffKey) < 0) {
                var v1 = map1.get(key);
                var v2 = map2.get(key);
                int t;
                if (v1 == null) {
                    t = -1;
                } else if (v2 == null) {
                    t = 1;
                } else {
                    t = ColumnType.compare(type1.getValueType(), v1, type2.getValueType(), v2);
                }
                if (t != 0) {
                    minDiffKey = key;
                    result = t;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static int compareObject(ColumnTypeObject type1, Object value1, ColumnTypeObject type2, Object value2) {
        var attr1 = type1.getAttributes();
        var attr2 = type2.getAttributes();
        var map1 = (Map<String, ?>) value1;
        var map2 = (Map<String, ?>) value2;
        int result = 0;
        String minDiffKey = null;
        for (var entry : map1.entrySet()) {
            var key = entry.getKey();
            var v1 = entry.getValue();
            if (minDiffKey == null || key.compareTo(minDiffKey) < 0) {
                var v2 = map2.get(key);
                var t = ColumnType.compare(attr1.get(key), v1, attr2.get(key), v2);
                if (t != 0) {
                    minDiffKey = key;
                    result = t;
                }
            }
        }
        return result;
    }

    public abstract Object encode(Object value, boolean rawResult);

    public abstract Object decode(Object value);

    public Wal.ColumnSchema.Builder newWalColumnSchema(int columnIndex, String columnName) {
        var ret = Wal.ColumnSchema.newBuilder()
                .setColumnIndex(columnIndex)
                .setColumnName(columnName)
                .setColumnType(this.getTypeName());
        this.fillWalColumnSchema(ret);
        return ret;
    }

    public abstract void fillWalColumnSchema(Wal.ColumnSchema.Builder builder);

    public abstract Object fromWal(Wal.Column col);

    public abstract Wal.Column.Builder toWal(int columnIndex, Object value);

    public Type toParquetType(String name) {
        var builder = Types.optionalGroup();
        builder.addField(Types.primitive(PrimitiveTypeName.BOOLEAN, Repetition.REQUIRED).named("null"));
        builder.addField(this.buildParquetType().named("value"));
        return builder.named(name);
    }

    protected abstract Types.Builder<?, ? extends Type> buildParquetType();

    public void writeParquetValue(RecordConsumer recordConsumer, Object value) {
        recordConsumer.startGroup();
        recordConsumer.startField("null", 0);
        recordConsumer.addBoolean(value == null);
        recordConsumer.endField("null", 0);
        if (value != null) {
            recordConsumer.startField("value", 1);
            this.writeNonNullParquetValue(recordConsumer, value);
            recordConsumer.endField("value", 1);
        }
        recordConsumer.endGroup();
    }

    protected abstract void writeNonNullParquetValue(RecordConsumer recordConsumer, @NonNull Object value);

    public Converter getParquetConverter(ValueSetter valueSetter) {
        var nullConverter = new PrimitiveConverter() {
            @Override
            public void addBoolean(boolean value) {
                if (value) {
                    valueSetter.setValue(null);
                }
            }
        };
        var valueConverter = getParquetValueConverter(valueSetter);
        return new GroupConverter() {
            @Override
            public Converter getConverter(int index) {
                if (index == 0) {
                    return nullConverter;
                }
                return valueConverter;
            }

            @Override
            public void start() {
            }

            @Override
            public void end() {
            }
        };
    }

    protected abstract Converter getParquetValueConverter(ValueSetter valueSetter);
}
