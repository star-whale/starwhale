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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ListValue extends ArrayList<BaseValue> implements BaseValue {

    @Override
    public ColumnType getColumnType() {
        return ColumnType.LIST;
    }

    @Override
    public ColumnSchemaDescBuilder generateColumnSchemaDesc() {
        if (this.isEmpty()) {
            return ColumnSchemaDesc.builder().type(ColumnType.LIST.name());
        }

        // get the major type in the list item
        // type -> (count, index)
        Map<ColumnSchemaDesc, Pair<Integer, Integer>> types = new HashMap<>();
        for (var i = 0; i < this.size(); i++) {
            var element = this.get(i);
            var type = element.generateColumnSchemaDesc().build();
            if (types.containsKey(type)) {
                var pair = types.get(type);
                types.put(type, Pair.of(pair.getKey() + 1, pair.getValue()));
            } else {
                types.put(type, Pair.of(1, i));
            }
        }

        if (types.size() == 1) {
            var type = types.keySet().iterator().next();
            return ColumnSchemaDesc.listOf(type.toBuilder());
        }

        var maxOpt = types.entrySet().stream().max((e1, e2) -> {
            var pair1 = e1.getValue();
            var pair2 = e2.getValue();
            if (!Objects.equals(pair1.getKey(), pair2.getKey())) {
                return Integer.compare(pair1.getKey(), pair2.getKey());
            }
            return Integer.compare(pair1.getValue(), pair2.getValue());
        });
        if (maxOpt.isEmpty()) {
            // this can not happen
            throw new RuntimeException("major type not found");
        }

        var majorType = maxOpt.get().getKey();

        List<ColumnSchemaDesc> attrs = new ArrayList<>();
        for (var entry : types.entrySet()) {
            var type = entry.getKey();
            if (type.equals(majorType)) {
                continue;
            }

            var pair = entry.getValue();
            // update index
            attrs.add(type.toBuilder().name("element").index(pair.getValue()).build());
        }

        var ret = ColumnSchemaDesc.listOf(majorType.toBuilder());
        attrs.sort(Comparator.comparingInt(ColumnSchemaDesc::getIndex));
        ret.attributes(attrs);
        return ret;
    }

    @Override
    public int compareTo(@NonNull BaseValue other) {
        if (other instanceof ListValue) {
            var iter1 = this.iterator();
            var iter2 = ((ListValue) other).iterator();
            for (; ; ) {
                if (iter1.hasNext() && iter2.hasNext()) {
                    var l = iter1.next();
                    var r = iter2.next();
                    if (l == null && r == null) {
                        continue;
                    } else if (l == null) {
                        return -1;
                    } else if (r == null) {
                        return 1;
                    }
                    var result = l.compareTo(r);
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
        return Integer.compare(this.getColumnType().getIndex(), other.getColumnType().getIndex());
    }

    @Override
    public Object encode(boolean rawResult, boolean encodeWithType) {
        var ret = new ArrayList<>();
        for (var element : this) {
            ret.add(BaseValue.encode(element, rawResult, encodeWithType));
        }
        if (encodeWithType) {
            return Map.of("type", this.getColumnType().name(), "value", ret);
        } else {
            return ret;
        }
    }

    @Override
    public Wal.Column.Builder encodeWal() {
        var ret = Wal.Column.newBuilder();
        this.stream().map(element -> BaseValue.encodeWal(element)).forEach(ret::addListValue);
        return ret;
    }

    @Override
    public String toString() {
        return "LIST" + super.toString();
    }
}
