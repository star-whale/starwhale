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

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc.ColumnSchemaDescBuilder;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.Wal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ObjectValue extends HashMap<String, BaseValue> implements BaseValue {

    private final String pythonType;

    public ObjectValue(String pythonType) {
        if (pythonType == null || pythonType.isEmpty()) {
            throw new IllegalArgumentException("pythonType should not be null or empty for OBJECT");
        }
        this.pythonType = pythonType;
    }

    public static ObjectValue valueOf(String pythonType, Map<String, Object> values) {
        var ret = new ObjectValue(pythonType);
        values.forEach((k, v) -> ret.put(k, BaseValue.valueOf(v)));
        return ret;
    }

    @Override
    public ColumnType getColumnType() {
        return ColumnType.OBJECT;
    }

    @Override
    public ColumnSchemaDescBuilder generateColumnSchemaDesc() {
        var attrs = this.entrySet()
                .stream()
                .map(entry -> entry.getValue().generateColumnSchemaDesc().name(entry.getKey()).build())
                .collect(Collectors.toList());
        return ColumnSchemaDesc.objectOf(this.pythonType, attrs);
    }

    @Override
    public int compareTo(@NonNull BaseValue other) {
        if (other instanceof ObjectValue) {
            if (!this.pythonType.equals(((ObjectValue) other).pythonType)) {
                return this.pythonType.compareTo(((ObjectValue) other).pythonType);
            }
            String minDiffKey = null;
            int result = 0;
            for (var key : Stream.concat(this.keySet().stream(), ((ObjectValue) other).keySet().stream())
                    .collect(Collectors.toSet())) {
                if (minDiffKey == null || key.compareTo(minDiffKey) < 0) {
                    var v1 = this.get(key);
                    var v2 = ((ObjectValue) other).get(key);
                    int t;
                    if (v1 == null) {
                        t = -1;
                    } else if (v2 == null) {
                        t = 1;
                    } else {
                        t = v1.compareTo(v2);
                    }
                    if (t != 0) {
                        minDiffKey = key;
                        result = t;
                    }
                }
            }
            return result;
        }
        return Integer.compare(this.getColumnType().getIndex(), other.getColumnType().getIndex());
    }

    @Override
    public Object encode(boolean rawResult, boolean encodeWithType) {
        var ret = new HashMap<>();
        for (var entry : this.entrySet()) {
            ret.put(entry.getKey(), BaseValue.encode(entry.getValue(), rawResult, encodeWithType));
        }
        if (encodeWithType) {
            return Map.of("type", this.getColumnType().name(), "pythonType", this.pythonType, "value", ret);
        } else {
            return ret;
        }
    }

    @Override
    public Wal.Column.Builder encodeWal() {
        var ret = Wal.Column.newBuilder();
        this.forEach((k, v) -> ret.putObjectValue(k, BaseValue.encodeWal(v).build()));
        ret.setStringValue(this.pythonType);
        return ret;
    }

    @Override
    public String toString() {
        return "OBJECT(" + this.pythonType + ")" + super.toString();
    }
}
