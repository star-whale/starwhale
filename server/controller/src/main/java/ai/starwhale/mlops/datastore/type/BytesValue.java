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
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class BytesValue implements ScalarValue {

    private ByteBuffer value;

    @Override
    public ColumnType getColumnType() {
        return ColumnType.BYTES;
    }

    @Override
    public ColumnSchemaDescBuilder generateColumnSchemaDesc() {
        return ColumnSchemaDesc.bytes();
    }

    @Override
    public int compareTo(@NonNull BaseValue other) {
        if (other instanceof BytesValue) {
            return this.value.compareTo(((BytesValue) other).value);
        } else if (other instanceof StringValue) {
            return this.value.compareTo(
                    ByteBuffer.wrap(((StringValue) other).getValue().getBytes(StandardCharsets.UTF_8)));
        }
        return Integer.compare(this.getColumnType().getIndex(), other.getColumnType().getIndex());
    }

    @Override
    public Object encode(boolean rawResult) {
        var v = this.value.duplicate();
        if (!rawResult) {
            v = Base64.getEncoder().encode(v);
        }
        return StandardCharsets.UTF_8.decode(v).toString();
    }

    @Override
    public Wal.Column.Builder encodeWal() {
        return Wal.Column.newBuilder()
                .setBytesValue(ByteString.copyFrom(this.value.array(), this.value.arrayOffset(), this.value.limit()));
    }

    @Override
    public String toString() {
        return "BYTES:" + this.value;
    }
}
