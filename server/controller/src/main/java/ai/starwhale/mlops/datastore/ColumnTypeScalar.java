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
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Type.Repetition;
import org.apache.parquet.schema.Types;

@Getter
public class ColumnTypeScalar extends ColumnType {

    // if the column type is UNKNOWN, all values in the column are null. It is used for those columns whose type is
    // unknown. A UNKNOWN column will be changed to other types when a value other than null is written into the column.
    public static final ColumnTypeScalar UNKNOWN = new ColumnTypeScalar("unknown", 1);
    public static final ColumnTypeScalar BOOL = new ColumnTypeScalar("bool", 1);
    public static final ColumnTypeScalar INT8 = new ColumnTypeScalar("int", 8);
    public static final ColumnTypeScalar INT16 = new ColumnTypeScalar("int", 16);
    public static final ColumnTypeScalar INT32 = new ColumnTypeScalar("int", 32);
    public static final ColumnTypeScalar INT64 = new ColumnTypeScalar("int", 64);
    public static final ColumnTypeScalar FLOAT32 = new ColumnTypeScalar("float", 32);
    public static final ColumnTypeScalar FLOAT64 = new ColumnTypeScalar("float", 64);
    public static final ColumnTypeScalar STRING = new ColumnTypeScalar("string", 8);
    public static final ColumnTypeScalar BYTES = new ColumnTypeScalar("bytes", 8);

    private static final Map<String, ColumnType> typeMap = Map.of(
            UNKNOWN.getTypeName(), UNKNOWN,
            BOOL.getTypeName(), BOOL,
            INT8.getTypeName(), INT8,
            INT16.getTypeName(), INT16,
            INT32.getTypeName(), INT32,
            INT64.getTypeName(), INT64,
            FLOAT32.getTypeName(), FLOAT32,
            FLOAT64.getTypeName(), FLOAT64,
            STRING.getTypeName(), STRING,
            BYTES.getTypeName(), BYTES);

    private final String category;
    private final int nbits;

    private ColumnTypeScalar(String category, int nbits) {
        this.category = category;
        this.nbits = nbits;
    }

    public static ColumnType getColumnTypeByName(String typeName) {
        return typeMap.get(typeName.toUpperCase());
    }

    public String getTypeName() {
        if (this.category.equals(INT32.getCategory())
                || this.category.equals(FLOAT32.getCategory())) {
            return this.category.toUpperCase() + this.nbits;
        } else {
            return this.category.toUpperCase();
        }
    }

    @Override
    public ColumnSchemaDesc toColumnSchemaDesc(String name) {
        return ColumnSchemaDesc.builder()
                .name(name)
                .type(this.getTypeName())
                .build();
    }

    @Override
    public boolean isComparableWith(ColumnType other) {
        if (this == UNKNOWN || other == UNKNOWN) {
            return true;
        }
        if (!(other instanceof ColumnTypeScalar)) {
            return false;
        }
        var otherCategory = ((ColumnTypeScalar) other).category;
        if (this.category.equals(otherCategory)) {
            return true;
        }
        return (this.category.equals(INT32.category) || this.category.equals(FLOAT32.category))
                && (otherCategory.equals(INT32.category) || otherCategory.equals(FLOAT32.category));
    }

    @Override
    public String toString() {
        return this.getTypeName();
    }

    @Override
    public Object encode(Object value, boolean rawResult) {
        if (value == null) {
            return null;
        }
        if (rawResult) {
            if (this == BOOL
                    || this == INT8
                    || this == INT16
                    || this == INT32
                    || this == INT64
                    || this == FLOAT32
                    || this == FLOAT64
                    || this == STRING) {
                return value.toString();
            } else if (this == BYTES) {
                return StandardCharsets.UTF_8.decode((ByteBuffer) value).toString();
            }
        } else {
            if (this == BOOL) {
                return (Boolean) value ? "1" : "0";
            } else if (this == INT8) {
                return StringUtils.leftPad(Integer.toHexString(((Number) value).byteValue() & 0xFF), 2, "0");
            } else if (this == INT16) {
                return StringUtils.leftPad(Integer.toHexString(((Number) value).shortValue() & 0xFFFF), 4, "0");
            } else if (this == INT32) {
                return StringUtils.leftPad(Integer.toHexString(((Number) value).intValue()), 8, "0");
            } else if (this == INT64) {
                return StringUtils.leftPad(Long.toHexString(((Number) value).longValue()), 16, "0");
            } else if (this == FLOAT32) {
                return StringUtils.leftPad(Integer.toHexString(Float.floatToIntBits((Float) value)), 8, "0");
            } else if (this == FLOAT64) {
                return StringUtils.leftPad(Long.toHexString(Double.doubleToLongBits((Double) value)), 16, "0");
            } else if (this == STRING) {
                return value;
            } else if (this == BYTES) {
                return Base64.getEncoder().encodeToString(((ByteBuffer) value).array());
            }
        }
        throw new IllegalArgumentException("invalid type " + this);
    }

