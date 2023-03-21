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

package ai.starwhale.mlops.datastore.type;

import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.Wal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public interface BaseValue extends Comparable<BaseValue> {

    ColumnType getColumnType();

    static ColumnType getColumnType(BaseValue value) {
        if (value == null) {
            return ColumnType.UNKNOWN;
        }
        return value.getColumnType();
    }

    Object encode(boolean rawResult, boolean encodeWithType);

    static Object encode(BaseValue value, boolean rawResult, boolean encodeWithType) {
        if (value == null) {
            if (encodeWithType) {
                var ret = new HashMap<String, Object>();
                ret.put("value", null);
                return ret;
            } else {
                return null;
            }
        }
        return value.encode(rawResult, encodeWithType);
    }

    Wal.Column.Builder encodeWal();

    static Wal.Column.Builder encodeWal(BaseValue value) {
        if (value == null) {
            return Wal.Column.newBuilder().setNullValue(true).setType(ColumnType.UNKNOWN.getIndex());
        }
        return value.encodeWal().setType(value.getColumnType().getIndex());
    }

    @Override
    int compareTo(@NonNull BaseValue other);

    static BaseValue valueOf(boolean value) {
        return value ? BoolValue.TRUE : BoolValue.FALSE;
    }

    static BaseValue valueOf(byte value) {
        return new Int8Value(value);
    }

    static BaseValue valueOf(short value) {
        return new Int16Value(value);
    }

    static BaseValue valueOf(int value) {
        return new Int32Value(value);
    }

    static BaseValue valueOf(long value) {
        return new Int64Value(value);
    }

    static BaseValue valueOf(String value) {
        return new StringValue(value);
    }

    static BaseValue valueOf(ByteBuffer value) {
        return new BytesValue(value);
    }

    static BaseValue valueOf(List<?> value) {
        var ret = new ListValue();
        for (var e : value) {
            ret.add(BaseValue.valueOf(e));
        }
        return ret;
    }

    static BaseValue valueOf(Map<?, ?> value) {
        var ret = new MapValue();
        value.forEach((k, v) -> ret.put(BaseValue.valueOf(k), BaseValue.valueOf(v)));
        return ret;
    }

    static BaseValue valueOf(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof BaseValue) {
            return (BaseValue) value;
        } else if (value instanceof Boolean) {
            return (Boolean) value ? BoolValue.TRUE : BoolValue.FALSE;
        } else if (value instanceof Byte) {
            return new Int8Value((Byte) value);
        } else if (value instanceof Short) {
            return new Int16Value((Short) value);
        } else if (value instanceof Integer) {
            return new Int32Value((Integer) value);
        } else if (value instanceof Long) {
            return new Int64Value((Long) value);
        } else if (value instanceof Float) {
            return new Float32Value((Float) value);
        } else if (value instanceof Double) {
            return new Float64Value((Double) value);
        } else if (value instanceof String) {
            return new StringValue((String) value);
        } else if (value instanceof ByteBuffer) {
            return new BytesValue((ByteBuffer) value);
        } else if (value instanceof List) {
            return BaseValue.valueOf((List<?>) value);
        } else if (value instanceof Map) {
            return BaseValue.valueOf((Map<?, ?>) value);
        }
        throw new IllegalArgumentException("invalid class " + value.getClass());
    }

    static int compare(BaseValue value1, BaseValue value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        return value1.compareTo(value2);
    }

}
