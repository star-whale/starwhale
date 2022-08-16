package ai.starwhale.mlops.datastore;

import ai.starwhale.mlops.exception.SWValidationException;
import lombok.Getter;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Map;

@Getter
public enum ColumnType {
    // if the column type is UNKNOWN, all values in the column are null. It is used for those columns whose type is
    // unknown. A UNKNOWN column will be changed to other types when a value other than null is written into the column.
    UNKNOWN("unknown", 1),
    BOOL("bool", 1),
    INT8("int", 8),
    INT16("int", 16),
    INT32("int", 32),
    INT64("int", 64),
    FLOAT32("float", 32),
    FLOAT64("float", 64),
    STRING("string", 8),
    BYTES("bytes", 8);

    private static final Map<Class<?>, ColumnType> typeMap = Map.of(
            Boolean.class, BOOL,
            Byte.class, INT8,
            Short.class, INT16,
            Integer.class, INT32,
            Long.class, INT64,
            Float.class, FLOAT32,
            Double.class, FLOAT64,
            String.class, STRING);

    private final String name;
    private int nbits;

    ColumnType(String name, int nbits) {
        this.name = name;
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

    @SneakyThrows
    public String encode(Object value) {
        if (value == null) {
            return null;
        }
        switch (this) {
            case BOOL:
                return (Boolean) value ? "1" : "0";
            case INT8:
            case INT16:
            case INT32:
            case INT64:
                return Long.toHexString(((Number) value).longValue());
            case FLOAT32:
                return Integer.toHexString(Float.floatToIntBits((Float) value));
            case FLOAT64:
                return Long.toHexString(Double.doubleToLongBits((Double) value));
            case STRING:
                return (String) value;
            case BYTES:
                return Base64.getEncoder().encodeToString(((ByteBuffer) value).array());
            default:
                throw new IllegalArgumentException("invalid type " + this);
        }
    }

    public Object decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            switch (this) {
                case UNKNOWN:
                    throw new IllegalArgumentException("invalid unknown value " + value);
                case BOOL:
                    if (value.equals("1")) {
                        return true;
                    } else if (value.equals("0")) {
                        return false;
                    }
                    throw new IllegalArgumentException("invalid bool value " + value);
                case INT8:
                    return Byte.parseByte(value, 16);
                case INT16:
                    return Short.parseShort(value, 16);
                case INT32:
                    return Integer.parseInt(value, 16);
                case INT64:
                    return Long.parseLong(value, 16);
                case FLOAT32:
                    return Float.intBitsToFloat(Integer.parseInt(value, 16));
                case FLOAT64:
                    return Double.longBitsToDouble(Long.parseLong(value, 16));
                case STRING:
                    return value;
                case BYTES:
                    return ByteBuffer.wrap(Base64.getDecoder().decode(value));
                default:
                    throw new IllegalArgumentException("invalid type " + this);
            }
        } catch (Exception e) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    MessageFormat.format("can not decode value {0} for type {1}: {2}", value, this, e.getMessage()));
        }
    }
}
