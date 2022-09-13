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

import ai.starwhale.mlops.exception.SwValidationException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Map;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class ColumnType {

    // if the column type is UNKNOWN, all values in the column are null. It is used for those columns whose type is
    // unknown. A UNKNOWN column will be changed to other types when a value other than null is written into the column.
    public static final ColumnType UNKNOWN = new ColumnType("unknown", 1);
    public static final ColumnType BOOL = new ColumnType("bool", 1);
    public static final ColumnType INT8 = new ColumnType("int", 8);
    public static final ColumnType INT16 = new ColumnType("int", 16);
    public static final ColumnType INT32 = new ColumnType("int", 32);
    public static final ColumnType INT64 = new ColumnType("int", 64);
    public static final ColumnType FLOAT32 = new ColumnType("float", 32);
    public static final ColumnType FLOAT64 = new ColumnType("float", 64);
    public static final ColumnType STRING = new ColumnType("string", 8);
    public static final ColumnType BYTES = new ColumnType("bytes", 8);

    private static final Map<Class<?>, ColumnType> typeMap = Map.of(
            Boolean.class, BOOL,
            Byte.class, INT8,
            Short.class, INT16,
            Integer.class, INT32,
            Long.class, INT64,
            Float.class, FLOAT32,
            Double.class, FLOAT64,
            String.class, STRING);

    private static final Map<String, ColumnType> typeMapByName = Map.of(
            UNKNOWN.toString(), UNKNOWN,
            BOOL.toString(), BOOL,
            INT8.toString(), INT8,
            INT16.toString(), INT16,
            INT32.toString(), INT32,
            INT64.toString(), INT64,
            FLOAT32.toString(), FLOAT32,
            FLOAT64.toString(), FLOAT64,
            STRING.toString(), STRING,
            BYTES.toString(), BYTES);

    private final String category;
    private final int nbits;

    ColumnType(String category, int nbits) {
        this.category = category;
        this.nbits = nbits;
    }

    public static ColumnType getColumnType(Object value) {
        if (value == null) {
            return UNKNOWN;
        }
        var ret = typeMap.get(value.getClass());
        if (ret == null) {
            if (value instanceof ByteBuffer) {
                return BYTES;
            }
            throw new IllegalArgumentException("unsupported column type " + value.getClass());
        }
        return ret;
    }

    public static ColumnType getColumnTypeByName(String typeName) {
        var ret = typeMapByName.get(typeName.toUpperCase());
        if (ret == null) {
            throw new IllegalArgumentException("invalid column type " + typeName);
        }
        return ret;
    }

    @Override
    public String toString() {
        if (this.category.equals(ColumnType.INT32.category) || this.category.equals(ColumnType.FLOAT32.category)) {
            return this.category.toUpperCase() + this.nbits;
        } else {
            return this.category.toUpperCase();
        }
    }

    @SneakyThrows
    public String encode(Object value, boolean rawResult) {
        if (value == null) {
            return null;
        }
        if (rawResult) {
            if (this == ColumnType.BOOL
                    || this == ColumnType.INT8
                    || this == ColumnType.INT16
                    || this == ColumnType.INT32
                    || this == ColumnType.INT64
                    || this == ColumnType.FLOAT32
                    || this == ColumnType.FLOAT64
                    || this == ColumnType.STRING) {
                return value.toString();
            } else if (this == ColumnType.BYTES) {
                return StandardCharsets.UTF_8.decode((ByteBuffer) value).toString();
            }
        } else {
            if (this == ColumnType.BOOL) {
                return (Boolean) value ? "1" : "0";
            } else if (this == ColumnType.INT8
                    || this == ColumnType.INT16
                    || this == ColumnType.INT32
                    || this == ColumnType.INT64) {
                return Long.toHexString(((Number) value).longValue());
            } else if (this == ColumnType.FLOAT32) {
                return Integer.toHexString(Float.floatToIntBits((Float) value));
            } else if (this == ColumnType.FLOAT64) {
                return Long.toHexString(Double.doubleToLongBits((Double) value));
            } else if (this == ColumnType.STRING) {
                return (String) value;
            } else if (this == ColumnType.BYTES) {
                return Base64.getEncoder().encodeToString(((ByteBuffer) value).array());
            }
        }
        throw new IllegalArgumentException("invalid type " + this);
    }

    public Object decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            if (this == ColumnType.UNKNOWN) {
                throw new IllegalArgumentException("invalid unknown value " + value);
            } else if (this == ColumnType.BOOL) {
                if (value.equals("1")) {
                    return true;
                } else if (value.equals("0")) {
                    return false;
                }
                throw new IllegalArgumentException("invalid bool value " + value);
            } else if (this == ColumnType.INT8) {
                return Byte.parseByte(value, 16);
            } else if (this == ColumnType.INT16) {
                return Short.parseShort(value, 16);
            } else if (this == ColumnType.INT32) {
                return Integer.parseInt(value, 16);
            } else if (this == ColumnType.INT64) {
                return Long.parseLong(value, 16);
            } else if (this == ColumnType.FLOAT32) {
                return Float.intBitsToFloat(Integer.parseInt(value, 16));
            } else if (this == ColumnType.FLOAT64) {
                return Double.longBitsToDouble(Long.parseLong(value, 16));
            } else if (this == ColumnType.STRING) {
                return value;
            } else if (this == ColumnType.BYTES) {
                return ByteBuffer.wrap(Base64.getDecoder().decode(value));
            }
            throw new IllegalArgumentException("invalid type " + this);
        } catch (Exception e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE).tip(
                    MessageFormat.format("can not decode value {0} for type {1}: {2}", value, this, e.getMessage()));
        }
    }
}
