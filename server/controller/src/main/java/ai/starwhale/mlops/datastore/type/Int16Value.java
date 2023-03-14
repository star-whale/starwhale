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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Int16Value implements IntValue {

    private short value;

    @Override
    public ColumnType getColumnType() {
        return ColumnType.INT16;
    }

    @Override
    public long longValue() {
        return this.value;
    }

    @Override
    public int compareTo(@NonNull BaseValue other) {
        if (other instanceof IntValue) {
            return Long.compare(this.value, ((IntValue) other).longValue());
        } else if (other instanceof FloatValue) {
            return Double.compare(this.value, ((FloatValue) other).doubleValue());
        }
        return Integer.compare(this.getColumnType().getIndex(), other.getColumnType().getIndex());
    }

    @Override
    public Object encode(boolean rawResult) {
        if (rawResult) {
            return Short.toString(this.value);
        } else {
            return StringUtils.leftPad(Integer.toHexString(this.value & 0xFFFF), 4, "0");
        }
    }


    @Override
    public Wal.Column.Builder encodeWal() {
        return Wal.Column.newBuilder().setIntValue(this.value);
    }

    @Override
    public String toString() {
        return "INT16:" + this.value;
    }
}
