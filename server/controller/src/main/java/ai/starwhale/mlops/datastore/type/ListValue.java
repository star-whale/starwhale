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
import java.util.ArrayList;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class ListValue extends ArrayList<BaseValue> implements BaseValue {

    @Override
    public ColumnType getColumnType() {
        return ColumnType.LIST;
    }

    @Override
    public int compareTo(@NonNull BaseValue other) {
        if (other instanceof ListValue) {
            var iter1 = this.iterator();
            var iter2 = ((ListValue) other).iterator();
            for (; ; ) {
                if (iter1.hasNext() && iter2.hasNext()) {
                    var result = iter1.next().compareTo(iter2.next());
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
