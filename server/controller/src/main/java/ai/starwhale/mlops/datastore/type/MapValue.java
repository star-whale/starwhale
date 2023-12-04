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
import ai.starwhale.mlops.datastore.ColumnSchemaDesc.KeyValuePairSchema;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.Wal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@EqualsAndHashCode(callSuper = true)
public class MapValue extends HashMap<BaseValue, BaseValue> implements BaseValue {

    @Override
    public ColumnType getColumnType() {
        return ColumnType.MAP;
    }

    @Override
    public ColumnSchemaDescBuilder generateColumnSchemaDesc() {
        if (this.isEmpty()) {
            return ColumnSchemaDesc.builder().type(ColumnType.MAP.name());
        }

        // (key type, value type) -> (count, index of entry set)
        Map<Pair<ColumnSchemaDesc, ColumnSchemaDesc>, Pair<Integer, Integer>> types = new HashMap<>();
        var idx = 0;
        for (var entry : this.entrySet()) {
            var keyType = entry.getKey().generateColumnSchemaDesc().build();
            var valueType = entry.getValue().generateColumnSchemaDesc().build();
            var pair = Pair.of(keyType, valueType);
            if (types.containsKey(pair)) {
                types.put(pair, Pair.of(types.get(pair).getKey() + 1, idx));
            } else {
                types.put(pair, Pair.of(1, idx));
            }
            idx++;
        }

        if (types.size() == 1) {
            var type = types.entrySet().stream().findFirst().get().getKey();
            return ColumnSchemaDesc.mapOf(type.getKey().toBuilder(), type.getValue().toBuilder());
        }

        // get the major type in the map entries
        var majorType = types.entrySet().stream()
                .max((e1, e2) -> {
                    var pair1 = e1.getValue();
                    var pair2 = e2.getValue();
                    if (!pair1.getKey().equals(pair2.getKey())) {
                        return Integer.compare(pair1.getKey(), pair2.getKey());
                    }
                    return Integer.compare(pair1.getValue(), pair2.getValue());
                })
                .get()
                .getKey();

        Map<Integer, KeyValuePairSchema> sparseKeyValuePairSchema = new HashMap<>();
        for (var entry : types.entrySet()) {
            var pair = entry.getKey();
            var offset = entry.getValue().getValue();
            var keyType = pair.getKey();
            var valueType = pair.getValue();
            if (keyType.equals(majorType.getKey()) && valueType.equals(majorType.getValue())) {
                continue;
            }
            sparseKeyValuePairSchema.put(offset, new KeyValuePairSchema(keyType, valueType));
        }

        return ColumnSchemaDesc.mapOf(
                majorType.getKey().toBuilder(),
                majorType.getValue().toBuilder(),
                sparseKeyValuePairSchema
        );
    }

    @Override
    public int compareTo(@NonNull BaseValue other) {
        if (other instanceof MapValue) {
            BaseValue minDiffKey = null;
            int result = 0;
            for (var key : Stream.concat(this.keySet().stream(), ((MapValue) other).keySet().stream())
                    .collect(Collectors.toSet())) {
                if (minDiffKey == null || key.compareTo(minDiffKey) < 0) {
                    var v1 = this.get(key);
                    var v2 = ((MapValue) other).get(key);
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
        if (encodeWithType) {
            var ret = new ArrayList<>();
            for (var entry : this.entrySet()) {
                ret.add(Map.of(
                        "key", BaseValue.encode(entry.getKey(), rawResult, true),
                        "value", BaseValue.encode(entry.getValue(), rawResult, true)
                ));
            }
            return Map.of("type", this.getColumnType().name(), "value", ret);
        } else {
            var ret = new HashMap<>();
            for (var entry : this.entrySet()) {
                ret.put(BaseValue.encode(entry.getKey(), rawResult, false),
                        BaseValue.encode(entry.getValue(), rawResult, false));
            }
            return ret;
        }
    }

    @Override
    public Wal.Column.Builder encodeWal() {
        var ret = Wal.Column.newBuilder();
        this.forEach((k, v) -> ret.addMapValue(
                Wal.Column.MapEntry.newBuilder()
                        .setKey(BaseValue.encodeWal(k))
                        .setValue(BaseValue.encodeWal(v))));
        return ret;
    }

    @Override
    public String toString() {
        return "MAP" + super.toString();
    }
}
