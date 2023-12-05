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

package ai.starwhale.mlops.datastore.impl;

import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.TableSchema;
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
import ai.starwhale.mlops.datastore.type.TupleValue;
import ai.starwhale.mlops.exception.SwValidationException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

public class WalRecordDecoder {

    public static Map<String, BaseValue> decodeRecord(@NonNull TableSchema recordSchema, @NonNull Wal.Record record) {
        var ret = new HashMap<String, BaseValue>();
        for (var col : record.getColumnsList()) {
            var index = col.getIndex();
            if (index < 0) {
                ret.put(MemoryTableImpl.DELETED_FLAG_COLUMN_NAME, BoolValue.TRUE);
                continue;
            }
            var colName = recordSchema.getColumnNameByIndex(index);
            try {
                ret.put(colName, WalRecordDecoder.decodeValue(col));
            } catch (Exception e) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        MessageFormat.format("failed to decode wal {0}", col.toString()),
                        e);
            }
        }
        return ret;
    }

    public static BaseValue decodeValue(Wal.Column col) {
        if (col.getNullValue()) {
            return null;
        }
        var type = ColumnType.getTypeByIndex(col.getType());
        switch (type) {
            case UNKNOWN:
                return null;
            case BOOL:
                return BaseValue.valueOf(col.getBoolValue());
            case INT8:
                return new Int8Value((byte) col.getIntValue());
            case INT16:
                return new Int16Value((short) col.getIntValue());
            case INT32:
                return new Int32Value((int) col.getIntValue());
            case INT64:
                return new Int64Value(col.getIntValue());
            case FLOAT32:
                return new Float32Value(col.getFloatValue());
            case FLOAT64:
                return new Float64Value(col.getDoubleValue());
            case STRING:
                return new StringValue(col.getStringValue());
            case BYTES:
                return new BytesValue(ByteBuffer.wrap(col.getBytesValue().toByteArray()));
            case LIST:
                return WalRecordDecoder.decodeList(col);
            case TUPLE:
                return WalRecordDecoder.decodeTuple(col);
            case MAP:
                return WalRecordDecoder.decodeMap(col);
            case OBJECT:
                return WalRecordDecoder.decodeObject(col);
            default:
                throw new IllegalArgumentException("invalid type " + type);
        }
    }

    private static BaseValue decodeList(@NonNull Wal.Column col) {
        var ret = new ListValue();
        var values = col.getListValueList();
        for (Wal.Column value : values) {
            ret.add(WalRecordDecoder.decodeValue(value));
        }
        return ret;
    }

    private static BaseValue decodeTuple(@NonNull Wal.Column col) {
        var ret = new TupleValue();
        ret.addAll((ListValue) WalRecordDecoder.decodeList(col));
        return ret;
    }

    private static BaseValue decodeMap(@NonNull Wal.Column col) {
        var ret = new MapValue();
        for (var entry : col.getMapValueList()) {
            ret.put(WalRecordDecoder.decodeValue(entry.getKey()),
                    WalRecordDecoder.decodeValue(entry.getValue()));
        }
        return ret;
    }

    private static BaseValue decodeObject(@NonNull Wal.Column col) {
        var pythonType = col.getStringValue();
        var ret = new ObjectValue(pythonType);
        col.getObjectValueMap().forEach(
                (k, v) -> ret.put(k, WalRecordDecoder.decodeValue(v)));
        return ret;
    }
}