    @Override
    public Object decode(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("value should be of type String");
            }
            if (this == UNKNOWN) {
                throw new IllegalArgumentException("invalid unknown value " + value);
            } else if (this == BOOL) {
                if (value.equals("1")) {
                    return true;
                } else if (value.equals("0")) {
                    return false;
                }
                throw new IllegalArgumentException("invalid bool value " + value);
            } else if (this == INT8) {
                return (byte) (Integer.parseInt((String) value, 16) & 0xFF);
            } else if (this == INT16) {
                return (short) (Integer.parseInt((String) value, 16) & 0xFFFF);
            } else if (this == INT32) {
                return Integer.parseUnsignedInt((String) value, 16);
            } else if (this == INT64) {
                return Long.parseUnsignedLong((String) value, 16);
            } else if (this == FLOAT32) {
                return Float.intBitsToFloat(Integer.parseUnsignedInt((String) value, 16));
            } else if (this == FLOAT64) {
                return Double.longBitsToDouble(Long.parseUnsignedLong((String) value, 16));
            } else if (this == STRING) {
                return value;
            } else if (this == BYTES) {
                return ByteBuffer.wrap(Base64.getDecoder().decode((String) value));
            }
            throw new IllegalArgumentException("invalid type " + this);
        } catch (Exception e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    MessageFormat.format("can not decode value {0} for type {1}: {2}", value, this, e));
        }
    }

    @Override
    public Object fromWal(Wal.Column col) {
        if (col.getNullValue()) {
            return null;
        }
        if (this == UNKNOWN) {
            return null;
        } else if (this == BOOL) {
            return col.getBoolValue();
        } else if (this == INT8) {
            return (byte) col.getIntValue();
        } else if (this == INT16) {
            return (short) col.getIntValue();
        } else if (this == INT32) {
            return (int) col.getIntValue();
        } else if (this == INT64) {
            return col.getIntValue();
        } else if (this == FLOAT32) {
            return col.getFloatValue();
        } else if (this == FLOAT64) {
            return col.getDoubleValue();
        } else if (this == STRING) {
            return col.getStringValue();
        } else if (this == BYTES) {
            return ByteBuffer.wrap(col.getBytesValue().toByteArray());
        }
        throw new IllegalArgumentException("invalid type " + this);
    }

    @Override
    public Wal.Column.Builder toWal(int columnIndex, Object value) {
        var ret = Wal.Column.newBuilder().setIndex(columnIndex);
        if (columnIndex >= 0) {
            if (value == null) {
                ret.setNullValue(true);
            } else {
                if (this == UNKNOWN) {
                    ret.setNullValue(true);
                } else if (this == BOOL) {
                    ret.setBoolValue((Boolean) value);
                } else if (this == INT8
                        || this == INT16
                        || this == INT32
                        || this == INT64) {
                    ret.setIntValue(((Number) value).longValue());
                } else if (this == FLOAT32) {
                    ret.setFloatValue((Float) value);
                } else if (this == FLOAT64) {
                    ret.setDoubleValue((Double) value);
                } else if (this == STRING) {
                    ret.setStringValue((String) value);
                } else if (this == BYTES) {
                    ret.setBytesValue(ByteString.copyFrom(((ByteBuffer) value).array()));
                } else {
                    throw new IllegalArgumentException("invalid type " + this);
                }
            }
        }
        return ret;
    }

    @Override
    public Types.Builder<?, ? extends Type> buildParquetType() {
        if (this == UNKNOWN || this == BOOL) {
            return Types.primitive(PrimitiveTypeName.BOOLEAN, Repetition.OPTIONAL);
        } else if (this == INT8) {
            return Types.primitive(PrimitiveTypeName.INT32, Repetition.OPTIONAL)
                    .as(LogicalTypeAnnotation.intType(8, true));
        } else if (this == INT16) {
            return Types.primitive(PrimitiveTypeName.INT32, Repetition.OPTIONAL)
                    .as(LogicalTypeAnnotation.intType(16, true));
        } else if (this == INT32) {
            return Types.primitive(PrimitiveTypeName.INT32, Repetition.OPTIONAL);
        } else if (this == INT64) {
            return Types.primitive(PrimitiveTypeName.INT64, Repetition.OPTIONAL);
        } else if (this == FLOAT32) {
            return Types.primitive(PrimitiveTypeName.FLOAT, Repetition.OPTIONAL);
        } else if (this == FLOAT64) {
            return Types.primitive(PrimitiveTypeName.DOUBLE, Repetition.OPTIONAL);
        } else if (this == STRING) {
            return Types.primitive(PrimitiveTypeName.BINARY, Repetition.OPTIONAL)
                    .as(LogicalTypeAnnotation.stringType());
        } else if (this == BYTES) {
            return Types.primitive(PrimitiveTypeName.BINARY, Repetition.OPTIONAL);
        } else {
            throw new IllegalArgumentException("invalid type " + this);
        }
    }

    @Override
    public void writeNonNullParquetValue(RecordConsumer recordConsumer, @NonNull Object value) {
        if (this == BOOL) {
            recordConsumer.addBoolean((Boolean) value);
        } else if (this == INT8) {
            recordConsumer.addInteger((Byte) value);
        } else if (this == INT16) {
            recordConsumer.addInteger((Short) value);
        } else if (this == INT32) {
            recordConsumer.addInteger((Integer) value);
        } else if (this == INT64) {
            recordConsumer.addLong((Long) value);
        } else if (this == FLOAT32) {
            recordConsumer.addFloat((Float) value);
        } else if (this == FLOAT64) {
            recordConsumer.addDouble((Double) value);
        } else if (this == STRING) {
            recordConsumer.addBinary(Binary.fromString((String) value));
        } else if (this == BYTES) {
            recordConsumer.addBinary(Binary.fromConstantByteBuffer((ByteBuffer) value));
        } else {
            throw new IllegalArgumentException("invalid type " + this);
        }
    }

    @Override
    protected Converter getParquetValueConverter(ValueSetter valueSetter) {
        return new PrimitiveConverter() {
            @Override
            public void addBinary(Binary value) {
                if (ColumnTypeScalar.this == STRING) {
                    valueSetter.setValue(value.toStringUsingUTF8());
                } else if (ColumnTypeScalar.this == BYTES) {
                    valueSetter.setValue(value.toByteBuffer());
                } else {
                    throw new SwProcessException(ErrorType.DATASTORE,
                            "expected " + ColumnTypeScalar.this + ", actual binary");
                }
            }

            @Override
            public void addBoolean(boolean value) {
                if (ColumnTypeScalar.this == BOOL) {
                    valueSetter.setValue(value);
                } else {
                    throw new SwProcessException(ErrorType.DATASTORE,
                            "expected " + ColumnTypeScalar.this + ", actual boolean");
                }
            }

            @Override
            public void addDouble(double value) {
                if (ColumnTypeScalar.this == FLOAT64) {
                    valueSetter.setValue(value);
                } else {
                    throw new SwProcessException(ErrorType.DATASTORE,
                            "expected " + ColumnTypeScalar.this + ", actual double");
                }
            }

            @Override
            public void addFloat(float value) {
                if (ColumnTypeScalar.this == FLOAT32) {
                    valueSetter.setValue(value);
                } else {
                    throw new SwProcessException(ErrorType.DATASTORE,
                            "expected " + ColumnTypeScalar.this + ", actual float");
                }
            }

            @Override
            public void addInt(int value) {
                if (ColumnTypeScalar.this == INT8) {
                    valueSetter.setValue((byte) value);
                } else if (ColumnTypeScalar.this == INT16) {
                    valueSetter.setValue((short) value);
                } else if (ColumnTypeScalar.this == INT32) {
                    valueSetter.setValue(value);
                } else {
                    throw new SwProcessException(ErrorType.DATASTORE,
                            "expected " + ColumnTypeScalar.this + ", actual int");
                }
            }

            @Override
            public void addLong(long value) {
                if (ColumnTypeScalar.this == INT64) {
                    valueSetter.setValue(value);
                } else {
                    throw new SwProcessException(ErrorType.DATASTORE,
                            "expected " + ColumnTypeScalar.this + ", actual long");
                }
            }
        };
    }
}
